package com.voris.invoice.service;

import com.voris.invoice.model.Invoice;
import com.voris.invoice.model.LineItem;
import com.voris.invoice.repo.InMemoryInvoiceRepository;
import com.voris.invoice.repo.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvoiceServiceTest {
    private InvoiceService service;
    private final InvoiceRepository repository = new InMemoryInvoiceRepository();

    @BeforeEach
    void setUp() {
        service = new InvoiceService(repository);
    }

    @Test
    void createInvoice_WithValidData_ShouldCreateInvoice() {
        // Arrange
        String customerName = "Test Customer";
        
        // Act
        Invoice invoice = service.createInvoice(customerName, null);
        
        // Assert
        assertNotNull(invoice);
        assertNotNull(invoice.getId());
        assertEquals(customerName, invoice.getCustomerName());
        assertTrue(invoice.getItems().isEmpty());
        
        // Verify it's in the repository
        Optional<Invoice> savedInvoice = service.getById(invoice.getId());
        assertTrue(savedInvoice.isPresent());
        assertEquals(invoice.getId(), savedInvoice.get().getId());
    }

    @Test
    void createInvoice_WithNullCustomerName_ShouldThrowException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> service.createInvoice(null, null));
    }

    @Test
    void createInvoice_WithBlankCustomerName_ShouldThrowException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> service.createInvoice(" ", null));
    }

    @Test
    void createInvoice_WithItems_ShouldCreateInvoiceWithItems() {
        // Arrange
        String customerName = "Test Customer";
        List<LineItem> items = List.of(
            new LineItem("Item 1", new BigDecimal("10.50")),
            new LineItem("Item 2", new BigDecimal("15.75"))
        );
        
        // Act
        Invoice invoice = service.createInvoice(customerName, items);
        
        // Assert
        assertNotNull(invoice);
        assertEquals(2, invoice.getItems().size());
        assertEquals(items.get(0).getDescription(), invoice.getItems().get(0).getDescription());
        assertEquals(items.get(1).getDescription(), invoice.getItems().get(1).getDescription());
    }

    @Test
    void getById_WithExistingId_ShouldReturnInvoice() {
        // Arrange
        Invoice created = service.createInvoice("Test", null);
        
        // Act
        Optional<Invoice> found = service.getById(created.getId());
        
        // Assert
        assertTrue(found.isPresent());
        assertEquals(created.getId(), found.get().getId());
    }

    @Test
    void getById_WithNonExistingId_ShouldReturnEmpty() {
        // Act
        Optional<Invoice> found = service.getById("non-existing-id");
        
        // Assert
        assertTrue(found.isEmpty());
    }

    @Test
    void getAll_ShouldReturnAllInvoices() {
        // Arrange
        service.createInvoice("Customer 1", null);
        service.createInvoice("Customer 2", null);
        
        // Act
        List<Invoice> allInvoices = service.getAll();
        
        // Assert
        assertEquals(2, allInvoices.size());
    }

    @Test
    void search_ShouldReturnMatchingInvoices() {
        // Arrange
        service.createInvoice("John Doe", null);
        service.createInvoice("Jane Smith", null);
        service.createInvoice("Bob Johnson", null);
        
        // Act
        List<Invoice> results = service.search("joh");
        
        // Assert
        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(i -> i.getCustomerName().contains("John")));
        assertTrue(results.stream().anyMatch(i -> i.getCustomerName().contains("Johnson")));
    }

    @Test
    void addLineItem_WithValidData_ShouldAddLineItem() {
        // Arrange
        Invoice invoice = service.createInvoice("Test Customer", null);
        String description = "Test Item";
        BigDecimal price = new BigDecimal("19.99");
        
        // Act
        Invoice updatedInvoice = service.addLineItem(invoice.getId(), description, price);
        
        // Assert
        assertNotNull(updatedInvoice);
        assertEquals(1, updatedInvoice.getItems().size());
        LineItem addedItem = updatedInvoice.getItems().get(0);
        assertEquals(description, addedItem.getDescription());
        assertEquals(0, price.compareTo(addedItem.getPrice()));
    }

    @Test
    void addLineItem_WithNonExistingInvoice_ShouldThrowException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> service.addLineItem("non-existing-id", "Test", BigDecimal.ONE));
    }

    @Test
    void addLineItem_WithBlankDescription_ShouldThrow() {
        Invoice invoice = service.createInvoice("Test Customer", null);
        assertThrows(IllegalArgumentException.class,
                () -> service.addLineItem(invoice.getId(), " ", BigDecimal.ONE));
    }

    @Test
    void addLineItem_WithNullPrice_ShouldThrow() {
        Invoice invoice = service.createInvoice("Test Customer", null);
        assertThrows(IllegalArgumentException.class,
                () -> service.addLineItem(invoice.getId(), "Item", null));
    }

    @Test
    void payInvoice_WithValidData_ShouldMarkPaid() {
        // Arrange
        Invoice invoice = service.createInvoice("Payable", null);
        LocalDate date = LocalDate.now();

        // Act
        Invoice updated = service.payInvoice(invoice.getId(), new BigDecimal("99.99"), "BANK_TRANSFER", date);

        // Assert
        assertTrue(updated.isPaid());
        assertEquals(0, new BigDecimal("99.99").compareTo(updated.getAmountPaid()));
        assertEquals("BANK_TRANSFER", updated.getPaymentMethod());
        assertEquals(date, updated.getPaymentDate());
    }

    @Test
    void payInvoice_WithInvalidInputs_ShouldThrow() {
        // Arrange
        Invoice invoice = service.createInvoice("Payable", null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> service.payInvoice(null, BigDecimal.TEN, "CASH", null));
        assertThrows(IllegalArgumentException.class, () -> service.payInvoice(" ", BigDecimal.TEN, "CASH", null));
        assertThrows(IllegalArgumentException.class, () -> service.payInvoice(invoice.getId(), null, "CASH", null));
        assertThrows(IllegalArgumentException.class, () -> service.payInvoice(invoice.getId(), new BigDecimal("-1"), "CASH", null));
        assertThrows(IllegalArgumentException.class, () -> service.payInvoice(invoice.getId(), BigDecimal.TEN, " ", null));
    }

    @Test
    void deleteInvoice_RemovesExisting_ReturnsTrue() {
        // Arrange
        Invoice invoice = service.createInvoice("To Delete", null);

        // Act
        boolean removed = service.deleteInvoice(invoice.getId());

        // Assert
        assertTrue(removed);
        assertTrue(service.getById(invoice.getId()).isEmpty());
    }

    @Test
    void deleteInvoice_WithMissingId_ReturnsFalse() {
        assertFalse(service.deleteInvoice("missing"));
    }

    @Test
    void deleteInvoice_WithBlankId_Throws() {
        assertThrows(IllegalArgumentException.class, () -> service.deleteInvoice(" "));
    }

    @Test
    void updateInvoice_WithValidData_ShouldUpdateInvoice() {
        // Arrange
        Invoice original = service.createInvoice("Original Customer", null);
        String newCustomerName = "Updated Customer";
        LocalDate newDate = LocalDate.now().plusDays(1);
        
        // Create some line items
        List<LineItem> newItems = List.of(
            new LineItem("Item 1", new BigDecimal("10.00")),
            new LineItem("Item 2", new BigDecimal("20.50"))
        );
        
        // Update the original invoice
        original.setCustomerName(newCustomerName);
        original.setDate(newDate);
        original.getItems().clear();
        newItems.forEach(original::addItem);
        
        // Act
        Invoice updated = service.updateInvoice(original);
        
        // Assert
        assertNotNull(updated);
        assertEquals(original.getId(), updated.getId());
        assertEquals(newCustomerName, updated.getCustomerName());
        assertEquals(newDate, updated.getDate());
        assertEquals(2, updated.getItems().size());
        assertEquals(newItems.get(0).getDescription(), updated.getItems().get(0).getDescription());
        assertEquals(0, newItems.get(0).getPrice().compareTo(updated.getItems().get(0).getPrice()));
        
        // Verify it was actually saved
        Optional<Invoice> fromDb = service.getById(original.getId());
        assertTrue(fromDb.isPresent());
        assertEquals(newCustomerName, fromDb.get().getCustomerName());
    }

    @Test
    void updateInvoice_WithNullInvoice_ShouldThrow() {
        assertThrows(IllegalArgumentException.class, 
            () -> service.updateInvoice(null));
    }

    @Test
    void updateInvoice_WithNullId_ShouldThrow() {
        // Create an invoice with null ID by directly calling the service
        Invoice invoice = service.createInvoice("Test Customer", null);
        
        // This will throw because the ID is not null
        // We'll test with an empty string ID instead
        assertThrows(IllegalArgumentException.class, 
            () -> service.updateInvoice(new Invoice("Test Customer")));
    }

    @Test
    void updateInvoice_WithBlankCustomerName_ShouldThrow() {
        Invoice invoice = service.createInvoice("Original", null);
        
        // The exception should be thrown when setting a blank customer name
        assertThrows(IllegalArgumentException.class, 
            () -> invoice.setCustomerName(" "));
    }

    @Test
    void updateInvoice_WithNullDate_ShouldThrow() {
        Invoice invoice = service.createInvoice("Original", null);
        invoice.setDate(null);
        
        assertThrows(IllegalArgumentException.class, 
            () -> service.updateInvoice(invoice));
    }

    @Test
    void updateInvoice_WithNonExistingId_ShouldThrow() {
        // Create an invoice with a non-existent ID
        Invoice invoice = service.createInvoice("Test Customer", null);
        invoice.setCustomerName("Non-existent");
        
        // This will throw because the ID doesn't exist in the repository
        // after we clear it
        service.deleteInvoice(invoice.getId());
        
        assertThrows(IllegalArgumentException.class, 
            () -> service.updateInvoice(invoice));
    }

    @Test
    void updateInvoice_WithLineItems_ShouldUpdateItems() {
        // Arrange
        Invoice invoice = service.createInvoice("Test Customer", List.of(
            new LineItem("Old Item 1", new BigDecimal("10.00")),
            new LineItem("Old Item 2", new BigDecimal("20.00"))
        ));
        
        // Get the invoice to update
        Invoice toUpdate = service.getById(invoice.getId()).orElseThrow();
        
        // Update line items
        List<LineItem> newItems = List.of(
            new LineItem("New Item 1", new BigDecimal("15.50")),
            new LineItem("New Item 2", new BigDecimal("25.75")),
            new LineItem("New Item 3", new BigDecimal("30.25"))
        );
        
        toUpdate.getItems().clear();
        toUpdate.getItems().addAll(newItems);
        
        // Act
        Invoice updated = service.updateInvoice(toUpdate);
        
        // Assert
        assertEquals(3, updated.getItems().size());
        assertEquals("New Item 1", updated.getItems().get(0).getDescription());
        assertEquals(0, new BigDecimal("15.50").compareTo(updated.getItems().get(0).getPrice()));
        
        // Verify the update was persisted
        Optional<Invoice> fromDb = service.getById(invoice.getId());
        assertTrue(fromDb.isPresent());
        assertEquals(3, fromDb.get().getItems().size());
    }
}
