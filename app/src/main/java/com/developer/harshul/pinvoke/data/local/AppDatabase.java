package com.developer.harshul.pinvoke.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.developer.harshul.pinvoke.GlobalAlarmConfig;
import com.developer.harshul.pinvoke.Card;


@Database(entities = {Card.class, GlobalAlarmConfig.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract CardDao cardDao();
    public abstract AlarmConfigDao alarmConfigDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "ccwidget_database")
                            .allowMainThreadQueries() // Temporarily allowed until UI gets ViewModels
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
