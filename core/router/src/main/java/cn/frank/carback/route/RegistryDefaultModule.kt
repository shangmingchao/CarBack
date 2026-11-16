package cn.frank.carback.route

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet

/**
 * 路由表的空默认集合。
 *
 * 使用 @ElementsIntoSet 提供空集，可与各 feature 的 @IntoSet 贡献合并；
 * 当宿主没有任何路由贡献者时，保证 Hilt 图仍可构造。
 *
 * @author shangmingchao
 */
@Module
@InstallIn(SingletonComponent::class)
object RegistryDefaultModule {

    @Provides
    @ElementsIntoSet
    fun provideDefaultRouteEntries(): Set<RouteEntry> = emptySet()
}
