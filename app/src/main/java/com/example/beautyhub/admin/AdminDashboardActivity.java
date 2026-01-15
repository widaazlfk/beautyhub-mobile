package com.example.beautyhub.admin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;

import com.bumptech.glide.Glide;
import com.example.beautyhub.R;
import com.example.beautyhub.auth.LoginActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class AdminDashboardActivity extends AppCompatActivity {

    private static final String TAG = "AdminDashboardActivity";
    private FirebaseAuth mAuth;
    private DatabaseReference usersRef, productsRef, ordersRef;

    private ValueEventListener usersListener, productsListener, ordersListener, profileListener;

    private TextView tvTotalUsers, tvTotalSellers, tvTotalProducts, tvTotalRevenue;
    private CircleImageView ivAdminProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_activity_admin);

        mAuth = FirebaseAuth.getInstance();

        FirebaseDatabase db = FirebaseDatabase.getInstance();
        usersRef = db.getReference("Users");
        productsRef = db.getReference("Products");
        ordersRef = db.getReference("Orders");

        initViews();
        setupToolbar();
        setupListeners();
    }

    private void initViews() {
        tvTotalUsers = findViewById(R.id.tv_total_users);
        tvTotalSellers = findViewById(R.id.tv_total_sellers);
        // ID ini tetap tv_total_products tetapi di XML anda labelnya adalah "Market Performance"
        tvTotalProducts = findViewById(R.id.tv_total_products);
        tvTotalRevenue = findViewById(R.id.tv_total_revenue);
        ivAdminProfile = findViewById(R.id.iv_admin_profile);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_admin_dashboard);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Admin Overview");
        }
    }

    private void setupListeners() {
        ivAdminProfile.setOnClickListener(this::showProfileMenu);

        // Navigation ke statistik
        bindClickListener(R.id.card_total_users, v -> startActivity(new Intent(this, UsersStatisticsActivity.class)));
        bindClickListener(R.id.card_total_sellers, v -> startActivity(new Intent(this, SellersStatisticsActivity.class)));

        // Klik Market Performance membawa ke Products Statistics
        bindClickListener(R.id.card_total_products, v -> startActivity(new Intent(this, ProductsStatisticsActivity.class)));

        bindClickListener(R.id.card_total_revenue, v -> startActivity(new Intent(this, RevenueStatisticsActivity.class)));

        // Quick Actions
        bindClickListener(R.id.card_manage_users, v -> startActivity(new Intent(this, ManageUsersActivity.class)));
        bindClickListener(R.id.card_manage_products, v -> startActivity(new Intent(this, ManageProductsActivity.class)));
        bindClickListener(R.id.card_manage_reports, v -> startActivity(new Intent(this, AdminManageReportsActivity.class)));
        bindClickListener(R.id.card_manage_categories, v -> startActivity(new Intent(this, ManageCategoriesActivity.class)));
        bindClickListener(R.id.card_activity_logs, v -> startActivity(new Intent(this, AdminLogActivity.class)));
    }

    private void showProfileMenu(View v) {
        PopupMenu popupMenu = new PopupMenu(this, v);
        popupMenu.getMenu().add(0, 1, 0, "My Profile");
        popupMenu.getMenu().add(0, 2, 1, "Logout");

        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                startActivity(new Intent(this, AdminProfileActivity.class));
                return true;
            } else if (item.getItemId() == 2) {
                showLogoutDialog();
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    private void loadPlatformStatistics() {
        // 1. STATISTIK: TOTAL USERS & SELLERS
        usersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long sellersCount = 0;
                long totalUsers = snapshot.getChildrenCount();
                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    String role = userSnap.child("ROLE").getValue(String.class);
                    if (role == null) role = userSnap.child("userType").getValue(String.class);
                    if ("SELLER".equalsIgnoreCase(role)) sellersCount++;
                }
                tvTotalUsers.setText(String.format(Locale.US, "%,d", totalUsers));
                tvTotalSellers.setText(String.format(Locale.US, "%,d", sellersCount));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        usersRef.addValueEventListener(usersListener);

        // 2. STATISTIK: MARKET PERFORMANCE (Fetch Nama Produk Best Seller)
        productsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ordersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot orderSnapshot) {
                        Map<String, Integer> productCounts = new HashMap<>();

                        for (DataSnapshot orderSnap : orderSnapshot.getChildren()) {
                            String status = orderSnap.child("status").getValue(String.class);

                            // Logik: Ikut ProductsStatisticsActivity (Hanya kira jika bukan Cancelled)
                            if (!"Cancelled".equalsIgnoreCase(status)) {
                                // Masuk ke dalam node orderItems
                                DataSnapshot itemsSnapshot = orderSnap.child("orderItems");
                                for (DataSnapshot itemSnapshot : itemsSnapshot.getChildren()) {
                                    String productName = itemSnapshot.child("productName").getValue(String.class);
                                    if (productName != null) {
                                        productCounts.put(productName, productCounts.getOrDefault(productName, 0) + 1);
                                    }
                                }
                            }
                        }

                        if (!productCounts.isEmpty()) {
                            // Cari produk yang paling banyak muncul dalam list
                            String bestSeller = "No Sales Yet";
                            int maxSales = 0;
                            for (Map.Entry<String, Integer> entry : productCounts.entrySet()) {
                                if (entry.getValue() > maxSales) {
                                    maxSales = entry.getValue();
                                    bestSeller = entry.getKey();
                                }
                            }
                            // Set nama produk paling laku (Contoh: "Anas Lip Moist")
                            tvTotalProducts.setText(bestSeller);
                        } else {
                            tvTotalProducts.setText("No Sales Yet");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        productsRef.addValueEventListener(productsListener);

        // 3. STATISTIK: TOTAL REVENUE (10% Admin Profit)
        ordersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                double totalAdminProfit = 0.0;
                for (DataSnapshot orderSnap : snapshot.getChildren()) {
                    String status = orderSnap.child("status").getValue(String.class);
                    if ("Completed".equalsIgnoreCase(status)) {
                        Object amtObj = orderSnap.child("totalAmount").getValue();
                        double totalAmount = 0.0;
                        if (amtObj instanceof Double) totalAmount = (Double) amtObj;
                        else if (amtObj instanceof Long) totalAmount = ((Long) amtObj).doubleValue();
                        totalAdminProfit += (totalAmount * 0.10);
                    }
                }
                NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("ms", "MY"));
                tvTotalRevenue.setText(currencyFormat.format(totalAdminProfit));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        ordersRef.addValueEventListener(ordersListener);
    }
    private void loadAdminProfileImage() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        profileListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String imageUrl = snapshot.child("profileImage").getValue(String.class);
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Glide.with(AdminDashboardActivity.this)
                                .load(imageUrl)
                                .placeholder(R.drawable.ic_admin)
                                .error(R.drawable.ic_admin)
                                .into(ivAdminProfile);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Profile Image Error: " + error.getMessage());
            }
        };
        usersRef.child(user.getUid()).addValueEventListener(profileListener);
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes", (dialog, which) -> logoutAdmin())
                .setNegativeButton("No", null)
                .show();
    }

    private void logoutAdmin() {
        mAuth.signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void bindClickListener(int id, View.OnClickListener listener) {
        View view = findViewById(id);
        if (view != null) view.setOnClickListener(listener);
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadPlatformStatistics();
        loadAdminProfileImage();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (usersRef != null && usersListener != null) usersRef.removeEventListener(usersListener);
        if (productsRef != null && productsListener != null) productsRef.removeEventListener(productsListener);
        if (ordersRef != null && ordersListener != null) ordersRef.removeEventListener(ordersListener);
        if (usersRef != null && profileListener != null) usersRef.removeEventListener(profileListener);
    }
}