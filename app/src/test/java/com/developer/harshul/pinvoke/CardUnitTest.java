package com.developer.harshul.pinvoke;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the Card entity covering various field assignments and JSON serialization/deserialization.
 */
public class CardUnitTest {

    @Test
    public void testCardConstructorAndGetters() {
        // Given
        String id = "test-card-123";
        String name = "Test Visa";
        long dueDate = 1672531199000L;
        int widgetId = 5;
        boolean isAlarmEnabled = true;
        boolean isPaid = false;

        // When
        Card card = new Card(id, name, dueDate, widgetId, isAlarmEnabled, isPaid);

        // Then
        assertEquals(id, card.getId());
        assertEquals(name, card.getName());
        assertEquals(dueDate, card.getDueDate());
        assertEquals(widgetId, card.getWidgetId());
        assertTrue(card.isAlarmEnabled());
        assertFalse(card.isPaid());
    }

    @Test
    public void testCardConvenienceConstructor() {
        // Given
        String name = "New Card";
        long dueDate = 1000L;
        int widgetId = 12;

        // When
        Card card = new Card(name, dueDate, widgetId);

        // Then
        assertNotNull(card.getId()); // UUID should be generated
        assertFalse(card.getId().isEmpty());
        assertEquals(name, card.getName());
        assertEquals(dueDate, card.getDueDate());
        assertEquals(widgetId, card.getWidgetId());
        assertFalse(card.isAlarmEnabled()); // defaults to false
        assertFalse(card.isPaid()); // defaults to false
    }

    @Test
    public void testCardSetters() {
        // Given
        Card card = new Card("id", "name", 0L, 0, false, false);

        // When
        card.setId("new-id");
        card.setName("new-name");
        card.setDueDate(999L);
        card.setWidgetId(99);
        card.setAlarmEnabled(true);
        card.setPaid(true);

        // Then
        assertEquals("new-id", card.getId());
        assertEquals("new-name", card.getName());
        assertEquals(999L, card.getDueDate());
        assertEquals(99, card.getWidgetId());
        assertTrue(card.isAlarmEnabled());
        assertTrue(card.isPaid());
    }

    @Test
    public void testJsonSerialization() throws JSONException {
        // Given
        Card card = new Card("id-json", "JSON Card", 5000L, 7, true, false);

        // When
        JSONObject json = card.toJson();

        // Then
        assertNotNull(json);
        assertEquals("id-json", json.getString("id"));
        assertEquals("JSON Card", json.getString("name"));
        assertEquals(5000L, json.getLong("dueDate"));
        assertEquals(7, json.getInt("widgetId"));
        assertTrue(json.getBoolean("isAlarmEnabled"));
        assertFalse(json.getBoolean("isPaid"));
    }

    @Test
    public void testJsonDeserialization() throws JSONException {
        // Given
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", "deserialized-id");
        jsonObject.put("name", "Deserialized Card");
        jsonObject.put("dueDate", 12345L);
        jsonObject.put("widgetId", 1); // Should be overwritten by parameter
        jsonObject.put("isAlarmEnabled", false);
        jsonObject.put("isPaid", true);

        // When
        Card card = Card.fromJson(jsonObject, 10); // widgetId 10 takes precedence

        // Then
        assertNotNull(card);
        assertEquals("deserialized-id", card.getId());
        assertEquals("Deserialized Card", card.getName());
        assertEquals(12345L, card.getDueDate());
        assertEquals(10, card.getWidgetId()); // Verify it uses the passed param
        assertFalse(card.isAlarmEnabled());
        assertTrue(card.isPaid());
    }
}
