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

import static android.hardware.DataSpace.STANDARD_BT2020;
import static android.hardware.DataSpace.TRANSFER_GAMMA2_2;
import static android.hardware.DataSpace.TRANSFER_GAMMA2_6;
import static android.hardware.DataSpace.TRANSFER_GAMMA2_8;
import static android.hardware.DataSpace.TRANSFER_HLG;
import static android.hardware.DataSpace.TRANSFER_SMPTE_170M;
import static android.hardware.DataSpace.TRANSFER_SRGB;
import static android.hardware.DataSpace.TRANSFER_UNSPECIFIED;

import static androidx.camera.core.DynamicRange.HLG_10_BIT;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;

import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.DataSpace;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Range;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.camera2.impl.Camera2ImplConfig;
import androidx.camera.camera2.internal.CaptureSession.State;
import androidx.camera.camera2.internal.compat.CameraAccessExceptionCompat;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.camera2.internal.compat.CameraManagerCompat;
import androidx.camera.camera2.internal.compat.params.DynamicRangesCompat;
import androidx.camera.camera2.internal.compat.params.OutputConfigurationCompat;
import androidx.camera.camera2.internal.compat.params.SessionConfigurationCompat;
import androidx.camera.camera2.internal.compat.quirk.CameraQuirks;
import androidx.camera.camera2.internal.compat.quirk.ConfigureSurfaceToSecondarySessionFailQuirk;
import androidx.camera.camera2.internal.compat.quirk.DeviceQuirks;
import androidx.camera.camera2.internal.compat.quirk.PreviewOrientationIncorrectQuirk;
import androidx.camera.core.DynamicRange;
import androidx.camera.core.impl.CameraCaptureCallback;
import androidx.camera.core.impl.CameraCaptureCallbacks;
import androidx.camera.core.impl.CameraCaptureResult;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.ImmediateSurface;
import androidx.camera.core.impl.Quirks;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.camera.testing.impl.CameraUtil;
import androidx.camera.testing.impl.SurfaceTextureProvider;
import androidx.camera.testing.impl.WakelockEmptyActivityRule;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.os.HandlerCompat;
import androidx.core.util.Preconditions;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.AssumptionViolatedException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tests for {@link CaptureSession}. This requires an environment where a valid {@link
 * android.hardware.camera2.CameraDevice} can be opened since it is used to open a {@link
 * android.hardware.camera2.CaptureRequest}.
 */
@SuppressWarnings("unchecked")
@LargeTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 21)
@RequiresApi(21)
public final class CaptureSessionTest {

    // Enumerate possible SDR transfer functions. This may need to be updated if more transfer
    // functions are added to the DataSpace class.
    // This set is notably missing the HLG and PQ transfer functions, though HLG could
    // technically be used with 8-bit for SDR.
    // We also exclude LINEAR as most devices should at least apply gamma for SDR.
    private static final HashSet<Integer> POSSIBLE_COLOR_STANDARDS_SDR =
            new HashSet<>(Arrays.asList(
                    TRANSFER_UNSPECIFIED, // Some devices may use this as a default for SDR
                    TRANSFER_GAMMA2_2,
                    TRANSFER_GAMMA2_6,
                    TRANSFER_GAMMA2_8,
                    TRANSFER_SMPTE_170M,
                    TRANSFER_SRGB));

    /** Thread for all asynchronous calls. */
    private static HandlerThread sHandlerThread;
    /** Handler for all asynchronous calls. */
    private Handler mHandler;
    /** Executor which delegates to Handler */
    private Executor mExecutor;
    /** Scheduled executor service which delegates to Handler */
    private ScheduledExecutorService mScheduledExecutor;

    private CaptureSessionTestParameters mTestParameters0;
    private CaptureSessionTestParameters mTestParameters1;

    private CameraUtil.CameraDeviceHolder mCameraDeviceHolder;

    private CaptureSessionRepository mCaptureSessionRepository;
    private SynchronizedCaptureSession.OpenerBuilder mCaptureSessionOpenerBuilder;

    private final List<CaptureSession> mCaptureSessions = new ArrayList<>();
    private final List<DeferrableSurface> mDeferrableSurfaces = new ArrayList<>();

    private CameraCharacteristicsCompat mCameraCharacteristics;

    private DynamicRangesCompat mDynamicRangesCompat;

    @Rule
    public TestRule wakelockEmptyActivityRule = new WakelockEmptyActivityRule();

    @Rule
    public TestRule getUseCameraRule() {
        return CameraUtil.grantCameraPermissionAndPreTest(
                new CameraUtil.PreTestCameraIdList(Camera2Config.defaultConfig())
        );
    }

    @BeforeClass
    public static void setUpClass() {
        sHandlerThread = new HandlerThread("CaptureSessionTest");
        sHandlerThread.start();
    }

    @AfterClass
    public static void tearDownClass() {
        if (sHandlerThread != null) {
            sHandlerThread.quitSafely();
        }
    }

    @Before
    public void setup() throws CameraAccessException, InterruptedException,
            AssumptionViolatedException, TimeoutException, ExecutionException {
        mHandler = new Handler(sHandlerThread.getLooper());

        mExecutor = CameraXExecutors.newHandlerExecutor(mHandler);
        mScheduledExecutor = CameraXExecutors.newHandlerExecutor(mHandler);

        mCaptureSessionRepository = new CaptureSessionRepository(mExecutor);

        String cameraId = CameraUtil.getBackwardCompatibleCameraIdListOrThrow().get(0);
        Context context = ApplicationProvider.getApplicationContext();
        CameraManagerCompat cameraManager = CameraManagerCompat.from(context, mHandler);
        try {
            mCameraCharacteristics =
                    cameraManager.getCameraCharacteristicsCompat(cameraId);
        } catch (CameraAccessExceptionCompat e) {
            throw new AssumptionViolatedException("Could not retrieve camera characteristics", e);
        }

        mCaptureSessionOpenerBuilder = new SynchronizedCaptureSession.OpenerBuilder(mExecutor,
                mScheduledExecutor, mHandler, mCaptureSessionRepository,
                CameraQuirks.get(cameraId, mCameraCharacteristics), DeviceQuirks.getAll());

        mTestParameters0 = new CaptureSessionTestParameters("mTestParameters0",
                mCameraCharacteristics);
        mTestParameters1 = new CaptureSessionTestParameters("mTestParameters1",
                mCameraCharacteristics);

        mDynamicRangesCompat =
                DynamicRangesCompat.fromCameraCharacteristics(mCameraCharacteristics);

        mCameraDeviceHolder = CameraUtil.getCameraDevice(cameraId,
                mCaptureSessionRepository.getCameraStateCallback());
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException, TimeoutException {
        // Ensure all capture sessions are fully closed
        List<ListenableFuture<Void>> releaseFutures = new ArrayList<>();
        for (CaptureSession captureSession : mCaptureSessions) {
            releaseFutures.add(captureSession.release(/*abortInFlightCaptures=*/false));
        }
        mCaptureSessions.clear();
        Future<?> aggregateReleaseFuture = Futures.allAsList(releaseFutures);
        aggregateReleaseFuture.get(10L, TimeUnit.SECONDS);

        if (mCameraDeviceHolder != null) {
            CameraUtil.releaseCameraDevice(mCameraDeviceHolder);
        }

        mTestParameters0.tearDown();
        mTestParameters1.tearDown();
        for (DeferrableSurface deferrableSurface : mDeferrableSurfaces) {
            deferrableSurface.close();
        }
    }

    @Test
    public void setCaptureSessionSucceed() {
        CaptureSession captureSession = createCaptureSession();

        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        assertThat(captureSession.getSessionConfig()).isEqualTo(mTestParameters0.mSessionConfig);
    }

    @Test(expected = IllegalStateException.class)
    public void setCaptureSessionOnClosedSession_throwsException() {
        CaptureSession captureSession = createCaptureSession();

        SessionConfig newSessionConfig = mTestParameters0.mSessionConfig;

        captureSession.close();

        // Should throw IllegalStateException
        captureSession.setSessionConfig(newSessionConfig);
    }

    @Test
    public void openCaptureSessionSucceed() throws InterruptedException {
        CaptureSession captureSession = createCaptureSession();

        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        FutureCallback<Void> mockFutureCallback = mock(FutureCallback.class);

        Futures.addCallback(captureSession.open(mTestParameters0.mSessionConfig,
                        mCameraDeviceHolder.get(), mCaptureSessionOpenerBuilder.build()),
                mockFutureCallback, CameraXExecutors.mainThreadExecutor());

        assertTrue(mTestParameters0.waitForData());

        assertThat(captureSession.getState()).isEqualTo(State.OPENED);

        verify(mockFutureCallback, times(1)).onSuccess(any());

        // StateCallback.onConfigured() should be called to signal the session is configured.
        verify(mTestParameters0.mSessionStateCallback, times(1))
                .onConfigured(any(CameraCaptureSession.class));

        // CameraCaptureCallback.onCaptureCompleted() should be called to signal a capture attempt.
        verify(mTestParameters0.mSessionCameraCaptureCallback, timeout(3000).atLeastOnce())
                .onCaptureCompleted(anyInt(), any(CameraCaptureResult.class));
    }

    private boolean isLegacyCamera() {
        return Preconditions.checkNotNull(mCameraCharacteristics
                .get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL))
                == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY;
    }

