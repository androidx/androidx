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

package androidx.camera.core;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.internal.CameraUseCaseAdapter;
import androidx.core.util.Preconditions;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.Lifecycle.State;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

import com.google.auto.value.AutoValue;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A repository of {@link LifecycleCamera} instances.
 *
 * <p> This repository maps each unique pair of {@link LifecycleOwner} and set of
 * {@link CameraInternal} to a single LifecycleCamera.
 *
 * <p> The repository ensures that only a single LifecycleCamera is active at a time so if the
 * Lifecycle of a camera starts then it will take over as the current active camera. This means
 * that at anytime only one of the LifecycleCamera in the repo will be in the unsuspended state.
 * All others will be suspended.
 *
 * <p> When the Lifecycle of the most recently active camera stops then it will make sure
 * that the next most recently started camera become the active camera.
 *
 * <p> A LifecycleCamera associated with the repository can also be released from the repository.
 * When it is released, all UseCases bound to the LifecycleCamera will be unbound and the
 * LifecycleCamera will be released.
 */
final class LifecycleCameraRepository {
    final Object mLock = new Object();

    @GuardedBy("mLock")
    final Map<Key, LifecycleCamera> mCameraMap = new HashMap<>();

    @GuardedBy("mLock")
    final Map<Key, LifecycleCameraRepositoryObserver> mLifecycleObserverMap = new HashMap<>();

    @GuardedBy("mLock")
    private final ArrayDeque<Key> mActiveCameras = new ArrayDeque<>();

    /**
     * Create a new {@link LifecycleCamera} associated with the given {@link LifecycleOwner}.
     *
     * <p>The {@link LifecycleCamera} is set to be an observer of the {@link
     * LifecycleOwner}.
     *
     * @param lifecycleOwner       to associate with the LifecycleCamera
     * @param cameraUseCaseAdaptor the CameraUseCaseAdapter to wrap in a LifecycleCamera
     *
     * @throws IllegalArgumentException if the LifecycleOwner is already in a destroyed state or
     * if the repository already contains a LifecycleCamera that has the same LifecycleOwner and
     * CameraInternal set as the CameraUseCaseAdapter.
     */
    LifecycleCamera createLifecycleCamera(
            @NonNull LifecycleOwner lifecycleOwner,
            @NonNull CameraUseCaseAdapter cameraUseCaseAdaptor) {
        LifecycleCamera lifecycleCamera;
        synchronized (mLock) {
            Key key = Key.create(lifecycleOwner, cameraUseCaseAdaptor.getCameraId());
            Preconditions.checkArgument(mCameraMap.get(key) == null, "LifecycleCamera already "
                    + "exists for the given LifecycleOwner and set of cameras");

            if (lifecycleOwner.getLifecycle().getCurrentState() == State.DESTROYED) {
                throw new IllegalArgumentException(
                        "Trying to create LifecycleCamera with destroyed lifecycle.");
            }

            // Need to add observer before creating LifecycleCamera to make sure
            // it can be stopped before the latest active one is started.'
            lifecycleCamera = new LifecycleCamera(lifecycleOwner, cameraUseCaseAdaptor);
            registerCamera(key, lifecycleCamera);
        }
        return lifecycleCamera;
    }

    /**
     * Get the LifecycleCamera which contains the same LifecycleOwner and a
     * CameraUseCaseAdapter.CameraId.
     *
     * @return null if no such LifecycleCamera exists.
     */
    @Nullable
    LifecycleCamera getLifecycleCamera(LifecycleOwner lifecycleOwner,
            CameraUseCaseAdapter.CameraId cameraId) {
        synchronized (mLock) {
            return mCameraMap.get(Key.create(lifecycleOwner, cameraId));
        }
    }

    /**
     * Returns all the LifecycleCamera that have been created by the repository which haven't
     * been destroyed yet.
     */
    Collection<LifecycleCamera> getLifecycleCameras() {
        synchronized (mLock) {
            return Collections.unmodifiableCollection(mCameraMap.values());
        }
    }

