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

    public AdaptivePersonalize(Context context) {
        super(context);
    }

    public void addDataPoint(int timeSpentMinutes, int noOfOpen) {
        insertDataIntoStore(timeSpentMinutes, noOfOpen);
        computeAndStoreNewAverage(ALL_TIME_SPENT, CURRENT_AVG_TIME_SPENT, LAST_N_DAYS);
        computeAndStoreNewAverage(ALL_NUM_OF_OPENS, CURRENT_AVG_NUM_OF_OPENS, LAST_N_DAYS);
    }

    private void computeAndStoreNewAverage(String storeKey, String avgKey, int lastKDays) {
        JSONArray allStoredValues = Store.getJsonArray(getCxt(), storeKey);
        if (allStoredValues.length() == 0) return;
        float total = 0;
        int limit = lastKDays <= allStoredValues.length() ? lastKDays : allStoredValues.length();
        int lastIndex = allStoredValues.length() - 1;
        int value;
        for (int i = 0; i < limit; i++) {
            value = allStoredValues.optInt(lastIndex - i);
            total += value;
        }
        Store.setInt(getCxt(), avgKey, Math.round(total / limit));
    }

}

