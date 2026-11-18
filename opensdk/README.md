下面是一个兼顾**内部开发效率**与**SDK 边界安全**的架构方案。核心思路是：**在现有模块体系之上，抽出独立的 `:opensdk-api`（门面/契约）和 `:opensdk-impl`（SDK 装配层），SDK 只对外发布 `:opensdk-api` + 合并后的实现 AAR；数据子集通过 SDK 专属模型与接口隔离，而不是直接把 `:core:data` 的 Repository 暴露出去。**

---

## 1. 为什么不能直接“把 `:core:data` 打成 AAR”

`app:prod` 依赖多个 `feature`，`feature` 又依赖 `:core:data` 和 `:core:ui`。如果直接把 `:core:data` 中的某个 Repository 打成 AAR，会带来两个问题：

1. **数据面过宽**：Repository 的构造依赖 DataSource、Network、Database 等完整实现，第三方拿到 AAR 后可能触及你不希望开放的数据路径。
2. **API 不稳定**：Repository 是内部数据层契约，随内部重构而变化；一旦成为 SDK 公共 API，内部演进会被锁死。

因此，SDK 需要一层**门面（Facade）**，只暴露“第三方 APK 真正需要的那部分数据和页面”，内部实现全部隐藏在门面之后。

---

## 2. 模块依赖关系（内部 + SDK）

```
:app:prod
 ├── :feature:feed
 ├── :feature:profile
 └── :feature:xxx
       │
       ├── :core:data ── :core:model
       │                :core:network
       │                :core:database
       │
       └── :core:ui
```

在上面这套**内部依赖**之上，增加 SDK 侧的两个模块：

```
:opensdk-api          ← 对外 AAR 的一部分（轻量契约，可单独发布）
 ├── (无内部依赖，或仅依赖 :core:model 的公开子集)

:opensdk-impl         ← 对外 AAR 的实现部分（与内部模块合并后发布）
 ├── :opensdk-api
 ├── :core:data（implementation，不对外暴露）
 ├── :core:model（implementation）
 └── 需要的 feature Fragment（通过薄封装引用，不直接暴露 feature 模块）
```

`app:prod` **不依赖** `:opensdk-api` 或 `:opensdk-impl`。SDK 是内部能力的“导出通道”，不是 app 的运行依赖，避免把 SDK 门面污染到生产 App 的依赖图中。

最终对外发布物是**两个 artifact**：

- `opensdk-api`：纯契约 AAR，第三方编译期依赖。
- `opensdk`：由 `:opensdk-impl` 与内部 `:core:data`、所需 feature UI 合并后的 **Fat AAR / Fused AAR**，第三方运行期依赖。

---

## 3. 数据子集怎么保证：SDK 专属模型 + 接口，而非直接暴露 Repository

“子集”不能靠“在 Repository 上少公开几个方法”来实现，而应该在 `:opensdk-api` 中**重新定义只属于 SDK 的数据契约**。

### 3.1 `:opensdk-api` 中定义 SDK 模型与接口

```kotlin
// :opensdk-api — 对外暴露的模型，字段是 :core:model 的子集
data class SdkUserProfile(
    val id: String,
    val displayName: String,
    val avatarUrl: String?
)

// :opensdk-api — 对外门面接口
interface SdkDataProvider {
    suspend fun getUserProfile(userId: String): SdkUserProfile?
    fun observeUserProfile(userId: String): Flow<SdkUserProfile?>
}
```

第三方只看到 `SdkUserProfile` 和 `SdkDataProvider`，完全不知道 `:core:model` 中 `User` 的完整字段，也不知道 Repository 的存在。

### 3.2 `:opensdk-impl` 做模型转换

```kotlin
// :opensdk-impl — 内部实现，implementation 依赖 :core:data
internal class SdkDataProviderImpl(
    private val userRepository: UserRepository  // 来自 :core:data
) : SdkDataProvider {

    override suspend fun getUserProfile(userId: String): SdkUserProfile? =
        userRepository.getUser(userId)?.toSdkModel()

    override fun observeUserProfile(userId: String): Flow<SdkUserProfile?> =
        userRepository.observeUser(userId).map { it?.toSdkModel() }
}

private fun User.toSdkModel() = SdkUserProfile(
    id = id,
    displayName = displayName,
    avatarUrl = avatarUrl
)
```

`:core:data` 中的 `User` 可能包含内部账号体系、实验分组、敏感标识等字段；`User.toSdkModel()` 是**编译期强制执行的子集裁剪**，第三方拿到的 `SdkUserProfile` 不可能包含未映射的字段。

> 这一模式在 Android 数据层实践中被明确推荐：Repository 应通过 DTO 或对外模型暴露受限视图，而不是直接返回内部 entity。

