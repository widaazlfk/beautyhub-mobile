package com.example.beautyhub.buyer;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.beautyhub.R;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;

public class OrderSuccessActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_success);

        // 1. Inisialisasi View
        TextView tvOrderId = findViewById(R.id.tv_order_id);
        TextView tvPaymentStatus = findViewById(R.id.tv_payment_status);
        MaterialButton btnTrackOrders = findViewById(R.id.btn_view_orders);
        MaterialButton btnBackHome = findViewById(R.id.btn_continue_shopping);

        // 2. Dapatkan maklumat dari Intent
        String paymentMethod = getIntent().getStringExtra("PAYMENT_METHOD");
        ArrayList<String> orderIds = getIntent().getStringArrayListExtra("ORDER_IDS");

        // 3. Logik Status Bayaran
        if ("Online Banking".equalsIgnoreCase(paymentMethod)) {
            tvPaymentStatus.setText("Paid");
            tvPaymentStatus.setTextColor(ContextCompat.getColor(this, R.color.green_success));
        } else if ("Cash on Delivery".equalsIgnoreCase(paymentMethod)) {
            tvPaymentStatus.setText("Pending (Cash on Delivery)");
            tvPaymentStatus.setTextColor(ContextCompat.getColor(this, R.color.status_pending));
        } else {
            tvPaymentStatus.setText("Processing");
        }

        // 4. Logik Paparan Order ID (Single vs Multiple Sellers) - DIKEMASKINI KE 8 HURUF TERAWAL
        if (orderIds != null && !orderIds.isEmpty()) {
            if (orderIds.size() > 1) {
                // Kes Multiple Sellers: Tunjuk jumlah order
                tvOrderId.setText(orderIds.size() + " Orders (Multi-Seller)");
            } else {
                // Kes Single Seller: Ambil ID pertama, buang '-' dan ambil 8 huruf terawal
                String fullId = orderIds.get(0);
                String cleanId = fullId.replace("-", "");
                String shortId = cleanId.substring(0, Math.min(cleanId.length(), 8)).toUpperCase();
                tvOrderId.setText("Order #" + shortId);
            }
        } else {
            // Fallback jika ArrayList kosong
            String backupId = getIntent().getStringExtra("ORDER_ID");
            if (backupId != null) {
                String cleanId = backupId.replace("-", "");
                String shortId = cleanId.substring(0, Math.min(cleanId.length(), 8)).toUpperCase();
                tvOrderId.setText("Order #" + shortId);
            } else {
                tvOrderId.setText("Order #N/A");
            }
        }

        // 5. Listener Track Order
        btnTrackOrders.setOnClickListener(v -> {
            Intent intent = new Intent(OrderSuccessActivity.this, MyOrdersActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // 6. Listener Back to Home
        btnBackHome.setOnClickListener(v -> navigateToHome());
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        navigateToHome();
    }

    private void navigateToHome() {
        Intent intent = new Intent(OrderSuccessActivity.this, BuyerActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}