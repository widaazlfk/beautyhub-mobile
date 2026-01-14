package com.example.beautyhub.admin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

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

import de.hdodenhof.circleimageview.CircleImageView;

public class AdminDashboardActivity extends AppCompatActivity {

    private static final String TAG = "AdminDashboardActivity";
    private FirebaseAuth mAuth;
    private DatabaseReference usersRef, productsRef, ordersRef;

    // Listeners disimpan sebagai pembolehubah supaya boleh dibuang (remove) bila aktiviti berhenti
    private ValueEventListener usersListener, productsListener, ordersListener, profileListener;

    private TextView tvTotalUsers, tvTotalSellers, tvTotalProducts, tvTotalRevenue;
    private CircleImageView ivAdminProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_activity_admin);

        mAuth = FirebaseAuth.getInstance();

        // Inisialisasi Rujukan Database
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
        // --- Profile Menu ---
        ivAdminProfile.setOnClickListener(this::showProfileMenu);

        // --- Statistik Navigation ---
        bindClickListener(R.id.card_total_users, v -> startActivity(new Intent(this, UsersStatisticsActivity.class)));
        bindClickListener(R.id.card_total_sellers, v -> startActivity(new Intent(this, SellersStatisticsActivity.class)));
        bindClickListener(R.id.card_total_products, v -> startActivity(new Intent(this, ProductsStatisticsActivity.class)));
        bindClickListener(R.id.card_total_revenue, v -> startActivity(new Intent(this, RevenueStatisticsActivity.class)));

        // --- Quick Actions ---
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

    /**
     * Mengambil data statistik masa-nyata dari Firebase.
     */
    /**
     * Mengambil data statistik masa-nyata dari Firebase.
     */
    private void loadPlatformStatistics() {
        // 1. STATISTIK: TOTAL USERS & ACTIVE SELLERS
        usersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long sellersCount = 0;
                long totalUsers = snapshot.getChildrenCount(); // Mengira semua nod di bawah "Users"

                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    // Pastikan nama child "userType" sepadan dengan yang ada di Firebase anda
                    String userType = userSnap.child("userType").getValue(String.class);
                    if ("Seller".equalsIgnoreCase(userType)) {
                        sellersCount++;
                    }
                }

                // Update UI dengan format ribuan (Contoh: 1,200)
                tvTotalUsers.setText(String.format(Locale.US, "%,d", totalUsers));
                tvTotalSellers.setText(String.format(Locale.US, "%,d", sellersCount));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Users Stats Error: " + error.getMessage());
            }
        };
        usersRef.addValueEventListener(usersListener);

        // 2. STATISTIK: TOTAL PRODUCTS
        productsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long prodCount = snapshot.getChildrenCount();
                tvTotalProducts.setText(String.format(Locale.US, "%,d", prodCount));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Products Stats Error: " + error.getMessage());
            }
        };
        productsRef.addValueEventListener(productsListener);

        // 3. STATISTIK: TOTAL REVENUE (10% Admin Commission)
        ordersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                double totalAdminProfit = 0.0;

                for (DataSnapshot orderSnap : snapshot.getChildren()) {
                    // LOGIK: Hanya kira revenue jika order sudah dibayar/selesai
                    String status = orderSnap.child("orderStatus").getValue(String.class);

                    // Anda boleh tapis: if ("Completed".equals(status) || "Shipped".equals(status))
                    // Buat masa ni, kita kira semua yang ada 'totalPayment'
                    Double totalPayment = orderSnap.child("totalPayment").getValue(Double.class);

                    if (totalPayment != null) {
                        // Admin Dashboard biasanya memaparkan keuntungan platform (Commission)
                        // Contoh: Jualan RM100, Admin untung RM10 (10%)
                        totalAdminProfit += (totalPayment * 0.10);
                    }
                }

                // Format mata wang Malaysia (RM)
                NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("ms", "MY"));
                tvTotalRevenue.setText(currencyFormat.format(totalAdminProfit));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Revenue Stats Error: " + error.getMessage());
            }
        };
        ordersRef.addValueEventListener(ordersListener);
    }

    /**
     * Memuatkan imej profil Admin yang sedang log masuk.
     */
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
                                .placeholder(R.drawable.ic_admin) // Ikon default
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
                .setMessage("Are you want to log out?")
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
        // Mula ambil data bila aktiviti bermula
        loadPlatformStatistics();
        loadAdminProfileImage();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Buang listener untuk jimat memori & data Firebase bila aktiviti tak aktif
        if (usersRef != null && usersListener != null) usersRef.removeEventListener(usersListener);
        if (productsRef != null && productsListener != null) productsRef.removeEventListener(productsListener);
        if (ordersRef != null && ordersListener != null) ordersRef.removeEventListener(ordersListener);
        if (usersRef != null && profileListener != null) usersRef.removeEventListener(profileListener);
    }
}