/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.example.android.support.appnavigation.app;

import android.app.Activity;
import android.os.Build;

/**
 * Very limited shim for enabling the action bar's up button on devices that support it.
 */
public class ActionBarCompat {
    /**
     * This class will only ever be loaded if the version check succeeds,
     * keeping the verifier from rejecting the use of framework classes that
     * don't exist on older platform versions.
     */
    static class ActionBarCompatImpl {
        static void setDisplayHomeAsUpEnabled(Activity activity, boolean enable) {
            activity.getActionBar().setDisplayHomeAsUpEnabled(enable);
        }
    }

    public static void setDisplayHomeAsUpEnabled(Activity activity, boolean enable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            ActionBarCompatImpl.setDisplayHomeAsUpEnabled(activity, enable);
        }
    }
}
