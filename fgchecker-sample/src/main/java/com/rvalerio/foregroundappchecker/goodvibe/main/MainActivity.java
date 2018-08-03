package com.rvalerio.foregroundappchecker.goodvibe.main;

import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.NotificationManagerCompat;
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
import com.rvalerio.foregroundappchecker.goodvibe.helper.AlarmHelper;
import com.rvalerio.foregroundappchecker.goodvibe.helper.DateHelper;
import com.rvalerio.foregroundappchecker.goodvibe.helper.FileHelper;
import com.rvalerio.foregroundappchecker.goodvibe.helper.IntentLauncher;
import com.rvalerio.foregroundappchecker.goodvibe.helper.JsonHelper;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

import io.fabric.sdk.android.Fabric;

import static android.view.View.GONE;


public class MainActivity extends AppCompatActivity {

    private Context mContext;
    private EditText etUsername;
    private EditText etStudyCode;
    private TextView tvSubmitFeedback;
    private static Thread.UncaughtExceptionHandler mDefaultUEH;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initResources();
        handleIncomingBundle();
        if (!userIsEnrolled()) enrollUser();
        populateStoredInfo();
        ForegroundToastService.startMonitoring(mContext);
    }

    private void initResources() {
        etUsername = findViewById(R.id.et_username);
        etStudyCode = findViewById(R.id.et_study_code);
        tvSubmitFeedback = findViewById(R.id.tv_submit_id_feedback);
        activateUsagePermission();
        FileHelper.prepareAllStorageFiles(mContext);
    }

    private void activateUsagePermission() {
        Button btUsagePermission = findViewById(R.id.btn_usage_permission);
        if (!needsUsageStatsPermission()) {
            btUsagePermission.setVisibility(GONE);
            TextView tvPermission = findViewById(R.id.tv_permission_desc);
            tvPermission.setText(R.string.usage_permission_granted);
        } else {
            btUsagePermission.setVisibility(View.VISIBLE);
            btUsagePermission.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!hasUsageStatsPermission(mContext)) {
                        startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                    }
                }
            });
        }
    }

    private void handleIncomingBundle() {
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            String username = bundle.getString("username");
            String code = bundle.getString("code");
            if (username != null && code != null) {
                Store.setString(mContext, Store.BUNDLE_USERNAME, username);
                Store.setString(mContext, Store.BUNDLE_CODE, code);
                Toast.makeText(mContext, "AppLogger successfully linked.", Toast.LENGTH_SHORT).show();
                Store.setBoolean(mContext, Store.IS_ACTIVE_BACK_TO_MAIN_APP, true);

                activateBackToMainApp();

                intent.removeExtra("username");
                intent.removeExtra("code");
            }
        }
    }

    private void activateBackToMainApp() {
        Button btnGoBack = findViewById(R.id.btn_go_back);
        btnGoBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IntentLauncher.launchApp(mContext, "io.smalldata.beehiveapp");
            }
        });

        if (Store.getBoolean(mContext, Store.IS_ACTIVE_BACK_TO_MAIN_APP)) {
            btnGoBack.setVisibility(View.VISIBLE);
        } else {
            btnGoBack.setVisibility(View.INVISIBLE);
        }

    }

    private void enrollUser() {
        if (isActiveCustomEntryMode()) {
            activateUserCustomEntryMode();
        } else {
            activateReceivedEntryMode();
        }
    }

    private void activateReceivedEntryMode() {
        JSONObject params = new JSONObject();
        JsonHelper.setJSONValue(params, "worker_id", StudyInfo.getUsername(mContext));
        JsonHelper.setJSONValue(params, "username", StudyInfo.getUsername(mContext));
        JsonHelper.setJSONValue(params, "study_code", StudyInfo.getCode(mContext));
        JsonHelper.setJSONValue(params, "code", StudyInfo.getCode(mContext));
        Helper.setJSONValue(params, "firebase_token", FirebaseInstanceId.getInstance().getToken());
        JSONObject deviceInfo = DeviceInfo.getPhoneDetails(mContext);
        Helper.copy(deviceInfo, params);
        if (!StudyInfo.getUsername(mContext).equals("") && !StudyInfo.getCode(mContext).equals("")) {
            CallAPI.submitTurkPrimeID(mContext, params, submitIDResponseHandler);
        }
    }


    private boolean isNotificationServiceEnabled() {
        boolean enabled = false;
        Set<String> alreadyEnabled = NotificationManagerCompat.getEnabledListenerPackages(mContext);
        for (String appName : alreadyEnabled) {
            if (appName.contains(getPackageName())) {
                enabled = true;
                break;
            }
        }
        return enabled;
    }

    private AlertDialog buildNotificationServiceAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("Goodvibe Permission");
        builder.setMessage("This permission is necessary for research studies that monitor how frequently apps on your phone send you notifications.");
        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
