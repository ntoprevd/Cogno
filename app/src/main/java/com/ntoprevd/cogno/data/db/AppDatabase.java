package com.ntoprevd.cogno.data.db;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import com.ntoprevd.cogno.data.db.dao.MessageDao;
import com.ntoprevd.cogno.data.db.dao.SessionDao;
import com.ntoprevd.cogno.data.db.entity.MessageEntity;
import com.ntoprevd.cogno.data.db.entity.SessionEntity;

@Database(
        entities = {
                SessionEntity.class,
                MessageEntity.class
        },
        version = 1,
        exportSchema = false
)
@TypeConverters(Converters.class)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "cogno.db";

    private static volatile AppDatabase instance;

    public abstract SessionDao sessionDao();

    public abstract MessageDao messageDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME
                    ).build();
                }
            }
        }
        return instance;
    }
}
