package com.rvalerio.foregroundappchecker.goodvibe.personalize;

import android.content.Context;

import com.rvalerio.foregroundappchecker.goodvibe.helper.DateHelper;
import com.rvalerio.foregroundappchecker.goodvibe.src.Store;

import org.json.JSONArray;

import java.util.Date;

/**
 * Created by fnokeke on 5/29/17.
 * Personalized to Facebook usage from just one period
 */

public class StaticPersonalize {

    public static final String ALL_TIME_SPENT = "allStoredTimeSpent";
    public static final String ALL_NUM_OF_OPENS = "allStoredNumOfOpens";
    public static final int FIRST_7_DAYS = 7;

    public static void addDataPoint(Context context, int timeSpent, int noOfOpen, String beforeMidnightOfDate) {
        if (isPast(beforeMidnightOfDate)) return;

        JSONArray allTimeSpent = Store.getJsonArray(context, ALL_TIME_SPENT);
        allTimeSpent.put(timeSpent);
        Store.setJsonArray(context, ALL_TIME_SPENT, allTimeSpent);

        JSONArray allOpens = Store.getJsonArray(context, ALL_NUM_OF_OPENS);
        allOpens.put(noOfOpen);
        Store.setJsonArray(context, ALL_NUM_OF_OPENS, allOpens);
    }

    private static boolean isPast(String beforeMidnightOfDateStr) {
        long rightNow = System.currentTimeMillis();
        Date beforeMidnightOfDate = DateHelper.strToDate(beforeMidnightOfDateStr);
        return rightNow > beforeMidnightOfDate.getTime();
    }

    public static int getAverage(Context context, String storeKey, int firstKDays) {
        JSONArray allTimeSpent = Store.getJsonArray(context, storeKey);
        float total = 0;
        int limit = firstKDays <= allTimeSpent.length() ? firstKDays : allTimeSpent.length();
        for (int i = 0; i < limit; i++) {
            total += allTimeSpent.optInt(i);
        }
        return Math.round(total/limit);
    }

}
