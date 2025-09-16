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

/**
 * JDBC implementation of the InvoiceRepository interface.
 * Provides database persistence for invoices, payments, and line items using SQLite.
 * Handles all CRUD operations and maintains data consistency through transactions.
 */

public class JdbcInvoiceRepository implements InvoiceRepository {
    /** JDBC connection URL for the SQLite database */
    private final String jdbcUrl;
    
    /**
     * Constructs a new JdbcInvoiceRepository with the specified JDBC URL.
     * Initializes the database schema if it doesn't exist.
     * 
     * @param jdbcUrl the JDBC URL for the SQLite database
     */

    public JdbcInvoiceRepository(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
        initializeSchema();
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }

    /**
     * Initializes the database schema by creating required tables if they don't exist.
     * Also handles migration by dropping old columns if they exist.
     */
    private void initializeSchema() {
        // SQL statement to create the invoices table
        String createInvoices = "CREATE TABLE IF NOT EXISTS invoices (" +
                "id TEXT PRIMARY KEY, " +           // Unique identifier for the invoice
                "customer_name TEXT NOT NULL, " +   // Name of the customer
                "date TEXT NOT NULL" +              // Date of the invoice (stored as ISO-8601 string)
                ")";
                
        // SQL statement to create the payments table
        String createPayments = "CREATE TABLE IF NOT EXISTS payments (" +
                "id TEXT PRIMARY KEY, " +           // Unique identifier for the payment
                "invoice_id TEXT NOT NULL, " +      // Reference to the invoice
                "amount TEXT NOT NULL, " +          // Payment amount (stored as string for precision)
                "method TEXT NOT NULL, " +          // Payment method (e.g., CASH, CREDIT_CARD)
                "date TEXT NOT NULL, " +            // Payment date (stored as ISO-8601 string)
                "reference TEXT, " +                // Optional payment reference
                "FOREIGN KEY(invoice_id) REFERENCES invoices(id) ON DELETE CASCADE" +  // Cascade delete payments when invoice is deleted
                ")";
                
        // SQL statement to create the line_items table
        String createItems = "CREATE TABLE IF NOT EXISTS line_items (" +
                "invoice_id TEXT NOT NULL, " +      // Reference to the invoice
                "description TEXT NOT NULL, " +      // Item description
                "price TEXT, " +                    // Item price (stored as string for precision, nullable)
                "FOREIGN KEY(invoice_id) REFERENCES invoices(id) ON DELETE CASCADE" +  // Cascade delete items when invoice is deleted
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

    /**
     * Saves an invoice to the database. If the invoice already exists, it updates it.
     * Handles saving/updating invoice details, payments, and line items in a transaction.
     * 
     * @param invoice the invoice to save
     * @return the saved invoice with updated data
     * @throws RuntimeException if there's an error during database operations
     */
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

    /**
     * Finds an invoice by its ID.
     * 
     * @param id the ID of the invoice to find
     * @return an Optional containing the found invoice, or empty if not found
     * @throws RuntimeException if there's an error during database operations
     */
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

    /**
     * Retrieves all invoices from the database.
     * 
     * @return a list of all invoices, or an empty list if none found
     * @throws RuntimeException if there's an error during database operations
     */
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

    /**
     * Searches for invoices by customer name or line item description.
     * 
     * @param query the search term (case-insensitive)
     * @return a list of matching invoices, or an empty list if no matches found
     * @throws RuntimeException if there's an error during database operations
     */
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

    /**
     * Deletes an invoice by its ID.
     * Due to foreign key constraints with CASCADE, this will also delete
     * associated payments and line items.
     * 
     * @param id the ID of the invoice to delete
     * @return true if the invoice was deleted, false if no invoice was found with the given ID
     * @throws RuntimeException if there's an error during database operations
     */
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
    
    /**
     * Retrieves the payment history for a specific invoice.
     * 
     * @param invoiceId the ID of the invoice
     * @return a list of payments for the specified invoice, ordered by date
     * @throws RuntimeException if there's an error during database operations
     */
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

    /**
     * Maps a database result set row to an Invoice object.
     * Loads associated line items and payments for the invoice.
     * 
     * @param rs the ResultSet containing invoice data
     * @param hasPayment whether to load payment information (can be false for performance in some cases)
     * @return a populated Invoice object
     * @throws SQLException if there's an error accessing the database
     */
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


    /**
     * Adds a payment to an existing invoice.
     * 
     * @param invoiceId the ID of the invoice to add the payment to
     * @param amount the payment amount
     * @param method the payment method (e.g., CASH, CREDIT_CARD)
     * @param date the payment date (if null, uses current date)
     * @param reference optional payment reference
     * @return the updated invoice with the new payment
     * @throws IllegalArgumentException if no invoice exists with the given ID
     * @throws RuntimeException if there's an error during database operations
     */
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
    
    /**
     * Loads all payments for a specific invoice.
     * 
     * @param conn the database connection to use
     * @param invoiceId the ID of the invoice to load payments for
     * @return a list of payments for the specified invoice, ordered by date
     * @throws SQLException if there's an error executing the database query
     */
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

    /**
     * Loads all line items for a specific invoice.
     * 
     * @param conn the database connection to use
     * @param invoiceId the ID of the invoice to load items for
     * @return a list of line items for the specified invoice
     * @throws SQLException if there's an error executing the database query
     */
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


