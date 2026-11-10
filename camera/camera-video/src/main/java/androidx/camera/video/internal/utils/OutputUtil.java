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

package androidx.camera.video.internal.utils;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;

import androidx.camera.core.Logger;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Utility class for output related operations.
 */
public final class OutputUtil {
    private static final String TAG = "OutputUtil";

    private OutputUtil(){}

    /** Gets the absolute path from a Uri. */
    public static @Nullable String getAbsolutePathFromUri(@NonNull ContentResolver resolver,
            @NonNull Uri contentUri, @NonNull String mediaStoreColumn) {
        Cursor cursor = null;
        try {
            String[] proj;
            int columnIndex;
            proj = new String[]{mediaStoreColumn};
            cursor = resolver.query(contentUri, proj, null, null, null);

            if (cursor == null) {
                return null;
            }

            columnIndex = cursor.getColumnIndexOrThrow(mediaStoreColumn);
            if (!cursor.moveToFirst()) {
                return null;
            }
            return cursor.getString(columnIndex);
        } catch (RuntimeException e) {
            Logger.e(TAG, String.format(
                    "Failed in getting absolute path for Uri %s with Exception %s",
                    contentUri.toString(), e.toString()));
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Creates parent folder for the input file.
     *
     * @param file the input file to create its parent folder
     * @return {@code true} if the parent folder already exists or is created successfully.
     * {@code false} if the existing parent folder path is not a folder or failed to create.
     */
    public static boolean createParentFolder(@NonNull File file) {
        File parentFile = file.getParentFile();
        if (parentFile == null) {
            return false;
        }
        return parentFile.exists() ? parentFile.isDirectory() : parentFile.mkdirs();
    }

    /**
     * Opens a {@link ParcelFileDescriptor} for a MediaStore URI.
     *
     * <p>If the target parent folder doesn't exist, this attempts to recreate it and retry.
     */
    public static @NonNull ParcelFileDescriptor openMediaStoreFileDescriptor(
            @NonNull ContentResolver resolver, @NonNull Uri outputUri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor;
        try {
            parcelFileDescriptor = resolver.openFileDescriptor(outputUri, "rw");
        } catch (FileNotFoundException e) {
            @SuppressWarnings("deprecation")
            String path = getAbsolutePathFromUri(
                    resolver, outputUri, MediaStore.MediaColumns.DATA);
            if (path == null) {
                throw e;
            }
            File file = new File(path);
            File parentFolder = file.getParentFile();
            if (parentFolder == null || parentFolder.exists()
                    || !createParentFolder(file)) {
                throw e;
            }
            Logger.w(TAG, "Re-created missing folder " + parentFolder + " for " + outputUri, e);
            try {
                parcelFileDescriptor = resolver.openFileDescriptor(outputUri, "rw");
            } catch (FileNotFoundException retryException) {
                retryException.addSuppressed(e);
                throw retryException;
            }
        }
        if (parcelFileDescriptor == null) {
            throw new IOException("Unable to open file descriptor from uri " + outputUri);
        }
        return parcelFileDescriptor;
    }

    /** Deletes a MediaStore entry without throwing exceptions on failure. */
    public static void deleteMediaStoreEntryQuietly(@NonNull ContentResolver resolver,
            @NonNull Uri outputUri) {
        try {
            resolver.delete(outputUri, null, null);
        } catch (RuntimeException e) {
            Logger.w(TAG, "Unable to delete MediaStore entry " + outputUri, e);
        }
    }
}
