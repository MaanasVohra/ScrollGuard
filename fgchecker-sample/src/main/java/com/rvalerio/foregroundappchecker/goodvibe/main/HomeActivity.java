package com.rvalerio.foregroundappchecker.goodvibe.main;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.rvalerio.foregroundappchecker.R;

import org.w3c.dom.Text;

import java.util.Locale;

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
        setInputValue( "Target app appears here.", Constants.CHOSEN_APP_LABEL, R.id.tv_app_chosen);
        setInputValue( "Vibration or Popup.", Constants.CHOSEN_REMINDER_MODE, R.id.tv_reminder_mode);
        setInputValue( "5 seconds, 30 seconds or 1 minute.", Constants.CHOSEN_FREQ_STYLE, R.id.tv_frequency_style);

        TextView tvDailyLimit = findViewById(R.id.tv_daily_limit);
        String limitText = "Limit appears here.";
        int storedValue = Store.getInt(mContext, Constants.CHOSEN_DAILY_LIMIT);
        if (storedValue != 0) {
            limitText = String.format(Locale.getDefault(), "%d minutes", storedValue);
        }
        tvDailyLimit.setText(limitText);
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
                startActivity(new Intent(mContext, About.class));
                break;
            case R.id.action_reset_app:
                break;
        }

        return true;
    }

    private void setInputValue(String defaultValue, String storeKey, int rid) {
        String storedStr = Store.getString(mContext, storeKey);
        if (!storedStr.equals("")) {
            defaultValue = storedStr;
        }
        TextView tv = findViewById(rid);
        tv.setText(defaultValue);
    }

}
