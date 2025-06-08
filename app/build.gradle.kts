

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services") // Firebase
    id("kotlin-parcelize")

    //vừa thêm đoạn 47.22
    id("kotlin-kapt")
}

android {
    namespace = "com.example.banhangs"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.banhangs"
        minSdk = 25
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.database)
    implementation(libs.firebase.storage)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.1.0")
    implementation(libs.androidx.compose.material)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)


    implementation ("com.tbuonomo:dotsindicator:5.1.0")


    implementation("androidx.recyclerview:recyclerview:1.3.2")


    implementation("com.github.bumptech.glide:glide:4.15.1")
    kapt("com.github.bumptech.glide:compiler:4.15.1")

    implementation ("com.google.code.gson:gson:2.10.1")

    implementation("com.google.firebase:firebase-auth") //tích hợp đăng ký đăng nhập


    implementation ("com.cloudinary:cloudinary-android:2.3.1") //up lên cloudinary

    implementation ("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("commons-codec:commons-codec:1.14")


    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.cashfree.pg:api:2.1.17")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

}




//
//plugins {
//    id("com.android.application")
//    id("org.jetbrains.kotlin.android")
//    id("com.google.gms.google-services") // Firebase
//
//    //vừa thêm đoạn 47.22
//    id("kotlin-kapt")
//}
//
//android {
//    namespace = "com.example.banhangs"
//    compileSdk = 35
//
//    defaultConfig {
//        applicationId = "com.example.banhangs"
//        minSdk = 24
//        targetSdk = 35
//        versionCode = 1
//        versionName = "1.0"
//        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
//    }
//
//    buildTypes {
//        release {
//            isMinifyEnabled = false
//            proguardFiles(
//                getDefaultProguardFile("proguard-android-optimize.txt"),
//                "proguard-rules.pro"
//            )
//        }
//    }
//
//    compileOptions {
//        sourceCompatibility = JavaVersion.VERSION_17
//        targetCompatibility = JavaVersion.VERSION_17
//    }
//
//    kotlinOptions {
//        jvmTarget = "17"
//    }
//
//    buildFeatures {
//        viewBinding = true
//
//    }
//}
//
//dependencies {
//    implementation("com.github.bumptech.glide:glide:4.16.0") // Dùng phiên bản mới nhất
//    kapt("com.github.bumptech.glide:compiler:4.16.0") // Đảm bảo `kapt` dùng chung phiên bản
//    implementation(platform(libs.firebase.bom))
//    implementation(libs.firebase.auth)
//    implementation(libs.firebase.firestore)
//    implementation(libs.firebase.database)
//    implementation(libs.firebase.storage)
//
//    implementation(libs.androidx.core.ktx)
//    implementation(libs.androidx.appcompat)
//    implementation(libs.material)
//    implementation(libs.androidx.activity)
//    implementation(libs.androidx.constraintlayout)
//    implementation("com.google.code.gson:gson:2.10.1")
//
//    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.1.0")

//    testImplementation(libs.junit)
//    androidTestImplementation(libs.androidx.junit)
//    androidTestImplementation(libs.androidx.espresso.core)
//
//
//    //vừa thêm đoạn 20:45
//    implementation ("com.tbuonomo:dotsindicator:5.1.0")
//
//
//
//    //vừa thêm đoạn 47.22
//
//    implementation("com.github.bumptech.glide:glide:4.12.0")
//    implementation("androidx.recyclerview:recyclerview:1.3.2")
//
//   implementation("com.google.firebase:firebase-auth") //tích hợp đăng ký đăng nhập

//implementation ("com.cloudinary:cloudinary-android:2.3.1") //up lên cloudinary

//    implementation ("com.squareup.okhttp3:okhttp:4.12.0")
//  implementation("commons-codec:commons-codec:1.14")
//
//implementation("com.squareup.retrofit2:retrofit:2.11.0")
//implementation("com.squareup.retrofit2:converter-gson:2.11.0")
//implementation("com.cashfree.pg:api:2.1.17")
//}
