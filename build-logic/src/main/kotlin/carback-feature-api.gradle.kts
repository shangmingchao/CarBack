/**
 * feature api 模块：纯契约，极薄。
 *
 * - FeatureApi 标记接口来自 :core:router 的 service 包；
 * - 契约签名中的 TrackModel / StateFlow 由 :core:model（api coroutines）提供；
 * - 消费方（feature impl / app / demo / opensdk）均显式依赖 :core:model 与 :core:router，
 *   故此处用 implementation，不再 api 传递。
 * - 需要跨 feature 复用契约时可在模块内显式依赖对方 api 模块。
 */
plugins {
    id("carback-android-library")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    add("implementation", project(":core:router"))
    add("implementation", project(":core:model"))
}
