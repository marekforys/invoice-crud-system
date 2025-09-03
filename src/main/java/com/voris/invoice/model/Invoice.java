package com.voris.invoice.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Invoice {
    private final String id;
    private String customerName;
    private LocalDate date;
    private final List<LineItem> items = new ArrayList<>();

    public Invoice(String customerName) {
        this.id = UUID.randomUUID().toString();
        this.customerName = customerName;
        this.date = LocalDate.now();
    }

    public String getId() {
        return id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
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
        this.items.add(item);
    }

    public BigDecimal getTotal() {
        return items.stream()
                .map(LineItem::getPrice)
                .filter(p -> p != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
