plugins {
    id("carback-android-application")
    id("carback-android-hilt")
}

android {
    namespace = "cn.frank.carback"

    defaultConfig {
        applicationId = "cn.frank.carback"
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

dependencies {
    implementation(project(":core:router"))
    implementation(project(":feature:home:impl"))
    implementation(project(":feature:mine:impl"))
    implementation(project(":feature:search:impl"))
    implementation(libs.material)
}
