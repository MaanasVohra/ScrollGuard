package com.rvalerio.foregroundappchecker.goodvibe.personalize;

import android.content.Context;

import com.rvalerio.foregroundappchecker.goodvibe.helper.DateHelper;
import com.rvalerio.foregroundappchecker.goodvibe.main.Store;

import org.json.JSONArray;

/**
 * Created by fnokeke on 5/29/17.
 * Personalized to Facebook usage from just one period
 */

public class StaticPersonalize {

    public static final String ALL_TIME_SPENT = "allStoredTimeSpent";
    static final String ALL_NUM_OF_OPENS = "allStoredNumOfOpens";
    static final String CURRENT_AVG_TIME_SPENT = "currentAvgTimeSpent";
    static final String CURRENT_AVG_NUM_OF_OPENS = "currentAvgNumOfOpens";
    private static final int FIRST_N_DAYS = 7;

    private Context mContext;
    private String mTreatmentStartDateStr;

    public StaticPersonalize(Context context, String treatmentStartDateStr) {
        mContext = context;
        mTreatmentStartDateStr = treatmentStartDateStr;
    }

    Context getCxt() {
        return mContext;
    }

    String getTreatStart() {
        return mTreatmentStartDateStr;
    }

    public int getAverageTimeSpent() {
        return Store.getInt(mContext, CURRENT_AVG_TIME_SPENT);
    }

    public int getAverageTimeOpen() {
        return Store.getInt(mContext, CURRENT_AVG_NUM_OF_OPENS);
    }

    public void addDataPoint(int timeSpentMinutes, int noOfOpen) {
        if (!DateHelper.isPastMidnightOfDate(mTreatmentStartDateStr)) {
            insertDataIntoStore(timeSpentMinutes, noOfOpen);
            computeAndStoreNewAverage(ALL_TIME_SPENT, CURRENT_AVG_TIME_SPENT, FIRST_N_DAYS);
            computeAndStoreNewAverage(ALL_NUM_OF_OPENS, CURRENT_AVG_NUM_OF_OPENS, FIRST_N_DAYS);
        }
    }

    void insertDataIntoStore(int timeSpentMinutes, int noOfOpen) {
        JSONArray allTimeSpent = Store.getJsonArray(mContext, ALL_TIME_SPENT);
        allTimeSpent.put(timeSpentMinutes);
        Store.setJsonArray(mContext, ALL_TIME_SPENT, allTimeSpent);

        JSONArray allOpens = Store.getJsonArray(mContext, ALL_NUM_OF_OPENS);
        allOpens.put(noOfOpen);
        Store.setJsonArray(mContext, ALL_NUM_OF_OPENS, allOpens);
    }

    private void computeAndStoreNewAverage(String storeKey, String avgKey, int firstKDays) {
        JSONArray allStoredValues = Store.getJsonArray(mContext, storeKey);
        if (allStoredValues.length() == 0) return;

        float total = 0;
        int limit = firstKDays <= allStoredValues.length() ? firstKDays : allStoredValues.length();
        for (int i = 0; i < limit; i++) {
            total += allStoredValues.optInt(i);
        }
        Store.setInt(mContext, avgKey, Math.round(total / limit));
    }

}
