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

import static androidx.app.slice.widget.SliceLiveData.SUPPORTED_SPECS;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.util.ArrayMap;
import android.util.Pair;

import java.util.List;
import java.util.concurrent.Executor;

import androidx.app.slice.compat.SliceProviderCompat;
import androidx.app.slice.widget.SliceLiveData;


/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class SliceManagerCompat extends SliceManager {
    private final ArrayMap<Pair<Uri, SliceCallback>, SliceListenerImpl> mListenerLookup =
            new ArrayMap<>();
    private final Context mContext;

    SliceManagerCompat(Context context) {
        mContext = context;
    }

    @Override
    public void registerSliceCallback(@NonNull Uri uri, @NonNull SliceCallback callback) {
        final Handler h = new Handler(Looper.getMainLooper());
        registerSliceCallback(uri, new Executor() {
            @Override
            public void execute(@NonNull Runnable command) {
                h.post(command);
            }
        }, callback);
    }

    @Override
    public void registerSliceCallback(@NonNull Uri uri, @NonNull Executor executor,
            @NonNull SliceCallback callback) {
        pinSlice(uri);
        getListener(uri, callback, new SliceListenerImpl(uri, executor, callback)).startListening();
    }

    @Override
    public void unregisterSliceCallback(@NonNull Uri uri, @NonNull SliceCallback callback) {
        unpinSlice(uri);
        SliceListenerImpl impl = mListenerLookup.remove(new Pair<>(uri, callback));
        if (impl != null) impl.stopListening();
    }

    @Override
    public void pinSlice(@NonNull Uri uri) {
        SliceProviderCompat.pinSlice(mContext, uri, SliceLiveData.SUPPORTED_SPECS);
    }

    @Override
    public void unpinSlice(@NonNull Uri uri) {
        SliceProviderCompat.unpinSlice(mContext, uri, SliceLiveData.SUPPORTED_SPECS);
    }

    @Override
    public @NonNull List<SliceSpec> getPinnedSpecs(@NonNull Uri uri) {
        return SliceProviderCompat.getPinnedSpecs(mContext, uri);
    }

    @Nullable
    @Override
    public Slice bindSlice(@NonNull Uri uri) {
        return SliceProviderCompat.bindSlice(mContext, uri, SUPPORTED_SPECS);
    }

    @Nullable
    @Override
    public Slice bindSlice(@NonNull Intent intent) {
        return SliceProviderCompat.bindSlice(mContext, intent, SUPPORTED_SPECS);
    }

    private SliceListenerImpl getListener(Uri uri, SliceCallback callback,
            SliceListenerImpl listener) {
        Pair<Uri, SliceCallback> key = new Pair<>(uri, callback);
        if (mListenerLookup.containsKey(key)) {
            mListenerLookup.get(key).stopListening();
        }
        mListenerLookup.put(key, listener);
        return listener;
    }

    private class SliceListenerImpl {

        private Uri mUri;
        private final Executor mExecutor;
        private final SliceCallback mCallback;

        SliceListenerImpl(Uri uri, Executor executor, SliceCallback callback) {
            mUri = uri;
            mExecutor = executor;
            mCallback = callback;
        }

        void startListening() {
            mContext.getContentResolver().registerContentObserver(mUri, true, mObserver);
        }

        void stopListening() {
            mContext.getContentResolver().unregisterContentObserver(mObserver);
        }

        private final Runnable mUpdateSlice = new Runnable() {
            @Override
            public void run() {
                final Slice s = Slice.bindSlice(mContext, mUri, SUPPORTED_SPECS);
                mExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.onSliceUpdated(s);
                    }
                });
            }
        };

        private final ContentObserver mObserver = new ContentObserver(
                new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange) {
                AsyncTask.execute(mUpdateSlice);
            }
        };
    }
}