### 3.3 如果某个 Repository 只读，就在 SDK 层只定义读接口

如果 `:core:data` 中某个 Repository 同时承担写操作，而 SDK 只需要读，不需要在 Repository 上加 `@VisibleForTesting` 或拆分内部模块——只需在 `:opensdk-api` 的接口中**不定义写方法**，`:opensdk-impl` 也不转发写调用。第三方编译期就没有写入口。

---

## 4. 部分 Fragment 页面的暴露方式

不要直接让第三方依赖 `:feature:profile` 模块。做法是在 `:opensdk-impl` 中提供**薄封装 Fragment**，或者提供一个 `SdkUiEntry` 门面：

```kotlin
// :opensdk-api
object SdkUi {
    fun createProfileFragment(args: Bundle?): Fragment
}
```

```kotlin
// :opensdk-impl
object SdkUiImpl {
    fun createProfileFragment(args: Bundle?): Fragment =
        ProfileFragment().apply { arguments = args }
}
```

`ProfileFragment` 来自 `:feature:profile`，但第三方拿到的是 `Fragment` 基类引用，**不暴露 `ProfileFragment` 的具体类型**，也不暴露 feature 模块的其他页面。`:feature:profile` 通过 `implementation` 被 `:opensdk-impl` 依赖，不会进入第三方的编译类路径。

如果 Fragment 内部直接依赖了 `:core:ui` 的某些组件，这些依赖会在合并 AAR 时一起打入，但**它们的类名和 API 不会因为“在 AAR 中”而自动成为对外契约**——对外契约只由 `:opensdk-api` 中 `SdkUi` 的方法签名决定。

---

## 5. SDK 打包方式

推荐使用 **Fused Library Plugin**（AGP 内置）或 **Fat AAR 插件**，把 `:opensdk-impl`、`:core:data` 以及所需的 feature UI 合并为**单个 AAR**。

### 5.1 方案 A：Fused Library Plugin（官方，AGP 9.0+）

Fused Library 的职责是把多个 Android Library 模块**打包成一个可发布的 AAR**，支持通过 `include` 声明要融合的模块。它不负责混淆，混淆需要另配 R8。

```kotlin
// :opensdk-impl/build.gradle.kts
plugins {
    id("com.android.library")
    id("com.android.fusedlibrary")  // Fused Library
    `maven-publish`
}

fusedLibrary {
    include(project(":opensdk-api"))
    include(project(":core:data"))
    include(project(":feature:profile"))  // 如果需要该 feature 的 Fragment
    // :core:model、:core:network、:core:database 会作为传递依赖被打入
}
```

发布时同时发布 `opensdk-api` 和融合后的 `opensdk` 两个 artifact。

### 5.2 方案 B：Fat AAR 插件（兼容较旧 AGP）

如果项目 AGP 版本低于 9.0，使用 `com.kezong:fat-aar`：

```kotlin
// :opensdk-impl/build.gradle.kts
plugins {
    id("com.android.library")
    id("com.kezong.fat-aar")
}

dependencies {
    embed(project(":opensdk-api"))
    embed(project(":core:data"))
    embed(project(":feature:profile"))
    implementation("androidx.core:core-ktx:1.12.0")  // 远程依赖保持 implementation
}
```

`embed` 会把模块的代码和资源合入最终 AAR，`implementation` 的远程依赖会写入 POM 由第三方自动拉取。

无论哪种方案，最终 AAR 内部都包含了 `:core:data` 的实现类。**这些类会出现在 AAR 的 `classes.jar` 中，但不会成为对外编译契约**，因为 `:opensdk-api` 才是第三方唯一 compile 的入口。

### 5.3 混淆与最小暴露：关键配置

打包合并不会自动隐藏内部实现。需要在 `:opensdk-impl` 的 release 构建中开启 R8，并且只 keep `:opensdk-api` 的门面：

```kotlin
// :opensdk-impl/build.gradle.kts
android {
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            consumerProguardFiles("consumer-rules.pro")
        }
    }
}
```

`proguard-rules.pro`（SDK 自身打包用）：

```proguard
# 只保留 opensdk-api 中对外契约
-keep,allowoptimization interface com.example.opensdk.api.** { *; }
-keep,allowoptimization class com.example.opensdk.api.** { *; }

# 不保留 :core:data 和 :opensdk-impl 的公共类
-dontkeep class com.example.core.data.** { *; }
-dontkeep class com.example.opensdk.impl.** { *; }
```

注意：**不要对整个 `:core:data` 包使用宽泛的 `-keep`**，否则 Repository、DataSource、Entity 的类名都会保留，等于把内部结构暴露在 AAR 中。

`consumer-rules.pro`（交给第三方 App 构建时使用）：

