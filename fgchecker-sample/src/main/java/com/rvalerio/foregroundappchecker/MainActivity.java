package com.rvalerio.foregroundappchecker;

import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
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

import static com.rvalerio.foregroundappchecker.Helper.setStoreInt;


public class MainActivity extends AppCompatActivity {

    private Context mContext;

    private TextView tvPermission;
    private Button btUsagePermission;
    private Button btStartService;

    private EditText etWorkerID;
    private TextView tvSubmitFeedback;
    private Button btSubmitMturkID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;

        tvPermission = (TextView) findViewById(R.id.tv_permission_text);
        btUsagePermission = (Button) findViewById(R.id.btn_usage_permission);

        if (!needsUsageStatsPermission()) {
            btUsagePermission.setVisibility(View.GONE);
            tvPermission.setText(R.string.usage_permission_granted);
            btUsagePermission.setVisibility(View.GONE);
        } else {
            btUsagePermission.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    requestUsageStatsPermission();
                }
            });
        }

        btStartService = (Button) findViewById(R.id.btn_service_start);
        btStartService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ForegroundToastService.start(mContext);
                Toast.makeText(mContext, getString(R.string.service_started), Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        etWorkerID = (EditText) findViewById(R.id.et_mturk_id);
        etWorkerID.setText(Helper.getStoreString(mContext, "workerID"));
        tvSubmitFeedback = (TextView) findViewById(R.id.tv_submit_id_feedback);

        btSubmitMturkID = (Button) findViewById(R.id.btn_submit_mturk_id);
        btSubmitMturkID.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Helper.setStoreString(mContext, "workerID", etWorkerID.getText().toString());

                JSONObject params = new JSONObject();
                Helper.setJSONValue(params, "worker_id", etWorkerID.getText().toString());

                JSONObject deviceInfo = DeviceInfo.getPhoneDetails(mContext);
                Helper.copy(deviceInfo, params);

                if (!Helper.isNetworkAvailable(mContext)) {
                    String msg = "You do not have any network connection.";
                    showError(tvSubmitFeedback, msg);
                    return;
                }

                CallAPI.submitMturkID(mContext, params, submitIDResponseHandler);
            }
        });

    }

    VolleyJsonCallback submitIDResponseHandler = new VolleyJsonCallback() {
        @Override
        public void onConnectSuccess(JSONObject result) {
            Log.e("submitIDSuccess: ", result.toString());
            String response = result.optString("response");
            if (result.optInt("status") == 200) {
                setStoreInt(mContext, "experimentGroup", result.optInt("experiment_group"));
                showSuccess(tvSubmitFeedback, response);
            } else {
                showError(tvSubmitFeedback, response);
            }
        }

        @Override
        public void onConnectFailure(VolleyError error) {
            String msg = "Error submitting your worker id. Please contact researcher. \n\nError details:\n" + error.toString();
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
        boolean granted = mode == AppOpsManager.MODE_ALLOWED;
        return granted;
    }

}

// TODO: 3/4/17 change every store value from camel case to the exact same name as retrieved from server
