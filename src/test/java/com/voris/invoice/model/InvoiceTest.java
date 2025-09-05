package com.voris.invoice.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InvoiceTest {
    private Invoice invoice;
    private final String customerName = "Test Customer";

    @BeforeEach
    void setUp() {
        invoice = new Invoice(customerName);
    }

    @Test
    void constructor_WithValidData_ShouldCreateInvoice() {
        // Assert
        assertNotNull(invoice.getId());
        assertEquals(customerName, invoice.getCustomerName());
        assertNotNull(invoice.getDate());
        assertTrue(invoice.getItems().isEmpty());
    }

    @Test
    void constructor_WithNullCustomerName_ShouldThrowException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> new Invoice(null));
    }

    @Test
    void constructor_WithBlankCustomerName_ShouldThrowException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> new Invoice(" "));
    }

    @Test
    void addItem_WithValidItem_ShouldAddItem() {
        // Arrange
        LineItem item = new LineItem("Test Item", new BigDecimal("10.00"));
        
        // Act
        invoice.addItem(item);
        
        // Assert
        assertEquals(1, invoice.getItems().size());
        assertTrue(invoice.getItems().contains(item));
    }

    @Test
    void addItem_WithNullItem_ShouldThrowException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> invoice.addItem(null));
    }

    @Test
    void getTotal_WithNoItems_ShouldReturnZero() {
        // Act
        BigDecimal total = invoice.getTotal();
        
        // Assert
        assertEquals(0, BigDecimal.ZERO.compareTo(total));
    }

    @Test
    void getTotal_WithMultipleItems_ShouldReturnCorrectSum() {
        // Arrange
        invoice.addItem(new LineItem("Item 1", new BigDecimal("10.50")));
        invoice.addItem(new LineItem("Item 2", new BigDecimal("15.75")));
        
        // Act
        BigDecimal total = invoice.getTotal();
        
        // Assert
        assertEquals(0, new BigDecimal("26.25").compareTo(total));
    }

    @Test
    void equals_WithSameInstance_ShouldReturnTrue() {
        // Act & Assert
        assertEquals(invoice, invoice);
    }

    @Test
    void equals_WithSameValues_ShouldReturnTrue() {
        // Arrange
        Invoice anotherInvoice = new Invoice(customerName);
        
        // Act & Assert
        assertEquals(invoice, anotherInvoice);
    }

    @Test
    void equals_WithDifferentValues_ShouldReturnFalse() {
        // Arrange
        Invoice differentInvoice = new Invoice("Different Customer");
        
        // Act & Assert
        assertNotEquals(invoice, differentInvoice);
    }

    @Test
    void testHashCode() {
        // Arrange
        Invoice sameInvoice = new Invoice(customerName);
        
        // Act & Assert
        assertEquals(invoice.hashCode(), sameInvoice.hashCode());
    }

    @Test
    void testToString() {
        // Act
        String result = invoice.toString();
        
        // Assert
        assertTrue(result.contains("Invoice{"));
        assertTrue(result.contains("customerName='" + customerName + "'"));
    }

    @Test
    void markPaid_WithValidData_SetsPaymentFields() {
        // Act
        LocalDate date = LocalDate.now();
        invoice.markPaid(new BigDecimal("100.00"), "CARD", date);

        // Assert
        assertTrue(invoice.isPaid());
        assertEquals(0, new BigDecimal("100.00").compareTo(invoice.getAmountPaid()));
        assertEquals("CARD", invoice.getPaymentMethod());
        assertEquals(date, invoice.getPaymentDate());
    }

    @Test
    void markPaid_WithNullAmount_Throws() {
        assertThrows(IllegalArgumentException.class,
                () -> invoice.markPaid(null, "CARD", LocalDate.now()));
    }

    @Test
    void markPaid_WithNegativeAmount_Throws() {
        assertThrows(IllegalArgumentException.class,
                () -> invoice.markPaid(new BigDecimal("-1"), "CARD", LocalDate.now()));
    }

    @Test
    void markPaid_WithBlankMethod_Throws() {
        assertThrows(IllegalArgumentException.class,
                () -> invoice.markPaid(new BigDecimal("10"), " ", LocalDate.now()));
    }
}
