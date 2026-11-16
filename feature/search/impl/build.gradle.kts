plugins {
    id("carback-feature-impl")
}

dependencies {
    // View 级契约：search 复用 home 的 TrackListAdapter（只依赖对方 api 契约，由构建守卫强制）
    implementation(project(":feature:home:api"))
}
