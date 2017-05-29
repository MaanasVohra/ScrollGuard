package com.rvalerio.foregroundappchecker;

import android.content.Context;

import org.json.JSONObject;

import java.util.Date;

/**
 * Study info for experiment
 * Created by fnokeke on 3/10/17.
 */

class StudyInfo {
    private static final int TREATMENT_START = 9;
    private static final int FOLLOWUP_START = 17;
    private static final int LOGGING_STOP = 25;
    private static final int TEMP_FB_MAX_DAILY_MINUTES = 10;
    private static final int TEMP_FB_MAX_DAILY_OPENS = 2;
    final static String FACEBOOK_PACKAGE_NAME = "com.facebook.katana";
    final static String GMAIL_PACKAGE_NAME = "com.google.android.gm";
    final static String INSTAGRAM_PACKAGE_NAME = "com.instagram.android";
    final static String PINTEREST_PACKAGE_NAME = "com.pinterest";
    final static String SNAPCHAT_PACKAGE_NAME = "com.snapchat.android";
    final static String WHATSAPP_PACKAGE_NAME = "com.whatsapp";
    final static String YOUTUBE_PACKAGE_NAME = "com.google.android.youtube";

    static void saveTodayAsExperimentJoinDate(Context context) {
        Store.setStoreString(context, Store.EXPERIMENT_JOIN_DATE, Helper.getTodayDateStr());
    }

    private static String countFromJoinDate(Context context, int noOfDays) {
        String joinDateStr = Store.getStoreString(context, Store.EXPERIMENT_JOIN_DATE);
        Date treatmentStart = Helper.strToDate(joinDateStr);
        treatmentStart = Helper.addDays(treatmentStart, noOfDays);
        return Helper.dateToStr(treatmentStart);
    }

    static String getTreatmentStartDateStr(Context context) {
        String treatmentStartStr = Store.getStoreString(context, Store.TREATMENT_START);
        treatmentStartStr = treatmentStartStr.equals("") ? countFromJoinDate(context, TREATMENT_START) : treatmentStartStr;
       return treatmentStartStr;
    }

    static String getFollowupStartDateStr(Context context) {
       String followupStartStr = Store.getStoreString(context, Store.FOLLOWUP_START) ;
        followupStartStr = followupStartStr.equals("") ? countFromJoinDate(context, FOLLOWUP_START) : followupStartStr;
        return followupStartStr;
    }

    static String getLoggingStopDateStr(Context context) {
        String loggingStopStr = Store.getStoreString(context, Store.LOGGING_STOP) ;
        loggingStopStr = loggingStopStr.equals("") ? countFromJoinDate(context, LOGGING_STOP) : loggingStopStr;
        return loggingStopStr;
    }

    static int getFBMaxDailyMinutes(Context context) {
        Integer fbMaxTime = Store.getStoreInt(context, Store.FB_MAX_TIME);
        fbMaxTime = fbMaxTime == 0 ? TEMP_FB_MAX_DAILY_MINUTES : fbMaxTime;
        return fbMaxTime;
    }

    static int getFBMaxDailyOpens(Context context) {
        Integer fbMaxOpens = Store.getStoreInt(context, Store.FB_MAX_OPENS) ;
        fbMaxOpens = fbMaxOpens == 0 ? TEMP_FB_MAX_DAILY_OPENS : fbMaxOpens;
        return fbMaxOpens;
    }

    /**
     * @param context The current application context
     * @return 0(midnight) - 23(11PM)
     */
    static int getDailyResetHour(Context context) {
        int resetHour = Store.getStoreInt(context, Store.DAILY_RESET_HOUR);
        resetHour = resetHour < 0 || resetHour > 23 ? 0 : resetHour;
        return resetHour;
    }

    static void updateStoredAdminParams(Context context, JSONObject result) {
        Store.setStoreInt(context, Store.EXPERIMENT_GROUP, result.optInt("admin_experiment_group"));
        Store.setStoreInt(context, Store.FB_MAX_TIME, result.optInt("admin_fb_max_mins"));
        Store.setStoreInt(context, Store.FB_MAX_OPENS, result.optInt("admin_fb_max_opens"));
        Store.setStoreString(context, Store.TREATMENT_START, result.optString("admin_treatment_start"));
        Store.setStoreString(context, Store.FOLLOWUP_START, result.optString("admin_followup_start"));
        Store.setStoreString(context, Store.LOGGING_STOP, result.optString("admin_logging_stop"));
        Store.setStoreInt(context, Store.DAILY_RESET_HOUR, result.optInt("admin_daily_reset_hour"));
    }

    static int getCurrentExperimentGroup(Context context) {
        int group = Store.getStoreInt(context, Store.EXPERIMENT_GROUP);
        if (group == 0) {
            group = 1;
        }
        return group;
    }

    static String getWorkerID(Context context) {
        return Store.getStoreString(context, Store.WORKER_ID);
    }
}
