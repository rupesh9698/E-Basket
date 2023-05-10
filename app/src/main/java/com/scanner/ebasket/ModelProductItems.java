package com.scanner.ebasket;

@SuppressWarnings("ALL")
public class ModelProductItems {

    String name, price, timestamp, weight;

    public ModelProductItems() {
    }

    public ModelProductItems(String name, String price, String timestamp, String weight) {
        this.name = name;
        this.price = price;
        this.timestamp = timestamp;
        this.weight = weight;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getWeight() {
        return weight;
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }
}
