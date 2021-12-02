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

import android.graphics.ImageFormat;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.util.Pair;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.RequiresApi;

import java.util.Map;

/**
 * The interface for processing a set of {@link Image}s that have captured.
 *
 * @since 1.0
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface CaptureProcessorImpl {
    /**
     * This gets called to update where the CaptureProcessor should write the output of {@link
     * #process(Map)}.
     *
     * @param surface The {@link Surface} that the CaptureProcessor should write data into.
     * @param imageFormat The format of that the surface expects.
     */
    void onOutputSurface(Surface surface, int imageFormat);

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
     * This callback will be invoked when CameraX changes the configured input resolution. After
     * this call, {@link CaptureProcessorImpl} should expect any {@link Image} received as input
     * to be at the specified resolution.
     *
     * @param size for the surface.
     */
    void onResolutionUpdate(Size size);

    /**
     * This callback will be invoked when CameraX changes the configured input image format.
     * After this call, {@link CaptureProcessorImpl} should expect any {@link Image} received as
     * input to have the specified image format.
     *
     * @param imageFormat for the surface.
     */
    void onImageFormatUpdate(int imageFormat);
}
