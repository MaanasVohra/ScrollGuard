package com.rvalerio.foregroundappchecker.goodvibe.main;

import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.google.firebase.iid.FirebaseInstanceId;
import com.rvalerio.fgchecker.AppChecker;
import com.rvalerio.foregroundappchecker.R;
import com.rvalerio.foregroundappchecker.goodvibe.api.CallAPI;
import com.rvalerio.foregroundappchecker.goodvibe.api.VolleyJsonCallback;
import com.rvalerio.foregroundappchecker.goodvibe.helper.AlarmHelper;
import com.rvalerio.foregroundappchecker.goodvibe.helper.FileHelper;
import com.rvalerio.foregroundappchecker.goodvibe.helper.JsonHelper;
import com.rvalerio.foregroundappchecker.goodvibe.helper.NetworkHelper;
import com.rvalerio.foregroundappchecker.goodvibe.personalize.AdaptivePersonalize;
import com.rvalerio.foregroundappchecker.goodvibe.personalize.StaticPersonalize;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static com.rvalerio.foregroundappchecker.goodvibe.main.Store.setBoolean;


public class ForegroundToastService extends Service {
    private static String TAG = "ForegroundToastService";
    private final static String STOP_SERVICE = ForegroundToastService.class.getPackage() + ".stop";
    private final static int NOTIFICATION_ID = 1234;
    private final static String FB_CURRENT_TIME_SPENT = "fbTimeSpent";
    private final static String FB_CURRENT_NUM_OF_OPENS = "fbNumOfOpens";

    private final Locale locale = Locale.getDefault();
    private Context mContext;

    private BroadcastReceiver appCheckerReceiver;
    private BroadcastReceiver networkConnReceiver;
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
        registerNetworkMonitorReceiver();

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

    private void applyPersonalizedFacebookLimits() {
        if (!workerExists(mContext)) return;

        String treatmentStartDateStr = StudyInfo.getTreatmentStartDateStr(mContext);
        int experimentGroup = StudyInfo.getCurrentExperimentGroup(mContext);
        StaticPersonalize personalize;

        switch (experimentGroup) {
            case StudyInfo.STATIC_GROUP:
                personalize = new StaticPersonalize(mContext, treatmentStartDateStr);
                break;
            case StudyInfo.ADAPTIVE_GROUP:
                personalize = new AdaptivePersonalize(mContext, treatmentStartDateStr);
                break;
            case StudyInfo.POPUP_ADAPTIVE_GROUP:
                personalize = new AdaptivePersonalize(mContext, treatmentStartDateStr);
                break;
            default:
                personalize = new StaticPersonalize(mContext, treatmentStartDateStr);
                break;
        }

        personalize.addDataPoint(getCurrentFBTimeSpent(), getCurrentFBNumOfOpens());
        int timeLimit = personalize.getAverageTimeSpent();
        int openLimit = personalize.getAverageTimeOpen();
        StudyInfo.updateFBLimitsOfStudy(mContext, timeLimit, openLimit);
    }

    private void updateLastDate() {
        boolean shouldUpdate = isNewDay() && isPastDailyResetHour();
        if (shouldUpdate) {

            long timeMillis = System.currentTimeMillis();
            String fbDate = Store.getString(mContext, "lastRecordedDate");
            fbDate = fbDate.equals("") ? "launchDate" : fbDate;
            int timeSpent = getCurrentFBTimeSpent();
            int timeOpen = getCurrentFBNumOfOpens();

            String data = String.format(locale, "%d, %s, %d, %d;\n", timeMillis, fbDate, timeSpent, timeOpen);
            FileHelper.appendToFile(mContext, Store.FB_LOGS_CSV_FILENAME, data);
            FileHelper.appendToFile(mContext, Store.BACKUP_FB_LOGS_CSV_FILENAME, data);


            applyPersonalizedFacebookLimits();
            updateServerRecords(mContext);
            Store.setString(mContext, "lastRecordedDate", Helper.getTodayDateStr());
            Store.setInt(mContext, FB_CURRENT_TIME_SPENT, 0);
            Store.setInt(mContext, FB_CURRENT_NUM_OF_OPENS, 0);
//            Store.setInt(mContext, "totalSeconds", 0); // FIXME: 6/19/17 put right value
//            Store.setInt(mContext, "totalOpens", 0);
//            Store.setString(mContext, Store.SCREEN_LOGS, "");
            Store.setBoolean(mContext, "serverUpdatedToday", false);
        }
    }

