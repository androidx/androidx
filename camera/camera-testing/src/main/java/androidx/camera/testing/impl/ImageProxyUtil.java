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

package androidx.camera.testing.impl;

import static android.graphics.ImageFormat.YUV_420_888;

import android.media.Image;
import android.media.ImageReader;
import android.media.ImageWriter;
import android.os.Build;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;
import androidx.camera.core.ImageProcessingUtil;
import androidx.camera.core.ImageProxy;

import org.jspecify.annotations.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;

/**
 * Utility class to generate image planes and buffer data in image proxy for unit test.
 */
public final class ImageProxyUtil {
    /**
     * I420 plane data type.
     */
    public static final int YUV_FORMAT_PLANE_DATA_TYPE_I420 = 0;
    /**
     * NV12 plane data type.
     */
    public static final int YUV_FORMAT_PLANE_DATA_TYPE_NV12 = 1;
    /**
     * NV21 plane data type.
     */
    public static final int YUV_FORMAT_PLANE_DATA_TYPE_NV21 = 2;

    /**
     * YUV format plane data type
     */
    @IntDef({YUV_FORMAT_PLANE_DATA_TYPE_I420, YUV_FORMAT_PLANE_DATA_TYPE_NV12,
            YUV_FORMAT_PLANE_DATA_TYPE_NV21})
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @interface YuvFormatPlaneDataType {
    }

    private ImageProxyUtil() {
    }

