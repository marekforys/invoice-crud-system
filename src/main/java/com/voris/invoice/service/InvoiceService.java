package com.voris.invoice.service;

import com.voris.invoice.model.Invoice;
import com.voris.invoice.model.LineItem;
import com.voris.invoice.repo.InvoiceRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class InvoiceService {
    private final InvoiceRepository repository;

    public InvoiceService(InvoiceRepository repository) {
        this.repository = repository;
    }

    public Invoice createInvoice(String customerName, List<LineItem> items) {
        if (customerName == null || customerName.isBlank()) {
            throw new IllegalArgumentException("Customer name is required");
        }
        Invoice invoice = new Invoice(customerName);
        if (items != null) {
            items.forEach(invoice::addItem);
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
        Invoice invoice = repository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));
        invoice.addItem(new LineItem(description, price));
        return repository.save(invoice);
    }
}
