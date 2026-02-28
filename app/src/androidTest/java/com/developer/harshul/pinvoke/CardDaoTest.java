package com.developer.harshul.pinvoke;

import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.developer.harshul.pinvoke.data.local.AppDatabase;
import com.developer.harshul.pinvoke.data.local.CardDao;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class CardDaoTest {

    // Ensures LiveData execution happens synchronously
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AppDatabase database;
    private CardDao cardDao;

    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        // Create an in-memory database so data is discarded when the process is killed
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries() // Allow for testing
                .build();
        cardDao = database.cardDao();
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void testInsertAndGetCardById() {
        // Given
        Card card = new Card("test-id-1", "My Card", 123456L, 10, true, false);
        
        // When
        cardDao.insert(card);
        Card retrievedCard = cardDao.getCardById("test-id-1");

        // Then
        assertNotNull(retrievedCard);
        assertEquals("test-id-1", retrievedCard.getId());
        assertEquals("My Card", retrievedCard.getName());
        assertEquals(10, retrievedCard.getWidgetId());
    }

    @Test
    public void testDeleteCardsForWidget() {
        // Given
        Card card1Widget1 = new Card("id1", "Card 1", 0, 1, false, false);
        Card card2Widget1 = new Card("id2", "Card 2", 0, 1, false, false);
        Card card3Widget2 = new Card("id3", "Card 3", 0, 2, false, false);
        
        cardDao.insert(card1Widget1);
        cardDao.insert(card2Widget1);
        cardDao.insert(card3Widget2);

        // When
        cardDao.deleteCardsForWidget(1);
        
        // Then
        List<Card> widget1Cards = cardDao.getCardsForWidget(1);
        List<Card> widget2Cards = cardDao.getCardsForWidget(2);
        
        assertTrue(widget1Cards.isEmpty()); // Should be deleted
        assertEquals(1, widget2Cards.size()); // Should remain intact
        assertEquals("id3", widget2Cards.get(0).getId());
    }

    @Test
    public void testGetAllCards() {
        // Given
        Card card1 = new Card("id1", "Card 1", 0, 1, false, false);
        Card card2 = new Card("id2", "Card 2", 0, 2, false, false);
        cardDao.insert(card1);
        cardDao.insert(card2);

        // When
        List<Card> allCards = cardDao.getAllCards();

        // Then
        assertEquals(2, allCards.size());
    }
}
