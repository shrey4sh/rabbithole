package com.shrey4sh.rabbithole.di

import com.shrey4sh.rabbithole.data.repository.MockTopicRepository
import com.shrey4sh.rabbithole.domain.repository.TopicRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton
    abstract fun bindTopicRepository(impl: MockTopicRepository): TopicRepository
}
