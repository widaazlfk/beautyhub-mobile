package com.example.beautyhub.info;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import com.example.beautyhub.R;
import com.google.android.material.appbar.MaterialToolbar;

public class ContactUsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_us);

        // Inisialisasi Toolbar
        setupToolbar();
    }

    private void setupToolbar() {
        // Pastikan ID ini sama dengan yang anda letakkan di dalam activity_contact_us.xml
        MaterialToolbar toolbar = findViewById(R.id.toolbar_contact);

        if (toolbar != null) {
            setSupportActionBar(toolbar);

            // Memberikan fungsi kepada butang navigasi (Back Button)
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Menutup activity semasa dan kembali ke skrin sebelumnya
                    onBackPressed();
                }
            });
        }
    }
}