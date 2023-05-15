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

import static org.junit.Assert.fail;
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
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;

import androidx.annotation.OptIn;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.camera2.internal.Camera2CameraControlImpl;
import androidx.camera.camera2.internal.util.TestUtil;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.internal.CameraUseCaseAdapter;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.CameraXUtil;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

@LargeTest
@RunWith(AndroidJUnit4.class)
@OptIn(markerClass = ExperimentalCamera2Interop.class)
@SdkSuppress(minSdkVersion = 21)
public final class Camera2CameraControlDeviceTest {
    private final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();
    private CameraSelector mCameraSelector;
    private CameraCaptureSession.CaptureCallback mMockCaptureCallback =
            mock(CameraCaptureSession.CaptureCallback.class);
    private Context mContext;
    private CameraUseCaseAdapter mCamera;
    private Camera2CameraControl mCamera2CameraControl;
    private Camera2CameraControlImpl mCamera2CameraControlImpl;

    @Rule
    public TestRule mUseCamera = CameraUtil.grantCameraPermissionAndPreTest(
            new CameraUtil.PreTestCameraIdList(Camera2Config.defaultConfig())
    );

    @Before
    public void setUp() {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK));
        mContext = ApplicationProvider.getApplicationContext();
        CameraXUtil.initialize(mContext, Camera2Config.defaultConfig());
        mCameraSelector = new CameraSelector.Builder().requireLensFacing(
                CameraSelector.LENS_FACING_BACK).build();
        mCamera = CameraUtil.createCameraUseCaseAdapter(mContext, mCameraSelector);
        mCamera2CameraControlImpl =
                TestUtil.getCamera2CameraControlImpl(mCamera.getCameraControl());
        mCamera2CameraControl = mCamera2CameraControlImpl.getCamera2CameraControl();
        mMockCaptureCallback = mock(CameraCaptureSession.CaptureCallback.class);
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException, TimeoutException {
        CameraXUtil.shutdown().get(10000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void canGetInteropApi() {
        assertThat(Camera2CameraControl.from(mCamera2CameraControlImpl))
                .isSameInstanceAs(mCamera2CameraControl);
    }

    @Test
    public void canSetAndRetrieveCaptureRequestOptions() {
        bindUseCase();
        CaptureRequestOptions.Builder builder = new CaptureRequestOptions.Builder()
                .setCaptureRequestOption(
                        CaptureRequest.CONTROL_CAPTURE_INTENT,
                        CaptureRequest.CONTROL_CAPTURE_INTENT_MANUAL)
                .setCaptureRequestOption(
                        CaptureRequest.COLOR_CORRECTION_MODE,
                        CameraMetadata.COLOR_CORRECTION_MODE_FAST);
        mCamera2CameraControl.setCaptureRequestOptions(builder.build());

        assertThat(mCamera2CameraControl.getCaptureRequestOptions().getCaptureRequestOption(
                CaptureRequest.CONTROL_CAPTURE_INTENT, null)).isEqualTo(
                CaptureRequest.CONTROL_CAPTURE_INTENT_MANUAL);
        assertThat(mCamera2CameraControl.getCaptureRequestOptions().getCaptureRequestOption(
                CaptureRequest.COLOR_CORRECTION_MODE, null)).isEqualTo(
                CameraMetadata.COLOR_CORRECTION_MODE_FAST);
    }

    @Test
    public void canSubmitCaptureRequestOptions_beforeBinding() {
        ListenableFuture<Void> future = updateCamera2Option(
                CaptureRequest.CONTROL_CAPTURE_INTENT,
                CaptureRequest.CONTROL_CAPTURE_INTENT_MANUAL);
        bindUseCase();

        assertFutureCompletes(future);

        verifyCaptureRequestParameter(mMockCaptureCallback,
                CaptureRequest.CONTROL_CAPTURE_INTENT,
                CaptureRequest.CONTROL_CAPTURE_INTENT_MANUAL);
    }

    @Test
    public void canSubmitCaptureRequestOptions_afterBinding() {
        bindUseCase();
        ListenableFuture<Void> future = updateCamera2Option(
                CaptureRequest.CONTROL_CAPTURE_INTENT,
                CaptureRequest.CONTROL_CAPTURE_INTENT_MANUAL);

        assertFutureCompletes(future);

        verifyCaptureRequestParameter(mMockCaptureCallback,
                CaptureRequest.CONTROL_CAPTURE_INTENT,
                CaptureRequest.CONTROL_CAPTURE_INTENT_MANUAL);
    }

    @Test
    public void canClearCaptureRequestOptions() {
        bindUseCase();
        CaptureRequestOptions.Builder builder = new CaptureRequestOptions.Builder()
                .setCaptureRequestOption(
                        CaptureRequest.CONTROL_CAPTURE_INTENT,
                        CaptureRequest.CONTROL_CAPTURE_INTENT_MANUAL)
                .setCaptureRequestOption(CaptureRequest.COLOR_CORRECTION_MODE,
                        CaptureRequest.COLOR_CORRECTION_MODE_FAST);

        ListenableFuture<Void> future =
                mCamera2CameraControl.setCaptureRequestOptions(builder.build());

        assertFutureCompletes(future);

        builder.clearCaptureRequestOption(CaptureRequest.COLOR_CORRECTION_MODE);

        future = mCamera2CameraControl.setCaptureRequestOptions(builder.build());

        assertFutureCompletes(future);

        assertThat(mCamera2CameraControl.getCaptureRequestOptions().getCaptureRequestOption(
                CaptureRequest.CONTROL_CAPTURE_INTENT, null)).isEqualTo(
                CaptureRequest.CONTROL_CAPTURE_INTENT_MANUAL);
        assertThat(mCamera2CameraControl.getCaptureRequestOptions().getCaptureRequestOption(
                CaptureRequest.COLOR_CORRECTION_MODE, null)).isEqualTo(null);
    }

    @Test
    public void canOverrideAfMode() {
        updateCamera2Option(CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_OFF);
        bindUseCase();

        verifyCaptureRequestParameter(mMockCaptureCallback, CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_OFF);
    }

    @Test
    public void canOverrideAeMode() {
        updateCamera2Option(CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_OFF);
        bindUseCase();

        verifyCaptureRequestParameter(mMockCaptureCallback, CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_OFF);
    }

    @Test
    public void canOverrideAwbMode() {
        updateCamera2Option(CaptureRequest.CONTROL_AWB_MODE,
                CaptureRequest.CONTROL_AWB_MODE_OFF);
        bindUseCase();

        verifyCaptureRequestParameter(mMockCaptureCallback, CaptureRequest.CONTROL_AWB_MODE,
                CaptureRequest.CONTROL_AWB_MODE_OFF);
    }

    @Test
    public void canOverrideScalarCropRegion() throws Exception {
        // scalar crop region must be larger than the region defined
        // by SCALER_AVAILABLE_MAX_DIGITAL_ZOOM otherwise it could cause a crash on some devices.
        // Thus we cannot simply specify some random crop region.
        Rect cropRegion = getZoom2XCropRegion();
        updateCamera2Option(CaptureRequest.SCALER_CROP_REGION, cropRegion);
        bindUseCase();

        verifyCaptureRequestParameter(mMockCaptureCallback, CaptureRequest.SCALER_CROP_REGION,
                cropRegion);
    }

    @Test
    public void canOverrideAfRegion() {
        MeteringRectangle[] meteringRectangles = new MeteringRectangle[]{
                new MeteringRectangle(0, 0, 100, 100, MeteringRectangle.METERING_WEIGHT_MAX)
        };
        updateCamera2Option(CaptureRequest.CONTROL_AF_REGIONS, meteringRectangles);
        bindUseCase();

        verifyCaptureRequestParameter(mMockCaptureCallback, CaptureRequest.CONTROL_AF_REGIONS,
                meteringRectangles);
    }

    @Test
    public void canOverrideAeRegion() {
        MeteringRectangle[] meteringRectangles = new MeteringRectangle[]{
                new MeteringRectangle(0, 0, 100, 100, MeteringRectangle.METERING_WEIGHT_MAX)
        };
        updateCamera2Option(CaptureRequest.CONTROL_AE_REGIONS, meteringRectangles);
        bindUseCase();

        verifyCaptureRequestParameter(mMockCaptureCallback, CaptureRequest.CONTROL_AE_REGIONS,
                meteringRectangles);
    }

    @Test
    public void canOverrideAwbRegion() {
        MeteringRectangle[] meteringRectangles = new MeteringRectangle[]{
                new MeteringRectangle(0, 0, 100, 100, MeteringRectangle.METERING_WEIGHT_MAX)
        };
        updateCamera2Option(CaptureRequest.CONTROL_AWB_REGIONS, meteringRectangles);
        bindUseCase();

        verifyCaptureRequestParameter(mMockCaptureCallback, CaptureRequest.CONTROL_AWB_REGIONS,
                meteringRectangles);
    }

    private Rect getZoom2XCropRegion() throws Exception {
        AtomicReference<String> cameraIdRef = new AtomicReference<>();
        String cameraId = TestUtil.getCamera2CameraInfoImpl(mCamera.getCameraInfo()).getCameraId();
        cameraIdRef.set(cameraId);

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

    private void bindUseCase() {
        ImageAnalysis.Builder imageAnalysisBuilder = new ImageAnalysis.Builder();
        new Camera2Interop.Extender<>(imageAnalysisBuilder).setSessionCaptureCallback(
                mMockCaptureCallback);
        ImageAnalysis imageAnalysis = imageAnalysisBuilder.build();
        // set analyzer to make it active.
        imageAnalysis.setAnalyzer(CameraXExecutors.highPriorityExecutor(),
                mock(ImageAnalysis.Analyzer.class));

        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext, mCameraSelector, imageAnalysis);
        mCamera2CameraControl = Camera2CameraControl.from(mCamera.getCameraControl());
    }

    private <T> ListenableFuture<Void> updateCamera2Option(CaptureRequest.Key<T> key, T value) {
        CaptureRequestOptions bundle = new CaptureRequestOptions.Builder()
                .setCaptureRequestOption(key, value)
                .build();
        return mCamera2CameraControl.setCaptureRequestOptions(bundle);
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

    private <T> T assertFutureCompletes(ListenableFuture<T> future) {
        T result = null;
        try {
            result = future.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            fail("future fail:" + e);
        }
        return result;
    }
}
