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

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.UseCase;
import androidx.camera.core.impl.CameraConfig;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.internal.CameraUseCaseAdapter;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.Lifecycle.State;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * A {@link CameraUseCaseAdapter} whose starting and stopping is controlled by a
 *  {@link Lifecycle}.
 */
final class LifecycleCamera implements LifecycleObserver, Camera {
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
    public void onStart(LifecycleOwner lifecycleOwner) {
        synchronized (mLock) {
            if (!mSuspended && !mReleased) {
                mCameraUseCaseAdapter.attachUseCases();
                mIsActive = true;
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onStop(LifecycleOwner lifecycleOwner) {
        synchronized (mLock) {
            if (!mSuspended && !mReleased) {
                mCameraUseCaseAdapter.detachUseCases();
                mIsActive = false;
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void onDestroy(LifecycleOwner lifecycleOwner) {
        synchronized (mLock) {
            mCameraUseCaseAdapter.removeUseCases(mCameraUseCaseAdapter.getUseCases());
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

    public boolean isBound(@NonNull UseCase useCase) {
        synchronized (mLock) {
            return mCameraUseCaseAdapter.getUseCases().contains(useCase);
        }
    }

    @NonNull
    public List<UseCase> getUseCases() {
        synchronized (mLock) {
            return Collections.unmodifiableList(mCameraUseCaseAdapter.getUseCases());
        }
    }

    public LifecycleOwner getLifecycleOwner() {
        synchronized (mLock) {
            return mLifecycleOwner;
        }
    }

    public CameraUseCaseAdapter getCameraUseCaseAdapter() {
        return mCameraUseCaseAdapter;
    }

    /**
     * Bind the UseCases to the lifecycle camera.
     *
     * <>This will attach the UseCases to the CameraUseCaseAdapter if successful.
     *
     * @throws CameraUseCaseAdapter.CameraException if unable to attach the UseCase to the camera.
     */
    void bind(Collection<UseCase> useCases) throws CameraUseCaseAdapter.CameraException {
        synchronized (mLock) {
            mCameraUseCaseAdapter.addUseCases(useCases);
        }
    }

    /**
     * Unbind the UseCases from the lifecycle camera.
     *
     * <>This will detach the UseCases from the CameraUseCaseAdapter.
     */
    void unbind(Collection<UseCase> useCases) {
        synchronized (mLock) {
            List<UseCase> useCasesToRemove = new ArrayList<>(useCases);
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
    @NonNull
    @Override
    public CameraControl getCameraControl() {
        return mCameraUseCaseAdapter.getCameraControl();
    }

    @NonNull
    @Override
    public CameraInfo getCameraInfo() {
        return mCameraUseCaseAdapter.getCameraInfo();
    }

    @NonNull
    @Override
    public LinkedHashSet<CameraInternal> getCameraInternals() {
        return mCameraUseCaseAdapter.getCameraInternals();
    }

    @NonNull
    @Override
    public CameraConfig getExtendedConfig() {
        return mCameraUseCaseAdapter.getExtendedConfig();
    }

    @Override
    public void setExtendedConfig(@Nullable CameraConfig cameraConfig)  {
        mCameraUseCaseAdapter.setExtendedConfig(cameraConfig);
    }
}
