/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.work.impl.utils;


import static android.content.Context.ACTIVITY_SERVICE;
import static android.os.Build.VERSION.SDK_INT;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Process;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.work.Configuration;
import androidx.work.Logger;
import androidx.work.WorkManager;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ProcessUtils {
    private static final String TAG = Logger.tagWithPrefix("ProcessUtils");

    private ProcessUtils() {
        // Does nothing
    }

    /**
     * @return {@code true} when {@link WorkManager} is running in the configured app process.
     */
    public static boolean isDefaultProcess(
            @NonNull Context context,
            @NonNull Configuration configuration) {

        String processName = getProcessName(context);

        if (!TextUtils.isEmpty(configuration.getDefaultProcessName())) {
            return TextUtils.equals(processName, configuration.getDefaultProcessName());
        } else {
            ApplicationInfo info = context.getApplicationInfo();
            return TextUtils.equals(processName, info.processName);
        }
    }

    /**
     * @return The name of the active process.
     */
    @Nullable
    @SuppressLint({"PrivateApi", "DiscouragedPrivateApi"})
    public static String getProcessName(@NonNull Context context) {
        if (SDK_INT >= 28) {
            return Application.getProcessName();
        }

        // Try using ActivityThread to determine the current process name.
        try {
            Class<?> activityThread = Class.forName(
                    "android.app.ActivityThread",
                    false,
                    ProcessUtils.class.getClassLoader());
            final Object packageName;
            if (SDK_INT >= 18) {
                Method currentProcessName = activityThread.getDeclaredMethod("currentProcessName");
                currentProcessName.setAccessible(true);
                packageName = currentProcessName.invoke(null);
            } else {
                Method getActivityThread = activityThread.getDeclaredMethod(
                        "currentActivityThread");
                getActivityThread.setAccessible(true);
                Method getProcessName = activityThread.getDeclaredMethod("getProcessName");
                getProcessName.setAccessible(true);
                packageName = getProcessName.invoke(getActivityThread.invoke(null));
            }
            if (packageName instanceof String) {
                return (String) packageName;
            }
        } catch (Throwable exception) {
            Logger.get().debug(TAG, "Unable to check ActivityThread for processName", exception);
        }

        // Fallback to the most expensive way
        int pid = Process.myPid();
        ActivityManager am =
                (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);

        if (am != null) {
            List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
            if (processes != null && !processes.isEmpty()) {
                for (ActivityManager.RunningAppProcessInfo process : processes) {
                    if (process.pid == pid) {
                        return process.processName;
                    }
                }
            }
        }

        return null;
    }
}
