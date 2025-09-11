package com.voris.invoice.service;

import com.voris.invoice.model.Invoice;
import com.voris.invoice.model.LineItem;
import com.voris.invoice.repo.InMemoryInvoiceRepository;
import com.voris.invoice.repo.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

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
}
