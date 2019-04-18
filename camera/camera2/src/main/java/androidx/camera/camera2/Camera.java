/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.camera2;

import android.annotation.SuppressLint;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.BaseCamera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraDeviceStateCallbacks;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraX;
import androidx.camera.core.CaptureConfig;
import androidx.camera.core.DeferrableSurface;
import androidx.camera.core.ImmediateSurface;
import androidx.camera.core.SessionConfig;
import androidx.camera.core.SessionConfig.ValidatingBuilder;
import androidx.camera.core.UseCase;
import androidx.camera.core.UseCaseAttachState;
import androidx.core.os.BuildCompat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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
final class Camera implements BaseCamera {
    private static final String TAG = "Camera";

    private final Object mAttachedUseCaseLock = new Object();

    /** Map of the use cases to the information on their state. */
    @GuardedBy("mAttachedUseCaseLock")
    private final UseCaseAttachState mUseCaseAttachState;

    /** The identifier for the {@link CameraDevice} */
    private final String mCameraId;

    /** Handle to the camera service. */
    private final CameraManager mCameraManager;

    private final Object mCameraInfoLock = new Object();
    /** The handler for camera callbacks and use case state management calls. */
    private final Handler mHandler;
    /**
     * State variable for tracking state of the camera.
     *
     * <p>Is an atomic reference because it is initialized in the constructor which is not called on
     * same thread as any of the other methods and callbacks.
     */
    final AtomicReference<State> mState = new AtomicReference<>(State.UNINITIALIZED);
    /** The camera control shared across all use cases bound to this Camera. */
    private final CameraControl mCameraControl;
    private final StateCallback mStateCallback = new StateCallback();
    /** Information about the characteristics of this camera */
    // Nullable because this is lazily instantiated
    @GuardedBy("mCameraInfoLock")
    @Nullable
    private CameraInfo mCameraInfo;
    /** The handle to the opened camera. */
    @Nullable
    CameraDevice mCameraDevice;
    /** The configured session which handles issuing capture requests. */
    private CaptureSession mCaptureSession = new CaptureSession(null);
    /** The session configuration of camera control. */
    private SessionConfig mCameraControlSessionConfig = SessionConfig.defaultEmptySessionConfig();

    /**
     * Constructor for a camera.
     *
     * @param cameraManager the camera service used to retrieve a camera
     * @param cameraId      the name of the camera as defined by the camera service
     * @param handler       the handler for the thread on which all camera operations run
     */
    Camera(CameraManager cameraManager, String cameraId, Handler handler) {
        mCameraManager = cameraManager;
        mCameraId = cameraId;
        mHandler = handler;
        mUseCaseAttachState = new UseCaseAttachState(cameraId);
        mState.set(State.INITIALIZED);
        mCameraControl = new Camera2CameraControl(this, handler);
    }

