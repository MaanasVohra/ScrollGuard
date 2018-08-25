package com.rvalerio.foregroundappchecker.goodvibe.main;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.android.volley.VolleyError;
import com.google.firebase.iid.FirebaseInstanceId;
import com.rvalerio.fgchecker.AppChecker;
import com.rvalerio.foregroundappchecker.R;
import com.rvalerio.foregroundappchecker.goodvibe.api.CallAPI;
import com.rvalerio.foregroundappchecker.goodvibe.api.VolleyJsonCallback;
import com.rvalerio.foregroundappchecker.goodvibe.fcm.AppJobService;
import com.rvalerio.foregroundappchecker.goodvibe.helper.AlarmHelper;
import com.rvalerio.foregroundappchecker.goodvibe.helper.DateHelper;
import com.rvalerio.foregroundappchecker.goodvibe.helper.FileHelper;
import com.rvalerio.foregroundappchecker.goodvibe.helper.JsonHelper;
import com.rvalerio.foregroundappchecker.goodvibe.personalize.AdaptivePersonalize;
import com.rvalerio.foregroundappchecker.goodvibe.personalize.StaticPersonalize;

import org.json.JSONObject;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;


public class ForegroundToastService extends Service {
    private static String TAG = "ForegroundToastService";
    private final static String STOP_SERVICE = ForegroundToastService.class.getPackage() + ".stop";
    private final static int NOTIFICATION_ID = 1234;
    private final static String TARGET_APP_CURRENT_TIME_SPENT = "fbTimeSpent";
    private final static String TARGET_APP_CURRENT_NUM_OPENS = "fbNumOfOpens";
    private final static String TOTAL_SECONDS = "totalSeconds";
    private final static String TOTAL_OPENS = "totalOpens";
    private final static String LAST_RECORDED_DATE = "lastRecordedDate";
    private final static String STAT_TITLE = "Testing Details Mode";
//    private final static String STAT_TITLE = "Recent Usage Stats";

    private final Locale locale = Locale.getDefault();
    private Context mContext;

    private BroadcastReceiver appCheckerReceiver;
    private BroadcastReceiver screenUnlockReceiver;
    private AppChecker appChecker;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;

//        registerStopCheckerReceiver();
        registerScreenUnlockReceiver();
        startFgChecker();

