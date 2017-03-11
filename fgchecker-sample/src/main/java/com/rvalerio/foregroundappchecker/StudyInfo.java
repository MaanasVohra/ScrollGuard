package com.rvalerio.foregroundappchecker;

import android.content.Context;

import org.json.JSONObject;

/**
 * Study info for experiment
 * Created by fnokeke on 3/10/17.
 */

class StudyInfo {
    private static final String TEMP_TREATMENT_START = "2017-03-20";
    private static final String TEMP_FOLLOWUP_START = "2017-03-27";
    private static final String TEMP_LOGGING_STOP = "2017-04-17";
    private static final int TEMP_FB_MAX_DAILY_MINUTES = 10;
    private static final int TEMP_FB_MAX_DAILY_OPENS = 5;

    static String getTreatmentStartDateStr(Context context) {
        String treatmentStartStr = Store.getStoreString(context, Store.TREATMENT_START);
        treatmentStartStr = treatmentStartStr.equals("") ? TEMP_TREATMENT_START : treatmentStartStr;
       return treatmentStartStr;
    }

    static String getFollowupStartDateStr(Context context) {
       String followupStartStr = Store.getStoreString(context, Store.FOLLOWUP_START) ;
        followupStartStr = followupStartStr.equals("") ? TEMP_FOLLOWUP_START : followupStartStr;
        return followupStartStr;
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

    static String getLoggingStopDateStr(Context context) {
        String loggingStopStr = Store.getStoreString(context, Store.LOGGING_STOP) ;
        loggingStopStr = loggingStopStr.equals("") ? TEMP_LOGGING_STOP : loggingStopStr;
        return loggingStopStr;
    }

    static void setParams(Context context, JSONObject result) {
        Store.setStoreString(context, Store.TREATMENT_START, result.optString("server_treatment_start_date"));
        Store.setStoreString(context, Store.FOLLOWUP_START, result.optString("server_followup_start_date"));
        Store.setStoreString(context, Store.LOGGING_STOP, result.optString("server_logging_stop_date"));
        Store.setStoreInt(context, Store.FB_MAX_TIME, result.optInt("server_fb_max_time"));
        Store.setStoreInt(context, Store.FB_MAX_OPENS, result.optInt("server_fb_max_opens"));
        Store.setStoreInt(context, Store.EXPERIMENT_GROUP, result.optInt("experiment_group"));
    }
}
