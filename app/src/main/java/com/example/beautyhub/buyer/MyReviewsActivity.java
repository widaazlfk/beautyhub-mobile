package com.example.beautyhub.buyer;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

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

        reviewsRef = FirebaseDatabase.getInstance().getReference("Reviews");

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

        reviewsRef.orderByChild("userId").equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        binding.progressBar.setVisibility(View.GONE);
                        reviewList.clear();

                        if (snapshot.exists()) {
                            for (DataSnapshot reviewSnapshot : snapshot.getChildren()) {
                                Review review = reviewSnapshot.getValue(Review.class);
                                if (review != null) {
                                    review.setReviewId(reviewSnapshot.getKey());
                                    reviewList.add(review);
                                }
                            }

                            adapter.notifyDataSetChanged();

                            if (reviewList.isEmpty()) {
                                binding.tvNoReviews.setVisibility(View.VISIBLE);
                                binding.rvMyReviews.setVisibility(View.GONE);
                            } else {
                                binding.tvNoReviews.setVisibility(View.GONE);
                                binding.rvMyReviews.setVisibility(View.VISIBLE);
                            }
                        } else {
                            binding.tvNoReviews.setVisibility(View.VISIBLE);
                            binding.rvMyReviews.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(MyReviewsActivity.this, "Failed to load reviews", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}