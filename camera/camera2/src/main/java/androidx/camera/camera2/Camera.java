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
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.BaseCamera;
import androidx.camera.core.BaseUseCase;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraDeviceStateCallbacks;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraX;
import androidx.camera.core.CaptureRequestConfiguration;
import androidx.camera.core.DeferrableSurface;
import androidx.camera.core.SessionConfiguration;
import androidx.camera.core.SessionConfiguration.ValidatingBuilder;
import androidx.camera.core.UseCaseAttachState;

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
final class Camera implements BaseCamera, Camera2RequestRunner {
    private static final String TAG = "Camera";

    private final Object attachedUseCaseLock = new Object();

    /** Map of the use cases to the information on their state. */
    @GuardedBy("attachedUseCaseLock")
    private final UseCaseAttachState useCaseAttachState;

    /** The identifier for the {@link CameraDevice} */
    private final String cameraId;

    /** Handle to the camera service. */
    private final CameraManager cameraManager;

    private final Object cameraInfoLock = new Object();
    /** The handler for camera callbacks and use case state management calls. */
    private final Handler handler;
    /**
     * State variable for tracking state of the camera.
     *
     * <p>Is an atomic reference because it is initialized in the constructor which is not called on
     * same thread as any of the other methods and callbacks.
     */
    final AtomicReference<State> state = new AtomicReference<>(State.UNINITIALIZED);
    /** The camera control shared across all use cases bound to this Camera. */
    private final CameraControl cameraControl;
    private final StateCallback stateCallback = new StateCallback();
    /** Information about the characteristics of this camera */
    // Nullable because this is lazily instantiated
    @GuardedBy("cameraInfoLock")
    @Nullable
    private CameraInfo cameraInfo;
    /** The handle to the opened camera. */
    @Nullable
    CameraDevice cameraDevice;
    /** The configured session which handles issuing capture requests. */
    private CaptureSession captureSession = new CaptureSession(null);

    /**
     * Constructor for a camera.
     *
     * @param cameraManager the camera service used to retrieve a camera
     * @param cameraId      the name of the camera as defined by the camera service
     * @param handler       the handler for the thread on which all camera operations run
     */
    Camera(CameraManager cameraManager, String cameraId, Handler handler) {
        this.cameraManager = cameraManager;
        this.cameraId = cameraId;
        this.handler = handler;
        useCaseAttachState = new UseCaseAttachState(cameraId);
        state.set(State.INITIALIZED);
        cameraControl = new Camera2CameraControl(this, handler);
    }

