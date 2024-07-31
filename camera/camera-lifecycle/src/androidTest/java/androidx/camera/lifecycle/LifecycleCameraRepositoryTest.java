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

package androidx.camera.lifecycle;

import static androidx.camera.core.concurrent.CameraCoordinator.CAMERA_OPERATING_MODE_CONCURRENT;

import static com.google.common.truth.Truth.assertThat;

import static java.util.Collections.emptyList;

import androidx.camera.core.CompositionSettings;
import androidx.camera.core.concurrent.CameraCoordinator;
import androidx.camera.core.impl.CameraConfig;
import androidx.camera.core.impl.CameraConfigs;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.RestrictedCameraInfo;
import androidx.camera.core.internal.CameraUseCaseAdapter;
import androidx.camera.testing.fakes.FakeCamera;
import androidx.camera.testing.impl.fakes.FakeCameraConfig;
import androidx.camera.testing.impl.fakes.FakeCameraCoordinator;
import androidx.camera.testing.impl.fakes.FakeCameraDeviceSurfaceManager;
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner;
import androidx.camera.testing.impl.fakes.FakeUseCase;
import androidx.camera.testing.impl.fakes.FakeUseCaseConfigFactory;
import androidx.lifecycle.LifecycleOwner;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@SmallTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 21)
public final class LifecycleCameraRepositoryTest {

    private FakeLifecycleOwner mLifecycle;
    private LifecycleCameraRepository mRepository;
    private CameraCoordinator mCameraCoordinator;
    private CameraUseCaseAdapter mCameraUseCaseAdapter;
    private int mCameraId = 0;
    private CameraInternal mCamera = new FakeCamera(String.valueOf(mCameraId));

    @Before
    public void setUp() {
        mCameraCoordinator = new FakeCameraCoordinator();
        mLifecycle = new FakeLifecycleOwner();
        mRepository = new LifecycleCameraRepository();
        mCameraUseCaseAdapter = new CameraUseCaseAdapter(mCamera,
                mCameraCoordinator,
                new FakeCameraDeviceSurfaceManager(),
                new FakeUseCaseConfigFactory());
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

        // Creates LifecycleCamera with different camera set
        LifecycleCamera secondLifecycleCamera =
                mRepository.createLifecycleCamera(mLifecycle, createNewCameraUseCaseAdapter());

        assertThat(firstLifecycleCamera).isNotEqualTo(secondLifecycleCamera);
    }

    @Test
    public void differentLifecycleCamerasAreCreated_forDifferentCameraConfig() {
        LifecycleCamera firstLifecycleCamera = mRepository.createLifecycleCamera(
                mLifecycle, mCameraUseCaseAdapter);

        // Creates LifecycleCamera with different camera set
        LifecycleCamera secondLifecycleCamera =
                mRepository.createLifecycleCamera(mLifecycle,
                        createCameraUseCaseAdapterWithNewCameraConfig());

        assertThat(firstLifecycleCamera).isNotEqualTo(secondLifecycleCamera);
    }

    @Test
    public void lifecycleCameraIsNotActive_createWithNoUseCasesAfterLifecycleStarted() {
        mLifecycle.start();
        LifecycleCamera lifecycleCamera = mRepository.createLifecycleCamera(mLifecycle,
                mCameraUseCaseAdapter);
        assertThat(lifecycleCamera.isActive()).isFalse();
    }

    @Test
    public void lifecycleCameraIsNotActive_createWithNoUseCasesBeforeLifecycleStarted() {
        LifecycleCamera lifecycleCamera = mRepository.createLifecycleCamera(mLifecycle,
                mCameraUseCaseAdapter);
        mLifecycle.start();
        assertThat(lifecycleCamera.isActive()).isFalse();
    }

    @Test
    public void lifecycleCameraIsNotActive_bindUseCase_whenLifecycleIsNotStarted() {
        LifecycleCamera lifecycleCamera = mRepository.createLifecycleCamera(mLifecycle,
                mCameraUseCaseAdapter);
        mRepository.bindToLifecycleCamera(lifecycleCamera, null, emptyList(),
                Collections.singletonList(new FakeUseCase()), mCameraCoordinator);
        // LifecycleCamera is inactive before the lifecycle state becomes ON_START.
        assertThat(lifecycleCamera.isActive()).isFalse();
    }

