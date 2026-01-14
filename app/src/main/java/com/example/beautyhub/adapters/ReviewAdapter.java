package com.example.beautyhub.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.beautyhub.R;
import com.example.beautyhub.models.Review;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    private final Context context;
    private final List<Review> reviewList;

    public ReviewAdapter(Context context, List<Review> reviewList) {
        this.context = context;
        this.reviewList = reviewList;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Menggunakan layout item_review_buyer.xml yang sudah anda buat
        View view = LayoutInflater.from(context).inflate(R.layout.item_review_buyer, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviewList.get(position);

        // 1. Set data teks
        holder.tvUserName.setText(review.getUsername() != null ? review.getUsername() : "Anonymous");
        holder.ratingBar.setRating(review.getRating());

        // Paparkan nama produk jika ada
        String title = (review.getProductName() != null) ? "Review for " + review.getProductName() : "Product Review";
        holder.tvReviewTitle.setText(title);
        holder.tvReviewContent.setText(review.getComment());

        // 2. Format tanggal
        long timestamp = review.getTimestampLong();
        if (timestamp > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            holder.tvReviewDate.setText(sdf.format(new Date(timestamp)));
        } else {
            holder.tvReviewDate.setText("");
        }

        // 3. Papar GAMBAR PRODUK (Gunakan productImageUrl)
        // Pastikan dalam model Review.java ada method getProductImageUrl()
        if (review.getProductImageUrl() != null && !review.getProductImageUrl().isEmpty()) {
            holder.ivProductImage.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(review.getProductImageUrl())
                    .placeholder(R.drawable.product_placeholder)
                    .error(R.drawable.product_placeholder)
                    .into(holder.ivProductImage);
        } else {
            // Jika tiada URL, kita paparkan placeholder supaya layout tidak lari
            holder.ivProductImage.setVisibility(View.VISIBLE);
            holder.ivProductImage.setImageResource(R.drawable.product_placeholder);
        }

        // 4. Gambar Profil User
        Glide.with(context)
                .load(R.drawable.ic_user_avatar)
                .into(holder.ivUserAvatar);

        // 5. Sembunyikan bahagian yang belum ada datanya (Helpful feature)
        holder.tvHelpfulCount.setVisibility(View.GONE);
        holder.btnHelpful.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return reviewList != null ? reviewList.size() : 0;
    }

    public static class ReviewViewHolder extends RecyclerView.ViewHolder {
        ImageView ivUserAvatar, ivProductImage;
        TextView tvUserName, tvReviewDate, tvReviewTitle, tvReviewContent, tvHelpfulCount;
        RatingBar ratingBar;
        Button btnHelpful;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            ivUserAvatar = itemView.findViewById(R.id.iv_user_avatar);
            tvUserName = itemView.findViewById(R.id.tv_user_name);
            ivProductImage = itemView.findViewById(R.id.iv_product_image); // Jika ada, tambahkan ini jika ada)
            tvReviewDate = itemView.findViewById(R.id.tv_review_date);
            ratingBar = itemView.findViewById(R.id.rating_bar);
            tvReviewTitle = itemView.findViewById(R.id.tv_review_title);
            tvReviewContent = itemView.findViewById(R.id.tv_review_content);
            tvHelpfulCount = itemView.findViewById(R.id.tv_helpful_count);
            btnHelpful = itemView.findViewById(R.id.btn_helpful);
        }
    }
}
