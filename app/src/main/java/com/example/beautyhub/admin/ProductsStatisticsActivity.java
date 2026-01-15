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
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProductsStatisticsActivity extends AppCompatActivity {

    private LineChart ogiveChart;
    private DatabaseReference ordersRef;
    private TextView tvTopProductName, tvTopProductCount;

    private RecyclerView recyclerView;
    private ProductAdapter adapter;
    private List<ProductStat> productList;
    private List<String> fullProductNamesForChart; // Untuk simpan nama penuh bagi Toast

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_products_statistics);

        fullProductNamesForChart = new ArrayList<>();
        initViews();
        setupToolbar();
        setupProfessionalOgiveChart();

        ordersRef = FirebaseDatabase.getInstance().getReference("Orders");
        fetchPopularProducts();
    }

    private void initViews() {
        ogiveChart = findViewById(R.id.products_ogive_chart);
        tvTopProductName = findViewById(R.id.tv_top_product_name);
        tvTopProductCount = findViewById(R.id.tv_top_product_count);

        recyclerView = findViewById(R.id.rv_products_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        productList = new ArrayList<>();
        adapter = new ProductAdapter(productList);
        recyclerView.setAdapter(adapter);

        // Listener: Apabila Admin klik pada titik graf
        ogiveChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                int index = (int) e.getX();
                if (index >= 0 && index < fullProductNamesForChart.size()) {
                    String name = fullProductNamesForChart.get(index);
                    int cumulativeValue = (int) e.getY();
                    Toast.makeText(ProductsStatisticsActivity.this,
                            "Product: " + name + "\nCumulative Sales: " + cumulativeValue,
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onNothingSelected() {
            }
        });
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_products_statistics);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Market Performance");
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void setupProfessionalOgiveChart() {
        ogiveChart.getDescription().setEnabled(false);
        ogiveChart.setDrawGridBackground(false);
        ogiveChart.setExtraOffsets(10f, 10f, 10f, 20f);
        ogiveChart.setTouchEnabled(true);
        ogiveChart.setDragEnabled(true);
        ogiveChart.setScaleEnabled(true);
        ogiveChart.setPinchZoom(true);

        XAxis xAxis = ogiveChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-45f);
        xAxis.setTextColor(Color.DKGRAY);

        YAxis leftAxis = ogiveChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawGridLines(true);

        ogiveChart.getAxisRight().setEnabled(false);
        ogiveChart.animateX(1500, Easing.EaseInOutQuart);
    }

    private void fetchPopularProducts() {
        ordersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) return;

                Map<String, Integer> productCounts = new HashMap<>();
                for (DataSnapshot orderSnapshot : dataSnapshot.getChildren()) {
                    // Hanya kira jika order tidak dibatalkan
                    String status = orderSnapshot.child("status").getValue(String.class);
                    if (!"Cancelled".equalsIgnoreCase(status)) {
                        DataSnapshot itemsSnapshot = orderSnapshot.child("orderItems");
                        for (DataSnapshot itemSnapshot : itemsSnapshot.getChildren()) {
                            String productName = itemSnapshot.child("productName").getValue(String.class);
                            if (productName != null) {
                                productCounts.put(productName, productCounts.getOrDefault(productName, 0) + 1);
                            }
                        }
                    }
                }

                if (!productCounts.isEmpty()) {
                    // Susun produk dari yang paling laris ke kurang laris
                    Map<String, Integer> sortedProducts = productCounts.entrySet()
                            .stream()
                            .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

                    prepareData(sortedProducts);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void prepareData(Map<String, Integer> sortedProducts) {
        productList.clear();
        fullProductNamesForChart.clear();
        List<Entry> chartEntries = new ArrayList<>();
        List<String> chartLabels = new ArrayList<>();

        int index = 0;
        int cumulativeSum = 0;

        for (Map.Entry<String, Integer> entry : sortedProducts.entrySet()) {
            String name = entry.getKey();
            int salesCount = entry.getValue();

            // Masukkan dalam list untuk RecyclerView di bawah
            productList.add(new ProductStat(name, salesCount));

            // Logik Ogive: Ambil 10 produk teratas untuk visual yang bersih
            if (index < 10) {
                cumulativeSum += salesCount;
                chartEntries.add(new Entry(index, (float) cumulativeSum));
                fullProductNamesForChart.add(name); // Simpan nama penuh untuk klik

                // Label pada paksi X (Dipendekkan jika terlalu panjang)
                String displayLabel = name.length() > 10 ? name.substring(0, 8) + ".." : name;
                chartLabels.add(displayLabel);
            }

            // Update KPI Cards (Top 1)
            if (index == 0) {
                tvTopProductName.setText(name);
                tvTopProductCount.setText(salesCount + " Units Sold");
            }
            index++;
        }

        loadChart(chartEntries, chartLabels);
        adapter.notifyDataSetChanged();
    }

    private void loadChart(List<Entry> entries, List<String> labels) {
        LineDataSet dataSet = new LineDataSet(entries, "Cumulative Market Share");

        // Styling Professional
        dataSet.setColor(Color.parseColor("#8a2128"));
        dataSet.setCircleColor(Color.parseColor("#8a2128"));
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(5f);
        dataSet.setDrawCircleHole(true);
        dataSet.setValueTextSize(10f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#8a2128"));
        dataSet.setFillAlpha(40);
        dataSet.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER); // Garisan nampak smooth

        ogiveChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        ogiveChart.getXAxis().setLabelCount(labels.size());

        LineData lineData = new LineData(dataSet);
        ogiveChart.setData(lineData);
        ogiveChart.invalidate();
    }

    // --- Model Class ---
    private static class ProductStat {
        String name;
        int sales;

        ProductStat(String name, int sales) {
            this.name = name;
            this.sales = sales;
        }
    }

    // --- Adapter Class ---
    private class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {
        private List<ProductStat> list;

        ProductAdapter(List<ProductStat> list) {
            this.list = list;
        }

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
            holder.tvSales.setText(stat.sales + " Sold");
            holder.tvRank.setText("Rank #" + (position + 1));

            // Highlight ranking 1
            if (position == 0) holder.tvRank.setTextColor(Color.parseColor("#8a2128"));
            else holder.tvRank.setTextColor(Color.GRAY);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ProductViewHolder extends RecyclerView.ViewHolder {
            TextView tvRank, tvName, tvSales;

            public ProductViewHolder(@NonNull View itemView) {
                super(itemView);
                // 1. Mapping ID
                tvRank = itemView.findViewById(R.id.tv_order_id);
                tvName = itemView.findViewById(R.id.tv_order_customer);
                tvSales = itemView.findViewById(R.id.tv_order_amount);

                // 2. Sembunyikan Status & Tarikh
                View status = itemView.findViewById(R.id.tv_order_status);
                if (status != null) status.setVisibility(View.GONE);

                View date = itemView.findViewById(R.id.tv_order_date);
                if (date != null) date.setVisibility(View.GONE);

                // 3. Sembunyikan bahagian "Earn (10%)"
                View commissionValue = itemView.findViewById(R.id.tv_admin_commission);
                if (commissionValue != null && commissionValue.getParent() instanceof View) {
                    ((View) commissionValue.getParent()).setVisibility(View.GONE);
                }

                // 4. Sembunyikan label "Total Amount" supaya hanya keluar jumlah unit sahaja
                if (tvSales != null && tvSales.getParent() instanceof View) {
                    View container = (View) tvSales.getParent();
                    if (container instanceof ViewGroup) {
                        ViewGroup vg = (ViewGroup) container;
                        if (vg.getChildCount() > 0) {
                            vg.getChildAt(0).setVisibility(View.GONE); // Sorok label statik "Total Amount"
                        }
                    }
                }
            }
        }
    }
}
