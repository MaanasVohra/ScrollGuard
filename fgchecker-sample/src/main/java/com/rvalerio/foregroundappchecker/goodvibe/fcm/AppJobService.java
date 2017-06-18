package com.rvalerio.foregroundappchecker.goodvibe.fcm;

import android.content.Context;
import android.util.Log;

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
        Log.d(TAG, "Performing long running task in scheduled job");
        new Thread(new Runnable() {
            @Override
            public void run() {
                completeJob(jobParameters);
            }
        }).start();
        return true;
    }

    public void completeJob(final JobParameters parameters) {
        ForegroundToastService.updateServerRecords(getApplicationContext());
        jobFinished(parameters, false);
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }

    public static void scheduleFirebaseJob(Context context) {
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));
        Job job = createDispatcherJob(dispatcher);
        dispatcher.mustSchedule(job);
    }

    public static Job createDispatcherJob(FirebaseJobDispatcher dispatcher) {
        int oneHour = 3600;
        return dispatcher.newJobBuilder()
                .setLifetime(Lifetime.FOREVER)
                .setService(AppJobService.class)
                .setRecurring(true)
                .setTrigger(Trigger.executionWindow(10 * 60, oneHour))
                .setConstraints(Constraint.ON_ANY_NETWORK)
                .setRetryStrategy(RetryStrategy.DEFAULT_LINEAR)
                .setTag("GoodVibeJob")
                .build();
    }

}
