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

package androidx.camera.camera2.internal;

import static com.google.common.truth.Truth.assertThat;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraInternal;
import androidx.camera.core.Observable;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.testing.fakes.FakeCamera;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowLooper;

import java.util.concurrent.atomic.AtomicReference;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public final class CameraAvailabilityRegistryTest {
    private int mAvailableCameraCount;
    private Observable.Observer<Integer> mCountObserver = new Observable.Observer<Integer>() {

        @Override
        public void onNewData(@Nullable Integer value) {
            mAvailableCameraCount = value;
        }

        @Override
        public void onError(@NonNull Throwable t) {

        }
    };

    @Before
    public void setUp() {
        mAvailableCameraCount = 0;
    }

    @Test
    public void singleOpenCamera_reducesAvailableCameras() {
        CameraAvailabilityRegistry registry = new CameraAvailabilityRegistry(1,
                CameraXExecutors.directExecutor());

        Observable<Integer> cameraCountObservable = registry.getAvailableCameraCount();

        cameraCountObservable.addObserver(CameraXExecutors.directExecutor(), mCountObserver);

        ShadowLooper.runUiThreadTasks();

        int initialAvailableCount = mAvailableCameraCount;

        FakeCamera camera = new FakeCamera();
        camera.open();

        registry.registerCamera(camera);

        ShadowLooper.runUiThreadTasks();

        int finalAvailableCount = mAvailableCameraCount;

        cameraCountObservable.removeObserver(mCountObserver);

        assertThat(initialAvailableCount).isEqualTo(1);
        assertThat(finalAvailableCount).isEqualTo(0);
    }

    @Test
    public void singleClosedCamera_doesNotReduceAvailableCameras() {
        CameraAvailabilityRegistry registry = new CameraAvailabilityRegistry(1,
                CameraXExecutors.directExecutor());

        Observable<Integer> cameraCountObservable = registry.getAvailableCameraCount();

        cameraCountObservable.addObserver(CameraXExecutors.directExecutor(), mCountObserver);

        ShadowLooper.runUiThreadTasks();

        int initialAvailableCount = mAvailableCameraCount;

        FakeCamera camera = new FakeCamera();
        // Do not open the camera. Leave in this state.

        registry.registerCamera(camera);

        ShadowLooper.runUiThreadTasks();

        int finalAvailableCount = mAvailableCameraCount;

        cameraCountObservable.removeObserver(mCountObserver);

        assertThat(initialAvailableCount).isEqualTo(1);
        assertThat(finalAvailableCount).isEqualTo(1);
    }

    @Test
    public void closingCameras_increasesAvailableCount() {
        CameraAvailabilityRegistry registry = new CameraAvailabilityRegistry(1,
                CameraXExecutors.directExecutor());

        Observable<Integer> cameraCountObservable = registry.getAvailableCameraCount();

        cameraCountObservable.addObserver(CameraXExecutors.directExecutor(), mCountObserver);

        FakeCamera camera = new FakeCamera();
        camera.open();

        registry.registerCamera(camera);

        ShadowLooper.runUiThreadTasks();

        int initialAvailableCount = mAvailableCameraCount;

        camera.close();

        ShadowLooper.runUiThreadTasks();

        int finalAvailableCount = mAvailableCameraCount;

        cameraCountObservable.removeObserver(mCountObserver);

        assertThat(initialAvailableCount).isEqualTo(0);
        assertThat(finalAvailableCount).isEqualTo(1);
    }

    @Test
    public void releasingCameras_increasesAvailableCount() {
        CameraAvailabilityRegistry registry = new CameraAvailabilityRegistry(1,
                CameraXExecutors.directExecutor());

        Observable<Integer> cameraCountObservable = registry.getAvailableCameraCount();

        cameraCountObservable.addObserver(CameraXExecutors.directExecutor(), mCountObserver);

        FakeCamera camera = new FakeCamera();
        camera.open();

        registry.registerCamera(camera);

        ShadowLooper.runUiThreadTasks();

        int initialAvailableCount = mAvailableCameraCount;

        camera.release();

        ShadowLooper.runUiThreadTasks();

        int finalAvailableCount = mAvailableCameraCount;

        cameraCountObservable.removeObserver(mCountObserver);

        assertThat(initialAvailableCount).isEqualTo(0);
        assertThat(finalAvailableCount).isEqualTo(1);
    }

    @Test
    public void availableCount_neverGoesNegative() {
        CameraAvailabilityRegistry registry = new CameraAvailabilityRegistry(1,
                CameraXExecutors.directExecutor());

        Observable<Integer> cameraCountObservable = registry.getAvailableCameraCount();

        cameraCountObservable.addObserver(CameraXExecutors.directExecutor(), mCountObserver);

        FakeCamera camera1 = new FakeCamera();
        camera1.open();
        FakeCamera camera2 = new FakeCamera();
        camera2.open();

        registry.registerCamera(camera1);
        registry.registerCamera(camera2);

        ShadowLooper.runUiThreadTasks();

        int finalAvailableCount = mAvailableCameraCount;

        cameraCountObservable.removeObserver(mCountObserver);

        assertThat(finalAvailableCount).isEqualTo(0);
    }

    @Test
    public void pendingOpen_isNotCountedAsOpen() {
        CameraAvailabilityRegistry registry = new CameraAvailabilityRegistry(1,
                CameraXExecutors.directExecutor());

        Observable<Integer> cameraCountObservable = registry.getAvailableCameraCount();

        cameraCountObservable.addObserver(CameraXExecutors.directExecutor(), mCountObserver);

        FakeCamera camera = new FakeCamera();

        AtomicReference<CameraInternal.State> cameraState = new AtomicReference<>(null);
        Observable.Observer<CameraInternal.State> stateObserver =
                new Observable.Observer<CameraInternal.State>() {

                    @Override
                    public void onNewData(@Nullable CameraInternal.State value) {
                        cameraState.set(value);
                    }

                    @Override
                    public void onError(@NonNull Throwable t) {

                    }
                };

        camera.getCameraState().addObserver(CameraXExecutors.directExecutor(), stateObserver);
        camera.setAvailableCameraCount(0);
        camera.open();

        ShadowLooper.runUiThreadTasks();

        camera.getCameraState().removeObserver(stateObserver);
        cameraCountObservable.removeObserver(mCountObserver);

        // Ensure that even though the camera is the PENDING_OPEN state, there is still 1 camera
        // available to be opened.
        assertThat(cameraState.get()).isEqualTo(CameraInternal.State.PENDING_OPEN);
        assertThat(mAvailableCameraCount).isEqualTo(1);
    }
}
