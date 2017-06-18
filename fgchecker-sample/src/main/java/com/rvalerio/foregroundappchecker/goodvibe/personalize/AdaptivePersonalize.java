package com.rvalerio.foregroundappchecker.goodvibe.personalize;

import android.content.Context;

import com.rvalerio.foregroundappchecker.goodvibe.helper.DateHelper;
import com.rvalerio.foregroundappchecker.goodvibe.main.Store;

import org.json.JSONArray;

import java.util.Date;

/**
 * Created by fnokeke on 5/29/17.
 * Personalized to Facebook usage from period to period
 */

public class AdaptivePersonalize extends StaticPersonalize {
    private static final int LAST_N_DAYS = 7;

    public AdaptivePersonalize(Context context, String treatmentStartDateStr) {
        super(context, treatmentStartDateStr);
    }

    public void addDataPoint(int timeSpent, int noOfOpen) {
        insertDataIntoStore(timeSpent, noOfOpen);
        if (canComputeNewAvg(getTreatStart())) {
            computeAndStoreNewAverage(ALL_TIME_SPENT, CURRENT_AVG_TIME_SPENT, LAST_N_DAYS);
            computeAndStoreNewAverage(ALL_NUM_OF_OPENS, CURRENT_AVG_NUM_OF_OPENS, LAST_N_DAYS);
        }
    }

    private void computeAndStoreNewAverage(String storeKey, String avgKey, int lastKDays) {
        JSONArray allTimeSpent = Store.getJsonArray(getCxt(), storeKey);
        if (allTimeSpent.length() == 0) return;
        float total = 0;
        int limit = lastKDays <= allTimeSpent.length() ? lastKDays : allTimeSpent.length();
        for (int i = limit - 1; i >= 0; i--) {
            total += allTimeSpent.optInt(i);
        }
        Store.setInt(getCxt(), avgKey, Math.round(total / limit));
    }

    private static boolean canComputeNewAvg(String treatmentStartDateStr) {
        Date treatmentStartDate = DateHelper.strToDate(treatmentStartDateStr);
        Date today = DateHelper.strToDate(DateHelper.getTodayDateStr());
        final long oneDayInMillis = 24 * 60 * 60 * 1000;
        long diffInDays = (today.getTime() - treatmentStartDate.getTime()) / oneDayInMillis;
        return diffInDays > LAST_N_DAYS;
    }
}

