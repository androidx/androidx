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

import android.graphics.SurfaceTexture;
import android.view.Surface;

import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.UseCase;
import androidx.camera.core.streamsharing.StreamSharing;

import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

/**
 * The camera interface. It is controlled by the change of state in use cases.
 *
 * <p> It is a Camera instance backed by a single physical camera.
 */
public interface CameraInternal extends Camera, UseCase.StateChangeCallback {
    /**
     * The state of a camera within the process.
     *
     * <p>The camera state is used to communicate events like when the camera is opening or
     * closing and can be used to determine when it is safe to interact with the camera.
     */
    enum State {
        /**
         * Camera has been closed and has released all held resources.
         */
        RELEASED(/*holdsCameraSlot=*/false),
        /**
         * Camera is in the process of being released and cannot be reopened.
         *
         * <p>This is a transient state. Note that this state holds a camera slot even though the
         * implementation may not actually hold camera resources.
         */
        // TODO: Check if this needs to be split up into multiple RELEASING states to
        //  differentiate between when the camera slot is being held or not.
        RELEASING(/*holdsCameraSlot=*/true),
        /**
         * Camera has been closed and should not be producing data.
         */
        CLOSED(/*holdsCameraSlot=*/false),
        /**
         * Camera is waiting for resources to become available before opening.
         *
         * <p>The camera will automatically transition to an {@link #OPENING} state once resources
         * have become available. Resources are typically made available by other cameras closing.
         */
        PENDING_OPEN(/*holdsCameraSlot=*/false),
        /**
         * Camera is in the process of closing.
         *
         * <p>This is a transient state.
         */
        CLOSING(/*holdsCameraSlot=*/true),
        /**
         * Camera is in the process of opening.
         *
         * <p>This is a transient state.
         */
        OPENING(/*holdsCameraSlot=*/true),
        /**
         * Camera is open and producing (or ready to produce) image data.
         */
        OPEN(/*holdsCameraSlot=*/true),
        /**
         * Camera is open and capture session is configured. This state is only used for concurrent
         * camera.
         *
         * <p>In concurrent mode, CONFIGURED refers to camera is opened and capture session is
         * configured, to differentiate from OPEN, which refers to camera device is opened but
         * capture session is not configured yet. External users will only see OPEN state, no
         * matter the internal state is CONFIGURED or OPEN.
         */
        CONFIGURED(/*holdsCameraSlot=*/true);

        private final boolean mHoldsCameraSlot;

        State(boolean holdsCameraSlot) {
            mHoldsCameraSlot = holdsCameraSlot;
        }

        /**
         * Returns whether a camera in this state could be holding on to a camera slot.
         *
         * <p>Holding on to a camera slot may preclude other cameras from being open. This is
         * generally the case when the camera implementation is in the process of opening a
         * camera, has already opened a camera, or is in the process of closing the camera.
         */
        boolean holdsCameraSlot() {
            return mHoldsCameraSlot;
        }
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
     * When in active resuming mode, it will actively retry opening the camera periodically to
     * resume regardless of the camera availability if the camera is interrupted in
     * OPEN/OPENING/PENDING_OPEN state.
     *
     * <p>When not in active resuming mode, it will retry opening camera only when camera
     * becomes available.
     */
    default void setActiveResumingMode(boolean enabled) {
    }

    /**
     * Whether the camera is front facing.
     */
    default boolean isFrontFacing() {
        return getCameraInfo().getLensFacing() == CameraSelector.LENS_FACING_FRONT;
    }

    /**
     * Release the camera.
     *
     * <p>Once the camera is released it is permanently closed. A new instance must be created to
     * access the camera.
     */
    @NonNull ListenableFuture<Void> release();

    /**
     * Retrieves an observable stream of the current state of the camera.
     */
    @NonNull Observable<State> getCameraState();

    /**
     * Sets the use case to be in the state where the capture session will be configured to handle
     * capture requests from the use cases.
     */
    void attachUseCases(@NonNull Collection<UseCase> useCases);

    /**
     * Removes the use case to be in the state where the capture session will be configured to
     * handle capture requests from the use cases.
     */
    void detachUseCases(@NonNull Collection<UseCase> useCases);

    /**
     * Signals that the camera has been physically removed and is no longer available.
     *
     * <p>This method is invoked by core CameraX components, such as the {@link CameraRepository},
     * when an availability event indicates the camera device has been removed from the system
     * (e.g., a USB camera has been unplugged).
     *
     * <p>Upon receiving this signal, the implementation must:
     * <ol>
     * <li>Immediately update its public-facing {@link androidx.camera.core.CameraState} to
     * {@link androidx.camera.core.CameraState.Type#CLOSED} with an error of
     * {@link androidx.camera.core.CameraState#ERROR_CAMERA_REMOVED}.</li>
     * <li>Begin asynchronous cleanup and release of all underlying resources, similar to
     * {@link #release()}.</li>
     * </ol>
     *
     * <p>After this method is called, the camera instance is considered to be in a terminal state
     * and should not be used further.
     */
    default void onRemoved() {
        // Default no-op for the interface to maintain backward compatibility.
    }

    /**
     * Returns true if the camera has been removed and is no longer valid.
     */
    default boolean isRemoved() {
        return false;
    }

    /** Returns the global CameraControlInternal attached to this camera. */
    @NonNull CameraControlInternal getCameraControlInternal();

    /** Returns an interface to retrieve characteristics of the camera. */
    @NonNull CameraInfoInternal getCameraInfoInternal();

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Camera interface
    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    default @NonNull CameraControl getCameraControl() {
        return getCameraControlInternal();
    }

    @Override
    default @NonNull CameraInfo getCameraInfo() {
        return getCameraInfoInternal();
    }

    /**
     * Whether the camera writes the camera transform to the Surface.
     *
     * <p> Camera2 writes the camera transform to the {@link Surface}, which can be used to
     * correct the output. However, if the producer is not the camera, for example, a OpenGL
     * renderer in {@link StreamSharing}, then this field will be false.
     *
     * @see SurfaceTexture#getTransformMatrix
     */
    default boolean getHasTransform() {
        return true;
    }

    /**
     * Sets the flag in camera instance as primary or secondary camera.
     *
     * <p> In dual camera case, the flag will be used to pick up the corresponding
     * {@link SessionConfig} in {@link UseCase}.
     *
     * <p> In single camera case, the flag will always be set to true.
     *
     * @param isPrimary whether the camera is primary or secondary.
     */
    default void setPrimary(boolean isPrimary) {}

    /**
     * Returns the current {@link CameraConfig}.
     */
    @Override
    default @NonNull CameraConfig getExtendedConfig() {
        return CameraConfigs.defaultConfig();
    }

    /**
     * Sets the {@link CameraConfig} to configure the camera.
     */
    default void setExtendedConfig(@Nullable CameraConfig cameraConfig) {
        // Ignore the config since CameraInternal won't use the config
    }
}
