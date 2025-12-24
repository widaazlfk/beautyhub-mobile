package com.example.beautyhub.buyer;

import android.app.ProgressDialog;
import android.os.Bundle;
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

    // Data yang diterima dari OrderDetailsActivity
    private String orderId;
    private String productId;
    private String productName;
    private String productImageUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddReviewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        databaseRef = FirebaseDatabase.getInstance().getReference();

        // Ambil data dari Intent
        orderId = getIntent().getStringExtra("ORDER_ID");
        productId = getIntent().getStringExtra("PRODUCT_ID");
        productName = getIntent().getStringExtra("PRODUCT_NAME");
        productImageUrl = getIntent().getStringExtra("PRODUCT_IMAGE_URL");

        if (currentUser == null || productId == null || orderId == null) {
            Toast.makeText(this, "Error: Missing required information.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupUI();
        setupListeners();
    }

    private void setupUI() {
        // Paparkan maklumat produk
        binding.productNameReview.setText(productName);
        if (productImageUrl != null && !productImageUrl.isEmpty()) {
            Glide.with(this)
                    .load(productImageUrl)
                    .placeholder(R.drawable.product_placeholder) // Gantikan dengan placeholder anda
                    .into(binding.productImageReview);
        }

        // Sediakan ProgressDialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Submitting Review");
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
    }

    private void setupListeners() {
        binding.toolbarAddReview.setNavigationOnClickListener(v -> finish());

        binding.btnSubmitReview.setOnClickListener(v -> {
            submitReview();
        });
    }

    private void submitReview() {
        float rating = binding.ratingBar.getRating();
        String comment = binding.etReviewComment.getText().toString().trim();

        if (rating == 0) {
            Toast.makeText(this, "Please provide a star rating.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();

        // Ambil nama pengguna dari profil mereka untuk dipaparkan bersama ulasan
        databaseRef.child("Users").child(currentUser.getUid()).child("username").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String username = snapshot.getValue(String.class);
                if (username == null) {
                    username = "Anonymous"; // Fallback
                }
                createReview(rating, comment, username);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.dismiss();
                Toast.makeText(AddReviewActivity.this, "Failed to get user data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createReview(float rating, String comment, String username) {
        String reviewId = databaseRef.child("Products").child(productId).child("reviews").push().getKey();
        if (reviewId == null) {
            progressDialog.dismiss();
            Toast.makeText(this, "Failed to create review ID.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Data ulasan yang akan disimpan
        HashMap<String, Object> reviewData = new HashMap<>();
        reviewData.put("reviewId", reviewId);
        reviewData.put("userId", currentUser.getUid());
        reviewData.put("username", username);
        reviewData.put("rating", rating);
        reviewData.put("comment", comment);
        reviewData.put("timestamp", ServerValue.TIMESTAMP);

        // Gunakan atomic update untuk menulis ulasan dan mengemas kini status pesanan
        Map<String, Object> atomicUpdate = new HashMap<>();

        // 1. Tambah ulasan pada produk
        atomicUpdate.put("Products/" + productId + "/reviews/" + reviewId, reviewData);

        // 2. Tandakan item ini sebagai "telah diulas" dalam pesanan
        // Ini akan mengelakkan pengguna dari memberi ulasan lebih dari sekali
        atomicUpdate.put("Orders/" + orderId + "/subOrders/" + productId + "/reviewed", true);
        // Nota: Anda mungkin perlu mengubah path ini bergantung pada struktur data "Orders" anda.
        // Jika produk ID bukan kunci untuk sub-pesanan, anda perlu cari cara lain untuk kenal pasti item.
        // Untuk sekarang kita anggap productId unik dalam satu pesanan.

        databaseRef.updateChildren(atomicUpdate)
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    Toast.makeText(AddReviewActivity.this, "Review submitted successfully!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK); // Hantar isyarat berjaya kembali ke OrderDetailsActivity
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(AddReviewActivity.this, "Failed to submit review: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
