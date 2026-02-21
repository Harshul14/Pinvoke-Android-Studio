package com.developer.harshul.pinvoke.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.developer.harshul.pinvoke.GlobalAlarmConfig;
import com.developer.harshul.pinvoke.Card;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Card.class, GlobalAlarmConfig.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract CardDao cardDao();
    public abstract AlarmConfigDao alarmConfigDao();

    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

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
