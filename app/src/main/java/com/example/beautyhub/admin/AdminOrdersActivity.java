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
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class AdminOrdersActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private OrderAdapter adapter;
    private List<Order> orderList;
    private DatabaseReference ordersRef;
    private String filterStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_orders);

        // Ambil filter dari intent (e.g. "Completed" dikirim dari RevenueStatisticsActivity)
        filterStatus = getIntent().getStringExtra("filterStatus");

        initViews();
        setupToolbar();

        ordersRef = FirebaseDatabase.getInstance().getReference("Orders");
        fetchOrders();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.rv_admin_orders);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        orderList = new ArrayList<>();
        adapter = new OrderAdapter(orderList);
        recyclerView.setAdapter(adapter);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_admin_orders);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            // Dinamik title
            String title = (filterStatus != null) ? filterStatus + " Orders" : "All Orders";
            getSupportActionBar().setTitle(title);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void fetchOrders() {
        Query query;
        if (filterStatus != null) {
            query = ordersRef.orderByChild("status").equalTo(filterStatus);
        } else {
            query = ordersRef;
        }

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                orderList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Order order = ds.getValue(Order.class);
                    if (order != null) {
                        // Extra safety check untuk filter "Completed"
                        if (filterStatus != null && filterStatus.equals("Completed")) {
                            if ("Completed".equalsIgnoreCase(order.getStatus())) {
                                orderList.add(order);
                            }
                        } else {
                            orderList.add(order);
                        }
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // --- RECYCLERVIEW ADAPTER ---
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
            double commission = total * 0.10;

            holder.tvId.setText("#" + order.getOrderId());
            holder.tvStatus.setText(order.getStatus());

            String customerName = (order.getUsername() != null) ? order.getUsername() : "Guest User";
            holder.tvCustomer.setText("Customer: " + customerName);

            // Set nilai RM sahaja (ID berasingan)
            holder.tvAmount.setText(String.format("RM %.2f", total));
            holder.tvCommission.setText(String.format("RM %.2f", commission));

            // Warna tulisan status HITAM mengikut permintaan
            holder.tvStatus.setTextColor(Color.BLACK);

            // Set background status (Opsional: supaya lebih kemas)
            if ("Completed".equalsIgnoreCase(order.getStatus())) {
                holder.tvStatus.setBackgroundColor(Color.parseColor("#E8F5E9")); // Hijau lembut
            } else if ("Cancelled".equalsIgnoreCase(order.getStatus())) {
                holder.tvStatus.setBackgroundColor(Color.parseColor("#FFEBEE")); // Merah lembut
            } else {
                holder.tvStatus.setBackgroundColor(Color.parseColor("#FFF3E0")); // Oren lembut
            }
        }

        @Override
        public int getItemCount() { return list.size(); }

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