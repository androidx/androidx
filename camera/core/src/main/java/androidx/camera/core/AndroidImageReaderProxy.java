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

import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;

/**
 * An {@link ImageReaderProxy} which wraps around an {@link ImageReader}.
 *
 * <p>All methods map one-to-one between this {@link ImageReaderProxy} and the wrapped {@link
 * ImageReader}.
 */
final class AndroidImageReaderProxy implements ImageReaderProxy {
    @GuardedBy("this")
    private final ImageReader mImageReader;

    /**
     * Creates a new instance which wraps the given image reader.
     *
     * @param imageReader to wrap
     * @return new {@link AndroidImageReaderProxy} instance
     */
    AndroidImageReaderProxy(ImageReader imageReader) {
        mImageReader = imageReader;
    }

    @Override
    @Nullable
    public synchronized ImageProxy acquireLatestImage() {
        Image image = mImageReader.acquireLatestImage();
        if (image == null) {
            return null;
        }
        return new AndroidImageProxy(image);
    }

    @Override
    @Nullable
    public synchronized ImageProxy acquireNextImage() {
        Image image = mImageReader.acquireNextImage();
        if (image == null) {
            return null;
        }
        return new AndroidImageProxy(image);
    }

    @Override
    public synchronized void close() {
        mImageReader.close();
    }

    @Override
    public synchronized int getHeight() {
        return mImageReader.getHeight();
    }

    @Override
    public synchronized int getWidth() {
        return mImageReader.getWidth();
    }

    @Override
    public synchronized int getImageFormat() {
        return mImageReader.getImageFormat();
    }

    @Override
    public synchronized int getMaxImages() {
        return mImageReader.getMaxImages();
    }

    @Override
    public synchronized Surface getSurface() {
        return mImageReader.getSurface();
    }

    @Override
    public synchronized void setOnImageAvailableListener(
            @Nullable final ImageReaderProxy.OnImageAvailableListener listener,
            @Nullable Handler handler) {
        ImageReader.OnImageAvailableListener transformedListener =
                new ImageReader.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(ImageReader reader) {
                        listener.onImageAvailable(AndroidImageReaderProxy.this);
                    }
                };
        mImageReader.setOnImageAvailableListener(transformedListener, handler);
    }
}
