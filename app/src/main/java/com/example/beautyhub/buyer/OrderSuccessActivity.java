package com.example.beautyhub.buyer;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.beautyhub.R;

public class OrderSuccessActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_success);

        // 1. Inisialisasi semua view dari layout
        TextView tvOrderId = findViewById(R.id.tv_order_id);
        Button btnViewOrders = findViewById(R.id.btn_view_orders);
        Button btnContinueShopping = findViewById(R.id.btn_continue_shopping);

        // 2. Dapatkan ID Pesanan yang dihantar dari CheckoutActivity
        String orderId = getIntent().getStringExtra("ORDER_ID");

        if (orderId != null && !orderId.isEmpty()) {
            tvOrderId.setText(orderId);
        } else {
            // Urus kes di mana ID pesanan mungkin tiada
            tvOrderId.setText("N/A");
            Toast.makeText(this, "Could not retrieve order ID.", Toast.LENGTH_SHORT).show();
        }

        // 3. Sediakan listener untuk butang "View My Orders"
        btnViewOrders.setOnClickListener(v -> {
                    Intent intent = new Intent(OrderSuccessActivity.this, OrderDetailsActivity.class);

                    // Dapatkan semula ID pesanan yang telah diterima oleh aktiviti ini
                    String orderIdToPass = getIntent().getStringExtra("ORDER_ID");
            if (orderIdToPass != null && !orderIdToPass.isEmpty()) {
                // Hantar ID pesanan ke OrderDetailsActivity
                intent.putExtra("ORDER_ID", orderIdToPass);

                // Kosongkan tindanan (back stack)
                intent.addFlags
                        (Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

                startActivity(intent);
                finish(); // Tutup activity ini
            } else {
                // Jika atas sebab tertentu ID pesanan hilang, beritahu pengguna
                Toast.makeText(this, "Cannot view order. Order ID  is missing.", Toast.LENGTH_LONG).show();
            }

            // --- TAMAT PEMBAIKAN ---
        });



        // 4. Sediakan listener untuk butang "Continue Shopping"
        btnContinueShopping.setOnClickListener(v -> {
            // Hantar pengguna kembali ke skrin utama pembeli
            navigateToHome();
        });
    } // <-- Kurungan penutup untuk onCreate() yang betul

    /**
     * Mengambil alih fungsi butang kembali fizikal.
     * Ia menghalang pengguna daripada kembali ke skrin checkout.
     * Sebaliknya, ia menghantar mereka ke aktiviti utama pembeli.
     */
    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        // JANGAN panggil super.onBackPressed(). Ini memastikan kelakuan asal diganti sepenuhnya.
        navigateToHome();
    }

    /**
     * Kaedah bantuan untuk mengemaskan navigasi ke skrin utama.
     * Ia membersihkan tindanan (back stack) dan memulakan BuyerActivity.
     */
    private void navigateToHome() {
        Intent intent = new Intent(OrderSuccessActivity.this, BuyerActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish(); // Tutup activity ini
    }
}
