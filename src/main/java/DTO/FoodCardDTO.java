package DTO;

import model.FoodItem;

public class FoodCardDTO {
    private FoodItem food;
    private String restaurantName;
    private String restaurantLogoUrl;

    public FoodCardDTO(FoodItem food, String restaurantName, String restaurantLogoUrl) {
        this.food = food;
        this.restaurantName = restaurantName;
        this.restaurantLogoUrl = restaurantLogoUrl;
    }

    public FoodItem getFood() {
        return food;
    }

    public String getRestaurantName() {
        return restaurantName;
    }

    public String getRestaurantLogoUrl() {
        return restaurantLogoUrl;
    }
}
