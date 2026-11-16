# CarBack

车载场景的模块化 Android 示例工程：以 **feature 三件套 + 注解路由 + 服务注册** 为主干，每个业务模块可以独立编译、独立运行，宿主只负责"组装"。

- **编译期装配**：`@Route` 由 KSP 生成 Hilt 多绑定，运行期不扫描、不反射查找路由
- **契约先行**：跨 feature 只允许依赖对方 `api` 模块，构建脚本强制守卫
- **失败前置**：路由路径冲突、服务重复注册在启动第一刻崩溃，不带到线上
- **可插拔**：宿主少装一个 feature，调用方仍能编译并优雅降级
- **UI 基建**：通用列表页、页面状态、生命周期修正、曝光埋点
- **对外 SDK**：`sdk` 模块零内部依赖，可单独发布 AAR

> 工程内数据均为内存 mock，重点在架构骨架而非完整业务。

## 快速开始

环境：JDK 17、Android SDK 35（Gradle 8.14.3 / AGP 8.10.1 由 wrapper 与 `build-logic` 提供）。

```bash
# 主壳：首页 / 我的 / 搜索 三 Tab
./gradlew :app:installDebug

# 单 feature 独立调试（三个 demo 壳）
./gradlew :feature:home:demo:installDebug
./gradlew :feature:mine:demo:installDebug
./gradlew :feature:search:demo:installDebug

# 架构一致性自检（模块结构 / 依赖守卫 / 路由唯一性）
python3 scripts/check.py
```

## 模块结构

```
app                        主壳：装配 feature impl + 底部导航 + SDK 入口 Activity
sdk                        对外契约（可独立发 AAR）：CarBackSdk / SdkContract
core/
  common                   模型、UiState/UiStateFlow、EventBus、ServiceRegistry
  ui                       BaseFragment、通用列表页、CommonListAdapter、PageStateView
  router                   @Route、Router、FragmentRouter（自维护返回栈）
  router-compiler          KSP 处理器：生成 <Feature>ImplRouterModule
  debug-scaffold           demo 壳通用 Application / Activity
feature/<name>/
  api                      跨 feature 契约：接口 / 事件 / 展示模型
  impl                     实现：Fragment + ViewModel + Hilt 注册
  demo                     可独立运行的薄壳
```

依赖方向（单向，不允许反向或 impl 之间互依赖）：

```
app ──► feature:home:impl ──► feature:home:api
 │            ├─► feature:mine:api      （只认契约）
 │            └─► core:ui / core:router / core:common
 ├─► feature:mine:impl ──► feature:mine:api
 ├─► feature:search:impl ──► feature:search:api ──► feature:home:api（复用 View 级契约）
 └─► sdk
```

## 设计原理

### 1. feature 三件套

| 模块 | 职责 | 允许依赖 |
| --- | --- | --- |
| `api` | 对外契约、事件、跨模块展示模型 | `core:common`、其他 `api` |
| `impl` | 页面、ViewModel、仓库、Hilt 注册 | 自己的 `api`、其他 `api`、`core:*` |
| `demo` | 独立可运行壳，只装自己的 `impl` | `core:debug-scaffold` |

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
Router.open() → FragmentRouter（add / hide / show + 自维护栈）
```

- **零反射装配**：路由表完全由编译期生成的 Hilt 多绑定拼装；`Router.fragment()` 只是用无参构造创建页面实例。
- **Tab 不进返回栈**：`Router.openTab()` 走 `FragmentRouter.switchTab`，Tab 页常驻（首次懒创建，之后 hide/show 复用同一实例，视图状态保留）；切换 Tab 会丢弃返回栈中的普通页面。
- **状态保留**：普通页面切换用 `hide/show` 而非 `replace`，返回后 View/滚动位置/输入内容仍在。
- **进程重建**：`FragmentRouter.saveInstanceState()` 持久化栈内 tag，`init()` 时用 `findFragmentByTag` 找回系统重建的 Fragment，并恢复"只有栈顶可见"的状态。
- **返回语义**：`pop()` 与 Activity 一致，返回栈为空（只剩 Tab 页）时返回 `false`，由宿主 `finish()`。

### 3. 服务注册：编译期认契约，运行期可裁剪

```
api 模块      interface MineApi : FeatureApi
impl 模块     @Provides @IntoSet fun ...(impl: MineApiImpl): FeatureApi
启动          ServiceRegistry.init → Set<FeatureApi>（重复注册立即失败）
消费          ServiceRegistry.service<MineApi>()        // 必须存在
              ServiceRegistry.serviceOrNull<HomeApi>()   // 可缺失，优雅降级
