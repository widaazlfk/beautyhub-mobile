package com.example.beautyhub.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beautyhub.databinding.BItemCarouselBinding;

import java.util.List;

public class CarouselAdapter extends RecyclerView.Adapter<CarouselAdapter.CarouselViewHolder> {

    private final List<Integer> imageList; // Akan menyimpan ID imej dari folder drawable

    public CarouselAdapter(List<Integer> imageList) {
        this.imageList = imageList;
    }

    @NonNull
    @Override
    public CarouselViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        BItemCarouselBinding binding = BItemCarouselBinding.inflate(inflater, parent, false);
        return new CarouselViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CarouselViewHolder holder, int position) {
        holder.bind(imageList.get(position));
    }

    @Override
    public int getItemCount() {
        return imageList.size();
    }

    static class CarouselViewHolder extends RecyclerView.ViewHolder {
        private final BItemCarouselBinding binding;

        public CarouselViewHolder(@NonNull BItemCarouselBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(int imageResource) {
            binding.carouselImageView.setImageResource(imageResource);
        }
    }
}
