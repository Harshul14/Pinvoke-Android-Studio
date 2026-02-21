package com.developer.harshul.pinvoke;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardCardAdapter extends RecyclerView.Adapter<DashboardCardAdapter.CardViewHolder> {

    private List<Card> cards = new ArrayList<>();
    private final OnCardInteractionListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    public interface OnCardInteractionListener {
        void onAlarmToggled(Card card, boolean isEnabled);
        void onCardClicked(Card card);
    }

    public DashboardCardAdapter(OnCardInteractionListener listener) {
        this.listener = listener;
    }

    public void setCards(List<Card> newCards) {
        this.cards = newCards;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dashboard_card, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        Card card = cards.get(position);
        holder.bind(card, listener, dateFormat);
    }

    @Override
    public int getItemCount() {
        return cards.size();
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        TextView dateText;
        SwitchMaterial alarmSwitch;

        public CardViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.text_card_name);
            dateText = itemView.findViewById(R.id.text_due_date);
            alarmSwitch = itemView.findViewById(R.id.switch_alarm);
        }

        public void bind(Card card, OnCardInteractionListener listener, SimpleDateFormat dateFormat) {
            nameText.setText(card.getName());
            dateText.setText("Due: " + dateFormat.format(new Date(card.getDueDate())));

            alarmSwitch.setOnCheckedChangeListener(null); // Prevent loop
            alarmSwitch.setChecked(card.isAlarmEnabled());
            
            alarmSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                card.setAlarmEnabled(isChecked);
                listener.onAlarmToggled(card, isChecked);
            });

            itemView.setOnClickListener(v -> listener.onCardClicked(card));
        }
    }
}
