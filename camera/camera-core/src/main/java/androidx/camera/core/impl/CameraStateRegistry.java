/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.core.impl;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.camera.core.Camera;
import androidx.camera.core.Logger;
import androidx.core.util.Preconditions;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * A registry that tracks the state of cameras.
 *
 * <p>The registry tracks internally how many cameras are open and how many are available to open.
 * Cameras that are in a {@link CameraInternal.State#PENDING_OPEN} state can be notified when
 * there is a slot available to open a camera.
 */
public final class CameraStateRegistry {
    private static final String TAG = "CameraStateRegistry";
    private final StringBuilder mDebugString = new StringBuilder();

    private final Object mLock = new Object();

    private final int mMaxAllowedOpenedCameras;
    @GuardedBy("mLock")
    private final Map<Camera, CameraRegistration> mCameraStates = new HashMap<>();
    @GuardedBy("mLock")
    private int mAvailableCameras;


    /**
     * Creates a new registry with a limit of {@code maxAllowedOpenCameras} allowed to be opened.
     *
     * @param maxAllowedOpenedCameras The limit for number of simultaneous open cameras.
     */
    public CameraStateRegistry(int maxAllowedOpenedCameras) {
        mMaxAllowedOpenedCameras = maxAllowedOpenedCameras;
        synchronized ("mLock") {
            mAvailableCameras = mMaxAllowedOpenedCameras;
        }
    }

    /**
     * Registers a camera with the registry.
     *
     * <p>Once registered, the camera's state must be updated through
     * {@link #markCameraState(Camera, CameraInternal.State)}.
     *
     * <p>Before attempting to open a camera, {@link #tryOpenCamera(Camera)} must be called and
     * callers should only continue to open the camera if it returns {@code true}.
     *
     * <p>Cameras will be automatically unregistered when they are marked as being in a
     * {@link CameraInternal.State#RELEASED} state.
     *
     * @param camera The camera to register.
     */
    public void registerCamera(@NonNull Camera camera, @NonNull Executor notifyExecutor,
            @NonNull OnOpenAvailableListener cameraAvailableListener) {
        synchronized (mLock) {
            Preconditions.checkState(!mCameraStates.containsKey(camera), "Camera is "
                    + "already registered: " + camera);
            mCameraStates.put(camera,
                    new CameraRegistration(null, notifyExecutor, cameraAvailableListener));
        }
    }

    /**
     * Should be called before attempting to actually open a camera.
     *
     * <p>This must be called before attempting to open a camera. If too many cameras are already
     * open, then this will return {@code false}, and the caller should not attempt to open the
     * camera. Instead, the caller should mark its state as
     * {@link CameraInternal.State#PENDING_OPEN} with
     * {@link #markCameraState(Camera, CameraInternal.State)}, and the listener registered with
     * {@link #registerCamera(Camera, Executor, OnOpenAvailableListener)} will be notified when a
     * camera becomes available. At that time, the caller should attempt to call this method again.
     *
     * @return {@code true} if it is safe to open the camera. If this returns {@code true}, it is
     * assumed the camera is now in an {@link CameraInternal.State#OPENING} state, and the
     * available camera count will be reduced by 1.
     */
    public boolean tryOpenCamera(@NonNull Camera camera) {
        synchronized (mLock) {
            CameraRegistration registration = Preconditions.checkNotNull(mCameraStates.get(camera),
                    "Camera must first be registered with registerCamera()");
            boolean success = false;
            if (Logger.isDebugEnabled(TAG)) {
                mDebugString.setLength(0);
                mDebugString.append(String.format(Locale.US, "tryOpenCamera(%s) [Available "
                                + "Cameras: %d, Already Open: %b (Previous state: %s)]",
                        camera, mAvailableCameras, isOpen(registration.getState()),
                        registration.getState()));
            }
            if (mAvailableCameras > 0 || isOpen(registration.getState())) {
                // Set state directly to OPENING.
                registration.setState(CameraInternal.State.OPENING);
                success = true;
            }

            if (Logger.isDebugEnabled(TAG)) {
                mDebugString.append(
                        String.format(Locale.US, " --> %s", success ? "SUCCESS" : "FAIL"));
                Logger.d(TAG, mDebugString.toString());
            }

            if (success) {
                // Successfully opening a camera should only make the total available count go
                // down, so no need to notify cameras in a PENDING_OPEN state.
                recalculateAvailableCameras();
            }

            return success;
        }
    }

    /**
     * Mark the state of a registered camera.
     *
     * <p>This is used to track the states of all cameras in order to determine how many cameras
     * are available to be opened.
     *
     * @param camera Registered camera whose state is being set
     * @param state  New state of the registered camera
     */
    public void markCameraState(@NonNull Camera camera, @NonNull CameraInternal.State state) {
        markCameraState(camera, state, true);
    }

    /**
     * Mark the state of a registered camera.
     *
     * <p>This is used to track the states of all cameras in order to determine how many cameras
     * are available to be opened.
     *
     * <p>If a camera slot if found to be available for opening during the execution of this
     * method, the caller will not be notified of it if {@code notifyImmediately} is set to
     * {@code false}. This can be useful if a camera moves its state to
     * {@link CameraInternal.State#PENDING_OPEN} but doesn't wish to be opened even if a camera
     * slot is available for opening, for example after the camera has continuously failed to open.
     *
     * @param camera            Registered camera whose state is being set
     * @param state             New state of the registered camera
     * @param notifyImmediately {@code true} if the registered camera should be notified
     *                          immediately if a new slot for opening is available, {@code false}
     *                          otherwise.
     */
    public void markCameraState(@NonNull Camera camera, @NonNull CameraInternal.State state,
            boolean notifyImmediately) {
        Map<Camera, CameraRegistration> camerasToNotify = null;
        synchronized (mLock) {
            CameraInternal.State previousState = null;
            int previousAvailableCameras = mAvailableCameras;
            if (state == CameraInternal.State.RELEASED) {
                previousState = unregisterCamera(camera);
            } else {
                previousState = updateAndVerifyState(camera, state);
            }

            if (previousState == state) {
                // Nothing has changed. No need to notify.
                return;
            }

            if (previousAvailableCameras < 1 && mAvailableCameras > 0) {
                // Cameras are now available, notify ALL cameras in a PENDING_OPEN state.
                camerasToNotify = new HashMap<>();
                for (Map.Entry<Camera, CameraRegistration> entry : mCameraStates.entrySet()) {
                    if (entry.getValue().getState() == CameraInternal.State.PENDING_OPEN) {
                        camerasToNotify.put(entry.getKey(), entry.getValue());
                    }
                }
            } else if (state == CameraInternal.State.PENDING_OPEN && mAvailableCameras > 0) {
                // This camera entered a PENDING_OPEN state while there are available cameras,
                // only notify the single camera.
                camerasToNotify = new HashMap<>();
                camerasToNotify.put(camera, mCameraStates.get(camera));
            }

            // Omit notifying this camera if `notifyImmediately` is false
            if (camerasToNotify != null && !notifyImmediately) {
                camerasToNotify.remove(camera);
            }
        }

        // Notify pending cameras unlocked.
        if (camerasToNotify != null) {
            for (CameraRegistration registration : camerasToNotify.values()) {
                registration.notifyListener();
            }
        }
    }

    // Unregisters the given camera and returns the state before being unregistered
    @GuardedBy("mLock")
    @Nullable
    private CameraInternal.State unregisterCamera(Camera camera) {
        CameraRegistration registration = mCameraStates.remove(camera);
        if (registration != null) {
            recalculateAvailableCameras();
            return registration.getState();
        }

        return null;
    }

    // Updates the state of the given camera and returns the previous state.
    @GuardedBy("mLock")
    @Nullable
    private CameraInternal.State updateAndVerifyState(@NonNull Camera camera,
            @NonNull CameraInternal.State state) {
        CameraInternal.State previousState = Preconditions.checkNotNull(mCameraStates.get(camera),
                "Cannot update state of camera which has not yet been registered. Register with "
                        + "CameraStateRegistry.registerCamera()").setState(state);

        if (state == CameraInternal.State.OPENING) {
            // A camera should only enter an OPENING state if it is already in an open state or
            // it has been allowed to by tryOpenCamera().
            Preconditions.checkState(isOpen(state) || previousState == CameraInternal.State.OPENING,
                    "Cannot mark camera as opening until camera was successful at calling "
                            + "CameraStateRegistry.tryOpenCamera()");
        }

        // Only update the available camera count if the camera state has changed.
        if (previousState != state) {
            recalculateAvailableCameras();
        }

        return previousState;
    }

    private static boolean isOpen(@Nullable CameraInternal.State state) {
        return state != null && state.holdsCameraSlot();
    }

    @WorkerThread
    @GuardedBy("mLock")
    private void recalculateAvailableCameras() {
        if (Logger.isDebugEnabled(TAG)) {
            mDebugString.setLength(0);
            mDebugString.append("Recalculating open cameras:\n");
            mDebugString.append(String.format(Locale.US, "%-45s%-22s\n", "Camera", "State"));
            mDebugString.append(
                    "-------------------------------------------------------------------\n");
        }
        // Count the number of cameras that are not in a closed state. Closed states are
        // considered to be CLOSED, PENDING_OPEN or OPENING, since we can't guarantee a camera
        // has actually been open in these states. All cameras that are in a CLOSING or RELEASING
        // state may have previously been open, so we will count them as open.
        int openCount = 0;
        for (Map.Entry<Camera, CameraRegistration> entry : mCameraStates.entrySet()) {
            if (Logger.isDebugEnabled(TAG)) {
                String stateString =
                        entry.getValue().getState() != null ? entry.getValue().getState().toString()
                                : "UNKNOWN";
                mDebugString.append(
                        String.format(Locale.US, "%-45s%-22s\n", entry.getKey().toString(),
                                stateString));
            }
            if (isOpen(entry.getValue().getState())) {
                openCount++;
            }
        }
        if (Logger.isDebugEnabled(TAG)) {
            mDebugString.append(
                    "-------------------------------------------------------------------\n");
            mDebugString.append(String.format(Locale.US, "Open count: %d (Max allowed: %d)",
                    openCount,
                    mMaxAllowedOpenedCameras));
            Logger.d(TAG, mDebugString.toString());
        }

        // Calculate available cameras value (clamped to 0 or more)
        mAvailableCameras = Math.max(mMaxAllowedOpenedCameras - openCount, 0);
    }

    /** Returns whether at least 1 camera is closing. */
    public boolean isCameraClosing() {
        synchronized (mLock) {
            for (Map.Entry<Camera, CameraRegistration> entry : mCameraStates.entrySet()) {
                if (entry.getValue().getState() == CameraInternal.State.CLOSING) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * A listener that is notified when a camera slot becomes available for opening.
     */
    public interface OnOpenAvailableListener {
        /**
         * Called when a camera slot becomes available for opening.
         *
         * <p>Listeners can attempt to open a slot with
         * {@link CameraStateRegistry#tryOpenCamera(Camera)} after receiving this signal.
         *
         * <p>Only cameras that are in a {@link CameraInternal.State#PENDING_OPEN} state will
         * receive this signal.
         */
        void onOpenAvailable();
    }

    private static class CameraRegistration {
        private CameraInternal.State mState;
        private final Executor mNotifyExecutor;
        private final OnOpenAvailableListener mCameraAvailableListener;

        CameraRegistration(@Nullable CameraInternal.State initialState,
                @NonNull Executor notifyExecutor,
                @NonNull OnOpenAvailableListener cameraAvailableListener) {
            mState = initialState;
            mNotifyExecutor = notifyExecutor;
            mCameraAvailableListener = cameraAvailableListener;
        }

        CameraInternal.State setState(@Nullable CameraInternal.State state) {
            CameraInternal.State previousState = mState;
            mState = state;
            return previousState;
        }

        CameraInternal.State getState() {
            return mState;
        }

        void notifyListener() {
            try {
                mNotifyExecutor.execute(mCameraAvailableListener::onOpenAvailable);
            } catch (RejectedExecutionException e) {
                Logger.e(TAG, "Unable to notify camera.", e);
            }
        }
    }
}
