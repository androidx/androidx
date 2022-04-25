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

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

/**
 * A fake lifecycle owner which obeys the lifecycle transition rules.
 *
 * @hide
 * @see <a href="https://developer.android.com/topic/libraries/architecture/lifecycle">lifecycle</a>
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@RestrictTo(Scope.LIBRARY_GROUP)
public final class FakeLifecycleOwner implements LifecycleOwner {
    private final LifecycleRegistry mLifecycleRegistry;

    /**
     * Creates a new lifecycle owner.
     *
     * <p>The lifecycle is initial put into the INITIALIZED and CREATED states.
     */
    public FakeLifecycleOwner() {
        mLifecycleRegistry = new LifecycleRegistry(this);
        mLifecycleRegistry.setCurrentState(Lifecycle.State.INITIALIZED);
        mLifecycleRegistry.setCurrentState(Lifecycle.State.CREATED);
    }

    /**
     * Starts and resumes the lifecycle.
     *
     * <p>The lifecycle is put into the STARTED and RESUMED states. The lifecycle must already be in
     * the CREATED state or an exception is thrown.
     */
    public void startAndResume() {
        if (mLifecycleRegistry.getCurrentState() != Lifecycle.State.CREATED) {
            throw new IllegalStateException("Invalid state transition.");
        }
        mLifecycleRegistry.setCurrentState(Lifecycle.State.STARTED);
        mLifecycleRegistry.setCurrentState(Lifecycle.State.RESUMED);
    }

    /**
     * Starts the lifecycle.
     *
     * <p>The lifecycle is put into the START state. The lifecycle must already be in the CREATED
     * state or an exception is thrown.
     */
    public void start() {
        if (mLifecycleRegistry.getCurrentState() != Lifecycle.State.CREATED) {
            throw new IllegalStateException("Invalid state transition.");
        }
        mLifecycleRegistry.setCurrentState(Lifecycle.State.STARTED);
    }

    /**
     * Pauses and stops the lifecycle.
     *
     * <p>The lifecycle is put into the STARTED and CREATED states. The lifecycle must already be in
     * the RESUMED state or an exception is thrown.
     */
    public void pauseAndStop() {
        if (mLifecycleRegistry.getCurrentState() != Lifecycle.State.RESUMED) {
            throw new IllegalStateException("Invalid state transition.");
        }
        mLifecycleRegistry.setCurrentState(Lifecycle.State.STARTED);
        mLifecycleRegistry.setCurrentState(Lifecycle.State.CREATED);
    }

    /**
     * Stops the lifecycle.
     *
     * <p>The lifecycle is put into the CREATED state. The lifecycle must already be in the STARTED
     * state or an exception is thrown.
     */
    public void stop() {
        if (mLifecycleRegistry.getCurrentState() != Lifecycle.State.STARTED) {
            throw new IllegalStateException("Invalid state transition.");
        }
        mLifecycleRegistry.setCurrentState(Lifecycle.State.CREATED);
    }

    /**
     * Destroys the lifecycle.
     *
     * <p>The lifecycle is put into the DESTROYED state. The lifecycle must already be in the
     * CREATED state or an exception is thrown.
     */
    public void destroy() {
        if (mLifecycleRegistry.getCurrentState() != Lifecycle.State.CREATED) {
            throw new IllegalStateException("Invalid state transition.");
        }
        mLifecycleRegistry.setCurrentState(Lifecycle.State.DESTROYED);
    }

    /** Returns the number of observers of this lifecycle. */
    public int getObserverCount() {
        return mLifecycleRegistry.getObserverCount();
    }

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return mLifecycleRegistry;
    }
}
