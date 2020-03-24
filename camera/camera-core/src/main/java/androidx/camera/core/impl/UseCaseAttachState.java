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
 * the use case can be in: online and active. Online means the use case is currently ready for the
 * camera capture, but not currently capturing. Active means the use case is either currently
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
        if (!useCaseAttachInfo.getOnline()) {
            mAttachedUseCasesToInfoMap.remove(useCase);
        }
    }

    /**
     * Sets the use case to an online state.
     *
     * <p>Adds the use case to the collection if not already in it.
     */
    public void setUseCaseOnline(@NonNull UseCase useCase) {
        UseCaseAttachInfo useCaseAttachInfo = getOrCreateUseCaseAttachInfo(useCase);
        useCaseAttachInfo.setOnline(true);
    }

    /**
     * Sets the use case to an offline state.
     *
     * <p>Removes the use case from the collection if also inactive.
     */
    public void setUseCaseOffline(@NonNull UseCase useCase) {
        if (!mAttachedUseCasesToInfoMap.containsKey(useCase)) {
            return;
        }
        UseCaseAttachInfo useCaseAttachInfo = mAttachedUseCasesToInfoMap.get(useCase);
        useCaseAttachInfo.setOnline(false);
        if (!useCaseAttachInfo.getActive()) {
            mAttachedUseCasesToInfoMap.remove(useCase);
        }
    }

    /** Returns if the use case is online or not. */
    public boolean isUseCaseOnline(@NonNull UseCase useCase) {
        if (!mAttachedUseCasesToInfoMap.containsKey(useCase)) {
            return false;
        }

        UseCaseAttachInfo useCaseAttachInfo = mAttachedUseCasesToInfoMap.get(useCase);
        return useCaseAttachInfo.getOnline();
    }

    @NonNull
    public Collection<UseCase> getOnlineUseCases() {
        return Collections.unmodifiableCollection(
                getUseCases(new AttachStateFilter() {
                    @Override
                    public boolean filter(UseCaseAttachInfo useCaseAttachInfo) {
                        return useCaseAttachInfo.getOnline();
                    }
                }));
    }

    @NonNull
    public Collection<UseCase> getActiveAndOnlineUseCases() {
        return Collections.unmodifiableCollection(
                getUseCases(
                        new AttachStateFilter() {
                            @Override
                            public boolean filter(UseCaseAttachInfo useCaseAttachInfo) {
                                return useCaseAttachInfo.getActive()
                                        && useCaseAttachInfo.getOnline();
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

        // Retain the online and active flags.
        UseCaseAttachInfo oldUseCaseAttachInfo = mAttachedUseCasesToInfoMap.get(useCase);
        newUseCaseAttachInfo.setOnline(oldUseCaseAttachInfo.getOnline());
        newUseCaseAttachInfo.setActive(oldUseCaseAttachInfo.getActive());
        mAttachedUseCasesToInfoMap.put(useCase, newUseCaseAttachInfo);
    }

    /** Returns a session configuration builder for use cases which are both active and online. */
    @NonNull
    public SessionConfig.ValidatingBuilder getActiveAndOnlineBuilder() {
        SessionConfig.ValidatingBuilder validatingBuilder = new SessionConfig.ValidatingBuilder();

        List<String> list = new ArrayList<>();
        for (Entry<UseCase, UseCaseAttachInfo> attachedUseCase :
                mAttachedUseCasesToInfoMap.entrySet()) {
            UseCaseAttachInfo useCaseAttachInfo = attachedUseCase.getValue();
            if (useCaseAttachInfo.getActive() && useCaseAttachInfo.getOnline()) {
                UseCase useCase = attachedUseCase.getKey();
                validatingBuilder.add(useCaseAttachInfo.getSessionConfig());
                list.add(useCase.getName());
            }
        }
        Log.d(TAG, "Active and online use case: " + list + " for camera: " + mCameraId);
        return validatingBuilder;
    }

    /** Returns a session configuration builder for use cases which are online. */
    @NonNull
    public SessionConfig.ValidatingBuilder getOnlineBuilder() {
        SessionConfig.ValidatingBuilder validatingBuilder = new SessionConfig.ValidatingBuilder();
        List<String> list = new ArrayList<>();
        for (Entry<UseCase, UseCaseAttachInfo> attachedUseCase :
                mAttachedUseCasesToInfoMap.entrySet()) {
            UseCaseAttachInfo useCaseAttachInfo = attachedUseCase.getValue();
            if (useCaseAttachInfo.getOnline()) {
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
                useCase.getBoundCamera().getCameraInfoInternal().getCameraId().equals(mCameraId));
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
         * True if the use case is currently online (i.e. camera should have a capture session
         * configured for it).
         */
        private boolean mOnline = false;

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

        boolean getOnline() {
            return mOnline;
        }

        void setOnline(boolean online) {
            mOnline = online;
        }

        boolean getActive() {
            return mActive;
        }

        void setActive(boolean active) {
            mActive = active;
        }
    }
}
