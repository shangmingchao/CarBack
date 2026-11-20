package cn.frank.carback.service

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * 跨 feature 服务注册中心（数据/能力解耦）
 *
 * 各 feature impl 将自己的 Api 实现通过 Hilt @IntoSet 多绑定注册，
 * 本类在宿主启动时聚合，消费方按接口解析：
 *
 * ```
 * val mineApi = ServiceRegistry.service<MineApi>()          // 必须存在，否则快速失败
 * val homeApi = ServiceRegistry.serviceOrNull<HomeApi>()    // 可能不存在，优雅降级
 * ```
 *
 * 服务重复注册（两个实现实现同一契约接口）在 [init] 时快速失败。
 *
 * @author shangmingchao
 */
object ServiceRegistry {

    private var services: Set<FeatureApi> = emptySet()

    /**
     * 初始化：收集所有 FeatureApi（Hilt 多绑定），并做重复注册快速失败检查。
     * 应在 HiltAndroidApp 的 Application#onCreate 中调用。
     */
    fun init(context: Context) {
        if (services.isNotEmpty()) {
            return
        }
        val entry = EntryPointAccessors.fromApplication(
            context.applicationContext,
            ServiceRegistryEntryPoint::class.java,
        )
        services = entry.featureApis()
        checkDuplicateRegistrations()
    }

    /**
     * 查找指定契约的服务，不存在（feature 未装配）时返回 null
     */
    fun <T : FeatureApi> find(clazz: Class<T>): T? = services.filterIsInstance(clazz).firstOrNull()

    /**
     * 查找指定契约的服务，不存在时抛异常
     */
    fun <T : FeatureApi> require(clazz: Class<T>): T =
        find(clazz) ?: error("FeatureApi not found: ${clazz.name}，请确认对应 feature impl 已编入 classpath")

    /**
     * 按契约解析服务（必须存在，否则快速失败）
     */
    inline fun <reified T : FeatureApi> service(): T = require(T::class.java)

    /**
     * 按契约解析服务（可能不存在，优雅降级）
     */
    inline fun <reified T : FeatureApi> serviceOrNull(): T? = find(T::class.java)

    /**
     * 服务重复注册检查：同一契约接口被两个实例实现时快速失败
     */
    private fun checkDuplicateRegistrations() {
        val byContract = mutableMapOf<Class<*>, MutableSet<String>>()
        services.forEach { service ->
            collectContracts(service.javaClass).forEach { contract ->
                val owners = byContract.getOrPut(contract) { mutableSetOf() }
                owners.add(service.javaClass.name)
            }
        }
        val duplicates = byContract.filterValues { it.size > 1 }
        require(duplicates.isEmpty()) {
            "服务重复注册：${duplicates.map { (contract, owners) -> "${contract.name} <- $owners" }}"
        }
    }

    private fun collectContracts(clazz: Class<*>): Set<Class<*>> {
        val result = mutableSetOf<Class<*>>()
        val queue = ArrayDeque<Class<*>>()
        queue.add(clazz)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            current.interfaces.forEach { iface ->
                if (iface != FeatureApi::class.java && FeatureApi::class.java.isAssignableFrom(iface)) {
                    result.add(iface)
                }
                queue.add(iface)
            }
            current.superclass?.let { queue.add(it) }
        }
        return result
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ServiceRegistryEntryPoint {
        fun featureApis(): Set<@JvmSuppressWildcards FeatureApi>
    }
}
