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

import androidx.annotation.RequiresApi;

/**
 * Processing a single {@link Image} and {@link TotalCaptureResult} to produce an output to a
 * stream.
 *
 * @since 1.0
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface PreviewImageProcessorImpl extends ProcessorImpl {
    /**
     * Processes the requested image capture.
     *
     * <p> The result of the processing step should be written to the {@link android.view.Surface}
     * that was received by {@link ProcessorImpl#onOutputSurface(android.view.Surface, int)}.
     *
     * @param image  The {@link ImageFormat#YUV_420_888} format image to process. This will be
     *               invalid after the method completes so no reference to it should be kept.
     * @param result The metadata associated with the image to process.
     */
    void process(Image image, TotalCaptureResult result);
}

