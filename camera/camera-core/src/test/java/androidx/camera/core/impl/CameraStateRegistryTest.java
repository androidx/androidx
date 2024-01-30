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
import static org.mockito.Mockito.when;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.concurrent.CameraCoordinator;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.testing.impl.fakes.FakeCameraCoordinator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.HashMap;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public final class CameraStateRegistryTest {

    private static final CameraStateRegistry.OnOpenAvailableListener NO_OP_OPEN_LISTENER =
            () -> {};
    private static final CameraStateRegistry.OnConfigureAvailableListener NO_OP_CONFIGURE_LISTENER =
            () -> {};

    private final FakeCameraCoordinator mCameraCoordinator = new FakeCameraCoordinator();

    @Test
    public void tryOpenSucceeds_whenNoCamerasOpen() {
        // Only allow a single open camera at a time
        CameraStateRegistry registry = new CameraStateRegistry(mCameraCoordinator, 1);

        String cameraId = "0";
        Camera camera = createMockedCamera(cameraId, mCameraCoordinator, null);

        registry.registerCamera(camera, CameraXExecutors.directExecutor(),
                NO_OP_CONFIGURE_LISTENER, NO_OP_OPEN_LISTENER);
        assertThat(registry.tryOpenCamera(camera)).isTrue();
    }

    @Test(expected = RuntimeException.class)
    public void cameraMustBeRegistered_beforeMarkingState() {
        // Only allow a single open camera at a time
        CameraStateRegistry registry = new CameraStateRegistry(mCameraCoordinator, 1);

        String cameraId = "0";
        Camera camera = createMockedCamera(cameraId, mCameraCoordinator, null);

        registry.markCameraState(camera, CameraInternal.State.CLOSED);
    }

    @Test(expected = RuntimeException.class)
    public void cameraMustBeRegistered_beforeTryOpen() {
        // Only allow a single open camera at a time
        CameraStateRegistry registry = new CameraStateRegistry(mCameraCoordinator, 1);

        String cameraId = "0";
        Camera camera = createMockedCamera(cameraId, mCameraCoordinator, null);

        registry.tryOpenCamera(camera);
    }

    @Test(expected = RuntimeException.class)
    public void cameraCannotBeRegisteredMultipleTimes() {
        // Only allow a single open camera at a time
        CameraStateRegistry registry = new CameraStateRegistry(mCameraCoordinator, 1);

        String cameraId = "0";
        Camera camera = createMockedCamera(cameraId, mCameraCoordinator, null);

        registry.registerCamera(camera, CameraXExecutors.directExecutor(),
                NO_OP_CONFIGURE_LISTENER, NO_OP_OPEN_LISTENER);
        registry.registerCamera(camera, CameraXExecutors.directExecutor(),
                NO_OP_CONFIGURE_LISTENER, NO_OP_OPEN_LISTENER);
    }

    @Test
    public void markingReleased_unregistersCamera() {
        // Only allow a single open camera at a time
        CameraStateRegistry registry = new CameraStateRegistry(mCameraCoordinator, 1);

        String cameraId = "0";
        Camera camera = createMockedCamera(cameraId, mCameraCoordinator, null);

        // Register the camera
        registry.registerCamera(camera, CameraXExecutors.directExecutor(),
                NO_OP_CONFIGURE_LISTENER, NO_OP_OPEN_LISTENER);

        // Mark the camera as released. This should unregister the camera.
        registry.markCameraState(camera, CameraInternal.State.RELEASED);

        // Should now be able to register the camera again since it was unregistered
        // Should not throw.
        registry.registerCamera(camera, CameraXExecutors.directExecutor(),
                NO_OP_CONFIGURE_LISTENER, NO_OP_OPEN_LISTENER);
    }

    @Test
    public void tryOpenFails_whenNoCamerasAvailable() {
        // Only allow a single open camera at a time
        CameraStateRegistry registry = new CameraStateRegistry(mCameraCoordinator, 1);

        String camera1Id = "1";
        String pairedCamera1Id = "2";
        String camera2Id = "2";
        String pairedCamera2Id = "1";
        String camera3Id = "3";
        Camera camera1 = createMockedCamera(camera1Id, mCameraCoordinator, pairedCamera1Id);
        Camera camera2 = createMockedCamera(camera2Id, mCameraCoordinator, pairedCamera2Id);
        Camera camera3 = createMockedCamera(camera3Id, mCameraCoordinator, null);

        registry.registerCamera(camera1, CameraXExecutors.directExecutor(),
                NO_OP_CONFIGURE_LISTENER, NO_OP_OPEN_LISTENER);
        registry.registerCamera(camera2, CameraXExecutors.directExecutor(),
                NO_OP_CONFIGURE_LISTENER, NO_OP_OPEN_LISTENER);
        registry.registerCamera(camera3, CameraXExecutors.directExecutor(),
                NO_OP_CONFIGURE_LISTENER, NO_OP_OPEN_LISTENER);

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
        CameraStateRegistry registry = new CameraStateRegistry(mCameraCoordinator, 2);

        String camera1Id = "1";
        String pairedCamera1Id = "2";
        String camera2Id = "2";
        String pairedCamera2Id = "1";
        Camera camera1 = createMockedCamera(camera1Id, mCameraCoordinator, pairedCamera1Id);
        Camera camera2 = createMockedCamera(camera2Id, mCameraCoordinator, pairedCamera2Id);

        registry.registerCamera(camera1, CameraXExecutors.directExecutor(),
                NO_OP_CONFIGURE_LISTENER, NO_OP_OPEN_LISTENER);
        registry.registerCamera(camera2, CameraXExecutors.directExecutor(),
                NO_OP_CONFIGURE_LISTENER, NO_OP_OPEN_LISTENER);

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
        CameraStateRegistry registry = new CameraStateRegistry(mCameraCoordinator, 1);

        Camera camera = mock(Camera.class);

        registry.registerCamera(camera, CameraXExecutors.directExecutor(),
                NO_OP_CONFIGURE_LISTENER, NO_OP_OPEN_LISTENER);

        // Try to open the same camera twice
        registry.tryOpenCamera(camera);
        assertThat(registry.tryOpenCamera(camera)).isTrue();
    }

    @Test
    public void closingCameras_freesUpCameraForOpen() {
        // Only allow a single open camera at a time
        CameraStateRegistry registry = new CameraStateRegistry(mCameraCoordinator, 1);

        String camera1Id = "1";
        String pairedCamera1Id = "2";
        String camera2Id = "2";
        String pairedCamera2Id = "1";
        Camera camera1 = createMockedCamera(camera1Id, mCameraCoordinator, pairedCamera1Id);
        Camera camera2 = createMockedCamera(camera2Id, mCameraCoordinator, pairedCamera2Id);

        registry.registerCamera(camera1, CameraXExecutors.directExecutor(),
                NO_OP_CONFIGURE_LISTENER, NO_OP_OPEN_LISTENER);
        registry.registerCamera(camera2, CameraXExecutors.directExecutor(),
                NO_OP_CONFIGURE_LISTENER, NO_OP_OPEN_LISTENER);

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
        CameraStateRegistry registry = new CameraStateRegistry(mCameraCoordinator, 1);

        String camera1Id = "1";
        String pairedCamera1Id = "2";
        String camera2Id = "2";
        String pairedCamera2Id = "1";
        Camera camera1 = createMockedCamera(camera1Id, mCameraCoordinator, pairedCamera1Id);
        Camera camera2 = createMockedCamera(camera2Id, mCameraCoordinator, pairedCamera2Id);

        registry.registerCamera(camera1, CameraXExecutors.directExecutor(),
                NO_OP_CONFIGURE_LISTENER, NO_OP_OPEN_LISTENER);
        registry.registerCamera(camera2, CameraXExecutors.directExecutor(),
                NO_OP_CONFIGURE_LISTENER, NO_OP_OPEN_LISTENER);

        registry.markCameraState(camera1, CameraInternal.State.PENDING_OPEN);

        // Still able to open second camera even though first camera is PENDING_OPEN
        assertThat(registry.tryOpenCamera(camera2)).isTrue();
    }

    @Test
    public void cameraInPendingOpenState_isNotifiedWhenCameraBecomesAvailable() {
        // Only allow a single open camera at a time
        CameraStateRegistry registry = new CameraStateRegistry(mCameraCoordinator, 1);

        String camera1Id = "1";
        String pairedCamera1Id = "2";
        String camera2Id = "2";
        String pairedCamera2Id = "1";
        Camera camera1 = createMockedCamera(camera1Id, mCameraCoordinator, pairedCamera1Id);
        Camera camera2 = createMockedCamera(camera2Id, mCameraCoordinator, pairedCamera2Id);

        CameraStateRegistry.OnOpenAvailableListener mockOpenListener =
                mock(CameraStateRegistry.OnOpenAvailableListener.class);
        CameraStateRegistry.OnConfigureAvailableListener mockConfigureListener =
                mock(CameraStateRegistry.OnConfigureAvailableListener.class);
        registry.registerCamera(camera1, CameraXExecutors.directExecutor(),
                NO_OP_CONFIGURE_LISTENER, NO_OP_OPEN_LISTENER);
        registry.registerCamera(camera2, CameraXExecutors.directExecutor(),
                mockConfigureListener, mockOpenListener);

        // Open first camera
        registry.tryOpenCamera(camera1);
        registry.markCameraState(camera1, CameraInternal.State.OPEN);

        // Set second camera to pending open state
        registry.markCameraState(camera2, CameraInternal.State.PENDING_OPEN);

        // Close first camera
        registry.markCameraState(camera1, CameraInternal.State.CLOSED);

        verify(mockOpenListener).onOpenAvailable();
        verify(mockConfigureListener, never()).onConfigureAvailable();
    }

    @Test
    public void cameraInPendingOpenState_isNotImmediatelyNotifiedWhenCameraBecomesAvailable() {
        // Only allow a single open camera at a time
        CameraStateRegistry registry = new CameraStateRegistry(mCameraCoordinator, 1);

        // Set up first camera
        String camera1Id = "1";
        String pairedCamera1Id = "2";
        Camera camera1 = createMockedCamera(camera1Id, mCameraCoordinator, pairedCamera1Id);
        CameraStateRegistry.OnOpenAvailableListener mockOpenListener1 = mock(
                CameraStateRegistry.OnOpenAvailableListener.class);
        CameraStateRegistry.OnConfigureAvailableListener mockConfigureListener1 = mock(
                CameraStateRegistry.OnConfigureAvailableListener.class);
        registry.registerCamera(camera1, CameraXExecutors.directExecutor(),
                mockConfigureListener1, mockOpenListener1);

        // Set up second camera
        String camera2Id = "2";
        String pairedCamera2Id = "1";
        Camera camera2 = createMockedCamera(camera2Id, mCameraCoordinator, pairedCamera2Id);
        CameraStateRegistry.OnOpenAvailableListener mockOpenListener2 =
                mock(CameraStateRegistry.OnOpenAvailableListener.class);
        CameraStateRegistry.OnConfigureAvailableListener mockConfigureListener2 = mock(
                CameraStateRegistry.OnConfigureAvailableListener.class);
        registry.registerCamera(camera2, CameraXExecutors.directExecutor(),
                mockConfigureListener2, mockOpenListener2);

        // Update state of both cameras to PENDING_OPEN, but omit notifying second camera of
        // available camera slot
        registry.markCameraState(camera1, CameraInternal.State.PENDING_OPEN);
        registry.markCameraState(camera2, CameraInternal.State.PENDING_OPEN, false);

        // Verify only first camera is notified of available camera slot
        verify(mockOpenListener1).onOpenAvailable();
        verify(mockOpenListener2, never()).onOpenAvailable();

        // Open then close first camera
        registry.tryOpenCamera(camera1);
        registry.markCameraState(camera1, CameraInternal.State.OPEN);
        registry.markCameraState(camera1, CameraInternal.State.CLOSED);

        // Verify second camera is notified of available camera slot for opening
        verify(mockOpenListener2).onOpenAvailable();
        verify(mockConfigureListener2, never()).onConfigureAvailable();
    }

    // Checks whether a camera in a pending open state is notified when one of 2 slots becomes
    // available to be open.
    @Test
    public void cameraInPendingOpenState_isNotifiedWhenCameraBecomesAvailable_multipleSlots() {
        // Allow for two cameras to be open simultaneously
        CameraStateRegistry registry = new CameraStateRegistry(mCameraCoordinator, 2);

        String camera1Id = "1";
        String pairedCamera1Id = "2";
        String camera2Id = "2";
        String pairedCamera2Id = "1";
        String camera3Id = "3";
        Camera camera1 = createMockedCamera(camera1Id, mCameraCoordinator, pairedCamera1Id);
        Camera camera2 = createMockedCamera(camera2Id, mCameraCoordinator, pairedCamera2Id);
        Camera camera3 = createMockedCamera(camera3Id, mCameraCoordinator, null);

        CameraStateRegistry.OnOpenAvailableListener mockOpenListener =
                mock(CameraStateRegistry.OnOpenAvailableListener.class);
        CameraStateRegistry.OnConfigureAvailableListener mockConfigureListener =
                mock(CameraStateRegistry.OnConfigureAvailableListener.class);
        registry.registerCamera(camera1, CameraXExecutors.directExecutor(),
                NO_OP_CONFIGURE_LISTENER, NO_OP_OPEN_LISTENER);
        registry.registerCamera(camera2, CameraXExecutors.directExecutor(),
                NO_OP_CONFIGURE_LISTENER, NO_OP_OPEN_LISTENER);
        registry.registerCamera(camera3, CameraXExecutors.directExecutor(),
                mockConfigureListener, mockOpenListener);

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

        verify(mockOpenListener).onOpenAvailable();
        verify(mockConfigureListener, never()).onConfigureAvailable();
    }

    @Test
    public void cameraInClosedState_isNotNotifiedWhenCameraBecomesAvailable() {
        // Only allow a single open camera at a time
        CameraStateRegistry registry = new CameraStateRegistry(mCameraCoordinator, 1);

        String camera1Id = "1";
        String pairedCamera1Id = "2";
        String camera2Id = "2";
        String pairedCamera2Id = "1";
        Camera camera1 = createMockedCamera(camera1Id, mCameraCoordinator, pairedCamera1Id);
        Camera camera2 = createMockedCamera(camera2Id, mCameraCoordinator, pairedCamera2Id);

        CameraStateRegistry.OnOpenAvailableListener mockOpenListener =
                mock(CameraStateRegistry.OnOpenAvailableListener.class);
        CameraStateRegistry.OnConfigureAvailableListener mockConfigureListener =
                mock(CameraStateRegistry.OnConfigureAvailableListener.class);
        registry.registerCamera(camera1, CameraXExecutors.directExecutor(),
                NO_OP_CONFIGURE_LISTENER, NO_OP_OPEN_LISTENER);
        registry.registerCamera(camera2, CameraXExecutors.directExecutor(),
                mockConfigureListener, mockOpenListener);

        // Open first camera
        registry.tryOpenCamera(camera1);
        registry.markCameraState(camera1, CameraInternal.State.OPEN);

        // Set second camera to CLOSED state
        registry.markCameraState(camera2, CameraInternal.State.CLOSED);

        // Close first camera
        registry.markCameraState(camera1, CameraInternal.State.CLOSED);

        verify(mockOpenListener, never()).onOpenAvailable();
        verify(mockConfigureListener, never()).onConfigureAvailable();
    }

    @Test
    public void cameraInOpenState_isNotNotifiedWhenCameraBecomesAvailable() {
        // Allow for two cameras to be open simultaneously
        CameraStateRegistry registry = new CameraStateRegistry(mCameraCoordinator, 2);

        String camera1Id = "1";
        String pairedCamera1Id = "2";
        String camera2Id = "2";
        String pairedCamera2Id = "1";
        Camera camera1 = createMockedCamera(camera1Id, mCameraCoordinator, pairedCamera1Id);
        Camera camera2 = createMockedCamera(camera2Id, mCameraCoordinator, pairedCamera2Id);

        CameraStateRegistry.OnOpenAvailableListener mockOpenListener =
                mock(CameraStateRegistry.OnOpenAvailableListener.class);
        CameraStateRegistry.OnConfigureAvailableListener mockConfigureListener =
                mock(CameraStateRegistry.OnConfigureAvailableListener.class);
        registry.registerCamera(camera1, CameraXExecutors.directExecutor(),
                NO_OP_CONFIGURE_LISTENER, NO_OP_OPEN_LISTENER);
        registry.registerCamera(camera2, CameraXExecutors.directExecutor(),
                mockConfigureListener, mockOpenListener);

        // Open first camera
        registry.tryOpenCamera(camera1);
        registry.markCameraState(camera1, CameraInternal.State.OPEN);

        // Open second camera
        registry.tryOpenCamera(camera2);
        registry.markCameraState(camera2, CameraInternal.State.OPEN);

        // Close first camera
        registry.markCameraState(camera1, CameraInternal.State.CLOSED);

        // Second camera is already open, should not be notified.
        verify(mockOpenListener, never()).onOpenAvailable();
        verify(mockConfigureListener, never()).onConfigureAvailable();
    }

    @Test
    public void cameraInConfiguredState_pairedCameraWillBeNotifiedToConfigureInConcurrentMode() {
        // Only allow a single open camera at a time
        CameraStateRegistry registry = new CameraStateRegistry(mCameraCoordinator, 1);
        mCameraCoordinator.setCameraOperatingMode(
                CameraCoordinator.CAMERA_OPERATING_MODE_CONCURRENT);

        String camera1Id = "1";
        String pairedCamera1Id = "2";
        String camera2Id = "2";
        String pairedCamera2Id = "1";
        Camera camera1 = createMockedCamera(camera1Id, mCameraCoordinator, pairedCamera1Id);
        Camera camera2 = createMockedCamera(camera2Id, mCameraCoordinator, pairedCamera2Id);

        CameraStateRegistry.OnOpenAvailableListener mockOpenListener =
                mock(CameraStateRegistry.OnOpenAvailableListener.class);
        CameraStateRegistry.OnConfigureAvailableListener mockConfigureListener =
                mock(CameraStateRegistry.OnConfigureAvailableListener.class);
        registry.registerCamera(camera1, CameraXExecutors.directExecutor(),
                NO_OP_CONFIGURE_LISTENER, NO_OP_OPEN_LISTENER);
        registry.registerCamera(camera2, CameraXExecutors.directExecutor(),
                mockConfigureListener, mockOpenListener);

        // Open first camera
        registry.tryOpenCamera(camera1);
        registry.markCameraState(camera1, CameraInternal.State.OPEN);

        verify(mockConfigureListener, never()).onConfigureAvailable();

        // Open the second camera
        registry.tryOpenCamera(camera2);
        registry.markCameraState(camera2, CameraInternal.State.OPEN);

        verify(mockConfigureListener, never()).onConfigureAvailable();

        // Configure the first camera
        registry.markCameraState(camera1, CameraInternal.State.CONFIGURED);

        verify(mockConfigureListener).onConfigureAvailable();
    }

    @Test
    public void cameraInConfiguredState_pairedCameraWillNotBeNotifiedToConfigureInSingleMode() {
        // Only allow a single open camera at a time
        CameraStateRegistry registry = new CameraStateRegistry(mCameraCoordinator, 1);

        String camera1Id = "1";
        String pairedCamera1Id = "2";
        String camera2Id = "2";
        String pairedCamera2Id = "1";
        Camera camera1 = createMockedCamera(camera1Id, mCameraCoordinator, pairedCamera1Id);
        Camera camera2 = createMockedCamera(camera2Id, mCameraCoordinator, pairedCamera2Id);

        CameraStateRegistry.OnOpenAvailableListener mockOpenListener =
                mock(CameraStateRegistry.OnOpenAvailableListener.class);
        CameraStateRegistry.OnConfigureAvailableListener mockConfigureListener =
                mock(CameraStateRegistry.OnConfigureAvailableListener.class);
        registry.registerCamera(camera1, CameraXExecutors.directExecutor(),
                NO_OP_CONFIGURE_LISTENER, NO_OP_OPEN_LISTENER);
        registry.registerCamera(camera2, CameraXExecutors.directExecutor(),
                mockConfigureListener, mockOpenListener);

        // Open first camera
        registry.tryOpenCamera(camera1);
        registry.markCameraState(camera1, CameraInternal.State.OPEN);

        verify(mockConfigureListener, never()).onConfigureAvailable();

        // Open the second camera
        registry.tryOpenCamera(camera2);
        registry.markCameraState(camera2, CameraInternal.State.OPEN);

        verify(mockConfigureListener, never()).onConfigureAvailable();

        // Configure the first camera
        registry.markCameraState(camera1, CameraInternal.State.CONFIGURED);

        verify(mockConfigureListener, never()).onConfigureAvailable();
    }

    @Test
    public void tryOpenCaptureSession_returnFalseInConcurrentMode() {
        // Only allow a single open camera at a time
        CameraStateRegistry registry = new CameraStateRegistry(mCameraCoordinator, 1);

        String camera1Id = "1";
        String pairedCamera1Id = "2";
        String camera2Id = "2";
        String pairedCamera2Id = "1";
        Camera camera1 = createMockedCamera(camera1Id, mCameraCoordinator, pairedCamera1Id);
        Camera camera2 = createMockedCamera(camera2Id, mCameraCoordinator, pairedCamera2Id);

        CameraStateRegistry.OnOpenAvailableListener mockOpenListener =
                mock(CameraStateRegistry.OnOpenAvailableListener.class);
        CameraStateRegistry.OnConfigureAvailableListener mockConfigureListener =
                mock(CameraStateRegistry.OnConfigureAvailableListener.class);
        registry.registerCamera(camera1, CameraXExecutors.directExecutor(),
                NO_OP_CONFIGURE_LISTENER, NO_OP_OPEN_LISTENER);
        registry.registerCamera(camera2, CameraXExecutors.directExecutor(),
                mockConfigureListener, mockOpenListener);

        // Open first camera
        registry.tryOpenCamera(camera1);
        registry.markCameraState(camera1, CameraInternal.State.OPEN);

        assertThat(registry.tryOpenCaptureSession(camera1Id, pairedCamera1Id)).isTrue();

        // Open the second camera
        registry.tryOpenCamera(camera2);
        registry.markCameraState(camera2, CameraInternal.State.OPEN);

        assertThat(registry.tryOpenCaptureSession(camera2Id, pairedCamera2Id)).isTrue();
    }

    @Test
    public void tryOpenCaptureSession_returnTrueOnlyIfPairedCameraIsAlsoOpenedInConcurrentMode() {
        // Only allow a single open camera at a time
        CameraStateRegistry registry = new CameraStateRegistry(mCameraCoordinator, 1);
        mCameraCoordinator.setCameraOperatingMode(
                CameraCoordinator.CAMERA_OPERATING_MODE_CONCURRENT);

        String camera1Id = "1";
        String pairedCamera1Id = "2";
        String camera2Id = "2";
        String pairedCamera2Id = "1";
        Camera camera1 = createMockedCamera(camera1Id, mCameraCoordinator, pairedCamera1Id);
        Camera camera2 = createMockedCamera(camera2Id, mCameraCoordinator, pairedCamera2Id);

        CameraStateRegistry.OnOpenAvailableListener mockOpenListener =
                mock(CameraStateRegistry.OnOpenAvailableListener.class);
        CameraStateRegistry.OnConfigureAvailableListener mockConfigureListener =
                mock(CameraStateRegistry.OnConfigureAvailableListener.class);
        registry.registerCamera(camera1, CameraXExecutors.directExecutor(),
                NO_OP_CONFIGURE_LISTENER, NO_OP_OPEN_LISTENER);
        registry.registerCamera(camera2, CameraXExecutors.directExecutor(),
                mockConfigureListener, mockOpenListener);

        // Open first camera
        registry.tryOpenCamera(camera1);
        registry.markCameraState(camera1, CameraInternal.State.OPEN);

        assertThat(registry.tryOpenCaptureSession(camera1Id, pairedCamera1Id)).isFalse();
        assertThat(registry.tryOpenCaptureSession(camera2Id, pairedCamera2Id)).isFalse();

        // Open the second camera
        registry.tryOpenCamera(camera2);
        registry.markCameraState(camera2, CameraInternal.State.OPEN);

        assertThat(registry.tryOpenCaptureSession(camera1Id, pairedCamera1Id)).isTrue();
        assertThat(registry.tryOpenCaptureSession(camera2Id, pairedCamera2Id)).isTrue();
    }

    @NonNull
    private static Camera createMockedCamera(
            @NonNull String cameraId,
            @NonNull FakeCameraCoordinator cameraCoordinator,
            @Nullable String pairedCameraId) {

        Camera camera = mock(Camera.class);
        CameraInfoInternal cameraInfoInternal = mock(CameraInfoInternal.class);
        when(camera.getCameraInfo()).thenReturn(cameraInfoInternal);
        when(cameraInfoInternal.getCameraId()).thenReturn(cameraId);

        if (pairedCameraId != null) {
            cameraCoordinator.addConcurrentCameraIdsAndCameraSelectors(
                    new HashMap<String, CameraSelector>() {{
                        put(cameraId, CameraSelector.DEFAULT_BACK_CAMERA);
                        put(pairedCameraId, CameraSelector.DEFAULT_FRONT_CAMERA);
                    }});
        }
        return camera;
    }
}
