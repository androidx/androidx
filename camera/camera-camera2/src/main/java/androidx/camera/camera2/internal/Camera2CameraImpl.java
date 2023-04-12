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

import static androidx.camera.core.concurrent.CameraCoordinator.CAMERA_OPERATING_MODE_CONCURRENT;

import android.annotation.SuppressLint;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.camera.camera2.internal.annotation.CameraExecutor;
import androidx.camera.camera2.internal.compat.ApiCompat;
import androidx.camera.camera2.internal.compat.CameraAccessExceptionCompat;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.camera2.internal.compat.CameraManagerCompat;
import androidx.camera.camera2.internal.compat.quirk.DeviceQuirks;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.CameraState;
import androidx.camera.core.CameraUnavailableException;
import androidx.camera.core.Logger;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCase;
import androidx.camera.core.concurrent.CameraCoordinator;
import androidx.camera.core.impl.CameraConfig;
import androidx.camera.core.impl.CameraConfigs;
import androidx.camera.core.impl.CameraControlInternal;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.CameraStateRegistry;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.ImmediateSurface;
import androidx.camera.core.impl.LiveDataObservable;
import androidx.camera.core.impl.Observable;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.SessionConfig.ValidatingBuilder;
import androidx.camera.core.impl.SessionProcessor;
import androidx.camera.core.impl.UseCaseAttachState;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.impl.annotation.ExecutedBy;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.util.Preconditions;

import com.google.auto.value.AutoValue;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A camera which is controlled by the change of state in use cases.
 *
 * <p>The camera needs to be in an open state in order for use cases to control the camera. Whenever
 * there is a non-zero number of use cases in the attached state the camera will either have a
 * capture session open or be in the process of opening up one. If the number of uses cases in
 * the attached state changes then the capture session will be reconfigured.
 *
 * <p>Capture requests will be issued only for use cases which are in both the attached and active
 * state.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
final class Camera2CameraImpl implements CameraInternal {
    private static final String TAG = "Camera2CameraImpl";
    private static final int ERROR_NONE = 0;

    /**
     * Map of the use cases to the information on their state. Should only be accessed on the
     * camera's executor.
     */
    private final UseCaseAttachState mUseCaseAttachState;

    /** Handle to the camera service. */
    private final CameraManagerCompat mCameraManager;

    /** The executor for camera callbacks and use case state management calls. */
    @CameraExecutor
    private final Executor mExecutor;
    private final ScheduledExecutorService mScheduledExecutorService;

    /**
     * State variable for tracking state of the camera.
     *
     * <p>Is volatile because it is initialized in the instance initializer which is not necessarily
     * called on the same thread as any of the other methods and callbacks.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    volatile InternalState mState = InternalState.INITIALIZED;
    private final LiveDataObservable<CameraInternal.State> mObservableState =
            new LiveDataObservable<>();
    private final CameraStateMachine mCameraStateMachine;
    /** The camera control shared across all use cases bound to this Camera. */
    private final Camera2CameraControlImpl mCameraControlInternal;
    private final StateCallback mStateCallback;
    /** Information about the characteristics of this camera */
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @NonNull
    final Camera2CameraInfoImpl mCameraInfoInternal;
    /** The handle to the opened camera. */
    @Nullable
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    CameraDevice mCameraDevice;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    int mCameraDeviceError = ERROR_NONE;

    /** The configured session which handles issuing capture requests. */
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    CaptureSessionInterface mCaptureSession;

    // Used to debug number of requests to release camera
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final AtomicInteger mReleaseRequestCount = new AtomicInteger(0);
    // Should only be accessed on code executed by mExecutor
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    ListenableFuture<Void> mUserReleaseFuture;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    CallbackToFutureAdapter.Completer<Void> mUserReleaseNotifier;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final Map<CaptureSessionInterface, ListenableFuture<Void>> mReleasedCaptureSessions =
            new LinkedHashMap<>();

    @NonNull final CameraAvailability mCameraAvailability;
    @NonNull final CameraConfigureAvailable mCameraConfigureAvailable;
    @NonNull final CameraCoordinator mCameraCoordinator;
    @NonNull final CameraStateRegistry mCameraStateRegistry;

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final Set<CaptureSession> mConfiguringForClose = new HashSet<>();

    // The metering repeating use case for ImageCapture only case.
    private MeteringRepeatingSession mMeteringRepeatingSession;

    @NonNull
    private final CaptureSessionRepository mCaptureSessionRepository;
    @NonNull
    private final SynchronizedCaptureSessionOpener.Builder mCaptureSessionOpenerBuilder;
    private final Set<String> mNotifyStateAttachedSet = new HashSet<>();

    @NonNull
    private CameraConfig mCameraConfig = CameraConfigs.emptyConfig();
    final Object mLock = new Object();
    // mSessionProcessor will be used to transform capture session if non-null.
    @GuardedBy("mLock")
    @Nullable
    private SessionProcessor mSessionProcessor;
    boolean mIsActiveResumingMode = false;

    @NonNull
    private final DisplayInfoManager mDisplayInfoManager;

    @NonNull
    private final CameraCharacteristicsCompat mCameraCharacteristicsCompat;

    /**
     * Constructor for a camera.
     *
     * @param cameraManager       the camera service used to retrieve a camera
     * @param cameraId            the name of the camera as defined by the camera service
     * @param cameraCoordinator   the camera coordinator for concurrent camera mode
     * @param cameraStateRegistry An registry used to track the state of multiple cameras.
     *                            Used as a fence to ensure the number of simultaneously
     *                            opened cameras is limited.
     * @param executor            the executor for on which all camera operations run
     * @throws CameraUnavailableException if the {@link CameraCharacteristics} is unavailable. This
     *                                    could occur if the camera was disconnected.
     */
    Camera2CameraImpl(
            @NonNull CameraManagerCompat cameraManager,
            @NonNull String cameraId,
            @NonNull Camera2CameraInfoImpl cameraInfoImpl,
            @NonNull CameraCoordinator cameraCoordinator,
            @NonNull CameraStateRegistry cameraStateRegistry,
            @NonNull Executor executor,
            @NonNull Handler schedulerHandler,
            @NonNull DisplayInfoManager displayInfoManager) throws CameraUnavailableException {
        mCameraManager = cameraManager;
        mCameraCoordinator = cameraCoordinator;
        mCameraStateRegistry = cameraStateRegistry;
        mScheduledExecutorService = CameraXExecutors.newHandlerExecutor(schedulerHandler);
        mExecutor = CameraXExecutors.newSequentialExecutor(executor);
        mStateCallback = new StateCallback(mExecutor, mScheduledExecutorService);
        mUseCaseAttachState = new UseCaseAttachState(cameraId);
        mObservableState.postValue(State.CLOSED);
        mCameraStateMachine = new CameraStateMachine(cameraStateRegistry);
        mCaptureSessionRepository = new CaptureSessionRepository(mExecutor);
        mDisplayInfoManager = displayInfoManager;
        mCaptureSession = newCaptureSession();

        try {
            mCameraCharacteristicsCompat =
                    mCameraManager.getCameraCharacteristicsCompat(cameraId);
            mCameraControlInternal = new Camera2CameraControlImpl(mCameraCharacteristicsCompat,
                    mScheduledExecutorService, mExecutor, new ControlUpdateListenerInternal(),
                    cameraInfoImpl.getCameraQuirks());
            mCameraInfoInternal = cameraInfoImpl;
            mCameraInfoInternal.linkWithCameraControl(mCameraControlInternal);
            mCameraInfoInternal.setCameraStateSource(mCameraStateMachine.getStateLiveData());
        } catch (CameraAccessExceptionCompat e) {
            throw CameraUnavailableExceptionHelper.createFrom(e);
        }
        mCaptureSessionOpenerBuilder = new SynchronizedCaptureSessionOpener.Builder(mExecutor,
                mScheduledExecutorService, schedulerHandler, mCaptureSessionRepository,
                cameraInfoImpl.getCameraQuirks(), DeviceQuirks.getAll());

        mCameraAvailability = new CameraAvailability(cameraId);
        mCameraConfigureAvailable = new CameraConfigureAvailable();

        // Register an observer to update the number of available cameras
        mCameraStateRegistry.registerCamera(
                this,
                mExecutor,
                mCameraConfigureAvailable,
                mCameraAvailability);
        mCameraManager.registerAvailabilityCallback(mExecutor, mCameraAvailability);
    }

    @NonNull
    private CaptureSessionInterface newCaptureSession() {
        synchronized (mLock) {
            if (mSessionProcessor == null) {
                return new CaptureSession();
            } else {
                return new ProcessingCaptureSession(mSessionProcessor,
                        mCameraInfoInternal, mExecutor, mScheduledExecutorService);
            }
        }
    }

    /**
     * Open the camera asynchronously.
     *
     * <p>Once the camera has been opened use case state transitions can be used to control the
     * camera pipeline.
     */
    @Override
    public void open() {
        mExecutor.execute(this::openInternal);
    }

