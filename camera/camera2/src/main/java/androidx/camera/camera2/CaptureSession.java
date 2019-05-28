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
import androidx.camera.core.CameraCaptureCallback;
import androidx.camera.core.CameraCaptureSessionStateCallbacks;
import androidx.camera.core.CaptureConfig;
import androidx.camera.core.Config;
import androidx.camera.core.Config.Option;
import androidx.camera.core.DeferrableSurface;
import androidx.camera.core.DeferrableSurfaces;
import androidx.camera.core.SessionConfig;

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
    private final Handler mHandler;
    /** The configuration for the currently issued single capture requests. */
    private final List<CaptureConfig> mCaptureConfigs = new ArrayList<>();
    /** Lock on whether the camera is open or closed. */
    final Object mStateLock = new Object();
    /** Callback for handling image captures. */
    private final CameraCaptureSession.CaptureCallback mCaptureCallback =
            new CaptureCallback() {
                @Override
                public void onCaptureCompleted(
                        CameraCaptureSession session,
                        CaptureRequest request,
                        TotalCaptureResult result) {
                }
            };
    private final StateCallback mCaptureSessionStateCallback = new StateCallback();
    /** The framework camera capture session held by this session. */
    @Nullable
    CameraCaptureSession mCameraCaptureSession;
    /** The configuration for the currently issued capture requests. */
    @Nullable
    private volatile SessionConfig mSessionConfig;
    /** The list of surfaces used to configure the current capture session. */
    private List<Surface> mConfiguredSurfaces = Collections.emptyList();
    /** The list of DeferrableSurface used to notify surface detach events */
    @GuardedBy("mConfiguredDeferrableSurfaces")
    private List<DeferrableSurface> mConfiguredDeferrableSurfaces = Collections.emptyList();
    /** Tracks the current state of the session. */
    @GuardedBy("mStateLock")
    State mState = State.UNINITIALIZED;

    /**
     * Constructor for CaptureSession.
     *
     * @param handler The handler is responsible for queuing up callbacks from capture requests. If
     *                this is null then when asynchronous methods are called on this session they
     *                will attempt
     *                to use the current thread's looper.
     */
    CaptureSession(@Nullable Handler handler) {
        this.mHandler = handler;
        mState = State.INITIALIZED;
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
                case OPENING:
                    mSessionConfig = sessionConfig;
                    break;
                case OPENED:
                    mSessionConfig = sessionConfig;

                    if (!mConfiguredSurfaces.containsAll(
                            DeferrableSurfaces.surfaceList(sessionConfig.getSurfaces()))) {
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
     * @param sessionConfig which is used to configure the camera capture session. This contains
     *                      configurations which may or may not be currently active in issuing
     *                      capture requests.
     * @param cameraDevice  the camera with which to generate the capture session
     * @throws CameraAccessException if the camera is in an invalid start state
     */
    void open(SessionConfig sessionConfig, CameraDevice cameraDevice)
            throws CameraAccessException {
        synchronized (mStateLock) {
            switch (mState) {
                case UNINITIALIZED:
                    throw new IllegalStateException(
                            "open() should not be possible in state: " + mState);
                case INITIALIZED:
                    List<DeferrableSurface> surfaces = sessionConfig.getSurfaces();

                    // Before creating capture session, some surfaces may need to refresh.
                    DeferrableSurfaces.refresh(surfaces);

                    mConfiguredDeferrableSurfaces = new ArrayList<>(surfaces);

                    mConfiguredSurfaces =
                            new ArrayList<>(
                                    DeferrableSurfaces.surfaceSet(
                                            mConfiguredDeferrableSurfaces));
                    if (mConfiguredSurfaces.isEmpty()) {
                        Log.e(TAG, "Unable to open capture session with no surfaces. ");
                        return;
                    }

                    notifySurfaceAttached();
                    mState = State.OPENING;
                    Log.d(TAG, "Opening capture session.");
                    List<CameraCaptureSession.StateCallback> callbacks =
                            new ArrayList<>(sessionConfig.getSessionStateCallbacks());
                    callbacks.add(mCaptureSessionStateCallback);
                    CameraCaptureSession.StateCallback comboCallback =
                            CameraCaptureSessionStateCallbacks.createComboCallback(callbacks);
                    cameraDevice.createCaptureSession(mConfiguredSurfaces, comboCallback, mHandler);
                    break;
                default:
                    Log.e(TAG, "Open not allowed in state: " + mState);
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
        synchronized (mStateLock) {
            switch (mState) {
                case UNINITIALIZED:
                    throw new IllegalStateException(
                            "close() should not be possible in state: " + mState);
                case INITIALIZED:
                    mState = State.RELEASED;
                    break;
                case OPENING:
                case OPENED:
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
     * Releases the capture session.
     *
     * <p>This releases all of the sessions resources and should be called when ready to close the
     * camera.
     *
     * <p>Once a session is released it can no longer be opened again. After the session is released
     * all method calls on it do nothing.
     */
    void release() {
        synchronized (mStateLock) {
            switch (mState) {
                case UNINITIALIZED:
                    throw new IllegalStateException(
                            "release() should not be possible in state: " + mState);
                case INITIALIZED:
                    mState = State.RELEASED;
                    break;
                case OPENING:
                    mState = State.RELEASING;
                    break;
                case OPENED:
                case CLOSED:
                    mCameraCaptureSession.close();
                    mState = State.RELEASING;
                    break;
                case RELEASING:
                case RELEASED:
            }
        }
    }

    // Notify the surface is attached to a new capture session.
    void notifySurfaceAttached() {
        synchronized (mConfiguredDeferrableSurfaces) {
            for (DeferrableSurface deferrableSurface : mConfiguredDeferrableSurfaces) {
                deferrableSurface.notifySurfaceAttached();
            }
        }
    }

    // Notify the surface is detached from current capture session.
    void notifySurfaceDetached() {
        synchronized (mConfiguredDeferrableSurfaces) {
            for (DeferrableSurface deferredSurface : mConfiguredDeferrableSurfaces) {
                deferredSurface.notifySurfaceDetached();
            }
            // Clears the mConfiguredDeferrableSurfaces to prevent from duplicate
            // notifySurfaceDetached calls.
            mConfiguredDeferrableSurfaces.clear();
        }
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
                case OPENING:
                    mCaptureConfigs.addAll(captureConfigs);
                    break;
                case OPENED:
                    mCaptureConfigs.addAll(captureConfigs);
                    issueBurstCaptureRequest();
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

    /**
     * Sets the {@link CaptureRequest} so that the camera will start producing data.
     *
     * <p>Will skip setting requests if there are no surfaces since it is illegal to do so.
     */
    void issueRepeatingCaptureRequests() {
        if (mSessionConfig == null) {
            Log.d(TAG, "Skipping issueRepeatingCaptureRequests for no configuration case.");
            return;
        }

        CaptureConfig captureConfig = mSessionConfig.getRepeatingCaptureConfig();

        try {
            Log.d(TAG, "Issuing request for session.");
            CaptureRequest.Builder builder =
                    captureConfig.buildCaptureRequest(mCameraCaptureSession.getDevice());
            if (builder == null) {
                Log.d(TAG, "Skipping issuing empty request for session.");
                return;
            }

            applyImplementationOptionTCaptureBuilder(
                    builder, captureConfig.getImplementationOptions());

            CameraCaptureSession.CaptureCallback comboCaptureCallback =
                    createCamera2CaptureCallback(
                            captureConfig.getCameraCaptureCallbacks(),
                            mCaptureCallback);

            mCameraCaptureSession.setRepeatingRequest(
                    builder.build(), comboCaptureCallback, mHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Unable to access camera: " + e.getMessage());
            Thread.dumpStack();
        }
    }

    private void applyImplementationOptionTCaptureBuilder(
            CaptureRequest.Builder builder, Config config) {
        Camera2Config camera2Config = new Camera2Config(config);
        for (Option<?> option : camera2Config.getCaptureRequestOptions()) {
            /* Although type is erased below, it is safe to pass it to CaptureRequest.Builder
            because
            these option are created via Camera2Config.Extender.setCaptureRequestOption
            (CaptureRequest.Key<ValueT> key, ValueT value) and hence the type compatibility of
            key and
            value are ensured by the compiler. */
            @SuppressWarnings("unchecked")
            Option<Object> typeErasedOption = (Option<Object>) option;
            @SuppressWarnings("unchecked")
            CaptureRequest.Key<Object> key = (CaptureRequest.Key<Object>) option.getToken();

            // TODO(b/129997028): Error of setting unavailable CaptureRequest.Key may need to
            //  send back out to the developer
            try {
                // Ignores keys that don't exist
                builder.set(key, camera2Config.retrieveOption(typeErasedOption));
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "CaptureRequest.Key is not supported: " + key);
            }
        }
    }

    /** Issues mCaptureConfigs to {@link CameraCaptureSession}. */
    void issueBurstCaptureRequest() {
        if (mCaptureConfigs.isEmpty()) {
            return;
        }
        try {
            CameraBurstCaptureCallback callbackAggregator = new CameraBurstCaptureCallback();
            List<CaptureRequest> captureRequests = new ArrayList<>();
            Log.d(TAG, "Issuing capture request.");
            for (CaptureConfig captureConfig : mCaptureConfigs) {
                if (captureConfig.getSurfaces().isEmpty()) {
                    Log.d(TAG, "Skipping issuing empty capture request.");
                    continue;
                }

                CaptureRequest.Builder builder =
                        captureConfig.buildCaptureRequest(mCameraCaptureSession.getDevice());

                applyImplementationOptionTCaptureBuilder(
                        builder, captureConfig.getImplementationOptions());

                CaptureRequest captureRequest = builder.build();
                callbackAggregator.addCallback(captureRequest,
                        captureConfig.getCameraCaptureCallbacks());
                captureRequests.add(captureRequest);

            }
            mCameraCaptureSession.captureBurst(captureRequests,
                    callbackAggregator,
                    mHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Unable to access camera: " + e.getMessage());
            Thread.dumpStack();
        }
        mCaptureConfigs.clear();
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
        /**
         * {@inheritDoc}
         *
         * <p>Once the {@link CameraCaptureSession} has been configured then the capture request
         * will be immediately issued.
         */
        @Override
        public void onConfigured(CameraCaptureSession session) {
            synchronized (mStateLock) {
                switch (mState) {
                    case UNINITIALIZED:
                    case INITIALIZED:
                    case OPENED:
                    case RELEASED:
                        throw new IllegalStateException(
                                "onConfigured() should not be possible in state: " + mState);
                    case OPENING:
                        mState = State.OPENED;
                        mCameraCaptureSession = session;
                        Log.d(TAG, "Attempting to send capture request onConfigured");
                        issueRepeatingCaptureRequests();
                        issueBurstCaptureRequest();
                        break;
                    case CLOSED:
                        mCameraCaptureSession = session;
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
            synchronized (mStateLock) {
                switch (mState) {
                    case UNINITIALIZED:
                        throw new IllegalStateException(
                                "onReady() should not be possible in state: " + mState);
                    default:
                }
                Log.d(TAG, "CameraCaptureSession.onReady()");
            }
        }

        @Override
        public void onClosed(CameraCaptureSession session) {
            synchronized (mStateLock) {
                switch (mState) {
                    case UNINITIALIZED:
                        throw new IllegalStateException(
                                "onClosed() should not be possible in state: " + mState);
                    default:
                        mState = State.RELEASED;
                        mCameraCaptureSession = null;
                }
                Log.d(TAG, "CameraCaptureSession.onClosed()");

                notifySurfaceDetached();

            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
            synchronized (mStateLock) {
                switch (mState) {
                    case UNINITIALIZED:
                    case INITIALIZED:
                    case OPENED:
                    case RELEASED:
                        throw new IllegalStateException(
                                "onConfiguredFailed() should not be possible in state: " + mState);
                    case OPENING:
                    case CLOSED:
                        mState = State.CLOSED;
                        mCameraCaptureSession = session;
                        break;
                    case RELEASING:
                        mState = State.RELEASING;
                        session.close();
                }
                Log.e(TAG, "CameraCaptureSession.onConfiguredFailed()");
            }
        }
    }

    /** Also notify the surface detach event if receives camera device close event */
    public void notifyCameraDeviceClose() {
        notifySurfaceDetached();
    }
}
