package com.voris.invoice.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Invoice {
    private final String id;
    private String customerName;
    private LocalDate date;
    private final List<LineItem> items = new ArrayList<>();
    private final List<Payment> payments = new ArrayList<>();

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
        return getRemainingBalance().compareTo(BigDecimal.ZERO) <= 0;
    }

    public LocalDate getLastPaymentDate() {
        if (payments.isEmpty()) {
            return null;
        }
        return payments.get(payments.size() - 1).getDate();
    }

    public BigDecimal getAmountPaid() {
        return payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<Payment> getPaymentHistory() {
        List<Payment> sortedPayments = new ArrayList<>(payments);
        sortedPayments.sort((p1, p2) -> p1.getDate().compareTo(p2.getDate()));
        return sortedPayments;
    }
    
    public String getPaymentMethod() {
        if (payments.isEmpty()) {
            return null;
        }
        return payments.get(payments.size() - 1).getMethod();
    }
    
    /**
     * Adds a payment to this invoice
     * @param amount The payment amount (must be positive)
     * @param method The payment method (e.g., CASH, CARD, BANK_TRANSFER)
     * @param when The date of the payment (if null, uses current date)
     * @param reference Optional reference for the payment
     * @throws IllegalArgumentException if amount is null or not positive, or if method is null/empty
     */
    public void addPayment(BigDecimal amount, String method, LocalDate when, String reference) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (method == null || method.trim().isEmpty()) {
            throw new IllegalArgumentException("Payment method cannot be null or empty");
        }
        
        BigDecimal remainingBalance = getRemainingBalance();
        if (amount.compareTo(remainingBalance) > 0) {
            throw new IllegalArgumentException("Payment amount cannot exceed remaining balance");
        }
        
        Payment payment = new Payment(
            amount, 
            method.trim(), 
            when != null ? when : LocalDate.now(),
            reference != null ? reference : ""
        );
        payments.add(payment);
    }

    public BigDecimal getRemainingBalance() {
        return getTotal().subtract(getAmountPaid());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Invoice invoice = (Invoice) o;
        return Objects.equals(customerName, invoice.customerName) &&
               Objects.equals(date, invoice.date) &&
               Objects.equals(items, invoice.items) &&
               Objects.equals(payments, invoice.payments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerName, date, items, payments);
    }

    @Override
    public String toString() {
        return "Invoice{" +
                "id='" + id + '\'' +
                ", customerName='" + customerName + '\'' +
                ", date=" + date +
                ", total=" + getTotal() +
                ", amountPaid=" + getAmountPaid() +
                ", remainingBalance=" + getRemainingBalance() +
                ", paid=" + isPaid() +
                '}';
    }
}
