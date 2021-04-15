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
import androidx.annotation.RestrictTo;
import androidx.camera.core.impl.utils.Exif;
import androidx.camera.view.TransformExperimental;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Factory for extracting transform info from on disk image files.
 *
 * TODO(b/179827713): unhide this class once all transform utils are done.
 *
 * @hide
 */
@TransformExperimental
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FileTransformFactory {

    private final boolean mUseExifOrientation;

    FileTransformFactory(boolean useExifOrientation) {
        mUseExifOrientation = useExifOrientation;
    }

    /**
     * Extracts transform from the given {@link Uri}.
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
     * Extracts transform from the given {@link File}.
     */
    @NonNull
    public OutputTransform getOutputTransform(@NonNull File file) throws IOException {
        try (InputStream inputStream = new FileInputStream(file)) {
            return getOutputTransform(inputStream);
        }
    }

    /**
     * Extracts transform from the given {@link InputStream}.
     */
    @NonNull
    public OutputTransform getOutputTransform(@NonNull InputStream inputStream) throws IOException {
        Exif exif = Exif.createFromInputStream(inputStream);
        Rect cropRect = new Rect(0, 0, exif.getWidth(), exif.getHeight());

        // Map the normalized space to the image buffer.
        Matrix matrix = getNormalizedToBuffer(cropRect);

        if (mUseExifOrientation) {
            // Add exif transform if enabled.
            matrix.postConcat(
                    getExifTransform(exif.getOrientation(), exif.getWidth(), exif.getHeight()));
        }

        return new OutputTransform(matrix, rectToSize(cropRect));
    }

    /**
     * Builder for {@link FileTransformFactory}.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static class Builder {
        boolean mUseExifOrientation = false;

        /**
         * Builds {@link FileTransformFactory}.
         */
        @NonNull
        public FileTransformFactory build() {
            return new FileTransformFactory(mUseExifOrientation);
        }

        /**
         * Whether to include the {@link ExifInterface#TAG_ORIENTATION}.
         *
         * <p> By default, the value is false. Loading image with {@link BitmapFactory} does not
         * apply the exif orientation to the loaded {@link Bitmap}. Only set this if the exif
         * orientation is applied to the loaded file. For example, if the image is loaded by a 3P
         * library that automatically applies exif orientation.
         */
        @NonNull
        public Builder setUseExifOrientation(boolean useExifOrientation) {
            mUseExifOrientation = useExifOrientation;
            return this;
        }
    }

}
