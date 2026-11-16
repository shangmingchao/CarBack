plugins {
    id("carback-android-application")
    id("carback-android-hilt")
}

android {
    // 主壳 namespace 保持根包，便于对外 SDK 入口 Activity 命名
    namespace = "cn.frank.carback"

    defaultConfig {
        applicationId = "cn.frank.carback"
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    // 主壳只做"组装"：注册路由 + 装配各 feature 的实现（KSP 生成装配代码 + Hilt 聚合，零反射）
    implementation(project(":core:router"))
    implementation(project(":feature:home:impl"))
    implementation(project(":feature:mine:impl"))
    implementation(project(":feature:search:impl"))
    implementation(project(":sdk"))

    implementation(libs.material)
}
