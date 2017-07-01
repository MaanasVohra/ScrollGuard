package com.rvalerio.foregroundappchecker.goodvibe.fcm;

import android.content.Context;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.RetryStrategy;
import com.firebase.jobdispatcher.Trigger;
import com.rvalerio.foregroundappchecker.goodvibe.helper.AlarmHelper;
import com.rvalerio.foregroundappchecker.goodvibe.helper.DateHelper;
import com.rvalerio.foregroundappchecker.goodvibe.helper.NetworkHelper;
import com.rvalerio.foregroundappchecker.goodvibe.main.ForegroundToastService;
import com.rvalerio.foregroundappchecker.goodvibe.main.Store;
import com.rvalerio.foregroundappchecker.goodvibe.main.StudyInfo;
import com.rvalerio.foregroundappchecker.goodvibe.personalize.StaticPersonalize;

import org.json.JSONArray;

import java.util.Locale;

import static com.rvalerio.foregroundappchecker.goodvibe.main.ForegroundToastService.getFirstK;
import static com.rvalerio.foregroundappchecker.goodvibe.main.ForegroundToastService.getLastK;

public class AppJobService extends JobService {

    private static final String TAG = "AppJobService";

    @Override
    public boolean onStartJob(final JobParameters jobParameters) {
        ForegroundToastService.updateServerRecords(getApplicationContext());
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }

    public static void updateServerThroughFirebaseJob(Context context) {
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));
        Job job = dispatcher.newJobBuilder()
                .setService(AppJobService.class)
                .setReplaceCurrent(true)
                .setLifetime(Lifetime.FOREVER)
                .setTrigger(Trigger.executionWindow(0, 60))
                .setConstraints(Constraint.ON_ANY_NETWORK)
                .setRetryStrategy(RetryStrategy.DEFAULT_LINEAR)
                .setTag(TAG)
                .build();
        dispatcher.mustSchedule(job);
    }

}
