# CarBack

模块化 Android 示例工程：以 **feature 三件套 + 注解路由 + 服务注册** 为主干，每个业务模块可以独立编译、独立运行，宿主只负责"组装"。

- **编译期装配**：`@Route` 由 KSP 生成 Hilt 多绑定，运行期不扫描、不反射查找路由
- **契约先行**：跨 feature 只允许依赖对方 `api` 模块，构建脚本强制守卫
- **失败前置**：路由路径冲突、服务重复注册在启动第一刻崩溃，不带到线上
- **可插拔可降级**：宿主少装一个 feature，调用方仍能编译并优雅降级（路由宽松模式 + `serviceOrNull`）
- **UI 基建**：通用列表页、页面状态、生命周期修正、曝光埋点
- **对外 SDK**：`opensdk:api`（纯契约）+ `opensdk:impl`（装配层）双层隔离，契约可独立发布 AAR

> 工程内数据均为内存 mock，重点在架构骨架而非完整业务。

## 快速开始

环境：JDK 17、Android SDK 35（compileSdk 35 / minSdk 23；Gradle 8.14.3 / AGP 8.10.1 / Kotlin 2.1.10 / Hilt 2.57.2 由 wrapper 与 `build-logic` 提供）。

```bash
# 主壳：单一 Fragment 容器，起始页 home/main
./gradlew :app:installDebug

# 单 feature 独立调试（三个 demo 壳）
./gradlew :feature:home:demo:installDebug
./gradlew :feature:mine:demo:installDebug
./gradlew :feature:search:demo:installDebug

# 架构一致性自检（模块结构 / 依赖守卫 / 路由唯一性）
python3 scripts/check.py

# 打包对外 SDK AAR（opensdk/api 与 opensdk/impl，详见 opensdk/README.md）
./gradlew :opensdk:api:assembleRelease :opensdk:impl:assembleRelease
```

## 模块结构

```
app                        主壳：装配全部 feature impl，单一 Fragment 容器
opensdk/
  api                      对外契约（可独立发 AAR，零内部依赖）：Sdk / SdkBridge / SdkTrack / SdkTrackProvider / SdkUi
  impl                     SDK 装配层：反射桥接 / 模型裁剪 / Fragment 工厂（implementation 隐藏内部模块）
core/
  model                    TrackModel、UiState / UiStateFlow / StateResult
  ui                       BaseFragment、通用列表页、CommonListAdapter、PageStateView、TitleBar、ExposureManager / EventTracker
  router                   @Route、Router、FragmentRouter、ServiceRegistry / FeatureApi（服务注册中心）
  router-compiler          KSP 处理器：生成 <Feature>ImplRouterModule
  debug-scaffold           demo 壳通用 Application（BaseDemoApplication）/ 宿主 Activity（ScaffoldActivity）
feature/<name>/
  api                      跨 feature 契约：能力接口 / 事件 / 展示模型 / View 级契约（如 home:api 的 TrackListAdapter）
  impl                     实现：Fragment + ViewModel + Hilt 注册
  demo                     可独立运行的薄壳
```

当前路由表：

| 路由 | 页面 | 所属模块 |
| --- | --- | --- |
| `home/main` | HomeFragment（内含 ViewPager 双 Tab：home/recommend + mine/main） | home:impl |
| `home/recommend` | RecommendContainerFragment（搜索入口 + 推荐流） | home:impl |
| `detail/list` | DetailListFragment（通用列表详情页，参数 `id`） | home:impl |
| `search/main` | SearchFragment | search:impl |
| `mine/main` | MineFragment（收听历史） | mine:impl |

依赖方向（单向，不允许反向或 impl 之间互依赖）：

```
app ──► feature:home:impl ──► feature:home:api（含 TrackListAdapter 抽象契约）
 │            ├─► feature:mine:api      （只认契约）
 │            └─► core:ui / core:router / core:model
 ├─► feature:mine:impl ──► feature:mine:api
 ├─► feature:search:impl ──► feature:search:api ──► feature:home:api（复用 View 级契约）
 │        （search:demo 额外装配 home:impl：运行期需要 HomeApi 提供 TrackListAdapter 与 detail/list 页面）
 └─► 不依赖任何 opensdk 模块（SDK 是纯导出通道）
```

