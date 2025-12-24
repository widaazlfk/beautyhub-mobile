package com.example.beautyhub.data;

import android.content.Context;
import com.example.beautyhub.models.Product;
import com.example.beautyhub.models.Variant;
import com.google.firebase.database.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.*;

public class ProductRepository {

    private final Context context;
    private final DatabaseReference productsRef;
    private final Gson gson;

    public ProductRepository(Context context) {
        this.context = context;
        this.productsRef = FirebaseDatabase.getInstance().getReference("Products");
        this.gson = new Gson();
    }

    // ==================== INTERFACES ====================
    public interface ProductsCallback {
        void onSuccess(List<Product> products);
        void onError(String message);
    }

    public interface ProductCallback {
        void onSuccess(Product product);
        void onError(String message);
    }

    public interface OperationCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface StringCallback {
        void onSuccess(String result);
        void onError(String message);
    }

    // ==================== PRIVATE METHODS ====================
    // Gantikan keseluruhan kaedah loadBaseProductsFromJson() yang lama
    // Gantikan keseluruhan kaedah loadBaseProductsFromJson()
    private List<Product> loadBaseProductsFromJson() {
        List<Product> products = new ArrayList<>();
        try {
            // ▼▼▼ PEMBETULAN 1: Baca fail JSON yang betul ▼▼▼
            String json = loadJsonFromAssets("beautyhub_data.json");

            // ▼▼▼ PEMBETULAN 2: Gunakan Gson dengan TypeAdapter khas ▼▼▼
            GsonBuilder gsonBuilder = new GsonBuilder();

            // "Ajar" Gson cara menukar 'variants' dari Array ke Map
            Type variantsType = new TypeToken<Map<String, Variant>>(){}.getType();
            gsonBuilder.registerTypeAdapter(variantsType, new VariantsTypeAdapter());

            Gson gson = gsonBuilder.create();

            // Langkah A: Baca JSON luar sebagai Map
            Type outerMapType = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> outerMap = gson.fromJson(json, outerMapType);

            // Langkah B: Ambil bahagian "Products" sahaja
            Object productsObject = outerMap.get("Products");

            if (productsObject instanceof Map) {
                // Tukar semula bahagian "Products" ke JSON
                String productsJson = gson.toJson(productsObject);

                // Tentukan jenis data sebagai Map<String, Product>
                Type productMapType = new TypeToken<Map<String, Product>>(){}.getType();
                Map<String, Product> productMap = gson.fromJson(productsJson, productMapType);

                // Langkah C: Ulang melalui setiap produk
                if (productMap != null) {
                    for (Product product : productMap.values()) {
                        product.setPreloaded(true);
                        product.setSellerId("system");
                        product.setOfficialStore(true);
                        products.add(product);
                    }
                }
            }

        } catch (Exception e) {
            android.util.Log.e("ProductRepository", "Gagal memuatkan produk dari beautyhub_data.json", e);
        }
        return products;
    }



    private String loadJsonFromAssets(String fileName) throws IOException {
        InputStream is = context.getAssets().open(fileName);
        int size = is.available();
        byte[] buffer = new byte[size];
        is.read(buffer);
        is.close();
        return new String(buffer, "UTF-8");
    }

    // ==================== PUBLIC METHODS ====================

