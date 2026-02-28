package com.developer.harshul.pinvoke;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;
import java.util.List;

public class AlarmScheduler {

    private static final String TAG = "AlarmScheduler";
    public static final String EXTRA_CARD_ID = "card_id";
    public static final String EXTRA_ALARM_ID = "alarm_id";

    public static void scheduleAlarms(Context context, Card card) {
        if (!card.isAlarmEnabled() || card.isPaid()) {
            return;
        }

        GlobalAlarmRepository repo = new GlobalAlarmRepository(context);
        List<GlobalAlarmConfig> alarms = repo.getAlarms();

        for (GlobalAlarmConfig alarm : alarms) {
            if (alarm.isEnabled()) {
                scheduleSingleAlarm(context, card, alarm);
            }
        }
    }

    @SuppressLint("MissingPermission")
    private static void scheduleSingleAlarm(Context context, Card card, GlobalAlarmConfig alarmConfig) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        long triggerTime = calculateNextTriggerTime(card.getDueDate(), alarmConfig.getHourOfDay(), alarmConfig.getMinute());

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra(EXTRA_CARD_ID, card.getId());
        intent.putExtra(EXTRA_ALARM_ID, alarmConfig.getId());

        int requestCode = getRequestCode(card.getId(), alarmConfig.getId());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Using setAlarmClock for maximum reliability (bypasses Doze, shows icon)
        Intent uiIntent = new Intent(context, MainActivity.class);
        PendingIntent piUi = PendingIntent.getActivity(context, 0, uiIntent, PendingIntent.FLAG_IMMUTABLE);

        AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(triggerTime, piUi);
        alarmManager.setAlarmClock(info, pendingIntent);

        Log.d(TAG, "Scheduled alarm ID " + alarmConfig.getId() + " for " + card.getName() + " at " + triggerTime);
    }

    private static long calculateNextTriggerTime(long dueDateMillis, int targetHour, int targetMinute) {
        Calendar now = Calendar.getInstance();

        Calendar due = Calendar.getInstance();
        due.setTimeInMillis(dueDateMillis);

        // Let's create a target time for TODAY
        Calendar targetToday = Calendar.getInstance();
        targetToday.set(Calendar.HOUR_OF_DAY, targetHour);
        targetToday.set(Calendar.MINUTE, targetMinute);
        targetToday.set(Calendar.SECOND, 0);
        targetToday.set(Calendar.MILLISECOND, 0);

        // If today is strictly BEFORE the due date's calendar day, the next valid cycle starts on the due date.
        // A simple way to check if today is before due date is to compare start of days.
        Calendar startOfToday = Calendar.getInstance();
        startOfToday.set(Calendar.HOUR_OF_DAY, 0);
        startOfToday.set(Calendar.MINUTE, 0);
        startOfToday.set(Calendar.SECOND, 0);
        startOfToday.set(Calendar.MILLISECOND, 0);

        Calendar startOfDue = Calendar.getInstance();
        startOfDue.setTimeInMillis(dueDateMillis);
        startOfDue.set(Calendar.HOUR_OF_DAY, 0);
        startOfDue.set(Calendar.MINUTE, 0);
        startOfDue.set(Calendar.SECOND, 0);
        startOfDue.set(Calendar.MILLISECOND, 0);

        if (startOfToday.getTimeInMillis() < startOfDue.getTimeInMillis()) {
            // We are before the due date. The next occurrence is on the due date.
            Calendar targetDue = Calendar.getInstance();
            targetDue.setTimeInMillis(dueDateMillis);
            targetDue.set(Calendar.HOUR_OF_DAY, targetHour);
            targetDue.set(Calendar.MINUTE, targetMinute);
            targetDue.set(Calendar.SECOND, 0);
            targetDue.set(Calendar.MILLISECOND, 0);
            return targetDue.getTimeInMillis();
        } else {
            // We are on or past the due date.
            if (targetToday.getTimeInMillis() > now.getTimeInMillis()) {
                // The time is later today
                return targetToday.getTimeInMillis();
            } else {
                // The time has passed today. Schedule for tomorrow.
                targetToday.add(Calendar.DAY_OF_YEAR, 1);
                return targetToday.getTimeInMillis();
            }
        }
    }

    public static void cancelAlarms(Context context, Card card) {
        cancelAlarm(context, card, 1);
        cancelAlarm(context, card, 2);
        cancelAlarm(context, card, 3);
    }

    private static void cancelAlarm(Context context, Card card, int alarmId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, AlarmReceiver.class);

        int requestCode = getRequestCode(card.getId(), alarmId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_NO_CREATE);

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
            Log.d(TAG, "Cancelled alarm request code: " + requestCode);
        }
    }

    private static int getRequestCode(String cardId, int alarmId) {
        return cardId.hashCode() + alarmId;
    }
}
