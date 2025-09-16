package com.voris.invoice.repo;

import com.voris.invoice.model.Invoice;
import com.voris.invoice.model.LineItem;
import com.voris.invoice.model.Payment;

import java.util.UUID;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcInvoiceRepository implements InvoiceRepository {
    private final String jdbcUrl;

    public JdbcInvoiceRepository(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
        initializeSchema();
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }

    private void initializeSchema() {
        String createInvoices = "CREATE TABLE IF NOT EXISTS invoices (" +
                "id TEXT PRIMARY KEY, " +
                "customer_name TEXT NOT NULL, " +
                "date TEXT NOT NULL" +
                ")";
                
        String createPayments = "CREATE TABLE IF NOT EXISTS payments (" +
                "id TEXT PRIMARY KEY, " +
                "invoice_id TEXT NOT NULL, " +
                "amount TEXT NOT NULL, " +
                "method TEXT NOT NULL, " +
                "date TEXT NOT NULL, " +
                "reference TEXT, " +
                "FOREIGN KEY(invoice_id) REFERENCES invoices(id) ON DELETE CASCADE" +
                ")";
                
        String createItems = "CREATE TABLE IF NOT EXISTS line_items (" +
                "invoice_id TEXT NOT NULL, " +
                "description TEXT NOT NULL, " +
                "price TEXT, " +
                "FOREIGN KEY(invoice_id) REFERENCES invoices(id) ON DELETE CASCADE" +
                ")";
                
        try (Connection conn = getConnection();
             Statement st = conn.createStatement()) {
            st.execute(createInvoices);
            st.execute(createPayments);
            st.execute(createItems);
            
            // Migration: Drop old payment columns if they exist
            try (Statement alter = conn.createStatement()) {
                alter.execute("ALTER TABLE invoices DROP COLUMN IF EXISTS paid");
                alter.execute("ALTER TABLE invoices DROP COLUMN IF EXISTS payment_date");
                alter.execute("ALTER TABLE invoices DROP COLUMN IF EXISTS amount_paid");
                alter.execute("ALTER TABLE invoices DROP COLUMN IF EXISTS payment_method");
            } catch (SQLException e) {
                // Ignore if columns don't exist
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed initializing schema", e);
        }
    }

    @Override
    public Invoice save(Invoice invoice) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            // Save invoice basic info
            try (PreparedStatement upsert = conn.prepareStatement(
                    "INSERT INTO invoices(id, customer_name, date) VALUES(?,?,?) " +
                            "ON CONFLICT(id) DO UPDATE SET customer_name=excluded.customer_name, date=excluded.date")) {
                upsert.setString(1, invoice.getId());
                upsert.setString(2, invoice.getCustomerName());
                upsert.setString(3, invoice.getDate().toString());
                upsert.executeUpdate();
            }

            // Delete existing payments
            try (PreparedStatement deletePayments = conn.prepareStatement(
                    "DELETE FROM payments WHERE invoice_id = ?")) {
                deletePayments.setString(1, invoice.getId());
                deletePayments.executeUpdate();
            }

            // Insert current payments
            try (PreparedStatement insertPayment = conn.prepareStatement(
                    "INSERT INTO payments(id, invoice_id, amount, method, date, reference) VALUES(?,?,?,?,?,?)")) {
                for (Payment payment : invoice.getPaymentHistory()) {
                    insertPayment.setString(1, UUID.randomUUID().toString());
                    insertPayment.setString(2, invoice.getId());
                    insertPayment.setString(3, payment.getAmount().toPlainString());
                    insertPayment.setString(4, payment.getMethod());
                    insertPayment.setString(5, payment.getDate().toString());
                    insertPayment.setString(6, payment.getReference());
                    insertPayment.addBatch();
                }
                insertPayment.executeBatch();
            }

            try (PreparedStatement deleteItems = conn.prepareStatement("DELETE FROM line_items WHERE invoice_id = ?")) {
                deleteItems.setString(1, invoice.getId());
                deleteItems.executeUpdate();
            }

            try (PreparedStatement insertItem = conn.prepareStatement(
                    "INSERT INTO line_items(invoice_id, description, price) VALUES(?,?,?)")) {
                for (LineItem item : invoice.getItems()) {
                    insertItem.setString(1, invoice.getId());
                    insertItem.setString(2, item.getDescription());
                    insertItem.setString(3, item.getPrice() == null ? null : item.getPrice().toPlainString());
                    insertItem.addBatch();
                }
                insertItem.executeBatch();
            }

            conn.commit();
            return invoice;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save invoice", e);
        }
    }

    @Override
    public Optional<Invoice> findById(String id) {
        try (Connection conn = getConnection()) {
            String sql = "SELECT id, customer_name, date FROM invoices WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(mapInvoiceRow(rs, true));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find invoice", e);
        }
    }

    @Override
    public List<Invoice> findAll() {
        try (Connection conn = getConnection()) {
            String sql = "SELECT id, customer_name, date FROM invoices";
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            List<Invoice> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapInvoiceRow(rs, true));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list invoices", e);
        }
    }

    @Override
    public List<Invoice> search(String query) {
        if (query == null) return new ArrayList<>();
        String q = query.trim();
        if (q.isEmpty()) return findAll();

        try (Connection conn = getConnection()) {
            String sql = "SELECT DISTINCT i.id, i.customer_name, i.date " +
                    "FROM invoices i LEFT JOIN line_items li ON i.id = li.invoice_id " +
                    "WHERE LOWER(i.customer_name) LIKE ? OR LOWER(li.description) LIKE ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            String like = "%" + q.toLowerCase() + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            try (ResultSet rs = ps.executeQuery()) {
                List<Invoice> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapInvoiceRow(rs, true));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to search invoices", e);
        }
    }

    @Override
    public boolean deleteById(String id) {
        try (Connection conn = getConnection();
             PreparedStatement st = conn.prepareStatement("DELETE FROM invoices WHERE id = ?")) {
            st.setString(1, id);
            return st.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete invoice", e);
        }
    }
    
    @Override
    public List<Payment> getPaymentHistory(String invoiceId) {
        String sql = "SELECT amount, method, date, reference FROM payments WHERE invoice_id = ? ORDER BY date";
        
        try (Connection conn = getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            
            st.setString(1, invoiceId);
            ResultSet rs = st.executeQuery();
            
            List<Payment> payments = new ArrayList<>();
            while (rs.next()) {
                BigDecimal amount = new BigDecimal(rs.getString("amount"));
                String method = rs.getString("method");
                LocalDate date = LocalDate.parse(rs.getString("date"));
                String reference = rs.getString("reference");
                
                payments.add(new Payment(amount, method, date, reference));
            }
            
            return payments;
            
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch payment history", e);
        }
    }

    private Invoice mapInvoiceRow(ResultSet rs, boolean hasPayment) throws SQLException {
        String id = rs.getString("id");
        String customer = rs.getString("customer_name");
        LocalDate date = LocalDate.parse(rs.getString("date"));
        Invoice invoice = new Invoice(id, customer, date);
        
        // Load items and payments for this invoice
        try (Connection conn = getConnection()) {
            // Load line items first
            List<LineItem> items = loadItems(conn, id);
            for (LineItem item : items) {
                invoice.addItem(item);
            }
            
            // Then load payments
            List<Payment> payments = loadPayments(conn, id);
            for (Payment payment : payments) {
                invoice.addPayment(payment.getAmount(), payment.getMethod(), payment.getDate(), payment.getReference());
            }
        }
        
        return invoice;
    }


    @Override
    public Invoice addPayment(String invoiceId, BigDecimal amount, String method, LocalDate date, String reference) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Ensure invoice exists
                Invoice invoice = findById(invoiceId)
                        .orElseThrow(() -> new IllegalArgumentException("Invoice not found with ID: " + invoiceId));

                // Add new payment
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO payments(id, invoice_id, amount, method, date, reference) VALUES(?,?,?,?,?,?)")) {
                    ps.setString(1, UUID.randomUUID().toString());
                    ps.setString(2, invoiceId);
                    ps.setString(3, amount.toPlainString());
                    ps.setString(4, method);
                    ps.setString(5, (date != null ? date : LocalDate.now()).toString());
                    ps.setString(6, reference);
                    ps.executeUpdate();
                }

                // Commit and return updated invoice
                conn.commit();
                return findById(invoiceId)
                        .orElseThrow(() -> new IllegalStateException("Invoice disappeared after payment: " + invoiceId));
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add payment to invoice", e);
        }
    }
    
    private List<Payment> loadPayments(Connection conn, String invoiceId) throws SQLException {
        String sql = "SELECT amount, method, date, reference FROM payments WHERE invoice_id = ? ORDER BY date";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Payment> payments = new ArrayList<>();
                while (rs.next()) {
                    BigDecimal amount = new BigDecimal(rs.getString("amount"));
                    String method = rs.getString("method");
                    LocalDate date = LocalDate.parse(rs.getString("date"));
                    String reference = rs.getString("reference");
                    payments.add(new Payment(amount, method, date, reference));
                }
                return payments;
            }
        }
    }

    private List<LineItem> loadItems(Connection conn, String invoiceId) throws SQLException {
        String sql = "SELECT description, price FROM line_items WHERE invoice_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                List<LineItem> items = new ArrayList<>();
                while (rs.next()) {
                    String desc = rs.getString("description");
                    String priceStr = rs.getString("price");
                    BigDecimal price = priceStr == null ? null : new BigDecimal(priceStr);
                    items.add(new LineItem(desc, price));
                }
                return items;
            }
        }
    }
}


