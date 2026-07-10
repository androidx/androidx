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

import static android.os.Looper.getMainLooper;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.fail;
import static org.robolectric.Shadows.shadowOf;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraUnavailableException;
import androidx.camera.core.InitializationException;
import androidx.camera.testing.fakes.FakeCamera;
import androidx.camera.testing.fakes.FakeCameraInfoInternal;
import androidx.camera.testing.impl.fakes.FakeCameraFactory;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(sdk = {Config.ALL_SDKS})
public final class CameraRepositoryTest {

    private static final String CAMERA_ID_0 = "0";
    private static final String CAMERA_ID_1 = "1";
    private static final String CAMERA_ID_2 = "2";

    private CameraRepository mCameraRepository;
    private FakeCameraFactory mFakeCameraFactory;
    private FakeCamera mCamera0;
    private FakeCamera mCamera1;
    private FakeCamera mCamera2;

    @Before
    public void setUp() throws InitializationException, CameraUnavailableException {
        mCameraRepository = new CameraRepository();
        mFakeCameraFactory = new FakeCameraFactory();

        // Use fake CameraInternal to easily verify method calls
        mCamera0 = new FakeCamera(CAMERA_ID_0, null,
                new FakeCameraInfoInternal(0, CameraSelector.LENS_FACING_BACK));
        mCamera1 = new FakeCamera(CAMERA_ID_1, null,
                new FakeCameraInfoInternal(0, CameraSelector.LENS_FACING_FRONT));
        mCamera2 = new FakeCamera(CAMERA_ID_2, null,
                new FakeCameraInfoInternal(0, CameraSelector.LENS_FACING_EXTERNAL));

        // Configure the factory to return our fake camera
        mFakeCameraFactory.insertCamera(CameraSelector.LENS_FACING_BACK, CAMERA_ID_0,
                () -> mCamera0);
        mFakeCameraFactory.insertCamera(CameraSelector.LENS_FACING_FRONT, CAMERA_ID_1,
                () -> mCamera1);
        // Don't add camera 2 to the factory yet, we'll do it in specific tests.

        // Initial state of the repository
        mCameraRepository.init(mFakeCameraFactory);
    }

    @Test
    public void cameraIdsCanBeAcquired() {
        Set<String> cameraIds = mCameraRepository.getCameraIds();

        assertThat(cameraIds).isNotEmpty();
    }

