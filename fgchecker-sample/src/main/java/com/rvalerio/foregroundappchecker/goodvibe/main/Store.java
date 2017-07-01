package com.rvalerio.foregroundappchecker.goodvibe.main;

import android.content.Context;
import android.content.SharedPreferences;

import com.rvalerio.foregroundappchecker.goodvibe.helper.JsonHelper;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by fnokeke on 3/10/17.
 *
 */

public class Store {
    final static String TREATMENT_START = "treatmentStart";
    final static String FOLLOWUP_START = "followupStart";
    final static String LOGGING_STOP = "loggingStop";
    final static String EXPERIMENT_GROUP = "experimentGroup";

    final static String FB_MAX_MINUTES = "adminFBMaxMins";
    final static String FB_MAX_OPENS = "adminFBMaxOpens";

    final static String DAILY_RESET_HOUR = "dailyResetHour";
    final static String WORKER_ID = "workerID";
    final static String STUDY_CODE = "vibeStudyCode";
    final static String EXPERIMENT_JOIN_DATE = "experimentJoinDate";

    private static final String PREF_NAME = "prefs";
    static final String CAN_SHOW_PERMISSION_BTN = "canShowPermissionBtn";
    static final String CAN_SHOW_SUBMIT_BTN = "canShowServiceBtn";
    final static String ENROLLED = "userIsEnrolled";

    public static final String FB_LOGS_CSV_FILENAME = "fbLogs.csv";
    public static final String APP_LOGS_CSV_FILENAME = "appLogs.csv";
    public static final String SCREEN_LOGS_CSV_FILENAME = "screenLogs.csv";
    public static final String BACKUP_FB_LOGS_CSV_FILENAME = "bk-fbLogs.csv";
    public static final String BACKUP_APP_LOGS_CSV_FILENAME = "bk-appLogs.csv";
    public static final String BACKUP_SCREEN_LOGS_CSV_FILENAME = "bk-screenLogs.csv";
    public static final String RESPONSE_TO_SUBMIT = "submitBtnResponse";
    public static final String SURVEY_LINK = "surveyLink";
    public static final String LAST_SCREEN_EVENT = "lastScreenEvent";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static JSONArray getJsonArray(Context context, String key) {
        return JsonHelper.strToJsonArray(getString(context, key));
    }

    public static JSONObject getJsonObject(Context context, String key) {
        return JsonHelper.strToJsonObject(getString(context, key));
    }

    public static String getString(Context context, String key) {
        return getPrefs(context).getString(key, "");
    }

    public static Integer getInt(Context context, String key) {
        return getPrefs(context).getInt(key, 0);
    }

    public static Boolean getBoolean(Context context, String key) {
        return getPrefs(context).getBoolean(key, false);
    }

    public static void setJsonArray(Context context, String key, JSONArray input) {
        getPrefs(context).edit().putString(key, input.toString()).apply();
    }

    public static void setJsonObject(Context context, String key, JSONObject input) {
        getPrefs(context).edit().putString(key, input.toString()).apply();
    }

    public static void setString(Context context, String key, String input) {
        getPrefs(context).edit().putString(key, input).apply();
    }

    public static void setInt(Context context, String key, Integer input) {
        getPrefs(context).edit().putInt(key, input).apply();
    }

    public static void increaseInt(Context context, String key, Integer increment) {
        Integer currentValue = getInt(context, key);
        setInt(context, key, currentValue + increment);
    }

    public static void setBoolean(Context context, String key, Boolean input) {
        getPrefs(context).edit().putBoolean(key, input).apply();
    }


}
