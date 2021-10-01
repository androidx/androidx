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

import static com.google.common.truth.Truth.assertThat;

import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.MutableOptionsBundle;
import androidx.camera.core.internal.CameraUseCaseAdapter;
import androidx.camera.testing.fakes.FakeCamera;
import androidx.camera.testing.fakes.FakeCameraDeviceSurfaceManager;
import androidx.camera.testing.fakes.FakeLifecycleOwner;
import androidx.camera.testing.fakes.FakeUseCase;
import androidx.camera.testing.fakes.FakeUseCaseConfigFactory;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.LinkedHashSet;

@SmallTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 21)
public class LifecycleCameraTest {
    private LifecycleCamera mLifecycleCamera;
    private FakeLifecycleOwner mLifecycleOwner;
    private CameraUseCaseAdapter mCameraUseCaseAdapter;
    private FakeCamera mFakeCamera;
    private FakeUseCase mFakeUseCase;

    @Before
    public void setUp() {
        mLifecycleOwner = new FakeLifecycleOwner();
        mFakeCamera = new FakeCamera();
        mCameraUseCaseAdapter = new CameraUseCaseAdapter(
                new LinkedHashSet<>(Collections.singleton(mFakeCamera)),
                new FakeCameraDeviceSurfaceManager(),
                new FakeUseCaseConfigFactory());
        mFakeUseCase = new FakeUseCase();
    }

    @Test
    public void lifecycleCameraCanBeMadeObserverOfLifecycle() {
        assertThat(mLifecycleOwner.getObserverCount()).isEqualTo(0);

        mLifecycleCamera = new LifecycleCamera(mLifecycleOwner, mCameraUseCaseAdapter);

        assertThat(mLifecycleOwner.getObserverCount()).isEqualTo(1);
    }

    @Test
    public void lifecycleCameraCanStopObservingALifecycle() {
        mLifecycleCamera = new LifecycleCamera(mLifecycleOwner, mCameraUseCaseAdapter);

        assertThat(mLifecycleOwner.getObserverCount()).isEqualTo(1);

        mLifecycleCamera.release();

        assertThat(mLifecycleOwner.getObserverCount()).isEqualTo(0);
    }

    @Test
    public void lifecycleCameraCanBeReleasedMultipleTimes() {
        mLifecycleCamera = new LifecycleCamera(mLifecycleOwner, mCameraUseCaseAdapter);

        mLifecycleCamera.release();
        mLifecycleCamera.release();
    }

    @Test
    public void lifecycleStart_triggersOnActive() {
        mLifecycleCamera = new LifecycleCamera(mLifecycleOwner, mCameraUseCaseAdapter);

        mLifecycleOwner.start();

        assertThat(mLifecycleCamera.isActive()).isTrue();
    }

    @Test
    public void lifecycleStop_triggersOnInactive() {
        mLifecycleCamera = new LifecycleCamera(mLifecycleOwner, mCameraUseCaseAdapter);

        mLifecycleOwner.start();

        mLifecycleOwner.stop();

        assertThat(mLifecycleCamera.isActive()).isFalse();
    }

    @Test
    public void lifecycleStart_doesNotTriggerOnActiveIfSuspended() {
        mLifecycleCamera = new LifecycleCamera(mLifecycleOwner, mCameraUseCaseAdapter);

        mLifecycleCamera.suspend();
        mLifecycleOwner.start();

        assertThat(mLifecycleCamera.isActive()).isFalse();
    }

    @Test
    public void lifecycleStart_restoreInteropConfig() {
        // Set an config to CameraControl internally.
        Config.Option<Integer> option = Config.Option.create("OPTION_ID", Integer.class);
        Integer value = 1;
        MutableOptionsBundle originalConfig = MutableOptionsBundle.create();
        originalConfig.insertOption(option, value);
        mFakeCamera.getCameraControlInternal().addInteropConfig(originalConfig);

        mLifecycleCamera = new LifecycleCamera(mLifecycleOwner, mCameraUseCaseAdapter);

        mLifecycleOwner.start();

        // Stop the lifecycle. The original config is cached and the config in CameraControl is
        // cleared internally.
        mLifecycleOwner.stop();

        // Set a different config.
        mFakeCamera.getCameraControlInternal().addInteropConfig(MutableOptionsBundle.create());

        // Starts the lifecycle and the cached config is restored internally.
        mLifecycleOwner.start();

        // Check the config in CameraControl has the same value as the original config.
        assertThat(
                mFakeCamera.getCameraControlInternal().getInteropConfig().containsOption(
                        option)).isTrue();
        assertThat(
                mFakeCamera.getCameraControlInternal().getInteropConfig().retrieveOption(
                        option)).isEqualTo(value);
    }

    @Test
    public void lifecycleStop_clearInteropConfig() {
        // Set an config to CameraControl.
        Config config = MutableOptionsBundle.create();
        mFakeCamera.getCameraControlInternal().addInteropConfig(config);

        mLifecycleCamera = new LifecycleCamera(mLifecycleOwner, mCameraUseCaseAdapter);

        mLifecycleOwner.start();

        // Stop the lifecycle. The original config is cached and the config in CameraControl is
        // cleared internally.
        mLifecycleOwner.stop();

        // Check the config in CameraControl is empty.
        assertThat(
                mFakeCamera.getCameraControlInternal().getInteropConfig().listOptions()).isEmpty();
    }

    @Test
    public void unsuspendOfStartedLifecycle_triggersOnActive() {
        mLifecycleCamera = new LifecycleCamera(mLifecycleOwner, mCameraUseCaseAdapter);

        mLifecycleCamera.suspend();
        mLifecycleOwner.start();
        mLifecycleCamera.unsuspend();

        assertThat(mLifecycleCamera.isActive()).isTrue();
    }

    @Test
    public void bind_willBindToCameraInternal() throws CameraUseCaseAdapter.CameraException {
        mLifecycleCamera = new LifecycleCamera(mLifecycleOwner, mCameraUseCaseAdapter);
        mLifecycleOwner.start();

        mLifecycleCamera.bind(Collections.singleton(mFakeUseCase));

        assertThat(mFakeCamera.getAttachedUseCases()).containsExactly(mFakeUseCase);
    }

    @Test
    public void unbind_willUnbindFromCameraInternal() throws CameraUseCaseAdapter.CameraException {
        mLifecycleCamera = new LifecycleCamera(mLifecycleOwner, mCameraUseCaseAdapter);
        mLifecycleOwner.start();

        mLifecycleCamera.bind(Collections.singleton(mFakeUseCase));
        mLifecycleCamera.unbind(Collections.singletonList(mFakeUseCase));

        assertThat(mFakeCamera.getAttachedUseCases()).isEmpty();
    }

    @Test
    public void unbindAll_willUnbindFromCameraInternal()
            throws CameraUseCaseAdapter.CameraException {
        mLifecycleCamera = new LifecycleCamera(mLifecycleOwner, mCameraUseCaseAdapter);
        mLifecycleOwner.start();

        mLifecycleCamera.bind(Collections.singleton(mFakeUseCase));
        mLifecycleCamera.unbindAll();

        assertThat(mFakeCamera.getAttachedUseCases()).isEmpty();
    }
}
