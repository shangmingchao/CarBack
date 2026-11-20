/**
 * feature demo 薄壳：让每个 feature 可独立编译运行调试。
 *
 * 自动依赖 :core:debug-scaffold（BaseDemoApplication + ScaffoldActivity），
 * 模块内只需再依赖自己的 impl。
 */
plugins {
    id("carback-android-application")
    id("carback-android-hilt")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    add("implementation", project(":core:debug-scaffold"))
    add("implementation", project(":core:router"))
    // demo 壳继承 ScaffoldActivity，需要 appcompat 可见
    add("implementation", libs.findLibrary("androidx-appcompat").get())
}