    // Registers the LifecycleCamera in the repository so that the repository ensures that only one
    // camera is active at one time.
    private void registerCamera(Key key, LifecycleCamera lifecycleCamera) {
        synchronized (mLock) {
            LifecycleCameraRepositoryObserver observer =
                    new LifecycleCameraRepositoryObserver(key, this);
            mLifecycleObserverMap.put(key, observer);
            mCameraMap.put(key, lifecycleCamera);
            key.getLifecycleOwner().getLifecycle().addObserver(observer);
        }
    }

    // Unregisters the Lifecycle from the repository so it is no longer tracked by the repository.
    // This does not stop the camera from receiving its lifecycle events. For the LifecycleCamera
    // to stop receiving lifecycle event it must be done manually either before or after.
    void unregisterCamera(Key key) {
        synchronized (mLock) {
            setInactive(key);
            mCameraMap.remove(key);
            LifecycleCameraRepositoryObserver observer = mLifecycleObserverMap.remove(key);
            if (observer != null) {
                key.getLifecycleOwner().getLifecycle().removeObserver(observer);
            }

        }
    }

    // Sets the LifecycleCamera with the associated key as the current active camera. This will
    // suspend all other cameras which are tracked by the repository.
    void setActive(Key key) {
        synchronized (mLock) {
            LifecycleCamera camera = mCameraMap.get(key);
            if (camera != null) {

                // Only keep the last {@link LifecycleOwner} active. Stop the others.
                if (mActiveCameras.isEmpty()) {
                    mActiveCameras.push(key);
                } else {
                    Key currentActiveCamera = mActiveCameras.peek();
                    if (currentActiveCamera != key) {
                        Preconditions.checkNotNull(
                                mCameraMap.get(currentActiveCamera)).suspend();
                        mActiveCameras.push(key);
                    }
                }
                camera.unsuspend();
            }
        }
    }

    // Removes the LifecycleCamera with the associated key from list of active cameras.
    // If this camera was the current active camera then the next most recently active camera in
    // the current active list will become the active camera.
    void setInactive(Key key) {
        synchronized (mLock) {
            // Removes stopped lifecycleOwner from active list.
            mActiveCameras.remove(key);

            // Start up next LifecycleCamera if there are still active cameras
            if (!mActiveCameras.isEmpty()) {
                Preconditions.checkNotNull(
                        mCameraMap.get(mActiveCameras.peek())).unsuspend();
            }
        }
    }

    /**
     * A key for mapping a {@link LifecycleOwner} and set of {@link CameraInternal} to a
     * {@link LifecycleCamera}.
     */
    @AutoValue
    abstract static class Key {
        static Key create(@NonNull LifecycleOwner lifecycleOwner,
                @NonNull CameraUseCaseAdapter.CameraId cameraId) {
            return new AutoValue_LifecycleCameraRepository_Key(lifecycleOwner, cameraId);
        }

        @NonNull
        public abstract LifecycleOwner getLifecycleOwner();

        @NonNull
        public abstract CameraUseCaseAdapter.CameraId getCameraId();
    }

    private static class LifecycleCameraRepositoryObserver implements LifecycleObserver {
        private final LifecycleCameraRepository mLifecycleCameraRepository;
        private final Key mKey;

        LifecycleCameraRepositoryObserver(Key key,
                LifecycleCameraRepository lifecycleCameraRepository) {
            mKey = key;
            mLifecycleCameraRepository = lifecycleCameraRepository;
        }

        /**
         * Monitors which {@link LifecycleOwner} receives an ON_START event and then stop
         * other {@link LifecycleCamera} to keep only one active at a time.
         */
        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        public void onStart(LifecycleOwner lifecycleOwner) {
            mLifecycleCameraRepository.setActive(mKey);
        }

        /**
         * Monitors which {@link LifecycleOwner} receives an ON_STOP event.
         */
        @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
        public void onStop(LifecycleOwner lifecycleOwner) {
            mLifecycleCameraRepository.setInactive(mKey);
        }

        /**
         * Monitors which {@link LifecycleOwner} receives an ON_DESTROY event and then
         * removes any {@link LifecycleCamera} associated with it from this
         * repository when that lifecycle is destroyed.
         */
        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        public void onDestroy(LifecycleOwner lifecycleOwner) {
            mLifecycleCameraRepository.unregisterCamera(mKey);
        }
    }
}
