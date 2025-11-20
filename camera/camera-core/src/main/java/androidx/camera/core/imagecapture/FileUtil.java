/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.core.imagecapture;

import static androidx.camera.core.ImageCapture.ERROR_FILE_IO;

import static java.util.Objects.requireNonNull;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.utils.Exif;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Utility class for file read and write operations.
 */
public final class FileUtil {

    private static final String TAG = "FileUtil";
    private static final String TEMP_FILE_PREFIX = "CameraX";
    private static final String TEMP_FILE_SUFFIX = ".tmp";
    private static final int COPY_BUFFER_SIZE = 1024;
    private static final int PENDING = 1;
    private static final int NOT_PENDING = 0;

    private FileUtil() {}

    /**
     * Creates a temporary Dng file.
     */
    static @NonNull File createTempFile(ImageCapture.@NonNull OutputFileOptions options)
            throws ImageCaptureException {
        try {
            File appProvidedFile = options.getFile();
            if (appProvidedFile != null) {
                // For saving-to-file case, write to the target folder and rename for better
                // performance. The file extensions must be the same as app provided to avoid the
                // directory access problem.
                return new File(appProvidedFile.getParent(),
                        TEMP_FILE_PREFIX + UUID.randomUUID().toString()
                                + getFileExtensionWithDot(appProvidedFile));
            } else {
                return File.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX);
            }
        } catch (IOException e) {
            throw new ImageCaptureException(ERROR_FILE_IO, "Failed to create temp file.", e);
        }
    }

    /**
     * Updates exif data.
     *
     * @param tempFile
     * @param originalExif
     * @param options
     * @param rotationDegrees
     * @throws ImageCaptureException
     */
    static void updateFileExif(
            @NonNull File tempFile,
            @NonNull Exif originalExif,
            ImageCapture.@NonNull OutputFileOptions options,
            int rotationDegrees)
            throws ImageCaptureException {
        try {
            // Create new exif based on the original exif.
            Exif exif = Exif.createFromFile(tempFile);
            originalExif.copyToCroppedImage(exif);

            if (exif.getRotation() == 0 && rotationDegrees != 0) {
                // When the HAL does not handle rotation, exif rotation is 0. In which case we
                // apply the packet rotation.
                // See: EXIF_ROTATION_AVAILABILITY
                exif.rotate(rotationDegrees);
            }

            // Overwrite exif based on metadata.
            ImageCapture.Metadata metadata = options.getMetadata();
            if (metadata.isReversedHorizontal()) {
                exif.flipHorizontally();
            }
            if (metadata.isReversedVertical()) {
                exif.flipVertically();
            }
            if (metadata.getLocation() != null) {
                exif.attachLocation(metadata.getLocation());
            }
            exif.save();
        } catch (IOException e) {
            throw new ImageCaptureException(ERROR_FILE_IO, "Failed to update Exif data", e);
        }
    }

    /**
     * Copies the file to target, deletes the original file and returns the target's {@link Uri}.
     *
     * @return null if the target is {@link OutputStream}.
     */
    static @Nullable Uri moveFileToTarget(
            @NonNull File tempFile, ImageCapture.@NonNull OutputFileOptions options)
            throws ImageCaptureException {
        Uri uri = null;
        try {
            if (isSaveToMediaStore(options)) {
                uri = copyFileToMediaStore(tempFile, options);
            } else if (isSaveToOutputStream(options)) {
                copyFileToOutputStream(tempFile, requireNonNull(options.getOutputStream()));
            } else if (isSaveToFile(options)) {
                uri = copyFileToFile(tempFile, requireNonNull(options.getFile()));
            }
        } catch (IOException e) {
            throw new ImageCaptureException(
                    ERROR_FILE_IO, "Failed to write to OutputStream.", null);
        } finally {
            tempFile.delete();
        }
        return uri;
    }

    private static String getFileExtensionWithDot(File file) {
        String fileName = file.getName();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex >= 0) {
            return fileName.substring(dotIndex);
        } else {
            return "";
        }
    }

    private static Uri copyFileToMediaStore(
            @NonNull File file,
            ImageCapture.@NonNull OutputFileOptions options)
            throws ImageCaptureException {
        ContentResolver contentResolver = requireNonNull(options.getContentResolver());
        ContentValues values = options.getContentValues() != null
                ? new ContentValues(options.getContentValues())
                : new ContentValues();

        setContentValuePendingFlag(values, PENDING);
        Uri uri = null;
        try {
            Logger.d(TAG, "copyFileToMediaStore: inserting values to MediaStore");
            uri = contentResolver.insert(options.getSaveCollection(), values);
            Logger.d(TAG, "copyFileToMediaStore: insert success");
            if (uri == null) {
                throw new ImageCaptureException(
                        ERROR_FILE_IO, "Failed to insert a MediaStore URI.", null);
            }
            copyTempFileToUri(file, uri, contentResolver);
        } catch (IOException | SecurityException e) {
            throw new ImageCaptureException(
                    ERROR_FILE_IO, "Failed to write to MediaStore URI: " + uri, e);
        } finally {
            if (uri != null) {
                updateUriPendingStatus(uri, contentResolver, NOT_PENDING);
            }
        }
        return uri;
    }

    private static Uri copyFileToFile(@NonNull File source, @NonNull File target)
            throws ImageCaptureException {
        // Normally File#renameTo will overwrite the targetFile even if it already exists.
        // Just in case of unexpected behavior on certain platforms or devices, delete the
        // target file before renaming.
        if (target.exists()) {
            target.delete();
        }
        if (!source.renameTo(target)) {
            throw new ImageCaptureException(
                    ERROR_FILE_IO,
                    "Failed to overwrite the file: " + target.getAbsolutePath(),
                    null);
        }
        return Uri.fromFile(target);
    }

    /**
     * Copies temp file to {@link Uri}.
     */
    private static void copyTempFileToUri(
            @NonNull File tempFile,
            @NonNull Uri uri,
            @NonNull ContentResolver contentResolver) throws IOException {
        try (OutputStream outputStream = contentResolver.openOutputStream(uri)) {
            if (outputStream == null) {
                throw new FileNotFoundException(uri + " cannot be resolved.");
            }
            copyFileToOutputStream(tempFile, outputStream);
        }
    }

    @SuppressWarnings("IOStreamConstructor")
    private static void copyFileToOutputStream(@NonNull File file,
            @NonNull OutputStream outputStream)
            throws IOException {
        try (InputStream in = new FileInputStream(file)) {
            byte[] buf = new byte[COPY_BUFFER_SIZE];
            int len;
            while ((len = in.read(buf)) > 0) {
                outputStream.write(buf, 0, len);
            }
        }
    }

    /**
     * Removes IS_PENDING flag during the writing to {@link Uri}.
     */
    private static void updateUriPendingStatus(@NonNull Uri outputUri,
            @NonNull ContentResolver contentResolver, int isPending) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            setContentValuePendingFlag(values, isPending);
            contentResolver.update(outputUri, values, null, null);
        }
    }

    /** Set IS_PENDING flag to {@link ContentValues}. */
    private static void setContentValuePendingFlag(@NonNull ContentValues values, int isPending) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.IS_PENDING, isPending);
        }
    }

    private static boolean isSaveToMediaStore(ImageCapture.OutputFileOptions outputFileOptions) {
        return outputFileOptions.getSaveCollection() != null
                && outputFileOptions.getContentResolver() != null
                && outputFileOptions.getContentValues() != null;
    }

    private static boolean isSaveToFile(ImageCapture.OutputFileOptions outputFileOptions) {
        return outputFileOptions.getFile() != null;
    }

    private static boolean isSaveToOutputStream(ImageCapture.OutputFileOptions outputFileOptions) {
        return outputFileOptions.getOutputStream() != null;
    }
}
