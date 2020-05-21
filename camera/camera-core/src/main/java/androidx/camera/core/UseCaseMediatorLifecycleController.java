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
import androidx.camera.core.impl.UseCaseMediator;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.Lifecycle.State;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

/** A {@link UseCaseMediator} whose starting and stopping is controlled by a {@link Lifecycle}. */
final class UseCaseMediatorLifecycleController implements LifecycleObserver {
    private final Object mUseCaseMediatorLock = new Object();

    @GuardedBy("mUseCaseMediatorLock")
    private final UseCaseMediator mUseCaseMediator;

    /** The lifecycle that controls the {@link UseCaseMediator}. */
    private final Lifecycle mLifecycle;

    /** Creates a new {@link UseCaseMediator} which gets controlled by lifecycle transitions. */
    UseCaseMediatorLifecycleController(Lifecycle lifecycle) {
        this(lifecycle, new UseCaseMediator());
    }

    /** Wraps an existing {@link UseCaseMediator} so it is controlled by lifecycle transitions. */
    UseCaseMediatorLifecycleController(Lifecycle lifecycle, UseCaseMediator useCaseMediator) {
        this.mUseCaseMediator = useCaseMediator;
        this.mLifecycle = lifecycle;
        lifecycle.addObserver(this);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart(LifecycleOwner lifecycleOwner) {
        synchronized (mUseCaseMediatorLock) {
            mUseCaseMediator.start();
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onStop(LifecycleOwner lifecycleOwner) {
        synchronized (mUseCaseMediatorLock) {
            mUseCaseMediator.stop();
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void onDestroy(LifecycleOwner lifecycleOwner) {
        synchronized (mUseCaseMediatorLock) {
            mUseCaseMediator.destroy();
        }
    }

    /**
     * Starts the underlying {@link UseCaseMediator} so that its {@link
     * UseCaseMediator.StateChangeCallback} can be notified.
     *
     * <p>This is required when the contained {@link Lifecycle} is in a STARTED state, since the
     * default state for a {@link UseCaseMediator} is inactive. The explicit call forces a check on
     * the actual state of the mediator.
     */
    void notifyState() {
        synchronized (mUseCaseMediatorLock) {
            if (mLifecycle.getCurrentState().isAtLeast(State.STARTED)) {
                mUseCaseMediator.start();
            }
            for (UseCase useCase : mUseCaseMediator.getUseCases()) {
                useCase.notifyState();
            }
        }
    }

    UseCaseMediator getUseCaseMediator() {
        synchronized (mUseCaseMediatorLock) {
            return mUseCaseMediator;
        }
    }

    /**
     * Stops observing lifecycle changes.
     *
     * <p>Once released the wrapped {@link UseCaseMediator} is still valid, but will no longer be
     * triggered by lifecycle state transitions. In order to observe lifecycle changes again a new
     * {@link UseCaseMediatorLifecycleController} instance should be created.
     *
     * <p>Calls subsequent to the first time will do nothing.
     */
    void release() {
        mLifecycle.removeObserver(this);
    }
}
