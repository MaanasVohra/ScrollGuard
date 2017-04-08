package com.rvalerio.foregroundappchecker;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static android.content.Context.TELEPHONY_SERVICE;

/**
 * Helper for all classes
 * Created by fnokeke on 1/26/17.
 */

class Helper {
    private static Locale LOCALE = Locale.getDefault();

    static void copy(JSONObject from, JSONObject to) {
        for (int i = 0; i < from.names().length(); i++) {
            String key = from.names().optString(i);
            Object value = from.opt(key);
            setJSONValue(to, key, value);
        }
    }

    static void setJSONValue(JSONObject jsonObject, String key, Object value) {
        try {
            jsonObject.put(key, value);
        } catch (JSONException je) {
            je.printStackTrace();
        }
    }

    static String getTodayDateStr() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime());
    }

    static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


    static String dateToStr(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", LOCALE);
        return sdf.format(date);
    }

    static Date strToDate(String dateStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", LOCALE);
        Date formattedDate = new Date();
        try {
            formattedDate = sdf.parse(dateStr);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return formattedDate;
    }

    static Date addDays(Date date, int days)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, days); //minus number would decrement the days
        return cal.getTime();
    }

    public boolean isEqualOrGreater(String mainDateStr, String compareDateStr) {
        Date mainDate = strToDate(mainDateStr);
        Date compareDate = strToDate(compareDateStr);
        return mainDate.getTime() >= compareDate.getTime();
    }

    static boolean isPackageInstalled(String packagename, PackageManager packageManager) {
        try {
            packageManager.getPackageInfo(packagename, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private static Uri getDefaultSound() {
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    }

    static void showInstantNotif(Context context, String title, String message) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
        mBuilder.setSmallIcon(R.drawable.ic_chart_pink)
                .setAutoCancel(true)
                .setContentTitle(title)
                .setSound(getDefaultSound())
                .setContentText(message);

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(5555, mBuilder.build());
    }

    public static String getTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd h:mm:ss a", Locale.getDefault()).format(Calendar.getInstance().getTime());
    }
}
