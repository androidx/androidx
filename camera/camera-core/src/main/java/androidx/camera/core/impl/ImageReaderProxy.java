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

import android.media.ImageReader;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.ImageProxy;

import java.util.concurrent.Executor;

/**
 * An image reader proxy which has an analogous interface as {@link ImageReader}.
 *
 * <p>Whereas an {@link ImageReader} provides {@link android.media.Image} instances, an {@link
 * ImageReaderProxy} provides {@link ImageProxy} instances.
 *
 */

@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface ImageReaderProxy {
    /**
     * Acquires the latest image in the queue.
     *
     * <p>@see {@link ImageReader#acquireLatestImage()}.
     */
    @Nullable
    ImageProxy acquireLatestImage();

    /**
     * Acquires the next image in the queue.
     *
     * <p>@see {@link ImageReader#acquireNextImage()}.
     */
    @Nullable
    ImageProxy acquireNextImage();

    /**
     * Closes the reader.
     *
     * <p>@see {@link ImageReader#close()}.
     */
    void close();

    /**
     * Returns the image height.
     *
     * <p>@see {@link ImageReader#getHeight()}.
     */
    int getHeight();

    /**
     * Returns the image width.
     *
     * <p>@see {@link ImageReader#getWidth()}.
     */
    int getWidth();

    /**
     * Returns the image format.
     *
     * <p>@see {@link ImageReader#getImageFormat()}.
     */
    int getImageFormat();

    /**
     * Returns the max number of images in the queue.
     *
     * <p>@see {@link ImageReader#getMaxImages()}.
     */
    int getMaxImages();

    /**
     * Returns the underlying surface.
     *
     * <p>@see {@link ImageReader#getSurface()}.
     */
    @Nullable
    Surface getSurface();

    /**
     * Sets the on-image-available listener.
     *
     * @param listener The listener that will be run.
     * @param executor The executor on which the listener should be invoked.
     */
    void setOnImageAvailableListener(
            @NonNull ImageReaderProxy.OnImageAvailableListener listener,
            @NonNull Executor executor);

    /**
     * Clears the currently set {@link OnImageAvailableListener}.
     *
     * <p> This does not cancel any currently in progress listener.
     */
    void clearOnImageAvailableListener();

    /**
     * A listener for newly available images.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    interface OnImageAvailableListener {
        /**
         * Callback for a newly available image.
         *
         * <p>@see {@link ImageReader.OnImageAvailableListener#onImageAvailable(ImageReader)}.
         */
        void onImageAvailable(@NonNull ImageReaderProxy imageReader);
    }
}
