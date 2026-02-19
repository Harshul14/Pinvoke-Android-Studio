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
        
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start service, trying activity directly", e);
            // Fallback: Try starting activity directly (might work if app is in foreground or exempted)
            Intent activityIntent = new Intent(context, AlarmActivity.class);
            activityIntent.putExtra(AlarmActivity.EXTRA_CARD_ID, card.getId());
            activityIntent.putExtra(AlarmActivity.EXTRA_CARD_NAME, card.getName());
            activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(activityIntent);
        }
    }
}
