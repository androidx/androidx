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

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCaptureSession.CaptureCallback;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.camera2.impl.Camera2ImplConfig;
import androidx.camera.camera2.impl.CameraEventCallbacks;
import androidx.camera.camera2.internal.annotation.CameraExecutor;
import androidx.camera.camera2.internal.compat.CameraCaptureSessionCompat;
import androidx.camera.camera2.internal.compat.CameraDeviceCompat;
import androidx.camera.camera2.internal.compat.params.OutputConfigurationCompat;
import androidx.camera.camera2.internal.compat.params.SessionConfigurationCompat;
import androidx.camera.core.impl.CameraCaptureCallback;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.DeferrableSurfaces;
import androidx.camera.core.impl.MutableOptionsBundle;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.utils.futures.FutureChain;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

/**
 * A session for capturing images from the camera which is tied to a specific {@link CameraDevice}.
 *
 * <p>A session can only be opened a single time. Once has {@link CaptureSession#close()} been
 * called then it is permanently closed so a new session has to be created for capturing images.
 */
final class CaptureSession {
    private static final String TAG = "CaptureSession";

    // TODO: Find a proper timeout threshold.
    private static final long TIMEOUT_GET_SURFACE_IN_MS = 5000L;
    /** Lock on whether the camera is open or closed. */
    final Object mStateLock = new Object();
    /** Executor for all the callbacks from the {@link CameraCaptureSession}. */
    @CameraExecutor
    private final Executor mExecutor;
    /** Handler used on lower API levels to schedule callbacks to run on Executor */
    private final Handler mCompatHandler;
    /** Scheduled Executor to schedule the delayed event. */
    private final ScheduledExecutorService mScheduleExecutor;
    /** The configuration for the currently issued single capture requests. */
    private final List<CaptureConfig> mCaptureConfigs = new ArrayList<>();
    /** Callback for handling image captures. */
    private final CameraCaptureSession.CaptureCallback mCaptureCallback =
            new CaptureCallback() {
                @Override
                public void onCaptureStarted(
                        @NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                        long timestamp, long frameNumber) {
                    synchronized (mStateLock) {
                        if (mStartStreamingCompleter != null) {
                            mStartStreamingCompleter.set(null);
                            mStartStreamingCompleter = null;
                        }
                    }
                }

                @Override
                public void onCaptureSequenceAborted(
                        @NonNull CameraCaptureSession session, int sequenceId) {
                    synchronized (mStateLock) {
                        if (mStartStreamingCompleter != null) {
                            mStartStreamingCompleter.setCancelled();
                            mStartStreamingCompleter = null;
                        }
                    }
                }
            };
    private final StateCallback mCaptureSessionStateCallback;
    /** The framework camera capture session held by this session. */
    @Nullable
    CameraCaptureSessionCompat mCaptureSessionCompat;
    /** The configuration for the currently issued capture requests. */
    @Nullable
    volatile SessionConfig mSessionConfig;
    /** The capture options from CameraEventCallback.onRepeating(). **/
    @Nullable
    volatile Config mCameraEventOnRepeatingOptions;
    /**
     * The map of DeferrableSurface to Surface. It is both for restoring the surfaces used to
     * configure the current capture session and for getting the configured surface from a
     * DeferrableSurface.
     */
    private Map<DeferrableSurface, Surface> mConfiguredSurfaceMap = new HashMap<>();

    /**
     * Whether the device is LEGACY.
     */
    private final boolean mIsLegacyDevice;
    /** The list of DeferrableSurface used to notify surface detach events */
    @GuardedBy("mStateLock")
    List<DeferrableSurface> mConfiguredDeferrableSurfaces = Collections.emptyList();
    /** Tracks the current state of the session. */
    @GuardedBy("mStateLock")
    State mState = State.UNINITIALIZED;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @GuardedBy("mStateLock")
    ListenableFuture<Void> mReleaseFuture;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @GuardedBy("mStateLock")
    CallbackToFutureAdapter.Completer<Void> mReleaseCompleter;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @GuardedBy("mStateLock")
    ListenableFuture<Void> mOpenFuture;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @GuardedBy("mStateLock")
    CallbackToFutureAdapter.Completer<Void> mOpenCaptureSessionCompleter;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final ListenableFuture<Void> mStartStreamingFuture;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @GuardedBy("mStateLock")
    CallbackToFutureAdapter.Completer<Void> mStartStreamingCompleter;

    /** Whether the capture session has submitted the repeating request. */
    @GuardedBy("mStateLock")
    private boolean mHasSubmittedRepeating;

