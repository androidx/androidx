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

package androidx.camera.core.processing;

import static androidx.camera.core.internal.utils.ImageUtil.isJpegFormats;
import static androidx.core.util.Preconditions.checkNotNull;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.Image;
import android.os.Build;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ImageInfo;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.impl.CameraCaptureResult;
import androidx.camera.core.impl.utils.Exif;
import androidx.camera.core.impl.utils.TransformUtils;

import com.google.auto.value.AutoValue;

import java.nio.ByteBuffer;

/**
 * Represents a single image and its metadata such as Exif and transformation info.
 *
 * <p>The metadata of this class, e.g. {@link #getSize()} or {@link #getFormat()} must be
 * consistent with the image data in {@link #getData()}. For example, if the {@link T} is
 * {@link Bitmap}, then {@link #getSize()} must equal to {@link Bitmap#getWidth()}x
 * {@link Bitmap#getHeight()}.
 *
 * <p>On the other hand, the metadata in {@link #getData()} should be ignored. For example, when
 * {@link T} is {@link ImageProxy}. The value of {@link Packet#getCropRect()} should be used
 * regardless of {@link ImageProxy#getCropRect()}. Similarly, if the {@link #getData()} is JPEG
 * bytes, the caller should use {@link #getExif()} as the source-of-the-truth and ignore the Exif
 * info encoded in the JPEG bytes. This enables us to modify the metadata efficiently without
 * encoding it in the image.
 *
 * @param <T> image data type. Possible values are {@link ImageProxy}, {@link ByteBuffer},
 *            {@link Bitmap} etc.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
@AutoValue
public abstract class Packet<T> {

    /**
     * The image data object.
     *
     * <p>Possible values are {@link ImageProxy}, {@link ByteBuffer}, {@link Bitmap} etc. The
     * metadata in the data, e.g. {@link ImageProxy#getCropRect()}, should be ignored.
     * Instead, the caller should use the metadata of the {@link Packet}. e.g.
     * {@link #getCropRect()}.
     */
    @NonNull
    public abstract T getData();

    /**
     * The Exif info extracted from JPEG bytes.
     *
     * @return null if {@link #getData()} is a non-JPEG {@link ImageProxy}. {@link Exif} does not
     * work with non-JPEG format. In that case, the exif data can be obtained via
     * {@link ImageInfo#populateExifData}.
     */
    @Nullable
    public abstract Exif getExif();

    /**
     * Gets the format of the image.
     *
     * <p>This value must match the format of the image in {@link #getData()}.
     *
     * <p>For {@link Bitmap} type, the value is {@link ImageFormat#FLEX_RGBA_8888}. If the Bitmap
     * has a gainmap, it can be converted to JPEG_R format on API level 34+
     */
    public abstract int getFormat();

    /**
     * Gets the size of the image.
     *
     * <p>This value must match the dimension of the image in {@link #getData()}.
     */
    @NonNull
    public abstract Size getSize();

    /**
     * Gets the crop rect.
     *
     * <p>This value is based on the coordinate system of the image in {@link #getData()}.
     */
    @NonNull
    public abstract Rect getCropRect();

    /**
     * Gets rotation degrees
     *
     * <p>This value is based on the coordinate system of the image in {@link #getData()}.
     */
    public abstract int getRotationDegrees();

    /**
     * Gets sensor-to-buffer transformation.
     *
     * <p>This value represents the transformation from sensor coordinate system to the
     * coordinate system of the image buffer in {@link #getData()}.
     */
    @NonNull
    public abstract Matrix getSensorToBufferTransform();


    /**
     * Gets the {@link CameraCaptureResult} associated with this frame.
     */
    @NonNull
    public abstract CameraCaptureResult getCameraCaptureResult();

    /**
     * Returns true if the {@link Packet} needs cropping.
     */
    public boolean hasCropping() {
        return TransformUtils.hasCropping(getCropRect(), getSize());
    }

    /**
     * Creates {@link Bitmap} based {@link Packet}.
     */
    @NonNull
    public static Packet<Bitmap> of(@NonNull Bitmap data, @NonNull Exif exif,
            @NonNull Rect cropRect, int rotationDegrees, @NonNull Matrix sensorToBufferTransform,
            @NonNull CameraCaptureResult cameraCaptureResult) {
        return new AutoValue_Packet<>(data, exif, ImageFormat.FLEX_RGBA_8888,
                new Size(data.getWidth(), data.getHeight()),
                cropRect, rotationDegrees, sensorToBufferTransform, cameraCaptureResult);
    }

    /**
     * Creates {@link ImageProxy} based {@link Packet}.
     */
    @NonNull
    public static Packet<ImageProxy> of(@NonNull ImageProxy data, @Nullable Exif exif,
            @NonNull Rect cropRect, int rotationDegrees, @NonNull Matrix sensorToBufferTransform,
            @NonNull CameraCaptureResult cameraCaptureResult) {
        return of(data, exif, new Size(data.getWidth(), data.getHeight()), cropRect,
                rotationDegrees, sensorToBufferTransform, cameraCaptureResult);
    }

    /**
     * Creates {@link ImageProxy} based {@link Packet} with overridden image size.
     *
     * <p>When the image is rotated in the HAL, the size of the {@link Image} class do not
     * match the image content. We might need to override it with the correct value. The size of
     * the {@link Image} class always matches the {@link android.view.Surface} size.
     */
    @NonNull
    public static Packet<ImageProxy> of(@NonNull ImageProxy data, @Nullable Exif exif,
            @NonNull Size size, @NonNull Rect cropRect, int rotationDegrees,
            @NonNull Matrix sensorToBufferTransform,
            @NonNull CameraCaptureResult cameraCaptureResult) {
        if (isJpegFormats(data.getFormat())) {
            checkNotNull(exif, "JPEG image must have Exif.");
        }
        return new AutoValue_Packet<>(data, exif, data.getFormat(), size, cropRect, rotationDegrees,
                sensorToBufferTransform, cameraCaptureResult);
    }

    /**
     * Creates byte array based {@link Packet}.
     */
    @NonNull
    public static Packet<byte[]> of(@NonNull byte[] data, @NonNull Exif exif,
            int format, @NonNull Size size, @NonNull Rect cropRect,
            int rotationDegrees, @NonNull Matrix sensorToBufferTransform,
            @NonNull CameraCaptureResult cameraCaptureResult) {
        return new AutoValue_Packet<>(data, exif, format, size, cropRect,
                rotationDegrees, sensorToBufferTransform, cameraCaptureResult);
    }
}
