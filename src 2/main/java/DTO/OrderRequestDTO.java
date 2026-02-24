package DTO;

import java.util.List;

public class OrderRequestDTO {
    private  int buyerId;
    private int restaurantId;
    private List<OrderItem> items;
    private double totalPrice;
    private List<Integer> foodItemIds;

    public OrderRequestDTO() {}
    public OrderRequestDTO(int restaurantId, List<OrderItem> items, double totalPrice) {
        this.restaurantId = restaurantId;
        this.items = items;
        this.totalPrice = totalPrice;
    }

    public OrderRequestDTO(int buyerId, List<Integer> foodItemIds) {
        this.buyerId = buyerId;
        this.foodItemIds = foodItemIds;
    }

    public int getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(int restaurantId) {
        this.restaurantId = restaurantId;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public int getBuyerId() { return buyerId; }
    public void setBuyerId(int buyerId) { this.buyerId = buyerId; }

    public static class OrderItem {
        private int foodId;
        private int quantity;
        private int buyerId;
        private List<Integer> foodItemIds;

        public OrderItem() {}

        public OrderItem(int foodId, int quantity) {
            this.foodId = foodId;
            this.quantity = quantity;
        }

        public int getFoodId() {
            return foodId;
        }

        public void setFoodId(int foodId) {
            this.foodId = foodId;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public int getBuyerId() {
            return buyerId;
        }

        public void setBuyerId(int buyerId) {
            this.buyerId = buyerId;
        }

        public List<Integer> getFoodItemIds() {
            return foodItemIds;
        }

        public void setFoodItemIds(List<Integer> foodItemIds) {
            this.foodItemIds = foodItemIds;
        }
    }
}
