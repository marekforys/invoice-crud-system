package com.voris.invoice.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public class Payment {
    private final BigDecimal amount;
    private final String method;
    private final LocalDate date;
    private final String reference;

    public Payment(BigDecimal amount, String method, LocalDate date, String reference) {
        this.amount = amount;
        this.method = method;
        this.date = date != null ? date : LocalDate.now();
        this.reference = reference != null ? reference : "";
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getMethod() {
        return method;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getReference() {
        return reference;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Payment payment = (Payment) o;
        return Objects.equals(amount, payment.amount) &&
               Objects.equals(method, payment.method) &&
               Objects.equals(date, payment.date) &&
               Objects.equals(reference, payment.reference);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, method, date, reference);
    }

    @Override
    public String toString() {
        return "Payment{" +
                "amount=" + amount +
                ", method='" + method + '\'' +
                ", date=" + date +
                ", reference='" + reference + '\'' +
                '}';
    }
}