    private boolean isNewDay() {
        String lastRecordedDate = Store.getString(mContext, "lastRecordedDate");
        String today = new SimpleDateFormat("yyyy-MM-dd", locale).format(Calendar.getInstance().getTime());
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
        if (isLockedScreen()) return;

        int timer = 0;
        if (packageName.equals(getLastFgApp())) {
            timer = Store.getInt(mContext, packageName);
        } else {
            String lastAppId = getLastFgApp();
            int lastAppTimeSpent = Store.getInt(mContext, lastAppId);
            String data = String.format(locale, "%s, %d, %d;\n", lastAppId, lastAppTimeSpent, System.currentTimeMillis());
            FileHelper.appendToFile(mContext, Store.APP_LOGS_CSV_FILENAME, data);
            FileHelper.appendToFile(mContext, Store.BACKUP_APP_LOGS_CSV_FILENAME, data);
        }

        timer += 5;
        if (!packageName.equals(StudyInfo.FACEBOOK_PACKAGE_NAME)) {
            String msg = String.format(locale, "%s: %d secs.", packageName, timer);
            updateNotification(mContext, msg);
        }
        Store.setInt(mContext, packageName, timer);
        setLastFgApp(packageName);
    }

    private String getLastFgApp() {
        return Store.getString(mContext, "lastFgApp");
    }

    private void setLastFgApp(String packageName) {
        Store.setString(mContext, "lastFgApp", packageName);
    }

    private void doFacebookOperation() {
//        Store.increaseInt(mContext, "totalSeconds", 5);
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
        updateLastDate();


        if (fbNumOfOpens % 3 == 0) { // FIXME: 6/19/17 remove
            int maxMins = StudyInfo.getFBMaxDailyMinutes(mContext);
            int maxOpens = StudyInfo.getFBMaxDailyOpens(mContext);
            AlarmHelper.showInstantNotif(mContext, String.format(locale, "*Day Reset* (max: %dsecs / %dx)", maxMins * 60, maxOpens),
                    String.format(locale, "Current: %dsecs / %dx", fbTimeSpent, fbNumOfOpens), "", 8001);
            Store.setString(mContext, "lastRecordedDate", "");
        }

        checkAndActivateIfShouldSubmitID(fbTimeSpent, fbNumOfOpens);
        if (fbTimeSpent > StudyInfo.getFBMaxDailyMinutes(mContext) * 60 || fbNumOfOpens > StudyInfo.getFBMaxDailyOpens(mContext)) {
            if (!isTreatmentPeriod()) return;
            vibrateOrPopup();
        }
    }

    private void vibrateOrPopup() {
        int experimentGroup = StudyInfo.getCurrentExperimentGroup(mContext);
        if (experimentGroup == 3) {
            Toast.makeText(mContext, "Facebook Limit Exceeded", Toast.LENGTH_SHORT).show();
        } else {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            long[] pattern = {0, 100, 100, 100, 100};
            v.vibrate(pattern, -1);
        }
    }

