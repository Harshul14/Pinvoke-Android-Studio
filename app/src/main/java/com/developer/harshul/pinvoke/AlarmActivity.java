package com.developer.harshul.pinvoke;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.TextView;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;


public class AlarmActivity extends AppCompatActivity {

    private static final String TAG = "AlarmActivity";
    public static final String EXTRA_CARD_ID = "card_id";
    public static final String EXTRA_CARD_NAME = "card_name";

    private String cardId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        showOnLockScreen();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_ringing);

        cardId = getIntent().getStringExtra(EXTRA_CARD_ID);
        String cardName = getIntent().getStringExtra(EXTRA_CARD_NAME);

        TextView cardNameText = findViewById(R.id.card_name_text);
        if (cardName != null && !cardName.trim().isEmpty()) {
            cardNameText.setText(getString(R.string.bill_due_message, cardName));
        } else {
            cardNameText.setText(getString(R.string.bill_due_message, "Credit Card"));
        }

        Button btnPaid = findViewById(R.id.btn_paid);
        Button btnWillPay = findViewById(R.id.btn_will_pay);

        btnPaid.setOnClickListener(v -> onPaidClicked());
        btnWillPay.setOnClickListener(v -> onWillPayClicked());

        startPulseAnimation();
    }

    private void startPulseAnimation() {
        View pulseBg = findViewById(R.id.pulse_bg);
        if (pulseBg != null) {
            ScaleAnimation scaleAnimation = new ScaleAnimation(1f, 1.5f, 1f, 1.5f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            scaleAnimation.setDuration(1000);
            scaleAnimation.setRepeatCount(Animation.INFINITE);
            scaleAnimation.setRepeatMode(Animation.REVERSE);
            pulseBg.startAnimation(scaleAnimation);
        }
    }


    private void showOnLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (keyguardManager != null) {
                keyguardManager.requestDismissKeyguard(this, null);
            }
        }

        // For older devices or additional robustness
        // FLAG_FULLSCREEN makes the activity take full screen which helps on lock screen
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);

        Log.d(TAG, "Lock screen flags set");
    }


    private void stopAlarm() {
        Intent serviceIntent = new Intent(this, AlarmService.class);
        stopService(serviceIntent);
    }

    private void onPaidClicked() {
        stopAlarm();
        if (cardId != null) {
            CardRepository repository = new CardRepository(this);
            Card card = repository.getCardById(cardId);
            if (card != null) {
                // Advance due date by 1 month instead of marking as paid
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.setTimeInMillis(card.getDueDate());
                cal.add(java.util.Calendar.MONTH, 1);
                card.setDueDate(cal.getTimeInMillis());

                // Do not mark as paid so the next cycle works properly
                card.setPaid(false);

                repository.updateCard(card);

                // Reschedule for the new due date
                AlarmScheduler.cancelAlarms(this, card);
                if (card.isAlarmEnabled()) {
                    AlarmScheduler.scheduleAlarms(this, card);
                }
                Log.d(TAG, "Card due date advanced by 1 month, alarms rescheduled.");
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask();
        } else {
            finish();
        }
    }

    private void onWillPayClicked() {
        stopAlarm();
        // Do nothing else, so next alarm rings at scheduled time.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask();
        } else {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        // We don't necessarily want to stop the service IF the user just backs out without choosing?
        // But usually back = dismiss.
        // If we want "Snooze" behavior on back, we should handle onBackPressed.
        // For now, let's assume destroying activity stops the ringing.
        stopAlarm();
        super.onDestroy();
    }
}
