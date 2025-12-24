package com.example.beautyhub.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beautyhub.R;
import com.example.beautyhub.models.Category;

import java.util.List;

/**
 * Adapter ini digunakan untuk memaparkan senarai kategori yang terperinci
 * di dalam laman pentadbir (ManageCategoriesActivity).
 */
public class AdminCategoryAdapter extends RecyclerView.Adapter<AdminCategoryAdapter.CategoryViewHolder> {

    private List<Category> categoryList;
    private final OnCategoryActionListener listener;

    public interface OnCategoryActionListener {
        void onActionClick(Category category);
    }

    public AdminCategoryAdapter(List<Category> categoryList, OnCategoryActionListener listener) {
        this.categoryList = categoryList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.a_item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categoryList.get(position);
        holder.bind(category, listener);
    }

    @Override
    public int getItemCount() {
        return categoryList != null ? categoryList.size() : 0;
    }

    public void updateList(List<Category> newList) {
        this.categoryList = newList;
        notifyDataSetChanged();
    }

    class CategoryViewHolder extends RecyclerView.ViewHolder {
        // ▼▼▼ PERBAIKAN: Buang TextView yang tidak perlu ▼▼▼
        private final TextView tvCategoryName;
        private final ImageView ivActionMenu;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            // ▼▼▼ PERBAIKAN: Selaraskan dengan layout yang ringkas ▼▼▼
            tvCategoryName = itemView.findViewById(R.id.tv_category_name);
            ivActionMenu = itemView.findViewById(R.id.iv_delete_category);
        }

        public void bind(final Category category, final OnCategoryActionListener listener) {
            // ▼▼▼ PERBAIKAN: Guna getCategoryName() ▼▼▼
            tvCategoryName.setText(category.getCategoryName());

            // Tetapkan listener klik pada ikon menu tiga titik
            ivActionMenu.setOnClickListener(v -> listener.onActionClick(category));

            // Listener untuk keseluruhan item
            itemView.setOnClickListener(v -> listener.onActionClick(category));
        }
    }
}
