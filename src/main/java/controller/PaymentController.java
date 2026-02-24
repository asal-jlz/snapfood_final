package controller;

import DAO.OrderDAO;
import DAO.WalletDAO;
import com.google.gson.Gson;
import model.Order;
import model.Wallet;

import static spark.Spark.*;

public class PaymentController {
    public static void initRoutes() {
        Gson gson = new Gson();

        post("/payment/:orderId", (req, res) -> {
            int orderId = Integer.parseInt(req.params(":orderId"));
            String method = req.queryParams("method"); // "card" or "wallet"
            String cardNumber = req.queryParams("cardNumber"); // optional

            Order order = OrderDAO.getOrderById(orderId);
            if (order == null) {
                res.status(404);
                return gson.toJson(new ErrorResponse("Order not found"));
            }

            double amount = order.getTotalAmount() + order.getTax() + order.getDeliveryFee();

            if ("wallet".equalsIgnoreCase(method)) {
                Wallet wallet = WalletDAO.getWalletByUserId(order.getUserId());
                if (wallet == null || wallet.getBalance() < amount) {
                    res.status(400);
                    return gson.toJson(new ErrorResponse("Insufficient wallet balance"));
                }

                double newBalance = wallet.getBalance() - amount;
                WalletDAO.updateWalletBalance(order.getUserId(), newBalance);
            }

            OrderDAO.updateOrderStatus(orderId, "PAID");

            return gson.toJson(new SuccessResponse("Payment successful. Order is paid."));
        });
    }

    private static class ErrorResponse {
        String error;
        public ErrorResponse(String error) { this.error = error; }
    }

    private static class SuccessResponse {
        String message;
        public SuccessResponse(String message) { this.message = message; }
    }
}