    // Set stream use case is not supported before API 33
    @SdkSuppress(maxSdkVersion = 32, minSdkVersion = 21)
    @Test
    public void setStreamUseCaseNotSupported() {
        ImageReader imageReader0 = ImageReader.newInstance(640, 480, ImageFormat.YUV_420_888, 2);
        OutputConfigurationCompat outputConfigurationCompat =
                new OutputConfigurationCompat(imageReader0.getSurface());
        assertTrue(outputConfigurationCompat.getStreamUseCase()
                == OutputConfigurationCompat.STREAM_USE_CASE_NONE);
        outputConfigurationCompat.setStreamUseCase(1);
        assertTrue(outputConfigurationCompat.getStreamUseCase()
                == OutputConfigurationCompat.STREAM_USE_CASE_NONE);
    }

    @SdkSuppress(minSdkVersion = 33)
    @Test
    public void setStreamUseCase() {
        ImageReader imageReader0 = ImageReader.newInstance(640, 480, ImageFormat.YUV_420_888, 2);
        OutputConfigurationCompat outputConfigurationCompat =
                new OutputConfigurationCompat(imageReader0.getSurface());
        assertTrue(outputConfigurationCompat.getStreamUseCase()
                == CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_DEFAULT);
        outputConfigurationCompat.setStreamUseCase(
                CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW);
        assertTrue(outputConfigurationCompat.getStreamUseCase()
                == CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW);
    }

    @SdkSuppress(minSdkVersion = 33) // Can only verify data space on API 33+
    @Test
    public void openCaptureSessionWithDefault_usesSdrDynamicRange()
            throws ExecutionException, InterruptedException, TimeoutException {
        openCaptureSessionAndVerifyDynamicRangeApplied(
                /*inputDynamicRange=*/null, // Should default to SDR
                /*possibleColorStandards=*/null, // Do not check ColorSpace for SDR; could be many.
                POSSIBLE_COLOR_STANDARDS_SDR
        );
    }

    @SdkSuppress(minSdkVersion = 33) // HLG dynamic range only supported since API 33
    @Test
    public void openCaptureSessionWithHlgDynamicRange()
            throws ExecutionException, InterruptedException, TimeoutException {
        openCaptureSessionAndVerifyDynamicRangeApplied(
                HLG_10_BIT,
                Collections.singleton(STANDARD_BT2020),
                Collections.singleton(TRANSFER_HLG));
    }

