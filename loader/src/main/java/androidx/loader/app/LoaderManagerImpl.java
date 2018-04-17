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

package androidx.loader.app;

import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.SparseArrayCompat;
import androidx.core.util.DebugUtils;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStore;
import androidx.loader.content.Loader;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.Modifier;

class LoaderManagerImpl extends LoaderManager {
    static final String TAG = "LoaderManager";
    static boolean DEBUG = false;

    /**
     * Class which manages the state of a {@link Loader} and its associated
     * {@link LoaderCallbacks}
     *
     * @param <D> Type of data the Loader handles
     */
    public static class LoaderInfo<D> extends MutableLiveData<D>
            implements Loader.OnLoadCompleteListener<D> {

        private final int mId;
        private final @Nullable Bundle mArgs;
        private final @NonNull Loader<D> mLoader;
        private LifecycleOwner mLifecycleOwner;
        private LoaderObserver<D> mObserver;
        private Loader<D> mPriorLoader;

        LoaderInfo(int id, @Nullable Bundle args, @NonNull Loader<D> loader,
                @Nullable Loader<D> priorLoader) {
            mId = id;
            mArgs = args;
            mLoader = loader;
            mPriorLoader = priorLoader;
            mLoader.registerListener(id, this);
        }

        @NonNull
        Loader<D> getLoader() {
            return mLoader;
        }

        @Override
        protected void onActive() {
            if (DEBUG) Log.v(TAG, "  Starting: " + LoaderInfo.this);
            mLoader.startLoading();
        }

        @Override
        protected void onInactive() {
            if (DEBUG) Log.v(TAG, "  Stopping: " + LoaderInfo.this);
            mLoader.stopLoading();
        }

        /**
         * Set the {@link LoaderCallbacks} to associate with this {@link Loader}. This
         * removes any existing {@link LoaderCallbacks}.
         *
         * @param owner The lifecycle that should be used to start and stop the {@link Loader}
         * @param callback The new {@link LoaderCallbacks} to use
         * @return The {@link Loader} associated with this LoaderInfo
         */
        @MainThread
        @NonNull
        Loader<D> setCallback(@NonNull LifecycleOwner owner,
                @NonNull LoaderCallbacks<D> callback) {
            LoaderObserver<D> observer = new LoaderObserver<>(mLoader, callback);
            // Add the new observer
            observe(owner, observer);
            // Loaders only support one observer at a time, so remove the current observer, if any
            if (mObserver != null) {
                removeObserver(mObserver);
            }
            mLifecycleOwner = owner;
            mObserver = observer;
            return mLoader;
        }

        void markForRedelivery() {
            LifecycleOwner lifecycleOwner = mLifecycleOwner;
            LoaderObserver<D> observer = mObserver;
            if (lifecycleOwner != null && observer != null) {
                // Removing and re-adding the observer ensures that the
                // observer is called again, even if they had already
                // received the current data
                // Use super.removeObserver to avoid nulling out mLifecycleOwner & mObserver
                super.removeObserver(observer);
                observe(lifecycleOwner, observer);
            }
        }

        boolean isCallbackWaitingForData() {
            //noinspection SimplifiableIfStatement
            if (!hasActiveObservers()) {
                // No active observers means no one is waiting for data
                return false;
            }
            return mObserver != null && !mObserver.hasDeliveredData();
        }

        @Override
        public void removeObserver(@NonNull Observer<? super D> observer) {
            super.removeObserver(observer);
            // Clear out our references when the observer is removed to avoid leaking
            mLifecycleOwner = null;
            mObserver = null;
        }

        /**
         * Destroys this LoaderInfo, its underlying {@link #getLoader() Loader}, and removes any
         * existing {@link androidx.loader.app.LoaderManager.LoaderCallbacks}.
         *
         * @param reset Whether the LoaderCallbacks and Loader should be reset.
         * @return When reset is false, returns any Loader that still needs to be reset
         */
        @MainThread
        Loader<D> destroy(boolean reset) {
            if (DEBUG) Log.v(TAG, "  Destroying: " + this);
            // First tell the Loader that we don't need it anymore
            mLoader.cancelLoad();
            mLoader.abandon();
            // Then clean up the LoaderObserver
            LoaderObserver<D> observer = mObserver;
            if (observer != null) {
                removeObserver(observer);
                if (reset) {
                    observer.reset();
                }
            }
            // Finally, clean up the Loader
            mLoader.unregisterListener(this);
            if ((observer != null && !observer.hasDeliveredData()) || reset) {
                mLoader.reset();
                return mPriorLoader;
            }
            return mLoader;
        }

        @Override
        public void onLoadComplete(@NonNull Loader<D> loader, @Nullable D data) {
            if (DEBUG) Log.v(TAG, "onLoadComplete: " + this);
            if (Looper.myLooper() == Looper.getMainLooper()) {
                setValue(data);
            } else {
                // The Loader#deliverResult method that calls this should
                // only be called on the main thread, so this should never
                // happen, but we don't want to lose the data
                if (DEBUG) {
                    Log.w(TAG, "onLoadComplete was incorrectly called on a "
                            + "background thread");
                }
                postValue(data);
            }
        }

        @Override
        public void setValue(D value) {
            super.setValue(value);
            // Now that the new data has arrived, we can reset any prior Loader
            if (mPriorLoader != null) {
                mPriorLoader.reset();
                mPriorLoader = null;
            }
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(64);
            sb.append("LoaderInfo{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(" #");
            sb.append(mId);
            sb.append(" : ");
            DebugUtils.buildShortClassTag(mLoader, sb);
            sb.append("}}");
            return sb.toString();
        }

        @SuppressWarnings("deprecation")
        public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
            writer.print(prefix); writer.print("mId="); writer.print(mId);
            writer.print(" mArgs="); writer.println(mArgs);
            writer.print(prefix); writer.print("mLoader="); writer.println(mLoader);
            mLoader.dump(prefix + "  ", fd, writer, args);
            if (mObserver != null) {
                writer.print(prefix); writer.print("mCallbacks="); writer.println(mObserver);
                mObserver.dump(prefix + "  ", writer);
            }
            writer.print(prefix); writer.print("mData="); writer.println(
                    getLoader().dataToString(getValue()));
            writer.print(prefix); writer.print("mStarted="); writer.println(
                    hasActiveObservers());
        }
    }

