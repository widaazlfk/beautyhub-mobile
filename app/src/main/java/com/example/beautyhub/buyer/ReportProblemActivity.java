package com.example.beautyhub.buyer;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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

public class ReportProblemActivity extends AppCompatActivity {

    private ActivityReportProblemBinding binding;
    private Uri imageUri = null;
    private static final int IMAGE_PICK_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReportProblemBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (binding.toolbar != null) {
            setSupportActionBar(binding.toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);

            // Logik apabila butang back diklik
            binding.toolbar.setNavigationOnClickListener(v -> {
                onBackPressed();
            });
        }
        // Klik pada Card/Image untuk pilih gambar
        binding.cardImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, IMAGE_PICK_CODE);
        });

        binding.buttonSubmitReport.setOnClickListener(v -> validateData());
    }

    private void validateData() {
        String title = binding.editTextSubject.getText().toString().trim();
        String description = binding.editTextDescription.getText().toString().trim();

        if (title.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Sila isi semua ruangan", Toast.LENGTH_SHORT).show();
            return;
        }

        if (imageUri != null) {
            uploadToCloudinary(title, description);
        } else {
            saveToFirebase(title, description, "");
        }
    }

    private void uploadToCloudinary(String title, String description) {
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Uploading the image...");
        pd.show();

        MediaManager.get().upload(imageUri).callback(new UploadCallback() {
            @Override
            public void onSuccess(String requestId, Map resultData) {
                pd.dismiss();
                String imageUrl = (String) resultData.get("secure_url");
                saveToFirebase(title, description, imageUrl);
            }

            @Override
            public void onError(String requestId, ErrorInfo error) {
                pd.dismiss();
                Toast.makeText(ReportProblemActivity.this, "Failed to upload image", Toast.LENGTH_SHORT).show();
            }
            @Override public void onStart(String requestId) {}
            @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
            @Override public void onReschedule(String requestId, ErrorInfo error) {}
        }).dispatch();
    }

    private void saveToFirebase(String title, String description, String imageUrl) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Reports");
        String reportId = ref.push().getKey();
        // Dapatkan UID user yang tengah login
        String uid = FirebaseAuth.getInstance().getUid();

        HashMap<String, Object> map = new HashMap<>();
        map.put("reportId", reportId);
        map.put("userId", uid);
        map.put("title", title);
        map.put("description", description);
        map.put("imageUrl", imageUrl);
        map.put("status", "PENDING");
        map.put("timestamp", System.currentTimeMillis());

        if (reportId != null) {
            ref.child(reportId).setValue(map).addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Report has been sent!", Toast.LENGTH_SHORT).show();
                finish();
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == IMAGE_PICK_CODE && data != null) {
            imageUri = data.getData();
            binding.ivReportImage.setImageURI(imageUri);
            // Jadikan gambar penuh dalam kotak
            binding.ivReportImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
            binding.ivReportImage.setPadding(0, 0, 0, 0);
        }
    }
}