package com.example.beautyhub.models;

import android.os.Parcel;
import android.os.Parcelable;

public class Variant implements Parcelable {
    private String id;          // ▼▼▼ TAMBAHAN: ID untuk Firebase reference
    private String name;        // Contoh: "Merah", "Saiz L", "500ml"
    private double priceModifier; // Contoh: 5.00 (harga asal + RM5.00), -2.00 (harga asal - RM2.00)
    private int stock;          // Stok spesifik untuk varian ini
    private String sku;         // ▼▼▼ TAMBAHAN: SKU/Barcode untuk varian
    private boolean isDefault;  // ▼▼▼ TAMBAHAN: Jika ini adalah varian default

    // Konstruktor kosong diperlukan untuk Firebase
    public Variant() {
    }

    // Constructor untuk varian biasa
    public Variant(String name, double priceModifier, int stock) {
        this.name = name;
        this.priceModifier = priceModifier;
        this.stock = stock;
        this.isDefault = false;
    }

    // ▼▼▼ TAMBAHAN: Constructor lengkap
    public Variant(String id, String name, double priceModifier, int stock, String sku, boolean isDefault) {
        this.id = id;
        this.name = name;
        this.priceModifier = priceModifier;
        this.stock = stock;
        this.sku = sku;
        this.isDefault = isDefault;
    }

    // --- GETTERS ---
    public String getId() { return id; }
    public String getName() { return name; }
    public double getPriceModifier() { return priceModifier; }
    public int getStock() { return stock; }
    public String getSku() { return sku; }
    public boolean isDefault() { return isDefault; }

    // --- SETTERS ---
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setPriceModifier(double priceModifier) { this.priceModifier = priceModifier; }
    public void setStock(int stock) { this.stock = stock; }
    public void setSku(String sku) { this.sku = sku; }
    public void setDefault(boolean isDefault) { this.isDefault = isDefault; }

    // ============ TAMBAHAN: HELPER METHODS ============

    /**
     * Check jika varian ini ada dalam stok
     */
    public boolean isInStock() {
        return stock > 0;
    }

    /**
     * Check jika varian ini out of stock
     */
    public boolean isOutOfStock() {
        return stock <= 0;
    }

    /**
     * Dapatkan harga dengan modifier untuk display
     * @param basePrice Harga asas product
     * @return Harga akhir untuk varian ini
     */
    public double getFinalPrice(double basePrice) {
        return basePrice + priceModifier;
    }

    /**
     * Dapatkan string untuk display harga modifier
     * Contoh: "+ RM5.00" atau "- RM2.00"
     */
    public String getPriceModifierDisplay() {
        if (priceModifier == 0) {
            return "";
        } else if (priceModifier > 0) {
            return String.format("+ RM%.2f", priceModifier);
        } else {
            return String.format("- RM%.2f", Math.abs(priceModifier));
        }
    }

    /**
     * Dapatkan nama dengan price modifier untuk display
     * Contoh: "Large (+RM5.00)" atau "Small (-RM2.00)"
     */
    public String getDisplayNameWithPrice() {
        String modifierDisplay = getPriceModifierDisplay();
        if (modifierDisplay.isEmpty()) {
            return name;
        }
        return name + " (" + modifierDisplay + ")";
    }

    /**
     * Check jika varian ini free (tidak tambah harga)
     */
    public boolean isFreeModifier() {
        return priceModifier == 0;
    }

    /**
     * Check jika varian ini lebih mahal
     */
    public boolean isMoreExpensive() {
        return priceModifier > 0;
    }

    /**
     * Check jika varian ini lebih murah
     */
    public boolean isCheaper() {
        return priceModifier < 0;
    }

    /**
     * Dapatkan stock status untuk display
     */
    public String getStockStatus() {
        if (stock <= 0) {
            return "Out of stock";
        } else if (stock <= 5) {
            return "Low stock: " + stock + " left";
        } else {
            return "In stock: " + stock;
        }
    }
    // ============ AKHIR TAMBAHAN ============

    // --- Implementasi Parcelable (updated dengan field baru) ---
    protected Variant(Parcel in) {
        id = in.readString();
        name = in.readString();
        priceModifier = in.readDouble();
        stock = in.readInt();
        sku = in.readString();
        isDefault = in.readByte() != 0;
    }

    public static final Creator<Variant> CREATOR = new Creator<Variant>() {
        @Override
        public Variant createFromParcel(Parcel in) {
            return new Variant(in);
        }

        @Override
        public Variant[] newArray(int size) {
            return new Variant[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeDouble(priceModifier);
        dest.writeInt(stock);
        dest.writeString(sku);
        dest.writeByte((byte) (isDefault ? 1 : 0));
    }

    // ============ TAMBAHAN: toString() untuk debugging ============
    @Override
    public String toString() {
        return "Variant{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", priceModifier=" + priceModifier +
                ", stock=" + stock +
                ", inStock=" + isInStock() +
                ", sku='" + sku + '\'' +
                ", isDefault=" + isDefault +
                '}';
    }

    // ============ TAMBAHAN: equals() dan hashCode() untuk comparison ============
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Variant variant = (Variant) o;

        // Compare by id jika ada
        if (id != null && variant.id != null) {
            return id.equals(variant.id);
        }

        // Fallback: compare by name (case insensitive)
        return name != null && name.equalsIgnoreCase(variant.name);
    }

    @Override
    public int hashCode() {
        // Gunakan name untuk hashCode jika id null
        String key = (id != null) ? id : name;
        return key != null ? key.hashCode() : 0;
    }
}