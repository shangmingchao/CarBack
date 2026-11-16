plugins {
    id("carback-android-library")
}

dependencies {
    api(project(":core:common"))

    api(libs.material)
    api(libs.androidx.recyclerview)
    api(libs.androidx.constraintlayout)
    api(libs.androidx.fragment.ktx)
    api(libs.smartrefreshlayout)
    api(libs.smartrefreshlayout.classicsheader)
    api(libs.smartrefreshlayout.classicsfooter)
}
