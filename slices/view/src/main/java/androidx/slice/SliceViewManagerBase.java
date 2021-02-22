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

package androidx.slice;

import static androidx.slice.widget.SliceLiveData.SUPPORTED_SPECS;

import android.content.ContentProviderClient;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.ArrayMap;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

import java.util.concurrent.Executor;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@RequiresApi(19)
public abstract class SliceViewManagerBase extends SliceViewManager {
    private final ArrayMap<Pair<Uri, SliceCallback>, SliceListenerImpl> mListenerLookup =
            new ArrayMap<>();
    protected final Context mContext;

    SliceViewManagerBase(Context context) {
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
        getListener(uri, callback, new SliceListenerImpl(uri, executor, callback)).startListening();
    }

    @Override
    public void unregisterSliceCallback(@NonNull Uri uri, @NonNull SliceCallback callback) {
        synchronized (mListenerLookup) {
            SliceListenerImpl impl = mListenerLookup.remove(new Pair<>(uri, callback));
            if (impl != null) impl.stopListening();
        }
    }


    private SliceListenerImpl getListener(Uri uri, SliceCallback callback,
            SliceListenerImpl listener) {
        Pair<Uri, SliceCallback> key = new Pair<>(uri, callback);
        synchronized (mListenerLookup) {
            SliceListenerImpl oldImpl = mListenerLookup.put(key, listener);
            if (oldImpl != null) {
                oldImpl.stopListening();
            }
        }
        return listener;
    }

    private class SliceListenerImpl {

        Uri mUri;
        final Executor mExecutor;
        final SliceCallback mCallback;
        private boolean mPinned;

        SliceListenerImpl(Uri uri, Executor executor, SliceCallback callback) {
            mUri = uri;
            mExecutor = executor;
            mCallback = callback;
        }

        void startListening() {
            ContentProviderClient provider =
                    mContext.getContentResolver().acquireContentProviderClient(mUri);
            if (provider != null) {
                provider.release();
                mContext.getContentResolver().registerContentObserver(mUri, true, mObserver);
                tryPin();
            }
        }

        void tryPin() {
            if (!mPinned) {
                try {
                    pinSlice(mUri);
                    mPinned = true;
                } catch (SecurityException e) {
                    // No permission currently.
                }
            }
        }

        void stopListening() {
            mContext.getContentResolver().unregisterContentObserver(mObserver);
            if (mPinned) {
                unpinSlice(mUri);
                mPinned = false;
            }
        }

        final Runnable mUpdateSlice = new Runnable() {
            @Override
            public void run() {
                tryPin();
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
            @SuppressWarnings("deprecation") /* AsyncTask */
            public void onChange(boolean selfChange) {
                android.os.AsyncTask.execute(mUpdateSlice);
            }
        };
    }
}
