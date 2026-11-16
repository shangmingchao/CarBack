plugins {
    id("carback-feature-api")
}

dependencies {
    // TrackUiItem 出现在 SearchApi 的公开签名中，必须用 api 传递（api -> api 允许）
    api(project(":feature:home:api"))
}
