package com.example.beautyhub.seller;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.beautyhub.R;
import com.example.beautyhub.buyer.NotificationActivity;
import com.example.beautyhub.buyer.ReportProblemActivity;
import com.example.beautyhub.info.AboutUsActivity;
import com.example.beautyhub.info.ContactUsActivity;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class SellerActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private ImageView iconMenu;
    private NavigationView navigationView;
    private BottomNavigationView bottomNavigationView;

    // Notification Elements
    private FrameLayout btnNotifications;
    private TextView tvNotificationBadge;

    // Dashboard Stats (Business Insight)
    private TextView tvTotalSales, tvTotalOrders, tvMiniLowStock, tvTodayVisitors;
    private BarChart barChartStatistics;

    // Quick Action Containers
    private LinearLayout cardAddProduct, cardManageProducts, cardViewOrders;

    // To-Do List Cards & Badges
    private MaterialCardView cardToShip, cardLowStockMini;
    private TextView tvToShipCount, tvLowStockBadge;

    private FirebaseAuth mAuth;
    private String currentSellerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.s_activity_seller);

        mAuth = FirebaseAuth.getInstance();
        currentSellerId = mAuth.getUid();

        if (currentSellerId == null) {
            finish();
            return;
        }

        initViews();
        setupListeners();
        loadDashboardData();
        updateNotificationBadge();
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout_seller);
        iconMenu = findViewById(R.id.icon_menu_seller);
        navigationView = findViewById(R.id.nav_view_seller);

        // Notifications
        btnNotifications = findViewById(R.id.btn_notifications_seller);
        ImageView ivNotificationIcon = findViewById(R.id.iv_notification_icon);
        if (ivNotificationIcon != null) {
            ivNotificationIcon.setColorFilter(Color.BLACK);
        }
        tvNotificationBadge = findViewById(R.id.tv_notification_badge);

        // Stats (Business Insight)
        tvTotalSales = findViewById(R.id.tv_total_sales);
        tvTotalOrders = findViewById(R.id.tv_total_orders);
        tvMiniLowStock = findViewById(R.id.tv_mini_low_stock);
        tvTodayVisitors = findViewById(R.id.tv_today_visitors); // Ensure this ID exists in XML

        // To-Do List Badges (Update these IDs based on your XML)
        tvToShipCount = findViewById(R.id.tv_to_ship_count);
        tvLowStockBadge = findViewById(R.id.tv_mini_low_stock);

        // To-Do List Cards
        cardToShip = findViewById(R.id.card_to_ship);
        cardLowStockMini = findViewById(R.id.low_stock_card_mini);

        // Chart
        barChartStatistics = findViewById(R.id.bar_chart_statistics);

        // Quick Actions
        cardAddProduct = findViewById(R.id.card_add_product);
        cardManageProducts = findViewById(R.id.card_manage_products);
        cardViewOrders = findViewById(R.id.card_view_orders);

        bottomNavigationView = findViewById(R.id.bottom_navigation_bar);
    }

    private void setupListeners() {
        iconMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        btnNotifications.setOnClickListener(v -> startActivity(new Intent(this, NotificationActivity.class)));

        navigationView.setNavigationItemSelectedListener(menuItem -> {
            int itemId = menuItem.getItemId();

            if (itemId == R.id.nav_seller_home) {
                // Berada di Dashboard, cuma tutup drawer
                drawerLayout.closeDrawers();
            } else if (itemId == R.id.nav_about_us) {
                // Pergi ke halaman Manage Products
                startActivity(new Intent(this, AboutUsActivity.class));
            } else if (itemId == R.id.nav_contact_us) {
                // Pergi ke halaman Seller Orders
                startActivity(new Intent(this, ContactUsActivity.class));
            } else if (itemId == R.id.nav_report) {
                // Pergi ke Report Problem
                Intent intent = new Intent(this, ReportProblemActivity.class);
                intent.putExtra("userRole", "Seller");
                startActivity(intent);
            } else if (itemId == R.id.nav_logout) {
                // Panggil fungsi logout
                logoutUser();
            }

            // Mesti tutup drawer selepas klik mana-mana item
            drawerLayout.closeDrawers();
            return true;
        });
        cardAddProduct.setOnClickListener(v -> startActivity(new Intent(this, AddProductActivity.class)));
        cardManageProducts.setOnClickListener(v -> startActivity(new Intent(this, ManageProductsActivity.class)));
        cardViewOrders.setOnClickListener(v -> startActivity(new Intent(this, SellerOrderActivity.class)));

        cardToShip.setOnClickListener(v -> {
            Intent intent = new Intent(this, SellerOrderActivity.class);
            intent.putExtra("filter", "Pending");
            startActivity(intent);
        });

        cardLowStockMini.setOnClickListener(v -> {
            Intent intent = new Intent(this, ManageProductsActivity.class);
            intent.putExtra("filter", "low_stock");
            startActivity(intent);
        });

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_seller_home) return true;
            if (itemId == R.id.nav_seller_profile) {
                startActivity(new Intent(this, SellerProfileActivity.class));
                return true;
            }
            if (itemId == R.id.nav_seller_logout) {
                logoutUser();
                return true;
            }
            return false;
        });
    }

    private void loadDashboardData() {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();

        // 1. Fetch Orders (Sales & To-Ship Count)
        rootRef.child("Orders").orderByChild("sellerId").equalTo(currentSellerId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        double totalSales = 0;
                        int totalOrders = 0;
                        int toShipCount = 0;

                        for (DataSnapshot ds : snapshot.getChildren()) {
                            String status = ds.child("status").getValue(String.class);
                            Double amount = ds.child("totalAmount").getValue(Double.class);

                            if (amount != null && !"Cancelled".equalsIgnoreCase(status)) {
                                totalSales += amount;
                            }

                            totalOrders++;

                            // To-Do List logic: Parallel with "Pending" or "Processing" status
                            if ("Pending".equalsIgnoreCase(status) || "Processing".equalsIgnoreCase(status)) {
                                toShipCount++;
                            }
                        }

                        tvTotalSales.setText(String.format(Locale.US, "RM %.2f", totalSales));
                        tvTotalOrders.setText(String.valueOf(totalOrders));
                        if (tvToShipCount != null) tvToShipCount.setText(String.valueOf(toShipCount));

                        // Update the chart whenever order data changes
                        setupStatisticsChart(snapshot);
                    }

                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });

        // 2. Fetch Low Stock Products
        rootRef.child("Products").orderByChild("sellerId").equalTo(currentSellerId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int lowStockCount = 0;
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Integer stock = ds.child("stock").getValue(Integer.class);
                            if (stock != null && stock <= 5) { // Threshold for low stock
                                lowStockCount++;
                            }
                        }
                        tvMiniLowStock.setText(String.valueOf(lowStockCount));
                        if (tvLowStockBadge != null) tvLowStockBadge.setText(String.valueOf(lowStockCount));
                    }

                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });

        // 3. Fetch Visitors (Total views from Seller Profile)
        rootRef.child("SellerProfiles").child(currentSellerId).child("views")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Long views = snapshot.getValue(Long.class);
                        if (tvTodayVisitors != null) {
                            tvTodayVisitors.setText(views != null ? String.valueOf(views) : "0");
                        }
                    }

                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void setupStatisticsChart(DataSnapshot ordersSnapshot) {
        float[] monthlySales = new float[4]; // Oct, Nov, Dec, Jan
        Calendar cal = Calendar.getInstance();

        for (DataSnapshot ds : ordersSnapshot.getChildren()) {
            Double amount = ds.child("totalAmount").getValue(Double.class);
            Long timestamp = ds.child("orderDate").getValue(Long.class);
            String status = ds.child("status").getValue(String.class);

            if (amount != null && timestamp != null && !"Cancelled".equalsIgnoreCase(status)) {
                cal.setTimeInMillis(timestamp);
                int month = cal.get(Calendar.MONTH); // 0=Jan, 9=Oct, 10=Nov, 11=Dec

                if (month == Calendar.OCTOBER) monthlySales[0] += amount;
                else if (month == Calendar.NOVEMBER) monthlySales[1] += amount;
                else if (month == Calendar.DECEMBER) monthlySales[2] += amount;
                else if (month == Calendar.JANUARY) monthlySales[3] += amount;
            }
        }

        ArrayList<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < monthlySales.length; i++) {
            entries.add(new BarEntry(i, monthlySales[i]));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Monthly Sales");
        dataSet.setColor(ContextCompat.getColor(this, R.color.seed));
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(10f);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.5f);

        XAxis xAxis = barChartStatistics.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        final String[] months = {"Oct", "Nov", "Dec", "Jan"};
        xAxis.setValueFormatter(new IndexAxisValueFormatter(months));

        barChartStatistics.getDescription().setEnabled(false);
        barChartStatistics.getLegend().setEnabled(false);
        barChartStatistics.getAxisRight().setEnabled(false);
        barChartStatistics.getAxisLeft().setAxisMinimum(0f);
        barChartStatistics.setData(barData);
        barChartStatistics.invalidate();
        barChartStatistics.animateY(1000);
    }

    private void updateNotificationBadge() {
        DatabaseReference notifRef = FirebaseDatabase.getInstance().getReference("Notifications").child(currentSellerId);
        Query unreadQuery = notifRef.orderByChild("unread").equalTo(true);

        unreadQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long count = snapshot.getChildrenCount();
                if (count > 0) {
                    tvNotificationBadge.setText(String.valueOf(count));
                    tvNotificationBadge.setVisibility(View.VISIBLE);
                } else {
                    tvNotificationBadge.setVisibility(View.GONE);
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void logoutUser() {
        FirebaseAuth.getInstance().signOut();
        finish();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}