package com.example.beautyhub.models;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

public class User {

    // --- Data Pengguna Asas ---
    private String uid;
    private String username;
    private String email;
    private String userType;
    private String registrationDate;
    private int points;
    private boolean suspended;
    private String profileImage;
    private long timestamp;

    // --- Data Khusus Penjual (Seller) ---
    private String sellerName;
    private String shopDescription;
    private String phone;
    private String address; // Alamat penuh
    private String city;
    private String state;

    // --- Data Pembeli (Buyer) ---
    private Map<String, ShippingAddress> addresses;


    // Constructor kosong diperlukan untuk Firebase
    public User() {
    }

    // Constructor untuk mencipta pengguna baru
    public User(String username, String email, String userType, String registrationDate) {
        this.username = username;
        this.email = email;
        this.userType = userType;
        this.registrationDate = registrationDate;
        this.points = 0;
        this.suspended = false;
        this.addresses = new HashMap<>();
        this.profileImage = ""; // Inisialisasi sebagai string kosong
        this.sellerName = "";
        this.shopDescription = "";
        this.phone = "";
        this.address = "";
        this.city = "";
        this.state = "";
        this.timestamp = 0;
    }

    // --- GETTERS ---
    @Exclude
    public String getUid() {
        return uid;
    }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getUserType() { return userType; }
    public String getRegistrationDate() { return registrationDate; }
    public int getPoints() { return points; }
    public boolean isSuspended() { return suspended; }
    public Map<String, ShippingAddress> getAddresses() { return addresses; }
    public String getProfileImage() { return profileImage; }
    public long getTimestamp() { return timestamp; }


    // =========================================================
    // ▼▼▼ GETTER UNTUK DATA PENJUAL (YANG MENYELESAIKAN RALAT) ▼▼▼
    // =========================================================
    public String getSellerName() {
        return sellerName;
    }

    public String getShopDescription() {
        return shopDescription;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }
    // =========================================================


    // --- SETTERS ---
    public void setUid(String uid) {
        this.uid = uid;
    }
    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email) { this.email = email; }
    public void setUserType(String userType) { this.userType = userType; }
    public void setRegistrationDate(String registrationDate) { this.registrationDate = registrationDate; }
    public void setPoints(int points) { this.points = points; }
    public void setSuspended(boolean suspended) { this.suspended = suspended; }
    public void setAddresses(Map<String, ShippingAddress> addresses) { this.addresses = addresses; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }


    // =========================================================
    // ▼▼▼ SETTER UNTUK DATA PENJUAL (YANG MENYELESAIKAN RALAT) ▼▼▼
    // =========================================================
    public void setSellerName(String storeName) {
        this.sellerName = sellerName;
    }

    public void setShopDescription(String shopDescription) {
        this.shopDescription = shopDescription;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setState(String state) {
        this.state = state;
    }
    // =========================================================
}
