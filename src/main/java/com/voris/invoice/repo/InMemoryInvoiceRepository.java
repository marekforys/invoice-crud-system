package com.voris.invoice.repo;

import com.voris.invoice.model.Invoice;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.time.LocalDate;

public class InMemoryInvoiceRepository implements InvoiceRepository {
    private final Map<String, Invoice> store = new ConcurrentHashMap<>();

    @Override
    public Invoice save(Invoice invoice) {
        store.put(invoice.getId(), invoice);
        return invoice;
    }

    @Override
    public Optional<Invoice> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Invoice> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<Invoice> search(String query) {
        if (query == null) {
            return new ArrayList<>();
        }
        if (query.isBlank()) {
            return findAll();
        }
        
        final String q = query.trim().toLowerCase();
        return store.values().stream()
                .filter(inv -> inv.getCustomerName().toLowerCase().contains(q)
                        || inv.getItems().stream().anyMatch(i ->
                        i.getDescription() != null && 
                        i.getDescription().toLowerCase().contains(q)
                ))
                .collect(Collectors.toList());
    }

    @Override
    public Invoice markPaid(String invoiceId, BigDecimal amount, String method, LocalDate date) {
        if (invoiceId == null || invoiceId.trim().isEmpty()) {
            throw new IllegalArgumentException("Invoice ID cannot be null or empty");
        }
        Invoice invoice = findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found with ID: " + invoiceId));
        invoice.markPaid(amount, method, date);
        return save(invoice);
    }
}
