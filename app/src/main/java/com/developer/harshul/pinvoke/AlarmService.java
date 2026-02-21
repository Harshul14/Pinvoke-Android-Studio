package com.developer.harshul.pinvoke;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.io.IOException;

public class AlarmService extends Service {
    private static final String TAG = "AlarmService";
    private static final String CHANNEL_ID = "bill_due_alarm_channel";
    
    public static final String EXTRA_CARD_ID = "card_id";
    public static final String EXTRA_CARD_NAME = "card_name";
    public static final String EXTRA_RINGTONE_URI = "ringtone_uri";

    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        String cardId = intent.getStringExtra(EXTRA_CARD_ID);
        String cardName = intent.getStringExtra(EXTRA_CARD_NAME);
        String ringtoneUriString = intent.getStringExtra(EXTRA_RINGTONE_URI);

        startForeground(1, buildNotification(cardId, cardName));
        startAlarm(ringtoneUriString);
        
        // Explicitly removed starting the activity to ensure it just rings as a notification

        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Bill Due Alarms",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Alarms for credit card bill due dates");
            channel.setSound(null, null); // We play sound manually
            channel.enableVibration(true);
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification(String cardId, String cardName) {
        Intent fullScreenIntent = new Intent(this, AlarmActivity.class);
        fullScreenIntent.putExtra(AlarmActivity.EXTRA_CARD_ID, cardId);
        fullScreenIntent.putExtra(AlarmActivity.EXTRA_CARD_NAME, cardName);
        fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                this, 
                0, 
                fullScreenIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher) // Ensure this exists
                .setContentTitle("Bill Due")
                .setContentText(getString(R.string.bill_due_message, cardName != null ? cardName : "Credit Card"))
                .setPriority(NotificationCompat.PRIORITY_MAX) // MAX priority for heads-up
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Show on lock screen
                .setContentIntent(fullScreenPendingIntent)
                .setAutoCancel(false)
                .setOngoing(true);
                
        return builder.build();
    }

    private void startAlarm(String customRingtoneUri) {
        Uri alarmUri = resolveValidAlarmUri(customRingtoneUri);
        initAndPlayMediaPlayer(alarmUri);
        startVibrator();
    }

    private Uri resolveValidAlarmUri(String customRingtoneUri) {
        Uri alarmUri = null;
        if (customRingtoneUri != null) {
            alarmUri = Uri.parse(customRingtoneUri);
        }
        
        if (alarmUri == null) {
            alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        }
        if (alarmUri == null) {
            alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        return alarmUri;
    }

    private void initAndPlayMediaPlayer(Uri alarmUri) {
        try {
            mediaPlayer = new MediaPlayer();
            try {
                mediaPlayer.setDataSource(this, alarmUri);
            } catch (Exception e) {
                // Fallback if custom uri fails (e.g., file deleted)
                Log.e(TAG, "Failed to set custom ringtone, falling back to default", e);
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                mediaPlayer.reset();
                mediaPlayer.setDataSource(this, alarmUri);
            }
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            Log.e(TAG, "Error starting alarm sound", e);
        }
    }

    private void startVibrator() {
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = {0, 1000, 1000};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
            } else {
                vibrator.vibrate(pattern, 0);
            }
        }
    }

    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
        }
        if (vibrator != null) {
            vibrator.cancel();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
