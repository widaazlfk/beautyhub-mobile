package com.example.beautyhub.buyer;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.beautyhub.databinding.ActivityPersonalInfoBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class PersonalInformationActivity extends AppCompatActivity {

    private ActivityPersonalInfoBinding binding;
    private FirebaseUser currentUser;
    private DatabaseReference userRef;
    // Define the listener as a field to manage its lifecycle
    private ValueEventListener userValueEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPersonalInfoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUser.getUid());

        setupToolbar();
        setupClickListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Start listening for real-time updates when the activity is visible
        attachDatabaseReadListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Detach the listener when the activity is not visible to save resources
        detachDatabaseReadListener();
    }

    private void setupToolbar() {
         binding.toolbarPersonalInfo.setNavigationOnClickListener(v -> finish());
    }

    private void attachDatabaseReadListener() {
        if (userValueEventListener == null) {
            userValueEventListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!snapshot.exists()) {
                        Toast.makeText(PersonalInformationActivity.this, "User data not found.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    updateUI(snapshot);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(PersonalInformationActivity.this, "Failed to load data: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
            };
            userRef.addValueEventListener(userValueEventListener);
        }
    }

    private void detachDatabaseReadListener() {
        if (userValueEventListener != null) {
            userRef.removeEventListener(userValueEventListener);
            userValueEventListener = null;
        }
    }

    private void updateUI(DataSnapshot snapshot) {
        // Load Full Name
        String fullName = snapshot.child("username").getValue(String.class);
        binding.tvFullName.setText(fullName != null && !fullName.isEmpty() ? fullName : "Not set");

        // Load Email
        String email = snapshot.child("email").getValue(String.class);
        binding.tvEmail.setText(email != null && !email.isEmpty() ? email : "Not set");

        // Load Phone
        String phone = snapshot.child("phoneNumber").getValue(String.class);
        binding.tvPhone.setText(phone != null && !phone.isEmpty() ? phone : "Not set");

        // Load Birth Date
        if (snapshot.hasChild("birthDate")) {
            Long birthDateTimestamp = snapshot.child("birthDate").getValue(Long.class);
            if (birthDateTimestamp != null && birthDateTimestamp > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
                binding.tvBirthDate.setText(sdf.format(new Date(birthDateTimestamp)));
            } else {
                binding.tvBirthDate.setText("Not set");
            }
        } else {
            binding.tvBirthDate.setText("Not set");
        }

        // Load Gender
        String gender = snapshot.child("gender").getValue(String.class);
        binding.tvGender.setText(gender != null && !gender.isEmpty() ? gender : "Not set");
    }


    private void setupClickListeners() {
        binding.rowFullName.setOnClickListener(v ->
                showEditDialog("Full Name", "username", binding.tvFullName.getText().toString(), InputType.TYPE_CLASS_TEXT)
        );

        binding.rowPhone.setOnClickListener(v ->
                showEditDialog("Phone Number", "phoneNumber", binding.tvPhone.getText().toString(), InputType.TYPE_CLASS_PHONE)
        );

        binding.rowBirthDate.setOnClickListener(v -> showDatePickerDialog());

        binding.rowGender.setOnClickListener(v -> showGenderSelectionDialog());
    }

    private void showEditDialog(String title, final String firebaseKey, String currentValue, int inputType) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update " + title);

        final EditText input = new EditText(this);
        input.setInputType(inputType);
        input.setText(currentValue.equals("Not set") ? "" : currentValue);
        input.setPadding(50, 50, 50, 50);
        builder.setView(input);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String newValue = input.getText().toString().trim();
            if (!newValue.isEmpty()) {
                userRef.child(firebaseKey).setValue(newValue)
                        .addOnSuccessListener(aVoid -> Toast.makeText(this, title + " updated.", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                // NO NEED to call loadUserData() here anymore
            } else {
                Toast.makeText(this, "Value cannot be empty.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth);
                    long timestamp = selectedDate.getTimeInMillis();

                    userRef.child("birthDate").setValue(timestamp)
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Birth date updated.", Toast.LENGTH_SHORT).show());
                    // NO NEED to call loadUserData() here anymore
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        dialog.show();
    }

    private void showGenderSelectionDialog() {
        final String[] genders = {"Female", "Male", "Prefer not to say"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Gender")
                .setItems(genders, (dialog, which) -> {
                    String selectedGender = genders[which];
                    userRef.child("gender").setValue(selectedGender)
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Gender updated.", Toast.LENGTH_SHORT).show());
                    // NO NEED to call loadUserData() here anymore
                });
        builder.create().show();
    }
}
