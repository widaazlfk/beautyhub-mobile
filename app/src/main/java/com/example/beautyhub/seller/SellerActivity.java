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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.beautyhub.R;
import com.example.beautyhub.buyer.NotificationActivity;
import com.example.beautyhub.buyer.ReportProblemActivity;
import com.example.beautyhub.info.AboutUsActivity;
import com.example.beautyhub.info.ContactUsActivity;
import com.example.beautyhub.auth.LoginActivity;
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
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;

public class SellerActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private ImageView iconMenu;
    private NavigationView navigationView;
    private BottomNavigationView bottomNavigationView;

    private FrameLayout btnNotifications;
    private TextView tvNotificationBadge;

    // tvTodayVisitors telah dibuang
    private TextView tvTotalSales, tvTotalOrders, tvMiniLowStock,  tvNewOrdersCount;
    private BarChart barChartStatistics;

    private LinearLayout cardAddProduct, cardManageProducts, cardViewOrders, cardFinance;

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
            startActivity(new Intent(this, LoginActivity.class));
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

        btnNotifications = findViewById(R.id.btn_notifications_seller);
        ImageView ivNotificationIcon = findViewById(R.id.iv_notification_icon);
        if (ivNotificationIcon != null) {
            ivNotificationIcon.setColorFilter(Color.BLACK);
        }
        tvNotificationBadge = findViewById(R.id.tv_notification_badge);

        tvTotalSales = findViewById(R.id.tv_total_sales);
        tvTotalOrders = findViewById(R.id.tv_total_orders);
        tvMiniLowStock = findViewById(R.id.tv_mini_low_stock);
        // tvTodayVisitors initialization dibuang

        tvToShipCount = findViewById(R.id.tv_to_ship_count);
        tvLowStockBadge = findViewById(R.id.tv_mini_low_stock);

        cardToShip = findViewById(R.id.card_to_ship);
        cardLowStockMini = findViewById(R.id.low_stock_card_mini);

        barChartStatistics = findViewById(R.id.bar_chart_statistics);

        cardAddProduct = findViewById(R.id.card_add_product);
        cardManageProducts = findViewById(R.id.card_manage_products);
        cardViewOrders = findViewById(R.id.card_view_orders);
        cardFinance = findViewById(R.id.card_finance);

        bottomNavigationView = findViewById(R.id.bottom_navigation_bar);
    }

    private void setupListeners() {
        iconMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        btnNotifications.setOnClickListener(v -> startActivity(new Intent(this, NotificationActivity.class)));

        navigationView.setNavigationItemSelectedListener(menuItem -> {
            int itemId = menuItem.getItemId();
            if (itemId == R.id.nav_seller_home) {
                drawerLayout.closeDrawers();
            } else if (itemId == R.id.nav_about_us) {
                startActivity(new Intent(this, AboutUsActivity.class));
            } else if (itemId == R.id.nav_contact_us) {
                startActivity(new Intent(this, ContactUsActivity.class));
            } else if (itemId == R.id.nav_report) {
                Intent intent = new Intent(this, ReportProblemActivity.class);
                intent.putExtra("userRole", "Seller");
                startActivity(intent);
            } else if (itemId == R.id.nav_logout) {
                logoutUser();
            }
            drawerLayout.closeDrawers();
            return true;
        });

        cardAddProduct.setOnClickListener(v -> startActivity(new Intent(this, AddProductActivity.class)));
        cardManageProducts.setOnClickListener(v -> startActivity(new Intent(this, ManageProductsActivity.class)));
        cardViewOrders.setOnClickListener(v -> startActivity(new Intent(this, SellerOrderActivity.class)));
        cardFinance.setOnClickListener(v -> {
            // Ganti FinanceActivity.class dengan nama activity kewangan anda
            Intent intent = new Intent(SellerActivity.this, SellerFinanceActivity.class);
            startActivity(intent);
        });

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
        // Tambah di setupListeners()
        findViewById(R.id.btn_view_full_report).setOnClickListener(v -> {
            Intent intent = new Intent(SellerActivity.this, SellerReportActivity.class);
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

        rootRef.child("Orders").orderByChild("sellerId").equalTo(currentSellerId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) return;

                        long latestTimestamp = 0;
                        // First pass: Find the latest order timestamp to determine "Recent Month"
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Long ts = ds.child("orderDate").getValue(Long.class);
                            if (ts != null && ts > latestTimestamp) {
                                latestTimestamp = ts;
                            }
                        }

                        Calendar calRecent = Calendar.getInstance();
                        if (latestTimestamp > 0) {
                            calRecent.setTimeInMillis(latestTimestamp);
                        }

                        int recentMonth = calRecent.get(Calendar.MONTH);
                        int recentYear = calRecent.get(Calendar.YEAR);

                        double monthlyNetSales = 0;
                        int totalOrdersInRecentMonth = 0;
                        int toShipBadgeCount = 0;

                        Calendar orderCal = Calendar.getInstance();

                        for (DataSnapshot ds : snapshot.getChildren()) {
                            String status = ds.child("status").getValue(String.class);
                            Double amount = ds.child("totalAmount").getValue(Double.class);
                            Long timestamp = ds.child("orderDate").getValue(Long.class);

                            if (timestamp != null) {
                                orderCal.setTimeInMillis(timestamp);

                                // Check if this order belongs to the recent month identified
                                if (orderCal.get(Calendar.MONTH) == recentMonth &&
                                        orderCal.get(Calendar.YEAR) == recentYear) {

                                    totalOrdersInRecentMonth++;

                                    if (amount != null && "Completed".equalsIgnoreCase(status)) {
                                        monthlyNetSales += (amount * 0.90);
                                    }
                                }
                            }

                            // Badge count remains global for current pending tasks
                            if ("Pending".equalsIgnoreCase(status) || "Processing".equalsIgnoreCase(status)) {
                                toShipBadgeCount++;
                            }
                        }

                        // Update UI
                        tvTotalSales.setText(String.format(Locale.US, "RM %.2f", monthlyNetSales));
                        tvTotalOrders.setText(String.valueOf(totalOrdersInRecentMonth));

                        // Update label to show which month is being viewed
                        String monthName = calRecent.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
                        // Assuming you have a label for the section
                        // tvSalesTitle.setText("Sales: " + monthName + " " + recentYear);

                        if (tvToShipCount != null) {
                            tvToShipCount.setText(String.valueOf(toShipBadgeCount));
                        }

                        setupStatisticsChart(snapshot, recentMonth, recentYear);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });



        rootRef.child("Products").orderByChild("sellerId").equalTo(currentSellerId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int lowStockCount = 0;
                        for (DataSnapshot ds : snapshot.getChildren()) {

                            Integer stock = ds.child("quantity").getValue(Integer.class);

                            if (stock == null) {
                                stock = ds.child("stock").getValue(Integer.class);
                            }

                            if (stock != null && stock <= 10) {
                                lowStockCount++;
                            }
                        }

                        // Update Badge di Dashboard
                        if (tvLowStockBadge != null) {
                            tvLowStockBadge.setText(String.valueOf(lowStockCount));
                            tvLowStockBadge.setVisibility(View.VISIBLE);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("SellerActivity", "Products Error: " + error.getMessage());
                    }
                });
    }
    private void setupStatisticsChart(DataSnapshot ordersSnapshot, int targetMonth, int targetYear) {
        Calendar cal = Calendar.getInstance();

        // Set the calendar to the target month/year to calculate days correctly
        cal.set(Calendar.YEAR, targetYear);
        cal.set(Calendar.MONTH, targetMonth);

        String monthName = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());

        // Dapatkan jumlah hari dalam bulan yang dikesan (Contoh: Jan=31, Feb=28)
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        float[] dailyNetSales = new float[daysInMonth];
        String[] labels = new String[daysInMonth];

        for (int i = 0; i < daysInMonth; i++) {
            labels[i] = String.valueOf(i + 1);
            dailyNetSales[i] = 0f;
        }

        // 2. Baca data dari Firebase SNAPSHOT
        if (ordersSnapshot.exists()) {
            for (DataSnapshot ds : ordersSnapshot.getChildren()) {
                String status = ds.child("status").getValue(String.class);
                Double amount = ds.child("totalAmount").getValue(Double.class);
                Long timestamp = ds.child("orderDate").getValue(Long.class);

                // Filter: Mesti "Completed" & Bulan/Tahun yang dikesan tadi
                if (amount != null && timestamp != null && "Completed".equalsIgnoreCase(status)) {
                    cal.setTimeInMillis(timestamp);

                    if (cal.get(Calendar.MONTH) == targetMonth && cal.get(Calendar.YEAR) == targetYear) {
                        int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);

                        if (dayOfMonth >= 1 && dayOfMonth <= daysInMonth) {
                            // Seller dapat 90%
                            dailyNetSales[dayOfMonth - 1] += (float) (amount * 0.90);
                        }
                    }
                }
            }
        }

        // 3. Masukkan data ke dalam MPAndroidChart
        ArrayList<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < dailyNetSales.length; i++) {
            entries.add(new BarEntry(i, dailyNetSales[i]));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Net Sales - " + monthName + " " + targetYear + " (RM)");
        dataSet.setColor(Color.parseColor("#FF69B4"));
        dataSet.setDrawValues(false);

        BarData data = new BarData(dataSet);
        barChartStatistics.setData(data);

        // 4. Konfigurasi X-Axis
        XAxis xAxis = barChartStatistics.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(10);
        xAxis.setDrawGridLines(false);

        barChartStatistics.getAxisRight().setEnabled(false);
        barChartStatistics.getAxisLeft().setAxisMinimum(0f);
        barChartStatistics.getDescription().setEnabled(false);

        // 5. Logik Klik
        barChartStatistics.setOnChartValueSelectedListener(new com.github.mikephil.charting.listener.OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(com.github.mikephil.charting.data.Entry e, com.github.mikephil.charting.highlight.Highlight h) {
                int selectedDay = (int) e.getX() + 1;

                Intent intent = new Intent(SellerActivity.this, com.example.beautyhub.seller.SellerOrderActivity.class);
                intent.putExtra("filter_status", "Completed");
                intent.putExtra("filter_day", selectedDay);
                intent.putExtra("filter_month", targetMonth);
                intent.putExtra("filter_year", targetYear);
                startActivity(intent);
            }

            @Override
            public void onNothingSelected() {}
        });

        barChartStatistics.animateY(1000);
        barChartStatistics.invalidate();
    }

    private void updateNotificationBadge() {
        if (currentSellerId == null) return;
        DatabaseReference notifRef = FirebaseDatabase.getInstance().getReference("Notifications").child(currentSellerId);
        notifRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int unreadCount = 0;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Boolean isUnread = ds.child("unread").getValue(Boolean.class);
                    if (isUnread != null && isUnread) unreadCount++;
                }
                if (unreadCount > 0) {
                    tvNotificationBadge.setVisibility(View.VISIBLE);
                    tvNotificationBadge.setText(String.valueOf(unreadCount));
                } else {
                    tvNotificationBadge.setVisibility(View.GONE);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("SellerActivity", "Badge Error: " + error.getMessage());
            }
        });
    }

    private void logoutUser() {
        mAuth.signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}