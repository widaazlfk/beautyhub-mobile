package com.example.beautyhub.buyer;

import android.app.ProgressDialog;
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
import com.example.beautyhub.models.CartItem;
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
import java.util.HashMap;
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
    private ProgressDialog progressDialog;
    private String filterType;
    private String brandName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop_view);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Terima data dari Intent
        sellerId = getIntent().getStringExtra("SELLER_ID");
        brandName = getIntent().getStringExtra("BRAND_NAME");
        filterType = getIntent().getStringExtra("FILTER_TYPE");

        // Jika tiada sellerId DAN tiada brandName, baru tutup activity
        if (TextUtils.isEmpty(sellerId) && TextUtils.isEmpty(brandName)) {
            Toast.makeText(this, "No data to display.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        databaseReference = FirebaseDatabase.getInstance().getReference();
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);

        initViews();
        setupToolbar();

        // Logik paparan berdasarkan jenis filter
        if ("BRAND".equals(filterType)) {
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("Brand: " + brandName);
            findViewById(R.id.seller_info_card_layout).setVisibility(View.GONE);
            loadBrandProducts();
        } else {
            loadSellerData();
            loadSellerReviews();
            loadShopProducts();
        }
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

                    if (!TextUtils.isEmpty(seller.getAddress())) {
                        tvAddressValue.setText(seller.getAddress());
                    } else {
                        String city = seller.getCity() != null ? seller.getCity() : "";
                        String state = seller.getState() != null ? seller.getState() : "";
                        String location = (TextUtils.isEmpty(city) && TextUtils.isEmpty(state))
                                ? "Location not set"
                                : city + ", " + state;
                        tvAddressValue.setText(location);
                    }

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
        final double[] totalRating = {0.0};
        final long[] totalReviewCount = {0};

        databaseReference.child("Reviews").child(sellerId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot shopSnapshot) {
                totalRating[0] = 0.0;
                totalReviewCount[0] = 0;

                if (shopSnapshot.exists()) {
                    totalReviewCount[0] += shopSnapshot.getChildrenCount();
                    for (DataSnapshot ds : shopSnapshot.getChildren()) {
                        Double r = ds.child("rating").getValue(Double.class);
                        if (r != null) totalRating[0] += r;
                    }
                }

                databaseReference.child("Products").orderByChild("sellerId").equalTo(sellerId)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot productsSnapshot) {
                                final int totalProducts = (int) productsSnapshot.getChildrenCount();
                                if (totalProducts == 0) {
                                    updateUIWithRatings(totalRating[0], totalReviewCount[0]);
                                    return;
                                }

                                final int[] processedProducts = {0};

                                for (DataSnapshot productDs : productsSnapshot.getChildren()) {
                                    String productId = productDs.getKey();
                                    databaseReference.child("ProductReviews").child(productId)
                                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot productReviewSnapshot) {
                                                    if (productReviewSnapshot.exists()) {
                                                        totalReviewCount[0] += productReviewSnapshot.getChildrenCount();
                                                        for (DataSnapshot pr : productReviewSnapshot.getChildren()) {
                                                            Double r = pr.child("rating").getValue(Double.class);
                                                            if (r != null) totalRating[0] += r;
                                                        }
                                                    }
                                                    processedProducts[0]++;
                                                    if (processedProducts[0] == totalProducts) {
                                                        updateUIWithRatings(totalRating[0], totalReviewCount[0]);
                                                    }
                                                }
                                                @Override public void onCancelled(@NonNull DatabaseError error) {}
                                            });
                                }
                            }
                            @Override public void onCancelled(@NonNull DatabaseError error) {}
                        });
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadShopProducts() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        databaseReference.child("Products")
                .orderByChild("sellerId")
                .equalTo(sellerId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        processProductSnapshot(snapshot);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void loadBrandProducts() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        databaseReference.child("Products")
                .orderByChild("brand")
                .equalTo(brandName)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        processProductSnapshot(snapshot);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void processProductSnapshot(DataSnapshot snapshot) {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        productList.clear();
        for (DataSnapshot ds : snapshot.getChildren()) {
            Product product = ds.getValue(Product.class);
            if (product != null) {
                if (product.getProductId() == null) product.setProductId(ds.getKey());
                productList.add(product);
            }
        }
        if (productList.isEmpty()) {
            tvNoProducts.setVisibility(View.VISIBLE);
            rvShopProducts.setVisibility(View.GONE);
        } else {
            tvNoProducts.setVisibility(View.GONE);
            rvShopProducts.setVisibility(View.VISIBLE);
            productAdapter.notifyDataSetChanged();
        }
    }

    private void updateUIWithRatings(double totalRating, long count) {
        if (count > 0) {
            double average = totalRating / count;
            ratingBarSeller.setRating((float) average);
            tvRatingValue.setText(String.format(Locale.getDefault(), "%.1f (%d Reviews)", average, count));
        } else {
            ratingBarSeller.setRating(0f);
            tvRatingValue.setText("0.0 (0 Reviews)");
        }
    }

    @Override
    public void onProductClick(Product product) {
        Intent intent = new Intent(this, ProductDetailActivity.class);
        intent.putExtra("PRODUCT_ID", product.getProductId());
        startActivity(intent);
    }

    @Override
    public void onAddToCartClick(Product product) {
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }
        addToCartLogic(product, true, null);
    }

    @Override
    public void onBuyNowClick(Product product) {
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }
        progressDialog.setMessage("Preparing checkout...");
        progressDialog.show();
        addToCartLogic(product, false, () -> {
            progressDialog.dismiss();
            ArrayList<CartItem> list = new ArrayList<>();
            CartItem item = new CartItem();
            item.setProductId(product.getProductId());
            item.setQuantity(1);
            item.setSellerId(product.getSellerId());
            item.setPrice(product.getFinalPrice());
            item.setName(product.getName());
            list.add(item);

            Intent intent = new Intent(this, CheckoutActivity.class);
            intent.putParcelableArrayListExtra("CHECKOUT_ITEMS", list);
            startActivity(intent);
        });
    }

    private void addToCartLogic(Product product, boolean goToCart, OnCartUpdatedListener listener) {
        String uid = currentUser.getUid();
        DatabaseReference cartRef = databaseReference.child("Carts").child(uid)
                .child(product.getSellerId()).child(product.getProductId());

        cartRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int qty = 1;
                if (snapshot.exists() && snapshot.hasChild("quantity")) {
                    qty = snapshot.child("quantity").getValue(Integer.class) + 1;
                }
                HashMap<String, Object> map = new HashMap<>();
                map.put("productId", product.getProductId());
                map.put("quantity", qty);
                map.put("sellerId", product.getSellerId());

                cartRef.setValue(map).addOnSuccessListener(aVoid -> {
                    if (goToCart) startActivity(new Intent(ShopViewActivity.this, CartActivity.class));
                    else if (listener != null) listener.onSuccess();
                });
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public interface OnCartUpdatedListener { void onSuccess(); }

    @Override
    public void onWishlistClick(Product product) {
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(ShopViewActivity.this, "Removed from wishlist", Toast.LENGTH_SHORT).show());
                } else {
                    favRef.setValue(true).addOnSuccessListener(aVoid ->
                            Toast.makeText(ShopViewActivity.this, "Added to wishlist", Toast.LENGTH_SHORT).show());
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
    @Override public void onSellerClick(String sellerId) {}
    @Override public void onEditClick(Product product) {}
    @Override public void onDeleteClick(Product product) {}
}