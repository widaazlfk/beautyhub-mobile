package com.example.beautyhub.buyer;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.beautyhub.databinding.ActivityWriteReviewBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser; // Import ditambah
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;

public class WriteReviewActivity extends AppCompatActivity {

    private ActivityWriteReviewBinding binding;
    private String orderId, productId, productName;
    private Uri imageUri = null;
    private static final int IMAGE_PICK_CODE = 1000;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWriteReviewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // --- PEMBAIKAN DI SINI ---
        // Anda perlu initialize currentUser supaya tidak null
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        // -------------------------

        orderId = getIntent().getStringExtra("ORDER_ID");
        productId = getIntent().getStringExtra("PRODUCT_ID");
        productName = getIntent().getStringExtra("PRODUCT_NAME");

        // Klik gambar untuk pilih dari galeri
        binding.ivProductImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, IMAGE_PICK_CODE);
        });

        // Juga boleh tambah listener pada butang Choose Photos jika mahu
        binding.btnSelectImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, IMAGE_PICK_CODE);
        });

        binding.btnSubmitReview.setOnClickListener(v -> validateData());
    }

    private void validateData() {
        // Pastikan user log masuk sebelum validate data yang lain
        if (currentUser == null) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
            return;
        }

        float rating = binding.ratingBar.getRating();
        String comment = binding.etComment.getText().toString().trim();

        if (rating == 0) {
            Toast.makeText(this, "Please give a rating", Toast.LENGTH_SHORT).show();
            return;
        }

        if (imageUri != null) {
            uploadImageToCloudinary(rating, comment);
        } else {
            saveReviewToFirebase(rating, comment, ""); // Simpan tanpa gambar
        }
    }

    private void uploadImageToCloudinary(float rating, String comment) {
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Uploading image...");
        pd.setCancelable(false);
        pd.show();

        MediaManager.get().upload(imageUri)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}
                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        pd.dismiss();
                        String imageUrl = (String) resultData.get("secure_url");
                        saveReviewToFirebase(rating, comment, imageUrl);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        pd.dismiss();
                        Toast.makeText(WriteReviewActivity.this, "Upload failed: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }

    private void saveReviewToFirebase(float rating, String comment, String imageUrl) {
        // Double check currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Error: User not found!", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference reviewRef = FirebaseDatabase.getInstance().getReference("Reviews").child(orderId);

        HashMap<String, Object> map = new HashMap<>();
        map.put("orderId", orderId);
        map.put("productId", productId);
        map.put("productName", productName);
        map.put("rating", rating);
        map.put("comment", comment);
        map.put("reviewImage", imageUrl);
        map.put("userId", currentUser.getUid());
        map.put("timestamp", System.currentTimeMillis());

        // Ambil nama: guna DisplayName, jika tiada guna Email, jika tiada guna fallback UID
        String name = currentUser.getDisplayName();
        if (name == null || name.isEmpty()) {
            name = currentUser.getEmail();
        }
        if (name == null || name.isEmpty()) {
            name = "User_" + currentUser.getUid().substring(0, 5);
        }

        map.put("username", name);
        map.put("status", "APPROVED");

        reviewRef.setValue(map).addOnSuccessListener(aVoid -> {
            // Juga simpan dalam node ProductReviews supaya senang nak tunjuk kat Product Page
            FirebaseDatabase.getInstance().getReference("ProductReviews")
                    .child(productId != null ? productId : "unknown")
                    .child(orderId)
                    .setValue(map);

            Toast.makeText(this, "Review submitted!", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == IMAGE_PICK_CODE && data != null) {
            imageUri = data.getData();
            binding.ivProductImage.setImageURI(imageUri);
        }
    }
}
