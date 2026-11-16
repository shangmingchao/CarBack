package cn.frank.carback.feature.mine.impl.di

import cn.frank.carback.feature.mine.api.MineApi
import cn.frank.carback.feature.mine.impl.service.MineApiImpl
import cn.frank.carback.service.FeatureApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

/**
 * :feature:mine:impl 的 Hilt 装配模块。
 *
 * 路由装配由 KSP 生成的 MineImplRouterModule 完成（零反射）。
 *
 * @author shangmingchao
 */
@Module
@InstallIn(SingletonComponent::class)
object MineImplModule {

    @Provides
    @Singleton
    fun provideMineApi(impl: MineApiImpl): MineApi = impl

    @Provides
    @IntoSet
    fun provideMineApiService(impl: MineApiImpl): FeatureApi = impl
}
