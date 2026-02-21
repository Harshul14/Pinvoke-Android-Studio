package com.developer.harshul.pinvoke.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.developer.harshul.pinvoke.GlobalAlarmConfig;

import java.util.List;

@Dao
public interface AlarmConfigDao {
    @Query("SELECT * FROM global_alarms ORDER BY id ASC")
    List<GlobalAlarmConfig> getAllAlarms();

    @Query("SELECT * FROM global_alarms ORDER BY id ASC")
    LiveData<List<GlobalAlarmConfig>> getAllAlarmsLiveData();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<GlobalAlarmConfig> alarms);

    @Update
    void update(GlobalAlarmConfig alarm);
}
