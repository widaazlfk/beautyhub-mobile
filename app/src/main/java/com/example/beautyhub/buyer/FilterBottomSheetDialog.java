package com.example.beautyhub.buyer;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.beautyhub.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.RangeSlider;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class FilterBottomSheetDialog extends BottomSheetDialogFragment {

    public static final String TAG = "FilterBottomSheetDialog";

    // Keys for passing data
    private static final String ARG_CATEGORY = "category";
    private static final String ARG_SUBCATEGORIES = "subcategories"; // <-- KEY UPDATED
    private static final String ARG_BRAND = "brand";
    private static final String ARG_MIN_PRICE = "min_price";
    private static final String ARG_MAX_PRICE = "max_price";
    private static final String ARG_SKIN_TYPE = "skin_type";
    private static final String ARG_INGREDIENT = "ingredient";

    private FilterListener listener;

    // Views
    private ChipGroup chipGroupCategory, chipGroupSkinType, chipGroupSubCategorySkincare, chipGroupSubCategoryMakeup;
    private TextView tvSubCategorySkincareTitle, tvSubCategoryMakeupTitle;
    private TextInputEditText etBrand, etIngredient;
    private RangeSlider priceSlider;
    private Button btnApply, btnReset;

    // Current filter values
    private String currentCategory;
    private ArrayList<String> currentSubCategories; // <-- VALUE TYPE UPDATED
    private String currentBrand;
    private float currentMinPrice;
    private float currentMaxPrice;
    private String currentSkinType;
    private String currentIngredient;

    // Interface to communicate with the calling Activity/Fragment
    public interface FilterListener {
        // This signature now matches ShopActivity's implementation
        void onFilterApplied(String category, List<String> subCategories, String brand, float minPrice, float maxPrice, String skinType, String ingredient);
    }

    // --- 1. CORRECTED newInstance FACTORY METHOD ---
    public static FilterBottomSheetDialog newInstance(String category, ArrayList<String> subcategories, String brand,
                                                      float minPrice, float maxPrice,
                                                      String skinType, String ingredient) {
        FilterBottomSheetDialog fragment = new FilterBottomSheetDialog();
        Bundle args = new Bundle();
        args.putString(ARG_CATEGORY, category);
        args.putStringArrayList(ARG_SUBCATEGORIES, subcategories); // <-- Pass ArrayList
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
            currentSubCategories = getArguments().getStringArrayList(ARG_SUBCATEGORIES); // <-- Read ArrayList
            if (currentSubCategories == null) {
                currentSubCategories = new ArrayList<>(); // Initialize if null
            }
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
        // Ensure the host activity implements the listener
        if (context instanceof FilterListener) {
            listener = (FilterListener) context;
        } else {
            throw new ClassCastException(context.toString() + " must implement FilterListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_filter, container, false);
        initViews(view);
        setupCurrentFilterValues();
        setupListeners();
        return view;
    }

    private void initViews(View view) {
        chipGroupCategory = view.findViewById(R.id.chip_group_category);
        chipGroupSkinType = view.findViewById(R.id.chip_group_skin_type);
        tvSubCategorySkincareTitle = view.findViewById(R.id.tv_subcategory_skincare_title);
        chipGroupSubCategorySkincare = view.findViewById(R.id.chip_group_subcategory_skincare);
        tvSubCategoryMakeupTitle = view.findViewById(R.id.tv_subcategory_makeup_title);
        chipGroupSubCategoryMakeup = view.findViewById(R.id.chip_group_subcategory_makeup);
        etBrand = view.findViewById(R.id.et_filter_brand);
        etIngredient = view.findViewById(R.id.et_filter_ingredient);
        priceSlider = view.findViewById(R.id.slider_price_range);
        btnApply = view.findViewById(R.id.btn_apply_filter);
        btnReset = view.findViewById(R.id.btn_reset_filter);
    }

    // --- 2. CORRECTED setupCurrentFilterValues TO HANDLE LIST ---
    private void setupCurrentFilterValues() {
        etBrand.setText(currentBrand);
        etIngredient.setText(currentIngredient);
        priceSlider.setValues(currentMinPrice, currentMaxPrice);
        priceSlider.setLabelFormatter(value -> String.format("RM %.0f", value));

        setSelectedChip(chipGroupCategory, currentCategory);
        setSelectedChip(chipGroupSkinType, currentSkinType);

        // Set initial visibility of sub-category sections
        updateSubCategoryVisibility(false);

        // Set selected chips for sub-categories
        setSelectedChips(chipGroupSubCategorySkincare, currentSubCategories);
        setSelectedChips(chipGroupSubCategoryMakeup, currentSubCategories);
    }

    private void setupListeners() {
        chipGroupCategory.setOnCheckedChangeListener((group, checkedId) -> {
            updateSubCategoryVisibility(true);
        });

        // --- 3. CORRECTED btnApply OnClickListener ---
        btnApply.setOnClickListener(v -> {
            String selectedCategory = getSelectedChipText(chipGroupCategory);
            List<String> selectedSubCategories = new ArrayList<>();

            // Get sub-categories only if a main category is selected
            if ("Skincare".equals(selectedCategory)) {
                selectedSubCategories.addAll(getSelectedChipTexts(chipGroupSubCategorySkincare));
            } else if ("Makeup".equals(selectedCategory)) {
                selectedSubCategories.addAll(getSelectedChipTexts(chipGroupSubCategoryMakeup));
            }

            // If sub-categories are selected, the main category filter in ShopActivity will be ignored.
            // If no sub-categories are selected, the main category will be used.
            String brand = etBrand.getText() != null ? etBrand.getText().toString().trim() : "";
            String ingredient = etIngredient.getText() != null ? etIngredient.getText().toString().trim() : "";
            String selectedSkinType = getSelectedChipText(chipGroupSkinType);

            List<Float> prices = priceSlider.getValues();
            float minPrice = prices.get(0);
            float maxPrice = prices.get(1);

            if (minPrice > maxPrice) {
                Toast.makeText(getContext(), "Minimum price cannot be higher than maximum price", Toast.LENGTH_SHORT).show();
                return;
            }

            if (listener != null) {
                // Pass the list of subcategories
                listener.onFilterApplied(selectedCategory, selectedSubCategories, brand, minPrice, maxPrice, selectedSkinType, ingredient);
            }
            dismiss();
        });

        // --- 4. CORRECTED btnReset OnClickListener ---
        btnReset.setOnClickListener(v -> {
            resetAllFilters();
            if (listener != null) {
                // Pass reset values, including an EMPTY list for sub-categories
                listener.onFilterApplied("All", new ArrayList<>(), "", 10.0f, 200.0f, "All", "");
            }
            dismiss();
        });
    }

    private void resetAllFilters() {
        chipGroupCategory.clearCheck();
        chipGroupSkinType.clearCheck();
        chipGroupSubCategorySkincare.clearCheck();
        chipGroupSubCategoryMakeup.clearCheck();
        updateSubCategoryVisibility(false);
        etBrand.setText("");
        etIngredient.setText("");
        priceSlider.setValues(10.0f, 200.0f);
    }

    private void updateSubCategoryVisibility(boolean clearSubCategorySelection) {
        String selectedCategory = getSelectedChipText(chipGroupCategory);
        boolean isSkincare = "Skincare".equals(selectedCategory);
        boolean isMakeup = "Makeup".equals(selectedCategory);

        tvSubCategorySkincareTitle.setVisibility(isSkincare ? View.VISIBLE : View.GONE);
        chipGroupSubCategorySkincare.setVisibility(isSkincare ? View.VISIBLE : View.GONE);
        tvSubCategoryMakeupTitle.setVisibility(isMakeup ? View.VISIBLE : View.GONE);
        chipGroupSubCategoryMakeup.setVisibility(isMakeup ? View.VISIBLE : View.GONE);

        if (clearSubCategorySelection) {
            chipGroupSubCategorySkincare.clearCheck();
            chipGroupSubCategoryMakeup.clearCheck();
        }
    }

    // --- HELPER METHODS for Single and Multiple Chip Selection ---

    // For single-choice groups (e.g., Main Category)
    private void setSelectedChip(ChipGroup chipGroup, String value) {
        if (chipGroup == null || value == null || value.isEmpty() || "All".equalsIgnoreCase(value)) {
            if (chipGroup != null) chipGroup.clearCheck();
            return;
        }
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            Chip chip = (Chip) chipGroup.getChildAt(i);
            if (chip.getText().toString().equalsIgnoreCase(value)) {
                chip.setChecked(true);
                return;
            }
        }
    }

    // For multi-choice groups (e.g., Sub-Categories)
    private void setSelectedChips(ChipGroup chipGroup, List<String> values) {
        if (chipGroup == null || values == null || values.isEmpty()) {
            return;
        }
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            Chip chip = (Chip) chipGroup.getChildAt(i);
            if (values.contains(chip.getText().toString())) {
                chip.setChecked(true);
            }
        }
    }

    // Gets text from a single-choice group
    private String getSelectedChipText(ChipGroup chipGroup) {
        if (chipGroup == null) return "All";
        int selectedId = chipGroup.getCheckedChipId();
        if (selectedId != View.NO_ID) {
            Chip selectedChip = chipGroup.findViewById(selectedId);
            return selectedChip.getText().toString();
        }
        return "All";
    }

    // Gets all texts from a multi-choice group
    private List<String> getSelectedChipTexts(ChipGroup chipGroup) {
        List<String> selectedTexts = new ArrayList<>();
        if (chipGroup == null) return selectedTexts;

        for (int id : chipGroup.getCheckedChipIds()) {
            Chip chip = chipGroup.findViewById(id);
            if (chip != null) {
                selectedTexts.add(chip.getText().toString());
            }
        }
        return selectedTexts;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }
}
