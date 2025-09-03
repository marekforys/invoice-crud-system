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
                "date TEXT NOT NULL" +
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
        } catch (SQLException e) {
            throw new RuntimeException("Failed initializing schema", e);
        }
    }

    @Override
    public Invoice save(Invoice invoice) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement upsert = conn.prepareStatement(
                    "INSERT INTO invoices(id, customer_name, date) VALUES(?,?,?) " +
                            "ON CONFLICT(id) DO UPDATE SET customer_name=excluded.customer_name, date=excluded.date")) {
                upsert.setString(1, invoice.getId());
                upsert.setString(2, invoice.getCustomerName());
                upsert.setString(3, invoice.getDate().toString());
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
        String sql = "SELECT id, customer_name, date FROM invoices WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                Invoice invoice = mapInvoiceRow(rs);
                invoice.getItems().addAll(loadItems(conn, id));
                return Optional.of(invoice);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find invoice", e);
        }
    }

    @Override
    public List<Invoice> findAll() {
        String sql = "SELECT id, customer_name, date FROM invoices";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Invoice> list = new ArrayList<>();
            while (rs.next()) {
                Invoice inv = mapInvoiceRow(rs);
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

        String sql = "SELECT DISTINCT i.id, i.customer_name, i.date " +
                "FROM invoices i LEFT JOIN line_items li ON i.id = li.invoice_id " +
                "WHERE LOWER(i.customer_name) LIKE ? OR LOWER(li.description) LIKE ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String like = "%" + q.toLowerCase() + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            try (ResultSet rs = ps.executeQuery()) {
                List<Invoice> list = new ArrayList<>();
                while (rs.next()) {
                    Invoice inv = mapInvoiceRow(rs);
                    inv.getItems().addAll(loadItems(conn, inv.getId()));
                    list.add(inv);
                }
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to search invoices", e);
        }
    }

    private Invoice mapInvoiceRow(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String customer = rs.getString("customer_name");
        LocalDate date = LocalDate.parse(rs.getString("date"));
        return new Invoice(id, customer, date);
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