    // Sharing surface of YUV format is supported since API 28
    @SdkSuppress(minSdkVersion = 28)
    @Test
    public void openCaptureSessionWithSharedSurface()
            throws InterruptedException, ExecutionException, TimeoutException {
        // 1. Arrange
        ImageReader imageReader0 = ImageReader.newInstance(640, 480, ImageFormat.YUV_420_888, 2);
        assumeTrue(
                new OutputConfigurationCompat(imageReader0.getSurface()).getMaxSharedSurfaceCount()
                        > 1);
        assumeFalse(isLegacyCamera());  // Legacy device doesn't support shared surface.

        DeferrableSurface surface0 = new ImmediateSurface(imageReader0.getSurface());
        ImageReader imageReader1 = ImageReader.newInstance(640, 480, ImageFormat.YUV_420_888, 2);
        DeferrableSurface surface1 = new ImmediateSurface(imageReader1.getSurface());
        surface0.getTerminationFuture().addListener(() -> imageReader0.close(),
                CameraXExecutors.mainThreadExecutor()
        );
        surface1.getTerminationFuture().addListener(() -> imageReader1.close(),
                CameraXExecutors.mainThreadExecutor());
        mDeferrableSurfaces.add(surface0);
        mDeferrableSurfaces.add(surface1);
        SessionConfig.OutputConfig outputConfig0 =
                SessionConfig.OutputConfig.builder(surface0).setSharedSurfaces(
                        Arrays.asList(surface1)).build();
        SessionConfig sessionConfig =
                new SessionConfig.Builder()
                        .addOutputConfig(outputConfig0)
                        .setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
                        .build();

        // 2. Act
        CaptureSession captureSession = createCaptureSession();
        captureSession.setSessionConfig(sessionConfig); // set repeating request
        ListenableFuture<Void> future = captureSession.open(sessionConfig,
                mCameraDeviceHolder.get(), mCaptureSessionOpenerBuilder.build());
        future.get(2, TimeUnit.SECONDS);

        // 3. Assert
        Handler handler = new Handler(Looper.getMainLooper());
        CountDownLatch latch0 = new CountDownLatch(1);
        CountDownLatch latch1 = new CountDownLatch(1);
        imageReader0.setOnImageAvailableListener(reader -> {
            latch0.countDown();
        }, handler);

        imageReader1.setOnImageAvailableListener(reader -> {
            latch1.countDown();
        }, handler);

        // Ensures main surface and shared share surface have outputs.
        assertThat(latch0.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(latch1.await(2, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void openCaptureSessionWithSessionType() {
        // 1. Arrange
        final int sessionTypeToVerify = 2;
        DeferrableSurface surface = createSurfaceTextureDeferrableSurface();
        SessionConfig.OutputConfig outputConfig0 =
                SessionConfig.OutputConfig.builder(surface).build();
        SessionConfig sessionConfig =
                new SessionConfig.Builder()
                        .addOutputConfig(outputConfig0)
                        .setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
                        .setSessionType(sessionTypeToVerify)
                        .build();
        FakeOpener fakeOpener = new FakeOpener();

        // 2. Act
        CaptureSession captureSession = new CaptureSession(mDynamicRangesCompat);
        captureSession.setSessionConfig(sessionConfig); // set repeating request
        captureSession.open(sessionConfig,
                mCameraDeviceHolder.get(), fakeOpener);
        ArgumentCaptor<SessionConfigurationCompat> captor =
                ArgumentCaptor.forClass(SessionConfigurationCompat.class);

        // 3. Assert
        verify(fakeOpener.mMock).openCaptureSession(any(), captor.capture(), any());
        assertThat(captor.getValue().getSessionType()).isEqualTo(sessionTypeToVerify);
    }

    // LOGICAL_MULTI_CAMERA_ACTIVE_PHYSICAL_ID is supported since API 29
    @SdkSuppress(minSdkVersion = 29)
    @Test
    public void openCaptureSessionWithPhysicalCameraId()
            throws InterruptedException, ExecutionException, TimeoutException {
        String cameraId = CameraUtil.getBackwardCompatibleCameraIdListOrThrow().get(0);
        // 1. Arrange
        List<String> physicalCameraIds = CameraUtil.getPhysicalCameraIds(cameraId);
        assumeFalse(physicalCameraIds.isEmpty());
        // get last physical camera id to make it different from default value
        String physicalCameraId = physicalCameraIds.get(physicalCameraIds.size() - 1);

        ImageReader imageReader0 = ImageReader.newInstance(640, 480, ImageFormat.YUV_420_888, 2);
        DeferrableSurface surface0 = new ImmediateSurface(imageReader0.getSurface());
        surface0.getTerminationFuture().addListener(() -> imageReader0.close(),
                CameraXExecutors.mainThreadExecutor()
        );
        mDeferrableSurfaces.add(surface0);
        SessionConfig.OutputConfig outputConfig0 =
                SessionConfig.OutputConfig.builder(surface0).setPhysicalCameraId(
                        physicalCameraId).build();
        SessionConfig.Builder sessionConfigBuilder =
                new SessionConfig.Builder()
                        .addOutputConfig(outputConfig0)
                        .setTemplateType(CameraDevice.TEMPLATE_PREVIEW);

        // future to receive the capture result
        ListenableFuture<CaptureResult> captureResultFuture =
                CallbackToFutureAdapter.getFuture(completer -> {
                    CameraCaptureCallback callback =
                            CaptureCallbackContainer.create(
                                    new CameraCaptureSession.CaptureCallback() {
                                        @Override
                                        public void onCaptureCompleted(
                                                @NonNull CameraCaptureSession session,
                                                @NonNull CaptureRequest request,
                                                @NonNull TotalCaptureResult result) {
                                            completer.set(result);
                                        }
                                    }
                            );
                    sessionConfigBuilder.addCameraCaptureCallback(callback);
                    return "capture result completer";
                });
        SessionConfig sessionConfig = sessionConfigBuilder.build();

        // 2. Act
        CaptureSession captureSession = createCaptureSession();
        captureSession.setSessionConfig(sessionConfig);
        captureSession.open(sessionConfig,
                mCameraDeviceHolder.get(), mCaptureSessionOpenerBuilder.build());

        // 3. Assert.
        CaptureResult captureResult = captureResultFuture.get(3, TimeUnit.SECONDS);
        assertThat(captureResult.get(CaptureResult.LOGICAL_MULTI_CAMERA_ACTIVE_PHYSICAL_ID))
                .isEqualTo(physicalCameraId);
    }

    @Test
    public void openCaptureSessionWithDuplicateSurface()
            throws InterruptedException, ExecutionException, TimeoutException {
        // 1. Arrange
        ImageReader imageReader = ImageReader.newInstance(640, 480, ImageFormat.YUV_420_888, 2);
        // deferrableSurface0 and deferrableSurface1 contain the same Surface.
        DeferrableSurface deferrableSurface0 = new ImmediateSurface(imageReader.getSurface());
        DeferrableSurface deferrableSurface1 = new ImmediateSurface(imageReader.getSurface());
        deferrableSurface0.getTerminationFuture().addListener(() -> imageReader.close(),
                CameraXExecutors.mainThreadExecutor()
        );
        mDeferrableSurfaces.add(deferrableSurface0);
        mDeferrableSurfaces.add(deferrableSurface1);
        SessionConfig sessionConfig =
                new SessionConfig.Builder()
                        .addSurface(deferrableSurface0)
                        .addSurface(deferrableSurface1)
                        .setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
                        .build();

        // 2. Act
        CaptureSession captureSession = createCaptureSession();
        captureSession.setSessionConfig(sessionConfig); // set repeating request
        ListenableFuture<Void> future = captureSession.open(sessionConfig,
                mCameraDeviceHolder.get(), mCaptureSessionOpenerBuilder.build());
        future.get(2, TimeUnit.SECONDS);

        // 3. Assert
        Handler handler = new Handler(Looper.getMainLooper());
        CountDownLatch latch0 = new CountDownLatch(1);
        CountDownLatch latch1 = new CountDownLatch(1);
        imageReader.setOnImageAvailableListener(reader -> {
            latch0.countDown();
        }, handler);

        assertThat(latch0.await(2, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void openCaptureSessionWithClosedSurfaceFails() {
        CaptureSession captureSession = createCaptureSession();

        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);
        DeferrableSurface surface = mTestParameters0.mSessionConfig.getSurfaces().get(0);
        surface.close();

        FutureCallback<Void> mockFutureCallback = mock(FutureCallback.class);

        Futures.addCallback(captureSession.open(mTestParameters0.mSessionConfig,
                        mCameraDeviceHolder.get(), mCaptureSessionOpenerBuilder.build()),
                mockFutureCallback, CameraXExecutors.mainThreadExecutor());

        verify(mockFutureCallback, timeout(3000)).onFailure(any(Throwable.class));
    }

    @Test
    public void captureSessionIncreasesSurfaceUseCountAfterOpen_andDecreasesAfterCameraIsClosed()
            throws InterruptedException, ExecutionException, TimeoutException {
        CaptureSession captureSession = createCaptureSession();

        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);
        DeferrableSurface surface = mTestParameters0.mSessionConfig.getSurfaces().get(0);
        int useCountBeforeOpen = surface.getUseCount();

        FutureCallback<Void> mockFutureCallback = mock(FutureCallback.class);

        Futures.addCallback(captureSession.open(mTestParameters0.mSessionConfig,
                        mCameraDeviceHolder.get(), mCaptureSessionOpenerBuilder.build()),
                mockFutureCallback, CameraXExecutors.mainThreadExecutor());

        verify(mockFutureCallback, timeout(3000)).onSuccess(any());
        int useCountAfterOpen = surface.getUseCount();

        reset(mockFutureCallback);

        captureSession.close();
        Futures.addCallback(captureSession.release(false), mockFutureCallback,
                CameraXExecutors.mainThreadExecutor());

        verify(mockFutureCallback, timeout(3000)).onSuccess(any());

        // Release the CaptureSession will not wait for the CameraCaptureSession close, the use
        // count of the surface will be decreased after the camera is closed or the new
        // CaptureSession is created. Close the CameraDevice to verify the surface use count
        // will actually decrease.
        CameraUtil.releaseCameraDevice(mCameraDeviceHolder);

        int useCountAfterRelease = surface.getUseCount();

        assertThat(useCountAfterOpen).isGreaterThan(useCountBeforeOpen);
        assertThat(useCountAfterRelease).isEqualTo(useCountBeforeOpen);
    }

    @Test
    public void captureSessionSurfaceUseCount_decreaseAllAfterCameraClose()
            throws InterruptedException, ExecutionException, TimeoutException {

        DeferrableSurface surface = mTestParameters0.mSessionConfig.getSurfaces().get(0);
        int useCount0BeforeOpen = surface.getUseCount();
        CaptureSession captureSession = createSessionAndWaitOpened(mTestParameters0, 3000);
        int useCount0AfterOpen = surface.getUseCount();

        captureSession.release(false);

        DeferrableSurface surface1 = mTestParameters1.mSessionConfig.getSurfaces().get(0);
        int useCount1BeforeOpen = surface1.getUseCount();
        CaptureSession captureSession1 = createSessionAndWaitOpened(mTestParameters1, 3000);
        int useCount1AfterOpen = surface1.getUseCount();

        captureSession1.release(false);

        CameraUtil.releaseCameraDevice(mCameraDeviceHolder);

        assertThat(useCount0AfterOpen).isGreaterThan(useCount0BeforeOpen);
        assertThat(useCount1AfterOpen).isGreaterThan(useCount1BeforeOpen);

        assertThat(surface.getUseCount()).isEqualTo(0);
        assertThat(surface1.getUseCount()).isEqualTo(0);
    }

    @Test
    public void captureSessionSurfaceUseCount_decreaseAfterNewCaptureSessionConfigured() {
        DeferrableSurface surface = mTestParameters0.mSessionConfig.getSurfaces().get(0);
        int useCountBeforeOpen = surface.getUseCount();
        CaptureSession captureSession = createSessionAndWaitOpened(mTestParameters0, 3000);
        int useCountAfterOpen = surface.getUseCount();

        captureSession.release(false);

        createSessionAndWaitOpened(mTestParameters1, 3000);
        int useCountAfterNewCaptureSessionConfigured = surface.getUseCount();

        assertThat(useCountAfterOpen).isGreaterThan(useCountBeforeOpen);
        assertThat(useCountAfterNewCaptureSessionConfigured).isEqualTo(useCountBeforeOpen);
    }

    @NonNull
    private CaptureSession createSessionAndWaitOpened(
            @NonNull CaptureSessionTestParameters parameters, long waitTimeout) {
        CaptureSession captureSession = createCaptureSession();
        captureSession.setSessionConfig(parameters.mSessionConfig);
        FutureCallback<Void> mockFutureCallback = mock(FutureCallback.class);
        Futures.addCallback(captureSession.open(parameters.mSessionConfig,
                        mCameraDeviceHolder.get(), mCaptureSessionOpenerBuilder.build()),
                mockFutureCallback, CameraXExecutors.mainThreadExecutor());

        verify(mockFutureCallback, timeout(waitTimeout)).onSuccess(any());

        return captureSession;
    }

    @Test
    public void openCaptureSessionWithOptionOverride() throws InterruptedException {
        CaptureSession captureSession = createCaptureSession();

        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        captureSession.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build());

        assertTrue(mTestParameters0.waitForData());

        assertThat(captureSession.getState()).isEqualTo(State.OPENED);

        // StateCallback.onConfigured() should be called to signal the session is configured.
        verify(mTestParameters0.mSessionStateCallback, times(1))
                .onConfigured(any(CameraCaptureSession.class));

        ArgumentCaptor<CameraCaptureResult> captureResultCaptor = ArgumentCaptor.forClass(
                CameraCaptureResult.class);

        // CameraCaptureCallback.onCaptureCompleted() should be called to signal a capture attempt.
        verify(mTestParameters0.mSessionCameraCaptureCallback, timeout(3000).atLeastOnce())
                .onCaptureCompleted(anyInt(), captureResultCaptor.capture());

        CameraCaptureResult cameraCaptureResult = captureResultCaptor.getValue();
        assertThat(cameraCaptureResult).isInstanceOf(Camera2CameraCaptureResult.class);

        CaptureResult captureResult =
                ((Camera2CameraCaptureResult) cameraCaptureResult).getCaptureResult();

        // From SessionConfig option
        assertThat(captureResult.getRequest().get(CaptureRequest.CONTROL_AF_MODE)).isEqualTo(
                CaptureRequest.CONTROL_AF_MODE_AUTO);
        assertThat(captureResult.getRequest().get(
                CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION)).isEqualTo(
                mTestParameters0.mEvRange.getUpper());
        assertThat(captureResult.getRequest().get(CaptureRequest.CONTROL_AE_MODE)).isEqualTo(
                CaptureRequest.CONTROL_AE_MODE_ON);
    }

    @Test
    public void closeUnopenedSession() {
        CaptureSession captureSession = createCaptureSession();

        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        captureSession.close();

        assertThat(captureSession.getState()).isEqualTo(State.RELEASED);
    }

    @Test
    public void releaseUnopenedSession() {
        CaptureSession captureSession = createCaptureSession();

        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        captureSession.release(/*abortInFlightCaptures=*/false);

        assertThat(captureSession.getState()).isEqualTo(State.RELEASED);
    }

    @Test
    public void closeOpenedSession() throws InterruptedException, ExecutionException {
        CaptureSession captureSession = createCaptureSession();

        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);
        captureSession.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build()).get();

        captureSession.close();

        // Session should be in closed state immediately after calling close() on an
        // opening/opened session.
        assertThat(captureSession.getState()).isEqualTo(State.CLOSED);
    }

    @Test
    public void releaseOpenedSession() throws InterruptedException, ExecutionException {
        CaptureSession captureSession = createCaptureSession();

        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);
        captureSession.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build()).get();
        ListenableFuture<Void> releaseFuture = captureSession.release(
                /*abortInFlightCaptures=*/false);

        // Wait for release
        releaseFuture.get();
        assertThat(captureSession.getState()).isEqualTo(State.RELEASED);

        // StateCallback.onClosed() should be called to signal the session is closed.
        verify(mTestParameters0.mSessionStateCallback, timeout(3000).times(1))
                .onClosed(any(CameraCaptureSession.class));
    }

