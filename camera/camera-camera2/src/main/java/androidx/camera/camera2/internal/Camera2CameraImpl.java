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

import android.annotation.SuppressLint;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.camera.camera2.internal.annotation.CameraExecutor;
import androidx.camera.camera2.internal.compat.CameraAccessExceptionCompat;
import androidx.camera.camera2.internal.compat.CameraManagerCompat;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraUnavailableException;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCase;
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
import androidx.camera.core.impl.UseCaseAttachState;
import androidx.camera.core.impl.annotation.ExecutedBy;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.FutureChain;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
final class Camera2CameraImpl implements CameraInternal {
    private static final String TAG = "Camera2CameraImpl";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);
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
    /** The camera control shared across all use cases bound to this Camera. */
    private final Camera2CameraControl mCameraControlInternal;
    private final StateCallback mStateCallback;
    /** Information about the characteristics of this camera */
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @NonNull
    final CameraInfoInternal mCameraInfoInternal;
    /** The handle to the opened camera. */
    @Nullable
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    CameraDevice mCameraDevice;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    int mCameraDeviceError = ERROR_NONE;

    private CaptureSession.Builder mCaptureSessionBuilder = new CaptureSession.Builder();

    /** The configured session which handles issuing capture requests. */
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    CaptureSession mCaptureSession;
    /** The session configuration of camera control. */
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    SessionConfig mCameraControlSessionConfig = SessionConfig.defaultEmptySessionConfig();

    // Used to debug number of requests to release camera
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final AtomicInteger mReleaseRequestCount = new AtomicInteger(0);
    // Should only be accessed on code executed by mExecutor
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    ListenableFuture<Void> mUserReleaseFuture;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    CallbackToFutureAdapter.Completer<Void> mUserReleaseNotifier;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final Map<CaptureSession, ListenableFuture<Void>> mReleasedCaptureSessions =
            new LinkedHashMap<>();

    private final CameraAvailability mCameraAvailability;
    private final CameraStateRegistry mCameraStateRegistry;

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final Set<CaptureSession> mConfiguringForClose = new HashSet<>();

    // The metering repeating use case for ImageCapture only case.
    private MeteringRepeating mMeteringRepeating;

    /**
     * Constructor for a camera.
     *
     * @param cameraManager       the camera service used to retrieve a camera
     * @param cameraId            the name of the camera as defined by the camera service
     * @param cameraStateRegistry An registry used to track the state of multiple cameras.
     *                            Used as a fence to ensure the number of simultaneously
     *                            opened cameras is limited.
     * @param executor            the executor for on which all camera operations run
     * @throws CameraUnavailableException if the {@link CameraCharacteristics} is unavailable. This
     *                                    could occur if the camera was disconnected.
     */
    Camera2CameraImpl(@NonNull CameraManagerCompat cameraManager,
            @NonNull String cameraId,
            @NonNull CameraStateRegistry cameraStateRegistry,
            @NonNull Executor executor,
            @NonNull Handler schedulerHandler) throws CameraUnavailableException {
        mCameraManager = cameraManager;
        mCameraStateRegistry = cameraStateRegistry;
        ScheduledExecutorService executorScheduler =
                CameraXExecutors.newHandlerExecutor(schedulerHandler);
        mExecutor = CameraXExecutors.newSequentialExecutor(executor);
        mStateCallback = new StateCallback(mExecutor, executorScheduler);
        mUseCaseAttachState = new UseCaseAttachState(cameraId);
        mObservableState.postValue(State.CLOSED);

        try {
            CameraCharacteristics cameraCharacteristics =
                    mCameraManager.getCameraCharacteristics(cameraId);
            mCameraControlInternal = new Camera2CameraControl(cameraCharacteristics,
                    executorScheduler, mExecutor, new ControlUpdateListenerInternal());
            mCameraInfoInternal = new Camera2CameraInfoImpl(
                    cameraId,
                    cameraCharacteristics,
                    mCameraControlInternal);
            Camera2CameraInfoImpl camera2CameraInfo = (Camera2CameraInfoImpl) mCameraInfoInternal;
            mCaptureSessionBuilder.setSupportedHardwareLevel(
                    camera2CameraInfo.getSupportedHardwareLevel());
        } catch (CameraAccessExceptionCompat e) {
            throw CameraUnavailableExceptionHelper.createFrom(e);
        }
        mCaptureSessionBuilder.setExecutor(mExecutor);
        mCaptureSessionBuilder.setCompatHandler(schedulerHandler);
        mCaptureSessionBuilder.setScheduledExecutorService(executorScheduler);
        mCaptureSession = mCaptureSessionBuilder.build();

        mCameraAvailability = new CameraAvailability(cameraId);

        // Register an observer to update the number of available cameras
        mCameraStateRegistry.registerCamera(this, mExecutor, mCameraAvailability);
        mCameraManager.registerAvailabilityCallback(mExecutor, mCameraAvailability);
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
                openCameraDevice();
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

    // Configure the camera with a dummy capture session in order to clear the
    // previous session. This should be released immediately after being configured.
    @ExecutedBy("mExecutor")
    private void configAndClose(boolean abortInFlightCaptures) {

        final CaptureSession dummySession = mCaptureSessionBuilder.build();

        mConfiguringForClose.add(dummySession);  // Make mCameraDevice is not closed and existed.
        resetCaptureSession(abortInFlightCaptures);

        final SurfaceTexture surfaceTexture = new SurfaceTexture(0);
        surfaceTexture.setDefaultBufferSize(640, 480);
        final Surface surface = new Surface(surfaceTexture);
        final Runnable closeAndCleanupRunner = () -> {
            surface.release();
            surfaceTexture.release();
        };

        SessionConfig.Builder builder = new SessionConfig.Builder();
        builder.addNonRepeatingSurface(new ImmediateSurface(surface));
        builder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
        debugLog("Start configAndClose.");
        ListenableFuture<Void> openDummyCaptureSession = dummySession.open(builder.build(),
                Preconditions.checkNotNull(mCameraDevice));
        Futures.addCallback(openDummyCaptureSession, new FutureCallback<Void>() {
            @Override
            @ExecutedBy("mExecutor")
            public void onSuccess(@Nullable Void result) {
                closeStaleCaptureSessions(dummySession);

                // Release the dummy Session and continue closing camera when in correct state.
                releaseDummySession(dummySession, closeAndCleanupRunner);
            }

            @Override
            @ExecutedBy("mExecutor")
            public void onFailure(Throwable t) {
                debugLog("Unable to configure camera due to " + t.getMessage());

                // Release the dummy Session and continue closing camera when in correct state.
                releaseDummySession(dummySession, closeAndCleanupRunner);
            }
        }, mExecutor);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @ExecutedBy("mExecutor")
    void releaseDummySession(CaptureSession dummySession, Runnable closeAndCleanupRunner) {
        // Config complete and remove the dummySession from the mConfiguringForClose map
        // after resetCaptureSession and before release the dummySession.
        mConfiguringForClose.remove(dummySession);

        // Don't need to abort captures since there are none submitted for this session.
        ListenableFuture<Void> releaseFuture = releaseSession(
                dummySession, /*abortInFlightCaptures=*/false);

        // Add a listener to clear the dummy surfaces
        releaseFuture.addListener(closeAndCleanupRunner, CameraXExecutors.directExecutor());
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
                setState(InternalState.RELEASING);
                closeCamera(/*abortInFlightCaptures=*/true);
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
    ListenableFuture<Void> releaseSession(@NonNull final CaptureSession captureSession,
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
                            mCameraDevice.close();
                            mCameraDevice = null;
                        }
                        break;
                    default:
                        // Ignore all other states
                }
            }

            @ExecutedBy("mExecutor")
            @Override
            public void onFailure(Throwable t) {
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
        mExecutor.execute(() -> {
            debugLog("Use case " + useCase + " ACTIVE");

            // TODO(b/150208070)Race condition where onUseCaseActive can be called, even after a
            //  UseCase has been unbound. The try-catch is to retain existing behavior where an
            //  unbound UseCase is silently ignored.
            try {
                mUseCaseAttachState.setUseCaseActive(useCase);
                mUseCaseAttachState.updateUseCase(useCase);
                updateCaptureSessionConfig();
            } catch (NullPointerException e) {
                debugLog("Failed to set already detached use case active");
            }
        });
    }


    /** Removes the use case from a state of issuing capture requests. */
    @Override
    public void onUseCaseInactive(@NonNull UseCase useCase) {
        Preconditions.checkNotNull(useCase);
        mExecutor.execute(() -> {
            debugLog("Use case " + useCase + " INACTIVE");
            mUseCaseAttachState.setUseCaseInactive(useCase);
            updateCaptureSessionConfig();
        });
    }

    /** Updates the capture requests based on the latest settings. */
    @Override
    public void onUseCaseUpdated(@NonNull UseCase useCase) {
        Preconditions.checkNotNull(useCase);
        mExecutor.execute(() -> {
            debugLog("Use case " + useCase + " UPDATED");
            mUseCaseAttachState.updateUseCase(useCase);
            updateCaptureSessionConfig();
        });
    }

    @Override
    public void onUseCaseReset(@NonNull UseCase useCase) {
        Preconditions.checkNotNull(useCase);
        mExecutor.execute(() -> {
            debugLog("Use case " + useCase + " RESET");
            mUseCaseAttachState.updateUseCase(useCase);

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
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.TESTS)
    boolean isUseCaseAttached(@NonNull UseCase useCase) {
        try {
            return CallbackToFutureAdapter.<Boolean>getFuture(completer -> {
                try {
                    mExecutor.execute(
                            () -> completer.set(mUseCaseAttachState.isUseCaseAttached(useCase)));
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

    /**
     * Sets the use case to be in the state where the capture session will be configured to handle
     * capture requests from the use case.
     */
    @Override
    public void attachUseCases(@NonNull Collection<UseCase> useCases) {
        if (!useCases.isEmpty()) {
            mCameraControlInternal.setActive(true);
            mExecutor.execute(() -> tryAttachUseCases(useCases));
        }
    }

    // Attempts to make use attach if they are not already attached.
    @ExecutedBy("mExecutor")
    private void tryAttachUseCases(@NonNull Collection<UseCase> toAdd) {
        // Figure out which use cases are not already attached and add them.
        List<UseCase> useCasesToAttach = new ArrayList<>();
        for (UseCase useCase : toAdd) {
            if (!mUseCaseAttachState.isUseCaseAttached(useCase)) {
                // TODO(b/150208070): Race condition where onUseCaseActive can be called, even
                //  after a UseCase has been unbound. The try-catch is to retain existing behavior
                //  where an unbound UseCase is silently ignored.
                try {
                    mUseCaseAttachState.setUseCaseAttached(useCase);

                    useCasesToAttach.add(useCase);
                } catch (NullPointerException e) {
                    debugLog("Failed to attach a detached use case");
                }
            }
        }

        if (useCasesToAttach.isEmpty()) {
            return;
        }

        debugLog("Use cases [" + TextUtils.join(", ", useCasesToAttach) + "] now ATTACHED");

        notifyStateAttachedToUseCases(useCasesToAttach);

        // Check if need to add or remove MeetingRepeatingUseCase.
        addOrRemoveMeteringRepeatingUseCase();

        updateCaptureSessionConfig();
        resetCaptureSession(/*abortInFlightCaptures=*/false);

        if (mState == InternalState.OPENED) {
            openCaptureSession();
        } else {
            openInternal();
        }

        updateCameraControlPreviewAspectRatio(useCasesToAttach);
    }

    private void notifyStateAttachedToUseCases(List<UseCase> useCases) {
        CameraXExecutors.mainThreadExecutor().execute(() -> {
            for (UseCase useCase : useCases) {
                useCase.onStateAttached();
            }
        });
    }

    private void notifyStateDetachedToUseCases(List<UseCase> useCases) {
        CameraXExecutors.mainThreadExecutor().execute(() -> {
            for (UseCase useCase : useCases) {
                useCase.onStateDetached();
            }
        });
    }

    @ExecutedBy("mExecutor")
    private void updateCameraControlPreviewAspectRatio(Collection<UseCase> useCases) {
        for (UseCase useCase : useCases) {
            if (useCase instanceof Preview) {
                Size resolution =
                        Preconditions.checkNotNull(useCase.getAttachedSurfaceResolution());
                Rational aspectRatio = new Rational(resolution.getWidth(), resolution.getHeight());
                mCameraControlInternal.setPreviewAspectRatio(aspectRatio);
                return;
            }
        }
    }

    @ExecutedBy("mExecutor")
    private void clearCameraControlPreviewAspectRatio(Collection<UseCase> removedUseCases) {
        for (UseCase useCase : removedUseCases) {
            if (useCase instanceof Preview) {
                mCameraControlInternal.setPreviewAspectRatio(null);
                return;
            }
        }
    }

    /**
     * Removes the use case to be in the state where the capture session will be configured to
     * handle capture requests from the use case.
     */
    @Override
    public void detachUseCases(@NonNull Collection<UseCase> useCases) {
        if (!useCases.isEmpty()) {
            mExecutor.execute(() -> tryDetachUseCases(useCases));
        }
    }

    // Attempts to make detach UseCases if they are attached.
    @ExecutedBy("mExecutor")
    private void tryDetachUseCases(@NonNull Collection<UseCase> toRemove) {
        List<UseCase> useCasesToDetach = new ArrayList<>();
        for (UseCase useCase : toRemove) {
            if (mUseCaseAttachState.isUseCaseAttached(useCase)) {
                mUseCaseAttachState.setUseCaseDetached(useCase);
                useCasesToDetach.add(useCase);
            }
        }

        if (useCasesToDetach.isEmpty()) {
            return;
        }

        debugLog("Use cases [" + TextUtils.join(", ", useCasesToDetach)
                + "] now DETACHED for camera");
        clearCameraControlPreviewAspectRatio(useCasesToDetach);

        notifyStateDetachedToUseCases(useCasesToDetach);

        // Check if need to add or remove MeetingRepeatingUseCase.
        addOrRemoveMeteringRepeatingUseCase();

        boolean allUseCasesDetached = mUseCaseAttachState.getAttachedUseCases().isEmpty();
        if (allUseCasesDetached) {
            mCameraControlInternal.setActive(false);
            resetCaptureSession(/*abortInFlightCaptures=*/false);
            // If all detached, manual nullify session config to avoid
            // memory leak. See: https://issuetracker.google.com/issues/141188637
            mCaptureSession = mCaptureSessionBuilder.build();
            closeInternal();
        } else {
            updateCaptureSessionConfig();
            resetCaptureSession(/*abortInFlightCaptures=*/false);

            if (mState == InternalState.OPENED) {
                openCaptureSession();
            }
        }
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
                if (mMeteringRepeating == null) {
                    mMeteringRepeating = new MeteringRepeating(this);
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
                    Log.d(TAG, "mMeteringRepeating is ATTACHED, "
                            + "SessionConfig Surfaces: " + sizeSessionSurfaces + ", "
                            + "CaptureConfig Surfaces: " + sizeRepeatingSurfaces);
                }
            }
        }
    }

    private  void removeMeteringRepeating() {
        if (mMeteringRepeating != null) {
            mUseCaseAttachState.setUseCaseDetached(mMeteringRepeating);
            notifyStateDetachedToUseCases(Arrays.asList(mMeteringRepeating));
            mMeteringRepeating.clear();
            mMeteringRepeating = null;
        }
    }

    private  void addMeteringRepeating() {
        if (mMeteringRepeating != null) {
            mUseCaseAttachState.setUseCaseAttached(mMeteringRepeating);
            notifyStateAttachedToUseCases(Arrays.asList(mMeteringRepeating));
        }
    }

    /** Returns an interface to retrieve characteristics of the camera. */
    @NonNull
    @Override
    public CameraInfoInternal getCameraInfoInternal() {
        return mCameraInfoInternal;
    }

    /** Opens the camera device */
    // TODO(b/124268878): Handle SecurityException and require permission in manifest.
    @SuppressLint("MissingPermission")
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @ExecutedBy("mExecutor")
    void openCameraDevice() {
        mStateCallback.cancelScheduledReopen();
        // Check that we have an available camera to open here before attempting
        // to open the camera again.
        if (!mCameraAvailability.isCameraAvailable() || !mCameraStateRegistry.tryOpenCamera(this)) {
            debugLog("No cameras available. Waiting for available camera before opening camera.");
            setState(InternalState.PENDING_OPEN);
            return;
        } else {
            setState(InternalState.OPENING);
        }

        debugLog("Opening camera.");

        try {
            mCameraManager.openCamera(mCameraInfoInternal.getCameraId(), mExecutor,
                    createDeviceStateCallback());
        } catch (CameraAccessExceptionCompat e) {
            debugLog("Unable to open camera due to " + e.getMessage());
            switch (e.getReason()) {
                case CameraAccessExceptionCompat.CAMERA_UNAVAILABLE_DO_NOT_DISTURB:
                    // Camera2 is unable to call the onError() callback for this case. It has to
                    // reset the state here.
                    setState(InternalState.INITIALIZED);
                    break;
                default:
                    // Camera2 will call the onError() callback with the specific error code that
                    // caused this failure. No need to do anything here.
            }
        }
    }

    /** Updates the capture request configuration for the current capture session. */
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @ExecutedBy("mExecutor")
    void updateCaptureSessionConfig() {
        ValidatingBuilder validatingBuilder = mUseCaseAttachState.getActiveAndAttachedBuilder();

        if (validatingBuilder.isValid()) {
            // Apply CameraControlInternal's SessionConfig to let CameraControlInternal be able
            // to control Repeating Request and process results.
            validatingBuilder.add(mCameraControlSessionConfig);

            SessionConfig sessionConfig = validatingBuilder.build();
            mCaptureSession.setSessionConfig(sessionConfig);
        }
    }

    /**
     * Opens a new capture session.
     *
     * <p>The previously opened session will be safely disposed of before the new session opened.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @ExecutedBy("mExecutor")
    void openCaptureSession() {
        Preconditions.checkState(mState == InternalState.OPENED);

        ValidatingBuilder validatingBuilder = mUseCaseAttachState.getAttachedBuilder();
        if (!validatingBuilder.isValid()) {
            debugLog("Unable to create capture session due to conflicting configurations");
            return;
        }

        CaptureSession captureSession = mCaptureSession;
        ListenableFuture<Void> openCaptureSession;

        if (!isLegacyDevice()) {
            openCaptureSession = captureSession.open(validatingBuilder.build(),
                    Preconditions.checkNotNull(mCameraDevice));
        } else {
            // Opening and releasing the capture session quickly and constantly is a problem for
            // LEGACY devices. See: b/146773463. It needs to check all the releasing capture
            // sessions are ready for opening next capture session.
            List<ListenableFuture<Void>> futureList = new ArrayList<>();
            for (CaptureSession releasedSession : mReleasedCaptureSessions.keySet()) {
                futureList.add(releasedSession.getStartStreamingFuture());
            }

            openCaptureSession = FutureChain.from(
                    Futures.successfulAsList(futureList)).transformAsync(v -> {
                        // To close the camera, create the new CaptureSession or receive camera
                        // error will release the previous CaptureSession. If the state of
                        // CaptureSession is released, it is mean there are multiple CaptureSession
                        // actions while waiting for a list of futures to complete. Then only the
                        // last CaptureSession that we create should actually open a CaptureSession.
                        if (captureSession.getState() == CaptureSession.State.RELEASED) {
                            return Futures.immediateFailedFuture(new CancellationException(
                                    "The capture session has been released before."));
                        } else {
                            // The camera state should be opened. Otherwise, this CaptureSession
                            // should be released.
                            Preconditions.checkState(mState == InternalState.OPENED);
                            return captureSession.open(validatingBuilder.build(),
                                    Preconditions.checkNotNull(mCameraDevice));
                        }
                    }, mExecutor);
        }

        Futures.addCallback(openCaptureSession, new FutureCallback<Void>() {
            @Override
            @ExecutedBy("mExecutor")
            public void onSuccess(@Nullable Void result) {
                closeStaleCaptureSessions(captureSession);
            }

            @Override
            @ExecutedBy("mExecutor")
            public void onFailure(Throwable t) {
                if (t instanceof CameraAccessException) {
                    debugLog("Unable to configure camera due to " + t.getMessage());
                } else if (t instanceof CancellationException) {
                    debugLog("Unable to configure camera cancelled");
                } else if (t instanceof DeferrableSurface.SurfaceClosedException) {
                    UseCase useCase =
                            findUseCaseForSurface(
                                    ((DeferrableSurface.SurfaceClosedException) t)
                                            .getDeferrableSurface());
                    if (useCase != null) {
                        postSurfaceClosedError(useCase);
                    }
                } else if (t instanceof TimeoutException) {
                    // TODO: Consider to handle the timeout error.
                    Log.e(TAG, "Unable to configure camera " + mCameraInfoInternal.getCameraId()
                            + ", timeout!");
                } else {
                    // Throw the unexpected error.
                    throw new RuntimeException(t);
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
    @ExecutedBy("mExecutor")
    void closeStaleCaptureSessions(CaptureSession captureSession) {
        // Once the new CameraCaptureSession is created, the under closing
        // CameraCaptureSession can be treated as closed (more detail in b/144817309).
        // Trigger the CaptureSession#forceClose() to finish the session release flow.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            CaptureSession[] captureSessions = mReleasedCaptureSessions.keySet().toArray(
                    new CaptureSession[0]);
            for (CaptureSession releasingSession : captureSessions) {
                // The new created CaptureSession might going to release before the previous
                // CameraCaptureSession is configured.
                // The code in this section would like to mark the previous CaptureSession to Closed
                // state if a new CaptureSession is configured. So we only force close the capture
                // session that created before the current configured session instance.
                if (captureSession == releasingSession) {
                    break;
                }
                releasingSession.forceClose();
            }
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @Nullable
    @ExecutedBy("mExecutor")
    UseCase findUseCaseForSurface(@NonNull DeferrableSurface surface) {
        for (UseCase useCase : mUseCaseAttachState.getAttachedUseCases()) {
            SessionConfig sessionConfig = Preconditions.checkNotNull(useCase.getSessionConfig());
            if (sessionConfig.getSurfaces().contains(surface)) {
                return useCase;
            }
        }

        return null;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    void postSurfaceClosedError(@NonNull UseCase useCase) {
        Executor executor = CameraXExecutors.mainThreadExecutor();
        SessionConfig sessionConfigError = Preconditions.checkNotNull(useCase.getSessionConfig());
        List<SessionConfig.ErrorListener> errorListeners =
                sessionConfigError.getErrorListeners();
        if (!errorListeners.isEmpty()) {
            SessionConfig.ErrorListener errorListener = errorListeners.get(0);
            debugLog("Posting surface closed", new Throwable());
            executor.execute(() -> errorListener.onError(sessionConfigError,
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
        CaptureSession oldCaptureSession = mCaptureSession;
        // Recreate an initialized (but not opened) capture session from the previous configuration
        SessionConfig previousSessionConfig = oldCaptureSession.getSessionConfig();
        List<CaptureConfig> unissuedCaptureConfigs = oldCaptureSession.getCaptureConfigs();
        mCaptureSession = mCaptureSessionBuilder.build();
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
            Log.w(TAG, "The capture config builder already has surface inside.");
            return false;
        }

        Collection<UseCase> activeUseCases = mUseCaseAttachState.getActiveAndAttachedUseCases();

        for (UseCase useCase : activeUseCases) {
            SessionConfig sessionConfig = Preconditions.checkNotNull(useCase.getSessionConfig());
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
            Log.w(TAG, "Unable to find a repeating surface to attach to CaptureConfig");
            return false;
        }

        return true;
    }

    /** Returns the Camera2CameraControl attached to Camera */
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

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    void debugLog(@NonNull String msg) {
        debugLog(msg, null);
    }

    private void debugLog(@NonNull String msg, @Nullable Throwable throwable) {
        if (DEBUG) {
            String msgString = String.format("{%s} %s", toString(), msg);
            if (throwable == null) {
                Log.d(TAG, msgString);
            } else {
                Log.d(TAG, msgString, throwable);
            }
        }
    }

    @NonNull
    @Override
    public CameraControl getCameraControl() {
        return getCameraControlInternal();
    }

    @NonNull
    @Override
    public CameraInfo getCameraInfo() {
        return getCameraInfoInternal();
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
        mCameraStateRegistry.markCameraState(this, publicState);
        mObservableState.postValue(publicState);
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

    final class StateCallback extends CameraDevice.StateCallback {

        // Delay long enough to guarantee the app could have been backgrounded.
        // See ProcessLifecycleProvider for where this delay comes from.
        private static final int REOPEN_DELAY_MS = 700;

        @CameraExecutor
        private final Executor mExecutor;
        private final ScheduledExecutorService mScheduler;
        private ScheduledReopen mScheduledReopenRunnable;
        @SuppressWarnings("WeakerAccess") /* synthetic accessor */
                ScheduledFuture<?> mScheduledReopenHandle;

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

            // CameraControl needs CaptureRequest.Builder to get default capture request options.
            updateDefaultRequestBuilderToCameraControl(cameraDevice);

            mCameraDeviceError = ERROR_NONE;
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
                    openCaptureSession();
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
                        Preconditions.checkState(mScheduledReopenRunnable == null);
                        Preconditions.checkState(mScheduledReopenHandle == null);
                        mScheduledReopenRunnable = new ScheduledReopen(mExecutor);
                        debugLog(
                                "Camera closed due to error: " + getErrorMessage(mCameraDeviceError)
                                        + ". Attempting re-open in " + REOPEN_DELAY_MS + "ms: "
                                        + mScheduledReopenRunnable);
                        mScheduledReopenHandle = mScheduler.schedule(mScheduledReopenRunnable,
                                REOPEN_DELAY_MS,
                                TimeUnit.MILLISECONDS);
                    } else {
                        openCameraDevice();
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

            // Need to force close the CaptureSessions, because onDisconnected () callback causes
            // condition where CameraCaptureSession won't receive the onClosed() callback. See
            // b/140955560 for more detail.
            for (CaptureSession captureSession : mReleasedCaptureSessions.keySet()) {
                captureSession.forceClose();
            }

            mCaptureSession.forceClose();

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
                    Log.e(
                            TAG,
                            "CameraDevice.onError(): "
                                    + cameraDevice.getId()
                                    + " with error: "
                                    + getErrorMessage(error));
                    closeCamera(/*abortInFlightCaptures=*/false);
                    break;
                case OPENING:
                case OPENED:
                case REOPENING:
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
                    reopenCameraAfterError();
                    break;
                default:
                    // TODO: Properly handle other errors. For now, we will close the camera.
                    Log.e(
                            TAG,
                            "Error observed on open (or opening) camera device "
                                    + cameraDevice.getId()
                                    + ": "
                                    + getErrorMessage(error));
                    setState(InternalState.CLOSING);
                    closeCamera(/*abortInFlightCaptures=*/false);
                    break;
            }
        }

        @ExecutedBy("mExecutor")
        private void reopenCameraAfterError() {
            // After an error, we must close the current camera device before we can open a new
            // one. To accomplish this, we will close the current camera and wait for the
            // onClosed() callback to reopen the device. It is also possible that the device can
            // be closed immediately, so in that case we will open the device manually.
            Preconditions.checkState(mCameraDeviceError != ERROR_NONE,
                    "Can only reopen camera device after error if the camera device is actually "
                            + "in an error state.");
            setState(InternalState.REOPENING);
            closeCamera(/*abortInFlightCaptures=*/false);
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
                        openCameraDevice();
                    }
                });
            }
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    void updateDefaultRequestBuilderToCameraControl(@NonNull CameraDevice cameraDevice) {
        try {
            int templateType = mCameraControlInternal.getDefaultTemplate();
            CaptureRequest.Builder builder =
                    cameraDevice.createCaptureRequest(templateType);
            mCameraControlInternal.setDefaultRequestBuilder(builder);
        } catch (CameraAccessException e) {
            Log.e(TAG, "fail to create capture request.", e);
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
                openCameraDevice();
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
                openCameraDevice();
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

    final class ControlUpdateListenerInternal implements
            CameraControlInternal.ControlUpdateCallback {

        @ExecutedBy("mExecutor")
        @Override
        public void onCameraControlUpdateSessionConfig(@NonNull SessionConfig sessionConfig) {
            mCameraControlSessionConfig = Preconditions.checkNotNull(sessionConfig);
            updateCaptureSessionConfig();
        }

        @ExecutedBy("mExecutor")
        @Override
        public void onCameraControlCaptureRequests(@NonNull List<CaptureConfig> captureConfigs) {
            submitCaptureRequests(Preconditions.checkNotNull(captureConfigs));
        }
    }
}
