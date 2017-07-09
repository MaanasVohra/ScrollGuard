package com.rvalerio.foregroundappchecker.goodvibe.main;

import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
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
import com.rvalerio.foregroundappchecker.goodvibe.helper.NetworkHelper;
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
    private final static String FB_CURRENT_TIME_SPENT = "fbTimeSpent";
    private final static String FB_CURRENT_NUM_OF_OPENS = "fbNumOfOpens";
    private final static String TOTAL_SECONDS = "totalSeconds";
    private final static String TOTAL_OPENS = "totalOpens";
    private final static String LAST_RECORDED_DATE = "lastRecordedDate";

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

        registerStopCheckerReceiver();
        registerScreenUnlockReceiver();

        startChecker();
        updateNotification(mContext, "Your stats updates during usage.");
    }

    public static void startMonitoringFacebookUsage(Context context) {
        ForegroundToastService.start(context);
    }

    public static void start(Context context) {
        context.startService(new Intent(context, ForegroundToastService.class));
    }

    public static void stop(Context context) {
        context.stopService(new Intent(context, ForegroundToastService.class));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ForegroundToastService.start(mContext);
    }

    public Boolean isLockedScreen() {
        KeyguardManager myKM = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        return myKM.inKeyguardRestrictedInputMode();
    }

    private int getCurrentFBTimeSpent() {
        return Store.getInt(mContext, FB_CURRENT_TIME_SPENT);
    }

    private int getCurrentFBNumOfOpens() {
        return Store.getInt(mContext, FB_CURRENT_NUM_OF_OPENS);
    }

    private static boolean workerExists(Context context) {
        return !StudyInfo.getWorkerID(context).equals("");
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
            FileHelper.appendToFile(mContext, Store.FB_LOGS_CSV_FILENAME, data);
            computeAndUpdatePersonalizedFBLimits();

            Store.setString(mContext, LAST_RECORDED_DATE, Helper.getTodayDateStr());
            Store.setInt(mContext, FB_CURRENT_TIME_SPENT, 0);
            Store.setInt(mContext, FB_CURRENT_NUM_OF_OPENS, 0);
            Store.setInt(mContext, TOTAL_SECONDS, 0);
            Store.setInt(mContext, TOTAL_OPENS, 0);
            AppJobService.updateServerThroughFirebaseJob(mContext);
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

    private void startChecker() {
        appChecker = AppChecker.getInstance();
        appChecker.other(new AppChecker.Listener() {
            @Override
            public void onForeground(String packageName) {
                updateLastDate();

                if (isLockedScreen()) return;

                if (packageName == null) {
                    return;
                }

                if (packageName.equals(StudyInfo.FACEBOOK_PACKAGE_NAME)) {
                    doFacebookOperation();
                } else {
                    Store.setBoolean(mContext, "fbVisitedAnotherApp", true);
                }

                recordTimeSpent(packageName);
            }

        }).timeout(5000).start(this);
    }

    private void recordTimeSpent(String packageName) {
        int timer = 0;
        if (packageName.equals(getLastFgApp())) {
            timer = Store.getInt(mContext, packageName);
        } else {
            String lastAppId = getLastFgApp();
            int lastAppTimeSpent = Store.getInt(mContext, lastAppId);
            String data = String.format(locale, "%s, %d, %d;\n", lastAppId, lastAppTimeSpent, System.currentTimeMillis());
            FileHelper.appendToFile(mContext, Store.APP_LOGS_CSV_FILENAME, data);
        }

        timer += 5;
        if (!packageName.equals(StudyInfo.FACEBOOK_PACKAGE_NAME)) {
//            String msg = String.format(locale, "%s: %d secs.", packageName, timer);
//            updateNotification(mContext, msg);
            updateNotification(mContext, getCurrentStats());
        }
        Store.setInt(mContext, packageName, timer);
        setLastFgApp(packageName);
        Store.increaseInt(mContext, TOTAL_SECONDS, 5);
    }

    private String getLastFgApp() {
        return Store.getString(mContext, "lastFgApp");
    }

    private void setLastFgApp(String packageName) {
        Store.setString(mContext, "lastFgApp", packageName);
    }

    private void doFacebookOperation() {
        Store.increaseInt(mContext, FB_CURRENT_TIME_SPENT, 5);

        if (Store.getBoolean(mContext, "fbVisitedAnotherApp")) {
            Store.increaseInt(mContext, FB_CURRENT_NUM_OF_OPENS, 1);
            Store.setBoolean(mContext, "fbVisitedAnotherApp", false);
        }

        int fbTimeSpent = Store.getInt(mContext, FB_CURRENT_TIME_SPENT);
        int fbNumOfOpens = Store.getInt(mContext, FB_CURRENT_NUM_OF_OPENS);
        if (fbTimeSpent > 0 && fbNumOfOpens == 0) {
            fbNumOfOpens += 1;
            Store.increaseInt(mContext, FB_CURRENT_NUM_OF_OPENS, 1);
        }

        updateNotification(mContext, getCurrentStats());
        checkAndActivateIfShouldSubmitID(fbTimeSpent, fbNumOfOpens);
        if (fbTimeSpent > StudyInfo.getFBMaxDailyMinutes(mContext) * 60) {
            if (isTreatmentPeriod() && !isControlGroup()) {
                vibrateOrPopup();
            }
        }

    }

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
        if (!Store.getBoolean(mContext, Store.ENROLLED)) {
            if (fbTimeSpent >= 0 && fbNumOfOpens >= 1) {
                updateNotification(mContext, "Successful! Submit ID.");
                AlarmHelper.showInstantNotif(mContext, "Successful activation!", "Tap to submit your ID.", "io.smalldata.goodvibe", 5005);
                Store.setBoolean(mContext, Store.CAN_SHOW_SUBMIT_BTN, true);
            }
        }
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
        if (!NetworkHelper.isDeviceOnline(context)) return;
        if (!workerExists(context)) return;
        if (!stopDateExists(context)) return;
        if (shouldStopServerLogging(context)) {
            updateNotification(context, "Experiment has ended. Please Uninstall app.");
            return;
        }
        sendCurrentUserStatsThenUpdateAdminState(context);
        sendFacebookLogsThenUpdateAdminState(context);
        sendFgAppLogs(context);
        sendScreenEventLogs(context);
    }

    private static void sendCurrentUserStatsThenUpdateAdminState(Context context) {
        JSONObject params = getFBParams(context);
        CallAPI.submitFBAndOtherCurrentStats(context, params, getFBResponseHandler(context));
    }

    private static void sendFacebookLogsThenUpdateAdminState(Context context) {
        JSONObject params = getLogParams(context, Store.FB_LOGS_CSV_FILENAME);
        CallAPI.submitFacebookLogs(context, params, getLogResponseHandler(context, Store.FB_LOGS_CSV_FILENAME, true));
    }

    private static void sendFgAppLogs(Context context) {
        JSONObject params = getLogParams(context, Store.APP_LOGS_CSV_FILENAME);
        CallAPI.submitAppLogs(context, params, getLogResponseHandler(context, Store.APP_LOGS_CSV_FILENAME, false));
    }

    private static void sendScreenEventLogs(Context context) {
        JSONObject params = getLogParams(context, Store.SCREEN_LOGS_CSV_FILENAME);
        CallAPI.submitScreenEventLogs(context, params, getLogResponseHandler(context, Store.SCREEN_LOGS_CSV_FILENAME, false));
    }

    private static JSONObject getLogParams(Context context, String filename) {
        JSONObject params = new JSONObject();
        JsonHelper.setJSONValue(params, "worker_id", StudyInfo.getWorkerID(context));
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
        Helper.setJSONValue(params, "time_spent", Store.getInt(context, FB_CURRENT_TIME_SPENT));
        Helper.setJSONValue(params, "time_open", Store.getInt(context, FB_CURRENT_NUM_OF_OPENS));
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

    private String getCurrentStats() {
        Integer fbTimeSpent = Store.getInt(mContext, FB_CURRENT_TIME_SPENT);
        Integer fbNumOfOpens = Store.getInt(mContext, FB_CURRENT_NUM_OF_OPENS);

        return String.format("Facebook: %s secs (%sx)",
                fbTimeSpent.toString(),
                fbNumOfOpens.toString());
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
        String event;

        switch (screenAction) {
            case Intent.ACTION_USER_PRESENT:
                event = "open";
                break;
            case Intent.ACTION_SCREEN_OFF:
                event = "lock";
                break;
            default:
                event = getLastNChars(screenAction, 15);
                break;
        }

        String data = String.format(locale, "%s, %d;\n", event, System.currentTimeMillis());
        FileHelper.appendToFile(context, Store.SCREEN_LOGS_CSV_FILENAME, data);
    }

    private String getLastNChars(String myString, int n) {
        if (myString.length() > n) {
            return myString.substring(myString.length() - n);
        } else {
            return myString;
        }
    }

    private static void updateNotification(Context context, String message) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
        String title = "Recent usage stats";
        mBuilder.setSmallIcon(R.drawable.ic_chart_pink)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(true)
                .setContentTitle(title)
                .setContentText(message);

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
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
