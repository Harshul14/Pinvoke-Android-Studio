package com.developer.harshul.pinvoke;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.developer.harshul.pinvoke.AppExecutors;

import java.util.concurrent.TimeUnit;

public class CreditCardWidgetProvider extends AppWidgetProvider {

    private static final String TAG = "CreditCardWidget";
    private static final String PREFS_NAME = "CCWidgetPrefs";
    private static final String CARDS_DATA_KEY = "cards_data";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            for (int appWidgetId : appWidgetIds) {
                try {
                    updateAppWidget(context, appWidgetManager, appWidgetId, null);
                } catch (Exception e) {
                    Log.e(TAG, "Error updating widget " + appWidgetId, e);
                    showErrorState(context, appWidgetManager, appWidgetId);
                }
            }
        });
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                updateAppWidget(context, appWidgetManager, appWidgetId, newOptions);
            } catch (Exception e) {
                Log.e(TAG, "Error updating widget on options changed " + appWidgetId, e);
                showErrorState(context, appWidgetManager, appWidgetId);
            }
        });
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        updateAppWidget(context, appWidgetManager, appWidgetId, null);
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle options) {
        if (context == null || appWidgetManager == null) {
            Log.w(TAG, "Context or AppWidgetManager is null");
            return;
        }

        try {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_credit_card);

            CardRepository repository = new CardRepository(context);
            List<Card> cards = repository.getCardsForWidget(appWidgetId);

            List<CardData> cardDataList = new ArrayList<>();
            for (Card card : cards) {
                long originalDueDate = card.getDueDate();
                long updatedDueDate = updateOverdueDateToNextMonth(originalDueDate);

                if (updatedDueDate != originalDueDate) {
                    card.setDueDate(updatedDueDate);
                    card.setPaid(false); // Reset paid status for new month
                    repository.updateCard(card);
                }

                cardDataList.add(new CardData(card.getName(), card.getDueDate()));
            }

            Collections.sort(cardDataList, (c1, c2) -> Long.compare(c1.dueDate, c2.dueDate));

            Intent intent = new Intent(context, MainActivity.class);
            intent.setAction(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            if (cardDataList.isEmpty()) {
                views.setViewVisibility(R.id.empty_view, View.VISIBLE);
                views.setViewVisibility(R.id.cards_view_flipper, View.GONE);
                views.setOnClickPendingIntent(R.id.widget_container, pendingIntent);
            } else {
                views.setViewVisibility(R.id.empty_view, View.GONE);
                views.setViewVisibility(R.id.cards_view_flipper, View.VISIBLE);

                views.removeAllViews(R.id.cards_view_flipper);
                int widgetWidth = getWidgetWidth(appWidgetManager, appWidgetId, options);

                for (CardData cardData : cardDataList) {
                    RemoteViews cardView = new RemoteViews(context.getPackageName(), R.layout.widget_card_item);
                    updateWidgetDisplay(context, cardView, cardData, widgetWidth);
                    cardView.setOnClickPendingIntent(R.id.widget_item_container, pendingIntent);
                    views.addView(R.id.cards_view_flipper, cardView);
                }
            }

            appWidgetManager.updateAppWidget(appWidgetId, views);

        } catch (Exception e) {
            Log.e(TAG, "Error in updateAppWidget", e);
            showErrorState(context, appWidgetManager, appWidgetId);
        }
    }

    private static int getWidgetWidth(AppWidgetManager appWidgetManager, int appWidgetId, Bundle options) {
        Bundle newOptions = (options == null) ? appWidgetManager.getAppWidgetOptions(appWidgetId) : options;
        if (newOptions != null && newOptions.containsKey(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)) {
            return newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        }
        return 0;
    }

    private static ParseResult parseCardData(String cardsDataJson) {
        if (cardsDataJson == null || cardsDataJson.trim().isEmpty()) {
            return new ParseResult(Collections.emptyList(), null, false);
        }

        try {
            JSONArray cardsArray = new JSONArray(cardsDataJson);
            if (cardsArray.length() == 0) {
                return new ParseResult(Collections.emptyList(), null, false);
            }

            return processCards(cardsArray);

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing card data", e);
            return new ParseResult(Collections.emptyList(), null, false);
        }
    }

    private static ParseResult processCards(JSONArray cardsArray) throws JSONException {
        List<CardData> cardDataList = new ArrayList<>();
        JSONArray updatedCardsArray = new JSONArray();
        boolean dataUpdated = false;

        for (int i = 0; i < cardsArray.length(); i++) {
            JSONObject cardObj = cardsArray.getJSONObject(i);
            Card card = Card.fromJson(cardObj, 0); // WidgetId irrelevant for this op

            long originalDueDate = card.getDueDate();
            long updatedDueDate = updateOverdueDateToNextMonth(originalDueDate);

            if (updatedDueDate != originalDueDate) {
                dataUpdated = true;
                card.setDueDate(updatedDueDate);
                card.setPaid(false); // Reset paid status for new month
            }

            cardDataList.add(new CardData(card.getName(), card.getDueDate()));

            updatedCardsArray.put(card.toJson());
        }

        Collections.sort(cardDataList, (c1, c2) -> Long.compare(c1.dueDate, c2.dueDate));

        return new ParseResult(cardDataList, dataUpdated ? updatedCardsArray : null, dataUpdated);
    }

    private static void updateWidgetDisplay(Context context, RemoteViews views, CardData cardData, int widgetWidth) {
        try {
            long currentTime = System.currentTimeMillis();
            long diffMillis = cardData.dueDate - currentTime;
            int daysRemaining = (int) TimeUnit.MILLISECONDS.toDays(diffMillis);

            SimpleDateFormat dateFormatter = new SimpleDateFormat("MMM dd", Locale.getDefault());
            String dueDateStr = dateFormatter.format(new Date(cardData.dueDate));

            views.setTextViewText(R.id.card_name, cardData.name);
            views.setTextViewText(R.id.due_date, dueDateStr);

            DaysDisplayInfo displayInfo = calculateDaysDisplay(context, daysRemaining, widgetWidth);
            views.setTextViewText(R.id.days_remaining, displayInfo.text);
            views.setTextColor(R.id.days_remaining, displayInfo.color);
            views.setTextViewText(R.id.days_label, displayInfo.label);

        } catch (Exception e) {
            Log.e(TAG, "Error updating widget display", e);
            views.setTextViewText(R.id.card_name, "Credit Card");
            views.setTextViewText(R.id.due_date, "---");
            views.setTextViewText(R.id.days_remaining, "--");
            views.setTextViewText(R.id.days_label, "days");
        }
    }

    private static DaysDisplayInfo calculateDaysDisplay(Context context, int daysRemaining, int widgetWidth) {
        String daysText;
        String label = context.getString(R.string.days);
        int textColor;
        boolean useShortText = widgetWidth > 0 && widgetWidth < 180; // 180dp as a threshold for small widget

        if (daysRemaining < 0) {
            daysText = useShortText ? "DUE" : "OVERDUE";
            label = "";
            textColor = ContextCompat.getColor(context, R.color.error);
        } else if (daysRemaining == 0) {
            daysText = useShortText ? "DUE" : "TODAY";
            label = "";
            textColor = ContextCompat.getColor(context, R.color.error);
        } else {
            daysText = String.valueOf(daysRemaining);
            if (daysRemaining == 1) {
                label = "day";
            }
            if (daysRemaining <= 3) {
                textColor = ContextCompat.getColor(context, R.color.warning);
            } else if (daysRemaining <= 7) {
                textColor = ContextCompat.getColor(context, R.color.amber);
            } else {
                textColor = ContextCompat.getColor(context, R.color.success);
            }
        }

        return new DaysDisplayInfo(daysText, label, textColor);
    }

    private static void showErrorState(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_credit_card);
                views.setViewVisibility(R.id.empty_view, View.VISIBLE);
                views.setTextViewText(R.id.empty_view, "Error. Tap to retry.");
                views.setViewVisibility(R.id.cards_view_flipper, View.GONE);
                appWidgetManager.updateAppWidget(appWidgetId, views);
            } catch (Exception e) {
                Log.e(TAG, "Error showing error state", e);
            }
        });
    }

    private static long getDefaultDueDate() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, 1);
        cal.set(Calendar.DAY_OF_MONTH, 15);
        return cal.getTimeInMillis();
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        CardRepository repository = new CardRepository(context);
        for (int appWidgetId : appWidgetIds) {
            repository.deleteCardsForWidget(appWidgetId);
        }
        super.onDeleted(context, appWidgetIds);
    }

    private static long updateOverdueDateToNextMonth(long dueDate) {
        Calendar currentCal = Calendar.getInstance();
        Calendar dueCal = Calendar.getInstance();
        dueCal.setTimeInMillis(dueDate);

        currentCal.set(Calendar.HOUR_OF_DAY, 0);
        currentCal.set(Calendar.MINUTE, 0);
        currentCal.set(Calendar.SECOND, 0);
        currentCal.set(Calendar.MILLISECOND, 0);

        dueCal.set(Calendar.HOUR_OF_DAY, 0);
        dueCal.set(Calendar.MINUTE, 0);
        dueCal.set(Calendar.SECOND, 0);
        dueCal.set(Calendar.MILLISECOND, 0);

        if (dueCal.before(currentCal)) {
            int dayOfMonth = dueCal.get(Calendar.DAY_OF_MONTH);

            Calendar nextMonth = Calendar.getInstance();
            nextMonth.setTimeInMillis(System.currentTimeMillis());
            nextMonth.add(Calendar.MONTH, 1);

            int maxDay = nextMonth.getActualMaximum(Calendar.DAY_OF_MONTH);
            if (dayOfMonth > maxDay) {
                dayOfMonth = maxDay;
            }

            nextMonth.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            nextMonth.set(Calendar.HOUR_OF_DAY, 0);
            nextMonth.set(Calendar.MINUTE, 0);
            nextMonth.set(Calendar.SECOND, 0);
            nextMonth.set(Calendar.MILLISECOND, 0);

            return nextMonth.getTimeInMillis();
        }

        return dueDate;
    }

    private static class ParseResult {
        final List<CardData> cardDataList;
        final JSONArray updatedCardsJson;
        final boolean dataUpdated;

        ParseResult(List<CardData> cardDataList, JSONArray updatedCardsJson, boolean dataUpdated) {
            this.cardDataList = cardDataList != null ? cardDataList : Collections.emptyList();
            this.updatedCardsJson = updatedCardsJson;
            this.dataUpdated = dataUpdated;
        }
    }

    private static class CardData {
        final String name;
        final long dueDate;

        CardData(String name, long dueDate) {
            this.name = name != null ? name : "Credit Card";
            this.dueDate = dueDate;
        }
    }

    private static class DaysDisplayInfo {
        final String text;
        final String label;
        final int color;

        DaysDisplayInfo(String text, String label, int color) {
            this.text = text != null ? text : "--";
            this.label = label;
            this.color = color;
        }
    }
}