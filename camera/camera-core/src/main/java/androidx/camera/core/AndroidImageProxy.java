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
import android.os.Build;

import androidx.annotation.GuardedBy;

import java.nio.ByteBuffer;

/** An {@link ImageProxy} which wraps around an {@link Image}. */
final class AndroidImageProxy implements ImageProxy {
    /**
     * Image.setTimestamp(long) was added in M. On lower API levels, we use our own timestamp field
     * to provide a more consistent behavior across more devices.
     */
    private static final boolean SET_TIMESTAMP_AVAILABLE_IN_FRAMEWORK =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;

    @GuardedBy("this")
    private final Image mImage;

    @GuardedBy("this")
    private final PlaneProxy[] mPlanes;

    @GuardedBy("this")
    private long mTimestamp;

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

        mTimestamp = image.getTimestamp();
    }

    @Override
    public synchronized void close() {
        mImage.close();
    }

    @Override
    public synchronized Rect getCropRect() {
        return mImage.getCropRect();
    }

    @Override
    public synchronized void setCropRect(Rect rect) {
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
    public synchronized long getTimestamp() {
        if (SET_TIMESTAMP_AVAILABLE_IN_FRAMEWORK) {
            return mImage.getTimestamp();
        } else {
            return mTimestamp;
        }
    }

    @Override
    public synchronized void setTimestamp(long timestamp) {
        if (SET_TIMESTAMP_AVAILABLE_IN_FRAMEWORK) {
            mImage.setTimestamp(timestamp);
        } else {
            mTimestamp = timestamp;
        }
    }

    @Override
    public synchronized ImageProxy.PlaneProxy[] getPlanes() {
        return mPlanes;
    }

    /** An {@link ImageProxy.PlaneProxy} which wraps around an {@link Image.Plane}. */
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
        public synchronized ByteBuffer getBuffer() {
            return mPlane.getBuffer();
        }
    }

    /**
     * The {@link Image} that comes from the framework does not contain any additional metadata, so
     * will always return null.
     */
    @Override
    public ImageInfo getImageInfo() {
        return null;
    }

    @Override
    public synchronized Image getImage() {
        return mImage;
    }
}
