package services;

import DAO.RestaurantDAO;
import model.Restaurant;
import utils.DB;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RestaurantService {
    private final RestaurantDAO dao = new RestaurantDAO();

    public Restaurant registerRestaurant(Restaurant r) throws SQLException {
        dao.addRestaurant(r);
        return r;
    }

    public List<Restaurant> getApprovedRestaurants() throws SQLException {
        return dao.getAllApprovedRestaurants();
    }


    public List<Restaurant> getPendingRestaurants() throws SQLException {
        return dao.getPendingRestaurants();
    }

    public void approveRestaurant(int id) throws SQLException {
        dao.approveRestaurant(id);
    }

    public List<Restaurant> getRestaurantsBySellerId(int sellerId) throws SQLException {
        return dao.getRestaurantsBySellerId(sellerId);
    }

    public boolean deleteRestaurant(int id) throws SQLException {
        return dao.deleteRestaurant(id);
    }

    public Restaurant getRestaurantById(int id) throws SQLException {
        return dao.getRestaurantById(id);
    }

    public Restaurant getRestaurantByName(String name) throws SQLException {
        return dao.getRestaurantByName(name);
    }
}