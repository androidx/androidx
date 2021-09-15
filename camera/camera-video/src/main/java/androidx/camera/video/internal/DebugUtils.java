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
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Logger;
import androidx.camera.video.internal.compat.Api28Impl;
import androidx.camera.video.internal.compat.Api31Impl;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for debugging.
 */
public final class DebugUtils {

    private static final String TAG = "DebugUtils";

    private static final String CODEC_CAPS_PREFIX = "[CodecCaps] ";
    private static final String VIDEO_CAPS_PREFIX = "[VideoCaps] ";
    private static final String AUDIO_CAPS_PREFIX = "[AudioCaps] ";
    private static final String ENCODER_CAPS_PREFIX = "[EncoderCaps] ";

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

    /**
     * Dumps {@link MediaCodecInfo} of input {@link MediaCodecList} and support for input
     * {@link MediaFormat}.
     */
    public static void dumpMediaCodecListForFormat(@NonNull MediaCodecList mediaCodecList,
            @NonNull MediaFormat mediaFormat) {
        Logger.d(TAG, "[Start] Dump MediaCodecList for mediaFormat " + mediaFormat);

        String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
        for (MediaCodecInfo mediaCodecInfo : mediaCodecList.getCodecInfos()) {
            if (!mediaCodecInfo.isEncoder()) {
                continue;
            }
            try {
                Preconditions.checkArgument(mime != null);
                MediaCodecInfo.CodecCapabilities caps = mediaCodecInfo.getCapabilitiesForType(mime);
                Preconditions.checkArgument(caps != null);

                Logger.d(TAG, "[Start] [" + mediaCodecInfo.getName() + "]");
                dumpCodecCapabilities(caps, mediaFormat);
                Logger.d(TAG, "[End] [" + mediaCodecInfo.getName() + "]");
            } catch (IllegalArgumentException e) {
                Logger.w(TAG, "[" + mediaCodecInfo.getName() + "] does not support mime " + mime);
            }
        }
        Logger.d(TAG, "[End] Dump MediaCodecList");
    }

    private static void dumpCodecCapabilities(@NonNull MediaCodecInfo.CodecCapabilities caps,
            @NonNull MediaFormat mediaFormat) {
        Logger.d(TAG,
                CODEC_CAPS_PREFIX + "isFormatSupported = " + caps.isFormatSupported(mediaFormat));
        Logger.d(TAG, CODEC_CAPS_PREFIX + "getDefaultFormat = " + caps.getDefaultFormat());
        if (caps.profileLevels != null) {
            StringBuilder stringBuilder = new StringBuilder("[");
            List<String> profileLevelsStr = new ArrayList<>();
            for (MediaCodecInfo.CodecProfileLevel profileLevel : caps.profileLevels) {
                profileLevelsStr.add(toString(profileLevel));
            }
            stringBuilder.append(TextUtils.join(", ", profileLevelsStr)).append("]");
            Logger.d(TAG, CODEC_CAPS_PREFIX + "profileLevels = " + stringBuilder);
        }
        if (caps.colorFormats != null) {
            Logger.d(TAG,
                    CODEC_CAPS_PREFIX + "colorFormats = " + Arrays.toString(caps.colorFormats));
        }

        MediaCodecInfo.VideoCapabilities videoCaps = caps.getVideoCapabilities();
        if (videoCaps != null) {
            dumpVideoCapabilities(videoCaps, mediaFormat);
        }

        MediaCodecInfo.AudioCapabilities audioCaps = caps.getAudioCapabilities();
        if (audioCaps != null) {
            dumpAudioCapabilities(audioCaps, mediaFormat);
        }

        MediaCodecInfo.EncoderCapabilities encoderCaps = caps.getEncoderCapabilities();
        if (encoderCaps != null) {
            dumpEncoderCapabilities(encoderCaps, mediaFormat);
        }
    }

