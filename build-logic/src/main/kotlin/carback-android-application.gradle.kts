/**
 * Android Application 通用配置：SDK 版本、Java 17、ViewBinding、namespace 自动推导。
 */
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    compileSdk = 35

    defaultConfig {
        minSdk = 23
        targetSdk = 35
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }

    namespace = "cn.frank.carback" + project.path.replace(':', '.').replace('-', '.')
}
