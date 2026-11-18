plugins {
    id("carback-android-library")
}

// AAR 产物名固定为 opensdk-impl-<variant>.aar，避免使用目录名 "impl"
base {
    archivesName = "opensdk-impl"
}

android {
    // Library R8（README 5.3）：AAR 自身只保留 :opensdk:api 契约与反射桥接，
    // 内部实现类混淆；consumer-rules.pro 已随 AAR 下发给第三方
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

// AAR 产物名固定为 opensdk-impl-<variant>.aar，避免使用目录名 "impl"
base {
    archivesName = "opensdk-impl"
}

dependencies {
    // 对外签名用到 api 类型，必须 api 传递
    api(project(":opensdk:api"))

    // 内部装配：implementation 依赖，不进入第三方编译类路径
    implementation(project(":core:common"))
    implementation(project(":core:router"))
    implementation(project(":feature:home:impl"))
    implementation(project(":feature:mine:impl"))
    implementation(project(":feature:search:impl"))

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
}
