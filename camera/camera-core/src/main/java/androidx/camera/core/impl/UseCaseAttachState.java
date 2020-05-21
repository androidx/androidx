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

import androidx.annotation.NonNull;
import androidx.camera.core.UseCase;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Collection of use cases which are attached to a specific camera.
 *
 * <p>This class tracks the current state of activity for each use case. There are two states that
 * the use case can be in: attached and active. Attached means the use case is currently ready for
 * the camera capture, but not currently capturing. Active means the use case is either currently
 * issuing a capture request or one has already been issued.
 */

public final class UseCaseAttachState {
    private static final String TAG = "UseCaseAttachState";
    /** The name of the camera the use cases are attached to. */
    private final String mCameraId;
    /** A map of the use cases to the corresponding state information. */
    private final Map<UseCase, UseCaseAttachInfo> mAttachedUseCasesToInfoMap = new HashMap<>();

    /** Constructs an instance of the attach state which corresponds to the named camera. */
    public UseCaseAttachState(@NonNull String cameraId) {
        mCameraId = cameraId;
    }

    /**
     * Sets the use case to an active state.
     *
     * <p>Adds the use case to the collection if not already in it.
     */
    public void setUseCaseActive(@NonNull UseCase useCase) {
        UseCaseAttachInfo useCaseAttachInfo = getOrCreateUseCaseAttachInfo(useCase);
        useCaseAttachInfo.setActive(true);
    }

    /**
     * Sets the use case to an inactive state.
     *
     * <p>Removes the use case from the collection if also offline.
     */
    public void setUseCaseInactive(@NonNull UseCase useCase) {
        if (!mAttachedUseCasesToInfoMap.containsKey(useCase)) {
            return;
        }

        UseCaseAttachInfo useCaseAttachInfo = mAttachedUseCasesToInfoMap.get(useCase);
        useCaseAttachInfo.setActive(false);
        if (!useCaseAttachInfo.getAttached()) {
            mAttachedUseCasesToInfoMap.remove(useCase);
        }
    }

    /**
     * Sets the use case to an attached state.
     *
     * <p>Adds the use case to the collection if not already in it.
     */
    public void setUseCaseAttached(@NonNull UseCase useCase) {
        UseCaseAttachInfo useCaseAttachInfo = getOrCreateUseCaseAttachInfo(useCase);
        useCaseAttachInfo.setAttached(true);
    }

    /**
     * Sets the use case to an detached state.
     *
     * <p>Removes the use case from the collection if also inactive.
     */
    public void setUseCaseDetached(@NonNull UseCase useCase) {
        if (!mAttachedUseCasesToInfoMap.containsKey(useCase)) {
            return;
        }
        UseCaseAttachInfo useCaseAttachInfo = mAttachedUseCasesToInfoMap.get(useCase);
        useCaseAttachInfo.setAttached(false);
        if (!useCaseAttachInfo.getActive()) {
            mAttachedUseCasesToInfoMap.remove(useCase);
        }
    }

    /** Returns if the use case is attached or not. */
    public boolean isUseCaseAttached(@NonNull UseCase useCase) {
        if (!mAttachedUseCasesToInfoMap.containsKey(useCase)) {
            return false;
        }

        UseCaseAttachInfo useCaseAttachInfo = mAttachedUseCasesToInfoMap.get(useCase);
        return useCaseAttachInfo.getAttached();
    }

    @NonNull
    public Collection<UseCase> getAttachedUseCases() {
        return Collections.unmodifiableCollection(
                getUseCases(new AttachStateFilter() {
                    @Override
                    public boolean filter(UseCaseAttachInfo useCaseAttachInfo) {
                        return useCaseAttachInfo.getAttached();
                    }
                }));
    }

    @NonNull
    public Collection<UseCase> getActiveAndAttachedUseCases() {
        return Collections.unmodifiableCollection(
                getUseCases(
                        new AttachStateFilter() {
                            @Override
                            public boolean filter(UseCaseAttachInfo useCaseAttachInfo) {
                                return useCaseAttachInfo.getActive()
                                        && useCaseAttachInfo.getAttached();
                            }
                        }));
    }

    /**
     * Updates the session configuration for a use case.
     *
     * <p>If the use case is not already in the collection, nothing is done.
     */
    public void updateUseCase(@NonNull UseCase useCase) {
        if (!mAttachedUseCasesToInfoMap.containsKey(useCase)) {
            return;
        }

        // Rebuild the attach info from scratch to get the updated SessionConfig.
        UseCaseAttachInfo newUseCaseAttachInfo =
                new UseCaseAttachInfo(useCase.getSessionConfig());

        // Retain the attached and active flags.
        UseCaseAttachInfo oldUseCaseAttachInfo = mAttachedUseCasesToInfoMap.get(useCase);
        newUseCaseAttachInfo.setAttached(oldUseCaseAttachInfo.getAttached());
        newUseCaseAttachInfo.setActive(oldUseCaseAttachInfo.getActive());
        mAttachedUseCasesToInfoMap.put(useCase, newUseCaseAttachInfo);
    }

