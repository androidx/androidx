/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.core.imagecapture;

import static androidx.camera.core.internal.utils.ImageUtil.DEFAULT_RGBA_PIXEL_STRIDE;
import static androidx.camera.core.internal.utils.ImageUtil.createBitmapFromPlane;
import static androidx.camera.core.internal.utils.ImageUtil.createDirectByteBuffer;
import static androidx.core.util.Preconditions.checkState;

import static java.util.Objects.requireNonNull;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.media.Image;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageInfo;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.impl.TagBundle;
import androidx.camera.core.impl.utils.ExifData;
import androidx.camera.core.processing.Packet;

import java.nio.ByteBuffer;

/**
 * A {@link ImageProxy} that is backed by a RGBA_8888 ByteBuffer.
 *
 * <p> This class is backed by a single {@link ByteBuffer}. The bytes are stored following the
 * {@link Bitmap.Config#ARGB_8888}.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class RgbaImageProxy implements ImageProxy {

    private final Object mLock = new Object();

    private final int mWidth;

    private final int mHeight;

    @NonNull
    private final Rect mCropRect;

    // Null if the ImageProxy is closed. Otherwise non-null.
    @GuardedBy("mLock")
    @Nullable
    PlaneProxy[] mPlaneProxy;

    @NonNull
    private final ImageInfo mImageInfo;

    /**
     * Constructs the object from a {@link Packet}.
     *
     * <p>The wrapped {@link Bitmap} must be {@link Bitmap.Config#ARGB_8888}.
     */
    public RgbaImageProxy(@NonNull Packet<Bitmap> packet) {
        this(packet.getData(),
                packet.getCropRect(),
                packet.getRotationDegrees(), packet.getSensorToBufferTransform(),
                packet.getCameraCaptureResult().getTimestamp());
    }

    /**
     * Constructs the object from a {@link Bitmap} and metadata.
     *
     * <p>The {@link Bitmap} must be {@link Bitmap.Config#ARGB_8888}.
     */
    public RgbaImageProxy(@NonNull Bitmap bitmap, @NonNull Rect cropRect, int rotationDegrees,
            @NonNull Matrix sensorToBuffer, long timestamp) {
        this(createDirectByteBuffer(bitmap),
                DEFAULT_RGBA_PIXEL_STRIDE,
                bitmap.getWidth(),
                bitmap.getHeight(),
                cropRect,
                rotationDegrees,
                sensorToBuffer,
                timestamp);
    }

    /**
     * Constructs the object from a {@link ByteBuffer} and metadata.
     *
     * <p>The data {@link ByteBuffer} must has a pixel stride of 4 and a row stride of width * 4.
     * Each pixel is stored in the order of R, G, B and A.For more details, see the JavaDoc of
     * {@code Bitmap.Config#ARGB_8888}.
     */
    public RgbaImageProxy(@NonNull ByteBuffer byteBuffer, int pixelStride,
            int width, int height, @NonNull Rect cropRect,
            int rotationDegrees, @NonNull Matrix sensorToBuffer, long timestamp) {
        mWidth = width;
        mHeight = height;
        mCropRect = cropRect;
        mImageInfo = createImageInfo(timestamp, rotationDegrees, sensorToBuffer);
        byteBuffer.rewind();
        mPlaneProxy = new PlaneProxy[]{
                createPlaneProxy(byteBuffer, width * pixelStride, pixelStride)
        };
    }

    @Override
    public void close() {
        synchronized (mLock) {
            checkNotClosed();
            // Nullify so it can be GCed.
            mPlaneProxy = null;
        }
    }

    @NonNull
    @Override
    public Rect getCropRect() {
        synchronized (mLock) {
            checkNotClosed();
            return mCropRect;
        }
    }

    @Override
    public void setCropRect(@Nullable Rect rect) {
        synchronized (mLock) {
            checkNotClosed();
            if (rect != null) {
                mCropRect.set(rect);
            }
        }
    }

    @Override
    public int getFormat() {
        synchronized (mLock) {
            checkNotClosed();
            return PixelFormat.RGBA_8888;
        }
    }

    @Override
    public int getHeight() {
        synchronized (mLock) {
            checkNotClosed();
            return mHeight;
        }
    }

    @Override
    public int getWidth() {
        synchronized (mLock) {
            checkNotClosed();
            return mWidth;
        }
    }

    @NonNull
    @Override
    public PlaneProxy[] getPlanes() {
        synchronized (mLock) {
            checkNotClosed();
            return requireNonNull(mPlaneProxy);
        }
    }

    @NonNull
    @Override
    public ImageInfo getImageInfo() {
        synchronized (mLock) {
            checkNotClosed();
            return mImageInfo;
        }
    }

    @Nullable
    @ExperimentalGetImage
    @Override
    public Image getImage() {
        synchronized (mLock) {
            checkNotClosed();
            return null;
        }
    }

    /**
     * Creates a {@link Bitmap} form the value of the underlying {@link ByteBuffer}.
     */
    @NonNull
    public Bitmap createBitmap() {
        synchronized (mLock) {
            checkNotClosed();
            return createBitmapFromPlane(getPlanes(), getWidth(), getHeight());
        }
    }

    private void checkNotClosed() {
        synchronized (mLock) {
            checkState(mPlaneProxy != null, "The image is closed.");
        }
    }

    private static PlaneProxy createPlaneProxy(
            @NonNull ByteBuffer byteBuffer, int rowStride, int pixelStride) {
        return new PlaneProxy() {
            @Override
            public int getRowStride() {
                return rowStride;
            }

            @Override
            public int getPixelStride() {
                return pixelStride;
            }

            @NonNull
            @Override
            public ByteBuffer getBuffer() {
                return byteBuffer;
            }
        };
    }

    private static ImageInfo createImageInfo(
            long timestamp, int rotationDegrees, @NonNull Matrix sensorToBuffer) {
        return new ImageInfo() {
            @NonNull
            @Override
            public TagBundle getTagBundle() {
                throw new UnsupportedOperationException(
                        "Custom ImageProxy does not contain TagBundle");
            }

            @Override
            public long getTimestamp() {
                return timestamp;
            }

            @Override
            public int getRotationDegrees() {
                return rotationDegrees;
            }

            @Override
            @NonNull
            public Matrix getSensorToBufferTransformMatrix() {
                return new Matrix(sensorToBuffer);
            }

            @Override
            public void populateExifData(@NonNull ExifData.Builder exifBuilder) {
                throw new UnsupportedOperationException(
                        "Custom ImageProxy does not contain Exif data.");
            }
        };
    }
}