```proguard
# 只保留第三方需要直接引用的 SDK 模型和门面
-keep class com.example.opensdk.api.SdkUserProfile { *; }
-keep interface com.example.opensdk.api.SdkDataProvider { *; }
```

使用 `consumerProguardFiles` 而不是 `proguardFiles` 的原因：这些规则会被打包进 AAR，第三方 App 执行 R8 时自动生效，避免他们手动补规则。

> 混淆是**增加逆向成本**，不是访问控制。真正的边界仍然是：`:opensdk-api` 中有什么，第三方才能编译期调用什么。

---

## 6. 第三方使用方式

### 6.1 依赖配置（Maven 推荐）

将 `opensdk-api` 和 `opensdk`（融合 AAR）发布到私有 Maven 仓库后，第三方在 `build.gradle` 中：

```kotlin
dependencies {
    implementation("com.example.sdk:opensdk-api:1.0.0")
    implementation("com.example.sdk:opensdk:1.0.0")
}
```

如果使用 Fat AAR 的 `embed` 方式，`opensdk` 的 POM 中不会包含内部模块坐标，第三方**不需要**再单独引入 `:core:data` 的 artifact；远程依赖（如 `androidx.core`）会通过 POM 自动传递。

### 6.2 第三方代码中的调用形态

```kotlin
// 第三方拿到的是 SdkDataProvider，而不是 UserRepository
val provider: SdkDataProvider = Sdk.createDataProvider(context)

lifecycleScope.launch {
    val profile = provider.getUserProfile("123")
    // profile 的类型是 SdkUserProfile，字段只有 id / displayName / avatarUrl
}

// 加载 SDK 提供的 Fragment
val fragment = SdkUi.createProfileFragment(bundleOf("userId" to "123"))
supportFragmentManager.beginTransaction()
    .replace(R.id.container, fragment)
    .commit()
```

第三方**看不到** `UserRepository`、`User` entity、`DefaultUserRepository`，也看不到 `ProfileFragment` 的具体类名。他们能用的只是 `:opensdk-api` 中声明的门面。

### 6.3 初始化

在 `:opensdk-impl` 中提供 `Sdk` 入口的 `internal` 实现，通过 `:opensdk-api` 中的 `Sdk` 对象或 `ServiceLoader` 绑定：

```kotlin
// :opensdk-api
object Sdk {
    private var dataProviderFactory: ((Context) -> SdkDataProvider)? = null

    @JvmStatic
    fun init(context: Context) {
        // 由 :opensdk-impl 通过反射或 ServiceLoader 注入实现
        dataProviderFactory = SdkImplLoader.load(context)
    }

    fun createDataProvider(context: Context): SdkDataProvider =
        requireNotNull(dataProviderFactory)(context) { "Sdk.init() not called" }
}
```

实现注入的机制可以是 `ServiceLoader`（在 `:opensdk-impl` 的 `META-INF/services` 中注册），也可以是简单的反射加载，取决于你希望第三方集成时是否额外配置 ProGuard。

---

## 7. 一张图总结依赖与发布边界

```
内部开发依赖（不对外）：
app:prod → feature:* → core:data → core:model / core:network / core:database
                    → core:ui

SDK 发布依赖（对外）：
第三方 APK
 ├── opensdk-api (compile)
 └── opensdk     (runtime)
       └── 合并了 opensdk-impl + core:data + 所需 feature UI
```

**关键判断**：

| 问题 | 答案 |
|---|---|
| 第三方能否直接调用 `UserRepository`？ | 不能，`:opensdk-api` 中不存在该类型 |
| 第三方拿到的 `SdkUserProfile` 能否包含 `core:model` 的全部字段？ | 不能，只能包含 `toSdkModel()` 显式映射的子集 |
| 第三方能否引用 `ProfileFragment` 的具体类？ | 不能，只能通过 `SdkUi` 拿到 `Fragment` |
| `core:data` 的实现类是否在 AAR 中？ | 是，但经过 R8 后类名被混淆，且不在 compile 契约中 |
| app:prod 是否依赖 SDK 模块？ | 不依赖，SDK 是独立的导出通道 |

这个架构的代价是需要在 `:opensdk-api` 中维护一套**独立的 SDK 模型和接口**，而不是复用内部 Repository。但这是“数据子集”能够被**编译期强制**而非靠约定来保证的唯一可靠方式。

---

## 8. 本项目落地（CarBack）

本项目按上述方案将原单模块 `:sdk` 重构为两层，模块名与目录对应关系：

