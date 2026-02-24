package controller;

import Session.Session;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import javafx.scene.Parent;
import model.User;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class SignUpController {

    @FXML private TextField fullNameField;
    @FXML private TextField phoneNumberField;
    @FXML private TextField addressField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Button nextButton;

    @FXML
    public void initialize() {
        roleComboBox.getItems().addAll("Buyer", "Seller", "Courier");
    }

    @FXML
    public void handleNextButton(ActionEvent event) {
        String name = fullNameField.getText();
        String phone = phoneNumberField.getText();
        String address = addressField.getText();
        String password = passwordField.getText();
        String role = roleComboBox.getValue();

        if (name.isEmpty() || phone.isEmpty() || address.isEmpty() || password.isEmpty() || role == null) {
            showAlert("Please fill in all fields.");
            return;
        }

        try {
            HttpClient client = HttpClient.newHttpClient();
            String json = String.format(
                    "{\"fullName\":\"%s\",\"phone\":\"%s\",\"address\":\"%s\",\"password\":\"%s\",\"role\":\"%s\"}",
                    name, phone, address, password, role.toLowerCase()
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8080/users/register"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            final ActionEvent capturedEvent = event;

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() == 201 || response.statusCode() == 200) {
                            try {
                                ObjectMapper mapper = new ObjectMapper();
                                JsonNode node = mapper.readTree(response.body());

                                int id = node.has("id") ? node.get("id").asInt() : 0;
                                String fullName = node.path("fullName").asText("");
                                String phoneNum = node.path("phone").asText("");
                                String addr = node.path("address").asText("");
                                String r = node.path("role").asText("");

                                User user = new User();
                                user.setId(String.valueOf(id));
                                user.setFullName(fullName);
                                user.setPhone(phoneNum);
                                user.setAddress(addr);
                                user.setRole(r);

                                loginAfterRegistration(phone, password, capturedEvent, user);

                            } catch (Exception e) {
                                e.printStackTrace();
                                Platform.runLater(() -> showAlert("Failed to parse registration response."));
                            }
                        } else {
                            Platform.runLater(() -> showAlert("Registration failed: " + response.body()));
                        }
                    });

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error during registration.");
        }
    }

    private void loginAfterRegistration(String phone, String password, ActionEvent event, User user) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            String loginJson = String.format(
                    "{\"phone\":\"%s\", \"password\":\"%s\"}", phone, password
            );

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
                                JsonNode loginNode = mapper.readTree(loginResponse.body());
                                String token = loginNode.path("token").asText("");

                                if (token == null || token.isEmpty()) {
                                    throw new IllegalArgumentException("Login token missing");
                                }

                                user.setToken(token);
                                Session.setUser(user);
                                Session.setToken(token);

                                Platform.runLater(() -> {
                                    try {
                                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxmls/complete-your-profile.fxml"));
                                        Parent root = loader.load();

                                        CompleteProfileController controller = loader.getController();
                                        controller.setUser(user);
                                        controller.setRole(user.getRole());
                                        controller.setToken(token);
                                        controller.setPlainPassword(password);

                                        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                                        stage.setScene(new Scene(root));
                                        stage.show();

                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        showAlert("Failed to load Complete Profile screen after login.");
                                    }
                                });

                            } catch (Exception e) {
                                e.printStackTrace();
                                showAlert("Failed to parse login response.");
                            }
                        } else {
                            Platform.runLater(() -> {
                                showAlert("Login failed after registration: " + loginResponse.body());
                            });
                        }
                    });

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error during login after registration.");
        }
    }

    private void showAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Validation Error");
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
