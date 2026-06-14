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
import com.ntoprevd.cogno.data.db.dao.TopicDao
import com.ntoprevd.cogno.data.db.entity.MessageEntity
import com.ntoprevd.cogno.data.db.entity.NoteTopicSegmentEntity
import com.ntoprevd.cogno.data.db.entity.NoteEntity
import com.ntoprevd.cogno.data.db.entity.SessionEntity
import com.ntoprevd.cogno.data.db.entity.TopicEntity

@Database(
    entities = [
        SessionEntity::class,
        MessageEntity::class,
        NoteEntity::class,
        TopicEntity::class,
        NoteTopicSegmentEntity::class
    ],
    version = 6,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao

    abstract fun messageDao(): MessageDao

    abstract fun noteDao(): NoteDao

    abstract fun topicDao(): TopicDao

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
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6
                    )
                    .build()
                    .also { instance = it }
            }
        }

        internal val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 记录用户对 assistant 回复的反馈；历史消息默认没有反馈。
                db.execSQL("ALTER TABLE messages ADD COLUMN feedback TEXT")
            }
        }

        internal val MIGRATION_2_3 = object : Migration(2, 3) {
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

        internal val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 主题只保存分类规则；笔记正文仍是按对话生成的唯一底稿。
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS topics (" +
                        "id TEXT NOT NULL PRIMARY KEY, " +
                        "name TEXT NOT NULL, " +
                        "keywords TEXT NOT NULL, " +
                        "is_builtin INTEGER NOT NULL, " +
                        "enabled INTEGER NOT NULL, " +
                        "pinned INTEGER NOT NULL, " +
                        "created_at INTEGER NOT NULL, " +
                        "updated_at INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS note_topic_segments (" +
                        "id TEXT NOT NULL PRIMARY KEY, " +
                        "note_id TEXT NOT NULL, " +
                        "topic_name TEXT NOT NULL, " +
                        "heading TEXT NOT NULL, " +
                        "content TEXT NOT NULL, " +
                        "position INTEGER NOT NULL, " +
                        "source_message_count INTEGER NOT NULL, " +
                        "created_at INTEGER NOT NULL, " +
                        "FOREIGN KEY(note_id) REFERENCES notes(id) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_note_topic_segments_note_id ON note_topic_segments(note_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_note_topic_segments_topic_name_created_at ON note_topic_segments(topic_name, created_at)")
            }
        }

        internal val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 图片文件保存在应用私有目录，消息表只记录可持久恢复的路径和 MIME 类型。
                db.execSQL("ALTER TABLE messages ADD COLUMN image_path TEXT")
                db.execSQL("ALTER TABLE messages ADD COLUMN image_mime_type TEXT")
            }
        }

        internal val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 旧笔记在下次生成时完整校准；新字段用于识别新增、编辑和删除消息。
                db.execSQL(
                    "ALTER TABLE notes ADD COLUMN source_last_message_created_at " +
                        "INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE notes ADD COLUMN source_message_revision " +
                        "INTEGER NOT NULL DEFAULT 0"
                )
            }
        }
    }
}
