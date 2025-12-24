package com.example.beautyhub;

import static com.google.firebase.FirebaseApp.*;

import android.app.Application;
import com.google.firebase.FirebaseApp;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        initializeApp(this);
    }
}