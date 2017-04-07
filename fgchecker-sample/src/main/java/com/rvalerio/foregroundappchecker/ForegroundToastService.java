package com.rvalerio.foregroundappchecker;

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
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.android.volley.VolleyError;
import com.rvalerio.fgchecker.AppChecker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import io.smalldata.api.CallAPI;
import io.smalldata.api.VolleyJsonCallback;

import static com.rvalerio.foregroundappchecker.Helper.strToDate;
import static com.rvalerio.foregroundappchecker.Store.getStoreBoolean;
import static com.rvalerio.foregroundappchecker.Store.getStoreInt;
import static com.rvalerio.foregroundappchecker.Store.getStoreString;
import static com.rvalerio.foregroundappchecker.Store.increaseStoreInt;
import static com.rvalerio.foregroundappchecker.Store.setStoreBoolean;
import static com.rvalerio.foregroundappchecker.Store.setStoreInt;
import static com.rvalerio.foregroundappchecker.Store.setStoreString;


public class ForegroundToastService extends Service {

    private final static int NOTIFICATION_ID = 1234;
    private final static String STOP_SERVICE = ForegroundToastService.class.getPackage() + ".stop";

    private final Locale locale = Locale.getDefault();
    private Context mContext;

    private BroadcastReceiver appCheckerReceiver;
    private BroadcastReceiver networkConnReceiver;
    private BroadcastReceiver screenUnlockReceiver;

    private AppChecker appChecker;

    private int fbTimeSpent = 0;
    private int fbNumOfOpens = 0;

    private final static long UPDATE_SERVER_INTERVAL_MS = 2 * 3600 * 1000 / 6;

    private NotificationCompat.Builder mBuilder;
    private NotificationManager mNotificationManager;
    private boolean firstTime = true;
    private Handler serverHandler = new Handler();

    private static String TAG = "ForegroundToastService";

    public static void start(Context context) {
        context.startService(new Intent(context, ForegroundToastService.class));
    }

    public static void stop(Context context) {
        context.stopService(new Intent(context, ForegroundToastService.class));
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        mBuilder = new NotificationCompat.Builder(this);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        registerStopCheckerReceiver();
        registerScreenUnlockReceiver();
        registerNetworkMonitorReceiver();

        startChecker();
//        updateNotification(getCurrentStats());
        updateNotification("Your stats updates during usage.");
        serverHandler.postDelayed(serverUpdateTask, 0);
    }

