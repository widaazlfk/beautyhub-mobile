package com.example.beautyhub.buyer;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.beautyhub.R;
import com.example.beautyhub.adapters.ProductAdapter;
import com.example.beautyhub.auth.LoginActivity;
import com.example.beautyhub.models.Product;
import com.example.beautyhub.models.User;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class ShopViewActivity extends AppCompatActivity implements ProductAdapter.OnProductClickListener {

    private String sellerId;
    private FirebaseUser currentUser;

    private CircleImageView ivShopProfile;
    private TextView tvShopNameHeader, tvRatingValue, tvStoreDescValue, tvPhoneValue, tvAddressValue, tvNoProducts;
    private RatingBar ratingBarSeller;
    private RecyclerView rvShopProducts;
    private ProgressBar progressBar;

    private ProductAdapter productAdapter;
    private List<Product> productList;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop_view);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        sellerId = getIntent().getStringExtra("SELLER_ID");
        if (sellerId == null || sellerId.isEmpty()) {
            Toast.makeText(this, "Seller ID not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        databaseReference = FirebaseDatabase.getInstance().getReference();

        initViews();
        setupToolbar();
        loadSellerData();
        loadSellerReviews();
        loadShopProducts();
    }

    private void initViews() {
        ivShopProfile = findViewById(R.id.shop_profile_image);
        tvShopNameHeader = findViewById(R.id.shop_name);
        ratingBarSeller = findViewById(R.id.rating_bar_seller);
        tvRatingValue = findViewById(R.id.tv_rating_value);

        tvStoreDescValue = findViewById(R.id.tv_store_description);
        tvPhoneValue = findViewById(R.id.tv_phone_value);
        tvAddressValue = findViewById(R.id.tv_shop_location);

        tvNoProducts = findViewById(R.id.tv_no_products);
        rvShopProducts = findViewById(R.id.rv_shop_products);
        progressBar = findViewById(R.id.progress_bar);

        rvShopProducts.setLayoutManager(new GridLayoutManager(this, 2));
        rvShopProducts.setNestedScrollingEnabled(false);
        productList = new ArrayList<>();

        // isSellerView = false: buyer mode
        productAdapter = new ProductAdapter(this, productList, false, this);
        rvShopProducts.setAdapter(productAdapter);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Shop Profile");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void loadSellerData() {
        databaseReference.child("Users").child(sellerId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User seller = snapshot.getValue(User.class);
                if (seller != null) {
                    String displayName = !TextUtils.isEmpty(seller.getSellerName()) ? seller.getSellerName() : seller.getUsername();
                    tvShopNameHeader.setText(displayName);
                    tvStoreDescValue.setText(TextUtils.isEmpty(seller.getShopDescription()) ? "No description provided" : seller.getShopDescription());

                    String city = seller.getCity() != null ? seller.getCity() : "";
                    String state = seller.getState() != null ? seller.getState() : "";
                    tvAddressValue.setText(TextUtils.isEmpty(city) ? "Location not set" : city + ", " + state);
                    tvPhoneValue.setText(TextUtils.isEmpty(seller.getPhone()) ? "No contact provided" : seller.getPhone());

                    if (!isFinishing() && !isDestroyed() && seller.getProfileImage() != null && !seller.getProfileImage().isEmpty()) {
                        Glide.with(ShopViewActivity.this)
                                .load(seller.getProfileImage())
                                .placeholder(R.drawable.ic_profile)
                                .into(ivShopProfile);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadSellerReviews() {
        databaseReference.child("Reviews").child(sellerId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                double totalRating = 0.0;
                long count = snapshot.getChildrenCount();
                if (count > 0) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        Double r = ds.child("rating").getValue(Double.class);
                        if (r != null) totalRating += r;
                    }
                    double average = totalRating / count;
                    ratingBarSeller.setRating((float) average);
                    tvRatingValue.setText(String.format(Locale.getDefault(), "%.1f (%d Reviews)", average, count));
                } else {
                    ratingBarSeller.setRating(0f);
                    tvRatingValue.setText("0.0 (0 Reviews)");
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadShopProducts() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        databaseReference.child("Products").orderByChild("sellerId").equalTo(sellerId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        productList.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Product product = ds.getValue(Product.class);
                            if (product != null) {
                                // CRITICAL: Map the Firebase key to productId if missing
                                if (product.getProductId() == null) {
                                    product.setProductId(ds.getKey());
                                }
                                productList.add(product);
                            }
                        } // Closed loop properly here

                        if (productList.isEmpty()) {
                            tvNoProducts.setVisibility(View.VISIBLE);
                            rvShopProducts.setVisibility(View.GONE);
                        } else {
                            tvNoProducts.setVisibility(View.GONE);
                            rvShopProducts.setVisibility(View.VISIBLE);
                            productAdapter.notifyDataSetChanged();
                        }
                    }

                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                    }
                });
    }

    @Override
    public void onWishlistClick(Product product) {
        // Method name from interface is onWishlistClick, but we use Favourite logic
        toggleFavourite(product);
    }

    private void toggleFavourite(Product product) {
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to manage your favourites", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }

        if (product == null || product.getProductId() == null) {
            Toast.makeText(this, "Error: Product ID missing", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference favRef = FirebaseDatabase.getInstance().getReference("Favourites")
                .child(currentUser.getUid())
                .child(product.getProductId());

        favRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    favRef.removeValue().addOnSuccessListener(aVoid ->
                            Toast.makeText(ShopViewActivity.this, "Removed from favourites", Toast.LENGTH_SHORT).show());
                } else {
                    favRef.setValue(true).addOnSuccessListener(aVoid ->
                            Toast.makeText(ShopViewActivity.this, "Added to favourites", Toast.LENGTH_SHORT).show());
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @Override
    public void onProductClick(Product product) {
        Intent intent = new Intent(this, ProductDetailActivity.class);
        intent.putExtra("productId", product.getProductId());
        startActivity(intent);
    }

    @Override
    public void onAddToCartClick(Product product) {
        Toast.makeText(this, product.getName() + " added to cart", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBuyNowClick(Product product) {
        Toast.makeText(this, "Proceeding to buy " + product.getName(), Toast.LENGTH_SHORT).show();
    }

    @Override public void onSellerClick(String sellerId) {}
    @Override public void onEditClick(Product product) {}
    @Override public void onDeleteClick(Product product) {}
}
