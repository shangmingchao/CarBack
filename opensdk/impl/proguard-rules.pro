# :opensdk:impl library R8 规则（SDK AAR 自身打包用，见 opensdk/README.md 第 5.3 节）
#
# 原则：只保留 :opensdk:api 对外契约与反射桥接，内部实现（core / feature 透传进来的
# 转发类、Provider/Ui 实现细节）一律允许混淆。不使用宽泛的 -keep，避免把内部结构
# 暴露在 AAR 中。

# 1. 对外契约：门面 / 模型 / 接口全部保留（第三方运行期可见）
-keep class cn.frank.carback.opensdk.api.** { *; }

# 2. 反射桥接：由 :opensdk:api 的 Sdk.init 经 Class.forName 加载，类名为运行期字符串
-keep class cn.frank.carback.opensdk.impl.SdkBridgeImpl { *; }

# 3. JDK 字符串拼接 invokedynamic（AGP 自动生成的抑制规则）
-dontwarn java.lang.invoke.StringConcatFactory
