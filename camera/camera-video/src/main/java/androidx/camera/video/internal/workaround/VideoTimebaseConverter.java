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
 * <p>The workaround accepts an {@code null} input timebase. This is useful when the timebase is
 * unknown, such as the problem described in b/197805856. If the input timebase is null, an
 * automatic detection mechanism is used to determine the timebase, which is by checking the input
 * timestamp is close to UPTIME or REALTIME. For performance reason, the detection will only check
 * the first input timestamp.
 *
 * @see CameraUseInconsistentTimebaseQuirk
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class VideoTimebaseConverter {
    private static final String TAG = "VideoTimebaseConverter";

    private final TimeProvider mTimeProvider;

    private long mUptimeToRealtimeOffsetUs = -1L;
    private Timebase mInputTimebase;

    /**
     * Constructs the VideoTimebaseConverter.
     *
     * @param timeProvider  the time provider.
     * @param inputTimebase the input video frame timebase. {@code null} if the timebase is unknown.
     */
    public VideoTimebaseConverter(@NonNull TimeProvider timeProvider,
            @Nullable Timebase inputTimebase) {
        mTimeProvider = timeProvider;
        mInputTimebase = inputTimebase;
    }

    /**
     * Converts the video timestamp to {@link Timebase#UPTIME} if necessary.
     *
     * @param timestampUs the video frame timestamp in micro seconds. The timebase is supposed
     *                    to be the input timebase in constructor.
     */
    public long convertToUptimeUs(long timestampUs) {
        if (mInputTimebase == null) {
            if (isCloseToRealtime(timestampUs)) {
                mInputTimebase = Timebase.REALTIME;
            } else {
                mInputTimebase = Timebase.UPTIME;
            }
            Logger.d(TAG, "Detect input timebase = " + mInputTimebase);
        }
        switch (mInputTimebase) {
            case REALTIME:
                if (mUptimeToRealtimeOffsetUs == -1) {
                    mUptimeToRealtimeOffsetUs = calculateUptimeToRealtimeOffsetUs();
                    Logger.d(TAG, "mUptimeToRealtimeOffsetUs = " + mUptimeToRealtimeOffsetUs);
                }
                return timestampUs - mUptimeToRealtimeOffsetUs;
            case UPTIME:
                return timestampUs;
            default:
                throw new AssertionError("Unknown timebase: " + mInputTimebase);
        }
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
