pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "CarBack"

include(":app")
include(":opensdk:api")
include(":opensdk:impl")

// 基础设施层：common / ui / router / router-compiler / debug-scaffold
listOf("common", "ui", "router", "router-compiler", "debug-scaffold").forEach {
    include(":core:$it")
}

// 业务层：每个 feature 固定三件套 api + impl + demo
listOf("home", "mine", "search").forEach { feature ->
    listOf("api", "impl", "demo").forEach {
        include(":feature:$feature:$it")
    }
}
