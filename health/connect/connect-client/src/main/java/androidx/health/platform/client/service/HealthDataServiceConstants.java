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
                (byte) 0xF0, (byte) 0xFD, (byte) 0x6C, (byte) 0x5B, (byte) 0x41, (byte) 0x0F,
                (byte) 0x25, (byte) 0xCB, (byte) 0x25, (byte) 0xC3, (byte) 0xB5, (byte) 0x33,
                (byte) 0x46, (byte) 0xC8, (byte) 0x97, (byte) 0x2F, (byte) 0xAE, (byte) 0x30,
                (byte) 0xF8, (byte) 0xEE, (byte) 0x74, (byte) 0x11, (byte) 0xDF, (byte) 0x91,
                (byte) 0x04, (byte) 0x80, (byte) 0xAD, (byte) 0x6B, (byte) 0x2D, (byte) 0x60,
                (byte) 0xDB, (byte) 0x83
            };

    // never allowed to change to stay backward compatible
    public static final byte[] DEFAULT_PROVIDER_DEV_CERT_SHA256 =
            new byte[] {
                (byte) 0xb2, (byte) 0xc0, (byte) 0xa8, (byte) 0x0e, (byte) 0x48, (byte) 0x59,
                (byte) 0x34, (byte) 0xbf, (byte) 0xb0, (byte) 0x8f, (byte) 0x90, (byte) 0x2c,
                (byte) 0xa2, (byte) 0x75, (byte) 0x05, (byte) 0x81, (byte) 0x3d, (byte) 0xf3,
                (byte) 0x15, (byte) 0x9e, (byte) 0x4e, (byte) 0x6d, (byte) 0xd4, (byte) 0xa4,
                (byte) 0xdf, (byte) 0x07, (byte) 0x8d, (byte) 0x66, (byte) 0xcb, (byte) 0x1c,
                (byte) 0x20, (byte) 0x03
            };

    private HealthDataServiceConstants() {}
}
