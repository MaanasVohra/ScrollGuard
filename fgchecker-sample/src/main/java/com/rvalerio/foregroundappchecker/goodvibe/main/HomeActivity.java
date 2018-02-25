package com.rvalerio.foregroundappchecker.goodvibe.main;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import com.rvalerio.foregroundappchecker.R;

public class HomeActivity extends AppCompatActivity {
    Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mContext = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        setTitle("Home");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }

        prepEnv();

    }

    private void prepEnv() {
        setStrInputValue( "Target app appears here.", Constants.CHOSEN_APP_LABEL, R.id.tv_app_chosen);
        setStrInputValue( "Vibration or Popup.", Constants.CHOSEN_REMINDER_MODE, R.id.tv_reminder_mode);
        setStrInputValue( "5 seconds, 30 seconds or 1 minute.", Constants.CHOSEN_FREQ_STYLE, R.id.tv_frequency_style);
        setIntInputValue("time", "Time limit appears here.", Constants.CHOSEN_TIME_LIMIT, R.id.tv_daily_limit);
        setIntInputValue( "open","Open limit appears here.", Constants.CHOSEN_OPEN_LIMIT, R.id.tv_daily_open);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.appinfo, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_configure:
                startActivity(new Intent(mContext, ConfigActivity.class));
                break;
            case R.id.action_about:
                startActivity(new Intent(mContext, AboutActivity.class));
                break;
            case R.id.action_reset_app:
                Store.wipeAll(mContext);
                startActivity(new Intent(mContext, MainActivity.class));
                break;
        }

        return true;
    }

    private void setStrInputValue(String defaultValue, String storeKey, int rid) {
        String storedStr = Store.getString(mContext, storeKey);
        if (!storedStr.equals("")) {
            defaultValue = storedStr;
        }
        TextView tv = findViewById(rid);
        tv.setText(defaultValue);
    }

    private void setIntInputValue(String type, String defaultValue, String storeKey, int rid) {
        int storedValue = Store.getInt(mContext, storeKey);
        String suffix = type.equals("open") ? "x" : "minute(s)";
        if (storedValue != 0) {
            String format = type.equals("open") ? "%s%s" : "%s %s";
            defaultValue = String.format(format, storedValue, suffix);
        }
        TextView tv = findViewById(rid);
        tv.setText(defaultValue);
    }


}
