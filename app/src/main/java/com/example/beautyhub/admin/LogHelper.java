package com.example.beautyhub.admin;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LogHelper {

    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_USER_TYPE = "user_type";
    private static Context appContext;

    public static void initialize(Context context) {
        appContext = context.getApplicationContext();
    }

    public static void logCurrentUserAction(String action, String details, String userType) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        // Save user type to SharedPreferences
        saveUserType(userType);

        DatabaseReference logsRef = FirebaseDatabase.getInstance().getReference("ActivityLogs");
        String logId = logsRef.push().getKey();

        if (logId != null) {
            ActivityLog log = new ActivityLog(
                    currentUser.getUid(),
                    currentUser.getEmail(),
                    userType,
                    action,
                    details,
                    getCurrentTimestamp()
            );

            logsRef.child(logId).setValue(log);
        }
    }

    public static void logAction(String action, String details) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        // Get saved user type
        String userType = getUserType();

        DatabaseReference logsRef = FirebaseDatabase.getInstance().getReference("ActivityLogs");
        String logId = logsRef.push().getKey();

        if (logId != null) {
            ActivityLog log = new ActivityLog(
                    currentUser.getUid(),
                    currentUser.getEmail(),
                    userType,
                    action,
                    details,
                    getCurrentTimestamp()
            );

            logsRef.child(logId).setValue(log);
        }
    }

    private static void saveUserType(String userType) {
        if (appContext == null) return;
        SharedPreferences prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_USER_TYPE, userType).apply();
    }

    private static String getUserType() {
        if (appContext == null) return "Unknown";
        SharedPreferences prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_USER_TYPE, "Unknown");
    }

    private static String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    public static class ActivityLog {
        public String userId;
        public String userEmail;
        public String userType;
        public String action;
        public String details;
        public String timestamp;

        public ActivityLog() {
            // Default constructor required for Firebase
        }

        public ActivityLog(String userId, String userEmail, String userType, String action, String details, String timestamp) {
            this.userId = userId;
            this.userEmail = userEmail;
            this.userType = userType;
            this.action = action;
            this.details = details;
            this.timestamp = timestamp;
        }
    }
}