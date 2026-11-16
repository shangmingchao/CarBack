plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // 供预编译脚本插件使用（版本与 gradle/libs.versions.toml 保持一致）
    implementation("com.android.tools.build:gradle:8.10.1")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.10")
    implementation("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:2.1.10-1.0.29")
    implementation("com.google.dagger:hilt-android-gradle-plugin:2.57.2")
}
