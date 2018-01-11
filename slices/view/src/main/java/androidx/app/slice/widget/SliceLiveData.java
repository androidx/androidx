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

import static android.support.annotation.RestrictTo.Scope.LIBRARY;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import java.util.Arrays;
import java.util.List;

import androidx.app.slice.Slice;
import androidx.app.slice.SliceManager;
import androidx.app.slice.SliceSpec;
import androidx.app.slice.SliceSpecs;

/**
 * Class with factory methods for creating LiveData that observes slices.
 *
 * @see #fromUri(Context, Uri)
 * @see LiveData
 */
public final class SliceLiveData {

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    public static final List<SliceSpec> SUPPORTED_SPECS = Arrays.asList(SliceSpecs.BASIC,
            SliceSpecs.LIST);

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
        private final Intent mIntent;
        private final SliceManager mSliceManager;
        private Uri mUri;

        private SliceLiveDataImpl(Context context, Uri uri) {
            super();
            mSliceManager = SliceManager.get(context);
            mUri = uri;
            mIntent = null;
            // TODO: Check if uri points at a Slice?
        }

        private SliceLiveDataImpl(Context context, Intent intent) {
            super();
            mSliceManager = SliceManager.get(context);
            mUri = null;
            mIntent = intent;
        }

        @Override
        protected void onActive() {
            AsyncTask.execute(mUpdateSlice);
            if (mUri != null) {
                mSliceManager.registerSliceCallback(mUri, mSliceCallback);
            }
        }

        @Override
        protected void onInactive() {
            if (mUri != null) {
                mSliceManager.unregisterSliceCallback(mUri, mSliceCallback);
            }
        }

        private final Runnable mUpdateSlice = new Runnable() {
            @Override
            public void run() {
                Slice s = mUri != null ? mSliceManager.bindSlice(mUri)
                        : mSliceManager.bindSlice(mIntent);
                if (mUri == null && s != null) {
                    mUri = s.getUri();
                    mSliceManager.registerSliceCallback(mUri, mSliceCallback);
                }
                postValue(s);
            }
        };

        private final SliceManager.SliceCallback mSliceCallback = new SliceManager.SliceCallback() {
            @Override
            public void onSliceUpdated(@NonNull Slice s) {
                postValue(s);
            }
        };
    }
}
