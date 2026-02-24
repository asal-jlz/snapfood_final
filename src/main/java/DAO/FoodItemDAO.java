package DAO;

import model.FoodItem;
import model.Restaurant;
import utils.DB;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static utils.DB.getConnection;

public class FoodItemDAO {

    public List<FoodItem> getFoodItemsByRestaurant(int restaurantId) {
        List<FoodItem> list = new ArrayList<>();
        String sql = "SELECT * FROM food_items WHERE restaurant_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, restaurantId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToFoodItem(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public FoodItem getFoodItem(int id) {
        String sql = "SELECT * FROM food_items WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToFoodItem(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public FoodItem addFoodItem(FoodItem item) {
        String sql = "INSERT INTO food_items (restaurant_id, name, image_url, description, price, stock, category, keywords) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, item.getRestaurantId());
            stmt.setString(2, item.getName());
            stmt.setString(3, item.getImageUrl());
            stmt.setString(4, item.getDescription());
            stmt.setDouble(5, item.getPrice());
            stmt.setInt(6, item.getStock());
            stmt.setString(7, item.getCategory());
            stmt.setString(8, item.getKeywords());

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                item.setId(rs.getInt("id"));
                return item;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean updateFoodItem(FoodItem item) {
        String sql = "UPDATE food_items SET name = ?, image_url = ?, description = ?, price = ?, stock = ?, category = ?, keywords = ? " +
                "WHERE id = ? AND restaurant_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, item.getName());
            stmt.setString(2, item.getImageUrl());
            stmt.setString(3, item.getDescription());
            stmt.setDouble(4, item.getPrice());
            stmt.setInt(5, item.getStock());
            stmt.setString(6, item.getCategory());
            stmt.setString(7, item.getKeywords());
            stmt.setInt(8, item.getId());
            stmt.setInt(9, item.getRestaurantId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteFoodItem(int id, int restaurantId) {
        String sql = "DELETE FROM food_items WHERE id = ? AND restaurant_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.setInt(2, restaurantId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<FoodItem> searchFoodItems(String keyword, Double minPrice, Double maxPrice, String category, Double minRating) {
        List<FoodItem> results = new ArrayList<>();

        String sql = "SELECT * FROM food_items WHERE " +
                "(LOWER(name) LIKE ? OR LOWER(keywords) LIKE ? OR LOWER(description) LIKE ? " +
                "OR restaurant_id IN (SELECT id FROM restaurants WHERE LOWER(name) LIKE ?)) " +
                "AND price BETWEEN ? AND ? " +
                "AND (? IS NULL OR LOWER(category) = LOWER(?)) " +
                "AND (? IS NULL OR rating >= ?)";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String search = "%" + keyword.toLowerCase() + "%";
            stmt.setString(1, search);
            stmt.setString(2, search);
            stmt.setString(3, search);
            stmt.setString(4, search);
            stmt.setDouble(5, minPrice != null ? minPrice : 0);
            stmt.setDouble(6, maxPrice != null ? maxPrice : Double.MAX_VALUE);
            stmt.setString(7, category);
            stmt.setString(8, category);
            if (minRating != null) {
                stmt.setDouble(9, minRating);
                stmt.setDouble(10, minRating);
            } else {
                stmt.setNull(9, Types.DOUBLE);
                stmt.setNull(10, Types.DOUBLE);
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                results.add(mapResultSetToFoodItem(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return results;
    }

    public List<FoodItem> findRelatedFoodItems(String category, String keyword) {
        List<FoodItem> related = new ArrayList<>();
        String sql = "SELECT * FROM food_items WHERE LOWER(category) = LOWER(?) OR LOWER(keywords) LIKE ? LIMIT 5";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, category);
            stmt.setString(2, "%" + keyword.toLowerCase() + "%");

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                related.add(mapResultSetToFoodItem(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return related;
    }

    private FoodItem mapResultSetToFoodItem(ResultSet rs) throws SQLException {
        FoodItem item = new FoodItem();
        item.setId(rs.getInt("id"));
        item.setRestaurantId(rs.getInt("restaurant_id"));
        item.setName(rs.getString("name"));
        item.setImageUrl(rs.getString("image_url"));
        item.setDescription(rs.getString("description"));
        item.setPrice(rs.getDouble("price"));
        item.setStock(rs.getInt("stock"));
        item.setCategory(rs.getString("category"));
        item.setKeywords(rs.getString("keywords"));
        return item;
    }

    private Restaurant mapResultSetToRestaurant(ResultSet rs) throws SQLException {
        Restaurant r = new Restaurant();
        r.setId(rs.getInt("id"));
        r.setSellerId(rs.getInt("seller_id"));
        r.setName(rs.getString("name"));
        r.setAddress(rs.getString("address"));
        r.setPhone(rs.getString("phone"));
        r.setOpeningHours(rs.getString("opening_hours"));
        r.setLogoUrl(rs.getString("logo_url"));
        r.setApproved(rs.getBoolean("approved"));
        return r;
    }

    public List<FoodItem> getFoodsByCategory(String category) {
        List<FoodItem> list = new ArrayList<>();
        String sql = "SELECT * FROM food_items WHERE LOWER(category) = LOWER(?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, category);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToFoodItem(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<FoodItem> getFoodsByRestaurantName(String restaurantName) {
        List<FoodItem> list = new ArrayList<>();
        String sql = "SELECT fi.* FROM food_items fi " +
                "JOIN restaurants r ON fi.restaurant_id = r.id " +
                "WHERE LOWER(r.name) = LOWER(?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, restaurantName);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToFoodItem(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<String> getAllCategories() {
        List<String> categories = new ArrayList<>();
        String sql = "SELECT DISTINCT category FROM food_items";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                categories.add(rs.getString("category"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return categories;
    }

    public List<FoodItem> searchByName(String query) {
        List<FoodItem> list = new ArrayList<>();
        String sql = "SELECT * FROM food_items WHERE LOWER(name) LIKE ? OR LOWER(keywords) LIKE ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            String pattern = "%" + query.toLowerCase() + "%";
            stmt.setString(1, pattern);
            stmt.setString(2, pattern);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToFoodItem(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    public int getRestaurantIdByFoodId(int foodId) {
        String query = "SELECT restaurant_id FROM food_items WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, foodId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("restaurant_id");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

}