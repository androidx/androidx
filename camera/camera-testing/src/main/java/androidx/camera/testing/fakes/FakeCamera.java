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
import android.view.Surface;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.Logger;
import androidx.camera.core.UseCase;
import androidx.camera.core.impl.CameraConfig;
import androidx.camera.core.impl.CameraConfigs;
import androidx.camera.core.impl.CameraControlInternal;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.LiveDataObservable;
import androidx.camera.core.impl.Observable;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.UseCaseAttachState;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.camera.testing.impl.DeferrableSurfacesUtil;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A fake camera which will not produce any data, but provides a valid Camera implementation.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class FakeCamera implements CameraInternal {
    private static final String TAG = "FakeCamera";
    private static final String DEFAULT_CAMERA_ID = "0";
    private final LiveDataObservable<CameraInternal.State> mObservableState =
            new LiveDataObservable<>();
    private final CameraControlInternal mCameraControlInternal;
    private final CameraInfoInternal mCameraInfoInternal;
    private final String mCameraId;
    private final UseCaseAttachState mUseCaseAttachState;
    private final Set<UseCase> mAttachedUseCases = new HashSet<>();
    private State mState = State.CLOSED;
    private int mAvailableCameraCount = 1;
    private final List<UseCase> mUseCaseActiveHistory = new ArrayList<>();
    private final List<UseCase> mUseCaseInactiveHistory = new ArrayList<>();
    private final List<UseCase> mUseCaseUpdateHistory = new ArrayList<>();
    private final List<UseCase> mUseCaseResetHistory = new ArrayList<>();
    private boolean mHasTransform = true;

    @Nullable
    private SessionConfig mSessionConfig;

    private List<DeferrableSurface> mConfiguredDeferrableSurfaces = Collections.emptyList();

    private CameraConfig mCameraConfig = CameraConfigs.emptyConfig();

    public FakeCamera() {
        this(DEFAULT_CAMERA_ID, /*cameraControl=*/null,
                new FakeCameraInfoInternal(DEFAULT_CAMERA_ID));
    }

    public FakeCamera(@NonNull CameraControlInternal cameraControl) {
        this(DEFAULT_CAMERA_ID, cameraControl, new FakeCameraInfoInternal(DEFAULT_CAMERA_ID));
    }

    public FakeCamera(@NonNull String cameraId) {
        this(cameraId, /*cameraControl=*/null, new FakeCameraInfoInternal(cameraId));
    }

    public FakeCamera(@Nullable CameraControlInternal cameraControl,
            @NonNull CameraInfoInternal cameraInfo) {
        this(DEFAULT_CAMERA_ID, cameraControl, cameraInfo);
    }

    public FakeCamera(@NonNull String cameraId, @Nullable CameraControlInternal cameraControl,
            @NonNull CameraInfoInternal cameraInfo) {
        mCameraInfoInternal = cameraInfo;
        mCameraId = cameraId;
        mUseCaseAttachState = new UseCaseAttachState(cameraId);
        mCameraControlInternal = cameraControl == null ? new FakeCameraControl(
                new CameraControlInternal.ControlUpdateCallback() {
                    @Override
                    public void onCameraControlUpdateSessionConfig() {
                        updateCaptureSessionConfig();
                    }

                    @Override
                    public void onCameraControlCaptureRequests(
                            @NonNull List<CaptureConfig> captureConfigs) {
                        Logger.d(TAG, "Capture requests submitted:\n    " + TextUtils.join("\n    ",
                                captureConfigs));
                    }
                })
                : cameraControl;
        mObservableState.postValue(State.CLOSED);
    }

    /**
     * Sets the number of cameras that are available to open.
     *
     * <p>If this number is set to 0, then calling {@link #open()} will wait in a {@code
     * PENDING_OPEN} state until the number is set to a value greater than 0 before entering an
     * {@code OPEN} state.
     *
     * @param count An integer number greater than 0 representing the number of available cameras
     *              to open on this device.
     */
    public void setAvailableCameraCount(@IntRange(from = 0) int count) {
        Preconditions.checkArgumentNonnegative(count);
        mAvailableCameraCount = count;
        if (mAvailableCameraCount > 0 && mState == State.PENDING_OPEN) {
            open();
        }
    }

    /**
     * Retrieves the number of cameras available to open on this device, as seen by this camera.
     *
     * @return An integer number greater than 0 representing the number of available cameras to
     * open on this device.
     */
    @IntRange(from = 0)
    public int getAvailableCameraCount() {
        return mAvailableCameraCount;
    }

    @Override
    public void open() {
        checkNotReleased();
        if (mState == State.CLOSED || mState == State.PENDING_OPEN) {
            if (mAvailableCameraCount > 0) {
                mState = State.OPEN;
                mObservableState.postValue(State.OPEN);
            } else {
                mState = State.PENDING_OPEN;
                mObservableState.postValue(State.PENDING_OPEN);
            }
        }
    }

    @Override
    public void close() {
        checkNotReleased();
        switch (mState) {
            case OPEN:
                mSessionConfig = null;
                reconfigure();
                // fall through
            case PENDING_OPEN:
                mState = State.CLOSED;
                mObservableState.postValue(State.CLOSED);
                break;
            default:
                break;
        }
    }

    @Override
    @NonNull
    public ListenableFuture<Void> release() {
        if (mState == State.OPEN) {
            close();
        }

        if (mState != State.RELEASED) {
            mState = State.RELEASED;
            mObservableState.postValue(State.RELEASED);
        }
        return Futures.immediateFuture(null);
    }

    @NonNull
    @Override
    public Observable<CameraInternal.State> getCameraState() {
        return mObservableState;
    }

    @Override
    public void onUseCaseActive(@NonNull UseCase useCase) {
        Logger.d(TAG, "Use case " + useCase + " ACTIVE for camera " + mCameraId);
        mUseCaseActiveHistory.add(useCase);
        mUseCaseAttachState.setUseCaseActive(useCase.getName() + useCase.hashCode(),
                useCase.getSessionConfig(), useCase.getCurrentConfig(),
                useCase.getAttachedStreamSpec(),
                Collections.singletonList(useCase.getCurrentConfig().getCaptureType()));
        updateCaptureSessionConfig();
    }

    /** Removes the use case from a state of issuing capture requests. */
    @Override
    public void onUseCaseInactive(@NonNull UseCase useCase) {
        Logger.d(TAG, "Use case " + useCase + " INACTIVE for camera " + mCameraId);
        mUseCaseInactiveHistory.add(useCase);
        mUseCaseAttachState.setUseCaseInactive(useCase.getName() + useCase.hashCode());
        updateCaptureSessionConfig();
    }

    /** Updates the capture requests based on the latest settings. */
    @Override
    public void onUseCaseUpdated(@NonNull UseCase useCase) {
        Logger.d(TAG, "Use case " + useCase + " UPDATED for camera " + mCameraId);
        mUseCaseUpdateHistory.add(useCase);
        mUseCaseAttachState.updateUseCase(useCase.getName() + useCase.hashCode(),
                useCase.getSessionConfig(), useCase.getCurrentConfig(),
                useCase.getAttachedStreamSpec(),
                Collections.singletonList(useCase.getCurrentConfig().getCaptureType()));
        updateCaptureSessionConfig();
    }

    @Override
    public void onUseCaseReset(@NonNull UseCase useCase) {
        Logger.d(TAG, "Use case " + useCase + " RESET for camera " + mCameraId);
        mUseCaseResetHistory.add(useCase);
        mUseCaseAttachState.updateUseCase(useCase.getName() + useCase.hashCode(),
                useCase.getSessionConfig(), useCase.getCurrentConfig(),
                useCase.getAttachedStreamSpec(),
                Collections.singletonList(useCase.getCurrentConfig().getCaptureType()));
        updateCaptureSessionConfig();
        openCaptureSession();
    }

    /**
     * Sets the use case to be in the state where the capture session will be configured to handle
     * capture requests from the use case.
     */
    @Override
    public void attachUseCases(@NonNull final Collection<UseCase> useCases) {
        if (useCases.isEmpty()) {
            return;
        }

        mAttachedUseCases.addAll(useCases);

        Logger.d(TAG, "Use cases " + useCases + " ATTACHED for camera " + mCameraId);
        for (UseCase useCase : useCases) {
            useCase.onStateAttached();
            mUseCaseAttachState.setUseCaseAttached(
                    useCase.getName() + useCase.hashCode(),
                    useCase.getSessionConfig(),
                    useCase.getCurrentConfig(),
                    useCase.getAttachedStreamSpec(),
                    Collections.singletonList(useCase.getCurrentConfig().getCaptureType()));
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
    public void detachUseCases(@NonNull final Collection<UseCase> useCases) {
        if (useCases.isEmpty()) {
            return;
        }

        mAttachedUseCases.removeAll(useCases);

        Logger.d(TAG, "Use cases " + useCases + " DETACHED for camera " + mCameraId);
        for (UseCase useCase : useCases) {
            mUseCaseAttachState.setUseCaseDetached(useCase.getName() + useCase.hashCode());
            useCase.onStateDetached();
        }

        if (mUseCaseAttachState.getAttachedSessionConfigs().isEmpty()) {
            close();
            return;
        }

        openCaptureSession();
        updateCaptureSessionConfig();
    }

    @NonNull
    public Set<UseCase> getAttachedUseCases() {
        return mAttachedUseCases;
    }

    // Returns fixed CameraControlInternal instance in order to verify the instance is correctly
    // attached.
    @NonNull
    @Override
    public CameraControlInternal getCameraControlInternal() {
        return mCameraControlInternal;
    }

    @NonNull
    @Override
    public CameraInfoInternal getCameraInfoInternal() {
        return mCameraInfoInternal;
    }

    /**
     * Returns a list of active use cases ordered chronologically according to
     * {@link #onUseCaseActive} invocations.
     */
    @NonNull
    public List<UseCase> getUseCaseActiveHistory() {
        return mUseCaseActiveHistory;
    }

    /**
     * Returns a list of inactive use cases ordered chronologically according to
     * {@link #onUseCaseInactive} invocations.
     */
    @NonNull
    public List<UseCase> getUseCaseInactiveHistory() {
        return mUseCaseInactiveHistory;
    }


    /**
     * Returns a list of updated use cases ordered chronologically according to
     * {@link #onUseCaseUpdated} invocations.
     */
    @NonNull
    public List<UseCase> getUseCaseUpdateHistory() {
        return mUseCaseUpdateHistory;
    }


    /**
     * Returns a list of reset use cases ordered chronologically according to
     * {@link #onUseCaseReset} invocations.
     */
    @NonNull
    public List<UseCase> getUseCaseResetHistory() {
        return mUseCaseResetHistory;
    }

    @Override
    public boolean getHasTransform() {
        return mHasTransform;
    }

    /**
     * Sets whether the camera has a transform.
     */
    public void setHasTransform(boolean hasCameraTransform) {
        mHasTransform = hasCameraTransform;
    }

    private void checkNotReleased() {
        if (mState == State.RELEASED) {
            throw new IllegalStateException("Camera has been released.");
        }
    }

    private void openCaptureSession() {
        SessionConfig.ValidatingBuilder validatingBuilder;
        validatingBuilder = mUseCaseAttachState.getAttachedBuilder();
        if (!validatingBuilder.isValid()) {
            Logger.d(TAG, "Unable to create capture session due to conflicting configurations");
            return;
        }

        if (mState != State.OPEN) {
            Logger.d(TAG, "CameraDevice is not opened");
            return;
        }

        mSessionConfig = validatingBuilder.build();
        reconfigure();
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    private void updateCaptureSessionConfig() {
        SessionConfig.ValidatingBuilder validatingBuilder;
        validatingBuilder = mUseCaseAttachState.getActiveAndAttachedBuilder();

        if (validatingBuilder.isValid()) {
            // Apply CameraControlInternal's SessionConfig to let CameraControlInternal be able
            // to control Repeating Request and process results.
            validatingBuilder.add(mCameraControlInternal.getSessionConfig());

            mSessionConfig = validatingBuilder.build();
        }
    }

    private void reconfigure() {
        notifySurfaceDetached();

        if (mSessionConfig != null) {
            List<DeferrableSurface> surfaces = mSessionConfig.getSurfaces();

            mConfiguredDeferrableSurfaces = new ArrayList<>(surfaces);

            // Since this is a fake camera, it is likely we will get null surfaces. Don't
            // consider them as failed.
            List<Surface> configuredSurfaces =
                    DeferrableSurfacesUtil.surfaceList(mConfiguredDeferrableSurfaces,
                            /*removeNullSurfaces=*/ false);
            if (configuredSurfaces.isEmpty()) {
                Logger.e(TAG, "Unable to open capture session with no surfaces. ");
                return;
            }
        }

        notifySurfaceAttached();
    }

    // Notify the surface is attached to a new capture session.
    private void notifySurfaceAttached() {
        for (DeferrableSurface deferrableSurface : mConfiguredDeferrableSurfaces) {
            try {
                deferrableSurface.incrementUseCount();
            } catch (DeferrableSurface.SurfaceClosedException e) {
                throw new RuntimeException("Surface in unexpected state", e);
            }
        }
    }

    // Notify the surface is detached from current capture session.
    private void notifySurfaceDetached() {
        for (DeferrableSurface deferredSurface : mConfiguredDeferrableSurfaces) {
            deferredSurface.decrementUseCount();
        }
        // Clears the mConfiguredDeferrableSurfaces to prevent from duplicate
        // notifySurfaceDetached calls.
        mConfiguredDeferrableSurfaces.clear();
    }

    @NonNull
    @Override
    public CameraConfig getExtendedConfig() {
        return mCameraConfig;
    }

    @Override
    public void setExtendedConfig(@Nullable CameraConfig cameraConfig) {
        mCameraConfig = cameraConfig;
    }
}
