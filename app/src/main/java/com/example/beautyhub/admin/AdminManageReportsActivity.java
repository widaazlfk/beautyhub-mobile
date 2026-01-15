package com.example.beautyhub.admin;

import com.example.beautyhub.adapters.ReportAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.beautyhub.R;
import com.example.beautyhub.databinding.ActivityAdminManageReportsBinding;
import com.example.beautyhub.models.Report;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdminManageReportsActivity extends AppCompatActivity {
    private ActivityAdminManageReportsBinding binding;
    private List<Report> allReports = new ArrayList<>(); // Simpan semua data asal
    private List<Report> reportList = new ArrayList<>();  // Senarai yang dipaparkan (ditapis)
    private ReportAdapter adapter;

    // Default filter adalah PENDING
    private String currentFilter = "PENDING";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminManageReportsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 1. Setup Toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Manage Reports");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        // 2. Setup RecyclerView & Adapter
        binding.recyclerViewReports.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReportAdapter(reportList, report -> {
            Intent intent = new Intent(AdminManageReportsActivity.this, AdminReportDetailsActivity.class);
            intent.putExtra("REPORT_ID", report.getReportId());
            intent.putExtra("REPORT_TYPE", report.getReportType());
            intent.putExtra("TARGET_NAME", report.getTargetName());
            startActivity(intent);
        });
        binding.recyclerViewReports.setAdapter(adapter);

        // 3. Setup Logik Filter menggunakan ChipGroup
        setupFilterLogic();

        // 4. PANGGIL DATA DARI FIREBASE
        loadReports();
    }

    private void setupFilterLogic() {
        // Mendengar perubahan pada pilihan Chip (Pending / Resolved)
        binding.chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chipPending)) {
                currentFilter = "PENDING";
            } else if (checkedIds.contains(R.id.chipResolved)) {
                currentFilter = "RESOLVED";
            }
            applyFilter();
        });
    }

    private void loadReports() {
        binding.progressBar.setVisibility(View.VISIBLE);
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Reports");

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) return;

                allReports.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    try {
                        Report report = ds.getValue(Report.class);
                        if (report != null) {
                            allReports.add(report);
                        }
                    } catch (Exception e) {
                        android.util.Log.e("FirebaseData", "Error mapping: " + e.getMessage());
                    }
                }

                // Susun ikut masa terbaru (Timestamp)
                Collections.sort(allReports, (r1, r2) -> Long.compare(r2.getTimestamp(), r1.getTimestamp()));

                // Tapis data mengikut filter semasa
                applyFilter();
                binding.progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isFinishing()) return;
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(AdminManageReportsActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyFilter() {
        reportList.clear();
        for (Report report : allReports) {
            // Jika status null, kita anggap sebagai PENDING
            String status = report.getStatus() != null ? report.getStatus() : "PENDING";

            if (currentFilter.equalsIgnoreCase(status)) {
                reportList.add(report);
            }
        }

        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (reportList.isEmpty()) {
            binding.tvNoData.setVisibility(View.VISIBLE);
            binding.tvNoData.setText("No " + currentFilter.toLowerCase() + " reports found.");
        } else {
            binding.tvNoData.setVisibility(View.GONE);
        }
    }
}