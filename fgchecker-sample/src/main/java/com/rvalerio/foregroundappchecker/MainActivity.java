package com.rvalerio.foregroundappchecker;

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

import org.json.JSONObject;

import io.smalldata.api.CallAPI;
import io.smalldata.api.VolleyJsonCallback;

import static android.view.View.GONE;
import static com.rvalerio.foregroundappchecker.Store.getStoreBoolean;
import static com.rvalerio.foregroundappchecker.Store.getStoreString;
import static com.rvalerio.foregroundappchecker.Store.setStoreBoolean;
import static com.rvalerio.foregroundappchecker.Store.setStoreString;


public class MainActivity extends AppCompatActivity {

    private Context mContext;
    private EditText etWorkerID;
    private TextView tvSubmitFeedback;
    private TextView tvSurveyLink;
    private Button btnSubmitMturkID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;
        setResources();
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
            setStoreBoolean(mContext, Store.CAN_SHOW_PERMISSION_BTN, false);
        } else {
            setStoreBoolean(mContext, Store.CAN_SHOW_PERMISSION_BTN, true);
        }
    }

    private void requestPermissionAndStartService() {
        if (!getStoreBoolean(mContext, Store.CAN_SHOW_PERMISSION_BTN)) return;

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
                Toast.makeText(mContext, getString(R.string.service_started), Toast.LENGTH_LONG).show();
                startTrackingService();
            }
        });
    }

    private void startTrackingService() {
        ForegroundToastService.start(mContext);
        RefreshService.startRefreshInIntervals(mContext);
//        finish();
    }


    private void prepareToReceiveWorkerID() {
        if (!getStoreBoolean(mContext, Store.CAN_SHOW_SUBMIT_BTN)) return;

        showPlain(etWorkerID, getStoreString(mContext, Store.WORKER_ID));
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

    private void confirmAndSubmitDialog() {
        new AlertDialog.Builder(mContext)
                .setTitle("Confirm workerId submission")
                .setMessage("This cannot be changed once submitted. Are you sure you entered correct workerId?")
                .setIcon(R.drawable.ic_chart_pink)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Toast.makeText(mContext, "WorkerId submitted.", Toast.LENGTH_SHORT).show();
                        submitWorkerID();
                        startTrackingService();
                    }
                })
                .setNegativeButton(android.R.string.no, null).show();
    }

    private void submitWorkerID() {
        setStoreString(mContext, Store.WORKER_ID, etWorkerID.getText().toString());
        JSONObject params = new JSONObject();
        Helper.setJSONValue(params, "worker_id", etWorkerID.getText().toString());

        JSONObject deviceInfo = DeviceInfo.getPhoneDetails(mContext);
        Helper.copy(deviceInfo, params);
        CallAPI.submitTurkPrimeID(mContext, params, submitIDResponseHandler);
    }

    VolleyJsonCallback submitIDResponseHandler = new VolleyJsonCallback() {
        @Override
        public void onConnectSuccess(JSONObject result) {
            Log.i("submitIDSuccess: ", result.toString());
            String response = result.optString("response");
            if (result.optInt("status") == 200) {
                setStoreBoolean(mContext, Store.ENROLLED, true);
                StudyInfo.saveTodayAsExperimentJoinDate(mContext);
                showSuccess(tvSubmitFeedback, response);
                showSuccess(tvSurveyLink, result.optString("survey_link"));
            } else {
                tvSurveyLink.setVisibility(View.GONE);
                setStoreBoolean(mContext, Store.ENROLLED, false);
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

}

// TODO: 3/4/17 change every store value from camel case to the exact same name as retrieved from server
