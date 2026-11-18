plugins {
    id("carback-android-library")
}

// AAR 产物名固定为 opensdk-api-<variant>.aar，避免使用目录名 "api"
base {
    archivesName = "opensdk-api"
}

dependencies {
    // 对外契约模块：零内部依赖，只允许外部 SDK（coroutines / fragment）
    api(libs.kotlinx.coroutines.android)
    api(libs.androidx.fragment.ktx)
}
