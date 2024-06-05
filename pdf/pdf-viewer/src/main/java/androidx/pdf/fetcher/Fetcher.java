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

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.pdf.data.ContentOpenable;
import androidx.pdf.data.FileOpenable;
import androidx.pdf.data.FutureValue;
import androidx.pdf.data.Openable;
import androidx.pdf.data.Opener;
import androidx.pdf.data.UiFutureValues;
import androidx.pdf.models.Dimensions;
import androidx.pdf.util.Preconditions;
import androidx.pdf.util.StrictModeUtils;
import androidx.pdf.util.Uris;

import java.io.FileNotFoundException;

/**
 * This class resolves {@link Uri}s (and similar) into usable data ({@link Openable}s).
 * It handles the schemes that {@link java.net.URLConnection} does, plus "content":
 * <ul>
 * <li>network (http:)
 * <li>local content provider (e.g. DownloadMgr) (content:)
 * <li>local file, e.g. on sdcard (file:)
 * </ul>
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class Fetcher extends Opener {
    private static final String TAG = Fetcher.class.getSimpleName();

    private static final int DEFAULT_NUM_THREADS = 3;

    private final DiskCache mCache;

    /**
     *
     */
    @NonNull
    public static Fetcher build(@NonNull Context context) {
        return build(context, DEFAULT_NUM_THREADS);
    }

    /**
     *
     */
    @NonNull
    public static Fetcher build(@NonNull Context context, int numThreads) {
        // TODO: StrictMode: disk read 144ms
        return StrictModeUtils.bypassAndReturn(
                () -> new Fetcher(context, new DiskCache(context), numThreads));
    }

    protected Fetcher(@NonNull Context ctx, @NonNull DiskCache diskCache, int numThreads) {
        super(ctx);
        this.mCache = diskCache;
    }

    @NonNull
    public DiskCache getCache() {
        return mCache;
    }

    /** Loads the contents of a local {@link Uri} into an {@link Openable}. */
    @NonNull
    public FutureValue<Openable> loadLocal(@NonNull Uri localUri) {
        Preconditions.checkArgument(Uris.isLocal(localUri),
                "Use fetch() for http URLs " + localUri);
        return Uris.isContentUri(localUri) ? loadContent(localUri) : loadFile(localUri);
    }

    /** Fetches (opens) a content Uri into an {@link Openable}. */
    @NonNull
    public FutureValue<Openable> loadContent(@NonNull Uri contentUri) {
        String useType = getContentType(contentUri);
        return loadContent(contentUri, useType);
    }

    /** Loads (prepares for opening) a content Uri into an {@link Openable}. */
    @NonNull
    public FutureValue<Openable> loadContent(@NonNull Uri contentUri, @NonNull String useType) {
        Openable content = new ContentOpenable(contentUri, useType);
        return UiFutureValues.immediateValue(content);
    }

    /** Loads (prepares for opening) a content Uri into an {@link Openable}. */
    @NonNull
    public FutureValue<Openable> loadContent(@NonNull Uri contentUri, @NonNull Dimensions size) {
        Openable content = new ContentOpenable(contentUri, size);
        return UiFutureValues.immediateValue(content);
    }

    /** Loads (prepares for opening) a file Uri into an {@link Openable}. */
    @NonNull
    public FutureValue<Openable> loadFile(@NonNull Uri fileUri) {
        try {
            Openable content = new FileOpenable(fileUri);
            return UiFutureValues.immediateValue(content);
        } catch (FileNotFoundException e) {
            return UiFutureValues.immediateFail(e);
        }
    }
}
