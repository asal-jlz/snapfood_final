package model;

public class OrderItem {
    private int id;
    private int orderId;
    private int foodItemId;
    private int quantity;
    private double unitPrice;

    public OrderItem() {}

    public OrderItem(int id, int orderId, int foodItemId, int quantity, double unitPrice) {
        this.id = id;
        this.orderId = orderId;
        this.foodItemId = foodItemId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getOrderId() {
        return orderId;
    }
    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getFoodItemId() {
        return foodItemId;
    }
    public void setFoodItemId(int foodItemId) {
        this.foodItemId = foodItemId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }
}