package com.example.beautyhub.buyer;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beautyhub.R;
import com.example.beautyhub.adapters.MyReviewsAdapter;
import com.example.beautyhub.databinding.ActivityMyReviewsBinding;
import com.example.beautyhub.models.Review;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import android.util.Log;

public class MyReviewsActivity extends AppCompatActivity {

    private ActivityMyReviewsBinding binding;
    private DatabaseReference reviewsRef;
    private MyReviewsAdapter adapter;
    private List<Review> reviewList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMyReviewsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String userId = getIntent().getStringExtra("USER_ID");
        if (userId == null) {
            finish();
            return;
        }

        reviewsRef = FirebaseDatabase.getInstance().getReference("ProductReviews");

        setupToolbar();
        setupRecyclerView();
        loadMyReviews(userId);
    }

    private void setupToolbar() {
        binding.toolbarMyReviews.setNavigationOnClickListener(v -> finish());
        binding.toolbarMyReviews.setTitle("My Reviews");
    }

    private void setupRecyclerView() {
        adapter = new MyReviewsAdapter(this, reviewList);
        binding.rvMyReviews.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMyReviews.setAdapter(adapter);
    }

    private void loadMyReviews(String userId) {
        binding.progressBar.setVisibility(View.VISIBLE);

        // 1. Rujukan ke node "Reviews"
        reviewsRef = FirebaseDatabase.getInstance().getReference("Reviews");
        // 2. Rujukan ke node "Products" untuk ambil gambar
        DatabaseReference productsRef = FirebaseDatabase.getInstance().getReference("Products");

        reviewsRef.orderByChild("userId").equalTo(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        reviewList.clear();

                        // Jika tiada review terus tutup progress bar & update UI
                        if (!snapshot.exists()) {
                            binding.progressBar.setVisibility(View.GONE);
                            updateUI();
                            return;
                        }

                        long totalReviews = snapshot.getChildrenCount();
                        final int[] processedCount = {0};

                        for (DataSnapshot reviewSnapshot : snapshot.getChildren()) {
                            Review review = reviewSnapshot.getValue(Review.class);

                            if (review != null) {
                                review.setReviewId(reviewSnapshot.getKey());
                                String pId = review.getProductId();

                                // 3. Ambil data produk untuk dapatkan imageUrls
                                productsRef.child(pId).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot productSnapshot) {
                                        if (productSnapshot.exists()) {
                                            // Ambil senarai imageUrls dari product
                                            List<String> urls = (List<String>) productSnapshot.child("imageUrls").getValue();
                                            if (urls != null && !urls.isEmpty()) {
                                                // Set gambar pertama ke dalam review (pastikan model Review ada field ini)
                                                review.setProductImageUrl(urls.get(0));
                                            }
                                        }

                                        reviewList.add(review);
                                        processedCount[0]++;

                                        // 4. Hanya update UI selepas semua data produk berjaya diambil
                                        if (processedCount[0] == totalReviews) {
                                            binding.progressBar.setVisibility(View.GONE);
                                            sortAndNotify();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        processedCount[0]++;
                                        if (processedCount[0] == totalReviews) {
                                            binding.progressBar.setVisibility(View.GONE);
                                            sortAndNotify();
                                        }
                                    }
                                });
                            } else {
                                processedCount[0]++;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        binding.progressBar.setVisibility(View.GONE);
                        Log.e("MyReviews", "Error: " + error.getMessage());
                    }
                });
    }

    // Helper method untuk sorting
    private void sortAndNotify() {
        if (!reviewList.isEmpty()) {
            reviewList.sort((r1, r2) -> Long.compare(r2.getTimestampLong(), r1.getTimestampLong()));
        }
        adapter.notifyDataSetChanged();
        updateUI();
    }



    private void updateUI() {
        if (reviewList.isEmpty()) {
            binding.tvNoReviews.setVisibility(View.VISIBLE);
            binding.rvMyReviews.setVisibility(View.GONE);
        } else {
            binding.tvNoReviews.setVisibility(View.GONE);
            binding.rvMyReviews.setVisibility(View.VISIBLE);
        }
    }

}