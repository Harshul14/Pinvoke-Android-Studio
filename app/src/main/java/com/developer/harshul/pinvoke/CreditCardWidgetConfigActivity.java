package com.developer.harshul.pinvoke;

import android.app.DatePickerDialog;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.ComponentName;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CreditCardWidgetConfigActivity extends AppCompatActivity {

    private static final String TAG = "WidgetConfig";
    private static final String PREFS_NAME = "CCWidgetPrefs";
    private static final String CARDS_DATA_KEY = "cards_data";
    private static final int MAX_CARDS = 10;
    private static final int MIN_CARDS = 1;
    public static final String EXTRA_VIEW_ALL_MODE = "view_all_mode";
    private boolean isViewAllMode = false;
    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private int[] allWidgetIds = new int[0];
    private LinearLayout cardsContainer;
    private Button addCardButton;
    private Button saveButton;
    private List<CardEntry> cardEntries;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widget_config);

        initializeViews();
        setupWidget();
        loadExistingData();
        setupEventListeners();
        setResult(RESULT_CANCELED);
    }

    private void initializeViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        cardsContainer = findViewById(R.id.cards_container);
        addCardButton = findViewById(R.id.add_card_button);
        saveButton = findViewById(R.id.save_button);
        cardEntries = new ArrayList<>();
    }

    private void setupWidget() {
        Intent intent = getIntent();
        if (intent != null) {
            appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            isViewAllMode = intent.getBooleanExtra(EXTRA_VIEW_ALL_MODE, false);
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID && !isViewAllMode) {
            Log.e(TAG, "Invalid widget ID");
            finish();
        }

        if (isViewAllMode) {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Manage Cards");
            }
            saveButton.setVisibility(View.VISIBLE);
            addCardButton.setVisibility(View.VISIBLE);
        }
    }

    private void setupEventListeners() {
        addCardButton.setOnClickListener(v -> addNewCardEntry());
        saveButton.setOnClickListener(v -> saveConfiguration());
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void loadExistingData() {
        if (isViewAllMode) {
            loadAllCards();
        } else {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME + appWidgetId, Context.MODE_PRIVATE);
            String cardsDataJson = prefs.getString(CARDS_DATA_KEY, "");

            if (!TextUtils.isEmpty(cardsDataJson)) {
                parseAndLoadCards(cardsDataJson, appWidgetId);
            } else {
                addDefaultCard(appWidgetId);
            }
        }
    }

    private void loadAllCards() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        ComponentName thisWidget = new ComponentName(this, CreditCardWidgetProvider.class);
        allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

        if (allWidgetIds.length == 0) {
            showToast("No active widgets found.");
            return;
        }

        for (int id : allWidgetIds) {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME + id, Context.MODE_PRIVATE);
            String cardsDataJson = prefs.getString(CARDS_DATA_KEY, "");
            if (!TextUtils.isEmpty(cardsDataJson)) {
                parseAndLoadCards(cardsDataJson, id);
            } else {
                addDefaultCard(id);
            }
        }
    }

    private void parseAndLoadCards(String cardsDataJson, int widgetId) {
        try {
            JSONArray cardsArray = new JSONArray(cardsDataJson);
            for (int i = 0; i < cardsArray.length(); i++) {
                JSONObject cardObj = cardsArray.optJSONObject(i);
                if (cardObj != null) {
                    String cardName = cardObj.optString("name", "");
                    long dueDate = cardObj.optLong("dueDate", getDefaultDueDate());
                    if (isValidDate(dueDate)) {
                        addCardEntry(cardName, dueDate, widgetId);
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing cards data", e);
            addDefaultCard(widgetId);
        }
    }

    private boolean isValidDate(long dateMillis) {
        long currentTime = System.currentTimeMillis();
        long fiveYears = TimeUnit.DAYS.toMillis(5 * 365);
        return dateMillis >= currentTime - fiveYears && dateMillis <= currentTime + fiveYears;
    }

    private void addDefaultCard(int widgetId) {
        addCardEntry("", getDefaultDueDate(), widgetId);
    }

    private void addNewCardEntry() {
        if (isViewAllMode) {
            if (allWidgetIds.length == 0) {
                showToast("No active widgets to add cards to.");
                return;
            }
            int targetWidgetId = allWidgetIds[0];
            if (getCardCountForWidget(targetWidgetId) >= MAX_CARDS) {
                showToast(getString(R.string.max_cards_allowed, MAX_CARDS) + " for the primary widget.");
                return;
            }
            addCardEntry("", getDefaultDueDate(), targetWidgetId);

        } else {
            if (cardEntries.size() >= MAX_CARDS) {
                showToast(getString(R.string.max_cards_allowed, MAX_CARDS));
                return;
            }
            addCardEntry("", getDefaultDueDate(), appWidgetId);
        }
    }

    private int getCardCountForWidget(int widgetId) {
        int count = 0;
        for (CardEntry entry : cardEntries) {
            if (entry.widgetId == widgetId) {
                count++;
            }
        }
        return count;
    }

    private void addCardEntry(String cardName, long dueDate, int widgetId) {
        View cardView = LayoutInflater.from(this).inflate(R.layout.card_entry_item, cardsContainer, false);
        TextInputEditText cardNameEdit = cardView.findViewById(R.id.card_name_edit);
        Button dueDateButton = cardView.findViewById(R.id.due_date_button);
        ImageButton removeButton = cardView.findViewById(R.id.remove_card_button);

        Calendar selectedDate = Calendar.getInstance();
        selectedDate.setTimeInMillis(dueDate);

        CardEntry cardEntry = new CardEntry(cardView, cardNameEdit, dueDateButton, selectedDate, widgetId);
        cardEntries.add(cardEntry);

        cardNameEdit.setText(cardName);
        updateDateButton(cardEntry);

        dueDateButton.setOnClickListener(v -> {
            showDatePicker(cardEntry);
        });
        removeButton.setOnClickListener(v -> removeCardEntry(cardEntry));

        cardsContainer.addView(cardView);
        updateRemoveButtonsVisibility();
    }

    private void removeCardEntry(CardEntry cardEntry) {
        int countForWidget = getCardCountForWidget(cardEntry.widgetId);
        if (countForWidget <= MIN_CARDS) {
            showToast(getString(R.string.at_least_one_card));
            return;
        }

        cardsContainer.removeView(cardEntry.cardView);
        cardEntries.remove(cardEntry);
        updateRemoveButtonsVisibility();
    }

    private void updateRemoveButtonsVisibility() {
        for (CardEntry entry : cardEntries) {
            ImageButton removeButton = entry.cardView.findViewById(R.id.remove_card_button);
            int count = getCardCountForWidget(entry.widgetId);
            removeButton.setVisibility(count > MIN_CARDS ? View.VISIBLE : View.GONE);
        }
    }

    private void showDatePicker(CardEntry cardEntry) {
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            cardEntry.selectedDate.set(year, month, dayOfMonth);
            updateDateButton(cardEntry);
        }, cardEntry.selectedDate.get(Calendar.YEAR), cardEntry.selectedDate.get(Calendar.MONTH), cardEntry.selectedDate.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void updateDateButton(CardEntry cardEntry) {
        String dateStr = android.text.format.DateFormat.getDateFormat(this).format(cardEntry.selectedDate.getTime());
        cardEntry.dueDateButton.setText(getString(R.string.due_date_button_text, dateStr));
    }

    private void saveConfiguration() {
        saveButton.setEnabled(false);
        saveButton.setText(getString(R.string.saving));

        if (isViewAllMode) {
            saveAllWidgetsSync();
        } else {
            saveSingleWidget(appWidgetId, cardEntries);
        }
    }

    private void saveAllWidgetsSync() {
        List<Integer> widgetIdsToUpdate = new ArrayList<>();
        for (int id : allWidgetIds) {
            widgetIdsToUpdate.add(id);
        }

        executorService.execute(() -> {
            boolean allSuccess = true;

            for (int widgetId : widgetIdsToUpdate) {
                List<CardEntry> entriesForWidget = new ArrayList<>();
                for (CardEntry entry : cardEntries) {
                    if (entry.widgetId == widgetId) {
                        entriesForWidget.add(entry);
                    }
                }

                if (entriesForWidget.isEmpty()) {
                    continue;
                }

                if (!saveCardsForWidget(widgetId, entriesForWidget)) {
                    allSuccess = false;
                }
            }

            boolean finalAllSuccess = allSuccess;
            mainHandler.post(() -> {
                if (finalAllSuccess) {
                    finishAndSuccess();
                } else {
                    showToast(getString(R.string.error_saving_configuration));
                    saveButton.setEnabled(true);
                    saveButton.setText(getString(R.string.save_widget));
                }
            });
        });
    }

    private void saveSingleWidget(int widgetId, List<CardEntry> entries) {
        if (entries.isEmpty()) {
            showToast(getString(R.string.please_add_at_least_one_card));
            saveButton.setEnabled(true);
            saveButton.setText(getString(R.string.save_widget));
            return;
        }

        executorService.execute(() -> {
            boolean success = saveCardsForWidget(widgetId, entries);
            mainHandler.post(() -> {
                if (success) {
                    Intent resultValue = new Intent();
                    resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
                    setResult(RESULT_OK, resultValue);
                    finish();
                } else {
                    showToast(getString(R.string.error_saving_configuration));
                    saveButton.setEnabled(true);
                    saveButton.setText(getString(R.string.save_widget));
                }
            });
        });
    }

    private boolean saveCardsForWidget(int widgetId, List<CardEntry> entries) {
        JSONArray cardsArray = new JSONArray();
        for (CardEntry entry : entries) {
            String cardName = entry.cardNameEdit.getText().toString().trim();
            if (TextUtils.isEmpty(cardName)) {
                cardName = getString(R.string.credit_card);
            }

            try {
                JSONObject cardObj = new JSONObject();
                cardObj.put("name", cardName);
                cardObj.put("dueDate", entry.selectedDate.getTimeInMillis());
                cardsArray.put(cardObj);
            } catch (JSONException e) {
                Log.w(TAG, "Error creating card JSON", e);
            }
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME + widgetId, Context.MODE_PRIVATE);
        boolean success = prefs.edit().putString(CARDS_DATA_KEY, cardsArray.toString()).commit();

        if (success) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
            CreditCardWidgetProvider.updateAppWidget(this, appWidgetManager, widgetId);
        }
        return success;
    }

    private void finishAndSuccess() {
        setResult(RESULT_OK);
        finish();
    }

    private long getDefaultDueDate() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, 1);
        cal.set(Calendar.DAY_OF_MONTH, 15);
        return cal.getTimeInMillis();
    }

    private void showToast(String message) {
        if (!isFinishing() && !isDestroyed()) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

    private void showErrorAndFinish(String message) {
        showToast(message);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cardEntries != null) {
            cardEntries.clear();
        }
    }

    private static class CardEntry {
        final View cardView;
        final TextInputEditText cardNameEdit;
        final Button dueDateButton;
        final Calendar selectedDate;
        final int widgetId;

        CardEntry(View cardView, TextInputEditText cardNameEdit, Button dueDateButton, Calendar selectedDate, int widgetId) {
            this.cardView = cardView;
            this.cardNameEdit = cardNameEdit;
            this.dueDateButton = dueDateButton;
            this.selectedDate = selectedDate;
            this.widgetId = widgetId;
        }
    }
}