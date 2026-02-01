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
import com.github.mikephil.charting.animation.Easing;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProductsStatisticsActivity extends AppCompatActivity {

    private LineChart ogiveChart;
    private DatabaseReference ordersRef;
    private TextView tvTopProductName, tvTopProductCount, tvOverviewTitle;

    private RecyclerView recyclerView;
    private ProductAdapter adapter;
    private List<ProductStat> filteredProductList = new ArrayList<>();

    // Data Storage
    private List<Order> allOrders2026 = new ArrayList<>();
    private String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_products_statistics);

        initViews();
        setupToolbar();
        setupProfessionalChart();

        ordersRef = FirebaseDatabase.getInstance().getReference("Orders");
        fetchOrderData();
    }

    private void initViews() {
        ogiveChart = findViewById(R.id.products_ogive_chart);
        tvTopProductName = findViewById(R.id.tv_top_product_name);
        tvTopProductCount = findViewById(R.id.tv_top_product_count);
        tvOverviewTitle = findViewById(R.id.tv_overview_title); // Pastikan ID ini ada di XML

        recyclerView = findViewById(R.id.rv_products_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProductAdapter(filteredProductList);
        recyclerView.setAdapter(adapter);

        // Listener: Filter produk bila bulan di graf ditekan
        ogiveChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                int monthIndex = (int) e.getX();
                if (monthIndex >= 0 && monthIndex < monthNames.length) {
                    filterProductsByMonth(monthNames[monthIndex]);
                }
            }

            @Override
            public void onNothingSelected() {
                showAllProductsFullYear();
            }
        });
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_products_statistics);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void setupProfessionalChart() {
        ogiveChart.getDescription().setEnabled(false);
        ogiveChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        ogiveChart.getAxisRight().setEnabled(false);
        ogiveChart.getXAxis().setGranularity(1f);
        ogiveChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(monthNames));
        ogiveChart.getXAxis().setLabelRotationAngle(-45f);
        ogiveChart.animateX(1000, Easing.EaseInOutQuart);
    }

    private void fetchOrderData() {
        ordersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                allOrders2026.clear();
                // Inisialisasi 12 bulan dengan nilai 0
                Map<String, Integer> monthlySalesCount = new LinkedHashMap<>();
                for (String m : monthNames) monthlySalesCount.put(m, 0);

                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    Order order = ds.getValue(Order.class);
                    // Hanya ambil order yang sudah siap/berjaya dan bukan Cancelled
                    if (order != null && "Completed".equalsIgnoreCase(order.getStatus())) {
                        Calendar cal = Calendar.getInstance();
                        cal.setTimeInMillis(order.getOrderDate());

                        if (cal.get(Calendar.YEAR) == 2026) {
                            allOrders2026.add(order);

                            SimpleDateFormat sdf = new SimpleDateFormat("MMM", Locale.getDefault());
                            String monthKey = sdf.format(cal.getTime());

                            // Kira total unit dalam order ini untuk graf
                            int unitsInOrder = 0;
                            DataSnapshot itemsSnapshot = ds.child("orderItems");
                            for (DataSnapshot item : itemsSnapshot.getChildren()) {
                                Integer qty = item.child("quantity").getValue(Integer.class);
                                unitsInOrder += (qty != null) ? qty : 1;
                            }

                            // Tambah ke bulan tersebut
                            if (monthlySalesCount.containsKey(monthKey)) {
                                monthlySalesCount.put(monthKey, monthlySalesCount.get(monthKey) + unitsInOrder);
                            }
                        }
                    }
                }
                updateChart(monthlySalesCount);
                showAllProductsFullYear();
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void filterProductsByMonth(String month) {
        Map<String, Integer> productMap = new HashMap<>();

        for (Order order : allOrders2026) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(order.getOrderDate());
            SimpleDateFormat sdf = new SimpleDateFormat("MMM", Locale.getDefault());

            if (sdf.format(cal.getTime()).equalsIgnoreCase(month)) {
                // Proses items dalam order
                processOrderItems(order, productMap);
            }
        }
        updateUI(productMap, "Market Performance: " + month + " 2026");
    }

    private void showAllProductsFullYear() {
        Map<String, Integer> productMap = new HashMap<>();
        for (Order order : allOrders2026) {
            processOrderItems(order, productMap);
        }
        updateUI(productMap, "Market Performance: Full Year 2026");
    }

    private void processOrderItems(Order order, Map<String, Integer> map) {
        // Check if the list is not null
        if (order.getOrderItems() != null) {
            // Change the loop to iterate through the List of OrderItem
            for (com.example.beautyhub.models.OrderItem item : order.getOrderItems()) {
                String name = item.getProductName();
                int qty = item.getQuantity();
                if (name != null) {
                    map.put(name, map.getOrDefault(name, 0) + qty);
                }
            }
        }
    }
    private void updateUI(Map<String, Integer> map, String title) {
        filteredProductList.clear();
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            filteredProductList.add(new ProductStat(entry.getKey(), entry.getValue()));
        }

        // Susun Ranking (Paling Laris di Atas)
        Collections.sort(filteredProductList, (o1, o2) -> Integer.compare(o2.sales, o1.sales));

        if (tvOverviewTitle != null) tvOverviewTitle.setText(title);

        if (!filteredProductList.isEmpty()) {
            tvTopProductName.setText(filteredProductList.get(0).name);
            tvTopProductCount.setText(filteredProductList.get(0).sales + " Units Sold");
        } else {
            tvTopProductName.setText("No Data");
            tvTopProductCount.setText("0 Units");
        }

        adapter.notifyDataSetChanged();
    }

    private void updateChart(Map<String, Integer> monthlyData) {
        List<Entry> entries = new ArrayList<>();
        int i = 0;
        for (String m : monthNames) {
            entries.add(new Entry(i++, monthlyData.get(m)));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Total Units Sold (2026)");
        dataSet.setColor(Color.parseColor("#8a2128"));
        dataSet.setCircleColor(Color.parseColor("#8a2128"));
        dataSet.setLineWidth(3f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#8a2128"));
        dataSet.setFillAlpha(40);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        ogiveChart.setData(new LineData(dataSet));
        ogiveChart.invalidate();
    }

    // --- Inner Classes ---
    private static class ProductStat {
        String name;
        int sales;
        ProductStat(String name, int sales) { this.name = name; this.sales = sales; }
    }

    private class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {
        private List<ProductStat> list;
        ProductAdapter(List<ProductStat> list) { this.list = list; }

        @NonNull
        @Override
        public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_order, parent, false);
            return new ProductViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
            ProductStat stat = list.get(position);
            holder.tvName.setText(stat.name);
            holder.tvSales.setText(stat.sales + " Units Sold");
            holder.tvRank.setText("Rank #" + (position + 1));
            holder.tvRank.setTextColor(position == 0 ? Color.parseColor("#8a2128") : Color.GRAY);
        }

        @Override public int getItemCount() { return list.size(); }

        class ProductViewHolder extends RecyclerView.ViewHolder {
            TextView tvRank, tvName, tvSales;
            public ProductViewHolder(@NonNull View itemView) {
                super(itemView);
                tvRank = itemView.findViewById(R.id.tv_order_id);
                tvName = itemView.findViewById(R.id.tv_order_customer);
                tvSales = itemView.findViewById(R.id.tv_order_amount);

                // Sembunyikan field yang tidak berkaitan
                if(itemView.findViewById(R.id.tv_order_status) != null)
                    itemView.findViewById(R.id.tv_order_status).setVisibility(View.GONE);
                if(itemView.findViewById(R.id.tv_admin_commission) != null)
                    itemView.findViewById(R.id.tv_admin_commission).setVisibility(View.GONE);
            }
        }
    }
}