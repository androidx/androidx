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

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.media.Image;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.nio.ByteBuffer;

/** An image proxy which has a similar interface as {@link android.media.Image}. */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface ImageProxy extends AutoCloseable {
    /**
     * Closes the underlying {@link android.media.Image}.
     *
     * @see android.media.Image#close()
     */
    @Override
    void close();

    /**
     * Returns the crop rectangle.
     *
     * @see android.media.Image#getCropRect()
     */
    @NonNull
    Rect getCropRect();

    /**
     * Sets the crop rectangle.
     *
     * @see android.media.Image#setCropRect(Rect)
     */
    void setCropRect(@Nullable Rect rect);

    /**
     * Returns the image format.
     *
     * <p> The image format can be one of the {@link android.graphics.ImageFormat} or
     * {@link android.graphics.PixelFormat} constants.
     *
     * @see android.media.Image#getFormat()
     */
    int getFormat();

    /**
     * Returns the image height.
     *
     * @see android.media.Image#getHeight()
     */
    int getHeight();

    /**
     * Returns the image width.
     *
     * @see android.media.Image#getWidth()
     */
    int getWidth();

    /**
     * Returns the array of planes.
     *
     * @see android.media.Image#getPlanes()
     */
    @NonNull
    @SuppressLint("ArrayReturn")
    PlaneProxy[] getPlanes();

    /** A plane proxy which has an analogous interface as {@link android.media.Image.Plane}. */
    @RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
    interface PlaneProxy {
        /**
         * Returns the row stride.
         *
         * @see android.media.Image.Plane#getRowStride()
         */
        int getRowStride();

        /**
         * Returns the pixel stride.
         *
         * @see android.media.Image.Plane#getPixelStride()
         */
        int getPixelStride();

        /**
         * Returns the pixels buffer.
         *
         * @see android.media.Image.Plane#getBuffer()
         */
        @NonNull
        ByteBuffer getBuffer();
    }

    /** Returns the {@link ImageInfo}. */
    @NonNull
    ImageInfo getImageInfo();

    /**
     * Returns the android {@link Image}.
     *
     * <p>If the ImageProxy is a wrapper for an android {@link Image}, it will return the
     * {@link Image}. It is possible for an ImageProxy to wrap something that isn't an
     * {@link Image}. If that's the case then it will return null.
     *
     * <p>The returned image should not be closed by the application. Instead it should be closed by
     * the ImageProxy, which happens, for example, on return from the {@link ImageAnalysis.Analyzer}
     * function.  Destroying the {@link ImageAnalysis} will close the underlying
     * {@link android.media.ImageReader}.  So an {@link Image} obtained with this method will behave
     * as such.
     *
     * @return the android image.
     * @see android.media.Image#close()
     */
    @Nullable
    @ExperimentalGetImage
    Image getImage();
}
