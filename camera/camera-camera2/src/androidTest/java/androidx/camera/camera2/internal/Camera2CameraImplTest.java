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

import static androidx.camera.camera2.internal.Camera2CameraImplTest.TestUseCase.SurfaceOption;
import static androidx.camera.camera2.internal.Camera2CameraImplTest.TestUseCase.SurfaceOption.NON_REPEATING;
import static androidx.camera.camera2.internal.Camera2CameraImplTest.TestUseCase.SurfaceOption.REPEATING;
import static androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA;
import static androidx.camera.core.concurrent.CameraCoordinator.CAMERA_OPERATING_MODE_CONCURRENT;
import static androidx.camera.core.concurrent.CameraCoordinator.CAMERA_OPERATING_MODE_SINGLE;
import static androidx.camera.core.resolutionselector.ResolutionSelector.ALLOWED_RESOLUTIONS_SLOW;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.truth.Truth.assertThat;

import static junit.framework.TestCase.assertTrue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.camera2.internal.compat.CameraManagerCompat;
import androidx.camera.camera2.internal.util.SemaphoreReleasingCamera2Callbacks;
import androidx.camera.camera2.interop.Camera2Interop;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.UseCase;
import androidx.camera.core.impl.CameraCaptureCallback;
import androidx.camera.core.impl.CameraCaptureResult;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.CameraStateRegistry;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.ImmediateSurface;
import androidx.camera.core.impl.Observable;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.StreamSpec;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.HandlerUtil;
import androidx.camera.testing.fakes.FakeCameraCoordinator;
import androidx.camera.testing.fakes.FakeUseCase;
import androidx.camera.testing.fakes.FakeUseCaseConfig;
import androidx.camera.testing.mocks.MockObserver;
import androidx.camera.testing.mocks.helpers.CallTimes;
import androidx.camera.testing.mocks.helpers.CallTimesAtLeast;
import androidx.core.os.HandlerCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Contains tests for {@link androidx.camera.camera2.internal.Camera2CameraImpl} internal
 * implementation.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 21)
public final class Camera2CameraImplTest {
    @CameraSelector.LensFacing
    private static final int DEFAULT_LENS_FACING = CameraSelector.LENS_FACING_BACK;
    @CameraSelector.LensFacing
    private static final int DEFAULT_PAIRED_CAMERA_LENS_FACING = CameraSelector.LENS_FACING_FRONT;
    // For the purpose of this test, always say we have 1 camera available.
    private static final int DEFAULT_AVAILABLE_CAMERA_COUNT = 1;
    private static final int DEFAULT_TEMPLATE_TYPE = CameraDevice.TEMPLATE_PREVIEW;
    private static final Map<Integer, Boolean> DEFAULT_TEMPLATE_TO_ZSL_DISABLED = new HashMap<>();

    static {
        DEFAULT_TEMPLATE_TO_ZSL_DISABLED.put(CameraDevice.TEMPLATE_PREVIEW, false);
        DEFAULT_TEMPLATE_TO_ZSL_DISABLED.put(CameraDevice.TEMPLATE_RECORD, true);
        DEFAULT_TEMPLATE_TO_ZSL_DISABLED.put(CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG, false);
    }

    private static final SurfaceOption DEFAULT_SURFACE_OPTION = REPEATING;

    private static final Set<CameraInternal.State> STABLE_STATES = new HashSet<>(asList(
            CameraInternal.State.CLOSED,
            CameraInternal.State.OPEN,
            CameraInternal.State.RELEASED));

    static ExecutorService sCameraExecutor;

    @Rule
    public TestRule mCameraRule = CameraUtil.grantCameraPermissionAndPreTest(
            new CameraUtil.PreTestCameraIdList(Camera2Config.defaultConfig())
    );

    private final ArrayList<FakeUseCase> mFakeUseCases = new ArrayList<>();
    private Camera2CameraImpl mCamera2CameraImpl;
    private static HandlerThread sCameraHandlerThread;
    private static Handler sCameraHandler;
    private FakeCameraCoordinator mCameraCoordinator;
    private CameraStateRegistry mCameraStateRegistry;
    Semaphore mSemaphore;
    OnImageAvailableListener mMockOnImageAvailableListener;
    CameraCaptureCallback mMockRepeatingCaptureCallback;
    String mCameraId;
    String mPairedCameraId;
    SemaphoreReleasingCamera2Callbacks.SessionStateCallback mSessionStateCallback;

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
        mMockOnImageAvailableListener = Mockito.mock(ImageReader.OnImageAvailableListener.class);
        mMockRepeatingCaptureCallback = Mockito.mock(CameraCaptureCallback.class);
        mSessionStateCallback = new SemaphoreReleasingCamera2Callbacks.SessionStateCallback();
        mCameraId = CameraUtil.getCameraIdWithLensFacing(DEFAULT_LENS_FACING);
        mPairedCameraId = CameraUtil.getCameraIdWithLensFacing(DEFAULT_PAIRED_CAMERA_LENS_FACING);
        mSemaphore = new Semaphore(0);
        mCameraCoordinator = new FakeCameraCoordinator();
        mCameraStateRegistry = new CameraStateRegistry(mCameraCoordinator,
                DEFAULT_AVAILABLE_CAMERA_COUNT);
        CameraManagerCompat cameraManagerCompat =
                CameraManagerCompat.from((Context) ApplicationProvider.getApplicationContext());

