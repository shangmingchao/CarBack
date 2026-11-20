plugins {
    id("carback-feature-api")
}

dependencies {
    api(project(":core:ui"))
    api(libs.androidx.recyclerview)
}
