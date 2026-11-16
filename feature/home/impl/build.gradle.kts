plugins {
    id("carback-feature-impl")
}

dependencies {
    // 数据流：home 调 mine 取收听历史（只依赖对方 api 契约，由构建守卫强制）
    implementation(project(":feature:mine:api"))
    implementation(libs.androidx.viewpager2)
}
