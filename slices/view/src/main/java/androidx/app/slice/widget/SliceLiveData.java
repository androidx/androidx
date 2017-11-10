/*
 * Copyright 2017 The Android Open Source Project
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
package androidx.app.slice.widget;

import android.app.slice.Slice;
import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;

/**
 * Class with factory methods for creating LiveData that observes slices.
 *
 * @see #fromUri(Context, Uri)
 * @see LiveData
 */
public final class SliceLiveData {

    /**
     * Produces an {@link LiveData} that tracks a Slice for a given Uri. To use
     * this method your app must have the permission to the slice Uri or hold
     * {@link android.Manifest.permission#BIND_SLICE}).
     */
    public static LiveData<Slice> fromUri(Context context, Uri uri) {
        return new SliceLiveDataImpl(context.getApplicationContext(), uri);
    }

    private static class SliceLiveDataImpl extends LiveData<Slice> {
        private final Uri mUri;
        private final Context mContext;

        private SliceLiveDataImpl(Context context, Uri uri) {
            super();
            mContext = context;
            mUri = uri;
            // TODO: Check if uri points at a Slice?
        }

        @Override
        protected void onActive() {
            AsyncTask.execute(this::updateSlice);
            mContext.getContentResolver().registerContentObserver(mUri, false, mObserver);
        }

        @Override
        protected void onInactive() {
            mContext.getContentResolver().unregisterContentObserver(mObserver);
        }

        private void updateSlice() {
            postValue(Slice.bindSlice(mContext.getContentResolver(), mUri));
        }

        private final ContentObserver mObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                AsyncTask.execute(SliceLiveDataImpl.this::updateSlice);
            }
        };
    }
}
