package com.example.beautyhub.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.beautyhub.R;
import com.example.beautyhub.models.Category;
import com.google.android.material.card.MaterialCardView;

import java.util.List;
import de.hdodenhof.circleimageview.CircleImageView;

public class BuyerCategoryAdapter extends RecyclerView.Adapter<BuyerCategoryAdapter.CategoryViewHolder> {

    private final Context context;
    private final List<Category> categoryList;
    private final OnCategoryClickListener listener;

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
        View view = LayoutInflater.from(context).inflate(R.layout.item_buyer_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categoryList.get(position);

        // 1. Ambil nama dan bersihkan (Contoh: " Cleansers " -> "cleanser")
        // Kita buang jarak, tukar huruf kecil, dan buang 's' di hujung supaya lebih tepat
        String originalName = category.getCategoryName();
        String name = "";
        if (originalName != null) {
            name = originalName.toLowerCase().trim();
        }

        holder.tvCategoryName.setText(originalName);

        int imageResId;
        int bgColor;

        // --- ICON MAPPING (Guna .contains() supaya 'cleansers' padan dengan 'cleanser') ---
        if (name.contains("moisturizer")) {
            imageResId = R.drawable.moisturizer_icon;
            bgColor = Color.parseColor("#E3F2FD");
        } else if (name.contains("serum")) {
            imageResId = R.drawable.serum_icon;
            bgColor = Color.parseColor("#F3E5F5");
        } else if (name.contains("toner")) {
            imageResId = R.drawable.toner_icon;
            bgColor = Color.parseColor("#E8F5E9");
        } else if (name.contains("cleanser")) { // Akan padan dengan "Cleansers"
            imageResId = R.drawable.cleanser_icon;
            bgColor = Color.parseColor("#E0F7FA");
        } else if (name.contains("mask")) { // Padan dengan "Face Mask" atau "FaceMask"
            imageResId = R.drawable.facemask_icon;
            bgColor = Color.parseColor("#F1F8E9");
        } else if (name.contains("sunscreen")) {
            imageResId = R.drawable.sunscreen_icon;
            bgColor = Color.parseColor("#FFF3E0");
        } else if (name.contains("lipstick")) {
            imageResId = R.drawable.lipstick_icon;
            bgColor = Color.parseColor("#FFEBEE");
        } else if (name.contains("eyeliner")) {
            imageResId = R.drawable.eyeliner_icon;
            bgColor = Color.parseColor("#ECEFF1");
        } else if (name.contains("powder")) {
            imageResId = R.drawable.powder_icon;
            bgColor = Color.parseColor("#EFEBE9");
        } else if (name.contains("mascara")) {
            imageResId = R.drawable.mascara_icon;
            bgColor = Color.parseColor("#F5F5F5");
        } else if (name.contains("foundation")) {
            imageResId = R.drawable.foundation_icon;
            bgColor = Color.parseColor("#FFF8E1");
        } else if (name.contains("concealer")) {
            imageResId = R.drawable.concealer_icon;
            bgColor = Color.parseColor("#FFFDE7");
        } else if (name.contains("primer")) {
            imageResId = R.drawable.primer_icon;
            bgColor = Color.parseColor("#F9FBE7");
        } else if (name.contains("blusher")) {
            imageResId = R.drawable.blusher_icon;
            bgColor = Color.parseColor("#FCE4EC");
        } else if (name.contains("eyeshadow")) {
            imageResId = R.drawable.eyeshadow_icon;
            bgColor = Color.parseColor("#F8BBD0");
        } else {
            imageResId = R.drawable.product_placeholder;
            bgColor = Color.parseColor("#F5F5F5");
        }

        // Apply Background & Ikon
        holder.categoryCard.setCardBackgroundColor(bgColor);
        Glide.with(context)
                .load(imageResId)
                .into(holder.ivCategoryImage);

        holder.itemView.setOnClickListener(v -> listener.onCategoryClick(category));
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvCategoryName;
        private final CircleImageView ivCategoryImage;
        private final MaterialCardView categoryCard; // CardView untuk background warna

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tv_category_name);
            ivCategoryImage = itemView.findViewById(R.id.iv_category_image);
            categoryCard = itemView.findViewById(R.id.category_card);
        }
    }
}