    private Runnable serverUpdateTask = new Runnable() {
        public void run() {
            updateServerRecords();
            serverHandler.postDelayed(this, UPDATE_SERVER_INTERVAL_MS);
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        start(mContext);
        updateNotification("Service destroyed. Should restart.");
    }

    //    @Override
//    public void onDestroy() {
//        super.onDestroy();
//        stopChecker();
//        removeNotification();
//        unregisterAllReceivers();
//        stopSelf();
//    }

    public Boolean isLockedScreen() {
        KeyguardManager myKM = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        return myKM.inKeyguardRestrictedInputMode();
    }

    private void updateLastDate() {

        if (isNewDay() && isPastDailyResetHour()) {
            updateServerRecords();

            setStoreString(mContext, "lastRecordedDate", Helper.getTodayDateStr());
            setStoreInt(mContext, "fbTimeSpent", 0);
            setStoreInt(mContext, "fbNumOfOpens", 0);
            setStoreInt(mContext, "totalSeconds", 0);
            setStoreInt(mContext, "totalOpens", 0);
            setStoreString(mContext, Store.SCREEN_LOGS, "");
            setStoreBoolean(mContext, "serverUpdatedToday", false);
        }
    }

    private boolean isNewDay() {
        String lastRecordedDate = getStoreString(mContext, "lastRecordedDate");
        String today = new SimpleDateFormat("yyyy-MM-dd", locale).format(Calendar.getInstance().getTime());
        return !lastRecordedDate.equals(today);
    }

    private boolean isPastDailyResetHour() {
        Calendar rightNow = Calendar.getInstance();
        Integer hour24 = rightNow.get(Calendar.HOUR_OF_DAY);
        return hour24 >= StudyInfo.getDailyResetHour(mContext);
    }

    private void startChecker() {
        appChecker = new AppChecker();
        appChecker.when(StudyInfo.FACEBOOK_PACKAGE_NAME, new AppChecker.Listener() {
            @Override
            public void onForeground(String packageName) {
                if (isLockedScreen()) return;

                increaseStoreInt(mContext, "totalSeconds", 5);
                increaseStoreInt(mContext, "fbTimeSpent", 5);

                if (getStoreBoolean(mContext, "fbVisitedAnotherApp")) {
                    increaseStoreInt(mContext, "fbNumOfOpens", 1);
                    setStoreBoolean(mContext, "fbVisitedAnotherApp", false);
                }

                fbTimeSpent = getStoreInt(mContext, "fbTimeSpent");
                fbNumOfOpens = getStoreInt(mContext, "fbNumOfOpens");
                updateNotification(getCurrentStats());
                updateLastDate();

                checkIfShouldSubmitID(fbTimeSpent, fbNumOfOpens);
                if (fbTimeSpent > StudyInfo.getFBMaxDailyMinutes(mContext) * 60 || fbNumOfOpens > StudyInfo.getFBMaxDailyOpens(mContext)) {

                    // vibration should only happen during treatment/intervention period
                    // and only participants in group 2 should experience vibration as group 1 is control group
                    if (!isTreatmentPeriod()) return;
                    if (!shouldAllowVibration()) return;
                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    long[] pattern = {0, 100, 100, 100, 100};
                    v.vibrate(pattern, -1);
                }

            }

        }).other(new AppChecker.Listener() {
            @Override
            public void onForeground(String packageName) {
                if (isLockedScreen()) return;

                increaseStoreInt(mContext, "totalSeconds", 5);
                setStoreBoolean(mContext, "fbVisitedAnotherApp", true);

                updateNotification(getCurrentStats());
                updateLastDate();
                updateServerRecords(); // FIXME: 4/6/17
                checkIfShouldSubmitID(fbTimeSpent, fbNumOfOpens);

            }

        }).timeout(5000).start(this);
    }

    private void checkIfShouldSubmitID(int fbTimeSpent, int fbNumOfOpens) {
        if (!getStoreBoolean(mContext, Store.ENROLLED)) {
            if (fbTimeSpent >= 15 && fbNumOfOpens >= 3) {
                updateNotification("Successful! Submit workerId in app to get code.");
                setStoreBoolean(mContext, Store.CAN_SHOW_SUBMIT_BTN, true);
            }
        }
    }

    private boolean isTreatmentPeriod() {
        long rightNow = Calendar.getInstance().getTimeInMillis();
        Date treatmentStart = strToDate(StudyInfo.getTreatmentStartDateStr(mContext));
        Date followupStart = strToDate(StudyInfo.getFollowupStartDateStr(mContext));
        return rightNow > treatmentStart.getTime() && rightNow < followupStart.getTime();
    }

    private boolean shouldAllowVibration() {
        return getStoreInt(mContext, Store.EXPERIMENT_GROUP) == 2;
    }

    private boolean shouldStopServerLogging() {
        long rightNow = Calendar.getInstance().getTimeInMillis();
        Date loggingStop = strToDate(StudyInfo.getLoggingStopDateStr(mContext));
        return rightNow > loggingStop.getTime();
    }


    // only update server records when user has submitted ID and study is still ongoing
    private void updateServerRecords() {
        if (StudyInfo.getWorkerID(mContext).equals("")) return;
        if (shouldStopServerLogging()) {
            updateNotification("Experiment has ended. Uninstall app.");
            return;
        }

        JSONObject params = new JSONObject();
        Helper.setJSONValue(params, "worker_id", getStoreString(mContext, Store.WORKER_ID));
        Helper.setJSONValue(params, "total_seconds", getStoreInt(mContext, "totalSeconds"));
        Helper.setJSONValue(params, "total_opens", getStoreInt(mContext, "totalOpens"));
        Helper.setJSONValue(params, "time_spent", getStoreInt(mContext, "fbTimeSpent"));
        Helper.setJSONValue(params, "time_open", getStoreInt(mContext, "fbNumOfOpens"));
        Helper.setJSONValue(params, "ringer_mode", DeviceInfo.getRingerMode(mContext));
        Helper.setJSONValue(params, "daily_reset_hour", StudyInfo.getDailyResetHour(mContext));
        Helper.setJSONValue(params, "screen_logs", getStoreString(mContext, Store.SCREEN_LOGS));

        Helper.setJSONValue(params, "current_experiment_group", StudyInfo.getCurrentExperimentGroup(mContext));
        Helper.setJSONValue(params, "current_fb_max_mins", StudyInfo.getFBMaxDailyMinutes(mContext));
        Helper.setJSONValue(params, "current_fb_max_opens", StudyInfo.getFBMaxDailyOpens(mContext));
        Helper.setJSONValue(params, "current_treatment_start", StudyInfo.getTreatmentStartDateStr(mContext));
        Helper.setJSONValue(params, "current_followup_start", StudyInfo.getFollowupStartDateStr(mContext));
        Helper.setJSONValue(params, "current_logging_stop", StudyInfo.getLoggingStopDateStr(mContext));

        CallAPI.submitFBStats(mContext, params, submitStatsResponseHandler);
    }

    VolleyJsonCallback submitStatsResponseHandler = new VolleyJsonCallback() {
        @Override
        public void onConnectSuccess(JSONObject result) {
            Log.i(TAG, "Submit Stats: " + result.toString());
            setStoreBoolean(mContext, "serverUpdatedToday", true);
            StudyInfo.updateStoredAdminParams(mContext, result);
        }

        @Override
        public void onConnectFailure(VolleyError error) {
            String msg = "Stats submit error: " + error.toString();
            Log.e(TAG, "StatsError: " + msg);
        }

    };

    private String getCurrentStats() {
        Integer fbTimeSpent = getStoreInt(mContext, "fbTimeSpent");
        Integer fbNumOfOpens = getStoreInt(mContext, "fbNumOfOpens");

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

                Boolean serverIsNotYetUpdated = !getStoreBoolean(mContext, "serverUpdatedToday");
                if (state == NetworkInfo.State.CONNECTED && serverIsNotYetUpdated) {
                    updateServerRecords();
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
                if (intent.getAction().equals(Intent.ACTION_USER_PRESENT)) {
                    increaseStoreInt(mContext, "totalOpens", 1);
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
        String strLogs = Store.getStoreString(context, Store.SCREEN_LOGS);

        JSONArray logs = new JSONArray();
        if (!strLogs.equals("")) {
            try {
                logs = new JSONArray(strLogs);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        String screenAction = intent.getAction();
        if (intent.getAction().equals(Intent.ACTION_USER_PRESENT)) {
            screenAction = "0"; //unlocked
        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            screenAction = "1"; // locked
        } else {
            screenAction = getLastNChars(screenAction, 10); // unknown
        }

        long nowInMillis = Calendar.getInstance().getTimeInMillis();
        String entry = String.format("%s, %s", nowInMillis, screenAction);
        logs.put(entry);
        Store.setStoreString(mContext, Store.SCREEN_LOGS, logs.toString());
    }

    private String getLastNChars(String myString, int n) {
        if(myString.length() > n) {
            return myString.substring(myString.length()-n);
        } else {
            return myString;
        }
    }

    private void updateNotification(String message) {
        String title = "Recent usage stats";

        if (firstTime) {
            mBuilder
                    .setSmallIcon(R.drawable.ic_chart_pink)
                    .setPriority(NotificationCompat.PRIORITY_MIN)
                    .setOngoing(true);
            firstTime = false;
        }

        mBuilder.setContentTitle(title);
        mBuilder.setContentText(message);
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
}
