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

package androidx.health.platform.client.service;

import androidx.annotation.RestrictTo;

/**
 * Class to hold common constants for AHP.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class HealthDataServiceConstants {
    public static final String ACTION_REQUEST_PERMISSIONS =
            "androidx.health.ACTION_REQUEST_PERMISSIONS";
    public static final String KEY_GRANTED_PERMISSIONS_JETPACK = "granted_permissions_jetpack";
    public static final String KEY_GRANTED_PERMISSIONS_STRING = "granted_permissions_string";
    public static final String KEY_REQUESTED_PERMISSIONS_JETPACK = "requested_permissions_jetpack";
    public static final String KEY_REQUESTED_PERMISSIONS_STRING = "requested_permissions_string";
    public static final String ACTION_REQUEST_ROUTE =
            "androidx.health.action.REQUEST_EXERCISE_ROUTE";
    public static final String EXTRA_SESSION_ID = "androidx.health.connect.extra.SESSION_ID";
    public static final String EXTRA_EXERCISE_ROUTE = "android.health.connect.extra.EXERCISE_ROUTE";

    private HealthDataServiceConstants() {}
}
