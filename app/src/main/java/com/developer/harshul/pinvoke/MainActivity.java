package com.developer.harshul.pinvoke;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;

import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity {

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
        if (!isGranted) {
            Toast.makeText(this, "Notifications are required for alarms!", Toast.LENGTH_SHORT).show();
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermissions();

        MaterialButton addWidgetButton = findViewById(R.id.add_widget_button);
        MaterialButton alarmSettingsButton = findViewById(R.id.alarm_settings_button);

        addWidgetButton.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                AppWidgetManager appWidgetManager = getSystemService(AppWidgetManager.class);
                ComponentName myProvider = new ComponentName(this, CreditCardWidgetProvider.class);

                if (appWidgetManager.isRequestPinAppWidgetSupported()) {
                    appWidgetManager.requestPinAppWidget(myProvider, null, null);
                } else {
                    Toast.makeText(this, "Pinning widgets is not supported on this launcher.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Long press on your home screen to add the widget.", Toast.LENGTH_LONG).show();
            }
        });

        alarmSettingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, GlobalAlarmSettingsActivity.class);
            startActivity(intent);
        });

        MaterialButton manageCardsButton = findViewById(R.id.manage_cards_button);
        manageCardsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreditCardWidgetConfigActivity.class);
            intent.putExtra(CreditCardWidgetConfigActivity.EXTRA_VIEW_ALL_MODE, true);
            startActivity(intent);
        });
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            String permission = android.Manifest.permission.POST_NOTIFICATIONS;
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(permission);
            }
        }
    }
}