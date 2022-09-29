/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.health.platform.client.impl.permission.foregroundstate;

import static android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
import static android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE;
import static android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

/** Utility class to check whether the current running process is in foreground. */
public final class ForegroundStateChecker {
    private ForegroundStateChecker() {}

    /** Returns {@code true} if the current running process is in foreground, false otherwise. */
    @SuppressLint("ObsoleteSdkInt")
    public static boolean isInForeground() {
        ActivityManager.RunningAppProcessInfo appProcessInfo =
                new ActivityManager.RunningAppProcessInfo();
        ActivityManager.getMyMemoryState(appProcessInfo);
        int importance = appProcessInfo.importance;
        if (VERSION.SDK_INT < VERSION_CODES.M) {
            return importance == IMPORTANCE_FOREGROUND || importance == IMPORTANCE_VISIBLE;
        } else {
            return importance == IMPORTANCE_FOREGROUND
                    || importance == IMPORTANCE_FOREGROUND_SERVICE
                    || importance == IMPORTANCE_VISIBLE;
        }
    }
}
