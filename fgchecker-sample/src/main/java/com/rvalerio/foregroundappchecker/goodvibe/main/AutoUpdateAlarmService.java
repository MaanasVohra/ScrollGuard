package com.rvalerio.foregroundappchecker.goodvibe.main;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class AutoUpdateAlarmService extends Service
{
    AutoUpdateAlarm autoUpdateAlarm = AutoUpdateAlarm.getInstance();
    public void onCreate()
    {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        autoUpdateAlarm.setAlarmForPeriodicUpdate(this);
        return START_STICKY;
    }

    @Override
    public void onStart(Intent intent, int startId)
    {
        autoUpdateAlarm.setAlarmForPeriodicUpdate(this);
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }
}