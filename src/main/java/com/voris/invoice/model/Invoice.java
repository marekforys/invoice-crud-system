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

    public Invoice(String customerName) {
        if (customerName == null || customerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name cannot be null or empty");
        }
        this.id = UUID.randomUUID().toString();
        this.customerName = customerName.trim();
        this.date = LocalDate.now();
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