//                Toast.makeText(mContext, "You clicked yes!", Toast.LENGTH_SHORT).show();
//                ForegroundToastService.startMonitoringFacebookUsage(mContext);
            }
        });
        builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Toast.makeText(mContext, "Your permission is needed for app to work :(", Toast.LENGTH_SHORT).show();
            }
        });
        return builder.create();
    }

    private void startUserHomeConfigMode() {
        if (userIsEnrolled()) {
            startActivity(new Intent(mContext, HomeActivity.class));
        } else {
            FirebaseMessaging.getInstance().subscribeToTopic("goodvibe");
            Fabric.with(this, new Crashlytics());
            mDefaultUEH = Thread.getDefaultUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler(getUnCaughtExceptionHandler(mContext));
        }
    }

    private boolean userIsEnrolled() {
        return Store.getBoolean(mContext, Store.IS_ENROLLED);
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
            }
        };
    }


    private void activateUserCustomEntryMode() {
        Button btnSubmitMturkID = findViewById(R.id.btn_submit_mturk_id);
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

    private boolean isActiveCustomEntryMode() {
        return Store.getBoolean(mContext, Store.IS_ACTIVE_CUSTOM_MODE);
    }

    private void populateStoredInfo() {
        String username;
        if (isActiveCustomEntryMode()) {
            username = Store.getString(mContext, Store.WORKER_ID);
            etUsername.setEnabled(true);
        } else {
            username = Store.getString(mContext, Store.BUNDLE_USERNAME);
            String code = Store.getString(mContext, Store.BUNDLE_CODE);
            username = username.equals("") ? "" : String.format("%s (%s)", username, code);
            etUsername.setEnabled(false);
        }
        showPlain(etUsername, username);
        showPlain(tvSubmitFeedback, Store.getString(mContext, Store.RESPONSE_TO_SUBMIT));
    }

    private void showStudyInfo() {
        String lastDay = StudyInfo.getLoggingStopDateStr(mContext);
        String surveyLink = Store.getString(mContext, Store.SURVEY_LINK);
        String msg = String.format(Locale.getDefault(), "%s\n(Study Ends: %s)", surveyLink, lastDay);
//        if (!surveyLink.equals("")) {
//            showPlain(tvSurveyLink, msg);
//        }
    }

    private void confirmAndSubmitDialog() {
        new AlertDialog.Builder(mContext)
                .setTitle(R.string.title_confirm_id)
                .setMessage(R.string.message_confirm_id)
                .setIcon(R.drawable.ic_chart_pink)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        submitCustomUsername();
                    }
                })
                .setNegativeButton(android.R.string.no, null).show();
    }

    private void submitCustomUsername() {
        String username = etUsername.getText().toString().toLowerCase().trim();
        String studyCode = "mobile";
        if (username.equals("") || studyCode.equals("")) {
            showError(tvSubmitFeedback, "Valid input required.");
            return;
        }

        etUsername.setText(username);
        etStudyCode.setText(studyCode);
        Store.setString(mContext, Store.WORKER_ID, username);
        StudyInfo.setCode(mContext, studyCode);
//        logCrashAnalyticsUser(username); fixme uncomment

        JSONObject params = new JSONObject();
        Helper.setJSONValue(params, "worker_id", username);
        Helper.setJSONValue(params, "study_code", studyCode);
        Helper.setJSONValue(params, "firebase_token", FirebaseInstanceId.getInstance().getToken());

        JSONObject deviceInfo = DeviceInfo.getPhoneDetails(mContext);
        Helper.copy(deviceInfo, params);
        CallAPI.submitTurkPrimeID(mContext, params, submitIDResponseHandler);
    }

    private void logCrashAnalyticsUser(String username) {
        Crashlytics.setUserIdentifier(username);
    }


    VolleyJsonCallback submitIDResponseHandler = new VolleyJsonCallback() {
        @Override
        public void onConnectSuccess(JSONObject result) {
            Log.i("submitIDSuccess: ", result.toString());
            String response = result.optString("response");
            if (result.optInt("status") == 200) {
                String studyCode = etStudyCode.getText().toString().toLowerCase().trim();
                StudyInfo.setDefaults(mContext, studyCode); //fallback to defaults if admin values are not set
                AutoUpdateAlarm.getInstance().setAlarmForPeriodicUpdate(mContext);

                showSuccess(tvSubmitFeedback, response);
                Store.setString(mContext, Store.RESPONSE_TO_SUBMIT, response);
                Toast.makeText(mContext, "Successfully linked AppLogger.", Toast.LENGTH_SHORT).show();

                String surveyLink = result.optString("survey_link");
                Store.setString(mContext, Store.SURVEY_LINK, surveyLink);
                showStudyInfo();

                Store.setBoolean(mContext, Store.IS_ENROLLED, true);

                if (Store.getBoolean(mContext, Constants.USER_FULL_CONFIG_ENABLED)) {
                    ConfigActivity.initAllAppList(mContext, true);
                }

                ForegroundToastService.startMonitoring(mContext);

            } else {
//                tvSurveyLink.setVisibility(View.GONE);
                Store.setBoolean(mContext, Store.IS_ENROLLED, false);
                showError(tvSubmitFeedback, response);
            }
        }

        @Override
        public void onConnectFailure(VolleyError error) {
            String msg = "Error submitting your id. Please contact researcher. \n\nError details:\n" + error.toString();
//            tvSurveyLink.setVisibility(View.GONE);
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
    private void startRequestOrMonitoringService() {
        if (!hasUsageStatsPermission(this)) {
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        }
//        } else {
//            ForegroundToastService.startMonitoringFacebookUsage(mContext);
//            Toast.makeText(mContext, getString(R.string.service_started), Toast.LENGTH_SHORT).show();
//        }
    }

    private boolean postLollipop() {
        return android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)


    private boolean hasUsageStatsPermission(Context context) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = 0;
        if (appOps != null) {
            mode = appOps.checkOpNoThrow("android:get_usage_stats", android.os.Process.myUid(), context.getPackageName());
        }
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    public static void crashApp() {
        String x = null;
        x.equals("");
    }

    private void requestNotificationMonitoringPermission() {
        if (!isNotificationServiceEnabled()) {
            buildNotificationServiceAlertDialog().show();
        }
    }


}
