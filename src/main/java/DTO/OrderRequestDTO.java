package DTO;

import model.OrderItem;

import java.util.List;

public class OrderRequestDTO {
    private int buyerId;
    private List<Integer> foodItemIds;
    private List<OrderItem> items;

    public OrderRequestDTO() {}

    public OrderRequestDTO(int buyerId, List<Integer> foodItemIds) {
        this.buyerId = buyerId;
        this.foodItemIds = foodItemIds;
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

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public static class OrderItem {
        private int foodId;
        private int quantity;

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
    }
}

