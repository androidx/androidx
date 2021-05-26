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

package androidx.camera.camera2.internal;

import static com.google.common.truth.Truth.assertThat;

import static junit.framework.TestCase.assertTrue;

import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.camera.camera2.internal.compat.CameraManagerCompat;
import androidx.camera.camera2.internal.util.SemaphoreReleasingCamera2Callbacks;
import androidx.camera.camera2.interop.Camera2Interop;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExposureState;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.impl.CameraControlInternal;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.CameraStateRegistry;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.ImmediateSurface;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.internal.CameraUseCaseAdapter;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.fakes.FakeCameraDeviceSurfaceManager;
import androidx.camera.testing.fakes.FakeUseCase;
import androidx.camera.testing.fakes.FakeUseCaseConfig;
import androidx.camera.testing.fakes.FakeUseCaseConfigFactory;
import androidx.core.os.HandlerCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Contains tests for {@link androidx.camera.camera2.internal.ExposureControl} internal
 * implementation.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class ExposureDeviceTest {

    @CameraSelector.LensFacing
    private static final int DEFAULT_LENS_FACING = CameraSelector.LENS_FACING_BACK;
    // For the purpose of this test, always say we have 1 camera available.
    private static final int DEFAULT_AVAILABLE_CAMERA_COUNT = 1;

    @Rule
    public TestRule mUseCamera = CameraUtil.grantCameraPermissionAndPreTest();

    private final ArrayList<FakeTestUseCase> mFakeTestUseCases = new ArrayList<>();
    private Camera2CameraImpl mCamera2CameraImpl;
    private static ExecutorService sCameraExecutor;
    private static HandlerThread sCameraHandlerThread;
    private static Handler sCameraHandler;
    private CameraStateRegistry mCameraStateRegistry;
    Semaphore mSemaphore;
    String mCameraId;
    SemaphoreReleasingCamera2Callbacks.SessionStateCallback mSessionStateCallback;
    private CameraUseCaseAdapter mCameraUseCaseAdapter;
    private CameraInfoInternal mCameraInfoInternal;
    private CameraControlInternal mCameraControlInternal;

    @BeforeClass
    public static void classSetup() {
        sCameraHandlerThread = new HandlerThread("cameraThread");
        sCameraHandlerThread.start();
        sCameraHandler = HandlerCompat.createAsync(sCameraHandlerThread.getLooper());
        sCameraExecutor = CameraXExecutors.newHandlerExecutor(sCameraHandler);
    }

    @AfterClass
    public static void classTeardown() {
        sCameraHandlerThread.quitSafely();
    }

    @Before
    public void setup() throws Exception {
        // TODO(b/162296654): Workaround the google_3a specific behavior.
        assumeFalse("Cuttlefish uses google_3a v1 or v2 it might fail to set EV before "
                + "first AE converge.", android.os.Build.MODEL.contains("Cuttlefish"));
        assumeFalse("Pixel uses google_3a v1 or v2 it might fail to set EV before "
                + "first AE converge.", android.os.Build.MODEL.contains("Pixel"));
        assumeFalse("Disable Nexus 5 in postsubmit for b/173743705",
                android.os.Build.MODEL.contains("Nexus 5") && !Log.isLoggable("MH", Log.DEBUG));

        assumeTrue(CameraUtil.deviceHasCamera());
        assumeTrue(CameraUtil.hasCameraWithLensFacing(DEFAULT_LENS_FACING));
        mSessionStateCallback = new SemaphoreReleasingCamera2Callbacks.SessionStateCallback();
        mCameraId = CameraUtil.getCameraIdWithLensFacing(DEFAULT_LENS_FACING);
        mSemaphore = new Semaphore(0);
        mCameraStateRegistry = new CameraStateRegistry(DEFAULT_AVAILABLE_CAMERA_COUNT);
        CameraManagerCompat cameraManagerCompat =
                CameraManagerCompat.from((Context) ApplicationProvider.getApplicationContext());
        Camera2CameraInfoImpl camera2CameraInfo = new Camera2CameraInfoImpl(
                mCameraId, cameraManagerCompat.getCameraCharacteristicsCompat(mCameraId));
        mCamera2CameraImpl = new Camera2CameraImpl(
                CameraManagerCompat.from((Context) ApplicationProvider.getApplicationContext()),
                mCameraId,
                camera2CameraInfo,
                mCameraStateRegistry, sCameraExecutor, sCameraHandler);

        mCameraInfoInternal = mCamera2CameraImpl.getCameraInfoInternal();
        mCameraControlInternal = mCamera2CameraImpl.getCameraControlInternal();
        mCamera2CameraImpl.open();

        FakeCameraDeviceSurfaceManager fakeCameraDeviceSurfaceManager =
                new FakeCameraDeviceSurfaceManager();
        fakeCameraDeviceSurfaceManager.setSuggestedResolution(mCameraId, FakeUseCaseConfig.class,
                new Size(640, 480));

        mCameraUseCaseAdapter = new CameraUseCaseAdapter(
                new LinkedHashSet<>(Collections.singleton(mCamera2CameraImpl)),
                fakeCameraDeviceSurfaceManager, new FakeUseCaseConfigFactory());
    }

    @After
    public void teardown() throws InterruptedException, ExecutionException {
        // Need to release the camera no matter what is done, otherwise the CameraDevice is not
        // closed.
        // When the CameraDevice is not closed, then it can cause problems with interferes with
        // other test cases.
        if (mCameraUseCaseAdapter != null) {
            mCameraUseCaseAdapter.removeUseCases(
                    Collections.unmodifiableCollection(mFakeTestUseCases));
        }
        if (mCamera2CameraImpl != null) {
            mCamera2CameraImpl.release().get();
        }

        for (FakeTestUseCase fakeUseCase : mFakeTestUseCases) {
            fakeUseCase.onDetached();
        }
    }

    private FakeTestUseCase openUseCase() throws CameraUseCaseAdapter.CameraException {
        FakeUseCaseConfig.Builder configBuilder =
                new FakeUseCaseConfig.Builder().setTargetName("UseCase");
        new Camera2Interop.Extender<>(configBuilder).setSessionStateCallback(mSessionStateCallback);

        FakeTestUseCase testUseCase = new FakeTestUseCase(configBuilder.getUseCaseConfig(),
                mCamera2CameraImpl, mSessionStateCallback);
        mFakeTestUseCases.add(testUseCase);

        mCameraUseCaseAdapter.addUseCases(Collections.singletonList(testUseCase));
        mCameraUseCaseAdapter.attachUseCases();

        return testUseCase;
    }

    @Test
    public void setExposure_futureResultTest() throws InterruptedException, TimeoutException,
            ExecutionException, CameraUseCaseAdapter.CameraException {
        ExposureState exposureState = mCameraInfoInternal.getExposureState();
        assumeTrue(exposureState.isExposureCompensationSupported());
        int upper = exposureState.getExposureCompensationRange().getUpper();

        openUseCase();
        // Wait a little bit for the camera to open.
        assertTrue(mSessionStateCallback.waitForOnConfigured(1));

        int ret = mCameraControlInternal.setExposureCompensationIndex(upper).get(3000,
                TimeUnit.MILLISECONDS);
        assertThat(ret).isEqualTo(upper);
    }

    @Test
    public void setExposureTest() throws InterruptedException, TimeoutException,
            ExecutionException, CameraUseCaseAdapter.CameraException {
        ExposureState exposureState = mCameraInfoInternal.getExposureState();
        assumeTrue(exposureState.isExposureCompensationSupported());
        int upper = exposureState.getExposureCompensationRange().getUpper();

        FakeTestUseCase useCase = openUseCase();
        // Wait a little bit for the camera to open.
        assertTrue(mSessionStateCallback.waitForOnConfigured(1));

        // Set the exposure compensation
        mCameraControlInternal.setExposureCompensationIndex(upper).get(3000, TimeUnit.MILLISECONDS);

        ArgumentCaptor<TotalCaptureResult> captureResultCaptor = ArgumentCaptor.forClass(
                TotalCaptureResult.class);
        CameraCaptureSession.CaptureCallback callback = mock(
                CameraCaptureSession.CaptureCallback.class);
        useCase.setCameraCaptureCallback(callback);
        verify(callback, timeout(3000).atLeastOnce()).onCaptureCompleted(
                any(CameraCaptureSession.class),
                any(CaptureRequest.class),
                captureResultCaptor.capture());
        List<TotalCaptureResult> totalCaptureResults = captureResultCaptor.getAllValues();
        TotalCaptureResult result = totalCaptureResults.get(totalCaptureResults.size() - 1);

        // Verify the exposure compensation target result is in the capture result.
        assertThat(result.get(CaptureResult.CONTROL_AE_EXPOSURE_COMPENSATION)).isEqualTo(upper);
    }

    @Test
    public void setExposureTest_runTwice()
            throws InterruptedException, TimeoutException, ExecutionException,
            CameraUseCaseAdapter.CameraException {
        ExposureState exposureState = mCameraInfoInternal.getExposureState();
        assumeTrue(exposureState.isExposureCompensationSupported());

        FakeTestUseCase useCase = openUseCase();
        // Wait a little bit for the camera to open.
        assertTrue(mSessionStateCallback.waitForOnConfigured(1));

        int upper = exposureState.getExposureCompensationRange().getUpper();

        // Set the EC value first time.
        mCameraControlInternal.setExposureCompensationIndex(upper - 1);

        // Set the EC value again, and verify this task should complete successfully.
        mCameraControlInternal.setExposureCompensationIndex(upper).get(3000, TimeUnit.MILLISECONDS);

        ArgumentCaptor<TotalCaptureResult> captureResultCaptor = ArgumentCaptor.forClass(
                TotalCaptureResult.class);
        CameraCaptureSession.CaptureCallback callback = mock(
                CameraCaptureSession.CaptureCallback.class);
        useCase.setCameraCaptureCallback(callback);
        verify(callback, timeout(3000).atLeastOnce()).onCaptureCompleted(
                any(CameraCaptureSession.class),
                any(CaptureRequest.class),
                captureResultCaptor.capture());
        List<TotalCaptureResult> totalCaptureResults = captureResultCaptor.getAllValues();
        TotalCaptureResult result = totalCaptureResults.get(totalCaptureResults.size() - 1);

        // Verify the exposure compensation target result is in the capture result.
        assertThat(result.get(CaptureResult.CONTROL_AE_EXPOSURE_COMPENSATION)).isEqualTo(upper);
    }

    @Test
    public void setExposureAndTriggerAe_theExposureSettingShouldApply()
            throws InterruptedException, ExecutionException, TimeoutException,
            CameraUseCaseAdapter.CameraException {
        ExposureState exposureState = mCameraInfoInternal.getExposureState();
        assumeTrue(exposureState.isExposureCompensationSupported());

        FakeTestUseCase useCase = openUseCase();
        ArgumentCaptor<TotalCaptureResult> captureResultCaptor = ArgumentCaptor.forClass(
                TotalCaptureResult.class);
        CameraCaptureSession.CaptureCallback callback = mock(
                CameraCaptureSession.CaptureCallback.class);
        useCase.setCameraCaptureCallback(callback);

        // Wait a little bit for the camera to open.
        assertTrue(mSessionStateCallback.waitForOnConfigured(1));

        // Set the exposure compensation
        int upper = exposureState.getExposureCompensationRange().getUpper();
        mCameraControlInternal.setExposureCompensationIndex(upper).get(3000, TimeUnit.MILLISECONDS);
        mCameraControlInternal.triggerAePrecapture().get(3000, TimeUnit.MILLISECONDS);

        // Verify the exposure compensation target result is in the capture result.
        verify(callback, timeout(3000).atLeastOnce()).onCaptureCompleted(
                any(CameraCaptureSession.class),
                any(CaptureRequest.class),
                captureResultCaptor.capture());
        List<TotalCaptureResult> totalCaptureResults = captureResultCaptor.getAllValues();
        TotalCaptureResult result = totalCaptureResults.get(totalCaptureResults.size() - 1);
        assertThat(result.get(CaptureResult.CONTROL_AE_EXPOSURE_COMPENSATION)).isEqualTo(upper);
    }

    @Test
    public void setExposureAndTriggerAf_theExposureSettingShouldApply()
            throws InterruptedException, ExecutionException, TimeoutException,
            CameraUseCaseAdapter.CameraException {
        ExposureState exposureState = mCameraInfoInternal.getExposureState();
        assumeTrue(exposureState.isExposureCompensationSupported());

        FakeTestUseCase useCase = openUseCase();
        ArgumentCaptor<TotalCaptureResult> captureResultCaptor = ArgumentCaptor.forClass(
                TotalCaptureResult.class);
        CameraCaptureSession.CaptureCallback callback = mock(
                CameraCaptureSession.CaptureCallback.class);
        useCase.setCameraCaptureCallback(callback);

        // Wait a little bit for the camera to open.
        assertTrue(mSessionStateCallback.waitForOnConfigured(1));

        int upper = exposureState.getExposureCompensationRange().getUpper();
        mCameraControlInternal.setExposureCompensationIndex(upper).get(3000, TimeUnit.MILLISECONDS);
        mCameraControlInternal.triggerAf().get(3000, TimeUnit.MILLISECONDS);

        // Verify the exposure compensation target result is in the capture result.
        verify(callback, timeout(3000).atLeastOnce()).onCaptureCompleted(
                any(CameraCaptureSession.class),
                any(CaptureRequest.class),
                captureResultCaptor.capture());
        List<TotalCaptureResult> totalCaptureResults = captureResultCaptor.getAllValues();
        TotalCaptureResult result = totalCaptureResults.get(totalCaptureResults.size() - 1);
        assertThat(result.get(CaptureResult.CONTROL_AE_EXPOSURE_COMPENSATION)).isEqualTo(upper);
    }

    @Test
    public void setExposureAndZoomRatio_theExposureSettingShouldApply()
            throws InterruptedException, ExecutionException, TimeoutException,
            CameraUseCaseAdapter.CameraException {
        ExposureState exposureState = mCameraInfoInternal.getExposureState();
        assumeTrue(exposureState.isExposureCompensationSupported());

        FakeTestUseCase useCase = openUseCase();
        ArgumentCaptor<TotalCaptureResult> captureResultCaptor = ArgumentCaptor.forClass(
                TotalCaptureResult.class);
        CameraCaptureSession.CaptureCallback callback = mock(
                CameraCaptureSession.CaptureCallback.class);
        useCase.setCameraCaptureCallback(callback);

        // Wait a little bit for the camera to open.
        assertTrue(mSessionStateCallback.waitForOnConfigured(1));

        int upper = exposureState.getExposureCompensationRange().getUpper();
        mCameraControlInternal.setExposureCompensationIndex(upper).get(3000, TimeUnit.MILLISECONDS);
        mCameraControlInternal.setZoomRatio(
                mCameraInfoInternal.getZoomState().getValue().getMaxZoomRatio()).get(3000,
                TimeUnit.MILLISECONDS);

        // Verify the exposure compensation target result is in the capture result.
        verify(callback, timeout(3000).atLeastOnce()).onCaptureCompleted(
                any(CameraCaptureSession.class),
                any(CaptureRequest.class),
                captureResultCaptor.capture());
        List<TotalCaptureResult> totalCaptureResults = captureResultCaptor.getAllValues();
        TotalCaptureResult result = totalCaptureResults.get(totalCaptureResults.size() - 1);
        assertThat(result.get(CaptureResult.CONTROL_AE_EXPOSURE_COMPENSATION)).isEqualTo(upper);
    }

    @Test
    public void setExposureAndLinearZoom_theExposureSettingShouldApply()
            throws InterruptedException, ExecutionException, TimeoutException,
            CameraUseCaseAdapter.CameraException {
        ExposureState exposureState = mCameraInfoInternal.getExposureState();
        assumeTrue(exposureState.isExposureCompensationSupported());

        FakeTestUseCase useCase = openUseCase();
        ArgumentCaptor<TotalCaptureResult> captureResultCaptor = ArgumentCaptor.forClass(
                TotalCaptureResult.class);
        CameraCaptureSession.CaptureCallback callback = mock(
                CameraCaptureSession.CaptureCallback.class);
        useCase.setCameraCaptureCallback(callback);

        // Wait a little bit for the camera to open.
        assertTrue(mSessionStateCallback.waitForOnConfigured(1));

        int upper = exposureState.getExposureCompensationRange().getUpper();
        mCameraControlInternal.setExposureCompensationIndex(upper).get(3000, TimeUnit.MILLISECONDS);
        mCameraControlInternal.setLinearZoom(0.5f).get(3000, TimeUnit.MILLISECONDS);

        // Verify the exposure compensation target result is in the capture result.
        verify(callback, timeout(3000).atLeastOnce()).onCaptureCompleted(
                any(CameraCaptureSession.class),
                any(CaptureRequest.class),
                captureResultCaptor.capture());
        List<TotalCaptureResult> totalCaptureResults = captureResultCaptor.getAllValues();
        TotalCaptureResult result = totalCaptureResults.get(totalCaptureResults.size() - 1);
        assertThat(result.get(CaptureResult.CONTROL_AE_EXPOSURE_COMPENSATION)).isEqualTo(upper);
    }

    @Test
    public void setExposureAndFlash_theExposureSettingShouldApply()
            throws InterruptedException, ExecutionException, TimeoutException,
            CameraUseCaseAdapter.CameraException {
        ExposureState exposureState = mCameraInfoInternal.getExposureState();
        assumeTrue(exposureState.isExposureCompensationSupported());

        FakeTestUseCase useCase = openUseCase();
        ArgumentCaptor<TotalCaptureResult> captureResultCaptor = ArgumentCaptor.forClass(
                TotalCaptureResult.class);
        CameraCaptureSession.CaptureCallback callback = mock(
                CameraCaptureSession.CaptureCallback.class);
        useCase.setCameraCaptureCallback(callback);

        // Wait a little bit for the camera to open.
        assertTrue(mSessionStateCallback.waitForOnConfigured(1));

        int upper = exposureState.getExposureCompensationRange().getUpper();
        mCameraControlInternal.setExposureCompensationIndex(upper).get(3000, TimeUnit.MILLISECONDS);
        mCameraControlInternal.setFlashMode(ImageCapture.FLASH_MODE_AUTO);

        // Verify the exposure compensation target result is in the capture result.
        verify(callback, timeout(3000).atLeastOnce()).onCaptureCompleted(
                any(CameraCaptureSession.class),
                any(CaptureRequest.class),
                captureResultCaptor.capture());
        List<TotalCaptureResult> totalCaptureResults = captureResultCaptor.getAllValues();
        TotalCaptureResult result = totalCaptureResults.get(totalCaptureResults.size() - 1);
        assertThat(result.get(CaptureResult.CONTROL_AE_EXPOSURE_COMPENSATION)).isEqualTo(upper);
    }

    @Test
    public void setExposureTimeout_theNextCallShouldWork()
            throws InterruptedException, ExecutionException, TimeoutException,
            CameraUseCaseAdapter.CameraException {
        ExposureState exposureState = mCameraInfoInternal.getExposureState();
        assumeTrue(exposureState.isExposureCompensationSupported());

        openUseCase();
        // Wait a little bit for the camera to open.
        assertTrue(mSessionStateCallback.waitForOnConfigured(1));

        try {
            // The set future should timeout in this test.
            mCameraControlInternal.setExposureCompensationIndex(1).get(0, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            assertThat(e).isInstanceOf(TimeoutException.class);
        }

        // Verify the second time call should set the new exposure value successfully.
        assertThat(mCameraControlInternal.setExposureCompensationIndex(2).get(3000,
                TimeUnit.MILLISECONDS)).isEqualTo(2);
    }

    public static class FakeTestUseCase extends FakeUseCase {
        private DeferrableSurface mDeferrableSurface;
        private final CameraCaptureSession.StateCallback mSessionStateCallback;
        CameraCaptureSession.CaptureCallback mCameraCaptureCallback;

        FakeTestUseCase(
                @NonNull FakeUseCaseConfig config,
                @NonNull CameraInternal cameraInternal,
                @NonNull CameraCaptureSession.StateCallback sessionStateCallback) {
            super(config);
            mSessionStateCallback = sessionStateCallback;
        }

        public void setCameraCaptureCallback(
                CameraCaptureSession.CaptureCallback cameraCaptureCallback) {
            mCameraCaptureCallback = cameraCaptureCallback;
        }

        @Override
        public void onDetached() {
            super.onDetached();
            if (mDeferrableSurface != null) {
                mDeferrableSurface.close();
            }
        }

        @Override
        @NonNull
        protected Size onSuggestedResolutionUpdated(
                @NonNull Size suggestedResolution) {
            createPipeline(suggestedResolution);
            notifyActive();
            return suggestedResolution;
        }

        private void createPipeline(Size resolution) {
            SessionConfig.Builder builder = SessionConfig.Builder.createFrom(getCurrentConfig());

            builder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
            if (mDeferrableSurface != null) {
                mDeferrableSurface.close();
            }

            // Create the metering DeferrableSurface
            SurfaceTexture surfaceTexture = new SurfaceTexture(0);
            surfaceTexture.setDefaultBufferSize(resolution.getWidth(), resolution.getHeight());
            Surface surface = new Surface(surfaceTexture);

            mDeferrableSurface = new ImmediateSurface(surface);
            mDeferrableSurface.getTerminationFuture().addListener(() -> {
                surface.release();
                surfaceTexture.release();
            }, CameraXExecutors.directExecutor());
            builder.addSurface(mDeferrableSurface);
            builder.addSessionStateCallback(mSessionStateCallback);
            builder.addRepeatingCameraCaptureCallback(CaptureCallbackContainer.create(
                    new CameraCaptureSession.CaptureCallback() {
                        @Override
                        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                @NonNull CaptureRequest request,
                                @NonNull TotalCaptureResult result) {
                            if (mCameraCaptureCallback != null) {
                                mCameraCaptureCallback.onCaptureCompleted(session, request, result);
                            }
                        }
                    }));

            builder.addErrorListener((sessionConfig, error) -> {
                // Create new pipeline and it will close the old one.
                createPipeline(resolution);
            });
            updateSessionConfig(builder.build());
        }
    }
}
