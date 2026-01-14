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

    private BActivityMyRewardsBinding binding;
    private DatabaseReference userRef, rewardsRef;
    private FirebaseUser currentUser;
    private RewardAdapter adapter;
    private final List<Reward> rewardList = new ArrayList<>();
    private long currentUserPoints = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = BActivityMyRewardsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Firebase
        userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUser.getUid());
        rewardsRef = FirebaseDatabase.getInstance().getReference("Rewards");

        setupToolbar();
        setupRecyclerView();
        loadUserPoints(); // Ambil baki mata
        loadAvailableRewards(); // Ambil senarai ganjaran
    }

    private void setupToolbar() {
        binding.toolbarMyRewards.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        // Setup adapter dengan callback untuk klik redeem
        adapter = new RewardAdapter(rewardList, this::showRedeemConfirmationDialog);
        binding.rvAvailableRewards.setLayoutManager(new LinearLayoutManager(this));
        binding.rvAvailableRewards.setAdapter(adapter);
    }

    private void loadUserPoints() {
        userRef.child("points").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentUserPoints = snapshot.exists() ? snapshot.getValue(Long.class) : 0L;
                // Update UI baki mata
                binding.tvRewardPoints.setText(String.valueOf(currentUserPoints));

                if (adapter != null) {
                    adapter.setCurrentUserPoints(currentUserPoints);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("MyRewardsActivity", "Failed to load points: " + error.getMessage());
            }
        });
    }

    private void loadAvailableRewards() {
        binding.progressBarRewards.setVisibility(View.VISIBLE);

        rewardsRef.orderByChild("pointsRequired").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
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
                binding.progressBarRewards.setVisibility(View.GONE);
                Log.e("MyRewardsActivity", "Error: " + error.getMessage());
            }
        });
    }

    private void updateRewardsUI() {
        if (rewardList.isEmpty()) {
            binding.tvNoRewardsContainer.setVisibility(View.VISIBLE);
            binding.rvAvailableRewards.setVisibility(View.GONE);
        } else {
            binding.tvNoRewardsContainer.setVisibility(View.GONE);
            binding.rvAvailableRewards.setVisibility(View.VISIBLE);
        }
        adapter.notifyDataSetChanged();
    }

    private void showRedeemConfirmationDialog(Reward reward) {
        new AlertDialog.Builder(this)
                .setTitle("Redeem Reward")
                .setMessage("Confirm spend " + reward.getPointsRequired() + " points for " + reward.getTitle() + "?")
                .setPositiveButton("Redeem", (dialog, which) -> redeemReward(reward))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void redeemReward(final Reward rewardToRedeem) {
        if (currentUserPoints < rewardToRedeem.getPointsRequired()) {
            Toast.makeText(this, "Insufficient points!", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBarRewards.setVisibility(View.VISIBLE);
        long newPoints = currentUserPoints - rewardToRedeem.getPointsRequired();

        // 1. Tolak mata pengguna dahulu
        userRef.child("points").setValue(newPoints).addOnSuccessListener(aVoid -> {

            // 2. Dapatkan rujukan (reference) baru untuk simpan ganjaran yang ditebus
            DatabaseReference redeemedRef = userRef.child("redeemedRewards").push();
            String pushId = redeemedRef.getKey(); // Ini adalah ID unik untuk baucar ini

            // 3. Kemaskini maklumat ganjaran sebelum simpan
            rewardToRedeem.setRewardId(pushId); // Simpan ID unik ke dalam objek Reward
            rewardToRedeem.setRedeemedAt(System.currentTimeMillis());

            // 4. Simpan ke Firebase
            redeemedRef.setValue(rewardToRedeem)
                    .addOnCompleteListener(task -> {
                        binding.progressBarRewards.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Successfully redeemed! You can use this at checkout.", Toast.LENGTH_LONG).show();
                        } else {
                            // Jika gagal simpan ganjaran, pulangkan balik mata pengguna
                            userRef.child("points").setValue(currentUserPoints);
                            Toast.makeText(this, "Failed to save reward. Points restored.", Toast.LENGTH_SHORT).show();
                        }
                    });

        }).addOnFailureListener(e -> {
            binding.progressBarRewards.setVisibility(View.GONE);
            Toast.makeText(this, "Transaction failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}
