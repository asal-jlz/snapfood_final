package model;

import DTO.FoodItemDTO;

public class FoodCardData {
    private final FoodItemDTO foodDTO;

    public FoodCardData(FoodItemDTO foodDTO) {
        this.foodDTO = foodDTO;
    }

    public FoodItemDTO getFoodDTO() {
        return foodDTO;
    }

    public String getRestaurantName() {
        return foodDTO.getRestaurantName();
    }

    public String getRestaurantLogoUrl() {
        return foodDTO.getRestaurantLogoUrl();
    }
}
