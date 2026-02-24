package model;

public class User {
    private static User instance;
    private String id;
    private String fullName;
    private String phone;
    private String email;
    private String address;
    private String profilePhotoPath;
    private String profileImagePath;
    private String token;
    private String role;
    private BankInfo bankInfo;
    private String profileImageBase64;
    private String brandName;
    private String logoUrl;
    private String shortDescription;
    private String salt;
    private String status;
    private double walletBalance;

    public User() {}

    public static User getInstance() {
        if (instance == null) {
            instance = new User();
        }
        return instance;
    }

    public String getId() { return id; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getAddress() { return address; }
    public String getProfilePhotoPath() { return profilePhotoPath; }
    public  String getToken() { return token; }
    public String getRole() { return role; }
    public void setRole(String role) {
        this.role = role;
    }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public void setId(String id) { this.id = id; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setEmail(String email) { this.email = email; }
    public void setAddress(String address) { this.address = address; }
    public void setProfilePhotoPath(String profilePhotoPath) { this.profilePhotoPath = profilePhotoPath; }
    public void setToken(String token) { this.token = token; }
    public BankInfo getBankInfo() { return bankInfo; }
    public void setBankInfo(BankInfo bankInfo) { this.bankInfo = bankInfo; }
    public String getProfileImageBase64() { return profileImageBase64; }
    public void setProfileImageBase64(String base64) { this.profileImageBase64 = base64; }

    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public String getShortDescription() { return shortDescription; }
    public void setShortDescription(String shortDescription) { this.shortDescription = shortDescription; }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public double getWalletBalance() {
        return walletBalance;
    }

    public void setWalletBalance(double walletBalance) {
        this.walletBalance = walletBalance;
    }


}

