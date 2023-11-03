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

package androidx.camera.video.internal.audio;

import static android.media.AudioFormat.ENCODING_PCM_16BIT;
import static android.media.AudioFormat.ENCODING_PCM_24BIT_PACKED;
import static android.media.AudioFormat.ENCODING_PCM_32BIT;
import static android.media.AudioFormat.ENCODING_PCM_8BIT;
import static android.media.AudioFormat.ENCODING_PCM_FLOAT;

import static androidx.core.util.Preconditions.checkArgument;

import android.media.AudioFormat;
import android.media.AudioTimestamp;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.concurrent.TimeUnit;

/** Utility class for audio-related operations and calculations. */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class AudioUtils {

    // Prevent instantiation
    private AudioUtils() {
    }

    /**
     * Converts a channel count to the channel config of {@link AudioFormat}.
     *
     * @return channel config.
     *
     * @see AudioFormat#CHANNEL_IN_MONO
     * @see AudioFormat#CHANNEL_IN_STEREO
     */
    public static int channelCountToChannelConfig(int channelCount) {
        return channelCount == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO;
    }

    /**
     * Converts a channel count to the channel mask of {@link AudioFormat}.
     *
     * @return channel mask.
     *
     * @see AudioFormat#CHANNEL_IN_MONO
     * @see AudioFormat#CHANNEL_IN_STEREO
     */
    public static int channelCountToChannelMask(int channelCount) {
        // Currently equivalent to channelCountToChannelConfig, but keep this logic separate
        // since technically channel masks are different from the legacy channel config and we don't
        // want any future updates to break things.
        return channelCount == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO;
    }

    /**
     * Gets the size in bytes per frame.
     *
     * @param audioEncoding the audio encoding of {@link AudioFormat}.
     * @param channelCount  the channel count.
     * @return bytes per frame.
     * @throws IllegalArgumentException if the channel count or audio encoding is invalid.
     *
     * @see AudioFormat#ENCODING_PCM_8BIT
     * @see AudioFormat#ENCODING_PCM_16BIT
     * @see AudioFormat#ENCODING_PCM_24BIT_PACKED
     * @see AudioFormat#ENCODING_PCM_32BIT
     * @see AudioFormat#ENCODING_PCM_FLOAT
     */
    public static int getBytesPerFrame(int audioEncoding, int channelCount) {
        checkArgument(channelCount > 0, "Invalid channel count: " + channelCount);
        switch (audioEncoding) {
            case ENCODING_PCM_8BIT:
                return channelCount;
            case ENCODING_PCM_16BIT:
                return channelCount * 2;
            case ENCODING_PCM_24BIT_PACKED:
                return channelCount * 3;
            case ENCODING_PCM_32BIT:
            case ENCODING_PCM_FLOAT:
                return channelCount * 4;
            default:
                throw new IllegalArgumentException("Invalid audio encoding: " + audioEncoding);
        }
    }

    /**
     * Calculates the frame count by the input size in bytes and the per frame size.
     *
     * <p>If the size is not divisible by the per frame size, the decimal will be rounded down.
     *
     * <p>Negative size is allowed which is useful to calculate frame count difference.
     *
     * @param sizeInBytes   size in bytes.
     * @param bytesPerFrame bytes per frame. Must be greater than 0.
     * @return frame count.
     * @throws IllegalArgumentException if bytesPerFrame is not greater than 0.
     */
    public static long sizeToFrameCount(long sizeInBytes, int bytesPerFrame) {
        checkArgument(bytesPerFrame > 0L, "bytesPerFrame must be greater than 0.");
        return sizeInBytes / bytesPerFrame;
    }

    /**
     * Calculates the size in bytes by the input frame count and the per frame size.
     *
     * <p>Negative frame count is allowed which is useful to calculate size difference.
     *
     * @param frameCount    frame count.
     * @param bytesPerFrame bytes per frame. Must be greater than 0.
     * @return size in bytes.
     * @throws IllegalArgumentException if bytesPerFrame is not greater than 0.
     */
    public static long frameCountToSize(long frameCount, int bytesPerFrame) {
        checkArgument(bytesPerFrame > 0L, "bytesPerFrame must be greater than 0.");
        return frameCount * bytesPerFrame;
    }

    /**
     * Calculates the duration in nanoseconds by the input frame count and sample rate.
     *
     * <p>Negative frame count is allowed which is useful to calculate negative duration offset.
     *
     * @param frameCount the frame count.
     * @param sampleRate the sample rate. Must be greater than 0.
     * @return duration in nanoseconds.
     * @throws IllegalArgumentException if sampleRate is not greater than 0.
     */
    public static long frameCountToDurationNs(long frameCount, int sampleRate) {
        checkArgument(sampleRate > 0L, "sampleRate must be greater than 0.");
        return TimeUnit.SECONDS.toNanos(1) * frameCount / sampleRate;
    }

    /**
     * Computes the interpolated timestamp.
     *
     * @param sampleRate    the sample rate. Must be greater than 0.
     * @param framePosition the frame count. Must be no less than 0.
     * @param timestamp     the audio timestamp object.
     * @return interpolated timestamp in nanoseconds.
     * @throws IllegalArgumentException if sampleRate is not greater than 0 or framePosition is
     *                                  less than 0.
     */
    public static long computeInterpolatedTimeNs(int sampleRate, long framePosition,
            @NonNull AudioTimestamp timestamp) {
        checkArgument(sampleRate > 0L, "sampleRate must be greater than 0.");
        checkArgument(framePosition >= 0L, "framePosition must be no less than 0.");
        long frameDiff = framePosition - timestamp.framePosition;
        long compensateTimeInNanoSec = frameCountToDurationNs(frameDiff, sampleRate);
        long resultInNanoSec = timestamp.nanoTime + compensateTimeInNanoSec;
        return resultInNanoSec < 0 ? 0 : resultInNanoSec;
    }
}
