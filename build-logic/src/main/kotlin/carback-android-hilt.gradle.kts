/**
 * Hilt 配置：插件 + 运行时 + 编译器。
 * 需与 android-library / android-application 之一配合使用。
 */
plugins {
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    add("implementation", libs.findLibrary("hilt-android").get())
    add("ksp", libs.findLibrary("hilt-compiler").get())
}