    @ExecutedBy("mExecutor")
    private void openInternal() {
        switch (mState) {
            case INITIALIZED:
            case PENDING_OPEN:
                tryForceOpenCameraDevice(/*fromScheduledCameraReopen*/false);
                break;
            case CLOSING:
                setState(InternalState.REOPENING);
                // If session close has not yet completed, then the camera is still open. We
                // can move directly back into an OPENED state.
                // If session close is already complete, then the camera is closing. We'll reopen
                // the camera in the camera state callback.
                // If the camera device is currently in an error state, we need to close the
                // camera before reopening, so we cannot directly reopen.
                if (!isSessionCloseComplete() && mCameraDeviceError == ERROR_NONE) {
                    Preconditions.checkState(mCameraDevice != null,
                            "Camera Device should be open if session close is not complete");
                    setState(InternalState.OPENED);
                    openCaptureSession();
                }
                break;
            default:
                debugLog("open() ignored due to being in state: " + mState);
        }
    }

    /**
     * Close the camera asynchronously.
     *
     * <p>Once the camera is closed the camera will no longer produce data. The camera must be
     * reopened for it to produce data again.
     */
    @Override
    public void close() {
        mExecutor.execute(this::closeInternal);
    }

    @ExecutedBy("mExecutor")
    private void closeInternal() {
        debugLog("Closing camera.");
        switch (mState) {
            case OPENED:
            case CONFIGURED:
                setState(InternalState.CLOSING);
                closeCamera(/*abortInFlightCaptures=*/false);
                break;
            case OPENING:
            case REOPENING:
                boolean canFinish = mStateCallback.cancelScheduledReopen();
                setState(InternalState.CLOSING);
                if (canFinish) {
                    Preconditions.checkState(isSessionCloseComplete());
                    finishClose();
                }
                break;
            case PENDING_OPEN:
                // We should be able to transition directly to an initialized state since the
                // camera is not yet opening.
                Preconditions.checkState(mCameraDevice == null);
                setState(InternalState.INITIALIZED);
                break;
            default:
                debugLog("close() ignored due to being in state: " + mState);
        }
    }