    /**
     * Constructor for CaptureSession.
     *
     * @param executor                 The executor is responsible for queuing up callbacks from
     *                                 capture requests.
     * @param compatHandler            Handler used on lower API levels for scheduling callbacks
     *                                 to run on executor.
     * @param scheduledExecutorService The executor service to schedule delayed event.
     * @param isLegacyDevice           whether the device is LEGACY.
     */
    CaptureSession(@NonNull @CameraExecutor Executor executor, @NonNull Handler compatHandler,
            @NonNull ScheduledExecutorService scheduledExecutorService, boolean isLegacyDevice) {
        mState = State.INITIALIZED;
        mExecutor = executor;
        mCompatHandler = compatHandler;
        mScheduleExecutor = scheduledExecutorService;
        mIsLegacyDevice = isLegacyDevice;
        mCaptureSessionStateCallback = new StateCallback(compatHandler);
        mStartStreamingFuture = CallbackToFutureAdapter.getFuture(
                completer -> {
                    synchronized (mStateLock) {
                        mStartStreamingCompleter = completer;
                        return "StartStreamingFuture[session="
                                + CaptureSession.this + "]";
                    }
                });
    }

    /**
     * Returns the configurations of the capture session, or null if it has not yet been set
     * or if the capture session has been closed.
     */
    @Nullable
    SessionConfig getSessionConfig() {
        synchronized (mStateLock) {
            return mSessionConfig;
        }
    }

    /**
     * Sets the active configurations for the capture session.
     *
     * <p>Once both the session configuration has been set and the session has been opened, then the
     * capture requests will immediately be issued.
     *
     * @param sessionConfig has the configuration that will currently active in issuing capture
     *                      request. The surfaces contained in this must be a subset of the
     *                      surfaces that were used to open this capture session.
     */
    void setSessionConfig(SessionConfig sessionConfig) {
        synchronized (mStateLock) {
            switch (mState) {
                case UNINITIALIZED:
                    throw new IllegalStateException(
                            "setSessionConfig() should not be possible in state: " + mState);
                case INITIALIZED:
                case GET_SURFACE:
                case OPENING:
                    mSessionConfig = sessionConfig;
                    break;
                case OPENED:
                    mSessionConfig = sessionConfig;

                    if (!mConfiguredSurfaceMap.keySet().containsAll(sessionConfig.getSurfaces())) {
                        Log.e(TAG, "Does not have the proper configured lists");
                        return;
                    }

                    Log.d(TAG, "Attempting to submit CaptureRequest after setting");
                    issueRepeatingCaptureRequests();
                    break;
                case CLOSED:
                case RELEASING:
                case RELEASED:
                    throw new IllegalStateException(
                            "Session configuration cannot be set on a closed/released session.");
            }
        }
    }

    /**
     * Opens the capture session.
     *
     * <p>When the session is opened and the configurations have been set then the capture requests
     * will be issued.
     *
     * <p>The cancellation of the returned ListenableFuture will not propagate into the inner
     * future, that is, the capture session creation process is not cancelable.
     *
     * @param sessionConfig which is used to configure the camera capture session. This contains
     *                      configurations which may or may not be currently active in issuing
     *                      capture requests.
     * @param cameraDevice  the camera with which to generate the capture session
     * @return A {@link ListenableFuture} that will be completed once the
     * {@link CameraCaptureSession} has been configured.
     */
    @NonNull
    ListenableFuture<Void> open(@NonNull SessionConfig sessionConfig,
            @NonNull CameraDevice cameraDevice) {
        synchronized (mStateLock) {
            switch (mState) {
                case INITIALIZED:
                    mState = State.GET_SURFACE;
                    List<DeferrableSurface> surfaces = sessionConfig.getSurfaces();
                    mConfiguredDeferrableSurfaces = new ArrayList<>(surfaces);
                    mOpenFuture = FutureChain.from(
                            DeferrableSurfaces.surfaceListWithTimeout(
                                    mConfiguredDeferrableSurfaces,
                                    false,
                                    TIMEOUT_GET_SURFACE_IN_MS,
                                    mExecutor, mScheduleExecutor))
                            .transformAsync(
                                    surfaceList -> openCaptureSession(surfaceList, sessionConfig,
                                            cameraDevice), mExecutor);

                    mOpenFuture.addListener(() -> {
                        synchronized (mStateLock) {
                            mOpenFuture = null;
                        }
                    }, mExecutor);

                    // The cancellation of the external ListenableFuture cannot actually stop
                    // the open session since we can't cancel the camera2 flow. The underlying
                    // Future is used to track the session is configured, we don't want to
                    // propagate the cancellation event to it. Wrap the Future in a
                    // NonCancellationPropagatingFuture, so that if the external caller cancels
                    // the Future it won't affect the underlying Future.
                    return Futures.nonCancellationPropagating(mOpenFuture);
                default:
                    Log.e(TAG, "Open not allowed in state: " + mState);
            }

            return Futures.immediateFailedFuture(
                    new IllegalStateException("open() should not allow the state: " + mState));
        }
    }

