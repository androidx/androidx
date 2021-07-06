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

import static com.google.common.truth.Truth.assertThat;

import static junit.framework.TestCase.assertTrue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import android.content.Context;
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
import androidx.camera.camera2.internal.compat.CameraManagerCompat;
import androidx.camera.camera2.internal.util.SemaphoreReleasingCamera2Callbacks;
import androidx.camera.camera2.interop.Camera2Interop;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.InitializationException;
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
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.HandlerUtil;
import androidx.camera.testing.fakes.FakeCamera;
import androidx.camera.testing.fakes.FakeCameraInfoInternal;
import androidx.camera.testing.fakes.FakeUseCase;
import androidx.camera.testing.fakes.FakeUseCaseConfig;
import androidx.core.os.HandlerCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
public final class Camera2CameraImplTest {
    @CameraSelector.LensFacing
    private static final int DEFAULT_LENS_FACING = CameraSelector.LENS_FACING_BACK;
    // For the purpose of this test, always say we have 1 camera available.
    private static final int DEFAULT_AVAILABLE_CAMERA_COUNT = 1;
    private static final Set<CameraInternal.State> STABLE_STATES = new HashSet<>(Arrays.asList(
            CameraInternal.State.CLOSED,
            CameraInternal.State.OPEN,
            CameraInternal.State.RELEASED));

    static ExecutorService sCameraExecutor;

    @Rule
    public TestRule mCameraRule = CameraUtil.grantCameraPermissionAndPreTest();


    private ArrayList<FakeUseCase> mFakeUseCases = new ArrayList<>();
    private Camera2CameraImpl mCamera2CameraImpl;
    private static HandlerThread sCameraHandlerThread;
    private static Handler sCameraHandler;
    private CameraStateRegistry mCameraStateRegistry;
    Semaphore mSemaphore;
    OnImageAvailableListener mMockOnImageAvailableListener;
    CameraCaptureCallback mMockRepeatingCaptureCallback;
    String mCameraId;
    SemaphoreReleasingCamera2Callbacks.SessionStateCallback mSessionStateCallback;

    @BeforeClass
    public static void classSetup() throws InitializationException {
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
        mSemaphore = new Semaphore(0);
        mCameraStateRegistry = new CameraStateRegistry(DEFAULT_AVAILABLE_CAMERA_COUNT);
        CameraManagerCompat cameraManagerCompat =
                CameraManagerCompat.from((Context) ApplicationProvider.getApplicationContext());
        Camera2CameraInfoImpl camera2CameraInfo = new Camera2CameraInfoImpl(
                mCameraId, cameraManagerCompat);
        mCamera2CameraImpl = new Camera2CameraImpl(cameraManagerCompat, mCameraId,
                camera2CameraInfo,
                mCameraStateRegistry, sCameraExecutor, sCameraHandler);
    }

    @After
    public void teardown() throws InterruptedException, ExecutionException {
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

        for (FakeUseCase fakeUseCase : mFakeUseCases) {
            fakeUseCase.onDetached();
        }
    }

    @Test
    public void attachUseCase() {
        mCamera2CameraImpl.open();

        UseCase useCase = createUseCase();
        mCamera2CameraImpl.attachUseCases(Collections.singletonList(useCase));

        verify(mMockOnImageAvailableListener, never()).onImageAvailable(any(ImageReader.class));

        mCamera2CameraImpl.detachUseCases(Collections.singletonList(useCase));
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
        mCamera2CameraImpl.attachUseCases(Collections.singletonList(useCase1));
        mCamera2CameraImpl.onUseCaseActive(useCase1);

        verify(mMockOnImageAvailableListener, timeout(4000).atLeastOnce())
                .onImageAvailable(any(ImageReader.class));

        mCamera2CameraImpl.detachUseCases(Collections.singletonList(useCase1));
    }

    @Test
    public void detachUseCase() {
        UseCase useCase1 = createUseCase();
        mCamera2CameraImpl.attachUseCases(Collections.singletonList(useCase1));

        mCamera2CameraImpl.detachUseCases(Collections.singletonList(useCase1));
        mCamera2CameraImpl.onUseCaseActive(useCase1);

        verify(mMockOnImageAvailableListener, never()).onImageAvailable(any(ImageReader.class));
    }

