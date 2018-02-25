package com.rvalerio.foregroundappchecker.goodvibe.main;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.rvalerio.foregroundappchecker.R;

public class About extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        setTitle("About");
    }
}
