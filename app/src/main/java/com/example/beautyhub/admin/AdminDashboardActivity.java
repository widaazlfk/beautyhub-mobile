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

import com.example.beautyhub.R;
import com.example.beautyhub.auth.LoginActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.util.Locale;

public class AdminDashboardActivity extends AppCompatActivity {

    private static final String TAG = "AdminDashboardActivity";
    private FirebaseAuth mAuth;

    // Firebase Database References
    private DatabaseReference usersRef;
    private DatabaseReference productsRef;
    private DatabaseReference ordersRef;

    // Firebase Listeners to prevent memory leaks
    private ValueEventListener usersListener;
    private ValueEventListener productsListener;
    private ValueEventListener ordersListener;

    // Views untuk Statistik
    private TextView tvTotalUsers, tvTotalSellers, tvTotalProducts, tvTotalRevenue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_activity_admin);

        mAuth = FirebaseAuth.getInstance();

        // Inisialisasi rujukan pangkalan data
        usersRef = FirebaseDatabase.getInstance().getReference("Users");
        productsRef = FirebaseDatabase.getInstance().getReference("Products");
        ordersRef = FirebaseDatabase.getInstance().getReference("Orders");

        initViews();
        setupToolbar();
        setupListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Mula memuatkan data statistik dari Firebase apabila aktiviti kelihatan
        loadPlatformStatistics();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Hentikan listener untuk menjimatkan sumber apabila aktiviti tidak kelihatan
        removeFirebaseListeners();
    }

    private void initViews() {
        // Hubungkan TextViews untuk statistik
        tvTotalUsers = findViewById(R.id.tv_total_users);
        tvTotalSellers = findViewById(R.id.tv_total_sellers);
        tvTotalProducts = findViewById(R.id.tv_total_products);
        tvTotalRevenue = findViewById(R.id.tv_total_revenue);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_admin_dashboard);
        setSupportActionBar(toolbar);
    }

    private void setupListeners() {
        bindClickListener(R.id.card_manage_users, v ->
                startActivity(new Intent(AdminDashboardActivity.this, ManageUsersActivity.class))
        );
        bindClickListener(R.id.card_manage_products, v ->
                startActivity(new Intent(AdminDashboardActivity.this, ManageProductsActivity.class))
        );
        bindClickListener(R.id.card_manage_categories, v ->
                startActivity(new Intent(AdminDashboardActivity.this, ManageCategoriesActivity.class))
        );
        bindClickListener(R.id.card_manage_reviews, v ->
                startActivity(new Intent(AdminDashboardActivity.this, ManageReviewsActivity.class))
        );
        bindClickListener(R.id.card_manage_rewards, v ->
                startActivity(new Intent(AdminDashboardActivity.this, ManageRewardsActivity.class))
        );
        bindClickListener(R.id.card_admin_profile, v -> showProfileAndLogoutDialog());
        bindClickListener(R.id.card_activity_logs, v ->
                startActivity(new Intent(AdminDashboardActivity.this, AdminLogActivity.class))
        );
    }

    private void loadPlatformStatistics() {
        // 1. Muat turun jumlah pengguna dan penjual
        usersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long totalSellers = 0;
                long totalUsers = snapshot.getChildrenCount();

                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    // [PEMBETULAN] Tukar "role" kepada "userType" dan bandingkan dengan "Seller"
                    String userType = userSnapshot.child("userType").getValue(String.class);
                    if ("Seller".equalsIgnoreCase(userType)) {
                        totalSellers++;
                    }
                }
                tvTotalUsers.setText(String.format(Locale.US, "%,d", totalUsers));
                tvTotalSellers.setText(String.format(Locale.US, "%,d", totalSellers));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Failed to read user count: " + error.getMessage());
                tvTotalUsers.setText("N/A");
                tvTotalSellers.setText("N/A");
            }
        };
        usersRef.addValueEventListener(usersListener);

        // 2. Muat turun jumlah produk
        productsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long totalProducts = snapshot.getChildrenCount();
                tvTotalProducts.setText(String.format(Locale.US, "%,d", totalProducts));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Failed to read product count: " + error.getMessage());
                tvTotalProducts.setText("N/A");
            }
        };
        productsRef.addValueEventListener(productsListener);

        // 3. Muat turun dan kira jumlah hasil jualan
        ordersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                double totalRevenue = 0.0;
                for (DataSnapshot orderSnapshot : snapshot.getChildren()) {
                    Double amount = orderSnapshot.child("totalPayment").getValue(Double.class);
                    if (amount != null) {
                        totalRevenue += amount;
                    }
                }
                NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("ms", "MY"));
                tvTotalRevenue.setText(currencyFormat.format(totalRevenue));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Failed to read revenue: " + error.getMessage());
                tvTotalRevenue.setText("N/A");
            }
        };
        ordersRef.addValueEventListener(ordersListener);
    }

    private void removeFirebaseListeners() {
        if (usersRef != null && usersListener != null) {
            usersRef.removeEventListener(usersListener);
        }
        if (productsRef != null && productsListener != null) {
            productsRef.removeEventListener(productsListener);
        }
        if (ordersRef != null && ordersListener != null) {
            ordersRef.removeEventListener(ordersListener);
        }
    }

    private void showProfileAndLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Profile & Logout")
                .setMessage("What would you like to do?")
                .setPositiveButton("Logout", (dialog, which) -> logoutAdmin())
                .setNeutralButton("View Profile", (dialog, which) ->
                        startActivity(new Intent(AdminDashboardActivity.this, AdminProfileActivity.class))
                )
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void logoutAdmin() {
        LogHelper.logCurrentUserAction("Admin Logout", "Admin logged out from the dashboard.", "Admin");
        mAuth.signOut();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void bindClickListener(int id, View.OnClickListener listener) {
        View view = findViewById(id);
        if (view != null) {
            view.setOnClickListener(listener);
        } else {
            Log.e(TAG, "Error: View with ID '" + getResources().getResourceEntryName(id) + "' not found.");
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
