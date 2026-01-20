package com.example.beautyhub.buyer;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.beautyhub.databinding.ActivityReportProblemBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

/**
 * Flexible Report Activity used for:
 * 1. Buyer reporting Seller (Order)
 * 2. Seller reporting Buyer (Order)
 * 3. General App Issues (Help Center)
 */
public class ReportProblemActivity extends AppCompatActivity {

    private ActivityReportProblemBinding binding;
    private Uri imageUri = null;
    private static final int IMAGE_PICK_CODE = 1001;

    // Report context data
    private String targetId, targetName, reportType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReportProblemBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 1. Get data from Intent
        // Expected types: "BUYER_REPORT_SELLER", "SELLER_REPORT_BUYER", "HELP_CENTRE_ISSUE"
        reportType = getIntent().getStringExtra("REPORT_TYPE");
        targetId = getIntent().getStringExtra("TARGET_ID");
        targetName = getIntent().getStringExtra("TARGET_NAME");

        setupToolbar();
        applyScenarioUI();

        // 2. Image Selection
        binding.cardImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, IMAGE_PICK_CODE);
        });

        // 3. Submit logic
        binding.buttonSubmitReport.setOnClickListener(v -> validateData());
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

            // Set dynamic title
            if ("HELP_CENTRE_ISSUE".equals(reportType)) {
                getSupportActionBar().setTitle("Help Center Report");
            } else if (targetName != null) {
                getSupportActionBar().setTitle("Reporting: " + targetName);
            } else {
                getSupportActionBar().setTitle("Submit a Report");
            }
        }
        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void applyScenarioUI() {
        // Change hints based on whether it is a person report or a general system issue
        if ("HELP_CENTRE_ISSUE".equals(reportType)) {
            binding.editTextSubject.setHint("Issue Type (e.g. Payment, Bug, Login)");
            binding.editTextDescription.setHint("Please describe the problem you encountered...");
        } else {
            binding.editTextSubject.setHint("Reason for report (e.g. Fraud, Scammed)");
            binding.editTextDescription.setHint("Please provide more details about the incident...");
        }
    }

    private void validateData() {
        String reason = binding.editTextSubject.getText().toString().trim();
        String description = binding.editTextDescription.getText().toString().trim();

        if (reason.isEmpty()) {
            binding.editTextSubject.setError("Reason is required");
            return;
        }
        if (description.isEmpty()) {
            binding.editTextDescription.setError("Description is required");
            return;
        }

        if (imageUri != null) {
            uploadToCloudinary(reason, description);
        } else {
            saveToFirebase(reason, description, "");
        }
    }

    private void uploadToCloudinary(String reason, String description) {
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Uploading evidence...");
        pd.setCancelable(false);
        pd.show();

        MediaManager.get().upload(imageUri).callback(new UploadCallback() {
            @Override
            public void onSuccess(String requestId, Map resultData) {
                pd.dismiss();
                String imageUrl = (String) resultData.get("secure_url");
                saveToFirebase(reason, description, imageUrl);
            }

            @Override
            public void onError(String requestId, ErrorInfo error) {
                pd.dismiss();
                Toast.makeText(ReportProblemActivity.this, "Upload failed: " + error.getDescription(), Toast.LENGTH_SHORT).show();
            }

            @Override public void onStart(String requestId) {}
            @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
            @Override public void onReschedule(String requestId, ErrorInfo error) {}
        }).dispatch();
    }

    private void saveToFirebase(String reason, String description, String imageUrl) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Reports");
        String reportId = ref.push().getKey();
        String senderUid = FirebaseAuth.getInstance().getUid();

        if (reportId == null) return;

        // Structured Map to match the Report model and Firebase schema
        HashMap<String, Object> map = new HashMap<>();
        map.put("reportId", reportId);
        map.put("senderId", senderUid);       // Reporter UID
        map.put("reportType", reportType);   // Category
        map.put("targetId", targetId != null ? targetId : "SYSTEM"); // UID of reported party or SYSTEM
        map.put("targetName", targetName != null ? targetName : "General Support");

        map.put("reason", reason);
        map.put("description", description);
        map.put("imageUrl", imageUrl);
        map.put("status", "PENDING");
        map.put("timestamp", System.currentTimeMillis());

        ref.child(reportId).setValue(map).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Report submitted! Admin will review it.", Toast.LENGTH_LONG).show();
            finish();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == IMAGE_PICK_CODE && data != null) {
            imageUri = data.getData();
            binding.ivReportImage.setImageURI(imageUri);
            binding.ivReportImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
            binding.ivReportImage.setPadding(0, 0, 0, 0);
        }
    }
}