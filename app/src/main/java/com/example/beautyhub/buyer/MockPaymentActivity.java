package com.example.beautyhub.buyer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log; // JANGAN LUPA IMPORT INI
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("TOTAL_PAYMENT")) {
            totalPayment = intent.getDoubleExtra("TOTAL_PAYMENT", 0.0);
            Log.d("MockPayment", "Received total payment: " + totalPayment);
        } else if (intent != null && intent.hasExtra("TOTAL_AMOUNT")) {
            // Fallback untuk keserasian
            totalPayment = intent.getDoubleExtra("TOTAL_AMOUNT", 0.0);
            Log.d("MockPayment", "Received TOTAL_AMOUNT as fallback: " + totalPayment);
        } else {
            Toast.makeText(this, "Error: No payment amount received", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 2. Sediakan semua komponen UI
        setupToolbar();
        setupPaymentDetails();
        setupBankSpinner();
        setupPayButton();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> {
            setResult(Activity.RESULT_CANCELED);
            finish();
        });
    }

    private void setupPaymentDetails() {
        // PERBETULAN: Hanya ada tv_total_payment dalam XML
        if (totalPayment > 0) {
            binding.tvTotalPayment.setText(String.format(Locale.US, "RM %.2f", totalPayment));
        } else {
            binding.tvTotalPayment.setText("RM 0.00");
            Toast.makeText(this, "Payment amount is invalid", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupBankSpinner() {
        // Senarai bank olok-olok untuk dipilih
        String[] banks = {"Select a bank", "Maybank2u", "CIMB Clicks", "Public Bank", "RHB Bank", "Hong Leong Bank"};

        // Gunakan ArrayAdapter biasa (tidak perlu Material Components)
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                banks
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Tetapkan adapter pada Spinner
        binding.spinnerBanks.setAdapter(adapter);
    }

    private void setupPayButton() {
        binding.btnPay.setOnClickListener(v -> {
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
            binding.etUsername.requestFocus();
            return;
        } else {
            binding.layoutUsername.setError(null);
        }

        if (password.isEmpty()) {
            binding.layoutPassword.setError("Password cannot be empty");
            binding.etPassword.requestFocus();
            return;
        } else {
            binding.layoutPassword.setError(null);
        }

        if (totalPayment <= 0) {
            Toast.makeText(this, "Invalid payment amount", Toast.LENGTH_SHORT).show();
            return;
        }

        // 5. Mulakan simulasi proses pembayaran
        setLoading(true);

        Toast.makeText(this,
                "Processing payment of RM" + String.format(Locale.US, "%.2f", totalPayment) + "...",
                Toast.LENGTH_SHORT).show();

        // Gunakan Handler untuk mencipta lengah masa selama 3 saat
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            setLoading(false);

            String successMessage = String.format(Locale.US,
                    "Payment Successful!\nAmount: RM%.2f\nBank: %s",
                    totalPayment, selectedBank);

            Toast.makeText(this, successMessage, Toast.LENGTH_LONG).show();

            // 6. Cipta Intent hasil
            Intent resultIntent = new Intent();
            resultIntent.putExtra("PAID_AMOUNT", totalPayment);
            resultIntent.putExtra("PAYMENT_METHOD_DETAIL", selectedBank);
            setResult(Activity.RESULT_OK, resultIntent);

            finish();

        }, 3000);
    }

    private void setLoading(boolean isLoading) {
        binding.loadingOverlay.setVisibility(isLoading ? View.VISIBLE : View.GONE);

        binding.btnPay.setEnabled(!isLoading);
        binding.spinnerBanks.setEnabled(!isLoading);
        binding.etUsername.setEnabled(!isLoading);
        binding.etPassword.setEnabled(!isLoading);

        if (isLoading) {
            binding.btnPay.setText("Processing...");
        } else {
            binding.btnPay.setText("Pay Now");
        }
    }

    @Override
    public void onBackPressed() {
        setResult(Activity.RESULT_CANCELED);
        super.onBackPressed();
    }
}