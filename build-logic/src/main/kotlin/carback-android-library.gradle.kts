/**
 * Android Library 通用配置：SDK 版本、Java 17、ViewBinding、namespace 自动推导。
 *
 * namespace 按模块路径推导：":feature:home:impl" -> "cn.frank.carback.feature.home.impl"，
 * 模块自己的 build.gradle.kts 中可覆盖。
 */
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    compileSdk = 35

    defaultConfig {
        minSdk = 23
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
