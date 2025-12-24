// app/build.gradle.kts

// ↓↓↓ ADD THIS PLUGINS BLOCK AT THE TOP ↓↓↓
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // Add the Google Services plugin if you are using Firebase services like Auth or Database
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.beautyhub"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.beautyhub"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = true
    }
}

// ↓↓↓ ADD THIS DEPENDENCIES BLOCK ↓↓↓
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.glide)

    // Import the Firebase BoM (Bill of Materials)
    // This will manage the versions of all your Firebase libraries
    implementation(platform(libs.firebase.bom))

    // Add the dependencies for the Firebase products you want to use
    // The versions are managed by the BoM, so you don't need to specify them here
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    // You can also use the bundle you created in your TOML file
    // implementation(libs.bundles.firebase)
    implementation("com.vanniktech:android-image-cropper:4.5.0")
    implementation ("com.cloudinary:cloudinary-android:2.4.0")
    implementation ("de.hdodenhof:circleimageview:3.1.0")
    implementation ("com.stripe:stripe-android:20.39.0")
    implementation ("androidx.gridlayout:gridlayout:1.1.0")
    implementation ("com.google.code.gson:gson:2.10.1")


    // Testing libraries
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