    /**
     * Creates YUV_420_888 image planes.
     *
     * @param width image width.
     * @param height image height.
     * @param yuvFormatPlaneDataType the plane data type for the created image planes
     * @param incrementValue true if the data value will increment by position, e.g. 1, 2, 3, etc,.
     * @return image planes in image proxy.
     */
    public static ImageProxy.PlaneProxy @NonNull [] createYUV420ImagePlanes(
            final int width,
            final int height,
            final @YuvFormatPlaneDataType int yuvFormatPlaneDataType,
            final boolean incrementValue) {
        return createYUV420ImagePlanes(width, height, 1,
                yuvFormatPlaneDataType == YUV_FORMAT_PLANE_DATA_TYPE_I420 ? 1 : 2,
                yuvFormatPlaneDataType == YUV_FORMAT_PLANE_DATA_TYPE_NV21, incrementValue);
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
    public static ImageProxy.PlaneProxy @NonNull [] createYUV420ImagePlanes(
            final int width,
            final int height,
            final int pixelStrideY,
            final int pixelStrideUV,
            final boolean flipUV,
            final boolean incrementValue) {
        ImageProxy.PlaneProxy[] planes = new ImageProxy.PlaneProxy[3];

        planes[0] = createPlane(width, height, pixelStrideY, /*dataValue=*/ 1, incrementValue);

        if (pixelStrideUV == 1) {
            // I420 memory layout. U and V plane data is not interleaved. Directly create two
            // planes with separate ByteBuffers.
            planes[1] = createPlane(width / 2, height / 2, pixelStrideUV, /*dataValue=*/ 1,
                    incrementValue);
            planes[2] = createPlane(width / 2, height / 2, pixelStrideUV, /*dataValue=*/ 1,
                    incrementValue);
        } else {
            // U and V plane data is interleaved.
            // Plane data starts from position 0 of a ByteBuffer.
            ImageProxy.PlaneProxy pos0StartedByteBufferPlane = createPlane(width / 2, height / 2,
                    pixelStrideUV, /*dataValue=*/ 1, incrementValue);
            // Plane data starts from position 1 of the same ByteBuffer.
            ImageProxy.PlaneProxy pos1StartedByteBufferPlane = createPlane(
                    ImageProcessingUtil.nativeNewDirectByteBuffer(
                            pos0StartedByteBufferPlane.getBuffer(),
                            1,
                            pos0StartedByteBufferPlane.getBuffer().capacity() - 1),
                    pos0StartedByteBufferPlane.getRowStride(),
                    pos0StartedByteBufferPlane.getPixelStride(),
                    /*dataValue=*/ 1,
                    incrementValue
            );

            planes[1] = flipUV ? pos1StartedByteBufferPlane : pos0StartedByteBufferPlane;
            planes[2] = flipUV ? pos0StartedByteBufferPlane : pos1StartedByteBufferPlane;
        }

        return planes;
    }

    /**
     * Determines the device's default YUV format plane data type by checking the type of the image
     * acquired from a YUV_420_888 image reader. This can make the tests more close to the real
     * processing situation on the device.
     */
    public static @ImageProxyUtil.YuvFormatPlaneDataType int getDefaultYuvFormatPlaneDataType(
            int width, int height) {
        if (Build.VERSION.SDK_INT < 23 || "robolectric".equals(Build.FINGERPRINT)) {
            return YUV_FORMAT_PLANE_DATA_TYPE_I420;
        }

        try (ImageReader imageReader = ImageReader.newInstance(width, height, YUV_420_888, 2);
                ImageWriter imageWriter = ImageWriter.newInstance(imageReader.getSurface(), 2);
                Image image = imageWriter.dequeueInputImage()) {

            if (image.getPlanes().length != 3) {
                return YUV_FORMAT_PLANE_DATA_TYPE_I420;
            }

            Image.Plane planeY = image.getPlanes()[0];
            Image.Plane planeU = image.getPlanes()[1];
            Image.Plane planeV = image.getPlanes()[2];

            if (planeY.getPixelStride() == 1) {
                // For all non-NV12 or non-NV21 cases, use I420 plane data type to run the test.
                if (planeU.getPixelStride() != 2 || planeV.getPixelStride() != 2) {
                    return YUV_FORMAT_PLANE_DATA_TYPE_I420;
                }

                int vuOff = ImageProcessingUtil.nativeGetYUVImageVUOff(planeV.getBuffer(),
                        planeU.getBuffer());
                switch (vuOff) {
                    case 1:
                        return YUV_FORMAT_PLANE_DATA_TYPE_NV12;
                    case -1:
                        return YUV_FORMAT_PLANE_DATA_TYPE_NV21;
                    default:
                        return YUV_FORMAT_PLANE_DATA_TYPE_I420;
                }
            }
        }

        return YUV_FORMAT_PLANE_DATA_TYPE_I420;
    }

    /**
     * Creates {@link android.graphics.ImageFormat.RAW_SENSOR} image planes.
     *
     * @param width image width.
     * @param height image height.
     * @param incrementValue true if the data value will increment by position, e.g. 1, 2, 3, etc,.
     * @return image planes in image proxy.
     */
    public static ImageProxy.PlaneProxy @NonNull [] createRawImagePlanes(
            final int width,
            final int height,
            final int pixelStride,
            final boolean incrementValue) {
        ImageProxy.PlaneProxy[] planes = new ImageProxy.PlaneProxy[1];

        planes[0] =
                createPlane(width, height, pixelStride, /*dataValue=*/ 1, incrementValue);
        return planes;
    }

    private static ImageProxy.@NonNull PlaneProxy createPlane(
            final int width,
            final int height,
            final int pixelStride,
            final int dataValue,
            final boolean incrementValue) {
        final ByteBuffer byteBuffer =
                createBuffer(width, height, pixelStride, dataValue, incrementValue);
        return createPlane(byteBuffer, width * pixelStride, pixelStride, dataValue, incrementValue);
    }

    private static ImageProxy.@NonNull PlaneProxy createPlane(
            final ByteBuffer byteBuffer,
            final int rowStride,
            final int pixelStride,
            final int dataValue,
            final boolean incrementValue) {
        return new ImageProxy.PlaneProxy() {
            final ByteBuffer mBuffer = byteBuffer;

            {
                int value = dataValue;
                for (int pos = 0; pos < mBuffer.capacity();) {
                    mBuffer.put(pos, (byte) value);
                    pos += pixelStride;
                    if (incrementValue) {
                        value++;
                    }
                }
            }

            @Override
            public int getRowStride() {
                return rowStride;
            }

            @Override
            public int getPixelStride() {
                return pixelStride;
            }

            @Override
            public @NonNull ByteBuffer getBuffer() {
                return mBuffer;
            }
        };
    }

    private static @NonNull ByteBuffer createBuffer(
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