    // Wait for future completion. The test fails if it timeouts.
    private <T> void assertFutureCompletes(Future<T> future, long timeout, TimeUnit timeUnit)
            throws InterruptedException, ExecutionException {
        try {
            future.get(timeout, timeUnit);
            assertTrue(true);
        } catch (TimeoutException e) {
            fail("Future cannot complete in time");
        }
    }

    @Test
    public void openSecondSession() throws InterruptedException, ExecutionException {
        CaptureSession captureSession0 = createCaptureSession();
        captureSession0.setSessionConfig(mTestParameters0.mSessionConfig);

        // First session is opened
        Future<Void> openFuture0 = captureSession0.open(mTestParameters0.mSessionConfig,
                mCameraDeviceHolder.get(), mCaptureSessionOpenerBuilder.build());
        assertFutureCompletes(openFuture0, 5, TimeUnit.SECONDS);

        captureSession0.close();

        // Open second session, which should cause first one to be released
        CaptureSession captureSession1 = createCaptureSession();
        captureSession1.setSessionConfig(mTestParameters1.mSessionConfig);
        Future<Void> openFuture1 = captureSession1.open(mTestParameters1.mSessionConfig,
                mCameraDeviceHolder.get(), mCaptureSessionOpenerBuilder.build());
        assertFutureCompletes(openFuture1, 5, TimeUnit.SECONDS);

        assertTrue(mTestParameters1.waitForData());
        assertThat(captureSession1.getState()).isEqualTo(State.OPENED);

        // First session should have StateCallback.onConfigured(), onClosed() calls.
        verify(mTestParameters0.mSessionStateCallback, times(1))
                .onConfigured(any(CameraCaptureSession.class));
        verify(mTestParameters0.mSessionStateCallback, times(1))
                .onClosed(any(CameraCaptureSession.class));
        assertThat(captureSession0.getState()).isEqualTo(State.RELEASED);

        // Second session should have StateCallback.onConfigured() call.
        verify(mTestParameters1.mSessionStateCallback, times(1))
                .onConfigured(any(CameraCaptureSession.class));

        // Second session should have CameraCaptureCallback.onCaptureCompleted() call.
        verify(mTestParameters1.mSessionCameraCaptureCallback, timeout(3000).atLeastOnce())
                .onCaptureCompleted(anyInt(), any(CameraCaptureResult.class));
    }

