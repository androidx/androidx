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

package androidx.pdf.fetcher;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A very simple disk cache that caches file contents using an {@link Uri} and mimeType as key.
 */
@SuppressLint("BanConcurrentHashMap")
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class DiskCache {

    private static final String TAG = DiskCache.class.getSimpleName();

    /** The folder where long term cache lives, in the app's cache directory. */
    private static final String LONG_TERM_CACHE_DIR = "projector";

    /** The root folder where this cache lives, in the app's cache directory. */
    private static final String SUB_CACHE_DIR = "projector-disk";

    /** A temporary folder which contains incomplete files until they've been fully fetched. */
    private static final String TMP_CACHE_DIR = "projector-tmp";

    private final File mCacheRoot;
    private final File mTmpCacheRoot;

    /** Catalog of entries in this cache, with their mime types. */
    // TODO: This should be persisted together with the cached files.
    private final Map<Uri, String> mEntries = new ConcurrentHashMap<>();

    public DiskCache(@NonNull Context context) {
        mCacheRoot = getDiskCacheDir(context);
        mTmpCacheRoot = getTmpCacheDir(context);
        mCacheRoot.mkdir();
        mTmpCacheRoot.mkdir();
    }

    /** Delete the contents of the cache directories, without deleting the actual directories. */
    public void cleanup() {
        clearDirectory(mCacheRoot);
        clearDirectory(mTmpCacheRoot);
        mEntries.clear();
    }

    /** Returns the cached MimeType of this Uri. */
    @Nullable
    public String getCachedMimeType(@NonNull Uri uri) {
        // TODO: this can often be null, since entries are not persisted to disk.
        return mEntries.get(uri);
    }

    private void clearDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            for (int i = 0; i < files.length; ++i) {
                files[i].delete();
            }
        }
    }

    /**
     * Returns a directory for a long term un-managed cache.
     *
     * <p>This dir is not guaranteed to exist. Users of this directory need to ensure that they
     * clean up their data overtime.
     */
    @NonNull
    public static File getLongTermCacheDir(@NonNull Context context) {
        return new File(context.getCacheDir(), LONG_TERM_CACHE_DIR);
    }

    // TODO: Make this private. Currently used by FileProvider to access a cached file.
    static File getDiskCacheDir(Context context) {
        return new File(context.getCacheDir(), SUB_CACHE_DIR);
    }

    private static File getTmpCacheDir(Context context) {
        return new File(context.getCacheDir(), TMP_CACHE_DIR);
    }
}