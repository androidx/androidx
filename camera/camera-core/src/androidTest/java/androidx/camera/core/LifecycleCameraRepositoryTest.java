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

import static com.google.common.truth.Truth.assertThat;

import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.internal.CameraUseCaseAdapter;
import androidx.camera.testing.fakes.FakeCamera;
import androidx.camera.testing.fakes.FakeCameraDeviceSurfaceManager;
import androidx.camera.testing.fakes.FakeLifecycleOwner;
import androidx.camera.testing.fakes.FakeUseCase;
import androidx.lifecycle.LifecycleOwner;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

@SmallTest
@RunWith(AndroidJUnit4.class)
public final class LifecycleCameraRepositoryTest {

    private FakeLifecycleOwner mLifecycle;
    private LifecycleCameraRepository mRepository;
    private CameraUseCaseAdapter mCameraUseCaseAdapter;
    private LinkedHashSet<CameraInternal> mCameraSet;

    @Before
    public void setUp() {
        mLifecycle = new FakeLifecycleOwner();
        mRepository = new LifecycleCameraRepository();
        CameraInternal camera = new FakeCamera();
        mCameraSet = new LinkedHashSet<>(Collections.singleton(camera));
        mCameraUseCaseAdapter = new CameraUseCaseAdapter(camera,
                mCameraSet,
                new FakeCameraDeviceSurfaceManager());
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwException_ifTryingToCreateWithExistingIdentifier() {
        LifecycleCamera firstLifecycleCamera = mRepository.createLifecycleCamera(
                mLifecycle, mCameraUseCaseAdapter);
        LifecycleCamera secondLifecycleCamera = mRepository.createLifecycleCamera(
                mLifecycle, mCameraUseCaseAdapter);

        assertThat(firstLifecycleCamera).isSameInstanceAs(secondLifecycleCamera);
    }

    @Test
    public void differentLifecycleCamerasAreCreated_forDifferentLifecycles() {
        LifecycleCamera firstLifecycleCamera = mRepository.createLifecycleCamera(
                mLifecycle, mCameraUseCaseAdapter);
        FakeLifecycleOwner secondLifecycle = new FakeLifecycleOwner();
        LifecycleCamera secondLifecycleCamera =
                mRepository.createLifecycleCamera(secondLifecycle,
                        mCameraUseCaseAdapter);

        assertThat(firstLifecycleCamera).isNotEqualTo(secondLifecycleCamera);
    }

    @Test
    public void differentLifecycleCamerasAreCreated_forDifferentCameraSets() {
        LifecycleCamera firstLifecycleCamera = mRepository.createLifecycleCamera(
                mLifecycle, mCameraUseCaseAdapter);

        CameraInternal fakeCamera = new FakeCamera("other");
        CameraUseCaseAdapter cameraUseCaseAdapter = new CameraUseCaseAdapter(fakeCamera,
                new LinkedHashSet<>(Collections.singleton(fakeCamera)),
                new FakeCameraDeviceSurfaceManager());

        LifecycleCamera secondLifecycleCamera =
                mRepository.createLifecycleCamera(mLifecycle,
                        cameraUseCaseAdapter);

        assertThat(firstLifecycleCamera).isNotEqualTo(secondLifecycleCamera);
    }

    @Test
    public void useCaseIsCleared_whenLifecycleIsDestroyed() throws
            CameraUseCaseAdapter.CameraException {
        LifecycleCamera lifecycleCamera = mRepository.createLifecycleCamera(
                mLifecycle, mCameraUseCaseAdapter);
        FakeUseCase useCase = new FakeUseCase();
        lifecycleCamera.bind(Collections.singleton(useCase));

        assertThat(useCase.isCleared()).isFalse();

        mLifecycle.destroy();

        assertThat(useCase.isCleared()).isTrue();
    }

    @Test(expected = IllegalArgumentException.class)
    public void exception_whenCreatingWithDestroyedLifecycle() {
        mLifecycle.destroy();

        // Should throw IllegalArgumentException
        mRepository.createLifecycleCamera(mLifecycle, mCameraUseCaseAdapter);
    }

    @Test
    public void lifecycleCameraIsStopped_whenNewLifecycleIsStarted() {
        // Starts first lifecycle and check LifecycleCamera active state is true.
        LifecycleCamera firstLifecycleCamera = mRepository.createLifecycleCamera(
                mLifecycle, mCameraUseCaseAdapter);
        mLifecycle.start();
        assertThat(firstLifecycleCamera.isActive()).isTrue();

        // Starts second lifecycle and check previous LifecycleCamera is stopped.
        FakeLifecycleOwner secondLifecycle = new FakeLifecycleOwner();
        LifecycleCamera secondLifecycleCamera =
                mRepository.createLifecycleCamera(
                        secondLifecycle, mCameraUseCaseAdapter);
        secondLifecycle.start();
        assertThat(secondLifecycleCamera.isActive()).isTrue();
        assertThat(firstLifecycleCamera.isActive()).isFalse();
    }

    @Test
    public void prioritizeStartedCamera() {
        // Arrange.
        // Starts first lifecycle and check LifecycleCamera active state is true.
        LifecycleCamera firstLifecycleCamera = mRepository.createLifecycleCamera(
                mLifecycle, mCameraUseCaseAdapter);
        mLifecycle.start();

        // Starts second lifecycle and check previous LifecycleCamera is stopped.
        FakeLifecycleOwner secondLifecycle = new FakeLifecycleOwner();
        LifecycleCamera secondLifecycleCamera =
                mRepository.createLifecycleCamera(
                        secondLifecycle, mCameraUseCaseAdapter);
        secondLifecycle.start();

        // Act. Call prioritize()
        mRepository.prioritize(firstLifecycleCamera);

        // Assert. The camera that was set as active should be active now. All other cameras will
        // be inactive.
        assertThat(firstLifecycleCamera.isActive()).isTrue();
        assertThat(secondLifecycleCamera.isActive()).isFalse();
    }

    @Test
    public void prioritizeNonStartedCamera() {
        // Arrange.
        // Starts first lifecycle and check LifecycleCamera active state is true.
        LifecycleCamera firstLifecycleCamera = mRepository.createLifecycleCamera(
                mLifecycle, mCameraUseCaseAdapter);
        mLifecycle.start();

        // Starts second lifecycle and check previous LifecycleCamera is stopped.
        FakeLifecycleOwner secondLifecycle = new FakeLifecycleOwner();
        LifecycleCamera secondLifecycleCamera =
                mRepository.createLifecycleCamera(
                        secondLifecycle, mCameraUseCaseAdapter);
        secondLifecycle.start();

        mLifecycle.stop();

        // Act. Call prioritize()
        mRepository.prioritize(firstLifecycleCamera);

        // Assert. The camera that was set as active should be active now. All other cameras will
        // be inactive.
        assertThat(firstLifecycleCamera.isActive()).isFalse();
        assertThat(secondLifecycleCamera.isActive()).isTrue();
    }

    @Test
    public void prioritizeNonCurrentCameraMaintainsState() {
        // Arrange.
        // Starts first lifecycle
        LifecycleCamera lifecycleCamera0 = mRepository.createLifecycleCamera(
                mLifecycle, mCameraUseCaseAdapter);
        mLifecycle.start();

        // Starts second lifecycle
        FakeLifecycleOwner lifecycle1 = new FakeLifecycleOwner();
        LifecycleCamera lifecycleCamera1 =
                mRepository.createLifecycleCamera(
                        lifecycle1, mCameraUseCaseAdapter);
        lifecycle1.start();

        // Starts third lifecycle
        FakeLifecycleOwner lifecycle2 = new FakeLifecycleOwner();
        LifecycleCamera lifecycleCamera2 =
                mRepository.createLifecycleCamera(
                        lifecycle2, mCameraUseCaseAdapter);
        lifecycle2.start();

        // Act. Call prioritize() on camera that isn't the current active camera. The order of
        // active camera priorities from highest priority to least is:
        // 2, 1, 0
        // After calling prioritize() it is:
        // 1, 2, 0
        mRepository.prioritize(lifecycleCamera1);
        // Stop the lifecycles for 1 and 2 so then the only camera remaining as active should be 0
        lifecycle1.stop();
        lifecycle2.stop();

        // Assert. Make sure that prioritize maintains correct internal state
        assertThat(lifecycleCamera0.isActive()).isTrue();
        assertThat(lifecycleCamera1.isActive()).isFalse();
        assertThat(lifecycleCamera2.isActive()).isFalse();
    }

    @Test
    public void lifecycleCameraOf2ndActiveLifecycleIsStarted_when1stActiveLifecycleIsStopped() {
        // Starts first lifecycle and check LifecycleCamera active state is true.
        LifecycleCamera firstLifecycleCamera = mRepository.createLifecycleCamera(
                mLifecycle, mCameraUseCaseAdapter);
        mLifecycle.start();
        assertThat(firstLifecycleCamera.isActive()).isTrue();

        // Starts second lifecycle and check previous LifecycleCamera is stopped.
        FakeLifecycleOwner secondLifecycle = new FakeLifecycleOwner();
        LifecycleCamera secondLifecycleCamera =
                mRepository.createLifecycleCamera(
                        secondLifecycle, mCameraUseCaseAdapter);
        secondLifecycle.start();
        assertThat(secondLifecycleCamera.isActive()).isTrue();
        assertThat(firstLifecycleCamera.isActive()).isFalse();

        // Stops second lifecycle and check previous LifecycleCamera is started again.
        secondLifecycle.stop();
        assertThat(secondLifecycleCamera.isActive()).isFalse();
        assertThat(firstLifecycleCamera.isActive()).isTrue();
    }

    @Test
    public void retrievesExistingCamera() {
        LifecycleCamera lifecycleCamera = mRepository.createLifecycleCamera(
                mLifecycle, mCameraUseCaseAdapter);
        CameraUseCaseAdapter.CameraId cameraId = CameraUseCaseAdapter.generateCameraId(mCameraSet);
        LifecycleCamera retrieved = mRepository.getLifecycleCamera(mLifecycle, cameraId);

        assertThat(lifecycleCamera).isSameInstanceAs(retrieved);
    }

    @Test
    public void keys() {
        LifecycleCameraRepository.Key key0 = LifecycleCameraRepository.Key.create(mLifecycle,
                mCameraUseCaseAdapter.getCameraId());
        LifecycleCameraRepository.Key key1 = LifecycleCameraRepository.Key.create(mLifecycle,
                CameraUseCaseAdapter.generateCameraId(mCameraSet));

        Map<LifecycleCameraRepository.Key, LifecycleOwner> map = new HashMap<>();
        map.put(key0, mLifecycle);
        assertThat(map).containsKey(key1);

        assertThat(key0).isEqualTo(key1);
    }
}
