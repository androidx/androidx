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

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCaptureSession.CaptureCallback;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraCaptureSessionStateCallbacks;
import androidx.camera.core.CaptureRequestConfiguration;
import androidx.camera.core.Configuration;
import androidx.camera.core.Configuration.Option;
import androidx.camera.core.DeferrableSurfaces;
import androidx.camera.core.SessionConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A session for capturing images from the camera which is tied to a specific {@link CameraDevice}.
 *
 * <p>A session can only be opened a single time. Once has {@link CaptureSession#close()} been
 * called then it is permanently closed so a new session has to be created for capturing images.
 */
final class CaptureSession {
    private static final String TAG = "CaptureSession";

    /** Handler for all the callbacks from the {@link CameraCaptureSession}. */
    @Nullable
    private final Handler handler;
    /** The configuration for the currently issued single capture requests. */
    private final List<CaptureRequestConfiguration> captureRequestConfigurations =
            new ArrayList<>();
    /** Lock on whether the camera is open or closed. */
    final Object stateLock = new Object();
    /** Callback for handling image captures. */
    private final CameraCaptureSession.CaptureCallback captureCallback =
            new CaptureCallback() {
                @Override
                public void onCaptureCompleted(
                        CameraCaptureSession session,
                        CaptureRequest request,
                        TotalCaptureResult result) {
                }
            };
    private final StateCallback captureSessionStateCallback = new StateCallback();
    /** The framework camera capture session held by this session. */
    @Nullable
    CameraCaptureSession cameraCaptureSession;
    /** The configuration for the currently issued capture requests. */
    private volatile SessionConfiguration sessionConfiguration =
            SessionConfiguration.defaultEmptySessionConfiguration();
    /** The list of surfaces used to configure the current capture session. */
    private List<Surface> configuredSurfaces = Collections.emptyList();
    /** Tracks the current state of the session. */
    @GuardedBy("stateLock")
    State state = State.UNINITIALIZED;

    /**
     * Constructor for CaptureSession.
     *
     * @param handler The handler is responsible for queuing up callbacks from capture requests. If
     *                this is null then when asynchronous methods are called on this session they
     *                will attempt
     *                to use the current thread's looper.
     */
    CaptureSession(@Nullable Handler handler) {
        this.handler = handler;
        state = State.INITIALIZED;
    }

    /** Returns the configurations of the capture session. */
    SessionConfiguration getSessionConfiguration() {
        synchronized (stateLock) {
            return sessionConfiguration;
        }
    }

    /**
     * Sets the active configurations for the capture session.
     *
     * <p>Once both the session configuration has been set and the session has been opened, then the
     * capture requests will immediately be issued.
     *
     * @param sessionConfiguration has the configuration that will currently active in issuing
     *                             capture request. The surfaces contained in this must be a
     *                             subset of the surfaces that
     *                             were used to open this capture session.
     */
    void setSessionConfiguration(SessionConfiguration sessionConfiguration) {
        synchronized (stateLock) {
            switch (state) {
                case UNINITIALIZED:
                    throw new IllegalStateException(
                            "setSessionConfiguration() should not be possible in state: " + state);
                case INITIALIZED:
                case OPENING:
                    this.sessionConfiguration = sessionConfiguration;
                    break;
                case OPENED:
                    this.sessionConfiguration = sessionConfiguration;

                    if (!configuredSurfaces.containsAll(
                            DeferrableSurfaces.surfaceList(sessionConfiguration.getSurfaces()))) {
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
     * Opens the capture session synchronously.
     *
     * <p>When the session is opened and the configurations have been set then the capture requests
     * will be issued.
     *
     * @param sessionConfiguration which is used to configure the camera capture session. This
     *                             contains configurations which may or may not be currently
     *                             active in issuing capture
     *                             requests.
     * @param cameraDevice         the camera with which to generate the capture session
     * @throws CameraAccessException if the camera is in an invalid start state
     */
    void open(SessionConfiguration sessionConfiguration, CameraDevice cameraDevice)
            throws CameraAccessException {
        synchronized (stateLock) {
            switch (state) {
                case UNINITIALIZED:
                    throw new IllegalStateException(
                            "open() should not be possible in state: " + state);
                case INITIALIZED:
                    configuredSurfaces =
                            new ArrayList<>(
                                    DeferrableSurfaces.surfaceSet(
                                            sessionConfiguration.getSurfaces()));
                    if (configuredSurfaces.isEmpty()) {
                        Log.e(TAG, "Unable to open capture session with no surfaces. ");
                        return;
                    }

                    state = State.OPENING;
                    Log.d(TAG, "Opening capture session.");
                    CameraCaptureSession.StateCallback comboCallback =
                            CameraCaptureSessionStateCallbacks.createComboCallback(
                                    captureSessionStateCallback,
                                    sessionConfiguration.getSessionStateCallback());
                    cameraDevice.createCaptureSession(configuredSurfaces, comboCallback, handler);
                    break;
                default:
                    Log.e(TAG, "Open not allowed in state: " + state);
            }
        }
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
        synchronized (stateLock) {
            switch (state) {
                case UNINITIALIZED:
                    throw new IllegalStateException(
                            "close() should not be possible in state: " + state);
                case INITIALIZED:
                    state = State.RELEASED;
                    break;
                case OPENING:
                case OPENED:
                    state = State.CLOSED;
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
    void release() {
        synchronized (stateLock) {
            switch (state) {
                case UNINITIALIZED:
                    throw new IllegalStateException(
                            "release() should not be possible in state: " + state);
                case INITIALIZED:
                    state = State.RELEASED;
                    break;
                case OPENING:
                    state = State.RELEASING;
                    break;
                case OPENED:
                case CLOSED:
                    cameraCaptureSession.close();
                    state = State.RELEASING;
                    break;
                case RELEASING:
                case RELEASED:
            }
        }
    }

    /**
     * Issues a single capture request.
     *
     * @param captureRequestConfiguration which is used to construct a {@link CaptureRequest}.
     */
    void issueSingleCaptureRequest(CaptureRequestConfiguration captureRequestConfiguration) {
        issueSingleCaptureRequests(Collections.singletonList(captureRequestConfiguration));
    }

    /**
     * Issues single capture requests.
     *
     * @param captureRequestConfigurations which is used to construct {@link CaptureRequest}.
     */
    void issueSingleCaptureRequests(
            List<CaptureRequestConfiguration> captureRequestConfigurations) {
        synchronized (stateLock) {
            switch (state) {
                case UNINITIALIZED:
                    throw new IllegalStateException(
                            "issueSingleCaptureRequests() should not be possible in state: "
                                    + state);
                case INITIALIZED:
                case OPENING:
                    Log.d(TAG, "issueSingleCaptureRequests() before capture session opened.");
                    this.captureRequestConfigurations.addAll(captureRequestConfigurations);
                    break;
                case OPENED:
                    this.captureRequestConfigurations.addAll(captureRequestConfigurations);
                    issueCaptureRequests();
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
    List<CaptureRequestConfiguration> getCaptureRequestConfigurations() {
        synchronized (stateLock) {
            return Collections.unmodifiableList(captureRequestConfigurations);
        }
    }

    /** Returns the current state of the session. */
    State getState() {
        synchronized (stateLock) {
            return state;
        }
    }

    /**
     * Sets the {@link CaptureRequest} so that the camera will start producing data.
     *
     * <p>Will skip setting requests if there are no surfaces since it is illegal to do so.
     */
    void issueRepeatingCaptureRequests() {
        CaptureRequestConfiguration captureRequestConfiguration =
                sessionConfiguration.getCaptureRequestConfiguration();

        try {
            Log.d(TAG, "Issuing request for session.");
            CaptureRequest.Builder builder =
                    captureRequestConfiguration.buildCaptureRequest(
                            cameraCaptureSession.getDevice());
            if (builder == null) {
                Log.d(TAG, "Skipping issuing empty request for session.");
                return;
            }

            applyImplementationOptionTCaptureBuilder(
                    builder, captureRequestConfiguration.getImplementationOptions());

            CameraCaptureSession.CaptureCallback comboCaptureCallback =
                    Camera2CaptureSessionCaptureCallbacks.createComboCallback(
                            captureCallback,
                            CaptureCallbackConverter.toCaptureCallback(
                                    captureRequestConfiguration.getCameraCaptureCallback()));
            cameraCaptureSession.setRepeatingRequest(
                    builder.build(), comboCaptureCallback, handler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Unable to access camera: " + e.getMessage());
            Thread.dumpStack();
        }
    }

    private void applyImplementationOptionTCaptureBuilder(
            CaptureRequest.Builder builder, Configuration configuration) {
        Camera2Configuration camera2Config = new Camera2Configuration(configuration);
        for (Option<?> option : camera2Config.getCaptureRequestOptions()) {
            /* Although type is erased below, it is safe to pass it to CaptureRequest.Builder
            because
            these option are created via Camera2Configuration.Extender.setCaptureRequestOption
            (CaptureRequest.Key<ValueT> key, ValueT value) and hence the type compatibility of
            key and
            value are ensured by the compiler. */
            @SuppressWarnings("unchecked")
            Option<Object> typeErasedOption = (Option<Object>) option;
            @SuppressWarnings("unchecked")
            CaptureRequest.Key<Object> key = (CaptureRequest.Key<Object>) option.getToken();
            builder.set(key, camera2Config.retrieveOption(typeErasedOption));
        }
    }

    /** Issues captureRequestConfigurations to {@link CameraCaptureSession}. */
    void issueCaptureRequests() {
        if (captureRequestConfigurations.isEmpty()) {
            return;
        }

        for (CaptureRequestConfiguration captureRequestConfiguration :
                captureRequestConfigurations) {
            if (captureRequestConfiguration.getSurfaces().isEmpty()) {
                Log.d(TAG, "Skipping issuing empty capture request.");
                continue;
            }
            try {
                Log.d(TAG, "Issuing capture request.");
                CaptureRequest.Builder builder =
                        captureRequestConfiguration.buildCaptureRequest(
                                cameraCaptureSession.getDevice());

                applyImplementationOptionTCaptureBuilder(
                        builder, captureRequestConfiguration.getImplementationOptions());

                cameraCaptureSession.capture(
                        builder.build(),
                        CaptureCallbackConverter.toCaptureCallback(
                                captureRequestConfiguration.getCameraCaptureCallback()),
                        handler);
            } catch (CameraAccessException e) {
                Log.e(TAG, "Unable to access camera: " + e.getMessage());
                Thread.dumpStack();
            }
        }
        captureRequestConfigurations.clear();
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
         * Transitional state when the {@link CameraCaptureSession} is in the process of being
         * opened.
         */
        OPENING,
        /**
         * Stable state where the {@link CameraCaptureSession} has been successfully opened. During
         * this state if a valid {@link SessionConfiguration} has been set then the {@link
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
        /**
         * {@inheritDoc}
         *
         * <p>Once the {@link CameraCaptureSession} has been configured then the capture request
         * will be immediately issued.
         */
        @Override
        public void onConfigured(CameraCaptureSession session) {
            synchronized (stateLock) {
                switch (state) {
                    case UNINITIALIZED:
                    case INITIALIZED:
                    case OPENED:
                    case RELEASED:
                        throw new IllegalStateException(
                                "onConfigured() should not be possible in state: " + state);
                    case OPENING:
                        state = State.OPENED;
                        cameraCaptureSession = session;
                        Log.d(TAG, "Attempting to send capture request onConfigured");
                        issueRepeatingCaptureRequests();
                        issueCaptureRequests();
                        break;
                    case CLOSED:
                        cameraCaptureSession = session;
                        break;
                    case RELEASING:
                        session.close();
                        break;
                }
                Log.d(TAG, "CameraCaptureSession.onConfigured()");
            }
        }

        @Override
        public void onReady(CameraCaptureSession session) {
            synchronized (stateLock) {
                switch (state) {
                    case UNINITIALIZED:
                        throw new IllegalStateException(
                                "onReady() should not be possible in state: " + state);
                    default:
                }
                Log.d(TAG, "CameraCaptureSession.onReady()");
            }
        }

        @Override
        public void onClosed(CameraCaptureSession session) {
            synchronized (stateLock) {
                switch (state) {
                    case UNINITIALIZED:
                        throw new IllegalStateException(
                                "onClosed() should not be possible in state: " + state);
                    default:
                        state = State.RELEASED;
                        cameraCaptureSession = null;
                }
                Log.d(TAG, "CameraCaptureSession.onClosed()");
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
            synchronized (stateLock) {
                switch (state) {
                    case UNINITIALIZED:
                    case INITIALIZED:
                    case OPENED:
                    case RELEASED:
                        throw new IllegalStateException(
                                "onConfiguredFailed() should not be possible in state: " + state);
                    case OPENING:
                    case CLOSED:
                        state = State.CLOSED;
                        cameraCaptureSession = session;
                        break;
                    case RELEASING:
                        state = State.RELEASING;
                        session.close();
                }
                Log.e(TAG, "CameraCaptureSession.onConfiguredFailed()");
            }
        }
    }
}
