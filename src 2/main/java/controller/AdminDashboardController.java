package controller;

import Session.Session;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.stage.Stage;
import javafx.beans.property.SimpleStringProperty;
import model.Order;
import model.User;
import model.Restaurant;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

public class AdminDashboardController {

    @FXML private Button logoutButton;
    @FXML private Button userListButton;
    @FXML private Button orderListButton;
    @FXML private Button salesReportButton;
    @FXML private Button systemStatsButton;
    @FXML private VBox userListView;
    @FXML private VBox orderListView;
    @FXML private VBox salesReportText;
    @FXML private VBox systemStatsView;
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> nameColumn;
    @FXML private TableColumn<User, String> phoneColumn;
    @FXML private TableColumn<User, String> roleColumn;
    @FXML private TableColumn<User, String> statusColumn;
    @FXML private Button approveUserButton;
    @FXML private Button deleteUserButton;
    @FXML private VBox restaurantListView;
    @FXML private TableView<Restaurant> restaurantTable;
    @FXML private TableColumn<Restaurant, String> restaurantNameColumn;
    @FXML private TableColumn<Restaurant, String> restaurantPhoneColumn;
    @FXML private TableColumn<Restaurant, String> restaurantAddressColumn;
    @FXML private Button approveRestaurantButton;
    @FXML private Button deleteRestaurantButton;
    @FXML private TableView<Order> orderTable;
    @FXML private TableColumn<Order, Number> orderIdColumn;
    @FXML private TableColumn<Order, String> restaurantNameCol;
    @FXML private TableColumn<Order, String> buyerAddressColumn;
    @FXML private TableColumn<Order, String> orderStatusColumn;
    @FXML private Label totalUsersLabel;
    @FXML private Label totalOrdersLabel;
    @FXML private Label totalSalesLabel;
    @FXML private Label ordersThisMonthLabel;
    @FXML private Label commissionPaidLabel;

    @FXML
    public void initialize() {
        setupUserTableColumns();
        hideAllViews();
        userListView.setVisible(true);
        loadUserTable();
        setupRestaurantTableColumns();
        setupOrderTableColumns();

    }

    private void setupOrderTableColumns() {
        orderIdColumn.setCellValueFactory(data -> data.getValue().idProperty());
        restaurantNameCol.setCellValueFactory(data -> new SimpleStringProperty("N/A")); // if not available
        buyerAddressColumn.setCellValueFactory(data -> data.getValue().buyerAddressProperty());
        orderStatusColumn.setCellValueFactory(data -> data.getValue().statusProperty());
    }

    private void hideAllViews() {
        userListView.setVisible(false);
        orderListView.setVisible(false);
        salesReportText.setVisible(false);
        systemStatsView.setVisible(false);
        restaurantListView.setVisible(false);
    }

    private void setupUserTableColumns() {
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFullName()));
        phoneColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPhone()));
        roleColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRole()));
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));
    }

    private void loadUserTable() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/admin/users"))
                .header("Authorization", "Bearer " + Session.getToken())
                .build();

        HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(response -> Platform.runLater(() -> {
                    Type userListType = new TypeToken<List<User>>() {}.getType();
                    List<User> users = new Gson().fromJson(response, userListType);
                    userTable.getItems().setAll(users);
                }));
    }

    @FXML
    public void onUserListClick(ActionEvent event) {
        hideAllViews();
        userListView.setVisible(true);
        loadUserTable();
    }

    private void loadOrderTable() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/admin/all"))
                .header("Authorization", "Bearer " + Session.getToken())
                .build();

        HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(response -> Platform.runLater(() -> {
                    Type orderListType = new TypeToken<List<Order>>() {}.getType();
                    List<Order> orders = new Gson().fromJson(response, orderListType);
                    orderTable.getItems().setAll(orders);
                }));
    }

    @FXML
    public void onOrderListClick(ActionEvent event) {
        hideAllViews();
        orderListView.setVisible(true);
        loadOrderTable();
    }

    @FXML
    public void onSalesReportClick(ActionEvent event) {
        hideAllViews();
        salesReportText.setVisible(true);

        totalSalesLabel.setText("Total Sales: $4250");
        ordersThisMonthLabel.setText("Orders This Month: 145");
        commissionPaidLabel.setText("Commission Paid: $380");
    }

    @FXML
    public void onSystemStatsClick(ActionEvent event) {
        hideAllViews();
        systemStatsView.setVisible(true);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/admin/stats"))
                .header("Authorization", "Bearer " + Session.getToken())
                .build();

        HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(json -> {
                    Gson gson = new Gson();
                    Map<String, Object> stats = gson.fromJson(json, Map.class);
                    Platform.runLater(() -> {
                        totalUsersLabel.setText("Total Users: " + ((Double) stats.get("totalUsers")).intValue());
                        totalOrdersLabel.setText("Total Orders: " + ((Double) stats.get("totalOrders")).intValue());
                    });
                });
    }

    @FXML
    public void onLogoutClick(ActionEvent event) {
        Session.setUser(null);
        Session.setToken(null);
        try {
            Parent loginRoot = FXMLLoader.load(getClass().getResource("/fxmls/login.fxml"));
            Scene loginScene = new Scene(loginRoot);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(loginScene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void onApproveUser(ActionEvent event) {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/admin/users/" + selected.getId() + "/approve"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("Authorization", "Bearer " + Session.getToken())
                .build();

        HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(this::loadUserTable));
    }

    @FXML
    public void onDeleteUser(ActionEvent event) {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/admin/users/" + selected.getId()))
                .DELETE()
                .header("Authorization", "Bearer " + Session.getToken())
                .build();

        HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(this::loadUserTable));
    }

    private void setupRestaurantTableColumns() {
        restaurantNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        restaurantPhoneColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPhone()));
        restaurantAddressColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAddress()));

    }

    private void loadRestaurantTable() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/admin/restaurants/pending"))
                .header("Authorization", "Bearer " + Session.getToken())
                .build();

        HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(response -> Platform.runLater(() -> {
                    Type listType = new TypeToken<List<Restaurant>>() {}.getType();
                    List<Restaurant> restaurants = new Gson().fromJson(response, listType);
                    restaurantTable.getItems().clear();
                    restaurantTable.getItems().setAll(restaurants);
                }));
    }

    @FXML
    public void onRestaurantListClick(ActionEvent event) {
        hideAllViews();
        restaurantListView.setVisible(true);
        loadRestaurantTable();
    }

    @FXML
    public void onApproveRestaurant(ActionEvent event) {
        Restaurant selected = restaurantTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/admin/restaurants/" + selected.getId() + "/approve"))
                .PUT(HttpRequest.BodyPublishers.noBody())
                .header("Authorization", "Bearer " + Session.getToken())
                .build();

        HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(this::loadRestaurantTable));
    }

    @FXML
    public void onDeleteRestaurant(ActionEvent event) {
        Restaurant selected = restaurantTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/admin/restaurants/" + selected.getId()))
                .DELETE()
                .header("Authorization", "Bearer " + Session.getToken())
                .build();

        HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(this::loadRestaurantTable));
    }

}
