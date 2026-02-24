package DAO;

import model.Order;
import model.OrderItem;
import utils.DB;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static utils.DB.getConnection;

public class OrderDAO {

    public Order createOrder(Order order) {
        String orderSql = "INSERT INTO orders (user_id, restaurant_id, address, total_amount, tax, delivery_fee, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        String itemSql = "INSERT INTO order_items (order_id, food_item_id, quantity, unit_price) VALUES (?, ?, ?, ?)";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement orderStmt = conn.prepareStatement(orderSql);
                 PreparedStatement itemStmt = conn.prepareStatement(itemSql)) {

                orderStmt.setInt(1, order.getUserId());
                orderStmt.setInt(2, order.getRestaurantId());
                orderStmt.setString(3, order.getAddress());
                orderStmt.setDouble(4, order.getTotalAmount());
                orderStmt.setDouble(5, order.getTax());
                orderStmt.setDouble(6, order.getDeliveryFee());
                orderStmt.setString(7, order.getStatus());
                orderStmt.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));

                ResultSet rs = orderStmt.executeQuery();
                if (rs.next()) {
                    int orderId = rs.getInt("id");
                    for (OrderItem item : order.getItems()) {
                        itemStmt.setInt(1, orderId);
                        itemStmt.setInt(2, item.getFoodItemId());
                        itemStmt.setInt(3, item.getQuantity());
                        itemStmt.setDouble(4, item.getUnitPrice());
                        itemStmt.addBatch();
                    }
                    itemStmt.executeBatch();
                    conn.commit();
                    order.setId(orderId);
                    return order;
                } else {
                    conn.rollback();
                }
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Order> getOrdersByUser(int userId) {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT * FROM orders WHERE user_id = ? ORDER BY created_at DESC";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Order order = mapResultSetToOrder(rs);
                order.setItems(getOrderItems(order.getId()));
                orders.add(order);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return orders;
    }

    public List<Order> getOrdersByUserWithFilters(int userId, String status, LocalDate fromDate, LocalDate toDate) {
        List<Order> orders = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM orders WHERE user_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(userId);

        if (status != null && !status.isEmpty()) {
            sql.append(" AND status = ?");
            params.add(status);
        }

        if (fromDate != null) {
            sql.append(" AND created_at >= ?");
            params.add(Date.valueOf(fromDate));
        }

        if (toDate != null) {
            sql.append(" AND created_at <= ?");
            params.add(Date.valueOf(toDate));
        }

        sql.append(" ORDER BY created_at DESC");

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Order order = mapResultSetToOrder(rs);
                order.setItems(getOrderItems(order.getId()));
                orders.add(order);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return orders;
    }

    public static Order getOrderById(int orderId) {
        String sql = "SELECT * FROM orders WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, orderId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Order order = mapResultSetToOrder(rs);
                order.setItems(getOrderItems(orderId));
                return order;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean updateOrderStatus(int orderId, String status) {
        String sql = "UPDATE orders SET status = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, orderId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean assignCourier(int orderId, int courierId) {
        String sql = "UPDATE orders SET courier_id = ?, status = 'ON_THE_WAY' WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, courierId);
            stmt.setInt(2, orderId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static List<OrderItem> getOrderItems(int orderId) {
        List<OrderItem> items = new ArrayList<>();
        String sql = "SELECT * FROM order_items WHERE order_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, orderId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                OrderItem item = new OrderItem(
                        rs.getInt("id"),
                        rs.getInt("order_id"),
                        rs.getInt("food_item_id"),
                        rs.getInt("quantity"),
                        rs.getDouble("unit_price")
                );
                items.add(item);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    public List<Order> getOrdersByRestaurant(int restaurantId) {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT o.*, u.full_name as customer_name, u.phone as customer_phone " +
                "FROM orders o " +
                "JOIN users u ON o.user_id = u.id " +
                "WHERE o.restaurant_id = ? ORDER BY o.created_at DESC";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, restaurantId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Order order = mapResultSetToOrder(rs);
                order.setItems(getOrderItems(order.getId()));
                order.setCustomerName(rs.getString("customer_name"));
                order.setCustomerPhone(rs.getString("customer_phone"));
                orders.add(order);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return orders;
    }

    private static Order mapResultSetToOrder(ResultSet rs) throws SQLException {
        Order order = new Order();
        order.setId(rs.getInt("id"));
        order.setUserId(rs.getInt("user_id"));
        order.setRestaurantId(rs.getInt("restaurant_id"));
        order.setAddress(rs.getString("address"));
        order.setTotalAmount(rs.getDouble("total_amount"));
        order.setTax(rs.getDouble("tax"));
        order.setDeliveryFee(rs.getDouble("delivery_fee"));
        order.setStatus(rs.getString("status"));
        order.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());

        if (hasColumn(rs, "courier_id")) {
            Object courierObj = rs.getObject("courier_id");
            if (courierObj != null) {
                order.setCourierId(rs.getInt("courier_id"));
            }
        }

        return order;
    }

    public static List<Order> getOrdersByStatus(String status) {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT * FROM orders WHERE status = ? ORDER BY created_at DESC";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Order order = mapResultSetToOrder(rs);
                order.setItems(getOrderItems(order.getId()));
                orders.add(order);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return orders;
    }

    public static List<Order> getAllOrders() {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT * FROM orders ORDER BY created_at DESC";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Order order = mapResultSetToOrder(rs);
                order.setItems(getOrderItems(order.getId()));
                orders.add(order);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return orders;
    }


    private static boolean hasColumn(ResultSet rs, String column) {
        try {
            rs.findColumn(column);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public void addStatusHistory(int orderId, String newStatus) {
        String sql = "INSERT INTO order_status_history (order_id, status, changed_at) VALUES (?, ?, CURRENT_TIMESTAMP)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, orderId);
            stmt.setString(2, newStatus);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Retrieve status change history
    public List<String> getOrderStatusHistory(int orderId) {
        List<String> history = new ArrayList<>();
        String sql = "SELECT status, changed_at FROM order_status_history WHERE order_id = ? ORDER BY changed_at ASC";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, orderId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String statusRecord = rs.getString("changed_at") + " → " + rs.getString("status");
                history.add(statusRecord);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return history;
    }

    public List<Order> getOrdersByStatusAndNoCourier(String status) {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT * FROM orders WHERE status = ? AND courier_id IS NULL ORDER BY created_at DESC";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Order order = mapResultSetToOrder(rs);
                order.setItems(getOrderItems(order.getId()));
                orders.add(order);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return orders;
    }

    public List<Order> getOrdersByCourier(int courierId) {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT * FROM orders WHERE courier_id = ? ORDER BY created_at DESC";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, courierId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Order order = mapResultSetToOrder(rs);
                order.setItems(getOrderItems(order.getId()));
                orders.add(order);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return orders;
    }

    public Order insert(Order order) {
        try (Connection conn = getConnection()) {
            String insertSql = "INSERT INTO orders (user_id, restaurant_id, tax, delivery_fee, total_amount, status, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
            PreparedStatement stmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
            stmt.setInt(1, order.getUserId());
            stmt.setInt(2, order.getRestaurantId());
            stmt.setDouble(3, order.getTax());
            stmt.setDouble(4, order.getDeliveryFee());
            stmt.setDouble(5, order.getTotalAmount());
            stmt.setString(6, order.getStatus());
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int orderId = rs.getInt(1);
                order.setId(orderId);
            }

            // Insert order items (optional if you have order_items table)
            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    String itemSql = "INSERT INTO order_items (order_id, food_item_id, quantity, unit_price) VALUES (?, ?, ?, ?)";
                    PreparedStatement itemStmt = conn.prepareStatement(itemSql);
                    itemStmt.setInt(1, order.getId());
                    itemStmt.setInt(2, item.getFoodItemId());
                    itemStmt.setInt(3, item.getQuantity());
                    itemStmt.setDouble(4, item.getUnitPrice());
                    itemStmt.executeUpdate();
                }
            }

            return order;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public int getRestaurantIdByFoodId(int foodId) {
        String query = "SELECT restaurant_id FROM food_items WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, foodId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt("restaurant_id");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

}
