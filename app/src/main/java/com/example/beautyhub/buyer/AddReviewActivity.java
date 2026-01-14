package com.example.beautyhub.buyer;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.beautyhub.R;
import com.example.beautyhub.databinding.ActivityAddReviewBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class AddReviewActivity extends AppCompatActivity {

    private ActivityAddReviewBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference databaseRef;
    private ProgressDialog progressDialog;

    // Data dari Intent
    private String orderId;
    private String productId;
    private String productName;
    private String productImageUrl;

    private static final String TAG = "AddReviewActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddReviewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        databaseRef = FirebaseDatabase.getInstance().getReference();

        // 1. Ambil data dari Intent (Dikirim oleh OrderDetailItemAdapter)
        orderId = getIntent().getStringExtra("ORDER_ID");
        productId = getIntent().getStringExtra("PRODUCT_ID");
        productName = getIntent().getStringExtra("PRODUCT_NAME");
        productImageUrl = getIntent().getStringExtra("PRODUCT_IMAGE_URL");

        if (currentUser == null || productId == null || orderId == null) {
            Toast.makeText(this, "Error: Information missing!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupUI();
        setupListeners();
    }

    private void setupUI() {
        // Papar Toolbar
        setSupportActionBar(binding.toolbarAddReview);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Papar Nama Produk
        binding.productNameReview.setText(productName);

        // Papar Gambar Produk guna Glide
        if (productImageUrl != null && !productImageUrl.isEmpty()) {
            Glide.with(this)
                    .load(productImageUrl)
                    .placeholder(R.drawable.product_placeholder)
                    .into(binding.productImageReview);
        }

        // Sediakan ProgressDialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Submitting your review...");
        progressDialog.setCancelable(false);
    }

    private void setupListeners() {
        binding.toolbarAddReview.setNavigationOnClickListener(v -> finish());

        // Klik butang Submit
        binding.btnSubmitReview.setOnClickListener(v -> submitReview());

        // Klik kad foto (Jika anda mahu tambah fungsi kamera nanti)
        binding.cardAddPhoto.setOnClickListener(v -> {
            Toast.makeText(this, "Photo upload coming soon!", Toast.LENGTH_SHORT).show();
        });
    }

    private void submitReview() {
        float rating = binding.ratingBar.getRating();
        String comment = binding.etReviewComment.getText().toString().trim();

        if (rating == 0) {
            Toast.makeText(this, "Please select at least 1 star.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();

        // Ambil username pembeli
        databaseRef.child("Users").child(currentUser.getUid()).child("username")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String username = snapshot.getValue(String.class);
                        if (username == null)
                            username = "User_" + currentUser.getUid().substring(0, 5);

                        saveReviewToFirebase(rating, comment, username);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressDialog.dismiss();
                        Toast.makeText(AddReviewActivity.this, "Database error!", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void saveReviewToFirebase(float rating, String comment, String username) {
        progressDialog.show();

        // 1. Jana key unik
        String rawKey = databaseRef.child("Reviews").push().getKey();
        String reviewId = "rev_" + (rawKey != null ? rawKey.substring(1, 6) : System.currentTimeMillis());

        // 2. Sediakan data review
        HashMap<String, Object> reviewData = new HashMap<>();
        reviewData.put("reviewId", reviewId);
        reviewData.put("orderId", orderId);
        reviewData.put("productId", productId);
        reviewData.put("userId", currentUser.getUid());
        reviewData.put("username", username);
        reviewData.put("rating", (int) rating);
        reviewData.put("comment", comment);
        reviewData.put("productName", productName);
        reviewData.put("status", "APPROVED");
        reviewData.put("timestamp", ServerValue.TIMESTAMP);
        reviewData.put("productImageUrl", productImageUrl);

        // 3. Simpan Review (Atomic Update)
        Map<String, Object> updates = new HashMap<>();
        updates.put("Reviews/" + reviewId, reviewData);
        updates.put("ProductReviews/" + productId + "/" + reviewId, reviewData);

        databaseRef.updateChildren(updates).addOnSuccessListener(aVoid -> {
            // --- TAMBAHAN LOGIK POINTS DI SINI ---
            addPointsToUser(10); // Beri 10 mata untuk review
        }).addOnFailureListener(e -> {
            progressDialog.dismiss();
            Log.e(TAG, "Firebase Error: " + e.getMessage());
            Toast.makeText(AddReviewActivity.this, "Submission failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    // Method baru untuk menambah points
    private void addPointsToUser(int pointsToAdd) {
        DatabaseReference userPointsRef = databaseRef.child("Users").child(currentUser.getUid()).child("points");

        userPointsRef.runTransaction(new com.google.firebase.database.Transaction.Handler() {
            @NonNull
            @Override
            public com.google.firebase.database.Transaction.Result doTransaction(@NonNull com.google.firebase.database.MutableData currentData) {
                Long currentPoints = currentData.getValue(Long.class);
                if (currentPoints == null) {
                    currentData.setValue(pointsToAdd);
                } else {
                    currentData.setValue(currentPoints + pointsToAdd);
                }
                return com.google.firebase.database.Transaction.success(currentData);
            }

            @Override
            public void onComplete(com.google.firebase.database.DatabaseError error, boolean committed, com.google.firebase.database.DataSnapshot snapshot) {
                progressDialog.dismiss();
                if (committed) {
                    Toast.makeText(AddReviewActivity.this, "Review submitted! You earned " + pointsToAdd + " points!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(AddReviewActivity.this, "Review submitted successfully!", Toast.LENGTH_SHORT).show();
                }
                setResult(RESULT_OK);
                finish();
            }
        });
    }
}