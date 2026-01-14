package com.example.beautyhub.admin;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.beautyhub.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class RevenueStatisticsActivity extends AppCompatActivity {

    private static final String TAG = "RevenueStatistics";
    private LineChart lineChart;
    private DatabaseReference ordersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_revenue_statistics);

        setupToolbar();

        lineChart = findViewById(R.id.revenue_line_chart);
        setupLineChart();

        // Inisialisasi rujukan Firebase ke nod "Orders"
        ordersRef = FirebaseDatabase.getInstance().getReference("Orders");
        fetchRevenueData();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_revenue_statistics);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Platform Revenue Analytics");
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
            }

            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }
    }

    private void setupLineChart() {
        lineChart.getDescription().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.setExtraOffsets(10, 10, 10, 20);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelRotationAngle(-45);

        lineChart.getAxisRight().setEnabled(false);
        lineChart.getAxisLeft().setAxisMinimum(0f);
        lineChart.getAxisLeft().setGridColor(Color.LTGRAY);
    }

    private void fetchRevenueData() {
        ordersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    Toast.makeText(RevenueStatisticsActivity.this, "No order data found", Toast.LENGTH_SHORT).show();
                    return;
                }

                // TreeMap memastikan bulan disusun mengikut urutan masa
                Map<String, Float> monthlyRevenue = new TreeMap<>();

                for (DataSnapshot orderSnapshot : dataSnapshot.getChildren()) {
                    // --- PERUBAHAN DI SINI: Guna totalAmount dan orderDate ---
                    Object amountObj = orderSnapshot.child("totalAmount").getValue();
                    Long timestamp = orderSnapshot.child("orderDate").getValue(Long.class);

                    if (amountObj != null && timestamp != null) {
                        float amountValue = 0f;

                        // Handle jika Firebase simpan sebagai Long atau Double
                        if (amountObj instanceof Long) {
                            amountValue = ((Long) amountObj).floatValue();
                        } else if (amountObj instanceof Double) {
                            amountValue = ((Double) amountObj).floatValue();
                        } else if (amountObj instanceof Float) {
                            amountValue = (Float) amountObj;
                        }

                        // Admin Commission 10%
                        float adminCommission = amountValue * 0.10f;

                        // Format tarikh ke bulan (Contoh: Jan 2024)
                        SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
                        String monthKey = sdf.format(new Date(timestamp));

                        // Tambah hasil ke dalam map
                        float currentTotal = monthlyRevenue.containsKey(monthKey) ? monthlyRevenue.get(monthKey) : 0f;
                        monthlyRevenue.put(monthKey, currentTotal + adminCommission);
                    }
                }

                if (monthlyRevenue.isEmpty()) {
                    Toast.makeText(RevenueStatisticsActivity.this, "No valid revenue data found", Toast.LENGTH_SHORT).show();
                } else {
                    loadLineChartData(monthlyRevenue);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to fetch order data: ", databaseError.toException());
                Toast.makeText(RevenueStatisticsActivity.this, "Failed to load statistics", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadLineChartData(Map<String, Float> monthlyRevenue) {
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;

        for (Map.Entry<String, Float> entry : monthlyRevenue.entrySet()) {
            entries.add(new Entry(index, entry.getValue()));
            labels.add(entry.getKey());
            index++;
        }

        lineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));

        LineDataSet dataSet = new LineDataSet(entries, "Admin Earnings (10% Comm)");
        dataSet.setColor(Color.parseColor("#8a2128"));
        dataSet.setCircleColor(Color.parseColor("#8a2128"));
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(5f);
        dataSet.setDrawCircleHole(true);
        dataSet.setCircleHoleColor(Color.WHITE);
        dataSet.setValueTextSize(11f);
        dataSet.setDrawFilled(true);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        dataSet.setFillColor(Color.parseColor("#8a2128"));
        dataSet.setFillAlpha(50);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.animateX(1200);
        lineChart.invalidate();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}