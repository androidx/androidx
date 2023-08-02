/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.extensions.internal.compat.workaround;

import android.os.SystemClock;

import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.Logger;
import androidx.camera.extensions.internal.compat.quirk.CrashWhenOnDisableTooSoon;
import androidx.camera.extensions.internal.compat.quirk.DeviceQuirks;

/**
 * A workaround to ensure the duration of onEnableSession to onDisableSession is long enough.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class OnEnableDisableSessionDurationCheck {
    private static final String TAG = "OnEnableDisableSessionDurationCheck";
    private final boolean mEnabledMinimumDuration;
    private long mOnEnableSessionTimeStamp = 0;
    @VisibleForTesting
    static final long MIN_DURATION_FOR_ENABLE_DISABLE_SESSION = 100L;

    public OnEnableDisableSessionDurationCheck() {
        this(DeviceQuirks.get(CrashWhenOnDisableTooSoon.class) != null);
    }

    @VisibleForTesting
    OnEnableDisableSessionDurationCheck(boolean enabledMinimalDuration) {
        mEnabledMinimumDuration = enabledMinimalDuration;
    }

    /**
     * Notify onEnableSession is invoked.
     */
    public void onEnableSessionInvoked() {
        if (mEnabledMinimumDuration) {
            mOnEnableSessionTimeStamp = SystemClock.elapsedRealtime();
        }
    }

    /**
     * Notify onDisableSession is invoked.
     */
    public void onDisableSessionInvoked() {
        if (mEnabledMinimumDuration) {
            ensureMinDurationAfterOnEnableSession();
        }
    }

    /**
     * Ensures onDisableSession not invoked too soon after onDisableSession. OEMs usually
     * releases resources at onDisableSession. Invoking onDisableSession too soon might cause
     * some crash during the initialization triggered by onEnableSession or onInit.
     *
     * It will ensure the duration is at least 100 ms after onEnabledSession is invoked. If the
     * camera is opened more than 100ms, then it won't add any extra delay. Since a regular
     * camera session will take more than 100ms, this change shouldn't cause any real impact for
     * the user. It only affects the auto testing by increasing a little bit delay which
     * should be okay.
     */
    private void ensureMinDurationAfterOnEnableSession() {
        long timeAfterOnEnableSession =
                SystemClock.elapsedRealtime() - mOnEnableSessionTimeStamp;
        while (timeAfterOnEnableSession < MIN_DURATION_FOR_ENABLE_DISABLE_SESSION) {
            try {
                long timeToWait =
                        MIN_DURATION_FOR_ENABLE_DISABLE_SESSION - timeAfterOnEnableSession;
                Logger.d(TAG, "onDisableSession too soon, wait " + timeToWait + " ms");
                Thread.sleep(timeToWait);
            } catch (InterruptedException e) {
                Logger.e(TAG, "sleep interrupted");
                return;
            }
            timeAfterOnEnableSession = SystemClock.elapsedRealtime() - mOnEnableSessionTimeStamp;
        }
    }
}
