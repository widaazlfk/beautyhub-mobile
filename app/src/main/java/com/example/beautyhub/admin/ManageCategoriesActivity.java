package com.example.beautyhub.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.beautyhub.R;
import com.example.beautyhub.adapters.CategoryAdapter;
import com.example.beautyhub.models.Category;
import com.google.android.material.appbar.MaterialToolbar; // Import ini
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class ManageCategoriesActivity extends AppCompatActivity implements CategoryAdapter.OnCategoryActionListener {

    private RecyclerView rvCategories;
    private CategoryAdapter categoryAdapter;
    private List<Category> categoryList;
    private DatabaseReference categoriesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_activity_manage_categories);

        // 1. Setup Toolbar dan Back Button
        setupToolbar();

        // Rujukan terus ke Firebase
        categoriesRef = FirebaseDatabase.getInstance().getReference("Categories");

        // Inisialisasi RecyclerView
        rvCategories = findViewById(R.id.rv_categories);
        categoryList = new ArrayList<>();
        categoryAdapter = new CategoryAdapter(this, categoryList, this);
        rvCategories.setLayoutManager(new LinearLayoutManager(this));
        rvCategories.setAdapter(categoryAdapter);

        // Butang untuk menambah kategori
        FloatingActionButton fabAddCategory = findViewById(R.id.fab_add_category);
        fabAddCategory.setOnClickListener(v -> showAddCategoryDialog());

        // Terus muatkan kategori
        fetchCategories();
    }

    private void setupToolbar() {
        // Gantikan R.id.toolbar_manage_categories dengan ID Toolbar dalam XML anda
        MaterialToolbar toolbar = findViewById(R.id.toolbar_manage_categories);

        if (toolbar != null) {
            setSupportActionBar(toolbar);

            // Memberikan fungsi butang kembali (Navigation Icon)
            toolbar.setNavigationOnClickListener(v -> {
                onBackPressed(); // Menutup aktiviti ini dan kembali ke Admin Dashboard
            });
        }
    }

    private void fetchCategories() {
        categoriesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                categoryList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Category category = dataSnapshot.getValue(Category.class);
                    if (category != null) {
                        category.setCategoryId(dataSnapshot.getKey());
                        categoryList.add(category);
                    }
                }
                categoryAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ManageCategoriesActivity.this, "Failed to load categories.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDeleteClick(Category category) {
        if (category.getCategoryId() != null) {
            categoriesRef.child(category.getCategoryId()).removeValue()
                    .addOnSuccessListener(aVoid -> Toast.makeText(ManageCategoriesActivity.this, "Category deleted.", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(ManageCategoriesActivity.this, "Deletion failed.", Toast.LENGTH_SHORT).show());
        }
    }

    private void showAddCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.a_dialog_add_edit_category, null);
        builder.setView(view);

        final EditText etCategoryName = view.findViewById(R.id.et_category_name);
        final Button btnSave = view.findViewById(R.id.btn_save_category);
        AlertDialog dialog = builder.create();

        btnSave.setOnClickListener(v -> {
            String name = etCategoryName.getText().toString().trim();
            if (TextUtils.isEmpty(name)) {
                etCategoryName.setError("Name is required");
                return;
            }

            String categoryId = categoriesRef.push().getKey();
            Category newCategory = new Category(categoryId, name);

            if (categoryId != null) {
                categoriesRef.child(categoryId).setValue(newCategory)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Category added.", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        });
            }
        });

        dialog.show();
    }
}