package cn.frank.carback.service

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet

/**
 * 服务表的空默认集合。
 *
 * 使用 @ElementsIntoSet 提供空集，可与各 feature 的 @IntoSet 贡献合并；
 * 当宿主（如只装配单个 feature 的 demo 壳）没有任何服务贡献者时，
 * 保证 Hilt 图仍可构造。
 *
 * @author shangmingchao
 */
@Module
@InstallIn(SingletonComponent::class)
object RegistryDefaultModule {

    @Provides
    @ElementsIntoSet
    fun provideDefaultApis(): Set<FeatureApi> = emptySet()
}
