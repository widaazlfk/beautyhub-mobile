package com.example.beautyhub.models;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.ServerValue;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Product implements Parcelable {

    @Exclude
    private String productId;

    private String name;
    private String description;
    private double price;
    private String category;
    private String brand;
    private String ingredients;
    private String skinType;
    private int stock;
    private List<String> imageUrls;

    private float averageRating;
    private int soldCount;
    private double discountPrice;
    private boolean isOfficialStore;

    private boolean active;

    // Changed to Object to support ServerValue.TIMESTAMP (Map) and long (Long)
    private Object creationTimestamp;

    // Info Penjual
    @SerializedName("sellerId")
    private String sellerId;

    @SerializedName("sellerName")
    private String sellerName;

    @SerializedName("sellerProfileImageUrl")
    private String sellerProfileImageUrl;

    @SerializedName("isPreloaded")
    private boolean isPreloaded;

    public Product() {
        // IMPORTANT: Set active to true so it appears in Buyer's list
        this.active = true;
        // Use Firebase ServerValue for automatic timestamping
        this.creationTimestamp = ServerValue.TIMESTAMP;
        this.imageUrls = new ArrayList<>();
        this.averageRating = 0.0f;
        this.soldCount = 0;
        this.discountPrice = 0.0;
        this.isOfficialStore = false;
        this.isPreloaded = false;
    }

    @Exclude
    public double getFinalPrice() {
        return (discountPrice > 0 && discountPrice < price) ? discountPrice : price;
    }

    @Exclude
    public boolean hasDiscount() {
        return discountPrice > 0 && discountPrice < price;
    }

    @Exclude
    public int getDiscountPercentage() {
        if (!hasDiscount()) return 0;
        double percentage = ((price - discountPrice) / price) * 100;
        return (int) Math.round(percentage);
    }

    @Exclude
    public boolean isFromJson() {
        return isPreloaded;
    }

    @Exclude
    public boolean hasStock() {
        return this.stock > 0;
    }

    @Exclude
    public int getTotalStock() {
        return this.stock;
    }

    @Exclude
    public boolean isOutOfStock() {
        return !hasStock();
    }

    public void decreaseStock(int quantityToDecrease) {
        if (this.stock < quantityToDecrease) {
            throw new IllegalArgumentException("Stok produk tidak mencukupi.");
        }
        this.setStock(this.stock - quantityToDecrease);
        this.setSoldCount(this.getSoldCount() + quantityToDecrease);
    }

    // --- GETTERS AND SETTERS ---

    @Exclude
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getIngredients() { return ingredients; }
    public void setIngredients(String ingredients) { this.ingredients = ingredients; }

    public String getSkinType() { return skinType; }
    public void setSkinType(String skinType) { this.skinType = skinType; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }

    public float getAverageRating() { return averageRating; }
    public void setAverageRating(float averageRating) { this.averageRating = averageRating; }

    public int getSoldCount() { return soldCount; }
    public void setSoldCount(int soldCount) { this.soldCount = soldCount; }

    public double getDiscountPrice() { return discountPrice; }
    public void setDiscountPrice(double discountPrice) { this.discountPrice = discountPrice; }

    public boolean isOfficialStore() { return isOfficialStore; }
    public void setOfficialStore(boolean officialStore) { isOfficialStore = officialStore; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    // Helper to get timestamp as long (used for sorting/Parcelable)
    @Exclude
    public long getCreationTimestampLong() {
        if (creationTimestamp instanceof Long) {
            return (long) creationTimestamp;
        }
        return 0L;
    }

    public Object getCreationTimestamp() { return creationTimestamp; }
    public void setCreationTimestamp(Object creationTimestamp) { this.creationTimestamp = creationTimestamp; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }

    public String getSellerProfileImageUrl() { return sellerProfileImageUrl; }
    public void setSellerProfileImageUrl(String sellerProfileImageUrl) { this.sellerProfileImageUrl = sellerProfileImageUrl; }

    public boolean isPreloaded() { return isPreloaded; }
    public void setPreloaded(boolean preloaded) { this.isPreloaded = preloaded; }

    // --- PARCELABLE IMPLEMENTATION ---
    protected Product(Parcel in) {
        productId = in.readString();
        name = in.readString();
        description = in.readString();
        price = in.readDouble();
        category = in.readString();
        brand = in.readString();
        ingredients = in.readString();
        skinType = in.readString();
        stock = in.readInt();
        imageUrls = in.createStringArrayList();
        averageRating = in.readFloat();
        soldCount = in.readInt();
        discountPrice = in.readDouble();
        isOfficialStore = in.readByte() != 0;
        active = in.readByte() != 0;
        creationTimestamp = in.readLong(); // Read as long
        sellerId = in.readString();
        sellerName = in.readString();
        sellerProfileImageUrl = in.readString();
        isPreloaded = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(productId);
        dest.writeString(name);
        dest.writeString(description);
        dest.writeDouble(price);
        dest.writeString(category);
        dest.writeString(brand);
        dest.writeString(ingredients);
        dest.writeString(skinType);
        dest.writeInt(stock);
        dest.writeStringList(imageUrls);
        dest.writeFloat(averageRating);
        dest.writeInt(soldCount);
        dest.writeDouble(discountPrice);
        dest.writeByte((byte) (isOfficialStore ? 1 : 0));
        dest.writeByte((byte) (active ? 1 : 0));
        dest.writeLong(getCreationTimestampLong()); // Write as long
        dest.writeString(sellerId);
        dest.writeString(sellerName);
        dest.writeString(sellerProfileImageUrl);
        dest.writeByte((byte) (isPreloaded ? 1 : 0));
    }

    public static final Creator<Product> CREATOR = new Creator<Product>() {
        @Override
        public Product createFromParcel(Parcel in) {
            return new Product(in);
        }

        @Override
        public Product[] newArray(int size) {
            return new Product[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return Objects.equals(productId, product.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId);
    }

    @Override
    public String toString() {
        return "Product{" +
                "productId='" + productId + '\'' +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", active=" + active +
                ", stock=" + stock +
                '}';
    }
}