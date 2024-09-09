/*
 * Copyright 2022 The Android Open Source Project
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

import static androidx.core.util.Preconditions.checkArgument;

import static java.nio.ByteBuffer.allocateDirect;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.util.Rational;
import android.util.Size;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.ImageProcessingUtil;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.utils.ExifData;
import androidx.camera.core.impl.utils.ExifOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Utility class for image related operations.
 */
public final class ImageUtil {
    private static final String TAG = "ImageUtil";

    /**
     * Default RGBA pixel stride used by CameraX, with R, G, B and A each occupies 1 byte.
     */
    public static final int DEFAULT_RGBA_PIXEL_STRIDE = 4;

    private ImageUtil() {
    }

    /**
     * Creates {@link Bitmap} from {@link ImageProxy}.
     *
     * <p> Currently only {@link ImageFormat#YUV_420_888}, {@link ImageFormat#JPEG},
     * {@link ImageFormat#JPEG_R} and {@link PixelFormat#RGBA_8888} are supported. If the format
     * is invalid, an {@link IllegalArgumentException} will be thrown. If the conversion to bimap
     * failed, an {@link UnsupportedOperationException} will be thrown.
     *
     * @param imageProxy The input {@link ImageProxy} instance.
     * @return {@link Bitmap} instance.
     */
    @NonNull
    public static Bitmap createBitmapFromImageProxy(@NonNull ImageProxy imageProxy) {
        switch (imageProxy.getFormat()) {
            case ImageFormat.YUV_420_888:
                return ImageProcessingUtil.convertYUVToBitmap(imageProxy);
            case ImageFormat.JPEG:
            case ImageFormat.JPEG_R:
                return createBitmapFromJpegImage(imageProxy);
            case PixelFormat.RGBA_8888:
                return createBitmapFromRgbaImage(imageProxy);
            default:
                throw new IllegalArgumentException(
                        "Incorrect image format of the input image proxy: "
                                + imageProxy.getFormat() + ", only ImageFormat.YUV_420_888 and "
                                + "PixelFormat.RGBA_8888 are supported");
        }
    }

    /**
     * Creates a {@link Bitmap} from an {@link ImageProxy.PlaneProxy} array.
     *
     * <p>This method expects a single plane with a pixel stride of 4 and a row stride of (width *
     * 4).
     */
    @NonNull
    public static Bitmap createBitmapFromPlane(
            @NonNull ImageProxy.PlaneProxy[] planes, int width, int height) {
        checkArgument(planes.length == 1, "Expect a single plane");
        checkArgument(planes[0].getPixelStride() == DEFAULT_RGBA_PIXEL_STRIDE,
                "Expect pixelStride=" + DEFAULT_RGBA_PIXEL_STRIDE);
        checkArgument(
                planes[0].getRowStride() == DEFAULT_RGBA_PIXEL_STRIDE * width,
                "Expect rowStride=width*" + DEFAULT_RGBA_PIXEL_STRIDE);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        // Rewind the buffer just to be safe.
        planes[0].getBuffer().rewind();
        ImageProcessingUtil.copyByteBufferToBitmap(bitmap, planes[0].getBuffer(),
                planes[0].getRowStride());
        return bitmap;
    }

    /**
     * Rotates the bitmap by the given rotation degrees.
     */
    @NonNull
    public static Bitmap rotateBitmap(@NonNull Bitmap bitmap, int rotationDegrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(rotationDegrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix,
                true);
    }

    /**
     * Creates a direct {@link ByteBuffer} and copy the content of the {@link Bitmap}.
     */
    @NonNull
    public static ByteBuffer createDirectByteBuffer(@NonNull Bitmap bitmap) {
        checkArgument(bitmap.getConfig() == Bitmap.Config.ARGB_8888,
                "Only accept Bitmap with ARGB_8888 format for now.");
        ByteBuffer byteBuffer = allocateDirect(bitmap.getAllocationByteCount());
        ImageProcessingUtil.copyBitmapToByteBuffer(bitmap, byteBuffer, bitmap.getRowBytes());
        byteBuffer.rewind();
        return byteBuffer;
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
     * Converts JPEG or JPEG_R {@link ImageProxy} to JPEG byte array.
     */
    @NonNull
    public static byte[] jpegImageToJpegByteArray(@NonNull ImageProxy image) {
        if (!isJpegFormats(image.getFormat())) {
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
        if (!isJpegFormats(image.getFormat())) {
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
     * be compressed by the specified quality value. The rotationDegrees is set to the EXIF of
     * the JPEG if it is not 0.
     */
    @NonNull
    public static byte[] yuvImageToJpegByteArray(@NonNull ImageProxy image,
            @Nullable Rect cropRect,
            @IntRange(from = 1, to = 100)
            int jpegQuality,
            int rotationDegrees) throws CodecFailedException {
        if (image.getFormat() != ImageFormat.YUV_420_888) {
            throw new IllegalArgumentException(
                    "Incorrect image format of the input image proxy: " + image.getFormat());
        }

        byte[] yuvBytes = yuv_420_888toNv21(image);
        YuvImage yuv = new YuvImage(yuvBytes, ImageFormat.NV21, image.getWidth(), image.getHeight(),
                null);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        OutputStream out = new ExifOutputStream(
                byteArrayOutputStream, ExifData.create(image, rotationDegrees));
        if (cropRect == null) {
            cropRect = new Rect(0, 0, image.getWidth(), image.getHeight());
        }
        boolean success =
                yuv.compressToJpeg(cropRect, jpegQuality, out);
        if (!success) {
            throw new CodecFailedException("YuvImage failed to encode jpeg.",
                    CodecFailedException.FailureType.ENCODE_FAILED);
        }
        return byteArrayOutputStream.toByteArray();
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

    /** Crops JPEG or JPEG_R byte array with given {@link android.graphics.Rect}. */
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

    /** True if the given image format is JPEG or JPEG/R. */
    public static boolean isJpegFormats(int imageFormat) {
        return imageFormat == ImageFormat.JPEG || imageFormat == ImageFormat.JPEG_R;
    }

    /** True if the given image format is RAW_SENSOR. */
    public static boolean isRawFormats(int imageFormat) {
        return imageFormat == ImageFormat.RAW_SENSOR;
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

    @NonNull
    private static Bitmap createBitmapFromRgbaImage(@NonNull ImageProxy imageProxy) {
        Bitmap bitmap =
                Bitmap.createBitmap(imageProxy.getWidth(),
                imageProxy.getHeight(),
                Bitmap.Config.ARGB_8888);
        // Rewind the buffer just to be safe.
        imageProxy.getPlanes()[0].getBuffer().rewind();
        ImageProcessingUtil.copyByteBufferToBitmap(bitmap, imageProxy.getPlanes()[0].getBuffer(),
                imageProxy.getPlanes()[0].getRowStride());
        return bitmap;
    }

    @NonNull
    private static Bitmap createBitmapFromJpegImage(@NonNull ImageProxy imageProxy) {
        byte[] bytes = jpegImageToJpegByteArray(imageProxy);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
        if (bitmap == null) {
            throw new UnsupportedOperationException("Decode jpeg byte array failed");
        }
        return bitmap;
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

        private final FailureType mFailureType;

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
