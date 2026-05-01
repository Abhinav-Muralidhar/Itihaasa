import java.util.Properties

plugins {
    id("com.android.application")

    // Kotlin (ONLY ONCE - IMPORTANT)
    id("org.jetbrains.kotlin.android")

    // KAPT (NO alias, direct plugin)
    id("org.jetbrains.kotlin.kapt")

    id("org.jetbrains.kotlin.plugin.compose")


    // Hilt
    id("com.google.dagger.hilt.android")

    // Firebase
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { stream ->
            load(stream)
        }
    }
}

fun secretProperty(name: String): String {
    return project.findProperty(name)?.toString()
        ?: localProperties.getProperty(name)
        ?: ""
}

    android {
        namespace = "com.itihaasa.nammakathey"
        compileSdk = 36


        defaultConfig {
            applicationId = "com.itihaasa.nammakathey"
            minSdk = 26
            targetSdk = 36
            versionCode = 1
            versionName = "1.0"

            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

            val mapsKey = secretProperty("MAPS_API_KEY")
            val geminiKey = secretProperty("GEMINI_API_KEY")

            manifestPlaceholders["MAPS_API_KEY"] = mapsKey
            buildConfigField("String", "MAPS_API_KEY", "\"$mapsKey\"")
            buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKey\"")
        }

        buildTypes {
            release {
                isMinifyEnabled = true
                isShrinkResources = true
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
                buildConfigField("Boolean", "DEBUG_MODE", "false")
            }

            debug {
                buildConfigField("Boolean", "DEBUG_MODE", "true")
            }
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }
        buildFeatures {
            compose = true
            buildConfig= true
        }
    }

    kotlin {
        jvmToolchain(11)
    }

    dependencies {

        // Core Android
        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.lifecycle.runtime.ktx)
        implementation(libs.androidx.activity.compose)

        // Jetpack Compose
        implementation(platform(libs.androidx.compose.bom))
        implementation(libs.androidx.compose.ui)
        implementation(libs.androidx.compose.ui.graphics)
        implementation(libs.androidx.compose.ui.tooling.preview)
        implementation(libs.androidx.compose.material3)
        implementation("androidx.compose.material:material-icons-extended")
        implementation("androidx.navigation:navigation-compose:2.7.7")
        implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.0")

        // Google Maps
        implementation("com.google.maps.android:maps-compose:4.3.3")
        implementation("com.google.maps.android:maps-compose-utils:4.3.3")
        implementation("com.google.android.gms:play-services-maps:18.2.0")

        // Firebase
        implementation(platform("com.google.firebase:firebase-bom:32.7.4"))
        implementation("com.google.firebase:firebase-auth-ktx")
        implementation("com.google.firebase:firebase-firestore-ktx")
        implementation("com.google.firebase:firebase-analytics-ktx")
        implementation("com.google.firebase:firebase-crashlytics-ktx")

        // Google Sign-In
        implementation("com.google.android.gms:play-services-auth:21.0.0")

        // Hilt
        implementation("com.google.dagger:hilt-android:2.51")
        kapt("com.google.dagger:hilt-android-compiler:2.51")
        implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

        // Retrofit + OkHttp
        implementation("com.squareup.retrofit2:retrofit:2.9.0")
        implementation("com.squareup.retrofit2:converter-gson:2.9.0")
        implementation("com.squareup.okhttp3:okhttp:4.12.0")
        implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

        // Coroutines
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

        // Image loading
        implementation("io.coil-kt:coil-compose:2.6.0")

        // Location
        implementation("com.google.android.gms:play-services-location:21.2.0")

        // Gson
        implementation("com.google.code.gson:gson:2.10.1")

        // Debug
        debugImplementation(libs.androidx.compose.ui.tooling)
        testImplementation(libs.junit)
        androidTestImplementation(libs.androidx.junit)
        androidTestImplementation(libs.androidx.espresso.core)
        androidTestImplementation(libs.androidx.compose.ui.test.junit4)
        debugImplementation(libs.androidx.compose.ui.test.manifest)
    }
