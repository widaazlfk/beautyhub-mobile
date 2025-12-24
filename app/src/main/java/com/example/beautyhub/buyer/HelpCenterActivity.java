package com.example.beautyhub.buyer;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.beautyhub.databinding.ActivityHelpCenterBinding;
import com.example.beautyhub.info.AboutUsActivity;
// Assuming ReportProblemActivity exists or will be created


public class HelpCenterActivity extends AppCompatActivity {

    private ActivityHelpCenterBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHelpCenterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        setupClickListeners();
    }

    private void setupToolbar() {
        // Sets the back button functionality
        binding.toolbarHelpCenter.setNavigationOnClickListener(v -> finish());
    }

    private void setupClickListeners() {

        // 1. Report a Problem
        binding.cardReportProblem.setOnClickListener(v -> {
            // Intent to navigate to the Report a Problem screen
            Intent intent = new Intent(this, ReportProblemActivity.class);
            startActivity(intent);
        });

        // 2. About Us
        binding.cardAboutUs.setOnClickListener(v -> {
            // Intent to navigate to the About Us screen
            Intent intent = new Intent(this, AboutUsActivity.class);
            startActivity(intent);
        });
    }
}