    @NonNull
    private ListenableFuture<Void> openCaptureSession(@NonNull List<Surface> configuredSurfaces,
            @NonNull SessionConfig sessionConfig, @NonNull CameraDevice cameraDevice) {
        synchronized (mStateLock) {
            switch (mState) {
                case UNINITIALIZED:
                case INITIALIZED:
                case OPENED:
                    return Futures.immediateFailedFuture(new IllegalStateException(
                            "openCaptureSession() should not be possible in state: " + mState));
                case GET_SURFACE:
                    return CallbackToFutureAdapter.getFuture(
                            completer -> {
                                synchronized (mStateLock) {
                                    openCaptureSessionLocked(completer, configuredSurfaces,
                                            sessionConfig, cameraDevice);
                                    return "openCaptureSession[session=" + this + "]";
                                }
                            });
                default:
                    return Futures.immediateFailedFuture(new CancellationException(
                            "openCaptureSession() not execute in state: " + mState));
            }
        }
    }

    @GuardedBy("mStateLock")
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    void openCaptureSessionLocked(
            @NonNull CallbackToFutureAdapter.Completer<Void> completer,
            @NonNull List<Surface> configuredSurfaces,
            @NonNull SessionConfig sessionConfig,
            @NonNull CameraDevice cameraDevice) throws CameraAccessException {
        Preconditions.checkState(mState == State.GET_SURFACE,
                "openCaptureSessionLocked() should not be possible in state: " + mState);

        // If a Surface in configuredSurfaces is null it means the Surface was not retrieved from
        // the ListenableFuture. Only handle the first failed Surface since subsequent calls to
        // CaptureSession.open() will handle the other failed Surfaces if there are any.
        if (configuredSurfaces.contains(null)) {
            int i = configuredSurfaces.indexOf(null);
            Log.d(TAG, "Some surfaces were closed.");
            DeferrableSurface deferrableSurface = mConfiguredDeferrableSurfaces.get(i);
            mConfiguredDeferrableSurfaces.clear();
            completer.setException(
                    new DeferrableSurface.SurfaceClosedException("Surface closed",
                            deferrableSurface));
            return;
        }

        if (configuredSurfaces.isEmpty()) {
            completer.setException(new IllegalArgumentException(
                    "Unable to open capture session with no surfaces"));
            return;
        }

        // Attempt to increase the usage count of all the configured deferrable surfaces before
        // adding them to the session.
        try {
            DeferrableSurfaces.incrementAll(mConfiguredDeferrableSurfaces);
        } catch (DeferrableSurface.SurfaceClosedException e) {
            mConfiguredDeferrableSurfaces.clear();
            completer.setException(e);
            return;
        }

        // Establishes the mapping of DeferrableSurface to Surface. Capture request will use this
        // mapping to get the Surface from DeferrableSurface.
        mConfiguredSurfaceMap.clear();
        for (int i = 0; i < configuredSurfaces.size(); i++) {
            mConfiguredSurfaceMap.put(mConfiguredDeferrableSurfaces.get(i),
                    configuredSurfaces.get(i));
        }

        // Some DeferrableSurfaces might actually point to the same Surface. And we need to pass
        // the unique Surface list to createCaptureSession.
        List<Surface> uniqueConfiguredSurface = new ArrayList<>(new HashSet<>(configuredSurfaces));

        Preconditions.checkState(mOpenCaptureSessionCompleter == null,
                "The openCaptureSessionCompleter can only set once!");
        mState = State.OPENING;
        Log.d(TAG, "Opening capture session.");
        List<CameraCaptureSession.StateCallback> callbacks =
                new ArrayList<>(sessionConfig.getSessionStateCallbacks());
        callbacks.add(mCaptureSessionStateCallback);
        CameraCaptureSession.StateCallback comboCallback =
                CameraCaptureSessionStateCallbacks.createComboCallback(callbacks);

        // Start check preset CaptureStage information.
        Config options = sessionConfig.getImplementationOptions();
        CameraEventCallbacks eventCallbacks = new Camera2ImplConfig(options)
                .getCameraEventCallback(CameraEventCallbacks.createEmptyCallback());
        List<CaptureConfig> presetList = eventCallbacks.createComboCallback().onPresetSession();

        // Generate the CaptureRequest builder from repeating request since Android recommend
        // use the same template type as the initial capture request. The tag and output targets
        // would be ignored by default.
        CaptureConfig.Builder captureConfigBuilder =
                CaptureConfig.Builder.from(sessionConfig.getRepeatingCaptureConfig());

        for (CaptureConfig config : presetList) {
            captureConfigBuilder.addImplementationOptions(config.getImplementationOptions());
        }

        List<OutputConfigurationCompat> outputConfigList = new ArrayList<>();
        for (Surface surface : uniqueConfiguredSurface) {
            outputConfigList.add(new OutputConfigurationCompat(surface));
        }

        SessionConfigurationCompat sessionConfigCompat =
                new SessionConfigurationCompat(
                        SessionConfigurationCompat.SESSION_REGULAR,
                        outputConfigList,
                        getExecutor(),
                        comboCallback);

        CameraDeviceCompat cameraDeviceCompat =
                CameraDeviceCompat.toCameraDeviceCompat(cameraDevice, mCompatHandler);

        CaptureRequest captureRequest =
                Camera2CaptureRequestBuilder.buildWithoutTarget(
                        captureConfigBuilder.build(),
                        cameraDeviceCompat.toCameraDevice());

        if (captureRequest != null) {
            sessionConfigCompat.setSessionParameters(captureRequest);
        }

        mOpenCaptureSessionCompleter = completer;

        cameraDeviceCompat.createCaptureSession(sessionConfigCompat);
    }