    /**
     * Open the camera asynchronously.
     *
     * <p>Once the camera has been opened use case state transitions can be used to control the
     * camera pipeline.
     */
    @Override
    public void open() {
        if (Looper.myLooper() != handler.getLooper()) {
            handler.post(() -> open());
            return;
        }

        switch (state.get()) {
            case INITIALIZED:
                openCameraDevice();
                break;
            case CLOSING:
                state.set(State.REOPENING);
                break;
            default:
                Log.d(TAG, "open() ignored due to being in state: " + state.get());
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
        if (Looper.myLooper() != handler.getLooper()) {
            handler.post(() -> close());
            return;
        }

        Log.d(TAG, "Closing camera: " + cameraId);
        switch (state.get()) {
            case OPENED:
                state.set(State.CLOSING);
                cameraDevice.close();
                cameraDevice = null;
                break;
            case OPENING:
            case REOPENING:
                state.set(State.CLOSING);
                break;
            default:
                Log.d(TAG, "close() ignored due to being in state: " + state.get());
        }
    }

    /**
     * Release the camera.
     *
     * <p>Once the camera is released it is permanently closed. A new instance must be created to
     * access the camera.
     */
    @Override
    public void release() {
        if (Looper.myLooper() != handler.getLooper()) {
            handler.post(() -> release());
            return;
        }

        switch (state.get()) {
            case INITIALIZED:
                state.set(State.RELEASED);
                break;
            case OPENED:
                state.set(State.RELEASING);
                cameraDevice.close();
                break;
            case OPENING:
            case CLOSING:
            case REOPENING:
                state.set(State.RELEASING);
                break;
            default:
                Log.d(TAG, "release() ignored due to being in state: " + state.get());
        }
    }

    /**
     * Sets the use case in a state to issue capture requests.
     *
     * <p>The use case must also be online in order for it to issue capture requests.
     */
    @Override
    public void onUseCaseActive(BaseUseCase useCase) {
        if (Looper.myLooper() != handler.getLooper()) {
            handler.post(() -> onUseCaseActive(useCase));
            return;
        }

        Log.d(TAG, "Use case " + useCase + " ACTIVE for camera " + cameraId);

        synchronized (attachedUseCaseLock) {
            useCaseAttachState.setUseCaseActive(useCase);
        }
        updateCaptureSessionConfiguration();
    }

    /** Removes the use case from a state of issuing capture requests. */
    @Override
    public void onUseCaseInactive(BaseUseCase useCase) {
        if (Looper.myLooper() != handler.getLooper()) {
            handler.post(() -> onUseCaseInactive(useCase));
            return;
        }

        Log.d(TAG, "Use case " + useCase + " INACTIVE for camera " + cameraId);
        synchronized (attachedUseCaseLock) {
            useCaseAttachState.setUseCaseInactive(useCase);
        }

        updateCaptureSessionConfiguration();
    }

    /** Updates the capture requests based on the latest settings. */
    @Override
    public void onUseCaseUpdated(BaseUseCase useCase) {
        if (Looper.myLooper() != handler.getLooper()) {
            handler.post(() -> onUseCaseUpdated(useCase));
            return;
        }

        Log.d(TAG, "Use case " + useCase + " UPDATED for camera " + cameraId);
        synchronized (attachedUseCaseLock) {
            useCaseAttachState.updateUseCase(useCase);
        }

        updateCaptureSessionConfiguration();
    }

    @Override
    public void onUseCaseReset(BaseUseCase useCase) {
        if (Looper.myLooper() != handler.getLooper()) {
            handler.post(() -> onUseCaseReset(useCase));
            return;
        }

        Log.d(TAG, "Use case " + useCase + " RESET for camera " + cameraId);
        synchronized (attachedUseCaseLock) {
            useCaseAttachState.updateUseCase(useCase);
        }

        updateCaptureSessionConfiguration();
        openCaptureSession();
    }

    @Override
    public void onUseCaseSingleRequest(
            BaseUseCase useCase, CaptureRequestConfiguration captureRequestConfiguration) {
        submitSingleRequest(captureRequestConfiguration);
    }

    /**
     * Sets the use case to be in the state where the capture session will be configured to handle
     * capture requests from the use case.
     */
    @Override
    public void addOnlineUseCase(Collection<BaseUseCase> useCases) {
        if (useCases.isEmpty()) {
            return;
        }

        if (Looper.myLooper() != handler.getLooper()) {
            handler.post(() -> addOnlineUseCase(useCases));
            return;
        }

        Log.d(TAG, "Use cases " + useCases + " ONLINE for camera " + cameraId);
        synchronized (attachedUseCaseLock) {
            for (BaseUseCase useCase : useCases) {
                useCaseAttachState.setUseCaseOnline(useCase);
            }
        }

        open();
        updateCaptureSessionConfiguration();
        openCaptureSession();
    }

    /**
     * Removes the use case to be in the state where the capture session will be configured to
     * handle capture requests from the use case.
     */
    @Override
    public void removeOnlineUseCase(Collection<BaseUseCase> useCases) {
        if (useCases.isEmpty()) {
            return;
        }

        if (Looper.myLooper() != handler.getLooper()) {
            handler.post(() -> removeOnlineUseCase(useCases));
            return;
        }

        Log.d(TAG, "Use cases " + useCases + " OFFLINE for camera " + cameraId);
        synchronized (attachedUseCaseLock) {
            for (BaseUseCase useCase : useCases) {
                useCaseAttachState.setUseCaseOffline(useCase);
            }

            if (useCaseAttachState.getOnlineUseCases().isEmpty()) {
                resetCaptureSession();
                close();
                return;
            }
        }

        updateCaptureSessionConfiguration();
    }

    /** Returns an interface to retrieve characteristics of the camera. */
    @Override
    public CameraInfo getCameraInfo() throws CameraInfoUnavailableException {
        synchronized (cameraInfoLock) {
            if (cameraInfo == null) {
                // Lazily instantiate camera info
                cameraInfo = new Camera2CameraInfo(cameraManager, cameraId);
            }

            return cameraInfo;
        }
    }

    /** Opens the camera device */
    // TODO(b/124268878): Handle SecurityException and require permission in manifest.
    @SuppressLint("MissingPermission")
    void openCameraDevice() {
        state.set(State.OPENING);

        Log.d(TAG, "Opening camera: " + cameraId);

        try {
            cameraManager.openCamera(cameraId, createDeviceStateCallback(), handler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Unable to open camera " + cameraId + " due to " + e.getMessage());
            state.set(State.INITIALIZED);
        }
    }

    /** Updates the capture request configuration for the current capture session. */
    private void updateCaptureSessionConfiguration() {
        ValidatingBuilder validatingBuilder;
        synchronized (attachedUseCaseLock) {
            validatingBuilder = useCaseAttachState.getActiveAndOnlineBuilder();
        }

        if (validatingBuilder.isValid()) {
            // Apply CameraControl's SessionConfiguration to let CameraControl be able to control
            // Repeating Request and process results.
            validatingBuilder.add(cameraControl.getControlSessionConfiguration());

            SessionConfiguration sessionConfiguration = validatingBuilder.build();
            captureSession.setSessionConfiguration(sessionConfiguration);
        }
    }

    /**
     * Opens a new capture session.
     *
     * <p>The previously opened session will be safely disposed of before the new session opened.
     */
    void openCaptureSession() {
        ValidatingBuilder validatingBuilder;
        synchronized (attachedUseCaseLock) {
            validatingBuilder = useCaseAttachState.getOnlineBuilder();
        }
        if (!validatingBuilder.isValid()) {
            Log.d(TAG, "Unable to create capture session due to conflicting configurations");
            return;
        }

        resetCaptureSession();

        if (cameraDevice == null) {
            Log.d(TAG, "CameraDevice is null");
            return;
        }

        try {
            captureSession.open(validatingBuilder.build(), cameraDevice);
        } catch (CameraAccessException e) {
            Log.d(TAG, "Unable to configure camera " + cameraId + " due to " + e.getMessage());
        }
    }

    /**
     * Closes the currently opened capture session, so it can be safely disposed. Replaces the old
     * session with a new session initialized with the old session's configuration.
     */
    void resetCaptureSession() {
        Log.d(TAG, "Closing Capture Session");
        captureSession.close();

        // Recreate an initialized (but not opened) capture session from the previous configuration
        SessionConfiguration previousSessionConfiguration =
                captureSession.getSessionConfiguration();
        List<CaptureRequestConfiguration> unissuedCaptureRequestConfigurations =
                captureSession.getCaptureRequestConfigurations();
        captureSession = new CaptureSession(handler);
        captureSession.setSessionConfiguration(previousSessionConfiguration);
        // When the previous capture session has not reached the open state, the issued single
        // capture
        // requests will still be in request queue and will need to be passed to the next capture
        // session.
        captureSession.issueSingleCaptureRequests(unissuedCaptureRequestConfigurations);
    }

    private CameraDevice.StateCallback createDeviceStateCallback() {
        synchronized (attachedUseCaseLock) {
            SessionConfiguration configuration = useCaseAttachState.getOnlineBuilder().build();
            return CameraDeviceStateCallbacks.createComboCallback(
                    stateCallback, configuration.getDeviceStateCallback());
        }
    }

    /**
     * Attach a repeating surface to a {@link CaptureRequestConfiguration} when the configuration
     * indicate that it needs a repeating surface.
     *
     * @param captureRequestConfiguration the configuration to attach a repeating surface
     */
    private void checkAndAttachRepeatingSurface(
            CaptureRequestConfiguration captureRequestConfiguration) {
        if (!captureRequestConfiguration.getSurfaces().isEmpty()) {
            return;
        }

        if (!captureRequestConfiguration.isUseRepeatingSurface()) {
            return;
        }

        Collection<BaseUseCase> activeUseCases;
        synchronized (attachedUseCaseLock) {
            activeUseCases = useCaseAttachState.getActiveAndOnlineUseCases();
        }

        DeferrableSurface repeatingSurface = null;
        for (BaseUseCase useCase : activeUseCases) {
            SessionConfiguration sessionConfiguration = useCase.getSessionConfiguration(cameraId);
            List<DeferrableSurface> surfaces =
                    sessionConfiguration.getCaptureRequestConfiguration().getSurfaces();
            if (!surfaces.isEmpty()) {
                // When an use case is active, all surfaces in its CaptureRequestConfiguration are
                // added to
                // the repeating request. Choose the first one here as the repeating surface.
                repeatingSurface = surfaces.get(0);
                break;
            }
        }

        if (repeatingSurface == null) {
            throw new IllegalStateException(
                    "Unable to find a repeating surface to attach to CaptureRequestConfiguration");
        }

        captureRequestConfiguration.addSurface(repeatingSurface);
    }

    /** Returns the Camera2CameraControl attached to Camera */
    @Override
    public CameraControl getCameraControl() {
        return cameraControl;
    }

    /**
     * Submits single request
     *
     * @param captureRequestConfiguration capture configuration used for creating CaptureRequest
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public void submitSingleRequest(CaptureRequestConfiguration captureRequestConfiguration) {
        if (Looper.myLooper() != handler.getLooper()) {
            handler.post(() -> submitSingleRequest(captureRequestConfiguration));
            return;
        }
        Log.d(TAG, "issue single capture request for camera " + cameraId);

        checkAndAttachRepeatingSurface(captureRequestConfiguration);

        // Recreates the Builder to add implementationOptions from CameraControl
        CaptureRequestConfiguration.Builder builder =
                CaptureRequestConfiguration.Builder.from(captureRequestConfiguration);
        builder.addImplementationOptions(cameraControl.getSingleRequestImplOptions());

        captureSession.issueSingleCaptureRequest(builder.build());
    }

    /**
     * Re-sends repeating request based on current SessionConfigurations and CameraControl's Global
     * SessionConfiguration
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public void updateRepeatingRequest() {
        if (Looper.myLooper() != handler.getLooper()) {
            handler.post(() -> updateRepeatingRequest());
            return;
        }

        updateCaptureSessionConfiguration();
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
            switch (state.get()) {
                case CLOSING:
                case RELEASING:
                    cameraDevice.close();
                    Camera.this.cameraDevice = null;
                    break;
                case OPENING:
                case REOPENING:
                    state.set(State.OPENED);
                    Camera.this.cameraDevice = cameraDevice;
                    openCaptureSession();
                    break;
                default:
                    throw new IllegalStateException(
                            "onOpened() should not be possible from state: " + state.get());
            }
        }

        @Override
        public void onClosed(CameraDevice cameraDevice) {
            Log.d(TAG, "CameraDevice.onClosed(): " + cameraDevice.getId());
            resetCaptureSession();
            switch (state.get()) {
                case CLOSING:
                    state.set(State.INITIALIZED);
                    Camera.this.cameraDevice = null;
                    break;
                case REOPENING:
                    state.set(State.OPENING);
                    openCameraDevice();
                    break;
                case RELEASING:
                    state.set(State.RELEASED);
                    Camera.this.cameraDevice = null;
                    break;
                default:
                    CameraX.postError(
                            CameraX.ErrorCode.CAMERA_STATE_INCONSISTENT,
                            "Camera closed while in state: " + state.get());
            }
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            Log.d(TAG, "CameraDevice.onDisconnected(): " + cameraDevice.getId());
            resetCaptureSession();
            switch (state.get()) {
                case CLOSING:
                    state.set(State.INITIALIZED);
                    Camera.this.cameraDevice = null;
                    break;
                case REOPENING:
                case OPENED:
                case OPENING:
                    state.set(State.CLOSING);
                    cameraDevice.close();
                    Camera.this.cameraDevice = null;
                    break;
                case RELEASING:
                    state.set(State.RELEASED);
                    cameraDevice.close();
                    Camera.this.cameraDevice = null;
                    break;
                default:
                    throw new IllegalStateException(
                            "onDisconnected() should not be possible from state: " + state.get());
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
            switch (state.get()) {
                case CLOSING:
                    state.set(State.INITIALIZED);
                    Camera.this.cameraDevice = null;
                    break;
                case REOPENING:
                case OPENED:
                case OPENING:
                    state.set(State.CLOSING);
                    cameraDevice.close();
                    Camera.this.cameraDevice = null;
                    break;
                case RELEASING:
                    state.set(State.RELEASED);
                    cameraDevice.close();
                    Camera.this.cameraDevice = null;
                    break;
                default:
                    throw new IllegalStateException(
                            "onError() should not be possible from state: " + state.get());
            }
        }
    }
}
