package com.developer.harshul.pinvoke;

import org.json.JSONException;
import org.json.JSONObject;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "global_alarms")
public class GlobalAlarmConfig {
    @PrimaryKey
    private int id; // 1, 2, or 3
    private int hourOfDay;
    private int minute;
    private boolean isEnabled;
    private String ringtoneUri;

    public GlobalAlarmConfig(int id, int hourOfDay, int minute, boolean isEnabled, String ringtoneUri) {
        this.id = id;
        this.hourOfDay = hourOfDay;
        this.minute = minute;
        this.isEnabled = isEnabled;
        this.ringtoneUri = ringtoneUri;
    }

    public int getId() { return id; }
    public int getHourOfDay() { return hourOfDay; }
    public int getMinute() { return minute; }
    public boolean isEnabled() { return isEnabled; }
    public String getRingtoneUri() { return ringtoneUri; }

    public void setHourOfDay(int hourOfDay) { this.hourOfDay = hourOfDay; }
    public void setMinute(int minute) { this.minute = minute; }
    public void setEnabled(boolean enabled) { isEnabled = enabled; }
    public void setRingtoneUri(String ringtoneUri) { this.ringtoneUri = ringtoneUri; }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("hourOfDay", hourOfDay);
        json.put("minute", minute);
        json.put("isEnabled", isEnabled);
        if (ringtoneUri != null) {
            json.put("ringtoneUri", ringtoneUri);
        }
        return json;
    }

    public static GlobalAlarmConfig fromJson(JSONObject json) {
        int id = json.optInt("id", 1);
        int hourOfDay = json.optInt("hourOfDay", 9);
        int minute = json.optInt("minute", 0);
        boolean isEnabled = json.optBoolean("isEnabled", true);
        String ringtoneUri = json.has("ringtoneUri") ? json.optString("ringtoneUri") : null;
        
        return new GlobalAlarmConfig(id, hourOfDay, minute, isEnabled, ringtoneUri);
    }
}
