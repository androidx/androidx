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

import static android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_OFF;
import static android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON;
import static android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON_ALWAYS_FLASH;
import static android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH;
import static android.hardware.camera2.CameraMetadata.CONTROL_AF_MODE_AUTO;
import static android.hardware.camera2.CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE;
import static android.hardware.camera2.CameraMetadata.CONTROL_AF_MODE_OFF;
import static android.hardware.camera2.CameraMetadata.CONTROL_AWB_MODE_AUTO;
import static android.hardware.camera2.CameraMetadata.CONTROL_AWB_MODE_OFF;
import static android.hardware.camera2.CameraMetadata.FLASH_MODE_OFF;
import static android.hardware.camera2.CameraMetadata.FLASH_MODE_TORCH;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.app.Instrumentation;
import android.content.Context;
import android.graphics.Rect;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.camera2.impl.Camera2ImplConfig;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.camera2.internal.compat.quirk.CameraQuirks;
import androidx.camera.camera2.internal.compat.workaround.AutoFlashAEModeDisabler;
import androidx.camera.camera2.internal.util.TestUtil;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.SurfaceOrientedMeteringPointFactory;
import androidx.camera.core.impl.CameraCaptureCallback;
import androidx.camera.core.impl.CameraCaptureResult;
import androidx.camera.core.impl.CameraControlInternal;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.Quirks;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.internal.CameraUseCaseAdapter;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.CameraXUtil;
import androidx.camera.testing.HandlerUtil;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.os.HandlerCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@SmallTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 21)
public final class Camera2CameraControlImplDeviceTest {
    @Rule
    public TestRule mUseCamera = CameraUtil.grantCameraPermissionAndPreTest(
            new CameraUtil.PreTestCameraIdList(Camera2Config.defaultConfig())
    );

    private Camera2CameraControlImpl mCamera2CameraControlImpl;
    private CameraControlInternal.ControlUpdateCallback mControlUpdateCallback;
    @SuppressWarnings("unchecked")
    private ArgumentCaptor<List<CaptureConfig>> mCaptureConfigArgumentCaptor =
            ArgumentCaptor.forClass(List.class);
    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private CameraCharacteristics mCameraCharacteristics;
    private CameraCharacteristicsCompat mCameraCharacteristicsCompat;
    private boolean mHasFlashUnit;
    private final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();
    private CameraUseCaseAdapter mCamera;
    private Quirks mCameraQuirks;

