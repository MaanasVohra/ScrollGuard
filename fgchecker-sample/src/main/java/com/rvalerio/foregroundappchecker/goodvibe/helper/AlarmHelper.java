package com.rvalerio.foregroundappchecker.goodvibe.helper;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.view.Gravity;
import android.widget.Toast;

/**
 * Helper.java
 * Created: 1/24/17
 * author: Fabian Okeke
 */

public class AlarmHelper {

    public static void showCenterToast(Context context, String msg) {
        Toast toast = Toast.makeText(context, msg, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    public static void showInstantNotif(Context context, String title, String message, String appIdToLaunch, Integer NOTIF_ID) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
        mBuilder.setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setAutoCancel(true)
                .setContentTitle(title)
                .setSound(getDefaultSound())
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setContentText(message);

        if (!appIdToLaunch.equals("")) {
            Intent launchAppIntent = IntentLauncher.getLaunchIntent(context, appIdToLaunch);
            PendingIntent contentIntent = PendingIntent.getActivity(context, 0, launchAppIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            mBuilder.setContentIntent(contentIntent);
        }

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(NOTIF_ID, mBuilder.build());
    }


    private static Uri getDefaultSound() {
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    }

}


