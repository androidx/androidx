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

import static androidx.camera.core.internal.utils.ImageUtil.createBitmapFromImageProxy;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.media.Image;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;

/** An image proxy which has a similar interface as {@link android.media.Image}. */
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
    @SuppressWarnings("GetterSetterNullability")
    @NonNull Rect getCropRect();

    /**
     * Sets the crop rectangle.
     *
     * @see android.media.Image#setCropRect(Rect)
     */
    void setCropRect(@Nullable Rect rect);

    /**
     * Returns the image format.
     *
     * <p> The image format can be one of the {@link ImageFormat} or
     * {@link PixelFormat} constants.
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
    @SuppressLint("ArrayReturn")
    PlaneProxy @NonNull [] getPlanes();

    /** A plane proxy which has an analogous interface as {@link android.media.Image.Plane}. */
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
        @NonNull ByteBuffer getBuffer();
    }

    /** Returns the {@link ImageInfo}. */
    @NonNull ImageInfo getImageInfo();

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
    @ExperimentalGetImage
    @Nullable Image getImage();

    /**
     * Converts {@link ImageProxy} to {@link Bitmap}.
     *
     * <p>The supported {@link ImageProxy} format is {@link ImageFormat#YUV_420_888},
     * {@link ImageFormat#JPEG} or {@link PixelFormat#RGBA_8888}. If format is invalid, an
     * {@link IllegalArgumentException} will be thrown. If the conversion to bimap failed, an
     * {@link UnsupportedOperationException} will be thrown.
     *
     * @return {@link Bitmap} instance.
     */
    default @NonNull Bitmap toBitmap() {
        return createBitmapFromImageProxy(this);
    }
}
