package controller;

import DAO.UserDAO;
import com.google.gson.Gson;
import model.Restaurant;
import services.RestaurantService;
import utils.SimpleJwtUtil;
import java.sql.SQLException;
import java.util.List;
import static spark.Spark.*;

public class RestaurantController {
    private static final RestaurantService restaurantService = new RestaurantService();
    private static final Gson gson = new Gson();

    public static void initRoutes() {

        post("/restaurants", (req, res) -> {
            res.type("application/json");

            String token = req.headers("Authorization");
            if (token == null || !token.startsWith("Bearer ")) {
                res.status(401);
                return gson.toJson(new ErrorResponse("Missing token"));
            }

            token = token.substring(7);
            if (!SimpleJwtUtil.validateToken(token)) {
                res.status(401);
                return gson.toJson(new ErrorResponse("Invalid or expired token"));
            }

            String role = SimpleJwtUtil.getRoleFromToken(token);
            int sellerId = SimpleJwtUtil.getUserIdFromToken(token);

            if (!role.equalsIgnoreCase("seller")) {
                res.status(403);
                return gson.toJson(new ErrorResponse("Only sellers can register restaurants"));
            }

            Restaurant restaurant = gson.fromJson(req.body(), Restaurant.class);
            restaurant.setSellerId(sellerId);

            Restaurant saved;
            try {
                saved = restaurantService.registerRestaurant(restaurant);
            } catch (SQLException e) {
                res.status(500);
                return gson.toJson(new ErrorResponse("Database error during restaurant registration"));
            }

            UserDAO.updateRestaurantIdForUser(saved.getSellerId(), saved.getId());

            return gson.toJson(new SuccessResponse("Restaurant registered. Awaiting approval."));
        });

        get("/restaurants", (req, res) -> {
            res.type("application/json");
            try {
                List<Restaurant> restaurants = restaurantService.getApprovedRestaurants();
                if (restaurants == null) restaurants = List.of();
                System.out.println("Returning restaurants: " + restaurants);
                return gson.toJson(restaurants);
            } catch (SQLException e) {
                res.status(500);
                return gson.toJson(List.of());
            }
        });

        put("/admin/restaurants/:id/approve", (req, res) -> {
            res.type("application/json");
            int id = Integer.parseInt(req.params("id"));
            try {
                restaurantService.approveRestaurant(id);
            } catch (SQLException e) {
                res.status(500);
                return gson.toJson(new ErrorResponse("Database error during approval"));
            }
            return gson.toJson(new SuccessResponse("Restaurant approved."));
        });

        get("/restaurants/seller/:sellerId", (req, res) -> {
            res.type("application/json");
            int sellerId = Integer.parseInt(req.params("sellerId"));
            List<Restaurant> restaurants;
            try {
                restaurants = restaurantService.getRestaurantsBySellerId(sellerId);
                if (restaurants == null) restaurants = List.of();
            } catch (SQLException e) {
                res.status(500);
                return gson.toJson(List.of());
            }
            return gson.toJson(restaurants);
        });
    }

    static class ErrorResponse {
        String error;
        public ErrorResponse(String error) { this.error = error; }
    }

    static class SuccessResponse {
        String message;
        public SuccessResponse(String message) { this.message = message; }
    }
}
