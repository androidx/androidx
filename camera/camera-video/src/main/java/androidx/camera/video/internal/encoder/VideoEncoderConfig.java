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

import android.media.MediaFormat;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.google.auto.value.AutoValue;

/** {@inheritDoc} */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@AutoValue
public abstract class VideoEncoderConfig implements EncoderConfig {

    // Restrict constructor to same package
    VideoEncoderConfig() {
    }

    /** Returns a build for this config. */
    @NonNull
    public static Builder builder() {
        return new AutoValue_VideoEncoderConfig.Builder();
    }

    /** {@inheritDoc} */
    @Override
    @NonNull
    public abstract String getMimeType();

    /** Gets the resolution. */
    @NonNull
    public abstract Size getResolution();

    /** Gets the color format. */
    public abstract int getColorFormat();

    /** Gets the frame rate. */
    public abstract int getFrameRate();

    /** Gets the i-frame interval. */
    public abstract int getIFrameInterval();

    /** Gets the bitrate. */
    public abstract int getBitrate();

    /** {@inheritDoc} */
    @NonNull
    @Override
    public MediaFormat toMediaFormat() {
        Size size = getResolution();
        MediaFormat format = MediaFormat.createVideoFormat(getMimeType(), size.getWidth(),
                size.getHeight());
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, getColorFormat());
        format.setInteger(MediaFormat.KEY_BIT_RATE, getBitrate());
        format.setInteger(MediaFormat.KEY_FRAME_RATE, getFrameRate());
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, getIFrameInterval());
        return format;
    }

    /** The builder of the config. */
    @AutoValue.Builder
    public abstract static class Builder {
        // Restrict construction to same package
        Builder() {
        }

        /** Sets the mime type. */
        @NonNull
        public abstract Builder setMimeType(@NonNull String mimeType);

        /** Sets the resolution. */
        @NonNull
        public abstract Builder setResolution(@NonNull Size resolution);

        /** Sets the color format. */
        @NonNull
        public abstract Builder setColorFormat(int colorFormat);

        /** Sets the frame rate. */
        @NonNull
        public abstract Builder setFrameRate(int frameRate);

        /** Sets the i-frame interval. */
        @NonNull
        public abstract Builder setIFrameInterval(int iFrameInterval);

        /** Sets the bitrate. */
        @NonNull
        public abstract Builder setBitrate(int bitrate);

        /** Builds the config instance. */
        @NonNull
        public abstract VideoEncoderConfig build();
    }
}
