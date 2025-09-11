package com.voris.invoice;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.voris.invoice.model.Invoice;
import com.voris.invoice.model.LineItem;
import com.voris.invoice.repo.JdbcInvoiceRepository;
import com.voris.invoice.service.InvoiceService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static spark.Spark.*;

public class ApiServer {
	public static void start() {
		port(8080);
		enableCORS("*");
		Gson gson = new GsonBuilder().create();

		String dbPath = System.getProperty("invoice.db", "jdbc:sqlite:invoice.db");
		InvoiceService service = new InvoiceService(new JdbcInvoiceRepository(dbPath));

		get("/health", (req, res) -> {
			res.type("text/plain");
			return "OK";
		});

		exception(Exception.class, (e, req, res) -> {
			e.printStackTrace();
			res.type("application/json");
			res.status(500);
			res.body(new Gson().toJson(Map.of(
					"error", "Internal Server Error",
					"message", e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()
			)));
		});

		notFound((req, res) -> {
			res.type("application/json");
			return new Gson().toJson(Map.of("error", "Not Found"));
		});

		get("/invoices", (req, res) -> {
			res.type("application/json");
			return gson.toJson(service.getAll().stream().map(ApiServer::toDto).toList());
		});

		get("/invoices/:id", (req, res) -> {
			res.type("application/json");
			return service.getById(req.params(":id"))
					.map(inv -> gson.toJson(toDto(inv)))
					.orElseGet(() -> {
						res.status(404);
						return gson.toJson(Map.of("error", "Not found"));
					});
		});

		get("/search", (req, res) -> {
			res.type("application/json");
			String q = req.queryParams("q");
			return gson.toJson(service.search(q).stream().map(ApiServer::toDto).toList());
		});

		post("/invoices", (req, res) -> {
			res.type("application/json");
			String contentType = req.contentType() == null ? "" : req.contentType().toLowerCase();
			String customerName;
			List<LineItem> items;
			if (contentType.contains("application/json")) {
				CreateInvoiceBody body = gson.fromJson(req.body(), CreateInvoiceBody.class);
				customerName = body == null ? null : (body.customerName != null ? body.customerName : req.queryParams("customerName"));
				if (customerName == null || customerName.isBlank()) {
					// Try fallback field name "name"
					customerName = body == null ? null : (String) (body.customerName == null ? null : body.customerName);
					if (customerName == null || customerName.isBlank()) {
						customerName = req.queryParams("name");
					}
				}
				items = (body != null && body.items != null) ? body.items : List.of();
			} else {
				// Handle HTML form posts (application/x-www-form-urlencoded or multipart)
				customerName = req.queryParams("customerName");
				if (customerName == null || customerName.isBlank()) {
					customerName = req.queryParams("name");
				}
				// Optional single item support from form: description, price
				String description = req.queryParams("description");
				String priceStr = req.queryParams("price");
				if (description != null && !description.isBlank() && priceStr != null && !priceStr.isBlank()) {
					try {
						BigDecimal price = new BigDecimal(priceStr.trim());
						items = List.of(new LineItem(description.trim(), price));
					} catch (NumberFormatException ex) {
						items = List.of();
					}
				} else {
					items = List.of();
				}
			}

			if (customerName == null || customerName.isBlank()) {
				res.status(400);
				return gson.toJson(Map.of("error", "customerName is required"));
			}

			Invoice created = service.createInvoice(customerName, items);
			res.status(201);
			return gson.toJson(toDto(created));
		});

		post("/invoices/:id/items", (req, res) -> {
			res.type("application/json");
			String id = req.params(":id");
			String contentType = req.contentType() == null ? "" : req.contentType().toLowerCase();
			String description;
			BigDecimal price;
			if (contentType.contains("application/json")) {
				LineItem body = gson.fromJson(req.body(), LineItem.class);
				description = body == null ? null : body.getDescription();
				price = body == null ? null : body.getPrice();
			} else {
				description = req.queryParams("description");
				String priceStr = req.queryParams("price");
				price = priceStr == null || priceStr.isBlank() ? null : new BigDecimal(priceStr.trim());
			}
			try {
				Invoice updated = service.addLineItem(id, description, price);
				return gson.toJson(toDto(updated));
			} catch (IllegalArgumentException e) {
				res.status(400);
				return gson.toJson(Map.of("error", e.getMessage()));
			}
		});

		// Replace all items for an invoice (supports add/edit/delete in one request)
		put("/invoices/:id/items", (req, res) -> {
			res.type("application/json");
			String id = req.params(":id");
			String contentType = req.contentType() == null ? "" : req.contentType().toLowerCase();
			List<LineItem> items;
			if (contentType.contains("application/json")) {
				UpdateItemsBody body = gson.fromJson(req.body(), UpdateItemsBody.class);
				items = body == null ? List.of() : (body.items == null ? List.of() : body.items);
			} else {
				items = List.of();
			}
			try {
				Invoice updated = service.updateLineItems(id, items);
				return gson.toJson(toDto(updated));
			} catch (IllegalArgumentException e) {
				res.status(400);
				return gson.toJson(Map.of("error", e.getMessage()));
			}
		});

		post("/invoices/:id/pay", (req, res) -> {
			res.type("application/json");
			String id = req.params(":id");
			PayBody body = gson.fromJson(req.body(), PayBody.class);
			try {
				Invoice updated = service.payInvoice(id,
						body.amount,
						body.method,
						body.date == null ? null : LocalDate.parse(body.date));
				return gson.toJson(toDto(updated));
			} catch (IllegalArgumentException e) {
				res.status(400);
				return gson.toJson(Map.of("error", e.getMessage()));
			}
		});

		delete("/invoices/:id", (req, res) -> {
			res.type("application/json");
			String id = req.params(":id");
			try {
				boolean removed = service.deleteInvoice(id);
				if (removed) {
					res.status(204);
					return "";
				} else {
					res.status(404);
					return gson.toJson(Map.of("error", "Not found"));
				}
			} catch (IllegalArgumentException e) {
				res.status(400);
				return gson.toJson(Map.of("error", e.getMessage()));
			}
		});

		init();
		awaitInitialization();
		System.out.println("API server is running on http://localhost:" + 8080);
	}

