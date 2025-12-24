package com.example.beautyhub.models;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

public class User {

    private String username;
    private String email;
    private String userType;
    private String registrationDate;
    private int points;
    private boolean suspended;
    private Map<String, ShippingAddress> addresses;
    private String uid;
    private String profileImage; // <-- 1. Field ditambah

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
        this.profileImage = null; // Inisialisasi field baru
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

    // vvv 2. Getter ditambah vvv
    public String getProfileImage() {
        return profileImage;
    }
    // ^^^ Akhir Getter ^^^

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

    // vvv 3. Setter ditambah vvv
    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }
    // ^^^ Akhir Setter ^^^
}
