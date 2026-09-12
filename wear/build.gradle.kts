plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "com.pixelwater.wear"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.pixelwater.app"
    minSdk = 26
    targetSdk = 34
    versionCode = 1
    versionName = "1.1"
  }

  signingConfigs {
    getByName("debug") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      signingConfig = signingConfigs.getByName("debug")
      isDebuggable = false
    }
    debug {
      signingConfig = signingConfigs.getByName("debug")
      isDebuggable = false
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
  }
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.foundation)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.play.services.wearable)
  implementation("com.google.guava:guava:33.4.0-android")
  implementation("androidx.wear.watchface:watchface-complications-data-source:1.2.1")
  implementation("androidx.wear.tiles:tiles:1.4.0")
  implementation("androidx.wear.tiles:tiles-material:1.4.0")
  implementation("androidx.wear.protolayout:protolayout:1.2.0")
  implementation("androidx.wear.protolayout:protolayout-material:1.2.0")
  implementation("androidx.wear.protolayout:protolayout-expression:1.2.0")
}

tasks.register<Copy>("copyWearApkToVisibleFolder") {
  outputs.upToDateWhen { false }
  from(layout.buildDirectory.dir("outputs/apk/debug"))
  into(rootProject.file("build-outputs"))
  include("wear-debug.apk")
}

tasks.whenTaskAdded {
  if (name == "assembleDebug") {
    finalizedBy("copyWearApkToVisibleFolder")
  }
}

tasks.register("printApkSizes") {
  doLast {
    val file1 = file("${rootDir}/build-outputs/wear-debug.apk")
    println("APK_SIZE_BUILD_OUTPUTS: ${file1.length()} bytes")
  }
}
