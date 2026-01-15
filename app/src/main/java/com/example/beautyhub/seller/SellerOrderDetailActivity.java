package com.example.beautyhub.seller;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beautyhub.R;
import com.example.beautyhub.adapters.SellerOrderItemAdapter;
import com.example.beautyhub.buyer.ReportProblemActivity;
import com.example.beautyhub.models.Order;
import com.example.beautyhub.models.OrderItem;
import com.example.beautyhub.models.ShippingAddress;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SellerOrderDetailActivity extends AppCompatActivity {

    private String orderId;
    private TextView tvOrderId, tvOrderStatus, tvOrderDate,
            tvShippingName, tvShippingPhone, tvShippingAddress,
            tvPaymentMethod, tvTotalAmount, tvSubtotal, tvShippingFee;
    private RecyclerView rvItems;
    private MaterialButton btnUpdateStatus, btnReportBuyer;
    private ProgressBar progressBar;
    private DatabaseReference orderRef;
    private SellerOrderItemAdapter itemAdapter;
    private Order currentOrder; // To store order data for reporting
    private static final String TAG = "SellerOrderDetail";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_order_detail);

        // Get Order ID from Intent
        orderId = getIntent().getStringExtra("ORDER_ID");

        initViews();
        setupToolbar();

        if (orderId != null) {
            fetchOrderDetails();
        } else {
            Toast.makeText(this, "Order ID not found", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnUpdateStatus.setOnClickListener(v -> showUpdateStatusDialog());

        // Logik untuk lapor Buyer
        btnReportBuyer.setOnClickListener(v -> {
            if (currentOrder != null) {
                Intent intent = new Intent(SellerOrderDetailActivity.this, ReportProblemActivity.class);
                intent.putExtra("REPORT_TYPE", "SELLER_REPORT_BUYER");
                intent.putExtra("TARGET_ID", currentOrder.getUserId()); // ID Buyer

                // Guna recipient name dari shipping address sebagai target name
                String buyerName = "Buyer";
                if (currentOrder.getShippingAddress() != null) {
                    buyerName = currentOrder.getShippingAddress().getRecipientName();
                }
                intent.putExtra("TARGET_NAME", buyerName);

                startActivity(intent);
            } else {
                Toast.makeText(this, "Order data is still loading...", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initViews() {
        tvOrderId = findViewById(R.id.tv_detail_order_id);
        tvOrderStatus = findViewById(R.id.tv_detail_order_status);
        tvOrderDate = findViewById(R.id.tv_detail_order_date);
        tvShippingName = findViewById(R.id.tv_shipping_name);
        tvShippingPhone = findViewById(R.id.tv_shipping_phone);
        tvShippingAddress = findViewById(R.id.tv_shipping_address);
        tvPaymentMethod = findViewById(R.id.tv_detail_payment_method);
        tvTotalAmount = findViewById(R.id.tv_detail_total_amount);
        tvSubtotal = findViewById(R.id.tv_detail_subtotal);
        tvShippingFee = findViewById(R.id.tv_detail_shipping_fee);
        rvItems = findViewById(R.id.rv_sub_order_items);
        btnUpdateStatus = findViewById(R.id.btn_update_status);
        btnReportBuyer = findViewById(R.id.btn_report_buyer);
        progressBar = findViewById(R.id.progress_bar_order_detail);

        rvItems.setLayoutManager(new LinearLayoutManager(this));
        rvItems.setHasFixedSize(true);
    }

    private void fetchOrderDetails() {
        progressBar.setVisibility(View.VISIBLE);
        orderRef = FirebaseDatabase.getInstance().getReference("Orders").child(orderId);

        orderRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing()) return;
                progressBar.setVisibility(View.GONE);

                if (snapshot.exists()) {
                    currentOrder = snapshot.getValue(Order.class);
                    if (currentOrder != null) {
                        currentOrder.setOrderId(snapshot.getKey());
                        displayGeneralInfo(currentOrder, snapshot.getKey());

                        String status = snapshot.child("status").getValue(String.class);
                        tvOrderStatus.setText(status != null ? status : "Pending");
                        updateStatusUI(status);

                        List<OrderItem> itemList = new ArrayList<>();
                        double merchandiseSubtotal = 0;

                        DataSnapshot itemsSnap = snapshot.child("orderItems");
                        if (itemsSnap.exists()) {
                            for (DataSnapshot itemDoc : itemsSnap.getChildren()) {
                                OrderItem item = itemDoc.getValue(OrderItem.class);
                                if (item != null) {
                                    itemList.add(item);
                                    merchandiseSubtotal += (item.getPrice() * item.getQuantity());
                                }
                            }
                        }

                        double totalAmount = parseToDouble(snapshot.child("totalAmount").getValue());
                        double shippingFee = totalAmount - merchandiseSubtotal;
                        if (shippingFee < 0) shippingFee = 0;

                        tvSubtotal.setText(String.format(Locale.US, "RM %.2f", merchandiseSubtotal));
                        tvShippingFee.setText(String.format(Locale.US, "RM %.2f", shippingFee));
                        tvTotalAmount.setText(String.format(Locale.US, "RM %.2f", totalAmount));

                        if (!itemList.isEmpty()) {
                            itemAdapter = new SellerOrderItemAdapter(SellerOrderDetailActivity.this, itemList);
                            rvItems.setAdapter(itemAdapter);
                        }
                    }
                } else {
                    Toast.makeText(SellerOrderDetailActivity.this, "Order data not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void displayGeneralInfo(Order order, String key) {
        String shortId = key.substring(Math.max(0, key.length() - 7)).toUpperCase();
        tvOrderId.setText("Order #" + shortId);
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
        tvOrderDate.setText("Placed on: " + sdf.format(new Date(order.getOrderDate())));
        tvPaymentMethod.setText(order.getPaymentMethod() != null ? order.getPaymentMethod().toUpperCase() : "N/A");

        if (order.getShippingAddress() != null) {
            ShippingAddress addr = order.getShippingAddress();
            tvShippingName.setText(addr.getRecipientName());
            tvShippingPhone.setText(addr.getPhoneNumber());
            tvShippingAddress.setText(String.format("%s, %s, %s, %s",
                    addr.getStreet(), addr.getCity(), addr.getState(), addr.getZipcode()));
        }
    }

    private void updateStatusUI(String status) {
        if (status == null) return;
        btnUpdateStatus.setVisibility((status.equalsIgnoreCase("Completed") || status.equalsIgnoreCase("Cancelled")) ? View.GONE : View.VISIBLE);

        int bgRes;
        switch (status.toLowerCase()) {
            case "shipped": bgRes = R.drawable.bg_order_status_shipped; break;
            case "completed": bgRes = R.drawable.bg_order_status_completed; break;
            case "cancelled": bgRes = R.drawable.bg_order_status_cancelled; break;
            default: bgRes = R.drawable.bg_order_status_pending; break;
        }
        tvOrderStatus.setBackgroundResource(bgRes);
    }

    private double parseToDouble(Object val) {
        if (val instanceof Number) return ((Number) val).doubleValue();
        if (val instanceof String) {
            try { return Double.parseDouble((String) val); } catch (Exception e) { return 0.0; }
        }
        return 0.0;
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_order_detail);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void showUpdateStatusDialog() {
        String[] statuses = {"Pending", "Processing", "Shipped", "Completed", "Cancelled"};
        new AlertDialog.Builder(this)
                .setTitle("Update Order Status")
                .setItems(statuses, (dialog, which) -> updateOrderStatusInFirebase(statuses[which]))
                .show();
    }

    private void updateOrderStatusInFirebase(String newStatus) {
        if (orderRef == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);

        orderRef.updateChildren(updates).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Status updated!", Toast.LENGTH_SHORT).show();
            sendNotificationToBuyer(newStatus);
        });
    }

    private void sendNotificationToBuyer(String newStatus) {
        orderRef.get().addOnSuccessListener(snapshot -> {
            String buyerId = snapshot.child("userId").getValue(String.class);
            String sellerName = snapshot.child("sellerName").getValue(String.class);

            DataSnapshot orderItemsSnap = snapshot.child("orderItems");
            String firstProductName = "";
            String firstProductImage = "";

            if (orderItemsSnap.exists()) {
                for (DataSnapshot itemDs : orderItemsSnap.getChildren()) {
                    firstProductName = itemDs.child("productName").getValue(String.class);
                    firstProductImage = itemDs.child("productImage").getValue(String.class);
                    break;
                }
            }

            if (buyerId != null) {
                DatabaseReference notifRef = FirebaseDatabase.getInstance().getReference("Notifications").child(buyerId).push();
                String shortOrderId = orderId.substring(Math.max(0, orderId.length() - 7)).toUpperCase();

                HashMap<String, Object> notifData = new HashMap<>();
                notifData.put("id", notifRef.getKey());
                notifData.put("title", "Order Status: " + newStatus);
                notifData.put("message", "Your order #" + shortOrderId + " from " + sellerName + " is now " + newStatus);
                notifData.put("timestamp", System.currentTimeMillis());
                notifData.put("type", "OrderStatusUpdate");
                notifData.put("productName", firstProductName);
                notifData.put("sellerName", sellerName);
                notifData.put("productImageUrl", firstProductImage);
                notifData.put("orderId", orderId);
                notifData.put("unread", true);

                notifRef.setValue(notifData);
            }
        });
    }
}