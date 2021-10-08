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

package androidx.camera.camera2.interop;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.app.Instrumentation;
import android.content.Context;
import android.graphics.Rect;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.util.Range;

import androidx.annotation.OptIn;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.internal.CameraUseCaseAdapter;
import androidx.camera.testing.CameraUtil;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

@LargeTest
@RunWith(AndroidJUnit4.class)
@OptIn(markerClass = ExperimentalCamera2Interop.class)
@SdkSuppress(minSdkVersion = 21)
public final class Camera2InteropDeviceTest {
    private final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();
    private CameraSelector mCameraSelector;
    private CameraCaptureSession.CaptureCallback mMockCaptureCallback =
            mock(CameraCaptureSession.CaptureCallback.class);
    private Context mContext;
    private CameraUseCaseAdapter mCamera;

    @Rule
    public TestRule mUseCamera = CameraUtil.grantCameraPermissionAndPreTest();

    @Before
    public void setUp() {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK));
        mContext = ApplicationProvider.getApplicationContext();
        CameraX.initialize(mContext, Camera2Config.defaultConfig());
        mCameraSelector = new CameraSelector.Builder().requireLensFacing(
                CameraSelector.LENS_FACING_BACK).build();
        mMockCaptureCallback = mock(CameraCaptureSession.CaptureCallback.class);
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException, TimeoutException {
        if (mCamera != null) {
            mInstrumentation.runOnMainSync(() ->
                    //TODO: The removeUseCases() call might be removed after clarifying the
                    // abortCaptures() issue in b/162314023.
                    mCamera.removeUseCases(mCamera.getUseCases())
            );
        }

        CameraX.shutdown().get(10000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void canHookCallbacks() {
        CameraCaptureSession.StateCallback mockSessionStateCallback =
                mock(CameraCaptureSession.StateCallback.class);
        CameraDevice.StateCallback mockDeviceCallback =
                mock(CameraDevice.StateCallback.class);
        ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
        new Camera2Interop.Extender<>(builder)
                .setSessionCaptureCallback(mMockCaptureCallback)
                .setSessionStateCallback(mockSessionStateCallback)
                .setDeviceStateCallback(mockDeviceCallback);
        ImageAnalysis imageAnalysis = builder.build();

        // set analyzer to make it active.
        imageAnalysis.setAnalyzer(CameraXExecutors.highPriorityExecutor(),
                mock(ImageAnalysis.Analyzer.class));

        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext, mCameraSelector, imageAnalysis);

        verify(mMockCaptureCallback, timeout(5000).atLeastOnce()).onCaptureCompleted(
                any(CameraCaptureSession.class),
                any(CaptureRequest.class),
                any(TotalCaptureResult.class));
        verify(mockSessionStateCallback, timeout(5000)).onActive(any(CameraCaptureSession.class));
        verify(mockDeviceCallback, timeout(5000)).onOpened(any(CameraDevice.class));
    }

    @Test
    public void canOverrideAfMode() {
        bindUseCaseWithCamera2Option(
                mMockCaptureCallback,
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_OFF);

        verifyCaptureRequestParameter(mMockCaptureCallback, CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_OFF);
    }

    @Test
    public void canOverrideAeMode() {
        bindUseCaseWithCamera2Option(mMockCaptureCallback,
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_OFF);

        verifyCaptureRequestParameter(mMockCaptureCallback, CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_OFF);
    }

    @Test
    public void canOverrideAwbMode() {
        bindUseCaseWithCamera2Option(mMockCaptureCallback,
                CaptureRequest.CONTROL_AWB_MODE,
                CaptureRequest.CONTROL_AWB_MODE_OFF);

        verifyCaptureRequestParameter(mMockCaptureCallback, CaptureRequest.CONTROL_AWB_MODE,
                CaptureRequest.CONTROL_AWB_MODE_OFF);
    }

    @Test
    public void canOverrideAeFpsRange() {
        bindUseCaseWithCamera2Option(mMockCaptureCallback,
                CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                new Range<>(30, 30));

        verifyCaptureRequestParameter(mMockCaptureCallback,
                CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                new Range<>(30, 30));
    }

    @Test
    public void canOverrideScalarCropRegion() throws Exception {
        // scalar crop region must be larger than the region defined
        // by SCALER_AVAILABLE_MAX_DIGITAL_ZOOM otherwise it could cause a crash on some devices.
        // Thus we cannot simply specify some random crop region.
        Rect cropRegion = getZoom2XCropRegion();
        bindUseCaseWithCamera2Option(mMockCaptureCallback,
                CaptureRequest.SCALER_CROP_REGION,
                cropRegion
        );

        verifyCaptureRequestParameter(mMockCaptureCallback, CaptureRequest.SCALER_CROP_REGION,
                cropRegion);
    }

    @Test
    public void canOverrideAfRegion() {
        MeteringRectangle[] meteringRectangles = new MeteringRectangle[]{
                new MeteringRectangle(0, 0, 100, 100, MeteringRectangle.METERING_WEIGHT_MAX)
        };
        bindUseCaseWithCamera2Option(mMockCaptureCallback,
                CaptureRequest.CONTROL_AF_REGIONS,
                meteringRectangles
        );

        verifyCaptureRequestParameter(mMockCaptureCallback, CaptureRequest.CONTROL_AF_REGIONS,
                meteringRectangles);
    }

    @Test
    public void canOverrideAeRegion() {
        MeteringRectangle[] meteringRectangles = new MeteringRectangle[]{
                new MeteringRectangle(0, 0, 100, 100, MeteringRectangle.METERING_WEIGHT_MAX)
        };
        bindUseCaseWithCamera2Option(mMockCaptureCallback,
                CaptureRequest.CONTROL_AE_REGIONS,
                meteringRectangles
        );

        verifyCaptureRequestParameter(mMockCaptureCallback, CaptureRequest.CONTROL_AE_REGIONS,
                meteringRectangles);
    }

    @Test
    public void canOverrideAwbRegion() {
        MeteringRectangle[] meteringRectangles = new MeteringRectangle[]{
                new MeteringRectangle(0, 0, 100, 100, MeteringRectangle.METERING_WEIGHT_MAX)
        };
        bindUseCaseWithCamera2Option(mMockCaptureCallback,
                CaptureRequest.CONTROL_AWB_REGIONS,
                meteringRectangles
        );

        verifyCaptureRequestParameter(mMockCaptureCallback, CaptureRequest.CONTROL_AWB_REGIONS,
                meteringRectangles);
    }

    private Rect getZoom2XCropRegion() throws Exception {
        AtomicReference<String> cameraIdRef = new AtomicReference<>();
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().build();
        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext, mCameraSelector, imageAnalysis);

        String cameraId = Camera2CameraInfo.from(mCamera.getCameraInfo()).getCameraId();
        cameraIdRef.set(cameraId);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                mCamera.removeUseCases(Collections.singleton(imageAnalysis))
        );

        CameraManager cameraManager =
                (CameraManager) mInstrumentation.getContext().getSystemService(
                        Context.CAMERA_SERVICE);
        CameraCharacteristics characteristics =
                cameraManager.getCameraCharacteristics(cameraIdRef.get());
        assumeTrue(
                characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)
                        >= 2);
        Rect sensorRect = characteristics
                .get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        return new Rect(sensorRect.centerX() - sensorRect.width() / 4,
                sensorRect.centerY() - sensorRect.height() / 4,
                sensorRect.centerX() + sensorRect.width() / 4,
                sensorRect.centerY() + sensorRect.height() / 4);
    }

    private <T> void bindUseCaseWithCamera2Option(CameraCaptureSession.CaptureCallback callback,
            CaptureRequest.Key<T> key,
            T value) {
        ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
        new Camera2Interop.Extender<>(builder)
                .setCaptureRequestOption(key, value)
                .setSessionCaptureCallback(callback);
        ImageAnalysis imageAnalysis = builder.build();

        // set analyzer to make it active.
        imageAnalysis.setAnalyzer(CameraXExecutors.highPriorityExecutor(),
                mock(ImageAnalysis.Analyzer.class));

        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext, mCameraSelector, imageAnalysis);
    }

    private <T> void verifyCaptureRequestParameter(
            CameraCaptureSession.CaptureCallback mockCallback,
            CaptureRequest.Key<T> key,
            T value) {
        ArgumentCaptor<CaptureRequest> captureRequest =
                ArgumentCaptor.forClass(CaptureRequest.class);
        verify(mockCallback, timeout(5000).atLeastOnce()).onCaptureCompleted(
                any(CameraCaptureSession.class),
                captureRequest.capture(), any(TotalCaptureResult.class));
        CaptureRequest request = captureRequest.getValue();
        assertThat(request.get(key)).isEqualTo(value);
    }
}
