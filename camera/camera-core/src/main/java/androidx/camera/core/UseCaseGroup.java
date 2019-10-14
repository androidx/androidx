/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.core;

import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

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
 * <p>The group of {@link UseCase} instances have synchronized interactions with the {@link
 * BaseCamera}.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public final class UseCaseGroup {
    private static final String TAG = "UseCaseGroup";

    /**
     * The lock for the single {@link StateChangeListener} held by the group.
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
    private StateChangeListener mListener;
    private volatile boolean mIsActive = false;

    /** Starts all the use cases so that they are brought into an online state. */
    void start() {
        synchronized (mListenerLock) {
            if (mListener != null) {
                mListener.onGroupActive(this);
            }
            mIsActive = true;
        }
    }

    /** Stops all the use cases so that they are brought into an offline state. */
    void stop() {
        synchronized (mListenerLock) {
            if (mListener != null) {
                mListener.onGroupInactive(this);
            }
            mIsActive = false;
        }
    }

    void setListener(StateChangeListener listener) {
        synchronized (mListenerLock) {
            this.mListener = listener;
        }
    }

    /**
     * Adds the {@link UseCase} to the group.
     *
     * @return true if the use case is added, or false if the use case already exists in the group.
     */
    public boolean addUseCase(UseCase useCase) {
        synchronized (mUseCasesLock) {
            return mUseCases.add(useCase);
        }
    }

    /** Returns true if the {@link UseCase} is contained in the group. */
    boolean contains(UseCase useCase) {
        synchronized (mUseCasesLock) {
            return mUseCases.contains(useCase);
        }
    }

    /**
     * Removes the {@link UseCase} from the group.
     *
     * @return Returns true if the use case is removed. Otherwise returns false (if the use case did
     * not exist in the group).
     */
    boolean removeUseCase(UseCase useCase) {
        synchronized (mUseCasesLock) {
            return mUseCases.remove(useCase);
        }
    }

    /** Clears all use cases from this group. */
    public void clear() {
        List<UseCase> useCasesToClear = new ArrayList<>();
        synchronized (mUseCasesLock) {
            useCasesToClear.addAll(mUseCases);
            mUseCases.clear();
        }

        for (UseCase useCase : useCasesToClear) {
            Log.d(TAG, "Clearing use case: " + useCase.getName());
            useCase.clear();
        }
    }

    /** Returns the collection of all the use cases currently contained by the UseCaseGroup. */
    Collection<UseCase> getUseCases() {
        synchronized (mUseCasesLock) {
            return Collections.unmodifiableCollection(mUseCases);
        }
    }

    Map<String, Set<UseCase>> getCameraIdToUseCaseMap() {
        Map<String, Set<UseCase>> cameraIdToUseCases = new HashMap<>();
        synchronized (mUseCasesLock) {
            for (UseCase useCase : mUseCases) {
                for (String cameraId : useCase.getAttachedCameraIds()) {
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

    boolean isActive() {
        return mIsActive;
    }

    /** Listener called when a {@link UseCaseGroup} transitions between active/inactive states. */
    interface StateChangeListener {
        /**
         * Called when a {@link UseCaseGroup} becomes active.
         *
         * <p>When a UseCaseGroup is active then all the contained {@link UseCase} become
         * online. This means that the {@link BaseCamera} should transition to a state as close as
         * possible to producing, but prior to actually producing data for the use case.
         */
        void onGroupActive(UseCaseGroup useCaseGroup);

        /**
         * Called when a {@link UseCaseGroup} becomes inactive.
         *
         * <p>When a UseCaseGroup is active then all the contained {@link UseCase} become
         * offline.
         */
        void onGroupInactive(UseCaseGroup useCaseGroup);
    }
}
