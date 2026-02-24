package model;

public class Restaurant {
    private int id;
    private String phone;
    private String address;
    private String name;
    private boolean approved;

    public Restaurant() {
    }

    public Restaurant(int id, String phone , String address, String name, Boolean approved) {
        this.id = id;
        this.phone = phone;
        this.address = address;
        this.name = name;
        this.approved = approved;
    }

    public int getId() {
        return id;
    }


    public String getName() {
        return name;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setAddress(String address) {
        this.address = address;
    }


}
