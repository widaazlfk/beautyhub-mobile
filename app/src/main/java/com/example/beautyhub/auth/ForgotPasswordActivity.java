package com.example.beautyhub.auth;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.beautyhub.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private ImageView ivBack;
    private TextInputLayout emailInputLayout;
    private TextInputEditText etEmail;
    private MaterialButton btnSendInstructions;
    private CircularProgressIndicator progressBar;
    private TextView tvBackToLogin;
    private LinearLayout successLayout;
    private TextView tvSuccessMessage;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        initViews();
        setupListeners();
        setupTextWatchers();
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        emailInputLayout = findViewById(R.id.emailInputLayout);
        etEmail = findViewById(R.id.etEmail);
        btnSendInstructions = findViewById(R.id.btnSendInstructions);
        progressBar = findViewById(R.id.progressBar);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);
        successLayout = findViewById(R.id.successLayout);
        tvSuccessMessage = findViewById(R.id.tvSuccessMessage);

        mAuth = FirebaseAuth.getInstance();
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> onBackPressed());

        btnSendInstructions.setOnClickListener(v -> validateAndSendResetEmail());

        tvBackToLogin.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // Handle Done/Enter key press
        etEmail.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                validateAndSendResetEmail();
                return true;
            }
            return false;
        });
    }

    private void setupTextWatchers() {
        etEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Clear error when user starts typing
                emailInputLayout.setError(null);
                emailInputLayout.setErrorEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void validateAndSendResetEmail() {
        String email = etEmail.getText().toString().trim();

        // Clear previous errors
        emailInputLayout.setError(null);
        emailInputLayout.setErrorEnabled(false);

        if (TextUtils.isEmpty(email)) {
            emailInputLayout.setError(getString(R.string.email_required));
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.setError(getString(R.string.invalid_email));
            return;
        }

        sendPasswordResetEmail(email);
    }

    private void sendPasswordResetEmail(String email) {
        showLoading(true);
        hideSuccessMessage();

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    showLoading(false);

                    if (task.isSuccessful()) {
                        showSuccessMessage(email);
                        clearForm();
                        logPasswordResetRequest(email);
                    } else {
                        showErrorMessage(task.getException());
                    }
                });
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            btnSendInstructions.setEnabled(false);
            btnSendInstructions.setText("Sending...");
        } else {
            progressBar.setVisibility(View.GONE);
            btnSendInstructions.setEnabled(true);
            btnSendInstructions.setText(getString(R.string.send_reset_instructions));
        }
    }

    private void showSuccessMessage(String email) {
        String successMessage = String.format(
                "Reset email sent to %s. Please check your inbox and spam folder.",
                email
        );

        tvSuccessMessage.setText(successMessage);
        successLayout.setVisibility(View.VISIBLE);

        Toast.makeText(this, R.string.reset_email_sent, Toast.LENGTH_LONG).show();
    }

    private void hideSuccessMessage() {
        successLayout.setVisibility(View.GONE);
    }

    private void showErrorMessage(Exception exception) {
        String errorMessage = getString(R.string.reset_failed);

        if (exception != null && exception.getMessage() != null) {
            String message = exception.getMessage();
            if (message.contains("user-not-found")) {
                errorMessage = "No account found with this email address";
            } else if (message.contains("invalid-email")) {
                errorMessage = "Invalid email address format";
            } else if (message.contains("network-request-failed")) {
                errorMessage = "Network error. Please check your internet connection";
            }
        }

        emailInputLayout.setError(errorMessage);
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    }

    private void clearForm() {
        etEmail.setText("");
    }

    private void logPasswordResetRequest(String email) {
        // Optional: Log password reset request for security purposes
        // You can use LogHelper or Firebase Analytics here
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up any resources if needed
    }
}