SDK 边界：`:opensdk:impl` 通过 `implementation` 依赖 core/feature 模块，第三方编译类路径上只有 `:opensdk:api` 的门面契约；主壳**不依赖任何 opensdk 模块**——SDK 是对外的导出通道，宿主只提供页面与数据，不消费门面。

## 设计原理

### 1. feature 三件套

| 模块 | 职责 | 允许依赖 |
| --- | --- | --- |
| `api` | 对外契约、跨模块展示模型、View 级契约（抽象 Adapter 等） | `core:model` / `core:router`、其他 `api` |
| `impl` | 页面、ViewModel、仓库、Hilt 注册 | 自己的 `api`、其他 `api`、`core:*` |
| `demo` | 独立可运行壳，只装自己的 `impl`（可额外装他人的 impl） | `core:debug-scaffold` |

`carback-feature-impl` 插件在 `afterEvaluate` 阶段扫描依赖，一旦出现 `:feature:*:(impl|demo)` 直接 `GradleException`。因此"绕过契约直接调别的 feature 实现"在编译期就不可能。

### 2. 路由：编译期装配，运行期只查表

```
@Route("home/main")
      │ KSP（参数 router.moduleName 由模块路径推导）
      ▼
HomeImplRouterModule（Hilt @IntoSet，编译进 home:impl）
      │ Bootstrap.install → EntryPointAccessors 取出 Set<RouteEntry>
      ▼
Router：Map<path, RouteEntry>，路径冲突立即 require 失败
      │
      ▼
Router.navigate() → FragmentRouter.push（add / hide / show + 自维护栈）
```

- **零反射装配**：路由表完全由编译期生成的 Hilt 多绑定拼装；`Router.fragment()` 只是用无参构造创建页面实例。
- **状态保留**：页面切换用 `hide/show` 而非 `replace`，返回后 View/滚动位置/输入内容仍在；支持 `singleInstance`（同 tag 单实例，重复跳转会弹掉旧实例之上的页面）。
- **动画**：入栈 / 出栈动画由 `FragmentAnimation`（`SLIDE_IN_RIGHT` / `SLIDE_OUT_RIGHT` / `NONE`）统一控制。
- **进程重建**：`FragmentRouter.saveInstanceState()` 持久化栈内 tag，`init()` 时用 `findFragmentByTag` 找回系统重建的 Fragment，并恢复"只有栈顶可见"的状态。
- **返回语义**：`pop()` 与 Activity 一致，返回栈为空时返回 `false`，由宿主 `finish()`。

**宽松降级模式（demo 专用）**：独立编译运行时 demo 壳的 classpath 上只有本 feature 的 impl，跳转到未装配路由（如 home demo 里点搜索图标跳 `search/main`）原本会直接崩溃。`Bootstrap.install(context, lenientRoutes = true)`（由 `BaseDemoApplication` 统一开启）后，`Router.fragment()` 返回 `null`、`Router.navigate()` 打日志并忽略；生产壳保持默认严格模式，路由缺失在第一跳快速失败。

### 3. 服务注册：编译期认契约，运行期可裁剪

```
api 模块      interface MineApi : FeatureApi
impl 模块     @Provides @IntoSet fun ...(impl: MineApiImpl): FeatureApi
启动          ServiceRegistry.init → Set<FeatureApi>（重复注册立即失败）
消费          ServiceRegistry.service<MineApi>()          // 必须存在（require）
              ServiceRegistry.serviceOrNull<HomeApi>()    // 可缺失，优雅降级
```

- 与 Hilt 分工：**Hilt 负责模块内部依赖注入**，**ServiceRegistry 负责跨 feature 解析**，避免服务定位器污染模块内代码。
- 宿主没装配某个 feature 时，`serviceOrNull` 返回 `null`，消费方自行降级（例如 search 页在无 HomeApi 时展示空态提示）。
- 重复注册检测会遍历实现的全部 `FeatureApi` 子接口，同一契约出现两个实现即启动崩溃。

### 4. 跨模块数据流：StateFlow

跨 feature 的数据共享统一走 `api` 契约中的 `StateFlow`，没有事件总线：

