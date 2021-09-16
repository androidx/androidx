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

package androidx.camera.core;

import android.graphics.ImageFormat;
import android.media.Image;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.impl.ImageReaderProxy;

import java.nio.ByteBuffer;

/** Utility class to convert an {@link Image} from YUV to RGB. */
final class ImageProcessingUtil {

    private static final String TAG = "ImageProcessingUtil";

    static {
        System.loadLibrary("image_processing_util_jni");
    }

    enum Result {
        UNKNOWN,
        SUCCESS,
        ERROR_FORMAT, // YUV format error.
        ERROR_CONVERSION,  // Native conversion error.
    }

    private ImageProcessingUtil() {
    }

    /**
     * Converts image proxy in YUV to RGB.
     *
     * Currently this config supports the devices which generated NV21, NV12, I420 YUV layout,
     * otherwise the input YUV layout will be converted to NV12 first and then to RGBA_8888 as a
     * fallback.
     *
     * @param imageProxy input image proxy in YUV.
     * @param rgbImageReaderProxy output image reader proxy in RGB.
     * @param onePixelShiftEnabled true if one pixel shift should be applied, otherwise false.
     * @return output image proxy in RGB.
     */
    @Nullable
    public static ImageProxy convertYUVToRGB(
            @NonNull ImageProxy imageProxy,
            @NonNull ImageReaderProxy rgbImageReaderProxy,
            boolean onePixelShiftEnabled) {
        if (!ImageProcessingUtil.isSupportedYUVFormat(imageProxy)) {
            Logger.e(TAG, "Unsupported format for YUV to RGB");
            return null;
        }

        // Convert YUV To RGB and write data to surface
        ImageProcessingUtil.Result result = convertYUVToRGBInternal(
                imageProxy, rgbImageReaderProxy.getSurface(), onePixelShiftEnabled);

        if (result == Result.ERROR_CONVERSION) {
            Logger.e(TAG, "YUV to RGB conversion failure");
            return null;
        }

        if (result == Result.ERROR_FORMAT) {
            Logger.e(TAG, "Unsupported format for YUV to RGB");
            return null;
        }

        // Retrieve ImageProxy in RGB
        final ImageProxy rgbImageProxy = rgbImageReaderProxy.acquireLatestImage();
        if (rgbImageProxy == null) {
            return null;
        }

        // Close ImageProxy for the next image
        SingleCloseImageProxy wrappedRgbImageProxy = new SingleCloseImageProxy(rgbImageProxy);
        wrappedRgbImageProxy.addOnImageCloseListener(image -> {
            // Close YUV image proxy when RGB image proxy is closed by app.
            if (rgbImageProxy != null && imageProxy != null) {
                imageProxy.close();
            }
        });
        return wrappedRgbImageProxy;
    }

    /**
     * Applies one pixel shift workaround for YUV image
     *
     * @param imageProxy input image proxy in YUV.
     * @return true if one pixel shift is applied successfully, otherwise false.
     */
    public static boolean applyPixelShiftForYUV(@NonNull ImageProxy imageProxy) {
        if (!ImageProcessingUtil.isSupportedYUVFormat(imageProxy)) {
            Logger.e(TAG, "Unsupported format for YUV to RGB");
            return false;
        }

        ImageProcessingUtil.Result result = applyPixelShiftInternal(imageProxy);

        if (result == Result.ERROR_CONVERSION) {
            Logger.e(TAG, "YUV to RGB conversion failure");
            return false;
        }

        if (result == Result.ERROR_FORMAT) {
            Logger.e(TAG, "Unsupported format for YUV to RGB");
            return false;
        }
        return true;
    }

    /**
     * Checks whether input image is in supported YUV format.
     *
     * @param imageProxy input image proxy in YUV.
     * @return true if in supported YUV format, false otherwise.
     */
    private static boolean isSupportedYUVFormat(@NonNull ImageProxy imageProxy) {
        return imageProxy.getFormat() == ImageFormat.YUV_420_888
                && imageProxy.getPlanes().length == 3;
    }

