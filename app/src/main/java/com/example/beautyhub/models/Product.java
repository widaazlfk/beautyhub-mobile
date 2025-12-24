package com.example.beautyhub.models;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.ServerValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    // ▼▼▼ PERUBAHAN 1: TUKAR DARI LIST KE MAP ▼▼▼
    private Map<String, Variant> variants;

    private float averageRating;
    private int soldCount;
    private double discountPrice;
    private boolean isOfficialStore;

    private boolean active;
    private Object creationTimestamp;

    // Info Penjual
    private String sellerId;
    private String sellerName;
    private String sellerProfileImageUrl;

    private boolean isPreloaded;

    public Product() {
        this.active = true;
        this.creationTimestamp = ServerValue.TIMESTAMP;
        // ▼▼▼ PERUBAHAN 2: GUNA HASHMAP UNTUK MAP ▼▼▼
        this.variants = new HashMap<>();
        this.imageUrls = new ArrayList<>();
        this.averageRating = 0.0f;
        this.soldCount = 0;
        this.discountPrice = 0.0;
        this.isOfficialStore = false;
        this.isPreloaded = false;
    }

    // ============ HELPER METHODS ============

    @Exclude
    public boolean hasVariants() {
        // Logik ini kini berfungsi dengan betul untuk Map
        return variants != null && !variants.isEmpty();
    }

    // ▼▼▼ PERUBAHAN 3: KEMAS KINI GETVARIANTS() UNTUK KEMBALIKAN LIST DARI MAP ▼▼▼
    /**
     * Mengembalikan senarai (List) objek Variant dari Map.
     * Kod lain yang menggunakan getVariants() tidak perlu diubah.
     */
    public List<Variant> getVariants() {
        if (variants == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(variants.values());
    }
    // ▲▲▲ AKHIR PERUBAHAN 3 ▲▲▲

    @Exclude
    public Variant getVariantByName(String variantName) {
        if (!hasVariants()) return null;
        // Gunakan Map untuk carian yang lebih efisien (jika perlu), atau kekalkan gelung
        for (Variant variant : getVariants()) { // getVariants() kini mengembalikan List dari Map
            if (variant.getName().equalsIgnoreCase(variantName)) {
                return variant;
            }
        }
        return null;
    }

    @Exclude
    public Variant getVariantById(String variantId) {
        if (!hasVariants() || variantId == null) return null;
        // Carian kini lebih mudah dengan Map
        return variants.get(variantId);
    }

    // ... (Helper method lain tidak perlu diubah kerana bergantung pada getVariants() yang telah dibetulkan) ...
    @Exclude
    public double getFinalPrice() {
        return (discountPrice > 0) ? discountPrice : price;
    }
    @Exclude
    public double getPriceWithVariant(Variant variant) {
        if (variant == null) {
            return getFinalPrice();
        }
        return getFinalPrice() + variant.getPriceModifier();
    }
    @Exclude
    public String getDisplayNameWithVariant(Variant variant) {
        if (variant == null || variant.getName() == null || variant.getName().isEmpty()) {
            return name;
        }
        return name + " (" + variant.getName() + ")";
    }
    @Exclude
    public boolean isVariantAvailable(Variant variant) {
        if (variant == null) {
            return stock > 0;
        }
        return variant.getStock() > 0;
    }
    @Exclude
    public int getVariantStock(Variant variant) {
        if (variant == null) {
            return stock;
        }
        return variant.getStock();
    }
    @Exclude
    public List<String> getVariantNames() {
        List<String> names = new ArrayList<>();
        if (hasVariants()) {
            for (Variant variant : getVariants()) { // Menggunakan getVariants() yang dikemas kini
                names.add(variant.getName());
            }
        }
        return names;
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

    // --- GETTERS ---
    @Exclude
    public String getProductId() { return productId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getPrice() { return price; }
    public String getCategory() { return category; }
    public String getBrand() { return brand; }
    public String getIngredients() { return ingredients; }
    public String getSkinType() { return skinType; }
    public int getStock() { return stock; }
    public List<String> getImageUrls() { return imageUrls; }
    // Getter untuk Map, digunakan oleh Firebase
    public Map<String, Variant> getVariantsMap() { return variants; }
    public float getAverageRating() { return averageRating; }
    public int getSoldCount() { return soldCount; }
    public double getDiscountPrice() { return discountPrice; }
    public boolean isOfficialStore() { return isOfficialStore; }
    public boolean isActive() { return active; }
    public Object getCreationTimestamp() { return creationTimestamp; }
    public String getSellerId() { return sellerId; }
    public String getSellerName() { return sellerName; }
    public String getSellerProfileImageUrl() { return sellerProfileImageUrl; }
    public boolean isPreloaded() { return isPreloaded; }

    // --- SETTERS ---
    @Exclude
    public void setProductId(String productId) { this.productId = productId; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setPrice(double price) { this.price = price; }
    public void setCategory(String category) { this.category = category; }
    public void setBrand(String brand) { this.brand = brand; }
    public void setIngredients(String ingredients) { this.ingredients = ingredients; }
    public void setSkinType(String skinType) { this.skinType = skinType; }
    public void setStock(int stock) { this.stock = stock; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
    // Setter untuk Map, digunakan oleh Firebase
    public void setVariants(Map<String, Variant> variants) { this.variants = variants; }
    public void setAverageRating(float averageRating) { this.averageRating = averageRating; }
    public void setSoldCount(int soldCount) { this.soldCount = soldCount; }
    public void setDiscountPrice(double discountPrice) { this.discountPrice = discountPrice; }
    public void setOfficialStore(boolean officialStore) { isOfficialStore = officialStore; }
    public void setActive(boolean active) { this.active = active; }
    public void setCreationTimestamp(Object creationTimestamp) { this.creationTimestamp = creationTimestamp; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }
    public void setSellerProfileImageUrl(String sellerProfileImageUrl) { this.sellerProfileImageUrl = sellerProfileImageUrl; }
    public void setPreloaded(boolean preloaded) { this.isPreloaded = preloaded; }

    // --- Implementasi Parcelable (WAJIB DIKEMAS KINI) ---
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

        // ▼▼▼ PERUBAHAN 4: BACA MAP DARI PARCEL ▼▼▼
        int size = in.readInt();
        if (size > -1) {
            variants = new HashMap<>();
            for (int i = 0; i < size; i++) {
                String key = in.readString();
                Variant value = in.readParcelable(Variant.class.getClassLoader());
                variants.put(key, value);
            }
        } else {
            variants = null;
        }
        // ▲▲▲ AKHIR PERUBAHAN 4 ▲▲▲

        averageRating = in.readFloat();
        soldCount = in.readInt();
        discountPrice = in.readDouble();
        isOfficialStore = in.readByte() != 0;
        active = in.readByte() != 0;
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

        // ▼▼▼ PERUBAHAN 5: TULIS MAP KE PARCEL ▼▼▼
        if (variants == null) {
            dest.writeInt(-1);
        } else {
            dest.writeInt(variants.size());
            for (Map.Entry<String, Variant> entry : variants.entrySet()) {
                dest.writeString(entry.getKey());
                dest.writeParcelable(entry.getValue(), flags);
            }
        }
        // ▲▲▲ AKHIR PERUBAHAN 5 ▲▲▲

        dest.writeFloat(averageRating);
        dest.writeInt(soldCount);
        dest.writeDouble(discountPrice);
        dest.writeByte((byte) (isOfficialStore ? 1 : 0));
        dest.writeByte((byte) (active ? 1 : 0));
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
    public String toString() {
        return "Product{" +
                "productId='" + productId + '\'' +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", stock=" + stock +
                ", hasVariants=" + hasVariants() +
                ", variantCount=" + (variants != null ? variants.size() : 0) +
                '}';
    }

    @Exclude
    public boolean hasStock() {
        if (hasVariants()) {
            for (Variant variant : getVariants()) { // Menggunakan getVariants() yang telah dibetulkan
                if (variant.getStock() > 0) {
                    return true;
                }
            }
            return false;
        } else {
            return this.stock > 0;
        }
    }
}
