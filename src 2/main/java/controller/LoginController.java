package controller;

import Session.Session;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.event.ActionEvent;
import javafx.stage.Stage;
import model.Courier;
import model.LoginResponse;
import model.Seller;
import model.User;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class LoginController {

    @FXML private TextField phoneNumberField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Hyperlink signUpLink;

    private void showAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Login Error");
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    @FXML
    void handleSignupLink(ActionEvent event) {
        try {
            Parent signUpRoot = FXMLLoader.load(getClass().getResource("/fxmls/SignUp.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(signUpRoot));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Failed to load SignUp page.");
        }
    }

    @FXML
    private void onLoginClick(ActionEvent event) {
        String phone = phoneNumberField.getText();
        String password = passwordField.getText();

        if (phone.isEmpty() || password.isEmpty()) {
            showAlert("Please enter both phone number and password.");
            return;
        }

        try {
            HttpClient client = HttpClient.newHttpClient();
            String json = String.format("{\"phone\":\"%s\", \"password\":\"%s\"}", phone, password);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/users/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() == 200) {
                            Gson gson = new Gson();
                            LoginResponse loginData = gson.fromJson(response.body(), LoginResponse.class);

                            String role = loginData.getRole().toLowerCase();

                            Session.setUser(null);
                            Session.setCurrentSeller(null);
                            Session.setCurrentCourier(null);
                            Session.setToken(loginData.getToken());

                            if (role.equals("admin")) {
                                User admin = new User() {};
                                admin.setId(loginData.getId());
                                admin.setFullName("Admin");
                                admin.setPhone(phone);
                                admin.setToken(loginData.getToken());

                                Session.setUser(admin);

                                Platform.runLater(() -> {
                                    try {
                                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxmls/Admin-dashboard.fxml"));
                                        Parent root = loader.load();
                                        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                                        stage.setScene(new Scene(root));
                                        stage.show();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        showAlert("Failed to load admin dashboard.");
                                    }
                                });

                            } else if (role.equals("seller") || role.equals("vendor")) {
                                Seller seller = new Seller();
                                seller.setId(loginData.getId());
                                seller.setPhone(loginData.getPhone() != null ? loginData.getPhone() : phone);
                                seller.setToken(loginData.getToken());
                                seller.setFullName(loginData.getName());
                                seller.setBrandName(loginData.getBrandName());
                                seller.setProfilePhotoUrl(loginData.getProfilePhotoUrl());
                                seller.setShortDescription(loginData.getShortDescription());

                                int restaurantId = loginData.getRestaurantId();
                                if (restaurantId <= 0) {
                                    fetchRestaurantIdAndLoadDashboard(seller, client, event);
                                } else {
                                    seller.setRestaurantId(restaurantId);
                                    Session.setCurrentSeller(seller);
                                    Session.setUser(seller);
                                    Platform.runLater(() -> loadSellerDashboard(event, seller));
                                }

                            } else if (role.equals("courier")) {
                                Courier courier = new Courier();
                                courier.setId(loginData.getId());
                                courier.setPhone(loginData.getPhone() != null ? loginData.getPhone() : phone);
                                courier.setToken(loginData.getToken());
                                courier.setFullName(loginData.getName());

                                Session.setCurrentCourier(courier);
                                Session.setUser(courier);

                                Platform.runLater(() -> {
                                    try {
                                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxmls/Courier-dashboard.fxml"));
                                        Parent root = loader.load();
                                        CourierDashboardController courierCtrl = loader.getController();
                                        courierCtrl.setUserData(courier);
                                        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                                        stage.setScene(new Scene(root));
                                        stage.show();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        showAlert("Failed to load courier dashboard.");
                                    }
                                });

                            } else if (role.equals("buyer")) {
                                User buyer = new User() {};
                                buyer.setId(loginData.getId());
                                buyer.setPhone(loginData.getPhone() != null ? loginData.getPhone() : phone);
                                buyer.setToken(loginData.getToken());
                                buyer.setFullName(loginData.getName());

                                Session.setUser(buyer);

                                Platform.runLater(() -> {
                                    try {
                                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxmls/buyer-dashboard.fxml"));
                                        Parent root = loader.load();
                                        BuyerDashboardController buyerCtrl = loader.getController();
                                        buyerCtrl.setUserData(buyer);
                                        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                                        stage.setScene(new Scene(root));
                                        stage.show();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        showAlert("Failed to load buyer dashboard.");
                                    }
                                });

                            } else {
                                showAlert("Unknown role: " + role);
                            }

                        } else {
                            showAlert("Login failed: Invalid credentials.");
                        }
                    })
                    .exceptionally(e -> {
                        e.printStackTrace();
                        showAlert("Connection error: " + e.getMessage());
                        return null;
                    });

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Unexpected error during login.");
        }
    }

    private void fetchRestaurantIdAndLoadDashboard(Seller seller, HttpClient client, ActionEvent event) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/restaurants/seller/" + seller.getId()))
                    .header("Authorization", "Bearer " + seller.getToken())
                    .GET()
                    .build();

            client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() == 200) {
                            try {
                                ObjectMapper mapper = new ObjectMapper();
                                var jsonNode = mapper.readTree(response.body());
                                if (jsonNode.isArray() && jsonNode.size() > 0) {
                                    int restaurantId = jsonNode.get(0).path("id").asInt();
                                    seller.setRestaurantId(restaurantId);
                                    System.out.println("✅ Restaurant ID fetched: " + restaurantId);
                                } else {
                                    System.err.println("⚠️ No restaurants found for seller");
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            System.err.println("⚠️ Failed to fetch restaurant, status: " + response.statusCode());
                        }

                        Session.setCurrentSeller(seller);
                        Session.setUser(seller);

                        Platform.runLater(() -> loadSellerDashboard(event, seller));
                    })
                    .exceptionally(e -> {
                        e.printStackTrace();
                        Platform.runLater(() -> showAlert("Error loading seller profile"));
                        return null;
                    });
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error fetching restaurant information.");
        }
    }

    private void loadSellerDashboard(ActionEvent event, Seller seller) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxmls/seller-dashboard.fxml"));
            Parent root = loader.load();
            SellerDashboardController controller = loader.getController();
            controller.setUserData(seller);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Failed to load seller dashboard.");
        }
    }
}
