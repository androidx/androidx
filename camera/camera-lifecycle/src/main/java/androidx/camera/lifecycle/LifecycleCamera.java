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

package androidx.camera.lifecycle;

import static androidx.camera.core.featurecombination.impl.ResolvedFeatureCombination.resolveFeatureCombination;

import android.annotation.SuppressLint;
import android.os.Build;

import androidx.annotation.GuardedBy;
import androidx.annotation.OptIn;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.ExperimentalSessionConfig;
import androidx.camera.core.LegacySessionConfig;
import androidx.camera.core.SessionConfig;
import androidx.camera.core.UseCase;
import androidx.camera.core.featurecombination.Feature;
import androidx.camera.core.featurecombination.impl.ResolvedFeatureCombination;
import androidx.camera.core.impl.CameraConfig;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.internal.CameraUseCaseAdapter;
import androidx.core.util.Preconditions;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.Lifecycle.State;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A {@link CameraUseCaseAdapter} whose starting and stopping is controlled by a
 *  {@link Lifecycle}.
 */
@SuppressLint("UsesNonDefaultVisibleForTesting")
@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@OptIn(markerClass = ExperimentalSessionConfig.class)
public final class LifecycleCamera implements LifecycleObserver, Camera {
    private final Object mLock = new Object();

    @GuardedBy("mLock")
    // The lifecycle that controls the LifecycleCamera
    private final LifecycleOwner mLifecycleOwner;

    private final CameraUseCaseAdapter mCameraUseCaseAdapter;

    @GuardedBy("mLock")
    private volatile boolean mIsActive = false;

    @GuardedBy("mLock")
    private boolean mSuspended = false;

    @GuardedBy("mLock")
    private boolean mReleased = false;

    @GuardedBy("mLock")
    private SessionConfig mBoundSessionConfig = null;

