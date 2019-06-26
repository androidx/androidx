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

package androidx.camera.testing.fakes;

import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.BaseCamera;
import androidx.camera.core.CameraControlInternal;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CaptureConfig;
import androidx.camera.core.DeferrableSurface;
import androidx.camera.core.DeferrableSurfaces;
import androidx.camera.core.SessionConfig;
import androidx.camera.core.UseCase;
import androidx.camera.core.UseCaseAttachState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A fake camera which will not produce any data, but provides a valid BaseCamera implementation.
 */
public class FakeCamera implements BaseCamera {
    private static final String TAG = "FakeCamera";
    private static final String DEFAULT_CAMERA_ID = "0";
    private final CameraControlInternal mCameraControlInternal;
    private final CameraInfo mCameraInfo;
    private String mCameraId;
    private UseCaseAttachState mUseCaseAttachState;
    private State mState = State.INITIALIZED;

    @Nullable
    private SessionConfig mSessionConfig;
    @Nullable
    private SessionConfig mCameraControlSessionConfig;

    private List<DeferrableSurface> mConfiguredDeferrableSurfaces = Collections.emptyList();

    public FakeCamera() {
        this(DEFAULT_CAMERA_ID, new FakeCameraInfo(), /*cameraControl=*/null);
    }

    public FakeCamera(String cameraId) {
        this(cameraId, new FakeCameraInfo(), /*cameraControl=*/null);
    }

    public FakeCamera(CameraInfo cameraInfo, @Nullable CameraControlInternal cameraControl) {
        this(DEFAULT_CAMERA_ID, cameraInfo, cameraControl);
    }

    public FakeCamera(String cameraId,
            CameraInfo cameraInfo,
            @Nullable CameraControlInternal cameraControl) {
        mCameraInfo = cameraInfo;
        mCameraId = cameraId;
        mUseCaseAttachState = new UseCaseAttachState(cameraId);
        mCameraControlInternal = cameraControl == null ? new FakeCameraControl(this)
                : cameraControl;
    }

    @Override
    public void open() {
        checkNotReleased();
        if (mState == State.INITIALIZED) {
            mState = State.OPENED;
        }
    }

    @Override
    public void close() {
        checkNotReleased();
        if (mState == State.OPENED) {
            mSessionConfig = null;
            reconfigure();
            mState = State.INITIALIZED;
        }
    }

    @Override
    public void release() {
        checkNotReleased();
        if (mState == State.OPENED) {
            close();
        }

        mState = State.RELEASED;
    }

    @Override
    public void onUseCaseActive(final UseCase useCase) {
        Log.d(TAG, "Use case " + useCase + " ACTIVE for camera " + mCameraId);

        mUseCaseAttachState.setUseCaseActive(useCase);
        updateCaptureSessionConfig();
    }

    /** Removes the use case from a state of issuing capture requests. */
    @Override
    public void onUseCaseInactive(final UseCase useCase) {
        Log.d(TAG, "Use case " + useCase + " INACTIVE for camera " + mCameraId);

        mUseCaseAttachState.setUseCaseInactive(useCase);
        updateCaptureSessionConfig();
    }

    /** Updates the capture requests based on the latest settings. */
    @Override
    public void onUseCaseUpdated(final UseCase useCase) {
        Log.d(TAG, "Use case " + useCase + " UPDATED for camera " + mCameraId);

        mUseCaseAttachState.updateUseCase(useCase);
        updateCaptureSessionConfig();
    }

