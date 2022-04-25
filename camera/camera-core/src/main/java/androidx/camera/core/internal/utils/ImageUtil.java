/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.core.internal.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.util.Rational;
import android.util.Size;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Utility class for image related operations.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class ImageUtil {
    private static final String TAG = "ImageUtil";

    private ImageUtil() {
    }

    /**
     * Converts a {@link Size} to an float array of vertexes.
     */
    @NonNull
    public static float[] sizeToVertexes(@NonNull Size size) {
        return new float[]{0, 0, size.getWidth(), 0, size.getWidth(), size.getHeight(), 0,
                size.getHeight()};
    }

    /**
     * Returns the min value.
     */
    public static float min(float value1, float value2, float value3, float value4) {
        return Math.min(Math.min(value1, value2), Math.min(value3, value4));
    }

    /**
     * Rotates aspect ratio based on rotation degrees.
     */
    @NonNull
    public static Rational getRotatedAspectRatio(
            @IntRange(from = 0, to = 359) int rotationDegrees,
            @NonNull Rational aspectRatio) {
        if (rotationDegrees == 90 || rotationDegrees == 270) {
            return inverseRational(aspectRatio);
        }

        return new Rational(aspectRatio.getNumerator(), aspectRatio.getDenominator());
    }

    /**
     * Converts JPEG {@link ImageProxy} to JPEG byte array.
     */
    @NonNull
    public static byte[] jpegImageToJpegByteArray(@NonNull ImageProxy image) {
        if (image.getFormat() != ImageFormat.JPEG) {
            throw new IllegalArgumentException(
                    "Incorrect image format of the input image proxy: " + image.getFormat());
        }

        ImageProxy.PlaneProxy[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        byte[] data = new byte[buffer.capacity()];
        buffer.rewind();
        buffer.get(data);

        return data;
    }

    /**
     * Converts JPEG {@link ImageProxy} to JPEG byte array. The input JPEG image will be cropped
     * by the specified crop rectangle and compressed by the specified quality value.
     */
    @NonNull
    public static byte[] jpegImageToJpegByteArray(@NonNull ImageProxy image,
            @NonNull Rect cropRect, @IntRange(from = 1, to = 100) int jpegQuality)
            throws CodecFailedException {
        if (image.getFormat() != ImageFormat.JPEG) {
            throw new IllegalArgumentException(
                    "Incorrect image format of the input image proxy: " + image.getFormat());
        }

        byte[] data = jpegImageToJpegByteArray(image);
        data = cropJpegByteArray(data, cropRect, jpegQuality);

        return data;
    }

    /**
     * Converts YUV_420_888 {@link ImageProxy} to JPEG byte array. The input YUV_420_888 image
     * will be cropped if a non-null crop rectangle is specified. The output JPEG byte array will
     * be compressed by the specified quality value.
     */
    @NonNull
    public static byte[] yuvImageToJpegByteArray(@NonNull ImageProxy image,
            @Nullable Rect cropRect, @IntRange(from = 1, to = 100) int jpegQuality)
            throws CodecFailedException {
        if (image.getFormat() != ImageFormat.YUV_420_888) {
            throw new IllegalArgumentException(
                    "Incorrect image format of the input image proxy: " + image.getFormat());
        }

        return ImageUtil.nv21ToJpeg(
                ImageUtil.yuv_420_888toNv21(image),
                image.getWidth(),
                image.getHeight(),
                cropRect,
                jpegQuality);
    }

    /** {@link android.media.Image} to NV21 byte array. */
    @NonNull
    public static byte[] yuv_420_888toNv21(@NonNull ImageProxy image) {
        ImageProxy.PlaneProxy yPlane = image.getPlanes()[0];
        ImageProxy.PlaneProxy uPlane = image.getPlanes()[1];
        ImageProxy.PlaneProxy vPlane = image.getPlanes()[2];

        ByteBuffer yBuffer = yPlane.getBuffer();
        ByteBuffer uBuffer = uPlane.getBuffer();
        ByteBuffer vBuffer = vPlane.getBuffer();
        yBuffer.rewind();
        uBuffer.rewind();
        vBuffer.rewind();

        int ySize = yBuffer.remaining();

        int position = 0;
        // TODO(b/115743986): Pull these bytes from a pool instead of allocating for every image.
        byte[] nv21 = new byte[ySize + (image.getWidth() * image.getHeight() / 2)];

        // Add the full y buffer to the array. If rowStride > 1, some padding may be skipped.
        for (int row = 0; row < image.getHeight(); row++) {
            yBuffer.get(nv21, position, image.getWidth());
            position += image.getWidth();
            yBuffer.position(
                    Math.min(ySize, yBuffer.position() - image.getWidth() + yPlane.getRowStride()));
        }

        int chromaHeight = image.getHeight() / 2;
        int chromaWidth = image.getWidth() / 2;
        int vRowStride = vPlane.getRowStride();
        int uRowStride = uPlane.getRowStride();
        int vPixelStride = vPlane.getPixelStride();
        int uPixelStride = uPlane.getPixelStride();

        // Interleave the u and v frames, filling up the rest of the buffer. Use two line buffers to
        // perform faster bulk gets from the byte buffers.
        byte[] vLineBuffer = new byte[vRowStride];
        byte[] uLineBuffer = new byte[uRowStride];
        for (int row = 0; row < chromaHeight; row++) {
            vBuffer.get(vLineBuffer, 0, Math.min(vRowStride, vBuffer.remaining()));
            uBuffer.get(uLineBuffer, 0, Math.min(uRowStride, uBuffer.remaining()));
            int vLineBufferPosition = 0;
            int uLineBufferPosition = 0;
            for (int col = 0; col < chromaWidth; col++) {
                nv21[position++] = vLineBuffer[vLineBufferPosition];
                nv21[position++] = uLineBuffer[uLineBufferPosition];
                vLineBufferPosition += vPixelStride;
                uLineBufferPosition += uPixelStride;
            }
        }

        return nv21;
    }

    /** Crops JPEG byte array with given {@link android.graphics.Rect}. */
    @NonNull
    @SuppressWarnings("deprecation")
    private static byte[] cropJpegByteArray(@NonNull byte[] data, @NonNull Rect cropRect,
            @IntRange(from = 1, to = 100) int jpegQuality) throws CodecFailedException {
        Bitmap bitmap;
        try {
            BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(data, 0, data.length,
                    false);
            bitmap = decoder.decodeRegion(cropRect, new BitmapFactory.Options());
            decoder.recycle();
        } catch (IllegalArgumentException e) {
            throw new CodecFailedException("Decode byte array failed with illegal argument." + e,
                    CodecFailedException.FailureType.DECODE_FAILED);
        } catch (IOException e) {
            throw new CodecFailedException("Decode byte array failed.",
                    CodecFailedException.FailureType.DECODE_FAILED);
        }

        if (bitmap == null) {
            throw new CodecFailedException("Decode byte array failed.",
                    CodecFailedException.FailureType.DECODE_FAILED);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        boolean success = bitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, out);
        if (!success) {
            throw new CodecFailedException("Encode bitmap failed.",
                    CodecFailedException.FailureType.ENCODE_FAILED);
        }
        bitmap.recycle();

        return out.toByteArray();
    }

    /** True if the given aspect ratio is meaningful. */
    public static boolean isAspectRatioValid(@Nullable Rational aspectRatio) {
        return aspectRatio != null && aspectRatio.floatValue() > 0 && !aspectRatio.isNaN();
    }

    /** True if the given aspect ratio is meaningful and has effect on the given size. */
    public static boolean isAspectRatioValid(@NonNull Size sourceSize,
            @Nullable Rational aspectRatio) {
        return aspectRatio != null
                && aspectRatio.floatValue() > 0
                && isCropAspectRatioHasEffect(sourceSize, aspectRatio)
                && !aspectRatio.isNaN();
    }

    /**
     * Calculates crop rect with the specified aspect ratio on the given size. Assuming the rect is
     * at the center of the source.
     */
    @Nullable
    public static Rect computeCropRectFromAspectRatio(@NonNull Size sourceSize,
            @NonNull Rational aspectRatio) {
        if (!isAspectRatioValid(aspectRatio)) {
            Logger.w(TAG, "Invalid view ratio.");
            return null;
        }

        int sourceWidth = sourceSize.getWidth();
        int sourceHeight = sourceSize.getHeight();
        float srcRatio = sourceWidth / (float) sourceHeight;
        int cropLeft = 0;
        int cropTop = 0;
        int outputWidth = sourceWidth;
        int outputHeight = sourceHeight;
        int numerator = aspectRatio.getNumerator();
        int denominator = aspectRatio.getDenominator();

        if (aspectRatio.floatValue() > srcRatio) {
            outputHeight = Math.round((sourceWidth / (float) numerator) * denominator);
            cropTop = (sourceHeight - outputHeight) / 2;
        } else {
            outputWidth = Math.round((sourceHeight / (float) denominator) * numerator);
            cropLeft = (sourceWidth - outputWidth) / 2;
        }

        return new Rect(cropLeft, cropTop, cropLeft + outputWidth, cropTop + outputHeight);
    }

    /**
     * Calculates crop rect based on the dispatch resolution and rotation degrees info.
     *
     * <p> The original crop rect is calculated based on camera sensor buffer. On some devices,
     * the buffer is rotated before being passed to users, in which case the crop rect also
     * needs additional transformations.
     *
     * <p> There are two most common scenarios: 1) exif rotation is 0, or 2) exif rotation
     * equals output rotation. 1) means the HAL rotated the buffer based on target
     * rotation. 2) means HAL no-oped on the rotation. Theoretically only 1) needs
     * additional transformations, but this method is also generic enough to handle all possible
     * HAL rotations.
     */
    @NonNull
    public static Rect computeCropRectFromDispatchInfo(@NonNull Rect surfaceCropRect,
            int surfaceToOutputDegrees, @NonNull Size dispatchResolution,
            int dispatchToOutputDegrees) {
        // There are 3 coordinate systems: surface, dispatch and output. Surface is where
        // the original crop rect is defined. We need to figure out what HAL
        // has done to the buffer (the surface->dispatch mapping) and apply the same
        // transformation to the crop rect.
        // The surface->dispatch mapping is calculated by inverting a dispatch->surface mapping.

        Matrix matrix = new Matrix();
        // Apply the dispatch->surface rotation.
        matrix.setRotate(dispatchToOutputDegrees - surfaceToOutputDegrees);
        // Apply the dispatch->surface translation. The translation is calculated by
        // compensating for the offset caused by the dispatch->surface rotation.
        float[] vertexes = sizeToVertexes(dispatchResolution);
        matrix.mapPoints(vertexes);
        float left = min(vertexes[0], vertexes[2], vertexes[4], vertexes[6]);
        float top = min(vertexes[1], vertexes[3], vertexes[5], vertexes[7]);
        matrix.postTranslate(-left, -top);
        // Inverting the dispatch->surface mapping to get the surface->dispatch mapping.
        matrix.invert(matrix);

        // Apply the surface->dispatch mapping to surface crop rect.
        RectF dispatchCropRectF = new RectF();
        matrix.mapRect(dispatchCropRectF, new RectF(surfaceCropRect));
        dispatchCropRectF.sort();
        Rect dispatchCropRect = new Rect();
        dispatchCropRectF.round(dispatchCropRect);
        return dispatchCropRect;
    }

    private static byte[] nv21ToJpeg(@NonNull byte[] nv21, int width, int height,
            @Nullable Rect cropRect, @IntRange(from = 1, to = 100) int jpegQuality)
            throws CodecFailedException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        YuvImage yuv = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
        boolean success =
                yuv.compressToJpeg(cropRect == null ? new Rect(0, 0, width, height) : cropRect,
                        jpegQuality, out);
        if (!success) {
            throw new CodecFailedException("YuvImage failed to encode jpeg.",
                    CodecFailedException.FailureType.ENCODE_FAILED);
        }
        return out.toByteArray();
    }

    private static boolean isCropAspectRatioHasEffect(@NonNull Size sourceSize,
            @NonNull Rational aspectRatio) {
        int sourceWidth = sourceSize.getWidth();
        int sourceHeight = sourceSize.getHeight();
        int numerator = aspectRatio.getNumerator();
        int denominator = aspectRatio.getDenominator();

        return sourceHeight != Math.round((sourceWidth / (float) numerator) * denominator)
                || sourceWidth != Math.round((sourceHeight / (float) denominator) * numerator);
    }

    private static Rational inverseRational(@Nullable Rational rational) {
        if (rational == null) {
            return rational;
        }
        return new Rational(
                /*numerator=*/ rational.getDenominator(),
                /*denominator=*/ rational.getNumerator());
    }

    /**
     * Checks whether the image's crop rectangle is the same as the source image size.
     */
    public static boolean shouldCropImage(@NonNull ImageProxy image) {
        return shouldCropImage(image.getWidth(), image.getHeight(), image.getCropRect().width(),
                image.getCropRect().height());
    }

    /**
     * Checks whether the image's crop rectangle is the same as the source image size.
     */
    public static boolean shouldCropImage(int sourceWidth, int sourceHeight, int cropRectWidth,
            int cropRectHeight) {
        return sourceWidth != cropRectWidth || sourceHeight != cropRectHeight;
    }

    /** Exception for error during transcoding image. */
    public static final class CodecFailedException extends Exception {
        public enum FailureType {
            ENCODE_FAILED,
            DECODE_FAILED,
            UNKNOWN
        }

        private FailureType mFailureType;

        CodecFailedException(@NonNull String message) {
            super(message);
            mFailureType = FailureType.UNKNOWN;
        }

        CodecFailedException(@NonNull String message, @NonNull FailureType failureType) {
            super(message);
            mFailureType = failureType;
        }

        @NonNull
        public FailureType getFailureType() {
            return mFailureType;
        }
    }
}
