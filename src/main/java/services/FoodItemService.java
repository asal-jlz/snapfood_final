package services;

import DAO.FoodItemDAO;
import model.FoodItem;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FoodItemService {

    private final FoodItemDAO dao = new FoodItemDAO();

    public List<FoodItem> getMenuByRestaurant(int restaurantId) {
        return dao.getFoodItemsByRestaurant(restaurantId);
    }

    public FoodItem getFoodItem(int id) {
        return dao.getFoodItem(id);
    }

    public FoodItem addFoodItem(FoodItem item) {
        return dao.addFoodItem(item);
    }

    public boolean updateFoodItem(FoodItem item) {
        return dao.updateFoodItem(item);
    }

    public boolean deleteFoodItem(int id, int restaurantId) {
        return dao.deleteFoodItem(id, restaurantId);
    }

    public Map<String, Object> searchFoodItems(String keyword, Double minPrice, Double maxPrice, String category, Double minRating) {
        List<FoodItem> matched = dao.searchFoodItems(keyword, minPrice, maxPrice, category, minRating);
        List<FoodItem> related = dao.findRelatedFoodItems(category != null ? category : "", keyword);
        Map<String, Object> result = new HashMap<>();
        result.put("results", matched);
        result.put("related", related);
        return result;
    }

    public List<String> getAllCategories() throws SQLException {
        return dao.getAllCategories();
    }

    public List<FoodItem> getFoodsByCategory(String category) throws SQLException {
        return dao.getFoodsByCategory(category);
    }

    public List<FoodItem> getFoodsByRestaurantName(String restaurantName) throws SQLException {
        return dao.getFoodsByRestaurantName(restaurantName);
    }

    public List<FoodItem> searchByName(String query) throws SQLException {
        return dao.searchByName(query);
    }

    public List<FoodItem> searchFoodItemsAsList(String keyword, Double minPrice, Double maxPrice, String category, Double minRating) {
        Map<String, Object> result = searchFoodItems(keyword, minPrice, maxPrice, category, minRating);
        if (result != null && result.containsKey("results")) {
            Object results = result.get("results");
            if (results instanceof List<?>) {
                return (List<FoodItem>) results;
            }
        }
        return List.of();
    }

}
