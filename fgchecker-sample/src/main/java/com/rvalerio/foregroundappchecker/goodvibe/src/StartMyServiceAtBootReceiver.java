package com.rvalerio.foregroundappchecker.goodvibe.src;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Restart service on reboot
 * Created by fnokeke on 1/20/17.
 */

public class StartMyServiceAtBootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            new AutoUpdateAlarm().setAlarmForPeriodicUpdate(context);
        }
    }

}
