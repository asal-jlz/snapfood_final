package DAO;

import model.Restaurant;
import utils.DB;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RestaurantDAO {

    public void addRestaurant(Restaurant r) throws SQLException {
        String sql = "INSERT INTO restaurants (seller_id, name, address, phone, opening_hours, logo_url, approved) VALUES (?, ?, ?, ?, ?, ?, false)";
        try (Connection conn = DB.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, r.getSellerId());
            stmt.setString(2, r.getName());
            stmt.setString(3, r.getAddress());
            stmt.setString(4, r.getPhone());
            stmt.setString(5, r.getOpeningHours());
            stmt.setString(6, r.getLogoUrl());
            stmt.executeUpdate();
        }
    }


    public List<Restaurant> getPendingRestaurants() throws SQLException {
        List<Restaurant> list = new ArrayList<>();
        String sql = "SELECT * FROM restaurants WHERE approved = false";
        try (Connection conn = DB.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToRestaurant(rs));
            }
        }
        return list;
    }


    public void approveRestaurant(int id) throws SQLException {
        String sql = "UPDATE restaurants SET approved = true WHERE id = ?";
        try (Connection conn = DB.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    public List<Restaurant> getRestaurantsBySellerId(int sellerId) throws SQLException {
        List<Restaurant> list = new ArrayList<>();
        String sql = "SELECT * FROM restaurants WHERE seller_id = ?";
        try (Connection conn = DB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, sellerId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Restaurant r = new Restaurant();
                r.setId(rs.getInt("id"));
                r.setSellerId(rs.getInt("seller_id"));
                r.setName(rs.getString("name"));
                r.setAddress(rs.getString("address"));
                r.setPhone(rs.getString("phone"));
                r.setOpeningHours(rs.getString("opening_hours"));
                r.setLogoUrl(rs.getString("logo_url"));
                r.setApproved(rs.getBoolean("approved"));
                list.add(r);
            }
        }
        return list;
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

    public List<Restaurant> getAllApprovedRestaurants() throws SQLException {
        List<Restaurant> restaurants = new ArrayList<>();
        String sql = "SELECT * FROM restaurants WHERE approved = true";

        try (Connection conn = DB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Restaurant r = new Restaurant();
                r.setId(rs.getInt("id"));
                r.setSellerId(rs.getInt("seller_id"));
                r.setName(rs.getString("name"));
                r.setAddress(rs.getString("address"));
                r.setPhone(rs.getString("phone"));
                r.setOpeningHours(rs.getString("opening_hours"));
                r.setLogoUrl(rs.getString("logo_url"));
                r.setApproved(rs.getBoolean("approved"));
                restaurants.add(r);
            }
        }

        return restaurants;
    }

    public boolean deleteRestaurant(int id) throws SQLException {
        String sql = "DELETE FROM restaurants WHERE id = ?";
        try (Connection conn = DB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    public Restaurant getRestaurantById(int id) throws SQLException {
        String sql = "SELECT * FROM restaurants WHERE id = ?";
        try (Connection conn = DB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Restaurant r = new Restaurant();
                r.setId(rs.getInt("id"));
                r.setName(rs.getString("name"));
                r.setLogoUrl(rs.getString("logo_url"));
                r.setSellerId(rs.getInt("seller_id"));
                r.setApproved(rs.getBoolean("approved"));
                return r;

            }
        }
        return null;
    }

    public Restaurant getRestaurantByName(String name) throws SQLException {
        String sql = "SELECT * FROM restaurants WHERE name = ?";
        try (Connection conn = DB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Restaurant r = new Restaurant();
                r.setId(rs.getInt("id"));
                r.setName(rs.getString("name"));
                r.setLogoUrl(rs.getString("logo_url"));
                r.setSellerId(rs.getInt("seller_id"));
                r.setApproved(rs.getBoolean("approved"));
                return r;

            }
        }
        return null;
    }
}
