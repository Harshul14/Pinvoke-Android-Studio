package com.developer.harshul.pinvoke;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
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
        if (cardName != null) {
            cardNameText.setText(getString(R.string.bill_due_message, cardName));
        } else {
            cardNameText.setText("Your credit card bill is due today.");
        }

        Button btnPaid = findViewById(R.id.btn_paid);
        Button btnWillPay = findViewById(R.id.btn_will_pay);

        btnPaid.setOnClickListener(v -> onPaidClicked());
        btnWillPay.setOnClickListener(v -> onWillPayClicked());
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
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_FULLSCREEN |
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
        
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
                card.setPaid(true);
                repository.updateCard(card);
                AlarmScheduler.cancelAlarms(this, card);
                Log.d(TAG, "Card marked as paid, alarms cancelled.");
            }
        }
        finish();
    }

    private void onWillPayClicked() {
        stopAlarm();
        // Do nothing else, so next alarm rings at scheduled time.
        finish();
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
