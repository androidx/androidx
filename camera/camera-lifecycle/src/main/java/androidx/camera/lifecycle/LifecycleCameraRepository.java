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
import androidx.annotation.RequiresApi;
import androidx.camera.core.UseCase;
import androidx.camera.core.ViewPort;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A repository of {@link LifecycleCamera} instances.
 *
 * <p> This repository maps each unique pair of {@link LifecycleOwner} and set of
 * {@link CameraInternal} to a single LifecycleCamera.
 *
 * <p> The repository ensures that a LifecycleCamera can be active only when there is any use
 * case bound on it. And, only a single LifecycleCamera is active at a time. A Lifecycle can
 * control multiple LifecycleCameras. For the LifecycleCameras controlled by a single Lifecycle,
 * only one LifecycleCamera among them can have use cases bound on it.
 *
 * <p> LifecycleCameras managed by the repository can be controlled by multiple Lifecycles. The
 * repository ensures that a Lifecycle can be active only when any LifecycleCamera controlled by
 * the Lifecycle has any use case bound on it. More than one Lifecycle can become ON_START at
 * the same time. Only a single Lifecycle can be active at a time so if a Lifecycle becomes ON_START
 * then it will take over as the current active Lifecycle. The original active Lifecycle will
 * become inactive but is kept in the active Lifecycle array. When the Lifecycle of the most
 * recently active camera stops then it will make sure that the next most recently started Lifecycle
 * becomes the active Lifecycle.
 *
 * <p> A LifecycleCamera associated with the repository can also be released from the repository.
 * When it is released, all UseCases bound to the LifecycleCamera will be unbound and the
 * LifecycleCamera will be released.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
final class LifecycleCameraRepository {
    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private final Map<Key, LifecycleCamera> mCameraMap = new HashMap<>();

    @GuardedBy("mLock")
    private final Map<LifecycleCameraRepositoryObserver, Set<Key>> mLifecycleObserverMap =
            new HashMap<>();

    @GuardedBy("mLock")
    private final ArrayDeque<LifecycleOwner> mActiveLifecycleOwners = new ArrayDeque<>();

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
            // Suspend the LifecycleCamera if there is no use case bound.
            if (cameraUseCaseAdaptor.getUseCases().isEmpty()) {
                lifecycleCamera.suspend();
            }
            registerCamera(lifecycleCamera);
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

    /**
     * Clears out all of the cameras from the repository.
     */
    void clear() {
        synchronized (mLock) {
            Set<LifecycleCameraRepositoryObserver> keySet =
                    new HashSet<>(mLifecycleObserverMap.keySet());
            for (LifecycleCameraRepositoryObserver observer : keySet) {
                unregisterLifecycle(observer.getLifecycleOwner());
            }
        }
    }

    /**
     * Registers the LifecycleCamera in the repository so that the repository ensures that only
     * one camera is active at one time.
     *
     * <p>Multiple LifecycleCameras may be controlled by a single LifecycleOwner. Only one
     * lifecycle event observer will be created to monitor a LifecycleOwner's state events. When
     * receiving a state event, the corresponding operations will be applied onto all
     * LifecycleCameras controlled by the same LifecycleOwner.
     */
    private void registerCamera(LifecycleCamera lifecycleCamera) {
        synchronized (mLock) {
            LifecycleOwner lifecycleOwner = lifecycleCamera.getLifecycleOwner();
            Key key = Key.create(lifecycleOwner,
                    lifecycleCamera.getCameraUseCaseAdapter().getCameraId());
            LifecycleCameraRepositoryObserver observer =
                    getLifecycleCameraRepositoryObserver(lifecycleOwner);
            Set<Key> lifecycleCameraKeySet;

            // Retrieves original or creates new key set.
            if (observer != null) {
                lifecycleCameraKeySet = mLifecycleObserverMap.get(observer);
            } else {
                lifecycleCameraKeySet = new HashSet<>();
            }

            lifecycleCameraKeySet.add(key);
            mCameraMap.put(key, lifecycleCamera);

            // Create and put new observer and key set into the map if it didn't exist.
            if (observer == null) {
                observer = new LifecycleCameraRepositoryObserver(lifecycleOwner, this);
                mLifecycleObserverMap.put(observer, lifecycleCameraKeySet);
                lifecycleOwner.getLifecycle().addObserver(observer);
            }
        }
    }

