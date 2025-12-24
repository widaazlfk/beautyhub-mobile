package com.example.beautyhub.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.beautyhub.R;
import com.example.beautyhub.auth.ForgotPasswordActivity;

public class AdminLoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_activity_admin_login);

        // Back button
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // Forgot password
        TextView tvForgotPassword = findViewById(R.id.tv_forgot_password);
        tvForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(this, ForgotPasswordActivity.class));
        });

        // Login button
        Button btnLogin = findViewById(R.id.btn_login);
        btnLogin.setOnClickListener(v -> {
            // Add admin authentication logic here
            startActivity(new Intent(this, AdminDashboardActivity.class));
            finish();
        });
    }
}