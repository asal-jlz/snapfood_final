package controller;

import Session.Session;
import com.google.gson.Gson;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import model.Courier;
import model.Seller;
import model.User;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

public class UpdateProfileController {

    @FXML private TextField fullNameField;
    @FXML private TextField phoneNumberField;
    @FXML private TextField emailField;
    @FXML private TextField addressField;
    @FXML private TextField bankNameField;
    @FXML private TextField accountNumberField;
    @FXML private VBox bankInfoVBox;
    @FXML private Button updateButton;
    @FXML private ImageView profileImageView;

    private User currentUser;
    private String profileImageBase64;

    public void setUserData(User user) {
        this.currentUser = user;
        if (user == null) {
            throw new IllegalArgumentException("UpdateProfileController: passed user is null");
        }

        fullNameField.setText(user.getFullName());
        phoneNumberField.setText(user.getPhone());
        emailField.setText(user.getEmail());
        addressField.setText(user.getAddress());

        boolean isCourierOrSeller = user.getRole() != null &&
                (user.getRole().equalsIgnoreCase("courier") || user.getRole().equalsIgnoreCase("seller"));

        bankInfoVBox.setVisible(isCourierOrSeller);
        bankInfoVBox.setManaged(isCourierOrSeller);

        if (isCourierOrSeller && user.getBankInfo() != null) {
            bankNameField.setText(user.getBankInfo().getBankName());
            accountNumberField.setText(user.getBankInfo().getAccountNumber());
        }

        if (user.getProfileImageBase64() != null && !user.getProfileImageBase64().isEmpty()) {
            try {
                byte[] imageBytes = Base64.getDecoder().decode(user.getProfileImageBase64());
                Image image = new Image(new ByteArrayInputStream(imageBytes));
                profileImageView.setImage(image);
                profileImageBase64 = user.getProfileImageBase64();
            } catch (Exception e) {
                System.err.println("Failed to decode base64 image: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleUploadPhoto(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif"));

        File selectedFile = fileChooser.showOpenDialog(updateButton.getScene().getWindow());
        if (selectedFile != null) {
            try {
                Image image = new Image(new FileInputStream(selectedFile));
                profileImageView.setImage(image);

                byte[] fileContent = java.nio.file.Files.readAllBytes(selectedFile.toPath());
                profileImageBase64 = Base64.getEncoder().encodeToString(fileContent);

            } catch (IOException e) {
                e.printStackTrace();
                showAlert("Error", "Failed to load image: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void handleUpdateProfile(ActionEvent event) {
        if (currentUser == null) {
            showAlert("Error", "User data not initialized properly!", Alert.AlertType.ERROR);
            return;
        }

        try {
            JSONObject userJson = new JSONObject();
            userJson.put("fullName", fullNameField.getText());
            userJson.put("phone", phoneNumberField.getText());
            userJson.put("email", emailField.getText());
            userJson.put("address", addressField.getText());
            userJson.put("role", currentUser.getRole());

            if (currentUser.getRole() != null &&
                    (currentUser.getRole().equalsIgnoreCase("courier") || currentUser.getRole().equalsIgnoreCase("seller"))) {
                userJson.put("bankName", bankNameField.getText());
                userJson.put("accountNumber", accountNumberField.getText());
            }

            if (profileImageBase64 != null && !profileImageBase64.isEmpty()) {
                userJson.put("profileImageBase64", profileImageBase64);
            }

            URL url = new URL("http://localhost:8080/users/" + currentUser.getId() + "/profile");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Content-Type", "application/json");

            String jwt = Session.getToken();
            if (jwt != null && !jwt.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + jwt);
            }

            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = userJson.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int status = conn.getResponseCode();
            if (status == 200) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                    StringBuilder responseText = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseText.append(line.trim());
                    }
                    User updatedUser;
                    if ("seller".equalsIgnoreCase(currentUser.getRole())) {
                        updatedUser = new Gson().fromJson(responseText.toString(), Seller.class);
                    } else {
                        updatedUser = new Gson().fromJson(responseText.toString(), User.class);
                    }

                    if (updatedUser != null) {
                        currentUser = updatedUser;
                        Session.setUser(updatedUser);

                        System.out.println("Updated user role: " + currentUser.getRole());
                        if (currentUser.getRole() == null || currentUser.getRole().isBlank()) {
                            showAlert("Warning", "User role is missing after update!", Alert.AlertType.WARNING);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Failed to parse updated user: " + e.getMessage());
                }

                showAlert("Success", "Profile updated successfully!", Alert.AlertType.INFORMATION);
                redirectToDashboard();
            } else {
                InputStream errorStream = conn.getErrorStream();
                String errorMsg = "";
                if (errorStream != null) {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(errorStream))) {
                        errorMsg = br.readLine();
                    } catch (Exception e) {
                        errorMsg = "Error reading error message";
                    }
                }
                showAlert("Error", "Update failed with status: " + status + (errorMsg.isEmpty() ? "" : " → " + errorMsg), Alert.AlertType.ERROR);
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Exception", "Failed to update: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void redirectToDashboard() {
        try {
            if (currentUser == null || currentUser.getRole() == null || currentUser.getRole().isBlank()) {
                showAlert("Error", "User role is missing, cannot navigate", Alert.AlertType.ERROR);
                return;
            }
            String role = currentUser.getRole().toLowerCase();

            if (role.equals("seller") && !role.equals("seller")) {
                showAlert("Error", "Invalid user role '" + currentUser.getRole() + "' for seller dashboard", Alert.AlertType.ERROR);
                return;
            }

            String fxmlPath;
            switch (role) {
                case "buyer" -> fxmlPath = "/fxmls/buyer-dashboard.fxml";
                case "courier" -> fxmlPath = "/fxmls/Courier-dashboard.fxml";
                case "seller" -> fxmlPath = "/fxmls/seller-dashboard.fxml";
                default -> {
                    showAlert("Error", "Unknown role: " + currentUser.getRole(), Alert.AlertType.ERROR);
                    return;
                }
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent dashboardRoot = loader.load();

            Object controller = loader.getController();
            if (controller instanceof BuyerDashboardController buyerController) {
                buyerController.setUserData(currentUser);
            } else if (controller instanceof CourierDashboardController courierController) {
                Courier courier = new Courier();
                courier.setId(currentUser.getId());
                courier.setFullName(currentUser.getFullName());
                courier.setPhone(currentUser.getPhone());
                courier.setEmail(currentUser.getEmail());
                courier.setAddress(currentUser.getAddress());
                courier.setToken(currentUser.getToken());
                courier.setWalletBalance(currentUser.getWalletBalance());
                courier.setProfilePhotoPath(currentUser.getProfilePhotoPath());
                courier.setBankInfo(currentUser.getBankInfo());
                courier.setRole(currentUser.getRole());
                courier.setStatus(currentUser.getStatus());

                Session.setUser(courier);
                Session.setCurrentCourier(courier);

                courierController.setUserData(courier);
            } else if (controller instanceof SellerDashboardController sellerController) {
                sellerController.setUserData(currentUser);
            }

            Stage stage = (Stage) updateButton.getScene().getWindow();
            stage.setScene(new Scene(dashboardRoot));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Navigation Error", "Could not load dashboard: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
