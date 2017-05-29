package com.rvalerio.foregroundappchecker.goodvibe.api;


import com.android.volley.VolleyError;

import org.json.JSONObject;

/**
 * Callback function for REST requests with Android Volley
 * Created by fnokeke on 1/22/17.
 */


public interface VolleyJsonCallback {
    void onConnectSuccess(JSONObject result);

    void onConnectFailure(VolleyError error);
}

