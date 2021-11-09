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

package androidx.camera.core.internal.utils;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.Logger;
import androidx.core.util.Preconditions;

/**
 * Utility class for video recording related operations.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class VideoUtil {
    private static final String TAG = "VideoUtil";

    private VideoUtil(){}

    /** Gets the absolute path from a Uri. */
    @Nullable
    @SuppressWarnings("deprecation")
    public static String getAbsolutePathFromUri(@NonNull ContentResolver resolver,
            @NonNull Uri contentUri) {
        Cursor cursor = null;
        try {
            // We should include in any Media collections.
            String[] proj;
            int columnIndex;
            proj = new String[]{MediaStore.Video.Media.DATA};
            cursor = resolver.query(contentUri, proj, null, null, null);
            cursor = Preconditions.checkNotNull(cursor);
            columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(columnIndex);
        } catch (RuntimeException e) {
            Logger.e(TAG, String.format(
                    "Failed in getting absolute path for Uri %s with Exception %s",
                    contentUri.toString(), e.toString()));
            return "";
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
