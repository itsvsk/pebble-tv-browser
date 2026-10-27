plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val pebbleVersionName = providers.gradleProperty("pebbleVersionName").orElse("0.1.1").get()
require(Regex("(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)").matches(pebbleVersionName)) {
    "pebbleVersionName must be MAJOR.MINOR.PATCH without leading zeroes"
}
val versionParts = pebbleVersionName.split('.').map { it.toIntOrNull() ?: error("Version component is too large") }
require(versionParts[0] in 0..2000 && versionParts[1] in 0..999 && versionParts[2] in 0..999) {
    "Version components must fit the Android versionCode: major <= 2000, minor and patch <= 999"
}
val pebbleVersionCode = versionParts[0] * 1_000_000 + versionParts[1] * 1_000 + versionParts[2]
require(pebbleVersionCode > 0) { "Version 0.0.0 cannot be released" }

val signingEnvironment = listOf(
    "PEBBLE_KEYSTORE_PATH", "PEBBLE_STORE_PASSWORD", "PEBBLE_KEY_ALIAS", "PEBBLE_KEY_PASSWORD",
).associateWith { providers.environmentVariable(it).orNull }
val hasReleaseSigning = signingEnvironment.values.all { !it.isNullOrBlank() }
require(hasReleaseSigning || signingEnvironment.values.all { it.isNullOrBlank() }) {
    "Release signing requires PEBBLE_KEYSTORE_PATH, PEBBLE_STORE_PASSWORD, PEBBLE_KEY_ALIAS and PEBBLE_KEY_PASSWORD"
}
if (providers.gradleProperty("requireReleaseSigning").orNull == "true") {
    require(hasReleaseSigning) { "Release signing credentials are missing; refusing an unsigned release" }
}

android {
    namespace = "com.tvbrowser.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.tvbrowser.app"
        minSdk = 24
        targetSdk = 35
        versionCode = pebbleVersionCode
        versionName = pebbleVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    if (hasReleaseSigning) {
        signingConfigs.create("release") {
            storeFile = file(signingEnvironment.getValue("PEBBLE_KEYSTORE_PATH")!!)
            storePassword = signingEnvironment.getValue("PEBBLE_STORE_PASSWORD")
            keyAlias = signingEnvironment.getValue("PEBBLE_KEY_ALIAS")
            keyPassword = signingEnvironment.getValue("PEBBLE_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(project(":core:common"))
    implementation(project(":core:design"))
    implementation(project(":core:db"))
    implementation(project(":core:webview"))
    implementation(project(":core:adblock"))
    implementation(project(":core:cursor"))
    implementation(project(":core:downloads"))
    implementation(project(":feature:home"))
    implementation(project(":feature:browser"))
    implementation(project(":feature:tabs"))
    implementation(project(":feature:bookmarks"))
    implementation(project(":feature:history"))
    implementation(project(":feature:downloads"))
    implementation(project(":feature:settings"))

    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)

    implementation(libs.navigation.compose)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    implementation(libs.work.runtime.ktx)
    implementation(libs.hilt.work)

    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.room.runtime)
    androidTestImplementation(libs.webkit)
}
