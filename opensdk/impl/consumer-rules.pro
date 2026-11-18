# 保留第三方需要直接引用的 SDK 门面与模型（对外契约只由 :opensdk:api 决定）
-keep class cn.frank.carback.opensdk.api.** { *; }

# 反射加载的桥接实现
-keep class cn.frank.carback.opensdk.impl.SdkBridgeImpl { *; }
