/*
 * Copyright 2019 The Android Open Source Project
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
import androidx.annotation.NonNull;
import androidx.camera.core.UseCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A collection of {@link UseCase}.
 *
 * <p>The set of {@link UseCase} instances have synchronized interactions with the {@link
 * CameraInternal}.
 */

public final class UseCaseMediator {
    private static final String TAG = "UseCaseMediator";

    /**
     * The lock for the single {@link StateChangeCallback} held by the mediator.
     *
     * <p>This lock is always acquired prior to acquiring the mUseCasesLock so that there is no
     * lock-ordering deadlock.
     */
    private final Object mListenerLock = new Object();
    /**
     * The lock for accessing the map of use case types to use case instances.
     *
     * <p>This lock is always acquired after acquiring the mListenerLock so that there is no
     * lock-ordering deadlock.
     */
    private final Object mUseCasesLock = new Object();
    @GuardedBy("mUseCasesLock")
    private final Set<UseCase> mUseCases = new HashSet<>();
    @GuardedBy("mListenerLock")
    private StateChangeCallback mStateChangeCallback;
    private volatile boolean mIsActive = false;

    /** Starts all the use cases so that they are brought into an online state. */
    public void start() {
        synchronized (mListenerLock) {
            if (mStateChangeCallback != null) {
                mStateChangeCallback.onActive(this);
            }
            mIsActive = true;
        }
    }

    /** Stops all the use cases so that they are brought into an offline state. */
    public void stop() {
        synchronized (mListenerLock) {
            if (mStateChangeCallback != null) {
                mStateChangeCallback.onInactive(this);
            }
            mIsActive = false;
        }
    }

    /** Sets the StateChangeCallback listener */
    public void setListener(@NonNull StateChangeCallback stateChangeCallback) {
        synchronized (mListenerLock) {
            this.mStateChangeCallback = stateChangeCallback;
        }
    }

    /**
     * Adds the {@link UseCase} to the mediator.
     *
     * @return true if the use case is added, or false if the use case already exists in the
     * mediator.
     */
    public boolean addUseCase(@NonNull UseCase useCase) {
        synchronized (mUseCasesLock) {
            return mUseCases.add(useCase);
        }
    }

    /** Returns true if the {@link UseCase} is contained in the mediator. */
    public boolean contains(@NonNull UseCase useCase) {
        synchronized (mUseCasesLock) {
            return mUseCases.contains(useCase);
        }
    }

    /**
     * Removes the {@link UseCase} from the mediator.
     *
     * @return Returns true if the use case is removed. Otherwise returns false (if the use case did
     * not exist in the mediator).
     */
    public boolean removeUseCase(@NonNull UseCase useCase) {
        synchronized (mUseCasesLock) {
            return mUseCases.remove(useCase);
        }
    }

    /**
     * Called when lifecycle ends. Destroys all use cases in this mediator.
     */
    public void destroy() {
        List<UseCase> useCasesToClear = new ArrayList<>();
        synchronized (mUseCasesLock) {
            useCasesToClear.addAll(mUseCases);
            mUseCases.clear();
        }

        for (UseCase useCase : useCasesToClear) {
            Log.d(TAG, "Destroying use case: " + useCase.getName());
            useCase.onDetach();
            useCase.onDestroy();
        }
    }

    /**
     * Returns the collection of all the use cases currently contained by
     * the{@link UseCaseMediator}.
     */
    @NonNull
    public Collection<UseCase> getUseCases() {
        synchronized (mUseCasesLock) {
            return Collections.unmodifiableCollection(mUseCases);
        }
    }

    /** Returns the map of all the use cases to its attached camera id. */
    @NonNull
    public Map<String, Set<UseCase>> getCameraIdToUseCaseMap() {
        Map<String, Set<UseCase>> cameraIdToUseCases = new HashMap<>();
        synchronized (mUseCasesLock) {
            for (UseCase useCase : mUseCases) {
                CameraInternal attachedCamera = useCase.getCamera();
                if (attachedCamera != null) {
                    String cameraId = attachedCamera.getCameraInfoInternal().getCameraId();
                    Set<UseCase> useCaseSet = cameraIdToUseCases.get(cameraId);
                    if (useCaseSet == null) {
                        useCaseSet = new HashSet<>();
                    }
                    useCaseSet.add(useCase);
                    cameraIdToUseCases.put(cameraId, useCaseSet);
                }
            }
        }
        return Collections.unmodifiableMap(cameraIdToUseCases);
    }

    public boolean isActive() {
        return mIsActive;
    }

    /**
     * Listener called when a {@link UseCaseMediator} transitions between active/inactive states
     * .
     */
    public interface StateChangeCallback {
        /**
         * Called when a {@link UseCaseMediator} becomes active.
         *
         * <p>When a {@link UseCaseMediator} is active then all the contained {@link UseCase} become
         * online. This means that the {@link CameraInternal} should transition to a state as
         * close as possible to producing, but prior to actually producing data for the use case.
         */
        void onActive(@NonNull UseCaseMediator useCaseMediator);

        /**
         * Called when a {@link UseCaseMediator} becomes inactive.
         *
         * <p>When a {@link UseCaseMediator} is active then all the contained {@link UseCase} become
         * offline.
         */
        void onInactive(@NonNull UseCaseMediator useCaseMediator);
    }
}
