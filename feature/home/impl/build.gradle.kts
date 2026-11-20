plugins {
    id("carback-feature-impl")
}

dependencies {
    implementation(project(":feature:mine:api"))
    implementation(libs.androidx.viewpager2)
    // TabLayout / ConstraintLayout 仅本模块布局与代码使用
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
}
