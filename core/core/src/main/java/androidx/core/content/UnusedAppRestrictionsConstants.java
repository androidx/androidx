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

    /**
     * The status of Unused App Restrictions could not be retrieved from this app.
     *
     * Note: check the logs for the reason (e.g. if the app's target SDK version < 30 or the user
     * is in locked device boot mode).
     */
    public static final int ERROR = 0;

    /** There are no available Unused App Restrictions for this app. */
    public static final int FEATURE_NOT_AVAILABLE = 1;

    /**
     * Any available Unused App Restrictions on the device are disabled for this app.
     *
     * In other words, this app is exempt from having its permissions automatically removed
     * or being hibernated.
     */
    public static final int DISABLED = 2;

    /**
     * Unused App Restrictions introduced by Android API 30, and since made available on earlier
     * (API 23-29) devices are enabled for this app:
     * <a href="https://developer.android.com/training/permissions/requesting?hl=hu
     * #auto-reset-permissions-unused-apps">permission auto-reset</a>.
     *
     * Note: This value is only used on API 29 or earlier devices.
     */
    public static final int API_30_BACKPORT = 3;

    /**
     * Unused App Restrictions introduced by Android API 30 are enabled for this app:
     * <a href="https://developer.android.com/training/permissions/requesting?hl=hu
     * #auto-reset-permissions-unused-apps">permission auto-reset</a>.
     *
     * Note: This value is only used on API 30 or later devices.
     */
    public static final int API_30 = 4;

    /**
     * Unused App Restrictions introduced by Android API 31 are enabled for this app:
     * <a href="https://developer.android.com/training/permissions/requesting?hl=hu
     * #auto-reset-permissions-unused-apps">permission auto-reset</a> and
     * <a href="https://developer.android.com/about/versions/12/behavior-changes-12#app-hibernation"
     * >app hibernation</a>.
     *
     * Note: This value is only used on API 31 or later devices.
     */
    public static final int API_31 = 5;
}
