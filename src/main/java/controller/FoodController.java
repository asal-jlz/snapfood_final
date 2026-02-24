package controller;

import DTO.FoodCardDTO;
import DTO.FoodItemDTO;
import com.google.gson.Gson;
import model.FoodItem;
import model.Restaurant;
import services.FoodItemService;
import services.RestaurantService;
import utils.SimpleJwtUtil;
import java.sql.SQLException;
import java.util.List;
import static java.lang.Double.parseDouble;
import static spark.Spark.*;

public class FoodController {
    private static final FoodItemService service = new FoodItemService();
    private static final RestaurantService restaurantService = new RestaurantService();
    private static final Gson gson = new Gson();

    public static void initRoutes() {
        path("/foods", () -> {

            get("/categories", (req, res) -> {
                res.type("application/json");
                try {
                    List<String> categories = service.getAllCategories();
                    return gson.toJson(categories != null ? categories : List.of());
                } catch (SQLException e) {
                    res.status(500);
                    return gson.toJson(new ErrorResponse("Database error: " + e.getMessage()));
                }
            });

            get("/restaurant/:restaurantId", (req, res) -> {
                res.type("application/json");
                try {
                    int restaurantId = Integer.parseInt(req.params("restaurantId"));
                    if (restaurantId <= 0) return gson.toJson(List.of());
                    List<FoodItem> menu = service.getMenuByRestaurant(restaurantId);
                    Restaurant r = restaurantService.getRestaurantById(restaurantId);

                    List<FoodItemDTO> dtos = menu.stream().map(food ->
                            new FoodItemDTO(
                                    food.getId(),
                                    food.getRestaurantId(),
                                    food.getName(),
                                    food.getImageUrl(),
                                    food.getDescription(),
                                    food.getPrice(),
                                    food.getStock(),
                                    food.getCategory(),
                                    food.getKeywords(),
                                    r.getName(),
                                    r.getLogoUrl()
                            )
                    ).toList();


                    return gson.toJson(dtos);

                } catch (Exception e) {
                    res.status(500);
                    return gson.toJson(List.of());
                }
            });

            get("/category/:category", (req, res) -> {
                res.type("application/json");
                try {
                    String category = req.params(":category");
                    List<FoodItem> items = service.getFoodsByCategory(category);
                    List<FoodItemDTO> dtos = items.stream().map(food -> {
                        Restaurant r;
                        try {
                            r = restaurantService.getRestaurantById(food.getRestaurantId());
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                        return new FoodItemDTO(
                                food.getId(),
                                food.getRestaurantId(),
                                food.getName(),
                                food.getImageUrl(),
                                food.getDescription(),
                                food.getPrice(),
                                food.getStock(),
                                food.getCategory(),
                                food.getKeywords(),
                                r.getName(),
                                r.getLogoUrl()
                        );
                    }).toList();

                    return gson.toJson(dtos);

                } catch (SQLException e) {
                    res.status(500);
                    return gson.toJson(List.of());
                }
            });

            get("/restaurant/name/:name", (req, res) -> {
                res.type("application/json");
                try {
                    String name = req.params(":name");
                    List<FoodItem> items = service.getFoodsByRestaurantName(name);
                    Restaurant r = restaurantService.getRestaurantByName(name);

                    List<FoodItemDTO> dtos = items.stream().map(food ->
                            new FoodItemDTO(
                                    food.getId(),
                                    food.getRestaurantId(),
                                    food.getName(),
                                    food.getImageUrl(),
                                    food.getDescription(),
                                    food.getPrice(),
                                    food.getStock(),
                                    food.getCategory(),
                                    food.getKeywords(),
                                    r.getName(),
                                    r.getLogoUrl()
                            )
                    ).toList();


                    return gson.toJson(dtos);

                } catch (SQLException e) {
                    res.status(500);
                    return gson.toJson(List.of());
                }
            });

            get("/search", (req, res) -> {
                res.type("application/json");
                String keyword = req.queryParams("query");
                if (keyword == null || keyword.trim().isEmpty()) {
                    res.status(400);
                    return gson.toJson(List.of());
                }

                try {
                    Double minPrice = req.queryParams("minPrice") != null ? parseDouble(req.queryParams("minPrice")) : null;
                    Double maxPrice = req.queryParams("maxPrice") != null ? parseDouble(req.queryParams("maxPrice")) : null;
                    Double minRating = req.queryParams("rating") != null ? parseDouble(req.queryParams("rating")) : null;
                    String category = req.queryParams("category");

                    List<FoodItem> result = service.searchFoodItemsAsList(keyword, minPrice, maxPrice, category, minRating);
                    return gson.toJson(result != null ? result : List.of());
                } catch (Exception e) {
                    res.status(500);
                    return gson.toJson(List.of());
                }
            });

            before("", (req, res) -> {
                if (req.requestMethod().equalsIgnoreCase("POST")) {
                    checkSellerAuth(req, res);
                }
            });

            before("/:id", (req, res) -> {
                String id = req.params(":id");
                String method = req.requestMethod().toUpperCase();

                if ((method.equals("PUT") || method.equals("DELETE")) && id.matches("\\d+")) {
                    checkSellerAuth(req, res);
                }
            });

            post("", (req, res) -> {
                res.type("application/json");

                FoodItem item = gson.fromJson(req.body(), FoodItem.class);
                String token = req.headers("Authorization").substring(7);
                int sellerId = SimpleJwtUtil.getUserIdFromToken(token);

                try {
                    List<Restaurant> restaurants = restaurantService.getRestaurantsBySellerId(sellerId);
                    if (restaurants.isEmpty()) {
                        res.status(400);
                        return gson.toJson(new ErrorResponse("Seller has no registered restaurant"));
                    }

                    item.setRestaurantId(restaurants.get(0).getId());
                    FoodItem added = service.addFoodItem(item);
                    if (added == null) {
                        res.status(500);
                        return gson.toJson(new ErrorResponse("Failed to add food item"));
                    }

                    res.status(201);
                    return gson.toJson(added);
                } catch (SQLException e) {
                    res.status(500);
                    return gson.toJson(new ErrorResponse("Database error"));
                }
            });

            put("/:id", (req, res) -> {
                res.type("application/json");
                int foodId = Integer.parseInt(req.params("id"));
                FoodItem update = gson.fromJson(req.body(), FoodItem.class);
                String token = req.headers("Authorization").substring(7);
                int sellerId = SimpleJwtUtil.getUserIdFromToken(token);

                FoodItem existing = service.getFoodItem(foodId);
                if (existing == null) {
                    res.status(404);
                    return gson.toJson(new ErrorResponse("Food item not found"));
                }

                List<Restaurant> restaurants = restaurantService.getRestaurantsBySellerId(sellerId);
                boolean ownsFood = restaurants.stream().anyMatch(r -> r.getId() == existing.getRestaurantId());
                if (!ownsFood) {
                    res.status(403);
                    return gson.toJson(new ErrorResponse("Permission denied"));
                }

                update.setRestaurantId(existing.getRestaurantId());
                update.setId(foodId);

                boolean ok = service.updateFoodItem(update);
                if (!ok) {
                    res.status(500);
                    return gson.toJson(new ErrorResponse("Failed to update food item"));
                }

                return gson.toJson(update);
            });

            delete("/:id", (req, res) -> {
                res.type("application/json");
                int foodId = Integer.parseInt(req.params("id"));
                String token = req.headers("Authorization").substring(7);
                int sellerId = SimpleJwtUtil.getUserIdFromToken(token);

                FoodItem existing = service.getFoodItem(foodId);
                if (existing == null) {
                    res.status(404);
                    return gson.toJson(new ErrorResponse("Food item not found"));
                }

                List<Restaurant> restaurants = restaurantService.getRestaurantsBySellerId(sellerId);
                boolean ownsFood = restaurants.stream().anyMatch(r -> r.getId() == existing.getRestaurantId());
                if (!ownsFood) {
                    res.status(403);
                    return gson.toJson(new ErrorResponse("Permission denied"));
                }

                boolean ok = service.deleteFoodItem(foodId, existing.getRestaurantId());
                if (!ok) {
                    res.status(500);
                    return gson.toJson(new ErrorResponse("Failed to delete food item"));
                }

                return gson.toJson(new SuccessResponse("Food item deleted"));
            });
        });
    }

    private static void checkSellerAuth(spark.Request req, spark.Response res) {
        String authHeader = req.headers("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            halt(401, gson.toJson(new ErrorResponse("Missing or invalid token")));
        }

        String token = authHeader.substring(7);
        if (!SimpleJwtUtil.validateToken(token)) {
            halt(401, gson.toJson(new ErrorResponse("Invalid or expired token")));
        }

        String role = SimpleJwtUtil.getRoleFromToken(token);
        if (!role.equalsIgnoreCase("seller")) {
            halt(403, gson.toJson(new ErrorResponse("Access denied, only sellers allowed")));
        }
    }

    private static class ErrorResponse {
        String error;
        public ErrorResponse(String error) {
            this.error = error;
        }
    }

    private static class SuccessResponse {
        String message;
        public SuccessResponse(String message) {
            this.message = message;
        }
    }
}
