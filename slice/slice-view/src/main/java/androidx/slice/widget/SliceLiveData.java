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
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.collection.ArraySet;
import androidx.lifecycle.LiveData;
import androidx.slice.Slice;
import androidx.slice.SliceItem;
import androidx.slice.SliceMetadata;
import androidx.slice.SliceSpec;
import androidx.slice.SliceSpecs;
import androidx.slice.SliceStructure;
import androidx.slice.SliceUtils;
import androidx.slice.SliceViewManager;
import androidx.slice.core.SliceQuery;

import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Class with factory methods for creating LiveData that observes slices.
 *
 * @see #fromUri(Context, Uri)
 * @see LiveData
 */
@RequiresApi(19)
public final class SliceLiveData {
    private static final String TAG = "SliceLiveData";

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
            Arrays.asList(SliceSpecs.BASIC, SliceSpecs.LIST, SliceSpecs.LIST_V2, OLD_BASIC,
                    OLD_LIST));

    /**
     * Produces a {@link LiveData} that tracks a Slice for a given Uri. To use
     * this method your app must have the permission to the slice Uri.
     */
    public static @NonNull LiveData<Slice> fromUri(@NonNull Context context, @NonNull Uri uri) {
        return new SliceLiveDataImpl(context.getApplicationContext(), uri, null);
    }

    /**
     * Produces a {@link LiveData} that tracks a Slice for a given Uri. To use
     * this method your app must have the permission to the slice Uri.
     */
    public static @NonNull LiveData<Slice> fromUri(
            @NonNull Context context, @NonNull Uri uri, @Nullable OnErrorListener listener) {
        return new SliceLiveDataImpl(context.getApplicationContext(), uri, listener);
    }

    /**
     * Produces a {@link LiveData} that tracks a Slice for a given Intent. To use
     * this method your app must have the permission to the slice Uri.
     */
    public static @NonNull LiveData<Slice> fromIntent(@NonNull Context context,
            @NonNull Intent intent) {
        return new SliceLiveDataImpl(context.getApplicationContext(), intent, null);
    }

    /**
     * Produces a {@link LiveData} that tracks a Slice for a given Intent. To use
     * this method your app must have the permission to the slice Uri.
     */
    public static @NonNull LiveData<Slice> fromIntent(@NonNull Context context,
            @NonNull Intent intent, @Nullable OnErrorListener listener) {
        return new SliceLiveDataImpl(context.getApplicationContext(), intent, listener);
    }

    /**
     * Produces a {@link LiveData} that tracks a Slice for a given InputStream. To use
     * this method your app must have the permission to the slice Uri.
     *
     * This will not ask the hosting app for a slice immediately, instead it will display
     * the slice passed in through the input. When the user interacts with the slice, then
     * the app will be started to obtain the current slice and trigger the user action.
     */
    public static @NonNull LiveData<Slice> fromStream(@NonNull Context context,
            @NonNull InputStream input, OnErrorListener listener) {
        return fromStream(context, SliceViewManager.getInstance(context), input, listener);
    }

    /**
     * Same as {@link #fromStream(Context, InputStream, OnErrorListener)} except returns
     * as type {@link CachedSliceLiveData}.
     */
    public static @NonNull CachedSliceLiveData fromCachedSlice(@NonNull Context context,
            @NonNull InputStream input, OnErrorListener listener) {
        return fromStream(context, SliceViewManager.getInstance(context), input, listener);
    }

    /**
     * Version for testing
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @NonNull
    public static CachedSliceLiveData fromStream(@NonNull Context context,
            SliceViewManager manager, @NonNull InputStream input, OnErrorListener listener) {
        return new CachedSliceLiveData(context, manager, input, listener);
    }

    /**
     * Implementation of {@link LiveData}<Slice> that provides controls over how
     * cached vs live slices work.
     */
    public static class CachedSliceLiveData extends LiveData<Slice> {
        final SliceViewManager mSliceViewManager;
        private final OnErrorListener mListener;
        final Context mContext;
        private InputStream mInput;
        Uri mUri;
        private boolean mActive;
        List<Uri> mPendingUri = new ArrayList<>();
        private boolean mLive;
        SliceStructure mStructure;
        List<Context> mPendingContext = new ArrayList<>();
        List<Intent> mPendingIntent = new ArrayList<>();
        private boolean mSliceCallbackRegistered;
        private boolean mInitialSliceLoaded;

        CachedSliceLiveData(final Context context, final SliceViewManager manager,
                final InputStream input, final OnErrorListener listener) {
            super();
            mContext = context;
            mSliceViewManager = manager;
            mUri = null;
            mListener = listener;
            mInput = input;
        }

        /**
         * Generally the InputStream are parsed asynchronously once the
         * LiveData goes into the active state. When this is called, regardless of
         * state, the slice will be read from the input stream and then the input
         * stream's reference will be released when finished.
         * <p>
         * Calling parseStream() multiple times or after the stream has already
         * been parsed asynchronously will have no effect.
         */
        public void parseStream() {
            loadInitialSlice();
        }

        /**
         * Moves this CachedSliceLiveData into a "live" state, causing the providing
         * app to start up and provide an up to date version of the slice. After
         * calling this method the slice will always be pinned as long as this
         * LiveData is in the active state.
         * <p>
         * If the slice has already received a click or goLive() has already been
         * called, then this method will have no effect.
         * <p>
         * Once goLive() has been called, there is no way to reverse it, this LiveData
         * will then behave the same way as one created using {@link #fromUri(Context, Uri)}.
         */
        public void goLive() {
            // Go live with no click.
            goLive(null, null, null);
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        @SuppressWarnings("deprecation") /* AsyncTask */
        protected synchronized void loadInitialSlice() {
            if (mInitialSliceLoaded) {
                return;
            }
            try {
                Slice s = SliceUtils.parseSlice(mContext, mInput, "UTF-8",
                        new SliceUtils.SliceActionListener() {
                            @Override
                            public void onSliceAction(Uri actionUri, Context context,
                                    Intent intent) {
                                goLive(actionUri, context, intent);
                            }
                        });
                mStructure = new SliceStructure(s);
                mUri = s.getUri();
                if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
                    setValue(s);
                } else {
                    postValue(s);
                }
            } catch (Exception e) {
                mListener.onSliceError(OnErrorListener.ERROR_INVALID_INPUT, e);
            }
            mInput = null;
            mInitialSliceLoaded = true;
        }

        void goLive(Uri actionUri, Context context, Intent intent) {
            mLive = true;
            if (actionUri != null) {
                mPendingUri.add(actionUri);
                mPendingContext.add(context);
                mPendingIntent.add(intent);
            }
            if (mActive && !mSliceCallbackRegistered) {
                android.os.AsyncTask.execute(mUpdateSlice);
                mSliceViewManager.registerSliceCallback(mUri, mSliceCallback);
                mSliceCallbackRegistered = true;
            }
        }

        @Override
        protected void onActive() {
            mActive = true;
            if (!mInitialSliceLoaded) {
                android.os.AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        loadInitialSlice();
                    }
                });
            }
            if (mLive && !mSliceCallbackRegistered) {
                android.os.AsyncTask.execute(mUpdateSlice);
                mSliceViewManager.registerSliceCallback(mUri, mSliceCallback);
                mSliceCallbackRegistered = true;
            }
        }

        @Override
        protected void onInactive() {
            mActive = false;
            if (mLive && mSliceCallbackRegistered) {
                mSliceViewManager.unregisterSliceCallback(mUri, mSliceCallback);
                mSliceCallbackRegistered = false;
            }
        }

        void onSliceError(int error, Throwable t) {
            mListener.onSliceError(error, t);
            if (mLive) {
                if (mSliceCallbackRegistered) {
                    mSliceViewManager.unregisterSliceCallback(mUri, mSliceCallback);
                    mSliceCallbackRegistered = false;
                }
                mLive = false;
            }
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        protected void updateSlice() {
            try {
                Slice s = mSliceViewManager.bindSlice(mUri);
                mSliceCallback.onSliceUpdated(s);
            } catch (Exception e) {
                mListener.onSliceError(OnErrorListener.ERROR_UNKNOWN, e);
            }
        }


        private final Runnable mUpdateSlice = new Runnable() {
            @Override
            public void run() {
                updateSlice();
            }
        };

        final SliceViewManager.SliceCallback mSliceCallback =
                new SliceViewManager.SliceCallback() {
            @Override
            public void onSliceUpdated(@Nullable Slice s) {
                if (mPendingUri.size() > 0) {
                    if (s == null) {
                        onSliceError(OnErrorListener.ERROR_SLICE_NO_LONGER_PRESENT, null);
                        return;
                    }
                    SliceStructure structure = new SliceStructure(s);
                    if (!mStructure.equals(structure)) {
                        onSliceError(OnErrorListener.ERROR_STRUCTURE_CHANGED, null);
                        return;
                    }
                    SliceMetadata metaData = SliceMetadata.from(mContext, s);
                    if (metaData.getLoadingState() == SliceMetadata.LOADED_ALL) {
                        for (int i = 0; i < mPendingUri.size(); i++) {
                            SliceItem item = SliceQuery.findItem(s, mPendingUri.get(i));
                            if (item != null) {
                                try {
                                    item.fireAction(mPendingContext.get(i), mPendingIntent.get(i));
                                } catch (PendingIntent.CanceledException e) {
                                    onSliceError(OnErrorListener.ERROR_UNKNOWN, e);
                                    return;
                                }
                            } else {
                                onSliceError(
                                        OnErrorListener.ERROR_UNKNOWN, new NullPointerException());
                                return;
                            }
                        }
                        mPendingUri.clear();
                        mPendingContext.clear();
                        mPendingIntent.clear();
                    }
                }
                postValue(s);
            }
        };
    }

    private static class SliceLiveDataImpl extends LiveData<Slice> {
        final Intent mIntent;
        final SliceViewManager mSliceViewManager;
        final OnErrorListener mListener;
        Uri mUri;

        SliceLiveDataImpl(Context context, Uri uri, OnErrorListener listener) {
            super();
            mSliceViewManager = SliceViewManager.getInstance(context);
            mUri = uri;
            mIntent = null;
            mListener = listener;
        }

        SliceLiveDataImpl(Context context, Intent intent, OnErrorListener listener) {
            super();
            mSliceViewManager = SliceViewManager.getInstance(context);
            mUri = null;
            mIntent = intent;
            mListener = listener;
        }

        @Override
        @SuppressWarnings("deprecation") /* AsyncTask */
        protected void onActive() {
            android.os.AsyncTask.execute(mUpdateSlice);
            if (mUri != null) {
                mSliceViewManager.registerSliceCallback(mUri, mSliceCallback);
            }
        }

        @Override
        protected void onInactive() {
            if (mUri != null) {
                mSliceViewManager.unregisterSliceCallback(mUri, mSliceCallback);
            }
        }

        private final Runnable mUpdateSlice = new Runnable() {
            @Override
            public void run() {
                try {
                    Slice s = mUri != null ? mSliceViewManager.bindSlice(mUri)
                            : mSliceViewManager.bindSlice(mIntent);
                    if (mUri == null && s != null) {
                        mUri = s.getUri();
                        mSliceViewManager.registerSliceCallback(mUri, mSliceCallback);
                    }
                    postValue(s);
                } catch (IllegalArgumentException e) {
                    onSliceError(OnErrorListener.ERROR_INVALID_INPUT, e);
                    postValue(null);
                } catch (Exception e) {
                    onSliceError(OnErrorListener.ERROR_UNKNOWN, e);
                    postValue(null);
                }
            }
        };

        final SliceViewManager.SliceCallback mSliceCallback = value -> postValue(value);

        void onSliceError(int error, Throwable t) {
            if (mListener != null) {
                mListener.onSliceError(error, t);
                return;
            }
            Log.e(TAG, "Error binding slice", t);
        }
    }

    private SliceLiveData() {
    }

    /**
     * Listener for errors when using {@link #fromStream(Context, InputStream, OnErrorListener)}.
     */
    public interface OnErrorListener {
        int ERROR_UNKNOWN = 0;
        int ERROR_STRUCTURE_CHANGED = 1;
        int ERROR_SLICE_NO_LONGER_PRESENT = 2;
        int ERROR_INVALID_INPUT = 3;

        @IntDef({ERROR_UNKNOWN, ERROR_STRUCTURE_CHANGED, ERROR_SLICE_NO_LONGER_PRESENT,
                ERROR_INVALID_INPUT})
        @Retention(RetentionPolicy.SOURCE)
        @interface ErrorType {

        }

        /**
         * Called when an error occurs converting a serialized slice into a live slice.
         */
        void onSliceError(@ErrorType int type, @Nullable Throwable source);
    }
}
