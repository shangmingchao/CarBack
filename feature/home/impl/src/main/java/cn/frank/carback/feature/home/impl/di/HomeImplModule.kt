package cn.frank.carback.feature.home.impl.di

import cn.frank.carback.feature.home.api.HomeApi
import cn.frank.carback.feature.home.impl.HomeApiImpl
import cn.frank.carback.service.FeatureApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

/**
 * :feature:home:impl 的 Hilt 装配模块。
 *
 * 路由装配由 KSP 生成的 HomeImplRouterModule 完成（零反射）。
 *
 * @author shangmingchao
 */
@Module
@InstallIn(SingletonComponent::class)
object HomeImplModule {

    @Provides
    @Singleton
    fun provideHomeApi(impl: HomeApiImpl): HomeApi = impl

    @Provides
    @IntoSet
    fun provideHomeApiService(impl: HomeApiImpl): FeatureApi = impl
}
