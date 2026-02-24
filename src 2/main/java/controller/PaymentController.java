package controller;

import Session.Session;
import com.google.gson.Gson;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.User;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class PaymentController {

    @FXML private Label totalPriceLabel;
    @FXML private TextField cardNumberField, cvvField, expMonthField, expYearField;
    @FXML private PasswordField paymentPasswordField;

    private double totalPrice = 0.0;
    private double userWalletBalance = 0.0;

    public void setTotalPrice(double price) {
        this.totalPrice = price;
        if (totalPriceLabel != null)
            totalPriceLabel.setText("Total Price : $ " + String.format("%.2f", totalPrice));
    }

    public void setUserWalletBalance(double balance) {
        this.userWalletBalance = balance;
    }

    @FXML
    private void onCheckout() {
        if (validateCardInputs()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Payment Successful via Card!");
            alert.showAndWait();
            goToBuyerDashboard();
        } else {
            showError("Please complete all credit card fields correctly.");
        }
    }

    @FXML
    private void onPayWithWallet() {
        if (userWalletBalance < totalPrice) {
            showError("Not enough wallet balance. Please charge your wallet.");
            return;
        }

        userWalletBalance -= totalPrice;
        User user = Session.getUser();
        user.setWalletBalance(userWalletBalance);

        try {
            URL url = new URL("http://localhost:8080/users/" + user.getId() + "/profile");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + Session.getToken());
            conn.setDoOutput(true);

            String json = new Gson().toJson(user);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes());
                os.flush();
            }

            if (conn.getResponseCode() == 200) {
                System.out.println("✅ Wallet updated on backend.");
            } else {
                System.err.println("⚠️ Failed to update wallet. HTTP Code: " + conn.getResponseCode());
            }
            conn.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Network error during wallet update.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION,
                "Paid with wallet.\nRemaining Balance: $" + String.format("%.2f", userWalletBalance));
        alert.showAndWait();
        goToBuyerDashboard();
    }

    private boolean validateCardInputs() {
        return !cardNumberField.getText().trim().isEmpty()
                && !cvvField.getText().trim().isEmpty()
                && !expMonthField.getText().trim().isEmpty()
                && !expYearField.getText().trim().isEmpty()
                && !paymentPasswordField.getText().trim().isEmpty();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Payment Failed");
        alert.setHeaderText("Payment Error");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void goToBuyerDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxmls/buyer-dashboard.fxml"));
            Scene scene = new Scene(loader.load());

            BuyerDashboardController controller = loader.getController();
            controller.setUserData(Session.getUser());

            Stage stage = (Stage) totalPriceLabel.getScene().getWindow();
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
