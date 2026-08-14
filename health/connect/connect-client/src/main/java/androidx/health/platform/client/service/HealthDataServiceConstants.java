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

/** Class to hold common constants for AHP. */
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
    public static final String DEFAULT_PROVIDER_PACKAGE_NAME = "com.google.android.apps.healthdata";
    public static final int DEFAULT_PROVIDER_MIN_VERSION_CODE = 68623;

    public static final byte[] DEFAULT_PROVIDER_RELEASE_CERT_SHA256 =
            new byte[] {
                (byte) 0xb2, (byte) 0xc0, (byte) 0xa8, (byte) 0x0e, (byte) 0x48, (byte) 0x59,
                (byte) 0x34, (byte) 0xbf, (byte) 0xb0, (byte) 0x8f, (byte) 0x90, (byte) 0x2c,
                (byte) 0xa2, (byte) 0x75, (byte) 0x05, (byte) 0x81, (byte) 0x3d, (byte) 0xf3,
                (byte) 0x15, (byte) 0x9e, (byte) 0x4e, (byte) 0x6d, (byte) 0xd4, (byte) 0xa4,
                (byte) 0xdf, (byte) 0x07, (byte) 0x8d, (byte) 0x66, (byte) 0xcb, (byte) 0x1c,
                (byte) 0x20, (byte) 0x03
            };

    // never allowed to change to stay backward compatible
    public static final byte[] DEFAULT_PROVIDER_DEV_CERT_SHA256 =
            new byte[] {
                (byte) 0x45, (byte) 0x13, (byte) 0x03, (byte) 0x66, (byte) 0x94, (byte) 0xcb,
                (byte) 0x96, (byte) 0x39, (byte) 0x75, (byte) 0x1a, (byte) 0x68, (byte) 0x44,
                (byte) 0xf2, (byte) 0x07, (byte) 0x48, (byte) 0x0d, (byte) 0xd9, (byte) 0x40,
                (byte) 0xbd, (byte) 0x53, (byte) 0xa4, (byte) 0x89, (byte) 0xf8, (byte) 0xac,
                (byte) 0xf3, (byte) 0x2c, (byte) 0x00, (byte) 0x58, (byte) 0x20, (byte) 0xca,
                (byte) 0xc3, (byte) 0xeb
            };

    private HealthDataServiceConstants() {}
}
