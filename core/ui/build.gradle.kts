plugins {
    id("carback-android-library")
}

dependencies {
    // UiState / UiError / LoadType / UiStateFlow 出现在公共签名中，api 传递
    api(project(":core:model"))

    // 公共签名中出现的类型（Fragment / RecyclerView.Adapter / ViewModel / SmartRefreshLayout），api 传递
    api(libs.androidx.fragment.ktx)
    api(libs.androidx.recyclerview)
    api(libs.androidx.lifecycle.ktx)
    api(libs.androidx.viewmodel.ktx)
    api(libs.androidx.core.ktx)
    api(libs.smartrefreshlayout)
    api(libs.smartrefreshlayout.classicsheader)
    api(libs.smartrefreshlayout.classicsfooter)
    api(libs.androidx.constraintlayout)

    implementation(libs.androidx.appcompat)
}
