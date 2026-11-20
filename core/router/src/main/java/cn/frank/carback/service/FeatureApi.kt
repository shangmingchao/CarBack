package cn.frank.carback.service

/**
 * 跨 feature 能力契约的公共标记接口。
 *
 * 约定：
 * - 所有跨 feature 服务接口（如 HomeApi / MineApi）定义在对应 feature 的 :api 模块中；
 * - impl 提供实现并通过 Hilt @IntoSet 多绑定注册；
 * - 消费方通过 [ServiceRegistry] 按接口解析，编译期只认契约；
 * - 服务可能不存在（宿主没装配该 feature），用 [ServiceRegistry.serviceOrNull] 优雅降级。
 *
 * @author shangmingchao
 */
interface FeatureApi
