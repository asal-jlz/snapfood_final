package controller;

import DTO.OrderRequestDTO;
import com.google.gson.Gson;
import model.Order;
import services.OrderService;
import utils.LocalDateTimeAdapter;
import utils.SimpleJwtUtil;
import model.OrderItem;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import model.OrderItem;
import model.Order;
import DTO.OrderRequestDTO;
import com.google.gson.Gson;
import services.OrderService;
import utils.SimpleJwtUtil;
import com.google.gson.GsonBuilder;


import static spark.Spark.*;

public class OrderController {
    private static final OrderService orderService = new OrderService();
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    public static void initRoutes() {
        path("/orders", () -> {
            before("/*", (req, res) -> {
                String authHeader = req.headers("Authorization");
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    halt(401, gson.toJson(new ErrorResponse("Missing or invalid token")));
                }
                String token = authHeader.substring(7);
                if (!SimpleJwtUtil.validateToken(token)) {
                    halt(401, gson.toJson(new ErrorResponse("Invalid or expired token")));
                }
            });

            post("", (req, res) -> {
                try {
                    String token = req.headers("Authorization").substring(7);
                    int userId = SimpleJwtUtil.getUserIdFromToken(token);

                    OrderRequestDTO dto = gson.fromJson(req.body(), OrderRequestDTO.class);
                    if (dto == null) {
                        res.status(400);
                        return gson.toJson(new ErrorResponse("Invalid order request data"));
                    }

                    List<Integer> foodItemIds;
                    List<OrderItem> orderItems = null;

                    if (dto.getItems() != null && !dto.getItems().isEmpty()) {
                        foodItemIds = dto.getItems().stream()
                                .map(OrderRequestDTO.OrderItem::getFoodId)
                                .collect(Collectors.toList());

                        orderItems = dto.getItems().stream()
                                .map(dtoItem -> {
                                    OrderItem oi = new OrderItem();
                                    oi.setFoodItemId(dtoItem.getFoodId());
                                    oi.setQuantity(dtoItem.getQuantity());
                                    return oi;
                                })
                                .collect(Collectors.toList());

                    } else if (dto.getFoodItemIds() != null && !dto.getFoodItemIds().isEmpty()) {
                        foodItemIds = dto.getFoodItemIds();

                        orderItems = dto.getFoodItemIds().stream()
                                .map(id -> {
                                    OrderItem oi = new OrderItem();
                                    oi.setFoodItemId(id);
                                    oi.setQuantity(1);  // default quantity = 1
                                    return oi;
                                })
                                .collect(Collectors.toList());

                    } else {
                        res.status(400);
                        return gson.toJson(new ErrorResponse("No food items provided in order"));
                    }

                    int restaurantId = orderService.getRestaurantIdFromFirstFoodItem(foodItemIds);
                    if (restaurantId == 0) {
                        res.status(400);
                        return gson.toJson(new ErrorResponse("Invalid restaurant ID from food items"));
                    }

                    Order order = new Order();
                    order.setUserId(userId);
                    order.setFoodItemIds(foodItemIds);
                    order.setRestaurantId(restaurantId);
                    order.setItems(orderItems);


                    Order saved = orderService.placeOrder(order);

                    if (saved == null) {
                        res.status(500);
                        return gson.toJson(new ErrorResponse("Failed to place order"));
                    }

                    res.status(201);
                    return gson.toJson(saved);

                } catch (Exception e) {
                    e.printStackTrace();
                    res.status(500);
                    return gson.toJson(new ErrorResponse("Internal server error: " + e.getMessage()));
                }
            });

            get("", (req, res) -> {
                String token = req.headers("Authorization").substring(7);
                int userId = SimpleJwtUtil.getUserIdFromToken(token);
                List<Order> orders = orderService.getOrdersByUser(userId);
                res.type("application/json");
                return gson.toJson(orders);
            });

            post("/cancel/:id", (req, res) -> {
                String token = req.headers("Authorization").substring(7);
                int userId = SimpleJwtUtil.getUserIdFromToken(token);
                int orderId = Integer.parseInt(req.params("id"));

                boolean cancelled = orderService.cancelOrder(orderId, userId);
                if (!cancelled) {
                    res.status(400);
                    return gson.toJson(new ErrorResponse("Cannot cancel order (maybe already shipped or doesn't belong to you)"));
                }
                return gson.toJson(new SuccessResponse("Order cancelled successfully"));
            });

            get("/restaurant", (req, res) -> {
                String token = req.headers("Authorization").substring(7);
                int restaurantId = SimpleJwtUtil.getRestaurantIdFromToken(token);
                List<Order> orders = orderService.getOrdersByRestaurant(restaurantId);
                res.type("application/json");
                return gson.toJson(orders);
            });

            put("/:id/confirm", (req, res) -> {
                String token = req.headers("Authorization").substring(7);
                int restaurantId = SimpleJwtUtil.getRestaurantIdFromToken(token);
                int orderId = Integer.parseInt(req.params("id"));
                boolean confirmed = orderService.confirmOrder(orderId, restaurantId);
                if (!confirmed) {
                    res.status(400);
                    return gson.toJson(new ErrorResponse("Cannot confirm order"));
                }
                return gson.toJson(new SuccessResponse("Order confirmed"));
            });

            put("/:id/assign", (req, res) -> {
                String token = req.headers("Authorization").substring(7);
                String role = SimpleJwtUtil.getRoleFromToken(token);
                if (!"courier".equals(role)) {
                    halt(403, gson.toJson(new ErrorResponse("Forbidden")));
                }
                int courierId = SimpleJwtUtil.getUserIdFromToken(token);
                int orderId = Integer.parseInt(req.params("id"));

                boolean assigned = orderService.assignCourier(orderId, courierId);
                if (!assigned) {
                    res.status(400);
                    return gson.toJson(new ErrorResponse("Failed to assign courier"));
                }
                return gson.toJson(new SuccessResponse("Courier assigned successfully"));
            });

            put("/:id/status", (req, res) -> {
                String token = req.headers("Authorization").substring(7);
                int courierId = SimpleJwtUtil.getUserIdFromToken(token);
                String newStatus = gson.fromJson(req.body(), StatusRequest.class).status;
                int orderId = Integer.parseInt(req.params("id"));

                boolean updated = orderService.updateDeliveryStatus(orderId, courierId, newStatus);
                if (!updated) {
                    res.status(400);
                    return gson.toJson(new ErrorResponse("Failed to update delivery status"));
                }
                return gson.toJson(new SuccessResponse("Delivery status updated"));
            });

            get("/history", (req, res) -> {
                String token = req.headers("Authorization").substring(7);
                int userId = SimpleJwtUtil.getUserIdFromToken(token);

                String status = req.queryParams("status");
                String fromStr = req.queryParams("from");
                String toStr = req.queryParams("to");

                LocalDate from = (fromStr != null) ? LocalDate.parse(fromStr) : null;
                LocalDate to = (toStr != null) ? LocalDate.parse(toStr) : null;

                List<Order> orders = orderService.getUserOrderHistory(userId, status, from, to);
                res.type("application/json");
                return gson.toJson(orders);
            });

            put("/orders/:id/status", (req, res) -> {
                int orderId = Integer.parseInt(req.params(":id"));
                String newStatus = new Gson().fromJson(req.body(), Map.class).get("status").toString();

                String userRole = SimpleJwtUtil.getRoleFromRequest(req);
                if (!"seller".equals(userRole)) {
                    res.status(403);
                    return gson.toJson(new ErrorResponse("Only sellers can update status"));
                }

                boolean result = orderService.updateStatusBySeller(orderId, newStatus);
                if (result) return gson.toJson(Map.of("message", "Status updated to " + newStatus));
                res.status(404);
                return gson.toJson(new ErrorResponse("Order not found or update failed"));
            });

            get("/orders/:id/status-history", (req, res) -> {
                int orderId = Integer.parseInt(req.params(":id"));
                List<String> history = orderService.getStatusHistory(orderId);
                return gson.toJson(history);
            });

            get("/available", (req, res) -> {
                String token = req.headers("Authorization").substring(7);
                String role = SimpleJwtUtil.getRoleFromToken(token);
                if (!role.equalsIgnoreCase("courier")) {
                    halt(403, gson.toJson(new ErrorResponse("Only couriers can access available orders")));
                }

                List<Order> available = orderService.getAvailableOrdersForCouriers();
                return gson.toJson(available);
            });

            get("/courier/:id", (req, res) -> {
                String token = req.headers("Authorization").substring(7);
                String role = SimpleJwtUtil.getRoleFromToken(token);

                if (!"courier".equals(role)) {
                    halt(403, gson.toJson(new ErrorResponse("Forbidden")));
                }

                int courierId = Integer.parseInt(req.params(":id"));
                List<Order> orders = orderService.getOrdersByCourier(courierId);

                return gson.toJson(orders);
            });
        });
    }

    private static class ErrorResponse {
        String error;
        public ErrorResponse(String error) { this.error = error; }
    }

    private static class SuccessResponse {
        String message;
        public SuccessResponse(String message) { this.message = message; }
    }

    private static class StatusRequest {
        String status;
    }
}
