package com.rvalerio.foregroundappchecker.goodvibe.main;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.rvalerio.foregroundappchecker.R;
import com.rvalerio.foregroundappchecker.goodvibe.helper.JsonHelper;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ConfigActivity extends AppCompatActivity {
    Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mContext = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        setTitle("Configuration");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        activateAppDropdown();
        activateReminderDropDown();
        activateFreqDropDown();
        activateEditTexts();
        activateSaveButton();
    }


    private String getAppId(String item) {
        return getInstalledApps(mContext).get(item);
    }

    private void activateAppDropdown() {
        Spinner spinner = findViewById(R.id.spinner_target_app);
//        final String[] dropDownItems = getAllInstalledAppNames();
        String appLabels = Store.getString(mContext, Constants.STORED_APPS_LABELS);
        final String[] dropDownItems = appLabels.split(";");
//        Arrays.sort(dropDownItems);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(mContext, android.R.layout.simple_spinner_item, dropDownItems);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String item = (String) parent.getItemAtPosition(position);
                Store.setString(mContext, Constants.CHOSEN_APP_LABEL, item);
                Store.setString(mContext, Constants.CHOSEN_APP_ID, getAppId(item));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    public static HashMap<String, String> getInstalledApps(Context context) {
        final PackageManager pm = context.getPackageManager();
        List<PackageInfo> packList = pm.getInstalledPackages(0);
        HashMap<String, String> allApps = new HashMap<>();

        String appName, appId;
        for (PackageInfo packageInfo : packList) {
            if (pm.getLaunchIntentForPackage(packageInfo.packageName) != null) {  // remove lots of system apps
                appName = packageInfo.applicationInfo.loadLabel(pm).toString();
                appId = packageInfo.packageName;
                allApps.put(appName, appId);
            }
        }
        return allApps;

    }

    private void activateReminderDropDown() {
        int spinnerId = R.id.spinner_reminder_mode;
        Spinner spinner = findViewById(spinnerId);
        final String[] dropDownItems = new String[]{"Vibration", "Popup"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(mContext, android.R.layout.simple_spinner_item, dropDownItems);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String item = (String) parent.getItemAtPosition(position);
                Store.setString(mContext, Constants.CHOSEN_REMINDER_MODE, item);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void activateFreqDropDown() {
        int spinnerId = R.id.spinner_freq_style;
        Spinner spinner = findViewById(spinnerId);
        final String[] dropDownItems = new String[]{"Every 5 seconds", "Every 30 seconds", "Every 1 minute"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(mContext, android.R.layout.simple_spinner_item, dropDownItems);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String item = (String) parent.getItemAtPosition(position);
                Store.setString(mContext, Constants.CHOSEN_FREQ_STYLE, item);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private int getDailyTimeLimit() {
        EditText editText = findViewById(R.id.et_time_limit);
        String limitStr = editText.getText().toString();
        int limit;
        if (limitStr.equals("")) {
            limit = 30;
        } else {
            limit = Integer.parseInt(limitStr);
        }
        return limit;
    }

    private int getDailyOpenLimit() {
        EditText editText = findViewById(R.id.et_open_limit);
        String limitStr = editText.getText().toString();
        int limit;
        if (limitStr.equals("")) {
            limit = 10;
        } else {
            limit = Integer.parseInt(limitStr);
        }
        return limit;
    }

    private void activateSaveButton() {
        Button btnSave = findViewById(R.id.img_btn_save_config);
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int limit = getDailyTimeLimit();
                if (limit >= 1440) {
                    Toast.makeText(mContext, "Really? That's more than one day.", Toast.LENGTH_SHORT).show();
                    return;
                } else if (limit >= 720) {
                    Toast.makeText(mContext, "That's at least 12 hours. C'mon now.", Toast.LENGTH_SHORT).show();
                    return;
                }
                Store.setInt(mContext, Constants.CHOSEN_TIME_LIMIT, limit);

                limit = getDailyOpenLimit();
                if (limit >= 200) {
                    Toast.makeText(mContext, "Wow! That high? Rejected!!", Toast.LENGTH_SHORT).show();
                    return;
                }
                Store.setInt(mContext, Constants.CHOSEN_OPEN_LIMIT, limit);

                startActivity(new Intent(getApplicationContext(), HomeActivity.class));
                Toast.makeText(mContext, "Configurations saved.", Toast.LENGTH_SHORT).show();
                ForegroundToastService.startMonitoring(mContext);
            }
        });
    }

    private void activateEditTexts() {
        int storeValue = Store.getInt(mContext, Constants.CHOSEN_TIME_LIMIT);
        int value = storeValue == 0 ? 30 : storeValue;
        EditText et = findViewById(R.id.et_time_limit);
        et.setText(String.valueOf(value));

        storeValue = Store.getInt(mContext, Constants.CHOSEN_OPEN_LIMIT);
        value = storeValue == 0 ? 10 : storeValue;
        et = findViewById(R.id.et_open_limit);
        et.setText(String.valueOf(value));
    }

    public static void refreshAppList(Context context) {
        initAllAppList(context, false);
    }

    public static void initAllAppList(Context context, boolean resetAll) {
        HashMap<String, String> installedApps = ConfigActivity.getInstalledApps(context);
        JSONObject timeSpentList = new JSONObject();
        JSONObject numOpenList = new JSONObject();

        String[] appKeys = installedApps.keySet().toArray(new String[0]);
        Arrays.sort(appKeys);

        String appLabels = "";
        String appIdValue;
        for (String key : appKeys) {
            appLabels = appLabels.concat(key + ";");
            appIdValue = installedApps.get(key);
            if (resetAll || timeSpentList.optString(appIdValue).equals("")) {
                JsonHelper.setJSONValue(timeSpentList, appIdValue, 0);
                JsonHelper.setJSONValue(numOpenList, appIdValue, 0);
            }
        }

        Store.setJsonObject(context, Constants.STORED_APPS_TIME_SPENT, timeSpentList);
        Store.setJsonObject(context, Constants.STORED_APPS_NUM_OPENS, numOpenList);
        Store.setString(context, Constants.STORED_APPS_LABELS, appLabels);
    }

}
