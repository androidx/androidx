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

import java.io.IOException;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.HardwareBuffer;
import android.hardware.SyncFence;
import android.media.Image;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.camera.common.AndroidImage;
import androidx.camera.common.ImagePlane;
import androidx.camera.core.impl.TagBundle;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** An {@link ImageProxy} which wraps around an {@link Image}. */
final class AndroidImageProxy implements ImageProxy {
    private final AndroidImage mAndroidImage;

    private final PlaneProxy[] mPlanes;

    private final ImageInfo mImageInfo;

    /**
     * Creates a new instance which wraps the given image.
     *
     * @param image to wrap
     */
    AndroidImageProxy(@NonNull Image image) {
        mAndroidImage = new AndroidImage(image);

        List<ImagePlane> planes = mAndroidImage.getImagePlanes();
        mPlanes = new PlaneProxy[planes.size()];
        for (int i = 0; i < planes.size(); i++) {
            mPlanes[i] = new PlaneProxy(planes.get(i));
        }

        mImageInfo = ImmutableImageInfo.create(
                TagBundle.emptyBundle(),
                mAndroidImage.getTimestamp(),
                0,
                new Matrix(),
                FlashState.UNKNOWN);
    }

    @Override
    public void close() {
        mAndroidImage.close();
    }

    @Override
    public @NonNull Rect getCropRect() {
        return mAndroidImage.getCropRect();
    }

    @Override
    public void setCropRect(@Nullable Rect rect) {
        mAndroidImage.setCropRect(rect);
    }

    @Override
    public int getFormat() {
        return mAndroidImage.getFormat();
    }

    @Override
    public int getHeight() {
        return mAndroidImage.getHeight();
    }

    @Override
    public int getWidth() {
        return mAndroidImage.getWidth();
    }

    @Override
    public ImageProxy.PlaneProxy @NonNull [] getPlanes() {
        return mPlanes;
    }

    @Override
    public @NonNull List<ImagePlane> getImagePlanes() {
        return mAndroidImage.getImagePlanes();
    }



    @Override
    public @Nullable SyncFence getSyncFence() {
        return mAndroidImage.getSyncFence();
    }

    /** An {@link ImageProxy.PlaneProxy} which wraps around an {@link ImagePlane}. */
    private static final class PlaneProxy implements ImageProxy.PlaneProxy {
        private final ImagePlane mPlane;

        PlaneProxy(ImagePlane plane) {
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
            return java.util.Objects.requireNonNull(mPlane.getBuffer());
        }

        @Override
        public <T> T unwrapAs(@NonNull Class<T> type) {
            return mPlane.unwrapAs(type);
        }
    }

    @Override
    public @NonNull ImageInfo getImageInfo() {
        return mImageInfo;
    }

    @Override
    @ExperimentalGetImage
    public Image getImage() {
        return mAndroidImage.unwrapAs(Image.class);
    }

    @Override
    @RequiresApi(Build.VERSION_CODES.P)
    public @Nullable HardwareBuffer getHardwareBuffer() {
        return mAndroidImage.getHardwareBuffer();
    }

    @Override
    public <T> T unwrapAs(@NonNull Class<T> type) {
        return mAndroidImage.unwrapAs(type);
    }
}
