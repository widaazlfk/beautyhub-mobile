package com.example.beautyhub.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.beautyhub.R;
import com.example.beautyhub.buyer.FullScreenImageActivity;

import java.util.ArrayList;
import java.util.List;

public class ImageSliderAdapter extends RecyclerView.Adapter<ImageSliderAdapter.SliderViewHolder> {

    private final Context context;
    private final List<String> imageUrls;

    public ImageSliderAdapter(Context context, List<String> imageUrls) {
        this.context = context;
        this.imageUrls = imageUrls;
    }

    @NonNull
    @Override
    public SliderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Anda mungkin sudah mempunyai fail layout ini. Pastikan ia ada.
        View view = LayoutInflater.from(context).inflate(R.layout.item_image_slider, parent, false);
        return new SliderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SliderViewHolder holder, int position) {
        String imageUrl = imageUrls.get(position);
        Glide.with(context)
                .load(imageUrl)
                .placeholder(R.color.gray_light)
                .into(holder.imageView);

        // --- TAMBAHAN BAHARU ---
        // Tetapkan listener pada setiap imej
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, FullScreenImageActivity.class);
            // Hantar semua URL imej dan posisi imej yang sedang dilihat
            intent.putStringArrayListExtra(FullScreenImageActivity.EXTRA_IMAGE_URLS, new ArrayList<>(imageUrls));
            intent.putExtra(FullScreenImageActivity.EXTRA_CURRENT_POSITION, position);
            context.startActivity(intent);
        });
        // --- AKHIR TAMBAHAN ---
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    static class SliderViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public SliderViewHolder(@NonNull View itemView) {
            super(itemView);
            // Pastikan ID ini sepadan dengan ID dalam item_image_slider.xml
            imageView = itemView.findViewById(R.id.iv_slider_image);
        }
    }
}
