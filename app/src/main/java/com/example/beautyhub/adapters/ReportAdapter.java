package com.example.beautyhub.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.beautyhub.R;
import com.example.beautyhub.databinding.ItemReportBinding;
import com.example.beautyhub.models.Report;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.List;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {

    private List<Report> reportList;
    private OnReportClickListener listener;

    public interface OnReportClickListener {
        void onReportClick(Report report);
    }

    public ReportAdapter(List<Report> reportList, OnReportClickListener listener) {
        this.reportList = reportList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemReportBinding binding = ItemReportBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ReportViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        Report report = reportList.get(position);

        // 1. Set Data Asas
        holder.binding.tvSubject.setText(report.getReason());
        holder.binding.tvDescription.setText(report.getDescription());

        // 2. Paparan Status & Indikasi Visual (DITAMBAH UNTUK KEJELASAN)
        String status = report.getStatus() != null ? report.getStatus() : "PENDING";

        if ("RESOLVED".equals(status)) {
            holder.binding.tvStatus.setText("Status: RESOLVED");
            holder.binding.tvStatus.setTextColor(Color.parseColor("#4CAF50")); // Warna Hijau
            holder.itemView.setAlpha(0.7f); // Nampak pudar sedikit jika sudah selesai
        } else {
            holder.binding.tvStatus.setText("Status: PENDING");
            holder.binding.tvStatus.setTextColor(Color.parseColor("#F44336")); // Warna Merah
            holder.itemView.setAlpha(1.0f);
        }

        // 3. Maklumat Tambahan (Target Name)
        holder.binding.tvTarget.setText("Target: " + report.getTargetName()); // Pastikan ada tvTarget di XML, atau gunakan TextView sedia ada

        // 4. Ambil Nama Pengadu (Reporter) secara Real-time
        String reporterUid = report.getSenderId();
        if (reporterUid != null) {
            FirebaseDatabase.getInstance().getReference("Users").child(reporterUid)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String name = snapshot.child("username").getValue(String.class);
                            if (name == null) name = snapshot.child("name").getValue(String.class);
                            holder.binding.tvReporter.setText("By: " + (name != null ? name : "User"));
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
        }

        // 5. Gambar Bukti
        if (report.getImageUrl() != null && !report.getImageUrl().isEmpty()) {
            holder.binding.ivReportImage.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext())
                    .load(report.getImageUrl())
                    .placeholder(R.drawable.ic_image_placeholder)
                    .into(holder.binding.ivReportImage);
        } else {
            holder.binding.ivReportImage.setVisibility(View.GONE);
        }

        // 6. Klik Item & Butang
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onReportClick(report);
        });



        // Sembunyikan butang resolve di senarai (tindakan dibuat di Details)
        holder.binding.btnResolve.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return reportList == null ? 0 : reportList.size();
    }

    public void updateList(List<Report> newList) {
        this.reportList = newList;
        notifyDataSetChanged();
    }

    static class ReportViewHolder extends RecyclerView.ViewHolder {
        ItemReportBinding binding;
        ReportViewHolder(ItemReportBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}