    @Test
    public void issueCaptureRequest() throws InterruptedException {
        CaptureSession captureSession = createCaptureSession();

        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);
        captureSession.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build());

        assertTrue(mTestParameters0.waitForData());

        assertThat(captureSession.getState()).isEqualTo(State.OPENED);

        captureSession.issueCaptureRequests(
                Collections.singletonList(mTestParameters0.mCaptureConfig));

        assertTrue(mTestParameters0.waitForCameraCaptureCallback());

        int expectedCaptureConfigId = mTestParameters0.mCaptureConfig.getId();
        ArgumentCaptor<Integer> captureConfigIdCaptor1 = ArgumentCaptor.forClass(Integer.class);
        verify(mTestParameters0.mCameraCaptureCallback, timeout(3000).times(1))
                .onCaptureStarted(captureConfigIdCaptor1.capture());
        assertThat(captureConfigIdCaptor1.getValue()).isEqualTo(expectedCaptureConfigId);

        ArgumentCaptor<Integer> captureConfigIdCaptor2 = ArgumentCaptor.forClass(Integer.class);
        verify(mTestParameters0.mCameraCaptureCallback, timeout(3000).times(1))
                .onCaptureCompleted(captureConfigIdCaptor2.capture(),
                        any(CameraCaptureResult.class));
        assertThat(captureConfigIdCaptor2.getValue()).isEqualTo(expectedCaptureConfigId);
    }

    @Test
    public void issueCaptureRequestAppendAndOverrideRepeatingOptions() throws InterruptedException {
        CaptureSession captureSession = createCaptureSession();

        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);
        captureSession.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build());

        assertTrue(mTestParameters0.waitForData());

        assertThat(captureSession.getState()).isEqualTo(State.OPENED);

        captureSession.issueCaptureRequests(
                Collections.singletonList(mTestParameters0.mCaptureConfig));

        assertTrue(mTestParameters0.waitForCameraCaptureCallback());

        ArgumentCaptor<CameraCaptureResult> captureResultCaptor = ArgumentCaptor.forClass(
                CameraCaptureResult.class);

        // CameraCaptureCallback.onCaptureCompleted() should be called to signal a capture attempt.
        verify(mTestParameters0.mCameraCaptureCallback, timeout(3000).times(1))
                .onCaptureCompleted(anyInt(), captureResultCaptor.capture());

        CameraCaptureResult cameraCaptureResult = captureResultCaptor.getValue();
        assertThat(cameraCaptureResult).isInstanceOf(Camera2CameraCaptureResult.class);

        CaptureResult captureResult =
                ((Camera2CameraCaptureResult) cameraCaptureResult).getCaptureResult();

        // From CaptureConfig option
        assertThat(captureResult.getRequest().get(CaptureRequest.CONTROL_AF_MODE)).isEqualTo(
                CaptureRequest.CONTROL_AF_MODE_OFF);

        // From SessionConfig option
        assertThat(captureResult.getRequest().get(
                CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION)).isEqualTo(
                mTestParameters0.mEvRange.getUpper());
        assertThat(captureResult.getRequest().get(CaptureRequest.CONTROL_AE_MODE)).isEqualTo(
                CaptureRequest.CONTROL_AE_MODE_ON);
    }

    @Test
    public void issueCaptureRequestAcrossCaptureSessions() throws InterruptedException {
        CaptureSession captureSession0 = createCaptureSession();
        captureSession0.setSessionConfig(mTestParameters0.mSessionConfig);

        captureSession0.issueCaptureRequests(
                Collections.singletonList(mTestParameters0.mCaptureConfig));
        captureSession0.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build());

        captureSession0.close();

        // Most close the capture session before start a new one, some legacy devices or Android
        // API < M need to recreate the surface for the new CaptureSession.
        captureSession0.release(false);

        CaptureSession captureSession1 = createCaptureSession();
        captureSession1.setSessionConfig(captureSession0.getSessionConfig());
        if (!captureSession0.getCaptureConfigs().isEmpty()) {
            captureSession1.issueCaptureRequests(captureSession0.getCaptureConfigs());
        }
        captureSession1.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build());

        assertTrue(mTestParameters0.waitForCameraCaptureCallback());

        // CameraCaptureCallback.onCaptureCompleted() should be called to signal a capture attempt.
        verify(mTestParameters0.mCameraCaptureCallback, timeout(3000).times(1))
                .onCaptureCompleted(anyInt(), any(CameraCaptureResult.class));
    }

    @Test
    public void issueCaptureRequestBeforeCaptureSessionOpened() throws InterruptedException {
        CaptureSession captureSession = createCaptureSession();

        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        captureSession.issueCaptureRequests(
                Collections.singletonList(mTestParameters0.mCaptureConfig));
        captureSession.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build());

        assertTrue(mTestParameters0.waitForCameraCaptureCallback());

        // CameraCaptureCallback.onCaptureCompleted() should be called to signal a capture attempt.
        verify(mTestParameters0.mCameraCaptureCallback, timeout(3000).times(1))
                .onCaptureCompleted(anyInt(), any(CameraCaptureResult.class));
    }

    @Test(expected = IllegalStateException.class)
    public void issueCaptureRequestOnClosedSession_throwsException() {
        CaptureSession captureSession = createCaptureSession();

        captureSession.close();

        // Should throw IllegalStateException
        captureSession.issueCaptureRequests(
                Collections.singletonList(mTestParameters0.mCaptureConfig));
    }

    @Test
    public void startStreamingAfterOpenCaptureSession()
            throws InterruptedException, ExecutionException {
        CaptureSession captureSession = createCaptureSession();
        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);
        captureSession.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build());

        assertTrue(mTestParameters0.waitForData());
        assertThat(captureSession.getState()).isEqualTo(State.OPENED);

        SynchronizedCaptureSession syncCaptureSession = captureSession.mSynchronizedCaptureSession;
        assertFutureCompletes(syncCaptureSession.getOpeningBlocker(), 5, TimeUnit.SECONDS);

        verify(mTestParameters0.mCamera2CaptureCallback, timeout(3000).atLeastOnce())
                .onCaptureStarted(any(CameraCaptureSession.class), any(CaptureRequest.class),
                        any(Long.class), any(Long.class));
    }

    @Test
    public void surfaceTerminationFutureIsCalledWhenSessionIsClose() throws InterruptedException {
        mCaptureSessionOpenerBuilder = new SynchronizedCaptureSession.OpenerBuilder(mExecutor,
                mScheduledExecutor, mHandler, mCaptureSessionRepository,
                new Quirks(Arrays.asList(new PreviewOrientationIncorrectQuirk())),
                DeviceQuirks.getAll());

        CaptureSession captureSession = createCaptureSession();
        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        captureSession.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build());

        assertTrue(mTestParameters0.waitForData());

        Runnable runnable = mock(Runnable.class);
        mTestParameters0.mDeferrableSurface.getTerminationFuture().addListener(runnable,
                CameraXExecutors.directExecutor());

        captureSession.release(/*abortInFlightCaptures=*/false);

        Mockito.verify(runnable, timeout(3000).times(1)).run();
    }

    @Test
    public void cameraOnError_closeDeferrableSurfaces() throws InterruptedException {
        mCaptureSessionOpenerBuilder = new SynchronizedCaptureSession.OpenerBuilder(mExecutor,
                mScheduledExecutor, mHandler, mCaptureSessionRepository,
                new Quirks(Collections.emptyList()), DeviceQuirks.getAll());

        CaptureSession captureSession = createCaptureSession();
        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        captureSession.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build());

        assertTrue(mTestParameters0.waitForData());

        Runnable runnable = mock(Runnable.class);
        mTestParameters0.mDeferrableSurface.getTerminationFuture().addListener(runnable,
                CameraXExecutors.directExecutor());

        // Act. Simulate CameraDevice.StateCallback#onError
        mCaptureSessionRepository.getCameraStateCallback().onError(mCameraDeviceHolder.get(),
                CameraDevice.StateCallback.ERROR_CAMERA_SERVICE);

        // Assert. Verify DeferrableSurfaces are closed.
        Mockito.verify(runnable, timeout(3000).times(1)).run();
    }

    @Test
    public void closingCaptureSessionClosesDeferrableSurface()
            throws ExecutionException, InterruptedException {
        mCaptureSessionOpenerBuilder = new SynchronizedCaptureSession.OpenerBuilder(mExecutor,
                mScheduledExecutor, mHandler, mCaptureSessionRepository,
                new Quirks(Arrays.asList(new ConfigureSurfaceToSecondarySessionFailQuirk())),
                DeviceQuirks.getAll());

        CaptureSession captureSession = createCaptureSession();

        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        Future<Void> openFuture = captureSession.open(mTestParameters0.mSessionConfig,
                mCameraDeviceHolder.get(), mCaptureSessionOpenerBuilder.build());
        assertFutureCompletes(openFuture, 5, TimeUnit.SECONDS);

        captureSession.release(false);

        // Verify the next CaptureSession should get an invalid DeferrableSurface.
        CaptureSession captureSession1 = createCaptureSession();
        ListenableFuture<Void> openFuture1 = captureSession1.open(mTestParameters0.mSessionConfig,
                mCameraDeviceHolder.get(), mCaptureSessionOpenerBuilder.build());

        FutureCallback<Void> futureCallback = Mockito.mock(FutureCallback.class);
        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);
        Futures.addCallback(openFuture1, futureCallback,
                CameraXExecutors.directExecutor());
        verify(futureCallback, timeout(3000).times(1)).onFailure(throwableCaptor.capture());

        assertThat(throwableCaptor.getValue()).isInstanceOf(
                DeferrableSurface.SurfaceClosedException.class);
    }

    @Test
    public void openCaptureSessionTwice_theSecondaryCallShouldNoop() {
        CaptureSession captureSession = createCaptureSession();
        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        captureSession.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build());
        captureSession.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build());

        // StateCallback.onConfigured() should be called to signal the session is configured.
        verify(mTestParameters0.mSessionStateCallback, timeout(3000).times(1))
                .onConfigured(any(CameraCaptureSession.class));
        assertThat(captureSession.getState()).isEqualTo(State.OPENED);
    }

    @Test
    public void releaseImmediateAfterOpenCaptureSession()
            throws ExecutionException, InterruptedException {
        CaptureSession captureSession = createCaptureSession();
        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        captureSession.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build());
        Future<Void> releaseFuture = captureSession.release(false);
        assertFutureCompletes(releaseFuture, 5, TimeUnit.SECONDS);

        // The captureSession state should change to RELEASED state
        assertThat(captureSession.getState()).isEqualTo(State.RELEASED);
    }

    @Test
    @SuppressWarnings("deprecation") /* AsyncTask */
    public void cancelOpenCaptureSessionListenableFuture_shouldNoop() {
        CaptureSession captureSession = createCaptureSession();
        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        FutureCallback<Void> mockFutureCallback = mock(FutureCallback.class);
        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);
        ListenableFuture<Void> openingFuture = captureSession.open(mTestParameters0.mSessionConfig,
                mCameraDeviceHolder.get(), mCaptureSessionOpenerBuilder.build());
        Futures.addCallback(openingFuture, mockFutureCallback,
                android.os.AsyncTask.THREAD_POOL_EXECUTOR);
        openingFuture.cancel(true);

        // The captureSession opening should callback onFailure with a CancellationException.
        verify(mockFutureCallback, timeout(3000).times(1)).onFailure(throwableCaptor.capture());
        assertThat(throwableCaptor.getValue()).isInstanceOf(CancellationException.class);

        // The opening task should not propagate the cancellation to the internal
        // ListenableFuture task, so the captureSession should keep running. The
        // StateCallback.onConfigured() should be called and change the state to OPENED.
        verify(mTestParameters0.mSessionStateCallback, timeout(3000).times(1))
                .onConfigured(any(CameraCaptureSession.class));
        assertThat(captureSession.getState()).isEqualTo(State.OPENED);
    }

    @Test
    public void openCaptureSessionFailed_withClosedDeferrableSurface() {
        // Close the configured DeferrableSurface for testing.
        mTestParameters0.mDeferrableSurface.close();

        CaptureSession captureSession = createCaptureSession();
        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        FutureCallback<Void> mockFutureCallback = mock(FutureCallback.class);
        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);

        Futures.addCallback(captureSession.open(mTestParameters0.mSessionConfig,
                        mCameraDeviceHolder.get(), mCaptureSessionOpenerBuilder.build()),
                mockFutureCallback, CameraXExecutors.mainThreadExecutor());

        verify(mockFutureCallback, timeout(3000).times(1)).onFailure(throwableCaptor.capture());
        // The captureSession opening should callback onFailure when the DeferrableSurface is
        // already closed.
        assertThat(throwableCaptor.getValue()).isInstanceOf(
                DeferrableSurface.SurfaceClosedException.class);
    }

    private CaptureSession createCaptureSession() {
        CaptureSession captureSession = new CaptureSession(mDynamicRangesCompat);
        mCaptureSessions.add(captureSession);
        return captureSession;
    }

    @NonNull
    private DeferrableSurface createSurfaceTextureDeferrableSurface() {
        SurfaceTexture surfaceTexture = new SurfaceTexture(0);
        surfaceTexture.setDefaultBufferSize(640, 480);
        surfaceTexture.detachFromGLContext();
        Surface surface = new Surface(surfaceTexture);
        DeferrableSurface deferrableSurface = new ImmediateSurface(surface);
        deferrableSurface.getTerminationFuture().addListener(() -> {
            surface.release();
            surfaceTexture.release();
        }, CameraXExecutors.directExecutor());
        mDeferrableSurfaces.add(deferrableSurface);
        return deferrableSurface;
    }

    @Test
    public void issueCaptureCancelledBeforeExecuting() {
        CaptureSession captureSession = createCaptureSession();
        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);
        captureSession.issueCaptureRequests(
                Collections.singletonList(mTestParameters0.mCaptureConfig));

        captureSession.cancelIssuedCaptureRequests();

        ArgumentCaptor<Integer> captureConfigIdCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(mTestParameters0.mCameraCaptureCallback, timeout(3000).times(1))
                .onCaptureCancelled(captureConfigIdCaptor.capture());

        int expectedCaptureConfigId = mTestParameters0.mCaptureConfig.getId();
        assertThat(captureConfigIdCaptor.getValue()).isEqualTo(expectedCaptureConfigId);
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
    public void cameraDisconnected_whenOpeningCaptureSessions_onClosedShouldBeCalled()
            throws CameraAccessException, InterruptedException, ExecutionException,
            TimeoutException {
        assumeFalse("Known device issue, b/255461164", "cph1931".equalsIgnoreCase(Build.MODEL));

        List<OutputConfigurationCompat> outputConfigList = new LinkedList<>();
        outputConfigList.add(
                new OutputConfigurationCompat(mTestParameters0.mImageReader.getSurface()));

        CountDownLatch endedCountDown = new CountDownLatch(1);
        CameraCaptureSession.StateCallback testStateCallback =
                new CameraCaptureSession.StateCallback() {

                    @Override
                    public void onClosed(@NonNull CameraCaptureSession session) {
                        endedCountDown.countDown();
                    }

                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {

                    }

                    @Override
                    public void onConfigureFailed(
                            @NonNull CameraCaptureSession cameraCaptureSession) {
                        endedCountDown.countDown();
                    }
                };

        SynchronizedCaptureSession.Opener opener = mCaptureSessionOpenerBuilder.build();
        SessionConfigurationCompat sessionConfigCompat = opener.createSessionConfigurationCompat(
                SessionConfigurationCompat.SESSION_REGULAR,
                outputConfigList,
                new SynchronizedCaptureSessionStateCallbacks.Adapter(testStateCallback));

        // Open the CameraCaptureSession without waiting for the onConfigured() callback.
        opener.openCaptureSession(mCameraDeviceHolder.get(),
                sessionConfigCompat, mTestParameters0.mSessionConfig.getSurfaces());

        // Open the camera again to simulate the cameraDevice is disconnected
        CountDownLatch countDownLatch = new CountDownLatch(1);
        CameraUtil.CameraDeviceHolder holder = CameraUtil.getCameraDevice(
                new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(@NonNull CameraDevice camera) {
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onDisconnected(@NonNull CameraDevice camera) {

                    }

                    @Override
                    public void onError(@NonNull CameraDevice camera, int error) {

                    }
                });
        // Only verify the result when the camera can open successfully.
        assumeTrue(countDownLatch.await(3, TimeUnit.SECONDS));

        // The opened CaptureSession should be closed after the CameraDevice is disconnected.
        assumeTrue(endedCountDown.await(3, TimeUnit.SECONDS));
        assertThat(mCaptureSessionRepository.getCaptureSessions().size()).isEqualTo(0);

        CameraUtil.releaseCameraDevice(holder);
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
    public void cameraDisconnected_captureSessionsOnClosedShouldBeCalled_repeatingStarted()
            throws ExecutionException, InterruptedException, TimeoutException,
            CameraAccessException {
        assumeFalse("Known device issue, b/255461164", "cph1931".equalsIgnoreCase(Build.MODEL));

        CaptureSession captureSession = createCaptureSession();
        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);
        captureSession.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build()).get();
        assertTrue(mTestParameters0.waitForData());
        CaptureSession captureSession1 = createCaptureSession();
        captureSession1.setSessionConfig(mTestParameters1.mSessionConfig);
        captureSession1.open(mTestParameters1.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build()).get();
        assertTrue(mTestParameters1.waitForData());

        // Open the camera again to simulate the cameraDevice is disconnected
        CountDownLatch countDownLatch = new CountDownLatch(1);
        CameraUtil.CameraDeviceHolder holder = CameraUtil.getCameraDevice(
                new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(@NonNull CameraDevice camera) {
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onDisconnected(@NonNull CameraDevice camera) {

                    }

                    @Override
                    public void onError(@NonNull CameraDevice camera, int error) {

                    }
                });

        // Only verify the result when the camera can open successfully.
        assumeTrue(countDownLatch.await(3000, TimeUnit.MILLISECONDS));

        // The opened CaptureSession should be closed after the CameraDevice is disconnected.
        verify(mTestParameters0.mSessionStateCallback, timeout(3000).times(1))
                .onClosed(any(CameraCaptureSession.class));
        verify(mTestParameters1.mSessionStateCallback, timeout(3000).times(1))
                .onClosed(any(CameraCaptureSession.class));
        assertThat(mCaptureSessionRepository.getCaptureSessions().size()).isEqualTo(0);
        assertThat(captureSession.getState()).isEqualTo(State.RELEASED);

        CameraUtil.releaseCameraDevice(holder);
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
    public void cameraDisconnected_captureSessionsOnClosedShouldBeCalled_withoutRepeating()
            throws CameraAccessException, InterruptedException, ExecutionException,
            TimeoutException {
        assumeFalse("Known device issue, b/255461164", "cph1931".equalsIgnoreCase(Build.MODEL));

        // The CameraCaptureSession will call close() automatically when CameraDevice is
        // disconnected, and the CameraCaptureSession should receive the onClosed() callback if
        // the CameraDevice status is idling.
        // In this test, we didn't start the repeating for the CaptureSession, the CameraDevice
        // status should be in idle statue when we trying to disconnect the CameraDevice. So the
        // CaptureSession can receive the onClosed() callback after the camera is disconnected.
        CaptureSession captureSession = createCaptureSession();
        captureSession.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build()).get();
        CaptureSession captureSession1 = createCaptureSession();
        captureSession1.open(mTestParameters1.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build()).get();

        // Open the camera again to simulate the cameraDevice is disconnected
        CountDownLatch countDownLatch = new CountDownLatch(1);
        CameraUtil.CameraDeviceHolder holder = CameraUtil.getCameraDevice(
                new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(@NonNull CameraDevice camera) {
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onDisconnected(@NonNull CameraDevice camera) {

                    }

                    @Override
                    public void onError(@NonNull CameraDevice camera, int error) {

                    }
                });
        // Only verify the result when the camera can open successfully.
        assumeTrue(countDownLatch.await(3000, TimeUnit.MILLISECONDS));

        // The opened CaptureSession should be closed after the CameraDevice is disconnected.
        verify(mTestParameters0.mSessionStateCallback, timeout(5000)).onClosed(
                any(CameraCaptureSession.class));
        assertThat(mCaptureSessionRepository.getCaptureSessions().size()).isEqualTo(0);

        CameraUtil.releaseCameraDevice(holder);
    }

    @Test
    public void closePreviousOpeningCaptureSession_afterNewCaptureSessionCreated()
            throws ExecutionException, InterruptedException {

        List<OutputConfigurationCompat> outputConfigList = new LinkedList<>();
        outputConfigList.add(
                new OutputConfigurationCompat(mTestParameters0.mImageReader.getSurface()));

        SynchronizedCaptureSession.Opener synchronizedCaptureSessionOpener =
                mCaptureSessionOpenerBuilder.build();

        SessionConfigurationCompat sessionConfigCompat =
                synchronizedCaptureSessionOpener.createSessionConfigurationCompat(
                        SessionConfigurationCompat.SESSION_REGULAR,
                        outputConfigList,
                        new SynchronizedCaptureSessionStateCallbacks.Adapter(
                                mTestParameters0.mSessionStateCallback));

        // Open the CameraCaptureSession without waiting for the onConfigured() callback.
        synchronizedCaptureSessionOpener.openCaptureSession(mCameraDeviceHolder.get(),
                sessionConfigCompat, mTestParameters0.mSessionConfig.getSurfaces());

        CaptureSession captureSession1 = createCaptureSession();
        captureSession1.open(mTestParameters1.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build()).get();

        // The first capture sessions should be closed since the new CaptureSession is created.
        verify(mTestParameters0.mSessionStateCallback, timeout(3000).times(1))
                .onClosed(any(CameraCaptureSession.class));

        verify(mTestParameters1.mSessionStateCallback, timeout(3000).times(1))
                .onConfigured(any(CameraCaptureSession.class));
        assertThat(mCaptureSessionRepository.getCaptureSessions().size()).isEqualTo(1);
    }

    @Test
    public void closePreviousCaptureSession_afterNewCaptureSessionCreated()
            throws ExecutionException, InterruptedException {

        CaptureSession captureSession = createCaptureSession();
        captureSession.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build()).get();
        // Not close the first capture session before opening the next CaptureSession.

        CaptureSession captureSession1 = createCaptureSession();
        captureSession1.open(mTestParameters1.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build()).get();

        // The first capture sessions should be closed since the new CaptureSession is created.
        verify(mTestParameters0.mSessionStateCallback, timeout(3000).times(1))
                .onClosed(any(CameraCaptureSession.class));

        verify(mTestParameters1.mSessionStateCallback, timeout(3000).times(1))
                .onConfigured(any(CameraCaptureSession.class));
        assertThat(mCaptureSessionRepository.getCaptureSessions().size()).isEqualTo(1);
        assertThat(captureSession.getState()).isEqualTo(State.RELEASED);
    }

    @Test
    public void openCaptureSession_surfaceOrderShouldBeRetained()
            throws ExecutionException, InterruptedException {
        // If this test is flaky, the more surfaces produced, the more likely it is able to detect
        // problems.
        final int surfaceCount = 6;
        List<DeferrableSurface> surfaceList = new ArrayList<>();
        for (int i = 0; i < surfaceCount; i++) {
            surfaceList.add(createSurfaceTextureDeferrableSurface());
        }
        SessionConfig.Builder sessionConfigBuilder = new SessionConfig.Builder();
        sessionConfigBuilder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
        for (DeferrableSurface deferrableSurface : surfaceList) {
            sessionConfigBuilder.addSurface(deferrableSurface);
        }

        FakeOpener fakeOpener = new FakeOpener();
        // Don't use #createCaptureSession since FakeOpenerImpl won't create CameraCaptureSession
        // so no need to be released.
        CaptureSession captureSession = new CaptureSession(mDynamicRangesCompat);
        captureSession.open(sessionConfigBuilder.build(), mCameraDeviceHolder.get(), fakeOpener);

        ArgumentCaptor<SessionConfigurationCompat> captor =
                ArgumentCaptor.forClass(SessionConfigurationCompat.class);
        verify(fakeOpener.mMock).openCaptureSession(any(), captor.capture(), any());

        List<OutputConfigurationCompat> outputConfigurationCompatList =
                captor.getValue().getOutputConfigurations();
        assertThat(outputConfigurationCompatList.size()).isEqualTo(surfaceCount);
        for (int i = 0; i < surfaceCount; i++) {
            assertThat(outputConfigurationCompatList.get(i).getSurface())
                    .isEqualTo(surfaceList.get(i).getSurface().get());
        }
    }

    @Test
    public void closePreviousCaptureSession_afterNewCaptureSessionCreated_runningRepeating()
            throws ExecutionException, InterruptedException {

        CaptureSession captureSession = createCaptureSession();
        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);
        captureSession.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build()).get();
        assertTrue(mTestParameters0.waitForData());

        CaptureSession captureSession1 = createCaptureSession();
        captureSession1.setSessionConfig(mTestParameters1.mSessionConfig);
        captureSession1.open(mTestParameters1.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build()).get();

        // The opened capture sessions should be closed.
        verify(mTestParameters0.mSessionStateCallback, timeout(3000).times(1))
                .onClosed(any(CameraCaptureSession.class));

        verify(mTestParameters1.mSessionStateCallback, timeout(3000).times(1))
                .onConfigured(any(CameraCaptureSession.class));
        assertThat(mCaptureSessionRepository.getCaptureSessions().size()).isEqualTo(1);
        assertThat(captureSession.getState()).isEqualTo(State.RELEASED);
    }

    @Test
    public void closePreviousClosingCaptureSession_afterNewCaptureSessionCreated_runningRepeating()
            throws ExecutionException, InterruptedException, TimeoutException {

        CaptureSession captureSession = createCaptureSession();
        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);
        captureSession.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build()).get();
        assertTrue(mTestParameters0.waitForData());
        // Call close() before the creating the next CaptureSession.
        captureSession.release(false);

        CaptureSession captureSession1 = createCaptureSession();
        captureSession1.setSessionConfig(mTestParameters1.mSessionConfig);
        captureSession1.open(mTestParameters1.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build()).get();

        // The opened capture sessions should be closed.
        verify(mTestParameters0.mSessionStateCallback, timeout(3000).times(1))
                .onClosed(any(CameraCaptureSession.class));

        verify(mTestParameters1.mSessionStateCallback, timeout(3000).times(1))
                .onConfigured(any(CameraCaptureSession.class));

        CameraUtil.releaseCameraDevice(mCameraDeviceHolder);

        // Close camera device should close all sessions.
        assertThat(mCaptureSessionRepository.getCaptureSessions().size()).isEqualTo(0);
        assertThat(captureSession.getState()).isEqualTo(State.RELEASED);
    }

    @Test
    public void closePreviousClosingCaptureSession_afterNewCaptureSessionCreated()
            throws ExecutionException, InterruptedException {

        CaptureSession captureSession = createCaptureSession();
        captureSession.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build()).get();
        // Call close() before the creating the next CaptureSession.
        captureSession.release(false);

        CaptureSession captureSession1 = createCaptureSession();
        captureSession1.open(mTestParameters1.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build()).get();

        // The opened capture sessions should be closed.
        verify(mTestParameters0.mSessionStateCallback, timeout(3000).times(1))
                .onClosed(any(CameraCaptureSession.class));

        verify(mTestParameters1.mSessionStateCallback, timeout(3000).times(1))
                .onConfigured(any(CameraCaptureSession.class));
        assertThat(mCaptureSessionRepository.getCaptureSessions().size()).isEqualTo(1);
        assertThat(captureSession.getState()).isEqualTo(State.RELEASED);
    }

    @Test
    public void setSessionConfigWithoutSurface_shouldStopRepeating()
            throws ExecutionException, InterruptedException {
        AtomicBoolean isStartMonitor = new AtomicBoolean(false);
        AtomicLong latestFrameTimeMs = new AtomicLong(0L);
        CountDownLatch onReadyCountDown = new CountDownLatch(1);

        // Create Surface
        ImageReader imageReader =
                ImageReader.newInstance(640, 480, ImageFormat.YUV_420_888, /*maxImages*/ 2);
        imageReader.setOnImageAvailableListener(reader -> {
            if (isStartMonitor.get()) {
                latestFrameTimeMs.set(System.currentTimeMillis());
            }
            Image image = reader.acquireNextImage();
            if (image != null) {
                image.close();
            }
        }, mHandler);
        DeferrableSurface surface = new ImmediateSurface(imageReader.getSurface());
        surface.getTerminationFuture().addListener(() -> imageReader.close(),
                CameraXExecutors.directExecutor());
        mDeferrableSurfaces.add(surface);

        // Prepare SessionConfig builder
        SessionConfig.Builder builder = new SessionConfig.Builder();
        builder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
        builder.addSessionStateCallback(new CameraCaptureSession.StateCallback() {

            @Override
            public void onConfigured(@NonNull CameraCaptureSession session) {
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            }

            @Override
            public void onReady(@NonNull CameraCaptureSession session) {
                if (isStartMonitor.get()) {
                    onReadyCountDown.countDown();
                }
            }
        });
        CameraCaptureCallback captureCallback =
                Mockito.mock(CameraCaptureCallback.class);
        builder.addRepeatingCameraCaptureCallback(captureCallback);

        // Create SessionConfig without Surface
        SessionConfig sessionConfigWithoutSurface = builder.build();

        // Create SessionConfig with Surface
        builder.addSurface(surface);
        SessionConfig sessionConfigWithSurface = builder.build();

        // Open CaptureSession
        CaptureSession captureSession = createCaptureSession();
        captureSession.open(sessionConfigWithSurface, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build()).get();

        // Activate repeating request
        captureSession.setSessionConfig(sessionConfigWithSurface);
        verify(captureCallback, timeout(3000L).atLeast(3)).onCaptureCompleted(anyInt(), any());

        // Deactivate repeating request
        isStartMonitor.set(true);
        captureSession.setSessionConfig(sessionConfigWithoutSurface);

        // Wait for #onReady which means there is no repeating request.
        // Some devices have a known issue where the #onReady callback may not be called after
        // calling the stopRepeating() method. The alternative way is to verify that the output
        // is no longer being produced. Please see b/303739264
        assertTrue(onReadyCountDown.await(3, TimeUnit.SECONDS)
                || (System.currentTimeMillis() - latestFrameTimeMs.get()) > 1000L);
    }

    @RequiresApi(33) // SurfaceTexture.getDataSpace() was added in API 33
    private void openCaptureSessionAndVerifyDynamicRangeApplied(
            @Nullable DynamicRange inputDynamicRange,
            @Nullable Set<Integer> possibleColorStandards,
            @Nullable Set<Integer> possibleTransferFns)
            throws ExecutionException, InterruptedException, TimeoutException {
        // 1. Arrange
        if (inputDynamicRange != null) {
            // Only run test on devices that support the
            assumeTrue(
                    mDynamicRangesCompat.getSupportedDynamicRanges().contains(inputDynamicRange));
        }

        CountDownLatch latch0 = new CountDownLatch(1);
        AtomicInteger dataSpace = new AtomicInteger(DataSpace.DATASPACE_UNKNOWN);
        ListenableFuture<SurfaceTextureProvider.SurfaceTextureHolder> surfaceTextureHolderFuture =
                SurfaceTextureProvider.createAutoDrainingSurfaceTextureAsync(mExecutor, 640, 480,
                        surfaceTexture -> {
                            dataSpace.set(surfaceTexture.getDataSpace());
                            latch0.countDown();
                        }, /* onClosed= */null);

        DeferrableSurface deferrableSurface = new DeferrableSurface() {
            @NonNull
            @Override
            protected ListenableFuture<Surface> provideSurface() {
                return Futures.transform(surfaceTextureHolderFuture,
                        surfaceTextureHolder -> {
                            Surface surface = new Surface(surfaceTextureHolder.getSurfaceTexture());
                            getTerminationFuture().addListener(surface::release, mExecutor);
                            return surface;
                        },
                        CameraXExecutors.directExecutor());
            }
        };

        deferrableSurface.getTerminationFuture().addListener(
                () -> Futures.addCallback(surfaceTextureHolderFuture,
                        new FutureCallback<SurfaceTextureProvider.SurfaceTextureHolder>() {
                            @Override
                            public void onSuccess(
                                    @Nullable SurfaceTextureProvider.SurfaceTextureHolder result) {
                                try {
                                    Preconditions.checkNotNull(result).close();
                                } catch (Exception e) {
                                    throw new AssertionError("Unable to release SurfaceTexture", e);
                                }
                            }

                            @Override
                            public void onFailure(@NonNull Throwable t) {
                                throw new AssertionError("Unable to retrieve SurfaceTexture", t);
                            }
                        }, mExecutor), CameraXExecutors.directExecutor());

        mDeferrableSurfaces.add(deferrableSurface);

        SessionConfig.OutputConfig.Builder outputConfigBuilder =
                SessionConfig.OutputConfig.builder(deferrableSurface);
        if (inputDynamicRange != null) {
            outputConfigBuilder.setDynamicRange(inputDynamicRange);
        }
        SessionConfig sessionConfig =
                new SessionConfig.Builder()
                        .addOutputConfig(outputConfigBuilder.build())
                        .setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
                        .build();

        // 2. Act
        CaptureSession captureSession = createCaptureSession();
        captureSession.setSessionConfig(sessionConfig); // set repeating request
        ListenableFuture<Void> future = captureSession.open(sessionConfig,
                mCameraDeviceHolder.get(), mCaptureSessionOpenerBuilder.build());
        future.get(2, TimeUnit.SECONDS);

        // 3. Assert
        assertWithMessage("Timed out while waiting for frame to be produced.")
                .that(latch0.await(2, TimeUnit.SECONDS))
                .isTrue();

        // Ensure the dataspace matches what is expected
        if (possibleColorStandards != null) {
            assertThat(DataSpace.getStandard(dataSpace.get())).isIn(possibleColorStandards);
        }
        if (possibleTransferFns != null) {
            assertThat(DataSpace.getTransfer(dataSpace.get())).isIn(possibleTransferFns);
        }
    }

    private static <T> CaptureConfig getCaptureConfig(CaptureRequest.Key<T> key, T effectValue,
            CameraCaptureCallback callback) {
        CaptureConfig.Builder captureConfigBuilder = new CaptureConfig.Builder();
        Camera2ImplConfig.Builder camera2ConfigurationBuilder =
                new Camera2ImplConfig.Builder();
        camera2ConfigurationBuilder.setCaptureRequestOption(key, effectValue);
        captureConfigBuilder.addImplementationOptions(camera2ConfigurationBuilder.build());
        captureConfigBuilder.addCameraCaptureCallback(callback);
        return captureConfigBuilder.build();
    }

    private static class FakeOpener implements SynchronizedCaptureSession.Opener {

        final SynchronizedCaptureSession.Opener mMock = mock(
                SynchronizedCaptureSession.Opener.class);

        @NonNull
        @Override
        public ListenableFuture<Void> openCaptureSession(@NonNull CameraDevice cameraDevice,
                @NonNull SessionConfigurationCompat sessionConfigurationCompat,
                @NonNull List<DeferrableSurface> deferrableSurfaces) {
            mMock.openCaptureSession(cameraDevice, sessionConfigurationCompat, deferrableSurfaces);
            return Futures.immediateFuture(null);
        }

        @NonNull
        @Override
        public SessionConfigurationCompat createSessionConfigurationCompat(int sessionType,
                @NonNull List<OutputConfigurationCompat> outputsCompat,
                @NonNull SynchronizedCaptureSession.StateCallback stateCallback) {
            mMock.createSessionConfigurationCompat(sessionType, outputsCompat, stateCallback);
            return new SessionConfigurationCompat(sessionType, outputsCompat, getExecutor(),
                    mock(CameraCaptureSession.StateCallback.class));
        }

        @NonNull
        @Override
        public Executor getExecutor() {
            mMock.getExecutor();
            return CameraXExecutors.directExecutor();
        }

        @NonNull
        @Override
        public ListenableFuture<List<Surface>> startWithDeferrableSurface(
                @NonNull List<DeferrableSurface> deferrableSurfaces, long timeout) {
            mMock.startWithDeferrableSurface(deferrableSurfaces, timeout);
            List<ListenableFuture<Surface>> listenableFutureSurfaces = new ArrayList<>();
            for (DeferrableSurface deferrableSurface : deferrableSurfaces) {
                listenableFutureSurfaces.add(deferrableSurface.getSurface());
            }
            return Futures.successfulAsList(listenableFutureSurfaces);
        }

        @Override
        public boolean stop() {
            mMock.stop();
            return false;
        }
    }

    /**
     * Collection of parameters required for setting a {@link CaptureSession} and wait for it to
     * produce data.
     */
    private static class CaptureSessionTestParameters {
        private static final int TIME_TO_WAIT_FOR_DATA_SECONDS = 3;
        /** Thread for all asynchronous calls. */
        private final HandlerThread mHandlerThread;
        /** Handler for all asynchronous calls. */
        private final Handler mHandler;
        /** Latch to wait for first image data to appear. */
        private final CountDownLatch mDataLatch = new CountDownLatch(1);

        /** Latch to wait for camera capture callback to be invoked. */
        private final CountDownLatch mCameraCaptureCallbackLatch = new CountDownLatch(1);

        /** Image reader that unlocks the latch waiting for the first image data to appear. */
        private final OnImageAvailableListener mOnImageAvailableListener =
                new OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(ImageReader reader) {
                        Image image = reader.acquireNextImage();
                        if (image != null) {
                            image.close();
                            mDataLatch.countDown();
                        }
                    }
                };

        private final ImageReader mImageReader;
        private final SessionConfig mSessionConfig;
        private final CaptureConfig mCaptureConfig;
        private static final int CAPTURE_CONFIG_ID = 110;

        private final CameraCaptureSession.StateCallback mSessionStateCallback =
                Mockito.mock(CameraCaptureSession.StateCallback.class);
        private final CameraCaptureCallback mSessionCameraCaptureCallback =
                Mockito.mock(CameraCaptureCallback.class);
        private final CameraCaptureCallback mCameraCaptureCallback =
                Mockito.mock(CameraCaptureCallback.class);
        private final CameraCaptureSession.CaptureCallback mCamera2CaptureCallback =
                Mockito.mock(CameraCaptureSession.CaptureCallback.class);

        private final DeferrableSurface mDeferrableSurface;
        private final Range<Integer> mEvRange;
        /**
         * A composite capture callback that dispatches callbacks to both mock and real callbacks.
         * The mock callback is used to verify the callback result. The real callback is used to
         * unlock the latch waiting.
         */
        private final CameraCaptureCallback mComboCameraCaptureCallback =
                CameraCaptureCallbacks.createComboCallback(
                        mCameraCaptureCallback,
                        new CameraCaptureCallback() {
                            @Override
                            public void onCaptureCompleted(int captureConfigId,
                                    @NonNull CameraCaptureResult result) {
                                mCameraCaptureCallbackLatch.countDown();
                            }
                        });

        CaptureSessionTestParameters(String name, CameraCharacteristicsCompat characteristics) {
            mHandlerThread = new HandlerThread(name);
            mHandlerThread.start();
            mHandler = HandlerCompat.createAsync(mHandlerThread.getLooper());

            mImageReader =
                    ImageReader.newInstance(640, 480, ImageFormat.YUV_420_888, /*maxImages*/ 2);
            mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mHandler);

            SessionConfig.Builder builder = new SessionConfig.Builder();
            builder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
            mDeferrableSurface = new ImmediateSurface(mImageReader.getSurface());
            builder.addSurface(mDeferrableSurface);
            builder.addSessionStateCallback(mSessionStateCallback);
            builder.addRepeatingCameraCaptureCallback(mSessionCameraCaptureCallback);
            builder.addRepeatingCameraCaptureCallback(
                    CaptureCallbackContainer.create(mCamera2CaptureCallback));

            // Set capture request options
            // ==================================================================================
            // Priority | Component        | AF_MODE       | EV MODE            | AE_MODE
            // ----------------------------------------------------------------------------------
            // P1 | CaptureConfig          | AF_MODE_OFF  |                     |
            // ----------------------------------------------------------------------------------
            // P2 | SessionConfig          | AF_MODE_AUTO  | Max EV             | AE_MODE_ON
            // ==================================================================================

            mEvRange = characteristics != null
                    ? characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)
                    : new Range<>(0, 0);

            Camera2ImplConfig.Builder camera2ConfigBuilder = new Camera2ImplConfig.Builder();

            // Add capture request options for SessionConfig
            camera2ConfigBuilder
                    .setCaptureRequestOption(
                            CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
                    .setCaptureRequestOption(
                            CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, mEvRange.getUpper())
                    .setCaptureRequestOption(
                            CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);

            builder.addImplementationOptions(camera2ConfigBuilder.build());

            mSessionConfig = builder.build();

            CaptureConfig.Builder captureConfigBuilder = new CaptureConfig.Builder();
            captureConfigBuilder.setId(CAPTURE_CONFIG_ID);
            captureConfigBuilder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
            captureConfigBuilder.addSurface(mDeferrableSurface);
            captureConfigBuilder.addCameraCaptureCallback(mComboCameraCaptureCallback);

            // Add capture request options for CaptureConfig
            captureConfigBuilder.addImplementationOptions(new Camera2ImplConfig.Builder()
                    .setCaptureRequestOption(
                            CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
                    .build());

            mCaptureConfig = captureConfigBuilder.build();
        }

        /**
         * Wait for data to get produced by the session.
         *
         * @throws InterruptedException if data is not produced after a set amount of time
         */
        boolean waitForData() throws InterruptedException {
            return mDataLatch.await(TIME_TO_WAIT_FOR_DATA_SECONDS, TimeUnit.SECONDS);
        }

        boolean waitForCameraCaptureCallback() throws InterruptedException {
            return mCameraCaptureCallbackLatch.await(TIME_TO_WAIT_FOR_DATA_SECONDS,
                    TimeUnit.SECONDS);
        }

        /** Clean up resources. */
        void tearDown() {
            mDeferrableSurface.close();
            mImageReader.close();
            mHandlerThread.quitSafely();
        }
    }
}
