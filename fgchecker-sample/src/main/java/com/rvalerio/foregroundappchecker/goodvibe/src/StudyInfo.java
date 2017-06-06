package com.rvalerio.foregroundappchecker.goodvibe.src;

import android.content.Context;

import com.rvalerio.foregroundappchecker.goodvibe.helper.FileHelper;

import org.json.JSONObject;

import java.util.Date;

/**
 * Study info for experiment
 * Created by fnokeke on 3/10/17.
 */

class StudyInfo {
    static final int STATIC_GROUP = 1;
    static final int ADAPTIVE_GROUP = 2;
    private static final int TREATMENT_START = 8;
    private static final int FOLLOWUP_START = 50;
    private static final int LOGGING_STOP = 70;
    private static final int INIT_DAILY_RESET_HOUR = 0;
    private static final int INIT_FB_MAX_DAILY_MINUTES = 30;
    private static final int INIT_FB_MAX_DAILY_OPENS = 10;
    final static String FACEBOOK_PACKAGE_NAME = "com.facebook.katana";

    static void setDefaults(Context context) {
        Store.setInt(context, Store.EXPERIMENT_GROUP, STATIC_GROUP);
        Store.setString(context, Store.TREATMENT_START, getTreatmentStartDateStr(context));
        Store.setString(context, Store.FOLLOWUP_START, getFollowupStartDateStr(context));
        Store.setString(context, Store.LOGGING_STOP, getLoggingStopDateStr(context));
        Store.setString(context, Store.LOGGING_STOP, getLoggingStopDateStr(context));
        Store.setInt(context, Store.DAILY_RESET_HOUR, INIT_DAILY_RESET_HOUR);
        Store.setInt(context, Store.FB_MAX_TIME, INIT_FB_MAX_DAILY_MINUTES);
        Store.setInt(context, Store.FB_MAX_OPENS, INIT_FB_MAX_DAILY_OPENS);
        FileHelper.prepareAllStorageFiles(context);
    }

    static void saveTodayAsExperimentJoinDate(Context context) {
        Store.setString(context, Store.EXPERIMENT_JOIN_DATE, Helper.getTodayDateStr());
    }

    private static String countFromJoinDate(Context context, int noOfDays) {
        String joinDateStr = Store.getString(context, Store.EXPERIMENT_JOIN_DATE);
        Date treatmentStart = Helper.strToDate(joinDateStr);
        treatmentStart = Helper.addDays(treatmentStart, noOfDays);
        return Helper.dateToStr(treatmentStart);
    }

    static String getTreatmentStartDateStr(Context context) {
        String treatmentStartStr = Store.getString(context, Store.TREATMENT_START);
        treatmentStartStr = treatmentStartStr.equals("") ? countFromJoinDate(context, TREATMENT_START) : treatmentStartStr;
        return treatmentStartStr;
    }

    static String getFollowupStartDateStr(Context context) {
        String followupStartStr = Store.getString(context, Store.FOLLOWUP_START);
        followupStartStr = followupStartStr.equals("") ? countFromJoinDate(context, FOLLOWUP_START) : followupStartStr;
        return followupStartStr;
    }

    static String getLoggingStopDateStr(Context context) {
        String loggingStopStr = Store.getString(context, Store.LOGGING_STOP);
        loggingStopStr = loggingStopStr.equals("") ? countFromJoinDate(context, LOGGING_STOP) : loggingStopStr;
        return loggingStopStr;
    }

    static int getFBMaxDailyMinutes(Context context) {
        return Store.getInt(context, Store.FB_MAX_TIME);
    }

    static int getFBMaxDailyOpens(Context context) {
        return Store.getInt(context, Store.FB_MAX_OPENS) ;
    }

    /**
     * @param context The current application context
     * @return 0(midnight) - 23(11PM)
     */
    static int getDailyResetHour(Context context) {
        int resetHour = Store.getInt(context, Store.DAILY_RESET_HOUR);
        resetHour = resetHour < 0 || resetHour > 23 ? 0 : resetHour;
        return resetHour;
    }

    static void updateStoredAdminParams(Context context, JSONObject result) {
        Store.setInt(context, Store.EXPERIMENT_GROUP, result.optInt("admin_experiment_group"));
        Store.setInt(context, Store.FB_MAX_TIME, result.optInt("admin_fb_max_mins", getFBMaxDailyMinutes(context)));
        Store.setInt(context, Store.FB_MAX_OPENS, result.optInt("admin_fb_max_opens", getFBMaxDailyOpens(context)));
        Store.setString(context, Store.TREATMENT_START, result.optString("admin_treatment_start", getTreatmentStartDateStr(context)));
        Store.setString(context, Store.FOLLOWUP_START, result.optString("admin_followup_start", getFollowupStartDateStr(context)));
        Store.setString(context, Store.LOGGING_STOP, result.optString("admin_logging_stop", getLoggingStopDateStr(context)));
        Store.setInt(context, Store.DAILY_RESET_HOUR, result.optInt("admin_daily_reset_hour", getDailyResetHour(context)));
    }

    static int getCurrentExperimentGroup(Context context) {
        int group = Store.getInt(context, Store.EXPERIMENT_GROUP);
        return group == 0 ? STATIC_GROUP : group;
    }

    static String getWorkerID(Context context) {
        return Store.getString(context, Store.WORKER_ID);
    }

    static void updateFBLimitsOfStudy(Context context, int fbTimeSpent, int fbNumOfOpens) {
        Store.setInt(context, Store.FB_MAX_TIME, fbTimeSpent);
        Store.setInt(context, Store.FB_MAX_OPENS, fbNumOfOpens);
    }

}