    @Test
    public void unopenedCamera() {
        UseCase useCase1 = createUseCase();
        mCamera2CameraImpl.attachUseCases(Collections.singletonList(useCase1));
        mCamera2CameraImpl.detachUseCases(Collections.singletonList(useCase1));

        verify(mMockOnImageAvailableListener, never()).onImageAvailable(any(ImageReader.class));
    }

    @Test
    public void closedCamera() {
        UseCase useCase1 = createUseCase();
        mCamera2CameraImpl.attachUseCases(Collections.singletonList(useCase1));
        mCamera2CameraImpl.detachUseCases(Collections.singletonList(useCase1));

        verify(mMockOnImageAvailableListener, never()).onImageAvailable(any(ImageReader.class));
    }

    @Test
    public void releaseUnopenedCamera() {
        UseCase useCase1 = createUseCase();
        // Checks that if a camera has been released then calling open() will no longer open it.
        mCamera2CameraImpl.release();
        mCamera2CameraImpl.open();

        mCamera2CameraImpl.attachUseCases(Collections.singletonList(useCase1));
        mCamera2CameraImpl.onUseCaseActive(useCase1);

        verify(mMockOnImageAvailableListener, never()).onImageAvailable(any(ImageReader.class));

        mCamera2CameraImpl.detachUseCases(Collections.singletonList(useCase1));
    }

    @Test
    public void releasedOpenedCamera() {
        UseCase useCase1 = createUseCase();
        mCamera2CameraImpl.open();
        mCamera2CameraImpl.release();

        mCamera2CameraImpl.attachUseCases(Collections.singletonList(useCase1));
        mCamera2CameraImpl.onUseCaseActive(useCase1);

        verify(mMockOnImageAvailableListener, never()).onImageAvailable(any(ImageReader.class));

        mCamera2CameraImpl.detachUseCases(Collections.singletonList(useCase1));
    }

    @Test
    public void attach_oneUseCase_isAttached() {
        UseCase useCase1 = createUseCase();
        mCamera2CameraImpl.attachUseCases(Collections.singletonList(useCase1));

        assertThat(mCamera2CameraImpl.isUseCaseAttached(useCase1)).isTrue();

        mCamera2CameraImpl.detachUseCases(Collections.singletonList(useCase1));
    }

    @Test
    public void attach_sameUseCases_staysAttached() {
        UseCase useCase1 = createUseCase();
        mCamera2CameraImpl.attachUseCases(Collections.singletonList(useCase1));
        boolean attachedAfterFirstAdd = mCamera2CameraImpl.isUseCaseAttached(useCase1);

        mCamera2CameraImpl.attachUseCases(Collections.singletonList(useCase1));

        assertThat(attachedAfterFirstAdd).isTrue();
        assertThat(mCamera2CameraImpl.isUseCaseAttached(useCase1)).isTrue();

        mCamera2CameraImpl.detachUseCases(Collections.singletonList(useCase1));
    }

    @Test
    public void attach_twoUseCases_bothBecomeAttached() {
        UseCase useCase1 = createUseCase();
        UseCase useCase2 = createUseCase();
        mCamera2CameraImpl.attachUseCases(Arrays.asList(useCase1, useCase2));

        assertThat(mCamera2CameraImpl.isUseCaseAttached(useCase1)).isTrue();
        assertThat(mCamera2CameraImpl.isUseCaseAttached(useCase2)).isTrue();

        mCamera2CameraImpl.detachUseCases(Arrays.asList(useCase1, useCase2));
    }

    @Test
    public void detach_detachedUseCase_staysDetached() {
        UseCase useCase1 = createUseCase();
        mCamera2CameraImpl.detachUseCases(Collections.singletonList(useCase1));

        assertThat(mCamera2CameraImpl.isUseCaseAttached(useCase1)).isFalse();
    }

    @Test
    public void detachOneAttachedUseCase_fromAttachedUseCases_onlyDetachedSingleUseCase() {
        UseCase useCase1 = createUseCase();
        UseCase useCase2 = createUseCase();
        mCamera2CameraImpl.attachUseCases(Arrays.asList(useCase1, useCase2));

        boolean useCase1isAttachedAfterFirstAdd = mCamera2CameraImpl.isUseCaseAttached(useCase1);
        boolean useCase2isAttachedAfterFirstAdd = mCamera2CameraImpl.isUseCaseAttached(useCase2);

        mCamera2CameraImpl.detachUseCases(Collections.singletonList(useCase1));

        assertThat(useCase1isAttachedAfterFirstAdd).isTrue();
        assertThat(useCase2isAttachedAfterFirstAdd).isTrue();
        assertThat(mCamera2CameraImpl.isUseCaseAttached(useCase1)).isFalse();
        assertThat(mCamera2CameraImpl.isUseCaseAttached(useCase2)).isTrue();

        mCamera2CameraImpl.detachUseCases(Collections.singletonList(useCase2));
    }

