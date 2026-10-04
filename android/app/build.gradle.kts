import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.fusheng.poetry"
    compileSdk = 35

    // 本地密钥（android/local.properties，.gitignore 覆盖，不进 Git）
    val localProps = Properties().apply {
        val f = rootProject.file("local.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }

    defaultConfig {
        applicationId = "com.fusheng.poetry"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "4.0.3"

        buildConfigField("String", "AGNES_API_KEY", "\"${localProps.getProperty("AGNES_API_KEY") ?: ""}\"")
    }

    buildFeatures {
        buildConfig = true
    }

    // 发布签名（android/keystore/fusheng.keystore 本地保管不入库，密码在 local.properties）
    signingConfigs {
        create("release") {
            val ksFile = rootProject.file(localProps.getProperty("KEYSTORE_FILE") ?: "keystore/fusheng.keystore")
            if (ksFile.exists()) {
                storeFile = ksFile
                storePassword = localProps.getProperty("KEYSTORE_PASSWORD")
                keyAlias = localProps.getProperty("KEY_ALIAS")
                keyPassword = localProps.getProperty("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.coil.compose)
    implementation(libs.androidx.exifinterface)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
}
