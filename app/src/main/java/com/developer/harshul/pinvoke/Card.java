package com.developer.harshul.pinvoke;

import java.util.UUID;
import org.json.JSONException;
import org.json.JSONObject;

public class Card {
    private String id;
    private String name;
    private long dueDate;
    private int widgetId;
    private boolean isAlarmEnabled;
    private boolean isPaid;

    public Card(String id, String name, long dueDate, int widgetId, boolean isAlarmEnabled, boolean isPaid) {
        this.id = id;
        this.name = name;
        this.dueDate = dueDate;
        this.widgetId = widgetId;
        this.isAlarmEnabled = isAlarmEnabled;
        this.isPaid = isPaid;
    }

    public Card(String name, long dueDate, int widgetId) {
        this(UUID.randomUUID().toString(), name, dueDate, widgetId, false, false);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public long getDueDate() { return dueDate; }
    public int getWidgetId() { return widgetId; }
    public boolean isAlarmEnabled() { return isAlarmEnabled; }
    public boolean isPaid() { return isPaid; }

    public void setName(String name) { this.name = name; }
    public void setDueDate(long dueDate) { this.dueDate = dueDate; }
    public void setAlarmEnabled(boolean alarmEnabled) { isAlarmEnabled = alarmEnabled; }
    public void setPaid(boolean paid) { isPaid = paid; }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("name", name);
        json.put("dueDate", dueDate);
        json.put("widgetId", widgetId);
        json.put("isAlarmEnabled", isAlarmEnabled);
        json.put("isPaid", isPaid);
        return json;
    }

    public static Card fromJson(JSONObject json, int widgetId) {
        String id = json.optString("id", UUID.randomUUID().toString());
        String name = json.optString("name", "Credit Card");
        long dueDate = json.optLong("dueDate", 0);
        // Fallback for migration or missing fields
        boolean isAlarmEnabled = json.optBoolean("isAlarmEnabled", false);
        boolean isPaid = json.optBoolean("isPaid", false);
        
        return new Card(id, name, dueDate, widgetId, isAlarmEnabled, isPaid);
    }
}
