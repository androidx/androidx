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

package androidx.camera.view.transform;

import static androidx.camera.view.TransformUtils.getExifTransform;
import static androidx.camera.view.TransformUtils.getNormalizedToBuffer;
import static androidx.camera.view.TransformUtils.rectToSize;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.UseCase;
import androidx.camera.core.impl.utils.Exif;
import androidx.camera.view.TransformExperimental;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Factory for extracting transform info from image files.
 *
 * <p> This class is for extracting a {@link OutputTransform} from an image file saved by
 * {@link ImageCapture}. The {@link OutputTransform} represents the transform being applied to
 * the original camera buffer, which can be used by {@link CoordinateTransform} to transform
 * coordinates between {@link UseCase}s.
 *
 * @see OutputTransform
 * @see CoordinateTransform
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@TransformExperimental
public final class FileTransformFactory {

    private boolean mUsingExifOrientation;

    /**
     * Whether to include the {@link ExifInterface#TAG_ORIENTATION}.
     *
     * By default, this value is false, e.g. loading image with {@link BitmapFactory} does
     * not apply the exif orientation to the loaded {@link Bitmap}. Only set this if the exif
     * orientation is applied to the loaded file. For example, if the image is loaded by a 3P
     * library that automatically applies exif orientation.
     */
    public void setUsingExifOrientation(boolean usingExifOrientation) {
        mUsingExifOrientation = usingExifOrientation;
    }

    /**
     * Whether the factory respects the exif of the image file.
     */
    public boolean isUsingExifOrientation() {
        return mUsingExifOrientation;
    }

    /**
     * Extracts transform info from the given {@link Uri}.
     */
    @NonNull
    public OutputTransform getOutputTransform(@NonNull ContentResolver contentResolver,
            @NonNull Uri uri)
            throws IOException {
        try (InputStream inputStream = contentResolver.openInputStream(uri)) {
            return getOutputTransform(inputStream);
        }
    }

    /**
     * Extracts transform info from the given {@link File}.
     */
    @NonNull
    public OutputTransform getOutputTransform(@NonNull File file) throws IOException {
        try (InputStream inputStream = new FileInputStream(file)) {
            return getOutputTransform(inputStream);
        }
    }

    /**
     * Extracts transform info from the given {@link InputStream}.
     */
    @NonNull
    public OutputTransform getOutputTransform(@NonNull InputStream inputStream) throws IOException {
        Exif exif = Exif.createFromInputStream(inputStream);
        Rect cropRect = new Rect(0, 0, exif.getWidth(), exif.getHeight());

        // Map the normalized space to the image buffer.
        Matrix matrix = getNormalizedToBuffer(cropRect);

        if (mUsingExifOrientation) {
            // Add exif transform if enabled.
            matrix.postConcat(
                    getExifTransform(exif.getOrientation(), exif.getWidth(), exif.getHeight()));
        }

        return new OutputTransform(matrix, rectToSize(cropRect));
    }
}
