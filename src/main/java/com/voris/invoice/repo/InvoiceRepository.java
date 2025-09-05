package com.voris.invoice.repo;

import com.voris.invoice.model.Invoice;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import java.time.LocalDate;

public interface InvoiceRepository {
    Invoice save(Invoice invoice);

    Optional<Invoice> findById(String id);

    List<Invoice> findAll();

    List<Invoice> search(String query);

    Invoice markPaid(String invoiceId, BigDecimal amount, String method, LocalDate date);
}
