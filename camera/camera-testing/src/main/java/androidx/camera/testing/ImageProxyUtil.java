/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.testing;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ImageProxy;

import java.nio.ByteBuffer;

/**
 * Utility class to generate image planes and buffer data in image proxy for unit test.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class ImageProxyUtil {

    private ImageProxyUtil() {
    }

    /**
     * Creates YUV_420_888 image planes.
     *
     * @param width image width.
     * @param height image height.
     * @param flipUV true if v data is before u data in memory, false otherwise.
     * @param incrementValue true if the data value will increment by position, e.g. 1, 2, 3, etc,.
     * @return image planes in image proxy.
     */
    @NonNull
    public static ImageProxy.PlaneProxy[] createYUV420ImagePlanes(
            final int width,
            final int height,
            final int pixelStrideY,
            final int pixelStrideUV,
            final boolean flipUV,
            final boolean incrementValue) {
        ImageProxy.PlaneProxy[] planes = new ImageProxy.PlaneProxy[3];

        planes[0] =
                createPlane(width, height, pixelStrideY, /*dataValue=*/ 1, incrementValue);

        if (flipUV) {
            planes[2] =
                    createPlane(
                            width / 2, height / 2, pixelStrideUV, /*dataValue=*/ 1, incrementValue);
            planes[1] =
                    createPlane(
                            width / 2, height / 2, pixelStrideUV, /*dataValue=*/ 1, incrementValue);
        } else {
            planes[1] =
                    createPlane(
                            width / 2, height / 2, pixelStrideUV, /*dataValue=*/ 1, incrementValue);
            planes[2] =
                    createPlane(
                            width / 2, height / 2, pixelStrideUV, /*dataValue=*/ 1, incrementValue);
        }
        return planes;
    }

    @NonNull
    private static ImageProxy.PlaneProxy createPlane(
            final int width,
            final int height,
            final int pixelStride,
            final int dataValue,
            final boolean incrementValue) {
        return new ImageProxy.PlaneProxy() {
            @SuppressLint("SyntheticAccessor")
            final ByteBuffer mBuffer =
                    createBuffer(width, height, pixelStride, dataValue, incrementValue);

            @Override
            public int getRowStride() {
                return width * pixelStride;
            }

            @Override
            public int getPixelStride() {
                return pixelStride;
            }

            @Override
            @NonNull
            public ByteBuffer getBuffer() {
                return mBuffer;
            }
        };
    }

    @NonNull
    private static ByteBuffer createBuffer(
            final int width,
            final int height,
            final int pixelStride,
            final int dataValue,
            final boolean incrementValue) {
        int rowStride = width * pixelStride;
        ByteBuffer buffer = ByteBuffer.allocateDirect(rowStride * height);
        int value = dataValue;
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                buffer.position(y * rowStride + x * pixelStride);
                buffer.put((byte) (value & 0xFF));
                if (incrementValue) {
                    value++;
                }
            }
        }
        return buffer;
    }
}
