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

package androidx.camera.video.internal;

import android.media.MediaCodec;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for debugging.
 */
public final class DebugUtils {

    private DebugUtils() {}

    /**
     * Returns a formatted string according to the input time, the format is
     * "hours:minutes:seconds.milliseconds".
     *
     * @param time input time in microseconds.
     * @return the formatted string.
     */
    @NonNull
    public static String readableUs(long time) {
        return readableMs(TimeUnit.MICROSECONDS.toMillis(time));
    }

    /**
     * Returns a formatted string according to the input time, the format is
     * "hours:minutes:seconds.milliseconds".
     *
     * @param time input time in milliseconds.
     * @return the formatted string.
     */
    @NonNull
    public static String readableMs(long time) {
        return formatInterval(time);
    }

    /**
     * Returns a formatted string according to the input {@link MediaCodec.BufferInfo}.
     *
     * @param bufferInfo the {@link MediaCodec.BufferInfo}.
     * @return the formatted string.
     */
    @NonNull
    @SuppressWarnings("ObjectToString")
    public static String readableBufferInfo(@NonNull MediaCodec.BufferInfo bufferInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append("Dump BufferInfo: " + bufferInfo.toString() + "\n");
        sb.append("\toffset: " + bufferInfo.offset + "\n");
        sb.append("\tsize: " + bufferInfo.size + "\n");
        {
            sb.append("\tflag: " + bufferInfo.flags);
            List<String> flagList = new ArrayList<>();
            if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                flagList.add("EOS");
            }
            if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                flagList.add("CODEC_CONFIG");
            }
            if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) {
                flagList.add("KEY_FRAME");
            }
            if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_PARTIAL_FRAME) != 0) {
                flagList.add("PARTIAL_FRAME");
            }
            if (!flagList.isEmpty()) {
                sb.append(" (").append(TextUtils.join(" | ", flagList)).append(")");
            }
            sb.append("\n");
        }
        sb.append("\tpresentationTime: " + bufferInfo.presentationTimeUs + " ("
                + readableUs(bufferInfo.presentationTimeUs) + ")\n");
        return sb.toString();
    }

    private static String formatInterval(long millis) {
        final long hr = TimeUnit.MILLISECONDS.toHours(millis);
        final long min = TimeUnit.MILLISECONDS.toMinutes(millis - TimeUnit.HOURS.toMillis(hr));
        final long sec = TimeUnit.MILLISECONDS.toSeconds(
                millis - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min));
        final long ms = millis - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min)
                - TimeUnit.SECONDS.toMillis(sec);
        return String.format(Locale.US, "%02d:%02d:%02d.%03d", hr, min, sec, ms);
    }
}
