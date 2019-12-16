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

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.impl.CameraControlInternal;
import androidx.camera.core.impl.CameraInfoInternal;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Collection;

/**
 * The camera interface. It is controlled by the change of state in use cases.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public interface CameraInternal extends Camera, UseCase.StateChangeCallback {
    /**
     * The state of a camera within the process.
     *
     * <p>The camera state is used to communicate events like when the camera is opening or
     * closing and can be used to determine when it is safe to interact with the camera.
     */
    enum State {
        /**
         * Camera is waiting for resources to become available before opening.
         *
         * <p>The camera will automatically transition to an {@link #OPENING} state once resources
         * have become available. Resources are typically made available by other cameras closing.
         */
        PENDING_OPEN,
        /**
         * Camera is in the process of opening.
         *
         * <p>This is a transitive state.
         */
        OPENING,
        /**
         * Camera is open and producing (or ready to produce) image data.
         */
        OPEN,
        /**
         * Camera is in the process of closing.
         *
         * <p>This is a transitive state.
         */
        CLOSING,
        /**
         * Camera has been closed and should not be producing data.
         */
        CLOSED,
        /**
         * Camera is in the process of being released and cannot be reopened.
         *
         * <p>This is a transitive state.
         */
        RELEASING,
        /**
         * Camera has been closed and has released all held resources.
         */
        RELEASED
    }

    /**
     * Open the camera asynchronously.
     *
     * <p>Once the camera has been opened use case state transitions can be used to control the
     * camera pipeline.
     */
    void open();

    /**
     * Close the camera asynchronously.
     *
     * <p>Once the camera is closed the camera will no longer produce data. The camera must be
     * reopened for it to produce data again.
     */
    void close();

    /**
     * Release the camera.
     *
     * <p>Once the camera is released it is permanently closed. A new instance must be created to
     * access the camera.
     */
    @NonNull
    ListenableFuture<Void> release();

    /**
     * Retrieves an observable stream of the current state of the camera.
     */
    @NonNull
    Observable<State> getCameraState();

    /**
     * Sets the use case to be in the state where the capture session will be configured to handle
     * capture requests from the use cases.
     */
    void addOnlineUseCase(@NonNull Collection<UseCase> useCases);

    /**
     * Removes the use case to be in the state where the capture session will be configured to
     * handle capture requests from the use cases.
     */
    void removeOnlineUseCase(@NonNull Collection<UseCase> useCases);

    /** Returns the global CameraControlInternal attached to this camera. */
    @NonNull
    CameraControlInternal getCameraControlInternal();

    /** Returns an interface to retrieve characteristics of the camera. */
    @NonNull
    CameraInfoInternal getCameraInfoInternal();
}
