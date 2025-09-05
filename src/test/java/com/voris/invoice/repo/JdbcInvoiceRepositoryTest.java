package com.voris.invoice.repo;

import com.voris.invoice.model.Invoice;
import com.voris.invoice.model.LineItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JdbcInvoiceRepositoryTest {
    private Path tempDb;

    private String jdbcUrlFor(Path path) {
        return "jdbc:sqlite:" + path.toAbsolutePath();
    }

    @AfterEach
    void cleanup() throws Exception {
        if (tempDb != null) {
            Files.deleteIfExists(tempDb);
        }
    }

    @Test
    void save_findAll_markPaid_flow_works() throws Exception {
        tempDb = Files.createTempFile("invoice-test-", ".db");
        JdbcInvoiceRepository repo = new JdbcInvoiceRepository(jdbcUrlFor(tempDb));

        Invoice inv = new Invoice("Customer A");
        inv.addItem(new LineItem("Item", new BigDecimal("12.34")));
        repo.save(inv);

        List<Invoice> all = repo.findAll();
        assertEquals(1, all.size());
        assertEquals("Customer A", all.get(0).getCustomerName());
        assertFalse(all.get(0).isPaid());

        Invoice paid = repo.markPaid(inv.getId(), new BigDecimal("12.34"), "CARD", LocalDate.now());
        assertTrue(paid.isPaid());
        assertEquals(0, new BigDecimal("12.34").compareTo(paid.getAmountPaid()));
        assertEquals("CARD", paid.getPaymentMethod());

        Invoice fetched = repo.findById(inv.getId()).orElseThrow();
        assertTrue(fetched.isPaid());
        assertEquals(0, new BigDecimal("12.34").compareTo(fetched.getAmountPaid()));
        assertEquals("CARD", fetched.getPaymentMethod());
    }
}


