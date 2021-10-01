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

import android.graphics.Rect;
import android.media.Image;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.TagBundle;

import java.nio.ByteBuffer;

/** An {@link ImageProxy} which wraps around an {@link Image}. */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
final class AndroidImageProxy implements ImageProxy {
    @GuardedBy("this")
    private final Image mImage;

    @GuardedBy("this")
    private final PlaneProxy[] mPlanes;

    private final ImageInfo mImageInfo;

    /**
     * Creates a new instance which wraps the given image.
     *
     * @param image to wrap
     * @return new {@link AndroidImageProxy} instance
     */
    AndroidImageProxy(Image image) {
        mImage = image;

        Image.Plane[] originalPlanes = image.getPlanes();
        if (originalPlanes != null) {
            mPlanes = new PlaneProxy[originalPlanes.length];
            for (int i = 0; i < originalPlanes.length; ++i) {
                mPlanes[i] = new PlaneProxy(originalPlanes[i]);
            }
        } else {
            mPlanes = new PlaneProxy[0];
        }

        mImageInfo = ImmutableImageInfo.create(TagBundle.emptyBundle(), image.getTimestamp(), 0);
    }

    @Override
    public synchronized void close() {
        mImage.close();
    }

    @Override
    @NonNull
    public synchronized Rect getCropRect() {
        return mImage.getCropRect();
    }

    @Override
    public synchronized void setCropRect(@Nullable Rect rect) {
        mImage.setCropRect(rect);
    }

    @Override
    public synchronized int getFormat() {
        return mImage.getFormat();
    }

    @Override
    public synchronized int getHeight() {
        return mImage.getHeight();
    }

    @Override
    public synchronized int getWidth() {
        return mImage.getWidth();
    }

    @Override
    @NonNull
    public synchronized ImageProxy.PlaneProxy[] getPlanes() {
        return mPlanes;
    }

    /** An {@link ImageProxy.PlaneProxy} which wraps around an {@link Image.Plane}. */
    @RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
    private static final class PlaneProxy implements ImageProxy.PlaneProxy {
        @GuardedBy("this")
        private final Image.Plane mPlane;

        PlaneProxy(Image.Plane plane) {
            mPlane = plane;
        }

        @Override
        public synchronized int getRowStride() {
            return mPlane.getRowStride();
        }

        @Override
        public synchronized int getPixelStride() {
            return mPlane.getPixelStride();
        }

        @Override
        @NonNull
        public synchronized ByteBuffer getBuffer() {
            return mPlane.getBuffer();
        }
    }

    @Override
    @NonNull
    public ImageInfo getImageInfo() {
        return mImageInfo;
    }

    @Override
    @ExperimentalGetImage
    public synchronized Image getImage() {
        return mImage;
    }
}
