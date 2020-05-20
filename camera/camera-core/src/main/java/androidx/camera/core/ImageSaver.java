/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.content.ContentValues;
import android.graphics.ImageFormat;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.impl.utils.Exif;
import androidx.camera.core.internal.utils.ImageUtil;
import androidx.camera.core.internal.utils.ImageUtil.CodecFailedException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

final class ImageSaver implements Runnable {
    private static final String TAG = "ImageSaver";

    private static final String TEMP_FILE_PREFIX = "CameraX";
    private static final String TEMP_FILE_SUFFIX = ".tmp";
    private static final int COPY_BUFFER_SIZE = 1024;
    private static final int PENDING = 1;
    private static final int NOT_PENDING = 0;

    // The image that was captured
    private final ImageProxy mImage;
    // The orientation of the image
    private final int mOrientation;
    // The target location to save the image to.
    @NonNull
    private final ImageCapture.OutputFileOptions mOutputFileOptions;
    // The executor to call back on
    private final Executor mExecutor;
    // The callback to call on completion
    final OnImageSavedCallback mCallback;

    ImageSaver(
            ImageProxy image,
            @NonNull ImageCapture.OutputFileOptions outputFileOptions,
            int orientation,
            Executor executor,
            OnImageSavedCallback callback) {
        mImage = image;
        mOutputFileOptions = outputFileOptions;
        mOrientation = orientation;
        mCallback = callback;
        mExecutor = executor;
    }

    @Override
    public void run() {
        // Finally, we save the file to disk
        SaveError saveError = null;
        String errorMessage = null;
        Exception exception = null;

        File file;
        Uri outputUri = null;
        try {
            // Create a temp file if the save location is not a file. This is necessary because
            // ExifInterface only supports File.
            file = isSaveToFile() ? mOutputFileOptions.getFile() :
                    File.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX);
        } catch (IOException e) {
            postError(SaveError.FILE_IO_FAILED, "Failed to create temp file", e);
            return;
        }

        try (ImageProxy imageToClose = mImage;
             FileOutputStream output = new FileOutputStream(file)) {
            byte[] bytes = ImageUtil.imageToJpegByteArray(mImage);
            output.write(bytes);

            Exif exif = Exif.createFromFile(file);
            exif.attachTimestamp();

            // Use exif for orientation (contains rotation only) from the original image if JPEG,
            // because imageToJpegByteArray removes EXIF in certain conditions. See b/124280392
            if (mImage.getFormat() == ImageFormat.JPEG) {
                ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
                // Rewind to make sure it is at the beginning of the buffer
                buffer.rewind();

                byte[] data = new byte[buffer.capacity()];
                buffer.get(data);
                InputStream inputStream = new ByteArrayInputStream(data);
                Exif originalExif = Exif.createFromInputStream(inputStream);

                exif.setOrientation(originalExif.getOrientation());
            } else {
                exif.rotate(mOrientation);
            }

            ImageCapture.Metadata metadata = mOutputFileOptions.getMetadata();
            if (metadata.isReversedHorizontal()) {
                exif.flipHorizontally();
            }
            if (metadata.isReversedVertical()) {
                exif.flipVertically();
            }
            if (metadata.getLocation() != null) {
                exif.attachLocation(mOutputFileOptions.getMetadata().getLocation());
            }

            exif.save();

            if (isSaveToMediaStore()) {
                ContentValues values = mOutputFileOptions.getContentValues() != null
                        ? new ContentValues(mOutputFileOptions.getContentValues())
                        : new ContentValues();
                setContentValuePending(values, PENDING);
                outputUri = mOutputFileOptions.getContentResolver().insert(
                        mOutputFileOptions.getSaveCollection(),
                        values);
                if (outputUri == null) {
                    saveError = SaveError.FILE_IO_FAILED;
                    errorMessage = "Failed to insert URI.";
                } else {
                    if (!copyTempFileToUri(file, outputUri)) {
                        saveError = SaveError.FILE_IO_FAILED;
                        errorMessage = "Failed to save to URI.";
                    }
                    setUriNotPending(outputUri);
                }
            } else if (isSaveToOutputStream()) {
                copyTempFileToOutputStream(file, mOutputFileOptions.getOutputStream());
            }
        } catch (IOException | IllegalArgumentException e) {
            saveError = SaveError.FILE_IO_FAILED;
            errorMessage = "Failed to write or close the file";
            exception = e;
        } catch (CodecFailedException e) {
            switch (e.getFailureType()) {
                case ENCODE_FAILED:
                    saveError = SaveError.ENCODE_FAILED;
                    errorMessage = "Failed to encode mImage";
                    break;
                case DECODE_FAILED:
                    saveError = SaveError.CROP_FAILED;
                    errorMessage = "Failed to crop mImage";
                    break;
                case UNKNOWN:
                default:
                    saveError = SaveError.UNKNOWN;
                    errorMessage = "Failed to transcode mImage";
                    break;
            }
            exception = e;
        } finally {
            if (!isSaveToFile()) {
                // Cleanup temp file if created.
                file.delete();
            }
        }

