/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.core.impl;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.core.util.Preconditions;

import com.google.auto.value.AutoValue;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An observable which reports a dynamic state.
 *
 * <p>The state of a state observable is conflated. That is, the value received by an
 * {@link androidx.camera.core.impl.Observable.Observer} will only be the latest state; some
 * state updates may never be observed if the state changes quickly enough.
 *
 * <p>State observables require an initial state, and thus always have a state available for
 * retrieval via {@link #fetchData()}, which will return an already-complete
 * {@link ListenableFuture}.
 *
 * <p>Errors are also possible as states, and when an error is present, any previous state
 * information is lost. State observables may transition in and out of error states at any time,
 * including the initial state.
 *
 * <p>All states, including errors, are conflated via {@link Object#equals(Object)}. That is, if
 * two states evaluate to {@code true}, it will be as if the state didn't change and no update
 * will be sent to observers.
 *
 * @param <T> The state type.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public abstract class StateObservable<T> implements Observable<T> {
    private static final int INITIAL_VERSION = 0;

    private final Object mLock = new Object();
    private final AtomicReference<Object> mState;
    @GuardedBy("mLock")
    private int mVersion = INITIAL_VERSION;
    @GuardedBy("mLock")
    private boolean mUpdating = false;

    // Must be updated together under lock
    @GuardedBy("mLock")
    private final Map<Observer<? super T>, ObserverWrapper<T>> mWrapperMap = new HashMap<>();
    @GuardedBy("mLock")
    private final CopyOnWriteArraySet<ObserverWrapper<T>> mNotifySet = new CopyOnWriteArraySet<>();

    StateObservable(@Nullable Object initialState, boolean isError) {
        if (isError) {
            Preconditions.checkArgument(initialState instanceof Throwable, "Initial errors must "
                    + "be Throwable");
            mState = new AtomicReference<>(ErrorWrapper.wrap((Throwable) initialState));
        } else {
            mState = new AtomicReference<>(initialState);
        }

    }

    void updateState(@Nullable T state) {
        updateStateInternal(state);
    }

    void updateStateAsError(@NonNull Throwable error) {
        updateStateInternal(ErrorWrapper.wrap(error));
    }

    private void updateStateInternal(@Nullable Object newState) {
        Iterator<ObserverWrapper<T>> notifyIter;
        int currentVersion;
        synchronized (mLock) {
            Object oldState = mState.getAndSet(newState);
            // If new state is equal to old state, no need to do anything.
            if (Objects.equals(oldState, newState)) return;
            currentVersion = ++mVersion; // State was updated. Next version.
            if (mUpdating) return; // Already updating. New state will get used due to version bump.
            mUpdating = true;
            notifyIter = mNotifySet.iterator();
        }

        while (true) {
            // Update observers unlocked in case of direct executor.
            while (notifyIter.hasNext()) {
                notifyIter.next().update(currentVersion);
            }

            // Check if a new version was added while updating
            synchronized (mLock) {
                if (mVersion == currentVersion) {
                    // Updating complete. Break out.
                    mUpdating = false;
                    break;
                }

                // A new version was added. Update again on next loop.
                // Get a new iterator in case the observers changed during update.
                notifyIter = mNotifySet.iterator();
                currentVersion = mVersion;
            }
        }
    }

    /**
     * Fetch the latest state.
     *
     * <p>For state observables, the future returned by {@code fetchData()} is guaranteed to be
     * complete and will contain either the current state or an error state which will be thrown
     * as an exception from {@link ListenableFuture#get()}.
     *
     * @return A future which will contain the latest value or an error.
     */
    @SuppressWarnings("unchecked")
    @NonNull
    @Override
    public ListenableFuture<T> fetchData() {
        Object state = mState.get();
        if (state instanceof ErrorWrapper) {
            return Futures.immediateFailedFuture(((ErrorWrapper) state).getError());
        } else {
            return Futures.immediateFuture((T) state);
        }
    }

    @Override
    public void addObserver(@NonNull Executor executor, @NonNull Observer<? super T> observer) {
        ObserverWrapper<T> wrapper;
        synchronized (mLock) {
            // If observer is already registered, remove it. It will get notified again immediately.
            removeObserverLocked(observer);

            wrapper = new ObserverWrapper<>(mState, executor, observer);
            mWrapperMap.put(observer, wrapper);
            mNotifySet.add(wrapper);
        }

        // INITIAL_VERSION won't necessarily match the current tracked version constant, but it
        // will be the initial version this wrapper receives. Any future version updates will
        // always be higher than INITIAL_VERSION.
        wrapper.update(INITIAL_VERSION);
    }

    @Override
    public void removeObserver(@NonNull Observer<? super T> observer) {
        synchronized (mLock) {
            removeObserverLocked(observer);
        }
    }

    @GuardedBy("mLock")
    private void removeObserverLocked(@NonNull Observable.Observer<? super T> observer) {
        ObserverWrapper<T> wrapper = mWrapperMap.remove(observer);
        if (wrapper != null) {
            wrapper.close();
            mNotifySet.remove(wrapper);
        }
    }

    @RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
    private static final class ObserverWrapper<T> implements Runnable {
        private static final Object NOT_SET = new Object();
        private static final int NO_VERSION = -1;

        private final Executor mExecutor;
        private final Observer<? super T> mObserver;
        private final AtomicBoolean mActive = new AtomicBoolean(true);
        private final AtomicReference<Object> mStateRef;

        // Since run() will always run sequentially, no need to lock for this variable.
        private Object mLastState = NOT_SET;
        @GuardedBy("this")
        private int mLatestSignalledVersion = NO_VERSION;
        @GuardedBy("this")
        private boolean mWrapperUpdating = false;

        ObserverWrapper(@NonNull AtomicReference<Object> stateRef, @NonNull Executor executor,
                @NonNull Observer<? super T> observer) {
            mStateRef = stateRef;
            mExecutor = executor;
            mObserver = observer;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            Object newState;
            int currentVersion;
            synchronized (this) {
                // Only update if we're still active.
                if (!mActive.get()) {
                    mWrapperUpdating = false;
                    return;
                }
                // Get latest state.
                newState = mStateRef.get();
                currentVersion = mLatestSignalledVersion;
            }

            // Continue to check if we're active before updating
            while (true) {
                // Conflate notification using equality
                if (!Objects.equals(mLastState, newState)) {
                    mLastState = newState;
                    if (newState instanceof ErrorWrapper) {
                        mObserver.onError(((ErrorWrapper) newState).getError());
                    } else {
                        mObserver.onNewData((T) newState);
                    }
                }

                synchronized (this) {
                    if (currentVersion == mLatestSignalledVersion || !mActive.get()) {
                        // Updating complete or no longer active. Break out of update loop.
                        mWrapperUpdating = false;
                        break;
                    }

                    // Get state and version for next update.
                    newState = mStateRef.get();
                    currentVersion = mLatestSignalledVersion;
                }
            }
        }

        void update(int version) {
            synchronized (this) {
                // If no longer active, then don't attempt update.
                if (!mActive.get()) return;
                // No need to update (but this probably shouldn't happen anyways)
                if (version <= mLatestSignalledVersion) return;
                mLatestSignalledVersion = version;
                // No need to update if already updating. Version bump will cause update.
                if (mWrapperUpdating) return;
                mWrapperUpdating = true;
            }

            try {
                mExecutor.execute(this);
            } catch (Throwable t) {
                // Unable to notify due to state of Executor. The update is lost, but there's
                // not much we can do here since the executor rejected the update. Note this
                // may also mean that any updates which occurred while mWrapperUpdating ==
                // true will have also been lost.
                synchronized (this) {
                    // Update mWrapperUpdating so the next update can try again
                    mWrapperUpdating = false;
                }
            }
        }

        void close() {
            // Best effort cancellation. In progress updates will not be cancelled.
            mActive.set(false);
        }
    }

    @AutoValue
    abstract static class ErrorWrapper {
        @NonNull
        static ErrorWrapper wrap(@NonNull Throwable error) {
            return new AutoValue_StateObservable_ErrorWrapper(error);
        }

        @NonNull
        public abstract Throwable getError();
    }
}