    /**
     * Unregisters the Lifecycle from the repository so it is no longer tracked by the repository.
     *
     * <p>This does not stop the camera from receiving its lifecycle events. For the
     * LifecycleCamera to stop receiving lifecycle event it must be done manually either before
     * or after.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void unregisterLifecycle(LifecycleOwner lifecycleOwner) {
        synchronized (mLock) {
            LifecycleCameraRepositoryObserver observer =
                    getLifecycleCameraRepositoryObserver(lifecycleOwner);

            // There is an error condition that can happen where onDestroy() can possibly be
            // called twice if using Robolectric. The observer for the lifecycle would be removed
            // in the first onDestroy() call and become null in the second onDestroy() call. It
            // will cause an exception when executing the code after the null checker.
            if (observer == null) {
                return;
            }

            setInactive(lifecycleOwner);

            for (Key key: mLifecycleObserverMap.get(observer)) {
                mCameraMap.remove(key);
            }

            mLifecycleObserverMap.remove(observer);
            observer.getLifecycleOwner().getLifecycle().removeObserver(observer);

        }
    }

    private LifecycleCameraRepositoryObserver getLifecycleCameraRepositoryObserver(
            LifecycleOwner lifecycleOwner) {
        synchronized (mLock) {
            for (LifecycleCameraRepositoryObserver observer : mLifecycleObserverMap.keySet()) {
                if (lifecycleOwner.equals(observer.getLifecycleOwner())) {
                    return observer;
                }
            }

            return null;
        }
    }

    /**
     * Binds the use cases to the specified LifecycleCamera.
     *
     * <p>The LifecycleCamera will become active if its Lifecycle state is ON_START. When
     * multiple LifecycleCameras are controlled by the same Lifecycle, only one LifecycleCamera
     * can have use cases bound and become active. When multiple Lifecycles are in ON_START
     * state, only the most recently started Lifecycle which has any LifecycleCamera with use
     * case bound can become active.
     *
     * @param lifecycleCamera The LifecycleCamera which the use cases will be bound to.
     * @param viewPort The viewport which represents the visible camera sensor rect.
     * @param useCases The use cases to bind to a lifecycle.
     * @throws IllegalArgumentException If multiple LifecycleCameras with use cases are
     * registered to the same LifecycleOwner. Or all use cases will exceed the capability of the
     * camera after binding them to the LifecycleCamera.
     */
    void bindToLifecycleCamera(@NonNull LifecycleCamera lifecycleCamera,
            @Nullable ViewPort viewPort, @NonNull Collection<UseCase> useCases) {
        synchronized (mLock) {
            Preconditions.checkArgument(!useCases.isEmpty());
            LifecycleOwner lifecycleOwner = lifecycleCamera.getLifecycleOwner();
            // Disallow multiple LifecycleCameras with use cases to be registered to the same
            // LifecycleOwner.
            LifecycleCameraRepositoryObserver observer =
                    getLifecycleCameraRepositoryObserver(lifecycleOwner);
            Set<Key> lifecycleCameraKeySet = mLifecycleObserverMap.get(observer);

            for (Key key : lifecycleCameraKeySet) {
                LifecycleCamera camera = Preconditions.checkNotNull(mCameraMap.get(key));
                if (!camera.equals(lifecycleCamera) && !camera.getUseCases().isEmpty()) {
                    throw new IllegalArgumentException("Multiple LifecycleCameras with use cases "
                            + "are registered to the same LifecycleOwner.");
                }
            }

            try {
                lifecycleCamera.getCameraUseCaseAdapter().setViewPort(viewPort);
                lifecycleCamera.bind(useCases);
            } catch (CameraUseCaseAdapter.CameraException e) {
                throw new IllegalArgumentException(e.getMessage());
            }

            // The target LifecycleCamera has use case bound. If the target LifecycleOwner has been
            // started, set the target LifecycleOwner as active.
            if (lifecycleOwner.getLifecycle().getCurrentState().isAtLeast(
                    Lifecycle.State.STARTED)) {
                setActive(lifecycleOwner);
            }
        }
    }

