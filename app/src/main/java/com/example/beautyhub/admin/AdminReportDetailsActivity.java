package com.example.beautyhub.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.beautyhub.R;
import com.example.beautyhub.databinding.ActivityAdminReportDetailsBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class AdminReportDetailsActivity extends AppCompatActivity {

    private ActivityAdminReportDetailsBinding binding;
    private String reportId;
    private String targetId;
    private DatabaseReference reportRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminReportDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        reportId = getIntent().getStringExtra("REPORT_ID");
        if (reportId == null) {
            Toast.makeText(this, "Report ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        reportRef = FirebaseDatabase.getInstance().getReference("Reports").child(reportId);

        setupToolbar();
        loadReportDetails();
        setupButtons();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Report Details");
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadReportDetails() {
        reportRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                String reason = snapshot.child("reason").getValue(String.class);
                String description = snapshot.child("description").getValue(String.class);
                String imageUrl = snapshot.child("imageUrl").getValue(String.class);
                String targetName = snapshot.child("targetName").getValue(String.class);
                String senderId = snapshot.child("senderId").getValue(String.class);
                String reportType = snapshot.child("reportType").getValue(String.class);
                String status = snapshot.child("status").getValue(String.class);
                targetId = snapshot.child("targetId").getValue(String.class);

                // Info Tambahan Audit (Jika ada)
                String resolvedBy = snapshot.child("resolvedBy").getValue(String.class);

                // Set Data ke UI
                binding.tvReportReason.setText(reason != null ? reason : "No Reason");
                binding.tvReportDescription.setText(description != null ? description : "No Description");

                if ("HELP_CENTRE_ISSUE".equals(reportType)) {
                    binding.tvSellerName.setText("Category: Help Center");
                } else {
                    binding.tvSellerName.setText("Reported Account: " + targetName);
                }

                // Ambil Nama Pengadu
                if (senderId != null) {
                    FirebaseDatabase.getInstance().getReference("Users").child(senderId).child("username")
                            .get().addOnSuccessListener(ds -> {
                                if (ds.exists()) {
                                    binding.tvReporterName.setText("By: " + ds.getValue(String.class));
                                }
                            });
                }

                // Gambar Bukti
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    binding.ivReportEvidence.setVisibility(View.VISIBLE);
                    Glide.with(AdminReportDetailsActivity.this)
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_image_placeholder)
                            .into(binding.ivReportEvidence);
                } else {
                    binding.ivReportEvidence.setVisibility(View.GONE);
                }

                // Logik Kawalan UI untuk Audit
                if ("RESOLVED".equals(status)) {
                    binding.btnTakeAction.setVisibility(View.GONE);
                    binding.btnDismissReport.setText("Back to List");

                    // Optional: Tunjukkan siapa admin yang resolve laporan ini dlm log
                    if (resolvedBy != null) {
                        Toast.makeText(AdminReportDetailsActivity.this, "Resolved by Admin ID: " + resolvedBy, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    binding.btnTakeAction.setVisibility(View.VISIBLE);
                    binding.btnDismissReport.setText("Dismiss (Close)");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupButtons() {
        // Dismiss hanya untuk keluar dari skrin (Cancel tindakan)
        binding.btnDismissReport.setOnClickListener(v -> finish());

        binding.btnTakeAction.setOnClickListener(v -> showActionOptionsDialog());
    }

    private void showActionOptionsDialog() {
        String[] options;
        if (targetId != null && !"SYSTEM".equals(targetId)) {
            options = new String[]{"Mark as Resolved", "Suspend Reported User", "Cancel"};
        } else {
            options = new String[]{"Mark as Resolved", "Cancel"};
        }

        new AlertDialog.Builder(this)
                .setTitle("Admin Action")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        resolveReport("Resolved without suspension");
                    } else if (which == 1 && options.length > 2) {
                        confirmSuspendUser();
                    }
                })
                .show();
    }

    private void confirmSuspendUser() {
        new AlertDialog.Builder(this)
                .setTitle("Suspend User Account")
                .setMessage("Are you sure? This action will be logged for audit purposes. The report status will change to RESOLVED.")
                .setPositiveButton("Confirm Suspend", (dialog, which) -> {
                    if (targetId != null) {
                        // 1. Suspend the user
                        FirebaseDatabase.getInstance().getReference("Users")
                                .child(targetId)
                                .child("suspended")
                                .setValue(true)
                                .addOnSuccessListener(aVoid -> {
                                    // 2. Resolve the report with audit info
                                    resolveReport("User Suspended");
                                    Toast.makeText(this, "User has been suspended", Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void resolveReport(String adminNote) {
        String currentAdminId = FirebaseAuth.getInstance().getUid();
        long timestamp = System.currentTimeMillis();

        Map<String, Object> update = new HashMap<>();
        update.put("status", "RESOLVED");
        update.put("resolvedBy", currentAdminId != null ? currentAdminId : "Unknown Admin");
        update.put("resolvedAt", timestamp);
        update.put("adminNote", adminNote);

        // 1. Update status laporan (sedia ada)
        reportRef.updateChildren(update).addOnSuccessListener(aVoid -> {

            // 2. TAMBAHAN: Simpan ke Global Admin Logs untuk Audit yang lebih kuat
            DatabaseReference logRef = FirebaseDatabase.getInstance().getReference("AdminLogs").push();
            Map<String, Object> logData = new HashMap<>();
            logData.put("adminId", currentAdminId);
            logData.put("action", "RESOLVED_REPORT");
            logData.put("reportId", reportId);
            logData.put("details", adminNote);
            logData.put("timestamp", timestamp);

            logRef.setValue(logData);

            Toast.makeText(this, "Report audit updated & resolved", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}