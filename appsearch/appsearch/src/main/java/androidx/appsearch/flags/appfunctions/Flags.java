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

// @exportToFramework:skipFile()
package androidx.appsearch.flags.appfunctions;

import androidx.annotation.RestrictTo;

/**
 * Flags to control different features for app functions.
 *
 * <p>In Jetpack, those values can't be changed during runtime.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Flags {
    private Flags() {
    }

    // The prefix of all the flags defined for appfunctions.
    public static final String FLAG_PREFIX = "com.android.appfunctions.flags.";

    // This flags the App Interaction Feature.
    public static final String FLAG_ENABLE_APP_FUNCTION_PERMISSION_V2 =
            FLAG_PREFIX + "enable_app_function_permission_v2";

    /**
     * Whether the EXECUTE_APP_FUNCTIONS_SYSTEM should be enabled in
     * {@link androidx.appsearch.app.SetSchemaRequest.Builder
     * #addRequiredPermissionsForSchemaTypeVisibility}
     */
    public static boolean enableAppFunctionPermissionV2() {
        return true;
    }
}