    /**
     * Unbinds all specified use cases from the LifecycleCameras managed by the repository.
     *
     * <p>If a LifecycleCamera is active but all use cases are removed at the end of this call,
     * the LifecycleCamera will become inactive. This will also initiate a close of the existing
     * open camera since there is zero {@link UseCase} associated with it.
     *
     * <p>If a use case in the argument list is not bound, then it is simply ignored.
     *
     * @param useCases The collection of use cases to remove.
     */
    void unbind(@NonNull Collection<UseCase> useCases) {
        synchronized (mLock) {
            for (Key key : mCameraMap.keySet()) {
                LifecycleCamera lifecycleCamera = mCameraMap.get(key);
                boolean hasUseCase = !lifecycleCamera.getUseCases().isEmpty();
                lifecycleCamera.unbind(useCases);

                // For a LifecycleOwner, there can be only one LifecycleCamera with use cases bound.
                // Set the target LifecycleOwner as inactive if the LifecycleCamera originally had
                // use cases bound but now all use cases have been unbound.
                if (hasUseCase && lifecycleCamera.getUseCases().isEmpty()) {
                    setInactive(lifecycleCamera.getLifecycleOwner());
                }
            }
        }
    }

    /**
     * Unbinds all use cases from all LifecycleCameras managed by the repository.
     *
     * <p>All LifecycleCameras will become inactive after all use cases are unbound. All
     * Lifecycles that control LifecycleCameras will also be removed from the active Lifecycle
     * array.
     *
     * <p>This will initiate a close of the existing open camera.
     */
    void unbindAll() {
        synchronized (mLock) {
            for (Key key : mCameraMap.keySet()) {
                LifecycleCamera lifecycleCamera = mCameraMap.get(key);
                lifecycleCamera.unbindAll();
                setInactive(lifecycleCamera.getLifecycleOwner());
            }
        }
    }

