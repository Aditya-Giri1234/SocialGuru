plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.google.gms.google-services")
    id("androidx.navigation.safeargs.kotlin")
    id("kotlin-kapt")
    id("com.google.devtools.ksp")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.aditya.socialguru"
    compileSdk = 34



    // Index.List , LICENSE.md  ,META-INF/NOTICE.md and dependencies have duplicate file so remove one during gradle build
    packaging {
        resources {
            excludes += "META-INF/INDEX.LIST"
            excludes +="META-INF/DEPENDENCIES"
            excludes +="META-INF/LICENSE.md"
            excludes +="META-INF/NOTICE.md"
        }
    }

    defaultConfig {
        applicationId = "com.aditya.socialguru"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
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

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.databinding.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)


    //For Memory Leak
//    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")

    //For text size
    implementation("com.intuit.sdp:sdp-android:1.1.1")
    implementation("com.intuit.ssp:ssp-android:1.1.1")

    //Navigation Component
    val nav_version = "2.7.7"
    implementation("androidx.navigation:navigation-fragment-ktx:$nav_version")
    implementation("androidx.navigation:navigation-ui-ktx:$nav_version")


    //Circular Image
    implementation("de.hdodenhof:circleimageview:3.1.0")


    //Glide Dependency
    implementation("com.github.bumptech.glide:glide:4.16.0")
    ksp("com.github.bumptech.glide:ksp:4.15.1")


    //Firebase Dependency

    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-messaging")


    //For Viewmodel
    val lifecycle_version = "2.7.0"
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycle_version")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycle_version")
    kapt("androidx.lifecycle:lifecycle-compiler:$lifecycle_version")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")


    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.squareup.okhttp3:logging-interceptor:4.9.0")

    //Three dot
    implementation("com.github.ybq:Android-SpinKit:1.4.0")

    //Dot indicator for viewPager
    implementation("com.tbuonomo:dotsindicator:4.3")

    //Data Store Preference
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    //Emoji Picker
    implementation("com.vanniktech:emoji-google:0.20.0")


    //Media 3
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-exoplayer-dash:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")


    // Swipe behaviour
    implementation("it.xabaras.android:recyclerview-swipedecorator:1.4")


    //Google Auth
    implementation("com.google.auth:google-auth-library-oauth2-http:1.23.0")

    // swipe reveal  view
    implementation("com.chauthai.swipereveallayout:swipe-reveal-layout:1.4.1")



}