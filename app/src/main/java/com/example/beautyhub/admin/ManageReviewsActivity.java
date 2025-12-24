package com.example.beautyhub.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beautyhub.R;
import com.example.beautyhub.adapters.MyReviewsAdapter; // Kita akan cipta adapter ini
import com.example.beautyhub.models.Review;       // Kita akan cipta model ini
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ManageReviewsActivity extends AppCompatActivity {

    private RecyclerView rvReviews;
    private ProgressBar progressBar;
    private TextView tvNoReviews;

    private DatabaseReference reviewsRef;
    private MyReviewsAdapter myReviewsAdapter;
    private List<Review> reviewList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Anda perlu cipta layout 'a_activity_manage_reviews.xml'
        setContentView(R.layout.a_activity_manage_reviews);

        // Rujukan ke nod 'Reviews' di Firebase
        // Anda mungkin perlu menyesuaikan laluan ini, cth: "ProductReviews"
        reviewsRef = FirebaseDatabase.getInstance().getReference("Reviews");

        initViews();
        setupToolbar();
        setupRecyclerView();
        loadReviews();
    }

    private void initViews() {
        rvReviews = findViewById(R.id.rv_all_reviews);
        progressBar = findViewById(R.id.progress_bar_reviews);
        tvNoReviews = findViewById(R.id.tv_no_reviews_found);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_manage_reviews);
        toolbar.setTitle("Manage Reviews");
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        reviewList = new ArrayList<>();
        // Adapter akan dicipta dengan listener untuk tindakan (cth: padam)
        myReviewsAdapter = new MyReviewsAdapter(this, reviewList, review -> {
            confirmDeleteReview(review);
        });
        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        rvReviews.setAdapter(myReviewsAdapter);
    }

    private void loadReviews() {
        progressBar.setVisibility(View.VISIBLE);
        reviewsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                reviewList.clear();
                for (DataSnapshot reviewSnapshot : snapshot.getChildren()) {
                    Review review = reviewSnapshot.getValue(Review.class);
                    if (review != null) {
                        review.setReviewId(reviewSnapshot.getKey());
                        reviewList.add(review);
                    }
                }
                myReviewsAdapter.notifyDataSetChanged();
                updateUI();
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ManageReviewsActivity.this, "Failed to load reviews.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmDeleteReview(Review review) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Review")
                .setMessage("Are you sure you want to delete this review? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Logik untuk memadam ulasan dari Firebase
                    reviewsRef.child(review.getReviewId()).removeValue()
                            .addOnSuccessListener(aVoid -> Toast.makeText(ManageReviewsActivity.this, "Review deleted.", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(ManageReviewsActivity.this, "Failed to delete review.", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateUI() {
        if (reviewList.isEmpty()) {
            rvReviews.setVisibility(View.GONE);
            tvNoReviews.setVisibility(View.VISIBLE);
        } else {
            rvReviews.setVisibility(View.VISIBLE);
            tvNoReviews.setVisibility(View.GONE);
        }
    }
}
