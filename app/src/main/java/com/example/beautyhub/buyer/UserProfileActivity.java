package com.example.beautyhub.buyer;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.beautyhub.R;
import com.example.beautyhub.auth.LoginActivity;
import com.example.beautyhub.models.ShippingAddress;
import com.example.beautyhub.models.User;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserProfileActivity extends AppCompatActivity {

    private static final String TAG = "UserProfileActivity";

    // Views
    private MaterialToolbar toolbar;
    private CircleImageView ivProfilePicture;
    private MaterialButton btnChangePhoto;
    private TextView tvUserName, tvUserEmail, tvMemberSince, tvShippingAddressInfo;
    private TextView tvTotalOrders, tvPointsBalance, tvReviewsCount;
    private CardView cardPersonalInfo, cardShippingAddresses, cardOrderHistory, cardMyReviews;
    private CardView cardHelpCenter, cardLogout;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference userRef, reviewsRef, ordersRef;
    private ValueEventListener userProfileListener;
    private FirebaseUser currentUser;

    private ProgressDialog progressDialog;

    private final ActivityResultLauncher<CropImageContractOptions> cropImage =
            registerForActivityResult(new CropImageContract(), result -> {
                if (result.isSuccessful()) {
                    Uri croppedImageUri = result.getUriContent();
                    uploadProfileImageToCloudinary(croppedImageUri);
                } else {
                    Log.e(TAG, "Image cropping failed: ", result.getError());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.b_activity_profile);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            redirectToLogin();
            return;
        }

        // Initialize Firebase References
        userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUser.getUid());
        reviewsRef = FirebaseDatabase.getInstance().getReference("ProductReviews");
        ordersRef = FirebaseDatabase.getInstance().getReference("Orders");

        initializeViews();
        setupAllClickListeners();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar_user_profile);
        ivProfilePicture = findViewById(R.id.iv_profile_picture);
        btnChangePhoto = findViewById(R.id.btn_change_photo);
        tvUserName = findViewById(R.id.tv_user_name);
        tvUserEmail = findViewById(R.id.tv_user_email);
        tvMemberSince = findViewById(R.id.tv_member_since);
        tvTotalOrders = findViewById(R.id.tv_total_orders);
        tvPointsBalance = findViewById(R.id.tv_points_balance);
        tvReviewsCount = findViewById(R.id.tv_reviews_count);
        tvShippingAddressInfo = findViewById(R.id.tv_shipping_address_info);

        cardPersonalInfo = findViewById(R.id.card_personal_info);
        cardShippingAddresses = findViewById(R.id.card_shipping_addresses);
        cardOrderHistory = findViewById(R.id.card_order_history);
        cardMyReviews = findViewById(R.id.card_my_reviews);
        cardHelpCenter = findViewById(R.id.card_help_center);
        cardLogout = findViewById(R.id.card_logout);

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Updating Profile");
        progressDialog.setCancelable(false);
    }

    private void setupAllClickListeners() {
        toolbar.setNavigationOnClickListener(v -> finish());
        btnChangePhoto.setOnClickListener(v -> startImageCrop());
        cardPersonalInfo.setOnClickListener(v -> openPersonalInformation());
        cardShippingAddresses.setOnClickListener(v -> openShippingAddresses());
        cardOrderHistory.setOnClickListener(v -> openOrderHistory());
        cardMyReviews.setOnClickListener(v -> openMyReviews());
        cardHelpCenter.setOnClickListener(v -> openHelpCenter());
        cardLogout.setOnClickListener(v -> logoutUser());

        tvTotalOrders.setOnClickListener(v -> openOrderHistory());
        tvReviewsCount.setOnClickListener(v -> openMyReviews());
        tvPointsBalance.setOnClickListener(v -> openRewardsPage());
    }

    private void loadReviewCount() {
        // Menggunakan nested loop untuk menembusi ProductID (cth: prod_009)
        reviewsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int count = 0;
                for (DataSnapshot productSnapshot : snapshot.getChildren()) {
                    for (DataSnapshot reviewSnapshot : productSnapshot.getChildren()) {
                        String reviewUserId = reviewSnapshot.child("userId").getValue(String.class);
                        if (reviewUserId != null && reviewUserId.equals(currentUser.getUid())) {
                            count++;
                        }
                    }
                }
                tvReviewsCount.setText(String.valueOf(count));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error counting reviews: " + error.getMessage());
            }
        });
    }

    private void loadOrderCount() {
        ordersRef.orderByChild("userId").equalTo(currentUser.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        tvTotalOrders.setText(String.valueOf(snapshot.getChildrenCount()));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error counting orders: " + error.getMessage());
                    }
                });
    }

    private void loadUserProfile() {
        if (userProfileListener == null) {
            userProfileListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        User currentUserData = snapshot.getValue(User.class);
                        if (currentUserData != null) {
                            tvUserName.setText(currentUserData.getUsername() != null ? currentUserData.getUsername() : "N/A");
                            tvUserEmail.setText(currentUserData.getEmail() != null ? currentUserData.getEmail() : "N/A");
                            tvPointsBalance.setText(String.valueOf(currentUserData.getPoints()));

                            String profileImageUrl = currentUserData.getProfileImage();
                            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                                Glide.with(UserProfileActivity.this)
                                        .load(profileImageUrl)
                                        .placeholder(R.drawable.ic_user_avatar)
                                        .error(R.drawable.ic_user_avatar)
                                        .into(ivProfilePicture);
                            } else {
                                ivProfilePicture.setImageResource(R.drawable.ic_user_avatar);
                            }

                            ShippingAddress defaultAddress = findDefaultAddress(currentUserData);
                            if (defaultAddress != null) {
                                String fullAddress = defaultAddress.getStreet() + ", " + defaultAddress.getCity() + ", " +
                                        defaultAddress.getState() + " " + defaultAddress.getZipcode();
                                tvShippingAddressInfo.setText(fullAddress);
                            } else {
                                tvShippingAddressInfo.setText("No shipping address set. Tap to add.");
                            }
                        }

                        Long memberSinceTimestamp = currentUser.getMetadata().getCreationTimestamp();
                        if (memberSinceTimestamp != null) {
                            tvMemberSince.setText("Member since " + new SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(new Date(memberSinceTimestamp)));
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to read user data.", error.toException());
                }
            };
        }
        userRef.addValueEventListener(userProfileListener);
    }

    private ShippingAddress findDefaultAddress(User user) {
        if (user.getAddresses() == null || user.getAddresses().isEmpty()) return null;
        for (ShippingAddress address : user.getAddresses().values()) {
            if (address.isDefault()) return address;
        }
        return user.getAddresses().values().iterator().next();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentUser != null) {
            loadUserProfile();
            loadReviewCount();
            loadOrderCount();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (userRef != null && userProfileListener != null) {
            userRef.removeEventListener(userProfileListener);
        }
    }

    // --- Intent & Helper Methods ---
    private void openPersonalInformation() { startActivity(new Intent(this, PersonalInformationActivity.class)); }
    private void openShippingAddresses() { startActivity(new Intent(this, ShippingAddressListActivity.class)); }
    private void openOrderHistory() { startActivity(new Intent(this, MyOrdersActivity.class)); }
    private void openMyReviews() {
        Intent intent = new Intent(this, MyReviewsActivity.class);
        intent.putExtra("USER_ID", currentUser.getUid());
        startActivity(intent);
    }
    private void openHelpCenter() { startActivity(new Intent(this, HelpCenterActivity.class)); }
    private void openRewardsPage() { startActivity(new Intent(this, MyRewardsActivity.class)); }

    private void startImageCrop() {
        CropImageOptions cropImageOptions = new CropImageOptions();
        cropImageOptions.guidelines = com.canhub.cropper.CropImageView.Guidelines.ON;
        cropImageOptions.aspectRatioX = 1;
        cropImageOptions.aspectRatioY = 1;
        cropImageOptions.fixAspectRatio = true;
        cropImageOptions.outputCompressQuality = 70;
        CropImageContractOptions options = new CropImageContractOptions(null, cropImageOptions);
        cropImage.launch(options);
    }

    private void uploadProfileImageToCloudinary(Uri imageUri) {
        if (imageUri == null) return;
        progressDialog.setMessage("Uploading photo...");
        progressDialog.show();

        MediaManager.get().upload(imageUri).callback(new UploadCallback() {
            @Override public void onStart(String requestId) { }
            @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
            @Override
            public void onSuccess(String requestId, Map resultData) {
                String downloadUrl = (String) resultData.get("secure_url");
                if (downloadUrl != null) saveImageUrlToFirebase(downloadUrl);
                else progressDialog.dismiss();
            }
            @Override
            public void onError(String requestId, ErrorInfo error) {
                progressDialog.dismiss();
                Toast.makeText(UserProfileActivity.this, "Upload failed.", Toast.LENGTH_SHORT).show();
            }
            @Override public void onReschedule(String requestId, ErrorInfo error) {}
        }).dispatch();
    }

    private void saveImageUrlToFirebase(String downloadUrl) {
        userRef.child("profileImage").setValue(downloadUrl).addOnCompleteListener(task -> {
            progressDialog.dismiss();
            if (task.isSuccessful()) Toast.makeText(this, "Profile photo updated!", Toast.LENGTH_SHORT).show();
        });
    }

    private void logoutUser() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    mAuth.signOut();
                    redirectToLogin();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
