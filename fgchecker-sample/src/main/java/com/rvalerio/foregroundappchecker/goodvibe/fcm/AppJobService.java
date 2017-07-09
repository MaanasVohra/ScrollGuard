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
import com.rvalerio.foregroundappchecker.goodvibe.main.ForegroundToastService;

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
                .setTrigger(Trigger.executionWindow(0, 2)) //// FIXME: 7/9/17 change to (0,60)
                .setConstraints(Constraint.ON_ANY_NETWORK)
                .setRetryStrategy(RetryStrategy.DEFAULT_LINEAR)
                .setTag(TAG)
                .build();
        dispatcher.mustSchedule(job);
    }

}
