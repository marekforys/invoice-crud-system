package com.voris.invoice.repo;

import com.voris.invoice.model.Invoice;
import com.voris.invoice.model.LineItem;
import com.voris.invoice.model.Payment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JdbcInvoiceRepositoryTest {
    private Path tempDb;
    private JdbcInvoiceRepository repo;
    private Invoice testInvoice;

    @BeforeEach
    void setUp() throws Exception {
        tempDb = Files.createTempFile("invoice-test-", ".db");
        repo = new JdbcInvoiceRepository(jdbcUrlFor(tempDb));
        
        // Create a test invoice with items
        testInvoice = new Invoice("Test Customer");
        testInvoice.addItem(new LineItem("Test Item 1", new BigDecimal("25.00")));
        testInvoice.addItem(new LineItem("Test Item 2", new BigDecimal("35.50")));
        repo.save(testInvoice);
    }

    @AfterEach
    void cleanup() throws Exception {
        if (tempDb != null) {
            Files.deleteIfExists(tempDb);
        }
    }

    private String jdbcUrlFor(Path path) {
        return "jdbc:sqlite:" + path.toAbsolutePath();
    }

    @Test
    void addPayment_shouldAddPaymentToInvoice() {
        // Act
        Invoice updated = repo.addPayment(
            testInvoice.getId(), 
            new BigDecimal("30.00"), 
            "CARD", 
            LocalDate.of(2023, 1, 1), 
            "REF123"
        );
        
        // Assert
        List<Payment> payments = updated.getPaymentHistory();
        assertEquals(1, payments.size());
        assertEquals(0, new BigDecimal("30.00").compareTo(payments.get(0).getAmount()));
        assertEquals("CARD", payments.get(0).getMethod());
        assertEquals(LocalDate.of(2023, 1, 1), payments.get(0).getDate());
        assertEquals("REF123", payments.get(0).getReference());
    }
    
    @Test
    void addPayment_multiplePayments_shouldAccumulate() {
        // Arrange - Add first payment
        repo.addPayment(testInvoice.getId(), new BigDecimal("20.00"), "CASH", LocalDate.of(2023, 1, 1), "REF1");
        
        // Act - Add second payment
        Invoice updated = repo.addPayment(
            testInvoice.getId(), 
            new BigDecimal("15.50"), 
            "CARD", 
            LocalDate.of(2023, 1, 2), 
            "REF2"
        );
        
        // Assert
        List<Payment> payments = updated.getPaymentHistory();
        assertEquals(2, payments.size());
        assertEquals(0, new BigDecimal("35.50").compareTo(updated.getAmountPaid()));
        
        // Verify payment order (should be by date)
        assertEquals("REF1", payments.get(0).getReference());
        assertEquals("REF2", payments.get(1).getReference());
    }
    
    @Test
    void getPaymentHistory_shouldReturnAllPaymentsInOrder() {
        // Arrange - Add payments out of order
        repo.addPayment(testInvoice.getId(), new BigDecimal("20.00"), "CARD", LocalDate.of(2023, 1, 2), "LATER");
        repo.addPayment(testInvoice.getId(), new BigDecimal("10.00"), "CASH", LocalDate.of(2023, 1, 1), "EARLIER");
        
        // Act
        List<Payment> history = repo.getPaymentHistory(testInvoice.getId());
        
        // Assert
        assertEquals(2, history.size());
        // Should be ordered by date
        assertEquals("EARLIER", history.get(0).getReference());
        assertEquals("LATER", history.get(1).getReference());
    }
    
    @Test
    void saveAndRetrieveInvoice_withPayments() {
        // Arrange - Create an invoice with payments
        Invoice invoice = new Invoice("Customer with Payments");
        invoice.addItem(new LineItem("Item 1", new BigDecimal("50.00")));
        invoice.addPayment(new BigDecimal("25.00"), "CARD", LocalDate.of(2023, 1, 1), "PART1");
        invoice.addPayment(new BigDecimal("25.00"), "CASH", LocalDate.of(2023, 1, 2), "PART2");
        
        // Act - Save and retrieve
        Invoice saved = repo.save(invoice);
        Invoice retrieved = repo.findById(saved.getId()).orElseThrow();
        
        // Assert
        assertNotNull(retrieved);
        assertEquals(2, retrieved.getPaymentHistory().size());
        assertEquals(0, new BigDecimal("50.00").compareTo(retrieved.getAmountPaid()));
        assertTrue(retrieved.isPaid());
    }
    
    @Test
    void addPayment_withNonExistentInvoice_shouldThrow() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            repo.addPayment(
                "non-existent-id", 
                new BigDecimal("10.00"), 
                "CASH", 
                LocalDate.now(), 
                ""
            )
        );
    }
}


