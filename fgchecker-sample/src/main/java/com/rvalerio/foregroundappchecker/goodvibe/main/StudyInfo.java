package com.rvalerio.foregroundappchecker.goodvibe.main;

import android.content.Context;

import com.rvalerio.foregroundappchecker.goodvibe.helper.FileHelper;

import org.json.JSONObject;

import java.util.Date;

/**
 * Study info for experiment
 * Created by fnokeke on 3/10/17.
 */

public class StudyInfo {
    static final int CONTROL_GROUP = 0;
    static final int STATIC_GROUP = 1;
    static final int ADAPTIVE_GROUP = 2;
    static final int POPUP_ADAPTIVE_GROUP = 3;

    private static final Integer INIT_STATIC_RATIO_100 = 50;
    private static final Integer INIT_ADAPTIVE_RATIO_100 = 80;

    private static int TREATMENT_START = 8;
    private static int FOLLOWUP_START = 36;
    private static int LOGGING_STOP = 50;

    private static final int TREATMENT_START_MTURK = 8;
    private static final int FOLLOWUP_START_MTURK = 36;
    private static final int LOGGING_STOP_MTURK = 50;

    private static final int TREATMENT_START_TECHNION = 8;
    private static final int FOLLOWUP_START_TECHNION = 15;
    private static final int LOGGING_STOP_TECHNION = 22;

    private static final int TREATMENT_START_HCI = 15;
    private static final int FOLLOWUP_START_HCI = 29;
    private static final int LOGGING_STOP_HCI = 36;

    private static final int INIT_DAILY_RESET_HOUR = 0;
    private static final int INIT_FB_MAX_DAILY_MINUTES = 999;
    private static final int INIT_FB_MAX_DAILY_OPENS = 999;
    final static String FACEBOOK_PACKAGE_NAME = "com.facebook.katana";

    static void setDefaults(Context context, String studyCode) {
        setStudyPeriods(context, studyCode);

        Store.setInt(context, Store.EXPERIMENT_GROUP, CONTROL_GROUP);
        Store.setInt(context, Store.ADMIN_STATIC_RATIO_100, INIT_STATIC_RATIO_100);
        Store.setInt(context, Store.ADMIN_ADAPTIVE_RATIO_100, INIT_ADAPTIVE_RATIO_100);

        Store.setString(context, Store.TREATMENT_START, getTreatmentStartDateStr(context));
        Store.setString(context, Store.FOLLOWUP_START, getFollowupStartDateStr(context));
        Store.setString(context, Store.LOGGING_STOP, getLoggingStopDateStr(context));

        Store.setInt(context, Store.DAILY_RESET_HOUR, INIT_DAILY_RESET_HOUR);
        Store.setInt(context, Store.FB_MAX_MINUTES, INIT_FB_MAX_DAILY_MINUTES);
        Store.setInt(context, Store.FB_MAX_OPENS, INIT_FB_MAX_DAILY_OPENS);
        FileHelper.prepareAllStorageFiles(context);
    }

    private static void resetStudyPeriods(Context context) {
        Store.setString(context, Store.TREATMENT_START, "");
        Store.setString(context, Store.FOLLOWUP_START, "");
        Store.setString(context, Store.LOGGING_STOP, "");
    }

    private static void setStudyPeriods(Context context, String studyCode) {
        resetStudyPeriods(context);
        switch (studyCode) {
            case "mturk":
                TREATMENT_START = TREATMENT_START_MTURK;
                FOLLOWUP_START = FOLLOWUP_START_MTURK;
                LOGGING_STOP = LOGGING_STOP_MTURK;
                break;
            case "tech":
                TREATMENT_START = TREATMENT_START_TECHNION;
                FOLLOWUP_START = FOLLOWUP_START_TECHNION;
                LOGGING_STOP = LOGGING_STOP_TECHNION;
                break;
            case "hci":
                TREATMENT_START = TREATMENT_START_HCI;
                FOLLOWUP_START = FOLLOWUP_START_HCI;
                LOGGING_STOP = LOGGING_STOP_HCI;
                break;
        }
    }

    static void saveTodayAsExperimentJoinDate(Context context) {
        if (Store.getString(context, Store.EXPERIMENT_JOIN_DATE).equals("")) {
            Store.setString(context, Store.EXPERIMENT_JOIN_DATE, Helper.getTodayDateStr());
        }
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
        return Store.getInt(context, Store.FB_MAX_MINUTES);
    }

    static int getFBMaxDailyOpens(Context context) {
        return Store.getInt(context, Store.FB_MAX_OPENS);
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
        Store.setInt(context, Store.ADMIN_SET_FB_MAX_MINUTES, result.optInt("admin_fb_max_mins", Store.UNAVAILABLE));
        Store.setInt(context, Store.ADMIN_SET_FB_MAX_OPENS, result.optInt("admin_fb_max_opens", Store.UNAVAILABLE));
        Store.setInt(context, Store.ADMIN_ADAPTIVE_RATIO_100, result.optInt("admin_adaptive_ratio_100", Store.UNAVAILABLE));
        Store.setInt(context, Store.ADMIN_STATIC_RATIO_100, result.optInt("admin_static_ratio_100", Store.UNAVAILABLE));

        Store.setInt(context, Store.EXPERIMENT_GROUP, result.optInt("admin_experiment_group", getCurrentExperimentGroup(context)));
        Store.setString(context, Store.TREATMENT_START, result.optString("admin_treatment_start", getTreatmentStartDateStr(context)));
        Store.setString(context, Store.FOLLOWUP_START, result.optString("admin_followup_start", getFollowupStartDateStr(context)));
        Store.setString(context, Store.LOGGING_STOP, result.optString("admin_logging_stop", getLoggingStopDateStr(context)));
        Store.setInt(context, Store.DAILY_RESET_HOUR, result.optInt("admin_daily_reset_hour", getDailyResetHour(context)));
        Store.setInt(context, Store.FB_MAX_MINUTES, result.optInt("admin_fb_max_mins", getFBMaxDailyMinutes(context)));
        Store.setInt(context, Store.FB_MAX_OPENS, result.optInt("admin_fb_max_opens", getFBMaxDailyOpens(context)));
    }

    static int getCurrentExperimentGroup(Context context) {
        int group = Store.getInt(context, Store.EXPERIMENT_GROUP);
        return group == 0 ? CONTROL_GROUP : group;
    }

    static String getWorkerID(Context context) {
        return Store.getString(context, Store.WORKER_ID);
    }

    static void updateFBLimitsOfStudy(Context context, int fbTimeSpentMinutes, int fbNumOfOpens) {
        Store.setInt(context, Store.FB_MAX_MINUTES, fbTimeSpentMinutes);
        Store.setInt(context, Store.FB_MAX_OPENS, fbNumOfOpens);
    }

    public static double getRatioOfLimit(Context context) {
        int studyGroup = StudyInfo.getCurrentExperimentGroup(context);
        String key = (studyGroup == StudyInfo.STATIC_GROUP) ? Store.ADMIN_STATIC_RATIO_100 : Store.ADMIN_ADAPTIVE_RATIO_100;
        int defaultRatio100 = (studyGroup == StudyInfo.STATIC_GROUP) ? INIT_STATIC_RATIO_100 : INIT_ADAPTIVE_RATIO_100;
        int storedRatio = Store.getInt(context, key);
        storedRatio = (storedRatio == Store.UNAVAILABLE) ? defaultRatio100 : storedRatio;
        return (double) storedRatio / 100;
    }

}
