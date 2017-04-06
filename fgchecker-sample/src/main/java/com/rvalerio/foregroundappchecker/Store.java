package com.rvalerio.foregroundappchecker;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by fnokeke on 3/10/17.
 *
 */

class Store {
    final static String TREATMENT_START = "treatmentStart";
    final static String FOLLOWUP_START = "followupStart";
    final static String LOGGING_STOP = "loggingStop";
    final static String EXPERIMENT_GROUP = "experimentGroup";

    final static String FB_MAX_TIME = "adminFBMaxMins";
    final static String FB_MAX_OPENS = "adminFBMaxOpens";
    static final String DAILY_RESET_HOUR = "dailyResetHour";

    final static String CANNOT_CONTINUE = "cannotContinue";
    private static final String PREF_NAME = "prefs";
    final static String WORKER_ID = "workerID";
    static final String EXPERIMENT_JOIN_DATE = "experimentJoinDate";

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
