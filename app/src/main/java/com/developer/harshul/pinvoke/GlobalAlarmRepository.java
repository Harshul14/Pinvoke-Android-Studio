package com.developer.harshul.pinvoke;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.developer.harshul.pinvoke.data.local.AlarmConfigDao;
import com.developer.harshul.pinvoke.data.local.AppDatabase;
import com.developer.harshul.pinvoke.AppExecutors;

import java.util.ArrayList;
import java.util.List;

public class GlobalAlarmRepository {

    private final AlarmConfigDao alarmConfigDao;

    public GlobalAlarmRepository(Context context) {
        AppDatabase db = AppDatabase.getDatabase(context.getApplicationContext());
        this.alarmConfigDao = db.alarmConfigDao();
    }

    public LiveData<List<GlobalAlarmConfig>> getAlarmsLiveData() {
        return alarmConfigDao.getAllAlarmsLiveData();
    }

    public List<GlobalAlarmConfig> getAlarms() {
        List<GlobalAlarmConfig> alarms = alarmConfigDao.getAllAlarms();

        // Initialize default 3 alarms if none exist
        if (alarms.isEmpty()) {
            alarms.add(new GlobalAlarmConfig(1, 9, 0, true, null)); // 9:00 AM
            alarms.add(new GlobalAlarmConfig(2, 13, 0, true, null)); // 1:00 PM
            alarms.add(new GlobalAlarmConfig(3, 18, 0, true, null)); // 6:00 PM
            AppExecutors.getInstance().diskIO().execute(() -> alarmConfigDao.insertAll(alarms));
        }

        return alarms;
    }

    public void saveAlarms(List<GlobalAlarmConfig> alarms) {
        AppExecutors.getInstance().diskIO().execute(() -> alarmConfigDao.insertAll(alarms));
    }
}
