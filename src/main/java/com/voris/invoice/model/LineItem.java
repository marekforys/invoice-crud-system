package com.voris.invoice.model;

import java.math.BigDecimal;
import java.util.Objects;

public class LineItem {
    private String description;
    private BigDecimal price;

    public LineItem() {
    }

    public LineItem(String description, BigDecimal price) {
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
        return Objects.equals(description, lineItem.description) && Objects.equals(price, lineItem.price);
    }

    @Override
    public int hashCode() {
        return Objects.hash(description, price);
    }

    @Override
    public String toString() {
        return "LineItem{" +
                "description='" + description + '\'' +
                ", price=" + price +
                '}';
    }
}
