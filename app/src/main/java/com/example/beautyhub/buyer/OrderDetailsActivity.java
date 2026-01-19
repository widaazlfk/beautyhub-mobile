package com.example.beautyhub.buyer;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.beautyhub.R;
import com.example.beautyhub.adapters.OrderDetailItemAdapter;
import com.example.beautyhub.databinding.ActivityOrderDetailsBinding;
import com.example.beautyhub.models.Order;
import com.example.beautyhub.models.OrderItem;
import com.example.beautyhub.models.ShippingAddress;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class OrderDetailsActivity extends AppCompatActivity {

    private ActivityOrderDetailsBinding binding;
    private String orderId;
    private DatabaseReference orderRef;
    private Order currentOrder;

    private final ActivityResultLauncher<Intent> addReviewLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    loadOrderDetails();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        orderId = getIntent().getStringExtra("ORDER_ID");

        if (orderId == null || orderId.isEmpty()) {
            Toast.makeText(this, "Error: Order ID missing.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        orderRef = FirebaseDatabase.getInstance().getReference("Orders").child(orderId);

        setupToolbar();
        setupButtonListeners();
        loadOrderDetails();
    }

    private void setupToolbar() {
        binding.toolbarOrderDetail.setNavigationOnClickListener(v -> finish());
    }

    private void loadOrderDetails() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.contentLayout.setVisibility(View.GONE);

        orderRef.get().addOnCompleteListener(task -> {
            binding.progressBar.setVisibility(View.GONE);
            if (task.isSuccessful() && task.getResult().exists()) {
                currentOrder = task.getResult().getValue(Order.class);
                if (currentOrder != null) {
                    currentOrder.setOrderId(task.getResult().getKey());
                    displayOrderData(currentOrder);
                    updateUIBasedOnStatus(currentOrder.getStatus());
                    binding.contentLayout.setVisibility(View.VISIBLE);
                }
            } else {
                Toast.makeText(this, "Order details not found.", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void displayOrderData(Order order) {
        String fullId = order.getOrderId();
        String displayId = "N/A";
        if (fullId != null && !fullId.isEmpty()) {
            // Buang tanda '-' dan ambil 8 aksara terawal
            String cleanId = fullId.replace("-", "");
            displayId = cleanId.substring(0, Math.min(cleanId.length(), 8)).toUpperCase();
        }
        binding.tvDetailOrderId.setText("Order #" + displayId);
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        binding.tvDetailOrderDate.setText("Placed on: " + sdf.format(new Date(order.getOrderDate())));

        // 2. Alamat Penghantaran
        ShippingAddress addr = order.getShippingAddress();
        if (addr != null) {
            String recipientInfo = addr.getRecipientName() + " | " + addr.getPhoneNumber();
            String fullAddress = String.format("%s\n%s, %s, %s, %s",
                    recipientInfo,
                    addr.getStreet(),
                    addr.getCity(),
                    addr.getState(),
                    addr.getZipcode());

            binding.tvDetailShippingAddress.setText(fullAddress);
        }

        // 3. Nama Kedai
        if (order.getSellerName() != null) {
            binding.tvDetailSellerName.setText(order.getSellerName());
        }

        // 4. Senarai Produk & Merchandise Subtotal
        double merchandiseSubtotal = 0;
        if (order.getOrderItems() != null) {
            for (OrderItem item : order.getOrderItems()) {
                merchandiseSubtotal += (item.getPrice() * item.getQuantity());
            }

            OrderDetailItemAdapter adapter = new OrderDetailItemAdapter(
                    this,
                    new ArrayList<>(order.getOrderItems()),
                    order.getOrderId(),
                    order.getStatus(),
                    addReviewLauncher
            );
            binding.rvOrderDetailItems.setLayoutManager(new LinearLayoutManager(this));
            binding.rvOrderDetailItems.setAdapter(adapter);
        }

        // 5. Rumusan Pembayaran (Payment Summary)
        double discount = order.getDiscountAmount();
        // Kira Shipping: Total = (Subtotal + Shipping) - Discount -> Shipping = Total - Subtotal + Discount
        double shippingFee = order.getTotalAmount() - merchandiseSubtotal + discount;

        // Papar kos pos inline (bawah produk)
        binding.tvDetailShippingCostInline.setText(String.format(Locale.US, "RM %.2f", shippingFee));

        // Papar pada Payment Summary
        binding.tvDetailSubtotal.setText(String.format(Locale.US, "RM %.2f", merchandiseSubtotal));
        binding.tvDetailShippingFee.setText(String.format(Locale.US, "RM %.2f", shippingFee));

        // --- LOGIK VOUCHER ---
        if (discount > 0) {
            binding.layoutDetailVoucher.setVisibility(View.VISIBLE);
            binding.tvDetailDiscountAmount.setText(String.format(Locale.US, "- RM %.2f", discount));
        } else {
            binding.layoutDetailVoucher.setVisibility(View.GONE);
        }

        binding.tvPaymentTotal.setText(String.format(Locale.US, "RM %.2f", order.getTotalAmount()));

        // 6. Kaedah Pembayaran (Payment Method)
        if (order.getPaymentMethod() != null) {
            binding.tvDetailPaymentMethod.setText(order.getPaymentMethod());
            if ("Cash on Delivery".equalsIgnoreCase(order.getPaymentMethod())) {
                binding.tvDetailPaymentMethod.setTextColor(ContextCompat.getColor(this, R.color.status_pending));
            } else {
                binding.tvDetailPaymentMethod.setTextColor(ContextCompat.getColor(this, android.R.color.black));
            }
        } else {
            binding.tvDetailPaymentMethod.setText("N/A");
        }
    }

    private void updateUIBasedOnStatus(String status) {
        binding.btnCancelOrder.setVisibility(View.GONE);
        binding.btnOrderReceived.setVisibility(View.GONE);
        binding.btnOrderAgain.setVisibility(View.GONE);

        if (status == null) return;
        updateStatusBadgeStyle(status);

        switch (status) {
            case "Pending":
                binding.btnCancelOrder.setVisibility(View.VISIBLE);
                break;
            case "Shipped":
                binding.btnOrderReceived.setVisibility(View.VISIBLE);
                break;
            case "Completed":
            case "Cancelled":
                binding.btnOrderAgain.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void handleOrderReceived() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Received")
                .setMessage("Are you sure you have received all items from this store?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    orderRef.child("status").setValue("Completed")
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Order Completed!", Toast.LENGTH_SHORT).show();
                                loadOrderDetails();
                            });
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void setupButtonListeners() {
        binding.btnOrderReceived.setOnClickListener(v -> handleOrderReceived());

        binding.btnCancelOrder.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Cancel Order")
                    .setMessage("Are you sure you want to cancel this order?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        orderRef.child("status").setValue("Cancelled")
                                .addOnSuccessListener(aVoid -> loadOrderDetails());
                    })
                    .setNegativeButton("No", null)
                    .show();
        });

        binding.layoutSellerProfile.setOnClickListener(v -> {
            if (currentOrder != null && currentOrder.getSellerId() != null) {
                Intent intent = new Intent(this, ShopViewActivity.class);
                intent.putExtra("SELLER_ID", currentOrder.getSellerId());
                startActivity(intent);
            }
        });
        binding.btnReportSeller.setOnClickListener(v -> {
            if (currentOrder != null) {
                Intent intent = new Intent(OrderDetailsActivity.this, ReportProblemActivity.class);

                // Masukkan data yang diperlukan oleh ReportProblemActivity
                intent.putExtra("REPORT_TYPE", "BUYER_REPORT_SELLER");
                intent.putExtra("TARGET_ID", currentOrder.getSellerId());
                intent.putExtra("TARGET_NAME", currentOrder.getSellerName());

                startActivity(intent);
            } else {
                Toast.makeText(this, "Order data not loaded yet.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateStatusBadgeStyle(String status) {
        if (binding.tvDetailOrderStatus.getBackground() instanceof GradientDrawable) {
            GradientDrawable drawable = (GradientDrawable) binding.tvDetailOrderStatus.getBackground();
            int colorRes = R.color.status_pending;
            if ("Shipped".equals(status)) colorRes = R.color.status_shipped;
            else if ("Completed".equals(status)) colorRes = R.color.status_completed;
            else if ("Cancelled".equals(status)) colorRes = R.color.status_cancelled;

            drawable.setColor(ContextCompat.getColor(this, colorRes));
        }
        binding.tvDetailOrderStatus.setText(status);
    }
}
