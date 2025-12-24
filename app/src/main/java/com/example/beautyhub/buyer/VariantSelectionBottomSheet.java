package com.example.beautyhub.buyer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.Glide;
import com.example.beautyhub.R;
import com.example.beautyhub.databinding.BottomSheetVariantSelectionBinding;
import com.example.beautyhub.models.Product;
import com.example.beautyhub.models.Variant;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.Locale;

public class VariantSelectionBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "VariantSelectionBottomSheet";
    private static final String ARG_PRODUCT_ID = "product_id";

    private BottomSheetVariantSelectionBinding binding;
    private String productId;
    private Product currentProduct;
    private Variant selectedVariant;
    private int quantity = 1;
    private boolean isBuyNowMode = false;

    public interface VariantSelectionListener {
        void onVariantSelected(Product product, Variant variant, int quantity);
    }

    private VariantSelectionListener variantSelectionListener;

    public void setVariantSelectionListener(VariantSelectionListener listener) {
        this.variantSelectionListener = listener;
    }

    public void setBuyNowMode(boolean isBuyNowMode) {
        this.isBuyNowMode = isBuyNowMode;
    }

    public static VariantSelectionBottomSheet newInstance(String productId) {
        VariantSelectionBottomSheet fragment = new VariantSelectionBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_PRODUCT_ID, productId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            productId = getArguments().getString(ARG_PRODUCT_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetVariantSelectionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (productId == null) {
            Toast.makeText(getContext(), "Product ID is missing.", Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }

        loadProductDetails();
        setupListeners();

        // Teks butang diset di dalam updatePriceAndStock, tetapi kita set awal di sini juga
        if (isBuyNowMode) {
            binding.btnConfirmAddToCart.setText("Buy Now");
        }
    }

    private void loadProductDetails() {
        DatabaseReference productRef = FirebaseDatabase.getInstance().getReference("Products").child(productId);
        productRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    currentProduct = snapshot.getValue(Product.class);
                    if (currentProduct != null) {
                        currentProduct.setProductId(snapshot.getKey());
                        displayProductInfo();
                    }
                } else {
                    Toast.makeText(getContext(), "Product not found.", Toast.LENGTH_SHORT).show();
                    dismiss();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load product details.", Toast.LENGTH_SHORT).show();
                dismiss();
            }
        });
    }

    private void displayProductInfo() {
        if (currentProduct == null) return;

        // Papar gambar
        if (currentProduct.getImageUrls() != null && !currentProduct.getImageUrls().isEmpty()) {
            Glide.with(this)
                    .load(currentProduct.getImageUrls().get(0))
                    .placeholder(R.drawable.placeholder)
                    .into(binding.ivProductImageBottomSheet);
        }

        // Check jika product ada variants
        if (currentProduct.getVariants() != null && !currentProduct.getVariants().isEmpty()) {
            binding.tvVariantLabelBottomSheet.setVisibility(View.VISIBLE);
            binding.chipGroupVariantsBottomSheet.setVisibility(View.VISIBLE);
            binding.chipGroupVariantsBottomSheet.removeAllViews(); // Bersihkan chip lama

            for (Variant variant : currentProduct.getVariants()) {
                Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_chip_filter,
                        binding.chipGroupVariantsBottomSheet, false);
                chip.setText(variant.getName());
                chip.setTag(variant);

                // ▼▼▼ DISABLE jika out of stock ▼▼▼
                if (variant.getStock() <= 0) {
                    chip.setEnabled(false);
                    chip.setAlpha(0.5f);
                    chip.setText(variant.getName() + " (Out of stock)");
                }

                binding.chipGroupVariantsBottomSheet.addView(chip);
            }

            // Auto-select varian pertama YANG ADA STOK
            boolean isAnyVariantSelected = false;
            for (int i = 0; i < binding.chipGroupVariantsBottomSheet.getChildCount(); i++) {
                Chip chip = (Chip) binding.chipGroupVariantsBottomSheet.getChildAt(i);
                if (chip.isEnabled()) {
                    chip.setChecked(true);
                    selectedVariant = (Variant) chip.getTag();
                    isAnyVariantSelected = true;
                    break;
                }
            }
            // Jika semua varian habis, pastikan selectedVariant adalah null
            if (!isAnyVariantSelected) {
                selectedVariant = null;
            }

        } else {
            binding.tvVariantLabelBottomSheet.setVisibility(View.GONE);
            binding.chipGroupVariantsBottomSheet.setVisibility(View.GONE);
            selectedVariant = null; // Tiada varian, jadi pastikan ia null
        }

        updatePriceAndStock();
    }

    private void setupListeners() {
        binding.btnCloseBottomSheet.setOnClickListener(v -> dismiss());

        binding.chipGroupVariantsBottomSheet.setOnCheckedChangeListener((group, checkedId) -> {
            Chip checkedChip = group.findViewById(checkedId);
            if (checkedChip != null && checkedChip.isChecked()) {
                selectedVariant = (Variant) checkedChip.getTag();
            }
            // Reset kuantiti ke 1 setiap kali varian ditukar untuk elak kekeliruan
            quantity = 1;
            binding.tvQuantity.setText(String.valueOf(quantity));
            updatePriceAndStock();
        });


        binding.btnIncreaseQuantity.setOnClickListener(v -> {
            int availableStock = getAvailableStock();
            if (quantity < availableStock) {
                quantity++;
                binding.tvQuantity.setText(String.valueOf(quantity));
                updatePriceAndStock();
            } else {
                Toast.makeText(getContext(), "Maximum quantity reached for this item.", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnDecreaseQuantity.setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                binding.tvQuantity.setText(String.valueOf(quantity));
                updatePriceAndStock();
            }
        });

        // ▼▼▼ KOD YANG DIPERBAIKI ▼▼▼
        binding.btnConfirmAddToCart.setOnClickListener(v -> {
            // Validation
            if (currentProduct == null) {
                Toast.makeText(getContext(), "Product not loaded yet. Please wait.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Pastikan varian dipilih jika produk mempunyai varian
            if (currentProduct.hasVariants() && selectedVariant == null) {
                Toast.makeText(getContext(), "Please select a variant.", Toast.LENGTH_SHORT).show();
                return;
            }

            int availableStock = getAvailableStock();

            // Semak stok
            if (availableStock <= 0) {
                String message = selectedVariant != null ?
                        "Selected variant is out of stock." :
                        "This product is out of stock.";
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                return;
            }

            if (quantity <= 0) {
                Toast.makeText(getContext(), "Please select a valid quantity.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (quantity > availableStock) {
                Toast.makeText(getContext(),
                        "Quantity exceeds available stock (" + availableStock + ").",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Guna listener untuk hantar data kembali, bukan rujukan terus ke ShopActivity
            if (variantSelectionListener != null) {
                variantSelectionListener.onVariantSelected(currentProduct, selectedVariant, quantity);
                dismiss(); // Tutup bottom sheet hanya jika berjaya dihantar
            } else {
                // Mesej ini membantu untuk nyahpepijat jika listener tidak di-set di ShopActivity
                Toast.makeText(getContext(), "Action listener is not configured.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int getAvailableStock() {
        if (currentProduct == null) return 0;

        if (selectedVariant != null) {
            return selectedVariant.getStock();
        }
        // Jika tiada varian, guna stok produk utama
        return currentProduct.getStock();
    }

    private void updatePriceAndStock() {
        if (currentProduct == null) return;

        double basePrice = currentProduct.hasDiscount() ?
                currentProduct.getDiscountPrice() :
                currentProduct.getPrice();

        int availableStock = getAvailableStock();

        // Tambah harga pengubah suai varian jika ada
        if (selectedVariant != null) {
            basePrice += selectedVariant.getPriceModifier();
        }

        double totalPrice = basePrice * quantity;

        binding.tvProductPriceBottomSheet.setText(String.format(Locale.US, "RM%.2f", totalPrice));
        binding.tvProductStockBottomSheet.setText(String.format(Locale.US, "Stock: %d", availableStock));

        // Dayakan atau lumpuhkan butang kuantiti berdasarkan stok
        binding.btnIncreaseQuantity.setEnabled(quantity < availableStock);
        binding.btnDecreaseQuantity.setEnabled(quantity > 1);
        binding.btnConfirmAddToCart.setEnabled(availableStock > 0);

        // Jika stok rendah, tukar warna teks
        if (getContext() != null) {
            if (availableStock > 0 && availableStock <= 5) {
                binding.tvProductStockBottomSheet.setTextColor(getResources().getColor(R.color.orange_500));
            } else {
                binding.tvProductStockBottomSheet.setTextColor(getResources().getColor(R.color.gray_600));
            }
        }

        // Kemas kini teks butang
        String buttonText = isBuyNowMode ? "Buy Now" : "Add to Cart";
        if (availableStock <= 0) {
            binding.btnConfirmAddToCart.setText("Out of Stock");
        } else {
            if (quantity > 1) {
                buttonText += " (" + quantity + ")";
            }
            binding.btnConfirmAddToCart.setText(buttonText);
        }
    }

    public void setInitialQuantity(int quantity) {
        this.quantity = quantity;
        if (binding != null) {
            // Pastikan kuantiti awal tidak melebihi stok
            int availableStock = getAvailableStock();
            if (this.quantity > availableStock) {
                this.quantity = availableStock > 0 ? 1 : 0;
            }
            if(this.quantity <= 0) this.quantity = 1;

            binding.tvQuantity.setText(String.valueOf(this.quantity));
            updatePriceAndStock();
        }
    }
}
