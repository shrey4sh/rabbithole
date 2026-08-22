package com.shrey4sh.rabbithole.di

import com.shrey4sh.rabbithole.BuildConfig
import com.shrey4sh.rabbithole.data.remote.AiRanker
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

    /** AI key from BuildConfig (CI secret); empty key = heuristic fallback ranking. */
    @Provides @Singleton
    fun provideAiRanker(): AiRanker = AiRanker(apiKey = BuildConfig.OPENROUTER_API_KEY)

    @Provides @Singleton
    fun provideTopicRepository(impl: WikipediaTopicRepository): TopicRepository = impl
}
