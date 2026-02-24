package controller;

import com.google.gson.Gson;
import static spark.Spark.*;
import model.User;
import utils.SimpleJwtUtil;

public class AuthController {
    private static final Gson gson = new Gson();
    private static final String ADMIN_PHONE = "09002419261";
    private static final String ADMIN_PASSWORD = "admin";

    public static void initRoutes() {
        post("/admin/login", (req, res) -> {
            res.type("application/json");
            User loginRequest = gson.fromJson(req.body(), User.class);

            if (ADMIN_PHONE.equals(loginRequest.getPhone()) && ADMIN_PASSWORD.equals(loginRequest.getPassword())) {
                String token = SimpleJwtUtil.generateToken(-1, "admin");
                return gson.toJson(new LoginResponse(token, "admin"));
            } else {
                res.status(401);
                return gson.toJson(new ErrorResponse("Invalid admin credentials"));
            }
        });
    }

    static class LoginResponse {
        String token;
        String role;

        public LoginResponse(String token, String role) {
            this.token = token;
            this.role = role;
        }
    }

    static class ErrorResponse {
        String error;

        public ErrorResponse(String error) {
            this.error = error;
        }
    }
}
