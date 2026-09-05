import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val releaseKeystoreProperties = Properties().apply {
    val propertiesFile = rootProject.file("keystore.properties")
    if (propertiesFile.isFile) propertiesFile.inputStream().use { load(it) }
}
val releaseStoreFile = rootProject.file(
    releaseKeystoreProperties.getProperty("storeFile", "keeply-upload-key.jks")
)
val releaseKeyAlias = releaseKeystoreProperties.getProperty("keyAlias", "keeply-upload")
val releaseStorePassword = releaseKeystoreProperties.getProperty("storePassword")
val releaseKeyPassword = releaseKeystoreProperties.getProperty("keyPassword")

val validateReleaseSigningCredentials = tasks.register("validateReleaseSigningCredentials") {
    val keystoreFile = releaseStoreFile
    val credentialsPresent = releaseKeyAlias == "keeply-upload" &&
        keystoreFile.name == "keeply-upload-key.jks" &&
        !releaseStorePassword.isNullOrBlank() && !releaseKeyPassword.isNullOrBlank()
    doLast {
        check(keystoreFile.isFile && credentialsPresent) {
            "Release signing requires keeply-upload-key.jks, alias keeply-upload, and both passwords in root keystore.properties."
        }
    }
}
tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    dependsOn(validateReleaseSigningCredentials)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    namespace = "com.keeply.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.oobertappnetwork.keeply"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("upload") {
            storeFile = releaseStoreFile
            keyAlias = releaseKeyAlias
            storePassword = releaseStorePassword
            keyPassword = releaseKeyPassword
        }
    }

    buildTypes {
        debug {
            resValue("string", "admob_banner_ad_unit_id", "ca-app-pub-3940256099942544/6300978111")
        }
        release {
            signingConfig = signingConfigs.getByName("upload")
            resValue("string", "admob_banner_ad_unit_id", "ca-app-pub-6815772620942145/1102840397")
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        resValues = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation("androidx.fragment:fragment:1.9.0")
    implementation(libs.androidx.room.runtime)
    implementation(libs.google.mobile.ads)
    implementation(libs.google.ump)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.room.testing)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