    @Test
    public void detachSameAttachedUseCaseTwice_onlyDetachesSameUseCase() {
        UseCase useCase1 = createUseCase();
        UseCase useCase2 = createUseCase();
        mCamera2CameraImpl.attachUseCases(Arrays.asList(useCase1, useCase2));

        mCamera2CameraImpl.detachUseCases(Collections.singletonList(useCase1));
        mCamera2CameraImpl.detachUseCases(Collections.singletonList(useCase1));

        assertThat(mCamera2CameraImpl.isUseCaseAttached(useCase1)).isFalse();
        assertThat(mCamera2CameraImpl.isUseCaseAttached(useCase2)).isTrue();

        mCamera2CameraImpl.detachUseCases(Collections.singletonList(useCase2));
    }

    @Test
    public void attachUseCase_changeSurface_onUseCaseReset_correctAttachCount()
            throws ExecutionException, InterruptedException {
        blockHandler();

        UseCase useCase1 = createUseCase();
        mCamera2CameraImpl.attachUseCases(Arrays.asList(useCase1));
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

        // Old surface is decremented when CameraCaptueSession is closed by new
        // CameraCaptureSession.
        assertThat(surface1.getUseCount()).isEqualTo(0);
        // New surface is decremented when CameraCaptueSession is closed by
        // mCamera2CameraImpl.release()
        assertThat(surface2.getUseCount()).isEqualTo(0);
        mCamera2CameraImpl.detachUseCases(Arrays.asList(useCase1));
    }

    @Test
    public void pendingSingleRequestRunSuccessfully_whenAnotherUseCaseAttached()
            throws InterruptedException {

        // Block camera thread to queue all the camera operations.
        blockHandler();

        UseCase useCase1 = createUseCase();
        mCamera2CameraImpl.attachUseCases(Arrays.asList(useCase1));

        CameraCaptureCallback captureCallback = mock(CameraCaptureCallback.class);
        CaptureConfig.Builder captureConfigBuilder = new CaptureConfig.Builder();
        captureConfigBuilder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
        captureConfigBuilder.addSurface(useCase1.getSessionConfig().getSurfaces().get(0));
        captureConfigBuilder.addCameraCaptureCallback(captureCallback);

        mCamera2CameraImpl.getCameraControlInternal().submitCaptureRequests(
                Arrays.asList(captureConfigBuilder.build()));

        UseCase useCase2 = createUseCase();
        mCamera2CameraImpl.attachUseCases(Arrays.asList(useCase2));

        // Unblock camera handler to make camera operation run quickly .
        // To make the single request not able to run in 1st capture session.  and verify if it can
        // be carried over to the new capture session and run successfully.
        unblockHandler();
        HandlerUtil.waitForLooperToIdle(sCameraHandler);

        // CameraCaptureCallback.onCaptureCompleted() should be called to signal a capture attempt.
        verify(captureCallback, timeout(3000).times(1))
                .onCaptureCompleted(any(CameraCaptureResult.class));

        mCamera2CameraImpl.detachUseCases(Arrays.asList(useCase1, useCase2));
    }