    /**
     * Encapsulates the {@link LoaderCallbacks} as a {@link Observer}.
     *
     * @param <D> Type of data the LoaderCallbacks handles
     */
    static class LoaderObserver<D> implements Observer<D> {

        private final @NonNull Loader<D> mLoader;
        private final @NonNull LoaderCallbacks<D> mCallback;

        private boolean mDeliveredData = false;

        LoaderObserver(@NonNull Loader<D> loader, @NonNull LoaderCallbacks<D> callback) {
            mLoader = loader;
            mCallback = callback;
        }

        @Override
        public void onChanged(@Nullable D data) {
            if (DEBUG) {
                Log.v(TAG, "  onLoadFinished in " + mLoader + ": "
                        + mLoader.dataToString(data));
            }
            mCallback.onLoadFinished(mLoader, data);
            mDeliveredData = true;
        }

        boolean hasDeliveredData() {
            return mDeliveredData;
        }

        @MainThread
        void reset() {
            if (mDeliveredData) {
                if (DEBUG) Log.v(TAG, "  Resetting: " + mLoader);
                mCallback.onLoaderReset(mLoader);
            }
        }

        @Override
        public String toString() {
            return mCallback.toString();
        }

        public void dump(String prefix, PrintWriter writer) {
            writer.print(prefix); writer.print("mDeliveredData="); writer.println(
                    mDeliveredData);
        }
    }

    /**
     * ViewModel responsible for retaining {@link LoaderInfo} instances across configuration changes
     */
    static class LoaderViewModel extends ViewModel {
        private static final ViewModelProvider.Factory FACTORY = new ViewModelProvider.Factory() {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new LoaderViewModel();
            }
        };

        @NonNull
        static LoaderViewModel getInstance(ViewModelStore viewModelStore) {
            return new ViewModelProvider(viewModelStore, FACTORY).get(LoaderViewModel.class);
        }

        private SparseArrayCompat<LoaderInfo> mLoaders = new SparseArrayCompat<>();
        private boolean mCreatingLoader = false;

        void startCreatingLoader() {
            mCreatingLoader = true;
        }

        boolean isCreatingLoader() {
            return mCreatingLoader;
        }

        void finishCreatingLoader() {
            mCreatingLoader = false;
        }

        void putLoader(int id, @NonNull LoaderInfo info) {
            mLoaders.put(id, info);
        }

        @SuppressWarnings("unchecked")
        <D> LoaderInfo<D> getLoader(int id) {
            return mLoaders.get(id);
        }

        void removeLoader(int id) {
            mLoaders.remove(id);
        }

        boolean hasRunningLoaders() {
            int size = mLoaders.size();
            for (int index = 0; index < size; index++) {
                LoaderInfo info = mLoaders.valueAt(index);
                if (info.isCallbackWaitingForData()) {
                    return true;
                }
            }
            return false;
        }

        void markForRedelivery() {
            int size = mLoaders.size();
            for (int index = 0; index < size; index++) {
                LoaderInfo info = mLoaders.valueAt(index);
                info.markForRedelivery();
            }
        }

        @Override
        protected void onCleared() {
            super.onCleared();
            int size = mLoaders.size();
            for (int index = 0; index < size; index++) {
                LoaderInfo info = mLoaders.valueAt(index);
                info.destroy(true);
            }
            mLoaders.clear();
        }

