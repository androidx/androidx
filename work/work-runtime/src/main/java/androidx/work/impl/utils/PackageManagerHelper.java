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
package androidx.work.impl.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.work.Logger;

/**
 * Helper class for common {@link PackageManager} functions
 */

public class PackageManagerHelper {
    private static final String TAG = Logger.tagWithPrefix("PackageManagerHelper");

    private PackageManagerHelper() {
    }

    /**
     * Uses {@link PackageManager} to enable/disable a manifest-defined component
     *
     * @param context {@link Context}
     * @param klazz   The class of component
     * @param enabled {@code true} if component should be enabled
     */
    public static void setComponentEnabled(
            @NonNull Context context,
            @NonNull Class<?> klazz,
            boolean enabled) {
        try {
            boolean current = isComponentEnabled(
                    getComponentEnabledSetting(context, klazz.getName()), false);

            if (enabled == current) {
                Logger.get().debug(TAG, "Skipping component enablement for " + klazz.getName());
                return;
            }

            PackageManager packageManager = context.getPackageManager();
            ComponentName componentName = new ComponentName(context, klazz.getName());
            packageManager.setComponentEnabledSetting(componentName,
                    enabled
                            ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                            : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);

            Logger.get().debug(TAG, klazz.getName() + " " + (enabled ? "enabled" : "disabled"));
        } catch (Exception exception) {
            Logger.get().debug(TAG,
                    klazz.getName() + "could not be " + (enabled ? "enabled" : "disabled"),
                    exception);
        }
    }

    /**
     * Convenience method for {@link #isComponentExplicitlyEnabled(Context, String)}
     */
    public static boolean isComponentExplicitlyEnabled(
            @NonNull Context context,
            @NonNull Class<?> klazz) {
        int setting = getComponentEnabledSetting(context, klazz.getName());
        return isComponentEnabled(setting, false);
    }

    /**
     * Checks if a manifest-defined component is explicitly enabled
     *
     * @param context   {@link Context}
     * @param className {@link Class#getName()} name of component
     * @return {@code true} if component is explicitly enabled
     */
    public static boolean isComponentExplicitlyEnabled(@NonNull Context context,
            @NonNull String className) {
        int state = getComponentEnabledSetting(context, className);
        return state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
    }

    /**
     * Retrieves the component enabled setting from {@link PackageManager}.
     */
    private static int getComponentEnabledSetting(
            @NonNull Context context,
            @NonNull String className) {
        PackageManager packageManager = context.getPackageManager();
        ComponentName componentName = new ComponentName(context, className);
        return packageManager.getComponentEnabledSetting(componentName);
    }

    private static boolean isComponentEnabled(
            int setting,
            boolean defaults) {
        if (setting == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) {
            return defaults;
        }
        // Simplifying the expression here, given the linter is extremely unhappy.
        // Treating the below cases as enabled == false
        // COMPONENT_ENABLED_STATE_DISABLED
        // COMPONENT_ENABLED_STATE_DISABLED_USER
        // COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED
        return setting == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
    }
}
