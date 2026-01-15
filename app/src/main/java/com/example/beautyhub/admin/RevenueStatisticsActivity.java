package com.example.beautyhub.admin;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beautyhub.R;
import com.example.beautyhub.models.Order;
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

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class RevenueStatisticsActivity extends AppCompatActivity {

    private LineChart lineChart;
    private DatabaseReference ordersRef;
    private TextView tvSummaryTotalRevenue, tvSummaryAdminProfit, tvSummaryOrderCount;
    private RecyclerView recyclerView;
    private OrderAdapter adapter;
    private List<Order> orderList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_revenue_statistics);

        initViews();
        setupToolbar();
        setupLineChart();

        ordersRef = FirebaseDatabase.getInstance().getReference("Orders");
        fetchRevenueData();
    }

    private void initViews() {
        lineChart = findViewById(R.id.revenue_line_chart);
        tvSummaryTotalRevenue = findViewById(R.id.tv_summary_total_revenue);
        tvSummaryAdminProfit = findViewById(R.id.tv_summary_admin_profit);
        tvSummaryOrderCount = findViewById(R.id.tv_summary_order_count);

        // Setup RecyclerView untuk senarai di bawah graf
        recyclerView = findViewById(R.id.rv_recent_orders);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        orderList = new ArrayList<>();
        adapter = new OrderAdapter(orderList);
        recyclerView.setAdapter(adapter);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_revenue_statistics);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupLineChart() {
        lineChart.getDescription().setEnabled(false);
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getXAxis().setGranularity(1f);
    }

    private void fetchRevenueData() {
        ordersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Map<String, Float> monthlyRevenue = new TreeMap<>();
                double totalGrossRevenue = 0;
                int orderCount = 0;
                orderList.clear();

                for (DataSnapshot orderSnapshot : dataSnapshot.getChildren()) {
                    Order order = orderSnapshot.getValue(Order.class);
                    String status = orderSnapshot.child("status").getValue(String.class);

                    // Hanya ambil order yang "Completed"
                    if ("Completed".equalsIgnoreCase(status) && order != null) {
                        orderList.add(order);

                        double amountValue = order.getTotalAmount();
                        totalGrossRevenue += amountValue;
                        orderCount++;

                        // Ambil tarikh untuk graf
                        Long timestamp = orderSnapshot.child("orderDate").getValue(Long.class);
                        if (timestamp != null) {
                            SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
                            String monthKey = sdf.format(new Date(timestamp));
                            float currentTotal = monthlyRevenue.getOrDefault(monthKey, 0f);
                            // Untung admin 10%
                            monthlyRevenue.put(monthKey, currentTotal + (float)(amountValue * 0.10));
                        }
                    }
                }

                // Susun order terbaru di atas sekali
                Collections.reverse(orderList);
                adapter.notifyDataSetChanged();

                updateSummaryCards(totalGrossRevenue, orderCount);
                loadLineChartData(monthlyRevenue);
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateSummaryCards(double totalGross, int count) {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("ms", "MY"));
        tvSummaryTotalRevenue.setText(currencyFormat.format(totalGross));
        tvSummaryAdminProfit.setText(currencyFormat.format(totalGross * 0.10));
        tvSummaryOrderCount.setText(String.valueOf(count));
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
        LineDataSet dataSet = new LineDataSet(entries, "Monthly Admin Profit (10%)");
        dataSet.setColor(Color.parseColor("#8a2128"));
        dataSet.setLineWidth(2.5f);
        dataSet.setCircleColor(Color.parseColor("#8a2128"));
        dataSet.setDrawFilled(true);
        dataSet.setFillAlpha(40);
        dataSet.setFillColor(Color.parseColor("#8a2128"));

        lineChart.setData(new LineData(dataSet));
        lineChart.animateX(1000);
        lineChart.invalidate();
    }

    // --- ADAPTER UNTUK SENARAI DI BAWAH GRAF ---
    private class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {
        private List<Order> list;
        public OrderAdapter(List<Order> list) { this.list = list; }

        @NonNull
        @Override
        public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_order, parent, false);
            return new OrderViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
            Order order = list.get(position);
            double total = order.getTotalAmount();

            // 1. PAPAR ORDER ID PENUH (Jangan guna substring supaya Admin senang cari)
            holder.tvId.setText("#" + order.getOrderId().toUpperCase());

            holder.tvStatus.setText(order.getStatus());
            holder.tvStatus.setTextColor(Color.BLACK);

            // 2. AMBIL NAMA BUYER DARI SHIPPING ADDRESS (Sebab data buyer ada di sini)
            String buyerName = "Guest User";
            if (order.getShippingAddress() != null && order.getShippingAddress().getRecipientName() != null) {
                buyerName = order.getShippingAddress().getRecipientName();
            } else if (order.getUsername() != null) {
                buyerName = order.getUsername();
            }

            holder.tvCustomer.setText("Buyer: " + buyerName);

            holder.tvAmount.setText(String.format("RM %.2f", total));
            holder.tvCommission.setText(String.format("RM %.2f", total * 0.10));
        }

        @Override public int getItemCount() { return list.size(); }

        class OrderViewHolder extends RecyclerView.ViewHolder {
            TextView tvId, tvStatus, tvCustomer, tvAmount, tvCommission;
            public OrderViewHolder(@NonNull View itemView) {
                super(itemView);
                tvId = itemView.findViewById(R.id.tv_order_id);
                tvStatus = itemView.findViewById(R.id.tv_order_status);
                tvCustomer = itemView.findViewById(R.id.tv_order_customer);
                tvAmount = itemView.findViewById(R.id.tv_order_amount);
                tvCommission = itemView.findViewById(R.id.tv_admin_commission);
            }
        }
    }
}