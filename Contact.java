package com.example.my;

public class Contact {
    private String name;
    private String phone;

    // Empty constructor needed for Firebase
    public Contact() {}

    public Contact(String name, String phone) {
        this.name = name;
        this.phone = phone;
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
