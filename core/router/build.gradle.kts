plugins {
    id("carback-android-library")
    id("carback-android-hilt")
}

dependencies {
    // FragmentManager / Fragment 出现在 FragmentRouter 公共签名中，api 传递
    api(libs.androidx.fragment.ktx)
}
