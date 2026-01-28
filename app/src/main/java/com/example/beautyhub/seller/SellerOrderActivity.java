package com.example.beautyhub.seller;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beautyhub.R;
import com.example.beautyhub.adapters.SellerOrderAdapter;
import com.example.beautyhub.models.Order;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class SellerOrderActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private SellerOrderAdapter adapter;
    private List<Order> orderList;
    private ProgressBar progressBar;
    private TextView tvNoOrders, tvFilterInfo;
    private DatabaseReference ordersRef;
    private FirebaseUser currentUser;
    private Query sellerQuery;
    private ValueEventListener ordersListener;
    private TextView tvTotalAmount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_orders);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupToolbar();
        initViews();
        setupRecyclerView();
    }

    @Override
    protected void onStart() {
        super.onStart();
        fetchOrders();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (sellerQuery != null && ordersListener != null) {
            sellerQuery.removeEventListener(ordersListener);
        }
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_seller_orders);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Manage Orders");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        recyclerView = findViewById(R.id.rv_seller_orders);
        progressBar = findViewById(R.id.progress_bar_seller_orders);
        tvNoOrders = findViewById(R.id.tv_no_orders);
        tvTotalAmount = findViewById(R.id.tv_detail_total_amount);
        tvFilterInfo = findViewById(R.id.tv_filter_info);

        com.google.android.material.chip.ChipGroup chipGroup = findViewById(R.id.chip_group_seller_status);
        if (chipGroup != null) {
            chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
                // Apabila chip ditekan secara manual, kita reset filter dari intent graf
                getIntent().removeExtra("filter_day");
                getIntent().removeExtra("filter_month");
                getIntent().removeExtra("filter_year");
                getIntent().removeExtra("filter_status");
                fetchOrders();
            });
        }

        Button btnGoToDashboard = findViewById(R.id.btn_go_to_dashboard);
        if (btnGoToDashboard != null) {
            btnGoToDashboard.setOnClickListener(v -> finish());
        }
    }

    private void setupRecyclerView() {
        orderList = new ArrayList<>();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SellerOrderAdapter(this, orderList, order -> {
            Intent intent = new Intent(SellerOrderActivity.this, SellerOrderDetailActivity.class);
            intent.putExtra("ORDER_ID", order.getOrderId());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);
    }

    private void fetchOrders() {
        showLoadingState(true);
        ordersRef = FirebaseDatabase.getInstance().getReference("Orders");

        // Ambil data filter daripada Intent
        int filterDay = getIntent().getIntExtra("filter_day", -1);
        int filterMonth = getIntent().getIntExtra("filter_month", -1);
        int filterYear = getIntent().getIntExtra("filter_year", -1);
        String filterStatus = getIntent().getStringExtra("filter_status");

        // Update UI Info Filter
        if (tvFilterInfo != null) {
            if (filterDay != -1) {
                Calendar c = Calendar.getInstance();
                c.set(Calendar.MONTH, filterMonth);
                String monthName = c.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
                tvFilterInfo.setText("Filtered: " + filterDay + " " + monthName + " " + filterYear + " (" + filterStatus + ")");
                tvFilterInfo.setVisibility(View.VISIBLE);
            } else {
                tvFilterInfo.setVisibility(View.GONE);
            }
        }

        com.google.android.material.chip.ChipGroup chipGroup = findViewById(R.id.chip_group_seller_status);
        int checkedId = (chipGroup != null) ? chipGroup.getCheckedChipId() : R.id.chip_seller_all;

        sellerQuery = ordersRef.orderByChild("sellerId").equalTo(currentUser.getUid());

        ordersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                orderList.clear();
                double grandTotal = 0.0;

                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Order order = snapshot.getValue(Order.class);
                        if (order != null) {
                            if (order.getOrderId() == null) order.setOrderId(snapshot.getKey());

                            String status = order.getStatus();
                            long timestamp = order.getOrderDate();

                            Calendar cal = Calendar.getInstance();
                            cal.setTimeInMillis(timestamp);

                            boolean matchesFilter = false;

                            // 1. Logik Drill-down dari Graf (Filter Tarikh Tepat)
                            if (filterDay != -1) {
                                if (cal.get(Calendar.DAY_OF_MONTH) == filterDay &&
                                        cal.get(Calendar.MONTH) == filterMonth &&
                                        cal.get(Calendar.YEAR) == filterYear &&
                                        status.equalsIgnoreCase(filterStatus)) {
                                    matchesFilter = true;
                                }
                            }
                            // 2. Logik Chip Group (Filter Status Biasa)
                            else {
                                if (checkedId == R.id.chip_seller_all || checkedId == -1) {
                                    matchesFilter = true;
                                } else if (checkedId == R.id.chip_seller_pending && "Pending".equalsIgnoreCase(status)) {
                                    matchesFilter = true;
                                } else if (checkedId == R.id.chip_seller_processing && "Processing".equalsIgnoreCase(status)) {
                                    matchesFilter = true;
                                } else if (checkedId == R.id.chip_seller_shipped && "Shipped".equalsIgnoreCase(status)) {
                                    matchesFilter = true;
                                } else if (checkedId == R.id.chip_seller_completed && "Completed".equalsIgnoreCase(status)) {
                                    matchesFilter = true;
                                } else if (checkedId == R.id.chip_seller_cancelled && "Cancelled".equalsIgnoreCase(status)) {
                                    matchesFilter = true;
                                }
                            }

                            if (matchesFilter) {
                                orderList.add(order);
                                grandTotal += order.getTotalAmount();
                            }
                        }
                    }
                    // Susun paling baru di atas
                    Collections.sort(orderList, (o1, o2) -> Long.compare(o2.getOrderDate(), o1.getOrderDate()));
                }

                if (tvTotalAmount != null) {
                    tvTotalAmount.setText(String.format(Locale.US, "RM %.2f", grandTotal));
                }

                updateUI();
                showLoadingState(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                showLoadingState(false);
            }
        };
        sellerQuery.addValueEventListener(ordersListener);
    }

    private void updateUI() {
        if (orderList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            tvNoOrders.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            tvNoOrders.setVisibility(View.GONE);
        }
        adapter.notifyDataSetChanged();
    }

    private void showLoadingState(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }
}