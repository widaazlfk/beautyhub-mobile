package com.example.beautyhub.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beautyhub.R;
import com.example.beautyhub.adapters.AdminLogAdapter;
// ▼▼▼ GUNAKAN MODEL AdminLog ▼▼▼
import com.example.beautyhub.models.AdminLog;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class AdminLogActivity extends AppCompatActivity {

    private RecyclerView rvLogs;
    private ProgressBar progressBar;
    private TextView tvNoLogs;

    private DatabaseReference logsRef;
    private AdminLogAdapter logAdapter;
    // ▼▼▼ GUNAKAN ArrayList<AdminLog> ▼▼▼
    private ArrayList<AdminLog> logList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_activity_admin_log);

        logsRef = FirebaseDatabase.getInstance().getReference("ActivityLogs");

        initViews();
        setupToolbar();
        setupRecyclerView();
        loadLogs();
    }

    private void initViews() {
        rvLogs = findViewById(R.id.rv_logs);
        progressBar = findViewById(R.id.progress_bar_logs);
        tvNoLogs = findViewById(R.id.tv_no_logs);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_logs);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        logList = new ArrayList<>();
        // Pastikan Adapter juga menggunakan List<AdminLog>
        logAdapter = new AdminLogAdapter(this, logList);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setReverseLayout(true);
        layoutManager.setStackFromEnd(true);

        rvLogs.setLayoutManager(layoutManager);
        rvLogs.setAdapter(logAdapter);
    }

    private void loadLogs() {
        progressBar.setVisibility(View.VISIBLE);
        Query logsQuery = logsRef.orderByChild("timestamp").limitToLast(100);

        logsQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                logList.clear();
                for (DataSnapshot logSnapshot : snapshot.getChildren()) {
                    // ▼▼▼ GUNA AdminLog.class UNTUK DAPATKAN DATA ▼▼▼
                    AdminLog log = logSnapshot.getValue(AdminLog.class);
                    if (log != null) {
                        logList.add(log);
                    }
                }

                logAdapter.notifyDataSetChanged();
                updateUI();
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AdminLogActivity.this, "Failed to load logs: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI() {
        if (logList.isEmpty()) {
            rvLogs.setVisibility(View.GONE);
            tvNoLogs.setVisibility(View.VISIBLE);
        } else {
            rvLogs.setVisibility(View.VISIBLE);
            tvNoLogs.setVisibility(View.GONE);
        }
    }
}
