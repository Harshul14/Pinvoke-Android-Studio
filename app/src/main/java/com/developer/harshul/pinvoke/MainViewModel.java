package com.developer.harshul.pinvoke;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

public class MainViewModel extends AndroidViewModel {
    private final CardRepository cardRepository;
    private final LiveData<List<Card>> allCardsLiveData;

    public MainViewModel(@NonNull Application application) {
        super(application);
        cardRepository = new CardRepository(application);
        allCardsLiveData = cardRepository.getAllCardsLiveData();
    }

    public LiveData<List<Card>> getAllCardsLiveData() {
        return allCardsLiveData;
    }

    public void updateCard(Card card) {
        cardRepository.updateCard(card);
    }
}
