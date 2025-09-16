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
        if (invoice == null) {
            throw new NullPointerException("Invoice cannot be null");
        }
        if (invoice.getItems() != null) {
            // Check for null items in the list
            if (invoice.getItems().contains(null)) {
                throw new NullPointerException("Invoice items cannot contain null");
            }
        }
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
    public List<Invoice> 
    search(String query) {
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
    public Invoice addPayment(String invoiceId, BigDecimal amount, String method, LocalDate date, String reference) {
        if (invoiceId == null || invoiceId.trim().isEmpty()) {
            throw new IllegalArgumentException("Invoice ID cannot be null or empty");
        }
        if (amount == null) {
            throw new NullPointerException("Amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (method == null) {
            throw new NullPointerException("Payment method cannot be null");
        }
        
        LocalDate paymentDate = (date != null) ? date : LocalDate.now();
        
        Invoice invoice = findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found with ID: " + invoiceId));
        invoice.addPayment(amount, method, paymentDate, reference);
        return save(invoice);
    }

    @Override
    public boolean deleteById(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Invoice ID cannot be null or empty");
        }
        return store.remove(id) != null;
    }
    
    @Override
    public List<com.voris.invoice.model.Payment> getPaymentHistory(String invoiceId) {
        if (invoiceId == null || invoiceId.trim().isEmpty()) {
            throw new IllegalArgumentException("Invoice ID cannot be null or empty");
        }
        
        return findById(invoiceId)
            .map(Invoice::getPaymentHistory)
            .orElse(Collections.emptyList());
    }
}
