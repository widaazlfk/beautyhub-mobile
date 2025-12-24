package com.example.beautyhub.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beautyhub.R;
import com.example.beautyhub.models.Reward;

import java.util.List;
import java.util.Locale;

public class AvailableRewardAdapter extends RecyclerView.Adapter<AvailableRewardAdapter.RewardViewHolder> {

    private final Context context;
    private final List<Reward> rewardList;
    private final long userPoints; // Mata ganjaran semasa pengguna

    public AvailableRewardAdapter(Context context, List<Reward> rewardList, long userPoints) {
        this.context = context;
        this.rewardList = rewardList;
        this.userPoints = userPoints;
    }

    @NonNull
    @Override
    public RewardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.b_item_available_reward, parent, false);
        return new RewardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RewardViewHolder holder, int position) {
        Reward reward = rewardList.get(position);

        holder.tvRewardTitle.setText(reward.getTitle());
        holder.tvRewardDescription.setText(reward.getDescription());
        holder.tvPointsRequired.setText(String.format(Locale.US, "%d Points", reward.getPointsRequired()));

        // Logik untuk menentukan sama ada pengguna layak
        if (userPoints >= reward.getPointsRequired()) {
            // Pengguna LAYAK
            holder.tvEligibilityStatus.setText("Available");
            holder.tvEligibilityStatus.setTextColor(ContextCompat.getColor(context, R.color.log_color_login)); // Warna hijau
            holder.itemView.setAlpha(1.0f);
        } else {
            // Pengguna TIDAK LAYAK
            holder.tvEligibilityStatus.setText("Insufficient Points");
            holder.tvEligibilityStatus.setTextColor(ContextCompat.getColor(context, R.color.log_color_delete)); // Warna merah
            // Jadikan keseluruhan item kelihatan pudar
            holder.itemView.setAlpha(0.5f);
        }
    }

    @Override
    public int getItemCount() {
        return rewardList.size();
    }

    static class RewardViewHolder extends RecyclerView.ViewHolder {
        TextView tvRewardTitle, tvRewardDescription, tvPointsRequired, tvEligibilityStatus;

        public RewardViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRewardTitle = itemView.findViewById(R.id.tv_reward_title);
            tvRewardDescription = itemView.findViewById(R.id.tv_reward_description);
            tvPointsRequired = itemView.findViewById(R.id.tv_points_required);
            tvEligibilityStatus = itemView.findViewById(R.id.tv_eligibility_status);
        }
    }
}
