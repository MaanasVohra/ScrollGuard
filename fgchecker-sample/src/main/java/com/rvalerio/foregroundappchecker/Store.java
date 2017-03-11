package com.rvalerio.foregroundappchecker;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;

/**
 * Created by fnokeke on 3/10/17.
 *
 */

class Store {
    public final static String STUDY_START = "studyStart";
    public final static String TREATMENT_START = "treatmentStart";
    public final static String FOLLOWUP_START = "followupStart";
    public final static String LOGGING_STOP = "loggingStop";
    public final static String EXPERIMENT_GROUP = "experimentGroup";
    public final static String CANNOT_CONTINUE = "cannotContinue";

    public final static String FB_MAX_TIME = "serverFBMaxTime";
    public final static String FB_MAX_OPENS = "serverFBMaxOpens";

    private static final String PREF_NAME = "prefs";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
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

}
