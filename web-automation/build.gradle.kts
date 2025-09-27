import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import java.io.FileInputStream
import java.util.Properties
import kotlin.apply

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktx.serialization)
}

val localPropertiesFile = rootProject.file("local.properties")

android {
    namespace = "com.mp.web_automation"
    compileSdk = 36

    defaultConfig {
        minSdk = 30
        buildConfigField("String", "api", getProperty("api"))
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    compileOnly(files("libs/plugins-release.aar"))

    implementation(libs.jsoup)
    implementation(libs.okhttp)
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    implementation(libs.okhttp.sse)

    implementation(libs.jetbrains.kotlin.stdlib)
    implementation(libs.kotlin.stdlib.jdk8)
    implementation(libs.kotlin.stdlib.common)
    implementation(libs.kotlinx.serialization.json)



    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.animation)
    implementation(libs.material)

    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.runtime.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

fun getProperty(value: String): String {
    return if (localPropertiesFile.exists()) {
        val localProps = Properties().apply {
            load(FileInputStream(localPropertiesFile))
        }
        localProps.getProperty(value) ?: "\"sample_val\""
    } else {
        System.getenv(value) ?: "\"sample_val\""
    }
}

apply(from = rootProject.file("export.gradle.kts"))