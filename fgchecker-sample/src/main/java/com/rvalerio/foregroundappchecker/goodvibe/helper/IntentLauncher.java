package com.rvalerio.foregroundappchecker.goodvibe.helper;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

/**
 * Created by fnokeke on 2/14/17.
 *
 */

public class IntentLauncher {

    public static Intent getLaunchIntent(Context context, String appPackageName) {
        PackageManager pm = context.getPackageManager();
        Intent appStartIntent = pm.getLaunchIntentForPackage(appPackageName);
        if (appStartIntent == null) {
            appStartIntent = pm.getLaunchIntentForPackage(context.getPackageName());
        }
        return appStartIntent;
    }

    public static void launchApp(Context context, String appPackageName) {
        context.startActivity(getLaunchIntent(context, appPackageName));
    }
}
