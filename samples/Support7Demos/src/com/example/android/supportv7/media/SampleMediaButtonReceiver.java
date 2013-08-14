/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.example.android.supportv7.media;

import com.example.android.supportv7.R;

import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.util.Log;
import android.view.KeyEvent;

/**
 * Broadcast receiver for handling ACTION_MEDIA_BUTTON.
 *
 * This is needed to create the RemoteControlClient for controlling
 * remote route volume in lock screen. It routes media key events back
 * to main app activity SampleMediaRouterActivity.
 */
public class SampleMediaButtonReceiver extends BroadcastReceiver {
    private static final String TAG = "SampleMediaButtonReceiver";
    private static SampleMediaRouterActivity mActivity;

    public static void setActivity(SampleMediaRouterActivity activity) {
        mActivity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (mActivity != null && Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            mActivity.handleMediaKey(
                    (KeyEvent)intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT));
        }
    }
}
