package com.example.beautyhub.admin;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beautyhub.R;
// PERBAIKAN 1: Import kelas 'Review' dari pakej model anda
import com.example.beautyhub.models.Review;

import java.util.List;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    // PERBAIKAN 2: Hanya satu definisi antaramuka yang diperlukan
    public interface ReviewActionListener {
        void onApproveReview(Review review);
        void onRejectReview(Review review);
        void onViewDetails(Review review);
    }

    private List<Review> reviewList;
    private final ReviewActionListener listener;

    public ReviewAdapter(List<Review> reviewList, ReviewActionListener listener) {
        this.reviewList = reviewList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // PERBAIKAN 3: Guna nama reka letak item yang betul (bukan reka letak Activity)
        // Saya andaikan namanya 'a_item_review.xml'. Sila tukar jika perlu.
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.a_item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviewList.get(position);
        holder.bind(review, listener);
    }

    @Override
    public int getItemCount() {
        return reviewList.size();
    }

    public void updateList(List<Review> newList) {
        reviewList = newList;
        notifyDataSetChanged();
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvUserName, tvProductName, tvRating, tvContent, tvStatus;
        private final Button btnApprove, btnReject, btnViewDetails;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tv_user_name);
            tvProductName = itemView.findViewById(R.id.tv_product_name);
            tvRating = itemView.findViewById(R.id.tv_rating);
            tvContent = itemView.findViewById(R.id.tv_review_content);
            tvStatus = itemView.findViewById(R.id.tv_review_status);
            btnApprove = itemView.findViewById(R.id.btn_approve);
            btnReject = itemView.findViewById(R.id.btn_reject);
            btnViewDetails = itemView.findViewById(R.id.btn_view_details);
        }

        public void bind(final Review review, final ReviewActionListener listener) {
            // Andaian: Model 'Review' anda mempunyai kaedah-kaedah ini.
            tvUserName.setText(review.getUsername());
            tvProductName.setText(review.getProductName());
            tvRating.setText(String.format("%s ★", review.getRating()));
            tvContent.setText(review.getContent());
            tvStatus.setText(review.getStatus());

            // Tetapkan warna status
            if (review.getStatus() != null) {
                switch (review.getStatus()) {
                    case "PENDING":
                        tvStatus.setTextColor(Color.parseColor("#FFA000")); // Amber
                        break;
                    case "APPROVED":
                        tvStatus.setTextColor(Color.parseColor("#388E3C")); // Green
                        break;
                    case "REJECTED":
                        tvStatus.setTextColor(Color.parseColor("#D32F2F")); // Red
                        break;
                    default:
                        tvStatus.setTextColor(Color.GRAY);
                        break;
                }
            }

            // Tentukan sama ada butang 'Approve' dan 'Reject' perlu dipaparkan
            if ("PENDING".equals(review.getStatus())) {
                btnApprove.setVisibility(View.VISIBLE);
                btnReject.setVisibility(View.VISIBLE);
            } else {
                btnApprove.setVisibility(View.GONE);
                btnReject.setVisibility(View.GONE);
            }

            // Tetapkan listener untuk butang
            btnApprove.setOnClickListener(v -> listener.onApproveReview(review));
            btnReject.setOnClickListener(v -> listener.onRejectReview(review));
            btnViewDetails.setOnClickListener(v -> listener.onViewDetails(review));
        }
    }
}
