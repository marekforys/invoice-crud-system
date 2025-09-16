package com.voris.invoice.repo;

import com.voris.invoice.model.Invoice;
import com.voris.invoice.model.LineItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryInvoiceRepositoryTest {
    private InMemoryInvoiceRepository repository;
    private Invoice testInvoice;

    @BeforeEach
    void setUp() {
        repository = new InMemoryInvoiceRepository();
        testInvoice = new Invoice("Test Customer");
        testInvoice.addItem(new LineItem("Test Item", new BigDecimal("10.00")));
    }

    @Test
    void save_NewInvoice_ShouldSaveAndReturnWithId() {
        // Act
        Invoice savedInvoice = repository.save(testInvoice);
        
        // Assert
        assertNotNull(savedInvoice.getId());
        assertEquals(testInvoice.getCustomerName(), savedInvoice.getCustomerName());
        assertEquals(1, savedInvoice.getItems().size());
    }

    @Test
    void save_ExistingInvoice_ShouldUpdateAndReturn() {
        // Arrange
        Invoice savedInvoice = repository.save(testInvoice);
        String originalId = savedInvoice.getId();
        
        // Modify the invoice
        savedInvoice.addItem(new LineItem("Another Item", new BigDecimal("15.00")));
        
        // Act
        Invoice updatedInvoice = repository.save(savedInvoice);
        
        // Assert
        assertEquals(originalId, updatedInvoice.getId());
        assertEquals(2, updatedInvoice.getItems().size());
    }

    @Test
    void findById_ExistingId_ShouldReturnInvoice() {
        // Arrange
        Invoice savedInvoice = repository.save(testInvoice);
        
        // Act
        Optional<Invoice> found = repository.findById(savedInvoice.getId());
        
        // Assert
        assertTrue(found.isPresent());
        assertEquals(savedInvoice.getId(), found.get().getId());
    }

    @Test
    void findById_NonExistingId_ShouldReturnEmpty() {
        // Act
        Optional<Invoice> found = repository.findById("non-existing-id");
        
        // Assert
        assertTrue(found.isEmpty());
    }

    @Test
    void findAll_ShouldReturnAllInvoices() {
        // Arrange
        repository.save(new Invoice("Customer 1"));
        repository.save(new Invoice("Customer 2"));
        
        // Act
        List<Invoice> allInvoices = repository.findAll();
        
        // Assert
        assertEquals(2, allInvoices.size());
    }

    @Test
    void search_ShouldReturnMatchingInvoices() {
        // Arrange
        repository.save(new Invoice("John Doe"));
        repository.save(new Invoice("Jane Smith"));
        repository.save(new Invoice("Bob Johnson"));
        
        // Act
        List<Invoice> results = repository.search("joh");
        
        // Assert
        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(i -> i.getCustomerName().contains("John")));
        assertTrue(results.stream().anyMatch(i -> i.getCustomerName().contains("Johnson")));
    }

    @Test
    void search_WithEmptyQuery_ShouldReturnAllInvoices() {
        // Arrange
        repository.save(new Invoice("Customer 1"));
        repository.save(new Invoice("Customer 2"));
        
        // Act
        List<Invoice> results = repository.search("");
        
        // Assert
        assertEquals(2, results.size());
    }

    @Test
    void search_WithNullQuery_ShouldReturnEmptyList() {
        // Act
        List<Invoice> results = repository.search(null);
        
        // Assert
        assertTrue(results.isEmpty());
    }

    @Test
    void search_ByItemDescription_ShouldReturnInvoicesContainingMatchingItems() {
        // Arrange
        Invoice inv1 = new Invoice("Acme Co");
        inv1.addItem(new LineItem("Consulting", new BigDecimal("100.00")));
        Invoice inv2 = new Invoice("Beta LLC");
        inv2.addItem(new LineItem("Hardware", new BigDecimal("50.00")));
        repository.save(inv1);
        repository.save(inv2);

        // Act
        List<Invoice> results = repository.search("consult");

        // Assert
        assertEquals(1, results.size());
        assertEquals(inv1.getId(), results.get(0).getId());
    }

    @Test
    void search_IsCaseInsensitive_And_TrimsWhitespace() {
        // Arrange
        Invoice inv = new Invoice("Gamma Inc");
        inv.addItem(new LineItem("Cloud Services", new BigDecimal("25.00")));
        repository.save(inv);

        // Act
        List<Invoice> results = repository.search("  CLOUd   ");

        // Assert
        assertEquals(1, results.size());
        assertEquals(inv.getId(), results.get(0).getId());
    }

    @Test
    void addPayment_ShouldAddPaymentToInvoice() {
        // Arrange
        testInvoice.addItem(new LineItem("Test Item", new BigDecimal("100.00")));
        Invoice saved = repository.save(testInvoice);
        LocalDate date = LocalDate.now();
        
        // Act
        Invoice updated = repository.addPayment(
            saved.getId(), 
            new BigDecimal("25.50"), 
            "CASH", 
            date, 
            "REF123"
        );
        
        // Assert
        List<com.voris.invoice.model.Payment> payments = updated.getPaymentHistory();
        assertEquals(1, payments.size());
        assertEquals(0, new BigDecimal("25.50").compareTo(payments.get(0).getAmount()));
        assertEquals("CASH", payments.get(0).getMethod());
        assertEquals(date, payments.get(0).getDate());
        assertEquals("REF123", payments.get(0).getReference());
    }
    
    @Test
    void addPayment_WithMultiplePayments_ShouldAccumulate() {
        // Arrange
        testInvoice.addItem(new LineItem("Test Item", new BigDecimal("100.00")));
        Invoice saved = repository.save(testInvoice);
        LocalDate date = LocalDate.now();
        
        // Act - Add two partial payments
        repository.addPayment(saved.getId(), new BigDecimal("10.00"), "CARD", date, "REF1");
        Invoice updated = repository.addPayment(
            saved.getId(), 
            new BigDecimal("15.50"), 
            "CASH", 
            date.plusDays(1), 
            "REF2"
        );
        
        // Assert
        List<com.voris.invoice.model.Payment> payments = updated.getPaymentHistory();
        assertEquals(2, payments.size());
        assertEquals(0, new BigDecimal("25.50").compareTo(updated.getAmountPaid()));
    }
    
    @Test
    void addPayment_WithNonExistentInvoice_ShouldThrow() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            repository.addPayment(
                "non-existent-id", 
                new BigDecimal("10.00"), 
                "CASH", 
                LocalDate.now(), 
                ""
            )
        );
    }
    
    @Test
    void getPaymentHistory_ReturnsAllPaymentsInOrder() {
        // Arrange
        testInvoice.addItem(new LineItem("Test Item", new BigDecimal("100.00")));
        Invoice saved = repository.save(testInvoice);
        LocalDate date1 = LocalDate.of(2023, 1, 1);
        LocalDate date2 = LocalDate.of(2023, 1, 2);
        
        // Add payments out of order
        repository.addPayment(saved.getId(), new BigDecimal("20.00"), "CARD", date2, "LATER");
        repository.addPayment(saved.getId(), new BigDecimal("10.00"), "CASH", date1, "EARLIER");
        
        // Act
        List<com.voris.invoice.model.Payment> history = repository.getPaymentHistory(saved.getId());
        
        // Assert
        assertEquals(2, history.size());
        // Should be ordered by date
        assertEquals("EARLIER", history.get(0).getReference());
        assertEquals("LATER", history.get(1).getReference());
    }

    @Test
    void deleteById_RemovesInvoiceAndReturnsTrue_WhenPresent() {
        // Arrange
        Invoice saved = repository.save(new Invoice("Del Customer"));
        String id = saved.getId();

        // Act
        boolean removed = repository.deleteById(id);

        // Assert
        assertTrue(removed);
        assertTrue(repository.findById(id).isEmpty());
    }

    @Test
    void deleteById_ReturnsFalse_WhenNotPresent() {
        // Act
        boolean removed = repository.deleteById("missing");

        // Assert
        assertFalse(removed);
    }
}
