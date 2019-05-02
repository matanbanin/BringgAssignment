package ordersAPI;

/**
 * Represents an order from a customer
 */
public class Order {

    private final String name;

    private final String phone;

    private final String address;
    private final String orderDetails;
    public Order(String name, String phone, String address, String orderDetails) {
        this.name = name;
        this.phone = phone;
        this.address = address;
        this.orderDetails = orderDetails;
    }


    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public String getOrderDetails() {
        return orderDetails;
    }

}