package com.example.beautyhub.admin;

import com.example.beautyhub.adapters.ReportAdapter;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.beautyhub.databinding.ActivityAdminManageReportsBinding;
import com.example.beautyhub.models.Report;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdminManageReportsActivity extends AppCompatActivity {
    private ActivityAdminManageReportsBinding binding;
    private List<Report> reportList = new ArrayList<>();
    private ReportAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminManageReportsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup Toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Manage Reports");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        // Setup RecyclerView
        binding.recyclerViewReports.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReportAdapter(reportList);
        binding.recyclerViewReports.setAdapter(adapter);

        loadReports();
    }

    private void loadReports() {
        binding.progressBar.setVisibility(View.VISIBLE);
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Reports");

        // Menggunakan addValueEventListener supaya senarai update secara automatik (real-time)
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Periksa jika Activity masih aktif sebelum update UI
                if (isFinishing() || isDestroyed()) return;

                reportList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    try {
                        Report report = ds.getValue(Report.class);
                        if (report != null) {
                            reportList.add(report);
                        }
                    } catch (Exception e) {
                        // Log jika terdapat ralat mapping data
                        android.util.Log.e("FirebaseData", "Ralat membaca data: " + e.getMessage());
                    }
                }

                // Susun laporan terbaru di atas (Berdasarkan timestamp jika perlu)
                Collections.reverse(reportList);

                adapter.notifyDataSetChanged();
                binding.progressBar.setVisibility(View.GONE);

                if (reportList.isEmpty()) {
                    binding.tvNoData.setVisibility(View.VISIBLE);
                } else {
                    binding.tvNoData.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isFinishing()) return;
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(AdminManageReportsActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
