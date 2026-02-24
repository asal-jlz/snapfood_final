package controller;

import com.google.gson.Gson;
import model.User;
import model.Restaurant;
import services.RestaurantService;
import services.UserService;
import utils.SimpleJwtUtil;
import java.util.List;
import static spark.Spark.*;

public class UserController {
    private static final UserService userService = new UserService();
    private static final RestaurantService restaurantService = new RestaurantService();
    private static final Gson gson = new Gson();

    public static void initRoutes() {
        before("/users/*", (req, res) -> {
            if (req.requestMethod().equals("OPTIONS") ||
                    req.pathInfo().equals("/users/register") ||
                    req.pathInfo().equals("/users/login")) {
                return;
            }

            String authHeader = req.headers("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                res.status(401);
                halt(401, gson.toJson(new ErrorResponse("Missing or invalid token")));
            }

            String token = authHeader.substring(7);
            if (!SimpleJwtUtil.validateToken(token)) {
                res.status(401);
                halt(401, gson.toJson(new ErrorResponse("Invalid or expired token")));
            }
        });

        path("/users", () -> {
            get("", (req, res) -> gson.toJson(userService.getAllUsers()));

            get("/:id", (req, res) -> {
                int id = Integer.parseInt(req.params(":id"));
                User user = userService.getUser(id);
                if (user == null) {
                    res.status(404);
                    return "User not found";
                }
                return gson.toJson(user);
            });

            post("/register", (req, res) -> {
                User user = gson.fromJson(req.body(), User.class);
                User registered = userService.register(user);
                if (registered == null) {
                    res.status(400);
                    return gson.toJson(new ErrorResponse("Phone already registered"));
                }
                res.status(201);
                return gson.toJson(registered);
            });

            post("/login", (req, res) -> {
                User loginUser = gson.fromJson(req.body(), User.class);
                User loggedIn = userService.login(loginUser.getPhone(), loginUser.getPassword());
                if (loggedIn == null) {
                    res.status(401);
                    return gson.toJson(new ErrorResponse("Invalid phone or password"));
                }

                String token = SimpleJwtUtil.generateToken(loggedIn.getId(), loggedIn.getRole());
                LoginResponse response = new LoginResponse(loggedIn, token);

                if ("seller".equalsIgnoreCase(loggedIn.getRole())) {
                    response.setBrandName(loggedIn.getBrandName());
                    response.setProfilePhotoUrl(loggedIn.getProfilePhotoUrl());
                    response.setShortDescription(loggedIn.getShortDescription());
                    if (loggedIn.getRestaurantId() > 0) {
                        response.setRestaurantId(loggedIn.getRestaurantId());
                    }

                    try {
                        List<Restaurant> restaurants = restaurantService.getRestaurantsBySellerId(loggedIn.getId());
                        if (!restaurants.isEmpty()) {
                            response.setRestaurantId(restaurants.get(0).getId());
                        }
                    } catch (Exception e) {
                        System.err.println("Error fetching restaurant for seller: " + e.getMessage());
                    }
                }

                response.setWallet(loggedIn.getWallet());

                return gson.toJson(response);
            });

            get("/:id/profile", (req, res) -> {
                int id = Integer.parseInt(req.params(":id"));
                User user = userService.getUser(id);
                if (user == null) {
                    res.status(404);
                    return gson.toJson(new ErrorResponse("User not found"));
                }
                return gson.toJson(user);
            });

            put("/:id/profile", (req, res) -> {
                int id = Integer.parseInt(req.params(":id"));
                User existing = userService.getUser(id);
                if (existing == null) {
                    res.status(404);
                    return gson.toJson(new ErrorResponse("User not found"));
                }

                User update = gson.fromJson(req.body(), User.class);

                if (update.getFullName() != null) existing.setFullName(update.getFullName());
                if (update.getPhone() != null) existing.setPhone(update.getPhone());
                if (update.getEmail() != null) existing.setEmail(update.getEmail());
                if (update.getAddress() != null) existing.setAddress(update.getAddress());
                if (update.getProfilePhotoUrl() != null) existing.setProfilePhotoUrl(update.getProfilePhotoUrl());
                if (update.getWallet() >= 0) existing.setWallet(update.getWallet());

                if ("seller".equalsIgnoreCase(existing.getRole()) || "courier".equalsIgnoreCase(existing.getRole())) {
                    if (update.getBankInfo() != null) {
                        existing.setBankInfo(update.getBankInfo());
                    }
                }

                if ("seller".equalsIgnoreCase(existing.getRole())) {
                    if (update.getBrandName() != null) {
                        existing.setBrandName(update.getBrandName());
                    }
                    if (update.getRestaurantDescription() != null) {
                        existing.setRestaurantDescription(update.getRestaurantDescription());
                    }
                    if (update.getShortDescription() != null) {
                        existing.setShortDescription(update.getShortDescription());
                    }
                }

                User updated = userService.updateUser(id, existing);
                if (updated == null) {
                    res.status(500);
                    return gson.toJson(new ErrorResponse("Update failed"));
                }

                if ("seller".equalsIgnoreCase(existing.getRole())) {
                    try {
                        List<Restaurant> restaurants = restaurantService.getRestaurantsBySellerId(id);
                        if (restaurants.isEmpty()) {
                            Restaurant newRestaurant = new Restaurant();
                            newRestaurant.setSellerId(id);
                            newRestaurant.setName(existing.getBrandName() != null ? existing.getBrandName() : "Brand Name");
                            newRestaurant.setAddress(existing.getAddress());
                            newRestaurant.setPhone(existing.getPhone());
                            newRestaurant.setLogoUrl(existing.getProfilePhotoUrl());
                            newRestaurant.setApproved(false);
                            restaurantService.registerRestaurant(newRestaurant);

                            restaurants = restaurantService.getRestaurantsBySellerId(id);
                        }
                        if (!restaurants.isEmpty()) {
                            existing.setRestaurantId(restaurants.get(0).getId());
                            userService.updateUser(id, existing);
                        }
                    } catch (Exception e) {
                        System.err.println("Error during restaurant creation/linking for seller: " + e.getMessage());
                    }
                }

                return gson.toJson(existing);
            });

            put("/:id/wallet/charge", (req, res) -> {
                int id = Integer.parseInt(req.params(":id"));
                User user = userService.getUser(id);
                if (user == null) {
                    res.status(404);
                    return gson.toJson(new ErrorResponse("User not found"));
                }

                User update = gson.fromJson(req.body(), User.class);
                double amountToAdd = update.getWallet();

                if (amountToAdd <= 0) {
                    res.status(400);
                    return gson.toJson(new ErrorResponse("Invalid amount"));
                }

                user.setWallet(user.getWallet() + amountToAdd);
                boolean success = userService.updateUser(id, user) != null;
                return gson.toJson(success ? user : new ErrorResponse("Wallet update failed"));
            });

            delete("/:id", (req, res) -> {
                int id = Integer.parseInt(req.params(":id"));
                boolean deleted = userService.deleteUser(id);
                if (!deleted) {
                    res.status(404);
                    return "User not found";
                }
                return "User deleted";
            });
        });
    }

    private static class ErrorResponse {
        String error;
        public ErrorResponse(String error) { this.error = error; }
    }

    private static class LoginResponse {
        private int id;
        private String name;
        private String role;
        private String message;
        private String token;
        private String brandName;
        private String profilePhotoUrl;
        private String shortDescription;
        private int restaurantId;
        private double wallet;

        public LoginResponse(User user, String token) {
            this.id = user.getId();
            this.name = user.getFullName();
            this.role = user.getRole();
            this.message = "Login successful!";
            this.token = token;
            this.brandName = user.getBrandName();
            this.profilePhotoUrl = user.getProfilePhotoUrl();
            this.shortDescription = user.getShortDescription();
            this.wallet = user.getWallet();
        }

        public void setBrandName(String brandName) { this.brandName = brandName; }
        public void setProfilePhotoUrl(String profilePhotoUrl) { this.profilePhotoUrl = profilePhotoUrl; }
        public void setShortDescription(String shortDescription) { this.shortDescription = shortDescription; }
        public void setRestaurantId(int restaurantId) { this.restaurantId = restaurantId; }
        public void setWallet(double wallet) { this.wallet = wallet; }
    }
}
