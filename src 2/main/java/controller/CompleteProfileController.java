package controller;

import Session.Session;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.Seller;
import model.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class CompleteProfileController {

    @FXML private Button uploadPhotoButton;
    @FXML private VBox bankInfoSection;
    @FXML private TextField bankNameField;
    @FXML private TextField accountNumberField;
    @FXML private VBox brandInfoSection;
    @FXML private TextField brandNameField;
    @FXML private TextField descriptionField;
    @FXML private Button continueButton;
    @FXML private ImageView profileImageView;

    private String role;
    private User user;
    private String token;
    private String plainPassword;

    public void setRole(String role) {
        this.role = role;
        System.out.println("User role set to: " + role);

        if (role.equalsIgnoreCase("buyer")) {
            bankInfoSection.setVisible(true);
            bankNameField.setDisable(true);
            accountNumberField.setDisable(true);
            brandInfoSection.setVisible(true);
            brandNameField.setDisable(true);
            descriptionField.setDisable(true);
        } else if (role.equalsIgnoreCase("courier")) {
            bankInfoSection.setVisible(true);
            bankNameField.setDisable(false);
            accountNumberField.setDisable(false);
            brandInfoSection.setVisible(true);
            brandNameField.setDisable(true);
            descriptionField.setDisable(true);
        } else if (role.equalsIgnoreCase("seller")) {
            bankInfoSection.setVisible(true);
            bankNameField.setDisable(false);
            accountNumberField.setDisable(false);
            brandInfoSection.setVisible(true);
            brandNameField.setDisable(false);
            descriptionField.setDisable(false);
        }
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setPlainPassword(String plainPassword) {
        this.plainPassword = plainPassword;
    }

    @FXML
    public void initialize() {
        if (user != null && "seller".equalsIgnoreCase(role)) {
            if (user instanceof Seller seller) {
                brandNameField.setText(seller.getBrandName() != null ? seller.getBrandName() : "");
                descriptionField.setText(seller.getShortDescription() != null ? seller.getShortDescription() : "");
            }

            if (user instanceof Seller seller && seller.getLogoUrl() != null) {
                profileImageView.setImage(new Image(seller.getLogoUrl()));
            }
        }
    }

    @FXML
    public void onUploadPhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Profile Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        File selectedFile = fileChooser.showOpenDialog(null);

        if (selectedFile != null) {
            Image image = new Image(selectedFile.toURI().toString());
            profileImageView.setImage(image);
            System.out.println("Image uploaded: " + selectedFile.getName());

            if (user instanceof Seller seller) {
                seller.setLogoUrl(selectedFile.toURI().toString());
                Session.setCurrentSeller(seller);
                Session.setUser(seller);
            }
        }
    }

    @FXML
    public void onContinue() {
        if (user == null) {
            showAlert("User session is missing. Please login again.");
            return;
        }

        String bankName = bankNameField.getText();
        String accountNumber = accountNumberField.getText();
        String brandName = brandNameField.getText();
        String description = descriptionField.getText();

        String json = String.format(
                "{\"role\":\"%s\", \"bankName\":\"%s\", \"accountNumber\":\"%s\", \"brandName\":\"%s\", \"description\":\"%s\"}",
                role, bankName, accountNumber, brandName, description
        );

        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8080/users/" + user.getId() + "/profile"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        System.out.println("Response status: " + response.statusCode());
                        System.out.println("Response body: " + response.body());

                        if (response.statusCode() == 200) {
                            System.out.println("Profile completed successfully.");
                            if ("seller".equalsIgnoreCase(role) && user instanceof Seller seller) {
                                createRestaurantForSeller(seller); // ✅ Register restaurant
                            }
                            autoLogin(user.getPhone(), plainPassword);
                        } else {
                            javafx.application.Platform.runLater(() ->
                                    showAlert("Failed to complete profile: " + response.body()));
                        }
                    });

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error completing profile.");
        }
    }

    private void autoLogin(String phone, String password) {
        try {
            String loginJson = String.format("{\"phone\":\"%s\", \"password\":\"%s\"}", phone, password);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest loginRequest = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8080/users/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(loginJson))
                    .build();

            client.sendAsync(loginRequest, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(loginResponse -> {
                        if (loginResponse.statusCode() == 200) {
                            try {
                                ObjectMapper mapper = new ObjectMapper();
                                JsonNode node = mapper.readTree(loginResponse.body());

                                String role = node.path("role").asText(""); // ✅ Safe
                                User loggedUser;
                                this.role = role;

                                if ("seller".equalsIgnoreCase(role)) {
                                    Seller seller = new Seller();
                                    seller.setId(String.valueOf(node.path("id").asInt())); // ✅ Safe
                                    seller.setFullName(node.path("fullName").asText(""));
                                    seller.setPhone(node.path("phone").asText(""));
                                    seller.setRole(role);
                                    seller.setAddress(node.path("address").asText(""));
                                    seller.setToken(node.path("token").asText(""));
                                    seller.setBrandName(node.path("brandName").asText(""));
                                    seller.setShortDescription(node.path("shortDescription").asText(""));
                                    seller.setRestaurantId(node.path("restaurantId").asInt());
                                    loggedUser = seller;
                                    Session.setCurrentSeller(seller);
                                } else {
                                    User genericUser = new User();
                                    genericUser.setId(String.valueOf(node.path("id").asInt()));
                                    genericUser.setFullName(node.path("fullName").asText(""));
                                    genericUser.setPhone(node.path("phone").asText(""));
                                    genericUser.setEmail(node.path("email").asText(""));
                                    genericUser.setRole(role);
                                    genericUser.setAddress(node.path("address").asText(""));
                                    genericUser.setSalt(node.path("salt").asText(""));
                                    genericUser.setToken(node.path("token").asText(""));
                                    loggedUser = genericUser;
                                }

                                Session.setUser(loggedUser);
                                Session.setToken(loggedUser.getToken());
                                redirectToDashboard();

                            } catch (Exception e) {
                                e.printStackTrace();
                                showAlert("Failed to parse login response.");
                            }
                        } else {
                            showAlert("Auto-login failed after profile completion.");
                        }
                    });

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error during auto-login.");
        }
    }



    private void redirectToDashboard() {
        javafx.application.Platform.runLater(() -> {
            try {
                String fxmlPath = switch (role.toLowerCase()) {
                    case "buyer" -> "/fxmls/buyer-dashboard.fxml";
                    case "courier" -> "/fxmls/Courier-dashboard.fxml";
                    case "seller" -> "/fxmls/seller-dashboard.fxml";
                    default -> "/fxmls/login.fxml";
                };

                Parent dashboardRoot = FXMLLoader.load(getClass().getResource(fxmlPath));
                Stage stage = (Stage) continueButton.getScene().getWindow();
                stage.setScene(new Scene(dashboardRoot));
            } catch (IOException e) {
                e.printStackTrace();
                showAlert("Failed to load dashboard.");
            }
        });
    }

    private void showAlert(String message) {
        javafx.application.Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Profile Error");
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void createRestaurantForSeller(Seller seller) {
        try {
            String json = String.format(
                    "{\"sellerId\": %d, \"name\": \"%s\", \"address\": \"%s\", \"phone\": \"%s\", \"openingHours\": \"%s\", \"logoUrl\": \"%s\"}",
                    Integer.parseInt(seller.getId()),
                    seller.getBrandName(),
                    seller.getAddress(),
                    seller.getPhone(),
                    "9:00 - 23:00",
                    seller.getLogoUrl() != null ? seller.getLogoUrl() : ""
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8080/restaurants"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(res -> {
                        if (res.statusCode() == 201) {
                            System.out.println("✅ Restaurant created successfully.");
                        } else {
                            System.err.println("❌ Failed to create restaurant: " + res.body());
                        }
                    });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