    /**
     * Converts image from YUV to RGB.
     *
     * @param imageProxy input image proxy in YUV.
     * @param surface output surface for RGB data.
     * @param onePixelShiftEnabled true if one pixel shift should be applied, otherwise false.
     * @return {@link Result}.
     */
    @NonNull
    private static Result convertYUVToRGBInternal(
            @NonNull ImageProxy imageProxy,
            @NonNull Surface surface,
            boolean onePixelShiftEnabled) {
        int imageWidth = imageProxy.getWidth();
        int imageHeight = imageProxy.getHeight();
        int srcStrideY = imageProxy.getPlanes()[0].getRowStride();
        int srcStrideU = imageProxy.getPlanes()[1].getRowStride();
        int srcStrideV = imageProxy.getPlanes()[2].getRowStride();
        int srcPixelStrideY = imageProxy.getPlanes()[0].getPixelStride();
        int srcPixelStrideUV = imageProxy.getPlanes()[1].getPixelStride();

        int startOffsetY = onePixelShiftEnabled ? srcPixelStrideY : 0;
        int startOffsetU = onePixelShiftEnabled ? srcPixelStrideUV : 0;
        int startOffsetV = onePixelShiftEnabled ? srcPixelStrideUV : 0;

        int result = convertAndroid420ToABGR(
                imageProxy.getPlanes()[0].getBuffer(),
                srcStrideY,
                imageProxy.getPlanes()[1].getBuffer(),
                srcStrideU,
                imageProxy.getPlanes()[2].getBuffer(),
                srcStrideV,
                srcPixelStrideY,
                srcPixelStrideUV,
                surface,
                imageWidth,
                imageHeight,
                startOffsetY,
                startOffsetU,
                startOffsetV);
        if (result != 0) {
            return Result.ERROR_CONVERSION;
        }
        return Result.SUCCESS;
    }

    @NonNull
    private static Result applyPixelShiftInternal(@NonNull ImageProxy imageProxy) {
        int imageWidth = imageProxy.getWidth();
        int imageHeight = imageProxy.getHeight();
        int srcStrideY = imageProxy.getPlanes()[0].getRowStride();
        int srcStrideU = imageProxy.getPlanes()[1].getRowStride();
        int srcStrideV = imageProxy.getPlanes()[2].getRowStride();
        int srcPixelStrideY = imageProxy.getPlanes()[0].getPixelStride();
        int srcPixelStrideUV = imageProxy.getPlanes()[1].getPixelStride();

        int startOffsetY = srcPixelStrideY;
        int startOffsetU = srcPixelStrideUV;
        int startOffsetV = srcPixelStrideUV;

        int result = shiftPixel(
                imageProxy.getPlanes()[0].getBuffer(),
                srcStrideY,
                imageProxy.getPlanes()[1].getBuffer(),
                srcStrideU,
                imageProxy.getPlanes()[2].getBuffer(),
                srcStrideV,
                srcPixelStrideY,
                srcPixelStrideUV,
                imageWidth,
                imageHeight,
                startOffsetY,
                startOffsetU,
                startOffsetV);
        if (result != 0) {
            return Result.ERROR_CONVERSION;
        }
        return Result.SUCCESS;
    }

    /**
     * Converts Android YUV_420_888 to RGBA.
     *
     * @param srcByteBufferY Source Y data.
     * @param srcStrideY Source Y row stride.
     * @param srcByteBufferU Source U data.
     * @param srcStrideU Source U row stride.
     * @param srcByteBufferV Source V data.
     * @param srcStrideV Source V row stride.
     * @param srcPixelStrideY Pixel stride for Y.
     * @param srcPixelStrideUV Pixel stride for UV.
     * @param surface Destination surface for ABGR data.
     * @param width Destination image width.
     * @param height Destination image height.
     * @param startOffsetY Position in Y source data to begin reading from.
     * @param startOffsetU Position in U source data to begin reading from.
     * @param startOffsetV Position in V source data to begin reading from.
     * @return zero if succeeded, otherwise non-zero.
     */
    private static native int convertAndroid420ToABGR(
            @NonNull ByteBuffer srcByteBufferY,
            int srcStrideY,
            @NonNull ByteBuffer srcByteBufferU,
            int srcStrideU,
            @NonNull ByteBuffer srcByteBufferV,
            int srcStrideV,
            int srcPixelStrideY,
            int srcPixelStrideUV,
            @NonNull Surface surface,
            int width,
            int height,
            int startOffsetY,
            int startOffsetU,
            int startOffsetV);

    private static native int shiftPixel(
            @NonNull ByteBuffer srcByteBufferY,
            int srcStrideY,
            @NonNull ByteBuffer srcByteBufferU,
            int srcStrideU,
            @NonNull ByteBuffer srcByteBufferV,
            int srcStrideV,
            int srcPixelStrideY,
            int srcPixelStrideUV,
            int width,
            int height,
            int startOffsetY,
            int startOffsetU,
            int startOffsetV);

}
