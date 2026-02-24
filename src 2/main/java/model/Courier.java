package model;

public class Courier extends User {

    private double monthlyEarnings;

    public Courier() {
        super();
    }

    public double getMonthlyEarnings() {
        return monthlyEarnings;
    }

    public void setMonthlyEarnings(double monthlyEarnings) {
        this.monthlyEarnings = monthlyEarnings;
    }

}
