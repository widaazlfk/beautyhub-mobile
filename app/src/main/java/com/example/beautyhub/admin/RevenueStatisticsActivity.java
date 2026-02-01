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
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RevenueStatisticsActivity extends AppCompatActivity {

    private LineChart lineChart;
    private DatabaseReference ordersRef;
    private TextView tvSummaryTotalRevenue, tvSummaryAdminProfit, tvSummaryOrderCount, tvOverviewTitle;
    private RecyclerView recyclerView;
    private OrderAdapter adapter;
    private List<Order> allOrdersList = new ArrayList<>(); // Semua order 2026
    private List<Order> filteredOrderList = new ArrayList<>(); // Order ikut bulan yang dipilih
    private String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

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
        tvOverviewTitle = findViewById(R.id.tv_overview_title); // Tambah ID ini di XML jika perlu untuk tunjuk bulan apa

        recyclerView = findViewById(R.id.rv_recent_orders);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderAdapter(filteredOrderList);
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
        lineChart.getXAxis().setLabelRotationAngle(-45);

        // --- FILTER APABILA GRAF DITEKAN ---
        lineChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                int monthIndex = (int) e.getX();
                if (monthIndex >= 0 && monthIndex < monthNames.length) {
                    filterDataByMonth(monthNames[monthIndex]);
                }
            }

            @Override
            public void onNothingSelected() {
                // Jika user tekan luar titik, tunjuk balik semua data tahun 2026
                showAllData2026();
            }
        });
    }

    private void fetchRevenueData() {
        ordersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Map<String, Float> monthlyProfitMap = initializeFullYear2026();
                allOrdersList.clear();

                for (DataSnapshot orderSnapshot : dataSnapshot.getChildren()) {
                    Order order = orderSnapshot.getValue(Order.class);
                    if (order != null && "Completed".equalsIgnoreCase(order.getStatus())) {

                        Long timestamp = order.getOrderDate();
                        if (timestamp != null) {
                            Calendar cal = Calendar.getInstance();
                            cal.setTimeInMillis(timestamp);

                            if (cal.get(Calendar.YEAR) == 2026) {
                                allOrdersList.add(order);

                                SimpleDateFormat sdf = new SimpleDateFormat("MMM", Locale.getDefault());
                                String monthKey = sdf.format(cal.getTime());

                                // Simpan profit 10% ke dalam graf
                                float currentProfit = monthlyProfitMap.getOrDefault(monthKey, 0f);
                                monthlyProfitMap.put(monthKey, currentProfit + (float)(order.getTotalAmount() * 0.10));
                            }
                        }
                    }
                }

                loadLineChartData(monthlyProfitMap);
                showAllData2026(); // Secara default tunjuk semua data 2026
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void filterDataByMonth(String monthShortName) {
        filteredOrderList.clear();
        double totalRevenue = 0;
        int count = 0;

        for (Order order : allOrdersList) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(order.getOrderDate());
            SimpleDateFormat sdf = new SimpleDateFormat("MMM", Locale.getDefault());

            if (sdf.format(cal.getTime()).equalsIgnoreCase(monthShortName)) {
                filteredOrderList.add(order);
                totalRevenue += order.getTotalAmount();
                count++;
            }
        }

        tvOverviewTitle.setText("Overview: " + monthShortName + " 2026");
        updateSummaryCards(totalRevenue, count);
        Collections.reverse(filteredOrderList);
        adapter.notifyDataSetChanged();
    }

    private void showAllData2026() {
        filteredOrderList.clear();
        filteredOrderList.addAll(allOrdersList);

        double totalRevenue = 0;
        for (Order o : allOrdersList) totalRevenue += o.getTotalAmount();

        tvOverviewTitle.setText("Overview: Full Year 2026");
        updateSummaryCards(totalRevenue, allOrdersList.size());
        Collections.reverse(filteredOrderList);
        adapter.notifyDataSetChanged();
    }

    private Map<String, Float> initializeFullYear2026() {
        Map<String, Float> fullYear = new java.util.LinkedHashMap<>();
        for (String m : monthNames) fullYear.put(m, 0f);
        return fullYear;
    }

    private void updateSummaryCards(double totalGross, int count) {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("ms", "MY"));
        tvSummaryTotalRevenue.setText(currencyFormat.format(totalGross));
        tvSummaryAdminProfit.setText(currencyFormat.format(totalGross * 0.10));
        tvSummaryOrderCount.setText(String.valueOf(count));
    }

    private void loadLineChartData(Map<String, Float> monthlyProfitMap) {
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;

        for (Map.Entry<String, Float> entry : monthlyProfitMap.entrySet()) {
            entries.add(new Entry(index, entry.getValue()));
            labels.add(entry.getKey());
            index++;
        }

        lineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        lineChart.getXAxis().setLabelCount(labels.size());

        LineDataSet dataSet = new LineDataSet(entries, "2026 Admin Profit (10%)");
        dataSet.setColor(Color.parseColor("#8a2128"));
        dataSet.setCircleColor(Color.parseColor("#8a2128"));
        dataSet.setLineWidth(2.5f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#8a2128"));
        dataSet.setFillAlpha(40);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        lineChart.setData(new LineData(dataSet));
        lineChart.invalidate();
    }

    // --- ADAPTER REMAINS THE SAME ---
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
            holder.tvId.setText("#" + order.getOrderId().toUpperCase());
            holder.tvStatus.setText(order.getStatus());

            // --- KEMASKINI DI SINI ---
            String buyerName = "Guest User";

            // Semak jika ShippingAddress wujud dan recipientName tidak kosong
            if (order.getShippingAddress() != null && order.getShippingAddress().getRecipientName() != null) {
                buyerName = order.getShippingAddress().getRecipientName();
            }
            // Alternatif jika recipientName tiada, guna username
            else if (order.getUsername() != null) {
                buyerName = order.getUsername();
            }

            holder.tvCustomer.setText("Buyer: " + buyerName);
            // --------------------------

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