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

import android.graphics.ImageFormat;
import android.os.Handler;
import android.view.Surface;

import androidx.annotation.Nullable;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.ImageReaderProxy;

/**
 * A fake implementation of ImageReaderProxy where the values are settable and the
 * OnImageAvailableListener can be triggered.
 */
public class FakeImageReaderProxy implements ImageReaderProxy {
    private int mWidth = 100;
    private int mHeight = 100;
    private int mImageFormat = ImageFormat.JPEG;
    private int mMaxImages = 8;
    private Surface mSurface;
    private Handler mHandler;
    private ImageProxy mImageProxy;

    ImageReaderProxy.OnImageAvailableListener mListener;

    @Override
    public ImageProxy acquireLatestImage() {
        return mImageProxy;
    }

    @Override
    public ImageProxy acquireNextImage() {
        return mImageProxy;
    }

    @Override
    public void close() {

    }

    @Override
    public int getWidth() {
        return mWidth;
    }

    @Override
    public int getHeight() {
        return mHeight;
    }

    @Override
    public int getImageFormat() {
        return mImageFormat;
    }

    @Override
    public int getMaxImages() {
        return mMaxImages;
    }

    @Override
    @Nullable
    public Surface getSurface() {
        return mSurface;
    }

    @Override
    public void setOnImageAvailableListener(
            @Nullable final ImageReaderProxy.OnImageAvailableListener listener,
            @Nullable Handler handler) {
        mListener = listener;
        mHandler = handler;
    }

    public void setMaxImages(int maxImages) {
        mMaxImages = maxImages;
    }

    public void setImageProxy(ImageProxy imageProxy) {
        mImageProxy = imageProxy;
    }

    public void setSurface(Surface surface) {
        mSurface = surface;
    }

    /**
     * Manually trigger OnImageAvailableListener to notify the Image is ready.
     */
    public void triggerImageAvailable() {
        if (mListener != null) {
            if (mHandler != null) {
                mHandler.post(
                        new Runnable() {
                            @Override
                            public void run() {
                                mListener.onImageAvailable(FakeImageReaderProxy.this);
                            }
                        });
            } else {
                mListener.onImageAvailable(FakeImageReaderProxy.this);
            }
        }
    }
}
