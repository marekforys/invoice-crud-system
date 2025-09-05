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
}
