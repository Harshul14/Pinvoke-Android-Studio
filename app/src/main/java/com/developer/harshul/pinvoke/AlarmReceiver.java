package com.developer.harshul.pinvoke;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Alarm received");

        String cardId = intent.getStringExtra("card_id");
        int alarmId = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1);
        if (cardId == null) {
            Log.e(TAG, "No card ID provided in alarm intent");
            return;
        }

        CardRepository repository = new CardRepository(context);
        Card card = repository.getCardById(cardId);

        if (card == null) {
            Log.d(TAG, "Card not found (deleted?), ignoring alarm");
            return;
        }

        if (card.isPaid()) {
            Log.d(TAG, "Card is already paid, ignoring alarm");
            return;
        }

        if (!card.isAlarmEnabled()) {
            Log.d(TAG, "Alarm disabled for this card, ignoring");
            return;
        }

        // Start Alarm Service (Foreground)
        Intent serviceIntent = new Intent(context, AlarmService.class);
        serviceIntent.putExtra(AlarmService.EXTRA_CARD_ID, card.getId());
        serviceIntent.putExtra(AlarmService.EXTRA_CARD_NAME, card.getName());

        if (alarmId != -1) {
            GlobalAlarmRepository globalRepo = new GlobalAlarmRepository(context);
            for (GlobalAlarmConfig config : globalRepo.getAlarms()) {
                if (config.getId() == alarmId && config.getRingtoneUri() != null) {
                    serviceIntent.putExtra(AlarmService.EXTRA_RINGTONE_URI, config.getRingtoneUri());
                    break;
                }
            }
        }

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start service", e);
        }
    }
}
