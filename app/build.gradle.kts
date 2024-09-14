plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.scanbusapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.scanbusapp"
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

    // Désactiver View Binding
    buildFeatures {
        viewBinding = false
    }

    packagingOptions {
        resources.excludes.add("META-INF/AL2.0")
        resources.excludes.add("META-INF/LGPL2.1")
    }
}

configurations.all {
    resolutionStrategy {
        // Forcer la version de la bibliothèque Kotlin stdlib
        force("org.jetbrains.kotlin:kotlin-stdlib:1.8.10")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.8.10")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.10")
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.activity:activity:1.7.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Retrofit pour la communication avec l'API
    implementation("com.squareup.retrofit2:retrofit:2.9.0") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
    }
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // ZXing pour scanner les QR codes
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.google.zxing:core:3.4.0")

    // Ajout des dépendances pour Navigation
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    // Tests unitaires
    testImplementation("junit:junit:4.13.2")

    // Tests Android
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Ajout de la dépendance pour GridLayout
    implementation("androidx.gridlayout:gridlayout:1.0.0")
}