```

- 与 Hilt 分工：**Hilt 负责模块内部依赖注入**，**ServiceRegistry 负责跨 feature 解析**，避免服务定位器污染模块内代码。
- 宿主没装配某个 feature 时，`serviceOrNull` 返回 `null`，消费方自行降级（例如 search 页在无 home 时展示空态）。
- 重复注册检测会遍历实现的全部 `FeatureApi` 子接口，同一契约出现两个实现即启动崩溃。

### 4. 事件总线

`EventBus` 基于 `MutableSharedFlow(extraBufferCapacity = 64)`：非粘性、发送不挂起、无订阅者时丢弃。事件定义放在 `api` 模块（如 `TrackPlayedEvent`），**"已发生"的通知走 EventBus，"当前状态"走 StateFlow**（如 `MineApi.listenHistory`）。

### 5. UI 基建

**BaseFragment**（`core:ui`）

- `onCreateView` 为 `final`，通过泛型 `ViewBinding` 反射 inflate 一次，`onDestroyView` 自动置空；
- show/hide 切换时 Fragment 不会回调 `onResume/onPause`，`safeLifecycleOwner` 把"宿主生命周期 + 页面可见性"合成为可靠生命周期；
- `startCollect { }` 统一在 `RESUMED` 时收集、`PAUSED` 时暂停、`DESTROYED` 后永久停止；多个 Flow 在 `supervisorScope` 中并行，单条异常只记录日志，不影响其他订阅。

**UiStateFlow**（`core:common`）

- `UiState`：`Loading / Success / Error`，`LoadType` 区分刷新与加载更多；
- `RefreshTrigger` 经 `flatMapLatest` 合并请求，新请求自动取消旧请求；`asStateResult` 统一捕获异常、发 Loading；
- 空数据自动转 `EmptyDataError`；`Success.uniqueId` 绕过 StateFlow 的相等去重，保证"重复刷新同一份数据"也能走完 `finishRefresh`。

**通用列表**

- `CommonListFragment` = `SmartRefreshLayout` + `RecyclerView` + `PageStateView`，子类只需提供 `viewModel` 与 `registerPresenter`；
- `CommonListViewModel` 负责页码累加与全量缓存，子类只写 `getListFlow(pageNo)`；
- `CommonListAdapter` 按 Model 类型路由到对应 `ItemPresenter`，一个列表可混排多种条目；
- `PageStateView` 按需 inflate 状态页、支持配置变更后恢复，并同时作为 `NestedScrollingChild3/Parent3` 转发滚动，避免状态页挡住外层折叠头部和下拉刷新。

**ExposureManager**（`feature:home:impl` 内的可复用工具）：可见面积占比 + 连续停留时长 + key 去重；有待判定条目时走帧回调，空闲时降级为低频轮询，避免长期占用 VSYNC。

### 6. 构建约定

所有模块只应用 `build-logic` 里的插件，根脚本保持空实现，版本集中在 `gradle/libs.versions.toml`。

| 插件 | 作用 |
| --- | --- |
| `carback-android-application` | SDK 版本、Java 17、ViewBinding、namespace 推导 |
| `carback-android-library` | 同上（library 版） |
| `carback-android-hilt` | Hilt 插件 + 运行时 + KSP 编译器 |
| `carback-feature-api` | 契约模块默认依赖（`core:common` + coroutines） |
| `carback-feature-impl` | core 三件套 + 自有 api + 路由 KSP + **跨 feature 依赖守卫** |
| `carback-feature-demo` | application + hilt + `core:debug-scaffold`，开箱即可运行 |
| `carback-kotlin-jvm` | 纯 JVM 模块（KSP 处理器） |

`namespace` 由模块路径自动推导：`:feature:home:impl` → `cn.frank.carback.feature.home.impl`。`carback-feature-impl` 同时把 `router.moduleName` 注入 KSP，`:feature:home:impl` → `HomeImplRouterModule`。

### 7. 对外 SDK

`sdk` 模块不依赖任何内部模块，只暴露 `SdkContract`（Action / Extra / 宿主类名）与门面 `CarBackSdk`。宿主侧的 `SdkEntryActivity`（`exported`）校验路由后交给 `Router` 打开页面。第三方调用前会先 `resolveActivity`，宿主未安装时返回 `false`，便于引导下载。

## 如何使用

### 页面跳转与传参

```kotlin
// 普通入栈
Router.open("home/main")

