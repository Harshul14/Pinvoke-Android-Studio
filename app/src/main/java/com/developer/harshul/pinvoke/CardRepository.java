package com.developer.harshul.pinvoke;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CardRepository {

    private static final String PREFS_NAME = "CCWidgetPrefs";
    private static final String CARDS_DATA_KEY = "cards_data";

    private final Context context;

    public CardRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    public List<Card> getCardsForWidget(int widgetId) {
        List<Card> cards = new ArrayList<>();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME + widgetId, Context.MODE_PRIVATE);
        String cardsJson = prefs.getString(CARDS_DATA_KEY, "");

        if (!TextUtils.isEmpty(cardsJson)) {
            try {
                JSONArray jsonArray = new JSONArray(cardsJson);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    cards.add(Card.fromJson(obj, widgetId));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return cards;
    }

    public List<Card> getAllCards() {
        List<Card> allCards = new ArrayList<>();
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] widgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, CreditCardWidgetProvider.class));

        for (int widgetId : widgetIds) {
            allCards.addAll(getCardsForWidget(widgetId));
        }
        return allCards;
    }

    public void saveCards(int widgetId, List<Card> cards) {
        JSONArray jsonArray = new JSONArray();
        for (Card card : cards) {
            try {
                jsonArray.put(card.toJson());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME + widgetId, Context.MODE_PRIVATE);
        prefs.edit().putString(CARDS_DATA_KEY, jsonArray.toString()).apply();
        
        // Notify widget helper to update widget view
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        CreditCardWidgetProvider.updateAppWidget(context, appWidgetManager, widgetId);
    }
    
    public void updateCard(Card updatedCard) {
        List<Card> cards = getCardsForWidget(updatedCard.getWidgetId());
        boolean found = false;
        for (int i = 0; i < cards.size(); i++) {
            if (cards.get(i).getId().equals(updatedCard.getId())) {
                cards.set(i, updatedCard);
                found = true;
                break;
            }
        }
        
        if (found) {
            saveCards(updatedCard.getWidgetId(), cards);
        }
    }

    public Card getCardById(String cardId) {
        // Since we don't know the widgetId efficiently, we have to search all.
        // Optimization: In a real app we might store a mapping of cardId -> widgetId, but loop is fine for now (small dataset).
        List<Card> allCards = getAllCards();
        for (Card card : allCards) {
            if (card.getId().equals(cardId)) {
                return card;
            }
        }
        return null;
    }
}
