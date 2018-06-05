package com.rvalerio.foregroundappchecker.goodvibe.main;

import android.content.Intent;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.widget.Toast;

import com.rvalerio.foregroundappchecker.goodvibe.helper.DateHelper;
import com.rvalerio.foregroundappchecker.goodvibe.helper.FileHelper;

import java.util.Locale;

public class GoodvibeNotifService extends NotificationListenerService {
    private String lastMsgPosted = "";
    private String lastMsgRemoved = "";
    private final String IGNORE_LIST = "com.android.systemui";

    @Override
    public IBinder onBind(Intent intent) {
        Toast.makeText(this, "Notification in bind state.", Toast.LENGTH_SHORT).show();
        startService(intent);
        return super.onBind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Toast.makeText(this, "Permission is necessary for app to work properly.", Toast.LENGTH_LONG).show();
        stopService(intent);
        return super.onUnbind(intent);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String message = String.format("Posted: %s @ %s", sbn.getPackageName(), DateHelper.millisToDateFormat(sbn.getPostTime()));
        if (message.equals(lastMsgPosted) || IGNORE_LIST.contains(sbn.getPackageName())) return;
        lastMsgPosted = message;

        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        Log.i("NotifyService", message);

        String data = String.format(Locale.getDefault(), "%d, %s, %d, %s;\n",
                System.currentTimeMillis(),
                sbn.getPackageName(),
                sbn.getPostTime(),
                "posted"
        );
        FileHelper.appendToFile(getApplicationContext(), Store.PHONE_NOTIF_LOGS_CSV, data);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        String message = String.format("Removed: %s @ %s", sbn.getPackageName(), DateHelper.millisToDateFormat(sbn.getPostTime()));
        if (message.equals(lastMsgRemoved) || IGNORE_LIST.contains(sbn.getPackageName())) return;
        lastMsgRemoved = message;

        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        Log.i("NotifyService", message);

        String data = String.format(Locale.getDefault(), "%d, %s, %d, %s;\n",
                System.currentTimeMillis(),
                sbn.getPackageName(),
                sbn.getPostTime(),
                "removed"
        );
        FileHelper.appendToFile(getApplicationContext(), Store.PHONE_NOTIF_LOGS_CSV, data);
    }
}