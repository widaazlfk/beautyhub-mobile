package com.example.beautyhub.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
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

    public ReportAdapter(List<Report> reportList) {
        this.reportList = reportList;
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

        // Set data asas
        holder.binding.tvSubject.setText(report.getTitle());
        holder.binding.tvDescription.setText(report.getDescription());
        holder.binding.tvStatus.setText("Status: " + report.getStatus());

        // --- LOGIK AMBIL NAMA PENGGUNA ---
        String reporterUid = report.getUserId();
        if (reporterUid != null && !reporterUid.isEmpty()) {
            // Set teks sementara sementara menunggu data dari Firebase
            holder.binding.tvReporter.setText("By: Loading...");

            FirebaseDatabase.getInstance().getReference("Users")
                    .child(reporterUid)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        // Di dalam ReportAdapter.java bahagian onDataChange
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                // AMBIL "username" kerana anda kata di database guna "username"
                                String name = snapshot.child("username").getValue(String.class);

                                if (name != null) {
                                    holder.binding.tvReporter.setText("By: " + name);
                                } else {
                                    // Jika username pun null, kita cuba paparkan email atau tulis No Username
                                    holder.binding.tvReporter.setText("By: No Username Found");
                                }
                            } else {
                                holder.binding.tvReporter.setText("By: User Not Found");
                            }
                        }


                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            holder.binding.tvReporter.setText("By: Error loading name");
                        }
                    });
        } else {
            holder.binding.tvReporter.setText("By: Anonymous");
        }

        // Paparkan imej bukti jika ada
        if (report.getImageUrl() != null && !report.getImageUrl().isEmpty()) {
            holder.binding.ivReportImage.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext())
                    .load(report.getImageUrl())
                    .placeholder(R.drawable.ic_image_placeholder)
                    .into(holder.binding.ivReportImage);
        } else {
            holder.binding.ivReportImage.setVisibility(View.GONE);
        }

        // Logik butang Resolve
        if ("RESOLVED".equalsIgnoreCase(report.getStatus())) {
            holder.binding.btnResolve.setVisibility(View.GONE);
        } else {
            holder.binding.btnResolve.setVisibility(View.VISIBLE);
        }

        holder.binding.btnResolve.setOnClickListener(v -> {
            FirebaseDatabase.getInstance().getReference("Reports")
                    .child(report.getReportId())
                    .child("status")
                    .setValue("RESOLVED")
                    .addOnSuccessListener(aVoid -> Toast.makeText(v.getContext(), "Report marked as Resolved", Toast.LENGTH_SHORT).show());
        });

        holder.binding.btnDelete.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(v.getContext())
                    .setTitle("Delete Report")
                    .setMessage("Are you sure you want to delete this report?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        FirebaseDatabase.getInstance().getReference("Reports")
                                .child(report.getReportId())
                                .removeValue()
                                .addOnSuccessListener(aVoid -> Toast.makeText(v.getContext(), "Deleted successfully", Toast.LENGTH_SHORT).show());
                    })
                    .setNegativeButton("No", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return reportList == null ? 0 : reportList.size();
    }

    static class ReportViewHolder extends RecyclerView.ViewHolder {
        ItemReportBinding binding;
        ReportViewHolder(ItemReportBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
