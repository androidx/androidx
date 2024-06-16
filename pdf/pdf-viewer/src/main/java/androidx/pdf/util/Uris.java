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

import static android.content.ContentResolver.SCHEME_CONTENT;
import static android.content.ContentResolver.SCHEME_FILE;

import android.content.ContentResolver;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/**
 * Helpers with {@link Uri}s
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class Uris {
    private Uris() {
    }

    public static final String SCHEME_HTTP = "http";
    public static final String SCHEME_HTTPS = "https";
    private static final String DATA_DIR = "data/data/";

    /** Returns true if the Uri is an 'http:' one. */
    public static boolean isHttp(@NonNull Uri uri) {
        return SCHEME_HTTP.equals(uri.getScheme());
    }

    /** Returns true if the Uri is an 'https:' one. */
    public static boolean isHttps(@NonNull Uri uri) {
        return SCHEME_HTTPS.equals(uri.getScheme());
    }

    /** Returns true if the Uri is a remote (http/s) one. */
    public static boolean isRemote(@NonNull Uri uri) {
        String scheme = uri.getScheme();
        return SCHEME_HTTP.equals(scheme) || SCHEME_HTTPS.equals(scheme);
    }

    /** Returns true if the Uri is a local (on-device) one. */
    public static boolean isLocal(@NonNull Uri uri) {
        String scheme = uri.getScheme();
        return SCHEME_FILE.equals(scheme) || SCHEME_CONTENT.equals(scheme);
    }

    /** Returns true if the Uri is a 'content:' one. */
    public static boolean isContentUri(@NonNull Uri uri) {
        return SCHEME_CONTENT.equals(uri.getScheme());
    }

    /** Returns true if the Uri is a 'file:' one. */
    public static boolean isFileUri(@NonNull Uri uri) {
        return SCHEME_FILE.equals(uri.getScheme());
    }

    /**
     * Extract a content-type from the given {@link Uri} by mapping its file extension to a known
     * mime-type. This is based on the Uri only, it doesn't open any connection.
     */
    @Nullable
    public static String extractContentType(@NonNull Uri uri) {
        // Note: MimeTypeMap.getFileExtensionFromUrl(path); fails on unusual characters in path.
        String name = uri.getLastPathSegment();
        if (name != null) {
            int dot = name.lastIndexOf('.');
            if (dot >= 0) {
                String extension = name.substring(dot + 1).toLowerCase();
                return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            }
        }
        return null;
    }

    /**
     * Extracts a file name from the given {@link Uri} - either its last segment of the whole Uri.
     * This is based on the Uri only, it doesn't open any connection.
     */
    @NonNull
    public static String extractFileName(@NonNull Uri uri) {
        String name = uri.getLastPathSegment();
        if (name == null) {
            name = uri.toString();
        }
        return name;
    }

    /**
     * Extracts the name of the file from the Uri - either the last segment of the Uri, or for
     * content
     * Uris, the name must be queried from the contentResolver.
     */
    @NonNull
    public static String extractName(@NonNull Uri uri, @NonNull ContentResolver contentResolver) {
        if (Uris.isContentUri(uri)) {
            return ContentUriOpener.extractContentName(contentResolver, uri);
        } else {
            return Uris.extractFileName(uri);
        }
    }

    /**
     * Returns true if the Uri is a 'file:' one, and if it points to a file in the
     * data/data/package.name directory. We should not support these uris, as the request could be a
     * QUICK_VIEW intent from a thirdparty app.
     */
    public static boolean isFileUriInSamePackageDataDir(@NonNull Uri uri) {
        return isFileUri(uri)
                && uri.getPath() != null
                && uri.getPath().contains(DATA_DIR + AppInfo.get().getPackageName());
    }
}
