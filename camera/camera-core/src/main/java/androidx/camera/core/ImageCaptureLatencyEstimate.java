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

package androidx.camera.core;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

import java.util.Objects;

/**
 * Defines the estimated duration an image capture will take capturing and processing for the
 * current scene condition and/or camera configuration.
 *
 * <p>The estimate comprises of two components: {@link #captureLatencyMillis},
 * {@link #processingLatencyMillis}
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class ImageCaptureLatencyEstimate {
    /** The capture latency is unsupported or undefined */
    public static final long UNDEFINED_CAPTURE_LATENCY = -1;

    /** The processing latency is unsupported or undefined */
    public static final long UNDEFINED_PROCESSING_LATENCY = -1;

    /** The image capture latency estimate is unsupported or undefined */
    @NonNull
    public static final ImageCaptureLatencyEstimate UNDEFINED_IMAGE_CAPTURE_LATENCY =
            new ImageCaptureLatencyEstimate(UNDEFINED_CAPTURE_LATENCY,
                    UNDEFINED_PROCESSING_LATENCY);

    /**
     * The estimated duration in milliseconds from when the camera begins capturing frames to the
     * moment the camera has completed capturing frames. If this estimate is not supported or not
     * available then it will be {@link #UNDEFINED_CAPTURE_LATENCY}.
     */
    public final long captureLatencyMillis;

    /**
     * The estimated duration in milliseconds from when the processing begins until the processing
     * has completed and the final processed capture is available. If this estimate is not supported
     * or not available then it will be {@link #UNDEFINED_PROCESSING_LATENCY}.
     */
    public final long processingLatencyMillis;

    ImageCaptureLatencyEstimate(long captureLatencyMillis, long processingLatencyMillis) {
        this.captureLatencyMillis = captureLatencyMillis;
        this.processingLatencyMillis = processingLatencyMillis;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ImageCaptureLatencyEstimate)) return false;
        ImageCaptureLatencyEstimate that = (ImageCaptureLatencyEstimate) o;
        return captureLatencyMillis == that.captureLatencyMillis
                && processingLatencyMillis == that.processingLatencyMillis;
    }

    @Override
    public int hashCode() {
        return Objects.hash(captureLatencyMillis, processingLatencyMillis);
    }

    @NonNull
    @Override
    public String toString() {
        return "captureLatencyMillis=" + captureLatencyMillis
                + ", processingLatencyMillis=" + processingLatencyMillis;
    }
}
