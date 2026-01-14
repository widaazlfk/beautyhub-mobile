package com.example.beautyhub.seller;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class SellerOrderActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private SellerOrderAdapter adapter;
    private List<Order> orderList;
    private ProgressBar progressBar;
    private TextView tvNoOrders;
    private DatabaseReference ordersRef;
    private FirebaseUser currentUser;
    private Query sellerQuery; // Gunakan Query untuk filter
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

        // --- PEMBETULAN DI SINI ---
        // Kita cari semua order yang field 'sellerId' nya sama dengan UID seller sekarang
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
                            // Masukkan ID dari key Firebase jika field orderId kosong
                            if (order.getOrderId() == null) order.setOrderId(snapshot.getKey());

                            orderList.add(order);
                            grandTotal += order.getTotalAmount();
                        }
                    }
                    // Susun ikut tarikh terbaru
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
                Log.e("DATABASE_ERROR", databaseError.getMessage());
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
        if (!isLoading) {
            updateUI();
        }
    }
}