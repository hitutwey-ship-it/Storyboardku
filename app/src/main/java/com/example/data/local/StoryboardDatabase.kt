package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ProjectEntity::class, CharacterEntity::class, SceneEntity::class],
    version = 2,
    exportSchema = false
)
abstract class StoryboardDatabase : RoomDatabase() {
    abstract fun storyboardDao(): StoryboardDao

    companion object {
        @Volatile
        private var INSTANCE: StoryboardDatabase? = null

        fun getInstance(context: Context): StoryboardDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    StoryboardDatabase::class.java,
                    "storyboardku.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
