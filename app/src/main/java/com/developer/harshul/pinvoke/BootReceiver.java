package com.developer.harshul.pinvoke;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.List;

public class BootReceiver extends BroadcastReceiver {
    
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Boot completed, rescheduling alarms...");
            
            CardRepository repository = new CardRepository(context);
            List<Card> allCards = repository.getAllCards();
            
            for (Card card : allCards) {
                if (card.isAlarmEnabled() && !card.isPaid()) {
                    AlarmScheduler.scheduleAlarms(context, card);
                }
            }
        }
    }
}