```
写侧（home/search 播放时）   ServiceRegistry.service<MineApi>().record(track)
读侧（首页"最近收听"）       ServiceRegistry.serviceOrNull<MineApi>()?.listenHistory?.collect { ... }
```

`MineApi.listenHistory: StateFlow<List<TrackModel>>` 是单一数据源：`record` 回写后，首页与"我的"页响应式刷新，多页面订阅同一份状态。"当前状态"走 StateFlow，不用事件广播；跨 feature 契约（接口 + 数据模型）都定义在 `api` 模块。

### 5. UI 基建

**BaseFragment**（`core:ui`）

- `onCreateView` 为 `final`，通过泛型 `ViewBinding` 反射 inflate 一次，`onDestroyView` 自动置空；
- show/hide 切换时 Fragment 不会回调 `onResume/onPause`，`safeLifecycleOwner` 把"宿主生命周期 + 页面可见性"合成为可靠生命周期；
- `startCollect { }` 统一在 `RESUMED` 时收集、`PAUSED` 时暂停、`DESTROYED` 后永久停止；多个 Flow 在 `supervisorScope` 中并行，单条异常只记录日志，不影响其他订阅。

**UiStateFlow**（`core:model`）

- `UiState`：`Loading / Success / Error`，`LoadType` 区分刷新与加载更多；
- `RefreshTrigger` 经 `flatMapLatest` 合并请求，新请求自动取消旧请求；`asStateResult` 统一捕获异常、发 Loading；
- 空数据自动转 `EmptyDataError`；`Success.uniqueId` 绕过 StateFlow 的相等去重，保证"重复刷新同一份数据"也能走完 `finishRefresh`。

**通用列表**

- `AbsListFragment` / `CommonListFragment` = `SmartRefreshLayout` + `RecyclerView` + `PageStateView` + `TitleBar`，子类提供 `viewModel`，并通过 `adapter` 属性或 `registerPresenter` 定制条目；
- `CommonListViewModel` 负责页码累加与全量缓存，子类只写 `getListFlow(pageNo)`（参数经 `SavedStateHandle` 传入，如 `detail/list` 的 `id`）；
- `CommonListAdapter` 按 Model 类型路由到对应 `ItemPresenter`，一个列表可混排多种条目；
- `PageStateView` 按需 inflate 状态页、支持配置变更后恢复，并同时作为 `NestedScrollingChild3/Parent3` 转发滚动，避免状态页挡住外层折叠头部和下拉刷新。

**曝光埋点**（`core:ui` 的 `cn.frank.carback.track` 包）

`ExposureManager`：可见面积占比 + 连续停留时长 + key 去重；有待判定条目时走帧回调，空闲时降级为低频轮询，避免长期占用 VSYNC。`EventTracker` 封装曝光 / 点击上报，由页面注入给条目 Presenter。

### 6. 构建约定

所有模块只应用 `build-logic` 里的插件，根脚本保持空实现，版本集中在 `gradle/libs.versions.toml`。

| 插件 | 作用 |
| --- | --- |
| `carback-android-application` | SDK 版本、Java 17、ViewBinding、namespace 推导 |
| `carback-android-library` | 同上（library 版） |
| `carback-android-hilt` | Hilt 插件 + 运行时 + KSP 编译器 |
| `carback-feature-api` | 契约模块默认依赖（`core:model` / `core:router`，implementation） |
| `carback-feature-impl` | core 三件套 + 自有 api + 路由 KSP + **跨 feature 依赖守卫** |
| `carback-feature-demo` | application + hilt + `core:debug-scaffold`，开箱即可运行 |
| `carback-kotlin-jvm` | 纯 JVM 模块（KSP 处理器） |

`namespace` 由模块路径自动推导：`:feature:home:impl` → `cn.frank.carback.feature.home.impl`。`carback-feature-impl` 同时把 `router.moduleName` 注入 KSP，`:feature:home:impl` → `HomeImplRouterModule`。

### 7. 对外 SDK

SDK 按方案拆为两层（模块名与物理目录一致）：

