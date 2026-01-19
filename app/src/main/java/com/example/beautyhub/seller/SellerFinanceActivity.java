package com.example.beautyhub.seller;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beautyhub.R;
import com.example.beautyhub.adapters.FinanceAdapter;
import com.example.beautyhub.models.FinanceModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class SellerFinanceActivity extends AppCompatActivity {

    private TextView tvAvailableBalance, tvPendingBalance;
    private Button btnWithdraw;
    private RecyclerView rvTransactions;
    private LinearLayout layoutNoTransactions;

    private DatabaseReference ordersRef, walletRef;
    private String currentSellerId;
    private double currentAvailableBalance = 0.0;

    // History variables
    private FinanceAdapter financeAdapter;
    private List<FinanceModel> financeList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_finance);

        currentSellerId = FirebaseAuth.getInstance().getUid();

        initViews();
        setupFirebase();
        loadFinanceData();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar_finance);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Finance");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        tvAvailableBalance = findViewById(R.id.tv_wallet_balance);
        tvPendingBalance = findViewById(R.id.tv_pending_balance);
        rvTransactions = findViewById(R.id.rv_transactions);
        layoutNoTransactions = findViewById(R.id.layout_no_transactions);

        // Setup RecyclerView
        // Dalam initViews()
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        financeList = new ArrayList<>();

// Tambah logic klik di sini
        financeAdapter = new FinanceAdapter(financeList, orderId -> {
            // Buka activity butiran pesanan
            Intent intent = new Intent(SellerFinanceActivity.this, SellerOrderDetailActivity.class);
            intent.putExtra("orderId", orderId);
            startActivity(intent);
        });

        rvTransactions.setAdapter(financeAdapter);
        // Withdrawal Button logic (Add this if you have a btn_withdraw in XML)
        if (btnWithdraw != null) {
            btnWithdraw.setOnClickListener(v -> {
                if (currentAvailableBalance <= 0) {
                    Toast.makeText(this, "Insufficient balance for withdrawal", Toast.LENGTH_SHORT).show();
                } else {
                    showWithdrawDialog();
                }
            });
        }
    }

    private void setupFirebase() {
        if (currentSellerId != null) {
            ordersRef = FirebaseDatabase.getInstance().getReference("Orders");
            walletRef = FirebaseDatabase.getInstance().getReference("SellerWallet").child(currentSellerId);
        }
    }

    private void loadFinanceData() {
        if (currentSellerId == null) return;

        ordersRef.orderByChild("sellerId").equalTo(currentSellerId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        double totalAvailable = 0.0;
                        double totalPending = 0.0;
                        financeList.clear();

                        for (DataSnapshot ds : snapshot.getChildren()) {
                            String status = ds.child("status").getValue(String.class);
                            Double amount = ds.child("totalAmount").getValue(Double.class);
                            Long timestamp = ds.child("orderDate").getValue(Long.class);
                            String orderId = ds.getKey();

                            if (amount != null) {
                                // Deduct 10% platform fee
                                double netProfit = amount * 0.90;

                                if ("Completed".equalsIgnoreCase(status)) {
                                    totalAvailable += netProfit;

                                    // Add to history list
                                    financeList.add(new FinanceModel(
                                            orderId,
                                            netProfit,
                                            timestamp != null ? timestamp : System.currentTimeMillis()
                                    ));
                                } else if ("Shipped".equalsIgnoreCase(status) || "Processing".equalsIgnoreCase(status) || "Pending".equalsIgnoreCase(status)) {
                                    totalPending += netProfit;
                                }
                            }
                        }

                        currentAvailableBalance = totalAvailable;

                        // Update UI text
                        tvAvailableBalance.setText(String.format(Locale.US, "RM %.2f", totalAvailable));
                        tvPendingBalance.setText(String.format(Locale.US, "RM %.2f", totalPending));

                        // Refresh history list
                        financeAdapter.notifyDataSetChanged();

                        if (financeList.isEmpty()) {
                            layoutNoTransactions.setVisibility(View.VISIBLE);
                            rvTransactions.setVisibility(View.GONE);
                        } else {
                            layoutNoTransactions.setVisibility(View.GONE);
                            rvTransactions.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(SellerFinanceActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showWithdrawDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Request Withdrawal");
        builder.setMessage("Withdraw RM " + String.format(Locale.US, "%.2f", currentAvailableBalance) + "?\n\n(10% platform fee has been deducted from each order already.)");

        builder.setPositiveButton("Withdraw", (dialog, which) -> processWithdrawal());
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void processWithdrawal() {
        DatabaseReference withdrawReqRef = FirebaseDatabase.getInstance().getReference("WithdrawRequests").push();

        HashMap<String, Object> reqData = new HashMap<>();
        reqData.put("sellerId", currentSellerId);
        reqData.put("amount", currentAvailableBalance);
        reqData.put("status", "Pending");
        reqData.put("timestamp", System.currentTimeMillis());

        withdrawReqRef.setValue(reqData).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                walletRef.child("balance").runTransaction(new Transaction.Handler() {
                    @NonNull
                    @Override
                    public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                        currentData.setValue(0.0);
                        return Transaction.success(currentData);
                    }

                    @Override
                    public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot snapshot) {
                        if (committed) {
                            Toast.makeText(SellerFinanceActivity.this, "Withdrawal request submitted!", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
    }
}