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

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.ArrayMap;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.util.concurrent.Executor;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public abstract class SliceManagerBase extends SliceManager {
    private final ArrayMap<Pair<Uri, SliceCallback>, SliceListenerImpl> mListenerLookup =
            new ArrayMap<>();
    protected final Context mContext;

    SliceManagerBase(Context context) {
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
        SliceListenerImpl impl = mListenerLookup.remove(new Pair<>(uri, callback));
        if (impl != null) impl.stopListening();
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
        private boolean mPinned;

        SliceListenerImpl(Uri uri, Executor executor, SliceCallback callback) {
            mUri = uri;
            mExecutor = executor;
            mCallback = callback;
        }

        void startListening() {
            mContext.getContentResolver().registerContentObserver(mUri, true, mObserver);
            tryPin();
        }

        private void tryPin() {
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

        private final Runnable mUpdateSlice = new Runnable() {
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
            public void onChange(boolean selfChange) {
                AsyncTask.execute(mUpdateSlice);
            }
        };
    }
}
