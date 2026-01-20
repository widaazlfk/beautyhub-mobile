package com.example.beautyhub.buyer;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beautyhub.R;
import com.example.beautyhub.adapters.NotificationAdapter;
import com.example.beautyhub.models.NotificationModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificationActivity extends AppCompatActivity {

    private RecyclerView rvNotifications;
    private NotificationAdapter adapter;
    private List<NotificationModel> notificationList;
    private ProgressBar progressBar;
    private LinearLayout emptyView; // Added for the empty state
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        currentUserId = FirebaseAuth.getInstance().getUid();

        // Setup Toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar_notification);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Init Views
        // Note: Using R.id.notifications_recycler_view as defined in your XML
        rvNotifications = findViewById(R.id.notifications_recycler_view);
        progressBar = findViewById(R.id.progress_bar_notifications);
        emptyView = findViewById(R.id.empty_notification_view);

        rvNotifications.setLayoutManager(new LinearLayoutManager(this));

        notificationList = new ArrayList<>();
        adapter = new NotificationAdapter(this, notificationList);
        rvNotifications.setAdapter(adapter);

        if (currentUserId != null) {
            fetchNotifications();
        } else {
            progressBar.setVisibility(View.GONE);
            updateUI(true);
        }
    }

    // Dalam NotificationActivity.java
    private void fetchNotifications() {
        progressBar.setVisibility(View.VISIBLE);
        DatabaseReference notifRef = FirebaseDatabase.getInstance()
                .getReference("Notifications")
                .child(currentUserId);

        notifRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                notificationList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    NotificationModel model = ds.getValue(NotificationModel.class);
                    if (model != null) {
                        notificationList.add(model);
                        // PADAM LOGIK LAMA DI SINI (Jangan letak setValue(false) di sini)
                    }
                }
                Collections.reverse(notificationList);
                adapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
                updateUI(notificationList.isEmpty());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void updateUI(boolean isEmpty) {
        if (isEmpty) {
            rvNotifications.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            rvNotifications.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }
}
