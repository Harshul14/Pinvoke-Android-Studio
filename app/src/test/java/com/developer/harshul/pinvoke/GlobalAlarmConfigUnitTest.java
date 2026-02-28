package com.developer.harshul.pinvoke;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the GlobalAlarmConfig entity.
 */
public class GlobalAlarmConfigUnitTest {

    @Test
    public void testConstructorAndGetters() {
        // Given
        int id = 1;
        int hourOfDay = 14;
        int minute = 30;
        boolean isEnabled = true;
        String ringtoneUri = "content://media/external/audio/media/123";

        // When
        GlobalAlarmConfig config = new GlobalAlarmConfig(id, hourOfDay, minute, isEnabled, ringtoneUri);

        // Then
        assertEquals(id, config.getId());
        assertEquals(hourOfDay, config.getHourOfDay());
        assertEquals(minute, config.getMinute());
        assertTrue(config.isEnabled());
        assertEquals(ringtoneUri, config.getRingtoneUri());
    }

    @Test
    public void testSetters() {
        // Given
        GlobalAlarmConfig config = new GlobalAlarmConfig(1, 0, 0, false, null);

        // When
        config.setHourOfDay(8);
        config.setMinute(45);
        config.setEnabled(true);
        config.setRingtoneUri("default");

        // Then
        assertEquals(8, config.getHourOfDay());
        assertEquals(45, config.getMinute());
        assertTrue(config.isEnabled());
        assertEquals("default", config.getRingtoneUri());
    }

    @Test
    public void testJsonSerialization() throws JSONException {
        // Given
        GlobalAlarmConfig config = new GlobalAlarmConfig(2, 23, 59, true, "uri_string");

        // When
        JSONObject json = config.toJson();

        // Then
        assertNotNull(json);
        assertEquals(2, json.getInt("id"));
        assertEquals(23, json.getInt("hourOfDay"));
        assertEquals(59, json.getInt("minute"));
        assertTrue(json.getBoolean("isEnabled"));
        assertEquals("uri_string", json.getString("ringtoneUri"));
    }

    @Test
    public void testJsonSerializationWithNullRingtone() throws JSONException {
        // Given
        GlobalAlarmConfig config = new GlobalAlarmConfig(3, 10, 15, false, null);

        // When
        JSONObject json = config.toJson();

        // Then
        assertNotNull(json);
        assertEquals(3, json.getInt("id"));
        assertEquals(10, json.getInt("hourOfDay"));
        assertEquals(15, json.getInt("minute"));
        assertFalse(json.getBoolean("isEnabled"));
        assertFalse(json.has("ringtoneUri"));
    }

    @Test
    public void testJsonDeserialization() throws JSONException {
        // Given
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", 1);
        jsonObject.put("hourOfDay", 6);
        jsonObject.put("minute", 0);
        jsonObject.put("isEnabled", true);
        jsonObject.put("ringtoneUri", "test_uri");

        // When
        GlobalAlarmConfig config = GlobalAlarmConfig.fromJson(jsonObject);

        // Then
        assertNotNull(config);
        assertEquals(1, config.getId());
        assertEquals(6, config.getHourOfDay());
        assertEquals(0, config.getMinute());
        assertTrue(config.isEnabled());
        assertEquals("test_uri", config.getRingtoneUri());
    }

    @Test
    public void testJsonDeserializationMissingFields() {
        // Given
        JSONObject emptyJson = new JSONObject(); // No fields set

        // When
        GlobalAlarmConfig config = GlobalAlarmConfig.fromJson(emptyJson);

        // Then
        // Should fall back to defined defaults:
        assertEquals(1, config.getId());
        assertEquals(9, config.getHourOfDay());
        assertEquals(0, config.getMinute());
        assertTrue(config.isEnabled());
        assertNull(config.getRingtoneUri());
    }
}