    private static void dumpVideoCapabilities(@NonNull MediaCodecInfo.VideoCapabilities caps,
            @NonNull MediaFormat mediaFormat) {
        // Bitrate
        Logger.d(TAG, VIDEO_CAPS_PREFIX + "getBitrateRange = " + caps.getBitrateRange());

        // Size
        Logger.d(TAG, VIDEO_CAPS_PREFIX + "getSupportedWidths = " + caps.getSupportedWidths()
                + ", getWidthAlignment = " + caps.getWidthAlignment());
        Logger.d(TAG, VIDEO_CAPS_PREFIX + "getSupportedHeights = " + caps.getSupportedHeights()
                + ", getHeightAlignment = " + caps.getHeightAlignment());

        boolean hasSize = true;
        int width;
        int height;
        try {
            width = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
            height = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
            Preconditions.checkArgument(width > 0 && height > 0);
        } catch (NullPointerException | IllegalArgumentException e) {
            Logger.w(TAG,
                    VIDEO_CAPS_PREFIX + "mediaFormat does not contain valid width and height");
            width = height = 0;
            hasSize = false;
        }

        if (hasSize) {
            try {
                Logger.d(TAG, VIDEO_CAPS_PREFIX + "getSupportedHeightsFor " + width + " = "
                        + caps.getSupportedHeightsFor(width));
            } catch (IllegalArgumentException e) {
                Logger.w(TAG, VIDEO_CAPS_PREFIX + "could not getSupportedHeightsFor " + width, e);
            }
            try {
                Logger.d(TAG, VIDEO_CAPS_PREFIX + "getSupportedWidthsFor " + height + " = "
                        + caps.getSupportedWidthsFor(height));
            } catch (IllegalArgumentException e) {
                Logger.w(TAG, VIDEO_CAPS_PREFIX + "could not getSupportedWidthsFor " + height, e);
            }
            Logger.d(TAG, VIDEO_CAPS_PREFIX + "isSizeSupported for " + width + "x" + height
                    + " = " + caps.isSizeSupported(width, height));
        }

        // Frame rate
        Logger.d(TAG,
                VIDEO_CAPS_PREFIX + "getSupportedFrameRates = " + caps.getSupportedFrameRates());
        int frameRate;
        try {
            frameRate = mediaFormat.getInteger(MediaFormat.KEY_FRAME_RATE);
            Preconditions.checkArgument(frameRate > 0);
        } catch (NullPointerException | IllegalArgumentException e) {
            Logger.w(TAG, VIDEO_CAPS_PREFIX + "mediaFormat does not contain frame rate");
            frameRate = 0;
        }
        if (hasSize) {
            Logger.d(TAG,
                    VIDEO_CAPS_PREFIX + "getSupportedFrameRatesFor " + width + "x" + height + " = "
                            + caps.getSupportedFrameRatesFor(width, height));
        }
        if (hasSize && frameRate > 0) {
            Logger.d(TAG, VIDEO_CAPS_PREFIX + "areSizeAndRateSupported for "
                    + width + "x" + height + ", " + frameRate
                    + " = " + caps.areSizeAndRateSupported(width, height, frameRate));
        }
    }

    private static void dumpAudioCapabilities(@NonNull MediaCodecInfo.AudioCapabilities caps,
            @NonNull MediaFormat mediaFormat) {
        // Bitrate
        Logger.d(TAG, AUDIO_CAPS_PREFIX + "getBitrateRange = " + caps.getBitrateRange());

        // Channel count
        Logger.d(TAG,
                AUDIO_CAPS_PREFIX + "getMaxInputChannelCount = " + caps.getMaxInputChannelCount());

        if (Build.VERSION.SDK_INT >= 31) {
            Logger.d(TAG, AUDIO_CAPS_PREFIX + "getMinInputChannelCount = "
                    + Api31Impl.getMinInputChannelCount(caps));
            Logger.d(TAG, AUDIO_CAPS_PREFIX + "getInputChannelCountRanges = "
                    + Arrays.toString(Api31Impl.getInputChannelCountRanges(caps)));
        }

        // Sample rate
        Logger.d(TAG, AUDIO_CAPS_PREFIX + "getSupportedSampleRateRanges = "
                + Arrays.toString(caps.getSupportedSampleRateRanges()));
        Logger.d(TAG, AUDIO_CAPS_PREFIX + "getSupportedSampleRates = "
                + Arrays.toString(caps.getSupportedSampleRates()));

        try {
            int sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            Logger.d(TAG, AUDIO_CAPS_PREFIX + "isSampleRateSupported for " + sampleRate
                    + " = " + caps.isSampleRateSupported(sampleRate));
        } catch (NullPointerException | IllegalArgumentException e) {
            Logger.w(TAG, AUDIO_CAPS_PREFIX + "mediaFormat does not contain sample rate");
        }
    }

    private static void dumpEncoderCapabilities(@NonNull MediaCodecInfo.EncoderCapabilities caps,
            @NonNull MediaFormat mediaFormat) {

        Logger.d(TAG, ENCODER_CAPS_PREFIX + "getComplexityRange = " + caps.getComplexityRange());

        if (Build.VERSION.SDK_INT >= 28) {
            Logger.d(TAG,
                    ENCODER_CAPS_PREFIX + "getQualityRange = " + Api28Impl.getQualityRange(caps));
        }

        int bitrateMode;
        try {
            bitrateMode = mediaFormat.getInteger(MediaFormat.KEY_BITRATE_MODE);
            Logger.d(TAG, ENCODER_CAPS_PREFIX + "isBitrateModeSupported = "
                    + caps.isBitrateModeSupported(bitrateMode));
        } catch (NullPointerException | IllegalArgumentException e) {
            Logger.w(TAG, ENCODER_CAPS_PREFIX + "mediaFormat does not contain bitrate mode");
        }
    }

    @NonNull
    private static String toString(@Nullable MediaCodecInfo.CodecProfileLevel codecProfileLevel) {
        if (codecProfileLevel == null) {
            return "null";
        }
        return String.format("{level=%d, profile=%d}", codecProfileLevel.level,
                codecProfileLevel.profile);
    }
}
