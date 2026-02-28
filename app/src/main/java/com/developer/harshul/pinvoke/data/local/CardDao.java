package com.developer.harshul.pinvoke.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.developer.harshul.pinvoke.Card;

import java.util.List;

@Dao
public interface CardDao {
    @Query("SELECT * FROM cards")
    List<Card> getAllCards();

    @Query("SELECT * FROM cards")
    LiveData<List<Card>> getAllCardsLiveData();

    @Query("SELECT * FROM cards WHERE widgetId = :widgetId")
    List<Card> getCardsForWidget(int widgetId);

    @Query("SELECT * FROM cards WHERE widgetId = :widgetId")
    LiveData<List<Card>> getCardsForWidgetLiveData(int widgetId);

    @Query("SELECT * FROM cards WHERE id = :id")
    Card getCardById(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Card card);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Card> cards);

    @Update
    void update(Card card);

    @Delete
    void delete(Card card);

    @Query("DELETE FROM cards WHERE widgetId = :widgetId")
    void deleteCardsForWidget(int widgetId);

    @Query("DELETE FROM cards")
    void deleteAll();
}
