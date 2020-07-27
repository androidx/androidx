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

package androidx.camera.core;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.app.Instrumentation;
import android.content.Context;
import android.util.Size;

import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.ImageCaptureConfig;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.internal.CameraUseCaseAdapter;
import androidx.camera.testing.fakes.FakeAppConfig;
import androidx.camera.testing.fakes.FakeCamera;
import androidx.camera.testing.fakes.FakeCameraControl;
import androidx.camera.testing.fakes.FakeCameraDeviceSurfaceManager;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Instrument tests for {@link ImageCapture}.
 */
@MediumTest
@RunWith(AndroidJUnit4.class)
public class ImageCaptureTest {
    private CameraUseCaseAdapter mCameraUseCaseAdapter;
    private final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();

    @Before
    public void setup() throws ExecutionException, InterruptedException {
        CameraXConfig cameraXConfig = CameraXConfig.Builder.fromConfig(
                FakeAppConfig.create()).build();

        Context context = ApplicationProvider.getApplicationContext();
        CameraX.initialize(context, cameraXConfig).get();

        FakeCamera fakeCamera = new FakeCamera("fakeCameraId");

        FakeCameraDeviceSurfaceManager fakeCameraDeviceSurfaceManager =
                new FakeCameraDeviceSurfaceManager();
        fakeCameraDeviceSurfaceManager.setSuggestedResolution("fakeCameraId",
                ImageCaptureConfig.class,
                new Size(640, 480));

        mCameraUseCaseAdapter = new CameraUseCaseAdapter(fakeCamera,
                new LinkedHashSet<>(Collections.singleton(fakeCamera)),
                fakeCameraDeviceSurfaceManager);
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException, TimeoutException {
        CameraX.shutdown().get(10000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void onCaptureCancelled_onErrorCAMERA_CLOSED() {
        ImageCapture imageCapture = createImageCapture();

        mInstrumentation.runOnMainSync(() -> {
            try {
                mCameraUseCaseAdapter.addUseCases(Collections.singleton(imageCapture));
            } catch (CameraUseCaseAdapter.CameraException ignore) {
            }
        });

        ImageCapture.OnImageCapturedCallback callback = mock(
                ImageCapture.OnImageCapturedCallback.class);
        FakeCameraControl fakeCameraControl =
                ((FakeCameraControl) mCameraUseCaseAdapter.getCameraControlInternal());

        fakeCameraControl.setOnNewCaptureRequestListener(captureConfigs -> {
            // Notify the cancel after the capture request has been successfully submitted
            fakeCameraControl.notifyAllRequestOnCaptureCancelled();
        });

        mInstrumentation.runOnMainSync(
                () -> imageCapture.takePicture(CameraXExecutors.mainThreadExecutor(), callback));

        final ArgumentCaptor<ImageCaptureException> exceptionCaptor = ArgumentCaptor.forClass(
                ImageCaptureException.class);
        verify(callback, timeout(1000).times(1)).onError(exceptionCaptor.capture());
        assertThat(exceptionCaptor.getValue().getImageCaptureError()).isEqualTo(
                ImageCapture.ERROR_CAMERA_CLOSED);
    }

    @Test
    public void onRequestFailed_OnErrorCAPTURE_FAILED() {
        ImageCapture imageCapture = createImageCapture();

        mInstrumentation.runOnMainSync(() -> {
            try {
                mCameraUseCaseAdapter.addUseCases(Collections.singleton(imageCapture));
            } catch (CameraUseCaseAdapter.CameraException ignore) {
            }
        });

        ImageCapture.OnImageCapturedCallback callback = mock(
                ImageCapture.OnImageCapturedCallback.class);
        FakeCameraControl fakeCameraControl =
                ((FakeCameraControl) mCameraUseCaseAdapter.getCameraControlInternal());
        fakeCameraControl.setOnNewCaptureRequestListener(captureConfigs -> {
            // Notify the failure after the capture request has been successfully submitted
            fakeCameraControl.notifyAllRequestsOnCaptureFailed();
        });

        mInstrumentation.runOnMainSync(
                () -> imageCapture.takePicture(CameraXExecutors.mainThreadExecutor(),
                        callback));


        final ArgumentCaptor<ImageCaptureException> exceptionCaptor = ArgumentCaptor.forClass(
                ImageCaptureException.class);
        verify(callback, timeout(1000).times(1)).onError(exceptionCaptor.capture());
        assertThat(exceptionCaptor.getValue().getImageCaptureError()).isEqualTo(
                ImageCapture.ERROR_CAPTURE_FAILED);
    }

    // TODO(b/149336664): add a test to verify jpeg quality is 100 when CaptureMode is MAX_QUALITY.
    @Test
    public void captureWithMinLatency_jpegQualityIs95() throws InterruptedException {
        // Arrange.
        ImageCapture imageCapture = createImageCapture();
        mInstrumentation.runOnMainSync(() -> {
            try {
                mCameraUseCaseAdapter.addUseCases(Collections.singleton(imageCapture));
            } catch (CameraUseCaseAdapter.CameraException ignore) {
            }
        });
        FakeCameraControl fakeCameraControl =
                ((FakeCameraControl) mCameraUseCaseAdapter.getCameraControlInternal());

        FakeCameraControl.OnNewCaptureRequestListener mockCaptureRequestListener =
                mock(FakeCameraControl.OnNewCaptureRequestListener.class);
        fakeCameraControl.setOnNewCaptureRequestListener(mockCaptureRequestListener);

        // Act.
        mInstrumentation.runOnMainSync(
                () -> imageCapture.takePicture(CameraXExecutors.mainThreadExecutor(),
                        mock(ImageCapture.OnImageCapturedCallback.class)));

        // Assert.
        ArgumentCaptor<List<CaptureConfig>> argumentCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(mockCaptureRequestListener,
                timeout(1000).times(1)).onNewCaptureRequests(argumentCaptor.capture());
        assertThat(hasJpegQuality(argumentCaptor.getValue(), (byte) 95)).isTrue();
    }

    private boolean hasJpegQuality(List<CaptureConfig> captureConfigs, byte jpegQuality) {
        for (CaptureConfig captureConfig : captureConfigs) {
            if (jpegQuality == captureConfig.getImplementationOptions().retrieveOption(
                    CaptureConfig.OPTION_JPEG_QUALITY)) {
                return true;
            }
        }
        return false;
    }

    private ImageCapture createImageCapture() {
        return new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setFlashMode(ImageCapture.FLASH_MODE_OFF)
                .setCaptureOptionUnpacker((config, builder) -> {
                })
                .setSessionOptionUnpacker((config, builder) -> {
                })
                .build();
    }
}
