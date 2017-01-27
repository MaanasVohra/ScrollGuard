package com.rvalerio.foregroundappchecker;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
//import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.widget.Toast;
import android.os.Vibrator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


import com.rvalerio.fgchecker.AppChecker;

import static com.rvalerio.foregroundappchecker.Helper.getStoreBoolean;
import static com.rvalerio.foregroundappchecker.Helper.getStoreInt;
import static com.rvalerio.foregroundappchecker.Helper.getStoreString;
import static com.rvalerio.foregroundappchecker.Helper.increaseStoreInt;
import static com.rvalerio.foregroundappchecker.Helper.setStoreBoolean;
import static com.rvalerio.foregroundappchecker.Helper.setStoreInt;
import static com.rvalerio.foregroundappchecker.Helper.setStoreString;


public class ForegroundToastService extends Service {

    private final static int NOTIFICATION_ID = 1234;
    private final static String STOP_SERVICE = ForegroundToastService.class.getPackage() + ".stop";
//    private static final String PREF_NAME = "prefs";
    private BroadcastReceiver stopServiceReceiver;
    private AppChecker appChecker;

    private Integer gmMaxTime;
    private Integer gmMaxOpen;
    private Integer fbMaxTime;
    private Integer fbMaxOpen;

    private Integer gmTimeSpent;
    private Integer gmNumOfOpens;
    private Integer fbTimeSpent;
    private Integer fbNumOfOpens;

    private String msg;

    private NotificationCompat.Builder mBuilder;
    private NotificationManager mNotificationManager;
    private boolean firstTime = true;


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
//        createStickyNotification();

        mBuilder = new NotificationCompat.Builder(this);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        updateNotification("Beehive just started", 50);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopChecker();
        removeNotification();
        unregisterReceivers();
        stopSelf();
    }

    public Boolean isLockedScreen() {
        KeyguardManager myKM = (KeyguardManager)getSystemService(Context.KEYGUARD_SERVICE);
        return myKM.inKeyguardRestrictedInputMode();
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
            setStoreBoolean(ctx, "canSubmitInput", true);
//            Toast.makeText(ctx, "mBeehive restarted for new day: " + today, Toast.LENGTH_LONG).show();
//        } else {
//            Toast.makeText(ctx, "Nope, still same day: " + today, Toast.LENGTH_LONG).show();
        }

    }

    private void startChecker() {

        final Context ctx = getBaseContext();

        gmMaxTime = getStoreInt(ctx, "gmMaxTime");
        gmMaxOpen = getStoreInt(ctx, "gmMaxOpen");
        fbMaxTime = getStoreInt(ctx, "fbMaxTime");
        fbMaxOpen = getStoreInt(ctx, "fbMaxOpen");

        appChecker = new AppChecker();
        appChecker.when(getPackageName(), new AppChecker.Listener() {
            @Override
            public void onForeground(String packageName) {
              Toast.makeText(getBaseContext(), "You must like looking at this app :P", Toast.LENGTH_SHORT).show();
            }
        }).when("com.google.android.gm", new AppChecker.Listener() {
            @Override
            public void onForeground(String packageName) {

                if (isLockedScreen()) return;
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

//                Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
                updateNotification(getCurrentStats(ctx), 80);

                // First item in pattern is time(ds) delay
                // Each element then alternates (in ms) between vibrate, sleep, vibrate, sleep...
                // The '-1' here means to vibrate once, as '-1' is out of bounds in the pattern array
                if (gmTimeSpent > gmMaxTime*60 || gmNumOfOpens > gmMaxOpen) {
                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    long[] pattern = {0, 100, 100, 100, 100};
                    v.vibrate(pattern, -1);
                }
                updateLastDate(ctx);
            }

        }).when("com.facebook.katana", new AppChecker.Listener() {
            @Override
            public void onForeground(String packageName) {
                if (isLockedScreen()) return;
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

//                Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
                updateNotification(getCurrentStats(ctx), 80);

                if (fbTimeSpent > fbMaxTime*60 || fbNumOfOpens > fbMaxOpen) {
                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    long[] pattern = {0, 100, 100, 100, 100, 100, 100};
                    v.vibrate(pattern, -1);
                }
                updateLastDate(ctx);
            }

        }).other(new AppChecker.Listener() {
            @Override
            public void onForeground(String packageName) {
                if (isLockedScreen()) return;
                setStoreBoolean(ctx, "gmVisitedAnotherApp", true);
                setStoreBoolean(ctx, "fbVisitedAnotherApp", true);
//                Toast.makeText(getBaseContext(), "Foreground: " + packageName, Toast.LENGTH_SHORT).show();
                msg = "Foreground: " + packageName;
                updateNotification(getCurrentStats(ctx), 80);
                updateLastDate(ctx);
            }
        }).timeout(3000).start(this);
    }

    private String getCurrentStats(Context ctx) {
        Integer  gmTimeSpent = getStoreInt(ctx, "gmTimeSpent");
        Integer gmNumOfOpens = getStoreInt(ctx, "gmNumOfOpens");
        Integer  fbTimeSpent = getStoreInt(ctx, "fbTimeSpent");
        Integer fbNumOfOpens = getStoreInt(ctx, "fbNumOfOpens");

        return String.format("Gmail: %s secs (%sx) / Facebook: %s secs (%sx)",
                gmTimeSpent.toString(),
                gmNumOfOpens.toString(),
                fbTimeSpent.toString(),
                fbNumOfOpens.toString());

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

    private void updateNotification(String message, int progress) {
//        String title = String.format("Limit: Gmail(%s secs/%sx) / FB(%s secs/%sx)",
//                gmMaxTime * 60,
//                gmMaxOpen,
//                fbMaxTime * 60,
//                fbMaxOpen);

        String title = "Recent usage stats";

        if (firstTime) {
            mBuilder.setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title)
                    .setOngoing(true);
            firstTime = false;
        }

        mBuilder.setContentText(message);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    private void removeNotification() {
        NotificationManager manager = ((NotificationManager) getSystemService(NOTIFICATION_SERVICE));
        manager.cancel(NOTIFICATION_ID);
    }
}
