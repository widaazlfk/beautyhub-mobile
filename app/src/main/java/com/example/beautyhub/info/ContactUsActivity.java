package com.example.beautyhub.info;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.example.beautyhub.R;

public class ContactUsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_us);

        // Tetapkan tajuk untuk AppBar
        setTitle("Contact Us");
    }
}
