package com.example.beautyhub.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide; // Pastikan Glide diimport
import com.example.beautyhub.R;
import com.example.beautyhub.models.Review;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MyReviewsAdapter extends RecyclerView.Adapter<MyReviewsAdapter.ReviewViewHolder> {

    private final Context context;
    private final List<Review> reviewList;
    private OnReviewActionListener listener;
    private final boolean isActionMode;

    public interface OnReviewActionListener {
        void onActionClick(Review review);
    }

    public MyReviewsAdapter(Context context, List<Review> reviewList, OnReviewActionListener listener) {
        this.context = context;
        this.reviewList = reviewList;
        this.listener = listener;
        this.isActionMode = true;
    }

    public MyReviewsAdapter(Context context, List<Review> reviewList) {
        this.context = context;
        this.reviewList = reviewList;
        this.listener = null;
        this.isActionMode = false;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.a_item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviewList.get(position);
        holder.bind(review, context, listener, isActionMode); // Tambah context ke bind
    }

    @Override
    public int getItemCount() {
        return reviewList.size();
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvProductName, tvComment, tvDate; // Tambah tvProductName
        private final RatingBar ratingBar;
        private final ImageView ivDeleteReview, ivProductImage; // Tambah ivProductImage

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            // Pastikan ID ini wujud dalam a_item_review.xml
            ivProductImage = itemView.findViewById(R.id.iv_review_product_image);
            tvProductName = itemView.findViewById(R.id.tv_review_product_name);
            tvComment = itemView.findViewById(R.id.tv_review_comment);
            tvDate = itemView.findViewById(R.id.tv_review_date);
            ratingBar = itemView.findViewById(R.id.review_rating_bar);
            ivDeleteReview = itemView.findViewById(R.id.iv_delete_review);
        }

        public void bind(final Review review, Context context, final OnReviewActionListener listener, boolean isActionMode) {
            // 1. Papar Nama Produk (Dulu guna username, sekarang guna ProductName lebih sesuai untuk Buyer)
            tvProductName.setText(review.getProductName());
            tvComment.setText(review.getComment());
            ratingBar.setRating(review.getRating());

            // 2. Papar Gambar Produk guna Glide
            if (review.getProductImageUrl() != null && !review.getProductImageUrl().isEmpty()) {
                Glide.with(context)
                        .load(review.getProductImageUrl())
                        .placeholder(R.drawable.product_placeholder)
                        .error(R.drawable.product_placeholder)
                        .into(ivProductImage);
            } else {
                ivProductImage.setImageResource(R.drawable.product_placeholder);
            }

            // 3. Papar Tarikh
            if (review.getTimestampLong() > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                tvDate.setText(sdf.format(new Date(review.getTimestampLong())));
            } else {
                tvDate.setText("N/A");
            }

            // 4. Logik Mod Tindakan (Padam)
            if (isActionMode && listener != null) {
                ivDeleteReview.setVisibility(View.VISIBLE);
                ivDeleteReview.setOnClickListener(v -> listener.onActionClick(review));
            } else {
                ivDeleteReview.setVisibility(View.GONE);
            }
        }
    }
}
