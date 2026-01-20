package com.example.beautyhub.buyer;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.example.beautyhub.R;
import com.google.android.material.button.MaterialButton;

public class OrderSuccessActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_success);

        // 1. Initialize Views (Order ID references removed)
        TextView tvPaymentStatus = findViewById(R.id.tv_payment_status);
        MaterialButton btnTrackOrders = findViewById(R.id.btn_view_orders);
        MaterialButton btnBackHome = findViewById(R.id.btn_continue_shopping);

        // 2. Get Payment Method
        String paymentMethod = getIntent().getStringExtra("PAYMENT_METHOD");

        // 3. Simple Payment Status Logic
        if ("Online Banking".equalsIgnoreCase(paymentMethod)) {
            tvPaymentStatus.setText("Paid");
            tvPaymentStatus.setTextColor(ContextCompat.getColor(this, R.color.green_success));
        } else if ("Cash on Delivery".equalsIgnoreCase(paymentMethod)) {
            tvPaymentStatus.setText("Pending (Cash on Delivery)");
            tvPaymentStatus.setTextColor(ContextCompat.getColor(this, R.color.status_pending));
        } else {
            tvPaymentStatus.setText("Processing");
        }

        // 4. View Orders
        btnTrackOrders.setOnClickListener(v -> {
            Intent intent = new Intent(this, MyOrdersActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        // 5. Back to Home
        btnBackHome.setOnClickListener(v -> navigateToHome());
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        navigateToHome();
    }

    private void navigateToHome() {
        Intent intent = new Intent(this, BuyerActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}