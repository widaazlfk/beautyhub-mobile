package com.example.beautyhub.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beautyhub.R;
import com.example.beautyhub.adapters.RewardAdapter;
import com.example.beautyhub.models.Reward;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ManageRewardsActivity extends AppCompatActivity implements RewardAdapter.OnRewardActionListener {

    private RecyclerView rvRewards;
    private FloatingActionButton fabAddReward;
    private ProgressBar progressBar;
    private TextView tvNoRewards;

    private RewardAdapter rewardAdapter;
    private List<Reward> rewardList;
    private DatabaseReference rewardsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Kita akan cipta layout ini pada langkah seterusnya
        setContentView(R.layout.a_activity_manage_rewards);

        rewardsRef = FirebaseDatabase.getInstance().getReference("Rewards");

        initViews();
        setupRecyclerView();
        fetchRewards();

        // Apabila butang tambah ditekan, panggil dialog tambah/edit
        fabAddReward.setOnClickListener(v -> showAddEditRewardDialog(null));
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_manage_rewards);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvRewards = findViewById(R.id.rv_rewards);
        fabAddReward = findViewById(R.id.fab_add_reward);
        progressBar = findViewById(R.id.progress_bar_rewards);
        tvNoRewards = findViewById(R.id.tv_no_rewards_found);
    }

    private void setupRecyclerView() {
        rewardList = new ArrayList<>();
        rewardAdapter = new RewardAdapter(this, rewardList, this);
        rvRewards.setLayoutManager(new LinearLayoutManager(this));
        rvRewards.setAdapter(rewardAdapter);
    }

    private void fetchRewards() {
        progressBar.setVisibility(View.VISIBLE);
        rvRewards.setVisibility(View.GONE);
        tvNoRewards.setVisibility(View.GONE);

        rewardsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                rewardList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Reward reward = dataSnapshot.getValue(Reward.class);
                    if (reward != null) {
                        rewardList.add(reward);
                    }
                }
                rewardAdapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);

                if (rewardList.isEmpty()) {
                    tvNoRewards.setVisibility(View.VISIBLE);
                    rvRewards.setVisibility(View.GONE);
                } else {
                    tvNoRewards.setVisibility(View.GONE);
                    rvRewards.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ManageRewardsActivity.this, "Failed to load rewards: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Dipanggil apabila admin menekan mana-mana item ganjaran dalam senarai
    // Dipanggil apabila admin menekan mana-mana item ganjaran dalam senarai
    @Override
    public void onRewardAction(Reward reward) {
        // Buka dialog dalam mod edit
        showAddEditRewardDialog(reward);
    }

    private void showAddEditRewardDialog(final Reward existingReward) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
        View view = LayoutInflater.from(this).inflate(R.layout.a_dialog_add_edit_reward, null);
        builder.setView(view);

        // Inisialisasi semua elemen dari dialog
        final TextView tvDialogTitle = view.findViewById(R.id.tv_dialog_title);
        final EditText etTitle = view.findViewById(R.id.et_reward_title);
        final EditText etDescription = view.findViewById(R.id.et_reward_description);
        final EditText etPoints = view.findViewById(R.id.et_points_required);
        final RadioGroup rgType = view.findViewById(R.id.rg_reward_type);
        final RadioButton rbPercentage = view.findViewById(R.id.rb_percentage);
        final RadioButton rbFixedAmount = view.findViewById(R.id.rb_fixed_amount);
        final EditText etValue = view.findViewById(R.id.et_reward_value);
        final SwitchCompat switchStatus = view.findViewById(R.id.switch_reward_status);
        final Button btnSave = view.findViewById(R.id.btn_save_reward);
        final Button btnDelete = view.findViewById(R.id.btn_delete_reward);
        final Button btnCancel = view.findViewById(R.id.btn_cancel); // Pastikan ID ini sama dalam XML

        // Tentukan mod: Tambah Baru atau Edit Sedia Ada
        if (existingReward != null) {
            tvDialogTitle.setText("Edit Reward");
            etTitle.setText(existingReward.getTitle());
            etDescription.setText(existingReward.getDescription());
            etPoints.setText(String.valueOf(existingReward.getPointsRequired()));
            etValue.setText(String.valueOf(existingReward.getDiscountValue()));
            switchStatus.setChecked(existingReward.isActive());

            if ("percentage".equalsIgnoreCase(existingReward.getRewardType())) {
                rbPercentage.setChecked(true);
            } else {
                rbFixedAmount.setChecked(true);
            }
            btnDelete.setVisibility(View.VISIBLE);
        } else {
            tvDialogTitle.setText("Add New Reward");
            btnDelete.setVisibility(View.GONE);
        }

        AlertDialog dialog = builder.create();

        // Logik butang Cancel - Menutup dialog tanpa simpan
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String description = etDescription.getText().toString().trim();
            String pointsStr = etPoints.getText().toString().trim();
            String valueStr = etValue.getText().toString().trim();
            boolean isActive = switchStatus.isChecked();
            int selectedTypeId = rgType.getCheckedRadioButtonId();

            if (TextUtils.isEmpty(title) || TextUtils.isEmpty(description) || TextUtils.isEmpty(pointsStr) || TextUtils.isEmpty(valueStr) || selectedTypeId == -1) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            int points = Integer.parseInt(pointsStr);
            double value = Double.parseDouble(valueStr);
            String type = (selectedTypeId == R.id.rb_percentage) ? "percentage" : "fixed_amount";
            String rewardId = (existingReward != null) ? existingReward.getRewardId() : rewardsRef.push().getKey();

            if (rewardId == null) return;

            Reward reward = new Reward(rewardId, title, description, points, type, value, isActive);

            rewardsRef.child(rewardId).setValue(reward)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Reward saved successfully", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        btnDelete.setOnClickListener(v -> {
            if (existingReward != null) {
                new AlertDialog.Builder(this)
                        .setTitle("Confirm Deletion")
                        .setMessage("Are you sure you want to delete this reward?")
                        .setPositiveButton("Delete", (dialogInterface, i) -> {
                            rewardsRef.child(existingReward.getRewardId()).removeValue()
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(ManageRewardsActivity.this, "Reward deleted", Toast.LENGTH_SHORT).show();
                                        dialog.dismiss();
                                    });
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        dialog.show();
    }
}