package com.example.beautyhub.buyer;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.example.beautyhub.R;
import com.google.android.material.appbar.MaterialToolbar;

public class NotificationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        // Setup Toolbar dengan butang kembali
        MaterialToolbar toolbar = findViewById(R.id.toolbar_notification);
        toolbar.setNavigationOnClickListener(v -> finish()); // Tutup activity ini dan kembali ke skrin sebelumnya
    }
}
