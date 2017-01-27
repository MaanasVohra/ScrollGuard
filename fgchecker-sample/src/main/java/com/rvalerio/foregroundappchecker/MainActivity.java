package com.rvalerio.foregroundappchecker;

import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
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

public class MainActivity extends AppCompatActivity {

    private EditText etFacebookLimitTime;
    private EditText etFacebookLimitOpened;

    private EditText etGmailLimitTime;
    private EditText etGmailLimitOpened;

    private TextView tvPermission;
    private Button btSubmit;
    private Button btUsagePermission;
    private Button btStartService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etFacebookLimitTime = (EditText) findViewById(R.id.et_limit_time_fb);
        etFacebookLimitOpened = (EditText) findViewById(R.id.et_limit_opened_fb);

        etGmailLimitTime = (EditText) findViewById(R.id.et_limit_time_gm);
        etGmailLimitOpened = (EditText) findViewById(R.id.et_limit_opened_gm);

        tvPermission = (TextView) findViewById(R.id.tv_permission_text);

        btSubmit = (Button) findViewById(R.id.btn_submit);
        btUsagePermission = (Button) findViewById(R.id.btn_usage_permission);
        btStartService = (Button) findViewById(R.id.btn_service_start);

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

        btStartService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ForegroundToastService.start(getBaseContext());
                Toast.makeText(getBaseContext(), getString(R.string.service_started), Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        btSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getBaseContext(), getString(R.string.submission_done), Toast.LENGTH_SHORT).show();
                Helper.setStoreInt(getBaseContext(), "fbMaxOpen", Integer.parseInt(etFacebookLimitOpened.getText().toString()));
                Helper.setStoreInt(getBaseContext(), "fbMaxTime", Integer.parseInt(etFacebookLimitTime.getText().toString()));
                Helper.setStoreInt(getBaseContext(), "gmMaxOpen", Integer.parseInt(etGmailLimitOpened.getText().toString()));
                Helper.setStoreInt(getBaseContext(), "gmMaxTime", Integer.parseInt(etGmailLimitTime.getText().toString()));
                Helper.setStoreBoolean(getBaseContext(), "canSubmitInput", false);
                Log.d("Submit clicked: ", Helper.getStoreBoolean(getBaseContext(), "canSubmitInput").toString());
                updateInputStatus(Helper.getStoreBoolean(getBaseContext(), "canSubmitInput"));
            }
        });

        updateInputStatus(Helper.getStoreBoolean(getBaseContext(), "canSubmitInput"));
    }

    private void updateInputStatus(Boolean shd_enable) {

        Context ctx = getBaseContext();
        Integer gmMaxTime = Helper.getStoreInt(ctx, "gmMaxTime");
        Integer gmMaxOpen = Helper.getStoreInt(ctx, "gmMaxOpen");
        Integer fbMaxTime = Helper.getStoreInt(ctx, "fbMaxTime");
        Integer fbMaxOpen = Helper.getStoreInt(ctx, "fbMaxOpen");

        if (!shd_enable) {
            etGmailLimitTime.setText(gmMaxTime.toString() + " mins");
            etGmailLimitOpened.setText(gmMaxOpen.toString() + "x");
            etFacebookLimitTime.setText(fbMaxTime.toString() + " mins");
            etFacebookLimitOpened.setText(fbMaxOpen.toString() + " x");
        }

        etFacebookLimitTime.setEnabled(shd_enable);
        etFacebookLimitOpened.setEnabled(shd_enable);
        etGmailLimitOpened.setEnabled(shd_enable);
        etGmailLimitTime.setEnabled(shd_enable);
        btSubmit.setEnabled(shd_enable);
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
