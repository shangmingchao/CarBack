/**
 * feature api 模块：纯契约，极薄。
 *
 * 默认依赖 :core:common（FeatureApi 标记 / EventBus / ServiceRegistry），
 * 需要跨 feature 复用契约时可在模块内显式依赖对方 api 模块。
 */
plugins {
    id("carback-android-library")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    add("api", project(":core:common"))
    add("api", libs.findLibrary("kotlinx-coroutines-android").get())
}