- `:opensdk:api`：纯契约，零内部依赖——SDK 专属模型 `SdkTrack`、只读数据门面 `SdkTrackProvider`（不含写入口）、UI 门面 `SdkUi`、统一入口 `Sdk`（反射绑定实现）、桥接契约 `SdkBridge`。
- `:opensdk:impl`：装配层——`SdkBridgeImpl` 完成反射绑定与 `Bootstrap.install`；`SdkTrackProviderImpl` 把内部 `MineApi` / `SearchApi` 经 `toSdkTrack()` 编译期裁剪为 `SdkTrack` 子集；`SdkUiImpl` 经 `Router` 查表创建 Fragment，第三方只拿到 `Fragment` 基类引用。

主壳 `:app` **不依赖任何 opensdk 模块**：SDK 仅支持第三方同进程集成，不支持跨进程拉起宿主页面（无 exported 入口 Activity、无跨进程门面）。

第三方编译类路径上只有 `:opensdk:api`；`:opensdk:impl` 对 core / feature 的依赖全部是 `implementation`，配合 `consumer-rules.pro` 只 keep 门面契约。发布时按 `opensdk/README.md` 以 Fused / Fat AAR 融合为单个 AAR。

## 如何使用

### 页面跳转与传参

```kotlin
// 普通入栈（默认右滑入动画）
Router.navigate("detail/list")

// 带参数：写入 Fragment.arguments（key 由目标页面约定），页面的 SavedStateHandle 可直接取
Router.navigate("detail/list", args = bundleOf("id" to menuId))

// 保证栈中同路由只有一个实例
Router.navigate("mine/main", singleInstance = true)

// 关闭转场动画
Router.navigate("home/main", animation = FragmentAnimation.NONE)

// 直接创建 Fragment 实例（如 ViewPager2 Adapter、SDK Fragment 工厂）
val fragment: Fragment? = Router.fragment("home/recommend")

// 返回
Router.back()
```

页面侧读取参数：

```kotlin
private val menuId: String = savedStateHandle["id"] ?: ""
```

底层栈操作（`FragmentRouter.push / replace / popTo / popAllAndPush / currentTag`）仅在 `Router` 覆盖不到的场景直接使用；业务代码统一走 `Router`。

### 新增一个 feature

以 `player` 为例：

1. `settings.gradle.kts` 的 feature 列表追加 `"player"`，并在 `scripts/check.py` 的 `FEATURES` 同步。
2. 创建 `feature/player/{api,impl,demo}`，三个 `build.gradle.kts` 分别只写：

```kotlin
// api
plugins { id("carback-feature-api") }

// impl
plugins { id("carback-feature-impl") }

// demo
plugins { id("carback-feature-demo") }

android {
    defaultConfig { applicationId = "cn.frank.carback.demo.player" }
}
dependencies { implementation(project(":feature:player:impl")) }
```

3. `api` 中定义契约（需要跨 feature 调用时才需要）：

```kotlin
interface PlayerApi : FeatureApi {
    fun play(trackId: String)
}
```

4. `impl` 中写页面并注册服务：

```kotlin
@Route("player/main")
@AndroidEntryPoint
class PlayerFragment : BaseFragment<FragmentPlayerBinding>() { /* ... */ }

@Module
@InstallIn(SingletonComponent::class)
object PlayerImplModule {
    @Provides
    @Singleton
    fun providePlayerApi(impl: PlayerApiImpl): PlayerApi = impl

    @Provides
    @IntoSet
    fun providePlayerService(impl: PlayerApiImpl): FeatureApi = impl
}
```

5. `demo` 中两行壳（`@HiltAndroidApp` 的 Application + 起始路由；`BaseDemoApplication` 已自动开启路由宽松降级）：

```kotlin
@HiltAndroidApp
class DemoApplication : BaseDemoApplication()

class DemoMainActivity : ScaffoldActivity() {
    override val startRoute: String get() = "player/main"
}
```

6. 主壳装配：`app/build.gradle.kts` 加 `implementation(project(":feature:player:impl"))`。

路由注册不需要额外代码，KSP 会在 `player:impl` 内生成 `PlayerImplRouterModule`。

### 跨 feature 调用

