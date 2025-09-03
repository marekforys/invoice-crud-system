package com.voris.invoice.repo;

import com.voris.invoice.model.Invoice;
import com.voris.invoice.model.LineItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

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
}
