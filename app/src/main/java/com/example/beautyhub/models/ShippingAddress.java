// Make sure this is in your com.example.beautyhub.models package
package com.example.beautyhub.models;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.PropertyName;

public class ShippingAddress implements Parcelable {

    // ▼▼▼ NAMA MEDAN INI MESTI SEPADAN DENGAN FIREBASE ▼▼▼
    private String addressId;
    private String addressType; // Jenis alamat, cth: "Home", "Work"
    private String city;
    private String phoneNumber; // Sepadan dengan 'phoneNumber' di Firebase
    private String recipientName; // Sepadan dengan 'recipientName' di Firebase
    private String state;
    private String street;
    private String zipcode; // Sepadan dengan 'zipcode' di Firebase
    private boolean isDefault; // Anda perlu satu medan untuk alamat utama

    // Medan baharu untuk zon penghantaran
    private String zone;

    // Constructor kosong untuk Firebase
    public ShippingAddress() {
    }

    // --- GETTERS & SETTERS ---

    public String getAddressId() {
        return addressId;
    }

    public void setAddressId(String addressId) {
        this.addressId = addressId;
    }

    public String getAddressType() {
        return addressType;
    }

    public void setAddressType(String addressType) {
        this.addressType = addressType;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getState() {
        return state;
    }

    // Override setter untuk mengemas kini zon secara automatik
    public void setState(String state) {
        this.state = state;
        setZoneFromState(state); // Panggil kaedah pembantu
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getZipcode() {
        return zipcode;
    }

    public void setZipcode(String zipcode) {
        this.zipcode = zipcode;
    }

    // Ini akan dipetakan ke medan 'default' di Firebase
    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    // Getter dan Setter untuk medan 'zone'
    public String getZone() {
        // Jika zon kosong, cuba tentukan daripada negeri sedia ada
        if (zone == null || zone.isEmpty()) {
            setZoneFromState(this.state);
        }
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    // --- KAEDAH PEMBANTU (HELPER METHOD) ---
    @Exclude // Halang Firebase daripada menyimpan kaedah ini
    private void setZoneFromState(String state) {
        if (state == null || state.isEmpty()) {
            this.zone = "West Malaysia"; // Lalai jika tiada negeri
            return;
        }
        // Gunakan .trim() dan .equalsIgnoreCase() untuk perbandingan yang lebih teguh
        String trimmedState = state.trim().toLowerCase();
        if (trimmedState.equals("sabah") || trimmedState.equals("sarawak") || trimmedState.equals("labuan")) {
            this.zone = "East Malaysia";
        } else {
            this.zone = "West Malaysia";
        }
    }


    // --- IMPLEMENTASI PARCELABLE (WAJIB DIKEMAS KINI) ---

    protected ShippingAddress(Parcel in) {
        addressId = in.readString();
        addressType = in.readString();
        city = in.readString();
        phoneNumber = in.readString();
        recipientName = in.readString();
        state = in.readString();
        street = in.readString();
        zipcode = in.readString();
        isDefault = in.readByte() != 0;
        zone = in.readString(); //  Baca medan 'zone'
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(addressId);
        dest.writeString(addressType);
        dest.writeString(city);
        dest.writeString(phoneNumber);
        dest.writeString(recipientName);
        dest.writeString(state);
        dest.writeString(street);
        dest.writeString(zipcode);
        dest.writeByte((byte) (isDefault ? 1 : 0));
        dest.writeString(zone); //  Tulis medan 'zone'
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ShippingAddress> CREATOR = new Creator<ShippingAddress>() {
        @Override
        public ShippingAddress createFromParcel(Parcel in) {
            return new ShippingAddress(in);
        }

        @Override
        public ShippingAddress[] newArray(int size) {
            return new ShippingAddress[size];
        }
    };
}
