package DAO;

import com.google.gson.Gson;
import model.BankInfo;
import model.User;
import utils.DB;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    private static final Gson gson = new Gson();

    public static User addUser(User user) {
        String sql = "INSERT INTO users (full_name, phone, email, password, role, address, profile_photo_url, bank_info, brand_name, restaurant_description, salt, short_description, wallet) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";

        try (Connection conn = DB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getFullName());
            stmt.setString(2, user.getPhone());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getPassword());
            stmt.setString(5, user.getRole());
            stmt.setString(6, user.getAddress());
            stmt.setString(7, user.getProfilePhotoUrl());
            stmt.setString(8, user.getBankInfo() != null ? gson.toJson(user.getBankInfo()) : null);
            stmt.setString(9, user.getBrandName());
            stmt.setString(10, user.getRestaurantDescription());
            stmt.setString(11, user.getSalt());
            stmt.setString(12, user.getShortDescription());
            stmt.setDouble(13, user.getWallet());

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                user.setId(rs.getInt("id"));
                return user;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static boolean updateUser(User user) {
        String sql = "UPDATE users SET full_name=?, phone=?, email=?, password=?, role=?, address=?, profile_photo_url=?, bank_info=?, brand_name=?, restaurant_description=?, short_description=?, wallet=? WHERE id=?";

        try (Connection conn = DB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getFullName());
            stmt.setString(2, user.getPhone());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getPassword());
            stmt.setString(5, user.getRole());
            stmt.setString(6, user.getAddress());
            stmt.setString(7, user.getProfilePhotoUrl());
            stmt.setString(8, user.getBankInfo() != null ? gson.toJson(user.getBankInfo()) : null);
            stmt.setString(9, user.getBrandName());
            stmt.setString(10, user.getRestaurantDescription());
            stmt.setString(11, user.getShortDescription());
            stmt.setDouble(12, user.getWallet());
            stmt.setInt(13, user.getId());

            return stmt.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean deleteUser(int id) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = DB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static User findByPhone(String phone) {
        String sql = "SELECT * FROM users WHERE phone = ?";
        try (Connection conn = DB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, phone);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return extractUser(rs);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static User getUserById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = DB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return extractUser(rs);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";

        try (Connection conn = DB.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                users.add(extractUser(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return users;
    }

    private static User extractUser(ResultSet rs) throws SQLException {
        BankInfo bankInfo = null;
        String bankInfoJson = rs.getString("bank_info");
        if (bankInfoJson != null && !bankInfoJson.isEmpty()) {
            try {
                bankInfo = gson.fromJson(bankInfoJson, BankInfo.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        int restaurantId = -1;
        try {
            restaurantId = rs.getInt("restaurantId");
        } catch (SQLException ignored) {}

        return new User(
                rs.getInt("id"),
                rs.getString("full_name"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getString("password"),
                rs.getString("role"),
                rs.getString("address"),
                rs.getString("profile_photo_url"),
                bankInfo,
                rs.getString("brand_name"),
                rs.getString("restaurant_description"),
                rs.getString("salt"),
                rs.getString("short_description"),
                restaurantId,
                rs.getDouble("wallet")
        );
    }

    public static void updateRestaurantIdForUser(int userId, int restaurantId) {
        String sql = "UPDATE users SET restaurantId = ? WHERE id = ?";
        try (Connection conn = DB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, restaurantId);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean approveUser(int userId) {
        String sql = "UPDATE users SET status = 'APPROVED' WHERE id = ?";
        try (Connection conn = DB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