        Camera2CameraInfoImpl camera2CameraInfo = new Camera2CameraInfoImpl(
                mCameraId, cameraManagerCompat);
        mCamera2CameraImpl = new Camera2CameraImpl(
                cameraManagerCompat, mCameraId, camera2CameraInfo, mCameraCoordinator,
                mCameraStateRegistry, sCameraExecutor, sCameraHandler,
                DisplayInfoManager.getInstance(ApplicationProvider.getApplicationContext())
        );
    }

    @After
    public void teardown() throws InterruptedException, ExecutionException {
        for (FakeUseCase fakeUseCase : mFakeUseCases) {
            fakeUseCase.unbindFromCamera(mCamera2CameraImpl);
            fakeUseCase.onUnbind();
        }
        // Need to release the camera no matter what is done, otherwise the CameraDevice is not
        // closed.
        // When the CameraDevice is not closed, then it can cause problems with interferes with
        // other test cases.
        if (mCamera2CameraImpl != null) {
            ListenableFuture<Void> cameraReleaseFuture = mCamera2CameraImpl.release();

            // Wait for camera to be fully closed
            cameraReleaseFuture.get();

            mCamera2CameraImpl = null;
        }
    }

    @Test
    public void attachUseCase() {
        mCamera2CameraImpl.open();

        UseCase useCase = createUseCase();
        mCamera2CameraImpl.attachUseCases(singletonList(useCase));

        verify(mMockOnImageAvailableListener, never()).onImageAvailable(any(ImageReader.class));

        mCamera2CameraImpl.detachUseCases(singletonList(useCase));
        mCamera2CameraImpl.release();
    }

    @Test
    public void activeUseCase() {
        mCamera2CameraImpl.open();
        mCamera2CameraImpl.onUseCaseActive(createUseCase());

        verify(mMockOnImageAvailableListener, never()).onImageAvailable(any(ImageReader.class));

        mCamera2CameraImpl.release();
    }

    @Test
    public void attachAndActiveUseCase() {
        UseCase useCase1 = createUseCase();
        mCamera2CameraImpl.attachUseCases(singletonList(useCase1));
        mCamera2CameraImpl.onUseCaseActive(useCase1);

        verify(mMockOnImageAvailableListener, timeout(4000).atLeastOnce())
                .onImageAvailable(any(ImageReader.class));

        mCamera2CameraImpl.detachUseCases(singletonList(useCase1));
    }

    @Test
    public void detachUseCase() {
        UseCase useCase1 = createUseCase();
        mCamera2CameraImpl.attachUseCases(singletonList(useCase1));

        mCamera2CameraImpl.detachUseCases(singletonList(useCase1));
        mCamera2CameraImpl.onUseCaseActive(useCase1);

        verify(mMockOnImageAvailableListener, never()).onImageAvailable(any(ImageReader.class));

        assertThat(mCamera2CameraImpl.getCameraControlInternal()
                .isZslDisabledByByUserCaseConfig()).isFalse();
    }

    @Test
    public void unopenedCamera() {
        UseCase useCase1 = createUseCase();
        mCamera2CameraImpl.attachUseCases(singletonList(useCase1));
        mCamera2CameraImpl.detachUseCases(singletonList(useCase1));

        verify(mMockOnImageAvailableListener, never()).onImageAvailable(any(ImageReader.class));
    }

    @Test
    public void closedCamera() {
        UseCase useCase1 = createUseCase();
        mCamera2CameraImpl.attachUseCases(singletonList(useCase1));
        mCamera2CameraImpl.detachUseCases(singletonList(useCase1));

        verify(mMockOnImageAvailableListener, never()).onImageAvailable(any(ImageReader.class));
    }

    @Test
    public void releaseUnopenedCamera() {
        UseCase useCase1 = createUseCase();
        // Checks that if a camera has been released then calling open() will no longer open it.
        mCamera2CameraImpl.release();
        mCamera2CameraImpl.open();

        mCamera2CameraImpl.attachUseCases(singletonList(useCase1));
        mCamera2CameraImpl.onUseCaseActive(useCase1);

        verify(mMockOnImageAvailableListener, never()).onImageAvailable(any(ImageReader.class));

        mCamera2CameraImpl.detachUseCases(singletonList(useCase1));
    }

    @Test
    public void releasedOpenedCamera() {
        UseCase useCase1 = createUseCase();
        mCamera2CameraImpl.open();
        mCamera2CameraImpl.release();

        mCamera2CameraImpl.attachUseCases(singletonList(useCase1));
        mCamera2CameraImpl.onUseCaseActive(useCase1);

        verify(mMockOnImageAvailableListener, never()).onImageAvailable(any(ImageReader.class));

        mCamera2CameraImpl.detachUseCases(singletonList(useCase1));
    }

    @Test
    public void attach_oneUseCase_isAttached() {
        UseCase useCase1 = createUseCase();
        mCamera2CameraImpl.attachUseCases(singletonList(useCase1));

        assertThat(mCamera2CameraImpl.isUseCaseAttached(useCase1)).isTrue();

        mCamera2CameraImpl.detachUseCases(singletonList(useCase1));
    }

    @Test
    public void attach_sameUseCases_staysAttached() {
        UseCase useCase1 = createUseCase();
        mCamera2CameraImpl.attachUseCases(singletonList(useCase1));
        boolean attachedAfterFirstAdd = mCamera2CameraImpl.isUseCaseAttached(useCase1);

        mCamera2CameraImpl.attachUseCases(singletonList(useCase1));

        assertThat(attachedAfterFirstAdd).isTrue();
        assertThat(mCamera2CameraImpl.isUseCaseAttached(useCase1)).isTrue();

        mCamera2CameraImpl.detachUseCases(singletonList(useCase1));
    }

    @Test
    public void attach_twoUseCases_bothBecomeAttached() {
        UseCase useCase1 = createUseCase();
        UseCase useCase2 = createUseCase();
        mCamera2CameraImpl.attachUseCases(asList(useCase1, useCase2));

        assertThat(mCamera2CameraImpl.isUseCaseAttached(useCase1)).isTrue();
        assertThat(mCamera2CameraImpl.isUseCaseAttached(useCase2)).isTrue();

        mCamera2CameraImpl.detachUseCases(asList(useCase1, useCase2));
    }

    @Test
    public void detach_detachedUseCase_staysDetached() {
        UseCase useCase1 = createUseCase();
        mCamera2CameraImpl.detachUseCases(singletonList(useCase1));

        assertThat(mCamera2CameraImpl.isUseCaseAttached(useCase1)).isFalse();
    }

    @Test
    public void detachOneAttachedUseCase_fromAttachedUseCases_onlyDetachedSingleUseCase() {
        UseCase useCase1 = createUseCase();
        UseCase useCase2 = createUseCase();
        mCamera2CameraImpl.attachUseCases(asList(useCase1, useCase2));

        boolean useCase1isAttachedAfterFirstAdd = mCamera2CameraImpl.isUseCaseAttached(useCase1);
        boolean useCase2isAttachedAfterFirstAdd = mCamera2CameraImpl.isUseCaseAttached(useCase2);

        mCamera2CameraImpl.detachUseCases(singletonList(useCase1));

        assertThat(useCase1isAttachedAfterFirstAdd).isTrue();
        assertThat(useCase2isAttachedAfterFirstAdd).isTrue();
        assertThat(mCamera2CameraImpl.isUseCaseAttached(useCase1)).isFalse();
        assertThat(mCamera2CameraImpl.isUseCaseAttached(useCase2)).isTrue();

        mCamera2CameraImpl.detachUseCases(singletonList(useCase2));
    }

    @Test
    public void detachSameAttachedUseCaseTwice_onlyDetachesSameUseCase() {
        UseCase useCase1 = createUseCase();
        UseCase useCase2 = createUseCase();
        mCamera2CameraImpl.attachUseCases(asList(useCase1, useCase2));

        mCamera2CameraImpl.detachUseCases(singletonList(useCase1));
        mCamera2CameraImpl.detachUseCases(singletonList(useCase1));

        assertThat(mCamera2CameraImpl.isUseCaseAttached(useCase1)).isFalse();
        assertThat(mCamera2CameraImpl.isUseCaseAttached(useCase2)).isTrue();

        mCamera2CameraImpl.detachUseCases(singletonList(useCase2));
    }

    @Test
    public void attachUseCase_changeSurface_onUseCaseReset_correctAttachCount()
            throws ExecutionException, InterruptedException {
        blockHandler();

        UseCase useCase1 = createUseCase();
        mCamera2CameraImpl.attachUseCases(singletonList(useCase1));
        DeferrableSurface surface1 = useCase1.getSessionConfig().getSurfaces().get(0);

        unblockHandler();
        HandlerUtil.waitForLooperToIdle(sCameraHandler);

        changeUseCaseSurface(useCase1);
        mCamera2CameraImpl.onUseCaseReset(useCase1);
        DeferrableSurface surface2 = useCase1.getSessionConfig().getSurfaces().get(0);

        // Wait for camera to be released to ensure it has finished closing
        ListenableFuture<Void> releaseFuture = mCamera2CameraImpl.release();
        releaseFuture.get();

        assertThat(surface1).isNotEqualTo(surface2);

        // Old surface is decremented when CameraCaptureSession is closed by new
        // CameraCaptureSession.
        assertThat(surface1.getUseCount()).isEqualTo(0);
        // New surface is decremented when CameraCaptureSession is closed by
        // mCamera2CameraImpl.release()
        assertThat(surface2.getUseCount()).isEqualTo(0);
        mCamera2CameraImpl.detachUseCases(singletonList(useCase1));
    }

    @Test
    public void pendingSingleRequestRunSuccessfully_whenAnotherUseCaseAttached()
            throws InterruptedException {

        // Block camera thread to queue all the camera operations.
        blockHandler();

        UseCase useCase1 = createUseCase();
        mCamera2CameraImpl.attachUseCases(singletonList(useCase1));

        CameraCaptureCallback captureCallback = mock(CameraCaptureCallback.class);
        CaptureConfig.Builder captureConfigBuilder = new CaptureConfig.Builder();
        captureConfigBuilder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
        captureConfigBuilder.addSurface(useCase1.getSessionConfig().getSurfaces().get(0));
        captureConfigBuilder.addCameraCaptureCallback(captureCallback);

        mCamera2CameraImpl.getCameraControlInternal().submitStillCaptureRequests(
                singletonList(captureConfigBuilder.build()),
                ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
                ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH);

        UseCase useCase2 = createUseCase();
        mCamera2CameraImpl.attachUseCases(singletonList(useCase2));

        // Unblock camera handler to make camera operation run quickly .
        // To make the single request not able to run in 1st capture session.  and verify if it can
        // be carried over to the new capture session and run successfully.
        unblockHandler();
        HandlerUtil.waitForLooperToIdle(sCameraHandler);

        // CameraCaptureCallback.onCaptureCompleted() should be called to signal a capture attempt.
        verify(captureCallback, timeout(3000).times(1))
                .onCaptureCompleted(any(CameraCaptureResult.class));

        mCamera2CameraImpl.detachUseCases(asList(useCase1, useCase2));
    }

    @Test
    public void pendingSingleRequestSkipped_whenTheUseCaseIsRemoved()
            throws InterruptedException {

        // Block camera thread to queue all the camera operations.
        blockHandler();

        UseCase useCase1 = createUseCase();
        UseCase useCase2 = createUseCase();

        mCamera2CameraImpl.attachUseCases(asList(useCase1, useCase2));

        CameraCaptureCallback captureCallback = mock(CameraCaptureCallback.class);
        CaptureConfig.Builder captureConfigBuilder = new CaptureConfig.Builder();
        captureConfigBuilder.setTemplateType(DEFAULT_TEMPLATE_TYPE);
        captureConfigBuilder.addSurface(useCase1.getSessionConfig().getSurfaces().get(0));
        captureConfigBuilder.addCameraCaptureCallback(captureCallback);

        mCamera2CameraImpl.getCameraControlInternal().submitStillCaptureRequests(
                singletonList(captureConfigBuilder.build()),
                ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
                ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH);
        mCamera2CameraImpl.detachUseCases(singletonList(useCase1));

        // Unblock camera handle to make camera operation run quickly .
        // To make the single request not able to run in 1st capture session.  and verify if it can
        // be carried to the new capture session and run successfully.
        unblockHandler();
        HandlerUtil.waitForLooperToIdle(sCameraHandler);

        // TODO: b/133710422 should provide a way to detect if request is cancelled.
        Thread.sleep(1000);

        // CameraCaptureCallback.onCaptureCompleted() is not called and there is no crash.
        verify(captureCallback, times(0))
                .onCaptureCompleted(any(CameraCaptureResult.class));

        mCamera2CameraImpl.detachUseCases(singletonList(useCase2));
    }

    @Test
    public void attachRepeatingUseCase_meteringRepeatingIsNotAttached() {
        UseCase repeating = createUseCase(REPEATING);

        mCamera2CameraImpl.attachUseCases(singletonList(repeating));

        assertThat(mCamera2CameraImpl.isMeteringRepeatingAttached()).isFalse();

        mCamera2CameraImpl.detachUseCases(singletonList(repeating));
    }

    @Test
    public void attachNonRepeatingUseCase_meteringRepeatingIsAttached() {
        UseCase nonRepeating = createUseCase(NON_REPEATING);

        mCamera2CameraImpl.attachUseCases(singletonList(nonRepeating));

        assertThat(mCamera2CameraImpl.isMeteringRepeatingAttached()).isTrue();

        mCamera2CameraImpl.detachUseCases(singletonList(nonRepeating));
    }

    @Test
    public void attachRepeatingUseCaseLater_meteringRepeatingIsRemoved() {
        UseCase nonRepeating = createUseCase(NON_REPEATING);
        UseCase repeating = createUseCase(REPEATING);

        mCamera2CameraImpl.attachUseCases(singletonList(nonRepeating));

        assertThat(mCamera2CameraImpl.isMeteringRepeatingAttached()).isTrue();

        mCamera2CameraImpl.attachUseCases(singletonList(repeating));

        assertThat(mCamera2CameraImpl.isMeteringRepeatingAttached()).isFalse();

        mCamera2CameraImpl.detachUseCases(asList(nonRepeating, repeating));
    }

    @Test
    public void detachRepeatingUseCaseLater_meteringRepeatingIsAttached() {
        UseCase repeating = createUseCase(REPEATING);
        UseCase nonRepeating = createUseCase(NON_REPEATING);

        mCamera2CameraImpl.attachUseCases(asList(repeating, nonRepeating));

        assertThat(mCamera2CameraImpl.isMeteringRepeatingAttached()).isFalse();

        mCamera2CameraImpl.detachUseCases(singletonList(repeating));

        assertThat(mCamera2CameraImpl.isMeteringRepeatingAttached()).isTrue();

        mCamera2CameraImpl.detachUseCases(singletonList(nonRepeating));
    }

    @Test
    public void onUseCaseReset_toRepeating_meteringRepeatingIsAttached() {
        TestUseCase useCase = createUseCase(NON_REPEATING);

        mCamera2CameraImpl.attachUseCases(singletonList(useCase));

        assertThat(mCamera2CameraImpl.isMeteringRepeatingAttached()).isTrue();

        useCase.setSurfaceOption(REPEATING);
        useCase.notifyResetForTesting();

        assertThat(mCamera2CameraImpl.isMeteringRepeatingAttached()).isFalse();

        mCamera2CameraImpl.detachUseCases(singletonList(useCase));
    }

    @Test
    public void onUseCaseReset_toNonRepeating_meteringRepeatingIsAttached() {
        TestUseCase useCase = createUseCase(REPEATING);

        mCamera2CameraImpl.attachUseCases(singletonList(useCase));

        assertThat(mCamera2CameraImpl.isMeteringRepeatingAttached()).isFalse();

        useCase.setSurfaceOption(NON_REPEATING);
        useCase.notifyResetForTesting();

        assertThat(mCamera2CameraImpl.isMeteringRepeatingAttached()).isTrue();

        mCamera2CameraImpl.detachUseCases(singletonList(useCase));
    }

    @Test
    public void cameraStateIsClosed_afterInitialization()
            throws ExecutionException, InterruptedException {
        Observable<CameraInternal.State> state = mCamera2CameraImpl.getCameraState();
        CameraInternal.State currentState = state.fetchData().get();
        assertThat(currentState).isEqualTo(CameraInternal.State.CLOSED);
    }

    @Test
    public void cameraStateTransitionTest() throws InterruptedException {

        final AtomicReference<CameraInternal.State> lastStableState = new AtomicReference<>(null);
        Observable.Observer<CameraInternal.State> observer =
                new Observable.Observer<CameraInternal.State>() {
                    @Override
                    public void onNewData(@Nullable CameraInternal.State value) {
                        // Ignore any transient states.
                        if (STABLE_STATES.contains(value)) {
                            lastStableState.set(value);
                            mSemaphore.release();
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable t) { /* Ignore any transient errors. */ }
                };

        List<CameraInternal.State> observedStates = new ArrayList<>();
        mCamera2CameraImpl.getCameraState().addObserver(CameraXExecutors.directExecutor(),
                observer);

        // Wait for initial CLOSED state
        mSemaphore.acquire();
        observedStates.add(lastStableState.get());

        // Wait for OPEN state
        mCamera2CameraImpl.open();
        mSemaphore.acquire();
        observedStates.add(lastStableState.get());

        // Wait for CLOSED state again
        mCamera2CameraImpl.close();
        mSemaphore.acquire();
        observedStates.add(lastStableState.get());

        // Wait for RELEASED state
        mCamera2CameraImpl.release();
        mSemaphore.acquire();
        observedStates.add(lastStableState.get());

        mCamera2CameraImpl.getCameraState().removeObserver(observer);

        assertThat(observedStates).containsExactly(
                CameraInternal.State.CLOSED,
                CameraInternal.State.OPEN,
                CameraInternal.State.CLOSED,
                CameraInternal.State.RELEASED);
    }

    @Test
    public void cameraTransitionsThroughPendingState_whenNoCamerasAvailable() {
        MockObserver<CameraInternal.State> mockObserver = new MockObserver<>();

        // Ensure real camera can't open due to max cameras being open
        Camera mockCamera = mock(Camera.class);
        mCameraStateRegistry.registerCamera(
                mockCamera,
                CameraXExecutors.directExecutor(),
                () -> {
                },
                () -> {
                });
        mCameraStateRegistry.tryOpenCamera(mockCamera);

        mCamera2CameraImpl.getCameraState().addObserver(CameraXExecutors.directExecutor(),
                mockObserver);

        mCamera2CameraImpl.open();

        // Ensure that the camera gets to a PENDING_OPEN state
        mockObserver.verifyOnNewDataCall(CameraInternal.State.PENDING_OPEN, 3000,
                new CallTimesAtLeast(1));

        // Allow camera to be opened
        mCameraStateRegistry.markCameraState(mockCamera, CameraInternal.State.CLOSED);

        mockObserver.verifyOnNewDataCall(CameraInternal.State.OPEN, 3000);

        mCamera2CameraImpl.getCameraState().removeObserver(mockObserver);
    }

    @Test
    public void cameraStateIsReleased_afterRelease()
            throws ExecutionException, InterruptedException {
        Observable<CameraInternal.State> state = mCamera2CameraImpl.getCameraState();

        // Wait for camera to release
        mCamera2CameraImpl.release().get();
        CameraInternal.State currentState = state.fetchData().get();

        assertThat(currentState).isEqualTo(CameraInternal.State.RELEASED);
    }

    @Test
    public void openNewCaptureSessionImmediateBeforePreviousCaptureSessionClosed()
            throws InterruptedException {
        mCamera2CameraImpl.open();
        UseCase useCase1 = createUseCase();
        mCamera2CameraImpl.attachUseCases(singletonList(useCase1));
        mCamera2CameraImpl.onUseCaseActive(useCase1);

        // Wait a little bit for the camera to open.
        assertTrue(mSessionStateCallback.waitForOnConfigured(1));

        // Remove the useCase1 and trigger the CaptureSession#close().
        mCamera2CameraImpl.detachUseCases(singletonList(useCase1));

        // Create the secondary use case immediately and open it before the first use case closed.
        UseCase useCase2 = createUseCase();
        mCamera2CameraImpl.attachUseCases(singletonList(useCase2));
        mCamera2CameraImpl.onUseCaseActive(useCase2);
        // Wait for the secondary capture session is configured.
        assertTrue(mSessionStateCallback.waitForOnConfigured(1));

        MockObserver<CameraInternal.State> mockObserver = new MockObserver<>();

        mCamera2CameraImpl.getCameraState().addObserver(CameraXExecutors.directExecutor(),
                mockObserver);
        mCamera2CameraImpl.detachUseCases(singletonList(useCase2));
        mCamera2CameraImpl.close();

        // Wait for the CLOSED state. If the test fail, the CameraX might in wrong internal state,
        // and the Camera2CameraImpl#release() might stuck.
        mockObserver.verifyOnNewDataCall(CameraInternal.State.CLOSED, 4000,
                new CallTimes(1));
    }

    @Test
    public void closeCaptureSessionImmediateAfterCreateCaptureSession()
            throws InterruptedException {
        mCamera2CameraImpl.open();
        // Create another use case to keep the camera open.
        UseCase useCaseDummy = createUseCase();
        UseCase useCase = createUseCase();
        mCamera2CameraImpl.attachUseCases(asList(useCase, useCaseDummy));
        mCamera2CameraImpl.onUseCaseActive(useCase);

        // Wait a little bit for the camera to open.
        assertTrue(mSessionStateCallback.waitForOnConfigured(2));

        // Remove the useCase and trigger the CaptureSession#close().
        mCamera2CameraImpl.detachUseCases(singletonList(useCase));
        assertTrue(mSessionStateCallback.waitForOnClosed(2));
    }

    // Blocks the camera thread handler.
    private void blockHandler() {
        sCameraHandler.post(() -> {
            try {
                mSemaphore.acquire();
            } catch (InterruptedException e) {
                // Do nothing.
            }
        });
    }

    // unblock camera thread handler
    private void unblockHandler() {
        mSemaphore.release();
    }

    @NonNull
    private TestUseCase createUseCase() {
        return createUseCase(DEFAULT_TEMPLATE_TYPE);
    }

    @NonNull
    private TestUseCase createUseCase(int template) {
        return createUseCase(template, DEFAULT_SURFACE_OPTION);
    }

    @NonNull
    private TestUseCase createUseCase(@NonNull SurfaceOption surfaceOption) {
        return createUseCase(DEFAULT_TEMPLATE_TYPE, surfaceOption);
    }

    @NonNull
    private TestUseCase createUseCase(int template, @NonNull SurfaceOption surfaceOption) {
        boolean isZslDisabled = getDefaultZslDisabled(template);
        FakeUseCaseConfig.Builder configBuilder =
                new FakeUseCaseConfig.Builder().setSessionOptionUnpacker(
                                new Camera2SessionOptionUnpacker()).setTargetName("UseCase")
                        .setZslDisabled(isZslDisabled);
        new Camera2Interop.Extender<>(configBuilder).setSessionStateCallback(mSessionStateCallback);
        return createUseCase(configBuilder.getUseCaseConfig(), template, surfaceOption);
    }

    @NonNull
    private TestUseCase createUseCase(@NonNull FakeUseCaseConfig config, int template,
            @NonNull SurfaceOption surfaceOption) {
        TestUseCase testUseCase = new TestUseCase(
                template,
                config,
                mCamera2CameraImpl,
                mMockOnImageAvailableListener,
                mMockRepeatingCaptureCallback,
                surfaceOption
        );

        testUseCase.updateSuggestedStreamSpec(StreamSpec.builder(new Size(640, 480)).build());
        mFakeUseCases.add(testUseCase);
        return testUseCase;
    }

    @Test
    public void useCaseOnStateAttached_isCalled() throws InterruptedException {
        TestUseCase useCase1 = spy((TestUseCase) createUseCase());
        TestUseCase useCase2 = spy((TestUseCase) createUseCase());

        mCamera2CameraImpl.attachUseCases(singletonList(useCase1));
        mCamera2CameraImpl.attachUseCases(asList(useCase1, useCase2));

        HandlerUtil.waitForLooperToIdle(sCameraHandler);

        Handler uiThreadHandler = new Handler(Looper.getMainLooper());
        HandlerUtil.waitForLooperToIdle(uiThreadHandler);

        verify(useCase1, times(1)).onStateAttached();
        verify(useCase2, times(1)).onStateAttached();

        mCamera2CameraImpl.detachUseCases(asList(useCase1, useCase2));
    }

    @Test
    public void useCaseOnStateDetached_isCalled() throws InterruptedException {
        TestUseCase useCase1 = spy((TestUseCase) createUseCase());
        TestUseCase useCase2 = spy((TestUseCase) createUseCase());
        TestUseCase useCase3 = spy((TestUseCase) createUseCase());

        mCamera2CameraImpl.attachUseCases(asList(useCase1, useCase2));

        mCamera2CameraImpl.detachUseCases(asList(useCase1, useCase2, useCase3));

        HandlerUtil.waitForLooperToIdle(sCameraHandler);

        Handler uiThreadHandler = new Handler(Looper.getMainLooper());
        HandlerUtil.waitForLooperToIdle(uiThreadHandler);

        verify(useCase1, times(1)).onStateDetached();
        verify(useCase2, times(1)).onStateDetached();
        verify(useCase3, times(0)).onStateDetached();
    }

    private boolean isCameraControlActive(Camera2CameraControlImpl camera2CameraControlImpl) {
        ListenableFuture<Void> listenableFuture = camera2CameraControlImpl.setZoomRatio(2.0f);
        try {
            // setZoom() will fail immediately when CameraControl is not active.
            listenableFuture.get(50, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof CameraControl.OperationCanceledException) {
                return false;
            }
        } catch (InterruptedException | TimeoutException e) {
            // Do nothing.
        }
        return true;
    }

    @Test
    public void activateCameraControl_whenExistsAttachedUseCases() throws InterruptedException {
        Camera2CameraControlImpl camera2CameraControlImpl =
                (Camera2CameraControlImpl) mCamera2CameraImpl.getCameraControlInternal();

        assertThat(isCameraControlActive(camera2CameraControlImpl)).isFalse();

        UseCase useCase1 = createUseCase();

        mCamera2CameraImpl.attachUseCases(singletonList(useCase1));
        HandlerUtil.waitForLooperToIdle(sCameraHandler);

        assertThat(isCameraControlActive(camera2CameraControlImpl)).isTrue();

        mCamera2CameraImpl.detachUseCases(singletonList(useCase1));
    }

    @Test
    public void deactivateCameraControl_whenNoAttachedUseCases() throws InterruptedException {
        Camera2CameraControlImpl camera2CameraControlImpl =
                (Camera2CameraControlImpl) mCamera2CameraImpl.getCameraControlInternal();
        UseCase useCase1 = createUseCase();

        mCamera2CameraImpl.attachUseCases(singletonList(useCase1));
        HandlerUtil.waitForLooperToIdle(sCameraHandler);
        assertThat(isCameraControlActive(camera2CameraControlImpl)).isTrue();

        mCamera2CameraImpl.detachUseCases(singletonList(useCase1));
        HandlerUtil.waitForLooperToIdle(sCameraHandler);

        assertThat(isCameraControlActive(camera2CameraControlImpl)).isFalse();
    }

    @Test
    public void attachUseCaseWithTemplatePreview() throws InterruptedException {
        UseCase preview = createUseCase(CameraDevice.TEMPLATE_PREVIEW);

        mCamera2CameraImpl.attachUseCases(singletonList(preview));
        mCamera2CameraImpl.onUseCaseActive(preview);
        HandlerUtil.waitForLooperToIdle(sCameraHandler);

        ArgumentCaptor<CameraCaptureResult> captor =
                ArgumentCaptor.forClass(CameraCaptureResult.class);
        verify(mMockRepeatingCaptureCallback, timeout(4000).atLeastOnce())
                .onCaptureCompleted(captor.capture());

        CaptureResult captureResult =
                ((Camera2CameraCaptureResult) captor.getValue()).getCaptureResult();

        assertThat(captureResult.get(CaptureResult.CONTROL_CAPTURE_INTENT))
                .isEqualTo(CaptureRequest.CONTROL_CAPTURE_INTENT_PREVIEW);

        mCamera2CameraImpl.detachUseCases(singletonList(preview));
    }

    @Test
    public void attachUseCaseWithTemplateRecord() throws InterruptedException {
        UseCase preview = createUseCase(CameraDevice.TEMPLATE_PREVIEW);
        UseCase record = createUseCase(CameraDevice.TEMPLATE_RECORD);

        mCamera2CameraImpl.attachUseCases(asList(preview, record));
        mCamera2CameraImpl.onUseCaseActive(preview);
        mCamera2CameraImpl.onUseCaseActive(record);
        HandlerUtil.waitForLooperToIdle(sCameraHandler);

        ArgumentCaptor<CameraCaptureResult> captor =
                ArgumentCaptor.forClass(CameraCaptureResult.class);
        verify(mMockRepeatingCaptureCallback, timeout(4000).atLeastOnce())
                .onCaptureCompleted(captor.capture());

        CaptureResult captureResult =
                ((Camera2CameraCaptureResult) captor.getValue()).getCaptureResult();

        assertThat(captureResult.get(CaptureResult.CONTROL_CAPTURE_INTENT))
                .isEqualTo(CaptureRequest.CONTROL_CAPTURE_INTENT_VIDEO_RECORD);

        mCamera2CameraImpl.detachUseCases(asList(preview, record));
    }

    @SdkSuppress(minSdkVersion = 23)
    @Test
    public void attachUseCaseWithTemplateZSLNoRecord() throws InterruptedException {
        if (!mCamera2CameraImpl.getCameraInfo().isZslSupported()) {
            return;
        }
        UseCase preview = createUseCase(CameraDevice.TEMPLATE_PREVIEW);
        UseCase zsl = createUseCase(CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG);

        mCamera2CameraImpl.attachUseCases(asList(preview, zsl));
        mCamera2CameraImpl.onUseCaseActive(preview);
        mCamera2CameraImpl.onUseCaseActive(zsl);
        HandlerUtil.waitForLooperToIdle(sCameraHandler);

        ArgumentCaptor<CameraCaptureResult> captor =
                ArgumentCaptor.forClass(CameraCaptureResult.class);
        verify(mMockRepeatingCaptureCallback, timeout(4000).atLeastOnce())
                .onCaptureCompleted(captor.capture());

        CaptureResult captureResult =
                ((Camera2CameraCaptureResult) captor.getValue()).getCaptureResult();

        assertThat(captureResult.get(CaptureResult.CONTROL_CAPTURE_INTENT))
                .isEqualTo(CaptureRequest.CONTROL_CAPTURE_INTENT_ZERO_SHUTTER_LAG);
        assertThat(
                mCamera2CameraImpl.getCameraControlInternal().isZslDisabledByByUserCaseConfig())
                .isFalse();

        mCamera2CameraImpl.detachUseCases(asList(preview, zsl));
        HandlerUtil.waitForLooperToIdle(sCameraHandler);
        assertThat(mCamera2CameraImpl.getCameraControlInternal()
                .isZslDisabledByByUserCaseConfig()).isFalse();
    }

    @SdkSuppress(minSdkVersion = 23)
    @Test
    public void attachUseCaseWithTemplateZSLHasRecord() throws InterruptedException {
        if (!mCamera2CameraImpl.getCameraInfo().isZslSupported()) {
            return;
        }
        UseCase preview = createUseCase(CameraDevice.TEMPLATE_PREVIEW);
        UseCase record = createUseCase(CameraDevice.TEMPLATE_RECORD);
        UseCase zsl = createUseCase(CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG);

        mCamera2CameraImpl.attachUseCases(asList(preview, record, zsl));
        mCamera2CameraImpl.onUseCaseActive(preview);
        mCamera2CameraImpl.onUseCaseActive(record);
        mCamera2CameraImpl.onUseCaseActive(zsl);
        HandlerUtil.waitForLooperToIdle(sCameraHandler);

        ArgumentCaptor<CameraCaptureResult> captor =
                ArgumentCaptor.forClass(CameraCaptureResult.class);
        verify(mMockRepeatingCaptureCallback, timeout(4000).atLeastOnce())
                .onCaptureCompleted(captor.capture());

        CaptureResult captureResult =
                ((Camera2CameraCaptureResult) captor.getValue()).getCaptureResult();

        assertThat(captureResult.get(CaptureResult.CONTROL_CAPTURE_INTENT))
                .isEqualTo(CaptureRequest.CONTROL_CAPTURE_INTENT_VIDEO_RECORD);
        assertThat(
                mCamera2CameraImpl.getCameraControlInternal().isZslDisabledByByUserCaseConfig())
                .isTrue();

        mCamera2CameraImpl.detachUseCases(asList(preview, record, zsl));
        HandlerUtil.waitForLooperToIdle(sCameraHandler);
        assertThat(
                mCamera2CameraImpl.getCameraControlInternal().isZslDisabledByByUserCaseConfig())
                .isFalse();
    }

    @SdkSuppress(minSdkVersion = 23)
    @Test
    public void attachAndDetachUseCasesMultipleTimes() throws InterruptedException {
        if (!mCamera2CameraImpl.getCameraInfo().isZslSupported()) {
            return;
        }
        UseCase preview = createUseCase(CameraDevice.TEMPLATE_PREVIEW);
        UseCase record = createUseCase(CameraDevice.TEMPLATE_RECORD);
        UseCase zsl = createUseCase(CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG);

        mCamera2CameraImpl.attachUseCases(asList(preview, zsl));
        mCamera2CameraImpl.onUseCaseActive(preview);
        mCamera2CameraImpl.onUseCaseActive(zsl);
        HandlerUtil.waitForLooperToIdle(sCameraHandler);

        assertThat(
                mCamera2CameraImpl.getCameraControlInternal().isZslDisabledByByUserCaseConfig())
                .isFalse();

        mCamera2CameraImpl.attachUseCases(singletonList(record));
        mCamera2CameraImpl.onUseCaseActive(record);
        HandlerUtil.waitForLooperToIdle(sCameraHandler);

        assertThat(
                mCamera2CameraImpl.getCameraControlInternal().isZslDisabledByByUserCaseConfig())
                .isTrue();

        mCamera2CameraImpl.detachUseCases(singletonList(record));
        HandlerUtil.waitForLooperToIdle(sCameraHandler);
        assertThat(
                mCamera2CameraImpl.getCameraControlInternal().isZslDisabledByByUserCaseConfig())
                .isFalse();

        mCamera2CameraImpl.attachUseCases(singletonList(record));
        mCamera2CameraImpl.onUseCaseActive(record);
        HandlerUtil.waitForLooperToIdle(sCameraHandler);
        assertThat(
                mCamera2CameraImpl.getCameraControlInternal().isZslDisabledByByUserCaseConfig())
                .isTrue();

        mCamera2CameraImpl.detachUseCases(singletonList(zsl));
        HandlerUtil.waitForLooperToIdle(sCameraHandler);
        assertThat(
                mCamera2CameraImpl.getCameraControlInternal().isZslDisabledByByUserCaseConfig())
                .isTrue();

        mCamera2CameraImpl.detachUseCases(singletonList(preview));
        HandlerUtil.waitForLooperToIdle(sCameraHandler);
        assertThat(
                mCamera2CameraImpl.getCameraControlInternal().isZslDisabledByByUserCaseConfig())
                .isTrue();

        mCamera2CameraImpl.detachUseCases(singletonList(record));
        HandlerUtil.waitForLooperToIdle(sCameraHandler);
        assertThat(
                mCamera2CameraImpl.getCameraControlInternal().isZslDisabledByByUserCaseConfig())
                .isFalse();
    }

    @SdkSuppress(minSdkVersion = 23)
    @Test
    public void zslDisabled_whenHighResolutionIsEnabled() throws InterruptedException {
        UseCase zsl = createUseCase(CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG);

        // Creates a test use case with high resolution enabled.
        ResolutionSelector highResolutionSelector =
                new ResolutionSelector.Builder().setAllowedResolutionMode(
                        ALLOWED_RESOLUTIONS_SLOW).build();
        FakeUseCaseConfig.Builder configBuilder =
                new FakeUseCaseConfig.Builder().setSessionOptionUnpacker(
                        new Camera2SessionOptionUnpacker()).setTargetName(
                        "UseCase").setResolutionSelector(highResolutionSelector);
        new Camera2Interop.Extender<>(configBuilder).setSessionStateCallback(mSessionStateCallback);
        UseCase highResolutionUseCase = createUseCase(configBuilder.getUseCaseConfig(),
                CameraDevice.TEMPLATE_PREVIEW, DEFAULT_SURFACE_OPTION);

        // Checks zsl is disabled after UseCase#onAttach() is called to merge/update config.
        assertThat(highResolutionUseCase.getCurrentConfig().isZslDisabled(false)).isTrue();

        if (!mCamera2CameraImpl.getCameraInfo().isZslSupported()) {
            return;
        }

        mCamera2CameraImpl.attachUseCases(asList(zsl, highResolutionUseCase));
        mCamera2CameraImpl.onUseCaseActive(zsl);
        mCamera2CameraImpl.onUseCaseActive(highResolutionUseCase);
        HandlerUtil.waitForLooperToIdle(sCameraHandler);
        assertThat(mCamera2CameraImpl.getCameraControlInternal().isZslDisabledByByUserCaseConfig())
                .isTrue();
    }

    @Test
    public void attachUseCaseWithTemplatePreviewInConcurrentMode() throws Exception {
        // Arrange.
        CameraManagerCompat cameraManagerCompat =
                CameraManagerCompat.from((Context) ApplicationProvider.getApplicationContext());

        PackageManager pm = ApplicationProvider.getApplicationContext().getPackageManager();
        if (mPairedCameraId != null
                && pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_CONCURRENT)) {
            Camera2CameraInfoImpl pairedCamera2CameraInfo = new Camera2CameraInfoImpl(
                    mPairedCameraId, cameraManagerCompat);
            Camera2CameraImpl pairedCamera2CameraImpl = new Camera2CameraImpl(
                    cameraManagerCompat, mPairedCameraId, pairedCamera2CameraInfo,
                    mCameraCoordinator,
                    mCameraStateRegistry, sCameraExecutor, sCameraHandler,
                    DisplayInfoManager.getInstance(ApplicationProvider.getApplicationContext()));
            mCameraCoordinator.addConcurrentCameraIdsAndCameraSelectors(
                    new HashMap<String, CameraSelector>() {{
                        put(mCameraId, DEFAULT_BACK_CAMERA);
                        put(mPairedCameraId, CameraSelector.DEFAULT_FRONT_CAMERA);
                    }});
            mCameraCoordinator.setCameraOperatingMode(CAMERA_OPERATING_MODE_CONCURRENT);
            mCameraStateRegistry.onCameraOperatingModeUpdated(
                    CAMERA_OPERATING_MODE_SINGLE, CAMERA_OPERATING_MODE_CONCURRENT);

            // Act.
            UseCase preview1 = createUseCase(CameraDevice.TEMPLATE_PREVIEW);
            mCamera2CameraImpl.attachUseCases(singletonList(preview1));
            mCamera2CameraImpl.onUseCaseActive(preview1);
            HandlerUtil.waitForLooperToIdle(sCameraHandler);

            // Assert.
            ArgumentCaptor<CameraCaptureResult> captor =
                    ArgumentCaptor.forClass(CameraCaptureResult.class);
            verify(mMockRepeatingCaptureCallback, never()).onCaptureCompleted(captor.capture());

            // Act.
            UseCase preview2 = createUseCase(CameraDevice.TEMPLATE_PREVIEW);
            pairedCamera2CameraImpl.attachUseCases(singletonList(preview2));
            pairedCamera2CameraImpl.onUseCaseActive(preview2);
            HandlerUtil.waitForLooperToIdle(sCameraHandler);

            // Assert.
            captor = ArgumentCaptor.forClass(CameraCaptureResult.class);
            verify(mMockRepeatingCaptureCallback, timeout(4000).atLeastOnce())
                    .onCaptureCompleted(captor.capture());
            CaptureResult captureResult = (captor.getValue()).getCaptureResult();
            assertThat(captureResult.get(CaptureResult.CONTROL_CAPTURE_INTENT))
                    .isEqualTo(CaptureRequest.CONTROL_CAPTURE_INTENT_PREVIEW);

            mCamera2CameraImpl.detachUseCases(singletonList(preview1));
        }
    }

    private void changeUseCaseSurface(UseCase useCase) {
        useCase.updateSuggestedStreamSpec(StreamSpec.builder(new Size(640, 480)).build());
    }

    private static boolean getDefaultZslDisabled(int templateType) {
        Boolean isZslDisabled = DEFAULT_TEMPLATE_TO_ZSL_DISABLED.get(templateType);
        checkState(isZslDisabled != null, "No default mapping from template to zsl disabled");
        return isZslDisabled;
    }

    public static class TestUseCase extends FakeUseCase {
        private final ImageReader.OnImageAvailableListener mImageAvailableListener;
        HandlerThread mHandlerThread = new HandlerThread("HandlerThread");
        Handler mHandler;
        FakeUseCaseConfig mConfig;
        private DeferrableSurface mDeferrableSurface;
        private SurfaceOption mSurfaceOption;
        private final CameraCaptureCallback mRepeatingCaptureCallback;
        private final int mTemplate;
        private SessionConfig.Builder mSessionConfigBuilder;

        @SuppressWarnings("NewClassNamingConvention")
        public enum SurfaceOption {
            /** UseCase will not add any surface in SessionConfig. */
            NO_SURFACE,
            /** UseCase will add a repeating surface in SessionConfig. */
            REPEATING,
            /** UseCase will add a non-repeating surface in SessionConfig. */
            NON_REPEATING,
        }

        TestUseCase(
                int template,
                @NonNull FakeUseCaseConfig config,
                @NonNull CameraInternal camera,
                @NonNull ImageReader.OnImageAvailableListener listener,
                @NonNull CameraCaptureCallback repeatingCaptureCallback,
                @NonNull SurfaceOption surfaceOption) {
            super(config);
            // Ensure we're using the combined configuration (user config + defaults)
            mConfig = (FakeUseCaseConfig) getCurrentConfig();
            mTemplate = template;
            mSurfaceOption = surfaceOption;

            mImageAvailableListener = listener;
            mRepeatingCaptureCallback = repeatingCaptureCallback;
            mHandlerThread.start();
            mHandler = new Handler(mHandlerThread.getLooper());
            bindToCamera(camera, null, null);
            updateSuggestedStreamSpec(StreamSpec.builder(new Size(640, 480)).build());
        }

        public void close() {
            mHandler.removeCallbacksAndMessages(null);
            mHandlerThread.quitSafely();
            if (mDeferrableSurface != null) {
                mDeferrableSurface.close();
                mDeferrableSurface = null;
            }
        }

        @Override
        public void onUnbind() {
            super.onUnbind();
            close();
        }

        @Override
        @NonNull
        protected StreamSpec onSuggestedStreamSpecUpdated(
                @NonNull StreamSpec suggestedStreamSpec) {
            if (mDeferrableSurface != null) {
                mDeferrableSurface.close();
            }
            mDeferrableSurface = createDeferrableSurface(suggestedStreamSpec);
            mSessionConfigBuilder = SessionConfig.Builder.createFrom(mConfig,
                    suggestedStreamSpec.getResolution());
            mSessionConfigBuilder.setTemplateType(mTemplate);
            mSessionConfigBuilder.addRepeatingCameraCaptureCallback(mRepeatingCaptureCallback);
            updateSessionBuilderBySurfaceOption();
            updateSessionConfig(mSessionConfigBuilder.build());
            return suggestedStreamSpec;
        }

        public void setSurfaceOption(@NonNull SurfaceOption surfaceOption) {
            if (mSurfaceOption != surfaceOption) {
                mSurfaceOption = surfaceOption;
                updateSessionBuilderBySurfaceOption();
                updateSessionConfig(mSessionConfigBuilder.build());
            }
        }

        private void updateSessionBuilderBySurfaceOption() {
            checkNotNull(mDeferrableSurface);
            mSessionConfigBuilder.clearSurfaces();
            switch (mSurfaceOption) {
                case NO_SURFACE:
                    break;
                case REPEATING:
                    mSessionConfigBuilder.addSurface(mDeferrableSurface);
                    break;
                case NON_REPEATING:
                    mSessionConfigBuilder.addNonRepeatingSurface(mDeferrableSurface);
                    break;
            }
        }

        @NonNull
        private DeferrableSurface createDeferrableSurface(@NonNull StreamSpec streamSpec) {
            Size suggestedResolution = streamSpec.getResolution();
            //noinspection resource
            ImageReader imageReader =
                    ImageReader.newInstance(
                            suggestedResolution.getWidth(),
                            suggestedResolution.getHeight(),
                            ImageFormat.YUV_420_888, /*maxImages*/
                            2);
            imageReader.setOnImageAvailableListener(mImageAvailableListener, mHandler);
            Surface surface = imageReader.getSurface();
            DeferrableSurface deferrableSurface = new ImmediateSurface(surface);
            deferrableSurface.getTerminationFuture().addListener(() -> {
                surface.release();
                imageReader.close();
            }, CameraXExecutors.directExecutor());
            return deferrableSurface;
        }
    }
}
