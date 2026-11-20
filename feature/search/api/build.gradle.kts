plugins {
    id("carback-feature-api")
}

dependencies {
    // 曲目 Model 来自 :core:model（feature-api 插件已 api 注入），不再依赖 home 契约
}
