package com.shrey4sh.rabbithole.di

import android.content.Context
import com.shrey4sh.rabbithole.data.local.RabbitHoleDao
import com.shrey4sh.rabbithole.data.local.RabbitHoleDatabase
import com.shrey4sh.rabbithole.data.local.SavedDao
import com.shrey4sh.rabbithole.data.local.buildDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): RabbitHoleDatabase =
        buildDatabase(ctx)

    @Provides fun provideHoleDao(db: RabbitHoleDatabase): RabbitHoleDao = db.rabbitHoleDao()

    @Provides fun provideSavedDao(db: RabbitHoleDatabase): SavedDao = db.savedDao()
}
