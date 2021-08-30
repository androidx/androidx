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

import static android.util.LayoutDirection.LTR;

import static androidx.camera.core.impl.Config.OptionPriority.ALWAYS_OVERRIDE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.ImageOutputConfig;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.impl.UseCaseConfigFactory;
import androidx.camera.core.internal.CameraUseCaseAdapter;
import androidx.camera.testing.fakes.FakeCamera;
import androidx.camera.testing.fakes.FakeCameraDeviceSurfaceManager;
import androidx.camera.testing.fakes.FakeCameraInfoInternal;
import androidx.camera.testing.fakes.FakeUseCase;
import androidx.camera.testing.fakes.FakeUseCaseConfig;
import androidx.camera.testing.fakes.FakeUseCaseConfigFactory;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.LinkedHashSet;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class UseCaseTest {
    private static final Size SURFACE_RESOLUTION = new Size(640, 480);
    private CameraInternal mMockCameraInternal;

    @Before
    public void setup() {
        mMockCameraInternal = mock(CameraInternal.class);
    }

    @Test
    public void getAttachedSessionConfig() {
        FakeUseCaseConfig config = new FakeUseCaseConfig.Builder().setTargetName(
                "UseCase").getUseCaseConfig();
        TestUseCase testUseCase = new TestUseCase(config);
        SessionConfig sessionToAttach = new SessionConfig.Builder().build();
        testUseCase.updateSessionConfig(sessionToAttach);

        SessionConfig attachedSession = testUseCase.getSessionConfig();

        assertThat(attachedSession).isEqualTo(sessionToAttach);
    }

    @Test
    public void removeListener() {
        FakeUseCaseConfig config = new FakeUseCaseConfig.Builder().setTargetName(
                "UseCase").getUseCaseConfig();
        TestUseCase testUseCase = new TestUseCase(config);

        testUseCase.onAttach(mMockCameraInternal, null, null);
        testUseCase.onDetach(mMockCameraInternal);

        testUseCase.activate();

        verify(mMockCameraInternal, never()).onUseCaseActive(any(UseCase.class));
    }

    @Test
    public void notifyActiveState() {
        FakeUseCaseConfig config = new FakeUseCaseConfig.Builder().setTargetName(
                "UseCase").getUseCaseConfig();
        TestUseCase testUseCase = new TestUseCase(config);
        testUseCase.onAttach(mMockCameraInternal, null, null);

        testUseCase.activate();
        verify(mMockCameraInternal, times(1)).onUseCaseActive(testUseCase);
    }

    @Test
    public void notifyInactiveState() {
        FakeUseCaseConfig config = new FakeUseCaseConfig.Builder().setTargetName(
                "UseCase").getUseCaseConfig();
        TestUseCase testUseCase = new TestUseCase(config);
        testUseCase.onAttach(mMockCameraInternal, null, null);

        testUseCase.deactivate();
        verify(mMockCameraInternal, times(1)).onUseCaseInactive(testUseCase);
    }

    @Test
    public void notifyUpdatedSettings() {
        FakeUseCaseConfig config = new FakeUseCaseConfig.Builder().setTargetName(
                "UseCase").getUseCaseConfig();
        TestUseCase testUseCase = new TestUseCase(config);
        testUseCase.onAttach(mMockCameraInternal, null, null);

        testUseCase.update();
        verify(mMockCameraInternal, times(1)).onUseCaseUpdated(testUseCase);
    }

    @Test
    public void notifyResetUseCase() {
        FakeUseCaseConfig config = new FakeUseCaseConfig.Builder().setTargetName(
                "UseCase").getUseCaseConfig();
        TestUseCase testUseCase = new TestUseCase(config);
        testUseCase.onAttach(mMockCameraInternal, null, null);

        testUseCase.notifyReset();
        verify(mMockCameraInternal, times(1)).onUseCaseReset(testUseCase);
    }

    @Test
    public void useCaseConfig_keepOptionPriority() {
        FakeUseCaseConfig.Builder builder =  new FakeUseCaseConfig.Builder();
        Config.Option<Integer> opt = Config.Option.create("OPT1", Integer.class);
        builder.getMutableConfig().insertOption(opt, ALWAYS_OVERRIDE, 1);

        FakeUseCase fakeUseCase = builder.build();
        UseCaseConfig<?> useCaseConfig = fakeUseCase.getCurrentConfig();

        assertThat(useCaseConfig.getOptionPriority(opt)).isEqualTo(ALWAYS_OVERRIDE);
    }

    @Test
    public void attachedSurfaceResolutionCanBeReset_whenOnDetach() {
        FakeUseCaseConfig config = new FakeUseCaseConfig.Builder().setTargetName(
                "UseCase").getUseCaseConfig();
        TestUseCase testUseCase = new TestUseCase(config);

        testUseCase.updateSuggestedResolution(new Size(640, 480));
        assertThat(testUseCase.getAttachedSurfaceResolution()).isNotNull();

        testUseCase.onAttach(mMockCameraInternal, null, null);
        testUseCase.onDetach(mMockCameraInternal);

        assertThat(testUseCase.getAttachedSurfaceResolution()).isNull();
    }

    @Test
    public void viewPortCropRectCanBeReset_whenOnDetach() {
        FakeUseCaseConfig config = new FakeUseCaseConfig.Builder().setTargetName(
                "UseCase").getUseCaseConfig();
        TestUseCase testUseCase = new TestUseCase(config);

        testUseCase.setViewPortCropRect(new Rect(0, 0, 640, 480));
        assertThat(testUseCase.getViewPortCropRect()).isNotNull();

        testUseCase.onAttach(mMockCameraInternal, null, null);
        testUseCase.onDetach(mMockCameraInternal);

        assertThat(testUseCase.getViewPortCropRect()).isNull();
    }

    @Test
    public void mergeConfigs() {
        int cameraDefaultPriority = 4;
        FakeUseCaseConfig defaultConfig = new FakeUseCaseConfig.Builder()
                .setTargetRotation(Surface.ROTATION_0)
                .setBufferFormat(ImageFormat.RAW10)
                .setSurfaceOccupancyPriority(cameraDefaultPriority).getUseCaseConfig();

        int useCaseImageFormat = ImageFormat.YUV_420_888;
        FakeUseCaseConfig useCaseConfig = new FakeUseCaseConfig.Builder()
                .setTargetRotation(Surface.ROTATION_90)
                .setBufferFormat(useCaseImageFormat).getUseCaseConfig();

        String extendedTargetName = "UseCase-extended";
        FakeUseCaseConfig extendedConfig = new FakeUseCaseConfig.Builder()
                .setTargetRotation(Surface.ROTATION_180).getUseCaseConfig();

        TestUseCase testUseCase = new TestUseCase(useCaseConfig);

        FakeCameraInfoInternal cameraInfo = new FakeCameraInfoInternal();

        UseCaseConfig<?> mergedConfig = testUseCase.mergeConfigs(cameraInfo, extendedConfig,
                defaultConfig);

        assertThat(mergedConfig.getSurfaceOccupancyPriority()).isEqualTo(cameraDefaultPriority);
        assertThat(mergedConfig.getInputFormat()).isEqualTo(useCaseImageFormat);
        ImageOutputConfig imageOutputConfig = (ImageOutputConfig) mergedConfig;
        assertThat(imageOutputConfig.getTargetRotation()).isEqualTo(Surface.ROTATION_180);
    }

    @Test
    public void returnNullResolutionInfo_beforeAddingToCameraUseCaseAdapter() {
        FakeUseCase fakeUseCase = new FakeUseCase();
        assertThat(fakeUseCase.getResolutionInfo()).isNull();
    }

    @Test
    public void returnResolutionInfo_afterAddingToCameraUseCaseAdapter()
            throws CameraUseCaseAdapter.CameraException {
        FakeUseCase fakeUseCase = new FakeUseCase();

        CameraUseCaseAdapter cameraUseCaseAdapter = createCameraUseCaseAdapter();
        cameraUseCaseAdapter.addUseCases(Collections.singletonList(fakeUseCase));

        ResolutionInfo resolutionInfo = fakeUseCase.getResolutionInfo();

        assertThat(resolutionInfo).isNotNull();
        assertThat(resolutionInfo.getResolution()).isEqualTo(SURFACE_RESOLUTION);
        assertThat(resolutionInfo.getCropRect()).isEqualTo(new Rect(0, 0,
                SURFACE_RESOLUTION.getWidth(), SURFACE_RESOLUTION.getHeight()));
        assertThat(resolutionInfo.getRotationDegrees()).isEqualTo(0);
    }

    @Test
    public void returnNullResolutionInfo_afterRemovedFromCameraUseCaseAdapter()
            throws CameraUseCaseAdapter.CameraException {
        FakeUseCase fakeUseCase = new FakeUseCase();

        CameraUseCaseAdapter cameraUseCaseAdapter = createCameraUseCaseAdapter();
        cameraUseCaseAdapter.addUseCases(Collections.singletonList(fakeUseCase));
        cameraUseCaseAdapter.removeUseCases(Collections.singletonList(fakeUseCase));

        ResolutionInfo resolutionInfo = fakeUseCase.getResolutionInfo();

        assertThat(resolutionInfo).isNull();
    }

    @Test
    public void correctRotationDegreesInResolutionInfo()
            throws CameraUseCaseAdapter.CameraException {
        FakeUseCase fakeUseCase = new FakeUseCase();
        fakeUseCase.setTargetRotationInternal(Surface.ROTATION_90);

        CameraUseCaseAdapter cameraUseCaseAdapter = createCameraUseCaseAdapter();
        cameraUseCaseAdapter.addUseCases(Collections.singletonList(fakeUseCase));

        ResolutionInfo resolutionInfo = fakeUseCase.getResolutionInfo();
        assertThat(resolutionInfo.getRotationDegrees()).isEqualTo(270);
    }

    @Test
    public void correctViewPortRectInResolutionInfo()
            throws CameraUseCaseAdapter.CameraException {
        FakeUseCase fakeUseCase = new FakeUseCase();

        CameraUseCaseAdapter cameraUseCaseAdapter = createCameraUseCaseAdapter();
        cameraUseCaseAdapter.setViewPort(new ViewPort(ViewPort.FILL_CENTER,
                new Rational(16, 9), Surface.ROTATION_0, LTR));

        cameraUseCaseAdapter.addUseCases(Collections.singletonList(fakeUseCase));

        ResolutionInfo resolutionInfo = fakeUseCase.getResolutionInfo();
        assertThat(resolutionInfo.getCropRect()).isEqualTo(new Rect(0, 60, 640, 420));
    }

    private CameraUseCaseAdapter createCameraUseCaseAdapter() {
        String cameraId = "fakeCameraId";
        FakeCamera fakeCamera = new FakeCamera(cameraId, null,
                new FakeCameraInfoInternal(cameraId));

        FakeCameraDeviceSurfaceManager fakeCameraDeviceSurfaceManager =
                new FakeCameraDeviceSurfaceManager();
        fakeCameraDeviceSurfaceManager.setSuggestedResolution(cameraId,
                FakeUseCaseConfig.class,
                SURFACE_RESOLUTION);

        UseCaseConfigFactory useCaseConfigFactory = new FakeUseCaseConfigFactory();

        return new CameraUseCaseAdapter(
                new LinkedHashSet<>(Collections.singleton(fakeCamera)),
                fakeCameraDeviceSurfaceManager,
                useCaseConfigFactory);
    }

    static class TestUseCase extends FakeUseCase {
        TestUseCase(FakeUseCaseConfig config) {
            super(config);
        }

        void activate() {
            notifyActive();
        }

        void deactivate() {
            notifyInactive();
        }

        void update() {
            notifyUpdated();
        }

        @Override
        @NonNull
        protected Size onSuggestedResolutionUpdated(@NonNull Size suggestedResolution) {
            return suggestedResolution;
        }
    }
}
