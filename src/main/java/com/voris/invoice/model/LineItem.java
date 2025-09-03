package com.voris.invoice.model;

import java.math.BigDecimal;
import java.util.Objects;

public class LineItem {
    private String description;
    private BigDecimal price;

    public LineItem() {
    }

    public LineItem(String description, BigDecimal price) {
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description cannot be null or empty");
        }
        if (price == null) {
            throw new IllegalArgumentException("Price cannot be null");
        }
        // Negative prices are allowed by tests (e.g., discounts)
        this.description = description;
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LineItem lineItem = (LineItem) o;
        return Objects.equals(description, lineItem.description) && 
               (price.compareTo(lineItem.price) == 0);
    }

    @Override
    public int hashCode() {
        return Objects.hash(description, price.stripTrailingZeros());
    }

    @Override
    public String toString() {
        return "LineItem{" +
                "description='" + description + '\'' +
                ", price=" + price +
                '}';
    }
}
