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

import android.util.Range;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.google.auto.value.AutoValue;

/**
 * A stream specification defining how a camera frame stream should be configured.
 *
 * <p>The values communicated by this class specify what the camera is expecting to produce as a
 * frame stream, and can be useful for configuring the frame consumer.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@AutoValue
public abstract class StreamSpec {

    /** A frame rate range with no specified upper or lower bound. */
    public static final Range<Integer> FRAME_RATE_RANGE_UNSPECIFIED = new Range<>(0, 0);

    /**
     * Returns the resolution for the stream associated with this stream specification.
     * @return the resolution for the stream.
     */
    @NonNull
    public abstract Size getResolution();

    /**
     * Returns the expected frame rate range for the stream associated with this stream
     * specification.
     * @return the expected frame rate range for the stream.
     */
    @NonNull
    public abstract Range<Integer> getExpectedFrameRateRange();

    /** Returns a build for a stream configuration that takes a required resolution. */
    @NonNull
    public static Builder builder(@NonNull Size resolution) {
        return new AutoValue_StreamSpec.Builder()
                .setResolution(resolution)
                .setExpectedFrameRateRange(FRAME_RATE_RANGE_UNSPECIFIED);
    }

    /** Returns a builder pre-populated with the current specification. */
    @NonNull
    public abstract Builder toBuilder();

    /** A builder for a stream specification */
    @AutoValue.Builder
    public abstract static class Builder {
        // Restrict construction to same package
        Builder() {
        }

        /** Sets the resolution, overriding the existing resolution set in this builder. */
        @NonNull
        public abstract Builder setResolution(@NonNull Size resolution);

        /**
         * Sets the expected frame rate range.
         *
         * <p>If not set, the default expected frame rate range is
         * {@link #FRAME_RATE_RANGE_UNSPECIFIED}.
         */
        @NonNull
        public abstract Builder setExpectedFrameRateRange(@NonNull Range<Integer> range);

        /** Builds the stream specification */
        @NonNull
        public abstract StreamSpec build();
    }

}
