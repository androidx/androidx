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

import android.location.Location;
import android.os.Handler;

import androidx.annotation.Nullable;
import androidx.camera.core.ImageUtil.CodecFailedException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

final class ImageSaver implements Runnable {
    private static final String TAG = "ImageSaver";
    @Nullable
    private final Location mLocation;
    // The image that was captured
    private final ImageProxy mImage;
    // The orientation of the image
    private final int mOrientation;
    // If true, the picture taken is reversed horizontally and needs to be flipped.
    // Typical with front facing cameras.
    private final boolean mIsReversedHorizontal;
    // If true, the picture taken is reversed vertically and needs to be flipped.
    private final boolean mIsReversedVertical;
    // The file to save the image to
    final File mFile;
    // The callback to call on completion
    final OnImageSavedListener mListener;
    // The handler to call back on
    private final Handler mHandler;

    ImageSaver(
            ImageProxy image,
            File file,
            int orientation,
            boolean reversedHorizontal,
            boolean reversedVertical,
            @Nullable Location location,
            OnImageSavedListener listener,
            Handler handler) {
        mImage = image;
        mFile = file;
        mOrientation = orientation;
        mIsReversedHorizontal = reversedHorizontal;
        mIsReversedVertical = reversedVertical;
        mListener = listener;
        mHandler = handler;
        mLocation = location;
    }

    @Override
    public void run() {
        // Finally, we save the file to disk
        SaveError saveError = null;
        String errorMessage = null;
        Exception exception = null;
        try (ImageProxy imageToClose = mImage;
             FileOutputStream output = new FileOutputStream(mFile)) {
            byte[] bytes = ImageUtil.imageToJpegByteArray(mImage);
            output.write(bytes);

            Exif exif = Exif.createFromFile(mFile);
            exif.attachTimestamp();
            exif.rotate(mOrientation);
            if (mIsReversedHorizontal) {
                exif.flipHorizontally();
            }
            if (mIsReversedVertical) {
                exif.flipVertically();
            }
            if (mLocation != null) {
                exif.attachLocation(mLocation);
            }
            exif.save();
        } catch (IOException e) {
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
        }

        if (saveError != null) {
            postError(saveError, errorMessage, exception);
        } else {
            postSuccess();
        }
    }

    private void postSuccess() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mListener.onImageSaved(mFile);
            }
        });
    }

    private void postError(final SaveError saveError, final String message,
            @Nullable final Throwable cause) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mListener.onError(saveError, message, cause);
            }
        });
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

    public interface OnImageSavedListener {

        void onImageSaved(File file);

        void onError(SaveError saveError, String message, @Nullable Throwable cause);
    }
}
