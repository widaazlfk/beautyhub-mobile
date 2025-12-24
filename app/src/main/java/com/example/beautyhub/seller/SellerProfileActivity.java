package com.example.beautyhub.seller;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.beautyhub.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import de.hdodenhof.circleimageview.CircleImageView;

public class SellerProfileActivity extends AppCompatActivity {

    private static final String TAG = "SellerProfileActivity";

    // Views
    private CircleImageView ivSellerProfile;
    private TextView tvSellerName, tvRatingValue, tvStoreId, tvStoreDescription;
    private TextView tvTotalProductsCount, tvTotalSales, tvConversionRate;
    private TextView tvStoreEmail, tvStorePhone, tvStoreAddress, tvMemberSince;
    private TextView tvVerificationStatus, tvEditDescription;
    private RatingBar ratingBarSeller;
    private MaterialButton btnEditDetails;
    private MaterialToolbar toolbar;

    // Firebase
    private DatabaseReference sellersRef;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    // Seller Info
    private String sellerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_profile);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Please login as seller", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        sellerId = currentUser.getUid();

        initViews();
        setupToolbar();
        fetchSellerProfile();
        setupListeners();
    }

    private void initViews() {
        ivSellerProfile = findViewById(R.id.iv_seller_profile);
        tvSellerName = findViewById(R.id.tv_seller_name);
        tvRatingValue = findViewById(R.id.tv_rating_value);
        tvStoreId = findViewById(R.id.tv_store_id);
        tvStoreDescription = findViewById(R.id.tv_store_description);
        ratingBarSeller = findViewById(R.id.rating_bar_seller);

        // Stats
        tvTotalProductsCount = findViewById(R.id.tv_total_products_count);
        tvTotalSales = findViewById(R.id.tv_total_sales);
        tvConversionRate = findViewById(R.id.tv_conversion_rate);

        // Store details
        tvStoreEmail = findViewById(R.id.tv_store_email);
        tvStorePhone = findViewById(R.id.tv_store_phone);
        tvStoreAddress = findViewById(R.id.tv_store_address);
        tvMemberSince = findViewById(R.id.tv_member_since);

        // Verification
        tvVerificationStatus = findViewById(R.id.tv_verification_status);
        tvEditDescription = findViewById(R.id.tv_edit_description);

        // Buttons
        btnEditDetails = findViewById(R.id.btn_edit_details);
        toolbar = findViewById(R.id.toolbar_seller_profile);
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        toolbar.setTitle("My Store Profile");
    }

    private void fetchSellerProfile() {
        sellersRef = FirebaseDatabase.getInstance().getReference("Users").child(sellerId);
        sellersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Check if user is a seller
                    String userType = snapshot.child("userType").getValue(String.class);
                    if (!"Seller".equals(userType)) {
                        Toast.makeText(SellerProfileActivity.this, "User is not a seller", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    // Store Name - CHECK IN Users NODE
                    String storeName = snapshot.child("storeName").getValue(String.class);
                    if (storeName == null || storeName.isEmpty()) {
                        // Try username or display name
                        storeName = snapshot.child("username").getValue(String.class);
                        if (storeName == null || storeName.isEmpty()) {
                            storeName = snapshot.child("displayName").getValue(String.class);
                            if (storeName == null || storeName.isEmpty()) {
                                storeName = snapshot.child("email").getValue(String.class);
                                if (storeName != null) {
                                    // Extract username from email
                                    storeName = storeName.split("@")[0];
                                }
                            }
                        }
                    }

                    if (storeName != null && !storeName.isEmpty()) {
                        tvSellerName.setText(storeName);
                    } else {
                        tvSellerName.setText("My Beauty Store");
                    }

                    // Store ID
                    tvStoreId.setText("Store ID: " + sellerId.substring(0, Math.min(8, sellerId.length())).toUpperCase());

                    // Profile Image
                    String profileImage = snapshot.child("profileImage").getValue(String.class);
                    if (profileImage == null || profileImage.isEmpty()) {
                        profileImage = snapshot.child("profileImageUrl").getValue(String.class);
                        if (profileImage == null || profileImage.isEmpty()) {
                            profileImage = snapshot.child("imageUrl").getValue(String.class);
                        }
                    }

                    if (profileImage != null && !profileImage.isEmpty()) {
                        Glide.with(SellerProfileActivity.this)
                                .load(profileImage)
                                .placeholder(R.drawable.ic_profile)
                                .error(R.drawable.ic_profile)
                                .into(ivSellerProfile);
                    }

                    // Store Description - might be in separate node
                    String description = snapshot.child("storeDescription").getValue(String.class);
                    if (description == null || description.isEmpty()) {
                        description = snapshot.child("description").getValue(String.class);
                        if (description == null || description.isEmpty()) {
                            description = snapshot.child("bio").getValue(String.class);
                        }
                    }

                    if (description != null && !description.isEmpty()) {
                        tvStoreDescription.setText(description);
                    } else {
                        tvStoreDescription.setText("Add a description for your store to attract more customers!");
                    }

                    // Rating - might need to calculate from reviews
                    fetchSellerRating();

                    // Store Contact Details
                    String email = snapshot.child("email").getValue(String.class);
                    if (email == null || email.isEmpty()) {
                        email = currentUser.getEmail();
                    }

                    String phone = snapshot.child("phone").getValue(String.class);
                    if (phone == null || phone.isEmpty()) {
                        phone = snapshot.child("phoneNumber").getValue(String.class);
                        if (phone == null || phone.isEmpty()) {
                            phone = snapshot.child("contactNumber").getValue(String.class);
                        }
                    }

                    String address = snapshot.child("address").getValue(String.class);
                    if (address == null || address.isEmpty()) {
                        address = snapshot.child("location").getValue(String.class);
                        if (address == null || address.isEmpty()) {
                            address = snapshot.child("city").getValue(String.class);
                        }
                    }

                    String memberSince = snapshot.child("createdAt").getValue(String.class);
                    if (memberSince == null || memberSince.isEmpty()) {
                        memberSince = snapshot.child("joinDate").getValue(String.class);
                        if (memberSince == null || memberSince.isEmpty()) {
                            memberSince = snapshot.child("createdDate").getValue(String.class);
                            if (memberSince == null || memberSince.isEmpty()) {
                                memberSince = snapshot.child("timestamp").getValue(String.class);
                            }
                        }
                    }

                    if (email != null) tvStoreEmail.setText(email);
                    if (phone != null) tvStorePhone.setText(phone);
                    if (address != null) tvStoreAddress.setText(address);
                    if (memberSince != null) {
                        // Format date if needed
                        tvMemberSince.setText("Member since: " + memberSince);
                    } else {
                        tvMemberSince.setText("Member since: N/A");
                    }

                    // Store Stats
                    fetchSellerStats();

                    // Verification Status
                    Boolean isVerified = snapshot.child("isVerified").getValue(Boolean.class);
                    if (isVerified == null) {
                        isVerified = snapshot.child("verified").getValue(Boolean.class);
                    }

                    if (isVerified != null && isVerified) {
                        tvVerificationStatus.setText("VERIFIED");
                        tvVerificationStatus.setBackgroundResource(R.drawable.bg_verified_badge);
                    } else {
                        tvVerificationStatus.setText("PENDING");
                        tvVerificationStatus.setBackgroundResource(R.drawable.bg_pending_badge);
                    }
                } else {
                    // User not found in database
                    tvSellerName.setText("Welcome to BeautyHub!");
                    tvStoreDescription.setText("Complete your seller profile to start selling.");
                    tvVerificationStatus.setText("SETUP REQUIRED");

                    // Set default values
                    tvStoreId.setText("Store ID: " + sellerId.substring(0, Math.min(8, sellerId.length())).toUpperCase());
                    if (currentUser.getEmail() != null) {
                        tvStoreEmail.setText(currentUser.getEmail());
                    }
                    tvStorePhone.setText("Not set");
                    tvStoreAddress.setText("Not set");
                    tvMemberSince.setText("Member since: Not set");
                    tvTotalProductsCount.setText("0");
                    tvTotalSales.setText("0");
                    tvConversionRate.setText("0%");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to fetch seller profile: " + error.getMessage());
                Toast.makeText(SellerProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void fetchSellerStats() {
        // Fetch total products count
        DatabaseReference productsRef = FirebaseDatabase.getInstance().getReference("Products");
        productsRef.orderByChild("sellerId").equalTo(sellerId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        long productCount = snapshot.getChildrenCount();
                        tvTotalProductsCount.setText(String.valueOf(productCount));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to fetch product count: " + error.getMessage());
                        tvTotalProductsCount.setText("0");
                    }
                });

        // Fetch total sales and conversion rate (you need to implement based on your orders structure)
        DatabaseReference ordersRef = FirebaseDatabase.getInstance().getReference("Orders");
        ordersRef.orderByChild("sellerId").equalTo(sellerId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        long totalOrders = snapshot.getChildrenCount();
                        double totalRevenue = 0;

                        for (DataSnapshot orderSnap : snapshot.getChildren()) {
                            Double amount = orderSnap.child("totalAmount").getValue(Double.class);
                            if (amount != null) {
                                totalRevenue += amount;
                            }
                        }

                        tvTotalSales.setText(formatCount((long) totalRevenue));

                        // Simple conversion rate calculation (adjust based on your business logic)
                        // This is just an example - you might want to calculate differently
                        double conversionRate = totalOrders > 0 ? 4.2 : 0.0; // Example fixed rate
                        tvConversionRate.setText(String.format("%.1f%%", conversionRate));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to fetch sales data: " + error.getMessage());
                        tvTotalSales.setText("0");
                        tvConversionRate.setText("0%");
                    }
                });
    }
    private void fetchSellerRating() {
        // Calculate rating from reviews
        DatabaseReference reviewsRef = FirebaseDatabase.getInstance().getReference("Reviews");
        reviewsRef.orderByChild("sellerId").equalTo(sellerId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        long reviewCount = snapshot.getChildrenCount();
                        double totalRating = 0;

                        for (DataSnapshot reviewSnap : snapshot.getChildren()) {
                            Double rating = reviewSnap.child("rating").getValue(Double.class);
                            if (rating != null) {
                                totalRating += rating;
                            }
                        }

                        double averageRating = reviewCount > 0 ? totalRating / reviewCount : 0;

                        ratingBarSeller.setRating((float) averageRating);
                        tvRatingValue.setText(String.format("%.1f (%d reviews)", averageRating, reviewCount));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to fetch seller rating: " + error.getMessage());
                        ratingBarSeller.setRating(0);
                        tvRatingValue.setText("No ratings yet");
                    }
                });
    }
    private void setupListeners() {
        btnEditDetails.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditSellerProfileActivity.class);
            startActivity(intent);
        });

        tvEditDescription.setOnClickListener(v -> {
            // Open edit description dialog
            // showEditDescriptionDialog();
            Toast.makeText(this, "Edit description", Toast.LENGTH_SHORT).show();
        });

        ivSellerProfile.setOnClickListener(v -> {
            // Change profile picture
            // openImagePicker();
            Toast.makeText(this, "Change profile picture", Toast.LENGTH_SHORT).show();
        });
    }

    private String formatCount(long count) {
        if (count < 1000) {
            return String.valueOf(count);
        } else if (count < 1000000) {
            return String.format("%.1fK", count / 1000.0);
        } else {
            return String.format("%.1fM", count / 1000000.0);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh profile data when returning from edit screens
        fetchSellerProfile();
    }
}