import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  val propVersionCode = project.findProperty("VERSION_CODE")?.toString()?.toIntOrNull()
      ?: System.getenv("VERSION_CODE")?.toIntOrNull()
      ?: 100
  val propVersionName = project.findProperty("VERSION_NAME")?.toString()
      ?: System.getenv("VERSION_NAME")
      ?: "2.5.0"

  defaultConfig {
    applicationId = "com.aistudio.kaziagro.poultr"
    minSdk = 24
    targetSdk = 36
    versionCode = propVersionCode
    versionName = propVersionName

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val kaziKeystore = file("${rootDir}/kazi_keystore.jks")
      val myUploadKey = file("${rootDir}/my-upload-key.jks")
      val keystorePath = System.getenv("RELEASE_KEYSTORE_PATH")
          ?: System.getenv("KEYSTORE_PATH")
      val keyFile = if (keystorePath != null) file(keystorePath) else if (kaziKeystore.exists()) kaziKeystore else myUploadKey

      if (keyFile.exists()) {
        storeFile = keyFile
        if (keyFile.name == "kazi_keystore.jks") {
          storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD") ?: "kaziagro123"
          keyAlias = System.getenv("RELEASE_KEY_ALIAS") ?: "kaziagro"
          keyPassword = System.getenv("RELEASE_KEY_PASSWORD") ?: "kaziagro123"
        } else {
          storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
              ?: System.getenv("KEYSTORE_PASSWORD")
              ?: System.getenv("STORE_PASSWORD")
              ?: "android"
          keyAlias = System.getenv("RELEASE_KEY_ALIAS")
              ?: System.getenv("KEY_ALIAS")
              ?: "upload"
          keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
              ?: System.getenv("KEY_PASSWORD")
              ?: "android"
        }
      } else {
        val rootDebugKeystore = file("${rootDir}/debug.keystore")
        if (rootDebugKeystore.exists()) {
          storeFile = rootDebugKeystore
          storePassword = "android"
          keyAlias = "androiddebugkey"
          keyPassword = "android"
        }
      }
    }
    create("debugConfig") {
      val rootDebugKeystore = file("${rootDir}/debug.keystore")
      if (rootDebugKeystore.exists()) {
        storeFile = rootDebugKeystore
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug { signingConfig = signingConfigs.getByName("debugConfig") }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  // Firebase Realtime Database & Auth
  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.database)
  implementation(libs.firebase.auth)
  implementation(libs.firebase.analytics)

  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  // Room removed in favor of Firebase Realtime Database
  // implementation(libs.androidx.room.ktx)
  // implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  // Uncomment to use Firestore:
  // implementation(libs.firebase.firestore)

  // Uncomment ALL FOUR of the following dependencies together to use Firebase Auth and Google
  // Sign-In via Credential Manager:
  // implementation(libs.firebase.auth)
  // implementation(libs.androidx.credentials)
  // implementation(libs.androidx.credentials.play.services)
  // implementation(libs.googleid)
  // implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.play.services.auth)
  implementation(libs.androidx.work.runtime.ktx)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
}
