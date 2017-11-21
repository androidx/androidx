/*
 * Copyright 2017 The Android Open Source Project
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
package android.arch.background.workmanager.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

/**
 * Helper class for common {@link PackageManager} functions
 */

public class PackageManagerHelper {
    private static final String TAG = "PackageManagerHelper";

    private PackageManagerHelper() {
    }

    /**
     * Convenience method for {@link #setComponentEnabled(Context, String, boolean)}
     */
    public static void setComponentEnabled(Context context, Class klazz, boolean enabled) {
        setComponentEnabled(context, klazz.getName(), enabled);
    }

    /**
     * Uses {@link PackageManager} to enable/disable a manifest-defined component
     *
     * @param context {@link Context}
     * @param className {@link Class#getName()} name of component
     * @param enabled {@code true} if component should be enabled
     */
    public static void setComponentEnabled(Context context, String className, boolean enabled) {
        try {
            PackageManager packageManager = context.getPackageManager();
            ComponentName componentName = new ComponentName(context, className);
            packageManager.setComponentEnabledSetting(componentName,
                    enabled
                            ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                            : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
            Log.d(TAG, className + " " + (enabled ? "enabled" : "disabled"));
        } catch (Exception e) {
            Log.d(TAG,
                    className + " could not be " + (enabled ? "enabled" : "disabled"),
                    e);
        }
    }

    /**
     * Convenience method for {@link #isComponentExplicitlyEnabled(Context, String)}
     */
    public static boolean isComponentExplicitlyEnabled(Context context, Class klazz) {
        return isComponentExplicitlyEnabled(context, klazz.getName());
    }

    /**
     * Checks if a manifest-defined component is explicitly enabled
     *
     * @param context {@link Context}
     * @param className {@link Class#getName()} name of component
     * @return {@code true} if component is explicitly enabled
     */
    public static boolean isComponentExplicitlyEnabled(Context context, String className) {
        PackageManager packageManager = context.getPackageManager();
        ComponentName componentName = new ComponentName(context, className);
        int state = packageManager.getComponentEnabledSetting(componentName);
        return state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
    }
}
