package com.example.beautyhub.data;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.beautyhub.models.Product;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

    private List<Product> loadBaseProductsFromJson() {
        List<Product> products = new ArrayList<>();
        try {
            String json = loadJsonFromAssets("base_products.json");

            Type outerMapType = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> outerMap = gson.fromJson(json, outerMapType);

            Object productsObject = outerMap.get("Products");

            if (productsObject instanceof Map) {
                String productsJson = gson.toJson(productsObject);
                Type productMapType = new TypeToken<Map<String, Product>>(){}.getType();
                Map<String, Product> productMap = gson.fromJson(productsJson, productMapType);

                if (productMap != null) {
                    for (Map.Entry<String, Product> entry : productMap.entrySet()) {
                        Product product = entry.getValue();
                        product.setProductId(entry.getKey());
                        product.setPreloaded(true);

                        if (product.getSellerId() == null || product.getSellerId().isEmpty()) {
                            Log.w("ProductRepository", "Produk JSON '" + product.getName() + "' tidak mempunyai sellerId!");
                        }

                        product.setOfficialStore(true);
                        products.add(product);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("ProductRepository", "Gagal memuatkan produk dari base_products.json", e);
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

    public void getAllProducts(ProductsCallback callback) {
        List<Product> allProducts = new ArrayList<>();
        allProducts.addAll(loadBaseProductsFromJson());

        productsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
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
            public void onCancelled(@NonNull DatabaseError databaseError) {
                callback.onError(databaseError.getMessage());
            }
        });
    }

    public void getProductById(String productId, ProductCallback callback) {
        List<Product> baseProducts = loadBaseProductsFromJson();
        for (Product product : baseProducts) {
            if (productId.equals(product.getProductId())) {
                callback.onSuccess(product);
                return;
            }
        }

        productsRef.child(productId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Product product = dataSnapshot.getValue(Product.class);
                if (product != null) {
                    product.setProductId(dataSnapshot.getKey());
                    callback.onSuccess(product);
                } else {
                    callback.onError("Product not found");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                callback.onError(databaseError.getMessage());
            }
        });
    }

    // --- KOD YANG TELAH DIBETULKAN ---
    public void addSellerProduct(Product product, StringCallback callback) {
        DatabaseReference newProductRef = productsRef.push();
        String productId = newProductRef.getKey();

        if (productId == null) {
            callback.onError("Failed to create a new product ID.");
            return;
        }

        product.setProductId(productId);
        product.setPreloaded(false);
        product.setActive(true);

        if (product.getImageUrls() == null) {
            product.setImageUrls(new ArrayList<>());
        }

        // 1. Tukar objek Product kepada Map untuk memanipulasi data sebelum dihantar
        // Ini cara yang lebih selamat apabila berurusan dengan jenis data yang bercampur seperti ServerValue.TIMESTAMP
        Type type = new TypeToken<Map<String, Object>>() {}.getType();
        Map<String, Object> productValues = gson.fromJson(gson.toJson(product), type);

        // 2. Tambah nilai timestamp server ke dalam Map
        productValues.put("creationTimestamp", ServerValue.TIMESTAMP);

        // 3. Hantar Map yang telah dikemas kini ke Firebase
        newProductRef.setValue(productValues)
                .addOnSuccessListener(aVoid -> callback.onSuccess(productId))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void updateProduct(Product product, OperationCallback callback) {
        if (product.getProductId() == null || product.getProductId().isEmpty()) {
            callback.onError("Product ID is required");
            return;
        }

        if (product.isPreloaded()) {
            callback.onError("Cannot update base products");
            return;
        }

        productsRef.child(product.getProductId()).setValue(product)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void deleteProduct(String productId, OperationCallback callback) {
        if (productId.startsWith("base_")) {
            callback.onError("Cannot delete base products");
            return;
        }

        productsRef.child(productId).child("active").setValue(false)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getProductsBySeller(String sellerId, ProductsCallback callback) {
        if ("system".equals(sellerId)) {
            List<Product> baseProducts = loadBaseProductsFromJson();
            callback.onSuccess(baseProducts);
        } else {
            productsRef.orderByChild("sellerId").equalTo(sellerId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
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
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            callback.onError(databaseError.getMessage());
                        }
                    });
        }
    }

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

    public void searchProducts(String query, ProductsCallback callback) {
        getAllProducts(new ProductsCallback() {
            @Override
            public void onSuccess(List<Product> allProducts) {
                List<Product> results = new ArrayList<>();
                String searchQuery = query.toLowerCase();

                for (Product product : allProducts) {
                    boolean nameMatches = product.getName() != null && product.getName().toLowerCase().contains(searchQuery);
                    boolean descMatches = product.getDescription() != null && product.getDescription().toLowerCase().contains(searchQuery);
                    boolean brandMatches = product.getBrand() != null && product.getBrand().toLowerCase().contains(searchQuery);
                    boolean catMatches = product.getCategory() != null && product.getCategory().toLowerCase().contains(searchQuery);

                    if (nameMatches || descMatches || brandMatches || catMatches) {
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

    public void updateStock(String productId, int newStock, OperationCallback callback) {
        productsRef.child(productId).child("stock").setValue(newStock)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getFeaturedProducts(int limit, ProductsCallback callback) {
        getAllProducts(new ProductsCallback() {
            @Override
            public void onSuccess(List<Product> allProducts) {
                // This is a simple client-side sort. For large datasets, consider a server-side query.
                allProducts.sort((p1, p2) -> Integer.compare(p2.getSoldCount(), p1.getSoldCount()));
                callback.onSuccess(allProducts.subList(0, Math.min(limit, allProducts.size())));
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }
}
