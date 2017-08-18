/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.arch.util.paging;

import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.WorkerThread;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @param <Type> Data type held by this list.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class PagerBase<Type> {
    List<Type> mItems;

    private boolean mLoadBefore = false;
    private boolean mLoadAfter = false;

    /**
     * Gets an item from the specified index, and loads content appropriately as a side effect.
     * Must be between 0 and {@link #size()}.
     *
     * @param index Index of item to return.
     * @return Item requested.
     */
    public abstract Type get(int index);

    /**
     * Gets an item from the specified index, but without side effects. Must be between 0 and
     * {@link #size()}.
     *
     * @param index Index of item to return.
     * @return Item requested.
     */
    public abstract Type access(int index);

    /**
     * @return Size of list.
     */
    public abstract int size();

    boolean mInitialized;

    @NonNull
    final Executor mMainThreadExecutor;
    @NonNull
    final Executor mBackgroundThreadExecutor;

    @Nullable
    abstract List<Type> loadBeforeImpl(int position, Type item);

    @Nullable
    abstract List<Type> loadAfterImpl(int position, Type item);

    // trigger loadBefore if positions loaded after/before aren't good enough
    abstract void loadBeforeIfNeeded();

    abstract void loadAfterIfNeeded();

    // used to communicated to list provider to issue a new version
    abstract void onItemsPrepended(int count);

    abstract void onItemsAppended(int count);

    AtomicBoolean mInvalid = new AtomicBoolean(false);
    final ListConfig mConfig;

    @WorkerThread
    PagerBase(@NonNull Executor mainThreadExecutor, @NonNull Executor backgroundThreadExecutor,
            ListConfig configuration) {
        mConfig = configuration;
        mMainThreadExecutor = mainThreadExecutor;
        mBackgroundThreadExecutor = backgroundThreadExecutor;
    }

    void freeze() {
        mInvalid.set(true);
    }

    void setInitialData(List<Type> items) {
        mItems = new ArrayList<>(items);
        if (mItems.size() == 0) {
            // No data loaded, so display no data, and don't try to load more.
            mLoadBefore = mLoadAfter = true;
        }
        loadAfterIfNeeded();
        loadBeforeIfNeeded();
    }

    @MainThread
    protected void loadBefore(final int position) {
        if (mLoadBefore) {
            return;
        }
        mLoadBefore = true;

        final Type item = mItems.get(0);
        mBackgroundThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {

                if (mInvalid.get()) {
                    return;
                }

                final List<Type> data = loadBeforeImpl(position, item);
                if (data != null) {
                    mMainThreadExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            if (mInvalid.get()) {
                                return;
                            }
                            prepend(data);
                        }
                    });
                } else {
                    freeze();
                }
            }
        });
    }

    @MainThread
    protected void loadAfter(final int position) {
        if (mLoadAfter) {
            return;
        }
        mLoadAfter = true;

        final Type item = mItems.get(mItems.size() - 1);
        mBackgroundThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (mInvalid.get()) {
                    return;
                }

                final List<Type> data = loadAfterImpl(position, item);
                if (data != null) {
                    mMainThreadExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            if (mInvalid.get()) {
                                return;
                            }
                            append(data);
                        }
                    });
                } else {
                    freeze();
                }
            }
        });
    }

    @MainThread
    private void prepend(List<Type> before) {
        final int count = before.size();
        if (count == 0) {
            // Nothing returned from source, stop loading in this direction
            return;
        }

        Collections.reverse(before);
        mItems.addAll(0, before);
        onItemsPrepended(count);

        mLoadBefore = false;
        loadBeforeIfNeeded();
    }

    @MainThread
    private void append(List<Type> after) {
        final int count = after.size();
        if (count == 0) {
            // Nothing returned from source, stop loading in this direction
            return;
        }

        mItems.addAll(after);
        onItemsAppended(count);

        mLoadAfter = false;
        loadAfterIfNeeded();
    }
}
