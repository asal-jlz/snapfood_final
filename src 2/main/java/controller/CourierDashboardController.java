package controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.Node;
import model.Courier;
import model.Order;
import model.User;
import Session.Session;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class CourierDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private ImageView profileImageView;
    @FXML private Button logoutButton;
    @FXML private Button editProfileButton;
    @FXML private Label monthlyEarningsLabel;
    @FXML private TableView<Order> ordersTable;
    @FXML private TableColumn<Order, String> restaurantAddressCol;
    @FXML private TableColumn<Order, String> buyerAddressCol;
    @FXML private TableColumn<Order, Double> deliveryFeeCol;
    @FXML private TableColumn<Order, String> statusCol;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Courier courier;
    private User currentUser;

    public void setUserData(User user) {
        if (user instanceof Courier c) {
            this.courier = c;
            Session.setCurrentCourier(c);
            initializeUI();
        } else {
            System.err.println("Invalid user type for Courier Dashboard.");
        }
    }

    private void initializeUI() {
        if (courier == null) {
            System.err.println("Courier is null during UI initialization.");
            return;
        }

        welcomeLabel.setText("Welcome, " + courier.getFullName());
        monthlyEarningsLabel.setText("Monthly Earnings: $" + courier.getMonthlyEarnings());

        try {
            String photoPath = courier.getProfilePhotoPath();
            if (photoPath != null) {
                File file = new File(photoPath);
                if (file.exists()) {
                    profileImageView.setImage(new Image(new FileInputStream(file)));
                }
            }
        } catch (Exception e) {
            System.err.println("Profile image load failed: " + e.getMessage());
        }

        restaurantAddressCol.setCellValueFactory(data -> data.getValue().restaurantAddressProperty());
        buyerAddressCol.setCellValueFactory(data -> data.getValue().buyerAddressProperty());
        deliveryFeeCol.setCellValueFactory(data -> data.getValue().deliveryFeeProperty().asObject());
        statusCol.setCellValueFactory(data -> data.getValue().statusProperty());

        fetchOrdersFromBackend(courier.getId());
    }

    @FXML
    public void initialize() {
        if (courier == null) {
            courier = Session.getCurrentCourier();
        }
        if (courier != null) {
            initializeUI();
        } else {
            System.err.println("Courier not set on initialization.");
        }
    }

    private void fetchOrdersFromBackend(String courierId) {
        new Thread(() -> {
            try {
                URL url = new URL("http://localhost:8080/orders/courier/" + courierId);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                User currentUser = Session.getUser();
                if (currentUser != null && currentUser.getToken() != null) {
                    connection.setRequestProperty("Authorization", "Bearer " + currentUser.getToken());
                }

                connection.connect();

                if (connection.getResponseCode() == 200) {
                    List<Order> orders = objectMapper.readValue(connection.getInputStream(), new TypeReference<List<Order>>() {});
                    ObservableList<Order> observableOrders = FXCollections.observableArrayList(orders);

                    javafx.application.Platform.runLater(() -> {
                        ordersTable.setItems(observableOrders);
                    });
                } else {
                    System.err.println("Failed to fetch orders: " + connection.getResponseCode());
                }

                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void handleLogout(javafx.event.ActionEvent event) throws IOException {
        Session.setUser(null);
        Session.setCurrentCourier(null);
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxmls/Login.fxml"));
        Scene scene = new Scene(loader.load());
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(scene);
    }

    @FXML
    private void handleEditProfile(javafx.event.ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxmls/UpdateYourProfile.fxml"));
        Scene scene = new Scene(loader.load());

        UpdateProfileController controller = loader.getController();
        if (courier != null) {
            controller.setUserData(courier);
        }

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(scene);

    }

    public void setCourier(Courier courier) {
        this.courier = courier;
    }
}
