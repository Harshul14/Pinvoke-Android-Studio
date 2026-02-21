package com.developer.harshul.pinvoke;

import android.appwidget.AppWidgetManager;
import android.content.Context;

import androidx.lifecycle.LiveData;

import com.developer.harshul.pinvoke.AppExecutors;
import com.developer.harshul.pinvoke.data.local.AppDatabase;
import com.developer.harshul.pinvoke.data.local.CardDao;

import java.util.List;

public class CardRepository {

    private final CardDao cardDao;
    private final Context context;

    public CardRepository(Context context) {
        this.context = context.getApplicationContext();
        AppDatabase db = AppDatabase.getDatabase(this.context);
        this.cardDao = db.cardDao();
    }

    public LiveData<List<Card>> getAllCardsLiveData() {
        return cardDao.getAllCardsLiveData();
    }

    public LiveData<List<Card>> getCardsForWidgetLiveData(int widgetId) {
        return cardDao.getCardsForWidgetLiveData(widgetId);
    }

    public List<Card> getCardsForWidget(int widgetId) {
        return cardDao.getCardsForWidget(widgetId);
    }

    public List<Card> getAllCards() {
        return cardDao.getAllCards();
    }

    public void saveCards(int widgetId, List<Card> cards) {
        // Run DB operation on background thread
        AppExecutors.getInstance().diskIO().execute(() -> {
            cardDao.deleteCardsForWidget(widgetId);
            for (Card card : cards) {
                card.setWidgetId(widgetId); // Ensure correct widget ID
                cardDao.insert(card);
            }
            
            // Notify widget helper to update widget view
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            CreditCardWidgetProvider.updateAppWidget(context, appWidgetManager, widgetId);
        });
    }
    
    public void updateCard(Card updatedCard) {
        AppExecutors.getInstance().diskIO().execute(() -> cardDao.update(updatedCard));
    }

    public Card getCardById(String cardId) {
        return cardDao.getCardById(cardId);
    }

    public void deleteCardsForWidget(int widgetId) {
        AppExecutors.getInstance().diskIO().execute(() -> cardDao.deleteCardsForWidget(widgetId));
    }
}
