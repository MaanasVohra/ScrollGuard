/**
 * Copyright 2016 Google Inc. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rvalerio.foregroundappchecker.goodvibe.fcm;

import android.content.Context;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.rvalerio.foregroundappchecker.goodvibe.helper.AlarmHelper;
import com.rvalerio.foregroundappchecker.goodvibe.main.AutoUpdateAlarm;
import com.rvalerio.foregroundappchecker.goodvibe.main.ForegroundToastService;

import java.util.Map;

public class AppFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "GoodVibeFirebase";
    private static final String SERVER_SYNC = "serverSync";
    private static final String NOTIFY_USER = "notifyUser";
    private static final String PROMPT_APP_UPDATE = "promptUpdate";

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            Context context = getApplicationContext();
            Map<String, String> data = remoteMessage.getData();
            String type = data.get("type");
            switch (type) {
                case SERVER_SYNC:
                    AlarmHelper.showInstantNotif(context, "GoodVibe Sync", "Done!", "", 2233); // FIXME: 7/1/17 remove alert
                    AutoUpdateAlarm.performUpdate(context);
                    ForegroundToastService.startMonitoringFacebookUsage(context);
                    AppJobService.updateServerThroughFirebaseJob(context);
                    break;
                case NOTIFY_USER:
                    String title = data.get("title");
                    String content = data.get("content");
                    AlarmHelper.showInstantNotif(context, title, content, "io.smalldata.goodvibe", 5003);
                    break;
                case PROMPT_APP_UPDATE:
                    updateApp();
                    break;
            }
        }
    }

    private void updateApp() { }

}
