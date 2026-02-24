package Session;

import model.Courier;
import model.Seller;
import model.User;

public class Session {
    public static User loggedInUser;
    private static Seller currentSeller;
    private static Courier currentCourier;
    private static String token;

    public static void setUser(User user) {
        loggedInUser = user;
    }

    public static User getUser() {
        return loggedInUser;
    }

    public static void setCurrentSeller(Seller seller) {
        currentSeller = seller;
    }

    public static Seller getCurrentSeller() {
        return currentSeller;
    }

    public static void setCurrentCourier(Courier courier) {
        currentCourier = courier;
    }

    public static Courier getCurrentCourier() {
        return currentCourier;
    }

    public static void setToken(String t) {
        token = t;
    }

    public static String getToken() {
        return token;
    }
}
