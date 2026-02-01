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
import com.example.beautyhub.models.Order;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SellersStatisticsActivity extends AppCompatActivity {

    private BarChart barChart;
    private DatabaseReference ordersRef, usersRef;
    private TextView tvTotalSellersCount, tvTopSellerName, tvOverviewTitle;

    private RecyclerView recyclerView;
    private SellerAdapter adapter;
    private List<SellerStat> filteredSellerList = new ArrayList<>();

    // Data storage
    private Map<String, String> sellerNamesMap = new HashMap<>();
    private List<Order> allOrders2026 = new ArrayList<>();
    private String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sellers_statistics);

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
        tvOverviewTitle = findViewById(R.id.tv_overview_title); // Pastikan ID ini ada di XML

        recyclerView = findViewById(R.id.rv_sellers_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SellerAdapter(filteredSellerList);
        recyclerView.setAdapter(adapter);

        // Filter apabila Admin klik pada Bar di graf (Bulan)
        barChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                int monthIndex = (int) e.getX();
                if (monthIndex >= 0 && monthIndex < monthNames.length) {
                    filterSellersByMonth(monthNames[monthIndex]);
                }
            }

            @Override
            public void onNothingSelected() {
                showAllSellersFullYear();
            }
        });
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_sellers_statistics);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void setupProfessionalBarChart() {
        barChart.getDescription().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.animateY(1000);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelRotationAngle(-45);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(monthNames));

        barChart.getAxisRight().setEnabled(false);
        barChart.getAxisLeft().setAxisMinimum(0f);
    }

    private void fetchSellersStatistics() {
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot usersSnapshot) {
                sellerNamesMap.clear();
                for (DataSnapshot ds : usersSnapshot.getChildren()) {
                    String role = ds.child("userType").getValue(String.class);
                    if ("SELLER".equalsIgnoreCase(role)) {
                        sellerNamesMap.put(ds.getKey(), ds.child("username").getValue(String.class));
                    }
                }

                ordersRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot ordersSnapshot) {
                        allOrders2026.clear();
                        Map<String, Float> monthlyProfitMap = new HashMap<>();
                        for (String m : monthNames) monthlyProfitMap.put(m, 0f);

                        for (DataSnapshot ds : ordersSnapshot.getChildren()) {
                            Order order = ds.getValue(Order.class);
                            if (order != null && "Completed".equalsIgnoreCase(order.getStatus())) {
                                Calendar cal = Calendar.getInstance();
                                cal.setTimeInMillis(order.getOrderDate());

                                if (cal.get(Calendar.YEAR) == 2026) {
                                    allOrders2026.add(order);

                                    SimpleDateFormat sdf = new SimpleDateFormat("MMM", Locale.getDefault());
                                    String monthKey = sdf.format(cal.getTime());

                                    // Graf tunjuk Total Profit Admin (10%) ikut bulan
                                    float profit = (float) (order.getTotalAmount() * 0.10);
                                    monthlyProfitMap.put(monthKey, monthlyProfitMap.get(monthKey) + profit);
                                }
                            }
                        }
                        updateChart(monthlyProfitMap);
                        showAllSellersFullYear();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void filterSellersByMonth(String month) {
        Map<String, Float> sellerRevenueMap = new HashMap<>();
        // Init semua seller RM 0
        for (String id : sellerNamesMap.keySet()) sellerRevenueMap.put(id, 0f);

        for (Order order : allOrders2026) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(order.getOrderDate());
            SimpleDateFormat sdf = new SimpleDateFormat("MMM", Locale.getDefault());

            if (sdf.format(cal.getTime()).equalsIgnoreCase(month)) {
                String sId = order.getSellerId();
                if (sId != null && sellerRevenueMap.containsKey(sId)) {
                    // Seller dapat 90%
                    float net = (float) (order.getTotalAmount() * 0.90);
                    sellerRevenueMap.put(sId, sellerRevenueMap.get(sId) + net);
                }
            }
        }
        updateList(sellerRevenueMap, "Overview: " + month + " 2026");
    }

    private void showAllSellersFullYear() {
        Map<String, Float> sellerRevenueMap = new HashMap<>();
        for (String id : sellerNamesMap.keySet()) sellerRevenueMap.put(id, 0f);

        for (Order order : allOrders2026) {
            String sId = order.getSellerId();
            if (sId != null && sellerRevenueMap.containsKey(sId)) {
                float net = (float) (order.getTotalAmount() * 0.90);
                sellerRevenueMap.put(sId, sellerRevenueMap.get(sId) + net);
            }
        }
        updateList(sellerRevenueMap, "Overview: Full Year 2026");
    }

    private void updateList(Map<String, Float> salesMap, String title) {
        filteredSellerList.clear();
        for (Map.Entry<String, String> entry : sellerNamesMap.entrySet()) {
            filteredSellerList.add(new SellerStat(entry.getValue(), salesMap.get(entry.getKey())));
        }
        Collections.sort(filteredSellerList, (o1, o2) -> Float.compare(o2.revenue, o1.revenue));

        tvOverviewTitle.setText(title);
        tvTotalSellersCount.setText(String.valueOf(sellerNamesMap.size()));
        if (!filteredSellerList.isEmpty()) tvTopSellerName.setText(filteredSellerList.get(0).name);

        adapter.notifyDataSetChanged();
    }

    private void updateChart(Map<String, Float> monthlyProfitMap) {
        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < monthNames.length; i++) {
            entries.add(new BarEntry(i, monthlyProfitMap.get(monthNames[i])));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Admin Profit 10% (RM)");
        dataSet.setColor(Color.parseColor("#8a2128"));

        BarData data = new BarData(dataSet);
        barChart.setData(data);
        barChart.invalidate();
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
        SellerAdapter(List<SellerStat> list) { this.list = list; }

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
            holder.tvRank.setTextColor(position == 0 ? Color.parseColor("#8a2128") : Color.GRAY);
        }

        @Override
        public int getItemCount() { return list.size(); }

        class SellerViewHolder extends RecyclerView.ViewHolder {
            TextView tvRank, tvName, tvRevenue;
            public SellerViewHolder(@NonNull View itemView) {
                super(itemView);
                tvRank = itemView.findViewById(R.id.tv_order_id);
                tvName = itemView.findViewById(R.id.tv_order_customer);
                tvRevenue = itemView.findViewById(R.id.tv_order_amount);

                // Hide unnecessary items
                itemView.findViewById(R.id.tv_order_status).setVisibility(View.GONE);
                itemView.findViewById(R.id.tv_admin_commission).setVisibility(View.GONE);
            }
        }
    }
}