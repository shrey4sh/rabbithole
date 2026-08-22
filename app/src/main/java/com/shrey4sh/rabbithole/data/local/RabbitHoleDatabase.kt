package com.shrey4sh.rabbithole.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [RabbitHoleEntity::class, SavedItemEntity::class], version = 1)
abstract class RabbitHoleDatabase : RoomDatabase() {
    abstract fun rabbitHoleDao(): RabbitHoleDao
    abstract fun savedDao(): SavedDao

    companion object {
        @Volatile private var instance: RabbitHoleDatabase? = null

        fun get(context: Context): RabbitHoleDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    RabbitHoleDatabase::class.java,
                    "rabbithole.db"
                ).build().also { instance = it }
            }
    }
}