	private static void enableCORS(String origin) {
		options("/*", (request, response) -> {
			response.header("Access-Control-Allow-Origin", origin);
			response.header("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
			response.header("Access-Control-Allow-Headers", "Content-Type,Authorization");
			response.status(200);
			return "OK";
		});
		before((request, response) -> {
			response.header("Access-Control-Allow-Origin", origin);
			response.header("Access-Control-Allow-Credentials", "true");
			response.header("Access-Control-Allow-Headers", "Content-Type,Authorization");
		});
	}

	private static InvoiceDto toDto(Invoice inv) {
		InvoiceDto dto = new InvoiceDto();
		dto.id = inv.getId();
		dto.customerName = inv.getCustomerName();
		dto.date = inv.getDate() == null ? null : inv.getDate().toString();
		dto.total = inv.getTotal() == null ? null : inv.getTotal().toPlainString();
		dto.paid = inv.isPaid();
		dto.paymentDate = inv.getPaymentDate() == null ? null : inv.getPaymentDate().toString();
		dto.amountPaid = inv.getAmountPaid() == null ? null : inv.getAmountPaid().toPlainString();
		dto.paymentMethod = inv.getPaymentMethod();
		dto.items = inv.getItems();
		return dto;
	}

	private static class InvoiceDto {
		String id;
		String customerName;
		String date;
		String total;
		boolean paid;
		String paymentDate;
		String amountPaid;
		String paymentMethod;
		List<LineItem> items;
	}

	private static class CreateInvoiceBody {
		String customerName;
		List<LineItem> items;
	}

	private static class PayBody {
		BigDecimal amount;
		String method;
		String date; // ISO yyyy-MM-dd
	}

	private static class UpdateItemsBody {
		List<LineItem> items;
	}
}


