/*
 * Copyright 2023 The Android Open Source Project
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
package androidx.camera.core.streamsharing;

import static androidx.camera.core.impl.utils.Threads.checkMainThread;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.camera.core.UseCase;
import androidx.camera.core.impl.CameraControlInternal;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.Observable;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Collection;

/**
 * A virtual implementation of {@link CameraInternal}.
 *
 * <p> This class manages children {@link UseCase} and connects/disconnects them to the
 * parent {@link StreamSharing}. It also forwards parent camera properties/events to the children.
 */
class VirtualCamera implements CameraInternal {
    private static final String UNSUPPORTED_MESSAGE = "Operation not supported by VirtualCamera.";
    // The parent camera instance.
    @NonNull
    private final CameraInternal mParentCamera;
    @NonNull
    private final VirtualCameraControl mVirtualCameraControl;
    @NonNull
    private final VirtualCameraInfo mVirtualCameraInfo;

    private final UseCase.StateChangeCallback mStateChangeCallback;

    /**
     * @param parentCamera the parent {@link CameraInternal} instance. For example, the
     *                     real camera.
     */
    VirtualCamera(@NonNull CameraInternal parentCamera,
            @NonNull UseCase.StateChangeCallback useCaseStateCallback,
            @NonNull StreamSharing.Control streamSharingControl) {
        mParentCamera = parentCamera;
        mStateChangeCallback = useCaseStateCallback;
        mVirtualCameraControl = new VirtualCameraControl(parentCamera.getCameraControlInternal(),
                streamSharingControl);
        mVirtualCameraInfo = new VirtualCameraInfo(parentCamera.getCameraInfoInternal());
    }

    /**
     * Sets the rotation applied by this virtual camera.
     */
    void setRotationDegrees(int sensorRotationDegrees) {
        mVirtualCameraInfo.setVirtualCameraRotationDegrees(sensorRotationDegrees);
    }

    // --- Forward UseCase state change to VirtualCameraAdapter ---

    @MainThread
    @Override
    public void onUseCaseActive(@NonNull UseCase useCase) {
        checkMainThread();
        mStateChangeCallback.onUseCaseActive(useCase);
    }

    @MainThread
    @Override
    public void onUseCaseInactive(@NonNull UseCase useCase) {
        checkMainThread();
        mStateChangeCallback.onUseCaseInactive(useCase);
    }

    @MainThread
    @Override
    public void onUseCaseUpdated(@NonNull UseCase useCase) {
        checkMainThread();
        mStateChangeCallback.onUseCaseUpdated(useCase);
    }

    @MainThread
    @Override
    public void onUseCaseReset(@NonNull UseCase useCase) {
        checkMainThread();
        mStateChangeCallback.onUseCaseReset(useCase);
    }

    // --- Forward parent camera properties and events ---

    @Override
    public boolean getHasTransform() {
        return false;
    }

    @NonNull
    @Override
    public CameraControlInternal getCameraControlInternal() {
        return mVirtualCameraControl;
    }

    @NonNull
    @Override
    public CameraInfoInternal getCameraInfoInternal() {
        return mVirtualCameraInfo;
    }

    @NonNull
    @Override
    public Observable<State> getCameraState() {
        return mParentCamera.getCameraState();
    }

    // --- Unused overrides ---

    @Override
    public void open() {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }

    @NonNull
    @Override
    public ListenableFuture<Void> release() {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }

    @Override
    public void attachUseCases(@NonNull Collection<UseCase> useCases) {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }

    @Override
    public void detachUseCases(@NonNull Collection<UseCase> useCases) {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }
}
