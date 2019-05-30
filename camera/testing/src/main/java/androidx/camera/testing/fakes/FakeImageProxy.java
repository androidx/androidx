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

package androidx.camera.testing.fakes;

import android.graphics.Rect;
import android.media.Image;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.camera.core.ImageInfo;
import androidx.camera.core.ImageProxy;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * A fake implementation of {@link ImageProxy} where the values are settable.
 */
public final class FakeImageProxy implements ImageProxy {
    private Rect mCropRect = new Rect();
    private int mFormat = 0;
    private int mHeight = 0;
    private int mWidth = 0;
    private Long mTimestamp = -1L;
    private PlaneProxy[] mPlaneProxy = new PlaneProxy[0];
    private ImageInfo mImageInfo;
    private Image mImage;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final Object mReleaseLock = new Object();
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @GuardedBy("mReleaseLock")
    ListenableFuture<Void> mReleaseFuture;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @GuardedBy("mReleaseLock")
    CallbackToFutureAdapter.Completer<Void> mReleaseCompleter;

    @Override
    public void close() {
        synchronized (mReleaseLock) {
            if (mReleaseCompleter != null) {
                mReleaseCompleter.set(null);
                mReleaseCompleter = null;
            }
        }
    }

    @Override
    public Rect getCropRect() {
        return mCropRect;
    }

    @Override
    public void setCropRect(Rect rect) {
        mCropRect = rect;
    }

    @Override
    public int getFormat() {
        return mFormat;
    }

    @Override
    public int getHeight() {
        return mHeight;
    }

    @Override
    public int getWidth() {
        return mWidth;
    }

    @Override
    public long getTimestamp() {
        return mTimestamp;
    }

    @Override
    public void setTimestamp(long timestamp) {
        mTimestamp = timestamp;
    }

    @Override
    public PlaneProxy[] getPlanes() {
        return mPlaneProxy;
    }

    @Override
    public ImageInfo getImageInfo() {
        return mImageInfo;
    }

    @Override
    public Image getImage() {
        return mImage;
    }

    public void setFormat(int format) {
        mFormat = format;
    }

    public void setHeight(int height) {
        mHeight = height;
    }

    public void setWidth(int width) {
        mWidth = width;
    }

    public void setPlanes(PlaneProxy[] planeProxy) {
        mPlaneProxy = planeProxy;
    }

    public void setImageInfo(ImageInfo imageInfo) {
        mImageInfo = imageInfo;
    }

    /**
     * Returns ListenableFuture that completes when the {@link FakeImageProxy} has closed.
     */
    @NonNull
    public ListenableFuture<Void> getCloseFuture() {
        synchronized (mReleaseLock) {
            if (mReleaseFuture == null) {
                mReleaseFuture = CallbackToFutureAdapter.getFuture(
                        new CallbackToFutureAdapter.Resolver<Void>() {
                            @Override
                            public Object attachCompleter(@NonNull
                                    CallbackToFutureAdapter.Completer<Void> completer) {
                                Preconditions.checkState(Thread.holdsLock(mReleaseLock));
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
}
