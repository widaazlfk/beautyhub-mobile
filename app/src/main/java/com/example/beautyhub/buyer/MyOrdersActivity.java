package com.example.beautyhub.buyer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button; // Import untuk Button
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

// ▼▼▼ PERUBAHAN 1: Implement interface dari OrderAdapter ▼▼▼
public class MyOrdersActivity extends AppCompatActivity implements OrderAdapter.OnOrderItemClickListener {

    private RecyclerView rvOrders;
    private ProgressBar progressBar;
    private LinearLayout layoutNoOrders;
    private OrderAdapter orderAdapter;
    private List<Order> orderList;

    private DatabaseReference databaseReference;
    private FirebaseUser currentUser;
    private View chipGroupStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_orders);

        // Inisialisasi Firebase
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        databaseReference = FirebaseDatabase.getInstance().getReference("Orders");

        // Persediaan UI
        setupToolbar();
        initViews();
        setupRecyclerView();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Memuatkan data apabila activity bermula atau kembali ke skrin
        if (currentUser != null) {
            loadOrders(currentUser.getUid());
        } else {
            Toast.makeText(this, "You must be logged in to view orders.", Toast.LENGTH_LONG).show();
            showEmptyState();
        }
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_my_orders);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        rvOrders = findViewById(R.id.rv_orders);
        progressBar = findViewById(R.id.progress_bar_orders);
        layoutNoOrders = findViewById(R.id.layout_no_orders);
        chipGroupStatus = findViewById(R.id.chip_group_status);


        // Tambah listener untuk butang "Start Shopping" pada skrin kosong
        Button btnStartShopping = findViewById(R.id.btn_start_shopping_from_orders);
        btnStartShopping.setOnClickListener(v -> {
            Intent intent = new Intent(MyOrdersActivity.this, BuyerActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });
    }

    private void setupRecyclerView() {
        orderList = new ArrayList<>();
        // ▼▼▼ PERUBAHAN 2: Serahkan 'this' sebagai listener ▼▼▼
        orderAdapter = new OrderAdapter(this, orderList, this);
        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        rvOrders.setAdapter(orderAdapter);
    }

    private void loadOrders(String userId) {
        showLoadingState();

        Query userOrdersQuery = databaseReference.orderByChild("userId").equalTo(userId);

        // ▼▼▼ PERUBAHAN 3: Menggunakan addListenerForSingleValueEvent untuk kecekapan ▼▼▼
        userOrdersQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                orderList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot orderSnapshot : snapshot.getChildren()) {
                        Order order = orderSnapshot.getValue(Order.class);
                        if (order != null) {
                            // Penting: Simpan ID pesanan dari Firebase key
                            order.setOrderId(orderSnapshot.getKey());
                            orderList.add(order);
                        }
                    }
                    // Isih pesanan dari yang terbaru ke terlama
                    Collections.sort(orderList, (o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));
                    showDataState();
                } else {
                    showEmptyState();
                }
                orderAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showEmptyState();
                Toast.makeText(MyOrdersActivity.this, "Failed to load orders: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ▼▼▼ PERUBAHAN 4: Kaedah implementasi dari OnOrderItemClickListener ▼▼▼
    @Override
    public void onOrderItemClick(Order order) {
        Intent intent = new Intent(this, OrderDetailsActivity.class);
        intent.putExtra("ORDER_ID", order.getOrderId()); // Hantar ID Pesanan ke activity seterusnya
        startActivity(intent);
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

    // Kaedah onStop() tidak lagi diperlukan kerana kita menggunakan addListenerForSingleValueEvent
}
