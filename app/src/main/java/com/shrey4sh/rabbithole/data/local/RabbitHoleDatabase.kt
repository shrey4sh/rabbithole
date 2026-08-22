package com.shrey4sh.rabbithole.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Database(entities = [RabbitHoleEntity::class, SavedItemEntity::class], version = 1, exportSchema = false)
abstract class RabbitHoleDatabase : RoomDatabase() {
    abstract fun rabbitHoleDao(): RabbitHoleDao
    abstract fun savedDao(): SavedDao
}
