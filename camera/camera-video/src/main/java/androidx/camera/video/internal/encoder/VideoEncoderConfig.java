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

package androidx.camera.video.internal.encoder;

import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Size;

import androidx.camera.core.impl.Timebase;

import com.google.auto.value.AutoValue;

import org.jspecify.annotations.NonNull;

/** {@inheritDoc} */
@AutoValue
public abstract class VideoEncoderConfig implements EncoderConfig {

    private static final int VIDEO_INTRA_FRAME_INTERVAL_DEFAULT = 1;
    private static final int VIDEO_COLOR_FORMAT_DEFAULT =
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface;

    // Restrict constructor to same package
    VideoEncoderConfig() {
    }

    /** Returns a build for this config. */
    public static @NonNull Builder builder() {
        return new AutoValue_VideoEncoderConfig.Builder()
                .setProfile(EncoderConfig.CODEC_PROFILE_NONE)
                .setIFrameInterval(VIDEO_INTRA_FRAME_INTERVAL_DEFAULT)
                .setColorFormat(VIDEO_COLOR_FORMAT_DEFAULT)
                .setDataSpace(VideoEncoderDataSpace.ENCODER_DATA_SPACE_UNSPECIFIED);
    }

    /** Returns a build from the input config. */
    public abstract @NonNull Builder toBuilder();

    @Override
    public abstract @NonNull String getMimeType();

    @Override
    public abstract int getProfile();

    @Override
    public abstract @NonNull Timebase getInputTimebase();

    /** Gets the resolution. */
    public abstract @NonNull Size getResolution();

    /** Gets the color format. */
    public abstract int getColorFormat();

    /** Gets the color data space. */
    public abstract @NonNull VideoEncoderDataSpace getDataSpace();

    /** Gets the capture frame rate. */
    public abstract int getCaptureFrameRate();

    /** Gets the encode frame rate. */
    public abstract int getEncodeFrameRate();

    /** Gets the i-frame interval. */
    public abstract int getIFrameInterval();

    /** Gets the bitrate. */
    public abstract int getBitrate();

    /** {@inheritDoc} */
    @Override
    public @NonNull MediaFormat toMediaFormat() {
        Size size = getResolution();
        MediaFormat format = MediaFormat.createVideoFormat(getMimeType(), size.getWidth(),
                size.getHeight());
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, getColorFormat());
        format.setInteger(MediaFormat.KEY_BIT_RATE, getBitrate());
        format.setInteger(MediaFormat.KEY_FRAME_RATE, getEncodeFrameRate());
        if (isSlowMotion()) {
            // Timestamps for slow-motion recording are automatically adjusted by the
            // GraphicBufferSource from API 30 onward (specifically when configuring
            // codec with different KEY_CAPTURE_RATE and KEY_FRAME_RATE). For devices
            // on earlier API levels, we manually adjust the timestamp.
            // See ACodec.cpp/CCodec.cpp/GraphicBufferSource.cpp for details.
            format.setInteger(MediaFormat.KEY_CAPTURE_RATE, getCaptureFrameRate());
            format.setInteger(MediaFormat.KEY_OPERATING_RATE, getCaptureFrameRate());
            format.setInteger(MediaFormat.KEY_PRIORITY, 0); // Smaller value, higher priority.
        }
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, getIFrameInterval());
        if (getProfile() != EncoderConfig.CODEC_PROFILE_NONE) {
            format.setInteger(MediaFormat.KEY_PROFILE, getProfile());
        }
        VideoEncoderDataSpace dataSpace = getDataSpace();
        if (dataSpace.getStandard() != VideoEncoderDataSpace.VIDEO_COLOR_STANDARD_UNSPECIFIED) {
            format.setInteger(MediaFormat.KEY_COLOR_STANDARD, dataSpace.getStandard());
        }
        if (dataSpace.getTransfer() != VideoEncoderDataSpace.VIDEO_COLOR_TRANSFER_UNSPECIFIED) {
            format.setInteger(MediaFormat.KEY_COLOR_TRANSFER, dataSpace.getTransfer());
        }
        if (dataSpace.getRange() != VideoEncoderDataSpace.VIDEO_COLOR_RANGE_UNSPECIFIED) {
            format.setInteger(MediaFormat.KEY_COLOR_RANGE, dataSpace.getRange());
        }
        return format;
    }

    /** Returns whether the encoder config is for slow-motion recording. */
    public boolean isSlowMotion() {
        return getCaptureFrameRate() > getEncodeFrameRate();
    }

    /** The builder of the config. */
    @AutoValue.Builder
    public abstract static class Builder {
        // Restrict construction to same package
        Builder() {
        }

        /** Sets the mime type. */
        public abstract @NonNull Builder setMimeType(@NonNull String mimeType);

        /** Sets (optional) profile for the mime type specified by {@link #setMimeType(String)}. */
        public abstract @NonNull Builder setProfile(int profile);

        /** Sets the source timebase. */
        public abstract @NonNull Builder setInputTimebase(@NonNull Timebase timebase);

        /** Sets the resolution. */
        public abstract @NonNull Builder setResolution(@NonNull Size resolution);

        /** Sets the color format. */
        public abstract @NonNull Builder setColorFormat(int colorFormat);

        /** Sets the color data space. */
        public abstract @NonNull Builder setDataSpace(@NonNull VideoEncoderDataSpace dataSpace);

        /** Sets the capture frame rate. */
        public abstract @NonNull Builder setCaptureFrameRate(int frameRate);

        /** Sets the encode frame rate. */
        public abstract @NonNull Builder setEncodeFrameRate(int frameRate);

        /** Sets the i-frame interval. */
        public abstract @NonNull Builder setIFrameInterval(int iFrameInterval);

        /** Sets the bitrate. */
        public abstract @NonNull Builder setBitrate(int bitrate);

        /** Builds the config instance. */
        public abstract @NonNull VideoEncoderConfig build();
    }
}
