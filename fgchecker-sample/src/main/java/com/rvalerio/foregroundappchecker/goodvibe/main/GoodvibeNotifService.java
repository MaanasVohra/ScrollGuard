package com.rvalerio.foregroundappchecker.goodvibe.main;

import android.content.Intent;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.widget.Toast;

import com.rvalerio.foregroundappchecker.goodvibe.helper.DateHelper;

public class GoodvibeNotifService extends NotificationListenerService {

    @Override
    public IBinder onBind(Intent intent) {
        Toast.makeText(this, "notification in bind state.", Toast.LENGTH_SHORT).show();
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
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        Log.i("NotifyService", "got notification");
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        String message = String.format("Removed: %s @ %s", sbn.getPackageName(), DateHelper.millisToDateFormat(sbn.getPostTime()));
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        Log.i("NotifyService", "removed notification");
    }
}

