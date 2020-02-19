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
import androidx.camera.camera2.internal.compat.CameraManagerCompat;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCase;
import androidx.camera.core.impl.CameraControlInternal;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.CameraInternal;
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
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
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
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A camera which is controlled by the change of state in use cases.
 *
 * <p>The camera needs to be in an open state in order for use cases to control the camera. Whenever
 * there is a non-zero number of use cases in the online state the camera will either have a capture
 * session open or be in the process of opening up one. If the number of uses cases in the online
 * state changes then the capture session will be reconfigured.
 *
 * <p>Capture requests will be issued only for use cases which are in both the online and active
 * state.
 */
final class Camera2CameraImpl implements CameraInternal {
    private static final String TAG = "Camera";
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
    private final StateCallback mStateCallback = new StateCallback();
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

    private final Observable<Integer> mAvailableCamerasObservable;
    private final CameraAvailability mCameraAvailability;

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final Set<CaptureSession> mConfiguringForClose = new HashSet<>();

    /**
     * Constructor for a camera.
     *
     * @param cameraManager              the camera service used to retrieve a camera
     * @param cameraId                   the name of the camera as defined by the camera service
     * @param availableCamerasObservable An observable updated with the current number of cameras
     *                                   that are available to be opened on the device.
     * @param handler                    the handler for the thread on which all camera
     *                                   operations run
     * @throws IllegalStateException if the {@link CameraCharacteristics} is unavailable. This
     *                               could occur if the camera was disconnected.
     */
    Camera2CameraImpl(CameraManagerCompat cameraManager, String cameraId,
            @NonNull Observable<Integer> availableCamerasObservable, Handler handler) {
        mCameraManager = cameraManager;
        mAvailableCamerasObservable = availableCamerasObservable;
        ScheduledExecutorService executorScheduler = CameraXExecutors.newHandlerExecutor(handler);
        mExecutor = executorScheduler;
        mUseCaseAttachState = new UseCaseAttachState(cameraId);
        mObservableState.postValue(State.CLOSED);

        try {
            CameraCharacteristics cameraCharacteristics =
                    mCameraManager.unwrap().getCameraCharacteristics(cameraId);
            mCameraControlInternal = new Camera2CameraControl(cameraCharacteristics,
                    executorScheduler, mExecutor, new ControlUpdateListenerInternal());
            mCameraInfoInternal = new Camera2CameraInfoImpl(
                    cameraId,
                    cameraCharacteristics,
                    mCameraControlInternal.getZoomControl(),
                    mCameraControlInternal.getTorchControl());
            Camera2CameraInfoImpl camera2CameraInfo = (Camera2CameraInfoImpl) mCameraInfoInternal;
            mCaptureSessionBuilder.setSupportedHardwareLevel(
                    camera2CameraInfo.getSupportedHardwareLevel());
        } catch (CameraAccessException e) {
            throw new IllegalStateException("Cannot access camera", e);
        }
        mCaptureSessionBuilder.setExecutor(mExecutor);
        mCaptureSessionBuilder.setScheduledExecutorService(executorScheduler);
        mCaptureSession = mCaptureSessionBuilder.build();

        mCameraAvailability = new CameraAvailability(cameraId);

        // Register an observer to update the number of available cameras
        mAvailableCamerasObservable.addObserver(mExecutor, mCameraAvailability);
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
                Log.d(TAG, "open() ignored due to being in state: " + mState);
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
        Log.d(TAG, "Closing camera: " + mCameraInfoInternal.getCameraId());
        switch (mState) {
            case OPENED:
                setState(InternalState.CLOSING);
                closeCamera(/*abortInFlightCaptures=*/false);
                break;
            case OPENING:
            case REOPENING:
                setState(InternalState.CLOSING);
                break;
            case PENDING_OPEN:
                // We should be able to transition directly to an initialized state since the
                // camera is not yet opening.
                Preconditions.checkState(mCameraDevice == null);
                setState(InternalState.INITIALIZED);
                break;
            default:
                Log.d(TAG, "close() ignored due to being in state: " + mState);
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
        Log.d(TAG, "Start configAndClose.");
        ListenableFuture<Void> openDummyCaptureSession = dummySession.open(builder.build(),
                mCameraDevice);
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
                Log.d(TAG, "Unable to configure camera " + mCameraInfoInternal.getCameraId()
                        + " due to " + t.getMessage());

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
            setState(InternalState.RELEASED);

            // After a camera is released, it cannot be reopened, so we don't need to listen for
            // available camera changes.
            mAvailableCamerasObservable.removeObserver(mCameraAvailability);
            mCameraManager.unregisterAvailabilityCallback(mCameraAvailability);

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

        Camera2CameraInfoImpl camera2CameraInfo = (Camera2CameraInfoImpl) getCameraInfoInternal();
        boolean isLegacyDevice = camera2CameraInfo.getSupportedHardwareLevel()
                == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY;

        // TODO: Check if any sessions have been previously configured. We can probably skip
        // configAndClose if there haven't been any sessions configured yet.
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M
                && Build.VERSION.SDK_INT < 29
                && isLegacyDevice
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
                // Wait for the camera async callback to finish releasing
                setState(InternalState.RELEASING);
                break;
            default:
                Log.d(TAG, "release() ignored due to being in state: " + mState);
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

        Log.d(TAG, "releasing session in state " + mState.name());
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
     * <p>The use case must also be online in order for it to issue capture requests.
     */
    @Override
    public void onUseCaseActive(@NonNull UseCase useCase) {
        Preconditions.checkNotNull(useCase);
        mExecutor.execute(() -> {
            Log.d(TAG, "Use case " + useCase + " ACTIVE for camera "
                    + mCameraInfoInternal.getCameraId());

            mUseCaseAttachState.setUseCaseActive(useCase);
            mUseCaseAttachState.updateUseCase(useCase);
            updateCaptureSessionConfig();
        });
    }


    /** Removes the use case from a state of issuing capture requests. */
    @Override
    public void onUseCaseInactive(@NonNull UseCase useCase) {
        Preconditions.checkNotNull(useCase);
        mExecutor.execute(() -> {
            Log.d(TAG, "Use case " + useCase + " INACTIVE for camera "
                    + mCameraInfoInternal.getCameraId());
            mUseCaseAttachState.setUseCaseInactive(useCase);
            updateCaptureSessionConfig();
        });
    }

    /** Updates the capture requests based on the latest settings. */
    @Override
    public void onUseCaseUpdated(@NonNull UseCase useCase) {
        Preconditions.checkNotNull(useCase);
        mExecutor.execute(() -> {
            Log.d(TAG, "Use case " + useCase + " UPDATED for camera "
                    + mCameraInfoInternal.getCameraId());
            mUseCaseAttachState.updateUseCase(useCase);
            updateCaptureSessionConfig();
        });
    }

    @Override
    public void onUseCaseReset(@NonNull UseCase useCase) {
        Preconditions.checkNotNull(useCase);
        mExecutor.execute(() -> {
            Log.d(TAG, "Use case " + useCase + " RESET for camera "
                    + mCameraInfoInternal.getCameraId());
            mUseCaseAttachState.updateUseCase(useCase);

            resetCaptureSession(/*abortInFlightCaptures=*/false);
            updateCaptureSessionConfig();
            openCaptureSession();
        });
    }

    /**
     * Returns whether the provided {@link UseCase} is considered online.
     *
     * <p>This method should only be used by tests. This will post to the Camera's thread and
     * block until completion.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.TESTS)
    boolean isUseCaseOnline(@NonNull UseCase useCase) {
        try {
            return CallbackToFutureAdapter.<Boolean>getFuture(completer -> {
                try {
                    mExecutor.execute(
                            () -> completer.set(mUseCaseAttachState.isUseCaseOnline(useCase)));
                } catch (RejectedExecutionException e) {
                    completer.setException(new RuntimeException("Unable to check if use case is "
                            + "online. Camera executor shut down."));
                }
                return "isUseCaseOnline";
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Unable to check if use case is online.", e);
        }
    }

    /**
     * Sets the use case to be in the state where the capture session will be configured to handle
     * capture requests from the use case.
     */
    @Override
    public void addOnlineUseCase(@NonNull Collection<UseCase> useCases) {
        if (!useCases.isEmpty()) {
            mCameraControlInternal.setActive(true);
            mExecutor.execute(() -> tryAddOnlineUseCases(useCases));
        }
    }

    // Attempts to make use cases online if they are not already online.
    @ExecutedBy("mExecutor")
    private void tryAddOnlineUseCases(@NonNull Collection<UseCase> toAdd) {
        // Figure out which use cases are not already online and add them.
        List<UseCase> useCasesChangedToOnline = new ArrayList<>();
        String cameraId = mCameraInfoInternal.getCameraId();
        for (UseCase useCase : toAdd) {
            if (!mUseCaseAttachState.isUseCaseOnline(useCase)) {
                useCasesChangedToOnline.add(useCase);
                mUseCaseAttachState.setUseCaseOnline(useCase);
            }
        }

        if (useCasesChangedToOnline.isEmpty()) {
            return;
        }

        Log.d(TAG, "Use cases [" + TextUtils.join(", ", useCasesChangedToOnline)
                + "] now ONLINE for camera "
                + cameraId);

        notifyStateOnlineToUseCases(useCasesChangedToOnline);

        updateCaptureSessionConfig();
        resetCaptureSession(/*abortInFlightCaptures=*/false);

        if (mState == InternalState.OPENED) {
            openCaptureSession();
        } else {
            openInternal();
        }

        updateCameraControlPreviewAspectRatio(useCasesChangedToOnline);
    }

    private void notifyStateOnlineToUseCases(List<UseCase> useCases) {
        CameraXExecutors.mainThreadExecutor().execute(() -> {
            for (UseCase useCase : useCases) {
                useCase.onStateOnline(mCameraInfoInternal.getCameraId());
            }
        });
    }

    private void notifyStateOfflineToUseCases(List<UseCase> useCases) {
        CameraXExecutors.mainThreadExecutor().execute(() -> {
            for (UseCase useCase : useCases) {
                useCase.onStateOffline(mCameraInfoInternal.getCameraId());
            }
        });
    }

    @ExecutedBy("mExecutor")
    private void updateCameraControlPreviewAspectRatio(Collection<UseCase> useCases) {
        for (UseCase useCase : useCases) {
            if (useCase instanceof Preview) {
                Size resolution = useCase.getAttachedSurfaceResolution(
                        mCameraInfoInternal.getCameraId());
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
    public void removeOnlineUseCase(@NonNull Collection<UseCase> useCases) {
        if (!useCases.isEmpty()) {
            mExecutor.execute(() -> tryRemoveOnlineUseCases(useCases));
        }
    }

    // Attempts to make use cases offline if they are online.
    @ExecutedBy("mExecutor")
    private void tryRemoveOnlineUseCases(@NonNull Collection<UseCase> toRemove) {
        List<UseCase> useCasesChangedToOffline = new ArrayList<>();
        for (UseCase useCase : toRemove) {
            if (mUseCaseAttachState.isUseCaseOnline(useCase)) {
                mUseCaseAttachState.setUseCaseOffline(useCase);
                useCasesChangedToOffline.add(useCase);
            }
        }

        if (useCasesChangedToOffline.isEmpty()) {
            return;
        }

        Log.d(TAG, "Use cases [" + TextUtils.join(", ", useCasesChangedToOffline)
                + "] now OFFLINE for camera "
                + mCameraInfoInternal.getCameraId());
        clearCameraControlPreviewAspectRatio(useCasesChangedToOffline);

        notifyStateOfflineToUseCases(useCasesChangedToOffline);

        boolean allUseCasesOffline = mUseCaseAttachState.getOnlineUseCases().isEmpty();
        if (allUseCasesOffline) {
            mCameraControlInternal.setActive(false);
            resetCaptureSession(/*abortInFlightCaptures=*/false);
            closeInternal();
        } else {
            updateCaptureSessionConfig();
            resetCaptureSession(/*abortInFlightCaptures=*/false);

            if (mState == InternalState.OPENED) {
                openCaptureSession();
            }
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
        // Check that we have an available camera to open here before attempting
        // to open the camera again.
        if (!mCameraAvailability.isCameraAvailable()) {
            Log.d(TAG, "No cameras available. Waiting for available camera before opening camera: "
                    + mCameraInfoInternal.getCameraId());
            setState(InternalState.PENDING_OPEN);
            return;
        } else {
            setState(InternalState.OPENING);
        }

        Log.d(TAG, "Opening camera: " + mCameraInfoInternal.getCameraId());

        try {
            mCameraManager.openCamera(mCameraInfoInternal.getCameraId(), mExecutor,
                    createDeviceStateCallback());
        } catch (CameraAccessException e) {
            // Camera2 will call the onError() callback with the specific error code that caused
            // this failure. No need to do anything here.
            Log.d(TAG, "Unable to open camera " + mCameraInfoInternal.getCameraId() + " due to "
                    + e.getMessage());
        }
    }

    /** Updates the capture request configuration for the current capture session. */
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @ExecutedBy("mExecutor")
    void updateCaptureSessionConfig() {
        ValidatingBuilder validatingBuilder = mUseCaseAttachState.getActiveAndOnlineBuilder();

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

        ValidatingBuilder validatingBuilder = mUseCaseAttachState.getOnlineBuilder();
        if (!validatingBuilder.isValid()) {
            Log.d(TAG, "Unable to create capture session due to conflicting configurations");
            return;
        }

        CaptureSession captureSession = mCaptureSession;
        ListenableFuture<Void> openCaptureSession = captureSession.open(validatingBuilder.build(),
                mCameraDevice);
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
                    Log.d(TAG, "Unable to configure camera " + mCameraInfoInternal.getCameraId()
                            + " due to " + t.getMessage());
                } else if (t instanceof CancellationException) {
                    Log.d(TAG, "Unable to configure camera " + mCameraInfoInternal.getCameraId()
                            + " cancelled");
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
        for (UseCase useCase : mUseCaseAttachState.getOnlineUseCases()) {
            SessionConfig sessionConfig = useCase.getSessionConfig(
                    mCameraInfoInternal.getCameraId());
            if (sessionConfig.getSurfaces().contains(surface)) {
                return useCase;
            }
        }

        return null;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    void postSurfaceClosedError(@NonNull UseCase useCase) {
        Executor executor = CameraXExecutors.mainThreadExecutor();
        SessionConfig sessionConfigError = useCase.getSessionConfig(
                mCameraInfoInternal.getCameraId());
        List<SessionConfig.ErrorListener> errorListeners =
                sessionConfigError.getErrorListeners();
        if (!errorListeners.isEmpty()) {
            SessionConfig.ErrorListener errorListener = errorListeners.get(0);
            Log.d(TAG, "Posting surface closed", new Throwable());
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
        Log.d(TAG, "Resetting Capture Session");
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
        SessionConfig config = mUseCaseAttachState.getOnlineBuilder().build();

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

        Collection<UseCase> activeUseCases = mUseCaseAttachState.getActiveAndOnlineUseCases();

        for (UseCase useCase : activeUseCases) {
            SessionConfig sessionConfig = useCase.getSessionConfig(
                    mCameraInfoInternal.getCameraId());
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

        Log.d(TAG, "issue capture request for camera " + mCameraInfoInternal.getCameraId());

        mCaptureSession.issueCaptureRequests(captureConfigsWithSurface);
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(Locale.US, "Camera@%x[id=%s]", hashCode(),
                mCameraInfoInternal.getCameraId());
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
    void setState(InternalState state) {
        Log.d(TAG, "Transitioning camera internal state: " + mState + " --> " + state);
        mState = state;
        // Convert the internal state to the publicly visible state
        switch (state) {
            case INITIALIZED:
                mObservableState.postValue(State.CLOSED);
                break;
            case PENDING_OPEN:
                mObservableState.postValue(State.PENDING_OPEN);
                break;
            case OPENING:
            case REOPENING:
                mObservableState.postValue(State.OPENING);
                break;
            case OPENED:
                mObservableState.postValue(State.OPEN);
                break;
            case CLOSING:
                mObservableState.postValue(State.CLOSING);
                break;
            case RELEASING:
                mObservableState.postValue(State.RELEASING);
                break;
            case RELEASED:
                mObservableState.postValue(State.RELEASED);
                break;
        }
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

        @Override
        @ExecutedBy("mExecutor")
        public void onOpened(CameraDevice cameraDevice) {
            Log.d(TAG, "CameraDevice.onOpened(): " + cameraDevice.getId());
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
        public void onClosed(CameraDevice cameraDevice) {
            Log.d(TAG, "CameraDevice.onClosed(): " + cameraDevice.getId());
            Preconditions.checkState(mCameraDevice == null,
                    "Unexpected onClose callback on camera device: " + cameraDevice);
            switch (mState) {
                case CLOSING:
                case RELEASING:
                    Preconditions.checkState(isSessionCloseComplete());
                    finishClose();
                    break;
                case REOPENING:
                    openCameraDevice();
                    break;
                default:
                    throw new IllegalStateException("Camera closed while in state: " + mState);
            }
        }

        @Override
        @ExecutedBy("mExecutor")
        public void onDisconnected(CameraDevice cameraDevice) {
            Log.d(TAG, "CameraDevice.onDisconnected(): " + cameraDevice.getId());

            // TODO(b/140955560) Need to force close the CaptureSessions, because onDisconnected
            //  () callback causes condition where CameraCaptureSession won't receive the
            //  onClosed() callback.
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
            Observable.Observer<Integer> {
        private final String mCameraId;

        /**
         * Availability as reported by the AvailabilityCallback. If this is true then the camera
         * is available for open. If this is false, either another process holds the camera or
         * this process. Potentially held by the Camera that is holding this instance of
         * CameraAvailability.
         */
        private boolean mCameraAvailable = true;

        /**
         * Tracks the number of cameras available for opening.
         *
         * <p>If there are no cameras available to open, the camera will wait until there is at
         * least
         * 1 camera available before opening a CameraDevice.
         */
        private int mNumAvailableCameras = 0;

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
        public void onNewData(@Nullable Integer value) {
            Preconditions.checkNotNull(value);
            if (value != mNumAvailableCameras) {
                mNumAvailableCameras = value;

                if (mState == InternalState.PENDING_OPEN) {
                    openCameraDevice();
                }
            }
        }

        @Override
        @ExecutedBy("mExecutor")
        public void onError(@NonNull Throwable t) {
            // No errors expected from available cameras yet. May need to be handled in the future.
        }

        /**
         * True if a camera is potentially available.
         */
        @ExecutedBy("mExecutor")
        boolean isCameraAvailable() {
            return mCameraAvailable && mNumAvailableCameras > 0;
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
