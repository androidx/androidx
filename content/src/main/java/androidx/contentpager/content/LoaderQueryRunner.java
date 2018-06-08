/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.contentpager.content;

import static androidx.core.util.Preconditions.checkArgument;

import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

/**
 * A {@link ContentPager.QueryRunner} that executes queries using a {@link LoaderManager}.
 * Use this when preparing {@link ContentPager} to run in an Activity or Fragment scope.
 */
public final class LoaderQueryRunner implements ContentPager.QueryRunner {

    private static final boolean DEBUG = false;
    private static final String TAG = "LoaderQueryRunner";
    private static final String CONTENT_URI_KEY = "contentUri";

    private final Context mContext;
    private final LoaderManager mLoaderMgr;

    public LoaderQueryRunner(@NonNull Context context, @NonNull LoaderManager loaderMgr) {
        mContext = context;
        mLoaderMgr = loaderMgr;
    }

    @Override
    @SuppressWarnings("unchecked")  // feels spurious. But can't commit line :80 w/o this.
    public void query(final @NonNull Query query, @NonNull final Callback callback) {
        if (DEBUG) Log.d(TAG, "Handling query: " + query);

        LoaderCallbacks callbacks = new LoaderCallbacks<Cursor>() {
            @Override
            public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
                if (DEBUG) Log.i(TAG, "Loading results for query: " + query);
                checkArgument(id == query.getId(), "Id doesn't match query id.");

                return new android.content.CursorLoader(mContext) {
                    @Override
                    public Cursor loadInBackground() {
                        return callback.runQueryInBackground(query);
                    }
                };
            }

            @Override
            public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
                if (DEBUG) Log.i(TAG, "Finished loading: " + query);
                mLoaderMgr.destroyLoader(query.getId());
                callback.onQueryFinished(query, cursor);
            }

            @Override
            public void onLoaderReset(Loader<Cursor> loader) {
                if (DEBUG) Log.w(TAG, "Ignoring loader reset for query: " + query);
            }
        };

        mLoaderMgr.restartLoader(query.getId(), null, callbacks);
    }

    @Override
    public boolean isRunning(@NonNull Query query) {
        Loader<Cursor> loader = mLoaderMgr.getLoader(query.getId());
        return loader != null && loader.isStarted();
        // Hmm, when exactly would the loader not be started? Does it imply that it will
        // be starting at some point?
    }

    @Override
    public void cancel(@NonNull Query query) {
        mLoaderMgr.destroyLoader(query.getId());
    }
}
