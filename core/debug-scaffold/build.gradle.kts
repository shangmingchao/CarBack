plugins {
    id("carback-android-library")
    id("carback-android-hilt")
}

dependencies {
    api(project(":core:router"))

    implementation(libs.material)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.smartrefreshlayout)
    implementation(libs.smartrefreshlayout.classicsheader)
    implementation(libs.smartrefreshlayout.classicsfooter)
}
