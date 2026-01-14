package com.example.beautyhub.buyer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beautyhub.R;
import com.example.beautyhub.adapters.BuyerProductAdapter;
import com.example.beautyhub.auth.LoginActivity;
import com.example.beautyhub.models.CartItem;
import com.example.beautyhub.models.Product;
// --- PERUBAHAN 1: Padam import Variant ---
// import com.example.beautyhub.models.Variant;
import com.example.beautyhub.seller.SellerProfileActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class FavouriteActivity extends AppCompatActivity implements BuyerProductAdapter.OnProductInteractionListener {

    private static final String TAG = "FavouriteActivity";

    // Views
    private RecyclerView favouriteRecyclerView;
    private ProgressBar progressBar;
    private LinearLayout noFavouritesLayout;
    private Button btnShopNow;

    // Adapter dan Data
    private BuyerProductAdapter productAdapter;
    private List<Product> favouriteProductList;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference favouritesRef;
    private ValueEventListener favouritesListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favourite);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupEmptyStateButton();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (currentUser != null) {
            listenToFavouriteProductIds(currentUser.getUid());
        } else {
            Toast.makeText(this, "Please log in to see your favourites.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (favouritesRef != null && favouritesListener != null) {
            favouritesRef.removeEventListener(favouritesListener);
        }
    }

    private void initViews() {
        favouriteRecyclerView = findViewById(R.id.rv_favourite_products);
        progressBar = findViewById(R.id.progress_bar_favourite);
        noFavouritesLayout = findViewById(R.id.layout_no_favourites);
        btnShopNow = findViewById(R.id.btn_shop_now);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_favourite);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        favouriteProductList = new ArrayList<>();
        productAdapter = new BuyerProductAdapter(this, favouriteProductList, this);
        favouriteRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        favouriteRecyclerView.setAdapter(productAdapter);
    }

    private void setupEmptyStateButton() {
        if (btnShopNow != null) {
            btnShopNow.setOnClickListener(v -> {
                Intent intent = new Intent(FavouriteActivity.this, BuyerActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            });
        }
    }

    private void listenToFavouriteProductIds(String userId) {
        setLoadingState(true);
        favouritesRef = FirebaseDatabase.getInstance().getReference("Favourites").child(userId);

        if (favouritesListener != null) {
            favouritesRef.removeEventListener(favouritesListener);
        }

        favouritesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> favouriteProductIds = new ArrayList<>();
                if (snapshot.exists()) {
                    for (DataSnapshot idSnapshot : snapshot.getChildren()) {
                        // Struktur data dipermudahkan: kini hanya simpan ID sebagai key dengan nilai true
                        String productId = idSnapshot.getKey();
                        Boolean isFavourite = idSnapshot.getValue(Boolean.class);
                        if (productId != null && Boolean.TRUE.equals(isFavourite)) {
                            favouriteProductIds.add(productId);
                        }
                    }
                }

                productAdapter.setFavouriteProductIds(favouriteProductIds);

                if (favouriteProductIds.isEmpty()) {
                    favouriteProductList.clear();
                    onAllProductsLoaded();
                } else {
                    Collections.reverse(favouriteProductIds);
                    loadFavouriteProducts(favouriteProductIds);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                setLoadingState(false);
                showEmptyState(true);
                Log.e(TAG, "Failed to load favourites list: " + error.getMessage());
                Toast.makeText(FavouriteActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
        favouritesRef.addValueEventListener(favouritesListener);
    }

    private void loadFavouriteProducts(List<String> productIds) {
        favouriteProductList.clear();
        DatabaseReference productsRef = FirebaseDatabase.getInstance().getReference("Products");
        AtomicInteger productsToLoadCounter = new AtomicInteger(productIds.size());

        if (productIds.isEmpty()) {
            onAllProductsLoaded();
            return;
        }

        for (String productId : productIds) {
            productsRef.child(productId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        Product product = snapshot.getValue(Product.class);
                        if (product != null && product.isActive()) {
                            product.setProductId(snapshot.getKey());
                            favouriteProductList.add(product);
                        }
                    }
                    if (productsToLoadCounter.decrementAndGet() == 0) {
                        onAllProductsLoaded();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.w(TAG, "Failed to load product details for " + productId, error.toException());
                    if (productsToLoadCounter.decrementAndGet() == 0) {
                        onAllProductsLoaded();
                    }
                }
            });
        }
    }

    private void onAllProductsLoaded() {
        setLoadingState(false);
        // Guna notifyDataSetChanged() untuk kesederhanaan, DiffUtil boleh ditambah kemudian jika perlu
        productAdapter.notifyDataSetChanged();
        showEmptyState(favouriteProductList.isEmpty());
    }

    private void setLoadingState(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        favouriteRecyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        if (isLoading) {
            noFavouritesLayout.setVisibility(View.GONE);
        }
    }

    private void showEmptyState(boolean show) {
        favouriteRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        noFavouritesLayout.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    // --- IMPLEMENTATION OF INTERFACE METHODS ---

    @Override
    public void onProductClick(Product product) {
        Intent intent = new Intent(this, ProductDetailActivity.class);
        intent.putExtra("PRODUCT_ID", product.getProductId());
        startActivity(intent);
    }

    // --- PERUBAHAN 2: Permudahkan kaedah onBuyNowClick ---
    @Override
    public void onBuyNowClick(Product product) {
        if (product == null || product.getProductId() == null) {
            Toast.makeText(this, "Product details not available.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Semak stok produk utama
        if (!product.hasStock()) {
            Toast.makeText(this, "Sorry, this product is out of stock.", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<CartItem> itemsForCheckout = new ArrayList<>();
        String imageUrl = (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) ? product.getImageUrls().get(0) : "";

        // Cipta CartItem tanpa sebarang rujukan varian
        CartItem item = new CartItem(
                product.getProductId(),
                product.getName(),
                product.getFinalPrice(),
                1,
                imageUrl,
                product.getSellerProfileImageUrl()
        );

        // Tetapkan maklumat penjual
        item.setSellerId(product.getSellerId());
        item.setSellerName(product.getSellerName());
        item.setFromJson(product.isPreloaded());

        itemsForCheckout.add(item);

        Intent intent = new Intent(this, CheckoutActivity.class);
        intent.putParcelableArrayListExtra("CHECKOUT_ITEMS", itemsForCheckout);
        intent.putExtra("SOURCE", "BUY_NOW");
        startActivity(intent);
    }

    // --- PERUBAHAN 3: Permudahkan kaedah onAddToCartClick ---
    @Override
    public void onAddToCartClick(Product product) {
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to add items to your cart", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }

        // Semak stok produk utama
        if (!product.hasStock()) {
            Toast.makeText(this, "Product is out of stock", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();

        // --- PEMBETULAN UTAMA DI SINI ---
        // Terus dapatkan sellerId dan pastikan ia wujud. Tiada lagi logik gantian.
        String sellerId = product.getSellerId();

        if (sellerId == null || sellerId.isEmpty()) {
            Toast.makeText(this, "Cannot add item: Seller information is missing.", Toast.LENGTH_LONG).show();
            Log.e("BuyerActivity", "Attempted to add product with missing sellerId: " + product.getProductId());
            return;
        }
        // --- AKHIR PEMBETULAN UTAMA ---

        // Rujukan ke troli seller kini menggunakan sellerId yang sah
        DatabaseReference sellerCartRef = FirebaseDatabase.getInstance()
                .getReference("Carts")
                .child(userId)
                .child(sellerId); // <-- Guna sellerId yang betul dan sah

        // ID item dalam troli adalah sama dengan ID produk
        String cartItemId = product.getProductId();
        DatabaseReference cartItemRef = sellerCartRef.child(cartItemId);

        // Guna Transaction untuk mengendalikan penambahan kuantiti dengan selamat
        cartItemRef.runTransaction(new com.google.firebase.database.Transaction.Handler() {
            @NonNull
            @Override
            public com.google.firebase.database.Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                CartItem currentItem = mutableData.getValue(CartItem.class);

                if (currentItem == null) {
                    // Item belum wujud, cipta item baru
                    String imageUrl = (product.getImageUrls() != null && !product.getImageUrls().isEmpty())
                            ? product.getImageUrls().get(0) : null;

                    CartItem newItem = new CartItem();
                    newItem.setProductId(product.getProductId());
                    newItem.setName(product.getName());
                    newItem.setPrice(product.getFinalPrice());
                    newItem.setQuantity(1);
                    newItem.setImageUrls(imageUrl);
                    newItem.setSelected(true); // Pilih secara lalai apabila ditambah
                    newItem.setSellerId(sellerId);
                    newItem.setSellerName(product.getSellerName());
                    newItem.setSellerProfileImageUrl(product.getSellerProfileImageUrl());

                    mutableData.setValue(newItem);
                } else {
                    // Item sudah ada, hanya tambah kuantiti
                    int newQuantity = currentItem.getQuantity() + 1;

                    // Semak semula stok sebelum mengemas kini
                    if (newQuantity > product.getStock()) {
                        // Jangan teruskan transaksi jika melebihi stok
                        // Mesej Toast akan dipaparkan dalam onComplete
                        return com.google.firebase.database.Transaction.abort();
                    }

                    currentItem.setQuantity(newQuantity);
                    mutableData.setValue(currentItem);
                }
                return com.google.firebase.database.Transaction.success(mutableData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                if (error != null) {
                    Toast.makeText(FavouriteActivity.this, "Failed to add to cart: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                } else if (committed) {
                    Toast.makeText(FavouriteActivity.this, "Item added to cart", Toast.LENGTH_SHORT).show();
                } else {
                    // Transaksi dibatalkan (kemungkinan besar kerana melebihi stok)
                    Toast.makeText(FavouriteActivity.this, "Maximum quantity in cart reached", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    @Override
    public void onFavouriteClick(Product product, boolean isFavourite) {
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to manage favourites.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (product == null || product.getProductId() == null) {
            Toast.makeText(this, "Invalid product.", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference favRef = FirebaseDatabase.getInstance()
                .getReference("Favourites")
                .child(currentUser.getUid())
                .child(product.getProductId());

        if (isFavourite) {
            favRef.setValue(true)
                    .addOnSuccessListener(aVoid -> Toast.makeText(FavouriteActivity.this, "Added to Favourites", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(FavouriteActivity.this, "Failed to add favourite", Toast.LENGTH_SHORT).show());
        } else {
            favRef.removeValue()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(FavouriteActivity.this, "Removed from Favourites", Toast.LENGTH_SHORT).show();
                        // ValueEventListener akan mengemas kini senarai secara automatik
                    })
                    .addOnFailureListener(e -> Toast.makeText(FavouriteActivity.this, "Failed to remove favourite", Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public void onSellerClick(String sellerId) {
        if (sellerId == null || sellerId.isEmpty() || sellerId.startsWith("json_")) {
            Toast.makeText(this, "This is an official store, no separate seller page.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, SellerProfileActivity.class);
        intent.putExtra("SELLER_ID", sellerId);
        startActivity(intent);
    }
}
