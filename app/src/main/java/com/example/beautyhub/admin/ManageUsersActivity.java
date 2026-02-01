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

    private RecyclerView rvUsers;
    private EditText etSearchUsers;
    private ChipGroup chipGroupUserTypes;
    private ProgressBar progressBar;
    private TextView tvNoUsersFound;

    private AdminUserAdapter userAdapter;
    private List<User> allUsersList;

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
                Toast.makeText(ManageUsersActivity.this, "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
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

            boolean matchesRole = true;
            if (!selectedRoles.isEmpty() && user.getUserType() != null) {
                // handles BUYER, Buyer, buyer by converting to lowercase
                matchesRole = selectedRoles.contains(user.getUserType().toLowerCase(Locale.ROOT));
            }

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
                String text = chip.getText().toString().toLowerCase(Locale.ROOT);
                if (text.contains("buyer")) selectedRoles.add("buyer");
                else if (text.contains("seller")) selectedRoles.add("seller");
                else if (text.contains("admin")) selectedRoles.add("admin");
            }
        }
        return selectedRoles;
    }

    private void showUserActionDialog(User user) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && user.getUid().equals(currentUser.getUid())) {
            Toast.makeText(this, "Cannot modify your own account.", Toast.LENGTH_SHORT).show();
            return;
        }

        final CharSequence[] options = {"Toggle Suspend Status", "Change Role", "Delete User", "Cancel"};
        new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setTitle("Actions for: " + user.getUsername())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) toggleSuspendStatus(user);
                    else if (which == 1) showChangeRoleDialog(user);
                    else if (which == 2) confirmDeleteUser(user);
                }).show();
    }

    private void toggleSuspendStatus(User user) {
        boolean newStatus = !Boolean.TRUE.equals(user.isSuspended());
        usersRef.child(user.getUid()).child("suspended").setValue(newStatus)
                .addOnSuccessListener(aVoid -> {
                    String action = newStatus ? "Suspended" : "Reactivated";
                    LogHelper.logCurrentUserAction(action, "Admin changed status for " + user.getEmail(), "Admin");
                    Toast.makeText(this, "Status updated.", Toast.LENGTH_SHORT).show();
                });
    }

    private void showChangeRoleDialog(User user) {
        final String[] roles = {"Buyer", "Seller", "Admin"};
        new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setTitle("Select New Role")
                .setItems(roles, (dialog, which) -> {
                    String newRole = roles[which];
                    usersRef.child(user.getUid()).child("userType").setValue(newRole)
                            .addOnSuccessListener(aVoid -> {
                                LogHelper.logCurrentUserAction("Role Change", "Changed " + user.getEmail() + " to " + newRole, "Admin");
                                Toast.makeText(this, "Role updated.", Toast.LENGTH_SHORT).show();
                            });
                }).show();
    }

    private void confirmDeleteUser(User user) {
        new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setTitle("Confirm Delete")
                .setMessage("Delete user " + user.getUsername() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    usersRef.child(user.getUid()).removeValue().addOnSuccessListener(aVoid -> {
                        LogHelper.logCurrentUserAction("Delete", "Deleted " + user.getEmail(), "Admin");
                        Toast.makeText(this, "Deleted.", Toast.LENGTH_SHORT).show();
                    });
                })
                .setNegativeButton("Cancel", null).show();
    }

    private void updateEmptyState(boolean isEmpty) {
        tvNoUsersFound.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvUsers.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    // This must be named onActionClick to match the interface in AdminUserAdapter
    @Override
    public void onActionClick(User user) {
        showUserActionDialog(user);
    }
}