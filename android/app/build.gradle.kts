plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.wcjk.triage"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.wcjk.triage"
        minSdk = 24          // Android 7.0
        targetSdk = 35
        versionCode = 1
        versionName = "3.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isMinifyEnabled = false
            buildConfigField("String", "SERVER", "\"42.198.84.82:7016\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    flavorDimensions += "hospital"
    productFlavors {
        create("default") { dimension = "hospital"; buildConfigField("Integer", "DrugSecondLineNum", "4"); buildConfigField("Boolean", "IsSecondCall", "false") }
        create("qzfe") { dimension = "hospital"; buildConfigField("Integer", "DrugSecondLineNum", "4"); buildConfigField("Boolean", "IsSecondCall", "true") }
        create("fzpfy") { dimension = "hospital"; buildConfigField("Integer", "DrugSecondLineNum", "1"); buildConfigField("Boolean", "IsSecondCall", "false") }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("com.google.android.material:material:1.12.0")

    // Network
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Gson
    implementation("com.google.code.gson:gson:2.11.0")

    // Socket.IO
    implementation("io.socket:socket.io-client:2.1.1")

    // Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