    /**
     * Closes the capture session.
     *
     * <p>Close needs be called on a session in order to safely open another session. However, this
     * stops minimal resources so that another session can be quickly opened.
     *
     * <p>Once a session is closed it can no longer be opened again. After the session is closed all
     * method calls on it do nothing.
     */
    void close() {
        synchronized (mStateLock) {
            switch (mState) {
                case UNINITIALIZED:
                    throw new IllegalStateException(
                            "close() should not be possible in state: " + mState);
                case GET_SURFACE:
                    if (mOpenFuture != null) {
                        mOpenFuture.cancel(true);
                    }
                    // Fall through
                case INITIALIZED:
                    mState = State.RELEASED;
                    break;
                case OPENED:
                    // Only issue onDisableSession requests at OPENED state.
                    if (mSessionConfig != null) {
                        CameraEventCallbacks eventCallbacks = new Camera2ImplConfig(
                                mSessionConfig.getImplementationOptions()).getCameraEventCallback(
                                CameraEventCallbacks.createEmptyCallback());
                        List<CaptureConfig> configList =
                                eventCallbacks.createComboCallback().onDisableSession();
                        if (!configList.isEmpty()) {
                            try {
                                issueCaptureRequests(setupConfiguredSurface(configList));
                            } catch (IllegalStateException e) {
                                // We couldn't issue the request before close the capture session,
                                // but we should continue the close flow.
                                Log.e(TAG, "Unable to issue the request before close the capture "
                                        + "session", e);
                            }
                        }
                    }
                    // Not break close flow. Fall through
                case OPENING:
                    mState = State.CLOSED;
                    mSessionConfig = null;
                    mCameraEventOnRepeatingOptions = null;
                    closeConfiguredDeferrableSurfaces();

                    break;
                case CLOSED:
                case RELEASING:
                case RELEASED:
                    break;
            }
        }
    }

    /**
     * Releases the capture session.
     *
     * <p>This releases all of the sessions resources and should be called when ready to close the
     * camera.
     *
     * <p>Once a session is released it can no longer be opened again. After the session is released
     * all method calls on it do nothing.
     */
    ListenableFuture<Void> release(boolean abortInFlightCaptures) {
        synchronized (mStateLock) {
            if (!mHasSubmittedRepeating) {
                // If the release() is called before any repeating requests have been issued,
                // then the startStreamingFuture should be cancelled.
                mStartStreamingFuture.cancel(true);
            }
            switch (mState) {
                case UNINITIALIZED:
                    throw new IllegalStateException(
                            "release() should not be possible in state: " + mState);
                case OPENED:
                case CLOSED:
                    if (mIsLegacyDevice && mHasSubmittedRepeating) {
                        // Checks the capture session is ready before closing. See: b/146773463.
                        mStartStreamingFuture.addListener(() -> {
                            synchronized (mStateLock) {
                                closeCameraCaptureSession(abortInFlightCaptures);
                            }
                        }, mExecutor);
                    } else {
                        closeCameraCaptureSession(abortInFlightCaptures);
                    }
                    // Fall through
                case OPENING:
                    mState = State.RELEASING;
                    // Not cancel the openCaptureSessionFuture since the create capture session
                    // flow cannot interrupt. The release should start after the capture session is
                    // configured.

                    // Fall through
                case RELEASING:
                    if (mReleaseFuture == null) {
                        mReleaseFuture = CallbackToFutureAdapter.getFuture(
                                completer -> {
                                    synchronized (mStateLock) {
                                        Preconditions.checkState(mReleaseCompleter == null,
                                                "Release completer expected to be null");
                                        mReleaseCompleter = completer;
                                        return "Release[session=" + CaptureSession.this + "]";
                                    }
                                });
                    }

                    return mReleaseFuture;
                case GET_SURFACE:
                    if (mOpenFuture != null) {
                        mOpenFuture.cancel(true);
                    }
                    // Fall through
                case INITIALIZED:
                    mState = State.RELEASED;
                    // Fall through
                case RELEASED:
                    break;
            }
        }

        // Already released. Return success immediately.
        return Futures.immediateFuture(null);
    }

