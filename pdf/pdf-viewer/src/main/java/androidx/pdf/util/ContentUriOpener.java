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

package androidx.pdf.util;

import android.content.ContentResolver;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.MediaColumns;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.io.FileNotFoundException;

/**
 * Opens content {@link Uri}s. Adds support for contents stored in the Media Provider (query name).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ContentUriOpener {
    private static final String TAG = ContentUriOpener.class.getSimpleName();

    private static final String[] NAME_COLUMNS = {MediaColumns.DISPLAY_NAME, MediaColumns.TITLE};

    private final ContentResolver mContentResolver;

    public ContentUriOpener(@NonNull ContentResolver contentResolver) {
        this.mContentResolver = contentResolver;
    }

    /** Opens an image preview (of the given size) of this content. */
    @NonNull
    public AssetFileDescriptor openPreview(@NonNull Uri contentUri, @NonNull Point size)
            throws FileNotFoundException {
        Preconditions.checkNotRunOnUIThread();
        Bundle extraSize = new Bundle();
        extraSize.putParcelable("android.content.extra.SIZE", size);
        return mContentResolver.openTypedAssetFileDescriptor(contentUri, "image/*", extraSize);
    }

    /**
     * Returns Exif orientation rotation value for this content.
     *
     * <p>Normally, we wouldn't need to get Exif orientation at this layer, as we would read and
     * apply
     * Exif data during bitmap decoding. However, when we request a lo-res thumbnail from a
     * contentResolver, the Exif data of the original file is not transferred to the thumbnail,
     * so we
     * use this to get the Exif orientation from the original file and manually apply it to the
     * thumbnail.
     */
    public int getExifOrientation(@NonNull Uri contentUri) {
        Preconditions.checkNotRunOnUIThread();
        return ExifThumbnailUtils.getExifOrientation(contentUri, mContentResolver);
    }

    /**
     * Opens this content as a specified content-type.
     *
     * @param contentUri  the content Uri
     * @param contentType the requested content type. If null, will use the default.
     */
    @NonNull
    public AssetFileDescriptor open(@NonNull Uri contentUri, @NonNull String contentType)
            throws FileNotFoundException {
        Preconditions.checkNotRunOnUIThread();
        if (contentType == null) {
            contentType = getContentType(contentUri);
        }
        return mContentResolver.openTypedAssetFileDescriptor(contentUri, contentType, null);
    }

    /**
     *
     */
    @Nullable
    public String getContentType(@NonNull Uri contentUri) {
        try {
            String[] availableTypes = mContentResolver.getStreamTypes(contentUri, "*/*");
            String declaredType = mContentResolver.getType(contentUri);
            // Sometimes the declared type is actually not available, then pick an available type
            // instead.
            String useType = null;
            if (availableTypes != null) {
                for (String type : availableTypes) {
                    if (useType == null) {
                        useType = type;
                    } else if (type.equals(declaredType)) {
                        useType = declaredType;
                    }
                }
            }
            if (useType == null) {
                useType = declaredType;
            }
            return useType;
        } catch (SecurityException se) {
            return null;
        }
    }

    /**
     * Returns the various content types that this content can be streamed as. If the content
     * provider
     * doesn't declare any (usual for older ones), the main content type is returned, but there
     * is no
     * guarantee the corresponding content can be streamed.
     */
    @Nullable
    public String[] getAvailableTypes(@NonNull Uri contentUri) {
        Preconditions.checkArgument(Uris.isContentUri(contentUri),
                "Can't handle Uri " + contentUri.getScheme());
        try {
            String[] streamTypes = mContentResolver.getStreamTypes(contentUri, "*/*");
            if (streamTypes != null) {
                return streamTypes;
            } else {
                return new String[]{mContentResolver.getType(contentUri)};
            }
        } catch (SecurityException se) {
            return new String[]{};
        }
    }

    /**
     *
     */
    @Nullable
    public static String extractContentName(@NonNull ContentResolver contentResolver,
            @NonNull Uri contentUri) {
        Cursor cursor = null;
        String[] queryColumn = new String[1];
        String name = null;
        for (String colName : NAME_COLUMNS) {
            queryColumn[0] = colName;
            try {
                cursor = contentResolver.query(contentUri, queryColumn, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    name = extractColumn(cursor, colName);
                    if (name != null) {
                        break;
                    }
                }
            } catch (Exception e) {
                // Misbehaved app!
                // TODO: Rethrow exception or return an error code
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        return name;
    }

    @Nullable
    private static String extractColumn(Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndex(columnName);
        if (columnIndex >= 0) {
            String result = cursor.getString(columnIndex);
            if (!TextUtils.isEmpty(result)) {
                return result;
            }
        }
        return null;
    }
}
