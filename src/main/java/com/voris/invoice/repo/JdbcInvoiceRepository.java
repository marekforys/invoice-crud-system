package com.voris.invoice.repo;

import com.voris.invoice.model.Invoice;
import com.voris.invoice.model.LineItem;

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
                "date TEXT NOT NULL, " +
                "paid INTEGER NOT NULL DEFAULT 0, " +
                "payment_date TEXT, " +
                "amount_paid TEXT, " +
                "payment_method TEXT" +
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
            st.execute(createItems);

            // Lightweight migration for existing DBs missing payment columns
            try (Statement alter = conn.createStatement()) {
                alter.execute("ALTER TABLE invoices ADD COLUMN IF NOT EXISTS paid INTEGER NOT NULL DEFAULT 0");
                alter.execute("ALTER TABLE invoices ADD COLUMN IF NOT EXISTS payment_date TEXT");
                alter.execute("ALTER TABLE invoices ADD COLUMN IF NOT EXISTS amount_paid TEXT");
                alter.execute("ALTER TABLE invoices ADD COLUMN IF NOT EXISTS payment_method TEXT");
            } catch (SQLException ignored) {
                // Older SQLite may not support IF NOT EXISTS for columns; try without and ignore duplicate errors
                try (Statement alter2 = conn.createStatement()) {
                    try { alter2.execute("ALTER TABLE invoices ADD COLUMN paid INTEGER NOT NULL DEFAULT 0"); } catch (SQLException e) { /* ignore duplicate */ }
                    try { alter2.execute("ALTER TABLE invoices ADD COLUMN payment_date TEXT"); } catch (SQLException e) { /* ignore duplicate */ }
                    try { alter2.execute("ALTER TABLE invoices ADD COLUMN amount_paid TEXT"); } catch (SQLException e) { /* ignore duplicate */ }
                    try { alter2.execute("ALTER TABLE invoices ADD COLUMN payment_method TEXT"); } catch (SQLException e) { /* ignore duplicate */ }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed initializing schema", e);
        }
    }

    @Override
    public Invoice save(Invoice invoice) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement upsert = conn.prepareStatement(
                    "INSERT INTO invoices(id, customer_name, date, paid, payment_date, amount_paid, payment_method) VALUES(?,?,?,?,?,?,?) " +
                            "ON CONFLICT(id) DO UPDATE SET customer_name=excluded.customer_name, date=excluded.date, paid=excluded.paid, payment_date=excluded.payment_date, amount_paid=excluded.amount_paid, payment_method=excluded.payment_method")) {
                upsert.setString(1, invoice.getId());
                upsert.setString(2, invoice.getCustomerName());
                upsert.setString(3, invoice.getDate().toString());
                upsert.setInt(4, invoice.isPaid() ? 1 : 0);
                upsert.setString(5, invoice.getPaymentDate() == null ? null : invoice.getPaymentDate().toString());
                upsert.setString(6, invoice.getAmountPaid() == null ? null : invoice.getAmountPaid().toPlainString());
                upsert.setString(7, invoice.getPaymentMethod());
                upsert.executeUpdate();
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
            boolean hasPayment = hasPaymentColumns(conn);
            String sql = hasPayment
                    ? "SELECT id, customer_name, date, paid, payment_date, amount_paid, payment_method FROM invoices WHERE id = ?"
                    : "SELECT id, customer_name, date FROM invoices WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                Invoice invoice = mapInvoiceRow(rs, hasPayment);
                invoice.getItems().addAll(loadItems(conn, id));
                return Optional.of(invoice);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find invoice", e);
        }
    }

    @Override
    public List<Invoice> findAll() {
        try (Connection conn = getConnection()) {
            boolean hasPayment = hasPaymentColumns(conn);
            String sql = hasPayment
                    ? "SELECT id, customer_name, date, paid, payment_date, amount_paid, payment_method FROM invoices"
                    : "SELECT id, customer_name, date FROM invoices";
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            List<Invoice> list = new ArrayList<>();
            while (rs.next()) {
                Invoice inv = mapInvoiceRow(rs, hasPayment);
                inv.getItems().addAll(loadItems(conn, inv.getId()));
                list.add(inv);
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
            boolean hasPayment = hasPaymentColumns(conn);
            String sql = (hasPayment
                    ? "SELECT DISTINCT i.id, i.customer_name, i.date, i.paid, i.payment_date, i.amount_paid, i.payment_method "
                    : "SELECT DISTINCT i.id, i.customer_name, i.date ") +
                    "FROM invoices i LEFT JOIN line_items li ON i.id = li.invoice_id " +
                    "WHERE LOWER(i.customer_name) LIKE ? OR LOWER(li.description) LIKE ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            String like = "%" + q.toLowerCase() + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            try (ResultSet rs = ps.executeQuery()) {
                List<Invoice> list = new ArrayList<>();
                while (rs.next()) {
                    Invoice inv = mapInvoiceRow(rs, hasPayment);
                    inv.getItems().addAll(loadItems(conn, inv.getId()));
                    list.add(inv);
                }
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to search invoices", e);
        }
    }

    private Invoice mapInvoiceRow(ResultSet rs, boolean hasPayment) throws SQLException {
        String id = rs.getString("id");
        String customer = rs.getString("customer_name");
        LocalDate date = LocalDate.parse(rs.getString("date"));
        Invoice invoice = new Invoice(id, customer, date);
        if (hasPayment) {
            int paid = rs.getInt("paid");
            boolean isPaid = paid == 1;
            if (isPaid) {
                String amountStr = rs.getString("amount_paid");
                String paymentDateStr = rs.getString("payment_date");
                String method = rs.getString("payment_method");
                invoice.markPaid(
                        amountStr == null ? BigDecimal.ZERO : new BigDecimal(amountStr),
                        method == null ? "" : method,
                        paymentDateStr == null ? null : LocalDate.parse(paymentDateStr)
                );
            }
        }
        return invoice;
    }

    private boolean hasPaymentColumns(Connection conn) {
        String pragma = "PRAGMA table_info(invoices)";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(pragma)) {
            boolean hasPaid = false;
            while (rs.next()) {
                String name = rs.getString("name");
                if ("paid".equalsIgnoreCase(name)) {
                    hasPaid = true;
                    break;
                }
            }
            return hasPaid;
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public Invoice markPaid(String invoiceId, BigDecimal amount, String method, LocalDate date) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Ensure invoice exists
                Invoice invoice = findById(invoiceId)
                        .orElseThrow(() -> new IllegalArgumentException("Invoice not found with ID: " + invoiceId));

                // Update payment fields
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE invoices SET paid = 1, payment_date = ?, amount_paid = ?, payment_method = ? WHERE id = ?")) {
                    ps.setString(1, date == null ? LocalDate.now().toString() : date.toString());
                    ps.setString(2, amount == null ? null : amount.toPlainString());
                    ps.setString(3, method);
                    ps.setString(4, invoiceId);
                    ps.executeUpdate();
                }

                // Commit before reloading so subsequent read sees committed data
                conn.commit();
                Invoice updated = findById(invoiceId)
                        .orElseThrow(() -> new IllegalStateException("Invoice disappeared after update: " + invoiceId));
                return updated;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to mark invoice as paid", e);
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