    // Configure the camera with a no-op capture session in order to clear the
    // previous session. This should be released immediately after being configured.
    @ExecutedBy("mExecutor")
    private void configAndClose(boolean abortInFlightCaptures) {

        final CaptureSession noOpSession = new CaptureSession();

        mConfiguringForClose.add(noOpSession);  // Make mCameraDevice is not closed and existed.
        resetCaptureSession(abortInFlightCaptures);

        final SurfaceTexture surfaceTexture = new SurfaceTexture(0);
        surfaceTexture.setDefaultBufferSize(640, 480);
        final Surface surface = new Surface(surfaceTexture);
        final Runnable closeAndCleanupRunner = () -> {
            surface.release();
            surfaceTexture.release();
        };

        SessionConfig.Builder builder = new SessionConfig.Builder();
        DeferrableSurface deferrableSurface = new ImmediateSurface(surface);
        builder.addNonRepeatingSurface(deferrableSurface);
        builder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
        debugLog("Start configAndClose.");
        ListenableFuture<Void> openNoOpCaptureSession = noOpSession.open(builder.build(),
                Preconditions.checkNotNull(mCameraDevice), mCaptureSessionOpenerBuilder.build());
        openNoOpCaptureSession.addListener(() -> {
            // Release the no-op Session and continue closing camera when in correct state.
            releaseNoOpSession(noOpSession, deferrableSurface, closeAndCleanupRunner);
        }, mExecutor);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @ExecutedBy("mExecutor")
    void releaseNoOpSession(@NonNull CaptureSession noOpSession,
            @NonNull DeferrableSurface deferrableSurface, @NonNull Runnable closeAndCleanupRunner) {
        // Config complete and remove the noOpSession from the mConfiguringForClose map
        // after resetCaptureSession and before release the noOpSession.
        mConfiguringForClose.remove(noOpSession);

        // Don't need to abort captures since there are none submitted for this session.
        ListenableFuture<Void> releaseFuture = releaseSession(
                noOpSession, /*abortInFlightCaptures=*/false);

        deferrableSurface.close();
        // Add a listener to clear the no-op surfaces
        Futures.successfulAsList(
                Arrays.asList(releaseFuture, deferrableSurface.getTerminationFuture())).addListener(
                closeAndCleanupRunner, CameraXExecutors.directExecutor());
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @ExecutedBy("mExecutor")
    boolean isSessionCloseComplete() {
        return mReleasedCaptureSessions.isEmpty() && mConfiguringForClose.isEmpty();
    }

    // This will notify futures of completion.
    // Should only be called once the camera device is actually closed.
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @ExecutedBy("mExecutor")
    void finishClose() {
        Preconditions.checkState(mState == InternalState.RELEASING
                || mState == InternalState.CLOSING);
        Preconditions.checkState(mReleasedCaptureSessions.isEmpty());

        mCameraDevice = null;
        if (mState == InternalState.CLOSING) {
            setState(InternalState.INITIALIZED);
        } else {
            // After a camera is released, it cannot be reopened, so we don't need to listen for
            // available camera changes.
            mCameraManager.unregisterAvailabilityCallback(mCameraAvailability);

            setState(InternalState.RELEASED);

            if (mUserReleaseNotifier != null) {
                mUserReleaseNotifier.set(null);
                mUserReleaseNotifier = null;
            }
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @ExecutedBy("mExecutor")
    void closeCamera(boolean abortInFlightCaptures) {
        Preconditions.checkState(mState == InternalState.CLOSING
                        || mState == InternalState.RELEASING
                        || (mState == InternalState.REOPENING && mCameraDeviceError != ERROR_NONE),
                "closeCamera should only be called in a CLOSING, RELEASING or REOPENING (with "
                        + "error) state. Current state: "
                        + mState + " (error: " + getErrorMessage(mCameraDeviceError) + ")");

        // TODO: Check if any sessions have been previously configured. We can probably skip
        // configAndClose if there haven't been any sessions configured yet.
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M
                && Build.VERSION.SDK_INT < 29
                && isLegacyDevice()
                && mCameraDeviceError == ERROR_NONE) { // Cannot open session on device in error
            // To configure surface again before close camera. This step would
            // disconnect previous connected surface in some legacy device to prevent exception.
            configAndClose(abortInFlightCaptures);
        } else {
            // Release the current session and replace with a new uninitialized session in case the
            // camera enters a REOPENING state during session closing.
            resetCaptureSession(abortInFlightCaptures);
        }

        mCaptureSession.cancelIssuedCaptureRequests();
    }

    /**
     * Release the camera.
     *
     * <p>Once the camera is released it is permanently closed. A new instance must be created to
     * access the camera.
     */
    @NonNull
    @Override
    public ListenableFuture<Void> release() {
        return CallbackToFutureAdapter.getFuture(
                completer -> {
                    mExecutor.execute(
                            () -> Futures.propagate(releaseInternal(), completer));
                    return "Release[request=" + mReleaseRequestCount.getAndIncrement() + "]";
                });
    }

    @ExecutedBy("mExecutor")
    private ListenableFuture<Void> releaseInternal() {
        ListenableFuture<Void> future = getOrCreateUserReleaseFuture();
        switch (mState) {
            case INITIALIZED:
            case PENDING_OPEN:
                Preconditions.checkState(mCameraDevice == null);
                setState(InternalState.RELEASING);
                Preconditions.checkState(isSessionCloseComplete());
                finishClose();
                break;
            case OPENED:
            case CONFIGURED:
                setState(InternalState.RELEASING);
                //TODO(b/162314023): Avoid calling abortCapture to prevent the many test failures
                // caused by shutdown(). We should consider re-enabling it once the cause is
                // found.
                closeCamera(/*abortInFlightCaptures=*/false);
                break;
            case OPENING:
            case CLOSING:
            case REOPENING:
            case RELEASING:
                boolean canFinish = mStateCallback.cancelScheduledReopen();
                // Wait for the camera async callback to finish releasing
                setState(InternalState.RELEASING);
                if (canFinish) {
                    Preconditions.checkState(isSessionCloseComplete());
                    finishClose();
                }
                break;
            default:
                debugLog("release() ignored due to being in state: " + mState);
        }

        return future;
    }

    @ExecutedBy("mExecutor")
    private ListenableFuture<Void> getOrCreateUserReleaseFuture() {
        if (mUserReleaseFuture == null) {
            if (mState != InternalState.RELEASED) {
                mUserReleaseFuture = CallbackToFutureAdapter.getFuture(
                        completer -> {
                            Preconditions.checkState(mUserReleaseNotifier == null,
                                    "Camera can only be released once, so release completer "
                                            + "should be null on creation.");
                            mUserReleaseNotifier = completer;
                            return "Release[camera=" + Camera2CameraImpl.this + "]";
                        });
            } else {
                // Set to an immediately successful future if already in the released state.
                mUserReleaseFuture = Futures.immediateFuture(null);
            }
        }

        return mUserReleaseFuture;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @ExecutedBy("mExecutor")
    ListenableFuture<Void> releaseSession(@NonNull final CaptureSessionInterface captureSession,
            boolean abortInFlightCaptures) {
        captureSession.close();
        ListenableFuture<Void> releaseFuture = captureSession.release(abortInFlightCaptures);

        debugLog("Releasing session in state " + mState.name());
        mReleasedCaptureSessions.put(captureSession, releaseFuture);

        // Add a callback to clear the future and notify if the camera and all capture sessions
        // are released

        Futures.addCallback(releaseFuture, new FutureCallback<Void>() {
            @ExecutedBy("mExecutor")
            @Override
            public void onSuccess(@Nullable Void result) {
                mReleasedCaptureSessions.remove(captureSession);
                switch (mState) {
                    case REOPENING:
                        if (mCameraDeviceError == ERROR_NONE) {
                            // When reopening, don't close the camera if there is no error.
                            break;
                        }
                        // Fall through if the camera device is in error. It needs to be closed.
                    case CLOSING:
                    case RELEASING:
                        if (isSessionCloseComplete() && mCameraDevice != null) {
                            ApiCompat.Api21Impl.close(mCameraDevice);
                            mCameraDevice = null;
                        }
                        break;
                    default:
                        // Ignore all other states
                }
            }

            @ExecutedBy("mExecutor")
            @Override
            public void onFailure(@NonNull Throwable t) {
                // Don't reset the internal release future as we want to keep track of the error
                // TODO: The camera should be put into an error state at this point
            }
            // Should always be called on the same executor thread, so directExecutor is OK here.
        }, CameraXExecutors.directExecutor());

        return releaseFuture;
    }

    @NonNull
    @Override
    public Observable<CameraInternal.State> getCameraState() {
        return mObservableState;
    }

    /**
     * Sets the use case in a state to issue capture requests.
     *
     * <p>The use case must also be attached in order for it to issue capture requests.
     */
    @Override
    public void onUseCaseActive(@NonNull UseCase useCase) {
        Preconditions.checkNotNull(useCase);
        String useCaseId = getUseCaseId(useCase);
        SessionConfig sessionConfig = useCase.getSessionConfig();
        UseCaseConfig<?> useCaseConfig = useCase.getCurrentConfig();
        mExecutor.execute(() -> {
            debugLog("Use case " + useCaseId + " ACTIVE");

            mUseCaseAttachState.setUseCaseActive(useCaseId, sessionConfig, useCaseConfig);
            mUseCaseAttachState.updateUseCase(useCaseId, sessionConfig, useCaseConfig);
            updateCaptureSessionConfig();
        });
    }


    /** Removes the use case from a state of issuing capture requests. */
    @Override
    public void onUseCaseInactive(@NonNull UseCase useCase) {
        Preconditions.checkNotNull(useCase);
        String useCaseId = getUseCaseId(useCase);
        mExecutor.execute(() -> {
            debugLog("Use case " + useCaseId + " INACTIVE");
            mUseCaseAttachState.setUseCaseInactive(useCaseId);
            updateCaptureSessionConfig();
        });
    }

    /** Updates the capture requests based on the latest settings. */
    @Override
    public void onUseCaseUpdated(@NonNull UseCase useCase) {
        Preconditions.checkNotNull(useCase);
        String useCaseId = getUseCaseId(useCase);
        SessionConfig sessionConfig = useCase.getSessionConfig();
        UseCaseConfig<?> useCaseConfig = useCase.getCurrentConfig();
        mExecutor.execute(() -> {
            debugLog("Use case " + useCaseId + " UPDATED");
            mUseCaseAttachState.updateUseCase(useCaseId, sessionConfig, useCaseConfig);
            updateCaptureSessionConfig();
        });
    }

    @Override
    public void onUseCaseReset(@NonNull UseCase useCase) {
        Preconditions.checkNotNull(useCase);
        String useCaseId = getUseCaseId(useCase);
        SessionConfig sessionConfig = useCase.getSessionConfig();
        UseCaseConfig<?> useCaseConfig = useCase.getCurrentConfig();
        mExecutor.execute(() -> {
            debugLog("Use case " + useCaseId + " RESET");
            mUseCaseAttachState.updateUseCase(useCaseId, sessionConfig, useCaseConfig);
            addOrRemoveMeteringRepeatingUseCase();
            resetCaptureSession(/*abortInFlightCaptures=*/false);
            updateCaptureSessionConfig();

            // If the use case is reset while the camera is open, a new capture session should be
            // opened. Otherwise, once the camera eventually becomes in an open state, it will
            // open a new capture session using the latest session config.
            if (mState == InternalState.OPENED) {
                openCaptureSession();
            }
        });
    }

    /**
     * Returns whether the provided {@link UseCase} is considered attached.
     *
     * <p>This method should only be used by tests. This will post to the Camera's thread and
     * block until completion.
     *
     */
    @RestrictTo(RestrictTo.Scope.TESTS)
    boolean isUseCaseAttached(@NonNull UseCase useCase) {
        try {
            String useCaseId = getUseCaseId(useCase);

            return CallbackToFutureAdapter.<Boolean>getFuture(completer -> {
                try {
                    mExecutor.execute(
                            () -> completer.set(mUseCaseAttachState.isUseCaseAttached(useCaseId)));
                } catch (RejectedExecutionException e) {
                    completer.setException(new RuntimeException("Unable to check if use case is "
                            + "attached. Camera executor shut down."));
                }
                return "isUseCaseAttached";
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Unable to check if use case is attached.", e);
        }
    }

    @VisibleForTesting
    boolean isMeteringRepeatingAttached() {
        try {
            return CallbackToFutureAdapter.<Boolean>getFuture(completer -> {
                try {
                    mExecutor.execute(() -> {
                        if (mMeteringRepeatingSession == null) {
                            completer.set(false);
                            return;
                        }
                        String id = getMeteringRepeatingId(mMeteringRepeatingSession);
                        completer.set(mUseCaseAttachState.isUseCaseAttached(id));
                    });
                } catch (RejectedExecutionException e) {
                    completer.setException(new RuntimeException(
                            "Unable to check if MeteringRepeating is attached. Camera executor "
                                    + "shut down."));
                }
                return "isMeteringRepeatingAttached";
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Unable to check if MeteringRepeating is attached.", e);
        }
    }

    /**
     * Sets the use case to be in the state where the capture session will be configured to handle
     * capture requests from the use case.
     */
    @Override
    public void attachUseCases(@NonNull Collection<UseCase> inputUseCases) {
        // Defensively copy the inputUseCases to prevent from being changed.
        Collection<UseCase> useCases = new ArrayList<>(inputUseCases);

        if (useCases.isEmpty()) {
            return;
        }

        /*
         * Increase the camera control use count so that camera control can accept requests
         * immediately before posting to the executor. The use count should be increased
         * again during the posted tryAttachUseCases task. After the posted task, decrease the
         * use count to recover the additional increment here.
         */
        mCameraControlInternal.incrementUseCount();
        notifyStateAttachedAndCameraControlReady(new ArrayList<>(useCases));
        List<UseCaseInfo> useCaseInfos = new ArrayList<>(toUseCaseInfos(useCases));
        try {
            mExecutor.execute(() -> {
                try {
                    tryAttachUseCases(useCaseInfos);
                } finally {
                    mCameraControlInternal.decrementUseCount();
                }
            });
        } catch (RejectedExecutionException e) {
            debugLog("Unable to attach use cases.", e);
            mCameraControlInternal.decrementUseCount();
        }
    }

    /** Attempts to attach use cases if they are not already attached. */
    @ExecutedBy("mExecutor")
    private void tryAttachUseCases(@NonNull Collection<UseCaseInfo> useCaseInfos) {
        final boolean attachUseCaseFromEmpty =
                mUseCaseAttachState.getAttachedSessionConfigs().isEmpty();
        // Figure out which use cases are not already attached and add them.
        List<String> useCaseIdsToAttach = new ArrayList<>();
        Rational previewAspectRatio = null;

        for (UseCaseInfo useCaseInfo : useCaseInfos) {
            if (!mUseCaseAttachState.isUseCaseAttached(useCaseInfo.getUseCaseId())) {
                mUseCaseAttachState.setUseCaseAttached(useCaseInfo.getUseCaseId(),
                        useCaseInfo.getSessionConfig(), useCaseInfo.getUseCaseConfig());

                useCaseIdsToAttach.add(useCaseInfo.getUseCaseId());

                if (useCaseInfo.getUseCaseType() == Preview.class) {
                    Size resolution = useCaseInfo.getSurfaceResolution();
                    if (resolution != null) {
                        previewAspectRatio = new Rational(resolution.getWidth(),
                                resolution.getHeight());
                    }
                }
            }
        }

        if (useCaseIdsToAttach.isEmpty()) {
            return;
        }

        debugLog("Use cases [" + TextUtils.join(", ", useCaseIdsToAttach) + "] now ATTACHED");

        if (attachUseCaseFromEmpty) {
            // Notify camera control when first use case is attached
            mCameraControlInternal.setActive(true);
            mCameraControlInternal.incrementUseCount();
        }

        // Check if need to add or remove MeetingRepeatingUseCase.
        addOrRemoveMeteringRepeatingUseCase();

        // Update Zsl disabled status by iterating all attached use cases.
        updateZslDisabledByUseCaseConfigStatus();

        updateCaptureSessionConfig();
        resetCaptureSession(/*abortInFlightCaptures=*/false);

        if (mState == InternalState.OPENED) {
            openCaptureSession();
        } else {
            openInternal();
        }

        // Sets camera control preview aspect ratio if the attached use cases include a preview.
        if (previewAspectRatio != null) {
            mCameraControlInternal.setPreviewAspectRatio(previewAspectRatio);
        }
    }

    @NonNull
    private Collection<UseCaseInfo> toUseCaseInfos(@NonNull Collection<UseCase> useCases) {
        List<UseCaseInfo> useCaseInfos = new ArrayList<>();

        for (UseCase useCase : useCases) {
            useCaseInfos.add(UseCaseInfo.from(useCase));
        }

        return useCaseInfos;
    }

    @Override
    public void setExtendedConfig(@Nullable CameraConfig cameraConfig) {
        if (cameraConfig == null) {
            cameraConfig = CameraConfigs.emptyConfig();
        }

        SessionProcessor sessionProcessor = cameraConfig.getSessionProcessor(null);
        mCameraConfig = cameraConfig;

        synchronized (mLock) {
            mSessionProcessor = sessionProcessor;
        }
    }

    @NonNull
    @Override
    public CameraConfig getExtendedConfig() {
        return mCameraConfig;
    }

    private void notifyStateAttachedAndCameraControlReady(List<UseCase> useCases) {
        for (UseCase useCase : useCases) {
            String useCaseId = getUseCaseId(useCase);
            if (mNotifyStateAttachedSet.contains(useCaseId)) {
                continue;
            }

            mNotifyStateAttachedSet.add(useCaseId);
            useCase.onStateAttached();
            useCase.onCameraControlReady();
        }
    }

    private void notifyStateDetachedToUseCases(List<UseCase> useCases) {
        for (UseCase useCase : useCases) {
            String useCaseId = getUseCaseId(useCase);
            if (!mNotifyStateAttachedSet.contains(useCaseId)) {
                continue;
            }

            useCase.onStateDetached();
            mNotifyStateAttachedSet.remove(useCaseId);
        }
    }

    /**
     * Removes the use case to be in the state where the capture session will be configured to
     * handle capture requests from the use case.
     */
    @Override
    public void detachUseCases(@NonNull Collection<UseCase> inputUseCases) {
        // Defensively copy the inputUseCases to prevent from being changed.
        Collection<UseCase> useCases = new ArrayList<>(inputUseCases);

        if (useCases.isEmpty()) {
            return;
        }

        List<UseCaseInfo> useCaseInfos = new ArrayList<>(toUseCaseInfos(useCases));
        notifyStateDetachedToUseCases(new ArrayList<>(useCases));
        mExecutor.execute(() -> tryDetachUseCases(useCaseInfos));
    }

    // Attempts to make detach UseCases if they are attached.
    @ExecutedBy("mExecutor")
    private void tryDetachUseCases(@NonNull Collection<UseCaseInfo> useCaseInfos) {
        List<String> useCaseIdsToDetach = new ArrayList<>();
        boolean clearPreviewAspectRatio = false;

        for (UseCaseInfo useCaseInfo : useCaseInfos) {
            if (mUseCaseAttachState.isUseCaseAttached(useCaseInfo.getUseCaseId())) {
                mUseCaseAttachState.removeUseCase(useCaseInfo.getUseCaseId());
                useCaseIdsToDetach.add(useCaseInfo.getUseCaseId());

                if (useCaseInfo.getUseCaseType() == Preview.class) {
                    clearPreviewAspectRatio = true;
                }
            }
        }

        if (useCaseIdsToDetach.isEmpty()) {
            return;
        }

        debugLog("Use cases [" + TextUtils.join(", ", useCaseIdsToDetach)
                + "] now DETACHED for camera");

        // Clear camera control preview aspect ratio if the detached use cases include a preview.
        if (clearPreviewAspectRatio) {
            mCameraControlInternal.setPreviewAspectRatio(null);
        }

        // Check if need to add or remove MeetingRepeatingUseCase.
        addOrRemoveMeteringRepeatingUseCase();

        // Reset Zsl disabled status if no attached use cases, otherwise update by iterating all
        // attached use cases.
        if (mUseCaseAttachState.getAttachedUseCaseConfigs().isEmpty()) {
            mCameraControlInternal.setZslDisabledByUserCaseConfig(false);
        } else {
            updateZslDisabledByUseCaseConfigStatus();
        }

        boolean allUseCasesDetached = mUseCaseAttachState.getAttachedSessionConfigs().isEmpty();
        if (allUseCasesDetached) {
            mCameraControlInternal.decrementUseCount();
            resetCaptureSession(/*abortInFlightCaptures=*/false);
            // Call CameraControl#setActive(false) after CaptureSession is closed can prevent
            // calling updateCaptureSessionConfig() from CameraControl, which may cause
            // unnecessary repeating request update.
            mCameraControlInternal.setActive(false);
            // If all detached, manual nullify session config to avoid
            // memory leak. See: https://issuetracker.google.com/issues/141188637
            mCaptureSession = newCaptureSession();
            closeInternal();
        } else {
            updateCaptureSessionConfig();
            resetCaptureSession(/*abortInFlightCaptures=*/false);

            if (mState == InternalState.OPENED) {
                openCaptureSession();
            }
        }
    }

    private void updateZslDisabledByUseCaseConfigStatus() {
        boolean isZslDisabledByUseCaseConfig = false;
        for (UseCaseConfig<?> useCaseConfig : mUseCaseAttachState.getAttachedUseCaseConfigs()) {
            isZslDisabledByUseCaseConfig |= useCaseConfig.isZslDisabled(false);
        }
        mCameraControlInternal.setZslDisabledByUserCaseConfig(isZslDisabledByUseCaseConfig);
    }

    // Check if it need the repeating surface for ImageCapture only use case.
    private void addOrRemoveMeteringRepeatingUseCase() {
        ValidatingBuilder validatingBuilder = mUseCaseAttachState.getAttachedBuilder();
        SessionConfig sessionConfig = validatingBuilder.build();
        CaptureConfig captureConfig = sessionConfig.getRepeatingCaptureConfig();
        int sizeRepeatingSurfaces = captureConfig.getSurfaces().size();
        int sizeSessionSurfaces = sessionConfig.getSurfaces().size();

        if (!sessionConfig.getSurfaces().isEmpty()) {
            if (captureConfig.getSurfaces().isEmpty()) {
                // Create the MeteringRepeating UseCase
                if (mMeteringRepeatingSession == null) {
                    mMeteringRepeatingSession = new MeteringRepeatingSession(
                            mCameraInfoInternal.getCameraCharacteristicsCompat(),
                            mDisplayInfoManager);
                }
                addMeteringRepeating();
            } else {
                // There is mMeteringRepeating and attached, check to remove it or not.
                if (sizeSessionSurfaces == 1 && sizeRepeatingSurfaces == 1) {
                    // The only attached use case is MeteringRepeating, directly remove it.
                    removeMeteringRepeating();
                } else if (sizeRepeatingSurfaces >= 2) {
                    // There are other repeating UseCases, remove the MeteringRepeating.
                    removeMeteringRepeating();
                } else {
                    // Other normal cases, do nothing.
                    Logger.d(TAG, "mMeteringRepeating is ATTACHED, "
                            + "SessionConfig Surfaces: " + sizeSessionSurfaces + ", "
                            + "CaptureConfig Surfaces: " + sizeRepeatingSurfaces);
                }
            }
        }
    }

    private void removeMeteringRepeating() {
        if (mMeteringRepeatingSession != null) {
            mUseCaseAttachState.setUseCaseDetached(
                    mMeteringRepeatingSession.getName() + mMeteringRepeatingSession.hashCode());
            mUseCaseAttachState.setUseCaseInactive(
                    mMeteringRepeatingSession.getName() + mMeteringRepeatingSession.hashCode());
            mMeteringRepeatingSession.clear();
            mMeteringRepeatingSession = null;
        }
    }

    private void addMeteringRepeating() {
        if (mMeteringRepeatingSession != null) {
            String id = getMeteringRepeatingId(mMeteringRepeatingSession);
            mUseCaseAttachState.setUseCaseAttached(
                    id,
                    mMeteringRepeatingSession.getSessionConfig(),
                    mMeteringRepeatingSession.getUseCaseConfig());
            mUseCaseAttachState.setUseCaseActive(
                    id,
                    mMeteringRepeatingSession.getSessionConfig(),
                    mMeteringRepeatingSession.getUseCaseConfig());
        }
    }

    /** Returns an interface to retrieve characteristics of the camera. */
    @NonNull
    @Override
    public CameraInfoInternal getCameraInfoInternal() {
        return mCameraInfoInternal;
    }

    @NonNull
    @RestrictTo(RestrictTo.Scope.TESTS)
    public CameraAvailability getCameraAvailability() {
        return mCameraAvailability;
    }

    /**
     * Attempts to force open the camera device, which may result in stealing it from a lower
     * priority client. This should only happen if another client doesn't close the camera when
     * it should, e.g. when its process is moved to the background.
     *
     * @param fromScheduledCameraReopen True if the attempt to open the camera originated from a
     *                                  {@linkplain StateCallback.ScheduledReopen scheduled
     *                                  reopen of the camera}. False otherwise.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @ExecutedBy("mExecutor")
    void tryForceOpenCameraDevice(boolean fromScheduledCameraReopen) {
        debugLog("Attempting to force open the camera.");
        final boolean shouldTryOpenCamera = mCameraStateRegistry.tryOpenCamera(this);
        if (!shouldTryOpenCamera) {
            debugLog("No cameras available. Waiting for available camera before opening camera.");
            setState(InternalState.PENDING_OPEN);
            return;
        }
        openCameraDevice(fromScheduledCameraReopen);
    }

    /**
     * Attempts to open the camera device. Unlike {@link #tryForceOpenCameraDevice(boolean)},
     * this method does not steal the camera away from other clients.
     *
     * @param fromScheduledCameraReopen True if the attempt to open the camera originated from a
     *                                  {@linkplain StateCallback.ScheduledReopen scheduled
     *                                  reopen of the camera}. False otherwise.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @ExecutedBy("mExecutor")
    void tryOpenCameraDevice(boolean fromScheduledCameraReopen) {
        debugLog("Attempting to open the camera.");
        final boolean shouldTryOpenCamera =
                mCameraAvailability.isCameraAvailable() && mCameraStateRegistry.tryOpenCamera(this);
        if (!shouldTryOpenCamera) {
            debugLog("No cameras available. Waiting for available camera before opening camera.");
            setState(InternalState.PENDING_OPEN);
            return;
        }
        openCameraDevice(fromScheduledCameraReopen);
    }

    @Override
    public void setActiveResumingMode(boolean enabled) {
        mExecutor.execute(() -> {
            // Enables/Disables active resuming mode which will reopen the camera regardless of the
            // availability when camera is interrupted.
            mIsActiveResumingMode = enabled;

            // If camera is interrupted currently, force open the camera right now regardless of the
            // camera availability.
            if (enabled && mState == InternalState.PENDING_OPEN) {
                tryForceOpenCameraDevice(/*fromScheduledCameraReopen*/false);
            }
        });
    }


    /**
     * Opens the camera device.
     *
     * @param fromScheduledCameraReopen True if the attempt to open the camera originated from a
     *                                  {@linkplain StateCallback.ScheduledReopen scheduled
     *                                  reopen of the camera}. False otherwise.
     */
    // TODO(b/124268878): Handle SecurityException and require permission in manifest.
    @SuppressLint("MissingPermission")
    @ExecutedBy("mExecutor")
    private void openCameraDevice(boolean fromScheduledCameraReopen) {
        if (!fromScheduledCameraReopen) {
            mStateCallback.resetReopenMonitor();
        }
        mStateCallback.cancelScheduledReopen();

        debugLog("Opening camera.");
        setState(InternalState.OPENING);

        try {
            mCameraManager.openCamera(mCameraInfoInternal.getCameraId(), mExecutor,
                    createDeviceStateCallback());
        } catch (CameraAccessExceptionCompat e) {
            debugLog("Unable to open camera due to " + e.getMessage());
            switch (e.getReason()) {
                case CameraAccessExceptionCompat.CAMERA_UNAVAILABLE_DO_NOT_DISTURB:
                    // Camera2 is unable to call the onError() callback for this case. It has to
                    // reset the state here.
                    setState(InternalState.INITIALIZED, CameraState.StateError.create(
                            CameraState.ERROR_DO_NOT_DISTURB_MODE_ENABLED, e));
                    break;
                default:
                    // Camera2 will call the onError() callback with the specific error code that
                    // caused this failure. No need to do anything here.
            }
        } catch (SecurityException e) {
            debugLog("Unable to open camera due to " + e.getMessage());
            // The camera manager throws a SecurityException when it is unable to access the
            // camera service due to lacking privileges (i.e. the camera permission). It is also
            // possible for the camera manager to erroneously throw a SecurityException when it
            // crashes even if the camera permission has been granted.
            // When this exception is thrown, the camera manager does not invoke the state
            // callback's onError() method, which is why we manually attempt to reopen the camera.
            setState(InternalState.REOPENING);
            mStateCallback.scheduleCameraReopen();
        }
    }

    /** Updates the capture request configuration for the current capture session. */
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @ExecutedBy("mExecutor")
    void updateCaptureSessionConfig() {
        ValidatingBuilder validatingBuilder = mUseCaseAttachState.getActiveAndAttachedBuilder();

        if (validatingBuilder.isValid()) {
            SessionConfig useCaseSessionConfig = validatingBuilder.build();
            mCameraControlInternal.setTemplate(useCaseSessionConfig.getTemplateType());
            validatingBuilder.add(mCameraControlInternal.getSessionConfig());

            SessionConfig sessionConfig = validatingBuilder.build();
            mCaptureSession.setSessionConfig(sessionConfig);
        } else {
            mCameraControlInternal.resetTemplate();
            // Always reset the session config if there is no valid session config.
            mCaptureSession.setSessionConfig(mCameraControlInternal.getSessionConfig());
        }
    }

    /**
     * Opens a new capture session.
     *
     * <p>The previously opened session will be safely disposed of before the new session opened.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    @ExecutedBy("mExecutor")
    void openCaptureSession() {
        Preconditions.checkState(mState == InternalState.OPENED);

        ValidatingBuilder validatingBuilder = mUseCaseAttachState.getAttachedBuilder();
        if (!validatingBuilder.isValid()) {
            debugLog("Unable to create capture session due to conflicting configurations");
            return;
        }

        // Checks if capture session is allowed to open in concurrent camera mode.
        if (!mCameraStateRegistry.tryOpenCaptureSession(
                mCameraDevice.getId(),
                mCameraCoordinator.getPairedConcurrentCameraId(mCameraDevice.getId()))) {
            debugLog("Unable to create capture session in camera operating mode = "
                    + mCameraCoordinator.getCameraOperatingMode());
            return;
        }

        Map<DeferrableSurface, Long> streamUseCaseMap = new HashMap<>();
        StreamUseCaseUtil.populateSurfaceToStreamUseCaseMapping(
                mUseCaseAttachState.getAttachedSessionConfigs(),
                streamUseCaseMap, mCameraCharacteristicsCompat, true);

        mCaptureSession.setStreamUseCaseMap(streamUseCaseMap);

        CaptureSessionInterface captureSession = mCaptureSession;
        ListenableFuture<Void> openCaptureSession = captureSession.open(validatingBuilder.build(),
                Preconditions.checkNotNull(mCameraDevice), mCaptureSessionOpenerBuilder.build());

        Futures.addCallback(openCaptureSession, new FutureCallback<Void>() {
            @Override
            @ExecutedBy("mExecutor")
            public void onSuccess(@Nullable Void result) {
                // TODO(b/271182406): Apply the CONFIGURED state to non-concurrent mode.
                if (mCameraCoordinator.getCameraOperatingMode() == CAMERA_OPERATING_MODE_CONCURRENT
                        && mState == InternalState.OPENED) {
                    setState(InternalState.CONFIGURED);
                }
            }

            @Override
            @ExecutedBy("mExecutor")
            public void onFailure(@NonNull Throwable t) {
                if (t instanceof DeferrableSurface.SurfaceClosedException) {
                    SessionConfig sessionConfig =
                            findSessionConfigForSurface(
                                    ((DeferrableSurface.SurfaceClosedException) t)
                                            .getDeferrableSurface());
                    if (sessionConfig != null) {
                        postSurfaceClosedError(sessionConfig);
                    }
                    return;
                }

                // A CancellationException is thrown when (1) A CaptureSession is closed while it
                // is opening. In this case, another CaptureSession should be opened shortly
                // after or (2) When opening a CaptureSession fails.
                // TODO(b/183504720): Distinguish between both scenarios, and communicate the
                //  second one to the developer.
                if (t instanceof CancellationException) {
                    debugLog("Unable to configure camera cancelled");
                    return;
                }

                // Only report camera config error if the camera is open. Ignore otherwise.
                if (mState == InternalState.OPENED) {
                    setState(InternalState.OPENED,
                            CameraState.StateError.create(CameraState.ERROR_STREAM_CONFIG, t));
                }

                if (t instanceof CameraAccessException) {
                    debugLog("Unable to configure camera due to " + t.getMessage());
                } else if (t instanceof TimeoutException) {
                    // TODO: Consider to handle the timeout error.
                    Logger.e(TAG, "Unable to configure camera " + mCameraInfoInternal.getCameraId()
                            + ", timeout!");
                }
            }
        }, mExecutor);
    }

    private boolean isLegacyDevice() {
        Camera2CameraInfoImpl camera2CameraInfo = (Camera2CameraInfoImpl) getCameraInfoInternal();
        return camera2CameraInfo.getSupportedHardwareLevel()
                == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @Nullable
    @ExecutedBy("mExecutor")
    SessionConfig findSessionConfigForSurface(@NonNull DeferrableSurface surface) {
        for (SessionConfig sessionConfig : mUseCaseAttachState.getAttachedSessionConfigs()) {
            if (sessionConfig.getSurfaces().contains(surface)) {
                return sessionConfig;
            }
        }

        return null;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    void postSurfaceClosedError(@NonNull SessionConfig sessionConfig) {
        Executor executor = CameraXExecutors.mainThreadExecutor();
        List<SessionConfig.ErrorListener> errorListeners =
                sessionConfig.getErrorListeners();
        if (!errorListeners.isEmpty()) {
            SessionConfig.ErrorListener errorListener = errorListeners.get(0);
            debugLog("Posting surface closed", new Throwable());
            executor.execute(() -> errorListener.onError(sessionConfig,
                    SessionConfig.SessionError.SESSION_ERROR_SURFACE_NEEDS_RESET));
        }
    }

    /**
     * Replaces the old session with a new session initialized with the old session's configuration.
     *
     * <p>This does not close the previous session. The previous session should be
     * explicitly released before calling this method so the camera can track the state of
     * closing that session.
     */
    @SuppressWarnings({"WeakerAccess", /* synthetic accessor */
            "FutureReturnValueIgnored"})
    @ExecutedBy("mExecutor")
    void resetCaptureSession(boolean abortInFlightCaptures) {
        Preconditions.checkState(mCaptureSession != null);
        debugLog("Resetting Capture Session");
        CaptureSessionInterface oldCaptureSession = mCaptureSession;
        // Recreate an initialized (but not opened) capture session from the previous configuration
        SessionConfig previousSessionConfig = oldCaptureSession.getSessionConfig();
        List<CaptureConfig> unissuedCaptureConfigs = oldCaptureSession.getCaptureConfigs();
        mCaptureSession = newCaptureSession();
        mCaptureSession.setSessionConfig(previousSessionConfig);
        mCaptureSession.issueCaptureRequests(unissuedCaptureConfigs);

        releaseSession(oldCaptureSession, /*abortInFlightCaptures=*/abortInFlightCaptures);
    }

    @ExecutedBy("mExecutor")
    private CameraDevice.StateCallback createDeviceStateCallback() {
        SessionConfig config = mUseCaseAttachState.getAttachedBuilder().build();

        List<CameraDevice.StateCallback> configuredStateCallbacks =
                config.getDeviceStateCallbacks();
        List<CameraDevice.StateCallback> allStateCallbacks =
                new ArrayList<>(configuredStateCallbacks);
        // The CaptureSessionRepository is an internal module of the Camera2CameraImpl, it needs
        // to be updated before Camera2CameraImpl receives the camera status change. Set the
        // state callback for CaptureSessionRepository before the Camera2CameraImpl, so the
        // CaptureSessionRepository can update first.
        allStateCallbacks.add(mCaptureSessionRepository.getCameraStateCallback());
        allStateCallbacks.add(mStateCallback);
        return CameraDeviceStateCallbacks.createComboCallback(allStateCallbacks);
    }

    /**
     * If the {@link CaptureConfig.Builder} hasn't had a surface attached, attaches all valid
     * repeating surfaces to it.
     *
     * @param captureConfigBuilder the configuration builder to attach repeating surfaces.
     * @return true if repeating surfaces have been successfully attached, otherwise false.
     */
    @ExecutedBy("mExecutor")
    private boolean checkAndAttachRepeatingSurface(CaptureConfig.Builder captureConfigBuilder) {
        if (!captureConfigBuilder.getSurfaces().isEmpty()) {
            Logger.w(TAG, "The capture config builder already has surface inside.");
            return false;
        }

        for (SessionConfig sessionConfig :
                mUseCaseAttachState.getActiveAndAttachedSessionConfigs()) {
            // Query the repeating surfaces attached to this use case, then add them to the builder.
            List<DeferrableSurface> surfaces =
                    sessionConfig.getRepeatingCaptureConfig().getSurfaces();
            if (!surfaces.isEmpty()) {
                for (DeferrableSurface surface : surfaces) {
                    captureConfigBuilder.addSurface(surface);
                }
            }
        }

        if (captureConfigBuilder.getSurfaces().isEmpty()) {
            Logger.w(TAG, "Unable to find a repeating surface to attach to CaptureConfig");
            return false;
        }

        return true;
    }

    /** Returns the Camera2CameraControlImpl attached to Camera */
    @NonNull
    @Override
    public CameraControlInternal getCameraControlInternal() {
        return mCameraControlInternal;
    }

    /**
     * Submits capture requests
     *
     * @param captureConfigs capture configuration used for creating CaptureRequest
     */
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @ExecutedBy("mExecutor")
    void submitCaptureRequests(@NonNull List<CaptureConfig> captureConfigs) {
        List<CaptureConfig> captureConfigsWithSurface = new ArrayList<>();
        for (CaptureConfig captureConfig : captureConfigs) {
            // Recreates the Builder to add extra config needed
            CaptureConfig.Builder builder = CaptureConfig.Builder.from(captureConfig);

            if (captureConfig.getTemplateType() == CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG
                    && captureConfig.getCameraCaptureResult() != null) {
                builder.setCameraCaptureResult(captureConfig.getCameraCaptureResult());
            }

            if (captureConfig.getSurfaces().isEmpty() && captureConfig.isUseRepeatingSurface()) {
                // Checks and attaches repeating surface to the request if there's no surface
                // has been already attached. If there's no valid repeating surface to be
                // attached, skip this capture request.
                if (!checkAndAttachRepeatingSurface(builder)) {
                    continue;
                }
            }
            captureConfigsWithSurface.add(builder.build());
        }

        debugLog("Issue capture request");

        mCaptureSession.issueCaptureRequests(captureConfigsWithSurface);
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(Locale.US, "Camera@%x[id=%s]", hashCode(),
                mCameraInfoInternal.getCameraId());
    }

    @NonNull
    static String getUseCaseId(@NonNull UseCase useCase) {
        return useCase.getName() + useCase.hashCode();
    }

    @NonNull
    static String getMeteringRepeatingId(@NonNull MeteringRepeatingSession meteringRepeating) {
        return meteringRepeating.getName() + meteringRepeating.hashCode();
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    void debugLog(@NonNull String msg) {
        debugLog(msg, null);
    }

    private void debugLog(@NonNull String msg, @Nullable Throwable throwable) {
        String msgString = String.format("{%s} %s", toString(), msg);
        Logger.d(TAG, msgString, throwable);
    }

    enum InternalState {
        /**
         * Stable state once the camera has been constructed.
         *
         * <p>At this state the {@link CameraDevice} should be invalid, but threads should be still
         * in a valid state. Whenever a camera device is fully closed the camera should return to
         * this state.
         *
         * <p>After an error occurs the camera returns to this state so that the device can be
         * cleanly reopened.
         */
        INITIALIZED,
        /**
         * Camera is waiting for the camera to be available to open.
         *
         * <p>A camera may enter a pending state if the camera has been stolen by another process
         * or if the maximum number of available cameras is already open.
         *
         * <p>At the end of this state, the camera should move into the OPENING state.
         */
        PENDING_OPEN,
        /**
         * A transitional state where the camera device is currently opening.
         *
         * <p>At the end of this state, the camera should move into either the OPENED or CLOSING
         * state.
         */
        OPENING,
        /**
         * A stable state where the camera has been opened.
         *
         * <p>During this state the camera device should be valid. It is at this time a valid
         * capture session can be active. Capture requests should be issued during this state only.
         */
        OPENED,
        /**
         * A stable state where the camera has been opened and capture session has been configured.
         *
         * <p>It is a state only used in concurrent mode to differentiate from OPENED state for
         * capture session configuration status.
         */
        CONFIGURED,
        /**
         * A transitional state where the camera device is currently closing.
         *
         * <p>At the end of this state, the camera should move into the INITIALIZED state.
         */
        CLOSING,
        /**
         * A transitional state where the camera was previously closing, but not fully closed before
         * a call to open was made.
         *
         * <p>At the end of this state, the camera should move into one of two states. The OPENING
         * state if the device becomes fully closed, since it must restart the process of opening a
         * camera. The OPENED state if the device becomes opened, which can occur if a call to close
         * had been done during the OPENING state.
         */
        REOPENING,
        /**
         * A transitional state where the camera will be closing permanently.
         *
         * <p>At the end of this state, the camera should move into the RELEASED state.
         */
        RELEASING,
        /**
         * A stable state where the camera has been permanently closed.
         *
         * <p>During this state all resources should be released and all operations on the camera
         * will do nothing.
         */
        RELEASED
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @ExecutedBy("mExecutor")
    void setState(@NonNull InternalState state) {
        setState(state, /*stateError=*/null);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @ExecutedBy("mExecutor")
    void setState(@NonNull InternalState state, @Nullable CameraState.StateError stateError) {
        setState(state, stateError, /*notifyImmediately=*/true);
    }

    /**
     * Moves the camera to a new state.
     *
     * @param state             New camera state
     * @param notifyImmediately {@code true} if {@link CameraStateRegistry} should immediately
     *                          notify this camera while updating its state if a camera slot
     *                          becomes available for opening, {@code false} otherwise.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @ExecutedBy("mExecutor")
    void setState(@NonNull InternalState state, @Nullable CameraState.StateError stateError,
            boolean notifyImmediately) {
        debugLog("Transitioning camera internal state: " + mState + " --> " + state);
        mState = state;
        // Convert the internal state to the publicly visible state
        State publicState;
        switch (state) {
            case INITIALIZED:
                publicState = State.CLOSED;
                break;
            case PENDING_OPEN:
                publicState = State.PENDING_OPEN;
                break;
            case OPENING:
            case REOPENING:
                publicState = State.OPENING;
                break;
            case OPENED:
                publicState = State.OPEN;
                break;
            case CONFIGURED:
                publicState = State.CONFIGURED;
                break;
            case CLOSING:
                publicState = State.CLOSING;
                break;
            case RELEASING:
                publicState = State.RELEASING;
                break;
            case RELEASED:
                publicState = State.RELEASED;
                break;
            default:
                throw new IllegalStateException("Unknown state: " + state);
        }
        mCameraStateRegistry.markCameraState(this, publicState, notifyImmediately);
        mObservableState.postValue(publicState);
        mCameraStateMachine.updateState(publicState, stateError);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    static String getErrorMessage(int errorCode) {
        switch (errorCode) {
            case ERROR_NONE:
                return "ERROR_NONE";
            case CameraDevice.StateCallback.ERROR_CAMERA_DEVICE:
                return "ERROR_CAMERA_DEVICE";
            case CameraDevice.StateCallback.ERROR_CAMERA_DISABLED:
                return "ERROR_CAMERA_DISABLED";
            case CameraDevice.StateCallback.ERROR_CAMERA_IN_USE:
                return "ERROR_CAMERA_IN_USE";
            case CameraDevice.StateCallback.ERROR_CAMERA_SERVICE:
                return "ERROR_CAMERA_SERVICE";
            case CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE:
                return "ERROR_MAX_CAMERAS_IN_USE";
            default: // fall out
        }
        return "UNKNOWN ERROR";
    }

    /**
     * Create a {@link UseCaseInfo} object which can provide the immutable use case information.
     *
     * <p>{@link UseCaseInfo} should only contain immutable class to avoid race condition between
     * caller thread and camera thread.
     */
    @AutoValue
    abstract static class UseCaseInfo {
        @NonNull
        static UseCaseInfo create(@NonNull String useCaseId,
                @NonNull Class<?> useCaseType,
                @NonNull SessionConfig sessionConfig,
                @NonNull UseCaseConfig<?> useCaseConfig,
                @Nullable Size surfaceResolution) {
            return new AutoValue_Camera2CameraImpl_UseCaseInfo(useCaseId, useCaseType,
                    sessionConfig, useCaseConfig, surfaceResolution);
        }

        @NonNull
        static UseCaseInfo from(@NonNull UseCase useCase) {
            return create(Camera2CameraImpl.getUseCaseId(useCase), useCase.getClass(),
                    useCase.getSessionConfig(), useCase.getCurrentConfig(),
                    useCase.getAttachedSurfaceResolution());
        }

        @NonNull
        abstract String getUseCaseId();

        @NonNull
        abstract Class<?> getUseCaseType();

        @NonNull
        abstract SessionConfig getSessionConfig();

        @NonNull
        abstract UseCaseConfig<?> getUseCaseConfig();

        @Nullable
        abstract Size getSurfaceResolution();
    }

    @RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
    final class StateCallback extends CameraDevice.StateCallback {
        @CameraExecutor
        private final Executor mExecutor;
        private final ScheduledExecutorService mScheduler;
        private ScheduledReopen mScheduledReopenRunnable;
        @SuppressWarnings("WeakerAccess") // synthetic accessor
        ScheduledFuture<?> mScheduledReopenHandle;
        @NonNull
        private final CameraReopenMonitor mCameraReopenMonitor = new CameraReopenMonitor();

        StateCallback(@NonNull @CameraExecutor Executor executor, @NonNull ScheduledExecutorService
                scheduler) {
            this.mExecutor = executor;
            this.mScheduler = scheduler;
        }

        @Override
        @ExecutedBy("mExecutor")
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            debugLog("CameraDevice.onOpened()");
            mCameraDevice = cameraDevice;
            mCameraDeviceError = ERROR_NONE;
            resetReopenMonitor();
            switch (mState) {
                case CLOSING:
                case RELEASING:
                    // No session should have yet been opened, so close camera directly here.
                    Preconditions.checkState(isSessionCloseComplete());
                    mCameraDevice.close();
                    mCameraDevice = null;
                    break;
                case OPENING:
                case REOPENING:
                    setState(InternalState.OPENED);
                    if (mCameraStateRegistry.tryOpenCaptureSession(
                            cameraDevice.getId(),
                            mCameraCoordinator.getPairedConcurrentCameraId(
                                    mCameraDevice.getId()))) {
                        openCaptureSession();
                    }
                    break;
                default:
                    throw new IllegalStateException(
                            "onOpened() should not be possible from state: " + mState);
            }
        }

        @Override
        @ExecutedBy("mExecutor")
        public void onClosed(@NonNull CameraDevice cameraDevice) {
            debugLog("CameraDevice.onClosed()");
            Preconditions.checkState(mCameraDevice == null,
                    "Unexpected onClose callback on camera device: " + cameraDevice);
            switch (mState) {
                case CLOSING:
                case RELEASING:
                    Preconditions.checkState(isSessionCloseComplete());
                    finishClose();
                    break;
                case REOPENING:
                    if (mCameraDeviceError != ERROR_NONE) {
                        debugLog("Camera closed due to error: " + getErrorMessage(
                                mCameraDeviceError));
                        scheduleCameraReopen();
                    } else {
                        tryOpenCameraDevice(/*fromScheduledCameraReopen=*/false);
                    }
                    break;
                default:
                    throw new IllegalStateException("Camera closed while in state: " + mState);
            }
        }

        @Override
        @ExecutedBy("mExecutor")
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            debugLog("CameraDevice.onDisconnected()");

            // Can be treated the same as camera in use because in both situations the
            // CameraDevice needs to be closed before it can be safely reopened and used.
            onError(cameraDevice, CameraDevice.StateCallback.ERROR_CAMERA_IN_USE);
        }

        @Override
        @ExecutedBy("mExecutor")
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            // onError could be called before onOpened if there is an error opening the camera
            // during initialization, so keep track of it here.
            mCameraDevice = cameraDevice;
            mCameraDeviceError = error;

            switch (mState) {
                case RELEASING:
                case CLOSING:
                    Logger.e(TAG, String.format("CameraDevice.onError(): %s failed with %s while "
                                    + "in %s state. Will finish closing camera.",
                            cameraDevice.getId(), getErrorMessage(error), mState.name()));
                    closeCamera(/*abortInFlightCaptures=*/false);
                    break;
                case OPENING:
                case OPENED:
                case CONFIGURED:
                case REOPENING:
                    Logger.d(TAG, String.format("CameraDevice.onError(): %s failed with %s while "
                                    + "in %s state. Will attempt recovering from error.",
                            cameraDevice.getId(), getErrorMessage(error), mState.name()));
                    handleErrorOnOpen(cameraDevice, error);
                    break;
                default:
                    throw new IllegalStateException(
                            "onError() should not be possible from state: " + mState);
            }
        }

        @ExecutedBy("mExecutor")
        private void handleErrorOnOpen(@NonNull CameraDevice cameraDevice, int error) {
            Preconditions.checkState(
                    mState == InternalState.OPENING || mState == InternalState.OPENED
                            || mState == InternalState.CONFIGURED
                            || mState == InternalState.REOPENING,
                    "Attempt to handle open error from non open state: " + mState);
            switch (error) {
                case CameraDevice.StateCallback.ERROR_CAMERA_DEVICE:
                    // A fatal error occurred. The device should be reopened.
                    // Fall through.
                case CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE:
                case CameraDevice.StateCallback.ERROR_CAMERA_IN_USE:
                    // Attempt to reopen the camera again. If there are no cameras available,
                    // this will wait for the next available camera.
                    Logger.d(TAG, String.format("Attempt to reopen camera[%s] after error[%s]",
                            cameraDevice.getId(), getErrorMessage(error)));
                    reopenCameraAfterError(error);
                    break;
                default:
                    // An irrecoverable error occurred. Close the camera and publish the error
                    // via CameraState so the user can take appropriate action.
                    Logger.e(
                            TAG,
                            "Error observed on open (or opening) camera device "
                                    + cameraDevice.getId()
                                    + ": "
                                    + getErrorMessage(error)
                                    + " closing camera.");

                    int publicErrorCode =
                            error == CameraDevice.StateCallback.ERROR_CAMERA_DISABLED
                                    ? CameraState.ERROR_CAMERA_DISABLED
                                    : CameraState.ERROR_CAMERA_FATAL_ERROR;
                    setState(InternalState.CLOSING, CameraState.StateError.create(publicErrorCode));

                    closeCamera(/*abortInFlightCaptures=*/false);
                    break;
            }
        }

        @ExecutedBy("mExecutor")
        private void reopenCameraAfterError(int error) {
            // After an error, we must close the current camera device before we can open a new
            // one. To accomplish this, we will close the current camera and wait for the
            // onClosed() callback to reopen the device. It is also possible that the device can
            // be closed immediately, so in that case we will open the device manually.
            Preconditions.checkState(mCameraDeviceError != ERROR_NONE,
                    "Can only reopen camera device after error if the camera device is actually "
                            + "in an error state.");

            int publicErrorCode;
            switch (error) {
                case CameraDevice.StateCallback.ERROR_CAMERA_IN_USE:
                    publicErrorCode = CameraState.ERROR_CAMERA_IN_USE;
                    break;
                case CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE:
                    publicErrorCode = CameraState.ERROR_MAX_CAMERAS_IN_USE;
                    break;
                default:
                    publicErrorCode = CameraState.ERROR_OTHER_RECOVERABLE_ERROR;
                    break;
            }
            setState(InternalState.REOPENING, CameraState.StateError.create(publicErrorCode));

            closeCamera(/*abortInFlightCaptures=*/false);
        }

        @ExecutedBy("mExecutor")
        void scheduleCameraReopen() {
            Preconditions.checkState(mScheduledReopenRunnable == null);
            Preconditions.checkState(mScheduledReopenHandle == null);

            if (mCameraReopenMonitor.canScheduleCameraReopen()) {
                mScheduledReopenRunnable = new ScheduledReopen(mExecutor);
                debugLog("Attempting camera re-open in "
                        + mCameraReopenMonitor.getReopenDelayMs() + "ms: "
                        + mScheduledReopenRunnable + " activeResuming = " + mIsActiveResumingMode);
                mScheduledReopenHandle = mScheduler.schedule(mScheduledReopenRunnable,
                        mCameraReopenMonitor.getReopenDelayMs(), TimeUnit.MILLISECONDS);
            } else {
                // TODO(b/174685338): Report camera opening error to the user
                Logger.e(TAG,
                        "Camera reopening attempted for "
                                + mCameraReopenMonitor.getReopenLimitMs()
                                + "ms without success.");

                // Set the state to PENDING_OPEN, so that an attempt to reopen the camera is made if
                // it later becomes available to open, but ignore immediate reopen attempt from
                // CameraStateRegistry.OnOpenAvailableListener.
                setState(InternalState.PENDING_OPEN,
                        /*stateError=*/null,
                        /*notifyImmediately=*/false);
            }
        }

        /**
         * Attempts to cancel reopen.
         *
         * <p>If successful, it is safe to finish closing the camera via {@link #finishClose()} as
         * a reopen will only be scheduled after {@link #onClosed(CameraDevice)} has been called.
         *
         * @return true if reopen was cancelled. False if no re-open was scheduled.
         */
        @ExecutedBy("mExecutor")
        boolean cancelScheduledReopen() {
            boolean cancelled = false;
            if (mScheduledReopenHandle != null) {
                // A reopen has been scheduled
                debugLog("Cancelling scheduled re-open: " + mScheduledReopenRunnable);

                // Ensure the runnable doesn't try to open the camera if it has already
                // been pushed to the executor.
                mScheduledReopenRunnable.cancel();
                mScheduledReopenRunnable = null;

                // Un-schedule the runnable in case if hasn't run.
                mScheduledReopenHandle.cancel(/*mayInterruptIfRunning=*/false);
                mScheduledReopenHandle = null;

                cancelled = true;
            }

            return cancelled;
        }

        /**
         * Resets the camera reopen attempts monitor. This should be called when the camera open is
         * not triggered by a scheduled camera reopen, but rather by an explicit request.
         */
        @ExecutedBy("mExecutor")
        void resetReopenMonitor() {
            mCameraReopenMonitor.reset();
        }

        /**
         * A {@link Runnable} which will attempt to reopen the camera after a scheduled delay.
         */
        class ScheduledReopen implements Runnable {

            @CameraExecutor
            private Executor mExecutor;
            private boolean mCancelled = false;

            ScheduledReopen(@NonNull @CameraExecutor Executor executor) {
                mExecutor = executor;
            }

            void cancel() {
                mCancelled = true;
            }

            @Override
            public void run() {
                mExecutor.execute(() -> {
                    // Scheduled reopen may have been cancelled after execute(). Check to ensure
                    // this is still the scheduled reopen.
                    if (!mCancelled) {
                        Preconditions.checkState(mState == InternalState.REOPENING);
                        if (shouldActiveResume()) {
                            // Ignore the camera availability when in active resuming mode.
                            tryForceOpenCameraDevice(/*fromScheduledCameraReopen*/true);
                        } else {
                            tryOpenCameraDevice(/*fromScheduledCameraReopen=*/true);
                        }
                    }
                });
            }
        }

        /**
         * Enables active resume only when camera is stolen by other apps.
         * ERROR_CAMERA_IN_USE: The same camera id is occupied.
         * ERROR_MAX_CAMERAS_IN_USE: when other app is opening camera but with different camera id.
         */
        boolean shouldActiveResume() {
            return mIsActiveResumingMode && (mCameraDeviceError == ERROR_CAMERA_IN_USE
                    || mCameraDeviceError == ERROR_MAX_CAMERAS_IN_USE);
        }

        /**
         * Keeps track of camera reopen attempts in order to limit them.
         *
         * When in active resuming mode, it will periodically retry opening the camera regardless
         * of the camera availability.
         * Elapsed time <= 2 minutes -> retry once per 1 second.
         * Elapsed time 2 to 5 minutes -> retry once per 2 seconds.
         * Elapsed time > 5 minutes -> retry once per 4 seconds.
         * Retry will stop after 30 minutes.
         *
         * When not in active resuming mode, it will reopen in every 700ms within the 10 seconds
         * limit. However, if the camera is unavailable the retry will stop immediately until it
         * becomes available.
         */
        class CameraReopenMonitor {
            // Delay long enough to guarantee the app could have been backgrounded.
            // See ProcessLifecycleOwner for where this delay comes from.
            static final int REOPEN_DELAY_MS = 700;
            // Time limit since the first camera reopen attempt after which reopening the camera
            // should no longer be attempted.
            static final int REOPEN_LIMIT_MS = 10_000;
            static final int ACTIVE_REOPEN_DELAY_BASE_MS = 1000;
            static final int ACTIVE_REOPEN_LIMIT_MS = 30 * 60 * 1000; // 30 minutes
            static final int INVALID_TIME = -1;
            private long mFirstReopenTime = INVALID_TIME;

            int getReopenDelayMs() {
                if (!shouldActiveResume()) {
                    return REOPEN_DELAY_MS;
                } else {
                    long elapsedTime = getElapsedTime();
                    if (elapsedTime <= 2 * 60 * 1000) { // <= 2 minutes
                        return ACTIVE_REOPEN_DELAY_BASE_MS;
                    } else if (elapsedTime <= 5 * 60 * 1000) { // <= 5 minutes
                        return ACTIVE_REOPEN_DELAY_BASE_MS * 2;
                    } else { // > 5 minutes
                        return ACTIVE_REOPEN_DELAY_BASE_MS * 4;
                    }
                }
            }

            int getReopenLimitMs() {
                if (!shouldActiveResume()) {
                    return REOPEN_LIMIT_MS;
                } else {
                    return ACTIVE_REOPEN_LIMIT_MS;
                }
            }

            long getElapsedTime() {
                final long now = SystemClock.uptimeMillis();
                // If it's the first attempt to reopen the camera
                if (mFirstReopenTime == INVALID_TIME) {
                    mFirstReopenTime = now;
                }

                return now - mFirstReopenTime;
            }

            boolean canScheduleCameraReopen() {
                final boolean hasReachedLimit = getElapsedTime() >= getReopenLimitMs();

                // If the limit has been reached, prevent further attempts to reopen the camera,
                // and reset [firstReopenTime].
                if (hasReachedLimit) {
                    reset();
                    return false;
                }

                return true;
            }

            void reset() {
                mFirstReopenTime = INVALID_TIME;
            }
        }
    }

    /**
     * A class that listens to signals to determine whether a camera with a particular id is
     * available for opening.
     */
    final class CameraAvailability extends CameraManager.AvailabilityCallback implements
            CameraStateRegistry.OnOpenAvailableListener {
        private final String mCameraId;

        /**
         * Availability as reported by the AvailabilityCallback. If this is true then the camera
         * is available for open. If this is false, either another process holds the camera or
         * this process. Potentially held by the Camera that is holding this instance of
         * CameraAvailability.
         */
        private boolean mCameraAvailable = true;

        CameraAvailability(String cameraId) {
            mCameraId = cameraId;
        }

        @Override
        @ExecutedBy("mExecutor")
        public void onCameraAvailable(@NonNull String cameraId) {
            if (!mCameraId.equals(cameraId)) {
                // Ignore availability for other cameras
                return;
            }

            mCameraAvailable = true;

            if (mState == InternalState.PENDING_OPEN) {
                tryOpenCameraDevice(/*fromScheduledCameraReopen=*/false);
            }
        }

        @Override
        @ExecutedBy("mExecutor")
        public void onCameraUnavailable(@NonNull String cameraId) {
            if (!mCameraId.equals(cameraId)) {
                // Ignore availability for other cameras
                return;
            }

            mCameraAvailable = false;
        }

        @Override
        @ExecutedBy("mExecutor")
        public void onOpenAvailable() {
            if (mState == InternalState.PENDING_OPEN) {
                tryOpenCameraDevice(/*fromScheduledCameraReopen=*/false);
            }
        }

        /**
         * True if a camera is potentially available.
         */
        @ExecutedBy("mExecutor")
        boolean isCameraAvailable() {
            return mCameraAvailable;
        }
    }

    final class CameraConfigureAvailable
            implements CameraStateRegistry.OnConfigureAvailableListener {

        @Override
        public void onConfigureAvailable() {
            if (mState == InternalState.OPENED) {
                openCaptureSession();
            }
        }
    }

    final class ControlUpdateListenerInternal implements
            CameraControlInternal.ControlUpdateCallback {

        @ExecutedBy("mExecutor")
        @Override
        public void onCameraControlUpdateSessionConfig() {
            updateCaptureSessionConfig();
        }

        @ExecutedBy("mExecutor")
        @Override
        public void onCameraControlCaptureRequests(@NonNull List<CaptureConfig> captureConfigs) {
            submitCaptureRequests(Preconditions.checkNotNull(captureConfigs));
        }
    }
}
