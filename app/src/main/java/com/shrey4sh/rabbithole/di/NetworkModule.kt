package com.shrey4sh.rabbithole.di

import com.shrey4sh.rabbithole.data.remote.WikipediaApi
import com.shrey4sh.rabbithole.data.repository.WikipediaTopicRepository
import com.shrey4sh.rabbithole.domain.repository.TopicRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun provideWikipediaApi(): WikipediaApi = WikipediaApi()

    @Provides @Singleton
    fun bindTopicRepository(impl: WikipediaTopicRepository): TopicRepository = impl
}
