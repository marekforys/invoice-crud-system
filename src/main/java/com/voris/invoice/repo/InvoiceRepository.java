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

    Invoice addPayment(String invoiceId, BigDecimal amount, String method, LocalDate date, String reference);

    boolean deleteById(String id);
    
    /**
     * Retrieves the payment history for a specific invoice.
     * @param invoiceId The ID of the invoice to get payment history for
     * @return List of payments in chronological order (oldest first)
     * @throws IllegalArgumentException if invoiceId is null or empty
     */
    List<com.voris.invoice.model.Payment> getPaymentHistory(String invoiceId);
}
