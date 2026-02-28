package com.developer.harshul.pinvoke;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.developer.harshul.pinvoke.data.local.AppDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

@RunWith(AndroidJUnit4.class)
public class CardRepositoryIntegrationTest {

    private AppDatabase database;
    private CardRepository repository;
    private Context context;

    @Before
    public void setup() {
        context = ApplicationProvider.getApplicationContext();
        repository = new CardRepository(context);
        
        // Access the singleton and clear data to prevent test pollution
        database = AppDatabase.getDatabase(context);
        database.cardDao().deleteAll();
    }

    @After
    public void tearDown() {
        database.cardDao().deleteAll();
    }

    @Test
    public void testDatabaseSingleton() {
        AppDatabase db1 = AppDatabase.getDatabase(context);
        AppDatabase db2 = AppDatabase.getDatabase(context);

        // Verify Singleton pattern returns the exact same instance
        assertSame("AppDatabase should return the same instance", db1, db2);
    }
    
    @Test
    public void testSaveCardsUpdatesDatabase() throws InterruptedException {
        // Given
        int widgetId = 55;
        List<Card> initialCards = new ArrayList<>();
        initialCards.add(new Card("1", "First Card", 1000L, widgetId, false, false));
        initialCards.add(new Card("2", "Second Card", 2000L, widgetId, false, false));

        // When 
        // We use saveCards from repo which acts on a background thread.
        repository.saveCards(widgetId, initialCards);
        
        // Wait briefly for background executor to finish disk IO operations
        Thread.sleep(500);

        // Then
        List<Card> savedCards = repository.getCardsForWidget(widgetId);
        assertNotNull(savedCards);
        assertEquals(2, savedCards.size());
        assertEquals(widgetId, savedCards.get(0).getWidgetId());
        assertEquals(widgetId, savedCards.get(1).getWidgetId());
    }
}
