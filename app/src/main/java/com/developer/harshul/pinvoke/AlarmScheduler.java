package com.developer.harshul.pinvoke;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;

public class AlarmScheduler {

    private static final String TAG = "AlarmScheduler";
    public static final String EXTRA_CARD_ID = "card_id";

    // Request codes: Base is hash of card ID. 
    // Offsets: 0 for 11:00, 1 for 13:45, 2 for 20:00.
    
    public static void scheduleAlarms(Context context, Card card) {
        if (!card.isAlarmEnabled() || card.isPaid()) {
            return;
        }

        long dueDate = card.getDueDate();
        scheduleAlarm(context, card, dueDate, 11, 0, 0); // 11:00 AM
        scheduleAlarm(context, card, dueDate, 14, 37, 1); // 1:45 PM
        scheduleAlarm(context, card, dueDate, 14, 39, 2); // 8:00 PM
    }

    private static void scheduleAlarm(Context context, Card card, long dueDate, int hour, int minute, int requestCodeOffset) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        // Check for exact alarm permission on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "Cannot schedule exact alarms: permission denied");
                // In a real app, we should prompt user to grant permission. 
                // For now, we proceed, as standard behavior might allow basic scheduling or we fallback.
                // But generally we should have asked for permission in UI.
                return; 
            }
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(dueDate);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        long triggerTime = calendar.getTimeInMillis();

        // If time is in the past, don't schedule (or schedule for next month? No, requirement says 'schedule only future alarms')
        if (triggerTime < System.currentTimeMillis()) {
            Log.d(TAG, "Alarm time passed for " + hour + ":" + minute + ", skipping.");
            return;
        }

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra(EXTRA_CARD_ID, card.getId());
        
        int requestCode = getRequestCode(card.getId(), requestCodeOffset);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 
                requestCode, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // API 23+ (Marshmallow) Doze mode handling
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        
        Log.d(TAG, "Scheduled alarm for " + card.getName() + " at " + calendar.getTime());
    }

    public static void cancelAlarms(Context context, Card card) {
        cancelAlarm(context, card, 0);
        cancelAlarm(context, card, 1);
        cancelAlarm(context, card, 2);
    }
    
    public static void cancelRemainingAlarmsForDay(Context context, Card card) {
        // Logic: Cancel all provided. Since we only schedule for "today" (due date), this is effectively same as cancelAlarms.
        // If we supported recurring alarms, we'd need to be careful. 
        // But for now, "cancel remaining alarms for the day" effectively means cancel all pending intents for this card's current cycle.
        cancelAlarms(context, card);
    }

    private static void cancelAlarm(Context context, Card card, int requestCodeOffset) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, AlarmReceiver.class);
        // Extras not strictly needed for matching execution, but intent filter matching requires strict validity.
        // Actually PendingIntent matching is based on Intent.filterEquals() + requestCode. 
        // Extras are NOT considered in filterEquals. 
        // So just same component/action is enough if data/categories match (none here).
        
        int requestCode = getRequestCode(card.getId(), requestCodeOffset);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 
                requestCode, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_NO_CREATE
        );

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
            Log.d(TAG, "Cancelled alarm request code: " + requestCode);
        }
    }
    
    private static int getRequestCode(String cardId, int offset) {
        // Simple hashcode combination. 
        // Potential collision if we have many cards, but unlikely for < 10 cards.
        return cardId.hashCode() + offset;
    }
}
