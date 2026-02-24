package DTO;

import javafx.beans.property.*;

public class OrderDTO {
    private final IntegerProperty id;
    private final StringProperty buyerName;
    private final StringProperty itemsSummary;
    private final DoubleProperty totalPrice;
    private final StringProperty status;
    private final StringProperty restaurantName;
    private final StringProperty buyerAddress;
    private final DoubleProperty deliveryFee;

    public OrderDTO(int id, String buyerName, String itemsSummary, double totalPrice, String status,
                    String restaurantName, String buyerAddress, double deliveryFee) {
        this.id = new SimpleIntegerProperty(id);
        this.buyerName = new SimpleStringProperty(buyerName);
        this.itemsSummary = new SimpleStringProperty(itemsSummary);
        this.totalPrice = new SimpleDoubleProperty(totalPrice);
        this.status = new SimpleStringProperty(status);
        this.restaurantName = new SimpleStringProperty(restaurantName);
        this.buyerAddress = new SimpleStringProperty(buyerAddress);
        this.deliveryFee = new SimpleDoubleProperty(deliveryFee);
    }

    public IntegerProperty idProperty() { return id; }
    public StringProperty buyerNameProperty() { return buyerName; }
    public StringProperty itemsSummaryProperty() { return itemsSummary; }
    public DoubleProperty totalPriceProperty() { return totalPrice; }
    public StringProperty statusProperty() { return status; }
    public StringProperty restaurantNameProperty() { return restaurantName; }
    public StringProperty buyerAddressProperty() { return buyerAddress; }
    public DoubleProperty deliveryFeeProperty() { return deliveryFee; }

    public int getId() { return id.get(); }
    public String getBuyerName() { return buyerName.get(); }
    public String getItemsSummary() { return itemsSummary.get(); }
    public double getTotalPrice() { return totalPrice.get(); }
    public String getStatus() { return status.get(); }
    public String getRestaurantName() { return restaurantName.get(); }
    public String getBuyerAddress() { return buyerAddress.get(); }
    public double getDeliveryFee() { return deliveryFee.get(); }
}
