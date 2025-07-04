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

import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.Image;

import androidx.camera.core.impl.TagBundle;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;

/** An {@link ImageProxy} which wraps around an {@link Image}. */
final class AndroidImageProxy implements ImageProxy {
    private final Image mImage;

    private final PlaneProxy[] mPlanes;

    private final ImageInfo mImageInfo;

    /**
     * Creates a new instance which wraps the given image.
     *
     * @param image to wrap
     */
    AndroidImageProxy(@NonNull Image image) {
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

        mImageInfo = ImmutableImageInfo.create(
                TagBundle.emptyBundle(),
                image.getTimestamp(),
                0,
                new Matrix(),
                FlashState.UNKNOWN);
    }

    @Override
    public void close() {
        mImage.close();
    }

    @Override
    public @NonNull Rect getCropRect() {
        return mImage.getCropRect();
    }

    @Override
    public void setCropRect(@Nullable Rect rect) {
        mImage.setCropRect(rect);
    }

    @Override
    public int getFormat() {
        return mImage.getFormat();
    }

    @Override
    public int getHeight() {
        return mImage.getHeight();
    }

    @Override
    public int getWidth() {
        return mImage.getWidth();
    }

    @Override
    public ImageProxy.PlaneProxy @NonNull [] getPlanes() {
        return mPlanes;
    }

    /** An {@link ImageProxy.PlaneProxy} which wraps around an {@link Image.Plane}. */
    private static final class PlaneProxy implements ImageProxy.PlaneProxy {
        private final Image.Plane mPlane;

        PlaneProxy(Image.Plane plane) {
            mPlane = plane;
        }

        @Override
        public int getRowStride() {
            return mPlane.getRowStride();
        }

        @Override
        public int getPixelStride() {
            return mPlane.getPixelStride();
        }

        @Override
        public @NonNull ByteBuffer getBuffer() {
            return mPlane.getBuffer();
        }
    }

    @Override
    public @NonNull ImageInfo getImageInfo() {
        return mImageInfo;
    }

    @Override
    @ExperimentalGetImage
    public Image getImage() {
        return mImage;
    }
}