    /** Returns a session configuration builder for use cases which are both active and attached. */
    @NonNull
    public SessionConfig.ValidatingBuilder getActiveAndAttachedBuilder() {
        SessionConfig.ValidatingBuilder validatingBuilder = new SessionConfig.ValidatingBuilder();

        List<String> list = new ArrayList<>();
        for (Entry<UseCase, UseCaseAttachInfo> attachedUseCase :
                mAttachedUseCasesToInfoMap.entrySet()) {
            UseCaseAttachInfo useCaseAttachInfo = attachedUseCase.getValue();
            if (useCaseAttachInfo.getActive() && useCaseAttachInfo.getAttached()) {
                UseCase useCase = attachedUseCase.getKey();
                validatingBuilder.add(useCaseAttachInfo.getSessionConfig());
                list.add(useCase.getName());
            }
        }
        Log.d(TAG, "Active and attached use case: " + list + " for camera: " + mCameraId);
        return validatingBuilder;
    }

    /** Returns a session configuration builder for use cases which are attached. */
    @NonNull
    public SessionConfig.ValidatingBuilder getAttachedBuilder() {
        SessionConfig.ValidatingBuilder validatingBuilder = new SessionConfig.ValidatingBuilder();
        List<String> list = new ArrayList<>();
        for (Entry<UseCase, UseCaseAttachInfo> attachedUseCase :
                mAttachedUseCasesToInfoMap.entrySet()) {
            UseCaseAttachInfo useCaseAttachInfo = attachedUseCase.getValue();
            if (useCaseAttachInfo.getAttached()) {
                validatingBuilder.add(useCaseAttachInfo.getSessionConfig());
                UseCase useCase = attachedUseCase.getKey();
                list.add(useCase.getName());
            }
        }
        Log.d(TAG, "All use case: " + list + " for camera: " + mCameraId);
        return validatingBuilder;
    }


    /** Returns current attached SessionConfig of given UseCase */
    @NonNull
    public SessionConfig getUseCaseSessionConfig(@NonNull UseCase useCase) {
        if (!mAttachedUseCasesToInfoMap.containsKey(useCase)) {
            return SessionConfig.defaultEmptySessionConfig();
        }

        UseCaseAttachInfo attachInfo = mAttachedUseCasesToInfoMap.get(useCase);
        return attachInfo.getSessionConfig();
    }

    private UseCaseAttachInfo getOrCreateUseCaseAttachInfo(UseCase useCase) {
        Preconditions.checkArgument(
                useCase.getCamera().getCameraInfoInternal().getCameraId().equals(mCameraId));
        UseCaseAttachInfo useCaseAttachInfo = mAttachedUseCasesToInfoMap.get(useCase);
        if (useCaseAttachInfo == null) {
            useCaseAttachInfo = new UseCaseAttachInfo(useCase.getSessionConfig());
            mAttachedUseCasesToInfoMap.put(useCase, useCaseAttachInfo);
        }
        return useCaseAttachInfo;
    }

    private Collection<UseCase> getUseCases(AttachStateFilter attachStateFilter) {
        List<UseCase> useCases = new ArrayList<>();
        for (Entry<UseCase, UseCaseAttachInfo> attachedUseCase :
                mAttachedUseCasesToInfoMap.entrySet()) {
            if (attachStateFilter == null || attachStateFilter.filter(attachedUseCase.getValue())) {
                useCases.add(attachedUseCase.getKey());
            }
        }
        return useCases;
    }

    private interface AttachStateFilter {
        boolean filter(UseCaseAttachInfo attachInfo);
    }

    /** The set of state and configuration information for an attached use case. */
    private static final class UseCaseAttachInfo {
        /** The configurations required of the camera for the use case. */
        @NonNull
        private final SessionConfig mSessionConfig;
        /**
         * True if the use case is currently attached (i.e. camera should have a capture session
         * configured for it).
         */
        private boolean mAttached = false;

        /**
         * True if the use case is currently active (i.e. camera should be issuing capture requests
         * for it).
         */
        private boolean mActive = false;

        UseCaseAttachInfo(@NonNull SessionConfig sessionConfig) {
            mSessionConfig = sessionConfig;
        }

        @NonNull
        SessionConfig getSessionConfig() {
            return mSessionConfig;
        }

        boolean getAttached() {
            return mAttached;
        }

        void setAttached(boolean attached) {
            mAttached = attached;
        }

        boolean getActive() {
            return mActive;
        }

        void setActive(boolean active) {
            mActive = active;
        }
    }
}
