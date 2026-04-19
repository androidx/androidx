/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.appsearch.testutil;

import android.os.Build;
import android.util.Log;

import org.jspecify.annotations.NonNull;
import org.junit.Assume;

import java.lang.reflect.Method;
import java.util.Objects;

/** Utility class for framework only flag operations in tests. */
// TODO(b/383440585): Use RequiresFlagsEnabled once it supports running on mainline modules.
public final class FrameworkFlagUtils {
    private static final String TAG = "FrameworkFlagUtils";

    private FrameworkFlagUtils() {}

    /**
     * Assumes a framework flag is enabled.
     *
     * <p>If the flag is not available, the test will be skipped too.
     *
     * @param flagName The name of the flag to check.
     */
    public static void assumeFlagIsEnabled(@NonNull String flagName) {
        Objects.requireNonNull(flagName);
        Assume.assumeTrue(
                "Skipping test because flag " + flagName + " is disabled.",
                isFlagEnabled(flagName));
    }

    /**
     * Assumes a framework flag is disabled.
     *
     * <p>If the flag is not available, it is also considered disabled.
     *
     * @param flagName The name of the flag to check.
     */
    public static void assumeFlagIsDisabled(@NonNull String flagName) {
        Objects.requireNonNull(flagName);
        Assume.assumeFalse(
                "Skipping test because flag " + flagName + " is enabled.", isFlagEnabled(flagName));
    }

    /**
     * Checks if a framework flag is enabled.
     *
     * <p>If the flag is not available, returns false.
     *
     * @param flagName The name of the flag to check.
     */
    public static boolean isFlagEnabled(@NonNull String flagName) {
        try {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.CUR_DEVELOPMENT) {
                return false;
            }
            int lastDot = flagName.lastIndexOf('.');
            if (lastDot == -1) {
                return false;
            }
            String packageName = flagName.substring(0, lastDot);
            String flagIdentifier = flagName.substring(lastDot + 1);
            String className = packageName + ".Flags";
            String methodName = snakeToCamelCase(flagIdentifier);
            Class<?> flagsClass = Class.forName(className);
            Method method = flagsClass.getMethod(methodName);
            return (Boolean) method.invoke(null);
        } catch (Exception e) {
            Log.w(TAG, "Failed to check flag: " + flagName, e);
            return false;
        }
    }

    private static String snakeToCamelCase(@NonNull String snakeCase) {
        StringBuilder camelCase = new StringBuilder();
        for (String part : snakeCase.split("_")) {
            if (part.isEmpty()) {
                continue;
            }
            if (camelCase.length() == 0) {
                camelCase.append(part);
            } else {
                camelCase.append(Character.toUpperCase(part.charAt(0)));
                camelCase.append(part.substring(1));
            }
        }
        return camelCase.toString();
    }
}