```kotlin
// 能力调用（api 契约）
val mineApi = ServiceRegistry.service<MineApi>()          // 必须存在
val homeApi = ServiceRegistry.serviceOrNull<HomeApi>()    // 可能未装配，降级处理

// 数据订阅（单一状态源，写侧 record 后所有订阅方响应式刷新）
ServiceRegistry.service<MineApi>().record(TrackModel(id, title))
startCollect {
    ServiceRegistry.serviceOrNull<MineApi>()?.listenHistory?.collect { tracks -> /* ... */ }
}

// 页面跳转（路由未装配时：demo 宽松降级，生产快速失败）
Router.navigate("mine/main")
```

### 写一个列表页

```kotlin
@HiltViewModel
class PlayerListViewModel @Inject constructor(
    private val repository: PlayerRepository,
) : CommonListViewModel() {
    override fun getListFlow(pageNo: Int): Flow<List<ItemModel>> =
        flow { emit(repository.page(pageNo).map { PlayerRow(it) }) }
}

@Route("player/list")
@AndroidEntryPoint
class PlayerListFragment : CommonListFragment() {
    override val viewModel: PlayerListViewModel by viewModels()

    override fun registerPresenter(adapter: CommonListAdapter<ItemModel>) {
        adapter.register(PlayerRowPresenter())
    }
}
```

`PlayerRowPresenter` 实现 `ItemPresenter<PlayerRow, ItemPlayerBinding>` 即可；也可以像 `DetailListFragment` 那样直接覆写 `adapter` 属性换整个 Adapter（如复用 `SongListAdapter`）。刷新、加载更多、页码累加、Loading/Empty/Error 切换都由基类完成。

页面状态可直接在 XML 配置：

```xml
<cn.frank.carback.page.PageStateView
    android:id="@+id/pageStateView"
    android:layout_width="0dp"
    android:layout_height="0dp"
    app:emptyText="暂无内容"
    app:errorText="网络开小差了"
    app:retryButtonText="重试"
    app:retryButtonVisible="true" />
```

### 对外 SDK

SDK 仅提供**同进程集成**（不支持跨进程拉起宿主页面）。第三方 App 依赖 `opensdk:api` 契约 + 融合后的 `opensdk:impl` AAR（Application 需为 `@HiltAndroidApp`）：

```kotlin
Sdk.init(this)                       // Application.onCreate 中，反射绑定 :opensdk:impl

val provider = Sdk.trackProvider()   // 只读数据门面
lifecycleScope.launch { provider.listenHistory.collect { tracks -> ... } }

val fragment = Sdk.ui().createPageFragment("search/main")   // Fragment 基类引用
supportFragmentManager.beginTransaction().replace(R.id.container, fragment).commit()
```

第三方看不到 `MineApi` / `SearchApi` / feature Fragment 具体类型，也拿不到写入口（record / clearHistory 不在 SDK 契约中）。

## 约定

- Fragment 必须提供无参构造，参数通过 `arguments` / `SavedStateHandle` 获取。
- 跨 feature 只能依赖对方 `api`；禁止 `implementation(project(":feature:xxx:impl"))`（构建守卫会拦截）。
- 不直接 `new` 其他 feature 的页面，统一走 `Router`。
- 路由 path 全局唯一，命名统一为 `feature/page`（或 `feature/形态`，如 `detail/list`）。
- 跨 feature 页面入口要么包一层 `Router.hasRoute` 判断，要么直接调用 `Router.navigate` 依赖降级机制（demo 宽松 / 生产快速失败）。
- 新增模块一律使用 `build-logic` 插件，不在模块内重复写 SDK 版本、Java 版本、namespace。
- `Bootstrap.install(this)` 必须在 `Application.onCreate()` 中调用，路由表与服务表依赖它初始化；仅 demo 壳传 `lenientRoutes = true`。

## 已知限制

- 业务数据全部为内存 mock（推荐流、搜索源、收听历史），替换为真实数据源即可。
- demo 壳对未装配的跨 feature 路由 / 服务走降级分支（打日志、不跳转或展示空态），属预期行为；search demo 因复用 home 的 `TrackListAdapter` 额外装配了 home impl，随之可跳转 `detail/list`。
- `PageStateView` 的可见性判定不覆盖"被兄弟 View 完全遮挡"的极端场景。
- `build-logic/build.gradle.kts` 中 AGP / Kotlin / KSP 版本需与 `gradle/libs.versions.toml` 手动保持一致。
