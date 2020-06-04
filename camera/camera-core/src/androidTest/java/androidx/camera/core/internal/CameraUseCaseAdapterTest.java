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

package androidx.camera.core.internal;

import static com.google.common.truth.Truth.assertThat;

import androidx.camera.testing.fakes.FakeCamera;
import androidx.camera.testing.fakes.FakeCameraDeviceSurfaceManager;
import androidx.camera.testing.fakes.FakeUseCase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;

/** JUnit test cases for {@link CameraUseCaseAdapter} class. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class CameraUseCaseAdapterTest {
    FakeCameraDeviceSurfaceManager mFakeCameraDeviceSurfaceManager;
    FakeCamera mFakeCamera;

    @Before
    public void setUp() {
        mFakeCameraDeviceSurfaceManager = new FakeCameraDeviceSurfaceManager();
        mFakeCamera =  new FakeCamera();
    }

    @Test
    public void attachUseCases() throws CameraUseCaseAdapter.CameraException {
        CameraUseCaseAdapter cameraUseCaseAdapter = new CameraUseCaseAdapter(mFakeCamera,
                mFakeCameraDeviceSurfaceManager);
        FakeUseCase fakeUseCase = new FakeUseCase();
        cameraUseCaseAdapter.attachUseCases(Collections.singleton(fakeUseCase));

        assertThat(fakeUseCase.getCamera()).isEqualTo(mFakeCamera);
    }

    @Test
    public void detachUseCases() throws CameraUseCaseAdapter.CameraException {
        CameraUseCaseAdapter cameraUseCaseAdapter = new CameraUseCaseAdapter(mFakeCamera,
                mFakeCameraDeviceSurfaceManager);
        FakeUseCase fakeUseCase = new FakeUseCase();
        cameraUseCaseAdapter.attachUseCases(Collections.singleton(fakeUseCase));
        cameraUseCaseAdapter.detachUseCases(Collections.singleton(fakeUseCase));

        assertThat(fakeUseCase.getCamera()).isNull();
    }
}
