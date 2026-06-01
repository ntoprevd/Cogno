package com.ntoprevd.cogno.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ntoprevd.cogno.data.db.dao.MessageDao
import com.ntoprevd.cogno.data.db.dao.NoteDao
import com.ntoprevd.cogno.data.db.dao.SessionDao
import com.ntoprevd.cogno.data.db.entity.MessageEntity
import com.ntoprevd.cogno.data.db.entity.NoteEntity
import com.ntoprevd.cogno.data.db.entity.SessionEntity

@Database(
    entities = [
        SessionEntity::class,
        MessageEntity::class,
        NoteEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao

    abstract fun messageDao(): MessageDao

    abstract fun noteDao(): NoteDao

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
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 记录用户对 assistant 回复的反馈；历史消息默认没有反馈。
                db.execSQL("ALTER TABLE messages ADD COLUMN feedback TEXT")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 基础笔记库：由会话生成 Markdown 笔记并保存到本地。
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS notes (" +
                        "id TEXT NOT NULL PRIMARY KEY, " +
                        "title TEXT NOT NULL, " +
                        "content TEXT NOT NULL, " +
                        "preview TEXT NOT NULL, " +
                        "source_session_id TEXT, " +
                        "source_message_count INTEGER NOT NULL, " +
                        "pinned INTEGER NOT NULL, " +
                        "created_at INTEGER NOT NULL, " +
                        "updated_at INTEGER NOT NULL" +
                        ")"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notes_source_session_id ON notes(source_session_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notes_pinned_updated_at ON notes(pinned, updated_at)")
            }
        }
    }
}
