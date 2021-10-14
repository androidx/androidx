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

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Collection of use cases which are attached to a specific camera.
 *
 * <p>This class tracks the current state of activity for each use case. There are two states that
 * the use case can be in: attached and active. Attached means the use case is currently ready for
 * the camera capture, but not currently capturing. Active means the use case is either currently
 * issuing a capture request or one has already been issued.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class UseCaseAttachState {
    private static final String TAG = "UseCaseAttachState";
    /** The name of the camera the use cases are attached to. */
    private final String mCameraId;
    /** A map of the use cases to the corresponding state information. */
    // Use LinkedHashMap to retain the attached order for bug fixing and unit testing.
    private final Map<String, UseCaseAttachInfo> mAttachedUseCasesToInfoMap = new LinkedHashMap<>();

    /** Constructs an instance of the attach state which corresponds to the named camera. */
    public UseCaseAttachState(@NonNull String cameraId) {
        mCameraId = cameraId;
    }

    /**
     * Sets the use case to an active state.
     *
     * <p>Adds the use case to the collection if not already in it.
     */
    public void setUseCaseActive(@NonNull String useCaseId, @NonNull SessionConfig sessionConfig) {
        UseCaseAttachInfo useCaseAttachInfo = getOrCreateUseCaseAttachInfo(useCaseId,
                sessionConfig);
        useCaseAttachInfo.setActive(true);
    }

    /**
     * Sets the use case to an inactive state.
     *
     * <p>Removes the use case from the collection if also offline.
     */
    public void setUseCaseInactive(@NonNull String useCaseId) {
        if (!mAttachedUseCasesToInfoMap.containsKey(useCaseId)) {
            return;
        }

        UseCaseAttachInfo useCaseAttachInfo = mAttachedUseCasesToInfoMap.get(useCaseId);
        useCaseAttachInfo.setActive(false);
        if (!useCaseAttachInfo.getAttached()) {
            mAttachedUseCasesToInfoMap.remove(useCaseId);
        }
    }

    /**
     * Sets the use case to an attached state.
     *
     * <p>Adds the use case to the collection if not already in it.
     */
    public void setUseCaseAttached(@NonNull String useCaseId,
            @NonNull SessionConfig sessionConfig) {
        UseCaseAttachInfo useCaseAttachInfo = getOrCreateUseCaseAttachInfo(useCaseId,
                sessionConfig);
        useCaseAttachInfo.setAttached(true);
    }

    /**
     * Sets the use case to an detached state.
     *
     * <p>Removes the use case from the collection if also inactive.
     */
    public void setUseCaseDetached(@NonNull String useCaseId) {
        if (!mAttachedUseCasesToInfoMap.containsKey(useCaseId)) {
            return;
        }
        UseCaseAttachInfo useCaseAttachInfo = mAttachedUseCasesToInfoMap.get(useCaseId);
        useCaseAttachInfo.setAttached(false);
        if (!useCaseAttachInfo.getActive()) {
            mAttachedUseCasesToInfoMap.remove(useCaseId);
        }
    }

    /** Returns if the use case is attached or not. */
    public boolean isUseCaseAttached(@NonNull String useCaseId) {
        if (!mAttachedUseCasesToInfoMap.containsKey(useCaseId)) {
            return false;
        }

        UseCaseAttachInfo useCaseAttachInfo = mAttachedUseCasesToInfoMap.get(useCaseId);
        return useCaseAttachInfo.getAttached();
    }

    @NonNull
    public Collection<SessionConfig> getAttachedSessionConfigs() {
        return Collections.unmodifiableCollection(
                getSessionConfigs((useCaseAttachInfo) -> useCaseAttachInfo.getAttached()));
    }

    @NonNull
    public Collection<SessionConfig> getActiveAndAttachedSessionConfigs() {
        return Collections.unmodifiableCollection(
                getSessionConfigs((useCaseAttachInfo) ->
                        useCaseAttachInfo.getActive() && useCaseAttachInfo.getAttached()));
    }

    /**
     * Updates the session configuration for a use case.
     *
     * <p>If the use case is not already in the collection, nothing is done.
     */
    public void updateUseCase(@NonNull String useCaseId, @NonNull SessionConfig sessionConfig) {
        if (!mAttachedUseCasesToInfoMap.containsKey(useCaseId)) {
            return;
        }

        // Rebuild the attach info from scratch to get the updated SessionConfig.
        UseCaseAttachInfo newUseCaseAttachInfo =
                new UseCaseAttachInfo(sessionConfig);

        // Retain the attached and active flags.
        UseCaseAttachInfo oldUseCaseAttachInfo = mAttachedUseCasesToInfoMap.get(useCaseId);
        newUseCaseAttachInfo.setAttached(oldUseCaseAttachInfo.getAttached());
        newUseCaseAttachInfo.setActive(oldUseCaseAttachInfo.getActive());
        mAttachedUseCasesToInfoMap.put(useCaseId, newUseCaseAttachInfo);
    }

    /**
     * Removes the item from the map.
     */
    public void removeUseCase(@NonNull String useCaseId) {
        mAttachedUseCasesToInfoMap.remove(useCaseId);
    }

    /** Returns a session configuration builder for use cases which are both active and attached. */
    @NonNull
    public SessionConfig.ValidatingBuilder getActiveAndAttachedBuilder() {
        SessionConfig.ValidatingBuilder validatingBuilder = new SessionConfig.ValidatingBuilder();

        List<String> list = new ArrayList<>();
        for (Map.Entry<String, UseCaseAttachInfo> attachedUseCase :
                mAttachedUseCasesToInfoMap.entrySet()) {
            UseCaseAttachInfo useCaseAttachInfo = attachedUseCase.getValue();
            if (useCaseAttachInfo.getActive() && useCaseAttachInfo.getAttached()) {
                String useCaseId = attachedUseCase.getKey();
                validatingBuilder.add(useCaseAttachInfo.getSessionConfig());
                list.add(useCaseId);
            }
        }
        Logger.d(TAG, "Active and attached use case: " + list + " for camera: " + mCameraId);
        return validatingBuilder;
    }

    /** Returns a session configuration builder for use cases which are attached. */
    @NonNull
    public SessionConfig.ValidatingBuilder getAttachedBuilder() {
        SessionConfig.ValidatingBuilder validatingBuilder = new SessionConfig.ValidatingBuilder();
        List<String> list = new ArrayList<>();
        for (Map.Entry<String, UseCaseAttachInfo> attachedUseCase :
                mAttachedUseCasesToInfoMap.entrySet()) {
            UseCaseAttachInfo useCaseAttachInfo = attachedUseCase.getValue();
            if (useCaseAttachInfo.getAttached()) {
                validatingBuilder.add(useCaseAttachInfo.getSessionConfig());
                String useCaseId = attachedUseCase.getKey();
                list.add(useCaseId);
            }
        }
        Logger.d(TAG, "All use case: " + list + " for camera: " + mCameraId);
        return validatingBuilder;
    }

    private UseCaseAttachInfo getOrCreateUseCaseAttachInfo(@NonNull String useCaseId,
            @NonNull SessionConfig sessionConfig) {
        UseCaseAttachInfo useCaseAttachInfo = mAttachedUseCasesToInfoMap.get(useCaseId);
        if (useCaseAttachInfo == null) {
            useCaseAttachInfo = new UseCaseAttachInfo(sessionConfig);
            mAttachedUseCasesToInfoMap.put(useCaseId, useCaseAttachInfo);
        }
        return useCaseAttachInfo;
    }

    private Collection<SessionConfig> getSessionConfigs(AttachStateFilter attachStateFilter) {
        List<SessionConfig> sessionConfigs = new ArrayList<>();
        for (Map.Entry<String, UseCaseAttachInfo> attachedUseCase :
                mAttachedUseCasesToInfoMap.entrySet()) {
            if (attachStateFilter == null || attachStateFilter.filter(attachedUseCase.getValue())) {
                sessionConfigs.add(attachedUseCase.getValue().getSessionConfig());
            }
        }
        return sessionConfigs;
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
