package com.example.beautyhub.buyer;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.beautyhub.adapters.RewardAdapter;
// No change needed here, this import is correct.
import com.example.beautyhub.databinding.BActivityMyRewardsBinding;
import com.example.beautyhub.models.Reward;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MyRewardsActivity extends AppCompatActivity {

    // This is correct
    private BActivityMyRewardsBinding binding;
    private DatabaseReference userRef, rewardsRef;
    private FirebaseUser currentUser;
    private RewardAdapter adapter;
    private final List<Reward> rewardList = new ArrayList<>();
    private long currentUserPoints = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // This is correct
        binding = BActivityMyRewardsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUser.getUid());
        rewardsRef = FirebaseDatabase.getInstance().getReference("Rewards");

        setupToolbar();
        setupRecyclerView();
        loadAvailableRewards();
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadUserPoints();
    }

    private void setupToolbar() {
        // CORRECTED: Access toolbar via the binding object
        binding.toolbarMyRewards.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new RewardAdapter(rewardList, this::showRedeemConfirmationDialog);
        // CORRECTED: Access RecyclerView via the binding object
        binding.rvAvailableRewards.setLayoutManager(new LinearLayoutManager(this));
        binding.rvAvailableRewards.setAdapter(adapter);
    }

    private void loadUserPoints() {
        userRef.child("points").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentUserPoints = snapshot.exists() ? snapshot.getValue(Long.class) : 0L;
                // CORRECTED: Access TextView via the binding object
                binding.tvRewardPoints.setText(String.format(Locale.US, "%d", currentUserPoints));

                if (adapter != null) {
                    adapter.setCurrentUserPoints(currentUserPoints);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("MyRewardsActivity", "Failed to load points: " + error.getMessage());
            }
        });
    }

    private void loadAvailableRewards() {
        // CORRECTED: Access ProgressBar via the binding object
        binding.progressBarRewards.setVisibility(View.VISIBLE);
        rewardsRef.orderByChild("pointsRequired").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // CORRECTED: Access ProgressBar via the binding object
                binding.progressBarRewards.setVisibility(View.GONE);
                rewardList.clear();

                if (snapshot.exists()) {
                    for (DataSnapshot rewardSnapshot : snapshot.getChildren()) {
                        Reward reward = rewardSnapshot.getValue(Reward.class);
                        if (reward != null && reward.isActive()) {
                            rewardList.add(reward);
                        }
                    }
                }
                updateRewardsUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // CORRECTED: Access ProgressBar via the binding object
                binding.progressBarRewards.setVisibility(View.GONE);
                Log.e("MyRewardsActivity", "Failed to load rewards: " + error.getMessage());
            }
        });
    }

    private void updateRewardsUI() {
        if (rewardList.isEmpty()) {
            // CORRECTED: Access views via the binding object
            binding.tvNoRewards.setVisibility(View.VISIBLE);
            binding.rvAvailableRewards.setVisibility(View.GONE);
        } else {
            // CORRECTED: Access views via the binding object
            binding.tvNoRewards.setVisibility(View.GONE);
            binding.rvAvailableRewards.setVisibility(View.VISIBLE);
        }
        adapter.setCurrentUserPoints(currentUserPoints);
        adapter.notifyDataSetChanged();
    }

    // ... sisa kod anda tidak perlu diubah ...
    // ... rest of your code does not need to be changed ...
    private void showRedeemConfirmationDialog(Reward reward) {
        new AlertDialog.Builder(this)
                .setTitle("Redeem Reward")
                .setMessage(String.format("Are you sure you want to spend %d points to redeem '%s'?", reward.getPointsRequired(), reward.getTitle()))
                .setPositiveButton("Redeem", (dialog, which) -> redeemReward(reward))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void redeemReward(final Reward rewardToRedeem) {
        if (currentUserPoints < rewardToRedeem.getPointsRequired()) {
            Toast.makeText(this, "Not enough points to redeem this reward.", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBarRewards.setVisibility(View.VISIBLE);
        long newPoints = currentUserPoints - rewardToRedeem.getPointsRequired();

        Reward redeemedReward = new Reward(
                rewardToRedeem.getRewardId(),
                rewardToRedeem.getTitle(),
                rewardToRedeem.getDescription(),
                rewardToRedeem.getPointsRequired(),
                rewardToRedeem.getRewardType(),
                rewardToRedeem.getDiscountValue(),
                rewardToRedeem.isActive()
        );
        redeemedReward.setRedeemedAt(System.currentTimeMillis());

        userRef.child("points").setValue(newPoints)
                .addOnSuccessListener(aVoid -> {
                    userRef.child("redeemedRewards").push().setValue(redeemedReward)
                            .addOnCompleteListener(task -> {
                                binding.progressBarRewards.setVisibility(View.GONE);
                                if (task.isSuccessful()) {
                                    Toast.makeText(MyRewardsActivity.this, "Reward redeemed successfully!", Toast.LENGTH_LONG).show();
                                } else {
                                    userRef.child("points").setValue(currentUserPoints);
                                    Toast.makeText(MyRewardsActivity.this, "Failed to save redemption. Points have been restored.", Toast.LENGTH_SHORT).show();
                                    Log.e("MyRewardsActivity", "Failed to save redeemed reward.", task.getException());
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    binding.progressBarRewards.setVisibility(View.GONE);
                    Toast.makeText(MyRewardsActivity.this, "Failed to update points. Please try again.", Toast.LENGTH_SHORT).show();
                    Log.e("MyRewardsActivity", "Failed to deduct points.", e);
                });
    }
}
