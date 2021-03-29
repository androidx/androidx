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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Logger;

/**
 * Utility class for output related operations.
 */
public final class OutputUtil {
    private static final String TAG = "OutputUtil";

    private OutputUtil(){}

    /** Gets the absolute path from a Uri. */
    @Nullable
    public static String getAbsolutePathFromUri(@NonNull ContentResolver resolver,
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
            cursor.moveToFirst();
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
}