        if (Build.VERSION.SDK_INT >= 26) {
            Notification notification = createNotifForAndroidO_And_Above(mContext);
            startForeground(NOTIFICATION_ID, notification);
        } else {
            updateNotification(mContext, "Your stats update during usage.");
        }
    }

    public static void startMonitoring(Context context) {
        Intent intent = new Intent(context, ForegroundToastService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void stop(Context context) {
        context.stopService(new Intent(context, ForegroundToastService.class));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= 26) {
            Notification notification = createNotifForAndroidO_And_Above(mContext);
            startForeground(NOTIFICATION_ID, notification);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ForegroundToastService.startMonitoring(mContext);
    }

    public Boolean isLockedScreen() {
        KeyguardManager myKM = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        return myKM.inKeyguardRestrictedInputMode();
    }

    private int getCurrentFBTimeSpent() {
        return Store.getInt(mContext, TARGET_APP_CURRENT_TIME_SPENT);
    }

    private int getCurrentFBNumOfOpens() {
        return Store.getInt(mContext, TARGET_APP_CURRENT_NUM_OPENS);
    }

    private static boolean workerExists(Context context) {
        return !StudyInfo.getFilledUsername(context).equals("");
    }

    private void computeAndUpdatePersonalizedFBLimits() {
        if (!workerExists(mContext)) return;

        String treatmentStartDateStr = StudyInfo.getTreatmentStartDateStr(mContext);
        int experimentGroup = StudyInfo.getCurrentExperimentGroup(mContext);
        StaticPersonalize personalize;

        switch (experimentGroup) { // explicitly handle each condition for clarity
            case StudyInfo.STATIC_GROUP:
                personalize = new StaticPersonalize(mContext, treatmentStartDateStr);
                break;
            case StudyInfo.ADAPTIVE_GROUP:
                personalize = new AdaptivePersonalize(mContext);
                break;
            case StudyInfo.POPUP_ADAPTIVE_GROUP:
                personalize = new AdaptivePersonalize(mContext);
                break;
            case StudyInfo.CONTROL_GROUP:
                personalize = new AdaptivePersonalize(mContext);
                break;
            default:
                personalize = new AdaptivePersonalize(mContext);
                break;
        }

        int timeMinutes = Math.round(getCurrentFBTimeSpent() / 60);
        personalize.addDataPoint(timeMinutes, getCurrentFBNumOfOpens());

        int timeLimit = personalize.getAverageTimeSpent();
        int openLimit = personalize.getAverageTimeOpen();
        StudyInfo.updateFBLimitsOfStudy(mContext, timeLimit, openLimit);
    }

    private String getYesterday() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        return DateHelper.dateToStr(cal.getTime());
    }

    private void updateLastDate() {
        boolean shouldUpdate = isNewDay() && isPastDailyResetHour();
        if (shouldUpdate) {
            long timeMillis = System.currentTimeMillis();
            String fbDate = getYesterday();
            int timeSpent = getCurrentFBTimeSpent();
            int timeOpen = getCurrentFBNumOfOpens();

            String data = String.format(locale, "%d, %s, %d, %d;\n", timeMillis, fbDate, timeSpent, timeOpen);
            FileHelper.appendToFile(mContext, Store.FB_LOGS_CSV, data);
            computeAndUpdatePersonalizedFBLimits();

            Store.setString(mContext, LAST_RECORDED_DATE, Helper.getTodayDateStr());
            Store.setInt(mContext, TARGET_APP_CURRENT_TIME_SPENT, 0);
            Store.setInt(mContext, TARGET_APP_CURRENT_NUM_OPENS, 0);
            Store.setInt(mContext, TOTAL_SECONDS, 0);
            Store.setInt(mContext, TOTAL_OPENS, 0);
            AppJobService.updateServerThroughFirebaseJob(mContext);

            ConfigActivity.refreshAppList(mContext);
        }
    }

    private boolean isNewDay() {
        String lastRecordedDate = Store.getString(mContext, LAST_RECORDED_DATE);
        String today = DateHelper.getTodayDateStr();
        return !lastRecordedDate.equals(today);
    }

    private boolean isPastDailyResetHour() {
        Calendar rightNow = Calendar.getInstance();
        Integer hour24 = rightNow.get(Calendar.HOUR_OF_DAY);
        return hour24 >= StudyInfo.getDailyResetHour(mContext);
    }


    private void startFgChecker() {
        appChecker = AppChecker.getInstance();
        appChecker.other(new AppChecker.Listener() {
            @Override
            public void onForeground(String packageName) {
                updateLastDate();

                if (isLockedScreen()) return;
                if (packageName == null) return;

//                if (packageName.equals(Store.getString(mContext, Constants.CHOSEN_APP_ID))) {
//                    doTargetAppOperation(packageName);
//                } else {
//                    Store.setBoolean(mContext, "visitedAnotherApp", true);
//                }

                recordAppUsageStats(packageName, !packageName.equals(getLastFgApp()));
                recordSessionStats(packageName);
                setLastFgApp(packageName);
                updateNotification(mContext, getCurrentStatsForChosenApp(packageName, packageName));
                doTargetAppOperation(packageName);
            }

        }).timeout(5000).start(this);
    }

    private void recordAppUsageStats(String packageName, Boolean visitedAnotherApp) {
        JSONObject timeSpentList = Store.getJsonObject(mContext, Constants.STORED_APPS_TIME_SPENT);
        if (!timeSpentList.optString(packageName).equals("")) {
            JsonHelper.setJSONValue(timeSpentList, packageName, 5 + timeSpentList.optInt(packageName));
            Store.setJsonObject(mContext, Constants.STORED_APPS_TIME_SPENT, timeSpentList);

            if (visitedAnotherApp) {
                JSONObject numOpenList = Store.getJsonObject(mContext, Constants.STORED_APPS_NUM_OPENS);
                JsonHelper.setJSONValue(numOpenList, packageName, 1 + numOpenList.optInt(packageName));
                Store.setJsonObject(mContext, Constants.STORED_APPS_NUM_OPENS, numOpenList);
            }
        }

    }

    private void recordSessionStats(String packageName) {
        int timer = 0;
        if (packageName.equals(getLastFgApp())) {
            timer = Store.getInt(mContext, packageName);
        } else {
            String lastAppId = getLastFgApp();
            int lastAppTimeSpent = Store.getInt(mContext, lastAppId);
            String data = String.format(locale, "%s, %d, %d;\n", lastAppId, lastAppTimeSpent, System.currentTimeMillis());
            FileHelper.appendToFile(mContext, Store.APP_LOGS_CSV, data);
        }

        timer += 5;
        Store.setInt(mContext, packageName, timer);
        Store.increaseInt(mContext, TOTAL_SECONDS, 5);
    }

    private void recordTimeSpentOld(String packageName) {
        int timer = 0;
        if (packageName.equals(getLastFgApp())) {
            timer = Store.getInt(mContext, packageName);
        } else {
            String lastAppId = getLastFgApp();
            int lastAppTimeSpent = Store.getInt(mContext, lastAppId);
            String data = String.format(locale, "%s, %d, %d;\n", lastAppId, lastAppTimeSpent, System.currentTimeMillis());
            FileHelper.appendToFile(mContext, Store.APP_LOGS_CSV, data);
        }

        timer += 5;
        Store.setInt(mContext, packageName, timer);
        setLastFgApp(packageName);
        Store.increaseInt(mContext, TOTAL_SECONDS, 5);


//        String msg = String.format(locale, "%s: %d secs.", packageName, timer);
//        updateNotification(mContext, msg);

//        if (!packageName.equals(StudyInfo.FACEBOOK_PACKAGE_NAME)) {
////            String msg = String.format(locale, "%s: %d secs.", packageName, timer);
////            updateNotification(mContext, msg);
//            updateNotification(mContext, getCurrentStatsForChosenApp());
//        }
    }

    private String getLastFgApp() {
        return Store.getString(mContext, Constants.LAST_FG_APP);
    }

    private void setLastFgApp(String packageName) {
        Store.setString(mContext, Constants.LAST_FG_APP, packageName);
    }

    private void doTargetAppOperation(String packageName) {
        String chosenAppId = Store.getString(mContext, Constants.CHOSEN_APP_ID);
        if (chosenAppId.equals(packageName)) {
            JSONObject timeSpentList = Store.getJsonObject(mContext, Constants.STORED_APPS_TIME_SPENT);
            JSONObject numOpenList = Store.getJsonObject(mContext, Constants.STORED_APPS_NUM_OPENS);
            Integer targetTimeSpent = timeSpentList.optInt(packageName);
            Integer targetNumOpens = numOpenList.optInt(packageName);

            int timeLimit = Store.getInt(mContext, Constants.CHOSEN_TIME_LIMIT) * 60;
            int opensLimit = Store.getInt(mContext, Constants.CHOSEN_OPEN_LIMIT);
            if (targetTimeSpent > timeLimit || targetNumOpens > opensLimit) {
                doChosenIntervention();
            }
        }


//        Store.increaseInt(mContext, TARGET_APP_CURRENT_TIME_SPENT, 5);
//
//        if (Store.getBoolean(mContext, "visitedAnotherApp")) {
//            Store.increaseInt(mContext, TARGET_APP_CURRENT_NUM_OPENS, 1);
//            Store.setBoolean(mContext, "visitedAnotherApp", false);
//        }
//
//        int targetAppTimeSpent = Store.getInt(mContext, TARGET_APP_CURRENT_TIME_SPENT);
//        int targetAppNumOfOpens = Store.getInt(mContext, TARGET_APP_CURRENT_NUM_OPENS);
//        if (targetAppTimeSpent > 0 && targetAppNumOfOpens == 0) {
//            targetAppNumOfOpens += 1;
//            Store.increaseInt(mContext, TARGET_APP_CURRENT_NUM_OPENS, 1);
//        }
//
    }

    private void doTargetAppOperationOld() {
        Store.increaseInt(mContext, TARGET_APP_CURRENT_TIME_SPENT, 5);

        if (Store.getBoolean(mContext, "visitedAnotherApp")) {
            Store.increaseInt(mContext, TARGET_APP_CURRENT_NUM_OPENS, 1);
            Store.setBoolean(mContext, "visitedAnotherApp", false);
        }

        int targetAppTimeSpent = Store.getInt(mContext, TARGET_APP_CURRENT_TIME_SPENT);
        int targetAppNumOfOpens = Store.getInt(mContext, TARGET_APP_CURRENT_NUM_OPENS);
        if (targetAppTimeSpent > 0 && targetAppNumOfOpens == 0) {
            targetAppNumOfOpens += 1;
            Store.increaseInt(mContext, TARGET_APP_CURRENT_NUM_OPENS, 1);
        }

        int timeLimit = Store.getInt(mContext, Constants.CHOSEN_TIME_LIMIT) * 60;
        int opensLimit = Store.getInt(mContext, Constants.CHOSEN_OPEN_LIMIT);
        if (targetAppTimeSpent > timeLimit || targetAppNumOfOpens > opensLimit) {
            doChosenIntervention();
        }
    }

    private void doChosenIntervention() {
        int targetAppTimeSpent = Store.getInt(mContext, TARGET_APP_CURRENT_TIME_SPENT);
        int freq = getChosenFreqStyle();
        if (targetAppTimeSpent % freq != 0) {
            return;
        }

        String appLabel = Store.getString(mContext, Constants.CHOSEN_APP_LABEL);
        String reminderMode = Store.getString(mContext, Constants.CHOSEN_REMINDER_MODE);
        String msg;

        if (reminderMode.contains("Popup")) {
            msg = String.format("%s limit exceeded.", appLabel);
            AlarmHelper.showCenterToast(mContext, msg);
        } else {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            long[] pattern = {0, 100, 100, 100, 100};
            if (v != null) {
                v.vibrate(pattern, -1);
            }
        }

    }

    private int getChosenFreqStyle() {
        String freqStyle = Store.getString(mContext, Constants.CHOSEN_FREQ_STYLE);
        int result = 5;
        if (freqStyle.contains("minute")) {
            result = 60;
        } else if (freqStyle.contains("30")) {
            result = 30;
        }
        return result;
    }

//        checkAndActivateIfShouldSubmitID(fbTimeSpent, fbNumOfOpens);
//        if (fbTimeSpent > StudyInfo.getFBMaxDailyMinutes(mContext) * 60) {
//            if (isTreatmentPeriod() && !isControlGroup()) {
//                vibrateOrPopup();
//            }
//        }


    private boolean isControlGroup() {
        return StudyInfo.getCurrentExperimentGroup(mContext) == StudyInfo.CONTROL_GROUP;
    }

    private void vibrateOrPopup() {
        int experimentGroup = StudyInfo.getCurrentExperimentGroup(mContext);
        if (experimentGroup == 3) {
            AlarmHelper.showCenterToast(mContext, "Facebook Limit Exceeded.");
        } else {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            long[] pattern = {0, 100, 100, 100, 100};
            v.vibrate(pattern, -1);
        }
    }

    private void checkAndActivateIfShouldSubmitID(int fbTimeSpent, int fbNumOfOpens) {
        AlarmHelper.showInstantNotif(mContext, "Successful activation!", "Tap to submit your ID.", "io.smalldata.goodvibe", 5005);
        Store.setBoolean(mContext, Store.CAN_SHOW_SUBMIT_BTN, true);
//        if (!Store.getBoolean(mContext, Store.ENROLLED)) {
//            if (fbTimeSpent >= 0 && fbNumOfOpens >= 1) {
//                updateNotification(mContext, "Successful! Submit ID.");
//                AlarmHelper.showInstantNotif(mContext, "Successful activation!", "Tap to submit your ID.", "io.smalldata.goodvibe", 5005);
//                Store.setBoolean(mContext, Store.CAN_SHOW_SUBMIT_BTN, true);
//            }
//        }
    }

    private boolean isTreatmentPeriod() {
        long rightNow = Calendar.getInstance().getTimeInMillis();
        Date treatmentStart = Helper.strToDate(StudyInfo.getTreatmentStartDateStr(mContext));
        Date followupStart = Helper.strToDate(StudyInfo.getFollowupStartDateStr(mContext));
        return rightNow > treatmentStart.getTime() && rightNow < followupStart.getTime();
    }

    private static boolean shouldStopServerLogging(Context context) {
        long rightNow = Calendar.getInstance().getTimeInMillis();
        Date loggingStop = Helper.strToDate(StudyInfo.getLoggingStopDateStr(context));
        return rightNow > loggingStop.getTime();
    }

    private static boolean stopDateExists(Context context) {
        return !StudyInfo.getLoggingStopDateStr(context).equals("");
    }

    public static void updateServerRecords(Context context) {
//        if (!NetworkHelper.isDeviceOnline(context)) return;
//        if (!workerExists(context)) return;
//        if (!stopDateExists(context)) return;
//        if (shouldStopServerLogging(context)) {
//            updateNotification(context, "Experiment has ended. Please Uninstall app.");
//            return;
//        }
//        sendFacebookLogsThenUpdateAdminState(context);
//        sendCurrentUserStatsThenUpdateAdminState(context);
        sendFgAppLogs(context);
        sendScreenEventLogs(context);
//        sendPhoneNotifLogs(context);
    }

    private static void sendCurrentUserStatsThenUpdateAdminState(Context context) {
        JSONObject params = getFBParams(context);
        CallAPI.submitFBAndOtherCurrentStats(context, params, getFBResponseHandler(context));
    }

    private static void sendFacebookLogsThenUpdateAdminState(Context context) {
        JSONObject params = getLogParams(context, Store.FB_LOGS_CSV);
        CallAPI.submitFacebookLogs(context, params, getLogResponseHandler(context, Store.FB_LOGS_CSV, true));
    }

    private static void sendFgAppLogs(Context context) {
        JSONObject params = getLogParams(context, Store.APP_LOGS_CSV);
        CallAPI.submitAppLogs(context, params, getLogResponseHandler(context, Store.APP_LOGS_CSV, false));
    }

    private static void sendScreenEventLogs(Context context) {
        JSONObject params = getLogParams(context, Store.SCREEN_LOGS_CSV);
        CallAPI.submitScreenEventLogs(context, params, getLogResponseHandler(context, Store.SCREEN_LOGS_CSV, false));
    }

    public static void sendPhoneNotifLogs(Context context) {
        JSONObject params = getLogParams(context, Store.PHONE_NOTIF_LOGS_CSV);
        CallAPI.submitPhoneNotifLogs(context, params, getLogResponseHandler(context, Store.PHONE_NOTIF_LOGS_CSV, false));
    }

    private static JSONObject getLogParams(Context context, String filename) {
        JSONObject params = new JSONObject();
        JsonHelper.setJSONValue(params, "worker_id", Store.getString(context, Store.BUNDLE_USERNAME));
        JsonHelper.setJSONValue(params, "code", Store.getString(context, Store.BUNDLE_CODE));
        JsonHelper.setJSONValue(params, "logs", FileHelper.readFromFile(context, filename));
        return params;
    }

    private static JSONObject getFBParams(Context context) {
        JSONObject params = new JSONObject();
        Helper.setJSONValue(params, "worker_id", Store.getString(context, Store.WORKER_ID));
        int totalSeconds = Store.getInt(context, TOTAL_SECONDS);
        int totalOpens = Store.getInt(context, TOTAL_OPENS);
        if (totalSeconds > 0 && totalOpens == 0) {
            totalOpens = 1;
        }

        Helper.setJSONValue(params, "total_seconds", totalSeconds);
        Helper.setJSONValue(params, "total_opens", totalOpens);
        Helper.setJSONValue(params, "time_spent", Store.getInt(context, TARGET_APP_CURRENT_TIME_SPENT));
        Helper.setJSONValue(params, "time_open", Store.getInt(context, TARGET_APP_CURRENT_NUM_OPENS));
        Helper.setJSONValue(params, "ringer_mode", DeviceInfo.getRingerMode(context));
        Helper.setJSONValue(params, "daily_reset_hour", StudyInfo.getDailyResetHour(context));

        Helper.setJSONValue(params, "current_static_ratio_100", Store.getInt(context, Store.ADMIN_STATIC_RATIO_100));
        Helper.setJSONValue(params, "current_adaptive_ratio_100", Store.getInt(context, Store.ADMIN_ADAPTIVE_RATIO_100));
        Helper.setJSONValue(params, "current_ratio_of_limit", StudyInfo.getRatioOfLimit(context));
        Helper.setJSONValue(params, "current_experiment_group", StudyInfo.getCurrentExperimentGroup(context));
        Helper.setJSONValue(params, "current_firebase_token", FirebaseInstanceId.getInstance().getToken());
        Helper.setJSONValue(params, "current_fb_max_mins", StudyInfo.getFBMaxDailyMinutes(context));
        Helper.setJSONValue(params, "current_fb_max_opens", StudyInfo.getFBMaxDailyOpens(context));
        Helper.setJSONValue(params, "current_treatment_start", StudyInfo.getTreatmentStartDateStr(context));
        Helper.setJSONValue(params, "current_followup_start", StudyInfo.getFollowupStartDateStr(context));
        Helper.setJSONValue(params, "current_logging_stop", StudyInfo.getLoggingStopDateStr(context));

        Helper.setJSONValue(params, "time_spent_list", Store.getString(context, StaticPersonalize.ALL_TIME_SPENT));
        Helper.setJSONValue(params, "num_opens_list", Store.getString(context, StaticPersonalize.ALL_NUM_OF_OPENS));
        Helper.setJSONValue(params, "local_time", DateHelper.currentMillisToDateFormat());

        return params;
    }

    public static VolleyJsonCallback getFBResponseHandler(final Context context) {
        return new VolleyJsonCallback() {
            @Override
            public void onConnectSuccess(JSONObject result) {
                Log.i(TAG, "Submit Stats: " + result.toString());
                StudyInfo.updateStoredAdminParams(context, result);
            }

            @Override
            public void onConnectFailure(VolleyError error) {
                String msg = "Stats submit error: " + error.toString();
                Log.e(TAG, "StatsError: " + msg);
            }
        };
    }

    private static VolleyJsonCallback getLogResponseHandler(final Context context, final String filenameToReset, final boolean hasAdminInfo) {
        return new VolleyJsonCallback() {
            @Override
            public void onConnectSuccess(JSONObject result) {
                Log.i(TAG, filenameToReset + " Submit Response: " + result.toString());
                FileHelper.resetFile(context, filenameToReset);
                if (hasAdminInfo) {
                    StudyInfo.updateStoredAdminParams(context, result);
                }
            }

            @Override
            public void onConnectFailure(VolleyError error) {
                String msg = "Stats submit error: " + error.toString();
                Log.e(TAG, filenameToReset + " StatsError: " + msg);
            }
        };

    }

    private String getCurrentStatsForChosenApp(String packageName, String appLabel) {
//        String packageName = Store.getString(mContext, Constants.CHOSEN_APP_ID);
//        String appLabel = Store.getString(mContext, Constants.CHOSEN_APP_LABEL);
        JSONObject timeSpentList = Store.getJsonObject(mContext, Constants.STORED_APPS_TIME_SPENT);
        JSONObject numOpenList = Store.getJsonObject(mContext, Constants.STORED_APPS_NUM_OPENS);
        Integer targetTimeSpent = timeSpentList.optInt(packageName);
        Integer targetNumOpens = numOpenList.optInt(packageName);
        return String.format("%s: %s secs (%sx)", appLabel, targetTimeSpent.toString(), targetNumOpens.toString());

    }

    private String getCurrentStatsForChosenApp() {
        String packageName = Store.getString(mContext, Constants.CHOSEN_APP_ID);
        JSONObject timeSpentList = Store.getJsonObject(mContext, Constants.STORED_APPS_TIME_SPENT);
        JSONObject numOpenList = Store.getJsonObject(mContext, Constants.STORED_APPS_NUM_OPENS);
        Integer targetTimeSpent = timeSpentList.optInt(packageName);
        Integer targetNumOpens = numOpenList.optInt(packageName);
        String appLabel = Store.getString(mContext, Constants.CHOSEN_APP_LABEL);
        return String.format("%s: %s secs (%sx)", appLabel, targetTimeSpent.toString(), targetNumOpens.toString());
    }

    private void registerStopCheckerReceiver() {
        appCheckerReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                stopSelf();
            }
        };
        registerReceiver(appCheckerReceiver, new IntentFilter(STOP_SERVICE));
    }

    private void registerScreenUnlockReceiver() {
        screenUnlockReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateScreenLog(context, intent);
                String screenEvent = intent.getAction();
                if (screenEvent.equals(Intent.ACTION_USER_PRESENT)) { //unlock
                    Log.d(TAG, "Phone unlocked");
                    if (Store.getString(context, Store.LAST_SCREEN_EVENT).equals(Intent.ACTION_SCREEN_OFF)) {
                        Store.increaseInt(mContext, TOTAL_OPENS, 1);
                    }
                } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) { //lock
                    Log.d(TAG, "Phone locked");
                }
                Store.setString(context, Store.LAST_SCREEN_EVENT, screenEvent);
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenUnlockReceiver, filter);
    }

    private void updateScreenLog(Context context, Intent intent) {
        String screenAction = intent.getAction();
        String event = "";

        if (screenAction != null) {
            switch (screenAction) {
                case Intent.ACTION_USER_PRESENT:
                    event = "open";
                    break;
                case Intent.ACTION_SCREEN_OFF:
                    event = "lock";
                    break;
                default:
                    event = getLast15Chars(screenAction);
                    break;
            }
        }

        String data = String.format(locale, "%s, %d;\n", event, System.currentTimeMillis());
        FileHelper.appendToFile(context, Store.SCREEN_LOGS_CSV, data);
    }

    private String getLast15Chars(String myString) {
        final int N = 15;
        if (myString.length() > N) {
            return myString.substring(myString.length() - N);
        } else {
            return myString;
        }
    }

    private static Notification createNotifForAndroidO_And_Above(Context context) {
        String message = "Your stats update during usage";
        return createNotifForAndroidO_And_Above(context, STAT_TITLE, message);
    }



    private static Notification createNotifForAndroidO_And_Above(Context context, String title, String message) {
        Notification notification = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            String channelId = "goodvibeId";
            String channelName = "Goodvibe Logger";
            int importance = NotificationManager.IMPORTANCE_MIN;
            NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
            notificationManager.createNotificationChannel(channel);

            notification = new NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.drawable.ic_chart_pink)
                    .setOngoing(true)