| 模块 | 目录 | 职责 |
|---|---|---|
| `:opensdk:api` | `opensdk/api` | 纯契约：`SdkTrack` / `SdkTrackProvider`（只读）/ `SdkUi` / `SdkBridge` / `Sdk`（反射绑定入口）。零内部依赖，可独立发 AAR |
| `:opensdk:impl` | `opensdk/impl` | 装配层：`SdkBridgeImpl`（反射桥接 + `Bootstrap.install`）、`SdkTrackProviderImpl`（`MineApi`/`SearchApi` → `SdkTrack` 裁剪）、`SdkUiImpl`（`Router` 查表创建 Fragment） |

与 README 通用方案的差异点：

1. **模块命名与目录一致**：实际模块名为 `:opensdk:api` / `:opensdk:impl`（上文 1–7 节为通用方案，命名 `:opensdk-api` / `:opensdk-impl` 是其历史写法）。**app 不依赖任何 opensdk 模块**：严格执行"app:prod 不依赖 opensdk"，SDK 是纯"导出通道"，宿主只提供页面与数据。SDK 仅支持第三方同进程集成（`Sdk.init` / `Sdk.ui()` / `Sdk.trackProvider()`），不支持跨进程拉起宿主页面——无 exported 入口 Activity，也无 `CarBackSdk` 跨进程门面。
2. **数据子集对应关系**：内部 `MineApi.listenHistory` / `SearchApi.search` 经 `toSdkTrack()` 显式映射为 `SdkTrack(id/title/artist)`；写操作（`record` / `clearHistory`）不在 `SdkTrackProvider` 契约中，第三方编译期无写入口（对应第 3.3 节）。
3. **UI 门面**：`SdkUiImpl.createPageFragment(path)` 通过 `Router.routeClass(path)` 查表反射实例化，不直接 `new` feature Fragment；第三方只拿到 `Fragment` 基类引用（对应第 4 节）。
4. **实现绑定**：采用第 6.3 节的"简单反射加载"（`Class.forName(SDK_BRIDGE_CLASS)`），避免 AAR 对 `META-INF/services` 的合并兼容问题。
5. **同进程集成约束**：宿主运行时（路由表/服务表）依赖 Hilt 多绑定，第三方同进程集成的 Application 必须是 `@HiltAndroidApp`。

打包：当前 AGP 8.10.1（< 9.0），发布融合 AAR 时采用第 5.2 节 Fat AAR 方案，`embed(project(":opensdk:api"))` + `embed(project(":core:data 等价物": core/feature impl))`；`consumer-rules.pro` 已随 `:opensdk:impl` 提供，只 keep `cn.frank.carback.opensdk.api.**` 与桥接实现类。

---

## 9. 打包输出 AAR 文件

### 9.1 命令与产物路径

```bash
# 打 release AAR（两个 artifact 一次打出）
./gradlew :opensdk:api:assembleRelease :opensdk:impl:assembleRelease

# 打 debug AAR（调试用）
./gradlew :opensdk:api:assembleDebug :opensdk:impl:assembleDebug
```

产物路径（模块名已通过 `base { archivesName }` 固定，与目录名 `api`/`impl` 无关）：

| Artifact | 路径 |
|---|---|
| `opensdk-api`（契约 AAR） | `opensdk/api/build/outputs/aar/opensdk-api-release.aar` |
| `opensdk-impl`（装配 AAR） | `opensdk/impl/build/outputs/aar/opensdk-impl-release.aar` |

`:opensdk:impl` 的 release AAR 已开启 **library R8**（见第 5.3 节）：`opensdk.api.**` 与 `SdkBridgeImpl` 保持原名，其余内部实现类混淆（如 `SdkTrackProviderImpl -> a.d`）。混淆映射表在 `opensdk/impl/build/outputs/mapping/release/mapping.txt`，线上还原堆栈必须保留对应版本。宿主 `:app` 的 release APK 同样开启了 R8 + 资源收缩，映射表在 `app/build/outputs/mapping/release/mapping.txt`。

### 9.2 两个 artifact 的使用方式

- **`opensdk-api-release.aar`**：纯契约 AAR，无内部依赖。第三方编译期与运行期都需要它。
- **`opensdk-impl-release.aar`**：装配层 AAR。`consumer-rules.pro` 已打包在 AAR 内，第三方 App 执行 R8 时自动生效。

**直接拷贝 `.aar` 文件分发时要注意**：`:opensdk:impl` 通过 `implementation` 依赖的 `:core:*`、`:feature:*` 模块**不会**被打进这个 AAR（它们只记录在 Gradle 的依赖关系中）。因此：

1. **Maven 仓库分发（推荐）**：先给两个模块加上 `maven-publish` 并补上发布坐标，第三方通过 POM 自动拉取传递依赖——需把 core / feature 各模块也发布到同一仓库；
2. **单文件分发**：按第 5 节用 Fat AAR（`embed`）把 `:opensdk:impl` 与内部模块融合成一个独立 AAR，第三方只需引入 `opensdk-api` + 融合后的 `opensdk` 两个文件。
