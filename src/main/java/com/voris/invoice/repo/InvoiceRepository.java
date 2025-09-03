package com.voris.invoice.repo;

import com.voris.invoice.model.Invoice;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository {
    Invoice save(Invoice invoice);

    Optional<Invoice> findById(String id);

    List<Invoice> findAll();

    List<Invoice> search(String query);
}