    @Test
    public void cameraCanBeObtainedWithValidId() {
        for (String cameraId : mCameraRepository.getCameraIds()) {
            CameraInternal cameraInternal = mCameraRepository.getCamera(cameraId);

            assertThat(cameraInternal).isNotNull();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void cameraCannotBeObtainedWithInvalidId() {
        // Should throw IllegalArgumentException
        mCameraRepository.getCamera("no_such_id");
    }

    @Test
    public void cameraIdsAreClearedAfterDeinit() {
        Set<String> cameraIdsBefore = mCameraRepository.getCameraIds();
        mCameraRepository.deinit();
        Set<String> cameraIdsAfter = mCameraRepository.getCameraIds();

        assertThat(cameraIdsBefore).isNotEmpty();
        assertThat(cameraIdsAfter).isEmpty();
    }

    @Test(expected = IllegalArgumentException.class)
    public void camerasAreClearedAfterDeinit() {
        Set<String> cameraIds = mCameraRepository.getCameraIds();
        String validId = cameraIds.iterator().next();
        mCameraRepository.deinit();
        mCameraRepository.getCamera(validId);
    }

    @Test
    public void camerasAreReleasedByDeinit() throws ExecutionException, InterruptedException {
        List<CameraInternal> cameraInternals = new ArrayList<>();
        for (String cameraId : mCameraRepository.getCameraIds()) {
            cameraInternals.add(
                    mCameraRepository.getCamera(cameraId));
        }

        ListenableFuture<Void> deinitFuture = mCameraRepository.deinit();

        // Needed since FakeCamera uses LiveDataObservable
        shadowOf(getMainLooper()).idle();

        assertThat(deinitFuture.isDone()).isTrue();
        for (CameraInternal cameraInternal : cameraInternals) {
            ListenableFuture<CameraInternal.State> stateFuture =
                    cameraInternal.getCameraState().fetchData();
            // Needed since FakeCamera uses LiveDataObservable
            shadowOf(getMainLooper()).idle();
            assertThat(stateFuture.get()).isEqualTo(CameraInternal.State.RELEASED);
        }
    }

    @Test
    public void onCamerasUpdated_addsNewCamera() throws CameraUpdateException {
        // Arrange
        // Make camera 2 available in the factory for this test
        mFakeCameraFactory.insertCamera(CameraSelector.LENS_FACING_EXTERNAL, CAMERA_ID_2,
                () -> mCamera2);
        List<String> newCameraIdSet = Arrays.asList(CAMERA_ID_0, CAMERA_ID_1, CAMERA_ID_2);

        // Act
        mCameraRepository.onCamerasUpdated(newCameraIdSet);

        // Assert
        assertThat(mCameraRepository.getCameraIds()).containsExactly(CAMERA_ID_0, CAMERA_ID_1,
                CAMERA_ID_2);
        assertThat(mCameraRepository.getCamera(CAMERA_ID_2)).isSameInstanceAs(mCamera2);
    }

    @Test
    public void onCamerasUpdated_removesCameraAndCallsDisconnect() throws CameraUpdateException {
        // Arrange
        List<String> newCameraIdSet = new ArrayList<>(Collections.singletonList(CAMERA_ID_1));
        assertThat(mCamera0.isRemoved()).isFalse();
        assertThat(mCamera1.isRemoved()).isFalse();

        // Act
        mCameraRepository.onCamerasUpdated(newCameraIdSet);

        // Assert
        assertThat(mCameraRepository.getCameraIds()).containsExactly(CAMERA_ID_1);
        assertThat(mCamera0.isRemoved()).isTrue();
        assertThat(mCamera1.isRemoved()).isFalse();
    }

    @Test
    public void onCamerasUpdated_abortsAddTransactionOnError() {
        // Arrange: Configure the factory to fail when creating camera "2"
        mFakeCameraFactory.insertCamera(CameraSelector.LENS_FACING_EXTERNAL, CAMERA_ID_2, () -> {
            throw new CameraUnavailableException(1, "test failure");
        });
        List<String> newCameraIdSet = Arrays.asList(CAMERA_ID_0, CAMERA_ID_1, CAMERA_ID_2);

        // Act & Assert
        try {
            mCameraRepository.onCamerasUpdated(newCameraIdSet);
            // If this line is reached, the test should fail.
            fail("Expected CameraUpdateException was not thrown.");
        } catch (CameraUpdateException e) {
            // The correct exception was thrown. Test passes.
            // We can also assert that the cause is what we expect.
            assertThat(e.getCause()).isInstanceOf(CameraUnavailableException.class);
        }

        // Assert: The repository state should not have changed.
        assertThat(mCameraRepository.getCameraIds()).containsExactly(CAMERA_ID_0, CAMERA_ID_1);
    }

    @Test
    public void onCamerasUpdated_reordersCameras() throws CameraUpdateException {
        // Arrange: New set has same elements but different order.
        List<String> newCameraIdSet = Arrays.asList(CAMERA_ID_1, CAMERA_ID_0);

        // Act
        mCameraRepository.onCamerasUpdated(newCameraIdSet);

        // Assert
        assertThat(mCamera0.isRemoved()).isFalse();
        assertThat(mCamera1.isRemoved()).isFalse();
        assertThat(mCameraRepository.getCameraIds()).containsExactly(CAMERA_ID_1,
                CAMERA_ID_0).inOrder();
    }
}
