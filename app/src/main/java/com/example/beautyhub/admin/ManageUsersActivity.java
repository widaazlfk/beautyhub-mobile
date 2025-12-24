package com.example.beautyhub.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beautyhub.R;
// ▼▼▼ 1. IMPORT LogHelper ▼▼▼
import com.example.beautyhub.admin.LogHelper;
// ▲▲▲ AKHIR IMPORT ▲▲▲
import com.example.beautyhub.adapters.AdminUserAdapter;
import com.example.beautyhub.models.User;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ManageUsersActivity extends AppCompatActivity implements AdminUserAdapter.OnUserActionListener {

    private static final String TAG = "ManageUsersActivity";

    // Views
    private RecyclerView rvUsers;
    private EditText etSearchUsers;
    private ChipGroup chipGroupUserTypes;
    private ProgressBar progressBar;
    private TextView tvNoUsersFound;

    // Adapter dan Data
    private AdminUserAdapter userAdapter;
    private List<User> allUsersList;

    // Firebase
    private DatabaseReference usersRef;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_activity_manage_users);

        usersRef = FirebaseDatabase.getInstance().getReference("Users");
        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupRecyclerView();
        setupFilterChips();
        setupSearch();
        fetchUsersFromFirebase();
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_manage_users);
        toolbar.setNavigationOnClickListener(v -> finish());
        rvUsers = findViewById(R.id.rv_all_users);
        etSearchUsers = findViewById(R.id.et_search_users);
        chipGroupUserTypes = findViewById(R.id.chipGroup_user_types);
        progressBar = findViewById(R.id.progress_bar_manage_users);
        tvNoUsersFound = findViewById(R.id.tv_no_users_found);
    }

    private void setupRecyclerView() {
        allUsersList = new ArrayList<>();
        userAdapter = new AdminUserAdapter(this, new ArrayList<>(), this);
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        rvUsers.setAdapter(userAdapter);
    }

    private void fetchUsersFromFirebase() {
        progressBar.setVisibility(View.VISIBLE);
        tvNoUsersFound.setVisibility(View.GONE);

        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                allUsersList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        user.setUid(snapshot.getKey());
                        allUsersList.add(user);
                    }
                }
                filterUsers();
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ManageUsersActivity.this, "Failed to load users: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Firebase DB Error: " + databaseError.getMessage());
            }
        });
    }

    private void setupFilterChips() {
        chipGroupUserTypes.setOnCheckedStateChangeListener((group, checkedIds) -> filterUsers());
    }

    private void setupSearch() {
        etSearchUsers.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterUsers() {
        String searchQuery = etSearchUsers.getText().toString().toLowerCase(Locale.ROOT);
        List<String> selectedRoles = getSelectedRoles();
        List<User> filteredList = new ArrayList<>();

        for (User user : allUsersList) {
            boolean matchesSearch = (user.getUsername() != null && user.getUsername().toLowerCase(Locale.ROOT).contains(searchQuery)) ||
                    (user.getEmail() != null && user.getEmail().toLowerCase(Locale.ROOT).contains(searchQuery));

            boolean matchesRole = selectedRoles.isEmpty() || (user.getUserType() != null && selectedRoles.contains(user.getUserType()));

            if (matchesSearch && matchesRole) {
                filteredList.add(user);
            }
        }
        userAdapter.updateList(filteredList);
        updateEmptyState(filteredList.isEmpty());
    }

    private List<String> getSelectedRoles() {
        List<String> selectedRoles = new ArrayList<>();
        for (int id : chipGroupUserTypes.getCheckedChipIds()) {
            Chip chip = findViewById(id);
            if (chip != null) {
                String chipText = chip.getText().toString();
                if (chipText.equalsIgnoreCase("Buyers")) {
                    selectedRoles.add("Buyer");
                } else if (chipText.equalsIgnoreCase("Sellers")) {
                    selectedRoles.add("Seller");
                } else if (chipText.equalsIgnoreCase("Admins")) {
                    selectedRoles.add("Admin");
                }
            }
        }
        return selectedRoles;
    }

    private void showUserActionDialog(User user) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && user.getUid().equals(currentUser.getUid())) {
            Toast.makeText(this, "You cannot perform actions on your own account.", Toast.LENGTH_SHORT).show();
            return;
        }

        final CharSequence[] options = {"Toggle Suspend Status", "Change Role", "Delete User", "Cancel"};

        new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setTitle("Actions for: " + user.getUsername())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            toggleSuspendStatus(user);
                            break;
                        case 1:
                            showChangeRoleDialog(user);
                            break;
                        case 2:
                            confirmDeleteUser(user);
                            break;
                        case 3:
                            dialog.dismiss();
                            break;
                    }
                })
                .show();
    }

    private void toggleSuspendStatus(User user) {
        boolean newStatus = !Boolean.TRUE.equals(user.isSuspended());
        usersRef.child(user.getUid()).child("suspended").setValue(newStatus)
                .addOnSuccessListener(aVoid -> {
                    // ▼▼▼ 2. TAMBAH LOG UNTUK STATUS PENGGANTUNGAN ▼▼▼
                    String action = newStatus ? "Account Suspended" : "Account Reactivated";
                    String logDetails = "Admin changed suspend status for user: " + user.getEmail() + " to " + newStatus;
                    LogHelper.logCurrentUserAction(action, logDetails, "Admin");
                    // ▲▲▲ AKHIR LOG ▲▲▲

                    Toast.makeText(ManageUsersActivity.this, "User status updated.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(ManageUsersActivity.this, "Failed to update status: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showChangeRoleDialog(User user) {
        final String[] roles = {"Buyer", "Seller", "Admin"};
        int currentRoleIndex = user.getUserType() != null ? Arrays.asList(roles).indexOf(user.getUserType()) : -1;

        new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setTitle("Change Role for " + user.getUsername())
                .setSingleChoiceItems(roles, currentRoleIndex, null)
                .setPositiveButton("Save", (dialog, which) -> {
                    int selectedPosition = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
                    if (selectedPosition != -1) {
                        String newRole = roles[selectedPosition];
                        String oldRole = user.getUserType(); // Simpan peranan lama untuk log

                        usersRef.child(user.getUid()).child("userType").setValue(newRole)
                                .addOnSuccessListener(aVoid -> {
                                    // ▼▼▼ 2. TAMBAH LOG UNTUK PERUBAHAN PERANAN ▼▼▼
                                    String logDetails = "Admin changed role for user: " + user.getEmail() + " from '" + oldRole + "' to '" + newRole + "'";
                                    LogHelper.logCurrentUserAction("User Role Changed", logDetails, "Admin");
                                    // ▲▲▲ AKHIR LOG ▲▲▲

                                    Toast.makeText(ManageUsersActivity.this, "User role updated.", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> Toast.makeText(ManageUsersActivity.this, "Failed to update role: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDeleteUser(User user) {
        new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setTitle("Confirm Deletion")
                .setMessage("Are you sure you want to permanently delete user '" + user.getUsername() + "'? This only removes the database entry.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    usersRef.child(user.getUid()).removeValue()
                            .addOnSuccessListener(aVoid -> {
                                // ▼▼▼ 2. TAMBAH LOG UNTUK PEMADAMAN PENGGUNA ▼▼▼
                                String logDetails = "Admin deleted user data for: " + user.getEmail() + " (UID: " + user.getUid() + ")";
                                LogHelper.logCurrentUserAction("User Data Deleted", logDetails, "Admin");
                                // ▲▲▲ AKHIR LOG ▲▲▲

                                Toast.makeText(ManageUsersActivity.this, "User data deleted from database.", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> Toast.makeText(ManageUsersActivity.this, "Failed to delete user data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateEmptyState(boolean isEmpty) {
        rvUsers.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        tvNoUsersFound.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onActionClick(User user) {
        showUserActionDialog(user);
    }
}
