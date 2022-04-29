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
import android.view.Surface;

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
    void process(Map<Integer, Pair<Image, TotalCaptureResult>> results);

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
    void process(Map<Integer, Pair<Image, TotalCaptureResult>> results,
            ProcessResultImpl resultCallback, Executor executor);
}
