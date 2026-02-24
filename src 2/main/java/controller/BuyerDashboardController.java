package controller;

import Session.Session;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.Food;
import model.Restaurant;
import model.User;
import DTO.FoodItemDTO;
import model.FoodCardData;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import DTO.OrderRequestDTO;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class BuyerDashboardController {

    @FXML private Button logoutButton, goToPaymentButton, editProfileButton;
    @FXML private TextField searchField;
    @FXML private ListView<String> categoryListView, restaurantListView;
    @FXML private VBox menuVBox, categoryVBox;
    @FXML private ListView<String> cartListView;
    @FXML private Label preTaxLabel, withTaxLabel, welcomeLabel;
    @FXML private ImageView profileImageView;
    @FXML private Button walletButton;

    private double cartTotal = 0;
    private User currentUser;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();
    private final List<FoodItemDTO> selectedFoodItems = new ArrayList<>();

    public void setUserData(User user) {
        this.currentUser = user;
        Session.setUser(user);
        updateUserUI();
    }

    @FXML
    public void initialize() {
        if (currentUser == null) {
            currentUser = Session.getUser();
        }

        updateUserUI();
        loadRestaurantsFromBackend();
        loadCategoriesFromBackend();

        categoryListView.setOnMouseClicked(e -> {
            String selectedCategory = categoryListView.getSelectionModel().getSelectedItem();
            if (selectedCategory != null) loadFoodsByCategory(selectedCategory);
        });

        restaurantListView.setOnMouseClicked(e -> {
            String selectedRestaurant = restaurantListView.getSelectionModel().getSelectedItem();
            if (selectedRestaurant != null) loadFoodsByRestaurantName(selectedRestaurant);
        });
    }

    private void updateUserUI() {
        if (currentUser != null) {
            welcomeLabel.setText("Welcome, " + currentUser.getFullName());

            String profileImagePath = currentUser.getProfilePhotoPath();

            if (profileImagePath != null) {
                try {
                    if (profileImagePath.startsWith("data:image")) {
                        String base64Data = profileImagePath.split(",")[1];
                        InputStream is = new ByteArrayInputStream(Base64.getDecoder().decode(base64Data));
                        profileImageView.setImage(new Image(is));
                    } else if (profileImagePath.startsWith("http")) {
                        profileImageView.setImage(new Image(profileImagePath));
                    } else if (new File(profileImagePath).exists()) {
                        profileImageView.setImage(new Image(new File(profileImagePath).toURI().toString()));
                    } else {
                        System.err.println("Profile photo path invalid or file not found: " + profileImagePath);
                    }
                } catch (Exception e) {
                    System.err.println("Failed to load profile photo: " + e.getMessage());
                }
            }
        } else {
            System.err.println("User data not initialized properly.");
        }
    }

    private boolean isErrorResponse(String json) {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            return obj.has("error");
        } catch (Exception e) {
            return false;
        }
    }

    private void loadRestaurantsFromBackend() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/restaurants"))
                .header("Authorization", "Bearer " + Session.getToken())
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(response -> {
                    if (isErrorResponse(response)) {
                        JsonObject obj = JsonParser.parseString(response).getAsJsonObject();
                        String errorMsg = obj.get("error").getAsString();
                        Platform.runLater(() -> showAlert("Error loading restaurants", errorMsg));
                        return;
                    }

                    Type listType = new TypeToken<List<Restaurant>>() {}.getType();
                    List<Restaurant> restaurants = gson.fromJson(response, listType);

                    Platform.runLater(() -> {
                        restaurantListView.getItems().clear();
                        for (Restaurant r : restaurants) {
                            restaurantListView.getItems().add(r.getName());
                        }
                    });
                })
                .exceptionally(ex -> {
                    System.err.println("Failed to load restaurants: " + ex.getMessage());
                    return null;
                });
    }

    private void loadCategoriesFromBackend() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/foods/categories"))
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(json -> {
                    if (isErrorResponse(json)) {
                        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                        String errorMsg = obj.get("error").getAsString();
                        Platform.runLater(() -> showAlert("Error loading categories", errorMsg));
                        return;
                    }

                    Type listType = new TypeToken<List<String>>() {}.getType();
                    List<String> categories = gson.fromJson(json, listType);

                    Platform.runLater(() -> {
                        categoryListView.getItems().clear();
                        categoryListView.getItems().addAll(categories);
                    });
                })
                .exceptionally(ex -> {
                    System.err.println("Failed to load categories: " + ex.getMessage());
                    return null;
                });
    }

    private void loadFoodsByCategory(String category) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/foods/category/" + URLEncoder.encode(category, StandardCharsets.UTF_8)))
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(json -> {
                    if (isErrorResponse(json)) {
                        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                        String errorMsg = obj.get("error").getAsString();
                        Platform.runLater(() -> showAlert("Error loading foods", errorMsg));
                        return;
                    }

                    Type listType = new TypeToken<List<FoodItemDTO>>() {}.getType();
                    List<FoodItemDTO> foodDTOs = gson.fromJson(json, listType);

                    Platform.runLater(() -> {
                        menuVBox.getChildren().clear();
                        if (foodDTOs.isEmpty()) {
                            menuVBox.getChildren().add(new Label("No food found for category: " + category));
                        } else {
                            for (FoodItemDTO dto : foodDTOs) {
                                addFoodToMenu(dto);

                            }
                        }
                    });
                })
                .exceptionally(ex -> {
                    System.err.println("Failed to load foods by category: " + ex.getMessage());
                    return null;
                });
    }

    private void loadFoodsByRestaurantName(String restaurantName) {
        try {
            String encodedName = URLEncoder.encode(restaurantName, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/foods/restaurant/name/" + encodedName))
                    .GET()
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .thenAccept(json -> {
                        if (isErrorResponse(json)) {
                            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                            String errorMsg = obj.get("error").getAsString();
                            Platform.runLater(() -> showAlert("Error loading foods", errorMsg));
                            return;
                        }

                        Type listType = new TypeToken<List<FoodItemDTO>>() {}.getType();
                        List<FoodItemDTO> foodDTOs = gson.fromJson(json, listType);

                        Platform.runLater(() -> {
                            menuVBox.getChildren().clear();
                            if (foodDTOs.isEmpty()) {
                                menuVBox.getChildren().add(new Label("No food found for this restaurant."));
                            } else {
                                for (FoodItemDTO dto : foodDTOs) {
                                    addFoodToMenu(dto);
                                }
                            }
                        });
                    })
                    .exceptionally(ex -> {
                        System.err.println("Failed to load foods by restaurant: " + ex.getMessage());
                        return null;
                    });
        } catch (Exception ex) {
            System.err.println("Encoding restaurant name failed: " + ex.getMessage());
        }
    }


    private double parsePriceSafe(Object priceObj) {
        if (priceObj == null) return 0;
        try {
            if (priceObj instanceof Number) {
                return ((Number) priceObj).doubleValue();
            } else {
                return Double.parseDouble(priceObj.toString());
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid price format: " + priceObj);
            return 0;
        }
    }

    private void addFoodToMenu(FoodItemDTO dto) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxmls/foodviewing.fxml"));
            AnchorPane card = loader.load();
            FoodViewingController controller = loader.getController();
            controller.setFoodItemDTO(dto);

            controller.setCartUpdateListener((foodName, foodPrice) -> {
                cartListView.getItems().add(foodName + " - $" + String.format("%.2f", foodPrice));
                cartTotal += foodPrice;
                selectedFoodItems.add(dto);
                updateCartLabels();
            });
            menuVBox.getChildren().add(card);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private void updateCartLabels() {
        preTaxLabel.setText("Total (before tax): $" + String.format("%.2f", cartTotal));
        double withTax = cartTotal * 1.1;
        withTaxLabel.setText("Total (with tax): $" + String.format("%.2f", withTax));
    }

    @FXML
    private void onSearch(KeyEvent event) {
        String query = searchField.getText().toLowerCase();
        menuVBox.getChildren().clear();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/foods/search?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8)))
                    .header("Authorization", "Bearer " + Session.getToken())
                    .GET()
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .thenAccept(json -> {
                        if (isErrorResponse(json)) {
                            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                            String errorMsg = obj.get("error").getAsString();
                            Platform.runLater(() -> showAlert("Search error", errorMsg));
                            return;
                        }

                        Type listType = new TypeToken<List<FoodItemDTO>>() {}.getType();
                        List<FoodItemDTO> foodDTOs = gson.fromJson(json, listType);

                        Platform.runLater(() -> {
                            if (foodDTOs.isEmpty()) {
                                menuVBox.getChildren().add(new Label("No food found for: " + query));
                            } else {
                                for (FoodItemDTO dto : foodDTOs) {
                                    addFoodToMenu(dto);
                                }
                            }
                        });
                    })
                    .exceptionally(ex -> {
                        System.err.println("Search failed: " + ex.getMessage());
                        return null;
                    });
        } catch (Exception e) {
            System.err.println("Failed to encode search query: " + e.getMessage());
        }
    }


    @FXML
    private void onLogoutClicked() {
        Session.setUser(null);
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxmls/login.fxml"));
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onPaymentClicked() {
        if (selectedFoodItems.isEmpty()) {
            showAlert("Empty Cart", "You must select at least one item before checkout.");
            return;
        }
        placeOrder();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxmls/payment.fxml"));
            Parent root = loader.load();

            PaymentController controller = loader.getController();
            controller.setUserWalletBalance(currentUser.getWalletBalance());
            controller.setTotalPrice(cartTotal * 1.1);

            Stage stage = (Stage) goToPaymentButton.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void placeOrder() {
        try {
            List<OrderRequestDTO.OrderItem> orderItems = selectedFoodItems.stream()
                    .map(food -> {
                        OrderRequestDTO.OrderItem item = new OrderRequestDTO.OrderItem();
                        item.setFoodId(food.getId());
                        item.setQuantity(1);
                        return item;
                    })
                    .collect(Collectors.toList());

            OrderRequestDTO order = new OrderRequestDTO();
            order.setBuyerId(Integer.parseInt(currentUser.getId()));
            order.setItems(orderItems);

            String json = gson.toJson(order);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/orders"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + Session.getToken())
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() == 200 || response.statusCode() == 201) {
                            Platform.runLater(() -> showAlert("Order Placed", "Your order has been placed successfully!"));
                            cartListView.getItems().clear();
                            selectedFoodItems.clear();
                            cartTotal = 0;
                            updateCartLabels();
                        } else {
                            Platform.runLater(() -> showAlert("Order Failed", "Status: " + response.statusCode() + "\n" + response.body()));
                        }
                    })
                    .exceptionally(ex -> {
                        Platform.runLater(() -> showAlert("Order Error", ex.getMessage()));
                        return null;
                    });

        } catch (Exception e) {
            showAlert("Unexpected Error", e.getMessage());
        }
    }


    @FXML
    private void onEditProfileClicked() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxmls/UpdateYourProfile.fxml"));
            Parent root = loader.load();
            controller.UpdateProfileController controller = loader.getController();
            controller.setUserData(currentUser);
            Stage stage = (Stage) editProfileButton.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onWalletClicked() {
        double balance = currentUser.getWalletBalance();

        TextInputDialog amountDialog = new TextInputDialog();
        amountDialog.setTitle("Wallet");
        amountDialog.setHeaderText("Wallet Balance: $" + String.format("%.2f", balance));
        amountDialog.setContentText("Enter amount to charge (or leave blank to skip):");

        amountDialog.showAndWait().ifPresent(amountStr -> {
            try {
                double chargeAmount = Double.parseDouble(amountStr);
                showCardInputDialogAndCharge(chargeAmount);
            } catch (NumberFormatException e) {
                showAlert("Invalid amount", "Please enter a valid number.");
            }
        });
    }

    private void showCardInputDialogAndCharge(double amount) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Charge Wallet");

        VBox box = new VBox(10);
        TextField cardField = new TextField();
        cardField.setPromptText("Card Number");
        TextField cvvField = new TextField();
        cvvField.setPromptText("CVV2");
        TextField monthField = new TextField();
        monthField.setPromptText("MM");
        TextField yearField = new TextField();
        yearField.setPromptText("YY");
        PasswordField passField = new PasswordField();
        passField.setPromptText("Password");

        box.getChildren().addAll(new Label("Card Info:"), cardField, cvvField,
                new HBox(5, monthField, new Label("/"), yearField),
                new Label("Password:"), passField);

        dialog.getDialogPane().setContent(box);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                double currentBalance = currentUser.getWalletBalance();
                currentUser.setWalletBalance(currentBalance + amount);
                Session.setUser(currentUser); // update session
                showAlert("Success", "Wallet charged successfully!");
            }
        });
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private FoodCardData toFoodCardData(FoodItemDTO dto) {
        Food food = new Food(
                dto.getId(),
                dto.getName(),
                String.valueOf(dto.getPrice()),
                dto.getCategory(),
                dto.getDescription(),
                dto.getImageUrl()
        );
        return new FoodCardData(dto);
    }


}
