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
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.Lifecycle.State;
import androidx.lifecycle.LifecycleOwner;

/** A {@link UseCaseGroup} whose starting and stopping is controlled by a {@link Lifecycle}. */
final class UseCaseGroupLifecycleController implements DefaultLifecycleObserver {
    private final Object mUseCaseGroupLock = new Object();

    @GuardedBy("mUseCaseGroupLock")
    private final UseCaseGroup mUseCaseGroup;

    /** The lifecycle that controls the {@link UseCaseGroup}. */
    private final Lifecycle mLifecycle;

    /** Creates a new {@link UseCaseGroup} which gets controlled by lifecycle transitions. */
    UseCaseGroupLifecycleController(Lifecycle lifecycle) {
        this(lifecycle, new UseCaseGroup());
    }

    /** Wraps an existing {@link UseCaseGroup} so it is controlled by lifecycle transitions. */
    UseCaseGroupLifecycleController(Lifecycle lifecycle, UseCaseGroup useCaseGroup) {
        this.mUseCaseGroup = useCaseGroup;
        this.mLifecycle = lifecycle;
        lifecycle.addObserver(this);
    }

    @Override
    public void onStart(LifecycleOwner lifecycleOwner) {
        synchronized (mUseCaseGroupLock) {
            mUseCaseGroup.start();
        }
    }

    @Override
    public void onStop(LifecycleOwner lifecycleOwner) {
        synchronized (mUseCaseGroupLock) {
            mUseCaseGroup.stop();
        }
    }

    @Override
    public void onDestroy(LifecycleOwner lifecycleOwner) {
        synchronized (mUseCaseGroupLock) {
            mUseCaseGroup.clear();
        }
    }

    /**
     * Starts the underlying {@link UseCaseGroup} so that its {@link
     * UseCaseGroup.StateChangeListener} can be notified.
     *
     * <p>This is required when the contained {@link Lifecycle} is in a STARTED state, since the
     * default state for a {@link UseCaseGroup} is inactive. The explicit call forces a check on the
     * actual state of the group.
     */
    void notifyState() {
        synchronized (mUseCaseGroupLock) {
            if (mLifecycle.getCurrentState().isAtLeast(State.STARTED)) {
                mUseCaseGroup.start();
            }
            for (BaseUseCase useCase : mUseCaseGroup.getUseCases()) {
                useCase.notifyState();
            }
        }
    }

    UseCaseGroup getUseCaseGroup() {
        synchronized (mUseCaseGroupLock) {
            return mUseCaseGroup;
        }
    }

    /**
     * Stops observing lifecycle changes.
     *
     * <p>Once released the wrapped {@link UseCaseGroup} is still valid, but will no longer be
     * triggered by lifecycle state transitions. In order to observe lifecycle changes again a new
     * {@link UseCaseGroupLifecycleController} instance should be created.
     *
     * <p>Calls subsequent to the first time will do nothing.
     */
    void release() {
        mLifecycle.removeObserver(this);
    }
}
