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
    fun provideLocalStorage(@ApplicationContext ctx: Context): LocalStorageRepository =
        LocalStorageRepository(ctx)
}