// 带参数：写入 Fragment.arguments（key 由目标页面约定），页面的 SavedStateHandle 可直接取
Router.open("home/main", Bundle().apply { putString("tabId", "recommend") })

// Tab 切换：Tab 页常驻且不进返回栈，重复点击当前 Tab 回到 Tab 根页面
Router.openTab("search/main")

// 保证栈中同路由只有一个实例
Router.open("mine/main", singleInstance = true)

// 返回
FragmentRouter.pop()
```

页面侧读取参数：

```kotlin
private val tabId: String = requireArguments().getString("tabId").orEmpty()
// 或 ViewModel 中：savedStateHandle["tabId"]
```

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

5. `demo` 中两行壳（`@HiltAndroidApp` 的 Application + 起始路由）：

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
val homeApi = ServiceRegistry.serviceOrNull<HomeApi>()    // 可能未装配

// 事件广播 / 订阅
EventBus.post(TrackPlayedEvent(track))
EventBus.on<TrackPlayedEvent>().collect { event -> /* ... */ }
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

`PlayerRowPresenter` 实现 `ItemPresenter<PlayerRow, ItemPlayerBinding>` 即可。刷新、加载更多、页码累加、Loading/Empty/Error 切换都由基类完成。

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

```kotlin
// 第三方 APK：依赖 sdk AAR 后拉起宿主页面
CarBackSdk.openPage(context, "search/main")
CarBackSdk.openHostMarket(context)   // 宿主未安装时引导下载
```

同进程集成（与宿主同渠道分发）可直接依赖 `core:router` + 目标 feature 的 `impl`，在 `Application.onCreate` 中调用 `Bootstrap.install(this)` 后使用 `Router` / `ServiceRegistry` / `EventBus`。

## 约定

- Fragment 必须提供无参构造，参数通过 `arguments` / `SavedStateHandle` 获取。
- 跨 feature 只能依赖对方 `api`；禁止 `implementation(project(":feature:xxx:impl"))`（构建守卫会拦截）。
- 不直接 `new` 其他 feature 的页面，统一走 `Router`。
- 路由 path 全局唯一，命名统一为 `feature/page`。
- 新增模块一律使用 `build-logic` 插件，不在模块内重复写 SDK 版本、Java 版本、namespace。
- `Bootstrap.install(this)` 必须在 `Application.onCreate()` 中调用，路由表与服务表依赖它初始化。

## 已知限制

- 业务数据全部为内存 mock（曲目、搜索源、收听历史），替换为真实数据源即可。
- `demo` 壳只装配单个 feature，跨 feature 能力会走降级分支，属预期行为。
- `PageStateView` 的可见性判定不覆盖"被兄弟 View 完全遮挡"的极端场景。
- `build-logic/build.gradle.kts` 中 AGP / Kotlin / KSP 版本需与 `gradle/libs.versions.toml` 手动保持一致。
