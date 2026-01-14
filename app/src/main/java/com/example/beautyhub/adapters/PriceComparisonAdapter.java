package com.example.beautyhub.adapters;

import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.beautyhub.R;
import com.example.beautyhub.buyer.ProductDetailActivity;
import com.example.beautyhub.databinding.ItemSimilarBeautyBinding;
import com.example.beautyhub.models.ProductComparison;
import java.util.List;

public class PriceComparisonAdapter extends RecyclerView.Adapter<PriceComparisonAdapter.ViewHolder> {
    private List<ProductComparison> list;

    public PriceComparisonAdapter(List<ProductComparison> list) { this.list = list; }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemSimilarBeautyBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProductComparison p = list.get(position);

        // 1. Set Nama dan Harga
        holder.binding.tvName.setText(p.getName());
        holder.binding.tvPriceStore.setText(p.getSellerName() + " - RM " + String.format("%.2f", p.getPrice()));

        // 2. Set Gambar (Pastikan bahagian ini di luar block else yang salah)
        if (p.getImageUrls() != null && !p.getImageUrls().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(p.getImageUrls().get(0))
                    .placeholder(R.drawable.product_placeholder)
                    .error(R.drawable.product_placeholder)
                    .into(holder.binding.ivProduct);
        } else {
            holder.binding.ivProduct.setImageResource(R.drawable.product_placeholder);
        }

        // 3. Set Match Percentage (Pastikan logik ini berada di luar block else gambar)
        int match = p.getMatchPercentage();
        if (match > 0) {
            holder.binding.tvIngredientCount.setVisibility(View.VISIBLE);
            holder.binding.tvIngredientCount.setText(match + "% Match");

            if (match >= 80) {
                holder.binding.tvIngredientCount.setBackgroundResource(R.drawable.bg_ingredient_tag);
                holder.binding.tvIngredientCount.setTextColor(Color.parseColor("#2E7D32"));
            } else if (match >= 50) {
                holder.binding.tvIngredientCount.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FFF9C4")));
                holder.binding.tvIngredientCount.setTextColor(Color.parseColor("#F57F17"));
            } else {
                holder.binding.tvIngredientCount.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F5F5F5")));
                holder.binding.tvIngredientCount.setTextColor(Color.parseColor("#757575"));
            }
        } else {
            holder.binding.tvIngredientCount.setVisibility(View.GONE);
        }

        // 4. Navigasi ke Product Detail
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ProductDetailActivity.class);
            // PENTING: Gunakan key yang sama dengan ComparisonActivity ("productId")
            intent.putExtra("productId", p.getProductId());
            v.getContext().startActivity(intent);
        });
    }

    @Override public int getItemCount() { return list.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ItemSimilarBeautyBinding binding;
        public ViewHolder(ItemSimilarBeautyBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}