    @Test
    public void pendingSingleRequestSkipped_whenTheUseCaseIsRemoved()
            throws InterruptedException {

        // Block camera thread to queue all the camera operations.
        blockHandler();

        UseCase useCase1 = createUseCase();
        UseCase useCase2 = createUseCase();

        mCamera2CameraImpl.attachUseCases(Arrays.asList(useCase1, useCase2));

        CameraCaptureCallback captureCallback = mock(CameraCaptureCallback.class);
        CaptureConfig.Builder captureConfigBuilder = new CaptureConfig.Builder();
        captureConfigBuilder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
        captureConfigBuilder.addSurface(useCase1.getSessionConfig().getSurfaces().get(0));
        captureConfigBuilder.addCameraCaptureCallback(captureCallback);

        mCamera2CameraImpl.getCameraControlInternal().submitCaptureRequests(
                Arrays.asList(captureConfigBuilder.build()));
        mCamera2CameraImpl.detachUseCases(Arrays.asList(useCase1));

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

        mCamera2CameraImpl.detachUseCases(Arrays.asList(useCase2));
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
        @SuppressWarnings("unchecked") // Cannot mock generic type inline
        Observable.Observer<CameraInternal.State> mockObserver =
                mock(Observable.Observer.class);

        // Ensure real camera can't open due to max cameras being open
        Camera mockCamera = mock(Camera.class);
        mCameraStateRegistry.registerCamera(mockCamera, CameraXExecutors.directExecutor(),
                () -> {
                });
        mCameraStateRegistry.tryOpenCamera(mockCamera);

        mCamera2CameraImpl.getCameraState().addObserver(CameraXExecutors.directExecutor(),
                mockObserver);

        mCamera2CameraImpl.open();

        // Ensure that the camera gets to a PENDING_OPEN state
        verify(mockObserver, timeout(3000).atLeastOnce()).onNewData(
                CameraInternal.State.PENDING_OPEN);

        // Allow camera to be opened
        mCameraStateRegistry.markCameraState(mockCamera, CameraInternal.State.CLOSED);

        verify(mockObserver, timeout(3000)).onNewData(CameraInternal.State.OPEN);

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

    @SuppressWarnings("unchecked")
    @Test
    public void openNewCaptureSessionImmediateBeforePreviousCaptureSessionClosed()
            throws InterruptedException {
        mCamera2CameraImpl.open();
        UseCase useCase1 = createUseCase();
        mCamera2CameraImpl.attachUseCases(Arrays.asList(useCase1));
        mCamera2CameraImpl.onUseCaseActive(useCase1);

        // Wait a little bit for the camera to open.
        assertTrue(mSessionStateCallback.waitForOnConfigured(1));

        // Remove the useCase1 and trigger the CaptureSession#close().
        mCamera2CameraImpl.detachUseCases(Arrays.asList(useCase1));

        // Create the secondary use case immediately and open it before the first use case closed.
        UseCase useCase2 = createUseCase();
        mCamera2CameraImpl.attachUseCases(Arrays.asList(useCase2));
        mCamera2CameraImpl.onUseCaseActive(useCase2);
        // Wait for the secondary capture session is configured.
        assertTrue(mSessionStateCallback.waitForOnConfigured(1));

        Observable.Observer<CameraInternal.State> mockObserver = mock(Observable.Observer.class);
        mCamera2CameraImpl.getCameraState().addObserver(CameraXExecutors.directExecutor(),
                mockObserver);
        mCamera2CameraImpl.detachUseCases(Arrays.asList(useCase2));
        mCamera2CameraImpl.close();

        // Wait for the CLOSED state. If the test fail, the CameraX might in wrong internal state,
        // and the Camera2CameraImpl#release() might stuck.
        verify(mockObserver, timeout(4000).times(1)).onNewData(CameraInternal.State.CLOSED);
    }

    @Test
    public void closeCaptureSessionImmediateAfterCreateCaptureSession()
            throws InterruptedException {
        mCamera2CameraImpl.open();
        // Create another use case to keep the camera open.
        UseCase useCaseDummy = createUseCase();
        UseCase useCase = createUseCase();
        mCamera2CameraImpl.attachUseCases(Arrays.asList(useCase, useCaseDummy));
        mCamera2CameraImpl.onUseCaseActive(useCase);

        // Wait a little bit for the camera to open.
        assertTrue(mSessionStateCallback.waitForOnConfigured(2));

        // Remove the useCase and trigger the CaptureSession#close().
        mCamera2CameraImpl.detachUseCases(Arrays.asList(useCase));
        assertTrue(mSessionStateCallback.waitForOnClosed(2));
    }

    // Blocks the camera thread handler.
    private void blockHandler() {
        sCameraHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mSemaphore.acquire();
                } catch (InterruptedException e) {

                }
            }
        });
    }

    // unblock camera thread handler
    private void unblockHandler() {
        mSemaphore.release();
    }

    private UseCase createUseCase() {
        return createUseCase(CameraDevice.TEMPLATE_PREVIEW);
    }

    private UseCase createUseCase(int template) {
        FakeUseCaseConfig.Builder configBuilder =
                new FakeUseCaseConfig.Builder().setSessionOptionUnpacker(
                        new Camera2SessionOptionUnpacker()).setTargetName("UseCase");
        new Camera2Interop.Extender<>(configBuilder).setSessionStateCallback(mSessionStateCallback);
        CameraSelector selector =
                new CameraSelector.Builder().requireLensFacing(
                        CameraSelector.LENS_FACING_BACK).build();
        TestUseCase testUseCase = new TestUseCase(template, configBuilder.getUseCaseConfig(),
                selector, mMockOnImageAvailableListener, mMockRepeatingCaptureCallback);
        testUseCase.updateSuggestedResolution(new Size(640, 480));
        mFakeUseCases.add(testUseCase);
        return testUseCase;
    }

    @Test
    public void useCaseOnStateAttached_isCalled() throws InterruptedException {
        TestUseCase useCase1 = spy((TestUseCase) createUseCase());
        TestUseCase useCase2 = spy((TestUseCase) createUseCase());

        mCamera2CameraImpl.attachUseCases(Collections.singletonList(useCase1));
        mCamera2CameraImpl.attachUseCases(Arrays.asList(useCase1, useCase2));

        HandlerUtil.waitForLooperToIdle(sCameraHandler);

        Handler uiThreadHandler = new Handler(Looper.getMainLooper());
        HandlerUtil.waitForLooperToIdle(uiThreadHandler);

        verify(useCase1, times(1)).onStateAttached();
        verify(useCase2, times(1)).onStateAttached();

        mCamera2CameraImpl.detachUseCases(Arrays.asList(useCase1, useCase2));
    }

    @Test
    public void useCaseOnStateDetached_isCalled() throws InterruptedException {
        TestUseCase useCase1 = spy((TestUseCase) createUseCase());
        TestUseCase useCase2 = spy((TestUseCase) createUseCase());
        TestUseCase useCase3 = spy((TestUseCase) createUseCase());

        mCamera2CameraImpl.attachUseCases(Arrays.asList(useCase1, useCase2));

        mCamera2CameraImpl.detachUseCases(Arrays.asList(useCase1, useCase2, useCase3));

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
            // setZoom() will fail immediately when Cameracontrol is not active.
            listenableFuture.get(50, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof CameraControl.OperationCanceledException) {
                return false;
            }
        } catch (InterruptedException | TimeoutException e) {
        }
        return true;
    }

    @Test
    public void activateCameraControl_whenExistsAttachedUseCases() throws InterruptedException {
        Camera2CameraControlImpl camera2CameraControlImpl =
                (Camera2CameraControlImpl) mCamera2CameraImpl.getCameraControlInternal();

        assertThat(isCameraControlActive(camera2CameraControlImpl)).isFalse();

        UseCase useCase1 = createUseCase();

        mCamera2CameraImpl.attachUseCases(Collections.singletonList(useCase1));
        HandlerUtil.waitForLooperToIdle(sCameraHandler);

        assertThat(isCameraControlActive(camera2CameraControlImpl)).isTrue();

        mCamera2CameraImpl.detachUseCases(Collections.singletonList(useCase1));
    }

    @Test
    public void deactivateCameraControl_whenNoAttachedUseCases() throws InterruptedException {
        Camera2CameraControlImpl camera2CameraControlImpl =
                (Camera2CameraControlImpl) mCamera2CameraImpl.getCameraControlInternal();
        UseCase useCase1 = createUseCase();

        mCamera2CameraImpl.attachUseCases(Arrays.asList(useCase1));
        HandlerUtil.waitForLooperToIdle(sCameraHandler);
        assertThat(isCameraControlActive(camera2CameraControlImpl)).isTrue();

        mCamera2CameraImpl.detachUseCases(Arrays.asList(useCase1));
        HandlerUtil.waitForLooperToIdle(sCameraHandler);

        assertThat(isCameraControlActive(camera2CameraControlImpl)).isFalse();
    }

    @Test
    public void attachUseCaseWithTemplatePreview() throws InterruptedException {
        UseCase preview = createUseCase(CameraDevice.TEMPLATE_PREVIEW);

        mCamera2CameraImpl.attachUseCases(Arrays.asList(preview));
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

        mCamera2CameraImpl.detachUseCases(Arrays.asList(preview));
    }

    @Test
    public void attachUseCaseWithTemplateRecord() throws InterruptedException {
        UseCase preview = createUseCase(CameraDevice.TEMPLATE_PREVIEW);
        UseCase record = createUseCase(CameraDevice.TEMPLATE_RECORD);

        mCamera2CameraImpl.attachUseCases(Arrays.asList(preview, record));
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

        mCamera2CameraImpl.detachUseCases(Arrays.asList(preview, record));
    }

    private DeferrableSurface getUseCaseSurface(UseCase useCase) {
        return useCase.getSessionConfig().getSurfaces().get(0);
    }

    private void changeUseCaseSurface(UseCase useCase) {
        useCase.updateSuggestedResolution(new Size(640, 480));
    }

    private void waitForCameraClose(Camera2CameraImpl camera2CameraImpl)
            throws InterruptedException {
        Semaphore semaphore = new Semaphore(0);

        Observable.Observer<CameraInternal.State> observer =
                new Observable.Observer<CameraInternal.State>() {
                    @Override
                    public void onNewData(@Nullable CameraInternal.State value) {
                        // Ignore any transient states.
                        if (value == CameraInternal.State.CLOSED) {
                            semaphore.release();
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable t) { /* Ignore any transient errors. */ }
                };

        camera2CameraImpl.getCameraState().addObserver(CameraXExecutors.directExecutor(), observer);

        // Wait until camera reaches closed state
        semaphore.acquire();
    }

    public static class TestUseCase extends FakeUseCase {
        private final ImageReader.OnImageAvailableListener mImageAvailableListener;
        HandlerThread mHandlerThread = new HandlerThread("HandlerThread");
        Handler mHandler;
        FakeUseCaseConfig mConfig;
        private String mCameraId;
        private DeferrableSurface mDeferrableSurface;
        private final CameraCaptureCallback mRepeatingCaptureCallback;
        private final int mTemplate;

        TestUseCase(
                int template,
                @NonNull FakeUseCaseConfig config,
                @NonNull CameraSelector cameraSelector,
                @NonNull ImageReader.OnImageAvailableListener listener,
                @NonNull CameraCaptureCallback repeatingCaptureCallback) {
            super(config);
            // Ensure we're using the combined configuration (user config + defaults)
            mConfig = (FakeUseCaseConfig) getCurrentConfig();
            mTemplate = template;

            mImageAvailableListener = listener;
            mRepeatingCaptureCallback = repeatingCaptureCallback;
            mHandlerThread.start();
            mHandler = new Handler(mHandlerThread.getLooper());
            Integer lensFacing =
                    cameraSelector.getLensFacing() == null ? CameraSelector.LENS_FACING_BACK :
                            cameraSelector.getLensFacing();
            mCameraId = CameraUtil.getCameraIdWithLensFacing(lensFacing);
            onAttach(new FakeCamera(mCameraId, null,
                            new FakeCameraInfoInternal(mCameraId, 0, lensFacing)),
                    null, null);
            updateSuggestedResolution(new Size(640, 480));
        }

        public void close() {
            mHandler.removeCallbacksAndMessages(null);
            mHandlerThread.quitSafely();
            if (mDeferrableSurface != null) {
                mDeferrableSurface.close();
            }
        }

        @Override
        public void onDetached() {
            super.onDetached();
            close();
        }

        @Override
        @NonNull
        protected Size onSuggestedResolutionUpdated(
                @NonNull Size suggestedResolution) {
            SessionConfig.Builder builder = SessionConfig.Builder.createFrom(mConfig);

            builder.setTemplateType(mTemplate);
            builder.addRepeatingCameraCaptureCallback(mRepeatingCaptureCallback);

            if (mDeferrableSurface != null) {
                mDeferrableSurface.close();
            }
            ImageReader imageReader =
                    ImageReader.newInstance(
                            suggestedResolution.getWidth(),
                            suggestedResolution.getHeight(),
                            ImageFormat.YUV_420_888, /*maxImages*/
                            2);
            imageReader.setOnImageAvailableListener(mImageAvailableListener, mHandler);
            Surface surface = imageReader.getSurface();
            mDeferrableSurface = new ImmediateSurface(surface);
            mDeferrableSurface.getTerminationFuture().addListener(() -> {
                surface.release();
                imageReader.close();
            }, CameraXExecutors.directExecutor());
            builder.addSurface(mDeferrableSurface);

            updateSessionConfig(builder.build());
            return suggestedResolution;
        }
    }
}
