package com.developer.harshul.pinvoke;

import android.appwidget.AppWidgetManager;
import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.developer.harshul.pinvoke.data.local.AppDatabase;
import com.developer.harshul.pinvoke.data.local.CardDao;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the CardRepository using Mockito to verify DAO interactions
 * without hitting an actual database.
 */
public class CardRepositoryUnitTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule(); // For LiveData

    @Rule
    public SynchronousExecutorRule synchronousExecutorRule = new SynchronousExecutorRule(); // For AppExecutors

    @Mock
    private Context mockContext;
    @Mock
    private Context mockAppContext;
    @Mock
    private AppDatabase mockDatabase;
    @Mock
    private CardDao mockCardDao;
    @Mock
    private AppWidgetManager mockAppWidgetManager;

    private CardRepository cardRepository;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);

        when(mockContext.getApplicationContext()).thenReturn(mockAppContext);

        // Mock AppDatabase.getDatabase() static method
        try (MockedStatic<AppDatabase> mockedDb = mockStatic(AppDatabase.class)) {
            mockedDb.when(() -> AppDatabase.getDatabase(mockAppContext)).thenReturn(mockDatabase);
            
            when(mockDatabase.cardDao()).thenReturn(mockCardDao);

            // Initialize repository while the static mock is active
            cardRepository = new CardRepository(mockContext);
        }
    }

    @Test
    public void testGetAllCardsLiveDataDelegatesToDao() {
        // Given
        MutableLiveData<List<Card>> fakeLiveData = new MutableLiveData<>(new ArrayList<>());
        when(mockCardDao.getAllCardsLiveData()).thenReturn(fakeLiveData);

        // When
        LiveData<List<Card>> result = cardRepository.getAllCardsLiveData();

        // Then
        verify(mockCardDao, times(1)).getAllCardsLiveData();
        assertEquals(fakeLiveData, result);
    }

    @Test
    public void testGetCardsForWidgetLiveDataDelegatesToDao() {
        // Given
        int widgetId = 5;
        MutableLiveData<List<Card>> fakeLiveData = new MutableLiveData<>(new ArrayList<>());
        when(mockCardDao.getCardsForWidgetLiveData(widgetId)).thenReturn(fakeLiveData);

        // When
        LiveData<List<Card>> result = cardRepository.getCardsForWidgetLiveData(widgetId);

        // Then
        verify(mockCardDao, times(1)).getCardsForWidgetLiveData(widgetId);
        assertEquals(fakeLiveData, result);
    }

    @Test
    public void testGetCardsForWidgetDelegatesToDao() {
        // Given
        int widgetId = 2;
        List<Card> expectedList = new ArrayList<>();
        when(mockCardDao.getCardsForWidget(widgetId)).thenReturn(expectedList);

        // When
        List<Card> result = cardRepository.getCardsForWidget(widgetId);

        // Then
        verify(mockCardDao, times(1)).getCardsForWidget(widgetId);
        assertEquals(expectedList, result);
    }

    @Test
    public void testUpdateCardDelegatesToDao() {
        // Given
        Card card = new Card("id", "Update me", 0L, 0, false, false);

        // When
        cardRepository.updateCard(card);

        // Then
        verify(mockCardDao, times(1)).update(card);
    }

    @Test
    public void testDeleteCardsForWidgetDelegatesToDao() {
        // Given
        int widgetId = 99;

        // When
        cardRepository.deleteCardsForWidget(widgetId);

        // Then
        verify(mockCardDao, times(1)).deleteCardsForWidget(widgetId);
    }

    @Test
    public void testGetCardByIdDelegatesToDao() {
        // Given
        String id = "some-id";
        Card expectedCard = new Card(id, "Card", 0L, 0, false, false);
        when(mockCardDao.getCardById(id)).thenReturn(expectedCard);

        // When
        Card result = cardRepository.getCardById(id);

        // Then
        verify(mockCardDao, times(1)).getCardById(id);
        assertEquals(expectedCard, result);
    }

    @Test
    public void testSaveCardsPerformsAppropriateSteps() {
        // Given
        int widgetId = 15;
        List<Card> newCards = new ArrayList<>();
        Card card1 = new Card("id1", "Card1", 0L, 0, false, false); // initial widgetId is 0
        Card card2 = new Card("id2", "Card2", 0L, 0, false, false);
        newCards.add(card1);
        newCards.add(card2);

        // Mock AppWidgetManager and static methods for the widget update logic
        try (MockedStatic<AppWidgetManager> mockedWidgetManager = mockStatic(AppWidgetManager.class);
             MockedStatic<CreditCardWidgetProvider> mockedProvider = mockStatic(CreditCardWidgetProvider.class)) {
            
            mockedWidgetManager.when(() -> AppWidgetManager.getInstance(mockAppContext)).thenReturn(mockAppWidgetManager);

            // When
            cardRepository.saveCards(widgetId, newCards);

            // Then
            // 1. Existing cards for widget are deleted
            verify(mockCardDao, times(1)).deleteCardsForWidget(widgetId);

            // 2. New cards are inserted with correct widgetId updated
            ArgumentCaptor<Card> cardCaptor = ArgumentCaptor.forClass(Card.class);
            verify(mockCardDao, times(2)).insert(cardCaptor.capture());
            
            List<Card> insertedCards = cardCaptor.getAllValues();
            assertEquals(15, insertedCards.get(0).getWidgetId());
            assertEquals(15, insertedCards.get(1).getWidgetId());

            // 3. Widget provider is updated
            mockedProvider.verify(() -> CreditCardWidgetProvider.updateAppWidget(mockAppContext, mockAppWidgetManager, widgetId), times(1));
        }
    }
}
