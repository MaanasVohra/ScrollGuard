package com.rvalerio.foregroundappchecker.goodvibe.main;

import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.crashlytics.android.Crashlytics;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.rvalerio.foregroundappchecker.R;
import com.rvalerio.foregroundappchecker.goodvibe.api.CallAPI;
import com.rvalerio.foregroundappchecker.goodvibe.api.VolleyJsonCallback;
import com.rvalerio.foregroundappchecker.goodvibe.fcm.AppJobService;
import com.rvalerio.foregroundappchecker.goodvibe.helper.AlarmHelper;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.Locale;

import io.fabric.sdk.android.Fabric;

import static android.view.View.GONE;


public class MainActivity extends AppCompatActivity {

    private Context mContext;
    private EditText etWorkerID;
    private EditText etStudyCode;
    private TextView tvSubmitFeedback;
    private TextView tvSurveyLink;
    private Button btnSubmitMturkID;
    private static Thread.UncaughtExceptionHandler mDefaultUEH;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_main);
        setResources();

        FirebaseMessaging.getInstance().subscribeToTopic("goodvibe");
        Fabric.with(this, new Crashlytics());
        mDefaultUEH = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(getUnCaughtExceptionHandler(mContext));
    }

    private static Thread.UncaughtExceptionHandler getUnCaughtExceptionHandler(final Context context) {
        return new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                Crashlytics.logException(ex);
                StackTraceElement ste = ex.getStackTrace()[0];
                String title = String.format("%s: Line%s", ste.getFileName(), ste.getLineNumber());
                String content = "Uh oh. Weird Error! Take a screenshot and send to researcher:\n\n" + Arrays.toString(ex.getStackTrace());
                AlarmHelper.showInstantNotif(context, title, content, "", 3490);
                mDefaultUEH.uncaughtException(thread, ex);
                ForegroundToastService.startMonitoringFacebookUsage(context);
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        promptIfNoFBInstalled();
        requestPermissionAndStartService();
        prepareToReceiveWorkerID();
    }

    private void setResources() {
        etWorkerID = (EditText) findViewById(R.id.et_mturk_id);
        etStudyCode = (EditText) findViewById(R.id.et_study_code);
        tvSubmitFeedback = (TextView) findViewById(R.id.tv_submit_id_feedback);
        tvSurveyLink = (TextView) findViewById(R.id.tv_survey_link);
        btnSubmitMturkID = (Button) findViewById(R.id.btn_submit_mturk_id);
    }

    private void promptIfNoFBInstalled() {
        PackageManager pm = mContext.getPackageManager();
        boolean isInstalled = Helper.isPackageInstalled(StudyInfo.FACEBOOK_PACKAGE_NAME, pm);
        if (!isInstalled) {
            String msg = "Error: cannot continue because your phone is not compatible with experiment.";
            showError(tvSubmitFeedback, msg);
            Store.setBoolean(mContext, Store.CAN_SHOW_PERMISSION_BTN, false);
        } else {
            Store.setBoolean(mContext, Store.CAN_SHOW_PERMISSION_BTN, true);
        }
    }

    private void requestPermissionAndStartService() {
        if (!Store.getBoolean(mContext, Store.CAN_SHOW_PERMISSION_BTN)) return;

        TextView tvPermission = (TextView) findViewById(R.id.tv_permission_text);
        tvPermission.setVisibility(View.VISIBLE);

        Button btUsagePermission = (Button) findViewById(R.id.btn_usage_permission);
        if (!needsUsageStatsPermission()) {
            tvPermission.setText(R.string.usage_permission_granted);
            btUsagePermission.setVisibility(GONE);
        } else {
            btUsagePermission.setVisibility(View.VISIBLE);
            btUsagePermission.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    requestUsageStatsPermission();
                }
            });
        }

        TextView tvServicePrompt = (TextView) findViewById(R.id.tv_fb_limit_prompt);
        tvServicePrompt.setVisibility(View.VISIBLE);

        Button btStartService = (Button) findViewById(R.id.btn_service_start);
        btStartService.setVisibility(View.VISIBLE);
        btStartService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ForegroundToastService.startMonitoringFacebookUsage(mContext);
                Toast.makeText(mContext, getString(R.string.service_started), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void prepareToReceiveWorkerID() {
        if (!Store.getBoolean(mContext, Store.CAN_SHOW_SUBMIT_BTN)) return;

        showPlain(etWorkerID, Store.getString(mContext, Store.WORKER_ID));
        showPlain(etStudyCode, Store.getString(mContext, Store.STUDY_CODE));
        showPlain(tvSubmitFeedback, Store.getString(mContext, Store.RESPONSE_TO_SUBMIT));
        showStudyInfo();

        btnSubmitMturkID.setVisibility(View.VISIBLE);
        btnSubmitMturkID.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!Helper.isNetworkAvailable(mContext)) {
                    String msg = "You do not have any network connection.";
                    showError(tvSubmitFeedback, msg);
                    return;
                }
                confirmAndSubmitDialog();
            }
        });
    }

    private void showStudyInfo() {
        String lastDay = StudyInfo.getLoggingStopDateStr(mContext);
        String surveyLink = Store.getString(mContext, Store.SURVEY_LINK);
        String msg = String.format(Locale.getDefault(), "%s\n(Study Ends: %s)", surveyLink, lastDay);
        if (!surveyLink.equals("")) {
            showPlain(tvSurveyLink, msg);
        }
    }

    private void confirmAndSubmitDialog() {
        new AlertDialog.Builder(mContext)
                .setTitle(R.string.title_confirm_id)
                .setMessage(R.string.message_confirm_id)
                .setIcon(R.drawable.ic_chart_pink)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        submitWorkerID();
                    }
                })
                .setNegativeButton(android.R.string.no, null).show();
    }

    private void submitWorkerID() {
        String workerId = etWorkerID.getText().toString().toLowerCase().trim();
        String studyCode = etStudyCode.getText().toString().toLowerCase().trim();
        if (workerId.equals("") || studyCode.equals("")) {
            showError(tvSubmitFeedback, "Valid input required.");
            return;
        }

        etWorkerID.setText(workerId);
        etStudyCode.setText(studyCode);
        Store.setString(mContext, Store.WORKER_ID, workerId);
        Store.setString(mContext, Store.STUDY_CODE, studyCode);
        logCrashAnalyticsUser(workerId);

        JSONObject params = new JSONObject();
        Helper.setJSONValue(params, "worker_id", workerId);
        Helper.setJSONValue(params, "study_code", studyCode);
        Helper.setJSONValue(params, "firebase_token", FirebaseInstanceId.getInstance().getToken());

        JSONObject deviceInfo = DeviceInfo.getPhoneDetails(mContext);
        Helper.copy(deviceInfo, params);
        CallAPI.submitTurkPrimeID(mContext, params, submitIDResponseHandler);
    }

    private void logCrashAnalyticsUser(String workerId) {
        Crashlytics.setUserIdentifier(workerId);
    }


    VolleyJsonCallback submitIDResponseHandler = new VolleyJsonCallback() {
        @Override
        public void onConnectSuccess(JSONObject result) {
            Log.i("submitIDSuccess: ", result.toString());
            String response = result.optString("response");
            if (result.optInt("status") == 200) {
                Store.setBoolean(mContext, Store.ENROLLED, true);
                String studyCode = etStudyCode.getText().toString().toLowerCase().trim();
                StudyInfo.setDefaults(mContext, studyCode); //fallback to defaults if admin values are not set
                AutoUpdateAlarm.getInstance().setAlarmForPeriodicUpdate(mContext);

                showSuccess(tvSubmitFeedback, response);
                Store.setString(mContext, Store.RESPONSE_TO_SUBMIT, response);
                Toast.makeText(mContext, "Successfully submitted.", Toast.LENGTH_SHORT).show();

                String surveyLink = result.optString("survey_link");
                Store.setString(mContext, Store.SURVEY_LINK, surveyLink);
                showStudyInfo();

            } else {
                tvSurveyLink.setVisibility(View.GONE);
                Store.setBoolean(mContext, Store.ENROLLED, false);
                showError(tvSubmitFeedback, response);
            }
        }

        @Override
        public void onConnectFailure(VolleyError error) {
            String msg = "Error submitting your worker id. Please contact researcher. \n\nError details:\n" + error.toString();
            tvSurveyLink.setVisibility(View.GONE);
            showError(tvSubmitFeedback, msg);
        }
    };

    private void showError(TextView tv, String msg) {
        showPlain(tv, msg);
        tv.setTextColor(Color.RED);
    }

    private void showSuccess(TextView tv, String msg) {
        showPlain(tv, msg);
        tv.setTextColor(Color.BLUE);
    }

    private void showPlain(TextView tv, String msg) {
        tv.setText(msg);
        tv.setVisibility(View.VISIBLE);
    }

    private boolean needsUsageStatsPermission() {
        return postLollipop() && !hasUsageStatsPermission(this);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void requestUsageStatsPermission() {
        if (!hasUsageStatsPermission(this)) {
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        }
    }

    private boolean postLollipop() {
        return android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)


    private boolean hasUsageStatsPermission(Context context) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow("android:get_usage_stats",
                android.os.Process.myUid(), context.getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    public static void crashApp() {
        String x = null;
        x.equals("");
    }

}
