/**
 * :core:model：模块间通信 Model 集中地。
 *
 * 只放跨模块（跨 feature / 对 opensdk）传递的数据契约，
 * 模块内部使用的 Model 留在各自 impl 中。
 */
plugins {
    id("carback-android-library")
}

dependencies {
    // Flow / StateFlow 出现在公共签名中，api 传递给订阅方
    api(libs.kotlinx.coroutines.android)
}
