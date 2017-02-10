package com.rvalerio.foregroundappchecker;

import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.android.volley.VolleyError;
import com.deploygate.sdk.DeployGate;
import com.rvalerio.fgchecker.AppChecker;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import io.smalldata.api.CallAPI;
import io.smalldata.api.VolleyJsonCallback;

import static com.rvalerio.foregroundappchecker.Helper.getStoreBoolean;
import static com.rvalerio.foregroundappchecker.Helper.getStoreInt;
import static com.rvalerio.foregroundappchecker.Helper.getStoreString;
import static com.rvalerio.foregroundappchecker.Helper.increaseStoreInt;
import static com.rvalerio.foregroundappchecker.Helper.setStoreBoolean;
import static com.rvalerio.foregroundappchecker.Helper.setStoreInt;
import static com.rvalerio.foregroundappchecker.Helper.setStoreString;


public class ForegroundToastService extends Service {

    private final static int BASELINE_ENDS = 1;
    private final static int FOLLOWUP_BEGINS = 4;

    private final static int NOTIFICATION_ID = 1234;
    private final static String STOP_SERVICE = ForegroundToastService.class.getPackage() + ".stop";

    private final Locale locale = Locale.getDefault();
    private Context mContext;

    private BroadcastReceiver stopServiceReceiver;
    private AppChecker appChecker;

    final private Integer fbMaxDailyMinutes = 15;
    final private Integer fbMaxDailyOpen = 5;

    private Integer fbTimeSpent;
    private Integer fbNumOfOpens;

    private final static long UPDATE_SERVER_INTERVAL_MS = 2*3600*1000;

    private NotificationCompat.Builder mBuilder;
    private NotificationManager mNotificationManager;
    private boolean firstTime = true;
    private Handler serverHandler = new Handler();



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

        registerReceivers();
        startChecker();

        mBuilder = new NotificationCompat.Builder(this);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        updateNotification("Daily stats will update throughout day.");
       serverHandler.postDelayed(serverUpdateTask, 0);
    }

    private Runnable serverUpdateTask = new Runnable() {
        public void run() {
            updateServerRecords(getStoreInt(mContext, "fbTimeSpent"), getStoreInt(mContext, "fbNumOfOpens"));
            serverHandler.postDelayed(this, UPDATE_SERVER_INTERVAL_MS);
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopChecker();
        removeNotification();
        unregisterReceivers();
        stopSelf();
    }

    public Boolean isLockedScreen() {
        KeyguardManager myKM = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        return myKM.inKeyguardRestrictedInputMode();
    }

    private void updateLastDate() {

        if (isNewDay() && isPast3am()) {
            Integer todayTimeSpent = getStoreInt(mContext, "fbTimeSpent");
            Integer todayNumOpens = getStoreInt(mContext, "fbNumOfOpens");
            updateServerRecords(todayTimeSpent, todayNumOpens);

            String info = String.format(locale, "Facebook: %d secs (%dx)", todayTimeSpent, todayNumOpens);
            DeployGate.logInfo(info);

            setStoreString(mContext, "lastRecordedDate", Helper.getTodayDateStr());
            increaseStoreInt(mContext, "studyDayCount", 1);
            setStoreInt(mContext, "fbTimeSpent", 0);
            setStoreInt(mContext, "fbNumOfOpens", 0);
            setStoreInt(mContext, "totalSeconds", 0);
        }
    }

    private boolean isNewDay() {
        String lastRecordeddDate = getStoreString(mContext, "lastRecordedDate");
        String today = new SimpleDateFormat("yyyy-MM-dd", locale).format(Calendar.getInstance().getTime());
        return !lastRecordeddDate.equals(today);
    }

    private boolean isPast3am() {
        Calendar rightNow = Calendar.getInstance();
        Integer hour24 = rightNow.get(Calendar.HOUR_OF_DAY);
        return hour24 >= 3;
    }

    private void startChecker() {

        appChecker = new AppChecker();
        appChecker.when("com.facebook.katana", new AppChecker.Listener() {
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

                if (fbTimeSpent > fbMaxDailyMinutes * 60 || fbNumOfOpens > fbMaxDailyOpen) {
                    String info = String.format(locale, "Facebook: %d secs (%dx)", fbTimeSpent, fbNumOfOpens);
                    DeployGate.logInfo(info);

                    if (!isTreatmentPeriod()) return;
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

            }

        }).timeout(5000).start(this);
    }

    private boolean isTreatmentPeriod() {
        Integer studyDayCount = getStoreInt(mContext, "studyDayCount");
        return studyDayCount > BASELINE_ENDS && studyDayCount < FOLLOWUP_BEGINS;
    }

    private void updateServerRecords(Integer timeSpent, Integer timeOpen) {
        JSONObject params = new JSONObject();
        Helper.setJSONValue(params, "deviceID", Helper.getDeviceID(mContext));
        Helper.setJSONValue(params, "workerID", getStoreString(mContext, "workerID"));
        Helper.setJSONValue(params, "totalSeconds", getStoreInt(mContext, "totalSeconds"));
        Helper.setJSONValue(params, "timeSpent", timeSpent);
        Helper.setJSONValue(params, "timeOpen", timeOpen);
        Log.d("updateServerParams", params.toString() + timeSpent.toString() + timeOpen.toString());
        CallAPI.submitFBStats(mContext, params, submitStatsResponseHandler);
    }


    VolleyJsonCallback submitStatsResponseHandler = new VolleyJsonCallback() {
        @Override
        public void onConnectSuccess(JSONObject result) {
            Log.e("Submit Stats: ", result.toString());
        }

        @Override
        public void onConnectFailure(VolleyError error) {
            String msg = "Stats submit error: " + error.toString();
            Log.d("StatsError", msg);
        }

    };

    private String getCurrentStats() {
//        Integer totalSpent = getStoreInt(mContext, "totalSeconds");
        Integer fbTimeSpent = getStoreInt(mContext, "fbTimeSpent");
        Integer fbNumOfOpens = getStoreInt(mContext, "fbNumOfOpens");

        return String.format("Facebook: %s secs (%sx)",
                fbTimeSpent.toString(),
                fbNumOfOpens.toString());

//        return String.format("Total Tme: %s secs / Facebook: %s secs (%sx)",
//                totalSpent.toString(),
//                fbTimeSpent.toString(),
//                fbNumOfOpens.toString());
    }

    private void stopChecker() {
        appChecker.stop();
    }

    private void registerReceivers() {
        stopServiceReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                stopSelf();
            }
        };
        registerReceiver(stopServiceReceiver, new IntentFilter(STOP_SERVICE));
    }

    private void unregisterReceivers() {
        unregisterReceiver(stopServiceReceiver);
    }

    private void updateNotification(String message) {
        String title = "Recent usage stats";

        if (firstTime) {
            mBuilder
                    .setSmallIcon(android.R.drawable.ic_notification_overlay)
                    .setPriority(NotificationCompat.PRIORITY_MIN)
                    .setOngoing(true);
            firstTime = false;
        }

        mBuilder.setContentTitle(title);
        mBuilder.setContentText(message);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
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
