import java.util.Date
import java.text.SimpleDateFormat
import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

val localProperties = Properties().apply {
    val localPropsFile = rootProject.file("local.properties")
    if (localPropsFile.exists()) load(FileInputStream(localPropsFile))
}

android {
    namespace = "com.bongbee.iptv"
    compileSdk = 35

    val date = Date()
    val formattedDate = SimpleDateFormat("yyMMdd.HHmm").format(date)
    val autoVersionCode = (date.time / 60000).toInt()

    signingConfigs {
        create("release") {
            storeFile = file("${rootProject.projectDir}/${localProperties["RELEASE_STORE_FILE"]}")
            storePassword = localProperties["RELEASE_STORE_PASSWORD"] as String
            keyAlias = localProperties["RELEASE_KEY_ALIAS"] as String
            keyPassword = localProperties["RELEASE_KEY_PASSWORD"] as String
        }
    }

    defaultConfig {
        applicationId = "com.bongbee.iptv"
        minSdk = 26
        targetSdk = 35
        versionCode = autoVersionCode
        versionName = "1.5.$formattedDate"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // TMDB API key — move to local.properties for production
        buildConfigField("String", "TMDB_API_KEY", "\"5e10bf06e4f15dae6e9ff35ff35e8df2\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    @Suppress("DEPRECATION")
    kotlinOptions {
        jvmTarget = "21"
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi"
        )
    }
    
    buildFeatures {
        compose = true
        buildConfig = true
    }

    androidResources {
        noCompress += listOf("proot-static-aarch64", "proot-aarch64", "proot-arm", "rootfs.tar.xz")
    }

    applicationVariants.all {
        val variant = this
        variant.outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output.outputFileName = "INDRAiptv_v${variant.versionName}.apk"
        }
    }
}

kotlin {
    jvmToolchain(21)
}

tasks.register("fixResourceExtension") {
    doLast {
        val file = file("src/main/res/drawable/ic_launcher_foreground.png")
        if (file.exists()) {
            val newFile = file("src/main/res/drawable/ic_launcher_foreground.jpg")
            if (file.renameTo(newFile)) {
                println("Renamed corrupted PNG to JPG")
            } else {
                println("Failed to rename file")
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.coil.compose)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.webkit)
    implementation(libs.zxing.core)
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    
    // Networking
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
}
