package com.example.beautyhub.buyer;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

// IMPORTANT: Assuming you are using View Binding for this Activity as well,
// and the layout file is named activity_report_problem.xml
import com.example.beautyhub.databinding.ActivityReportProblemBinding;

public class ReportProblemActivity extends AppCompatActivity {

    private ActivityReportProblemBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReportProblemBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        setupFormSubmission();
    }

    private void setupToolbar() {
        // Assuming your toolbar in activity_report_problem.xml is named 'toolbar'
        // and you want a back button.
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Report a Problem");
        }
        // Handle the back button click
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupFormSubmission() {
        // Assuming your button is named 'buttonSubmitReport'
        binding.buttonSubmitReport.setOnClickListener(v -> {
            String subject = binding.editTextSubject.getText().toString().trim();
            String description = binding.editTextDescription.getText().toString().trim();

            if (subject.isEmpty() || description.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
                return;
            }

            // TODO: Implement actual logic to send report data to a backend server (API call)

            Toast.makeText(this, "Your report has been submitted. Thank you!", Toast.LENGTH_LONG).show();
            finish(); // Close the activity after successful submission
        });
    }

    // You would also need a basic layout file named activity_report_problem.xml
}