    // 1. GET ALL PRODUCTS (JSON + Firebase)
    public void getAllProducts(ProductsCallback callback) {
        List<Product> allProducts = new ArrayList<>();

        // Add base products from JSON
        List<Product> baseProducts = loadBaseProductsFromJson();
        allProducts.addAll(baseProducts);

        // Get seller products from Firebase
        productsRef.orderByChild("isPreloaded").equalTo(false)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Product sellerProduct = snapshot.getValue(Product.class);
                            if (sellerProduct != null && sellerProduct.isActive()) {
                                sellerProduct.setProductId(snapshot.getKey());
                                allProducts.add(sellerProduct);
                            }
                        }
                        callback.onSuccess(allProducts);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        // If Firebase fails, return base products only
                        callback.onSuccess(baseProducts);
                    }
                });
    }

    // 2. GET PRODUCT BY ID
    public void getProductById(String productId, ProductCallback callback) {
        // Check if it's a base product
        if (productId.startsWith("base_")) {
            List<Product> baseProducts = loadBaseProductsFromJson();
            for (Product product : baseProducts) {
                if (productId.equals(product.getProductId())) {
                    callback.onSuccess(product);
                    return;
                }
            }
            callback.onError("Product not found");
            return;
        }

        // Check Firebase for seller product
        productsRef.child(productId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Product product = dataSnapshot.getValue(Product.class);
                if (product != null) {
                    product.setProductId(dataSnapshot.getKey());
                    callback.onSuccess(product);
                } else {
                    callback.onError("Product not found");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.onError(databaseError.getMessage());
            }
        });
    }

    // 3. ADD SELLER PRODUCT
    // 3. ADD SELLER PRODUCT
    public void addSellerProduct(Product product, List<Variant> variants, StringCallback callback) {
        DatabaseReference newProductRef = productsRef.push();
        String productId = newProductRef.getKey();

        // Set product properties
        product.setProductId(productId);
        product.setPreloaded(false);
        product.setActive(true);
        product.setCreationTimestamp(ServerValue.TIMESTAMP);

        // Set variants if any
        if (variants != null && !variants.isEmpty()) {
            // --- START OF FIX ---
            // Convert List<Variant> to Map<String, Variant>
            Map<String, Variant> variantsMap = new LinkedHashMap<>(); // Use LinkedHashMap to preserve order
            for (int i = 0; i < variants.size(); i++) {
                Variant variant = variants.get(i);
                String variantId = variant.getId();
                // Generate a unique ID if it doesn't exist
                if (variantId == null || variantId.isEmpty()) {
                    variantId = "var_" + productId + "_" + (i + 1);
                    variant.setId(variantId);
                }
                variantsMap.put(variantId, variant);
            }
            product.setVariants(variantsMap); // Now passing the correct Map type
            // --- END OF FIX ---
        } else {
            // If there are no variants, ensure you set an empty map if your setter expects one,
            // or handle it as your Product model requires.
            product.setVariants(new HashMap<>());
        }

        // Set default values
        if (product.getImageUrls() == null) {
            product.setImageUrls(new ArrayList<>());
        }

        // Save to Firebase
        newProductRef.setValue(product)
                .addOnSuccessListener(aVoid -> callback.onSuccess(productId))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // 4. UPDATE PRODUCT
    public void updateProduct(Product product, OperationCallback callback) {
        if (product.getProductId() == null || product.getProductId().isEmpty()) {
            callback.onError("Product ID is required");
            return;
        }

        // Cannot update base products
        if (product.isPreloaded()) {
            callback.onError("Cannot update base products");
            return;
        }

        productsRef.child(product.getProductId()).setValue(product)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // 5. DELETE PRODUCT (Soft delete - set active to false)
    public void deleteProduct(String productId, OperationCallback callback) {
        // Cannot delete base products
        if (productId.startsWith("base_")) {
            callback.onError("Cannot delete base products");
            return;
        }

        productsRef.child(productId).child("active").setValue(false)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // 6. GET PRODUCTS BY SELLER
    public void getProductsBySeller(String sellerId, ProductsCallback callback) {
        if ("system".equals(sellerId)) {
            // Get base products
            List<Product> baseProducts = loadBaseProductsFromJson();
            callback.onSuccess(baseProducts);
        } else {
            // Get seller's products from Firebase
            productsRef.orderByChild("sellerId").equalTo(sellerId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            List<Product> sellerProducts = new ArrayList<>();
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                Product product = snapshot.getValue(Product.class);
                                if (product != null && product.isActive()) {
                                    product.setProductId(snapshot.getKey());
                                    sellerProducts.add(product);
                                }
                            }
                            callback.onSuccess(sellerProducts);
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            callback.onError(databaseError.getMessage());
                        }
                    });
        }
    }

    // 7. GET PRODUCTS BY CATEGORY
    public void getProductsByCategory(String category, ProductsCallback callback) {
        getAllProducts(new ProductsCallback() {
            @Override
            public void onSuccess(List<Product> allProducts) {
                List<Product> filtered = new ArrayList<>();
                for (Product product : allProducts) {
                    if (category.equalsIgnoreCase(product.getCategory())) {
                        filtered.add(product);
                    }
                }
                callback.onSuccess(filtered);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    // 8. SEARCH PRODUCTS
    public void searchProducts(String query, ProductsCallback callback) {
        getAllProducts(new ProductsCallback() {
            @Override
            public void onSuccess(List<Product> allProducts) {
                List<Product> results = new ArrayList<>();
                String searchQuery = query.toLowerCase();

                for (Product product : allProducts) {
                    if (product.getName().toLowerCase().contains(searchQuery) ||
                            (product.getDescription() != null &&
                                    product.getDescription().toLowerCase().contains(searchQuery)) ||
                            (product.getBrand() != null &&
                                    product.getBrand().toLowerCase().contains(searchQuery)) ||
                            (product.getCategory() != null &&
                                    product.getCategory().toLowerCase().contains(searchQuery))) {
                        results.add(product);
                    }
                }
                callback.onSuccess(results);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    // 9. UPDATE STOCK
    public void updateStock(String productId, int newStock, OperationCallback callback) {
        productsRef.child(productId).child("stock").setValue(newStock)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // 10. UPDATE VARIANT STOCK
    public void updateVariantStock(String productId, String variantId, int newStock, OperationCallback callback) {
        productsRef.child(productId).child("variants").child(variantId).child("stock")
                .setValue(newStock)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // 11. GET FEATURED PRODUCTS (Top rated)
    public void getFeaturedProducts(int limit, ProductsCallback callback) {
        getAllProducts(new ProductsCallback() {
            @Override
            public void onSuccess(List<Product> allProducts) {
                // Sort by rating and sold count
                Collections.sort(allProducts, (p1, p2) -> {
                    int ratingCompare = Float.compare(p2.getAverageRating(), p1.getAverageRating());
                    if (ratingCompare != 0) return ratingCompare;
                    return Integer.compare(p2.getSoldCount(), p1.getSoldCount());
                });

                // Take top N products
                List<Product> featured = new ArrayList<>();
                for (int i = 0; i < Math.min(limit, allProducts.size()); i++) {
                    featured.add(allProducts.get(i));
                }
                callback.onSuccess(featured);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    // 12. GET NEW ARRIVALS
    public void getNewArrivals(int limit, ProductsCallback callback) {
        productsRef.orderByChild("creationTimestamp")
                .limitToLast(limit)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        List<Product> newArrivals = new ArrayList<>();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Product product = snapshot.getValue(Product.class);
                            if (product != null && product.isActive() && !product.isPreloaded()) {
                                product.setProductId(snapshot.getKey());
                                newArrivals.add(product);
                            }
                        }

                        // Reverse to get newest first
                        Collections.reverse(newArrivals);
                        callback.onSuccess(newArrivals);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        callback.onError(databaseError.getMessage());
                    }
                });
    }
}