/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.media.ImageReader;
import android.os.Handler;
import android.view.Surface;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

/**
 * An image reader proxy which has an analogous interface as {@link ImageReader}.
 *
 * <p>Whereas an {@link ImageReader} provides {@link android.media.Image} instances, an {@link
 * ImageReaderProxy} provides {@link ImageProxy} instances.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY_GROUP)
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
    Surface getSurface();

    /**
     * Sets the on-image-available listener.
     *
     * <p>@see {@link ImageReader#setOnImageAvailableListener}.
     */
    void setOnImageAvailableListener(
            @Nullable ImageReaderProxy.OnImageAvailableListener listener,
            @Nullable Handler handler);

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
        void onImageAvailable(ImageReaderProxy imageReader);
    }
}
