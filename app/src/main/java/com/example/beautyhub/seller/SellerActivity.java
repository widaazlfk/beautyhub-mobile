package com.example.beautyhub.seller;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.beautyhub.R;
import com.example.beautyhub.admin.LogHelper;
import com.example.beautyhub.auth.LoginActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.util.Locale;

public class SellerActivity extends AppCompatActivity {

    // Deklarasi semua elemen UI dari XML
    private TextView tvTotalSales, tvTotalOrders;
    private MaterialCardView cardAddProduct, cardManageProducts, cardViewOrders;
    private Button btnViewAllOrders, btnManageInventory;
    private BottomNavigationView bottomNavigationView;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageView iconMenu;
    private TextView navHeaderName, navHeaderEmail;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference ordersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.s_activity_seller);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Authentication required.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ordersRef = FirebaseDatabase.getInstance().getReference("Orders");

        initViews();
        setupDrawerNavigation(); // Panggil setup untuk Drawer Layout
        setupClickListeners();
        setupBottomNavigation(); // Panggil method untuk setup bottom navigation
        loadDashboardData();
        loadNavHeaderInfo();
    }

    private void initViews() {
        tvTotalSales = findViewById(R.id.tv_total_sales);
        tvTotalOrders = findViewById(R.id.tv_total_orders);
        cardAddProduct = findViewById(R.id.card_add_product);
        cardManageProducts = findViewById(R.id.card_manage_products);
        cardViewOrders = findViewById(R.id.card_view_orders);
        btnViewAllOrders = findViewById(R.id.btn_view_all_orders);
        btnManageInventory = findViewById(R.id.btn_manage_inventory);
        bottomNavigationView = findViewById(R.id.bottom_navigation_bar);

        // Inisialisasi Views untuk Drawer Layout
        drawerLayout = findViewById(R.id.drawer_layout_seller);
        navigationView = findViewById(R.id.navigation_view_seller);
        iconMenu = findViewById(R.id.icon_menu_seller);
        View headerView = navigationView.getHeaderView(0);
        navHeaderName = headerView.findViewById(R.id.nav_header_name);
        navHeaderEmail = headerView.findViewById(R.id.nav_header_email);
    }

    private void setupClickListeners() {
        cardAddProduct.setOnClickListener(v ->
                startActivity(new Intent(SellerActivity.this, AddProductActivity.class)));

        cardManageProducts.setOnClickListener(v ->
                startActivity(new Intent(SellerActivity.this, ManageProductsActivity.class)));

        View.OnClickListener viewOrdersListener = v ->
                startActivity(new Intent(SellerActivity.this, SellerOrderActivity.class));
        cardViewOrders.setOnClickListener(viewOrdersListener);
        btnViewAllOrders.setOnClickListener(viewOrdersListener);

        btnManageInventory.setOnClickListener(v -> {
            Toast.makeText(this, "Manage your stock in 'Manage Products'", Toast.LENGTH_LONG).show();
            startActivity(new Intent(SellerActivity.this, ManageProductsActivity.class));
        });
    }

    private void setupDrawerNavigation() {
        // Buka/tutup drawer apabila ikon menu ditekan
        iconMenu.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        // Kendalikan klik pada item menu di navigation view
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_logout) { // Menggunakan menu yang sama seperti Buyer
                showLogoutConfirmation();
            }
            // Tambah 'else if' untuk item menu lain jika perlu

            drawerLayout.closeDrawer(GravityCompat.START); // Tutup drawer selepas item dipilih
            return true;
        });
    }

    private void setupBottomNavigation() {
        // Set item "Dashboard" sebagai yang dipilih
        bottomNavigationView.setSelectedItemId(R.id.nav_seller_home);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_seller_home) {
                // Anda sudah berada di halaman ini, jadi tidak perlu lakukan apa-apa
                return true;
            } else if (itemId == R.id.nav_seller_profile) {
                // Buka SellerProfileActivity
                Intent intent = new Intent(SellerActivity.this, SellerProfileActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_seller_logout) {
                // ▼▼▼ PERBAIKAN: Panggil dialog pengesahan, jangan log keluar terus ▼▼▼
                showLogoutConfirmation();
                // ▲▲▲ AKHIR PERBAIKAN ▲▲▲
                return true;
            }
            return false;
        });
    }
    // Letakkan fungsi ini di mana-mana dalam kelas SellerActivity

    private void loadNavHeaderInfo() {
        if (currentUser != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUser.getUid());
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String name = snapshot.child("username").getValue(String.class);
                        String email = snapshot.child("email").getValue(String.class);

                        if (name != null) {
                            navHeaderName.setText(name);
                        }
                        if (email != null) {
                            navHeaderEmail.setText(email);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(SellerActivity.this, "Failed to load user info.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // --- ▼▼▼ FUNGSI YANG HILANG TELAH DITAMBAH DI SINI ▼▼▼ ---
    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setTitle("Confirm Logout")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Logout", (dialog, which) -> logoutUser())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void logoutUser() {
        LogHelper.logCurrentUserAction("Seller Logout", "Seller logged out from their activity.", "Seller");
        // ▲▲▲ AKHIR TAMBAHAN ▲▲▲
        mAuth.signOut();
        Toast.makeText(SellerActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(SellerActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finishAffinity(); // Guna finishAffinity untuk membersihkan semua aktiviti
    }

    // Kendalikan butang 'back' fizikal untuk menutup drawer dahulu
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
    // --- ▲▲▲ AKHIR FUNGSI TAMBAHAN ▲▲▲ ---

    private void loadDashboardData() {
        // Query untuk mendapatkan pesanan yang berkaitan dengan seller semasa
        // Query untuk mendapatkan pesanan yang berkaitan dengan seller semasa
        com.google.firebase.database.Query sellerOrdersQuery = ordersRef.orderByChild("sellerId").equalTo(currentUser.getUid());

        sellerOrdersQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                double totalSales = 0;
                long totalOrders = 0;

                if (snapshot.exists()) {
                    totalOrders = snapshot.getChildrenCount(); // Kira jumlah pesanan terus
                    for (DataSnapshot orderSnapshot : snapshot.getChildren()) {
                        Double orderTotal = orderSnapshot.child("totalAmount").getValue(Double.class);
                        if (orderTotal != null) {
                            totalSales += orderTotal;
                        }
                    }
                }

                // Format mata wang dan paparkan data
                NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("ms", "MY"));
                tvTotalSales.setText(currencyFormat.format(totalSales));
                tvTotalOrders.setText(String.valueOf(totalOrders));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SellerActivity.this, "Failed to load dashboard data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
