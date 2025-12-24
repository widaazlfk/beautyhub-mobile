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
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beautyhub.R;
import com.example.beautyhub.adapters.BuyerProductAdapter;
import com.example.beautyhub.models.CartItem;
import com.example.beautyhub.models.Product;
import com.example.beautyhub.models.Variant;
import com.example.beautyhub.seller.SellerProfileActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
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

    // --- NO CONSTRUCTOR SHOULD BE HERE ---

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
                // Navigate to a main shopping activity, for example, HomeActivity or ShopActivity
                Intent intent = new Intent(FavouriteActivity.this, BuyerActivity.class); // Corrected to BuyerActivity
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish(); // Finish this activity
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
                    onAllProductsLoaded(); // Use a single method to update UI state
                } else {
                    Collections.reverse(favouriteProductIds); // Show newest first
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
        productAdapter.notifyDataSetChanged();
        showEmptyState(favouriteProductList.isEmpty());
    }

    private void setLoadingState(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        favouriteRecyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        if(isLoading) {
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

    @Override
    public void onBuyNowClick(Product product, Variant variant) {
        if (product == null || product.getProductId() == null) {
            Toast.makeText(this, "Product details not available.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (product.hasVariants() && variant == null) {
            // If it has variants but none is selected, prompt user to select one
            Toast.makeText(this, "Please select a variant.", Toast.LENGTH_SHORT).show();
            onProductClick(product); // Go to detail page to select variant
        } else {
            if (!product.hasStock() && (variant == null || variant.getStock() <= 0)) {
                Toast.makeText(this, "Sorry, this product is out of stock.", Toast.LENGTH_SHORT).show();
                return;
            }

            ArrayList<CartItem> itemsForCheckout = new ArrayList<>();
            CartItem item;

            if(variant != null) {
                item = new CartItem(
                        product.getProductId(),
                        product.getName(),
                        variant.getPriceModifier(),
                        1,
                        (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) ? product.getImageUrls().get(0) : "",
                        variant.getId(),
                        variant.getName()
                );
            } else {
                item = new CartItem(
                        product.getProductId(),
                        product.getName(),
                        product.getFinalPrice(),
                        1,
                        (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) ? product.getImageUrls().get(0) : ""
                );
            }

            String sellerId = product.isPreloaded() ? "system" : product.getSellerId();
            String sellerName = product.isPreloaded() ? "BeautyHub Official" : product.getSellerName();
            item.setSellerId(sellerId);
            item.setSellerName(sellerName);
            item.setFromJson(product.isPreloaded());

            itemsForCheckout.add(item);

            Intent intent = new Intent(this, CheckoutActivity.class);
            intent.putParcelableArrayListExtra("CHECKOUT_ITEMS", itemsForCheckout);
            intent.putExtra("SOURCE", "BUY_NOW");
            startActivity(intent);
        }
    }

    @Override
    public void onAddToCartClick(Product product, Variant variant) {
        Toast.makeText(this, "Add to cart clicked: " + product.getName(), Toast.LENGTH_SHORT).show();
        // Implement Add to Cart logic here if needed
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
            // This is the important part for this activity
            favRef.removeValue()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(FavouriteActivity.this, "Removed from Favourites", Toast.LENGTH_SHORT).show();
                        // The ValueEventListener will automatically detect this change and update the list.
                        // Manually removing from the list here can cause issues.
                        // Let the listener handle the UI refresh.
                    })
                    .addOnFailureListener(e -> Toast.makeText(FavouriteActivity.this, "Failed to remove favourite", Toast.LENGTH_SHORT).show());
        }
    }

    // ▼▼▼ THIS IS THE MISSING METHOD ▼▼▼
    @Override
    public void onSellerClick(String sellerId) {
        Toast.makeText(this, "Seller clicked: " + sellerId, Toast.LENGTH_SHORT).show();
        // You can implement navigation to a seller's profile page here if you want
        Intent intent = new Intent(this, SellerProfileActivity.class);
        intent.putExtra("SELLER_ID", sellerId);
        startActivity(intent);
    }
}
