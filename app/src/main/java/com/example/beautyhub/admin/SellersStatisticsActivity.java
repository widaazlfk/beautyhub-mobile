package com.example.beautyhub.admin;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class SellersStatisticsActivity extends AppCompatActivity {

    private static final String TAG = "SellersStatistics";
    private PieChart pieChart;
    private DatabaseReference ordersRef;
    private DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sellers_statistics);

        setupToolbar();

        pieChart = findViewById(R.id.sellers_pie_chart);
        setupProfessionalPieChart();

        // Rujukan database
        ordersRef = FirebaseDatabase.getInstance().getReference("Orders");
        usersRef = FirebaseDatabase.getInstance().getReference("Users");

        fetchSellersStatistics();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_sellers_statistics);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Seller Sales Performance");
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            // Fungsi Butang Back
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }
    }

    private void setupProfessionalPieChart() {
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(10, 10, 10, 10);

        // Design Hole (Lubang Tengah) supaya nampak moden (Donut Chart)
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setTransparentCircleRadius(61f);
        pieChart.setHoleRadius(58f);

        pieChart.setEntryLabelColor(Color.DKGRAY);
        pieChart.setEntryLabelTextSize(11f);
        pieChart.setCenterText("Sales by Seller");
        pieChart.setCenterTextSize(16f);

        Legend l = pieChart.getLegend();
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l.setDrawInside(false);
        l.setTextSize(12f);
        l.setWordWrapEnabled(true);
    }

    private void fetchSellersStatistics() {
        ordersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    Toast.makeText(SellersStatisticsActivity.this, "No order data found", Toast.LENGTH_SHORT).show();
                    return;
                }

                Map<String, Float> sellerSales = new HashMap<>();
                for (DataSnapshot orderSnapshot : dataSnapshot.getChildren()) {
                    String sellerId = orderSnapshot.child("sellerId").getValue(String.class);

                    // TUKAR: totalPayment -> totalAmount (ikut Firebase anda)
                    Object amountObj = orderSnapshot.child("totalAmount").getValue();
                    float amountValue = 0f;

                    if (amountObj instanceof Long) amountValue = ((Long) amountObj).floatValue();
                    else if (amountObj instanceof Double) amountValue = ((Double) amountObj).floatValue();

                    if (sellerId != null && amountValue > 0) {
                        sellerSales.put(sellerId, sellerSales.getOrDefault(sellerId, 0f) + amountValue);
                    }
                }

                if (sellerSales.isEmpty()) {
                    Toast.makeText(SellersStatisticsActivity.this, "No sales found", Toast.LENGTH_SHORT).show();
                } else {
                    fetchSellerNamesAndLoadChart(sellerSales);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Database Error: " + databaseError.getMessage());
            }
        });
    }

    private void fetchSellerNamesAndLoadChart(Map<String, Float> sellerSales) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        AtomicInteger counter = new AtomicInteger(sellerSales.size());

        for (Map.Entry<String, Float> entry : sellerSales.entrySet()) {
            String sellerId = entry.getKey();
            Float totalSales = entry.getValue();

            usersRef.child(sellerId).child("username").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String name = snapshot.exists() ? snapshot.getValue(String.class) : "Unknown Seller";
                    entries.add(new PieEntry(totalSales, name));

                    if (counter.decrementAndGet() == 0) {
                        loadPieChartData(entries);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    if (counter.decrementAndGet() == 0) loadPieChartData(entries);
                }
            });
        }
    }

    private void loadPieChartData(ArrayList<PieEntry> entries) {
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);

        // Skema warna yang lebih matang/profesional
        int[] customColors = {
                Color.parseColor("#8a2128"), // Deep Red
                Color.parseColor("#2196F3"), // Blue
                Color.parseColor("#4CAF50"), // Green
                Color.parseColor("#FF9800"), // Orange
                Color.parseColor("#9C27B0")  // Purple
        };
        dataSet.setColors(customColors);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(pieChart));
        data.setValueTextSize(12f);
        data.setValueTextColor(Color.WHITE);

        pieChart.setData(data);
        pieChart.animateY(1400);
        pieChart.invalidate();
    }
}