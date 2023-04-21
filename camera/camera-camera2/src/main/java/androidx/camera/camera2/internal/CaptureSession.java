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
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.DynamicRangeProfiles;
import android.os.Build;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.impl.Camera2ImplConfig;
import androidx.camera.camera2.impl.CameraEventCallbacks;
import androidx.camera.camera2.internal.compat.params.DynamicRangeConversions;
import androidx.camera.camera2.internal.compat.params.DynamicRangesCompat;
import androidx.camera.camera2.internal.compat.params.InputConfigurationCompat;
import androidx.camera.camera2.internal.compat.params.OutputConfigurationCompat;
import androidx.camera.camera2.internal.compat.params.SessionConfigurationCompat;
import androidx.camera.camera2.internal.compat.workaround.StillCaptureFlow;
import androidx.camera.camera2.internal.compat.workaround.TorchStateReset;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.DynamicRange;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.CameraCaptureCallback;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.MutableOptionsBundle;
import androidx.camera.core.impl.OptionsBundle;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.FutureChain;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;

/**
 * A basic implementation of {@link CaptureSessionInterface} for capturing images from the camera
 * which is tied to a specific {@link CameraDevice}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
final class CaptureSession implements CaptureSessionInterface {
    private static final String TAG = "CaptureSession";

    // TODO: Find a proper timeout threshold.
    private static final long TIMEOUT_GET_SURFACE_IN_MS = 5000L;
    /** Lock to ensure session operations run atomically. */
    final Object mSessionLock = new Object();
    /** The configuration for the currently issued single capture requests. */
    @GuardedBy("mSessionLock")
    private final List<CaptureConfig> mCaptureConfigs = new ArrayList<>();
    /** Callback for handling image captures. */
    private final CameraCaptureSession.CaptureCallback mCaptureCallback =
            new CaptureCallback() {
                @Override
                public void onCaptureCompleted(
                        @NonNull CameraCaptureSession session,
                        @NonNull CaptureRequest request,
                        @NonNull TotalCaptureResult result) {
                }
            };
    @GuardedBy("mSessionLock")
    private final StateCallback mCaptureSessionStateCallback;
    /** The Opener to help on creating the SynchronizedCaptureSession. */
    @Nullable
    @GuardedBy("mSessionLock")
    SynchronizedCaptureSessionOpener mSynchronizedCaptureSessionOpener;
    /** The framework camera capture session held by this session. */
    @Nullable
    @GuardedBy("mSessionLock")
    SynchronizedCaptureSession mSynchronizedCaptureSession;
    /** The configuration for the currently issued capture requests. */
    @Nullable
    @GuardedBy("mSessionLock")
    SessionConfig mSessionConfig;
    /** The capture options from CameraEventCallback.onRepeating(). **/
    @NonNull
    @GuardedBy("mSessionLock")
    Config mCameraEventOnRepeatingOptions = OptionsBundle.emptyBundle();
    /** The CameraEventCallbacks for this capture session. */
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @GuardedBy("mSessionLock")
    @NonNull
    CameraEventCallbacks mCameraEventCallbacks = CameraEventCallbacks.createEmptyCallback();
    /**
     * The map of DeferrableSurface to Surface. It is both for restoring the surfaces used to
     * configure the current capture session and for getting the configured surface from a
     * DeferrableSurface.
     */
    @GuardedBy("mSessionLock")
    private final Map<DeferrableSurface, Surface> mConfiguredSurfaceMap = new HashMap<>();

    /** The list of DeferrableSurface used to notify surface detach events */
    @GuardedBy("mSessionLock")
    List<DeferrableSurface> mConfiguredDeferrableSurfaces = Collections.emptyList();
    /** Tracks the current state of the session. */
    @GuardedBy("mSessionLock")
    State mState = State.UNINITIALIZED;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @GuardedBy("mSessionLock")
    ListenableFuture<Void> mReleaseFuture;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @GuardedBy("mSessionLock")
    CallbackToFutureAdapter.Completer<Void> mReleaseCompleter;
    @NonNull
    @GuardedBy("mSessionLock")
    Map<DeferrableSurface, Long> mStreamUseCaseMap = new HashMap<>();
    final StillCaptureFlow mStillCaptureFlow = new StillCaptureFlow();
    final TorchStateReset mTorchStateReset = new TorchStateReset();

    private final DynamicRangesCompat mDynamicRangesCompat;

    /**
     * Constructor for CaptureSession.
     */
    CaptureSession(@NonNull DynamicRangesCompat dynamicRangesCompat) {
        mState = State.INITIALIZED;
        mDynamicRangesCompat = dynamicRangesCompat;
        mCaptureSessionStateCallback = new StateCallback();
    }

    @Override
    public void setStreamUseCaseMap(@NonNull Map<DeferrableSurface, Long> streamUseCaseMap) {
        synchronized (mSessionLock) {
            mStreamUseCaseMap = streamUseCaseMap;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    @Override
    public SessionConfig getSessionConfig() {
        synchronized (mSessionLock) {
            return mSessionConfig;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSessionConfig(@Nullable SessionConfig sessionConfig) {
        synchronized (mSessionLock) {
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
                    if (sessionConfig == null) {
                        return;
                    }

                    if (!mConfiguredSurfaceMap.keySet().containsAll(sessionConfig.getSurfaces())) {
                        Logger.e(TAG, "Does not have the proper configured lists");
                        return;
                    }

                    Logger.d(TAG, "Attempting to submit CaptureRequest after setting");
                    issueRepeatingCaptureRequests(mSessionConfig);
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
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public ListenableFuture<Void> open(@NonNull SessionConfig sessionConfig,
            @NonNull CameraDevice cameraDevice,
            @NonNull SynchronizedCaptureSessionOpener opener) {
        synchronized (mSessionLock) {
            switch (mState) {
                case INITIALIZED:
                    mState = State.GET_SURFACE;
                    List<DeferrableSurface> surfaces = sessionConfig.getSurfaces();
                    mConfiguredDeferrableSurfaces = new ArrayList<>(surfaces);
                    mSynchronizedCaptureSessionOpener = opener;
                    ListenableFuture<Void> openFuture = FutureChain.from(
                                    mSynchronizedCaptureSessionOpener.startWithDeferrableSurface(
                                            mConfiguredDeferrableSurfaces,
                                            TIMEOUT_GET_SURFACE_IN_MS))
                            .transformAsync(
                                    surfaceList -> openCaptureSession(surfaceList, sessionConfig,
                                            cameraDevice),
                                    mSynchronizedCaptureSessionOpener.getExecutor());

                    Futures.addCallback(openFuture, new FutureCallback<Void>() {
                        @Override
                        public void onSuccess(@Nullable Void result) {
                            // Nothing to do.
                        }

                        @Override
                        public void onFailure(@NonNull Throwable t) {
                            synchronized (mSessionLock) {
                                // Stop the Opener if we get any failure during opening.
                                mSynchronizedCaptureSessionOpener.stop();
                                switch (mState) {
                                    case OPENING:
                                    case CLOSED:
                                    case RELEASING:
                                        if (!(t instanceof CancellationException)) {
                                            Logger.w(TAG, "Opening session with fail " + mState, t);
                                            finishClose();
                                        }
                                        break;
                                    default:
                                }
                            }
                        }
                    }, mSynchronizedCaptureSessionOpener.getExecutor());

                    // The cancellation of the external ListenableFuture cannot actually stop
                    // the open session since we can't cancel the camera2 flow. The underlying
                    // Future is used to track the session is configured, we don't want to
                    // propagate the cancellation event to it. Wrap the Future in a
                    // NonCancellationPropagatingFuture, so that if the external caller cancels
                    // the Future it won't affect the underlying Future.
                    return Futures.nonCancellationPropagating(openFuture);
                default:
                    Logger.e(TAG, "Open not allowed in state: " + mState);
            }

            return Futures.immediateFailedFuture(
                    new IllegalStateException("open() should not allow the state: " + mState));
        }
    }

    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    @NonNull
    private ListenableFuture<Void> openCaptureSession(@NonNull List<Surface> configuredSurfaces,
            @NonNull SessionConfig sessionConfig, @NonNull CameraDevice cameraDevice) {
        synchronized (mSessionLock) {
            switch (mState) {
                case UNINITIALIZED:
                case INITIALIZED:
                case OPENED:
                    return Futures.immediateFailedFuture(new IllegalStateException(
                            "openCaptureSession() should not be possible in state: " + mState));
                case GET_SURFACE:
                    // Establishes the mapping of DeferrableSurface to Surface. Capture request
                    // will use this mapping to get the Surface from DeferrableSurface.
                    mConfiguredSurfaceMap.clear();
                    for (int i = 0; i < configuredSurfaces.size(); i++) {
                        mConfiguredSurfaceMap.put(mConfiguredDeferrableSurfaces.get(i),
                                configuredSurfaces.get(i));
                    }

                    mState = State.OPENING;
                    Logger.d(TAG, "Opening capture session.");
                    SynchronizedCaptureSession.StateCallback callbacks =
                            SynchronizedCaptureSessionStateCallbacks.createComboCallback(
                                    mCaptureSessionStateCallback,
                                    new SynchronizedCaptureSessionStateCallbacks.Adapter(
                                            sessionConfig.getSessionStateCallbacks())
                            );

                    Camera2ImplConfig camera2Config =
                            new Camera2ImplConfig(sessionConfig.getImplementationOptions());
                    // Start check preset CaptureStage information.
                    mCameraEventCallbacks = camera2Config
                            .getCameraEventCallback(CameraEventCallbacks.createEmptyCallback());
                    List<CaptureConfig> presetList =
                            mCameraEventCallbacks.createComboCallback().onInitSession();

                    // Generate the CaptureRequest builder from repeating request since Android
                    // recommend use the same template type as the initial capture request. The
                    // tag and output targets would be ignored by default.
                    CaptureConfig.Builder captureConfigBuilder =
                            CaptureConfig.Builder.from(sessionConfig.getRepeatingCaptureConfig());

                    for (CaptureConfig config : presetList) {
                        captureConfigBuilder.addImplementationOptions(
                                config.getImplementationOptions());
                    }

                    List<OutputConfigurationCompat> outputConfigList = new ArrayList<>();
                    String physicalCameraIdForAllStreams =
                            camera2Config.getPhysicalCameraId(null);
                    for (SessionConfig.OutputConfig outputConfig :
                            sessionConfig.getOutputConfigs()) {
                        OutputConfigurationCompat outputConfiguration =
                                getOutputConfigurationCompat(
                                        outputConfig,
                                        mConfiguredSurfaceMap,
                                        physicalCameraIdForAllStreams);
                        if (mStreamUseCaseMap.containsKey(outputConfig.getSurface())) {
                            outputConfiguration.setStreamUseCase(
                                    mStreamUseCaseMap.get(outputConfig.getSurface()));
                        }
                        outputConfigList.add(outputConfiguration);
                    }

                    // Some DeferrableSurfaces might actually point to the same Surface. For
                    // example, a Surface(ImageReader) could be shared between use cases.
                    // Therefore, there might be duplicate surfaces that need to be removed.
                    // We might consider removing this logic if this is no longer necessary.
                    outputConfigList = getUniqueOutputConfigurations(outputConfigList);

                    SessionConfigurationCompat sessionConfigCompat =
                            mSynchronizedCaptureSessionOpener.createSessionConfigurationCompat(
                                    SessionConfigurationCompat.SESSION_REGULAR, outputConfigList,
                                    callbacks);

                    if (sessionConfig.getTemplateType() == CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG
                            && sessionConfig.getInputConfiguration() != null) {
                        sessionConfigCompat.setInputConfiguration(
                                InputConfigurationCompat.wrap(
                                        sessionConfig.getInputConfiguration()));
                    }

                    try {
                        CaptureRequest captureRequest =
                                Camera2CaptureRequestBuilder.buildWithoutTarget(
                                        captureConfigBuilder.build(), cameraDevice);
                        if (captureRequest != null) {
                            sessionConfigCompat.setSessionParameters(captureRequest);
                        }
                    } catch (CameraAccessException e) {
                        return Futures.immediateFailedFuture(e);
                    }

                    return mSynchronizedCaptureSessionOpener.openCaptureSession(cameraDevice,
                            sessionConfigCompat, mConfiguredDeferrableSurfaces);
                default:
                    return Futures.immediateFailedFuture(new CancellationException(
                            "openCaptureSession() not execute in state: " + mState));
            }
        }
    }

    @NonNull
    private List<OutputConfigurationCompat> getUniqueOutputConfigurations(
            @NonNull List<OutputConfigurationCompat> outputConfigurations) {
        List<Surface> addedSurfaces = new ArrayList<>();
        List<OutputConfigurationCompat> results = new ArrayList<>();
        for (OutputConfigurationCompat outputConfiguration : outputConfigurations) {
            if (addedSurfaces.contains(outputConfiguration.getSurface())) {
                // Surface already added,  ignore this outputConfiguration.
                continue;
            }
            addedSurfaces.add(outputConfiguration.getSurface());
            results.add(outputConfiguration);
        }
        return results;
    }

    @NonNull
    private OutputConfigurationCompat getOutputConfigurationCompat(
            @NonNull SessionConfig.OutputConfig outputConfig,
            @NonNull Map<DeferrableSurface, Surface> configuredSurfaceMap,
            @Nullable String physicalCameraIdForAllStreams) {
        Surface surface = configuredSurfaceMap.get(outputConfig.getSurface());
        Preconditions.checkNotNull(surface,
                "Surface in OutputConfig not found in configuredSurfaceMap.");

        OutputConfigurationCompat outputConfiguration =
                new OutputConfigurationCompat(outputConfig.getSurfaceGroupId(),
                        surface);
        // Set the desired physical camera ID, or null to use the logical stream.
        // TODO(b/219414502): Configure different streams with different physical
        //  camera IDs.
        if (physicalCameraIdForAllStreams != null) {
            outputConfiguration.setPhysicalCameraId(physicalCameraIdForAllStreams);
        } else {
            outputConfiguration.setPhysicalCameraId(
                    outputConfig.getPhysicalCameraId());
        }

        if (!outputConfig.getSharedSurfaces().isEmpty()) {
            outputConfiguration.enableSurfaceSharing();
            for (DeferrableSurface sharedDeferSurface : outputConfig.getSharedSurfaces()) {
                Surface sharedSurface = configuredSurfaceMap.get(sharedDeferSurface);
                Preconditions.checkNotNull(sharedSurface,
                        "Surface in OutputConfig not found in configuredSurfaceMap.");
                outputConfiguration.addSurface(sharedSurface);
            }
        }

        long dynamicRangeProfile = DynamicRangeProfiles.STANDARD;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            DynamicRange requestedDynamicRange = outputConfig.getDynamicRange();
            Long dynamicRangeProfileOrNull =
                    DynamicRangeConversions.dynamicRangeToFirstSupportedProfile(
                            outputConfig.getDynamicRange(),
                            mDynamicRangesCompat.toDynamicRangeProfiles());
            if (dynamicRangeProfileOrNull == null) {
                Logger.e(TAG, "Requested dynamic range is not supported. Defaulting to STANDARD "
                        + "dynamic range profile.\nRequested dynamic range:\n  "
                        + requestedDynamicRange);
            } else {
                dynamicRangeProfile = dynamicRangeProfileOrNull;
            }
        }
        outputConfiguration.setDynamicRangeProfile(dynamicRangeProfile);
        return outputConfiguration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        synchronized (mSessionLock) {
            switch (mState) {
                case UNINITIALIZED:
                    throw new IllegalStateException(
                            "close() should not be possible in state: " + mState);
                case GET_SURFACE:
                    Preconditions.checkNotNull(mSynchronizedCaptureSessionOpener, "The "
                            + "Opener shouldn't null in state:" + mState);
                    mSynchronizedCaptureSessionOpener.stop();
                    // Fall through
                case INITIALIZED:
                    mState = State.RELEASED;
                    break;
                case OPENED:
                    // Only issue onDisableSession requests at OPENED state.
                    if (mSessionConfig != null) {
                        List<CaptureConfig> configList =
                                mCameraEventCallbacks.createComboCallback().onDisableSession();
                        if (!configList.isEmpty()) {
                            try {
                                issueCaptureRequests(setupConfiguredSurface(configList));
                            } catch (IllegalStateException e) {
                                // We couldn't issue the request before close the capture session,
                                // but we should continue the close flow.
                                Logger.e(TAG, "Unable to issue the request before close the "
                                        + "capture session", e);
                            }
                        }
                    }
                    // Not break close flow. Fall through
                case OPENING:
                    Preconditions.checkNotNull(mSynchronizedCaptureSessionOpener, "The "
                            + "Opener shouldn't null in state:" + mState);
                    mSynchronizedCaptureSessionOpener.stop();
                    mState = State.CLOSED;
                    mSessionConfig = null;

                    break;
                case CLOSED:
                case RELEASING:
                case RELEASED:
                    break;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("ObjectToString")
    @NonNull
    @Override
    public ListenableFuture<Void> release(boolean abortInFlightCaptures) {
        synchronized (mSessionLock) {
            switch (mState) {
                case UNINITIALIZED:
                    throw new IllegalStateException(
                            "release() should not be possible in state: " + mState);
                case OPENED:
                case CLOSED:
                    if (mSynchronizedCaptureSession != null) {
                        if (abortInFlightCaptures) {
                            try {
                                mSynchronizedCaptureSession.abortCaptures();
                            } catch (CameraAccessException e) {
                                // We couldn't abort the captures, but we should continue on to
                                // release the session.
                                Logger.e(TAG, "Unable to abort captures.", e);
                            }
                        }
                        mSynchronizedCaptureSession.close();
                    }
                    // Fall through
                case OPENING:
                    mCameraEventCallbacks.createComboCallback().onDeInitSession();
                    mState = State.RELEASING;
                    Preconditions.checkNotNull(mSynchronizedCaptureSessionOpener, "The "
                            + "Opener shouldn't null in state:" + mState);
                    if (mSynchronizedCaptureSessionOpener.stop()) {
                        // The CameraCaptureSession doesn't created finish the release flow
                        // directly.
                        finishClose();
                        break;
                    }
                    // Fall through
                case RELEASING:
                    if (mReleaseFuture == null) {
                        mReleaseFuture = CallbackToFutureAdapter.getFuture(
                                completer -> {
                                    synchronized (mSessionLock) {
                                        Preconditions.checkState(mReleaseCompleter == null,
                                                "Release completer expected to be null");
                                        mReleaseCompleter = completer;
                                        return "Release[session=" + CaptureSession.this + "]";
                                    }
                                });
                    }

                    return mReleaseFuture;
                case GET_SURFACE:
                    Preconditions.checkNotNull(mSynchronizedCaptureSessionOpener, "The "
                            + "Opener shouldn't null in state:" + mState);
                    mSynchronizedCaptureSessionOpener.stop();
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void issueCaptureRequests(@NonNull List<CaptureConfig> captureConfigs) {
        synchronized (mSessionLock) {
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

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public List<CaptureConfig> getCaptureConfigs() {
        synchronized (mSessionLock) {
            return Collections.unmodifiableList(mCaptureConfigs);
        }
    }

    /** Returns the current state of the session. */
    State getState() {
        synchronized (mSessionLock) {
            return mState;
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @GuardedBy("mSessionLock")
    void finishClose() {
        if (mState == State.RELEASED) {
            Logger.d(TAG, "Skipping finishClose due to being state RELEASED.");
            return;
        }

        mState = State.RELEASED;
        mSynchronizedCaptureSession = null;

        if (mReleaseCompleter != null) {
            mReleaseCompleter.set(null);
            mReleaseCompleter = null;
        }
    }

    /**
     * Sets the {@link CaptureRequest} so that the camera will start producing data.
     *
     * <p>It will stop running repeating if there are no surfaces.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    int issueRepeatingCaptureRequests(@Nullable SessionConfig sessionConfig) {
        synchronized (mSessionLock) {
            if (sessionConfig == null) {
                Logger.d(TAG, "Skipping issueRepeatingCaptureRequests for no configuration case.");
                return -1;
            }

            if (mState != State.OPENED) {
                Logger.d(TAG, "Skipping issueRepeatingCaptureRequests due to session closed");
                return -1;
            }

            CaptureConfig captureConfig = sessionConfig.getRepeatingCaptureConfig();
            if (captureConfig.getSurfaces().isEmpty()) {
                Logger.d(TAG, "Skipping issueRepeatingCaptureRequests for no surface.");
                try {
                    // At least from Android L, framework will ignore the stopRepeating() if
                    // there is no ongoing repeating request, so it should be safe to always call
                    // stopRepeating() without checking if there is a repeating request.
                    mSynchronizedCaptureSession.stopRepeating();
                } catch (CameraAccessException e) {
                    Logger.e(TAG, "Unable to access camera: " + e.getMessage());
                    Thread.dumpStack();
                }
                return -1;
            }

            try {
                Logger.d(TAG, "Issuing request for session.");

                // The override priority for implementation options
                // P1 CameraEventCallback onRepeating options
                // P2 SessionConfig options
                CaptureConfig.Builder captureConfigBuilder = CaptureConfig.Builder.from(
                        captureConfig);

                mCameraEventOnRepeatingOptions = mergeOptions(
                        mCameraEventCallbacks.createComboCallback().onRepeating());
                captureConfigBuilder.addImplementationOptions(mCameraEventOnRepeatingOptions);

                CaptureRequest captureRequest = Camera2CaptureRequestBuilder.build(
                        captureConfigBuilder.build(), mSynchronizedCaptureSession.getDevice(),
                        mConfiguredSurfaceMap);
                if (captureRequest == null) {
                    Logger.d(TAG, "Skipping issuing empty request for session.");
                    return -1;
                }

                CameraCaptureSession.CaptureCallback comboCaptureCallback =
                        createCamera2CaptureCallback(
                                captureConfig.getCameraCaptureCallbacks(),
                                mCaptureCallback);

                return mSynchronizedCaptureSession.setSingleRepeatingRequest(captureRequest,
                        comboCaptureCallback);
            } catch (CameraAccessException e) {
                Logger.e(TAG, "Unable to access camera: " + e.getMessage());
                Thread.dumpStack();
            }

            return -1;
        }
    }

    /** Issues mCaptureConfigs to {@link CameraCaptureSession}. */
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @GuardedBy("mSessionLock")
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

    /**
     * Issues input CaptureConfig list to {@link CameraCaptureSession}.
     *
     * @return A unique capture sequence ID or -1 if request is not submitted.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    int issueBurstCaptureRequest(List<CaptureConfig> captureConfigs) {
        synchronized (mSessionLock) {
            if (mState != State.OPENED) {
                Logger.d(TAG, "Skipping issueBurstCaptureRequest due to session closed");
                return -1;
            }
            if (captureConfigs.isEmpty()) {
                return -1;
            }
            try {
                CameraBurstCaptureCallback callbackAggregator = new CameraBurstCaptureCallback();
                List<CaptureRequest> captureRequests = new ArrayList<>();
                boolean isStillCapture = false;
                Logger.d(TAG, "Issuing capture request.");
                for (CaptureConfig captureConfig : captureConfigs) {
                    if (captureConfig.getSurfaces().isEmpty()) {
                        Logger.d(TAG, "Skipping issuing empty capture request.");
                        continue;
                    }

                    // Validate all surfaces belong to configured surfaces map
                    boolean surfacesValid = true;
                    for (DeferrableSurface surface : captureConfig.getSurfaces()) {
                        if (!mConfiguredSurfaceMap.containsKey(surface)) {
                            Logger.d(TAG,
                                    "Skipping capture request with invalid surface: " + surface);
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

                    if (captureConfig.getTemplateType() == CameraDevice.TEMPLATE_STILL_CAPTURE) {
                        isStillCapture = true;
                    }
                    CaptureConfig.Builder captureConfigBuilder = CaptureConfig.Builder.from(
                            captureConfig);

                    if (captureConfig.getTemplateType() == CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG
                            && captureConfig.getCameraCaptureResult() != null) {
                        captureConfigBuilder.setCameraCaptureResult(
                                captureConfig.getCameraCaptureResult());
                    }

                    // The override priority for implementation options
                    // P1 Single capture options
                    // P2 CameraEventCallback onRepeating options
                    // P3 SessionConfig options
                    if (mSessionConfig != null) {
                        captureConfigBuilder.addImplementationOptions(
                                mSessionConfig.getRepeatingCaptureConfig()
                                        .getImplementationOptions());
                    }

                    captureConfigBuilder.addImplementationOptions(mCameraEventOnRepeatingOptions);

                    // Need to override again since single capture options has highest priority.
                    captureConfigBuilder.addImplementationOptions(
                            captureConfig.getImplementationOptions());

                    CaptureRequest captureRequest = Camera2CaptureRequestBuilder.build(
                            captureConfigBuilder.build(), mSynchronizedCaptureSession.getDevice(),
                            mConfiguredSurfaceMap);
                    if (captureRequest == null) {
                        Logger.d(TAG, "Skipping issuing request without surface.");
                        return -1;
                    }

                    List<CameraCaptureSession.CaptureCallback> cameraCallbacks = new ArrayList<>();
                    for (CameraCaptureCallback callback :
                            captureConfig.getCameraCaptureCallbacks()) {
                        CaptureCallbackConverter.toCaptureCallback(callback, cameraCallbacks);
                    }
                    callbackAggregator.addCamera2Callbacks(captureRequest, cameraCallbacks);
                    captureRequests.add(captureRequest);
                }

                if (!captureRequests.isEmpty()) {
                    if (mStillCaptureFlow
                            .shouldStopRepeatingBeforeCapture(captureRequests, isStillCapture)) {
                        mSynchronizedCaptureSession.stopRepeating();
                        callbackAggregator.setCaptureSequenceCallback(
                                (session, sequenceId, isAborted) -> {
                                    synchronized (mSessionLock) {
                                        if (mState == State.OPENED) {
                                            issueRepeatingCaptureRequests(mSessionConfig);
                                        }
                                    }
                                });
                    }
                    if (mTorchStateReset.isTorchResetRequired(captureRequests, isStillCapture)) {
                        callbackAggregator.addCamera2Callbacks(
                                captureRequests.get(captureRequests.size() - 1),
                                Collections.singletonList(new CaptureCallback() {

                                    @Override
                                    public void onCaptureCompleted(
                                            @NonNull CameraCaptureSession session,
                                            @NonNull CaptureRequest request,
                                            @NonNull TotalCaptureResult result) {
                                        synchronized (mSessionLock) {
                                            if (mSessionConfig == null) {
                                                return;
                                            }
                                            CaptureConfig repeatingConfig =
                                                    mSessionConfig.getRepeatingCaptureConfig();
                                            Logger.d(TAG, "Submit FLASH_MODE_OFF request");
                                            issueCaptureRequests(Collections.singletonList(
                                                    mTorchStateReset.createTorchResetRequest(
                                                            repeatingConfig)));
                                        }
                                    }
                                }));
                    }
                    return mSynchronizedCaptureSession.captureBurstRequests(captureRequests,
                            callbackAggregator);
                } else {
                    Logger.d(TAG,
                            "Skipping issuing burst request due to no valid request elements");
                }
            } catch (CameraAccessException e) {
                Logger.e(TAG, "Unable to access camera: " + e.getMessage());
                Thread.dumpStack();
            }

            return -1;
        }
    }

    /**
     * Discards all captures currently pending and in-progress as fast as possible.
     */
    void abortCaptures() {
        synchronized (mSessionLock) {
            if (mState != State.OPENED) {
                Logger.e(TAG, "Unable to abort captures. Incorrect state:" + mState);
                return;
            }

            try {
                mSynchronizedCaptureSession.abortCaptures();
            } catch (CameraAccessException e) {
                Logger.e(TAG, "Unable to abort captures.", e);
            }
        }
    }

    /**
     * Cancels any ongoing repeating capture.
     */
    void stopRepeating() {
        synchronized (mSessionLock) {
            if (mState != State.OPENED) {
                Logger.e(TAG, "Unable to stop repeating. Incorrect state:" + mState);
                return;
            }

            try {
                mSynchronizedCaptureSession.stopRepeating();
            } catch (CameraAccessException e) {
                Logger.e(TAG, "Unable to stop repeating.", e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cancelIssuedCaptureRequests() {
        List<CaptureConfig> captureConfigs = null;
        synchronized (mSessionLock) {
            if (!mCaptureConfigs.isEmpty()) {
                captureConfigs = new ArrayList<>(mCaptureConfigs);
                mCaptureConfigs.clear();
            }
        }

        if (captureConfigs != null) {
            for (CaptureConfig captureConfig : captureConfigs) {
                for (CameraCaptureCallback cameraCaptureCallback :
                        captureConfig.getCameraCaptureCallbacks()) {
                    cameraCaptureCallback.onCaptureCancelled();
                }
            }
        }
    }

    @GuardedBy("mSessionLock")
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
                        Logger.d(TAG, "Detect conflicting option "
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
    final class StateCallback extends SynchronizedCaptureSession.StateCallback {

        /**
         * {@inheritDoc}
         *
         * <p>Once the {@link CameraCaptureSession} has been configured then the capture request
         * will be immediately issued.
         */
        @Override
        public void onConfigured(@NonNull SynchronizedCaptureSession session) {
            synchronized (mSessionLock) {
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
                        mSynchronizedCaptureSession = session;

                        // Issue capture request of enableSession if exists.
                        if (mSessionConfig != null) {
                            List<CaptureConfig> list =
                                    mCameraEventCallbacks.createComboCallback().onEnableSession();
                            if (!list.isEmpty()) {
                                issueBurstCaptureRequest(setupConfiguredSurface(list));
                            }
                        }

                        Logger.d(TAG, "Attempting to send capture request onConfigured");
                        issueRepeatingCaptureRequests(mSessionConfig);
                        issuePendingCaptureRequest();
                        break;
                    case CLOSED:
                        mSynchronizedCaptureSession = session;
                        break;
                    case RELEASING:
                        session.close();
                        break;
                }
                Logger.d(TAG, "CameraCaptureSession.onConfigured() mState=" + mState);
            }
        }

        @Override
        public void onReady(@NonNull SynchronizedCaptureSession session) {
            synchronized (mSessionLock) {
                switch (mState) {
                    case UNINITIALIZED:
                        throw new IllegalStateException(
                                "onReady() should not be possible in state: " + mState);
                    default:
                }
                Logger.d(TAG, "CameraCaptureSession.onReady() " + mState);
            }
        }

        @Override
        public void onSessionFinished(@NonNull SynchronizedCaptureSession session) {
            synchronized (mSessionLock) {
                if (mState == State.UNINITIALIZED) {
                    throw new IllegalStateException(
                            "onSessionFinished() should not be possible in state: " + mState);
                }
                Logger.d(TAG, "onSessionFinished()");

                finishClose();
            }
        }

        @Override
        public void onConfigureFailed(@NonNull SynchronizedCaptureSession session) {
            synchronized (mSessionLock) {
                switch (mState) {
                    case UNINITIALIZED:
                    case INITIALIZED:
                    case GET_SURFACE:
                    case OPENED:
                        throw new IllegalStateException(
                                "onConfigureFailed() should not be possible in state: " + mState);
                    case OPENING:
                    case CLOSED:
                    case RELEASING:
                        // For CaptureSession onConfigureFailed in framework, it will not allow
                        // any close function or callback work. Calling session.close() will not
                        // trigger StateCallback.onClosed(). It has to complete the close flow
                        // internally. Check b/147402661 for detail.
                        finishClose();
                        break;
                    case RELEASED:
                        Logger.d(TAG, "ConfigureFailed callback after change to RELEASED state");
                        break;
                }
                Logger.e(TAG, "CameraCaptureSession.onConfigureFailed() " + mState);
            }
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @GuardedBy("mSessionLock")
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
}
