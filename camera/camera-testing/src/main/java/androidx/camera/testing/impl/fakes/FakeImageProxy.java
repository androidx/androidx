/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.testing.impl.fakes;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.hardware.HardwareBuffer;
import android.media.Image;
import android.os.Build;

import androidx.annotation.GuardedBy;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageInfo;
import androidx.camera.core.ImageProxy;
import androidx.camera.common.ImagePlane;
import androidx.camera.common.testing.FakeImage;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.util.Preconditions;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A fake implementation of {@link ImageProxy} where the values are settable.
 */
public final class FakeImageProxy extends FakeImage implements ImageProxy {
    private ImageProxy.PlaneProxy @NonNull [] mPlaneProxy = new ImageProxy.PlaneProxy[0];

    private boolean mClosed = false;

    private @NonNull ImageInfo mImageInfo;
    private Image mImage;
    private @Nullable HardwareBuffer mHardwareBuffer;
    private @Nullable Bitmap mBitmap;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final Object mReleaseLock = new Object();
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @GuardedBy("mReleaseLock")
    ListenableFuture<Void> mReleaseFuture;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @GuardedBy("mReleaseLock")
    CallbackToFutureAdapter.Completer<Void> mReleaseCompleter;

    public FakeImageProxy(@NonNull ImageInfo imageInfo) {
        super(0, 0, 0, imageInfo.getTimestamp(), null, null, new Rect());
        mImageInfo = imageInfo;
    }

    public FakeImageProxy(@NonNull ImageInfo imageInfo, @NonNull Bitmap bitmap) {
        super(bitmap.getWidth(), bitmap.getHeight(), 0, imageInfo.getTimestamp(), null, null,
                new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()));
        mImageInfo = imageInfo;
        mBitmap = bitmap;
    }

    @Override
    public void close() {
        synchronized (mReleaseLock) {
            mClosed = true;
            if (mReleaseCompleter != null) {
                mReleaseCompleter.set(null);
                mReleaseCompleter = null;
            }
        }
    }

    @Override
    public @NonNull Rect getCropRect() {
        synchronized (mReleaseLock) {
            if (isClosed()) {
                throw new IllegalStateException("FakeImageProxy already closed");
            }
            return super.getCropRect();
        }
    }

    @Override
    public void setCropRect(@Nullable Rect rect) {
        super.setCropRect(rect != null ? rect : new Rect());
    }

    @Override
    public int getFormat() {
        synchronized (mReleaseLock) {
            if (isClosed()) {
                throw new IllegalStateException("FakeImageProxy already closed");
            }
            return super.getFormat();
        }
    }

    @Override
    public int getHeight() {
        synchronized (mReleaseLock) {
            if (isClosed()) {
                throw new IllegalStateException("FakeImageProxy already closed");
            }
            return super.getHeight();
        }
    }

    @Override
    public int getWidth() {
        synchronized (mReleaseLock) {
            if (isClosed()) {
                throw new IllegalStateException("FakeImageProxy already closed");
            }
            return super.getWidth();
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public ImageProxy.PlaneProxy @NonNull [] getPlanes() {
        synchronized (mReleaseLock) {
            if (isClosed()) {
                throw new IllegalStateException("FakeImageProxy already closed");
            }
            return mPlaneProxy;
        }
    }

    @Override
    public @NonNull List<ImagePlane> getImagePlanes() {
        synchronized (mReleaseLock) {
            if (isClosed()) {
                throw new IllegalStateException("FakeImageProxy already closed");
            }
            return super.getImagePlanes();
        }
    }

    @Override
    public @NonNull ImageInfo getImageInfo() {
        return mImageInfo;
    }

    @Override
    @ExperimentalGetImage
    public @Nullable Image getImage() {
        return mImage;
    }

    @Override
    @RequiresApi(Build.VERSION_CODES.P)
    public @Nullable HardwareBuffer getHardwareBuffer() {
        return mHardwareBuffer;
    }

    /**
     * Checks the image close status.
     * @return true if image closed, false otherwise.
     */
    @Override
    public boolean isClosed() {
        synchronized (mReleaseLock) {
            return mClosed;
        }
    }



    @Override
    public void setFormat(int format) {
        super.setFormat(format);
    }

    @Override
    public void setHeight(int height) {
        super.setHeight(height);
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrapAs(@NonNull Class<T> type) {
        T result = super.unwrapAs(type);
        if (result != null) {
            return result;
        }
        if (Build.VERSION.SDK_INT >= 26 && type == HardwareBuffer.class) {
            return (T) mHardwareBuffer;
        }
        return null;
    }

    public void setPlanes(ImageProxy.PlaneProxy @NonNull [] planeProxy) {
        mPlaneProxy = planeProxy;
        super.setImagePlanes(Arrays.asList(planeProxy));
    }

    public void setImageInfo(@NonNull ImageInfo imageInfo) {
        mImageInfo = imageInfo;
    }

    public void setImage(@Nullable Image image) {
        mImage = image;
    }

    @RequiresApi(Build.VERSION_CODES.P)
    public void setHardwareBuffer(@Nullable HardwareBuffer hardwareBuffer) {
        mHardwareBuffer = hardwareBuffer;
    }

    /**
     * Returns ListenableFuture that completes when the {@link FakeImageProxy} has closed.
     */
    @SuppressWarnings("ObjectToString")
    public @NonNull ListenableFuture<Void> getCloseFuture() {
        synchronized (mReleaseLock) {
            if (mReleaseFuture == null) {
                mReleaseFuture = CallbackToFutureAdapter.getFuture(
                        completer -> {
                            synchronized (mReleaseLock) {
                                Preconditions.checkState(mReleaseCompleter == null,
                                        "Release completer expected to be null");
                                mReleaseCompleter = completer;
                                return "Release[imageProxy=" + FakeImageProxy.this + "]";
                            }
                        });
            }
            return mReleaseFuture;
        }
    }

    @Override
    public @NonNull Bitmap toBitmap() {
        if (mBitmap != null) {
            return mBitmap;
        }
        return ImageProxy.super.toBitmap();
    }
}
