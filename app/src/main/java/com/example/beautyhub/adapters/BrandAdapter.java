package com.example.beautyhub.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beautyhub.R;

import java.util.List;

public class BrandAdapter extends RecyclerView.Adapter<BrandAdapter.ViewHolder> {

    private final List<String> brandList;
    private final OnBrandClickListener listener;
    private final Context context;

    // Interface untuk handle klik pada kotak brand
    public interface OnBrandClickListener {
        void onBrandClick(String brandName);
    }

    public BrandAdapter(Context context, List<String> brandList, OnBrandClickListener listener) {
        this.context = context;
        this.brandList = brandList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Menggunakan layout item_brand_box yang kita bincangkan sebelum ini
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_brand_box, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String brandName = brandList.get(position);
        holder.tvBrandName.setText(brandName);

        // Klik pada item
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBrandClick(brandName);
            }
        });
    }

    @Override
    public int getItemCount() {
        return brandList != null ? brandList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvBrandName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBrandName = itemView.findViewById(R.id.tvBrandName);
        }
    }
}