    private void closeCameraCaptureSession(boolean abortInFlightCaptures) {
        if (mCaptureSessionCompat != null) {
            if (abortInFlightCaptures) {
                try {
                    mCaptureSessionCompat.toCameraCaptureSession().abortCaptures();
                } catch (CameraAccessException e) {
                    // We couldn't abort the captures, but we should continue on to
                    // release the session.
                    Log.e(TAG, "Unable to abort captures.", e);
                }
            }
            mCaptureSessionCompat.toCameraCaptureSession().close();
        }
    }

    // Force the onClosed() callback to be made. This is necessary because the onClosed()
    // callback never gets called if CameraDevice.StateCallback.onDisconnected() is called. See
    // TODO(b/140955560) If the issue is fixed then on OS releases with the fix this should not
    //  be called and instead onClosed() should be called by the framework instead.
    void forceClose() {
        if (mCaptureSessionCompat != null) {
            mCaptureSessionStateCallback.onClosed(mCaptureSessionCompat.toCameraCaptureSession());
        }
    }

    // Notify the surface is detached from current capture session.
    @GuardedBy("mStateLock")
    void clearConfiguredSurfaces() {
        DeferrableSurfaces.decrementAll(mConfiguredDeferrableSurfaces);

        // Clears the mConfiguredDeferrableSurfaces to prevent from duplicate
        // decrement calls.
        mConfiguredDeferrableSurfaces.clear();
    }

    /**
     * Issues capture requests.
     *
     * @param captureConfigs which is used to construct {@link CaptureRequest}.
     */
    void issueCaptureRequests(List<CaptureConfig> captureConfigs) {
        synchronized (mStateLock) {
            switch (mState) {
                case UNINITIALIZED:
                    throw new IllegalStateException(
                            "issueCaptureRequests() should not be possible in state: "
                                    + mState);
                case INITIALIZED:
                case GET_SURFACE:
                case OPENING:
                    mCaptureConfigs.addAll(captureConfigs);
                    break;
                case OPENED:
                    mCaptureConfigs.addAll(captureConfigs);
                    issuePendingCaptureRequest();
                    break;
                case CLOSED:
                case RELEASING:
                case RELEASED:
                    throw new IllegalStateException(
                            "Cannot issue capture request on a closed/released session.");
            }
        }
    }

    /** Returns the configurations of the capture requests. */
    List<CaptureConfig> getCaptureConfigs() {
        synchronized (mStateLock) {
            return Collections.unmodifiableList(mCaptureConfigs);
        }
    }

    /** Returns the current state of the session. */
    State getState() {
        synchronized (mStateLock) {
            return mState;
        }
    }

    /** Returns the future which is completed once the session starts streaming frames */
    ListenableFuture<Void> getStartStreamingFuture() {
        return mStartStreamingFuture;
    }

    /**
     * Sets the {@link CaptureRequest} so that the camera will start producing data.
     *
     * <p>Will skip setting requests if there are no surfaces since it is illegal to do so.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @GuardedBy("mStateLock")
    void issueRepeatingCaptureRequests() {
        if (mSessionConfig == null) {
            Log.d(TAG, "Skipping issueRepeatingCaptureRequests for no configuration case.");
            return;
        }

        CaptureConfig captureConfig = mSessionConfig.getRepeatingCaptureConfig();

        try {
            Log.d(TAG, "Issuing request for session.");

            // The override priority for implementation options
            // P1 CameraEventCallback onRepeating options
            // P2 SessionConfig options
            CaptureConfig.Builder captureConfigBuilder = CaptureConfig.Builder.from(captureConfig);

            CameraEventCallbacks eventCallbacks = new Camera2ImplConfig(
                    mSessionConfig.getImplementationOptions()).getCameraEventCallback(
                    CameraEventCallbacks.createEmptyCallback());

            mCameraEventOnRepeatingOptions = mergeOptions(
                    eventCallbacks.createComboCallback().onRepeating());
            if (mCameraEventOnRepeatingOptions != null) {
                captureConfigBuilder.addImplementationOptions(mCameraEventOnRepeatingOptions);
            }

            CaptureRequest captureRequest = Camera2CaptureRequestBuilder.build(
                    captureConfigBuilder.build(),
                    mCaptureSessionCompat.toCameraCaptureSession().getDevice(),
                    mConfiguredSurfaceMap);
            if (captureRequest == null) {
                Log.d(TAG, "Skipping issuing empty request for session.");
                return;
            }

            CameraCaptureSession.CaptureCallback comboCaptureCallback =
                    createCamera2CaptureCallback(
                            captureConfig.getCameraCaptureCallbacks(),
                            mCaptureCallback);

            mHasSubmittedRepeating = true;
            mCaptureSessionCompat.setSingleRepeatingRequest(captureRequest, mExecutor,
                    comboCaptureCallback);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Unable to access camera: " + e.getMessage());
            Thread.dumpStack();
        }
    }

    /** Issues mCaptureConfigs to {@link CameraCaptureSession}. */
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    void issuePendingCaptureRequest() {
        if (mCaptureConfigs.isEmpty()) {
            return;
        }
        try {
            issueBurstCaptureRequest(mCaptureConfigs);
        } finally {
            mCaptureConfigs.clear();
        }
    }