    /**
     * Makes the LifecycleCamera which is controlled by the LifecycleOwner and has use case bound
     * become active.
     *
     * <p>If there is another LifecycleOwner still in ON_START state, it will be put into the
     * active LifecycleOwner array. The LifecycleCamera controlled by that LifecycleOwner will be
     * suspended and become inactive.
     *
     * <p>If no use case is bound to any LifecycleCamera controlled by the target LifecycleOwner,
     * all LifecycleCameras will keep their original status.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void setActive(LifecycleOwner lifecycleOwner) {
        synchronized (mLock) {
            // Returns if no use case is bound to any LifecycleCamera controlled by the target
            // LifecycleOwner
            if (!hasUseCaseBound(lifecycleOwner)) {
                return;
            }

            // Only keep LifecycleCameras controlled by the last {@link LifecycleOwner} active.
            // Stop the others.
            if (mActiveLifecycleOwners.isEmpty()) {
                mActiveLifecycleOwners.push(lifecycleOwner);
            } else {
                LifecycleOwner currentActiveLifecycleOwner = mActiveLifecycleOwners.peek();
                if (!lifecycleOwner.equals(currentActiveLifecycleOwner)) {
                    suspendUseCases(currentActiveLifecycleOwner);

                    mActiveLifecycleOwners.remove(lifecycleOwner);
                    mActiveLifecycleOwners.push(lifecycleOwner);
                }
            }

            unsuspendUseCases(lifecycleOwner);
        }
    }

    /**
     * Makes all LifecycleCameras controlled by the LifecycleOwner become inactive.
     *
     * <p>If the LifecycleOwner was the current active LifecycleOwner then the next most recent
     * active LifecycleOwner in the active LifecycleOwner array will replace it to become the
     * active one.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void setInactive(LifecycleOwner lifecycleOwner) {
        synchronized (mLock) {
            // Removes stopped lifecycleOwner from active list.
            mActiveLifecycleOwners.remove(lifecycleOwner);
            suspendUseCases(lifecycleOwner);

            // Start up LifecycleCameras controlled by the next LifecycleOwner if there are still
            // active LifecycleOwners.
            if (!mActiveLifecycleOwners.isEmpty()) {
                LifecycleOwner newActiveLifecycleOwner = mActiveLifecycleOwners.peek();
                unsuspendUseCases(newActiveLifecycleOwner);
            }
        }
    }

    /**
     * Checks whether any LifecycleCamera controlled by the LifecycleOwner has any use case bound.
     */
    private boolean hasUseCaseBound(LifecycleOwner lifecycleOwner) {
        synchronized (mLock) {
            LifecycleCameraRepositoryObserver observer =
                    getLifecycleCameraRepositoryObserver(lifecycleOwner);

            if (observer == null) {
                return false;
            }

            Set<Key> lifecycleCameraKeySet = mLifecycleObserverMap.get(observer);

            // Checks whether any LifecycleCamera controlled by the LifecycleOwner has any
            // use case bound.
            for (Key key : lifecycleCameraKeySet) {
                if (!Preconditions.checkNotNull(mCameraMap.get(key)).getUseCases().isEmpty()) {
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * Suspends all LifecycleCameras controlled by the LifecycleOwner.
     */
    private void suspendUseCases(LifecycleOwner lifecycleOwner) {
        synchronized (mLock) {
            LifecycleCameraRepositoryObserver observer =
                    getLifecycleCameraRepositoryObserver(lifecycleOwner);

            for (Key key : mLifecycleObserverMap.get(observer)) {
                Preconditions.checkNotNull(mCameraMap.get(key)).suspend();
            }
        }
    }

    /**
     * Unsuspends all LifecycleCameras controlled by the LifecycleOwner.
     *
     * <p>A LifecycleCamera can be unsuspended only when there is any use case bound on it.
     */
    private void unsuspendUseCases(LifecycleOwner lifecycleOwner) {
        synchronized (mLock) {
            LifecycleCameraRepositoryObserver observer =
                    getLifecycleCameraRepositoryObserver(lifecycleOwner);

            for (Key key : mLifecycleObserverMap.get(observer)) {
                LifecycleCamera lifecycleCamera = mCameraMap.get(key);
                // Only LifecycleCamera with use cases bound can be active.
                if (!Preconditions.checkNotNull(lifecycleCamera).getUseCases().isEmpty()) {
                    lifecycleCamera.unsuspend();
                }
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
        private final LifecycleOwner mLifecycleOwner;

        LifecycleCameraRepositoryObserver(LifecycleOwner lifecycleOwner,
                LifecycleCameraRepository lifecycleCameraRepository) {
            mLifecycleOwner = lifecycleOwner;
            mLifecycleCameraRepository = lifecycleCameraRepository;
        }

        LifecycleOwner getLifecycleOwner() {
            return mLifecycleOwner;
        }

        /**
         * Monitors which {@link LifecycleOwner} receives an ON_START event and then stop
         * other {@link LifecycleCamera} to keep only one active at a time.
         */
        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        public void onStart(LifecycleOwner lifecycleOwner) {
            mLifecycleCameraRepository.setActive(lifecycleOwner);
        }

        /**
         * Monitors which {@link LifecycleOwner} receives an ON_STOP event.
         */
        @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
        public void onStop(LifecycleOwner lifecycleOwner) {
            mLifecycleCameraRepository.setInactive(lifecycleOwner);
        }

        /**
         * Monitors which {@link LifecycleOwner} receives an ON_DESTROY event and then
         * removes any {@link LifecycleCamera} associated with it from this
         * repository when that lifecycle is destroyed.
         */
        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        public void onDestroy(LifecycleOwner lifecycleOwner) {
            mLifecycleCameraRepository.unregisterLifecycle(lifecycleOwner);
        }
    }
}
