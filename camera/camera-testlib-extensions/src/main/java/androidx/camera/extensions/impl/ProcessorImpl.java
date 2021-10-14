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

import android.util.Size;
import android.view.Surface;

import androidx.annotation.RequiresApi;

/**
 * Processes an input image stream and produces an output image stream.
 *
 * @since 1.0
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface ProcessorImpl {
    /**
     * Updates where the ProcessorImpl should write the output to.
     *
     * @param surface     The {@link Surface} that the ProcessorImpl should write data into.
     * @param imageFormat The format of that the surface expects.
     */
    void onOutputSurface(Surface surface, int imageFormat);

    /**
     * Invoked when CameraX changes the configured output resolution.
     *
     * <p>After this call, {@link CaptureProcessorImpl} should expect any {@link Image} received as
     * input to be at the specified resolution.
     *
     * @param size for the surface.
     */
    void onResolutionUpdate(Size size);

    /**
     * Invoked when CameraX changes the configured input image format.
     *
     * <p>After this call, {@link CaptureProcessorImpl} should expect any {@link Image} received as
     * input to have the specified image format.
     *
     * @param imageFormat for the surface.
     */
    void onImageFormatUpdate(int imageFormat);
}
