package com.rvalerio.foregroundappchecker;

import android.app.KeyguardManager;
//import android.app.Notification;
import android.app.NotificationManager;
//import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
//import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.os.Vibrator;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

//import java.util.Date;
//import android.widget.Toast;


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

    private BroadcastReceiver stopServiceReceiver;
    private AppChecker appChecker;

    final private Integer fbMaxDailyMinutes = 15;
    final private Integer fbMaxDailyOpen = 5;

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

        mBuilder = new NotificationCompat.Builder(this);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        updateNotification("Daily stats will update throughout day.");
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

//        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime());
        String lastRecordedDate = getStoreString(ctx, "lastRecordedDate");
        if (!lastRecordedDate.equals(today) && isPast3am()) {
            setStoreString(ctx, "lastRecordedDate", today);
            setStoreInt(ctx, "fbTimeSpent", 0);
            setStoreInt(ctx, "fbNumOfOpens", 0);
            setStoreBoolean(ctx, "canSubmitInput", true);

//            Toast.makeText(ctx, "mBeehive restarted for new day: " + today, Toast.LENGTH_LONG).show();
        }

    }

    private boolean isPast3am() {
        Calendar rightNow = Calendar.getInstance();
        return rightNow.HOUR_OF_DAY >= 3;
    }

    private void startChecker() {

        final Context ctx = getBaseContext();

//        fbMaxTime = getStoreInt(ctx, "fbMaxTime");
//        fbMaxOpen = getStoreInt(ctx, "fbMaxOpen");

        appChecker = new AppChecker();
        appChecker.when(getPackageName(), new AppChecker.Listener() {
            @Override
            public void onForeground(String packageName) {
//              Toast.makeText(getBaseContext(), "You must like looking at this app :P", Toast.LENGTH_SHORT).show();
                setStoreBoolean(ctx, "fbVisitedAnotherApp", true);
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

                updateNotification(getCurrentStats(ctx));

                if (fbTimeSpent > fbMaxDailyMinutes*60 || fbNumOfOpens > fbMaxDailyOpen) {
                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    long[] pattern = {0, 100, 100, 100, 100};
                    v.vibrate(pattern, -1);
                }

                updateLastDate(ctx);
            }

        }).other(new AppChecker.Listener() {
            @Override
            public void onForeground(String packageName) {
                if (isLockedScreen()) return;

                setStoreBoolean(ctx, "fbVisitedAnotherApp", true);

                msg = "Foreground: " + packageName;
                updateNotification(getCurrentStats(ctx));
                updateLastDate(ctx);
            }

        }).timeout(5000).start(this);
    }

    private String getCurrentStats(Context ctx) {
        Integer  fbTimeSpent = getStoreInt(ctx, "fbTimeSpent");
        Integer fbNumOfOpens = getStoreInt(ctx, "fbNumOfOpens");

        return String.format("Facebook: %s secs (%sx)",
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

    private void updateNotification(String message) {
        String title = "Recent usage stats";

        if (firstTime) {
            mBuilder.setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title)
                    .setPriority(NotificationCompat.PRIORITY_MIN)
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
