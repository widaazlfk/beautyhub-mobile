package com.example.beautyhub.buyer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beautyhub.R;
import com.example.beautyhub.adapters.OrderAdapter;
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
import java.util.Collections;
import java.util.List;

public class MyOrdersActivity extends AppCompatActivity implements OrderAdapter.OnOrderItemClickListener {

    private RecyclerView rvOrders;
    private ProgressBar progressBar;
    private LinearLayout layoutNoOrders;
    private OrderAdapter orderAdapter;
    private List<Order> orderList;

    private DatabaseReference databaseReference;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_orders);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        databaseReference = FirebaseDatabase.getInstance().getReference("Orders");

        setupToolbar();
        initViews();
        setupRecyclerView();

        if (currentUser != null) {
            loadOrders(currentUser.getUid());
        } else {
            Toast.makeText(this, "You must be logged in to view orders.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentUser != null) {
            loadOrders(currentUser.getUid());
        }
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_my_orders);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Orders");
        }

        // TUKAR DI SINI: Guna onBackPressed() bukannya finish() secara langsung
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void initViews() {
        rvOrders = findViewById(R.id.rv_orders);
        progressBar = findViewById(R.id.progress_bar_orders);
        layoutNoOrders = findViewById(R.id.layout_no_orders);

        com.google.android.material.chip.ChipGroup chipGroup = findViewById(R.id.chip_group_status);
        if (chipGroup != null) {
            chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
                if (currentUser != null) {
                    loadOrders(currentUser.getUid());
                }
            });
        }

        Button btnStartShopping = findViewById(R.id.btn_start_shopping_from_orders);
        if (btnStartShopping != null) {
            btnStartShopping.setOnClickListener(v -> {
                Intent intent = new Intent(MyOrdersActivity.this, BuyerActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            });
        }
    }

    private void setupRecyclerView() {
        orderList = new ArrayList<>();
        orderAdapter = new OrderAdapter(this, orderList, this);
        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        rvOrders.setAdapter(orderAdapter);
    }

    private void loadOrders(String userId) {
        showLoadingState();

        com.google.android.material.chip.ChipGroup chipGroup = findViewById(R.id.chip_group_status);
        int checkedId = (chipGroup != null) ? chipGroup.getCheckedChipId() : R.id.chip_all;

        // Query tetap sama
        Query userOrdersQuery = databaseReference.orderByChild("userId").equalTo(userId);

        // TUKAR DISINI: Guna addValueEventListener untuk update automatik (Real-time)
        userOrdersQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                orderList.clear();

                if (snapshot.exists()) {
                    for (DataSnapshot orderSnapshot : snapshot.getChildren()) {
                        Order order = orderSnapshot.getValue(Order.class);
                        if (order != null) {
                            // Set ID Penuh dari Firebase Key
                            order.setOrderId(orderSnapshot.getKey());

                            String status = order.getStatus();

                            // Filter mengikut Chip yang dipilih
                            if (checkedId == R.id.chip_all) {
                                orderList.add(order);
                            } else if (checkedId == R.id.chip_pending && "Pending".equalsIgnoreCase(status)) {
                                orderList.add(order);
                            } else if (checkedId == R.id.chip_processing && "Processing".equalsIgnoreCase(status)) {
                                orderList.add(order);
                            } else if (checkedId == R.id.chip_shipped && "Shipped".equalsIgnoreCase(status)) {
                                orderList.add(order);
                            } else if (checkedId == R.id.chip_completed && "Completed".equalsIgnoreCase(status)) {
                                orderList.add(order);
                            } else if (checkedId == R.id.chip_cancelled && "Cancelled".equalsIgnoreCase(status)) {
                                orderList.add(order);
                            }
                        }
                    }

                    if (!orderList.isEmpty()) {
                        // Susun yang terbaru di atas
                        Collections.sort(orderList, (o1, o2) -> Long.compare(o2.getOrderDate(), o1.getOrderDate()));
                        showDataState();
                    } else {
                        showEmptyState();
                    }
                } else {
                    showEmptyState();
                }
                // Adapter akan refresh secara automatik bila data berubah
                orderAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
            }
        });
    }
    /**
     * PENTING: Apabila klik, kita hantar ID ASLI (PENUH) supaya
     * OrderDetailsActivity boleh cari data dalam Firebase.
     * Logik pemotongan 8 huruf hanya berlaku di dalam ADAPTER untuk paparan sahaja.
     */
    @Override
    public void onOrderItemClick(Order order) {
        if (order != null && order.getOrderId() != null) {
            Intent intent = new Intent(this, OrderDetailsActivity.class);
            // Hantar ID Penuh (contoh: -NklX23847abc...)
            intent.putExtra("ORDER_ID", order.getOrderId());
            startActivity(intent);
        } else {
            Toast.makeText(this, "Order details not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void showLoadingState() {
        progressBar.setVisibility(View.VISIBLE);
        rvOrders.setVisibility(View.GONE);
        layoutNoOrders.setVisibility(View.GONE);
    }

    private void showDataState() {
        progressBar.setVisibility(View.GONE);
        rvOrders.setVisibility(View.VISIBLE);
        layoutNoOrders.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        progressBar.setVisibility(View.GONE);
        rvOrders.setVisibility(View.GONE);
        layoutNoOrders.setVisibility(View.VISIBLE);
    }

}