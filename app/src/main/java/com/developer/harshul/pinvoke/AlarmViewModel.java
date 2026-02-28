package com.developer.harshul.pinvoke; // Keeping in same package for now to minimize import churn, or moving to ui.alarm

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

public class AlarmViewModel extends AndroidViewModel {
    private final GlobalAlarmRepository repository;
    private final LiveData<List<GlobalAlarmConfig>> alarmsLiveData;

    public AlarmViewModel(@NonNull Application application) {
        super(application);
        repository = new GlobalAlarmRepository(application);
        alarmsLiveData = repository.getAlarmsLiveData();

        // Ensure default alarms are populated if empty
        repository.getAlarms();
    }

    public LiveData<List<GlobalAlarmConfig>> getAlarmsLiveData() {
        return alarmsLiveData;
    }

    public void saveAlarms(List<GlobalAlarmConfig> alarms) {
        repository.saveAlarms(alarms);
    }
}
