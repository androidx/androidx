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

import java.util.Objects;

/**
 * An immutable representation of the estimated duration an image capture will take
 * capturing and processing the current scene according to the scene's lighting condition and/or
 * camera configuration.
 *
 * <p>The latency estimate is produced by {@link ImageCapture#getRealtimeCaptureLatencyEstimate()}.
 *
 * <p>The estimate is comprised of two components: {@link #getCaptureLatencyMillis()},
 * {@link #getProcessingLatencyMillis()}.
 */
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

    private final long mCaptureLatencyMillis;
    private final long mProcessingLatencyMillis;
    private final long mTotalCaptureLatencyMillis;

    /**
     * Created by {@link ImageCapture#getRealtimeCaptureLatencyEstimate()} when querying for the
     * current realtime latency estimate. This can also be used for testing. It is not necessary to
     * explicitly construct this in any other scenario.
     *
     * @param captureLatencyMillis The estimated duration in milliseconds from when the camera
     *                             begins capturing frames to the moment the camera has completed
     *                             capturing frames.
     * @param processingLatencyMillis The estimated duration in milliseconds from when the
     *                                processing begins until the processing has completed and the
     *                                final processed capture is available.
     */
    public ImageCaptureLatencyEstimate(long captureLatencyMillis, long processingLatencyMillis) {
        mCaptureLatencyMillis = captureLatencyMillis;
        mProcessingLatencyMillis = processingLatencyMillis;
        mTotalCaptureLatencyMillis = computeTotalCaptureLatencyMillis(captureLatencyMillis,
                processingLatencyMillis);
    }

    /**
     * Returns the estimated duration in milliseconds from when the camera begins capturing
     * frames to the moment the camera has completed capturing frames. If this estimate is not
     * supported or not available then it will be {@link #UNDEFINED_CAPTURE_LATENCY}.
     */
    public long getCaptureLatencyMillis() {
        return mCaptureLatencyMillis;
    }

    /**
     * Returns the estimated duration in milliseconds from when the processing begins until the
     * processing has completed and the final processed capture is available. If this estimate is
     * not supported or not available then it will be {@link #UNDEFINED_PROCESSING_LATENCY}.
     */
    public long getProcessingLatencyMillis() {
        return mProcessingLatencyMillis;
    }

    /**
     * Returns the total estimated capture duration in milliseconds. This includes time spent in
     * capturing and processing.
     *
     * <p>If either the capture latency or processing latency is undefined then the total estimate
     * is {@link #UNDEFINED_CAPTURE_LATENCY}.
     */
    public long getTotalCaptureLatencyMillis() {
        return mTotalCaptureLatencyMillis;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ImageCaptureLatencyEstimate)) return false;
        ImageCaptureLatencyEstimate that = (ImageCaptureLatencyEstimate) o;
        return mCaptureLatencyMillis == that.getCaptureLatencyMillis()
                && mProcessingLatencyMillis == that.getProcessingLatencyMillis()
                && mTotalCaptureLatencyMillis == that.getTotalCaptureLatencyMillis();
    }

    @Override
    public int hashCode() {
        return Objects.hash(mCaptureLatencyMillis, mProcessingLatencyMillis,
                mTotalCaptureLatencyMillis);
    }

    @NonNull
    @Override
    public String toString() {
        return "captureLatencyMillis=" + mCaptureLatencyMillis
                + ", processingLatencyMillis=" + mProcessingLatencyMillis
                + ", totalCaptureLatencyMillis=" + mTotalCaptureLatencyMillis;
    }

    private long computeTotalCaptureLatencyMillis(long captureLatencyMillis,
            long processingLatencyMillis) {
        if (captureLatencyMillis == UNDEFINED_PROCESSING_LATENCY
                || processingLatencyMillis == UNDEFINED_CAPTURE_LATENCY) {
            return UNDEFINED_CAPTURE_LATENCY;
        }
        return captureLatencyMillis + processingLatencyMillis;
    }
}
