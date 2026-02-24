package DTO;

public class FoodItemDTO {
    public int id;
    public int restaurantId;
    public String name;
    public String imageUrl;
    public String description;
    public double price;
    public int stock;
    public String category;
    public String keywords;
    public String restaurantName;
    public String restaurantLogoUrl;

    public FoodItemDTO() {}

    public FoodItemDTO(int id, int restaurantId, String name, String imageUrl,
                       String description, double price, int stock, String category,
                       String keywords, String restaurantName, String restaurantLogoUrl) {
        this.id = id;
        this.restaurantId = restaurantId;
        this.name = name;
        this.imageUrl = imageUrl;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.category = category;
        this.keywords = keywords;
        this.restaurantName = restaurantName;
        this.restaurantLogoUrl = restaurantLogoUrl;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getRestaurantId() { return restaurantId; }
    public void setRestaurantId(int restaurantId) { this.restaurantId = restaurantId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }

    public String getRestaurantName() { return restaurantName; }
    public void setRestaurantName(String restaurantName) { this.restaurantName = restaurantName; }

    public String getRestaurantLogoUrl() { return restaurantLogoUrl; }
    public void setRestaurantLogoUrl(String restaurantLogoUrl) { this.restaurantLogoUrl = restaurantLogoUrl; }
}
