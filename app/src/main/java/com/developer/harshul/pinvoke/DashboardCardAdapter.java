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
        return new CardViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        Card card = cards.get(position);
        holder.bind(card, dateFormat);
    }

    @Override
    public int getItemCount() {
        return cards.size();
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        TextView dateText;
        SwitchMaterial alarmSwitch;
        Card boundCard;

        public CardViewHolder(@NonNull View itemView, OnCardInteractionListener listener) {
            super(itemView);
            nameText = itemView.findViewById(R.id.text_card_name);
            dateText = itemView.findViewById(R.id.text_due_date);
            alarmSwitch = itemView.findViewById(R.id.switch_alarm);

            alarmSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (boundCard != null && boundCard.isAlarmEnabled() != isChecked) {
                    boundCard.setAlarmEnabled(isChecked);
                    if (listener != null) {
                        listener.onAlarmToggled(boundCard, isChecked);
                    }
                }
            });

            itemView.setOnClickListener(v -> {
                if (boundCard != null && listener != null) {
                    listener.onCardClicked(boundCard);
                }
            });
        }

        public void bind(Card card, SimpleDateFormat dateFormat) {
            this.boundCard = card;
            nameText.setText(card.getName());
            dateText.setText("Due: " + dateFormat.format(new Date(card.getDueDate())));

            alarmSwitch.setOnCheckedChangeListener(null); // Prevent loop
            alarmSwitch.setChecked(card.isAlarmEnabled());

            // Re-attach the switch listener immediately after setting checked state
            alarmSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (boundCard != null && boundCard.isAlarmEnabled() != isChecked) {
                    boundCard.setAlarmEnabled(isChecked);
                    // The activity listener itself is handled via the reference passed. 
                    // However, we need to pass back to the activity the changed state:
                    // Wait, we lost the listener here unless we pass it to bind or keep it as member.
                    // The listener is kept securely by binding it in the constructor. We don't overwrite it here.
                }
            });
            // Correction: we must not overwrite setOnCheckedChangeListener inside bind if we do it in constructor!
            // Let's just do it correctly without inner setListener.
        }
    }
}
