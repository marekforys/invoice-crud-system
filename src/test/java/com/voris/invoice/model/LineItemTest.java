package com.voris.invoice.model;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class LineItemTest {

    @Test
    void constructor_WithValidData_ShouldCreateLineItem() {
        // Arrange
        String description = "Test Item";
        BigDecimal price = new BigDecimal("19.99");
        
        // Act
        LineItem item = new LineItem(description, price);
        
        // Assert
        assertEquals(description, item.getDescription());
        assertEquals(0, price.compareTo(item.getPrice()));
    }

    @Test
    void constructor_WithNullDescription_ShouldThrowException() {
        // Arrange
        BigDecimal price = new BigDecimal("19.99");
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> new LineItem(null, price));
    }

    @Test
    void constructor_WithBlankDescription_ShouldThrowException() {
        // Arrange
        BigDecimal price = new BigDecimal("19.99");
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> new LineItem(" ", price));
    }

    @Test
    void constructor_WithNullPrice_ShouldThrowException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> new LineItem("Test Item", null));
    }

    @Test
    void constructor_WithNegativePrice_ShouldAccept() {
        // The current implementation allows negative prices, so we'll test that it works
        // Arrange
        BigDecimal negativePrice = new BigDecimal("-10.00");
        
        // Act
        LineItem item = new LineItem("Test Item", negativePrice);
        
        // Assert
        assertEquals(0, negativePrice.compareTo(item.getPrice()));
    }

    @Test
    void setDescription_WithValidValue_ShouldUpdateDescription() {
        // Arrange
        LineItem item = new LineItem("Old Description", new BigDecimal("10.00"));
        String newDescription = "New Description";
        
        // Act
        item.setDescription(newDescription);
        
        // Assert
        assertEquals(newDescription, item.getDescription());
    }

    @Test
    void setPrice_WithValidValue_ShouldUpdatePrice() {
        // Arrange
        LineItem item = new LineItem("Test Item", new BigDecimal("10.00"));
        BigDecimal newPrice = new BigDecimal("15.50");
        
        // Act
        item.setPrice(newPrice);
        
        // Assert
        assertEquals(0, newPrice.compareTo(item.getPrice()));
    }

    @Test
    void equals_WithSameInstance_ShouldReturnTrue() {
        // Arrange
        LineItem item = new LineItem("Test", BigDecimal.ONE);
        
        // Act & Assert
        assertEquals(item, item);
    }

    @Test
    void equals_WithSameValues_ShouldReturnTrue() {
        // Arrange
        LineItem item1 = new LineItem("Test", BigDecimal.ONE);
        LineItem item2 = new LineItem("Test", BigDecimal.ONE);
        
        // Act & Assert
        assertEquals(item1, item2);
    }

    @Test
    void equals_WithDifferentValues_ShouldReturnFalse() {
        // Arrange
        LineItem item1 = new LineItem("Test 1", BigDecimal.ONE);
        LineItem item2 = new LineItem("Test 2", BigDecimal.TEN);
        
        // Act & Assert
        assertNotEquals(item1, item2);
    }
}
