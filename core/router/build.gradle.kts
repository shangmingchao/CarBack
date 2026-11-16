plugins {
    id("carback-android-library")
    id("carback-android-hilt")
}

dependencies {
    api(project(":core:common"))

    api(libs.androidx.fragment.ktx)
}
