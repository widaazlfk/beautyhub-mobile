package com.example.beautyhub.admin;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.beautyhub.R;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Locale;

public class UsersStatisticsActivity extends AppCompatActivity {

    private static final String TAG = "UsersStatisticsActivity";
    private DatabaseReference usersRef;
    private ValueEventListener usersValueEventListener;

    private TextView tvTotalUsers, tvTotalSellers, tvTotalBuyers;
    private PieChart pieChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users_statistics);

        // Initialize Firebase reference
        usersRef = FirebaseDatabase.getInstance().getReference("Users");

        initViews();
        setupToolbar();
        setupPieChart();
    }

    private void initViews() {
        tvTotalUsers = findViewById(R.id.tv_stats_total_users);
        tvTotalSellers = findViewById(R.id.tv_stats_total_sellers);
        tvTotalBuyers = findViewById(R.id.tv_stats_total_buyers);
        pieChart = findViewById(R.id.pie_chart_users);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_users_statistics);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
            }

            // Memberikan fungsi butang kembali (Navigation Icon)
            toolbar.setNavigationOnClickListener(v -> {
                onBackPressed(); // Menutup aktiviti ini dan kembali ke Admin Dashboard
            });
        }
    }

    // Nota: Method ini boleh dikekalkan atau dibuang jika guna setNavigationOnClickListener di atas
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupPieChart() {
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(5, 10, 5, 5);
        pieChart.setDragDecelerationFrictionCoef(0.95f);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setTransparentCircleRadius(61f);

        Legend legend = pieChart.getLegend();
        legend.setEnabled(true);
        legend.setTextSize(14f);
        legend.setForm(Legend.LegendForm.SQUARE);
        legend.setFormSize(10f);
        legend.setXEntrySpace(10f);
    }

    private void loadUserData() {
        usersValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long totalUsers = snapshot.getChildrenCount();
                long totalSellers = 0;
                long totalBuyers = 0;

                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String userType = userSnapshot.child("userType").getValue(String.class);
                    if ("Seller".equalsIgnoreCase(userType)) {
                        totalSellers++;
                    } else if ("Buyer".equalsIgnoreCase(userType)) {
                        totalBuyers++;
                    }
                }

                tvTotalUsers.setText(String.format(Locale.US, "%,d", totalUsers));
                tvTotalSellers.setText(String.format(Locale.US, "%,d", totalSellers));
                tvTotalBuyers.setText(String.format(Locale.US, "%,d", totalBuyers));

                updatePieChartData(totalBuyers, totalSellers);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to read user data: " + error.getMessage());
                Toast.makeText(UsersStatisticsActivity.this, "Failed to load statistics.", Toast.LENGTH_SHORT).show();
            }
        };
        usersRef.addValueEventListener(usersValueEventListener);
    }

    private void updatePieChartData(long buyersCount, long sellersCount) {
        if (buyersCount == 0 && sellersCount == 0) {
            pieChart.clear();
            pieChart.setCenterText("No User Data");
            pieChart.invalidate();
            return;
        }

        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(buyersCount, "Buyer"));
        entries.add(new PieEntry(sellersCount, "Seller"));

        PieDataSet dataSet = new PieDataSet(entries, "User Distribution");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);

        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(Color.parseColor("#4C4CAF")); // Warna Buyer

        android.util.TypedValue typedValue = new android.util.TypedValue();
        getTheme().resolveAttribute(com.google.android.material.R.attr.colorSecondary, typedValue, true);
        colors.add(typedValue.data); // Warna Seller

        dataSet.setColors(colors);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(pieChart));
        data.setValueTextSize(14f);
        data.setValueTextColor(Color.WHITE);
        data.setValueTypeface(Typeface.DEFAULT_BOLD);

        pieChart.setData(data);
        pieChart.setCenterText("User Types");
        pieChart.animateY(1000);
        pieChart.invalidate();
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadUserData();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (usersRef != null && usersValueEventListener != null) {
            usersRef.removeEventListener(usersValueEventListener);
        }
    }
}