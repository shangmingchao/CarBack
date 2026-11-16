package cn.frank.carback.feature.search.impl.di

import cn.frank.carback.feature.search.api.SearchApi
import cn.frank.carback.feature.search.impl.SearchApiImpl
import cn.frank.carback.service.FeatureApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * :feature:search:impl 的 Hilt 装配模块。
 *
 * 路由装配由 KSP 生成的 SearchImplRouterModule 完成（零反射）。
 *
 * @author shangmingchao
 */
@Module
@InstallIn(SingletonComponent::class)
object SearchImplModule {

    @Provides
    fun provideSearchApi(impl: SearchApiImpl): SearchApi = impl

    @Provides
    @IntoSet
    fun provideSearchApiService(impl: SearchApiImpl): FeatureApi = impl
}
