/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.support.mediacompat.service;


import static android.support.mediacompat.testlib.IntentConstants
        .ACTION_CALL_MEDIA_BROWSER_SERVICE_METHOD;
import static android.support.mediacompat.testlib.IntentConstants.KEY_ARGUMENT;
import static android.support.mediacompat.testlib.IntentConstants.KEY_METHOD_ID;
import static android.support.mediacompat.testlib.MediaBrowserConstants.CUSTOM_ACTION_SEND_ERROR;
import static android.support.mediacompat.testlib.MediaBrowserConstants
        .CUSTOM_ACTION_SEND_PROGRESS_UPDATE;
import static android.support.mediacompat.testlib.MediaBrowserConstants.CUSTOM_ACTION_SEND_RESULT;
import static android.support.mediacompat.testlib.MediaBrowserConstants.NOTIFY_CHILDREN_CHANGED;
import static android.support.mediacompat.testlib.MediaBrowserConstants.SEND_DELAYED_ITEM_LOADED;
import static android.support.mediacompat.testlib.MediaBrowserConstants
        .SEND_DELAYED_NOTIFY_CHILDREN_CHANGED;
import static android.support.mediacompat.testlib.MediaBrowserConstants.SET_SESSION_TOKEN;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class ServiceBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        if (ACTION_CALL_MEDIA_BROWSER_SERVICE_METHOD.equals(intent.getAction()) && extras != null) {
            StubMediaBrowserServiceCompat service = StubMediaBrowserServiceCompat.sInstance;
            int method = extras.getInt(KEY_METHOD_ID, 0);

            switch (method) {
                case NOTIFY_CHILDREN_CHANGED:
                    service.notifyChildrenChanged(extras.getString(KEY_ARGUMENT));
                    break;
                case SEND_DELAYED_NOTIFY_CHILDREN_CHANGED:
                    service.sendDelayedNotifyChildrenChanged();
                    break;
                case SEND_DELAYED_ITEM_LOADED:
                    service.sendDelayedItemLoaded();
                    break;
                case CUSTOM_ACTION_SEND_PROGRESS_UPDATE:
                    service.mCustomActionResult.sendProgressUpdate(extras.getBundle(KEY_ARGUMENT));
                    break;
                case CUSTOM_ACTION_SEND_ERROR:
                    service.mCustomActionResult.sendError(extras.getBundle(KEY_ARGUMENT));
                    break;
                case CUSTOM_ACTION_SEND_RESULT:
                    service.mCustomActionResult.sendResult(extras.getBundle(KEY_ARGUMENT));
                    break;
                case SET_SESSION_TOKEN:
                    StubMediaBrowserServiceCompatWithDelayedMediaSession.sInstance
                            .callSetSessionToken();
                    break;
            }
        }
    }
}