    @Override
    public void onUseCaseReset(final UseCase useCase) {
        Log.d(TAG, "Use case " + useCase + " RESET for camera " + mCameraId);

        mUseCaseAttachState.updateUseCase(useCase);
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

        Log.d(TAG, "Use cases " + useCases + " ONLINE for camera " + mCameraId);
        for (UseCase useCase : useCases) {
            mUseCaseAttachState.setUseCaseOnline(useCase);
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

        Log.d(TAG, "Use cases " + useCases + " OFFLINE for camera " + mCameraId);
        for (UseCase useCase : useCases) {
            mUseCaseAttachState.setUseCaseOffline(useCase);
        }

        if (mUseCaseAttachState.getOnlineUseCases().isEmpty()) {
            close();
            return;
        }

        openCaptureSession();
        updateCaptureSessionConfig();
    }

    // Returns fixed CameraControlInternal instance in order to verify the instance is correctly
    // attached.
    @Override
    public CameraControlInternal getCameraControlInternal() {
        return mCameraControlInternal;
    }

    @Override
    public CameraInfo getCameraInfo() {
        return mCameraInfo;
    }

    @Override
    public void onCameraControlUpdateSessionConfig(@NonNull SessionConfig sessionConfig) {
        mCameraControlSessionConfig = sessionConfig;
        updateCaptureSessionConfig();
    }

    @Override
    public void onCameraControlCaptureRequests(@NonNull List<CaptureConfig> captureConfigs) {
        Log.d(TAG, "Capture requests submitted:\n    " + TextUtils.join("\n    ", captureConfigs));
    }

    private void checkNotReleased() {
        if (mState == State.RELEASED) {
            throw new IllegalStateException("Camera has been released.");
        }
    }

    private void openCaptureSession() {
        SessionConfig.ValidatingBuilder validatingBuilder;
        validatingBuilder = mUseCaseAttachState.getOnlineBuilder();
        if (!validatingBuilder.isValid()) {
            Log.d(TAG, "Unable to create capture session due to conflicting configurations");
            return;
        }

        if (mState != State.OPENED) {
            Log.d(TAG, "CameraDevice is not opened");
            return;
        }

        mSessionConfig = validatingBuilder.build();
        reconfigure();
    }

    private void updateCaptureSessionConfig() {
        SessionConfig.ValidatingBuilder validatingBuilder;
        validatingBuilder = mUseCaseAttachState.getActiveAndOnlineBuilder();

        if (validatingBuilder.isValid()) {
            // Apply CameraControlInternal's SessionConfig to let CameraControlInternal be able
            // to control Repeating Request and process results.
            validatingBuilder.add(mCameraControlSessionConfig);

            mSessionConfig = validatingBuilder.build();
        }
    }

    private void reconfigure() {
        notifySurfaceDetached();

        if (mSessionConfig != null) {
            List<DeferrableSurface> surfaces = mSessionConfig.getSurfaces();

            // Before creating capture session, some surfaces may need to refresh.
            DeferrableSurfaces.refresh(surfaces);

            mConfiguredDeferrableSurfaces = new ArrayList<>(surfaces);

            List<Surface> configuredSurfaces = new ArrayList<>(
                    DeferrableSurfaces.surfaceSet(
                            mConfiguredDeferrableSurfaces));
            if (configuredSurfaces.isEmpty()) {
                Log.e(TAG, "Unable to open capture session with no surfaces. ");
                return;
            }
        }

        notifySurfaceAttached();
    }

    // Notify the surface is attached to a new capture session.
    private void notifySurfaceAttached() {
        for (DeferrableSurface deferrableSurface : mConfiguredDeferrableSurfaces) {
            deferrableSurface.notifySurfaceAttached();
        }
    }

    // Notify the surface is detached from current capture session.
    private void notifySurfaceDetached() {
        for (DeferrableSurface deferredSurface : mConfiguredDeferrableSurfaces) {
            deferredSurface.notifySurfaceDetached();
        }
        // Clears the mConfiguredDeferrableSurfaces to prevent from duplicate
        // notifySurfaceDetached calls.
        mConfiguredDeferrableSurfaces.clear();
    }

    enum State {
        /**
         * Stable state once the camera has been constructed.
         */
        INITIALIZED,
        /**
         * A stable state where the camera has been opened.
         */
        OPENED,
        /**
         * A stable state where the camera has been permanently closed.
         */
        RELEASED
    }

}
