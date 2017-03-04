package com.rvalerio.foregroundappchecker;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import static android.content.Context.TELEPHONY_SERVICE;

/**
 * Created by fnokeke on 1/26/17.
 */

public class Helper {

    private static final String PREF_NAME = "prefs";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static void copy(JSONObject from, JSONObject to) {
        for (int i = 0; i < from.names().length(); i++) {
            String key = from.names().optString(i);
            Object value = from.opt(key);
            setJSONValue(to, key, value);
        }
    }

    public static void setJSONValue(JSONObject jsonObject, String key, Object value) {
        try {
            jsonObject.put(key, value);
        } catch (JSONException je) {
            je.printStackTrace();
        }
    }

    public static String getTodayDateStr() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime());
    }


    public static String getStoreString(Context context, String key) {
        return getPrefs(context).getString(key, "");
    }

    public static Integer getStoreInt(Context context, String key) {
        return getPrefs(context).getInt(key, 0);
    }

    public static Boolean getStoreBoolean(Context context, String key) {
        return getPrefs(context).getBoolean(key, true); // use true as default value
    }

    public static void setStoreString(Context context, String key, String input) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putString(key, input);
        editor.apply();
    }

    public static void setStoreInt(Context context, String key, Integer input) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putInt(key, input);
        editor.apply();
    }

    public static void increaseStoreInt(Context context, String key, Integer increment) {
        Integer currentValue = getStoreInt(context, key);
        setStoreInt(context, key, currentValue + increment);
    }

    public static void setStoreBoolean(Context context, String key, Boolean input) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putBoolean(key, input);
        editor.apply();
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


}
