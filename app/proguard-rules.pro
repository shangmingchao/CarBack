# CarBack 主壳 R8 规则
#
# Hilt 等 AAR 自带的 consumer-rules 与 AGP 生成的 manifest 组件 keep 规则
# 会自动生效，这里只补路由跳转这类"反射创建"入口。

# 1. 路由 Fragment：@Route 生成 Hilt 多绑定持有 Class 引用，R8 可追踪；
#    但 Router.routeClass(path) 后 newInstance() 走无参构造，保证构造不被移除。
-keepclassmembers class * extends androidx.fragment.app.Fragment {
    public <init>();
}

# 2. 出问题排查时取消注释，保留行号便于还原堆栈
#-keepattributes SourceFile,LineNumberTable
#-renamesourcefileattribute SourceFile
