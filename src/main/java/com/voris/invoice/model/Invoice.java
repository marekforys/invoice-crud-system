package com.voris.invoice.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Invoice {
    private final String id;
    private String customerName;
    private LocalDate date;
    private final List<LineItem> items = new ArrayList<>();
    private boolean paid;
    private LocalDate paymentDate;
    private BigDecimal amountPaid;
    private String paymentMethod;

    public Invoice(String customerName) {
        if (customerName == null || customerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name cannot be null or empty");
        }
        this.id = UUID.randomUUID().toString();
        this.customerName = customerName.trim();
        this.date = LocalDate.now();
    }

    // Persistence constructor
    public Invoice(String id, String customerName, LocalDate date) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Id cannot be null or empty");
        }
        if (customerName == null || customerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name cannot be null or empty");
        }
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }
        this.id = id.trim();
        this.customerName = customerName.trim();
        this.date = date;
    }

    public String getId() {
        return id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        if (customerName == null || customerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name cannot be null or empty");
        }
        this.customerName = customerName.trim();
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public List<LineItem> getItems() {
        return items;
    }

    public void addItem(LineItem item) {
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }
        this.items.add(item);
    }

    public BigDecimal getTotal() {
        return items.stream()
                .map(LineItem::getPrice)
                .filter(p -> p != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isPaid() {
        return paid;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void markPaid(BigDecimal amount, String method, LocalDate when) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        if (method == null || method.trim().isEmpty()) {
            throw new IllegalArgumentException("Payment method cannot be null or empty");
        }
        this.paid = true;
        this.amountPaid = amount;
        this.paymentMethod = method.trim();
        this.paymentDate = when == null ? LocalDate.now() : when;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Invoice invoice = (Invoice) o;
        return Objects.equals(customerName, invoice.customerName) &&
               Objects.equals(date, invoice.date) &&
               Objects.equals(items, invoice.items);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerName, date, items);
    }

    @Override
    public String toString() {
        return "Invoice{" +
                "customerName='" + customerName + '\'' +
                "}";
    }
}
