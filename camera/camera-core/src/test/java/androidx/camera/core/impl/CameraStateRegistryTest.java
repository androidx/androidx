/*
 * Copyright 2020 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.os.Build;

import androidx.camera.core.Camera;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public final class CameraStateRegistryTest {

    private static final CameraStateRegistry.OnOpenAvailableListener NO_OP_LISTENER = () -> {
    };

    @Test
    public void tryOpenSucceeds_whenNoCamerasOpen() {
        // Only allow a single open camera at a time
        CameraStateRegistry registry = new CameraStateRegistry(1);

        Camera camera = mock(Camera.class);

        registry.registerCamera(camera, CameraXExecutors.directExecutor(), NO_OP_LISTENER);
        assertThat(registry.tryOpenCamera(camera)).isTrue();
    }

    @Test(expected = RuntimeException.class)
    public void cameraMustBeRegistered_beforeMarkingState() {
        // Only allow a single open camera at a time
        CameraStateRegistry registry = new CameraStateRegistry(1);

        Camera camera = mock(Camera.class);

        registry.markCameraState(camera, CameraInternal.State.CLOSED);
    }

    @Test(expected = RuntimeException.class)
    public void cameraMustBeRegistered_beforeTryOpen() {
        // Only allow a single open camera at a time
        CameraStateRegistry registry = new CameraStateRegistry(1);

        Camera camera = mock(Camera.class);

        registry.tryOpenCamera(camera);
    }

    @Test(expected = RuntimeException.class)
    public void cameraCannotBeRegisteredMultipleTimes() {
        // Only allow a single open camera at a time
        CameraStateRegistry registry = new CameraStateRegistry(1);

        Camera camera = mock(Camera.class);

        registry.registerCamera(camera, CameraXExecutors.directExecutor(), NO_OP_LISTENER);
        registry.registerCamera(camera, CameraXExecutors.directExecutor(), NO_OP_LISTENER);
    }

    @Test
    public void markingReleased_unregistersCamera() {
        // Only allow a single open camera at a time
        CameraStateRegistry registry = new CameraStateRegistry(1);

        Camera camera = mock(Camera.class);

        // Register the camera
        registry.registerCamera(camera, CameraXExecutors.directExecutor(), NO_OP_LISTENER);

        // Mark the camera as released. This should unregister the camera.
        registry.markCameraState(camera, CameraInternal.State.RELEASED);

        // Should now be able to register the camera again since it was unregistered
        // Should not throw.
        registry.registerCamera(camera, CameraXExecutors.directExecutor(), NO_OP_LISTENER);
    }

    @Test
    public void tryOpenFails_whenNoCamerasAvailable() {
        // Only allow a single open camera at a time
        CameraStateRegistry registry = new CameraStateRegistry(1);

        Camera camera1 = mock(Camera.class);
        Camera camera2 = mock(Camera.class);
        Camera camera3 = mock(Camera.class);

        registry.registerCamera(camera1, CameraXExecutors.directExecutor(), NO_OP_LISTENER);
        registry.registerCamera(camera2, CameraXExecutors.directExecutor(), NO_OP_LISTENER);
        registry.registerCamera(camera3, CameraXExecutors.directExecutor(), NO_OP_LISTENER);

        // Take the only available camera.
        registry.tryOpenCamera(camera1);

        // Attempt to open more cameras (this should fail)
        boolean tryOpen2 = registry.tryOpenCamera(camera2);
        boolean tryOpen3 = registry.tryOpenCamera(camera3);

        assertThat(tryOpen2).isFalse();
        assertThat(tryOpen3).isFalse();
    }

    @Test
    public void tryOpenSucceeds_forMultipleCameras() {
        // Allow for two cameras to be open simultaneously
        CameraStateRegistry registry = new CameraStateRegistry(2);

        Camera camera1 = mock(Camera.class);
        Camera camera2 = mock(Camera.class);

        registry.registerCamera(camera1, CameraXExecutors.directExecutor(), NO_OP_LISTENER);
        registry.registerCamera(camera2, CameraXExecutors.directExecutor(), NO_OP_LISTENER);

        // Open first camera
        boolean tryOpen1 = registry.tryOpenCamera(camera1);
        // Open second camera
        boolean tryOpen2 = registry.tryOpenCamera(camera2);

        assertThat(tryOpen1).isTrue();
        assertThat(tryOpen2).isTrue();
    }

    @Test
    public void tryOpenSucceeds_whenNoCamerasAvailable_butCameraIsAlreadyOpen() {
        // Only allow a single open camera at a time
        CameraStateRegistry registry = new CameraStateRegistry(1);

        Camera camera = mock(Camera.class);

        registry.registerCamera(camera, CameraXExecutors.directExecutor(), NO_OP_LISTENER);

        // Try to open the same camera twice
        registry.tryOpenCamera(camera);
        assertThat(registry.tryOpenCamera(camera)).isTrue();
    }

    @Test
    public void closingCameras_freesUpCameraForOpen() {
        // Only allow a single open camera at a time
        CameraStateRegistry registry = new CameraStateRegistry(1);

        Camera camera1 = mock(Camera.class);
        Camera camera2 = mock(Camera.class);

        registry.registerCamera(camera1, CameraXExecutors.directExecutor(), NO_OP_LISTENER);
        registry.registerCamera(camera2, CameraXExecutors.directExecutor(), NO_OP_LISTENER);

        // Open the first camera.
        registry.tryOpenCamera(camera1);
        registry.markCameraState(camera1, CameraInternal.State.OPEN);

        // Attempt to open second camera (should fail)
        boolean openCamera2WhileCamera1Open = registry.tryOpenCamera(camera2);

        // Close the first camera
        registry.markCameraState(camera1, CameraInternal.State.CLOSED);

        assertThat(openCamera2WhileCamera1Open).isFalse();
        assertThat(registry.tryOpenCamera(camera2)).isTrue();
    }

    @Test
    public void pendingOpen_isNotCountedAsOpen() {
        // Only allow a single open camera at a time
        CameraStateRegistry registry = new CameraStateRegistry(1);

        Camera camera1 = mock(Camera.class);
        Camera camera2 = mock(Camera.class);

        registry.registerCamera(camera1, CameraXExecutors.directExecutor(), NO_OP_LISTENER);
        registry.registerCamera(camera2, CameraXExecutors.directExecutor(), NO_OP_LISTENER);

        registry.markCameraState(camera1, CameraInternal.State.PENDING_OPEN);

        // Still able to open second camera even though first camera is PENDING_OPEN
        assertThat(registry.tryOpenCamera(camera2)).isTrue();
    }

    @Test
    public void cameraInPendingOpenState_isNotifiedWhenCameraBecomesAvailable() {
        // Only allow a single open camera at a time
        CameraStateRegistry registry = new CameraStateRegistry(1);

        Camera camera1 = mock(Camera.class);
        Camera camera2 = mock(Camera.class);

        CameraStateRegistry.OnOpenAvailableListener mockListener =
                mock(CameraStateRegistry.OnOpenAvailableListener.class);
        registry.registerCamera(camera1, CameraXExecutors.directExecutor(), NO_OP_LISTENER);
        registry.registerCamera(camera2, CameraXExecutors.directExecutor(), mockListener);

        // Open first camera
        registry.tryOpenCamera(camera1);
        registry.markCameraState(camera1, CameraInternal.State.OPEN);

        // Set second camera to pending open state
        registry.markCameraState(camera2, CameraInternal.State.PENDING_OPEN);

        // Close first camera
        registry.markCameraState(camera1, CameraInternal.State.CLOSED);

        verify(mockListener).onOpenAvailable();
    }

    @Test
    public void cameraInPendingOpenState_isNotImmediatelyNotifiedWhenCameraBecomesAvailable() {
        // Only allow a single open camera at a time
        CameraStateRegistry registry = new CameraStateRegistry(1);

        // Set up first camera
        Camera camera1 = mock(Camera.class);
        CameraStateRegistry.OnOpenAvailableListener mockListener1 = mock(
                CameraStateRegistry.OnOpenAvailableListener.class);
        registry.registerCamera(camera1, CameraXExecutors.directExecutor(), mockListener1);

        // Set up second camera
        Camera camera2 = mock(Camera.class);
        CameraStateRegistry.OnOpenAvailableListener mockListener2 =
                mock(CameraStateRegistry.OnOpenAvailableListener.class);
        registry.registerCamera(camera2, CameraXExecutors.directExecutor(), mockListener2);

        // Update state of both cameras to PENDING_OPEN, but omit notifying second camera of
        // available camera slot
        registry.markCameraState(camera1, CameraInternal.State.PENDING_OPEN);
        registry.markCameraState(camera2, CameraInternal.State.PENDING_OPEN, false);

        // Verify only first camera is notified of available camera slot
        verify(mockListener1).onOpenAvailable();
        verify(mockListener2, never()).onOpenAvailable();

        // Open then close first camera
        registry.tryOpenCamera(camera1);
        registry.markCameraState(camera1, CameraInternal.State.OPEN);
        registry.markCameraState(camera1, CameraInternal.State.CLOSED);

        // Verify second camera is notified of available camera slot for opening
        verify(mockListener2).onOpenAvailable();
    }

    // Checks whether a camera in a pending open state is notified when one of 2 slots becomes
    // available to be open.
    @Test
    public void cameraInPendingOpenState_isNotifiedWhenCameraBecomesAvailable_multipleSlots() {
        // Allow for two cameras to be open simultaneously
        CameraStateRegistry registry = new CameraStateRegistry(2);

        Camera camera1 = mock(Camera.class);
        Camera camera2 = mock(Camera.class);
        Camera camera3 = mock(Camera.class);

        CameraStateRegistry.OnOpenAvailableListener mockListener =
                mock(CameraStateRegistry.OnOpenAvailableListener.class);
        registry.registerCamera(camera1, CameraXExecutors.directExecutor(), NO_OP_LISTENER);
        registry.registerCamera(camera2, CameraXExecutors.directExecutor(), NO_OP_LISTENER);
        registry.registerCamera(camera3, CameraXExecutors.directExecutor(), mockListener);

        // Open first camera
        registry.tryOpenCamera(camera1);
        registry.markCameraState(camera1, CameraInternal.State.OPEN);

        // Open second camera
        registry.tryOpenCamera(camera2);
        registry.markCameraState(camera2, CameraInternal.State.OPEN);

        // Set third camera to pending open state
        registry.markCameraState(camera3, CameraInternal.State.PENDING_OPEN);

        // Close first camera
        registry.markCameraState(camera1, CameraInternal.State.CLOSED);

        verify(mockListener).onOpenAvailable();
    }

    @Test
    public void cameraInClosedState_isNotNotifiedWhenCameraBecomesAvailable() {
        // Only allow a single open camera at a time
        CameraStateRegistry registry = new CameraStateRegistry(1);

        Camera camera1 = mock(Camera.class);
        Camera camera2 = mock(Camera.class);

        CameraStateRegistry.OnOpenAvailableListener mockListener =
                mock(CameraStateRegistry.OnOpenAvailableListener.class);
        registry.registerCamera(camera1, CameraXExecutors.directExecutor(), NO_OP_LISTENER);
        registry.registerCamera(camera2, CameraXExecutors.directExecutor(), mockListener);

        // Open first camera
        registry.tryOpenCamera(camera1);
        registry.markCameraState(camera1, CameraInternal.State.OPEN);

        // Set second camera to CLOSED state
        registry.markCameraState(camera2, CameraInternal.State.CLOSED);

        // Close first camera
        registry.markCameraState(camera1, CameraInternal.State.CLOSED);

        verify(mockListener, never()).onOpenAvailable();
    }

    @Test
    public void cameraInOpenState_isNotNotifiedWhenCameraBecomesAvailable() {
        // Allow for two cameras to be open simultaneously
        CameraStateRegistry registry = new CameraStateRegistry(2);

        Camera camera1 = mock(Camera.class);
        Camera camera2 = mock(Camera.class);

        CameraStateRegistry.OnOpenAvailableListener mockListener =
                mock(CameraStateRegistry.OnOpenAvailableListener.class);
        registry.registerCamera(camera1, CameraXExecutors.directExecutor(), NO_OP_LISTENER);
        registry.registerCamera(camera2, CameraXExecutors.directExecutor(), mockListener);

        // Open first camera
        registry.tryOpenCamera(camera1);
        registry.markCameraState(camera1, CameraInternal.State.OPEN);

        // Open second camera
        registry.tryOpenCamera(camera2);
        registry.markCameraState(camera2, CameraInternal.State.OPEN);

        // Close first camera
        registry.markCameraState(camera1, CameraInternal.State.CLOSED);

        // Second camera is already open, should not be notified.
        verify(mockListener, never()).onOpenAvailable();
    }
}