        if (saveError != null) {
            postError(saveError, errorMessage, exception);
        } else {
            postSuccess(outputUri);
        }
    }

    private boolean isSaveToMediaStore() {
        return mOutputFileOptions.getSaveCollection() != null
                && mOutputFileOptions.getContentResolver() != null
                && mOutputFileOptions.getContentValues() != null;
    }

    private boolean isSaveToFile() {
        return mOutputFileOptions.getFile() != null;
    }

    private boolean isSaveToOutputStream() {
        return mOutputFileOptions.getOutputStream() != null;
    }

    /**
     * Removes IS_PENDING flag during the writing to {@link Uri}.
     */
    private void setUriNotPending(@NonNull Uri outputUri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            setContentValuePending(values, NOT_PENDING);
            mOutputFileOptions.getContentResolver().update(outputUri, values, null, null);
        }
    }

    /** Set IS_PENDING flag to {@link ContentValues}. */
    private void setContentValuePending(@NonNull ContentValues values, int isPending) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.IS_PENDING, isPending);
        }
    }

    /**
     * Copies temp file to {@link Uri}.
     *
     * @return false if the {@link Uri} is not writable.
     */
    private boolean copyTempFileToUri(@NonNull File tempFile, @NonNull Uri uri) throws IOException {
        try (OutputStream outputStream =
                     mOutputFileOptions.getContentResolver().openOutputStream(uri)) {
            if (outputStream == null) {
                // The URI is not writable.
                return false;
            }
            copyTempFileToOutputStream(tempFile, outputStream);
        }
        return true;
    }

    private void copyTempFileToOutputStream(@NonNull File tempFile,
            @NonNull OutputStream outputStream) throws IOException {
        try (InputStream in = new FileInputStream(tempFile)) {
            byte[] buf = new byte[COPY_BUFFER_SIZE];
            int len;
            while ((len = in.read(buf)) > 0) {
                outputStream.write(buf, 0, len);
            }
        }
    }

    private void postSuccess(@Nullable Uri outputUri) {
        try {
            mExecutor.execute(
                    () -> mCallback.onImageSaved(new ImageCapture.OutputFileResults(outputUri)));
        } catch (RejectedExecutionException e) {
            Log.e(TAG, "Application executor rejected executing OnImageSavedCallback.onImageSaved "
                    + "callback. Skipping.");
        }
    }

    private void postError(SaveError saveError, final String message,
            @Nullable final Throwable cause) {
        try {
            mExecutor.execute(() -> mCallback.onError(saveError, message, cause));
        } catch (RejectedExecutionException e) {
            Log.e(TAG, "Application executor rejected executing OnImageSavedCallback.onError "
                    + "callback. Skipping.");
        }
    }

    /** Type of error that occurred during save */
    public enum SaveError {
        /** Failed to write to or close the file */
        FILE_IO_FAILED,
        /** Failure when attempting to encode image */
        ENCODE_FAILED,
        /** Failure when attempting to crop image */
        CROP_FAILED,
        UNKNOWN
    }

    public interface OnImageSavedCallback {

        void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults);

        void onError(SaveError saveError, String message, @Nullable Throwable cause);
    }
}
