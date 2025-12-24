package com.example.beautyhub.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.beautyhub.R;
import com.example.beautyhub.models.ProductListing;
import com.example.beautyhub.models.User; // Anda perlukan model User untuk dapatkan nama kedai
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.List;
import java.util.Locale;

public class PriceComparisonAdapter extends RecyclerView.Adapter<PriceComparisonAdapter.ListingViewHolder> {

    private final Context context;
    private final List<ProductListing> listingList;
    private final OnItemActionListener listener;

    // Interface untuk tindakan seperti "Tambah ke Troli"
    public interface OnItemActionListener {
        void onAddToCartClicked(ProductListing listing);
    }

    public PriceComparisonAdapter(Context context, List<ProductListing> listingList, OnItemActionListener listener) {
        this.context = context;
        this.listingList = listingList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ListingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Kita akan cipta layout baru untuk item ini
        View view = LayoutInflater.from(context).inflate(R.layout.item_price_comparison, parent, false);
        return new ListingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ListingViewHolder holder, int position) {
        ProductListing listing = listingList.get(position);
        holder.bind(listing);
    }

    @Override
    public int getItemCount() {
        return listingList.size();
    }

    class ListingViewHolder extends RecyclerView.ViewHolder {
        TextView textViewSellerName, textViewPrice, textViewStock;
        Button buttonAddToCart;

        public ListingViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewSellerName = itemView.findViewById(R.id.text_view_seller_name);
            textViewPrice = itemView.findViewById(R.id.text_view_price);
            textViewStock = itemView.findViewById(R.id.text_view_stock);
            buttonAddToCart = itemView.findViewById(R.id.button_add_to_cart);
        }

        void bind(final ProductListing listing) {
            // Paparkan harga dan stok
            textViewPrice.setText(String.format(Locale.getDefault(), "RM %.2f", listing.getPrice()));
            textViewStock.setText("Stok: " + listing.getStock());

            // Dapatkan nama penjual dari /Users/{sellerId}
            loadSellerInfo(listing.getSellerId());

            // Set listener untuk butang
            buttonAddToCart.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAddToCartClicked(listing);
                }
            });
        }

        private void loadSellerInfo(String sellerId) {
            DatabaseReference sellerRef = FirebaseDatabase.getInstance().getReference("Users").child(sellerId);
            sellerRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        User seller = dataSnapshot.getValue(User.class);
                        if (seller != null && seller.getUsername() != null) {
                            // Anda mungkin mahu guna 'storeName' jika ada, jika tidak, 'username' pun boleh
                            textViewSellerName.setText(seller.getUsername());
                        } else {
                            textViewSellerName.setText("Penjual tidak diketahui");
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    textViewSellerName.setText("Gagal memuatkan nama penjual");
                }
            });
        }
    }
}
