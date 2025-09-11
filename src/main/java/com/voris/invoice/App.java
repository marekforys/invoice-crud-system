package com.voris.invoice;

import com.voris.invoice.model.Invoice;
import com.voris.invoice.model.LineItem;
import com.voris.invoice.repo.InMemoryInvoiceRepository;
import com.voris.invoice.repo.JdbcInvoiceRepository;
import com.voris.invoice.service.InvoiceService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class App {
    private final InvoiceService service;

    public App() {
        String dbPath = System.getProperty("invoice.db", "jdbc:sqlite:invoice.db");
        this.service = new InvoiceService(new JdbcInvoiceRepository(dbPath));
    }

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("server")) {
            ApiServer.start();
        } else {
            new App().run();
        }
    }

    private void run() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            printMenu();
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> createInvoiceFlow(scanner);
                case "2" -> listInvoices();
                case "3" -> searchInvoices(scanner);
                case "4" -> addLineItemFlow(scanner);
                case "5" -> payInvoiceFlow(scanner);
                case "6" -> deleteInvoiceFlow(scanner);
                case "7" -> modifyInvoiceFlow(scanner);
                case "0" -> {
                    System.out.println("Goodbye!");
                    return;
                }
                default -> System.out.println("Invalid choice. Try again.");
            }
        }
    }

    private void printMenu() {
        System.out.println("\n=== Invoice CRUD System ===");
        System.out.println("1) Create invoice");
        System.out.println("2) List all invoices");
        System.out.println("3) Search invoices");
        System.out.println("4) Add line item to existing invoice");
        System.out.println("5) Pay invoice");
        System.out.println("6) Delete invoice");
        System.out.println("7) Modify invoice");
        System.out.println("0) Exit");
        System.out.print("Select: ");
    }

    private void createInvoiceFlow(Scanner scanner) {
        System.out.print("Customer name: ");
        String customer = scanner.nextLine().trim();

        List<LineItem> items = new ArrayList<>();
        while (true) {
            System.out.print("Add item? (y/n): ");
            String yn = scanner.nextLine().trim().toLowerCase();
            if (!yn.equals("y")) break;

            System.out.print("  Description: ");
            String desc = scanner.nextLine().trim();
            BigDecimal price = readPrice(scanner, "  Price: ");
            items.add(new LineItem(desc, price));
        }

        try {
            Invoice invoice = service.createInvoice(customer, items);
            System.out.println("Created invoice: " + invoice.getId());
        } catch (IllegalArgumentException ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }

    private void payInvoiceFlow(Scanner scanner) {
        System.out.print("Invoice ID: ");
        String id = scanner.nextLine().trim();
        BigDecimal amount = readPrice(scanner, "Amount paid: ");
        System.out.print("Payment method (e.g., CASH/CARD/BANK_TRANSFER): ");
        String method = scanner.nextLine().trim();
        System.out.print("Payment date (YYYY-MM-DD, blank for today): ");
        String dateStr = scanner.nextLine().trim();
        LocalDate date = null;
        if (!dateStr.isEmpty()) {
            try {
                date = LocalDate.parse(dateStr);
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date format. Using today.");
                date = null;
            }
        }
        try {
            Invoice updated = service.payInvoice(id, amount, method, date);
            System.out.println("Invoice paid successfully:");
            printInvoice(updated);
        } catch (IllegalArgumentException ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }

    private void listInvoices() {
        List<Invoice> invoices = service.getAll();
        if (invoices.isEmpty()) {
            System.out.println("No invoices found.");
            return;
        }
        invoices.forEach(this::printInvoice);
    }

    private void searchInvoices(Scanner scanner) {
        System.out.print("Search text (customer or item description): ");
        String q = scanner.nextLine().trim();
        List<Invoice> results = service.search(q);
        if (results.isEmpty()) {
            System.out.println("No matching invoices.");
            return;
        }
        results.forEach(this::printInvoice);
    }

    private void addLineItemFlow(Scanner scanner) {
        System.out.print("Invoice ID: ");
        String id = scanner.nextLine().trim();
        System.out.print("Description: ");
        String desc = scanner.nextLine().trim();
        BigDecimal price = readPrice(scanner, "Price: ");
        try {
            Invoice updated = service.addLineItem(id, desc, price);
            System.out.println("Updated invoice: ");
            printInvoice(updated);
        } catch (IllegalArgumentException ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }

    private void modifyInvoiceFlow(Scanner scanner) {
        System.out.print("Enter invoice ID to modify: ");
        String id = scanner.nextLine().trim();
        
        try {
            // Get the current invoice
            Invoice invoice = service.getById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found with ID: " + id));
            
            System.out.println("\nCurrent invoice details:");
            printInvoice(invoice);
            
            // Show modification menu
            while (true) {
                System.out.println("\nWhat would you like to modify?");
                System.out.println("1) Customer name");
                System.out.println("2) Invoice date");
                System.out.println("3) Line items");
                System.out.println("0) Back to main menu");
                System.out.print("Select: ");
                
                String choice = scanner.nextLine().trim();
                switch (choice) {
                    case "1" -> {
                        System.out.print("New customer name: ");
                        String newName = scanner.nextLine().trim();
                        invoice.setCustomerName(newName);
                        service.updateInvoice(invoice);
                        System.out.println("Customer name updated successfully.");
                    }
                    case "2" -> {
                        System.out.print("New invoice date (YYYY-MM-DD): ");
                        String dateStr = scanner.nextLine().trim();
                        try {
                            LocalDate newDate = LocalDate.parse(dateStr);
                            invoice.setDate(newDate);
                            service.updateInvoice(invoice);
                            System.out.println("Invoice date updated successfully.");
                        } catch (DateTimeParseException e) {
                            System.out.println("Invalid date format. Please use YYYY-MM-DD format.");
                        }
                    }
                    case "3" -> modifyLineItemsFlow(scanner, invoice);
                    case "0" -> { return; }
                    default -> System.out.println("Invalid choice. Please try again.");
                }
                
                // Show updated invoice
                System.out.println("\nUpdated invoice:");
                printInvoice(invoice);
            }
            
        } catch (IllegalArgumentException ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }
    
    private void modifyLineItemsFlow(Scanner scanner, Invoice invoice) {
        while (true) {
            System.out.println("\nCurrent line items:");
            List<LineItem> items = invoice.getItems();
            if (items.isEmpty()) {
                System.out.println("  No line items yet.");
            } else {
                for (int i = 0; i < items.size(); i++) {
                    LineItem item = items.get(i);
                    System.out.printf("%d) %s - $%.2f%n", 
                        i + 1, item.getDescription(), item.getPrice());
                }
            }
            
            System.out.println("\nOptions:");
            System.out.println("1) Add new item");
            System.out.println("2) Remove item");
            System.out.println("3) Modify item");
            System.out.println("0) Back to previous menu");
            System.out.print("Select: ");
            
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> {
                    System.out.print("Description: ");
                    String desc = scanner.nextLine().trim();
                    BigDecimal price = readPrice(scanner, "Price: ");
                    invoice.addItem(new LineItem(desc, price));
                    service.updateInvoice(invoice);
                    System.out.println("Item added successfully.");
                }
                case "2" -> {
                    if (items.isEmpty()) {
                        System.out.println("No items to remove.");
                        continue;
                    }
                    System.out.print("Enter item number to remove: ");
                    try {
                        int index = Integer.parseInt(scanner.nextLine()) - 1;
                        if (index >= 0 && index < items.size()) {
                            items.remove(index);
                            service.updateInvoice(invoice);
                            System.out.println("Item removed successfully.");
                        } else {
                            System.out.println("Invalid item number.");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Please enter a valid number.");
                    }
                }
                case "3" -> {
                    if (items.isEmpty()) {
                        System.out.println("No items to modify.");
                        continue;
                    }
                    System.out.print("Enter item number to modify: ");
                    try {
                        int index = Integer.parseInt(scanner.nextLine()) - 1;
                        if (index >= 0 && index < items.size()) {
                            LineItem item = items.get(index);
                            System.out.print("New description (leave blank to keep current): ");
                            String newDesc = scanner.nextLine().trim();
                            if (!newDesc.isEmpty()) {
                                item.setDescription(newDesc);
                            }
                            
                            System.out.print("New price (leave blank to keep current): ");
                            String priceStr = scanner.nextLine().trim();
                            if (!priceStr.isEmpty()) {
                                try {
                                    BigDecimal newPrice = new BigDecimal(priceStr);
                                    if (newPrice.compareTo(BigDecimal.ZERO) < 0) {
                                        System.out.println("Price cannot be negative. Keeping current price.");
                                    } else {
                                        item.setPrice(newPrice);
                                    }
                                } catch (NumberFormatException e) {
                                    System.out.println("Invalid price format. Keeping current price.");
                                }
                            }
                            
                            service.updateInvoice(invoice);
                            System.out.println("Item updated successfully.");
                        } else {
                            System.out.println("Invalid item number.");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Please enter a valid number.");
                    }
                }
                case "0" -> { return; }
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private void deleteInvoiceFlow(Scanner scanner) {
        System.out.print("Enter invoice ID to delete: ");
        String id = scanner.nextLine().trim();
        
        try {
            boolean deleted = service.deleteInvoice(id);
            if (deleted) {
                System.out.println("Invoice " + id + " has been deleted successfully.");
            } else {
                System.out.println("Invoice " + id + " was not found or could not be deleted.");
            }
        } catch (IllegalArgumentException ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }

    private BigDecimal readPrice(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = scanner.nextLine().trim();
            try {
                return new BigDecimal(s);
            } catch (NumberFormatException e) {
                System.out.println("  Invalid number. Try again.");
            }
        }
    }

    private void printInvoice(Invoice invoice) {
        System.out.println("-----------------------------");
        System.out.println("ID: " + invoice.getId());
        System.out.println("Customer: " + invoice.getCustomerName());
        System.out.println("Date: " + invoice.getDate());
        if (invoice.getItems().isEmpty()) {
            System.out.println("  (no line items)");
        } else {
            for (int i = 0; i < invoice.getItems().size(); i++) {
                LineItem it = invoice.getItems().get(i);
                System.out.printf("  %d. %s - %s%n", i + 1, it.getDescription(), it.getPrice());
            }
        }
        System.out.println("Total: " + invoice.getTotal());
        if (invoice.isPaid()) {
            System.out.println("Paid: YES");
            System.out.println("Payment date: " + invoice.getPaymentDate());
            System.out.println("Amount paid: " + invoice.getAmountPaid());
            System.out.println("Payment method: " + invoice.getPaymentMethod());
        } else {
            System.out.println("Paid: NO");
        }
        System.out.println("-----------------------------");
    }
}
