/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.extensions.impl;

import android.annotation.SuppressLint;
import android.graphics.ImageFormat;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.util.Pair;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;
import java.util.concurrent.Executor;

/**
 * The interface for processing a set of {@link Image}s that have captured.
 *
 * @since 1.0
 */
@SuppressLint("UnknownNullness")
public interface CaptureProcessorImpl extends ProcessorImpl {
    /**
     * Process a set images captured that were requested.
     *
     * <p> The result of the processing step should be written to the {@link Surface} that was
     * received by {@link #onOutputSurface(Surface, int)}.
     *
     * @param results The map of {@link ImageFormat#YUV_420_888} format images and metadata to
     *                process. The {@link Image} that are contained within the map will become
     *                invalid after this method completes, so no references to them should be kept.
     */
    void process(@NonNull Map<Integer, Pair<Image, TotalCaptureResult>> results);

    /**
     * Informs the CaptureProcessorImpl where it should write the postview output to.
     * This will only be invoked once if a valid postview surface was set.
     *
     * @param surface A valid {@link ImageFormat#YUV_420_888} {@link Surface}
     *                that the CaptureProcessorImpl should write data into.
     * @since 1.4
     */
    void onPostviewOutputSurface(@NonNull Surface surface);

    /**
     * Invoked when the Camera Framework changes the configured output resolution for
     * still capture and postview.
     *
     * <p>After this call, {@link CaptureProcessorImpl} should expect any {@link Image} received as
     * input for still capture and postview to be at the specified resolutions.
     *
     * @param size for the surface for still capture.
     * @param postviewSize for the surface for postview.
     * @since 1.4
     */
    void onResolutionUpdate(@NonNull Size size, @NonNull Size postviewSize);

    /**
     * Process a set images captured that were requested.
     *
     * <p> The result of the processing step should be written to the {@link Surface} that was
     * received by {@link #onOutputSurface(Surface, int)}.
     *
     * @param results        The map of {@link ImageFormat#YUV_420_888} format images and metadata
     *                       to process. The {@link Image} that are contained within the map will
     *                       become invalid after this method completes, so no references to them
     *                       should be kept.
     * @param resultCallback Capture result callback to be called once the capture result
     *                       values of the processed image are ready.
     * @param executor       The executor to run the callback on. If null then the callback will
     *                       run on any arbitrary executor.
     * @since 1.3
     */
    void process(@NonNull Map<Integer, Pair<Image, TotalCaptureResult>> results,
            @NonNull ProcessResultImpl resultCallback, @Nullable Executor executor);

    /**
     * Process a set images captured that were requested for both postview and
     * still capture.
     *
     * <p> This processing method will be called if a postview was requested, therefore the
     * processed postview should be written to the
     * {@link Surface} received by {@link #onPostviewOutputSurface(Surface, int)}.
     * The final result of the processing step should be written to the {@link Surface} that was
     * received by {@link #onOutputSurface(Surface, int)}. Since postview should be available
     * before the capture, it should be processed and written to the surface before
     * the final capture is processed.
     *
     * @param results             The map of {@link ImageFormat#YUV_420_888} format images and
     *                            metadata to process. The {@link Image} that are contained within
     *                            the map will become invalid after this method completes, so no
     *                            references to them should be kept.
     * @param resultCallback      Capture result callback to be called once the capture result
     *                            values of the processed image are ready.
     * @param executor            The executor to run the callback on. If null then the callback
     *                            will run on any arbitrary executor.
     * @throws RuntimeException   if postview feature is not supported
     * @since 1.4
     */
    void processWithPostview(@NonNull Map<Integer, Pair<Image, TotalCaptureResult>> results,
            @NonNull ProcessResultImpl resultCallback, @Nullable Executor executor);
}
