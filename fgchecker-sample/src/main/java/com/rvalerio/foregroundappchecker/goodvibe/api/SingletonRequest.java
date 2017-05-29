package com.rvalerio.foregroundappchecker.goodvibe.api;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * REST API calls with a single network request instance
 * Created by fnokeke on 1/22/17.
 */


public class SingletonRequest {

    private static SingletonRequest mInstance;
    private RequestQueue mRequestQueue;
    private static Context mCtx;

    private SingletonRequest(Context context) {
        mCtx = context;
        mRequestQueue = getRequestQueue();

    }

    public static synchronized SingletonRequest getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new SingletonRequest(context);
        }
        return mInstance;
    }

    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(mCtx.getApplicationContext());
        }
        return mRequestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }

}