    private void checkAndActivateIfShouldSubmitID(int fbTimeSpent, int fbNumOfOpens) {
        if (!Store.getBoolean(mContext, Store.ENROLLED)) {
            if (fbTimeSpent >= 0 && fbNumOfOpens >= 1) {
                updateNotification(mContext, "Successful! Submit workerId.");
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
        sendFBStats(context);
        sendFgAppLogs(context);
        sendScreenEventLogs(context);
    }

    private static void sendFBStats(Context context) {
        CallAPI.submitFBStats(context, getFBParams(context), getFBResponseHandler(context));
        JSONObject params = getLogParams(context, Store.FB_LOGS_CSV_FILENAME);
        CallAPI.submitFacebookLogs(context, params, getLogResponseHandler(context, Store.FB_LOGS_CSV_FILENAME));
    }

    private static void sendFgAppLogs(Context context) {
        JSONObject params = getLogParams(context, Store.APP_LOGS_CSV_FILENAME);
        CallAPI.submitAppLogs(context, params, getLogResponseHandler(context, Store.APP_LOGS_CSV_FILENAME));
    }

    private static void sendScreenEventLogs(Context context) {
        JSONObject params = getLogParams(context, Store.SCREEN_LOGS_CSV_FILENAME);
        CallAPI.submitScreenEventLogs(context, params, getLogResponseHandler(context, Store.SCREEN_LOGS_CSV_FILENAME));
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
        Helper.setJSONValue(params, "total_seconds", Store.getInt(context, "totalSeconds"));
        Helper.setJSONValue(params, "total_opens", Store.getInt(context, "totalOpens"));
        Helper.setJSONValue(params, "time_spent", Store.getInt(context, FB_CURRENT_TIME_SPENT));
        Helper.setJSONValue(params, "time_open", Store.getInt(context, FB_CURRENT_NUM_OF_OPENS));
        Helper.setJSONValue(params, "ringer_mode", DeviceInfo.getRingerMode(context));
        Helper.setJSONValue(params, "daily_reset_hour", StudyInfo.getDailyResetHour(context));
        Helper.setJSONValue(params, "screen_logs", Store.getString(context, Store.SCREEN_LOGS));
        Helper.setJSONValue(params, "firebase_token", FirebaseInstanceId.getInstance().getToken());

        Helper.setJSONValue(params, "current_experiment_group", StudyInfo.getCurrentExperimentGroup(context));
        Helper.setJSONValue(params, "current_fb_max_mins", StudyInfo.getFBMaxDailyMinutes(context));
        Helper.setJSONValue(params, "current_fb_max_opens", StudyInfo.getFBMaxDailyOpens(context));
        Helper.setJSONValue(params, "current_treatment_start", StudyInfo.getTreatmentStartDateStr(context));
        Helper.setJSONValue(params, "current_followup_start", StudyInfo.getFollowupStartDateStr(context));
        Helper.setJSONValue(params, "current_logging_stop", StudyInfo.getLoggingStopDateStr(context));
        return params;
    }

    public static VolleyJsonCallback getFBResponseHandler(final Context context) {
        return new VolleyJsonCallback() {
            @Override
            public void onConnectSuccess(JSONObject result) {
                Log.i(TAG, "Submit Stats: " + result.toString());
                Store.setBoolean(context, "serverUpdatedToday", true);
                StudyInfo.updateStoredAdminParams(context, result);
            }

            @Override
            public void onConnectFailure(VolleyError error) {
                String msg = "Stats submit error: " + error.toString();
                Log.e(TAG, "StatsError: " + msg);
                AlarmHelper.showInstantNotif(context, "FB submit error", msg, "", 3333); // FIXME: 6/2/17 remove
            }
        };
    }

    private static VolleyJsonCallback getLogResponseHandler(final Context context, final String filenameToReset) {
        return new VolleyJsonCallback() {
            @Override
            public void onConnectSuccess(JSONObject result) {
                Log.i(TAG, "FgApp Submit response: " + result.toString());
                setBoolean(context, filenameToReset + "UpdatedToday", true);
                FileHelper.resetFile(context, filenameToReset);
            }

            @Override
            public void onConnectFailure(VolleyError error) {
                String msg = "Stats submit error: " + error.toString();
                AlarmHelper.showInstantNotif(context, filenameToReset + " submit error", msg, "", 3223); // FIXME: 6/2/17 remove
                Log.e(TAG, "FgApp StatsError: " + msg);
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

    private void registerNetworkMonitorReceiver() {
        networkConnReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle extras = intent.getExtras();
                NetworkInfo info = extras.getParcelable("networkInfo");
                NetworkInfo.State state = null;
                if (info != null) {
                    state = info.getState();
                }

                boolean userIsEnrolled = Store.getBoolean(mContext, Store.ENROLLED);
                boolean serverIsNotYetUpdated = !Store.getBoolean(mContext, "serverUpdatedToday");
                if (userIsEnrolled && serverIsNotYetUpdated && state == NetworkInfo.State.CONNECTED) {
//                    updateServerRecords(mContext);
                    Log.i(TAG, "onReceive: updateServerRecords.");
                }
            }
        };

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkConnReceiver, intentFilter);
    }

    private void registerScreenUnlockReceiver() {
        screenUnlockReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateScreenLog(context, intent);

                // FIXME: 6/2/17 big time bug with screen lock count!!!!!!!!!!!1
                if (intent.getAction().equals(Intent.ACTION_USER_PRESENT)) {
                    Store.increaseInt(mContext, "totalOpens", 1);
                    Log.d(TAG, "Phone unlocked");
                } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                    Log.d(TAG, "Phone locked");
                }
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
        FileHelper.appendToFile(context, Store.BACKUP_SCREEN_LOGS_CSV_FILENAME, data);
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
        unregisterReceiver(networkConnReceiver);
        unregisterReceiver(screenUnlockReceiver);
        unregisterReceiver(appCheckerReceiver);
    }

    private void removeNotification() {
        NotificationManager manager = ((NotificationManager) getSystemService(NOTIFICATION_SERVICE));
        manager.cancel(NOTIFICATION_ID);
    }

//    private Notification createStickyNotification() {
//        NotificationManager manager = ((NotificationManager) getSystemService(NOTIFICATION_SERVICE));
//        Notification notification = new NotificationCompat.Builder(this).setSmallIcon(R.mipmap.ic_launcher)
//                .setPriority(NotificationCompat.PRIORITY_MIN)
//                .setOngoing(true).setOnlyAlertOnce(true)
//                .setAutoCancel(false)
//                .setContentTitle(getString(R.string.app_name))
//                .setContentText(getString(R.string.stop_service))
//                .setContentIntent(PendingIntent.getBroadcast(this, 0,
//                        new Intent(STOP_SERVICE), PendingIntent.FLAG_UPDATE_CURRENT))
//                .setWhen(0).build();
//
//        manager.notify(NOTIFICATION_ID, notification);
//        return notification;
//    }

//    private void updateScreenLog22(Context context, Intent intent) {
//        String strLogs = Store.getString(context, Store.SCREEN_LOGS);
//
//        JSONArray logs = JsonHelper.strToJsonArray(strLogs);
//        String screenAction = intent.getAction();
//        String event;
//        if (intent.getAction().equals(Intent.ACTION_USER_PRESENT)) {
//            screenAction = "0"; //unlocked
//            event = "open";
//        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
//            screenAction = "1"; // locked
//            event = "lock";
//        } else {
//            screenAction = getLastNChars(screenAction, 15); // unknown
//            event = getLastNChars(screenAction, 15); // unknown
//        }
//
//        long nowInMillis = Calendar.getInstance().getTimeInMillis();
//        String entry = String.format("%s, %s", nowInMillis, screenAction);
//        logs.put(entry);
//        Store.setString(mContext, Store.SCREEN_LOGS, logs.toString());
//    }
//
}


// TODO: 5/29/17 refactor Store.getInt(mContext, FB_CURRENT_NUM_OF_OPENS) ==> getFBOpens()
