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

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Build;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.impl.utils.Exif;

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
     */
    @NonNull
    public abstract Exif getExif();

    /**
     * Gets the format of the image.
     *
     * <p>This value must match the format of the image in {@link #getData()}.
     *
     * <p>For {@link Bitmap} type, the value is {@link ImageFormat#FLEX_RGBA_8888}.
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
     * Creates {@link Bitmap} {@link Packet}.
     */
    @NonNull
    public static Packet<Bitmap> of(@NonNull Bitmap data, @Nullable Exif exif,
            @NonNull Rect cropRect, int rotationDegrees, @NonNull Matrix sensorToBufferTransform) {
        return new AutoValue_Packet<>(data, exif, ImageFormat.FLEX_RGBA_8888,
                new Size(data.getWidth(), data.getHeight()),
                cropRect, rotationDegrees, sensorToBufferTransform);
    }

    /**
     * Creates {@link Packet}.
     */
    @NonNull
    public static <T> Packet<T> of(@NonNull T data, @NonNull Exif exif,
            int format, @NonNull Size size, @NonNull Rect cropRect,
            int rotationDegrees, @NonNull Matrix sensorToBufferTransform) {
        return new AutoValue_Packet<>(data, exif, format, size, cropRect,
                rotationDegrees, sensorToBufferTransform);
    }
}
