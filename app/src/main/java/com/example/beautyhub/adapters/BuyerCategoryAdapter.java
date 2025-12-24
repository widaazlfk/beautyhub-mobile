package com.example.beautyhub.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beautyhub.R;
import com.example.beautyhub.models.Category;

import java.util.List;

public class BuyerCategoryAdapter extends RecyclerView.Adapter<BuyerCategoryAdapter.CategoryViewHolder> {

    private final Context context;
    private final List<Category> categoryList;
    private final OnCategoryClickListener listener;

    // Interface for handling simple clicks
    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    public BuyerCategoryAdapter(Context context, List<Category> categoryList, OnCategoryClickListener listener) {
        this.context = context;
        this.categoryList = categoryList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Use a different layout file for the buyer, without a delete button
        View view = LayoutInflater.from(context).inflate(R.layout.buyer_item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categoryList.get(position);
        holder.bind(category, listener);
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }

    class CategoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvCategoryName;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            // This layout only needs a TextView
            tvCategoryName = itemView.findViewById(R.id.tv_category_name);
        }

        public void bind(final Category category, final OnCategoryClickListener listener) {
            tvCategoryName.setText(category.getCategoryName());
            // Set the click listener on the whole item
            itemView.setOnClickListener(v -> listener.onCategoryClick(category));
        }
    }
}
