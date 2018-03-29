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
package androidx.slice.widget;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.collection.ArraySet;
import androidx.lifecycle.LiveData;
import androidx.slice.Slice;
import androidx.slice.SliceManager;
import androidx.slice.SliceSpec;
import androidx.slice.SliceSpecs;

import java.util.Arrays;
import java.util.Set;

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
    public static final SliceSpec OLD_BASIC = new SliceSpec("androidx.app.slice.BASIC", 1);

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    public static final SliceSpec OLD_LIST = new SliceSpec("androidx.app.slice.LIST", 1);

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    public static final Set<SliceSpec> SUPPORTED_SPECS = new ArraySet<>(
            Arrays.asList(SliceSpecs.BASIC, SliceSpecs.LIST, OLD_BASIC, OLD_LIST));

    /**
     * Produces an {@link LiveData} that tracks a Slice for a given Uri. To use
     * this method your app must have the permission to the slice Uri.
     */
    public static LiveData<Slice> fromUri(Context context, Uri uri) {
        return new SliceLiveDataImpl(context.getApplicationContext(), uri);
    }

    /**
     * Produces an {@link LiveData} that tracks a Slice for a given Intent. To use
     * this method your app must have the permission to the slice Uri.
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
            mSliceManager = SliceManager.getInstance(context);
            mUri = uri;
            mIntent = null;
            // TODO: Check if uri points at a Slice?
        }

        private SliceLiveDataImpl(Context context, Intent intent) {
            super();
            mSliceManager = SliceManager.getInstance(context);
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

    private SliceLiveData() {
    }
}
