import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.GradleException
import org.gradle.api.artifacts.ProjectDependency

/**
 * feature impl 模块：业务实现。
 *
 * - 自动依赖 core（model / ui / router）+ Hilt + KSP 路由编译器；
 * - KSP 自动注入 router.moduleName，生成 "<Feature>ImplRouterModule" 装配代码；
 * - 【编译期守卫】禁止依赖任何其他 feature 的 impl / demo 模块，
 *   跨 feature 只允许依赖对方的 :api 模块，违规直接构建失败。
 */
plugins {
    id("carback-android-library")
    id("carback-android-hilt")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

// KSP 选项：由模块路径推导路由装配模块名，如 ":feature:home:impl" -> "HomeImplRouterModule"
val featureName = project.path
    .removePrefix(":feature:")
    .removeSuffix(":impl")
    .replace('-', '.')

dependencies {
    // Model / Router（ServiceRegistry / FeatureApi）仅本模块编译需要，不对外传递
    add("implementation", project(":core:model"))
    add("implementation", project(":core:router"))
    // Fragment 基类 / 通用列表组件随公共签名暴露给消费方（app / demo / opensdk）
    add("api", project(":core:ui"))
    // 本 feature 的契约模块
    add("api", project(":feature:$featureName:api"))
    add("ksp", project(":core:router-compiler"))
}
val routerModuleName = featureName.split('.')
    .filter { it.isNotBlank() }
    .joinToString("") { it.replaceFirstChar { c -> c.uppercaseChar() } } + "ImplRouterModule"

extensions.configure<KspExtension>("ksp") {
    arg("router.moduleName", routerModuleName)
}

// 【核心守卫】impl 之间永不互相依赖
afterEvaluate {
    val forbidden = Regex("^:feature:[^:]+:(impl|demo)$")
    configurations.configureEach {
        dependencies.withType(ProjectDependency::class.java).configureEach {
            val target = path
            if (target != project.path && target.matches(forbidden)) {
                throw GradleException(
                    "Feature impl 依赖守卫失败：$project.path 禁止依赖 $target。\n" +
                        "跨 feature 只能依赖对方的 :api 契约模块（如 :feature:xxx:api）。",
                )
            }
        }
    }
}