    /**
     * Open the camera asynchronously.
     *
     * <p>Once the camera has been opened use case state transitions can be used to control the
     * camera pipeline.
     */
    @Override
    public void open() {
        if (Looper.myLooper() != mHandler.getLooper()) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Camera.this.open();
                }
            });
            return;
        }

        switch (mState.get()) {
            case INITIALIZED:
                openCameraDevice();
                break;
            case CLOSING:
                mState.set(State.REOPENING);
                break;
            default:
                Log.d(TAG, "open() ignored due to being in state: " + mState.get());
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
        if (Looper.myLooper() != mHandler.getLooper()) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Camera.this.close();
                }
            });
            return;
        }

        Log.d(TAG, "Closing camera: " + mCameraId);
        switch (mState.get()) {
            case OPENED:
                mState.set(State.CLOSING);
                closeCameraResource();
                break;
            case OPENING:
            case REOPENING:
                mState.set(State.CLOSING);
                break;
            default:
                Log.d(TAG, "close() ignored due to being in state: " + mState.get());
        }
    }

    private void configAndClose() {
        switch (mState.get()) {
            case OPENED:
                mState.set(State.CLOSING);

                resetCaptureSession();

                final SurfaceTexture surfaceTexture = new SurfaceTexture(0);
                surfaceTexture.setDefaultBufferSize(640, 480);
                final Surface surface = new Surface(surfaceTexture);
                final Runnable surfaceReleaseRunner = new Runnable() {
                    @Override
                    public void run() {
                        surface.release();
                        surfaceTexture.release();
                    }
                };

                SessionConfig.Builder builder = new SessionConfig.Builder();
                builder.addNonRepeatingSurface(new ImmediateSurface(surface));
                builder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
                builder.addSessionStateCallback(new CameraCaptureSession.StateCallback() {

                    @Override
                    public void onConfigured(CameraCaptureSession session) {
                        session.close();
                    }

                    @Override
                    public void onConfigureFailed(CameraCaptureSession session) {
                        closeCameraResource();
                        surfaceReleaseRunner.run();
                    }

                    @Override
                    public void onClosed(CameraCaptureSession session) {
                        closeCameraResource();
                        surfaceReleaseRunner.run();
                    }
                });

                try {
                    Log.d(TAG, "Start configAndClose.");
                    new CaptureSession(null).open(builder.build(), mCameraDevice);
                } catch (CameraAccessException e) {
                    Log.d(TAG, "Unable to configure camera " + mCameraId + " due to "
                            + e.getMessage());
                    surfaceReleaseRunner.run();
                }

                break;
            case OPENING:
            case REOPENING:
                mState.set(State.CLOSING);
                break;
            default:
                Log.d(TAG, "configAndClose() ignored due to being in state: " + mState.get());
        }

    }

    void closeCameraResource() {
        mCaptureSession.close();
        mCameraDevice.close();
        mCaptureSession.notifyCameraDeviceClose();
        resetCaptureSession();
        mCameraDevice = null;
    }

    /**
     * Release the camera.
     *
     * <p>Once the camera is released it is permanently closed. A new instance must be created to
     * access the camera.
     */
    @Override
    public void release() {
        if (Looper.myLooper() != mHandler.getLooper()) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Camera.this.release();
                }
            });
            return;
        }

        switch (mState.get()) {
            case INITIALIZED:
                mState.set(State.RELEASED);
                break;
            case OPENED:
                mState.set(State.RELEASING);
                mCameraDevice.close();
                mCaptureSession.notifyCameraDeviceClose();
                break;
            case OPENING:
            case CLOSING:
            case REOPENING:
                mState.set(State.RELEASING);
                break;
            default:
                Log.d(TAG, "release() ignored due to being in state: " + mState.get());
        }
    }

    /**
     * Sets the use case in a state to issue capture requests.
     *
     * <p>The use case must also be online in order for it to issue capture requests.
     */
    @Override
    public void onUseCaseActive(final UseCase useCase) {
        if (Looper.myLooper() != mHandler.getLooper()) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Camera.this.onUseCaseActive(useCase);
                }
            });
            return;
        }

        Log.d(TAG, "Use case " + useCase + " ACTIVE for camera " + mCameraId);

        synchronized (mAttachedUseCaseLock) {
            mUseCaseAttachState.setUseCaseActive(useCase);
        }
        updateCaptureSessionConfig();
    }

    /** Removes the use case from a state of issuing capture requests. */
    @Override
    public void onUseCaseInactive(final UseCase useCase) {
        if (Looper.myLooper() != mHandler.getLooper()) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Camera.this.onUseCaseInactive(useCase);
                }
            });
            return;
        }

        Log.d(TAG, "Use case " + useCase + " INACTIVE for camera " + mCameraId);
        synchronized (mAttachedUseCaseLock) {
            mUseCaseAttachState.setUseCaseInactive(useCase);
        }

        updateCaptureSessionConfig();
    }

    /** Updates the capture requests based on the latest settings. */
    @Override
    public void onUseCaseUpdated(final UseCase useCase) {
        if (Looper.myLooper() != mHandler.getLooper()) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Camera.this.onUseCaseUpdated(useCase);
                }
            });
            return;
        }

        Log.d(TAG, "Use case " + useCase + " UPDATED for camera " + mCameraId);
        synchronized (mAttachedUseCaseLock) {
            mUseCaseAttachState.updateUseCase(useCase);
        }

        updateCaptureSessionConfig();
    }

    @Override
    public void onUseCaseReset(final UseCase useCase) {
        if (Looper.myLooper() != mHandler.getLooper()) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Camera.this.onUseCaseReset(useCase);
                }
            });
            return;
        }

        Log.d(TAG, "Use case " + useCase + " RESET for camera " + mCameraId);
        synchronized (mAttachedUseCaseLock) {
            mUseCaseAttachState.updateUseCase(useCase);
        }

        updateCaptureSessionConfig();
        openCaptureSession();
    }

    /**
     * Sets the use case to be in the state where the capture session will be configured to handle
     * capture requests from the use case.
     */
    @Override
    public void addOnlineUseCase(final Collection<UseCase> useCases) {
        if (useCases.isEmpty()) {
            return;
        }

        if (Looper.myLooper() != mHandler.getLooper()) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Camera.this.addOnlineUseCase(useCases);
                }
            });
            return;
        }

        Log.d(TAG, "Use cases " + useCases + " ONLINE for camera " + mCameraId);
        synchronized (mAttachedUseCaseLock) {
            for (UseCase useCase : useCases) {
                mUseCaseAttachState.setUseCaseOnline(useCase);
            }
        }

        open();
        updateCaptureSessionConfig();
        openCaptureSession();
    }

    /**
     * Removes the use case to be in the state where the capture session will be configured to
     * handle capture requests from the use case.
     */
    @Override
    public void removeOnlineUseCase(final Collection<UseCase> useCases) {
        if (useCases.isEmpty()) {
            return;
        }

        if (Looper.myLooper() != mHandler.getLooper()) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Camera.this.removeOnlineUseCase(useCases);
                }
            });
            return;
        }

        Log.d(TAG, "Use cases " + useCases + " OFFLINE for camera " + mCameraId);
        synchronized (mAttachedUseCaseLock) {
            for (UseCase useCase : useCases) {
                mUseCaseAttachState.setUseCaseOffline(useCase);
            }

            if (mUseCaseAttachState.getOnlineUseCases().isEmpty()) {

                boolean isLegacyDevice = false;
                try {
                    Camera2CameraInfo camera2CameraInfo = (Camera2CameraInfo) getCameraInfo();
                    isLegacyDevice = camera2CameraInfo.getSupportedHardwareLevel()
                            == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY;
                } catch (CameraInfoUnavailableException e) {
                    Log.w(TAG, "Check legacy device failed.", e);
                }

                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M && !BuildCompat.isAtLeastQ()
                        && isLegacyDevice) {
                    // To configure surface again before close camera. This step would disconnect
                    // previous connected surface in some legacy device to prevent exception.
                    configAndClose();
                } else {
                    close();
                }
                return;
            }
        }

        openCaptureSession();
        updateCaptureSessionConfig();
    }

    /** Returns an interface to retrieve characteristics of the camera. */
    @Override
    public CameraInfo getCameraInfo() throws CameraInfoUnavailableException {
        synchronized (mCameraInfoLock) {
            if (mCameraInfo == null) {
                // Lazily instantiate camera info
                mCameraInfo = new Camera2CameraInfo(mCameraManager, mCameraId);
            }

            return mCameraInfo;
        }
    }

    /** Opens the camera device */
    // TODO(b/124268878): Handle SecurityException and require permission in manifest.
    @SuppressLint("MissingPermission")
    void openCameraDevice() {
        mState.set(State.OPENING);

        Log.d(TAG, "Opening camera: " + mCameraId);

        try {
            mCameraManager.openCamera(mCameraId, createDeviceStateCallback(), mHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Unable to open camera " + mCameraId + " due to " + e.getMessage());
            mState.set(State.INITIALIZED);
        }
    }

    /** Updates the capture request configuration for the current capture session. */
    private void updateCaptureSessionConfig() {
        ValidatingBuilder validatingBuilder;
        synchronized (mAttachedUseCaseLock) {
            validatingBuilder = mUseCaseAttachState.getActiveAndOnlineBuilder();
        }

        if (validatingBuilder.isValid()) {
            // Apply CameraControl's SessionConfig to let CameraControl be able to control
            // Repeating Request and process results.
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
    void openCaptureSession() {
        ValidatingBuilder validatingBuilder;
        synchronized (mAttachedUseCaseLock) {
            validatingBuilder = mUseCaseAttachState.getOnlineBuilder();
        }
        if (!validatingBuilder.isValid()) {
            Log.d(TAG, "Unable to create capture session due to conflicting configurations");
            return;
        }

        resetCaptureSession();

        if (mCameraDevice == null) {
            Log.d(TAG, "CameraDevice is null");
            return;
        }

        try {
            mCaptureSession.open(validatingBuilder.build(), mCameraDevice);
        } catch (CameraAccessException e) {
            Log.d(TAG, "Unable to configure camera " + mCameraId + " due to " + e.getMessage());
        }
    }

    /**
     * Closes the currently opened capture session, so it can be safely disposed. Replaces the old
     * session with a new session initialized with the old session's configuration.
     */
    void resetCaptureSession() {
        Log.d(TAG, "Closing Capture Session");

        // Recreate an initialized (but not opened) capture session from the previous configuration
        SessionConfig previousSessionConfig = mCaptureSession.getSessionConfig();

        mCaptureSession.close();

        List<CaptureConfig> unissuedCaptureConfigs = mCaptureSession.getCaptureConfigs();
        mCaptureSession = new CaptureSession(mHandler);
        mCaptureSession.setSessionConfig(previousSessionConfig);
        // When the previous capture session has not reached the open state, the issued single
        // capture
        // requests will still be in request queue and will need to be passed to the next capture
        // session.
        mCaptureSession.issueSingleCaptureRequests(unissuedCaptureConfigs);
    }

    private CameraDevice.StateCallback createDeviceStateCallback() {
        synchronized (mAttachedUseCaseLock) {
            SessionConfig config = mUseCaseAttachState.getOnlineBuilder().build();

            List<CameraDevice.StateCallback> configuredStateCallbacks =
                    config.getDeviceStateCallbacks();
            List<CameraDevice.StateCallback> allStateCallbacks =
                    new ArrayList<>(configuredStateCallbacks);
            allStateCallbacks.add(mStateCallback);
            return CameraDeviceStateCallbacks.createComboCallback(allStateCallbacks);
        }
    }

    /**
     * Checks if there's valid repeating surface and attaches one to {@link CaptureConfig.Builder}.
     *
     * @param captureConfigBuilder the configuration builder to attach a repeating surface
     * @return True if repeating surface has been successfully attached, otherwise false.
     */
    private boolean checkAndAttachRepeatingSurface(CaptureConfig.Builder captureConfigBuilder) {
        Collection<UseCase> activeUseCases;
        synchronized (mAttachedUseCaseLock) {
            activeUseCases = mUseCaseAttachState.getActiveAndOnlineUseCases();
        }

        DeferrableSurface repeatingSurface = null;
        for (UseCase useCase : activeUseCases) {
            SessionConfig sessionConfig = useCase.getSessionConfig(mCameraId);
            List<DeferrableSurface> surfaces =
                    sessionConfig.getRepeatingCaptureConfig().getSurfaces();
            if (!surfaces.isEmpty()) {
                // When an use case is active, all surfaces in its CaptureConfig are added to the
                // repeating request. Choose the first one here as the repeating surface.
                repeatingSurface = surfaces.get(0);
                break;
            }
        }

        if (repeatingSurface == null) {
            Log.w(TAG, "Unable to find a repeating surface to attach to CaptureConfig");
            return false;
        }

        captureConfigBuilder.addSurface(repeatingSurface);
        return true;
    }

    /** Returns the Camera2CameraControl attached to Camera */
    @Override
    public CameraControl getCameraControl() {
        return mCameraControl;
    }

    /**
     * Submits single request
     *
     * @param captureConfig capture configuration used for creating CaptureRequest
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public void submitSingleRequest(final CaptureConfig captureConfig) {
        if (Looper.myLooper() != mHandler.getLooper()) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Camera.this.submitSingleRequest(captureConfig);
                }
            });
            return;
        }

        // Recreates the Builder to add extra config needed
        CaptureConfig.Builder builder = CaptureConfig.Builder.from(captureConfig);

        if (captureConfig.getSurfaces().isEmpty() && captureConfig.isUseRepeatingSurface()) {
            // Checks and attaches if there's valid repeating surface. If there's no, skip this
            // single request.
            if (!checkAndAttachRepeatingSurface(builder)) {
                return;
            }
        }

        Log.d(TAG, "issue single capture request for camera " + mCameraId);

        mCaptureSession.issueSingleCaptureRequest(builder.build());
    }

    /** {@inheritDoc} */
    @Override
    public void onCameraControlUpdateSessionConfig(SessionConfig sessionConfig) {
        mCameraControlSessionConfig = sessionConfig;
        updateCaptureSessionConfig();
    }

    /** {@inheritDoc} */
    @Override
    public void onCameraControlSingleRequest(CaptureConfig captureConfig) {
        submitSingleRequest(captureConfig);
    }

    enum State {
        /** The default state of the camera before construction. */
        UNINITIALIZED,
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

    final class StateCallback extends CameraDevice.StateCallback {

        @Override
        public void onOpened(CameraDevice cameraDevice) {
            Log.d(TAG, "CameraDevice.onOpened(): " + cameraDevice.getId());
            switch (mState.get()) {
                case CLOSING:
                case RELEASING:
                    cameraDevice.close();
                    Camera.this.mCameraDevice = null;
                    break;
                case OPENING:
                case REOPENING:
                    mState.set(State.OPENED);
                    Camera.this.mCameraDevice = cameraDevice;
                    openCaptureSession();
                    break;
                default:
                    throw new IllegalStateException(
                            "onOpened() should not be possible from state: " + mState.get());
            }
        }

        @Override
        public void onClosed(CameraDevice cameraDevice) {
            Log.d(TAG, "CameraDevice.onClosed(): " + cameraDevice.getId());

            resetCaptureSession();
            switch (mState.get()) {
                case CLOSING:
                    mState.set(State.INITIALIZED);
                    Camera.this.mCameraDevice = null;
                    break;
                case REOPENING:
                    mState.set(State.OPENING);
                    openCameraDevice();
                    break;
                case RELEASING:
                    mState.set(State.RELEASED);
                    Camera.this.mCameraDevice = null;
                    break;
                default:
                    CameraX.postError(
                            CameraX.ErrorCode.CAMERA_STATE_INCONSISTENT,
                            "Camera closed while in state: " + mState.get());
            }


        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            Log.d(TAG, "CameraDevice.onDisconnected(): " + cameraDevice.getId());
            resetCaptureSession();
            switch (mState.get()) {
                case CLOSING:
                    mState.set(State.INITIALIZED);
                    Camera.this.mCameraDevice = null;
                    break;
                case REOPENING:
                case OPENED:
                case OPENING:
                    mState.set(State.CLOSING);
                    cameraDevice.close();
                    Camera.this.mCameraDevice = null;
                    break;
                case RELEASING:
                    mState.set(State.RELEASED);
                    cameraDevice.close();
                    Camera.this.mCameraDevice = null;
                    break;
                default:
                    throw new IllegalStateException(
                            "onDisconnected() should not be possible from state: " + mState.get());
            }
        }

        private String getErrorMessage(int errorCode) {
            switch (errorCode) {
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

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            Log.e(
                    TAG,
                    "CameraDevice.onError(): "
                            + cameraDevice.getId()
                            + " with error: "
                            + getErrorMessage(error));
            resetCaptureSession();
            switch (mState.get()) {
                case CLOSING:
                    mState.set(State.INITIALIZED);
                    Camera.this.mCameraDevice = null;
                    break;
                case REOPENING:
                case OPENED:
                case OPENING:
                    mState.set(State.CLOSING);
                    cameraDevice.close();
                    Camera.this.mCameraDevice = null;
                    break;
                case RELEASING:
                    mState.set(State.RELEASED);
                    cameraDevice.close();
                    Camera.this.mCameraDevice = null;
                    break;
                default:
                    throw new IllegalStateException(
                            "onError() should not be possible from state: " + mState.get());
            }
        }
    }
}
