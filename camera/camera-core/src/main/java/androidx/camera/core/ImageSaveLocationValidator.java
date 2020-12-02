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

package androidx.camera.core;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.camera.core.internal.compat.quirk.DeviceQuirks;
import androidx.camera.core.internal.compat.quirk.HuaweiMediaStoreLocationValidationQuirk;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

class ImageSaveLocationValidator {

    private static final String TAG = "SaveLocationValidator";

    private ImageSaveLocationValidator() {
    }

    /**
     * Verifies whether the result of an image capture operation can be saved to the specified
     * location in {@link androidx.camera.core.ImageCapture.OutputFileOptions}.
     * <p>
     * This method checks whether an image capture to a {@link File} or to
     * {@link android.provider.MediaStore} will work by checking whether the {@link File} exists
     * and is writable, and if the {@link Uri} to {@link android.provider.MediaStore} is valid.
     *
     * @param outputFileOptions Options for saving the result of an image capture operation
     * @return true if the image capture result can be saved to the specified storage option,
     * false otherwise.
     */
    @SuppressWarnings("ConstantConditions")
    static boolean isValid(final @NonNull ImageCapture.OutputFileOptions outputFileOptions) {
        if (isSaveToFile(outputFileOptions)) {
            return canSaveToFile(outputFileOptions.getFile());
        }

        if (isSaveToMediaStore(outputFileOptions)) {
            // Skip verification on Huawei devices
            final HuaweiMediaStoreLocationValidationQuirk huaweiQuirk =
                    DeviceQuirks.get(HuaweiMediaStoreLocationValidationQuirk.class);
            if (huaweiQuirk != null) {
                return huaweiQuirk.canSaveToMediaStore();
            }

            return canSaveToMediaStore(outputFileOptions.getContentResolver(),
                    outputFileOptions.getSaveCollection(), outputFileOptions.getContentValues());
        }
        return true;
    }

    private static boolean isSaveToFile(
            final @NonNull ImageCapture.OutputFileOptions outputFileOptions) {
        return outputFileOptions.getFile() != null;
    }

    private static boolean isSaveToMediaStore(
            final @NonNull ImageCapture.OutputFileOptions outputFileOptions) {
        return outputFileOptions.getSaveCollection() != null
                && outputFileOptions.getContentResolver() != null
                && outputFileOptions.getContentValues() != null;
    }

    private static boolean canSaveToFile(@NonNull final File file) {
        // Try opening a write stream to the output file. If this succeeds, the image save
        // destination is valid. Otherwise, it's invalid.
        try (FileOutputStream ignored = new FileOutputStream(file)) {
            return true;
        } catch (IOException exception) {
            Logger.e(TAG, "Failed to open a write stream to " + file.toString(), exception);
            return false;
        }
    }

    private static boolean canSaveToMediaStore(@NonNull final ContentResolver contentResolver,
            @NonNull final Uri uri, @NonNull ContentValues values) {
        final Uri outputUri;
        try {
            // Get the uri where the image will be saved
            outputUri = contentResolver.insert(uri, values);
        } catch (IllegalArgumentException exception) {
            Logger.e(TAG, "Failed to insert into " + uri.toString(), exception);
            return false;
        }

        // If the uri is null, saving the capture result to the given uri isn't possible
        if (outputUri == null) {
            return false;
        }

        // Try opening a write stream to the output uri. If this succeeds, the image save
        // destination is valid. Otherwise, it's invalid.
        try (OutputStream stream = contentResolver.openOutputStream(outputUri)) {
            return stream != null;
        } catch (IOException exception) {
            Logger.e(TAG, "Failed to open a write stream to" + outputUri.toString(), exception);
            return false;
        } finally {
            try {
                // Delete inserted row
                contentResolver.delete(outputUri, null, null);
            } catch (IllegalArgumentException exception) {
                Logger.e(TAG, "Failed to delete inserted row at " + outputUri.toString(),
                        exception);
            }
        }
    }
}
