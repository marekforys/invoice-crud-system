package com.voris.invoice.service;

import com.voris.invoice.model.Invoice;
import com.voris.invoice.model.LineItem;
import com.voris.invoice.repo.InvoiceRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class InvoiceService {
    private final InvoiceRepository repository;

    public InvoiceService(InvoiceRepository repository) {
        this.repository = repository;
    }

    public Invoice createInvoice(String customerName, List<LineItem> items) {
        if (customerName == null || customerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name is required and cannot be blank");
        }
        Invoice invoice = new Invoice(customerName.trim());
        if (items != null) {
            items.forEach(item -> {
                if (item != null) {
                    invoice.addItem(item);
                }
            });
        }
        return repository.save(invoice);
    }

    public Optional<Invoice> getById(String id) {
        return repository.findById(id);
    }

    public List<Invoice> getAll() {
        return repository.findAll();
    }

    public List<Invoice> search(String query) {
        return repository.search(query);
    }

    public Invoice addLineItem(String invoiceId, String description, BigDecimal price) {
        if (invoiceId == null || invoiceId.trim().isEmpty()) {
            throw new IllegalArgumentException("Invoice ID cannot be null or empty");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description cannot be null or empty");
        }
        if (price == null) {
            throw new IllegalArgumentException("Price cannot be null");
        }
        
        Invoice invoice = repository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found with ID: " + invoiceId));
                
        invoice.addItem(new LineItem(description.trim(), price));
        return repository.save(invoice);
    }

    public Invoice payInvoice(String invoiceId, BigDecimal amount, String method, LocalDate date) {
        if (invoiceId == null || invoiceId.trim().isEmpty()) {
            throw new IllegalArgumentException("Invoice ID cannot be null or empty");
        }
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        if (method == null || method.trim().isEmpty()) {
            throw new IllegalArgumentException("Payment method cannot be null or empty");
        }
        return repository.markPaid(invoiceId.trim(), amount, method.trim(), date);
    }

    public boolean deleteInvoice(String invoiceId) {
        if (invoiceId == null || invoiceId.trim().isEmpty()) {
            throw new IllegalArgumentException("Invoice ID cannot be null or empty");
        }
        return repository.deleteById(invoiceId.trim());
    }
    
    public Invoice updateInvoice(Invoice invoice) {
        if (invoice == null) {
            throw new IllegalArgumentException("Invoice cannot be null");
        }
        if (invoice.getId() == null || invoice.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("Invoice ID cannot be null or empty");
        }
        if (invoice.getCustomerName() == null || invoice.getCustomerName().trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name cannot be null or empty");
        }
        if (invoice.getDate() == null) {
            throw new IllegalArgumentException("Invoice date cannot be null");
        }
        
        // Verify the invoice exists
        if (!repository.findById(invoice.getId()).isPresent()) {
            throw new IllegalArgumentException("Invoice not found with ID: " + invoice.getId());
        }
        
        return repository.save(invoice);
    }

    public Invoice updateLineItems(String invoiceId, List<LineItem> items) {
        if (invoiceId == null || invoiceId.trim().isEmpty()) {
            throw new IllegalArgumentException("Invoice ID cannot be null or empty");
        }
        Invoice invoice = repository.findById(invoiceId.trim())
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found with ID: " + invoiceId));

        invoice.getItems().clear();
        if (items != null) {
            for (LineItem item : items) {
                if (item == null) continue;
                if (item.getDescription() == null || item.getDescription().trim().isEmpty()) {
                    throw new IllegalArgumentException("Item description cannot be blank");
                }
                if (item.getPrice() == null) {
                    throw new IllegalArgumentException("Item price cannot be null");
                }
                invoice.addItem(new LineItem(item.getDescription().trim(), item.getPrice()));
            }
        }
        return repository.save(invoice);
    }
}
