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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.util.Rational;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.UseCase;
import androidx.camera.core.ViewPort;
import androidx.camera.core.impl.CameraConfig;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.Identifier;
import androidx.camera.core.impl.MutableOptionsBundle;
import androidx.camera.core.impl.OptionsBundle;
import androidx.camera.core.impl.UseCaseConfigFactory;
import androidx.camera.testing.fakes.FakeCamera;
import androidx.camera.testing.fakes.FakeCameraDeviceSurfaceManager;
import androidx.camera.testing.fakes.FakeUseCase;
import androidx.camera.testing.fakes.FakeUseCaseConfig;
import androidx.camera.testing.fakes.FakeUseCaseConfigFactory;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.LinkedHashSet;

/** JUnit test cases for {@link CameraUseCaseAdapter} class. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class CameraUseCaseAdapterTest {
    FakeCameraDeviceSurfaceManager mFakeCameraDeviceSurfaceManager;
    FakeCamera mFakeCamera;
    UseCaseConfigFactory mUseCaseConfigFactory;
    LinkedHashSet<CameraInternal> mFakeCameraSet = new LinkedHashSet<>();

    @Before
    public void setUp() {
        mFakeCameraDeviceSurfaceManager = new FakeCameraDeviceSurfaceManager();
        mFakeCamera = new FakeCamera();
        mUseCaseConfigFactory = new FakeUseCaseConfigFactory();
        mFakeCameraSet.add(mFakeCamera);
    }

    @Test
    public void attachUseCases() throws CameraUseCaseAdapter.CameraException {
        CameraUseCaseAdapter cameraUseCaseAdapter = new CameraUseCaseAdapter(mFakeCameraSet,
                mFakeCameraDeviceSurfaceManager,
                mUseCaseConfigFactory);
        FakeUseCase fakeUseCase = new FakeUseCase();
        cameraUseCaseAdapter.addUseCases(Collections.singleton(fakeUseCase));

        assertThat(fakeUseCase.getCamera()).isEqualTo(mFakeCamera);
        assertThat(mFakeCamera.getAttachedUseCases()).containsExactly(fakeUseCase);
    }

    @Test
    public void detachUseCases() throws CameraUseCaseAdapter.CameraException {
        CameraUseCaseAdapter cameraUseCaseAdapter = new CameraUseCaseAdapter(mFakeCameraSet,
                mFakeCameraDeviceSurfaceManager,
                mUseCaseConfigFactory);
        FakeUseCase fakeUseCase = new FakeUseCase();
        cameraUseCaseAdapter.addUseCases(Collections.singleton(fakeUseCase));
        cameraUseCaseAdapter.removeUseCases(Collections.singleton(fakeUseCase));

        assertThat(fakeUseCase.getCamera()).isNull();
    }

    @Test
    public void attachUseCases_restoreInteropConfig() {
        // Set an config to CameraControl.
        Config.Option<Integer> option = Config.Option.create("OPTION_ID", Integer.class);
        Integer value = 1;
        MutableOptionsBundle originalConfig = MutableOptionsBundle.create();
        originalConfig.insertOption(option, value);
        mFakeCamera.getCameraControlInternal().addInteropConfig(originalConfig);
        CameraUseCaseAdapter cameraUseCaseAdapter = new CameraUseCaseAdapter(mFakeCameraSet,
                mFakeCameraDeviceSurfaceManager,
                mUseCaseConfigFactory);

        // This caches the original config and clears it from CameraControl internally.
        cameraUseCaseAdapter.detachUseCases();

        // Set a different config.
        mFakeCamera.getCameraControlInternal().addInteropConfig(MutableOptionsBundle.create());

        // This restores the cached config to CameraControl.
        cameraUseCaseAdapter.attachUseCases();

        // Check the config in CameraControl has the same value as the original config.
        assertThat(
                mFakeCamera.getCameraControlInternal().getInteropConfig().containsOption(
                        option)).isTrue();
        assertThat(
                mFakeCamera.getCameraControlInternal().getInteropConfig().retrieveOption(
                        option)).isEqualTo(value);
    }

    @Test
    public void detachUseCases_clearInteropConfig() {
        // Set an config to CameraControl.
        Config config = MutableOptionsBundle.create();
        mFakeCamera.getCameraControlInternal().addInteropConfig(config);
        CameraUseCaseAdapter cameraUseCaseAdapter = new CameraUseCaseAdapter(mFakeCameraSet,
                mFakeCameraDeviceSurfaceManager,
                mUseCaseConfigFactory);

        // This caches the original config and clears it from CameraControl internally.
        cameraUseCaseAdapter.detachUseCases();

        // Check the config in CameraControl is empty.
        assertThat(
                mFakeCamera.getCameraControlInternal().getInteropConfig().listOptions()).isEmpty();
    }

    @Test
    public void closeCameraUseCaseAdapter() throws CameraUseCaseAdapter.CameraException {
        CameraUseCaseAdapter cameraUseCaseAdapter = new CameraUseCaseAdapter(mFakeCameraSet,
                mFakeCameraDeviceSurfaceManager,
                mUseCaseConfigFactory);
        FakeUseCase fakeUseCase = new FakeUseCase();
        cameraUseCaseAdapter.addUseCases(Collections.singleton(fakeUseCase));
        cameraUseCaseAdapter.detachUseCases();

        assertThat(fakeUseCase.getCamera()).isEqualTo(mFakeCamera);
        assertThat(mFakeCamera.getAttachedUseCases()).isEmpty();
    }

    @Test
    public void cameraIdEquals() {
        CameraUseCaseAdapter cameraUseCaseAdapter = new CameraUseCaseAdapter(mFakeCameraSet,
                mFakeCameraDeviceSurfaceManager,
                mUseCaseConfigFactory);

        CameraUseCaseAdapter.CameraId otherCameraId =
                CameraUseCaseAdapter.generateCameraId(mFakeCameraSet);

        assertThat(cameraUseCaseAdapter.getCameraId().equals(otherCameraId)).isTrue();
    }

    @Test
    public void cameraEquivalent() {
        CameraUseCaseAdapter cameraUseCaseAdapter = new CameraUseCaseAdapter(mFakeCameraSet,
                mFakeCameraDeviceSurfaceManager,
                mUseCaseConfigFactory);

        CameraUseCaseAdapter otherCameraUseCaseAdapter = new CameraUseCaseAdapter(mFakeCameraSet,
                mFakeCameraDeviceSurfaceManager,
                mUseCaseConfigFactory);
        assertThat(cameraUseCaseAdapter.isEquivalent(otherCameraUseCaseAdapter)).isTrue();
    }

    @Test
    @MediumTest
    public void useCase_onAttach() throws CameraUseCaseAdapter.CameraException {
        CameraUseCaseAdapter cameraUseCaseAdapter = new CameraUseCaseAdapter(mFakeCameraSet,
                mFakeCameraDeviceSurfaceManager,
                mUseCaseConfigFactory);

        FakeUseCase fakeUseCase = spy(new FakeUseCase());
        cameraUseCaseAdapter.addUseCases(Collections.singleton(fakeUseCase));

        verify(fakeUseCase).onAttach(eq(mFakeCamera), isNull(), any(FakeUseCaseConfig.class));
    }

    @Test
    @MediumTest
    public void useCase_onDetach() throws CameraUseCaseAdapter.CameraException {
        CameraUseCaseAdapter cameraUseCaseAdapter = new CameraUseCaseAdapter(mFakeCameraSet,
                mFakeCameraDeviceSurfaceManager,
                mUseCaseConfigFactory);

        FakeUseCase fakeUseCase = spy(new FakeUseCase());
        cameraUseCaseAdapter.addUseCases(Collections.singleton(fakeUseCase));

        cameraUseCaseAdapter.removeUseCases(Collections.singleton(fakeUseCase));

        verify(fakeUseCase).onDetach(mFakeCamera);
    }

    @Test
    public void eventCallbackOnBind() throws CameraUseCaseAdapter.CameraException {
        CameraUseCaseAdapter cameraUseCaseAdapter = new CameraUseCaseAdapter(mFakeCameraSet,
                mFakeCameraDeviceSurfaceManager,
                mUseCaseConfigFactory);

        UseCase.EventCallback callback = mock(UseCase.EventCallback.class);
        FakeUseCase fakeUseCase =
                new FakeUseCaseConfig.Builder().setUseCaseEventCallback(callback).build();

        cameraUseCaseAdapter.addUseCases(Collections.singleton(fakeUseCase));

        verify(callback).onAttach(mFakeCamera.getCameraInfoInternal());
    }

    @Test
    public void eventCallbackOnUnbind() throws CameraUseCaseAdapter.CameraException {
        CameraUseCaseAdapter cameraUseCaseAdapter = new CameraUseCaseAdapter(mFakeCameraSet,
                mFakeCameraDeviceSurfaceManager,
                mUseCaseConfigFactory);

        UseCase.EventCallback callback = mock(UseCase.EventCallback.class);
        FakeUseCase fakeUseCase =
                new FakeUseCaseConfig.Builder().setUseCaseEventCallback(callback).build();

        cameraUseCaseAdapter.addUseCases(Collections.singleton(fakeUseCase));

        cameraUseCaseAdapter.removeUseCases(Collections.singleton(fakeUseCase));

        verify(callback).onDetach();
    }

    @Test
    @MediumTest
    public void addExistingUseCase_viewPortUpdated()
            throws CameraUseCaseAdapter.CameraException {
        Rational aspectRatio1 = new Rational(1, 1);
        Rational aspectRatio2 = new Rational(2, 1);

        // Arrange: set up adapter with aspect ratio 1.
        CameraUseCaseAdapter cameraUseCaseAdapter = new CameraUseCaseAdapter(mFakeCameraSet,
                mFakeCameraDeviceSurfaceManager,
                mUseCaseConfigFactory);
        cameraUseCaseAdapter.setViewPort(
                new ViewPort.Builder(aspectRatio1, Surface.ROTATION_0).build());
        FakeUseCase fakeUseCase = spy(new FakeUseCase());
        cameraUseCaseAdapter.addUseCases(Collections.singleton(fakeUseCase));
        // Use case gets aspect ratio 1
        assertThat(fakeUseCase.getViewPortCropRect()).isNotNull();
        assertThat(new Rational(fakeUseCase.getViewPortCropRect().width(),
                fakeUseCase.getViewPortCropRect().height())).isEqualTo(aspectRatio1);

        // Act: set aspect ratio 2 and attach the same use case.
        cameraUseCaseAdapter.setViewPort(
                new ViewPort.Builder(aspectRatio2, Surface.ROTATION_0).build());
        cameraUseCaseAdapter.addUseCases(Collections.singleton(fakeUseCase));

        // Assert: the viewport has aspect ratio 2.
        assertThat(fakeUseCase.getViewPortCropRect()).isNotNull();
        assertThat(new Rational(fakeUseCase.getViewPortCropRect().width(),
                fakeUseCase.getViewPortCropRect().height())).isEqualTo(aspectRatio2);
    }

    @Test
    public void canSetExtendedCameraConfig_whenNoUseCase() {
        CameraUseCaseAdapter cameraUseCaseAdapter = new CameraUseCaseAdapter(mFakeCameraSet,
                mFakeCameraDeviceSurfaceManager,
                mUseCaseConfigFactory);

        cameraUseCaseAdapter.setExtendedConfig(new FakeCameraConfig());
    }

    @Test(expected = IllegalStateException.class)
    public void canNotSetExtendedCameraConfig_whenUseCaseHasExisted()
            throws CameraUseCaseAdapter.CameraException {
        CameraUseCaseAdapter cameraUseCaseAdapter = new CameraUseCaseAdapter(mFakeCameraSet,
                mFakeCameraDeviceSurfaceManager,
                mUseCaseConfigFactory);

        // Adds use case first
        cameraUseCaseAdapter.addUseCases(Collections.singleton(new FakeUseCase()));

        // Sets extended config after a use case is added
        cameraUseCaseAdapter.setExtendedConfig(new FakeCameraConfig());
    }

    @Test
    public void canSetSameExtendedCameraConfig_whenUseCaseHasExisted()
            throws CameraUseCaseAdapter.CameraException {
        CameraUseCaseAdapter cameraUseCaseAdapter = new CameraUseCaseAdapter(mFakeCameraSet,
                mFakeCameraDeviceSurfaceManager,
                mUseCaseConfigFactory);

        CameraConfig cameraConfig = new FakeCameraConfig();
        cameraUseCaseAdapter.setExtendedConfig(cameraConfig);

        cameraUseCaseAdapter.addUseCases(Collections.singleton(new FakeUseCase()));

        // Sets extended config with the same camera config
        cameraUseCaseAdapter.setExtendedConfig(cameraConfig);
    }

    @Test
    public void canSwitchExtendedCameraConfig_afterUnbindUseCases()
            throws CameraUseCaseAdapter.CameraException {
        CameraUseCaseAdapter cameraUseCaseAdapter = new CameraUseCaseAdapter(mFakeCameraSet,
                mFakeCameraDeviceSurfaceManager,
                mUseCaseConfigFactory);

        CameraConfig cameraConfig1 = new FakeCameraConfig();
        cameraUseCaseAdapter.setExtendedConfig(cameraConfig1);

        // Binds use case
        FakeUseCase fakeUseCase = new FakeUseCase();
        cameraUseCaseAdapter.addUseCases(Collections.singleton(fakeUseCase));

        // Unbinds use case
        cameraUseCaseAdapter.removeUseCases(Collections.singleton(fakeUseCase));

        // Sets extended config with different camera config
        CameraConfig cameraConfig2 = new FakeCameraConfig();
        cameraUseCaseAdapter.setExtendedConfig(cameraConfig2);
    }

    private static final class FakeCameraConfig implements CameraConfig {
        private final UseCaseConfigFactory mUseCaseConfigFactory = new UseCaseConfigFactory() {
            @Nullable
            @Override
            public Config getConfig(@NonNull CaptureType captureType) {
                return null;
            }
        };

        private final Identifier mIdentifier = Identifier.create(new Object());

        @NonNull
        @Override
        public UseCaseConfigFactory getUseCaseConfigFactory() {
            return mUseCaseConfigFactory;
        }

        @NonNull
        @Override
        public Identifier getCompatibilityId() {
            return mIdentifier;
        }

        @NonNull
        @Override
        public Config getConfig() {
            return OptionsBundle.emptyBundle();
        }
    }
}
