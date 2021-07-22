/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.core.content;

/**
 * Shared constants related to Unused App Restrictions
 * (e.g. Permission Revocation, App Hibernation).
 */
public final class UnusedAppRestrictionsConstants {
    private UnusedAppRestrictionsConstants() {
        /* Hide constructor */
    }

    /** The status of Unused App Restrictions features could not be retrieved from this app. */
    public static final int ERROR = 0;

    /** There are no available Unused App Restrictions features for this app. */
    public static final int FEATURE_NOT_AVAILABLE = 1;

    /**
     * Any available Unused App Restrictions features on the device are disabled for this app (i.e.
     * this app is exempt from having its permissions automatically removed or being hibernated).
     */
    public static final int DISABLED = 2;

    /**
     * Unused App Restrictions introduced by Android API 30 and made available on earlier (API
     * 23-29) devices are enabled for this app (i.e. permissions will be automatically reset if
     * the app is unused).
     */
    public static final int API_30_BACKPORT = 3;

    /**
     * Unused App Restrictions introduced by Android API 30 are enabled for this app (i.e.
     * permissions will be automatically reset if the app is unused).
     */
    public static final int API_30 = 4;

    /**
     * API 31 Unused App Restrictions are enabled for this app (i.e. this app will
     * be hibernated and have its permissions reset if the app is unused).
     */
    public static final int API_31 = 5;
}
