package com.developer.harshul.pinvoke;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.lifecycle.ViewModelProvider;

public class GlobalAlarmSettingsActivity extends AppCompatActivity {

    private AlarmViewModel viewModel;
    private GlobalAlarmRepository repository; // Kept for rescheduleAllAlarms usages
    private List<GlobalAlarmConfig> alarms = new ArrayList<>();
    private LinearLayout alarmsContainer;

    private int currentEditingAlarmIndex = -1;

    private final ActivityResultLauncher<Intent> ringtonePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            Uri uri = result.getData().getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (currentEditingAlarmIndex != -1 && currentEditingAlarmIndex < alarms.size()) {
                String uriString = uri != null ? uri.toString() : null;
                alarms.get(currentEditingAlarmIndex).setRingtoneUri(uriString);
                viewModel.saveAlarms(alarms);

                // Reschedule alarms since config changed
                rescheduleAllAlarms();
            }
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_global_alarm_settings);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        alarmsContainer = findViewById(R.id.alarms_container);
        repository = new GlobalAlarmRepository(this);
        viewModel = new ViewModelProvider(this).get(AlarmViewModel.class);

        viewModel.getAlarmsLiveData().observe(this, globalAlarmConfigs -> {
            if (globalAlarmConfigs != null && !globalAlarmConfigs.isEmpty()) {
                this.alarms = new ArrayList<>(globalAlarmConfigs);
                refreshAlarmsUI();
            }
        });
    }

    private void refreshAlarmsUI() {
        // Remove existing alarm items but keep the header text
        int childCount = alarmsContainer.getChildCount();
        if (childCount > 1) {
            alarmsContainer.removeViews(1, childCount - 1);
        }

        for (int i = 0; i < alarms.size(); i++) {
            GlobalAlarmConfig alarm = alarms.get(i);
            int index = i;

            View alarmView = LayoutInflater.from(this).inflate(R.layout.item_global_alarm, alarmsContainer, false);
            TextView titleText = alarmView.findViewById(R.id.alarm_title);
            MaterialSwitch alarmSwitch = alarmView.findViewById(R.id.alarm_switch);
            MaterialButton timeButton = alarmView.findViewById(R.id.time_button);
            MaterialButton ringtoneButton = alarmView.findViewById(R.id.ringtone_button);

            titleText.setText("Reminder " + alarm.getId());
            alarmSwitch.setChecked(alarm.isEnabled());

            // Format time
            String timeStr = formatTime(alarm.getHourOfDay(), alarm.getMinute());
            timeButton.setText(timeStr);

            // Fetch ringtone name
            String ringtoneName = "Default Ringtone";
            if (alarm.getRingtoneUri() != null) {
                try {
                    Uri uri = Uri.parse(alarm.getRingtoneUri());
                    Ringtone ringtone = RingtoneManager.getRingtone(this, uri);
                    if (ringtone != null) {
                        ringtoneName = ringtone.getTitle(this);
                    } else {
                        ringtoneName = "Unknown Ringtone";
                    }
                } catch (Exception e) {
                    ringtoneName = "Unknown Ringtone";
                }
            }
            ringtoneButton.setText(ringtoneName);

            // Listeners
            alarmSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                alarm.setEnabled(isChecked);
                viewModel.saveAlarms(alarms);
                rescheduleAllAlarms();
            });

            timeButton.setOnClickListener(v -> showTimePicker(index, alarm));

            ringtoneButton.setOnClickListener(v -> showRingtonePicker(index, alarm));

            alarmsContainer.addView(alarmView);
        }
    }

    private void showTimePicker(int index, GlobalAlarmConfig alarm) {
        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            // check duplicates
            for (int i = 0; i < alarms.size(); i++) {
                if (i != index && alarms.get(i).getHourOfDay() == hourOfDay && alarms.get(i).getMinute() == minute) {
                    Toast.makeText(this, "This time is already set for another reminder.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            alarm.setHourOfDay(hourOfDay);
            alarm.setMinute(minute);
            viewModel.saveAlarms(alarms);
            rescheduleAllAlarms();
        }, alarm.getHourOfDay(), alarm.getMinute(), false);
        timePickerDialog.show();
    }

    private void showRingtonePicker(int index, GlobalAlarmConfig alarm) {
        currentEditingAlarmIndex = index;
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);

        Uri existingUri = alarm.getRingtoneUri() != null ? Uri.parse(alarm.getRingtoneUri()) : RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existingUri);

        ringtonePickerLauncher.launch(intent);
    }

    private String formatTime(int hourOfDay, int minute) {
        int hour = hourOfDay == 0 ? 12 : (hourOfDay > 12 ? hourOfDay - 12 : hourOfDay);
        String amPm = hourOfDay >= 12 ? "PM" : "AM";
        return String.format(Locale.getDefault(), "%02d:%02d %s", hour, minute, amPm);
    }

    private void rescheduleAllAlarms() {
        // Reschedule all cards' alarms according to new global times
        CardRepository cardRepo = new CardRepository(this);
        List<Card> allCards = cardRepo.getAllCards();
        for (Card card : allCards) {
            AlarmScheduler.cancelAlarms(this, card);
            if (card.isAlarmEnabled() && !card.isPaid()) {
                AlarmScheduler.scheduleAlarms(this, card);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
