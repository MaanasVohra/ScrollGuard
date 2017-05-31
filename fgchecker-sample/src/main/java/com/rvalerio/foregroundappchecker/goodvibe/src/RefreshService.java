package com.rvalerio.foregroundappchecker.goodvibe.src;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;

public class RefreshService extends Service {

    Context mContext;

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ForegroundToastService.start(mContext);
        return super.onStartCommand(intent, flags, startId);
    }

    public static void startRefreshInIntervals(Context context) {
        Intent refreshIntent = new Intent(context, RefreshService.class);
        PendingIntent pendingRefreshIntent = PendingIntent.getService(context, 0, refreshIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
//        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), 2 * AlarmManager.INTERVAL_HOUR, pendingRefreshIntent);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), AlarmManager.INTERVAL_HOUR, pendingRefreshIntent);
    }

//    public static void start(Context context) {
//        context.startService(new Intent(context, RefreshService.class));
//    }

}
