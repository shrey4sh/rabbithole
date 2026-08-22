package com.shrey4sh.rabbithole.di

import android.content.Context
import com.shrey4sh.rabbithole.data.repository.LocalStorageRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {
    @Provides @Singleton
    fun provideLocalStorage(
        holeDao: com.shrey4sh.rabbithole.data.local.RabbitHoleDao,
        savedDao: com.shrey4sh.rabbithole.data.local.SavedDao,
    ): LocalStorageRepository = LocalStorageRepository(holeDao, savedDao)

    @Provides @Singleton
    fun provideContext(@ApplicationContext ctx: Context): Context = ctx
}
