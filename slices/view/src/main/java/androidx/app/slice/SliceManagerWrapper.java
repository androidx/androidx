/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.app.slice;

import static androidx.app.slice.SliceConvert.unwrap;
import static androidx.app.slice.widget.SliceLiveData.SUPPORTED_SPECS;

import android.app.slice.Slice;
import android.app.slice.SliceSpec;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;

import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@RequiresApi(api = 28)
class SliceManagerWrapper extends SliceManager {
    private final android.app.slice.SliceManager mManager;
    private final WeakHashMap<SliceCallback, android.app.slice.SliceManager.SliceCallback>
            mCallbacks = new WeakHashMap<>();
    private final List<SliceSpec> mSpecs;
    private final Context mContext;

    SliceManagerWrapper(Context context) {
        this(context, context.getSystemService(android.app.slice.SliceManager.class));
    }

    SliceManagerWrapper(Context context, android.app.slice.SliceManager manager) {
        mContext = context;
        mManager = manager;
        mSpecs = unwrap(SUPPORTED_SPECS);
    }

    @Override
    public void registerSliceCallback(@NonNull Uri uri,
            @NonNull SliceCallback callback) {
        mManager.registerSliceCallback(uri, addCallback(callback), mSpecs);
    }

    @Override
    public void registerSliceCallback(@NonNull Uri uri, @NonNull Executor executor,
            @NonNull SliceCallback callback) {
        mManager.registerSliceCallback(uri, addCallback(callback), mSpecs, executor);
    }

    @Override
    public void unregisterSliceCallback(@NonNull Uri uri,
            @NonNull SliceCallback callback) {
        mManager.unregisterSliceCallback(uri, mCallbacks.get(callback));
    }

    @Override
    public void pinSlice(@NonNull Uri uri) {
        mManager.pinSlice(uri, mSpecs);
    }

    @Override
    public void unpinSlice(@NonNull Uri uri) {
        mManager.unpinSlice(uri);
    }

    @Override
    public @NonNull List<androidx.app.slice.SliceSpec> getPinnedSpecs(@NonNull Uri uri) {
        return SliceConvert.wrap(mManager.getPinnedSpecs(uri));
    }

    @Nullable
    @Override
    public androidx.app.slice.Slice bindSlice(@NonNull Uri uri) {
        return SliceConvert.wrap(android.app.slice.Slice.bindSlice(
                mContext.getContentResolver(), uri, unwrap(SUPPORTED_SPECS)));
    }

    @Nullable
    @Override
    public androidx.app.slice.Slice bindSlice(@NonNull Intent intent) {
        return SliceConvert.wrap(android.app.slice.Slice.bindSlice(
                mContext, intent, unwrap(SUPPORTED_SPECS)));
    }

    private android.app.slice.SliceManager.SliceCallback addCallback(final SliceCallback callback) {
        android.app.slice.SliceManager.SliceCallback ret = mCallbacks.get(callback);
        if (ret == null) {
            ret = new android.app.slice.SliceManager.SliceCallback() {
                @Override
                public void onSliceUpdated(Slice s) {
                    callback.onSliceUpdated(SliceConvert.wrap(s));
                }
            };
            mCallbacks.put(callback, ret);
        }
        return ret;
    }
}
