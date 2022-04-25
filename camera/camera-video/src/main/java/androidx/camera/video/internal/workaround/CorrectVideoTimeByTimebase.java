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

import android.media.MediaCodec;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.Logger;
import androidx.camera.video.internal.compat.quirk.CameraUseInconsistentTimebaseQuirk;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Corrects the video timestamps if video buffer contains REALTIME timestamp.
 *
 * <p>As described on b/197805856, some Samsung devices use inconsistent timebase for camera
 * frame. The workaround detects and corrects the timestamp by generating a new timestamp.
 * Note: this will sacrifice the precise timestamp of video buffer.
 *
 * @see CameraUseInconsistentTimebaseQuirk
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class CorrectVideoTimeByTimebase {
    private static final String TAG = "CorrectVideoTimeByTimebase";

    @Nullable
    private AtomicBoolean mNeedToCorrectVideoTimebase = null;

    /**
     * Corrects the video timestamp if necessary.
     *
     * <p>This method will modify the {@link MediaCodec.BufferInfo#presentationTimeUs} if necessary.
     *
     * @param bufferInfo the buffer info.
     */
    public void correctTimestamp(@NonNull MediaCodec.BufferInfo bufferInfo) {
        // For performance concern, only check the requirement once.
        if (mNeedToCorrectVideoTimebase == null) {
            // Skip invalid buffer
            if (bufferInfo.size <= 0 || bufferInfo.presentationTimeUs <= 0L
                    || (bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                return;
            }

            long uptimeUs = TimeUnit.MILLISECONDS.toMicros(SystemClock.uptimeMillis());
            long realtimeUs = TimeUnit.MILLISECONDS.toMicros(SystemClock.elapsedRealtime());
            // Expected to be uptime
            boolean closeToRealTime = Math.abs(bufferInfo.presentationTimeUs - realtimeUs)
                    < Math.abs(bufferInfo.presentationTimeUs - uptimeUs);
            if (closeToRealTime) {
                Logger.w(TAG, "Detected video buffer timestamp is close to real time.");
            }
            mNeedToCorrectVideoTimebase = new AtomicBoolean(closeToRealTime);
        }

        if (mNeedToCorrectVideoTimebase.get()) {
            bufferInfo.presentationTimeUs -= TimeUnit.MILLISECONDS.toMicros(
                    SystemClock.elapsedRealtime() - SystemClock.uptimeMillis());
        }
    }
}
