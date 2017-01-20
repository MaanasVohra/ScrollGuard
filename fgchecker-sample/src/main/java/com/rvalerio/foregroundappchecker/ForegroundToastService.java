package com.rvalerio.foregroundappchecker;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.widget.Toast;
import android.os.Vibrator;
import java.text.SimpleDateFormat;
import java.util.Date;


import com.rvalerio.fgchecker.AppChecker;


public class ForegroundToastService extends Service {

    private final static int NOTIFICATION_ID = 1234;
    private final static String STOP_SERVICE = ForegroundToastService.class.getPackage()+".stop";

    private BroadcastReceiver stopServiceReceiver;
    private AppChecker appChecker;
    private Integer gmailTimeSpent = 0;
    private Integer gmailNumOfOpens = 1;
    private Boolean gmailVisitedAnotherApp = false;

    private Integer fbTimeSpent = 0;
    private Integer fbNumOfOpens = 1;
    private Boolean fbVisitedAnotherApp = false;

    private String msg;
    private String lastRecordedDate="";

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

    private void startChecker() {

        // reset stat every day
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        if (!lastRecordedDate.equals(today)) {
            lastRecordedDate = today;

            gmailTimeSpent = 0;
            gmailNumOfOpens = 1;

            fbTimeSpent = 0;
            fbNumOfOpens = 1;
            Toast.makeText(getBaseContext(), "Stats reset for new day " + today, Toast.LENGTH_LONG).show();
        }


        appChecker = new AppChecker();
        appChecker
                .when(getPackageName(), new AppChecker.Listener() {
                    @Override
                    public void onForeground(String packageName) {
                        gmailVisitedAnotherApp = true;
                        fbVisitedAnotherApp = true;

//                        Toast.makeText(getBaseContext(), "Beehive app already in foreground.", Toast.LENGTH_SHORT).show();
                    }
                })
                .when("com.google.android.gm", new AppChecker.Listener() {
                    @Override
                    public void onForeground(String packageName) {
                        gmailTimeSpent += 5;
                        if (gmailVisitedAnotherApp) {
                            gmailNumOfOpens += 1;
                            gmailVisitedAnotherApp = false;
                        }
                        msg = "Gmail (limit: 3 mins / 5x)\nTime spent: " + gmailTimeSpent.toString() + " secs (opened: " + gmailNumOfOpens.toString() + "x)";
                        Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();

                        if (gmailTimeSpent > 180 || gmailNumOfOpens > 5) {
                            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                            // Start without a delay
                            // Each element then alternates (in ms) between vibrate, sleep, vibrate, sleep...
                            long[] pattern = {0, 100, 100, 100, 100};

                            // The '-1' here means to vibrate once, as '-1' is out of bounds in the pattern array
                            v.vibrate(pattern, -1);
                        }
                    }

                })
                .when("com.facebook.katana", new AppChecker.Listener() {
                    @Override
                    public void onForeground(String packageName) {
                        fbTimeSpent += 5;
                        if (fbVisitedAnotherApp) {
                            fbNumOfOpens += 1;
                            fbVisitedAnotherApp = false;
                        }
                        msg = "Facebook (limit: 5 mins / 10x)\nTime spent: " + fbTimeSpent.toString() + " secs (opened: " + fbNumOfOpens.toString() + "x)";
                        Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();

                        if (fbTimeSpent > 300 || fbNumOfOpens > 10) {
                            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                            // Start without a delay
                            // Each element then alternates (in ms) between vibrate, sleep, vibrate, sleep...
                            long[] pattern = {0, 100, 100, 100, 100, 100, 100};

                            // The '-1' here means to vibrate once, as '-1' is out of bounds in the pattern array
                            v.vibrate(pattern, -1);
                        }
                    }

                })
                .other(new AppChecker.Listener() {
                    @Override
                    public void onForeground(String packageName) {
                        gmailVisitedAnotherApp = true;
                        fbVisitedAnotherApp = true;
//                        Toast.makeText(getBaseContext(), "Foreground: " + packageName, Toast.LENGTH_SHORT).show();
                    }
                })
                .timeout(5000)
                .start(this);
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
        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setAutoCancel(false)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.stop_service))
                .setContentIntent(PendingIntent.getBroadcast(this, 0, new Intent(STOP_SERVICE), PendingIntent.FLAG_UPDATE_CURRENT))
                .setWhen(0)
                .build();
        manager.notify(NOTIFICATION_ID, notification);
        return notification;
    }

    private void removeNotification() {
        NotificationManager manager = ((NotificationManager) getSystemService(NOTIFICATION_SERVICE));
        manager.cancel(NOTIFICATION_ID);
    }
}
