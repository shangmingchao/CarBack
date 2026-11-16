plugins {
    id("carback-android-library")
    id("carback-android-hilt")
}

dependencies {
    // 公共层对上暴露模型、Flow 扩展、EventBus、ServiceRegistry 等，统一用 api 传递
    api(libs.androidx.core.ktx)
    api(libs.kotlinx.coroutines.android)
    api(libs.androidx.appcompat)
    api(libs.androidx.lifecycle.ktx)
    api(libs.androidx.viewmodel.ktx)
}
