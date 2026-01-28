package com.example.beautyhub.admin;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beautyhub.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SellersStatisticsActivity extends AppCompatActivity {

    private BarChart barChart;
    private DatabaseReference ordersRef, usersRef;
    private TextView tvTotalSellersCount, tvTopSellerName;

    private RecyclerView recyclerView;
    private SellerAdapter adapter;
    private List<SellerStat> sellerList;
    private List<String> fullNamesForChart; // Untuk rujukan klik pada graf

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sellers_statistics);

        fullNamesForChart = new ArrayList<>();
        initViews();
        setupToolbar();
        setupProfessionalBarChart();

        ordersRef = FirebaseDatabase.getInstance().getReference("Orders");
        usersRef = FirebaseDatabase.getInstance().getReference("Users");

        fetchSellersStatistics();
    }

    private void initViews() {
        barChart = findViewById(R.id.sellers_bar_chart);
        tvTotalSellersCount = findViewById(R.id.tv_total_sellers_count);
        tvTopSellerName = findViewById(R.id.tv_top_seller_name);

        recyclerView = findViewById(R.id.rv_sellers_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        sellerList = new ArrayList<>();
        adapter = new SellerAdapter(sellerList);
        recyclerView.setAdapter(adapter);

        // Listener: Apabila Admin klik pada Bar di dalam graf
        barChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                int index = (int) e.getX();
                if (index >= 0 && index < fullNamesForChart.size()) {
                    String fullName = fullNamesForChart.get(index);
                    float revenue = e.getY();
                    Toast.makeText(SellersStatisticsActivity.this,
                            "Seller: " + fullName + "\nTotal Revenue: RM " + String.format("%.2f", revenue),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onNothingSelected() {
            }
        });
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_sellers_statistics);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Seller Analytics");
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void setupProfessionalBarChart() {
        barChart.getDescription().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.animateY(1500);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelRotationAngle(-45);
        xAxis.setTextColor(Color.DKGRAY);

        barChart.getAxisRight().setEnabled(false);
        barChart.getAxisLeft().setAxisMinimum(0f);
        barChart.getAxisLeft().setDrawGridLines(true);

        barChart.setFitBars(true); // Supaya bar tidak rapat ke tepi
    }

    private void fetchSellersStatistics() {
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot usersSnapshot) {
                Map<String, String> sellerNamesMap = new HashMap<>();
                for (DataSnapshot ds : usersSnapshot.getChildren()) {
                    // Berdasarkan input anda, role adalah HURUF BESAR
                    String role = ds.child("userType").getValue(String.class);

                    if ("SELLER".equalsIgnoreCase(role)) {
                        String id = ds.getKey();
                        String name = ds.child("username").getValue(String.class);
                        sellerNamesMap.put(id, name != null ? name : "Unknown Seller");
                    }
                }

                ordersRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot ordersSnapshot) {
                        Map<String, Float> salesMap = new HashMap<>();

                        // Init RM 0 untuk semua seller
                        for (String id : sellerNamesMap.keySet()) {
                            salesMap.put(id, 0f);
                        }

                        // Di dalam method fetchSellersStatistics(), bahagian ordersRef
                        for (DataSnapshot ds : ordersSnapshot.getChildren()) {
                            String status = ds.child("status").getValue(String.class);

                            // HANYA AMBIL YANG COMPLETED SAHAJA
                            if ("Completed".equalsIgnoreCase(status)) {
                                String sId = ds.child("sellerId").getValue(String.class);
                                Object amt = ds.child("totalAmount").getValue();

                                float totalOrderAmount = 0f;
                                if (amt instanceof Double) totalOrderAmount = ((Double) amt).floatValue();
                                else if (amt instanceof Long) totalOrderAmount = ((Long) amt).floatValue();

                                // LOGIK BARU: Tolak 10% komisen platform (Seller dapat 90%)
                                float netSellerRevenue = totalOrderAmount * 0.90f;

                                if (sId != null && salesMap.containsKey(sId)) {
                                    // Simpan nilai yang telah ditolak komisen ke dalam map
                                    salesMap.put(sId, salesMap.get(sId) + netSellerRevenue);
                                }
                            }
                        }
                        prepareFinalData(sellerNamesMap, salesMap);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void prepareFinalData(Map<String, String> namesMap, Map<String, Float> salesMap) {
        sellerList.clear();
        fullNamesForChart.clear();

        for (Map.Entry<String, String> entry : namesMap.entrySet()) {
            sellerList.add(new SellerStat(entry.getValue(), salesMap.get(entry.getKey())));
        }

        // Susun Ranking Teratas ke Bawah
        Collections.sort(sellerList, (o1, o2) -> Float.compare(o2.revenue, o1.revenue));

        tvTotalSellersCount.setText(String.valueOf(sellerList.size()));
        if (!sellerList.isEmpty()) {
            tvTopSellerName.setText(sellerList.get(0).name);
        }

        List<BarEntry> chartEntries = new ArrayList<>();
        List<String> chartLabels = new ArrayList<>();

        // Hanya paparkan top 10 seller dalam carta supaya tidak terlalu sesak
        int limit = Math.min(sellerList.size(), 10);
        for (int i = 0; i < limit; i++) {
            SellerStat stat = sellerList.get(i);
            chartEntries.add(new BarEntry(i, stat.revenue));
            fullNamesForChart.add(stat.name); // Simpan nama penuh

            // Pendekkan nama pada Label Paksi-X
            String shortName = stat.name.length() > 10 ? stat.name.substring(0, 8) + ".." : stat.name;
            chartLabels.add(shortName);
        }

        updateUI(chartEntries, chartLabels);
    }

    private void updateUI(List<BarEntry> entries, List<String> labels) {
        BarDataSet dataSet = new BarDataSet(entries, "Revenue (RM)");
        dataSet.setColors(new int[]{
                Color.parseColor("#8a2128"),
                Color.parseColor("#0984E3"),
                Color.parseColor("#00B894"),
                Color.parseColor("#6C5CE7")
        });
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(Color.BLACK);

        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChart.getXAxis().setLabelCount(labels.size());

        BarData barData = new BarData(dataSet);
        barChart.setData(barData);
        barChart.invalidate();

        adapter.notifyDataSetChanged();
    }

    private static class SellerStat {
        String name;
        float revenue;

        SellerStat(String name, float revenue) {
            this.name = name;
            this.revenue = revenue;
        }
    }

    private class SellerAdapter extends RecyclerView.Adapter<SellerAdapter.SellerViewHolder> {
        private List<SellerStat> list;

        SellerAdapter(List<SellerStat> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public SellerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_order, parent, false);
            return new SellerViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull SellerViewHolder holder, int position) {
            SellerStat stat = list.get(position);
            holder.tvName.setText(stat.name);
            holder.tvRevenue.setText(String.format("RM %.2f", stat.revenue));
            holder.tvRank.setText("Rank #" + (position + 1));

            if (position == 0) holder.tvRank.setTextColor(Color.parseColor("#8a2128"));
            else holder.tvRank.setTextColor(Color.GRAY);

            holder.itemView.setOnClickListener(v -> {
                Toast.makeText(SellersStatisticsActivity.this, "Seller: " + stat.name, Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class SellerViewHolder extends RecyclerView.ViewHolder {
            TextView tvRank, tvName, tvRevenue;

            public SellerViewHolder(@NonNull View itemView) {
                super(itemView);
                // 1. Mapping ID utama
                tvRank = itemView.findViewById(R.id.tv_order_id);
                tvName = itemView.findViewById(R.id.tv_order_customer);
                tvRevenue = itemView.findViewById(R.id.tv_order_amount);

                // 2. Sembunyikan Status & Tarikh
                View status = itemView.findViewById(R.id.tv_order_status);
                if (status != null) status.setVisibility(View.GONE);

                View date = itemView.findViewById(R.id.tv_order_date);
                if (date != null) date.setVisibility(View.GONE);

                // 3. Sembunyikan bahagian "Earn (10%)" (Seluruh kotak kanan)
                View commissionValue = itemView.findViewById(R.id.tv_admin_commission);
                if (commissionValue != null && commissionValue.getParent() instanceof View) {
                    ((View) commissionValue.getParent()).setVisibility(View.GONE);
                }

                // 4. Sembunyikan label statik "Total Amount"
                // Kita cari parent kepada tvRevenue (LinearLayout) dan sorokkan anak pertama (label)
                if (tvRevenue != null && tvRevenue.getParent() instanceof View) {
                    View container = (View) tvRevenue.getParent();
                    if (container instanceof ViewGroup) {
                        ViewGroup vg = (ViewGroup) container;
                        if (vg.getChildCount() > 0) {
                            vg.getChildAt(0).setVisibility(View.GONE);
                        }
                    }
                }
            }
        }
    }
}