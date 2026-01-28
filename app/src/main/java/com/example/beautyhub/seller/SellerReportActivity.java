package com.example.beautyhub.seller;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.beautyhub.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class SellerReportActivity extends AppCompatActivity {
    private Spinner spinnerMonth;
    private TextView tvMonthlyTotal;
    private BarChart barChartReport;
    private Button btnViewReport;
    private DatabaseReference ordersRef;
    private String currentSellerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.s_activity_seller_report);

        // Inisialisasi View
        spinnerMonth = findViewById(R.id.spinner_month);
        tvMonthlyTotal = findViewById(R.id.tv_monthly_total);
        barChartReport = findViewById(R.id.bar_chart_report);
        btnViewReport = findViewById(R.id.btn_view_report);
        // Tambah dalam onCreate SellerReportActivity.java
        ImageView btnBack = findViewById(R.id.btn_back_report);
        btnBack.setOnClickListener(v -> {
            onBackPressed(); // Atau finish();
        });

        currentSellerId = FirebaseAuth.getInstance().getUid();
        ordersRef = FirebaseDatabase.getInstance().getReference("Orders");

        // 1. Setup Spinner untuk senarai bulan
        String[] months = {"January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, months);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMonth.setAdapter(adapter);

        // Set default ke bulan semasa
        Calendar now = Calendar.getInstance();
        spinnerMonth.setSelection(now.get(Calendar.MONTH));

        // 2. Klik butang untuk muat data
        btnViewReport.setOnClickListener(v -> loadMonthlyData(spinnerMonth.getSelectedItemPosition()));

        // Muat data bulan semasa secara automatik semasa mula
        loadMonthlyData(now.get(Calendar.MONTH));

    }

    private void loadMonthlyData(int selectedMonth) {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        // Tentukan jumlah hari dalam bulan yang dipilih
        Calendar cal = Calendar.getInstance();
        cal.set(currentYear, selectedMonth, 1);
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        float[] dailyNetSales = new float[daysInMonth];
        String[] labels = new String[daysInMonth];

        // Reset data awal
        for (int i = 0; i < daysInMonth; i++) {
            labels[i] = String.valueOf(i + 1);
            dailyNetSales[i] = 0;
        }

        ordersRef.orderByChild("sellerId").equalTo(currentSellerId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        double monthlyTotalAmount = 0;

                        if (snapshot.exists()) {
                            for (DataSnapshot ds : snapshot.getChildren()) {
                                String status = ds.child("status").getValue(String.class);
                                Double amount = ds.child("totalAmount").getValue(Double.class);
                                Long timestamp = ds.child("orderDate").getValue(Long.class);

                                // FILTER: Hanya status "Completed" dan Bulan/Tahun yang betul
                                if (amount != null && timestamp != null && "Completed".equalsIgnoreCase(status)) {
                                    cal.setTimeInMillis(timestamp);

                                    if (cal.get(Calendar.MONTH) == selectedMonth && cal.get(Calendar.YEAR) == currentYear) {
                                        int day = cal.get(Calendar.DAY_OF_MONTH);
                                        float netAmount = (float) (amount * 0.90); // Tolak 10% komisen

                                        dailyNetSales[day - 1] += netAmount;
                                        monthlyTotalAmount += netAmount;
                                    }
                                }
                            }
                        }

                        // Update UI Total
                        tvMonthlyTotal.setText(String.format(Locale.US, "Total Net Sales: RM %.2f", monthlyTotalAmount));

                        // Paparkan Graf
                        setupChart(dailyNetSales, labels, spinnerMonth.getSelectedItem().toString());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(SellerReportActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupChart(float[] dataPoints, String[] labels, String monthName) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < dataPoints.length; i++) {
            entries.add(new BarEntry(i, dataPoints[i]));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Daily Sales - " + monthName);
        dataSet.setColor(Color.parseColor("#FF69B4"));
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setDrawValues(false);

        BarData data = new BarData(dataSet);
        barChartReport.setData(data);

        // Konfigurasi X-Axis
        XAxis xAxis = barChartReport.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(10);
        xAxis.setDrawGridLines(false);

        // --- TAMBAH LOGIK KLIK DI SINI ---
        barChartReport.setOnChartValueSelectedListener(new com.github.mikephil.charting.listener.OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(com.github.mikephil.charting.data.Entry e, com.github.mikephil.charting.highlight.Highlight h) {
                // e.getX() memberikan index array (0 = Hari 1, 1 = Hari 2, ...)
                int selectedDay = (int) e.getX() + 1;
                int selectedMonth = spinnerMonth.getSelectedItemPosition(); // 0-11
                int currentYear = Calendar.getInstance().get(Calendar.YEAR);

                // Tunjukkan Toast sebagai maklum balas pantas
                String val = String.format(Locale.getDefault(), "RM %.2f", e.getY());
                Toast.makeText(SellerReportActivity.this, "Date: " + selectedDay + " " + monthName + "\nSales: " + val, Toast.LENGTH_SHORT).show();

                // Hantar data ke SellerOrderActivity dengan penapis (filter)
                Intent intent = new Intent(SellerReportActivity.this, SellerOrderActivity.class);
                intent.putExtra("filter_status", "Completed");
                intent.putExtra("filter_day", selectedDay);
                intent.putExtra("filter_month", selectedMonth);
                intent.putExtra("filter_year", currentYear);
                startActivity(intent);
            }

            @Override
            public void onNothingSelected() {}
        });
        // ---------------------------------

        barChartReport.getAxisRight().setEnabled(false);
        barChartReport.getDescription().setEnabled(false);
        barChartReport.animateY(1000);
        barChartReport.invalidate();
    }
}