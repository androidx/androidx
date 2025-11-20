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

package androidx.camera.core.impl;

import android.hardware.camera2.CameraMetadata;
import android.util.Range;
import android.util.Size;

import androidx.camera.core.DynamicRange;
import androidx.camera.core.ViewPort;
import androidx.camera.core.streamsharing.StreamSharing;

import com.google.auto.value.AutoValue;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A stream specification defining how a camera frame stream should be configured.
 *
 * <p>The values communicated by this class specify what the camera is expecting to produce as a
 * frame stream, and can be useful for configuring the frame consumer.
 */
@AutoValue
public abstract class StreamSpec {

    /** A frame rate range with no specified upper or lower bound. */
    public static final Range<Integer> FRAME_RATE_RANGE_UNSPECIFIED = new Range<>(0, 0);

    /**
     * Returns the resolution for the stream associated with this stream specification.
     * @return the resolution for the stream.
     */
    public abstract @NonNull Size getResolution();

    /**
     * Returns the original resolution configured by the camera. This value is useful for
     * debugging and analysis, as it represents the initial resolution intended for the stream,
     * even if the stream is later modified.
     *
     * <p>This value typically matches the resolution returned by {@link #getResolution()},
     * but may differ if the stream is modified (e.g., cropped, scaled, or rotated)
     * after being configured by the camera. For example, {@link StreamSharing} first determines
     * which child use case's requested resolution to be its configured resolution and then
     * request a larger resolution from the camera. The camera stream is further modified (e.g.,
     * cropped, scaled, or rotated) to fit the configured resolution and other requirements such
     * as {@link ViewPort} and rotation. The final resolution after these
     * modifications would be reflected by {@link #getResolution()}, while this method returns the
     * original configured resolution.
     *
     * @return The originally configured camera resolution.
     */
    public abstract @NonNull Size getOriginalConfiguredResolution();

    /**
     * Returns the {@link DynamicRange} for the stream associated with this stream specification.
     * @return the dynamic range for the stream.
     */
    public abstract @NonNull DynamicRange getDynamicRange();

    /** Returns the session type associated with this stream. */
    public abstract int getSessionType();

    /**
     * Returns the expected frame rate range for the stream associated with this stream
     * specification.
     * @return the expected frame rate range for the stream.
     */
    public abstract @NonNull Range<Integer> getExpectedFrameRateRange();

    /**
     * Returns the implementation options associated with this stream
     * specification.
     * @return the implementation options for the stream.
     */
    public abstract @Nullable Config getImplementationOptions();

    /**
     * Returns the flag if zero-shutter lag needs to be disabled by user case combinations.
     */
    public abstract boolean getZslDisabled();

    /** Returns a build for a stream configuration that takes a required resolution. */
    public static @NonNull Builder builder(@NonNull Size resolution) {
        return new AutoValue_StreamSpec.Builder()
                .setResolution(resolution)
                .setOriginalConfiguredResolution(resolution)
                .setSessionType(SessionConfig.DEFAULT_SESSION_TYPE)
                .setExpectedFrameRateRange(FRAME_RATE_RANGE_UNSPECIFIED)
                .setDynamicRange(DynamicRange.SDR)
                .setZslDisabled(false);
    }

    /** Returns a builder pre-populated with the current specification. */
    public abstract @NonNull Builder toBuilder();

    /** A builder for a stream specification */
    @AutoValue.Builder
    public abstract static class Builder {
        // Restrict construction to same package
        Builder() {
        }

        /** Sets the resolution, overriding the existing resolution set in this builder. */
        public abstract @NonNull Builder setResolution(@NonNull Size resolution);

        /**
         * Sets the original resolution configured by the camera. This value is useful for
         * debugging and analysis, as it represents the initial resolution intended by the stream
         * consumer, even if the stream is later modified.
         *
         * <p>This value typically matches the resolution set by {@link #setResolution(Size)},
         * but may differ if the stream is modified (e.g., cropped, scaled, or rotated)
         * after being configured by the camera. For example, {@link StreamSharing} first
         * determines which child use case's requested resolution to be its configured resolution
         * and then request a larger resolution from the camera. The camera stream is further
         * modified (e.g., cropped, scaled, or rotated) to fit the configured resolution and other
         * requirements such as {@link ViewPort} and rotation. The final resolution after these
         * modifications is set by {@link #setResolution(Size)}, while this method retains the
         * original configured resolution.
         *
         * <p>If not set, this value will default to the resolution set in this builder.
         */
        public abstract @NonNull Builder setOriginalConfiguredResolution(@NonNull Size resolution);

        /**
         * Sets the session type.
         *
         * <p>If not set, the default session type is {@link SessionConfig#DEFAULT_SESSION_TYPE}.
         */
        public abstract @NonNull Builder setSessionType(int sessionType);
        /**
         * Sets the dynamic range.
         *
         * <p>If not set, the default dynamic range is {@link DynamicRange#SDR}.
         */
        public abstract @NonNull Builder setDynamicRange(@NonNull DynamicRange dynamicRange);

        /**
         * Sets the expected frame rate range.
         *
         * <p>If not set, the default expected frame rate range is
         * {@link #FRAME_RATE_RANGE_UNSPECIFIED}.
         */
        public abstract @NonNull Builder setExpectedFrameRateRange(@NonNull Range<Integer> range);

        /**
         * Sets the implementation options.
         *
         * <p>If not set, the default expected frame rate range is
         * {@link CameraMetadata#SCALER_AVAILABLE_STREAM_USE_CASES_DEFAULT}.
         */
        public abstract @NonNull Builder setImplementationOptions(@NonNull Config config);

        /**
         * Sets the flag if zero-shutter lag needs to be disabled by user case combinations.
         */
        public abstract @NonNull Builder setZslDisabled(boolean disabled);

        /** Builds the stream specification */
        public abstract @NonNull StreamSpec build();
    }

}
