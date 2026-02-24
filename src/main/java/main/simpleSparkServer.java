package main;

import static spark.Spark.*;

import controller.*;
import utils.PasswordUtil;
import utils.SimpleJwtUtil;

public class simpleSparkServer {
    public static void main(String[] args) {
        port(8080);

        before("/admin/*", (req, res) -> {
            String token = req.headers("Authorization");

            if (token == null || !token.startsWith("Bearer ")) {
                halt(401, "Unauthorized");
            }

            token = token.substring(7); // Remove "Bearer "

            if (!SimpleJwtUtil.validateToken(token)) {
                halt(401, "Invalid or expired token");
            }

            String role = SimpleJwtUtil.getRoleFromToken(token);
            if (role == null || !role.equalsIgnoreCase("ADMIN")) {
                halt(403, "Access denied");
            }
        });

        UserController.initRoutes();
        RestaurantController.initRoutes();
        FoodController.initRoutes();
        AdminController.initRoutes();
        AuthController.initRoutes();
        OrderController.initRoutes();

        String plainPassword = "admin";
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(plainPassword, salt);

        System.out.println("👉 Salt: " + salt);
        System.out.println("🔐 Hash: " + hash);
        get("/", (req, res) -> "Food Service API is running!");
        System.out.println("==> Spark has started on http://localhost:8080");
    }
}
