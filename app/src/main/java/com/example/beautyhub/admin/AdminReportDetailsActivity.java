package com.example.beautyhub.admin;

import android.content.Intent;
import android.net.Uri;
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
    private String targetId; // ID user yang dilaporkan (Seller/Buyer)
    private String senderEmail; // Emel pengadu untuk notifikasi
    private String senderName;
    private DatabaseReference reportRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminReportDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Ambil Report ID dari Intent
        reportId = getIntent().getStringExtra("REPORT_ID");

        if (reportId == null || reportId.isEmpty()) {
            Toast.makeText(this, "Error: Report ID not found", Toast.LENGTH_SHORT).show();
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
            getSupportActionBar().setTitle("Report Management");
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadReportDetails() {
        reportRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    if (!isFinishing()) {
                        Toast.makeText(AdminReportDetailsActivity.this, "Report data has been removed", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                    return;
                }

                // Ekstrak data
                String reason = snapshot.child("reason").getValue(String.class);
                String description = snapshot.child("description").getValue(String.class);
                String imageUrl = snapshot.child("imageUrl").getValue(String.class);
                String targetName = snapshot.child("targetName").getValue(String.class);
                String senderId = snapshot.child("senderId").getValue(String.class);
                String status = snapshot.child("status").getValue(String.class);
                targetId = snapshot.child("targetId").getValue(String.class);

                // Set UI
                binding.tvReportReason.setText(reason != null ? reason : "No Reason Specified");
                binding.tvReportDescription.setText(description != null ? description : "No additional details.");
                binding.tvSellerName.setText("Reported: " + (targetName != null ? targetName : "Unknown"));

                // Ambil Data Pengadu (Reporter) secara real-time
                if (senderId != null) {
                    fetchSenderInfo(senderId);
                }

                // Kendali Gambar Bukti
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    binding.ivReportEvidence.setVisibility(View.VISIBLE);
                    Glide.with(AdminReportDetailsActivity.this)
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_image_placeholder)
                            .into(binding.ivReportEvidence);
                } else {
                    binding.ivReportEvidence.setVisibility(View.GONE);
                }

                // Kawalan Butang Berdasarkan Status
                if ("RESOLVED".equalsIgnoreCase(status)) {
                    binding.btnTakeAction.setVisibility(View.GONE);
                    binding.btnDismissReport.setText("Back to Dashboard");
                } else {
                    binding.btnTakeAction.setVisibility(View.VISIBLE);
                    binding.btnDismissReport.setText("Dismiss Report");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminReportDetailsActivity.this, "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchSenderInfo(String senderId) {
        FirebaseDatabase.getInstance().getReference("Users").child(senderId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot ds) {
                        if (ds.exists()) {
                            senderName = ds.child("username").getValue(String.class);
                            senderEmail = ds.child("email").getValue(String.class);
                            binding.tvReporterName.setText("Reporter: " + (senderName != null ? senderName : "Anonymous"));
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void setupButtons() {
        binding.btnDismissReport.setOnClickListener(v -> finish());
        binding.btnTakeAction.setOnClickListener(v -> showActionOptionsDialog());
    }

    private void showActionOptionsDialog() {
        String[] options;
        // Jangan benarkan suspension jika target adalah SYSTEM
        if (targetId != null && !"SYSTEM".equals(targetId)) {
            options = new String[]{"Resolve Only (Warning)", "Suspend User & Resolve", "Cancel"};
        } else {
            options = new String[]{"Mark as Resolved", "Cancel"};
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Admin Action")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        resolveReport("Issue investigated. User warned.");
                    } else if (which == 1 && options.length > 2) {
                        confirmSuspendUser();
                    }
                }).show();
    }

    private void confirmSuspendUser() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Suspension")
                .setMessage("Action: Suspend Account ID: " + targetId + "\nStatus will be updated to RESOLVED. Proceed?")
                .setPositiveButton("Confirm Suspend", (dialog, which) -> {
                    if (targetId != null) {
                        FirebaseDatabase.getInstance().getReference("Users").child(targetId)
                                .child("suspended").setValue(true)
                                .addOnSuccessListener(aVoid -> resolveReport("User account suspended for policy violation."));
                    }
                })
                .setNegativeButton("Cancel", null).show();
    }

    private void resolveReport(String adminNote) {
        String currentAdminId = FirebaseAuth.getInstance().getUid();
        long timestamp = System.currentTimeMillis();

        Map<String, Object> update = new HashMap<>();
        update.put("status", "RESOLVED");
        update.put("resolvedBy", currentAdminId != null ? currentAdminId : "Admin_System");
        update.put("resolvedAt", timestamp);
        update.put("adminNote", adminNote);

        reportRef.updateChildren(update).addOnSuccessListener(aVoid -> {
            // Log Action ke AdminLogs
            saveAdminLog(currentAdminId, adminNote, timestamp);

            Toast.makeText(this, "Report status: RESOLVED", Toast.LENGTH_SHORT).show();

            // Alur ke Notifikasi Emel
            if (senderEmail != null && !senderEmail.isEmpty()) {
                showEmailNotificationDialog(adminNote);
            } else {
                finish();
            }
        });
    }

    private void saveAdminLog(String adminId, String note, long time) {
        DatabaseReference logRef = FirebaseDatabase.getInstance().getReference("AdminLogs").push();
        Map<String, Object> log = new HashMap<>();
        log.put("adminId", adminId);
        log.put("action", "RESOLVE_REPORT");
        log.put("reportId", reportId);
        log.put("note", note);
        log.put("timestamp", time);
        logRef.setValue(log);
    }

    private void showEmailNotificationDialog(String note) {
        new AlertDialog.Builder(this)
                .setTitle("Send Update to Reporter?")
                .setMessage("Notify " + senderEmail + " about this resolution?")
                .setPositiveButton("Send Email", (dialog, which) -> sendEmail(note))
                .setNegativeButton("No, Just Finish", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private void sendEmail(String note) {
        String subject = "BeautyHub Support: Report Update #" + (reportId.length() > 6 ? reportId.substring(0, 6) : reportId);
        String message = "Dear " + (senderName != null ? senderName : "User") + ",\n\n" +
                "We have reviewed your report regarding " + binding.tvReportReason.getText().toString() + ".\n\n" +
                "Resolution Status: RESOLVED\n" +
                "Admin Remarks: " + note + "\n\n" +
                "Thank you for helping us maintain a safe community.\n\nRegards,\nBeautyHub Admin Team";

        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{senderEmail});
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, message);

        try {
            startActivity(Intent.createChooser(intent, "Open Email App"));
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "No email application found.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}