package com.example.beautyhub.buyer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.beautyhub.R;
import com.example.beautyhub.databinding.ActivityMockPaymentBinding;

import java.util.Locale;

public class MockPaymentActivity extends AppCompatActivity {

    private ActivityMockPaymentBinding binding;
    private double totalPayment = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMockPaymentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 1. Dapatkan jumlah bayaran dari Intent
        totalPayment = getIntent().getDoubleExtra("TOTAL_PAYMENT", 0.0);

        // 2. Sediakan semua komponen UI
        setupToolbar();
        setupPaymentDetails();
        setupBankSpinner();
        setupPayButton();
    }

    private void setupToolbar() {
        // Tetapkan fungsi untuk butang kembali di toolbar
        binding.toolbar.setNavigationOnClickListener(v -> {
            // Pengguna membatalkan pembayaran
            setResult(Activity.RESULT_CANCELED);
            finish();
        });
    }

    private void setupPaymentDetails() {
        // Paparkan jumlah bayaran dalam format RM XX.XX
        binding.tvTotalPayment.setText(String.format(Locale.US, "RM %.2f", totalPayment));
    }

    private void setupBankSpinner() {
        // Senarai bank olok-olok untuk dipilih
        String[] banks = {"Select a bank", "Maybank2u", "CIMB Clicks", "BSN", "Bank Islam"};

        // Cipta adapter untuk Spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, banks);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Tetapkan adapter pada Spinner
        binding.spinnerBanks.setAdapter(adapter);
    }

    private void setupPayButton() {
        binding.btnPay.setOnClickListener(v -> {
            // Panggil fungsi untuk mengesahkan input dan memulakan pembayaran
            processMockPayment();
        });
    }

    private void processMockPayment() {
        // 3. Ambil data dari input pengguna
        String selectedBank = binding.spinnerBanks.getSelectedItem().toString();
        String username = binding.etUsername.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        // 4. Lakukan pengesahan input
        if (binding.spinnerBanks.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select a bank", Toast.LENGTH_SHORT).show();
            return;
        }
        if (username.isEmpty()) {
            binding.layoutUsername.setError("Username cannot be empty");
            return;
        } else {
            binding.layoutUsername.setError(null);
        }
        if (password.isEmpty()) {
            binding.layoutPassword.setError("Password cannot be empty");
            return;
        } else {
            binding.layoutPassword.setError(null);
        }

        // 5. Mulakan simulasi proses pembayaran
        setLoading(true);

        // Gunakan Handler untuk mencipta lengah masa selama 3 saat untuk meniru proses sebenar
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Kod ini akan berjalan selepas 3 saat
            setLoading(false);

            Toast.makeText(this, "Mock Payment Successful!", Toast.LENGTH_LONG).show();

            // 6. Cipta Intent hasil dan hantar kembali data pembayaran
            Intent resultIntent = new Intent();
            // Kita juga boleh hantar balik bank yang dipilih jika perlu
            resultIntent.putExtra("PAYMENT_METHOD_DETAIL", selectedBank);
            setResult(Activity.RESULT_OK, resultIntent);

            // Tutup aktiviti ini dan kembali ke CheckoutActivity
            finish();

        }, 3000); // 3000ms = 3 saat
    }

    private void setLoading(boolean isLoading) {
        // Tunjukkan atau sembunyikan overlay pemuatan
        binding.loadingOverlay.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    // Pastikan pengguna kembali dengan RESULT_CANCELED jika mereka tekan butang back fizikal
    @Override
    public void onBackPressed() {
        setResult(Activity.RESULT_CANCELED);
        super.onBackPressed();
    }
}
