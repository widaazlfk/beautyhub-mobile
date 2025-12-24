package com.example.beautyhub.buyer;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.beautyhub.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.RangeSlider;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Arrays;
import java.util.List;

public class FilterBottomSheetDialog extends BottomSheetDialogFragment {

    public static final String TAG = "FilterBottomSheetDialog";

    // Keys for passing data
    private static final String ARG_CATEGORY = "category";
    private static final String ARG_BRAND = "brand";
    private static final String ARG_MIN_PRICE = "min_price";
    private static final String ARG_MAX_PRICE = "max_price";
    private static final String ARG_SKIN_TYPE = "skin_type";
    private static final String ARG_INGREDIENT = "ingredient";

    private FilterListener listener;

    // View untuk semua filter
    private ChipGroup chipGroupCategory, chipGroupSkinType;
    private TextInputEditText etBrand, etIngredient;
    private RangeSlider priceSlider;
    private Button btnApply, btnReset;

    // Current filter values
    private String currentCategory;
    private String currentBrand;
    private float currentMinPrice;
    private float currentMaxPrice;
    private String currentSkinType;
    private String currentIngredient;

    // Interface untuk berkomunikasi dengan Activity/Fragment pemanggil
    public interface FilterListener {
        void onFilterApplied(String category, String brand, float minPrice, float maxPrice, String skinType, String ingredient);
    }

    // Factory method untuk membuat instance baru dengan arguments
    public static FilterBottomSheetDialog newInstance(String category, String brand,
                                                      float minPrice, float maxPrice,
                                                      String skinType, String ingredient) {
        FilterBottomSheetDialog fragment = new FilterBottomSheetDialog();
        Bundle args = new Bundle();
        args.putString(ARG_CATEGORY, category);
        args.putString(ARG_BRAND, brand);
        args.putFloat(ARG_MIN_PRICE, minPrice);
        args.putFloat(ARG_MAX_PRICE, maxPrice);
        args.putString(ARG_SKIN_TYPE, skinType);
        args.putString(ARG_INGREDIENT, ingredient);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Read arguments
        if (getArguments() != null) {
            currentCategory = getArguments().getString(ARG_CATEGORY, "All");
            currentBrand = getArguments().getString(ARG_BRAND, "");
            currentMinPrice = getArguments().getFloat(ARG_MIN_PRICE, 10.0f);
            currentMaxPrice = getArguments().getFloat(ARG_MAX_PRICE, 200.0f);
            currentSkinType = getArguments().getString(ARG_SKIN_TYPE, "All");
            currentIngredient = getArguments().getString(ARG_INGREDIENT, "");
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Cuba dapatkan listener dari Activity (ShopActivity)
        if (context instanceof FilterListener) {
            listener = (FilterListener) context;
        } else if (getParentFragment() instanceof FilterListener) {
            listener = (FilterListener) getParentFragment();
        } else {
            throw new ClassCastException(context.toString() + " must implement FilterListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_filter, container, false);

        // Inisialisasi semua view dari layout
        initViews(view);

        // Set nilai semasa pada view
        setupCurrentFilterValues();

        // Tetapkan listener untuk butang-butang
        setupListeners();

        return view;
    }

    private void initViews(View view) {
        chipGroupCategory = view.findViewById(R.id.chip_group_category);
        chipGroupSkinType = view.findViewById(R.id.chip_group_skin_type);
        etBrand = view.findViewById(R.id.et_filter_brand);
        etIngredient = view.findViewById(R.id.et_filter_ingredient);
        priceSlider = view.findViewById(R.id.slider_price_range);
        btnApply = view.findViewById(R.id.btn_apply_filter);
        btnReset = view.findViewById(R.id.btn_reset_filter);
    }

    private void setupCurrentFilterValues() {
        // Set brand dan ingredient
        if (etBrand != null) {
            etBrand.setText(currentBrand);
        }

        if (etIngredient != null) {
            etIngredient.setText(currentIngredient);
        }

        // Set price slider values
        if (priceSlider != null) {
            priceSlider.setValues(currentMinPrice, currentMaxPrice);

            // Set label formatter untuk display harga
            priceSlider.setLabelFormatter(value -> String.format("RM %.0f", value));
        }

        // Set selected chips berdasarkan current values
        setSelectedChip(chipGroupCategory, currentCategory);
        setSelectedChip(chipGroupSkinType, currentSkinType);
    }

    private void setSelectedChip(ChipGroup chipGroup, String value) {
        if (chipGroup == null || value == null) return;

        // Jika value adalah "All" atau kosong, tidak pilih apa-apa
        if (value.equals("All") || value.isEmpty()) {
            chipGroup.clearCheck();
            return;
        }

        // Cari chip yang sesuai dengan value
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            View child = chipGroup.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                if (chip.getText().toString().equals(value)) {
                    chip.setChecked(true);
                    break;
                }
            }
        }
    }

    private void setupListeners() {
        // Listener untuk tombol "Apply"
        btnApply.setOnClickListener(v -> {
            // Ambil semua nilai dari input pengguna
            String selectedCategory = getSelectedChipText(chipGroupCategory);
            String brand = etBrand.getText() != null ? etBrand.getText().toString().trim() : "";
            String ingredient = etIngredient.getText() != null ? etIngredient.getText().toString().trim() : "";
            String selectedSkinType = getSelectedChipText(chipGroupSkinType);

            List<Float> prices = priceSlider.getValues();
            float minPrice = prices.get(0);
            float maxPrice = prices.get(1);

            // Validasi: min price tidak boleh lebih besar dari max price
            if (minPrice > maxPrice) {
                Toast.makeText(getContext(), "Minimum price cannot be higher than maximum price", Toast.LENGTH_SHORT).show();
                return;
            }

            // Hantar semua nilai kembali ke listener
            if (listener != null) {
                listener.onFilterApplied(selectedCategory, brand, minPrice, maxPrice, selectedSkinType, ingredient);
            }
            dismiss(); // Tutup bottom sheet selepas filter digunakan
        });

        // Listener untuk tombol "Reset"
        btnReset.setOnClickListener(v -> {
            // Reset semua nilai ke default
            resetAllFilters();

            // Hantar nilai reset kembali ke listener
            if (listener != null) {
                listener.onFilterApplied("All", "", 10.0f, 200.0f, "All", "");
            }

            // Boleh pilih untuk dismiss atau biarkan user apply reset
            dismiss(); // Tutup bottom sheet selepas filter direset
        });
    }

    private void resetAllFilters() {
        // Reset chip groups
        if (chipGroupCategory != null) {
            chipGroupCategory.clearCheck();
        }

        if (chipGroupSkinType != null) {
            chipGroupSkinType.clearCheck();
        }

        // Reset text fields
        if (etBrand != null) {
            etBrand.setText("");
        }

        if (etIngredient != null) {
            etIngredient.setText("");
        }

        // Reset price slider
        if (priceSlider != null) {
            priceSlider.setValues(10.0f, 200.0f);
        }
    }

    /**
     * Fungsi bantuan untuk mendapatkan teks dari Chip yang dipilih di dalam ChipGroup.
     * @param chipGroup ChipGroup yang ingin diperiksa.
     * @return Teks dari chip yang dipilih, atau "All" jika tiada yang dipilih.
     */
    private String getSelectedChipText(ChipGroup chipGroup) {
        if (chipGroup == null) return "All";

        int selectedId = chipGroup.getCheckedChipId();
        if (selectedId != View.NO_ID) {
            Chip selectedChip = chipGroup.findViewById(selectedId);
            if (selectedChip != null) {
                return selectedChip.getText().toString();
            }
        }
        return "All";
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }
}