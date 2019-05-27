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

import androidx.camera.core.ImageInfo;
import androidx.camera.core.ImageProxy;

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

    @Override
    public void close() {

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
}
