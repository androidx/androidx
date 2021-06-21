/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.mediarouter.media;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

/**
 * Helper class for deciding whether active scan is allowed and when should active scan should be
 * suppressed.
 */
class MediaRouterActiveScanThrottlingHelper {
    // The constant is package visible for tests can set it to a shorter duration.
    static final long MAX_ACTIVE_SCAN_DURATION_MS = 30000;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Runnable mUpdateDiscoveryRequestRunnable;
    private long mSuppressActiveScanTimeout;
    private long mCurrentTime;
    private boolean mActiveScan;

    MediaRouterActiveScanThrottlingHelper(Runnable updateDiscoveryRequestRunnable) {
        mUpdateDiscoveryRequestRunnable = updateDiscoveryRequestRunnable;
    }

    /** Resets the helper as if no active scan is requested. */
    public void reset() {
        mSuppressActiveScanTimeout = 0;
        mActiveScan = false;
        mCurrentTime = SystemClock.elapsedRealtime();
        mHandler.removeCallbacks(mUpdateDiscoveryRequestRunnable);
    }

    /** Add an active scan request. */
    public void requestActiveScan(boolean activeScanAsRequested, long requestTimestamp) {
        if (!activeScanAsRequested) {
            // Active scan not requested.
            return;
        }

        if (mCurrentTime - requestTimestamp >= MAX_ACTIVE_SCAN_DURATION_MS) {
            // Active scan should be suppressed.
            return;
        }

        mSuppressActiveScanTimeout =
                Math.max(
                        mSuppressActiveScanTimeout,
                        requestTimestamp + MAX_ACTIVE_SCAN_DURATION_MS - mCurrentTime);

        mActiveScan = true;
    }

    /**
     * Calculate whether active scan is needed based on all active scan requests since last
     *  {@link #reset()} and schedule a runnable to suppress active scan if needed.
     */
    public boolean finalizeActiveScanAndScheduleSuppressActiveScanRunnable() {
        if (mActiveScan && mSuppressActiveScanTimeout > 0) {
            mHandler.postDelayed(mUpdateDiscoveryRequestRunnable, mSuppressActiveScanTimeout);
        }
        return mActiveScan;
    }
}