    /**
     * Wraps an existing {@link CameraUseCaseAdapter} so it is controlled by lifecycle transitions.
     */
    LifecycleCamera(LifecycleOwner lifecycleOwner, CameraUseCaseAdapter cameraUseCaseAdaptor) {
        mLifecycleOwner = lifecycleOwner;
        mCameraUseCaseAdapter = cameraUseCaseAdaptor;

        // Make sure that the attach state of mCameraUseCaseAdapter matches that of the lifecycle
        if (mLifecycleOwner.getLifecycle().getCurrentState().isAtLeast(State.STARTED)) {
            mCameraUseCaseAdapter.attachUseCases();
        } else {
            mCameraUseCaseAdapter.detachUseCases();
        }
        lifecycleOwner.getLifecycle().addObserver(this);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart(@NonNull LifecycleOwner lifecycleOwner) {
        synchronized (mLock) {
            if (!mSuspended && !mReleased) {
                mCameraUseCaseAdapter.attachUseCases();
                mIsActive = true;
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onStop(@NonNull LifecycleOwner lifecycleOwner) {
        synchronized (mLock) {
            if (!mSuspended && !mReleased) {
                mCameraUseCaseAdapter.detachUseCases();
                mIsActive = false;
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void onDestroy(@NonNull LifecycleOwner lifecycleOwner) {
        synchronized (mLock) {
            mCameraUseCaseAdapter.removeUseCases(mCameraUseCaseAdapter.getUseCases());
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume(@NonNull LifecycleOwner lifecycleOwner) {
        // ActiveResumingMode is required for Multi-window which is supported since Android 7(N).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mCameraUseCaseAdapter.setActiveResumingMode(true);
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onPause(@NonNull LifecycleOwner lifecycleOwner) {
        // ActiveResumingMode is required for Multi-window which is supported since Android 7(N).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mCameraUseCaseAdapter.setActiveResumingMode(false);
        }
    }


    /**
     * Suspend the camera so that it ignore lifecycle events.
     *
     * <p> This will also close the {@link CameraUseCaseAdapter}.
     *
     * <p> This will be idempotent if the camera is already suspended.
     */
    public void suspend() {
        synchronized (mLock) {
            if (mSuspended) {
                return;
            }

            onStop(mLifecycleOwner);
            mSuspended = true;
        }
    }

    /**
     * Unsuspend the camera so it will start listening to lifecycle events.
     *
     * <p> This will also open the {@link CameraUseCaseAdapter} if the lifecycle is in a STARTED
     * state or above.
     *
     * <p> This will be idempotent if the camera is already in an unsuspended state.
     */
    public void unsuspend() {
        synchronized (mLock) {
            if (!mSuspended) {
                return;
            }

            mSuspended = false;
            if (mLifecycleOwner.getLifecycle().getCurrentState().isAtLeast(State.STARTED)) {
                onStart(mLifecycleOwner);
            }
        }
    }

    // TODO(b/154939118) remove when Extension.setExtension() is implemented since there no
    //  longer is a need to check if the camera is active.
    public boolean isActive() {
        synchronized (mLock) {
            return mIsActive;
        }
    }

    /**
     * Returns if the [UseCase] is bound to this camera.
     */
    public boolean isBound(@NonNull UseCase useCase) {
        synchronized (mLock) {
            return mCameraUseCaseAdapter.getUseCases().contains(useCase);
        }
    }

    /**
     * Returns if the [SessionConfig] is bound to this camera.
     */
    public boolean isBound(@NonNull SessionConfig sessionConfig) {
        // Should only be invoked on SessionConfig disallowing multiple binding.
        Preconditions.checkState(!sessionConfig.isLegacy());
        synchronized (mLock) {
            return mBoundSessionConfig == sessionConfig;
        }
    }

    public @NonNull List<UseCase> getUseCases() {
        synchronized (mLock) {
            return Collections.unmodifiableList(mCameraUseCaseAdapter.getUseCases());
        }
    }

    /**
     * Retrieves the lifecycle owner.
     */
    public @NonNull LifecycleOwner getLifecycleOwner() {
        synchronized (mLock) {
            return mLifecycleOwner;
        }
    }

    public @NonNull CameraUseCaseAdapter getCameraUseCaseAdapter() {
        return mCameraUseCaseAdapter;
    }

    /**
     * Binds the {@link SessionConfig} to the lifecycle camera.
     *
     * <p>This will attach the UseCases to the CameraUseCaseAdapter if successful.
     *
     * <p>{@link SessionConfig} that can't allow multiple binding, must be bound without any
     * other bound {@link SessionConfig}. If a {@link SessionConfig} that doesn't allow multiple
     * binding is bound, then it can't allow any other binding unless it is unbound.
     *
     * @throws CameraUseCaseAdapter.CameraException if unable to attach the UseCase to the camera.
     * @throws IllegalStateException if the {@link SessionConfig} can't be bound because either the
     * given {@link SessionConfig} or the previously bound {@link SessionConfig} disallows multiple
     * binding.
     */
    void bind(@NonNull SessionConfig sessionConfig)
            throws CameraUseCaseAdapter.CameraException {
        synchronized (mLock) {
            if (mBoundSessionConfig == null) {
                mBoundSessionConfig = sessionConfig;
            } else {
                if (sessionConfig.isLegacy()) { // Bind use cases
                    if (!mBoundSessionConfig.isLegacy()) {
                        throw new IllegalStateException(
                                "Cannot bind use cases when a SessionConfig is already bound to "
                                        + "this LifecycleOwner. Please unbind first");
                    }

                    List<UseCase> boundUseCases =
                            new ArrayList<>(mBoundSessionConfig.getUseCases());
                    boundUseCases.addAll(sessionConfig.getUseCases());
                    // Uses the latest SessionConfig parameters to update the mBoundSessionConfig.
                    mBoundSessionConfig = new LegacySessionConfig(
                            boundUseCases,
                            sessionConfig.getViewPort(),
                            sessionConfig.getEffects()
                    );
                }
                else { // Bind sessionConfig.
                    if (mBoundSessionConfig.isLegacy()) {
                        throw new IllegalStateException(
                                "Cannot bind the SessionConfig when use cases are bound to this"
                                        + " LifecycleOwner already. Please unbind first");
                    }
                    mBoundSessionConfig = sessionConfig;
                    // implicitly unbind the previous SessionConfig when SessionConfig is updated.
                    mCameraUseCaseAdapter.removeUseCases(mCameraUseCaseAdapter.getUseCases());
                }
            }
            mCameraUseCaseAdapter.setViewPort(sessionConfig.getViewPort());
            mCameraUseCaseAdapter.setEffects(sessionConfig.getEffects());
            mCameraUseCaseAdapter.setSessionType(sessionConfig.getSessionType());
            mCameraUseCaseAdapter.setFrameRate(sessionConfig.getFrameRateRange());

            ResolvedFeatureCombination resolvedFeatureCombination = resolveFeatureCombination(
                    sessionConfig, (CameraInfoInternal) getCameraInfo());

            sessionConfig.getFeatureSelectionListenerExecutor().execute(
                    () -> {
                        Set<Feature> features = new HashSet<>();

                        if (resolvedFeatureCombination != null) {
                            features.addAll(resolvedFeatureCombination.getFeatures());
                        }

                        sessionConfig.getFeatureSelectionListener().accept(features);
                    });

            mCameraUseCaseAdapter.addUseCases(sessionConfig.getUseCases(),
                    resolvedFeatureCombination);
        }
    }

    @Nullable
    SessionConfig getBoundSessionConfig() {
        synchronized (mLock) {
            return mBoundSessionConfig;
        }
    }

    boolean isLegacySessionConfigBound() {
        synchronized (mLock) {
            return mBoundSessionConfig == null ? false : mBoundSessionConfig.isLegacy();
        }
    }

    /**
     * Unbinds the SessionConfig from the lifecycle camera.
     *
     * <p>This will detach the UseCases from the CameraUseCaseAdapter.
     */
    void unbind(@NonNull SessionConfig sessionConfig) {
        synchronized (mLock) {
            if (mBoundSessionConfig == null
                    || (mBoundSessionConfig.isLegacy() != sessionConfig.isLegacy())) {
                // No-ops if no bound config or the unbinding SessionConfig is not compatible with
                // bound SessionConfig.
                return;
            }

            if (!mBoundSessionConfig.isLegacy() && !sessionConfig.isLegacy()) {
                // Unbinding SessionConfig
                if (mBoundSessionConfig == sessionConfig) {
                    // Unbind the bound SessionConfig successfully only when they are identical.
                    mBoundSessionConfig = null;
                } else {
                    // If the unbinding SessionConfig is different than the bound one. we do nothing
                    // Returning here is necessary to avoid removing the use cases.
                    return;
                }
            } else if (mBoundSessionConfig.isLegacy() && sessionConfig.isLegacy()) {
                // Unbinding LegacySessionConfig (UseCases or UseCaseGroup)
                List<UseCase> boundUseCases =
                        new ArrayList<>(mBoundSessionConfig.getUseCases());
                boundUseCases.removeAll(sessionConfig.getUseCases());
                mBoundSessionConfig = boundUseCases.isEmpty() ? null
                        : new LegacySessionConfig(
                                boundUseCases,
                                mBoundSessionConfig.getViewPort(),
                                mBoundSessionConfig.getEffects()
                        );
            }
            List<UseCase> useCasesToRemove = new ArrayList<>(sessionConfig.getUseCases());
            useCasesToRemove.retainAll(mCameraUseCaseAdapter.getUseCases());
            mCameraUseCaseAdapter.removeUseCases(useCasesToRemove);
        }
    }

    /**
     * Unbind all of the UseCases from the lifecycle camera.
     *
     * <p>This will detach all UseCases from the CameraUseCaseAdapter.
     */
    void unbindAll() {
        synchronized (mLock) {
            mCameraUseCaseAdapter.removeUseCases(mCameraUseCaseAdapter.getUseCases());
            mBoundSessionConfig = null;
        }
    }

    /**
     * Stops observing lifecycle changes.
     *
     * <p>Once released the wrapped {@link LifecycleCamera} is still valid, but will no longer be
     * triggered by lifecycle state transitions. In order to observe lifecycle changes again a new
     * {@link LifecycleCamera} instance should be created.
     *
     * <p>Calls subsequent to the first time will do nothing.
     */
    void release() {
        synchronized (mLock) {
            mReleased = true;
            mIsActive = false;
            mLifecycleOwner.getLifecycle().removeObserver(this);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Camera interface
    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public @NonNull CameraControl getCameraControl() {
        return mCameraUseCaseAdapter.getCameraControl();
    }

    @Override
    public @NonNull CameraInfo getCameraInfo() {
        return mCameraUseCaseAdapter.getCameraInfo();
    }

    @Nullable CameraInfo getSecondaryCameraInfo() {
        return mCameraUseCaseAdapter.getSecondaryCameraInfo();
    }

    @Override
    public @NonNull CameraConfig getExtendedConfig() {
        return mCameraUseCaseAdapter.getExtendedConfig();
    }

    @Override
    public boolean isUseCasesCombinationSupported(boolean withStreamSharing,
            UseCase @NonNull ... useCases) {
        return mCameraUseCaseAdapter.isUseCasesCombinationSupported(withStreamSharing, useCases);
    }
}
