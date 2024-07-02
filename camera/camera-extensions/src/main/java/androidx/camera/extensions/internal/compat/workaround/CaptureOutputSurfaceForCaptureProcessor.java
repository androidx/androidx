/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.extensions.internal.compat.workaround;

import android.graphics.ImageFormat;
import android.media.Image;
import android.media.ImageWriter;
import android.os.Build;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.ImageReaderProxys;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.ImageReaderProxy;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.extensions.internal.compat.quirk.CaptureOutputSurfaceOccupiedQuirk;
import androidx.camera.extensions.internal.compat.quirk.DeviceQuirks;

/**
 * A workaround for 2 purposes:
 * <p>1. Use a intermedia surface when passing the capture output surface
 * to a {@link androidx.camera.extensions.impl.CaptureProcessorImpl} if
 * {@link androidx.camera.extensions.internal.compat.quirk.CaptureOutputSurfaceOccupiedQuirk}
 * exists on the device. This is needed if the OEM doesn't close the internal {@link ImageWriter}
 * when the camera is closed and cause the output capture surface can't be connected to another
 * {@link ImageWriter}.
 *
 * <p>2. Overriding the timestamp of the output image (via an intermediate surface) to ensure it
 * matches the timestamp in {@link androidx.camera.core.impl.CameraCaptureResult}. For devices
 * implementing v1.2 or prior, it is possible that they don't write the same timestamp to the
 * output image.
 *
 * <p>If the quirk doesn't exist, it will pass along the output surface directly.
 */
@OptIn(markerClass = ExperimentalGetImage.class)
public class CaptureOutputSurfaceForCaptureProcessor {
    private static final String TAG = "CaptureOutputSurface";
    private static final int MAX_IMAGES = 2;
    private static final long UNSPECIFIED_TIMESTAMP = -1;

    private final Object mLock = new Object();
    @GuardedBy("mLock")
    private final ImageWriter mImageWriter;
    @GuardedBy("mLock")
    private final ImageReaderProxy mIntermediateImageReader;
    @GuardedBy("mLock")
    private boolean mIsClosed = false;
    @NonNull
    private final Surface mOutputSurface;
    private final boolean mNeedIntermediaSurface;
    private final boolean mNeedOverrideTimestamp;
    long mOutputImageTimeStamp = UNSPECIFIED_TIMESTAMP;


    public CaptureOutputSurfaceForCaptureProcessor(
            @NonNull Surface surface, @NonNull Size surfaceSize, boolean needOverrideTimestamp) {
        mNeedOverrideTimestamp = needOverrideTimestamp;
        mNeedIntermediaSurface = DeviceQuirks.get(CaptureOutputSurfaceOccupiedQuirk.class) != null
                || needOverrideTimestamp;
        if (Build.VERSION.SDK_INT >= 29 && mNeedIntermediaSurface) {
            Logger.d(TAG, "Enabling intermediate surface");
            mIntermediateImageReader = ImageReaderProxys.createIsolatedReader(
                    surfaceSize.getWidth(), surfaceSize.getHeight(),
                    ImageFormat.YUV_420_888, MAX_IMAGES);
            mOutputSurface = mIntermediateImageReader.getSurface();
            mImageWriter = ImageWriterCompat.newInstance(
                    surface, MAX_IMAGES, ImageFormat.YUV_420_888);
            mIntermediateImageReader.setOnImageAvailableListener(imageReader -> {
                synchronized (mLock) {
                    if (mIsClosed) {
                        return;
                    }
                    ImageProxy imageProxy = imageReader.acquireNextImage();
                    if (imageProxy != null) {
                        Image image = imageProxy.getImage();
                        if (image != null) {
                            if (mNeedOverrideTimestamp
                                    && mOutputImageTimeStamp != UNSPECIFIED_TIMESTAMP) {
                                Api23Impl.setImageTimestamp(image, mOutputImageTimeStamp);
                            }
                            ImageWriterCompat.queueInputImage(mImageWriter, image);
                        }
                    }
                }
            }, CameraXExecutors.directExecutor());
        } else {
            mOutputSurface = surface;
            mIntermediateImageReader = null;
            mImageWriter = null;
        }
    }

    /**
     * Set the timestamp on the output image.
     */
    public void setOutputImageTimestamp(long timestamp) {
        if (mNeedOverrideTimestamp) {
            mOutputImageTimeStamp = timestamp;
        }
    }

    /**
     * Returns the output surface that is supported to pass to
     * {@link androidx.camera.extensions.impl.CaptureProcessorImpl#onOutputSurface(Surface, int)}.
     */
    @NonNull
    public Surface getSurface() {
        return mOutputSurface;
    }

    /**
     * Close the resources.
     */
    public void close() {
        synchronized (mLock) {
            mIsClosed = true;
            if (Build.VERSION.SDK_INT >= 29 && mNeedIntermediaSurface) {
                mIntermediateImageReader.clearOnImageAvailableListener();
                mIntermediateImageReader.close();
                ImageWriterCompat.close(mImageWriter);
            }
        }
    }

    @RequiresApi(29)
    static final class ImageWriterCompat {
        private ImageWriterCompat() {
        }

        /**
         * Creates a {@link ImageWriter} instance.
         */
        @NonNull
        static ImageWriter newInstance(@NonNull Surface surface, int maxImages, int imageFormat) {
            return ImageWriter.newInstance(surface, maxImages, imageFormat);
        }

        static void queueInputImage(@NonNull ImageWriter imageWriter, @NonNull Image image) {
            imageWriter.queueInputImage(image);
        }

        static void close(ImageWriter imageWriter) {
            imageWriter.close();
        }
    }

    @RequiresApi(23)
    static final class Api23Impl {
        private Api23Impl() {}

        static void setImageTimestamp(@NonNull Image image, long timestamp) {
            image.setTimestamp(timestamp);
        }
    }
}