    /** Issues input CaptureConfig list to {@link CameraCaptureSession}. */
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    void issueBurstCaptureRequest(List<CaptureConfig> captureConfigs) {
        if (captureConfigs.isEmpty()) {
            return;
        }
        try {
            CameraBurstCaptureCallback callbackAggregator = new CameraBurstCaptureCallback();
            List<CaptureRequest> captureRequests = new ArrayList<>();
            Log.d(TAG, "Issuing capture request.");
            for (CaptureConfig captureConfig : captureConfigs) {
                if (captureConfig.getSurfaces().isEmpty()) {
                    Log.d(TAG, "Skipping issuing empty capture request.");
                    continue;
                }

                // Validate all surfaces belong to configured surfaces map
                boolean surfacesValid = true;
                for (DeferrableSurface surface : captureConfig.getSurfaces()) {
                    if (!mConfiguredSurfaceMap.containsKey(surface)) {
                        Log.d(TAG, "Skipping capture request with invalid surface: " + surface);
                        surfacesValid = false;
                        break;
                    }
                }

                if (!surfacesValid) {
                    // An invalid surface was detected in this request.
                    // Skip it and go on to the next request.
                    // TODO (b/133710422): Report this request as an error.
                    continue;
                }

                CaptureConfig.Builder captureConfigBuilder = CaptureConfig.Builder.from(
                        captureConfig);

                // The override priority for implementation options
                // P1 Single capture options
                // P2 CameraEventCallback onRepeating options
                // P3 SessionConfig options
                if (mSessionConfig != null) {
                    captureConfigBuilder.addImplementationOptions(
                            mSessionConfig.getRepeatingCaptureConfig().getImplementationOptions());
                }

                if (mCameraEventOnRepeatingOptions != null) {
                    captureConfigBuilder.addImplementationOptions(mCameraEventOnRepeatingOptions);
                }

                // Need to override again since single capture options has highest priority.
                captureConfigBuilder.addImplementationOptions(
                        captureConfig.getImplementationOptions());

                CaptureRequest captureRequest = Camera2CaptureRequestBuilder.build(
                        captureConfigBuilder.build(),
                        mCaptureSessionCompat.toCameraCaptureSession().getDevice(),
                        mConfiguredSurfaceMap);
                if (captureRequest == null) {
                    Log.d(TAG, "Skipping issuing request without surface.");
                    return;
                }

                List<CameraCaptureSession.CaptureCallback> cameraCallbacks = new ArrayList<>();
                for (CameraCaptureCallback callback : captureConfig.getCameraCaptureCallbacks()) {
                    CaptureCallbackConverter.toCaptureCallback(callback, cameraCallbacks);
                }
                callbackAggregator.addCamera2Callbacks(captureRequest, cameraCallbacks);
                captureRequests.add(captureRequest);
            }

            if (!captureRequests.isEmpty()) {
                mCaptureSessionCompat.captureBurstRequests(captureRequests, mExecutor,
                        callbackAggregator);
            } else {
                Log.d(TAG, "Skipping issuing burst request due to no valid request elements");
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Unable to access camera: " + e.getMessage());
            Thread.dumpStack();
        }
    }

    void cancelIssuedCaptureRequests() {
        if (!mCaptureConfigs.isEmpty()) {
            for (CaptureConfig captureConfig : mCaptureConfigs) {
                for (CameraCaptureCallback cameraCaptureCallback :
                        captureConfig.getCameraCaptureCallbacks()) {
                    cameraCaptureCallback.onCaptureCancelled();
                }
            }
            mCaptureConfigs.clear();
        }
    }

    private CameraCaptureSession.CaptureCallback createCamera2CaptureCallback(
            List<CameraCaptureCallback> cameraCaptureCallbacks,
            CameraCaptureSession.CaptureCallback... additionalCallbacks) {
        List<CameraCaptureSession.CaptureCallback> camera2Callbacks =
                new ArrayList<>(cameraCaptureCallbacks.size() + additionalCallbacks.length);
        for (CameraCaptureCallback callback : cameraCaptureCallbacks) {
            camera2Callbacks.add(CaptureCallbackConverter.toCaptureCallback(callback));
        }
        Collections.addAll(camera2Callbacks, additionalCallbacks);
        return Camera2CaptureCallbacks.createComboCallback(camera2Callbacks);
    }


    /**
     * Merges the implementation options from the input {@link CaptureConfig} list.
     *
     * <p>It will retain the first option if a conflict is detected.
     *
     * @param captureConfigList CaptureConfig list to be merged.
     * @return merged options.
     */
    @NonNull
    private static Config mergeOptions(List<CaptureConfig> captureConfigList) {
        MutableOptionsBundle options = MutableOptionsBundle.create();
        for (CaptureConfig captureConfig : captureConfigList) {
            Config newOptions = captureConfig.getImplementationOptions();
            for (Config.Option<?> option : newOptions.listOptions()) {
                @SuppressWarnings("unchecked") // Options/values are being copied directly
                        Config.Option<Object> objectOpt = (Config.Option<Object>) option;
                Object newValue = newOptions.retrieveOption(objectOpt, null);
                if (options.containsOption(option)) {
                    Object oldValue = options.retrieveOption(objectOpt, null);
                    if (!Objects.equals(oldValue, newValue)) {
                        Log.d(TAG, "Detect conflicting option "
                                + objectOpt.getId()
                                + " : "
                                + newValue
                                + " != "
                                + oldValue);
                    }
                } else {
                    options.insertOption(objectOpt, newValue);
                }
            }
        }
        return options;
    }

    enum State {
        /** The default state of the session before construction. */
        UNINITIALIZED,
        /**
         * Stable state once the session has been constructed, but prior to the {@link
         * CameraCaptureSession} being opened.
         */
        INITIALIZED,
        /**
         * Transitional state to get the configured surface from the configuration. Once the
         * surfaces is ready, we can create the {@link CameraCaptureSession}.
         */
        GET_SURFACE,
        /**
         * Transitional state when the {@link CameraCaptureSession} is in the process of being
         * opened.
         */
        OPENING,
        /**
         * Stable state where the {@link CameraCaptureSession} has been successfully opened. During
         * this state if a valid {@link SessionConfig} has been set then the {@link
         * CaptureRequest} will be issued.
         */
        OPENED,
        /**
         * Stable state where the session has been closed. However the {@link CameraCaptureSession}
         * is still valid. It will remain valid until a new instance is opened at which point {@link
         * CameraCaptureSession.StateCallback#onClosed(CameraCaptureSession)} will be called to do
         * final cleanup.
         */
        CLOSED,
        /** Transitional state where the resources are being cleaned up. */
        RELEASING,
        /**
         * Terminal state where the session has been cleaned up. At this point the session should
         * not be used as nothing will happen in this state.
         */
        RELEASED
    }

    /**
     * Callback for handling state changes to the {@link CameraCaptureSession}.
     *
     * <p>State changes are ignored once the CaptureSession has been closed.
     */
    final class StateCallback extends CameraCaptureSession.StateCallback {

        private final Handler mCompatHandler;

        StateCallback(@NonNull Handler compatHandler) {
            mCompatHandler = compatHandler;
        }

        /**
         * {@inheritDoc}
         *
         * <p>Once the {@link CameraCaptureSession} has been configured then the capture request
         * will be immediately issued.
         */
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            synchronized (mStateLock) {
                Preconditions.checkNotNull(mOpenCaptureSessionCompleter,
                        "OpenCaptureSession completer should not null");
                mOpenCaptureSessionCompleter.set(null);
                mOpenCaptureSessionCompleter = null;

                switch (mState) {
                    case UNINITIALIZED:
                    case INITIALIZED:
                    case GET_SURFACE:
                    case OPENED:
                    case RELEASED:
                        throw new IllegalStateException(
                                "onConfigured() should not be possible in state: " + mState);
                    case OPENING:
                        mState = State.OPENED;
                        mCaptureSessionCompat =
                                CameraCaptureSessionCompat.toCameraCaptureSessionCompat(session,
                                        mCompatHandler);

                        // Issue capture request of enableSession if exists.
                        if (mSessionConfig != null) {
                            Config implOptions = mSessionConfig.getImplementationOptions();
                            CameraEventCallbacks eventCallbacks = new Camera2ImplConfig(
                                    implOptions).getCameraEventCallback(
                                    CameraEventCallbacks.createEmptyCallback());
                            List<CaptureConfig> list =
                                    eventCallbacks.createComboCallback().onEnableSession();
                            if (!list.isEmpty()) {
                                issueBurstCaptureRequest(setupConfiguredSurface(list));
                            }
                        }

                        Log.d(TAG, "Attempting to send capture request onConfigured");
                        issueRepeatingCaptureRequests();
                        issuePendingCaptureRequest();
                        break;
                    case CLOSED:
                        mCaptureSessionCompat =
                                CameraCaptureSessionCompat.toCameraCaptureSessionCompat(session,
                                        mCompatHandler);
                        break;
                    case RELEASING:
                        session.close();
                        break;
                }
                Log.d(TAG, "CameraCaptureSession.onConfigured() mState=" + mState);
            }
        }

        @Override
        public void onReady(@NonNull CameraCaptureSession session) {
            synchronized (mStateLock) {
                switch (mState) {
                    case UNINITIALIZED:
                        throw new IllegalStateException(
                                "onReady() should not be possible in state: " + mState);
                    default:
                }
                Log.d(TAG, "CameraCaptureSession.onReady() " + mState);
            }
        }

        @Override
        public void onClosed(@NonNull CameraCaptureSession session) {
            synchronized (mStateLock) {
                if (mState == State.UNINITIALIZED) {
                    throw new IllegalStateException(
                            "onClosed() should not be possible in state: " + mState);
                }

                if (mState == State.RELEASED) {
                    // If released then onClosed() has already been called, but it can be ignored
                    // since a session can be forceClosed.
                    return;
                }

                Log.d(TAG, "CameraCaptureSession.onClosed()");

                closeConfiguredDeferrableSurfaces();

                mState = State.RELEASED;
                mCaptureSessionCompat = null;

                clearConfiguredSurfaces();

                if (mReleaseCompleter != null) {
                    mReleaseCompleter.set(null);
                    mReleaseCompleter = null;
                }
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            synchronized (mStateLock) {
                Preconditions.checkNotNull(mOpenCaptureSessionCompleter,
                        "OpenCaptureSession completer should not null");
                mOpenCaptureSessionCompleter.setException(new CancellationException(
                        "onConfigureFailed"));
                mOpenCaptureSessionCompleter = null;

                switch (mState) {
                    case UNINITIALIZED:
                    case INITIALIZED:
                    case GET_SURFACE:
                    case OPENED:
                    case RELEASED:
                        throw new IllegalStateException(
                                "onConfiguredFailed() should not be possible in state: " + mState);
                    case OPENING:
                    case CLOSED:
                        // For CaptureSession onConfigureFailed in framework, it will not allow
                        // any close function or callback work. Hence, change state to RELEASED.
                        // Check b/147402661 for detail.
                        mState = State.RELEASED;
                        mCaptureSessionCompat = null;
                        break;
                    case RELEASING:
                        // TODO(b/147402661): Refactor this part after detail checking.
                        mState = State.RELEASING;
                        session.close();
                }
                Log.e(TAG, "CameraCaptureSession.onConfiguredFailed() " + mState);
            }
        }
    }

    @GuardedBy("mStateLock")
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    void closeConfiguredDeferrableSurfaces() {
        // Do not close for non-LEGACY devices. Reusing {@link DeferrableSurface} is only a
        // problem for LEGACY devices. See: b/135050586.
        // Another problem is the behavior of TextureView below API 23. It releases {@link
        // SurfaceTexture}. Hence, request to close and recreate {@link DeferrableSurface}.
        // See: b/145725334.
        if (mIsLegacyDevice || Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            for (DeferrableSurface deferrableSurface : mConfiguredDeferrableSurfaces) {
                deferrableSurface.close();
            }
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    List<CaptureConfig> setupConfiguredSurface(List<CaptureConfig> list) {
        List<CaptureConfig> ret = new ArrayList<>();
        for (CaptureConfig c : list) {
            CaptureConfig.Builder builder = CaptureConfig.Builder.from(c);
            builder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
            for (DeferrableSurface deferrableSurface :
                    mSessionConfig.getRepeatingCaptureConfig().getSurfaces()) {
                builder.addSurface(deferrableSurface);
            }
            ret.add(builder.build());
        }

        return ret;
    }

    @CameraExecutor
    @NonNull
    private Executor getExecutor() {
        return mExecutor;
    }

    static final class Builder {
        @CameraExecutor
        private Executor mExecutor;
        private Handler mCompatHandler;
        private ScheduledExecutorService mScheduledExecutorService;
        private int mSupportedHardwareLevel = -1;

        CaptureSession build() {
            if (mExecutor == null) {
                throw new IllegalStateException("Missing camera executor. Executor must be set "
                        + "with setExecutor()");
            }

            if (mScheduledExecutorService == null) {
                throw new IllegalStateException(
                        "Missing ScheduledExecutorService. ScheduledExecutorService must be set "
                                + "with setScheduledExecutorService()");
            }

            if (mCompatHandler == null) {
                throw new IllegalStateException(
                        "Missing compat handler. Compat handler must be set with "
                                + "setCompatHandler()");
            }

            return new CaptureSession(mExecutor, mCompatHandler, mScheduledExecutorService,
                    mSupportedHardwareLevel
                    == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        }

        void setExecutor(@NonNull @CameraExecutor Executor executor) {
            mExecutor = Preconditions.checkNotNull(executor);
        }

        void setScheduledExecutorService(
                @NonNull ScheduledExecutorService scheduledExecutorService) {
            mScheduledExecutorService = Preconditions.checkNotNull(scheduledExecutorService);
        }

        void setCompatHandler(@NonNull Handler compatHandler) {
            mCompatHandler = Preconditions.checkNotNull(compatHandler);
        }

        void setSupportedHardwareLevel(int supportedHardwareLevel) {
            mSupportedHardwareLevel = supportedHardwareLevel;
        }
    }
}
