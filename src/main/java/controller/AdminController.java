package controller;

import com.google.gson.Gson;
import model.Order;
import model.User;
import model.Restaurant;
import services.OrderService;
import services.UserService;
import services.RestaurantService;
import utils.SimpleJwtUtil;
import java.util.List;
import static spark.Spark.*;

public class AdminController {
    private static final Gson gson = new Gson();
    private static final UserService userService = new UserService();
    static final OrderService orderService = new OrderService();
    private static final RestaurantService restaurantService = new RestaurantService();

    public static void initRoutes() {
        path("/admin", () -> {

            before("/*", (req, res) -> {
                String token = req.headers("Authorization");
                if (token == null || !token.startsWith("Bearer ")) {
                    halt(401, gson.toJson(new ErrorResponse("Unauthorized")));
                }
                token = token.substring(7);

                if (!SimpleJwtUtil.validateToken(token) || !"admin".equalsIgnoreCase(SimpleJwtUtil.getRoleFromToken(token))) {
                    halt(403, gson.toJson(new ErrorResponse("Forbidden: Admin only")));
                }
            });

            get("/users", (req, res) -> gson.toJson(userService.getAllUsers()));

            put("/users/:id/approve", (req, res) -> {
                int userId = Integer.parseInt(req.params(":id"));
                boolean ok = userService.approveUser(userId);
                if (!ok) {
                    res.status(404);
                    return gson.toJson(new ErrorResponse("User not found"));
                }
                return gson.toJson(new SuccessResponse("User approved"));
            });

            delete("/users/:id", (req, res) -> {
                int userId = Integer.parseInt(req.params(":id"));
                boolean deleted = userService.deleteUser(userId);
                if (!deleted) {
                    res.status(404);
                    return gson.toJson(new ErrorResponse("User not found"));
                }
                return gson.toJson(new SuccessResponse("User deleted"));
            });

            get("/orders/ongoing", (req, res) -> gson.toJson(orderService.getOngoingOrders()));

            get("/stats", (req, res) -> {
                AdminStats stats = new AdminStats();
                stats.totalUsers = userService.getAllUsers().size();
                stats.totalOrders = orderService.getAllOrders().size();
                stats.totalOngoingOrders = orderService.getOngoingOrders().size();
                stats.totalDeliveredOrders = orderService.getDeliveredOrders().size();
                return gson.toJson(stats);
            });

            get("/restaurants/pending", (req, res) -> {
                List<Restaurant> pending = restaurantService.getPendingRestaurants();
                System.out.println("📦 Pending Restaurants: " + pending);
                System.out.println("📦 JSON: " + gson.toJson(pending));
                return gson.toJson(pending);
            });

            put("/restaurants/:id/approve", (req, res) -> {
                int id = Integer.parseInt(req.params(":id"));
                restaurantService.approveRestaurant(id);
                return gson.toJson(new SuccessResponse("Restaurant approved"));
            });

            delete("/restaurants/:id", (req, res) -> {
                int id = Integer.parseInt(req.params(":id"));
                boolean deleted = restaurantService.deleteRestaurant(id);
                if (!deleted) {
                    res.status(404);
                    return gson.toJson(new ErrorResponse("Restaurant not found"));
                }
                return gson.toJson(new SuccessResponse("Restaurant deleted"));
            });

            get("/all", (req, res) -> {
                String token = req.headers("Authorization").substring(7);
                String role = SimpleJwtUtil.getRoleFromToken(token);
                if (!role.equalsIgnoreCase("admin")) {
                    halt(403, gson.toJson(new ErrorResponse("Only admins can access all orders")));
                }

                List<Order> all = orderService.getAllOrders();
                return gson.toJson(all);
            });

            get("/orders/ongoing", (req, res) -> gson.toJson(orderService.getOngoingOrders()));


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

    static class AdminStats {
        int totalUsers;
        int totalOrders;
        int totalOngoingOrders;
        int totalDeliveredOrders;
    }
}
