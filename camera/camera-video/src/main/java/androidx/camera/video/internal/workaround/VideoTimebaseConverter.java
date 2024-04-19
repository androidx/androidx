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

package androidx.camera.video.internal.workaround;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.Timebase;
import androidx.camera.video.internal.compat.quirk.CameraUseInconsistentTimebaseQuirk;
import androidx.camera.video.internal.encoder.TimeProvider;

/**
 * Converts the video timestamps to {@link Timebase#UPTIME} if video buffer contains
 * {@link Timebase#REALTIME} timestamp.
 *
 * <p>The workaround will ignore the input timebase and determine a new timebase by the following
 * 2 scenarios, to workaround the timebase inconsistent issue as described in b/197805856:
 * <ul>
 * <li>The device is listed in {@link CameraUseInconsistentTimebaseQuirk}.</li>
 * <li>The difference between system uptime and realtime exceeds a threshold.</li>
 * </ul>
 * The new timebase will be determined by checking whether the first input timestamp set to
 * {@link #convertToUptimeUs(long)} is close to UPTIME or REALTIME.
 * For performance reason, the detection will only check the first input timestamp.
 *
 * @see CameraUseInconsistentTimebaseQuirk
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class VideoTimebaseConverter {
    private static final String TAG = "VideoTimebaseConverter";

    // For 3 seconds threshold, see go/camerax-video-timebase-inconsistent-workaround.
    private static final long UPTIME_REALTIME_DIFF_THRESHOLD_US = 3_000_000L; // 3 seconds

    private final TimeProvider mTimeProvider;
    private final Timebase mInputTimebase;
    private final CameraUseInconsistentTimebaseQuirk mCameraUseInconsistentTimebaseQuirk;

    private long mUptimeToRealtimeOffsetUs = -1L;
    @Nullable
    private Timebase mResolvedInputTimebase;

    /**
     * Constructs the VideoTimebaseConverter.
     *
     * @param timeProvider                       the time provider.
     * @param inputTimebase                      the input video frame timebase.
     * @param cameraUseInconsistentTimebaseQuirk the quirk denotes the camera use inconsistent
     *                                           timebase.
     */
    public VideoTimebaseConverter(@NonNull TimeProvider timeProvider,
            @NonNull Timebase inputTimebase,
            @Nullable CameraUseInconsistentTimebaseQuirk cameraUseInconsistentTimebaseQuirk) {
        mTimeProvider = timeProvider;
        mInputTimebase = inputTimebase;
        mCameraUseInconsistentTimebaseQuirk = cameraUseInconsistentTimebaseQuirk;
    }

    /**
     * Converts the video timestamp to {@link Timebase#UPTIME} if necessary.
     *
     * @param timestampUs the video frame timestamp in micro seconds. The timebase is supposed
     *                    to be the input timebase in constructor.
     */
    public long convertToUptimeUs(long timestampUs) {
        if (mResolvedInputTimebase == null) {
            mResolvedInputTimebase = resolveInputTimebase(timestampUs);
        }
        switch (mResolvedInputTimebase) {
            case REALTIME:
                if (mUptimeToRealtimeOffsetUs == -1) {
                    mUptimeToRealtimeOffsetUs = calculateUptimeToRealtimeOffsetUs();
                    Logger.d(TAG, "mUptimeToRealtimeOffsetUs = " + mUptimeToRealtimeOffsetUs);
                }
                return timestampUs - mUptimeToRealtimeOffsetUs;
            case UPTIME:
                return timestampUs;
            default:
                throw new AssertionError("Unknown timebase: " + mResolvedInputTimebase);
        }
    }

    @NonNull
    private Timebase resolveInputTimebase(long timestampUs) {
        boolean isSystemTimeDiverged = false;
        if (mCameraUseInconsistentTimebaseQuirk != null) {
            Logger.w(TAG, "CameraUseInconsistentTimebaseQuirk is enabled");
        } else if (exceedUptimeRealtimeDiffThreshold()) {
            // When the system uptime and real-time diverge significantly, detect the
            // input timebase to avoid timebase inconsistent issue.
            // See go/camerax-video-timebase-inconsistent-workaround.
            isSystemTimeDiverged = true;
        } else {
            return mInputTimebase;
        }

        Timebase resolvedTimebase = isCloseToRealtime(timestampUs)
                ? Timebase.REALTIME : Timebase.UPTIME;
        if (isSystemTimeDiverged && resolvedTimebase != mInputTimebase) {
            String socModelInfo = "";
            if (Build.VERSION.SDK_INT >= 31) {
                socModelInfo = ", SOC: " + Build.SOC_MODEL;
            }
            Logger.e(TAG, String.format("Detected camera timebase inconsistent. "
                            + "Please file an issue at "
                            + "https://issuetracker.google.com/issues/new?component=618491"
                            + "&template=1257717 with this error message "
                            + "[Manufacturer: %s, Model: %s, Hardware: %s, API Level: %d%s].\n"
                            + "Camera timebase is inconsistent. The timebase reported by the "
                            + "camera is %s, but the actual timebase contained in the frame is "
                            + "detected as %s.",
                    Build.MANUFACTURER, Build.MODEL, Build.HARDWARE, Build.VERSION.SDK_INT,
                    socModelInfo, mInputTimebase, resolvedTimebase));
        } else {
            Logger.d(TAG, "Detect input timebase = " + resolvedTimebase);
        }
        return resolvedTimebase;
    }

    private boolean exceedUptimeRealtimeDiffThreshold() {
        long uptimeUs = mTimeProvider.uptimeUs();
        long realTimeUs = mTimeProvider.realtimeUs();
        return realTimeUs - uptimeUs > UPTIME_REALTIME_DIFF_THRESHOLD_US;
    }

    private boolean isCloseToRealtime(long timeUs) {
        long uptimeUs = mTimeProvider.uptimeUs();
        long realtimeUs = mTimeProvider.realtimeUs();
        return Math.abs(timeUs - realtimeUs) < Math.abs(timeUs - uptimeUs);
    }

    // The algorithm is from camera framework Camera3Device.cpp
    private long calculateUptimeToRealtimeOffsetUs() {
        // Try three times to get the clock offset, choose the one with the minimum gap in
        // measurements.
        long bestGap = Long.MAX_VALUE;
        long measured = 0L;
        for (int i = 0; i < 3; i++) {
            long uptime1 = mTimeProvider.uptimeUs();
            long realtime = mTimeProvider.realtimeUs();
            long uptime2 = mTimeProvider.uptimeUs();
            long gap = uptime2 - uptime1;
            if (i == 0 || gap < bestGap) {
                bestGap = gap;
                measured = realtime - ((uptime1 + uptime2) >> 1);
            }
        }
        return Math.max(0, measured);
    }
}