    @Before
    public void setUp() throws InterruptedException {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK));

        Context context = ApplicationProvider.getApplicationContext();
        CameraXConfig config = Camera2Config.defaultConfig();
        CameraXUtil.initialize(context, config);

        mCameraCharacteristics = CameraUtil.getCameraCharacteristics(
                CameraSelector.LENS_FACING_BACK);
        Boolean hasFlashUnit =
                mCameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
        mHasFlashUnit = hasFlashUnit != null && hasFlashUnit.booleanValue();

        mControlUpdateCallback = mock(CameraControlInternal.ControlUpdateCallback.class);
        mHandlerThread = new HandlerThread("ControlThread");
        mHandlerThread.start();
        mHandler = HandlerCompat.createAsync(mHandlerThread.getLooper());

        ScheduledExecutorService executorService = CameraXExecutors.newHandlerExecutor(mHandler);
        String cameraId = CameraUtil.getCameraIdWithLensFacing(CameraSelector.LENS_FACING_BACK);
        mCameraCharacteristicsCompat = CameraCharacteristicsCompat.toCameraCharacteristicsCompat(
                mCameraCharacteristics, cameraId);
        mCamera2CameraControlImpl = new Camera2CameraControlImpl(mCameraCharacteristicsCompat,
                executorService, executorService, mControlUpdateCallback);
        mCameraQuirks = CameraQuirks.get(cameraId, mCameraCharacteristicsCompat);

        mCamera2CameraControlImpl.incrementUseCount();
        mCamera2CameraControlImpl.setActive(true);
        HandlerUtil.waitForLooperToIdle(mHandler);
    }

    @After
    public void tearDown() throws InterruptedException, ExecutionException, TimeoutException {
        if (mCamera != null) {
            mInstrumentation.runOnMainSync(() ->
                    //TODO: The removeUseCases() call might be removed after clarifying the
                    // abortCaptures() issue in b/162314023.
                    mCamera.removeUseCases(mCamera.getUseCases())
            );
        }

        CameraXUtil.shutdown().get(10000, TimeUnit.MILLISECONDS);
        if (mHandlerThread != null) {
            mHandlerThread.quitSafely();
        }
    }

    private boolean isAndroidRZoomEnabled() {
        return ZoomControl.isAndroidRZoomSupported(mCameraCharacteristicsCompat);
    }

    private int getMaxAfRegionCount() {
        return mCameraCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF);
    }

    private int getMaxAeRegionCount() {
        return mCameraCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE);
    }

    private int getMaxAwbRegionCount() {
        return mCameraCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AWB);
    }

    private boolean isAeSupported() {
        int[] modes = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES);
        for (int mode : modes) {
            if (mode == CONTROL_AE_MODE_ON) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void canIncrementDecrementUseCount() {
        // incrementUseCount() in setup()
        assertThat(mCamera2CameraControlImpl.getUseCount()).isEqualTo(1);

        mCamera2CameraControlImpl.decrementUseCount();

        assertThat(mCamera2CameraControlImpl.getUseCount()).isEqualTo(0);
    }

    @Test(expected = IllegalStateException.class)
    public void decrementUseCountLessThanZero_getException() {
        // incrementUseCount() in setup()
        assertThat(mCamera2CameraControlImpl.getUseCount()).isEqualTo(1);

        mCamera2CameraControlImpl.decrementUseCount();
        mCamera2CameraControlImpl.decrementUseCount();
    }

    @Test
    public void setTemplate_updateCameraControlSessionConfig() {
        mCamera2CameraControlImpl.setTemplate(CameraDevice.TEMPLATE_RECORD);

        SessionConfig sessionConfig = mCamera2CameraControlImpl.getSessionConfig();
        assertThat(sessionConfig.getTemplateType()).isEqualTo(CameraDevice.TEMPLATE_RECORD);
    }

    @Test
    public void setTemplatePreview_afModeToContinuousPicture() {
        mCamera2CameraControlImpl.setTemplate(CameraDevice.TEMPLATE_PREVIEW);

        Camera2ImplConfig camera2Config =
                new Camera2ImplConfig(mCamera2CameraControlImpl.getSessionOptions());
        assertAfMode(camera2Config, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
    }

    @Test
    public void setTemplateRecord_afModeToContinuousVideo() {
        mCamera2CameraControlImpl.setTemplate(CameraDevice.TEMPLATE_RECORD);

        Camera2ImplConfig camera2Config =
                new Camera2ImplConfig(mCamera2CameraControlImpl.getSessionOptions());
        assertAfMode(camera2Config, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
    }

    @Test
    public void defaultAFAWBMode_ShouldBeCAFWhenNotFocusLocked() {
        Camera2ImplConfig singleConfig = new Camera2ImplConfig(
                mCamera2CameraControlImpl.getSessionOptions());
        assertThat(
                singleConfig.getCaptureRequestOption(
                        CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF))
                .isEqualTo(CaptureRequest.CONTROL_MODE_AUTO);

        assertAfMode(singleConfig, CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        assertAwbMode(singleConfig, CONTROL_AWB_MODE_AUTO);
    }

    @Test
    public void setFlashModeAuto_aeModeSetAndRequestUpdated() throws InterruptedException {
        mCamera2CameraControlImpl.setFlashMode(ImageCapture.FLASH_MODE_AUTO);

        HandlerUtil.waitForLooperToIdle(mHandler);

        verify(mControlUpdateCallback, times(1)).onCameraControlUpdateSessionConfig();
        SessionConfig sessionConfig = mCamera2CameraControlImpl.getSessionConfig();
        Camera2ImplConfig camera2Config = new Camera2ImplConfig(
                sessionConfig.getImplementationOptions());

        assertAeMode(camera2Config, CONTROL_AE_MODE_ON_AUTO_FLASH);
        assertThat(mCamera2CameraControlImpl.getFlashMode()).isEqualTo(
                ImageCapture.FLASH_MODE_AUTO);
        // ZSL only support API >= 23. ZslControlImpl will be created for API >= 23, otherwise
        // ZslControlNoOpImpl will be created, which always return false for this flag.
        if (Build.VERSION.SDK_INT >= 23) {
            assertThat(
                    mCamera2CameraControlImpl.getZslControl().isZslDisabledByFlashMode()).isTrue();
        } else {
            assertThat(
                    mCamera2CameraControlImpl.getZslControl().isZslDisabledByFlashMode()).isFalse();
        }
    }

    @Test
    public void setFlashModeOff_aeModeSetAndRequestUpdated() throws InterruptedException {
        mCamera2CameraControlImpl.setFlashMode(ImageCapture.FLASH_MODE_OFF);

        HandlerUtil.waitForLooperToIdle(mHandler);

        verify(mControlUpdateCallback, times(1)).onCameraControlUpdateSessionConfig();
        SessionConfig sessionConfig = mCamera2CameraControlImpl.getSessionConfig();
        Camera2ImplConfig camera2Config = new Camera2ImplConfig(
                sessionConfig.getImplementationOptions());

        assertAeMode(camera2Config, CONTROL_AE_MODE_ON);

        assertThat(mCamera2CameraControlImpl.getFlashMode()).isEqualTo(ImageCapture.FLASH_MODE_OFF);
        assertThat(mCamera2CameraControlImpl.getZslControl().isZslDisabledByFlashMode()).isFalse();
    }

    @Test
    public void setFlashModeOn_aeModeSetAndRequestUpdated() throws InterruptedException {
        mCamera2CameraControlImpl.setFlashMode(ImageCapture.FLASH_MODE_ON);

        HandlerUtil.waitForLooperToIdle(mHandler);

        verify(mControlUpdateCallback, times(1)).onCameraControlUpdateSessionConfig();
        SessionConfig sessionConfig = mCamera2CameraControlImpl.getSessionConfig();
        Camera2ImplConfig camera2Config = new Camera2ImplConfig(
                sessionConfig.getImplementationOptions());

        assertAeMode(camera2Config, CONTROL_AE_MODE_ON_ALWAYS_FLASH);

        assertThat(mCamera2CameraControlImpl.getFlashMode()).isEqualTo(ImageCapture.FLASH_MODE_ON);
        // ZSL only support API >= 23. ZslControlImpl will be created for API >= 23, otherwise
        // ZslControlNoOpImpl will be created, which always return false for this flag.
        if (Build.VERSION.SDK_INT >= 23) {
            assertThat(
                    mCamera2CameraControlImpl.getZslControl().isZslDisabledByFlashMode()).isTrue();
        } else {
            assertThat(
                    mCamera2CameraControlImpl.getZslControl().isZslDisabledByFlashMode()).isFalse();
        }
    }

    @Test
    public void enableTorch_aeModeSetAndRequestUpdated() throws InterruptedException {
        assumeTrue(mHasFlashUnit);
        mCamera2CameraControlImpl.enableTorch(true);

        HandlerUtil.waitForLooperToIdle(mHandler);

        verify(mControlUpdateCallback, times(1)).onCameraControlUpdateSessionConfig();
        SessionConfig sessionConfig = mCamera2CameraControlImpl.getSessionConfig();
        Camera2ImplConfig camera2Config = new Camera2ImplConfig(
                sessionConfig.getImplementationOptions());

        assertAeMode(camera2Config, CONTROL_AE_MODE_ON);

        assertThat(
                camera2Config.getCaptureRequestOption(
                        CaptureRequest.FLASH_MODE, FLASH_MODE_OFF))
                .isEqualTo(FLASH_MODE_TORCH);
    }

    @Test
    public void disableTorchFlashModeAuto_aeModeSetAndRequestUpdated() throws InterruptedException {
        assumeTrue(mHasFlashUnit);
        mCamera2CameraControlImpl.setFlashMode(ImageCapture.FLASH_MODE_AUTO);
        mCamera2CameraControlImpl.enableTorch(false);

        HandlerUtil.waitForLooperToIdle(mHandler);

        verify(mControlUpdateCallback, times(2)).onCameraControlUpdateSessionConfig();
        SessionConfig sessionConfig = mCamera2CameraControlImpl.getSessionConfig();
        Camera2ImplConfig camera2Config = new Camera2ImplConfig(
                sessionConfig.getImplementationOptions());

        assertAeMode(camera2Config, CONTROL_AE_MODE_ON_AUTO_FLASH);

        assertThat(camera2Config.getCaptureRequestOption(
                CaptureRequest.FLASH_MODE, -1))
                .isEqualTo(-1);

        verify(mControlUpdateCallback, times(1)).onCameraControlCaptureRequests(
                mCaptureConfigArgumentCaptor.capture());
        CaptureConfig captureConfig = mCaptureConfigArgumentCaptor.getValue().get(0);
        Camera2ImplConfig resultCaptureConfig =
                new Camera2ImplConfig(captureConfig.getImplementationOptions());

        assertAeMode(resultCaptureConfig, CONTROL_AE_MODE_ON);

    }

    @Test
    @LargeTest
    public void triggerAf_futureSucceeds() throws Exception {
        Camera2CameraControlImpl camera2CameraControlImpl =
                createCamera2CameraControlWithPhysicalCamera();

        ListenableFuture<CameraCaptureResult> future = CallbackToFutureAdapter.getFuture(c -> {
            camera2CameraControlImpl.mExecutor.execute(() ->
                    camera2CameraControlImpl.getFocusMeteringControl().triggerAf(
                            c, /* overrideAeMode */ false));
            return "triggerAf";
        });

        future.get(5, TimeUnit.SECONDS);
    }

    @Test
    public void captureMaxQuality_shouldSuccess()
            throws ExecutionException, InterruptedException, TimeoutException {
        captureTest(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY,
                ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH);
    }

    @Test
    public void captureMiniLatency_shouldSuccess()
            throws ExecutionException, InterruptedException, TimeoutException {
        captureTest(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
                ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH);
    }

    @Test
    public void captureMaxQuality_torchAsFlash_shouldSuccess()
            throws ExecutionException, InterruptedException, TimeoutException {
        captureTest(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY,
                ImageCapture.FLASH_TYPE_USE_TORCH_AS_FLASH);
    }

    @Test
    public void captureMiniLatency_torchAsFlash_shouldSuccess()
            throws ExecutionException, InterruptedException, TimeoutException {
        captureTest(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
                ImageCapture.FLASH_TYPE_USE_TORCH_AS_FLASH);
    }

    private void captureTest(int captureMode, int flashType)
            throws ExecutionException, InterruptedException, TimeoutException {
        ImageCapture imageCapture = new ImageCapture.Builder().build();

        mCamera = CameraUtil.createCameraAndAttachUseCase(
                ApplicationProvider.getApplicationContext(), CameraSelector.DEFAULT_BACK_CAMERA,
                imageCapture);

        Camera2CameraControlImpl camera2CameraControlImpl =
                TestUtil.getCamera2CameraControlImpl(mCamera.getCameraControl());

        CameraCaptureCallback captureCallback = mock(CameraCaptureCallback.class);
        CaptureConfig.Builder captureConfigBuilder = new CaptureConfig.Builder();
        captureConfigBuilder.setTemplateType(CameraDevice.TEMPLATE_STILL_CAPTURE);
        captureConfigBuilder.addSurface(imageCapture.getSessionConfig().getSurfaces().get(0));
        captureConfigBuilder.addCameraCaptureCallback(captureCallback);

        ListenableFuture<List<Void>> future = camera2CameraControlImpl.submitStillCaptureRequests(
                Arrays.asList(captureConfigBuilder.build()), captureMode, flashType);

        // The future should successfully complete
        future.get(10, TimeUnit.SECONDS);
        // CameraCaptureCallback.onCaptureCompleted() should be called to signal a capture attempt.
        verify(captureCallback, timeout(3000).times(1))
                .onCaptureCompleted(any(CameraCaptureResult.class));
    }

    private Camera2CameraControlImpl createCamera2CameraControlWithPhysicalCamera() {
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().build();
        // Make ImageAnalysis active.
        imageAnalysis.setAnalyzer(CameraXExecutors.mainThreadExecutor(), (image) -> image.close());

        mCamera = CameraUtil.createCameraAndAttachUseCase(
                ApplicationProvider.getApplicationContext(), CameraSelector.DEFAULT_BACK_CAMERA,
                imageAnalysis);

        return TestUtil.getCamera2CameraControlImpl(mCamera.getCameraControl());
    }

    private <T> void assertArraySize(T[] array, int expectedSize) {
        if (expectedSize == 0) {
            assertTrue(array == null || array.length == 0);
        } else {
            assertThat(array).hasLength(expectedSize);
        }
    }

    @Test
    public void startFocusAndMetering_3ARegionsUpdatedInSessionAndSessionOptions()
            throws InterruptedException {
        assumeTrue(getMaxAfRegionCount() > 0 || getMaxAeRegionCount() > 0
                || getMaxAwbRegionCount() > 0);

        SurfaceOrientedMeteringPointFactory factory = new SurfaceOrientedMeteringPointFactory(1.0f,
                1.0f);
        FocusMeteringAction action = new FocusMeteringAction.Builder(factory.createPoint(0, 0))
                .build();
        mCamera2CameraControlImpl.startFocusAndMetering(action);

        HandlerUtil.waitForLooperToIdle(mHandler);

        verify(mControlUpdateCallback, times(1)).onCameraControlUpdateSessionConfig();
        SessionConfig sessionConfig = mCamera2CameraControlImpl.getSessionConfig();
        Camera2ImplConfig repeatingConfig = new Camera2ImplConfig(
                sessionConfig.getImplementationOptions());

        // Here we verify only 3A region count is correct.  Values correctness are left to
        // FocusMeteringControlTest.
        int expectedAfCount = Math.min(getMaxAfRegionCount(), 1);
        int expectedAeCount = Math.min(getMaxAeRegionCount(), 1);
        int expectedAwbCount = Math.min(getMaxAwbRegionCount(), 1);
        assertArraySize(repeatingConfig.getCaptureRequestOption(
                CaptureRequest.CONTROL_AF_REGIONS, null), expectedAfCount);
        assertArraySize(repeatingConfig.getCaptureRequestOption(
                CaptureRequest.CONTROL_AE_REGIONS, null), expectedAeCount);
        assertArraySize(repeatingConfig.getCaptureRequestOption(
                CaptureRequest.CONTROL_AWB_REGIONS, null), expectedAwbCount);

        Camera2ImplConfig singleConfig = new Camera2ImplConfig(
                mCamera2CameraControlImpl.getSessionOptions());
        assertArraySize(singleConfig.getCaptureRequestOption(
                CaptureRequest.CONTROL_AF_REGIONS, null), expectedAfCount);
        assertArraySize(singleConfig.getCaptureRequestOption(
                CaptureRequest.CONTROL_AE_REGIONS, null), expectedAeCount);
        assertArraySize(singleConfig.getCaptureRequestOption(
                CaptureRequest.CONTROL_AWB_REGIONS, null), expectedAwbCount);
    }

    @Test
    public void startFocusAndMetering_AfIsTriggeredProperly() throws InterruptedException {
        assumeTrue(getMaxAfRegionCount() > 0);

        SurfaceOrientedMeteringPointFactory factory = new SurfaceOrientedMeteringPointFactory(1.0f,
                1.0f);
        FocusMeteringAction action = new FocusMeteringAction.Builder(factory.createPoint(0, 0))
                .build();
        mCamera2CameraControlImpl.startFocusAndMetering(action);
        HandlerUtil.waitForLooperToIdle(mHandler);

        verifyAfMode(CaptureRequest.CONTROL_AF_MODE_AUTO);

        verify(mControlUpdateCallback).onCameraControlCaptureRequests(
                mCaptureConfigArgumentCaptor.capture());

        CaptureConfig captureConfig = mCaptureConfigArgumentCaptor.getValue().get(0);
        Camera2ImplConfig resultCaptureConfig =
                new Camera2ImplConfig(captureConfig.getImplementationOptions());

        // Trigger AF
        assertThat(resultCaptureConfig.getCaptureRequestOption(
                CaptureRequest.CONTROL_AF_TRIGGER, null))
                .isEqualTo(CaptureRequest.CONTROL_AF_TRIGGER_START);

        // Ensures AE_MODE is overridden to CONTROL_AE_MODE_ON to prevent from flash being fired.
        if (isAeSupported()) {
            assertThat(resultCaptureConfig.getCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_MODE, null))
                    .isEqualTo(CONTROL_AE_MODE_ON);
        }
    }

    @Test
    public void startFocusAndMetering_AFNotInvolved_AfIsNotTriggered() throws InterruptedException {
        assumeTrue(getMaxAeRegionCount() > 0 || getMaxAwbRegionCount() > 0);

        SurfaceOrientedMeteringPointFactory factory = new SurfaceOrientedMeteringPointFactory(1.0f,
                1.0f);
        FocusMeteringAction action = new FocusMeteringAction.Builder(factory.createPoint(0, 0),
                FocusMeteringAction.FLAG_AE | FocusMeteringAction.FLAG_AWB)
                .build();
        mCamera2CameraControlImpl.startFocusAndMetering(action);
        HandlerUtil.waitForLooperToIdle(mHandler);

        verifyAfMode(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

        verify(mControlUpdateCallback, never()).onCameraControlCaptureRequests(any());
    }

    @Test
    public void cancelFocusAndMetering_3ARegionsReset() throws InterruptedException {
        assumeTrue(getMaxAfRegionCount() > 0 || getMaxAeRegionCount() > 0
                || getMaxAwbRegionCount() > 0);

        SurfaceOrientedMeteringPointFactory factory = new SurfaceOrientedMeteringPointFactory(1.0f,
                1.0f);
        FocusMeteringAction action = new FocusMeteringAction.Builder(factory.createPoint(0, 0))
                .build();
        mCamera2CameraControlImpl.startFocusAndMetering(action);
        HandlerUtil.waitForLooperToIdle(mHandler);
        Mockito.reset(mControlUpdateCallback);

        mCamera2CameraControlImpl.cancelFocusAndMetering();
        HandlerUtil.waitForLooperToIdle(mHandler);

        verify(mControlUpdateCallback, times(1)).onCameraControlUpdateSessionConfig();
        SessionConfig sessionConfig = mCamera2CameraControlImpl.getSessionConfig();
        Camera2ImplConfig repeatingConfig = new Camera2ImplConfig(
                sessionConfig.getImplementationOptions());

        assertThat(
                repeatingConfig.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AF_REGIONS, null)).isNull();
        assertThat(
                repeatingConfig.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_REGIONS, null)).isNull();
        assertThat(
                repeatingConfig.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AWB_REGIONS, null)).isNull();


        Camera2ImplConfig singleConfig = new Camera2ImplConfig(
                mCamera2CameraControlImpl.getSessionOptions());
        assertThat(
                singleConfig.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AF_REGIONS, null)).isNull();
        assertThat(
                singleConfig.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_REGIONS, null)).isNull();
        assertThat(
                singleConfig.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AWB_REGIONS, null)).isNull();
    }

    @Test
    public void cancelFocusAndMetering_cancelAfProperly() throws InterruptedException {
        assumeTrue(getMaxAfRegionCount() > 0);
        SurfaceOrientedMeteringPointFactory factory = new SurfaceOrientedMeteringPointFactory(1.0f,
                1.0f);
        FocusMeteringAction action = new FocusMeteringAction.Builder(factory.createPoint(0, 0))
                .build();
        mCamera2CameraControlImpl.startFocusAndMetering(action);
        HandlerUtil.waitForLooperToIdle(mHandler);
        Mockito.reset(mControlUpdateCallback);
        mCamera2CameraControlImpl.cancelFocusAndMetering();
        HandlerUtil.waitForLooperToIdle(mHandler);

        verifyAfMode(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

        verify(mControlUpdateCallback).onCameraControlCaptureRequests(
                mCaptureConfigArgumentCaptor.capture());

        CaptureConfig captureConfig = mCaptureConfigArgumentCaptor.getValue().get(0);
        Camera2ImplConfig resultCaptureConfig =
                new Camera2ImplConfig(captureConfig.getImplementationOptions());

        // Trigger AF
        assertThat(resultCaptureConfig.getCaptureRequestOption(
                CaptureRequest.CONTROL_AF_TRIGGER, null))
                .isEqualTo(CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
    }

    private void verifyAfMode(int expectAfMode) {
        verify(mControlUpdateCallback, times(1)).onCameraControlUpdateSessionConfig();
        SessionConfig sessionConfig = mCamera2CameraControlImpl.getSessionConfig();
        Camera2ImplConfig repeatingConfig = new Camera2ImplConfig(
                sessionConfig.getImplementationOptions());
        assertAfMode(repeatingConfig, expectAfMode);
    }

    @Test
    public void cancelFocusAndMetering_AFNotInvolved_notCancelAF() throws InterruptedException {
        SurfaceOrientedMeteringPointFactory factory = new SurfaceOrientedMeteringPointFactory(1.0f,
                1.0f);
        FocusMeteringAction action = new FocusMeteringAction.Builder(factory.createPoint(0, 0),
                FocusMeteringAction.FLAG_AE)
                .build();
        mCamera2CameraControlImpl.startFocusAndMetering(action);
        HandlerUtil.waitForLooperToIdle(mHandler);
        Mockito.reset(mControlUpdateCallback);
        mCamera2CameraControlImpl.cancelFocusAndMetering();
        HandlerUtil.waitForLooperToIdle(mHandler);

        verify(mControlUpdateCallback, never()).onCameraControlCaptureRequests(any());

        verifyAfMode(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
    }

    @Test
    public void startFocus_afModeIsSetToAuto() throws InterruptedException {
        assumeTrue(getMaxAfRegionCount() > 0);
        SurfaceOrientedMeteringPointFactory factory = new SurfaceOrientedMeteringPointFactory(1.0f,
                1.0f);
        FocusMeteringAction action = new FocusMeteringAction.Builder(factory.createPoint(0, 0))
                .build();
        mCamera2CameraControlImpl.startFocusAndMetering(action);
        HandlerUtil.waitForLooperToIdle(mHandler);

        Camera2ImplConfig singleConfig = new Camera2ImplConfig(
                mCamera2CameraControlImpl.getSessionOptions());
        assertAfMode(singleConfig, CaptureRequest.CONTROL_AF_MODE_AUTO);

        mCamera2CameraControlImpl.cancelFocusAndMetering();
        HandlerUtil.waitForLooperToIdle(mHandler);

        Camera2ImplConfig singleConfig2 = new Camera2ImplConfig(
                mCamera2CameraControlImpl.getSessionOptions());
        assertAfMode(singleConfig2, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
    }

    private boolean isAfModeSupported(int afMode) {
        int[] modes = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
        return isModeInList(afMode, modes);
    }

    private boolean isAeModeSupported(int aeMode) {
        int[] modes = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES);
        return isModeInList(aeMode, modes);
    }

    private boolean isAwbModeSupported(int awbMode) {
        int[] modes = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES);
        return isModeInList(awbMode, modes);
    }


    private boolean isModeInList(int mode, int[] modeList) {
        if (modeList == null) {
            return false;
        }
        for (int m : modeList) {
            if (mode == m) {
                return true;
            }
        }
        return false;
    }

    private void assertAfMode(Camera2ImplConfig config, int afMode) {
        if (isAfModeSupported(afMode)) {
            assertThat(config.getCaptureRequestOption(
                    CaptureRequest.CONTROL_AF_MODE, null)).isEqualTo(afMode);
        } else {
            int fallbackMode;
            if (isAfModeSupported(CONTROL_AF_MODE_CONTINUOUS_PICTURE)) {
                fallbackMode = CONTROL_AF_MODE_CONTINUOUS_PICTURE;
            } else if (isAfModeSupported(CONTROL_AF_MODE_AUTO)) {
                fallbackMode = CONTROL_AF_MODE_AUTO;
            } else {
                fallbackMode = CONTROL_AF_MODE_OFF;
            }

            assertThat(config.getCaptureRequestOption(
                    CaptureRequest.CONTROL_AF_MODE, null)).isEqualTo(fallbackMode);
        }
    }

    private void assertAeMode(Camera2ImplConfig config, int aeMode) {
        AutoFlashAEModeDisabler aeModeCorrector = new AutoFlashAEModeDisabler(mCameraQuirks);
        int aeModeCorrected = aeModeCorrector.getCorrectedAeMode(aeMode);

        if (isAeModeSupported(aeModeCorrected)) {
            assertThat(config.getCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_MODE, null)).isEqualTo(aeModeCorrected);
        } else {
            int fallbackMode;
            if (isAeModeSupported(CONTROL_AE_MODE_ON)) {
                fallbackMode = CONTROL_AE_MODE_ON;
            } else {
                fallbackMode = CONTROL_AE_MODE_OFF;
            }

            assertThat(config.getCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_MODE, null)).isEqualTo(fallbackMode);
        }
    }

    private void assertAwbMode(Camera2ImplConfig config, int awbMode) {
        if (isAwbModeSupported(awbMode)) {
            assertThat(config.getCaptureRequestOption(
                    CaptureRequest.CONTROL_AWB_MODE, null)).isEqualTo(awbMode);
        } else {
            int fallbackMode;
            if (isAwbModeSupported(CONTROL_AWB_MODE_AUTO)) {
                fallbackMode = CONTROL_AWB_MODE_AUTO;
            } else {
                fallbackMode = CONTROL_AWB_MODE_OFF;
            }

            assertThat(config.getCaptureRequestOption(
                    CaptureRequest.CONTROL_AWB_MODE, null)).isEqualTo(fallbackMode);
        }
    }

    private boolean isZoomSupported() {
        return mCameraCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)
                > 1.0f;
    }

    private Rect getSensorRect() {
        Rect rect = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        // Some device like pixel 2 will have (0, 8) as the left-top corner.
        return new Rect(0, 0, rect.width(), rect.height());
    }

    // Here we just test if setZoomRatio / setLinearZoom is working. For thorough tests, we
    // do it on ZoomControlTest and ZoomControlRoboTest.
    @Test
    public void setZoomRatio_CropRegionIsUpdatedCorrectly() throws InterruptedException {
        assumeTrue(isZoomSupported());
        assumeFalse(isAndroidRZoomEnabled());
        mCamera2CameraControlImpl.setZoomRatio(2.0f);

        HandlerUtil.waitForLooperToIdle(mHandler);

        Rect sessionCropRegion = getSessionCropRegion(mControlUpdateCallback);

        Rect sensorRect = getSensorRect();
        int cropX = (sensorRect.width() / 4);
        int cropY = (sensorRect.height() / 4);
        Rect cropRect = new Rect(cropX, cropY, cropX + sensorRect.width() / 2,
                cropY + sensorRect.height() / 2);
        assertThat(sessionCropRegion).isEqualTo(cropRect);
    }

    @Test
    @SdkSuppress(minSdkVersion = 30)
    public void setZoomRatio_androidRZoomRatioIsUpdatedCorrectly() throws InterruptedException {
        assumeTrue(isAndroidRZoomEnabled());
        mCamera2CameraControlImpl.setZoomRatio(2.0f);

        HandlerUtil.waitForLooperToIdle(mHandler);
        float sessionZoomRatio = getSessionZoomRatio(mControlUpdateCallback);

        assertThat(sessionZoomRatio).isEqualTo(2.0f);
    }

    @NonNull
    private Rect getSessionCropRegion(
            CameraControlInternal.ControlUpdateCallback controlUpdateCallback)
            throws InterruptedException {
        verify(controlUpdateCallback, times(1)).onCameraControlUpdateSessionConfig();
        SessionConfig sessionConfig = mCamera2CameraControlImpl.getSessionConfig();
        Camera2ImplConfig camera2Config = new Camera2ImplConfig(
                sessionConfig.getImplementationOptions());

        reset(controlUpdateCallback);
        return camera2Config.getCaptureRequestOption(
                CaptureRequest.SCALER_CROP_REGION, null);
    }

    @NonNull
    @RequiresApi(30)
    private Float getSessionZoomRatio(
            CameraControlInternal.ControlUpdateCallback controlUpdateCallback)
            throws InterruptedException {
        verify(controlUpdateCallback, times(1)).onCameraControlUpdateSessionConfig();
        SessionConfig sessionConfig = mCamera2CameraControlImpl.getSessionConfig();
        Camera2ImplConfig camera2Config = new Camera2ImplConfig(
                sessionConfig.getImplementationOptions());

        reset(controlUpdateCallback);
        return camera2Config.getCaptureRequestOption(
                CaptureRequest.CONTROL_ZOOM_RATIO, null);
    }

    @Test
    public void setLinearZoom_CropRegionIsUpdatedCorrectly() throws InterruptedException {
        assumeTrue(isZoomSupported());
        assumeFalse(isAndroidRZoomEnabled());
        mCamera2CameraControlImpl.setLinearZoom(1.0f);
        HandlerUtil.waitForLooperToIdle(mHandler);

        Rect cropRegionMaxZoom = getSessionCropRegion(mControlUpdateCallback);
        Rect cropRegionMinZoom = getSensorRect();

        mCamera2CameraControlImpl.setLinearZoom(0.5f);

        HandlerUtil.waitForLooperToIdle(mHandler);

        Rect cropRegionHalfZoom = getSessionCropRegion(mControlUpdateCallback);

        Assert.assertEquals(cropRegionHalfZoom.width(),
                (cropRegionMinZoom.width() + cropRegionMaxZoom.width()) / 2.0f, 1
                /* 1 pixel tolerance */);
    }

    @Test
    @SdkSuppress(minSdkVersion = 30)
    public void setLinearZoom_androidRZoomRatioUpdatedCorrectly() throws InterruptedException {
        assumeTrue(isAndroidRZoomEnabled());
        final float cropWidth = 10000f;

        mCamera2CameraControlImpl.setLinearZoom(1.0f);
        HandlerUtil.waitForLooperToIdle(mHandler);
        float sessionZoomRatioForLinearMax = getSessionZoomRatio(mControlUpdateCallback);
        float cropWidthForLinearMax = cropWidth / sessionZoomRatioForLinearMax;

        mCamera2CameraControlImpl.setLinearZoom(0f);
        HandlerUtil.waitForLooperToIdle(mHandler);
        float sessionZoomRatioForLinearMin = getSessionZoomRatio(mControlUpdateCallback);
        float cropWidthForLinearMin = cropWidth / sessionZoomRatioForLinearMin;

        mCamera2CameraControlImpl.setLinearZoom(0.5f);

        HandlerUtil.waitForLooperToIdle(mHandler);

        float sessionZoomRatioForLinearHalf = getSessionZoomRatio(mControlUpdateCallback);
        float cropWidthForLinearHalf = cropWidth / sessionZoomRatioForLinearHalf;

        Assert.assertEquals(cropWidthForLinearHalf,
                (cropWidthForLinearMin + cropWidthForLinearMax) / 2.0f, 1
                /* 1 pixel tolerance */);
    }

    @Test
    public void setZoomRatio_cameraControlInactive_operationCanceled() {
        mCamera2CameraControlImpl.setActive(false);
        ListenableFuture<Void> listenableFuture = mCamera2CameraControlImpl.setZoomRatio(1.0f);
        try {
            listenableFuture.get(1000, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof CameraControl.OperationCanceledException) {
                assertTrue(true);
                return;
            }
        } catch (Exception e) {
        }

        fail();
    }

    @Test
    public void setLinearZoom_cameraControlInactive_operationCanceled() {
        mCamera2CameraControlImpl.setActive(false);
        ListenableFuture<Void> listenableFuture = mCamera2CameraControlImpl.setLinearZoom(0.0f);
        try {
            listenableFuture.get(1000, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof CameraControl.OperationCanceledException) {
                assertTrue(true);
                return;
            }
        } catch (Exception e) {
        }

        fail();
    }


    @Test
    @LargeTest
    public void addSessionCameraCaptureCallback_canAddWithoutUpdateSessionConfig()
            throws Exception {
        Camera2CameraControlImpl camera2CameraControlImpl =
                createCamera2CameraControlWithPhysicalCamera();
        camera2CameraControlImpl.updateSessionConfig();
        HandlerUtil.waitForLooperToIdle(mHandler);

        TestCameraCaptureCallback callback1 = new TestCameraCaptureCallback();
        TestCameraCaptureCallback callback2 = new TestCameraCaptureCallback();
        camera2CameraControlImpl.addSessionCameraCaptureCallback(CameraXExecutors.directExecutor(),
                callback1);
        camera2CameraControlImpl.addSessionCameraCaptureCallback(CameraXExecutors.directExecutor(),
                callback2);

        callback1.assertCallbackIsCalled(5000);
        callback2.assertCallbackIsCalled(5000);
    }

    @Test
    @LargeTest
    public void removeSessionCameraCaptureCallback() throws Exception {
        Camera2CameraControlImpl camera2CameraControlImpl =
                createCamera2CameraControlWithPhysicalCamera();

        camera2CameraControlImpl.updateSessionConfig();
        HandlerUtil.waitForLooperToIdle(mHandler);

        TestCameraCaptureCallback callback1 = new TestCameraCaptureCallback();

        camera2CameraControlImpl.addSessionCameraCaptureCallback(CameraXExecutors.directExecutor(),
                callback1);
        callback1.assertCallbackIsCalled(5000);

        camera2CameraControlImpl.removeSessionCameraCaptureCallback(callback1);
        HandlerUtil.waitForLooperToIdle(mHandler);

        callback1.assertCallbackIsNotCalled(200);
    }

    @Test
    @LargeTest
    public void sessionCameraCaptureCallback_invokedOnSpecifiedExecutor()
            throws Exception {
        Camera2CameraControlImpl camera2CameraControlImpl =
                createCamera2CameraControlWithPhysicalCamera();
        camera2CameraControlImpl.updateSessionConfig();
        HandlerUtil.waitForLooperToIdle(mHandler);

        TestCameraCaptureCallback callback = new TestCameraCaptureCallback();
        TestExecutor executor = new TestExecutor();

        camera2CameraControlImpl.addSessionCameraCaptureCallback(executor, callback);

        callback.assertCallbackIsCalled(5000);
        executor.assertExecutorIsCalled(5000);
    }

    private static class TestCameraCaptureCallback extends CameraCaptureCallback {
        private CountDownLatch mLatchForOnCaptureCompleted;

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureResult cameraCaptureResult) {
            synchronized (this) {
                if (mLatchForOnCaptureCompleted != null) {
                    mLatchForOnCaptureCompleted.countDown();
                }
            }
        }

        public void assertCallbackIsCalled(long timeoutInMs) throws InterruptedException {
            CountDownLatch latch;
            synchronized (this) {
                mLatchForOnCaptureCompleted = new CountDownLatch(1);
                latch = mLatchForOnCaptureCompleted;
            }

            assertThat(latch.await(timeoutInMs, TimeUnit.MILLISECONDS))
                    .isTrue();
        }

        public void assertCallbackIsNotCalled(long timeoutInMs) throws InterruptedException {
            CountDownLatch latch;
            synchronized (this) {
                mLatchForOnCaptureCompleted = new CountDownLatch(1);
                latch = mLatchForOnCaptureCompleted;
            }
            assertThat(latch.await(timeoutInMs, TimeUnit.MILLISECONDS))
                    .isFalse();
        }
    }

    private static class TestExecutor implements Executor {
        private CountDownLatch mLatch = new CountDownLatch(1);

        @Override
        public void execute(@NonNull Runnable command) {
            command.run();
            mLatch.countDown();
        }

        public void assertExecutorIsCalled(long timeoutInMS) throws InterruptedException {
            assertThat(mLatch.await(timeoutInMS, TimeUnit.MILLISECONDS)).isTrue();
        }
    }
}
