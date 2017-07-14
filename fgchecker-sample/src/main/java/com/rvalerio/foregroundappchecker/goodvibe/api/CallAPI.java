package com.rvalerio.foregroundappchecker.goodvibe.api;

import android.content.Context;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

/**
 * Created by fnokeke on 1/22/17.
 * Handle All Rest API Calls
 */

public class CallAPI {

    final static private String BASE_URL = "https://slm.smalldata.io";
//        final static private String BASE_URL = "http://10.0.0.142:5000";
//    final static private String BASE_URL = "http://172.20.1.91:5000";
    final static private String TURKPRIME_REGISTER_URL = BASE_URL + "/mobile/turkprime/enroll";
    final static private String TURKPRIME_ADD_FB_STATS = BASE_URL + "/mobile/turkprime/fb-stats";
    private static final String TURKPRIME_FACEBOOK_LOGS = BASE_URL + "/mobile/turkprime/facebook-logs";
    private static final String TURKPRIME_APP_LOGS = BASE_URL + "/mobile/turkprime/app-logs";
    private static final String TURKPRIME_SCREEN_EVENT_LOGS = BASE_URL + "/mobile/turkprime/screen-event-logs";

    private static JsonObjectRequest createRequest(final String url, final JSONObject params, final VolleyJsonCallback callback) {

        JsonObjectRequest request = new JsonObjectRequest(url, params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject result) {
                        callback.onConnectSuccess(result);
                    }
                },

                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        callback.onConnectFailure(error);
                    }
                }
        );

        final int SOCKET_TIMEOUT_MILLIS = 3000;
        final int MAX_RETRIES = 2;
        final float DEFAULT_BACKOFF_MULTIPLIER = 2f;
        request.setRetryPolicy(new DefaultRetryPolicy(SOCKET_TIMEOUT_MILLIS, MAX_RETRIES, DEFAULT_BACKOFF_MULTIPLIER));
        return request;
    }

    public static void submitTurkPrimeID(final Context context, final JSONObject params, final VolleyJsonCallback callback) {
        JsonObjectRequest request = createRequest(TURKPRIME_REGISTER_URL, params, callback);
        SingletonRequest.getInstance(context).addToRequestQueue(request);
    }

    public static void submitFBAndOtherCurrentStats(Context context, JSONObject params, VolleyJsonCallback callback) {
        JsonObjectRequest request = createRequest(TURKPRIME_ADD_FB_STATS, params, callback);
        SingletonRequest.getInstance(context).addToRequestQueue(request);
    }

    public static void submitFacebookLogs(Context context, JSONObject params, VolleyJsonCallback callback) {
        JsonObjectRequest request = createRequest(TURKPRIME_FACEBOOK_LOGS, params, callback);
        SingletonRequest.getInstance(context).addToRequestQueue(request);
    }

    public static void submitAppLogs(Context context, JSONObject params, VolleyJsonCallback callback) {
        JsonObjectRequest request = createRequest(TURKPRIME_APP_LOGS, params, callback);
        SingletonRequest.getInstance(context).addToRequestQueue(request);
    }

    public static void submitScreenEventLogs(Context context, JSONObject params, VolleyJsonCallback callback) {
        JsonObjectRequest request = createRequest(TURKPRIME_SCREEN_EVENT_LOGS, params, callback);
        SingletonRequest.getInstance(context).addToRequestQueue(request);
    }
}

