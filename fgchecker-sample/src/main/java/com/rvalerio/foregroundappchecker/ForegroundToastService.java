package com.rvalerio.foregroundappchecker;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.widget.Toast;
import android.os.Vibrator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


import com.rvalerio.fgchecker.AppChecker;


public class ForegroundToastService extends Service {

    private final static int NOTIFICATION_ID = 1234;
    private final static String STOP_SERVICE = ForegroundToastService.class.getPackage() + ".stop";
    private static final String PREF_NAME = "prefs";
    private BroadcastReceiver stopServiceReceiver;
    private AppChecker appChecker;
    private Integer gmTimeSpent;
    private Integer gmNumOfOpens;
    private Integer fbTimeSpent;
    private Integer fbNumOfOpens;
    private String msg;

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static String getStoreString(Context context, String key) {
        return getPrefs(context).getString(key, "");
    }

    public static Integer getStoreInt(Context context, String key) {
        return getPrefs(context).getInt(key, 0);
    }

    public static Boolean getStoreBoolean(Context context, String key) {
        return getPrefs(context).getBoolean(key, false);
    }

    public static void setStoreString(Context context, String key, String input) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putString(key, input);
        editor.apply();
    }

    public static void setStoreInt(Context context, String key, Integer input) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putInt(key, input);
        editor.apply();
    }

    public static void increaseStoreInt(Context context, String key, Integer increment) {
        Integer currentValue = getStoreInt(context, key);
        setStoreInt(context, key, currentValue + increment);
    }

    public static void setStoreBoolean(Context context, String key, Boolean input) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putBoolean(key, input);
        editor.apply();
    }

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
        registerReceivers();
        startChecker();
        createStickyNotification();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopChecker();
        removeNotification();
        unregisterReceivers();
        stopSelf();
    }

    private void updateLastDate(Context ctx) {

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String lastRecordedDate = getStoreString(ctx, "lastRecordedDate");
        if (!lastRecordedDate.equals(today)) {
            setStoreString(ctx, "lastRecordedDate", today);
            setStoreInt(ctx, "gmTimeSpent", 0);
            setStoreInt(ctx, "gmNumOfOpens", 0);
            setStoreInt(ctx, "fbTimeSpent", 0);
            setStoreInt(ctx, "fbNumOfOpens", 0);
            Toast.makeText(ctx, "mBeehive restarted for new day: " + today, Toast.LENGTH_LONG).show();
//        } else {
//            Toast.makeText(ctx, "Nope, still same day: " + today, Toast.LENGTH_LONG).show();
        }

    }

    private void startChecker() {

        final Context ctx = getBaseContext();

        appChecker = new AppChecker();
        appChecker.when(getPackageName(), new AppChecker.Listener() {
            @Override
            public void onForeground(String packageName) {
              Toast.makeText(getBaseContext(), "You must like looking at this app :P", Toast.LENGTH_SHORT).show();
            }
        }).when("com.google.android.gm", new AppChecker.Listener() {
            @Override
            public void onForeground(String packageName) {
                increaseStoreInt(ctx, "gmTimeSpent", 5);
                if (getStoreBoolean(ctx, "gmVisitedAnotherApp")) {
                    increaseStoreInt(ctx, "gmNumOfOpens", 1);
                    setStoreBoolean(ctx, "gmVisitedAnotherApp", false);
                }

                gmTimeSpent = getStoreInt(ctx, "gmTimeSpent");
                gmNumOfOpens = getStoreInt(ctx, "gmNumOfOpens");
                msg = "GMail (limit: 3 mins / 5x)\nTime spent: " +
                        gmTimeSpent.toString() + " secs (opened: " +
                        gmNumOfOpens.toString() + "x)";

                Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();

                // First item in pattern is time(ds) delay
                // Each element then alternates (in ms) between vibrate, sleep, vibrate, sleep...
                // The '-1' here means to vibrate once, as '-1' is out of bounds in the pattern array
                if (gmTimeSpent > 180 || gmNumOfOpens > 5) {
                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    long[] pattern = {0, 100, 100, 100, 100};
                    v.vibrate(pattern, -1);
                }
                updateLastDate(ctx);
            }

        }).when("com.facebook.katana", new AppChecker.Listener() {
            @Override
            public void onForeground(String packageName) {
                increaseStoreInt(ctx, "fbTimeSpent", 5);
                if (getStoreBoolean(ctx, "fbVisitedAnotherApp")) {
                    increaseStoreInt(ctx, "fbNumOfOpens", 1);
                    setStoreBoolean(ctx, "fbVisitedAnotherApp", false);
                }

                fbTimeSpent = getStoreInt(ctx, "fbTimeSpent");
                fbNumOfOpens = getStoreInt(ctx, "fbNumOfOpens");
                msg = "Facebook (limit: 5 mins / 10x)\nTime spent: " +
                        fbTimeSpent.toString() + " secs (opened: " +
                        fbNumOfOpens.toString() + "x)";

                Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();

                if (fbTimeSpent > 300 || fbNumOfOpens > 10) {
                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    long[] pattern = {0, 100, 100, 100, 100, 100, 100};
                    v.vibrate(pattern, -1);
                }
                updateLastDate(ctx);
            }

        }).other(new AppChecker.Listener() {
            @Override
            public void onForeground(String packageName) {
                setStoreBoolean(ctx, "gmVisitedAnotherApp", true);
                setStoreBoolean(ctx, "fbVisitedAnotherApp", true);
//                Toast.makeText(getBaseContext(), "Foreground: " + packageName, Toast.LENGTH_SHORT).show();
                updateLastDate(ctx);
            }
        }).timeout(5000).start(this);
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

    private Notification createStickyNotification() {
        NotificationManager manager = ((NotificationManager) getSystemService(NOTIFICATION_SERVICE));
        Notification notification = new NotificationCompat.Builder(this).setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(true).setOnlyAlertOnce(true)
                .setAutoCancel(false)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.stop_service))
                .setContentIntent(PendingIntent.getBroadcast(this, 0,
                        new Intent(STOP_SERVICE), PendingIntent.FLAG_UPDATE_CURRENT))
                .setWhen(0).build();

        manager.notify(NOTIFICATION_ID, notification);
        return notification;
    }

    private void removeNotification() {
        NotificationManager manager = ((NotificationManager) getSystemService(NOTIFICATION_SERVICE));
        manager.cancel(NOTIFICATION_ID);
    }
}
