package com.example.beautyhub.admin;

import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.beautyhub.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.renderer.BarChartRenderer;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProductsStatisticsActivity extends AppCompatActivity {

    private static final String TAG = "ProductsStatistics";
    private BarChart barChart;
    private DatabaseReference ordersRef, productsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_products_statistics);

        setupToolbar();
        barChart = findViewById(R.id.products_bar_chart);

        setupProfessionalBarChart();

        ordersRef = FirebaseDatabase.getInstance().getReference("Orders");
        productsRef = FirebaseDatabase.getInstance().getReference("Products");

        fetchPopularProducts();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_products_statistics);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Product Popularity");
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }
    }

    private void setupProfessionalBarChart() {
        barChart.getDescription().setEnabled(false); // Buang description text
        barChart.setDrawGridBackground(false);
        barChart.setDrawBarShadow(false); // Jangan lukis shadow di belakang bar
        barChart.setHighlightFullBarEnabled(false);

        // Tambah padding supaya carta tidak rapat ke tepi
        barChart.setExtraOffsets(10f, 20f, 10f, 20f);

        // Paksi-X (Label Produk)
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false); // Buang garisan grid mencancang
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(10);
        xAxis.setLabelRotationAngle(-45f); // Sendengkan teks supaya tidak bertindih
        xAxis.setTextColor(Color.parseColor("#2D3436"));
        xAxis.setTextSize(11f);

        // Paksi-Y (Kiri)
        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#E0E0E0")); // Garisan grid halus
        leftAxis.setAxisMinimum(0f); // Mula dari 0
        leftAxis.setTextColor(Color.parseColor("#636E72"));
        leftAxis.setTextSize(11f);

        // Paksi-Y (Kanan) - Sembunyikan untuk rupa minimalis
        barChart.getAxisRight().setEnabled(false);

        // Animasi
        barChart.animateY(1500);
    }

    private void fetchPopularProducts() {
        ordersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    Toast.makeText(ProductsStatisticsActivity.this, "No orders found", Toast.LENGTH_SHORT).show();
                    return;
                }

                Map<String, Integer> productCounts = new HashMap<>();

                for (DataSnapshot orderSnapshot : dataSnapshot.getChildren()) {
                    // TUKAR: 'items' -> 'orderItems'
                    DataSnapshot itemsSnapshot = orderSnapshot.child("orderItems");

                    for (DataSnapshot itemSnapshot : itemsSnapshot.getChildren()) {
                        // TUKAR: Ambil 'name' terus dari item order
                        // (Biasanya populariti dikira berdasarkan nama produk dalam order)
                        String productName = itemSnapshot.child("productName").getValue(String.class);

                        if (productName != null) {
                            productCounts.put(productName, productCounts.getOrDefault(productName, 0) + 1);
                        }
                    }
                }

                if (!productCounts.isEmpty()) {
                    // Isih 10 produk paling popular
                    Map<String, Integer> sortedProducts = productCounts.entrySet()
                            .stream()
                            .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                            .limit(10)
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

                    prepareChartData(sortedProducts);
                } else {
                    Toast.makeText(ProductsStatisticsActivity.this, "No valid items found in orders", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error: " + error.getMessage());
            }
        });
    }

    private void prepareChartData(Map<String, Integer> sortedProducts) {
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;

        for (Map.Entry<String, Integer> entry : sortedProducts.entrySet()) {
            String name = entry.getKey();

            // Pendekkan nama jika terlalu panjang untuk carta
            if (name.length() > 12) {
                name = name.substring(0, 10) + "..";
            }

            entries.add(new BarEntry(index, entry.getValue().floatValue()));
            labels.add(name);
            index++;
        }

        loadProfessionalData(entries, labels);
    }
    private void fetchProductNamesAndLoadChart(Map<String, Integer> sortedProducts) {
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        final int totalToFetch = sortedProducts.size();
        final int[] fetchedCount = {0};

        int i = 0;
        for (Map.Entry<String, Integer> entry : sortedProducts.entrySet()) {
            final int index = i;
            final int count = entry.getValue();
            String productId = entry.getKey();

            productsRef.child(productId).child("productName").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String name = snapshot.exists() ? snapshot.getValue(String.class) : "Unknown";
                    // Jika nama terlalu panjang, potong (truncate)
                    if (name != null && name.length() > 12) name = name.substring(0, 10) + "..";

                    entries.add(new BarEntry(index, count));
                    labels.add(name);
                    fetchedCount[0]++;

                    if (fetchedCount[0] == totalToFetch) {
                        loadProfessionalData(entries, labels);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
            i++;
        }
    }

    private void loadProfessionalData(List<BarEntry> entries, List<String> labels) {
        BarDataSet dataSet = new BarDataSet(entries, "Total Orders");

        // Gunakan satu warna profesional (Contoh: Deep Red atau Blue)
        int startColor = Color.parseColor("#8a2128"); // Warna tema anda
        int endColor = Color.parseColor("#E57373");

        dataSet.setGradientColor(startColor, endColor);
        dataSet.setDrawValues(true); // Tunjukkan angka di atas bar
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(Color.parseColor("#2D3436"));

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.5f); // Kecilkan lebar bar supaya nampak kemas (tidak "gemuk")

        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChart.setData(data);
        barChart.invalidate();
    }
}