    @Test
    public void lifecycleCameraIsActive_lifecycleStartedAfterBindUseCase() {
        LifecycleCamera lifecycleCamera = mRepository.createLifecycleCamera(mLifecycle,
                mCameraUseCaseAdapter);
        mRepository.bindToLifecycleCamera(lifecycleCamera, null, emptyList(),
                Collections.singletonList(new FakeUseCase()), mCameraCoordinator);
        mLifecycle.start();
        // LifecycleCamera is active after the lifecycle state becomes ON_START.
        assertThat(lifecycleCamera.isActive()).isTrue();
    }

    @Test
    public void lifecycleCameraIsActive_bindToLifecycleCameraAfterLifecycleStarted() {
        LifecycleCamera lifecycleCamera = mRepository.createLifecycleCamera(mLifecycle,
                mCameraUseCaseAdapter);
        mLifecycle.start();
        mRepository.bindToLifecycleCamera(lifecycleCamera, null, emptyList(),
                Collections.singletonList(new FakeUseCase()), mCameraCoordinator);

        // LifecycleCamera is active after binding a use case when lifecycle state is ON_START.
        assertThat(lifecycleCamera.isActive()).isTrue();
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwException_withUseCase_twoLifecycleCamerasControlledByOneLifecycle() {
        // Creates first LifecycleCamera with use case bound.
        LifecycleCamera lifecycleCamera0 = mRepository.createLifecycleCamera(
                mLifecycle, mCameraUseCaseAdapter);
        mRepository.bindToLifecycleCamera(lifecycleCamera0, null, emptyList(),
                Collections.singletonList(new FakeUseCase()), mCameraCoordinator);

        // Creates second LifecycleCamera with use case bound to the same Lifecycle.
        LifecycleCamera lifecycleCamera1 = mRepository.createLifecycleCamera(mLifecycle,
                createNewCameraUseCaseAdapter());
        mRepository.bindToLifecycleCamera(lifecycleCamera1, null, emptyList(),
                Collections.singletonList(new FakeUseCase()), mCameraCoordinator);
    }

    @Test
    public void lifecycleCameraIsNotActive_withNoUseCase_unbindAfterLifecycleStarted() {
        // Creates LifecycleCamera with use case bound.
        LifecycleCamera lifecycleCamera = mRepository.createLifecycleCamera(mLifecycle,
                mCameraUseCaseAdapter);
        mLifecycle.start();
        FakeUseCase useCase = new FakeUseCase();
        mRepository.bindToLifecycleCamera(lifecycleCamera, null, emptyList(),
                Collections.singletonList(useCase), mCameraCoordinator);

        // Unbinds the use case that was bound previously.
        mRepository.unbind(Collections.singletonList(useCase));

        // LifecycleCamera is not active if LifecycleCamera has no use case bound after unbinding
        // the use case.
        assertThat(lifecycleCamera.isActive()).isFalse();
    }

    @Test
    public void lifecycleCameraIsActive_withUseCase_unbindAfterLifecycleStarted() {
        // Creates LifecycleCamera with two use cases bound.
        LifecycleCamera lifecycleCamera = mRepository.createLifecycleCamera(mLifecycle,
                mCameraUseCaseAdapter);
        mLifecycle.start();
        FakeUseCase useCase0 = new FakeUseCase();
        FakeUseCase useCase1 = new FakeUseCase();
        mRepository.bindToLifecycleCamera(lifecycleCamera, null, emptyList(),
                Arrays.asList(useCase0, useCase1), mCameraCoordinator);

        // Only unbinds one use case but another one is kept in the LifecycleCamera.
        mRepository.unbind(Collections.singletonList(useCase0));

        // LifecycleCamera is active if LifecycleCamera still has use case bound after unbinding
        // the use case.
        assertThat(lifecycleCamera.isActive()).isTrue();
    }

    @Test
    public void lifecycleCameraIsNotActive_unbindAllAfterLifecycleStarted() {
        // Creates LifecycleCamera with use case bound.
        LifecycleCamera lifecycleCamera = mRepository.createLifecycleCamera(mLifecycle,
                mCameraUseCaseAdapter);
        mLifecycle.start();
        mRepository.bindToLifecycleCamera(lifecycleCamera, null, emptyList(),
                Collections.singletonList(new FakeUseCase()), mCameraCoordinator);

        // Unbinds all use cases from all LifecycleCamera by the unbindAll() API.
        mRepository.unbindAll();

        // LifecycleCamera is not active after unbinding all use cases.
        assertThat(lifecycleCamera.isActive()).isFalse();
    }

    @Test
    public void lifecycleCameraOf1stActiveLifecycleIsInactive_bindToNewActiveLifecycleCamera() {
        // Starts first lifecycle with use case bound.
        LifecycleCamera lifecycleCamera0 = mRepository.createLifecycleCamera(mLifecycle,
                mCameraUseCaseAdapter);
        mLifecycle.start();
        mRepository.bindToLifecycleCamera(lifecycleCamera0, null, emptyList(),
                Collections.singletonList(new FakeUseCase()), mCameraCoordinator);

        // Starts second lifecycle with use case bound.
        FakeLifecycleOwner lifecycle1 = new FakeLifecycleOwner();
        LifecycleCamera lifecycleCamera1 = mRepository.createLifecycleCamera(lifecycle1,
                createNewCameraUseCaseAdapter());
        lifecycle1.start();
        mRepository.bindToLifecycleCamera(lifecycleCamera1, null, emptyList(),
                Collections.singletonList(new FakeUseCase()), mCameraCoordinator);

        // The previous LifecycleCamera becomes inactive after new LifecycleCamera becomes active.
        assertThat(lifecycleCamera0.isActive()).isFalse();
        // New LifecycleCamera becomes active after binding use case to it.
        assertThat(lifecycleCamera1.isActive()).isTrue();
    }

    @Test
    public void lifecycleCameraOf1stActiveLifecycleIsActive_bindNewUseCase() {
        // Starts first lifecycle with use case bound.
        LifecycleCamera lifecycleCamera0 = mRepository.createLifecycleCamera(mLifecycle,
                mCameraUseCaseAdapter);
        mLifecycle.start();
        mRepository.bindToLifecycleCamera(lifecycleCamera0, null, emptyList(),
                Collections.singletonList(new FakeUseCase()), mCameraCoordinator);

        // Starts second lifecycle with use case bound.
        FakeLifecycleOwner lifecycle1 = new FakeLifecycleOwner();
        LifecycleCamera lifecycleCamera1 = mRepository.createLifecycleCamera(lifecycle1,
                createNewCameraUseCaseAdapter());
        lifecycle1.start();
        mRepository.bindToLifecycleCamera(lifecycleCamera1, null, emptyList(),
                Collections.singletonList(new FakeUseCase()), mCameraCoordinator);

        // Binds new use case to the next most recent active LifecycleCamera.
        mRepository.bindToLifecycleCamera(lifecycleCamera0, null, emptyList(),
                Collections.singletonList(new FakeUseCase()), mCameraCoordinator);

        // The next most recent active LifecycleCamera becomes active after binding new use case.
        assertThat(lifecycleCamera0.isActive()).isTrue();
        // The original active LifecycleCamera becomes inactive after the next most recent active
        // LifecycleCamera becomes active.
        assertThat(lifecycleCamera1.isActive()).isFalse();
    }

    @Test
    public void lifecycleCameraOf2ndActiveLifecycleIsActive_unbindFromActiveLifecycleCamera() {
        // Starts first lifecycle with use case bound.
        LifecycleCamera lifecycleCamera0 = mRepository.createLifecycleCamera(mLifecycle,
                mCameraUseCaseAdapter);
        mLifecycle.start();
        mRepository.bindToLifecycleCamera(lifecycleCamera0, null, emptyList(),
                Collections.singletonList(new FakeUseCase()), mCameraCoordinator);

        // Starts second lifecycle with use case bound.
        FakeLifecycleOwner lifecycle1 = new FakeLifecycleOwner();
        LifecycleCamera lifecycleCamera1 = mRepository.createLifecycleCamera(lifecycle1,
                createNewCameraUseCaseAdapter());
        lifecycle1.start();
        FakeUseCase useCase = new FakeUseCase();
        mRepository.bindToLifecycleCamera(lifecycleCamera1, null, emptyList(),
                Collections.singletonList(useCase), mCameraCoordinator);

        // Unbinds use case from the most recent active LifecycleCamera.
        mRepository.unbind(Collections.singletonList(useCase));

        // The most recent active LifecycleCamera becomes inactive after all use case unbound
        // from it.
        assertThat(lifecycleCamera1.isActive()).isFalse();
        // The next most recent active LifecycleCamera becomes active after previous active
        // LifecycleCamera becomes inactive.
        assertThat(lifecycleCamera0.isActive()).isTrue();
    }

    @Test
    public void useCaseIsCleared_whenLifecycleIsDestroyed() {
        LifecycleCamera lifecycleCamera = mRepository.createLifecycleCamera(
                mLifecycle, mCameraUseCaseAdapter);
        FakeUseCase useCase = new FakeUseCase();
        mRepository.bindToLifecycleCamera(lifecycleCamera, null, emptyList(),
                Collections.singletonList(useCase), mCameraCoordinator);

        assertThat(useCase.isDetached()).isFalse();

        mLifecycle.destroy();

        assertThat(useCase.isDetached()).isTrue();
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
        mRepository.bindToLifecycleCamera(firstLifecycleCamera, null, emptyList(),
                Collections.singletonList(new FakeUseCase()), mCameraCoordinator);
        mLifecycle.start();
        assertThat(firstLifecycleCamera.isActive()).isTrue();

        // Starts second lifecycle and check previous LifecycleCamera is stopped.
        FakeLifecycleOwner secondLifecycle = new FakeLifecycleOwner();
        LifecycleCamera secondLifecycleCamera = mRepository.createLifecycleCamera(secondLifecycle,
                createNewCameraUseCaseAdapter());
        mRepository.bindToLifecycleCamera(secondLifecycleCamera, null, emptyList(),
                Collections.singletonList(new FakeUseCase()), mCameraCoordinator);
        secondLifecycle.start();
        assertThat(secondLifecycleCamera.isActive()).isTrue();
        assertThat(firstLifecycleCamera.isActive()).isFalse();
    }

    @Test
    public void lifecycleCameraOf2ndActiveLifecycleIsStarted_when1stActiveLifecycleIsStopped() {
        // Starts first lifecycle and check LifecycleCamera active state is true.
        LifecycleCamera firstLifecycleCamera = mRepository.createLifecycleCamera(
                mLifecycle, mCameraUseCaseAdapter);
        mRepository.bindToLifecycleCamera(firstLifecycleCamera, null, emptyList(),
                Collections.singletonList(new FakeUseCase()), mCameraCoordinator);
        mLifecycle.start();
        assertThat(firstLifecycleCamera.isActive()).isTrue();

        // Starts second lifecycle and check previous LifecycleCamera is stopped.
        FakeLifecycleOwner secondLifecycle = new FakeLifecycleOwner();
        LifecycleCamera secondLifecycleCamera = mRepository.createLifecycleCamera(secondLifecycle,
                createNewCameraUseCaseAdapter());
        mRepository.bindToLifecycleCamera(secondLifecycleCamera, null, emptyList(),
                Collections.singletonList(new FakeUseCase()), mCameraCoordinator);
        secondLifecycle.start();
        assertThat(secondLifecycleCamera.isActive()).isTrue();
        assertThat(firstLifecycleCamera.isActive()).isFalse();

        // Stops second lifecycle and check previous LifecycleCamera is started again.
        secondLifecycle.stop();
        assertThat(secondLifecycleCamera.isActive()).isFalse();
        assertThat(firstLifecycleCamera.isActive()).isTrue();
    }

    @Test
    public void lifecycleCameraWithUseCaseIsActive_whenNewLifecycleCameraWithoutUseCaseIsStarted() {
        // Starts first LifecycleCamera with use case bound.
        LifecycleCamera firstLifecycleCamera = mRepository.createLifecycleCamera(
                mLifecycle, mCameraUseCaseAdapter);
        mRepository.bindToLifecycleCamera(firstLifecycleCamera, null, emptyList(),
                Collections.singletonList(new FakeUseCase()), mCameraCoordinator);
        mLifecycle.start();
        assertThat(firstLifecycleCamera.isActive()).isTrue();

        // Starts second LifecycleCamera without use case bound.
        FakeLifecycleOwner secondLifecycle = new FakeLifecycleOwner();
        LifecycleCamera secondLifecycleCamera = mRepository.createLifecycleCamera(secondLifecycle,
                createNewCameraUseCaseAdapter());
        secondLifecycle.start();

        // The first LifecycleCamera is still active because the second LifecycleCamera won't
        // become active when there is no use case bound.
        assertThat(firstLifecycleCamera.isActive()).isTrue();
        assertThat(secondLifecycleCamera.isActive()).isFalse();
    }

    @Test
    public void onlyLifecycleCameraWithUseCaseIsActive_afterLifecycleIsStarted() {
        // Starts first LifecycleCamera with no use case bound.
        LifecycleCamera lifecycleCamera0 = mRepository.createLifecycleCamera(
                mLifecycle, mCameraUseCaseAdapter);
        mLifecycle.start();

        // Starts second LifecycleCamera with use case bound to the same Lifecycle.
        LifecycleCamera lifecycleCamera1 = mRepository.createLifecycleCamera(
                mLifecycle, createNewCameraUseCaseAdapter());
        mRepository.bindToLifecycleCamera(lifecycleCamera1, null, emptyList(),
                Collections.singletonList(new FakeUseCase()), mCameraCoordinator);

        // Starts third LifecycleCamera with no use case bound to the same Lifecycle.
        LifecycleCamera lifecycleCamera2 = mRepository.createLifecycleCamera(
                mLifecycle, createNewCameraUseCaseAdapter());

        // Checks only the LifecycleCamera with use case bound can become active.
        assertThat(lifecycleCamera0.isActive()).isFalse();
        assertThat(lifecycleCamera1.isActive()).isTrue();
        assertThat(lifecycleCamera2.isActive()).isFalse();

        // Stops and resumes the lifecycle
        mLifecycle.stop();
        mLifecycle.start();

        // Checks still only the LifecycleCamera with use case bound is active.
        assertThat(lifecycleCamera0.isActive()).isFalse();
        assertThat(lifecycleCamera1.isActive()).isTrue();
        assertThat(lifecycleCamera2.isActive()).isFalse();
    }

    @Test
    public void retrievesExistingCamera() {
        LifecycleCamera lifecycleCamera = mRepository.createLifecycleCamera(
                mLifecycle, mCameraUseCaseAdapter);
        LifecycleCamera retrieved = mRepository.getLifecycleCamera(mLifecycle,
                CameraUseCaseAdapter.CameraId.create(mCamera.getCameraInfoInternal().getCameraId(),
                        CameraConfigs.defaultConfig().getCompatibilityId()));

        assertThat(lifecycleCamera).isSameInstanceAs(retrieved);
    }

    @Test
    public void getLifecycleCameraWithDifferentCameraConfig_returnDifferentInstance() {
        LifecycleCamera lifecycleCamera1 = mRepository.createLifecycleCamera(mLifecycle,
                mCameraUseCaseAdapter);

        CameraUseCaseAdapter newCameraUseCaseAdapter =
                createCameraUseCaseAdapterWithNewCameraConfig();
        LifecycleCamera lifecycleCamera2 = mRepository.createLifecycleCamera(mLifecycle,
                newCameraUseCaseAdapter);

        LifecycleCamera retrieved1 = mRepository.getLifecycleCamera(mLifecycle,
                mCameraUseCaseAdapter.getCameraId());

        LifecycleCamera retrieved2 = mRepository.getLifecycleCamera(mLifecycle,
                newCameraUseCaseAdapter.getCameraId());

        assertThat(lifecycleCamera1).isSameInstanceAs(retrieved1);
        assertThat(lifecycleCamera2).isSameInstanceAs(retrieved2);
        assertThat(retrieved1).isNotSameInstanceAs(retrieved2);
    }

    @Test
    public void keys() {
        LifecycleCameraRepository.Key key0 = LifecycleCameraRepository.Key.create(mLifecycle,
                mCameraUseCaseAdapter.getCameraId());
        LifecycleCameraRepository.Key key1 = LifecycleCameraRepository.Key.create(mLifecycle,
                CameraUseCaseAdapter.CameraId.create(mCamera.getCameraInfoInternal().getCameraId(),
                CameraConfigs.defaultConfig().getCompatibilityId()));

        Map<LifecycleCameraRepository.Key, LifecycleOwner> map = new HashMap<>();
        map.put(key0, mLifecycle);
        assertThat(map).containsKey(key1);

        assertThat(key0).isEqualTo(key1);
    }

    @Test
    public void noException_setInactiveAfterUnregisterLifecycle() {
        // This test simulate an ON_STOP event comes after an ON_DESTROY event. It should be an
        // abnormal case and the FakeLifecycleOwner will throw IllegalStateException. See
        // b/222105787 for why this test is added.

        // Starts LifecycleCamera with use case bound.
        LifecycleCamera lifecycleCamera = mRepository.createLifecycleCamera(
                mLifecycle, mCameraUseCaseAdapter);
        mRepository.bindToLifecycleCamera(lifecycleCamera, null, emptyList(),
                Collections.singletonList(new FakeUseCase()), mCameraCoordinator);
        mLifecycle.start();
        assertThat(lifecycleCamera.isActive()).isTrue();

        // This will be called when an ON_DESTROY event is received.
        mRepository.unregisterLifecycle(mLifecycle);

        // This will be called when an ON_STOP event is received.
        mRepository.setInactive(mLifecycle);
    }

    @Test
    public void concurrentModeOn_twoLifecycleCamerasControlledByOneLifecycle_start() {
        mCameraCoordinator.setCameraOperatingMode(CAMERA_OPERATING_MODE_CONCURRENT);

        // Starts first lifecycle camera
        LifecycleCamera lifecycleCamera0 = mRepository.createLifecycleCamera(mLifecycle,
                mCameraUseCaseAdapter);
        mRepository.bindToLifecycleCamera(lifecycleCamera0, null, emptyList(),
                Collections.singletonList(new FakeUseCase()), mCameraCoordinator);

        // Starts second lifecycle camera
        LifecycleCamera lifecycleCamera1 = mRepository.createLifecycleCamera(mLifecycle,
                createNewCameraUseCaseAdapter());
        mRepository.bindToLifecycleCamera(lifecycleCamera1, null, emptyList(),
                Collections.singletonList(new FakeUseCase()), mCameraCoordinator);

        // Starts lifecycle
        mLifecycle.start();

        // Both cameras are active in concurrent mode
        assertThat(lifecycleCamera0.isActive()).isTrue();
        assertThat(lifecycleCamera1.isActive()).isTrue();
    }

    @Test
    public void concurrentModeOn_twoLifecycleCamerasControlledByTwoLifecycles_start() {
        mCameraCoordinator.setCameraOperatingMode(CAMERA_OPERATING_MODE_CONCURRENT);

        // Starts first lifecycle camera
        LifecycleCamera lifecycleCamera0 = mRepository.createLifecycleCamera(mLifecycle,
                mCameraUseCaseAdapter);
        mRepository.bindToLifecycleCamera(lifecycleCamera0, null, emptyList(),
                Collections.singletonList(new FakeUseCase()), mCameraCoordinator);

        // Starts lifecycle
        mLifecycle.start();

        // Starts second lifecycle camera
        FakeLifecycleOwner lifecycle1 = new FakeLifecycleOwner();
        LifecycleCamera lifecycleCamera1 = mRepository.createLifecycleCamera(lifecycle1,
                createNewCameraUseCaseAdapter());
        mRepository.bindToLifecycleCamera(lifecycleCamera1, null, emptyList(),
                Collections.singletonList(new FakeUseCase()), mCameraCoordinator);

        // Starts lifecycle1
        lifecycle1.start();

        // Both cameras are active in concurrent mode
        assertThat(lifecycleCamera0.isActive()).isTrue();
        assertThat(lifecycleCamera1.isActive()).isTrue();
    }

    @Test
    public void concurrentModeOn_twoLifecycleCamerasControlledByOneLifecycle_stop() {
        mCameraCoordinator.setCameraOperatingMode(CAMERA_OPERATING_MODE_CONCURRENT);

        // Starts first lifecycle camera
        LifecycleCamera firstLifecycleCamera = mRepository.createLifecycleCamera(
                mLifecycle, mCameraUseCaseAdapter);
        mRepository.bindToLifecycleCamera(firstLifecycleCamera, null, emptyList(),
                Collections.singletonList(new FakeUseCase()), mCameraCoordinator);

        // Starts second lifecycle camera
        LifecycleCamera secondLifecycleCamera = mRepository.createLifecycleCamera(mLifecycle,
                createNewCameraUseCaseAdapter());
        mRepository.bindToLifecycleCamera(secondLifecycleCamera, null, emptyList(),
                Collections.singletonList(new FakeUseCase()), mCameraCoordinator);

        // Starts lifecycle
        mLifecycle.start();
        assertThat(secondLifecycleCamera.isActive()).isTrue();
        assertThat(firstLifecycleCamera.isActive()).isTrue();

        // Stops lifecycle
        mLifecycle.stop();
        assertThat(secondLifecycleCamera.isActive()).isFalse();
        assertThat(firstLifecycleCamera.isActive()).isFalse();
    }

    @Test
    public void concurrentModeOn_twoLifecycleCamerasControlledByTwoLifecycles_stop() {
        mCameraCoordinator.setCameraOperatingMode(CAMERA_OPERATING_MODE_CONCURRENT);

        // Starts first lifecycle camera
        LifecycleCamera firstLifecycleCamera = mRepository.createLifecycleCamera(
                mLifecycle, mCameraUseCaseAdapter);
        mRepository.bindToLifecycleCamera(firstLifecycleCamera, null, emptyList(),
                Collections.singletonList(new FakeUseCase()), mCameraCoordinator);
        mLifecycle.start();
        assertThat(firstLifecycleCamera.isActive()).isTrue();

        // Starts second lifecycle camera
        FakeLifecycleOwner secondLifecycle = new FakeLifecycleOwner();
        LifecycleCamera secondLifecycleCamera = mRepository.createLifecycleCamera(secondLifecycle,
                createNewCameraUseCaseAdapter());
        mRepository.bindToLifecycleCamera(secondLifecycleCamera, null, emptyList(),
                Collections.singletonList(new FakeUseCase()), mCameraCoordinator);
        secondLifecycle.start();
        assertThat(secondLifecycleCamera.isActive()).isTrue();
        assertThat(firstLifecycleCamera.isActive()).isTrue();

        // Stops lifecycle
        secondLifecycle.stop();
        assertThat(secondLifecycleCamera.isActive()).isFalse();
        assertThat(firstLifecycleCamera.isActive()).isTrue();
    }

    private CameraUseCaseAdapter createNewCameraUseCaseAdapter() {
        String cameraId = String.valueOf(++mCameraId);
        CameraInternal fakeCamera = new FakeCamera(cameraId);
        return new CameraUseCaseAdapter(fakeCamera,
                mCameraCoordinator,
                new FakeCameraDeviceSurfaceManager(),
                new FakeUseCaseConfigFactory());
    }

    private CameraUseCaseAdapter createCameraUseCaseAdapterWithNewCameraConfig() {
        CameraConfig cameraConfig = new FakeCameraConfig();
        return new CameraUseCaseAdapter(mCamera,
                null,
                new RestrictedCameraInfo((CameraInfoInternal) mCamera.getCameraInfo(),
                        cameraConfig),
                null,
                CompositionSettings.DEFAULT,
                CompositionSettings.DEFAULT,
                mCameraCoordinator,
                new FakeCameraDeviceSurfaceManager(),
                new FakeUseCaseConfigFactory());
    }
}
