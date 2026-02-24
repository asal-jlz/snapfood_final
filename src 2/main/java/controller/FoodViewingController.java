package controller;

import DTO.FoodItemDTO;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.util.Base64;

public class FoodViewingController {

    @FXML private ImageView foodImageView;
    @FXML private ImageView restaurantLogo;
    @FXML private Label restaurantNameLabel;
    @FXML private Label foodDescriptionLabel;
    @FXML private Label foodPriceLabel;
    @FXML private Label foodCategoryLabel;
    @FXML private Label foodNameLabel;
    @FXML private TextArea opinionTextArea;
    @FXML private TextField ratingTextField;
    @FXML private Button photoButton;
    @FXML private Button submitButton;
    @FXML private Button addToCartButton;

    private File selectedPhotoFile;
    private String foodName;
    private double foodPrice;

    private CartUpdateListener cartUpdateListener;

    public interface CartUpdateListener {
        void onFoodAddedToCart(String foodName, double price);
    }

    public void setCartUpdateListener(CartUpdateListener listener) {
        this.cartUpdateListener = listener;
    }

    public void setFoodItemDTO(FoodItemDTO dto) {
        if (dto == null) return;
        this.foodName = dto.getName();
        this.foodPrice = dto.getPrice();

        if (foodNameLabel != null) foodNameLabel.setText(dto.getName());
        if (foodDescriptionLabel != null) foodDescriptionLabel.setText(dto.getDescription());
        if (foodPriceLabel != null) foodPriceLabel.setText("Price: $" + String.format("%.2f", dto.getPrice()));
        if (foodCategoryLabel != null) foodCategoryLabel.setText("Category: " + dto.getCategory());
        if (restaurantNameLabel != null) restaurantNameLabel.setText(dto.getRestaurantName());

        if (dto.getImageUrl() != null && !dto.getImageUrl().isBlank()) {
            try {
                foodImageView.setImage(new Image(dto.getImageUrl()));
            } catch (Exception e) {
                System.err.println("Invalid food image URL: " + dto.getImageUrl());
            }
        }

        if (dto.getRestaurantLogoUrl() != null && !dto.getRestaurantLogoUrl().isBlank()) {
            try {
                restaurantLogo.setImage(new Image(dto.getRestaurantLogoUrl()));
            } catch (Exception e) {
                System.err.println("Invalid logo URL: " + dto.getRestaurantLogoUrl());
            }
        }

        addToCartButton.setOnAction(e -> {
            if (cartUpdateListener != null) {
                cartUpdateListener.onFoodAddedToCart(foodName, foodPrice);
            } else {
                System.err.println("No cart listener set.");
            }
        });

        opinionTextArea.setDisable(false);
        ratingTextField.setDisable(false);

    }

    @FXML
    private void onPhotoButtonClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Food Photo");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            selectedPhotoFile = file;
            showAlert("Photo selected: " + file.getName());
        }
    }

    @FXML
    private void onSubmitButtonClick() {
        String opinion = opinionTextArea.getText();
        String ratingStr = ratingTextField.getText();

        if (opinion.isEmpty() || ratingStr.isEmpty()) {
            showAlert("Please fill in both opinion and rating.");
            return;
        }

        double rating;
        try {
            rating = Double.parseDouble(ratingStr);
            if (rating < 0 || rating > 5) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showAlert("Rating must be a number between 0 and 5.");
            return;
        }

        String encodedImage = "";
        if (selectedPhotoFile != null) {
            try (FileInputStream fis = new FileInputStream(selectedPhotoFile)) {
                byte[] imageBytes = fis.readAllBytes();
                encodedImage = Base64.getEncoder().encodeToString(imageBytes);
            } catch (IOException e) {
                showAlert("Error reading selected image.");
                return;
            }
        }

        sendReviewToServer(opinion, rating, encodedImage);
    }

    private void sendReviewToServer(String opinion, double rating, String base64Photo) {
        try {
            String jsonBody = String.format(
                    "{\"foodName\":\"%s\", \"opinion\":\"%s\", \"rating\": %.1f, \"photo\": \"%s\"}",
                    foodName, opinion, rating, base64Photo
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/review"))
                    .header("Content-Type", "application/json")
                    .POST(BodyPublishers.ofString(jsonBody))
                    .build();

            HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() == 200) {
                            showAlert("Review submitted successfully!");
                        } else {
                            showAlert("Failed to submit review. Server returned: " + response.statusCode());
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error submitting review.");
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.show();
    }

    public Button getAddToCartButton() {
        return addToCartButton;
    }
}
