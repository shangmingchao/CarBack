plugins {
    id("carback-feature-demo")
}

android {
    defaultConfig {
        applicationId = "cn.frank.carback.demo.home"
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    implementation(project(":feature:home:impl"))
}