        public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
            if (mLoaders.size() > 0) {
                writer.print(prefix); writer.println("Loaders:");
                String innerPrefix = prefix + "    ";
                for (int i = 0; i < mLoaders.size(); i++) {
                    LoaderInfo info = mLoaders.valueAt(i);
                    writer.print(prefix); writer.print("  #"); writer.print(mLoaders.keyAt(i));
                    writer.print(": "); writer.println(info.toString());
                    info.dump(innerPrefix, fd, writer, args);
                }
            }
        }
    }

    private final @NonNull LifecycleOwner mLifecycleOwner;
    private final @NonNull LoaderViewModel mLoaderViewModel;

    LoaderManagerImpl(@NonNull LifecycleOwner lifecycleOwner,
            @NonNull ViewModelStore viewModelStore) {
        mLifecycleOwner = lifecycleOwner;
        mLoaderViewModel = LoaderViewModel.getInstance(viewModelStore);
    }

    @MainThread
    @NonNull
    private <D> Loader<D> createAndInstallLoader(int id, @Nullable Bundle args,
            @NonNull LoaderCallbacks<D> callback, @Nullable Loader<D> priorLoader) {
        LoaderInfo<D> info;
        try {
            mLoaderViewModel.startCreatingLoader();
            Loader<D> loader = callback.onCreateLoader(id, args);
            if (loader == null) {
                throw new IllegalArgumentException("Object returned from onCreateLoader "
                        + "must not be null");
            }
            if (loader.getClass().isMemberClass()
                    && !Modifier.isStatic(loader.getClass().getModifiers())) {
                throw new IllegalArgumentException("Object returned from onCreateLoader "
                        + "must not be a non-static inner member class: "
                        + loader);
            }
            info = new LoaderInfo<>(id, args, loader, priorLoader);
            if (DEBUG) Log.v(TAG, "  Created new loader " + info);
            mLoaderViewModel.putLoader(id, info);
        } finally {
            mLoaderViewModel.finishCreatingLoader();
        }
        return info.setCallback(mLifecycleOwner, callback);
    }

    @MainThread
    @NonNull
    @Override
    public <D> Loader<D> initLoader(int id, @Nullable Bundle args,
            @NonNull LoaderCallbacks<D> callback) {
        if (mLoaderViewModel.isCreatingLoader()) {
            throw new IllegalStateException("Called while creating a loader");
        }
        if (Looper.getMainLooper() != Looper.myLooper()) {
            throw new IllegalStateException("initLoader must be called on the main thread");
        }

        LoaderInfo<D> info = mLoaderViewModel.getLoader(id);

        if (DEBUG) Log.v(TAG, "initLoader in " + this + ": args=" + args);

        if (info == null) {
            // Loader doesn't already exist; create.
            return createAndInstallLoader(id, args, callback, null);
        } else {
            if (DEBUG) Log.v(TAG, "  Re-using existing loader " + info);
            return info.setCallback(mLifecycleOwner, callback);
        }
    }

    @MainThread
    @NonNull
    @Override
    public <D> Loader<D> restartLoader(int id, @Nullable Bundle args,
            @NonNull LoaderCallbacks<D> callback) {
        if (mLoaderViewModel.isCreatingLoader()) {
            throw new IllegalStateException("Called while creating a loader");
        }
        if (Looper.getMainLooper() != Looper.myLooper()) {
            throw new IllegalStateException("restartLoader must be called on the main thread");
        }

        if (DEBUG) Log.v(TAG, "restartLoader in " + this + ": args=" + args);
        LoaderInfo<D> info = mLoaderViewModel.getLoader(id);
        Loader<D> priorLoader = null;
        if (info != null) {
            priorLoader = info.destroy(false);
        }
        // And create a new Loader
        return createAndInstallLoader(id, args, callback, priorLoader);
    }

    @MainThread
    @Override
    public void destroyLoader(int id) {
        if (mLoaderViewModel.isCreatingLoader()) {
            throw new IllegalStateException("Called while creating a loader");
        }
        if (Looper.getMainLooper() != Looper.myLooper()) {
            throw new IllegalStateException("destroyLoader must be called on the main thread");
        }

        if (DEBUG) Log.v(TAG, "destroyLoader in " + this + " of " + id);
        LoaderInfo info = mLoaderViewModel.getLoader(id);
        if (info != null) {
            info.destroy(true);
            mLoaderViewModel.removeLoader(id);
        }
    }

    @Nullable
    @Override
    public <D> Loader<D> getLoader(int id) {
        if (mLoaderViewModel.isCreatingLoader()) {
            throw new IllegalStateException("Called while creating a loader");
        }

        LoaderInfo<D> info = mLoaderViewModel.getLoader(id);
        return info != null ? info.getLoader() : null;
    }

    @Override
    public void markForRedelivery() {
        mLoaderViewModel.markForRedelivery();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("LoaderManager{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(" in ");
        DebugUtils.buildShortClassTag(mLifecycleOwner, sb);
        sb.append("}}");
        return sb.toString();
    }

    @Deprecated
    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        mLoaderViewModel.dump(prefix, fd, writer, args);
    }

    @Override
    public boolean hasRunningLoaders() {
        return mLoaderViewModel.hasRunningLoaders();
    }
}
