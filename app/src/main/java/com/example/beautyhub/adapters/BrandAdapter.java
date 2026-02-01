package com.example.beautyhub.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.beautyhub.R;
import com.example.beautyhub.models.Product;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class BrandAdapter extends RecyclerView.Adapter<BrandAdapter.ViewHolder> {

    private final List<Product> brandProductList;
    private final OnBrandClickListener listener;
    private final Context context;

    public interface OnBrandClickListener {
        void onBrandClick(Product product);
    }

    public BrandAdapter(Context context, List<Product> brandProductList, OnBrandClickListener listener) {
        this.context = context;
        this.brandProductList = brandProductList;
        this.listener = listener;
    }

    // --- TAMBAH KAEDAH INI UNTUK MENYELESAIKAN RALAT updateList ---
    public void updateList(List<Product> newList) {
        this.brandProductList.clear();
        if (newList != null) {
            this.brandProductList.addAll(newList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_brand_box, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = brandProductList.get(position);

        // Set Nama Brand
        holder.tvBrandName.setText(product.getBrand());

        // Set Logo Seller menggunakan profil image yang disimpan dalam model Product
        if (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) {
            Glide.with(context)
                    .load(product.getImageUrls().get(0))
                    .placeholder(R.drawable.ic_shop)
                    .error(R.drawable.ic_shop)
                    .centerCrop()
                    .into(holder.ivBrandLogo);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBrandClick(product);
            }
        });
    }

    @Override
    public int getItemCount() {
        return brandProductList != null ? brandProductList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvBrandName;
        CircleImageView ivBrandLogo;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBrandName = itemView.findViewById(R.id.tvBrandName);
            ivBrandLogo = itemView.findViewById(R.id.ivBrandLogo);
        }
    }
}