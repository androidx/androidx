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

package androidx.room;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.lifecycle.LiveData;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A LiveData implementation that closely works with {@link InvalidationTracker} to implement
 * database drive {@link androidx.lifecycle.LiveData} queries that are strongly hold as long
 * as they are active.
 * <p>
 * We need this extra handling for {@link androidx.lifecycle.LiveData} because when they are
 * observed forever, there is no {@link androidx.lifecycle.Lifecycle} that will keep them in
 * memory but they should stay. We cannot add-remove observer in {@link LiveData#onActive()},
 * {@link LiveData#onInactive()} because that would mean missing changes in between or doing an
 * extra query on every UI rotation.
 * <p>
 * This {@link LiveData} keeps a weak observer to the {@link InvalidationTracker} but it is hold
 * strongly by the {@link InvalidationTracker} as long as it is active.
 */
class RoomTrackingLiveData<T> extends LiveData<T> {
    @SuppressWarnings("WeakerAccess")
    final RoomDatabase mDatabase;

    @SuppressWarnings("WeakerAccess")
    final Callable<T> mComputeFunction;

    private final InvalidationLiveDataContainer mContainer;

    @SuppressWarnings("WeakerAccess")
    final InvalidationTracker.Observer mObserver;

    @SuppressWarnings("WeakerAccess")
    final AtomicBoolean mInvalid = new AtomicBoolean(true);

    @SuppressWarnings("WeakerAccess")
    final AtomicBoolean mComputing = new AtomicBoolean(false);

    @SuppressWarnings("WeakerAccess")
    final AtomicBoolean mRegisteredObserver = new AtomicBoolean(false);

    @SuppressWarnings("WeakerAccess")
    final Runnable mRefreshRunnable = new Runnable() {
        @WorkerThread
        @Override
        public void run() {
            if (mRegisteredObserver.compareAndSet(false, true)) {
                mDatabase.getInvalidationTracker().addWeakObserver(mObserver);
            }
            boolean computed;
            do {
                computed = false;
                // compute can happen only in 1 thread but no reason to lock others.
                if (mComputing.compareAndSet(false, true)) {
                    // as long as it is invalid, keep computing.
                    try {
                        T value = null;
                        while (mInvalid.compareAndSet(true, false)) {
                            computed = true;
                            try {
                                value = mComputeFunction.call();
                            } catch (Exception e) {
                                throw new RuntimeException("Exception while computing database"
                                        + " live data.", e);
                            }
                        }
                        if (computed) {
                            postValue(value);
                        }
                    } finally {
                        // release compute lock
                        mComputing.set(false);
                    }
                }
                // check invalid after releasing compute lock to avoid the following scenario.
                // Thread A runs compute()
                // Thread A checks invalid, it is false
                // Main thread sets invalid to true
                // Thread B runs, fails to acquire compute lock and skips
                // Thread A releases compute lock
                // We've left invalid in set state. The check below recovers.
            } while (computed && mInvalid.get());
        }
    };

    @SuppressWarnings("WeakerAccess")
    final Runnable mInvalidationRunnable = new Runnable() {
        @MainThread
        @Override
        public void run() {
            boolean isActive = hasActiveObservers();
            if (mInvalid.compareAndSet(false, true)) {
                if (isActive) {
                    mDatabase.getQueryExecutor().execute(mRefreshRunnable);
                }
            }
        }
    };

    RoomTrackingLiveData(
            RoomDatabase database,
            InvalidationLiveDataContainer container,
            Callable<T> computeFunction,
            String[] tableNames) {
        mDatabase = database;
        mComputeFunction = computeFunction;
        mContainer = container;
        mObserver = new InvalidationTracker.Observer(tableNames) {
            @Override
            public void onInvalidated(@NonNull Set<String> tables) {
                ArchTaskExecutor.getInstance().executeOnMainThread(mInvalidationRunnable);
            }
        };
    }

    @Override
    protected void onActive() {
        super.onActive();
        mContainer.onActive(this);
        mDatabase.getQueryExecutor().execute(mRefreshRunnable);
    }

    @Override
    protected void onInactive() {
        super.onInactive();
        mContainer.onInactive(this);
    }
}
