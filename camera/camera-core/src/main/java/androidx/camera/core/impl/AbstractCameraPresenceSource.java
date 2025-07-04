/*
 * Copyright 2025 The Android Open Source Project
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

import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.camera.core.CameraIdentifier;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

/**
 * Abstract base class for implementing an {@link Observable} that monitors camera availability.
 *
 * <p>This class handles the generic logic of managing observers, caching the latest data or
 * error, and dispatching notifications. Subclasses are responsible for implementing the
 * specific mechanism to monitor camera ID changes by overriding {@link #startMonitoring()} and
 * {@link #stopMonitoring()}.
 */
public abstract class AbstractCameraPresenceSource
        implements Observable<Set<CameraIdentifier>> {

    private static final String TAG = "CameraPresenceSrc";

    private final Object mLock = new Object();
    private final List<ObserverWrapper> mObservers = new CopyOnWriteArrayList<>();

    @GuardedBy("mLock")
    private Set<CameraIdentifier> mCurrentData = Collections.emptySet();
    @GuardedBy("mLock")
    private Throwable mCurrentError = null;
    @GuardedBy("mLock")
    private boolean mIsActive = false;

    private static class ObserverWrapper {
        final Executor mExecutor;
        final Observer<? super Set<CameraIdentifier>> mObserver;

        ObserverWrapper(@NonNull Executor executor,
                @NonNull Observer<? super Set<CameraIdentifier>> observer) {
            mExecutor = executor;
            mObserver = observer;
        }
    }

    /**
     * Called when the first observer is added. Subclasses should start monitoring the camera
     * availability source.
     */
    protected abstract void startMonitoring();

    /**
     * Called when the last observer is removed. Subclasses should stop monitoring the camera
     * availability source to conserve resources.
     */
    protected abstract void stopMonitoring();

    /**
     * Subclasses must call this method to update the state with a new set of available cameras.
     * This will trigger notifications to observers if the data has changed.
     *
     * @param newData The new set of available {@link CameraIdentifier}s.
     */
    protected void updateData(@NonNull Set<CameraIdentifier> newData) {
        updateState(newData, null);
    }

    /**
     * Fetch the latest data asynchronously, forcing a refresh from the source.
     *
     * <p>The returned future will complete with the refreshed value or an error.
     * Implementations of this method are responsible for querying the underlying hardware
     * or data source, calling {@link #updateData} or {@link #updateError}, and
     * completing the future.
     *
     * @return A future which will contain the latest value after a refresh.
     */
    @NonNull
    @Override
    public abstract ListenableFuture<Set<CameraIdentifier>> fetchData();

    /**
     * Subclasses must call this method to update the state with an error. This will trigger
     * notifications to observers.
     *
     * @param error The error that occurred while fetching camera availability.
     */
    protected void updateError(@NonNull Throwable error) {
        updateState(null, error);
    }

    private void updateState(@Nullable Set<CameraIdentifier> newData, @Nullable Throwable error) {
        boolean shouldNotify;
        Set<CameraIdentifier> dataSnapshot;
        Throwable errorSnapshot;

        synchronized (mLock) {
            if (error != null) {
                shouldNotify = (mCurrentError == null || !mCurrentData.isEmpty());
                mCurrentError = error;
                mCurrentData = Collections.emptySet();
            } else {
                Preconditions.checkNotNull(newData);
                shouldNotify = (mCurrentError != null || !mCurrentData.equals(newData));
                mCurrentError = null;
                mCurrentData = newData;
            }
            dataSnapshot = Collections.unmodifiableSet(new HashSet<>(mCurrentData));
            errorSnapshot = mCurrentError;
        }

        if (shouldNotify) {
            Log.d(TAG, "Data changed. Notifying " + mObservers.size() + " observers. Error: "
                    + (errorSnapshot != null));
            for (ObserverWrapper wrapper : mObservers) {
                notifyObserver(wrapper, dataSnapshot, errorSnapshot);
            }
        }
    }

    private void notifyObserver(@NonNull ObserverWrapper wrapper,
            @NonNull Set<CameraIdentifier> data, @Nullable Throwable error) {
        wrapper.mExecutor.execute(() -> {
            if (error != null) {
                wrapper.mObserver.onError(error);
            } else {
                wrapper.mObserver.onNewData(data);
            }
        });
    }

    @Override
    public void addObserver(@NonNull Executor executor,
            @NonNull Observer<? super Set<CameraIdentifier>> observer) {
        Preconditions.checkNotNull(executor);
        Preconditions.checkNotNull(observer);

        mObservers.add(new ObserverWrapper(executor, observer));

        Set<CameraIdentifier> dataSnapshot;
        Throwable errorSnapshot;

        synchronized (mLock) {
            if (!mIsActive && !mObservers.isEmpty()) {
                Log.i(TAG, "First observer added. Starting monitoring.");
                mIsActive = true;
                startMonitoring();
            }
            dataSnapshot = Collections.unmodifiableSet(new HashSet<>(mCurrentData));
            errorSnapshot = mCurrentError;
        }

        // Immediately notify the new observer with the current state.
        notifyObserver(new ObserverWrapper(executor, observer), dataSnapshot, errorSnapshot);
    }

    @Override
    public void removeObserver(@NonNull Observer<? super Set<CameraIdentifier>> observerToRemove) {
        Preconditions.checkNotNull(observerToRemove);

        ObserverWrapper wrapperToRemove = null;
        for (ObserverWrapper wrapper : mObservers) {
            if (wrapper.mObserver.equals(observerToRemove)) {
                wrapperToRemove = wrapper;
                break;
            }
        }

        if (wrapperToRemove != null) {
            mObservers.remove(wrapperToRemove);
        }

        synchronized (mLock) {
            if (mIsActive && mObservers.isEmpty()) {
                Log.i(TAG, "Last observer removed. Stopping monitoring.");
                mIsActive = false;
                stopMonitoring();
            }
        }
    }
}
