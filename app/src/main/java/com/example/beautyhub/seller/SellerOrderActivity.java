package com.example.beautyhub.seller;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button; // Import untuk butang
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher; // Import baru
import androidx.activity.result.contract.ActivityResultContracts; // Import baru
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

public class SellerOrderActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private SellerOrderAdapter adapter;
    private List<Order> orderList;
    private ProgressBar progressBar;
    private TextView tvNoOrders;
    private DatabaseReference ordersRef;
    private FirebaseUser currentUser;
    private ValueEventListener ordersListener;
    private Query sellerOrdersQuery;

    // ▼▼▼ PENAMBAHBAIKAN 1: Gunakan ActivityResultLauncher untuk penyegaran data ▼▼▼
    private final ActivityResultLauncher<Intent> orderDetailLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                // Walaupun kita guna ValueEventListener, ini adalah amalan baik
                // untuk trigger penyegaran data secara manual jika perlu pada masa hadapan.
                // Buat masa ini, biarkan kosong kerana listener masa nyata sudah menguruskannya.
                // Jika anda tukar ke addListenerForSingleValueEvent, anda akan panggil fetchOrders() di sini.
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_orders);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Authentication error. Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Susun atur logik yang lebih kemas
        setupToolbar();
        initViews();
        setupRecyclerView();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Mula mendengar perubahan data apabila activity kelihatan
        fetchOrders();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Penting: Hentikan listener untuk elak kebocoran memori apabila activity tidak kelihatan
        if (sellerOrdersQuery != null && ordersListener != null) {
            sellerOrdersQuery.removeEventListener(ordersListener);
        }
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_seller_orders);
        toolbar.setTitle("Manage Orders");
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        recyclerView = findViewById(R.id.rv_seller_orders);
        progressBar = findViewById(R.id.progress_bar_seller_orders);
        tvNoOrders = findViewById(R.id.tv_no_orders);

        // ▼▼▼ PENAMBAHBAIKAN 2: Tambah listener pada butang di skrin kosong ▼▼▼
        // Pastikan butang ini wujud dalam R.layout.activity_seller_orders
        Button btnGoToDashboard = findViewById(R.id.btn_go_to_dashboard);
        if (btnGoToDashboard != null) {
            btnGoToDashboard.setOnClickListener(v -> finish()); // Kembali ke skrin sebelumnya (papan pemuka)
        }
    }

    private void setupRecyclerView() {
        orderList = new ArrayList<>();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SellerOrderAdapter(this, orderList, order -> {
            // Logik untuk membuka halaman detail pesanan
            Intent intent = new Intent(SellerOrderActivity.this, SellerOrderDetailActivity.class);
            intent.putExtra("ORDER_ID", order.getOrderId());
            // Gunakan launcher untuk memulakan activity
            orderDetailLauncher.launch(intent);
        });
        recyclerView.setAdapter(adapter);
    }

    private void fetchOrders() {
        showLoadingState(true);

        if (ordersRef == null) {
            ordersRef = FirebaseDatabase.getInstance().getReference("Orders");
        }

        // Pertanyaan yang efisien untuk menapis pesanan di sisi pelayan
        sellerOrdersQuery = ordersRef.orderByChild("sellerId").equalTo(currentUser.getUid());

        if (ordersListener == null) {
            ordersListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    orderList.clear();
                    if (dataSnapshot.exists()) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Order order = snapshot.getValue(Order.class);
                            if (order != null) {
                                order.setOrderId(snapshot.getKey());
                                orderList.add(order);
                            }
                        }
                        // Isih mengikut timestamp secara menurun (terbaru dahulu)
                        Collections.sort(orderList, (o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));
                    }
                    updateUI();
                    showLoadingState(false);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    showLoadingState(false);
                    Toast.makeText(SellerOrderActivity.this, "Failed to load orders: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            };
        }
        // Listener akan sentiasa dikemas kini dengan data terbaru secara automatik
        sellerOrdersQuery.addValueEventListener(ordersListener);
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
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (isLoading) {
            recyclerView.setVisibility(View.GONE);
            tvNoOrders.setVisibility(View.GONE);
        }
    }
}
