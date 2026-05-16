package com.ntoprevd.cogno.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ntoprevd.cogno.data.db.dao.MessageDao
import com.ntoprevd.cogno.data.db.dao.SessionDao
import com.ntoprevd.cogno.data.db.entity.MessageEntity
import com.ntoprevd.cogno.data.db.entity.SessionEntity

@Database(
    entities = [
        SessionEntity::class,
        MessageEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao

    abstract fun messageDao(): MessageDao

    companion object {
        private const val DATABASE_NAME = "cogno.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                ).build().also { instance = it }
            }
        }
    }
}
