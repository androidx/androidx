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

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.NonNull;

import java.util.Arrays;
import java.util.List;

import androidx.app.slice.Slice;
import androidx.app.slice.SliceSpec;

/**
 * Class with factory methods for creating LiveData that observes slices.
 *
 * @see #fromUri(Context, Uri)
 * @see LiveData
 */
public final class SliceLiveData {

    private static final List<SliceSpec> SUPPORTED_SPECS = Arrays.asList();

    /**
     * Produces an {@link LiveData} that tracks a Slice for a given Uri. To use
     * this method your app must have the permission to the slice Uri or hold
     * {@link android.Manifest.permission#BIND_SLICE}).
     */
    public static LiveData<Slice> fromUri(Context context, Uri uri) {
        return new SliceLiveDataImpl(context.getApplicationContext(), uri);
    }

    /**
     * Produces an {@link LiveData} that tracks a Slice for a given Intent. To use
     * this method your app must have the permission to the slice Uri or hold
     * {@link android.Manifest.permission#BIND_SLICE}).
     */
    public static LiveData<Slice> fromIntent(@NonNull Context context, @NonNull Intent intent) {
        return new SliceLiveDataImpl(context.getApplicationContext(), intent);
    }

    private static class SliceLiveDataImpl extends LiveData<Slice> {
        private final Context mContext;
        private final Intent mIntent;
        private Uri mUri;

        private SliceLiveDataImpl(Context context, Uri uri) {
            super();
            mContext = context;
            mUri = uri;
            mIntent = null;
            // TODO: Check if uri points at a Slice?
        }

        private SliceLiveDataImpl(Context context, Intent intent) {
            super();
            mContext = context;
            mUri = null;
            mIntent = intent;
        }

        @Override
        protected void onActive() {
            AsyncTask.execute(mUpdateSlice);
            if (mUri != null) {
                mContext.getContentResolver().registerContentObserver(mUri, false, mObserver);
            }
        }

        @Override
        protected void onInactive() {
            if (mUri != null) {
                mContext.getContentResolver().unregisterContentObserver(mObserver);
            }
        }

        private final Runnable mUpdateSlice = new Runnable() {
            @Override
            public void run() {
                Slice s = mUri != null ? Slice.bindSlice(mContext, mUri)
                        : Slice.bindSlice(mContext, mIntent);
                if (mUri == null && s != null) {
                    mContext.getContentResolver().registerContentObserver(s.getUri(),
                            false, mObserver);
                    mUri = s.getUri();
                }
                postValue(s);
            }
        };

        private final ContentObserver mObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                AsyncTask.execute(mUpdateSlice);
            }
        };
    }
}
