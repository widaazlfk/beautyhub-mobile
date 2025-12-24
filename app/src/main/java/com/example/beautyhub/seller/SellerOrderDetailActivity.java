package com.example.beautyhub.seller;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.beautyhub.R;
// Import kelas ViewBinding anda
import com.example.beautyhub.databinding.ActivitySellerOrderDetailBinding;
import com.example.beautyhub.models.Order;
import com.example.beautyhub.models.OrderItem;
import com.example.beautyhub.models.ShippingAddress;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SellerOrderDetailActivity extends AppCompatActivity {

    // 1. Gunakan ViewBinding
    private ActivitySellerOrderDetailBinding binding;

    private String orderId;
    private String subOrderId;
    private DatabaseReference orderRef;
    private Order currentOrder; // Simpan data pesanan semasa

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 2. Inflate layout menggunakan ViewBinding
        binding = ActivitySellerOrderDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        orderId = getIntent().getStringExtra("ORDER_ID");
        subOrderId = getIntent().getStringExtra("SUB_ORDER_ID");

        if (orderId == null || orderId.isEmpty()) {
            Toast.makeText(this, "Error: Main Order ID not found.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // 3. Logik dinamik untuk rujukan pangkalan data
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference("Orders");
        if (subOrderId != null && !subOrderId.isEmpty()) {
            // Ini adalah sub-pesanan
            orderRef = rootRef.child(orderId).child("subOrders").child(subOrderId);
        } else {
            // Ini adalah pesanan ringkas (direct)
            orderRef = rootRef.child(orderId);
        }

        setupToolbar();
        setupButtonListener();
        loadOrderDetails();
    }

    private void setupToolbar() {
        binding.toolbarOrderDetail.setNavigationOnClickListener(v -> finish());
    }

    private void setupButtonListener() {
        binding.btnUpdateStatus.setOnClickListener(v -> showStatusUpdateDialog());
    }

    private void loadOrderDetails() {
        binding.progressBarOrderDetail.setVisibility(View.VISIBLE);
        orderRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                binding.progressBarOrderDetail.setVisibility(View.GONE);
                if (snapshot.exists()) {
                    currentOrder = snapshot.getValue(Order.class);
                    if (currentOrder != null) {
                        // Tetapkan ID secara manual kerana ia adalah key
                        if (subOrderId != null) {
                            currentOrder.setOrderId(subOrderId);
                        } else {
                            currentOrder.setOrderId(orderId);
                        }
                        displayOrderDetails(currentOrder);
                    }
                } else {
                    Toast.makeText(SellerOrderDetailActivity.this, "Order details not found.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.progressBarOrderDetail.setVisibility(View.GONE);
                Log.e("SellerDetail", "Failed to load order: " + error.getMessage());
                Toast.makeText(SellerOrderDetailActivity.this, "Failed to load data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayOrderDetails(Order order) {
        // Papar ID Pesanan dengan selamat
        String displayId = order.getOrderId();
        if (displayId != null && displayId.length() > 8) {
            displayId = displayId.substring(0, 8).toUpperCase();
        }
        binding.tvDetailOrderId.setText(String.format("Order #%s", displayId));

        binding.tvDetailOrderStatus.setText(order.getStatus());

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        binding.tvDetailOrderDate.setText(sdf.format(new Date(order.getTimestamp())));

        updateStatusBackground(order.getStatus());

        // Papar Alamat
        ShippingAddress address = order.getShippingAddress();
        if (address != null) {
            binding.tvShippingName.setText(address.getRecipientName());
            binding.tvShippingPhone.setText(address.getPhoneNumber());
            String fullAddress = address.getStreet() + ", " + address.getCity() + ", " + address.getState() + " " + address.getZipcode();
            binding.tvShippingAddress.setText(fullAddress);
        }

        // 4. Papar item menggunakan OrderItem, bukan CartItem
        binding.containerOrderItems.removeAllViews();
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            for (OrderItem item : order.getItems()) {
                View itemView = LayoutInflater.from(this).inflate(R.layout.item_product_simple, binding.containerOrderItems, false);
                TextView tvItemName = itemView.findViewById(R.id.tv_item_name);
                TextView tvItemQuantity = itemView.findViewById(R.id.tv_item_quantity);
                TextView tvItemPrice = itemView.findViewById(R.id.tv_item_price);

                tvItemName.setText(item.getProductName());
                tvItemQuantity.setText("x" + item.getQuantity());
                tvItemPrice.setText(String.format(Locale.US, "RM%.2f", item.getPrice()));
                binding.containerOrderItems.addView(itemView);
            }
        }

        // Sembunyikan butang jika status selesai
        if ("Completed".equalsIgnoreCase(order.getStatus()) || "Cancelled".equalsIgnoreCase(order.getStatus())) {
            binding.btnUpdateStatus.setVisibility(View.GONE);
        } else {
            binding.btnUpdateStatus.setVisibility(View.VISIBLE);
        }
    }

    private void showStatusUpdateDialog() {
        if (currentOrder == null || currentOrder.getStatus() == null) {
            Toast.makeText(this, "Order data not loaded yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 5. Logik status yang pintar
        String currentStatus = currentOrder.getStatus();
        final String[] availableStatuses;

        switch (currentStatus.toLowerCase(Locale.ROOT)) {
            case "pending":
                availableStatuses = new String[]{"Processing", "Cancelled"};
                break;
            case "processing":
                availableStatuses = new String[]{"Shipped"};
                break;
            case "shipped":
                availableStatuses = new String[]{"Completed"};
                break;
            default:
                Toast.makeText(this, "No further status updates available.", Toast.LENGTH_SHORT).show();
                return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Update Order Status")
                .setItems(availableStatuses, (dialog, which) -> {
                    String selectedStatus = availableStatuses[which];
                    updateOrderStatus(selectedStatus);
                })
                .show();
    }

    private void updateOrderStatus(String newStatus) {
        binding.progressBarOrderDetail.setVisibility(View.VISIBLE);
        // 6. Guna nama medan yang betul: "status"
        orderRef.child("status").setValue(newStatus)
                .addOnSuccessListener(aVoid -> {
                    binding.progressBarOrderDetail.setVisibility(View.GONE);
                    Toast.makeText(this, "Status updated to " + newStatus, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    binding.progressBarOrderDetail.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to update status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateStatusBackground(String status) {
        int colorResId;
        switch (status.toLowerCase(Locale.ROOT)) {
            case "processing": colorResId = R.color.status_processing; break;
            case "shipped": colorResId = R.color.status_shipped; break;
            case "completed": colorResId = R.color.status_completed; break;
            case "cancelled": colorResId = R.color.status_cancelled; break;
            case "pending": default: colorResId = R.color.status_pending; break;
        }
        if (binding.tvDetailOrderStatus.getBackground() instanceof GradientDrawable) {
            GradientDrawable background = (GradientDrawable) binding.tvDetailOrderStatus.getBackground().mutate();
            background.setColor(ContextCompat.getColor(this, colorResId));
        }
    }
}
