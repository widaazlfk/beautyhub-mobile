package com.example.beautyhub.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beautyhub.R;
import com.example.beautyhub.models.Reward;

import java.util.List;
import java.util.Locale;

public class RewardAdapter extends RecyclerView.Adapter<RewardAdapter.ViewHolder> {

    // 1. Definisikan kedua-dua jenis listener
    // Untuk skrin Pembeli (MyRewardsActivity)
    public interface OnRewardClickListener {
        void onRewardClick(Reward reward);
    }

    // Untuk skrin Admin (ManageRewardsActivity)
    public interface OnRewardActionListener {
        void onRewardAction(Reward reward);
    }

    private final List<Reward> rewardList;
    private Context context;
    private long currentUserPoints = 0;

    // Listener untuk setiap mod
    private OnRewardClickListener buyerListener;
    private OnRewardActionListener adminListener;

    private final boolean isAdminMode; // Flag untuk menentukan mod

    // 2. Constructor untuk mod Pembeli (MyRewardsActivity)
    public RewardAdapter(List<Reward> rewardList, OnRewardClickListener listener) {
        this.rewardList = rewardList;
        this.buyerListener = listener;
        this.isAdminMode = false; // Mod Pembeli
    }

    // 3. Constructor untuk mod Admin (ManageRewardsActivity)
    public RewardAdapter(Context context, List<Reward> rewardList, OnRewardActionListener listener) {
        this.context = context;
        this.rewardList = rewardList;
        this.adminListener = listener;
        this.isAdminMode = true; // Mod Admin
    }

    // Kaedah untuk mengemas kini mata pengguna dari Activity (hanya relevan untuk mod Pembeli)
    public void setCurrentUserPoints(long points) {
        this.currentUserPoints = points;
        notifyDataSetChanged(); // Panggil notify untuk melukis semula item
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inisialisasi context jika belum ditetapkan (untuk mod Pembeli)
        if (this.context == null) {
            this.context = parent.getContext();
        }
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_reward, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Reward reward = rewardList.get(position);

        holder.tvRewardName.setText(reward.getTitle());
        holder.tvRewardDescription.setText(reward.getDescription());
        holder.tvPointsRequired.setText(String.format(Locale.US, "%d points", reward.getPointsRequired()));

        // 4. Logik berbeza berdasarkan mod
        if (isAdminMode) {
            // --- LOGIK UNTUK ADMIN ---
            holder.btnRedeem.setText("Edit"); // Tukar teks butang
            holder.itemView.setOnClickListener(v -> {
                if (adminListener != null) {
                    adminListener.onRewardAction(reward);
                }
            });
            holder.btnRedeem.setOnClickListener(v -> {
                if (adminListener != null) {
                    adminListener.onRewardAction(reward);
                }
            });

            // Tunjukkan status aktif/tidak aktif
            if (!reward.isActive()) {
                holder.itemView.setAlpha(0.5f); // Jadikan item separa lutsinar jika tidak aktif
                holder.tvRewardName.append(" (Inactive)");
            } else {
                holder.itemView.setAlpha(1.0f);
            }

        } else {
            // --- LOGIK UNTUK PEMBELI ---
            holder.btnRedeem.setText("Redeem");
            boolean canRedeem = currentUserPoints >= reward.getPointsRequired();

            holder.btnRedeem.setEnabled(canRedeem); // Tetapkan keadaan butang
            if (!canRedeem) {
                // Jadikan butang kelabu jika tidak boleh ditebus
                holder.btnRedeem.setBackgroundColor(Color.LTGRAY);
            } else {
                // Kembalikan warna asal
                // Kembalikan warna asal menggunakan warna tema utama
                Button defaultButton = new Button(context);
                holder.btnRedeem.setBackground(defaultButton.getBackground());
            }

            if (canRedeem) {
                holder.btnRedeem.setOnClickListener(v -> {
                    if (buyerListener != null) {
                        buyerListener.onRewardClick(reward);
                    }
                });
            } else {
                holder.btnRedeem.setOnClickListener(null); // Tiada tindakan jika tidak boleh ditebus
            }
        }
    }

    @Override
    public int getItemCount() {
        return rewardList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRewardName, tvRewardDescription, tvPointsRequired;
        Button btnRedeem;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRewardName = itemView.findViewById(R.id.tvRewardName);
            tvRewardDescription = itemView.findViewById(R.id.tvRewardDescription);
            tvPointsRequired = itemView.findViewById(R.id.tvPointsRequired);
            btnRedeem = itemView.findViewById(R.id.btnRedeem);
        }
    }
}
