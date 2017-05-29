package com.rvalerio.foregroundappchecker.goodvibe.helper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Helper.java
 * Created: 1/24/17
 * author: Fabian Okeke
 */

public class JsonHelper {

    public static void setJSONValue(JSONObject jsonObject, String key, Object value) {
        try {
            jsonObject.put(key, value);
        } catch (JSONException je) {
            je.printStackTrace();
        }
    }

    public static JSONObject strToJsonObject(String jsonStr) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject = new JSONObject(jsonStr);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public static JSONArray strToJsonArray(String jsonStr) {
        JSONArray jsonArray = new JSONArray();
        try {
            jsonArray = new JSONArray(jsonStr);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonArray;
    }

    public static void copy(JSONObject from, JSONObject to) {
        for (int i = 0; i < from.names().length(); i++) {
            String key = from.names().optString(i);
            Object value = from.opt(key);
            setJSONValue(to, key, value);
        }
    }

}
