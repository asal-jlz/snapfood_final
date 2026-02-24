package model;

public class Food {
    private final int id;
    private final String name;
    private final String price;
    private final String category;
    private final String description;
    private final String imageUrl;

    public Food(int id, String name, String price, String category, String description, String imageUrl) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
        this.description = description;
        this.imageUrl = imageUrl;
    }

    public Food(String name, String price, String category, String description, String imageUrl) {
        this(0, name, price, category, description, imageUrl);
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getPrice() { return price; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Food food = (Food) o;
        return id == food.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
