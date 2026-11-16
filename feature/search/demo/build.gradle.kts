plugins {
    id("carback-feature-demo")
}

android {
    defaultConfig {
        applicationId = "cn.frank.carback.demo.search"
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    implementation(project(":feature:search:impl"))
    // 搜索页复用 home 的列表 Adapter（View 级契约），demo 需装配 home impl
    implementation(project(":feature:home:impl"))
}
