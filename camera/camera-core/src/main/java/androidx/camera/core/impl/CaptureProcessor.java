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

package androidx.camera.core.impl;

import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.impl.utils.futures.Futures;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * A processing step of the image capture pipeline.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface CaptureProcessor {
    /**
     * This gets called to update where the CaptureProcessor should write the output of {@link
     * #process(ImageProxyBundle)}.
     *
     * @param surface The {@link Surface} that the CaptureProcessor should write data into.
     * @param imageFormat The format of that the surface expects.
     */
    void onOutputSurface(@NonNull Surface surface, int imageFormat);

    /**
     * Process a {@link ImageProxyBundle} for the set of captures that were
     * requested.
     *
     * <p> A result of the processing step must be written to the {@link Surface} that was
     * received by {@link #onOutputSurface(Surface, int)}. Otherwise, it might cause the
     * {@link ImageCapture#takePicture} can't be complete or frame lost in {@link Preview}.
     * @param bundle The set of images to process. The ImageProxyBundle and the {@link ImageProxy}
     *               that are retrieved from it will become invalid after this method completes, so
     *               no references to them should be kept.
     */
    void process(@NonNull ImageProxyBundle bundle);

    /**
     * This will be invoked when the input surface resolution is updated.
     *
     * @param size for the surface.
     */
    void onResolutionUpdate(@NonNull Size size);

    /**
     * Triggers to close the capture processor.
     *
     * <p>The capture processor might stop the in-progress task or have a flag to stop handling
     * new tasks after this function is called. When the capture processor is closed completely,
     * the {@link ListenableFuture} returned by {@link #getCloseFuture()} needs to be completed.
     */
    default void close() {
        // No-op by default.
    }

    /**
     * Returns the {@link ListenableFuture} which allows to know when the capture processor has
     * been closed completely.
     */
    @NonNull
    default ListenableFuture<Void> getCloseFuture() {
        // Returns immediate future by default.
        return Futures.immediateFuture(null);
    }
}
