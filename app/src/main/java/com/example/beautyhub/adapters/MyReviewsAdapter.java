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

import com.example.beautyhub.R;
import com.example.beautyhub.models.Review;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// Pastikan nama fail dan kelas adalah MyReviewsAdapter
public class MyReviewsAdapter extends RecyclerView.Adapter<MyReviewsAdapter.ReviewViewHolder> {

    private final Context context;
    private final List<Review> reviewList;
    private OnReviewActionListener listener; // Boleh jadi null untuk mod pembeli

    private final boolean isActionMode; // Flag untuk menentukan mod

    // --- INTERFACE ---
    public interface OnReviewActionListener {
        void onActionClick(Review review);
    }

    // --- CONSTRUCTORS ---

    // Constructor untuk Admin/Seller (Mod Tindakan)
    public MyReviewsAdapter(Context context, List<Review> reviewList, OnReviewActionListener listener) {
        this.context = context;
        this.reviewList = reviewList;
        this.listener = listener;
        this.isActionMode = true; // Aktifkan mod tindakan
    }

    // Constructor untuk Pembeli (MyReviewsActivity - Mod Paparan Sahaja)
    public MyReviewsAdapter(Context context, List<Review> reviewList) {
        this.context = context;
        this.reviewList = reviewList;
        this.listener = null; // Tiada tindakan diperlukan
        this.isActionMode = false; // Matikan mod tindakan
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Anda perlu cipta fail layout 'a_item_review.xml' atau nama lain yang sesuai
        View view = LayoutInflater.from(context).inflate(R.layout.a_item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviewList.get(position);
        holder.bind(review, listener, isActionMode); // Hantar flag mod ke ViewHolder
    }

    @Override
    public int getItemCount() {
        return reviewList.size();
    }

    // --- VIEWHOLDER ---
    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvUsername, tvComment, tvDate;
        private final RatingBar ratingBar;
        private final ImageView ivDeleteReview;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tv_review_username);
            tvComment = itemView.findViewById(R.id.tv_review_comment);
            tvDate = itemView.findViewById(R.id.tv_review_date);
            ratingBar = itemView.findViewById(R.id.review_rating_bar);
            ivDeleteReview = itemView.findViewById(R.id.iv_delete_review);
        }

        public void bind(final Review review, final OnReviewActionListener listener, boolean isActionMode) {
            // Logik paparan yang sama untuk kedua-dua mod
            tvUsername.setText(review.getUsername());
            tvComment.setText(review.getComment());
            ratingBar.setRating(review.getRating());

            if (review.getTimestampLong() > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                tvDate.setText(sdf.format(new Date(review.getTimestampLong())));
            } else {
                tvDate.setText("N/A");
            }

            // Logik yang berbeza berdasarkan mod
            if (isActionMode && listener != null) {
                // Mod Admin/Seller: Tunjukkan ikon padam dan tetapkan listener
                ivDeleteReview.setVisibility(View.VISIBLE);
                ivDeleteReview.setOnClickListener(v -> listener.onActionClick(review));
            } else {
                // Mod Pembeli: Sembunyikan ikon padam
                ivDeleteReview.setVisibility(View.GONE);
            }
        }
    }
}
