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

package androidx.camera.core;

import static com.google.common.truth.Truth.assertThat;

import android.os.Build;

import androidx.camera.testing.fakes.FakeCameraFactory;
import androidx.test.filters.SmallTest;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowLooper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public final class CameraRepositoryTest {

    private CameraRepository mCameraRepository;

    @Before
    public void setUp() {
        mCameraRepository = new CameraRepository();
        mCameraRepository.init(new FakeCameraFactory());
    }

    @Test
    public void cameraIdsCanBeAcquired() {
        Set<String> cameraIds = mCameraRepository.getCameraIds();

        assertThat(cameraIds).isNotEmpty();
    }

    @Test
    public void cameraCanBeObtainedWithValidId() {
        for (String cameraId : mCameraRepository.getCameraIds()) {
            BaseCamera camera = mCameraRepository.getCamera(cameraId);

            assertThat(camera).isNotNull();
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
        List<BaseCamera> cameras = new ArrayList<>();
        for (String cameraId : mCameraRepository.getCameraIds()) {
            cameras.add(mCameraRepository.getCamera(cameraId));
        }

        ListenableFuture<Void> deinitFuture = mCameraRepository.deinit();

        // Needed since FakeCamera uses LiveDataObservable
        ShadowLooper.runUiThreadTasks();

        assertThat(deinitFuture.isDone()).isTrue();
        for (BaseCamera camera : cameras) {
            assertThat(camera.getCameraState().fetchData().get()).isEqualTo(
                    BaseCamera.State.RELEASED);
        }
    }
}