//                    .setContentTitle(title)
//                    .setContentText(message)
                    .setContentText("Logged into active mode.")
                    .setContentIntent(getPendingIntent(context))
                    .build();

            notificationManager.notify(NOTIFICATION_ID, notification);
        }
        return notification;
    }

    private static PendingIntent getPendingIntent(Context context) {
        Intent notifyIntent = new Intent(context, MainActivity.class);
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return PendingIntent.getActivity(context, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }


    private static void updateNotification(Context context, String title, String message) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            createNotifForAndroidO_And_Above(context, title, message);
        } else {
            NotificationCompat.Builder mBuilder;
            mBuilder = new NotificationCompat.Builder(context);
            Notification notification = mBuilder.setSmallIcon(R.drawable.ic_chart_pink)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setOngoing(true)
//                    .setContentTitle(title)
//                    .setContentText(message)
                    .setContentText("Logged into active mode.")
                    .setContentIntent(getPendingIntent(context))
                    .build();
            notificationManager.notify(NOTIFICATION_ID, notification);
        }

    }

    private static void updateNotification(Context context, String message) {
        updateNotification(context, STAT_TITLE, message);
    }

    private void stopChecker() {
        appChecker.stop();
    }

    private void unregisterAllReceivers() {
        unregisterReceiver(screenUnlockReceiver);
        unregisterReceiver(appCheckerReceiver);
    }

    private void removeNotification() {
        NotificationManager manager = ((NotificationManager) getSystemService(NOTIFICATION_SERVICE));
        manager.cancel(NOTIFICATION_ID);
    }

}
