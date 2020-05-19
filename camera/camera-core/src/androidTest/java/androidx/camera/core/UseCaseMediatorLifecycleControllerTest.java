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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.internal.CameraUseCaseAdapter;
import androidx.camera.testing.fakes.FakeCameraDeviceSurfaceManager;
import androidx.camera.testing.fakes.FakeLifecycleOwner;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class UseCaseMediatorLifecycleControllerTest {
    private UseCaseMediatorLifecycleController mUseCaseMediatorLifecycleController;
    private FakeLifecycleOwner mLifecycleOwner;
    private CameraUseCaseAdapter mCameraUseCaseAdapter;
    private CameraInternal mMockCamera = mock(CameraInternal.class);

    @Before
    public void setUp() {
        mLifecycleOwner = new FakeLifecycleOwner();
        mCameraUseCaseAdapter = new CameraUseCaseAdapter(mMockCamera,
                new FakeCameraDeviceSurfaceManager());
    }

    @Test
    public void mediatorCanBeMadeObserverOfLifecycle() {
        assertThat(mLifecycleOwner.getObserverCount()).isEqualTo(0);

        mUseCaseMediatorLifecycleController =
                new UseCaseMediatorLifecycleController(mLifecycleOwner, mCameraUseCaseAdapter);

        assertThat(mLifecycleOwner.getObserverCount()).isEqualTo(1);
    }

    @Test
    public void mediatorCanStopObservingALifeCycle() {
        mUseCaseMediatorLifecycleController =
                new UseCaseMediatorLifecycleController(mLifecycleOwner, mCameraUseCaseAdapter);
        assertThat(mLifecycleOwner.getObserverCount()).isEqualTo(1);

        mUseCaseMediatorLifecycleController.release();

        assertThat(mLifecycleOwner.getObserverCount()).isEqualTo(0);
    }

    @Test
    public void mediatorCanBeReleasedMultipleTimes() {
        mUseCaseMediatorLifecycleController =
                new UseCaseMediatorLifecycleController(mLifecycleOwner, mCameraUseCaseAdapter);

        mUseCaseMediatorLifecycleController.release();
        mUseCaseMediatorLifecycleController.release();
    }

    @Test
    public void lifecycleStart_triggersAttach() {
        mUseCaseMediatorLifecycleController =
                new UseCaseMediatorLifecycleController(mLifecycleOwner, mCameraUseCaseAdapter);

        mLifecycleOwner.start();

        verify(mMockCamera, times(1)).attachUseCases(any());
    }

    @Test
    public void lifecycleStop_triggersDetach() {
        mUseCaseMediatorLifecycleController =
                new UseCaseMediatorLifecycleController(mLifecycleOwner, mCameraUseCaseAdapter);
        mLifecycleOwner.start();

        mLifecycleOwner.stop();

        verify(mMockCamera, times(1)).detachUseCases(any());
    }
}
