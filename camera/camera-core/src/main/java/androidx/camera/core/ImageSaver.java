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

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.utils.Exif;
import androidx.camera.core.internal.compat.workaround.ExifRotationAvailability;
import androidx.camera.core.internal.utils.ImageUtil;
import androidx.camera.core.internal.utils.ImageUtil.CodecFailedException;
import androidx.core.util.Preconditions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
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
    // The compression quality level of the output JPEG image
    private final int mJpegQuality;
    // The target location to save the image to.
    @NonNull
    private final ImageCapture.OutputFileOptions mOutputFileOptions;
    // The executor to call back on
    @NonNull
    private final Executor mUserCallbackExecutor;
    // The callback to call on completion
    @NonNull
    private final OnImageSavedCallback mCallback;
    // The executor to handle the I/O operations
    @NonNull
    private final Executor mSequentialIoExecutor;

    ImageSaver(
            @NonNull ImageProxy image,
            @NonNull ImageCapture.OutputFileOptions outputFileOptions,
            int orientation,
            @IntRange(from = 1, to = 100) int jpegQuality,
            @NonNull Executor userCallbackExecutor,
            @NonNull Executor sequentialIoExecutor,
            @NonNull OnImageSavedCallback callback) {
        mImage = image;
        mOutputFileOptions = outputFileOptions;
        mOrientation = orientation;
        mJpegQuality = jpegQuality;
        mCallback = callback;
        mUserCallbackExecutor = userCallbackExecutor;
        mSequentialIoExecutor = sequentialIoExecutor;
    }

    @Override
    public void run() {
        // Save the image to a temp file first. This is necessary because ExifInterface only
        // supports saving to File.
        File tempFile = saveImageToTempFile();
        if (tempFile != null) {
            // Post copying on a sequential executor. If the user provided saving destination maps
            // to a specific file on disk, accessing the file from multiple threads is not safe.
            mSequentialIoExecutor.execute(() -> copyTempFileToDestination(tempFile));
        }
    }

    /**
     * Saves the {@link #mImage} to a temp file.
     *
     * <p> It also crops the image and update Exif if necessary. Returns null if saving failed.
     */
    @Nullable
    private File saveImageToTempFile() {
        File tempFile;
        try {
            if (isSaveToFile()) {
                // For saving to file, write to the target folder and rename for better performance.
                tempFile = new File(mOutputFileOptions.getFile().getParent(),
                        TEMP_FILE_PREFIX + UUID.randomUUID().toString() + TEMP_FILE_SUFFIX);
            } else {
                tempFile = File.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX);
            }
        } catch (IOException e) {
            postError(SaveError.FILE_IO_FAILED, "Failed to create temp file", e);
            return null;
        }

        SaveError saveError = null;
        String errorMessage = null;
        Exception exception = null;
        try (ImageProxy imageToClose = mImage;
             FileOutputStream output = new FileOutputStream(tempFile)) {
            byte[] bytes = imageToJpegByteArray(mImage, mJpegQuality);
            output.write(bytes);

            // Create new exif based on the original exif.
            Exif exif = Exif.createFromFile(tempFile);
            Exif.createFromImageProxy(mImage).copyToCroppedImage(exif);

            // Overwrite the original orientation if the quirk exists.
            if (!new ExifRotationAvailability().shouldUseExifOrientation(mImage)) {
                exif.rotate(mOrientation);
            }

            // Overwrite exif based on metadata.
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
        } catch (IOException | IllegalArgumentException e) {
            saveError = SaveError.FILE_IO_FAILED;
            errorMessage = "Failed to write temp file";
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
        }
        if (saveError != null) {
            postError(saveError, errorMessage, exception);
            tempFile.delete();
            return null;
        }
        return tempFile;
    }

    @NonNull
    private byte[] imageToJpegByteArray(@NonNull ImageProxy image, @IntRange(from = 1,
            to = 100) int jpegQuality) throws CodecFailedException {
        boolean shouldCropImage = ImageUtil.shouldCropImage(image);
        int imageFormat = image.getFormat();

        if (imageFormat == ImageFormat.JPEG) {
            if (!shouldCropImage) {
                // When cropping is unnecessary, the byte array doesn't need to be decoded and
                // re-encoded again. Therefore, jpegQuality is unnecessary in this case.
                return ImageUtil.jpegImageToJpegByteArray(image);
            } else {
                return ImageUtil.jpegImageToJpegByteArray(image, image.getCropRect(), jpegQuality);
            }
        } else if (imageFormat == ImageFormat.YUV_420_888) {
            return ImageUtil.yuvImageToJpegByteArray(image, shouldCropImage ? image.getCropRect() :
                    null, jpegQuality);
        } else {
            Logger.w(TAG, "Unrecognized image format: " + imageFormat);
        }

        return null;
    }

    /**
     * Copy the temp file to user specified destination.
     *
     * <p> The temp file will be deleted afterwards.
     */
    void copyTempFileToDestination(@NonNull File tempFile) {
        Preconditions.checkNotNull(tempFile);
        SaveError saveError = null;
        String errorMessage = null;
        Exception exception = null;
        Uri outputUri = null;
        try {
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
                    if (!copyTempFileToUri(tempFile, outputUri)) {
                        saveError = SaveError.FILE_IO_FAILED;
                        errorMessage = "Failed to save to URI.";
                    }
                    setUriNotPending(outputUri);
                }
            } else if (isSaveToOutputStream()) {
                copyTempFileToOutputStream(tempFile, mOutputFileOptions.getOutputStream());
            } else if (isSaveToFile()) {
                File targetFile = mOutputFileOptions.getFile();
                // Normally File#renameTo will overwrite the targetFile even if it already exists.
                // Just in case of unexpected behavior on certain platforms or devices, delete the
                // target file before renaming.
                if (targetFile.exists()) {
                    targetFile.delete();
                }
                if (!tempFile.renameTo(targetFile)) {
                    saveError = SaveError.FILE_IO_FAILED;
                    errorMessage = "Failed to rename file.";
                }
                outputUri = Uri.fromFile(targetFile);
            }
        } catch (IOException | IllegalArgumentException e) {
            saveError = SaveError.FILE_IO_FAILED;
            errorMessage = "Failed to write destination file.";
            exception = e;
        } finally {
            tempFile.delete();
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
            mUserCallbackExecutor.execute(
                    () -> mCallback.onImageSaved(new ImageCapture.OutputFileResults(outputUri)));
        } catch (RejectedExecutionException e) {
            Logger.e(TAG,
                    "Application executor rejected executing OnImageSavedCallback.onImageSaved "
                            + "callback. Skipping.");
        }
    }

    private void postError(SaveError saveError, final String message,
            @Nullable final Throwable cause) {
        try {
            mUserCallbackExecutor.execute(() -> mCallback.onError(saveError, message, cause));
        } catch (RejectedExecutionException e) {
            Logger.e(TAG, "Application executor rejected executing OnImageSavedCallback.onError "
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

        void onError(@NonNull SaveError saveError, @NonNull String message,
                @Nullable Throwable cause);
    }
}
