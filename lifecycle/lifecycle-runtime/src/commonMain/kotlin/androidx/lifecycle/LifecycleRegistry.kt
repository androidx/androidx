/*
 * Copyright (C) 2024 The Android Open Source Project
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
package androidx.lifecycle

import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.Lifecycle.State
import kotlin.jvm.JvmStatic

/**
 * An implementation of [Lifecycle] that can handle multiple observers.
 *
 * It is used by Fragments and Support Library Activities. You can also directly use it if you have
 * a custom LifecycleOwner.
 */
public expect open class LifecycleRegistry

/**
 * Creates a new LifecycleRegistry for the given provider.
 *
 * You should usually create this inside your LifecycleOwner class's constructor and hold onto the
 * same instance.
 *
 * @param provider The owner LifecycleOwner
 */
constructor(provider: LifecycleOwner) : Lifecycle {
    override var currentState: State

    @MainThread() override fun addObserver(observer: LifecycleObserver)

    @MainThread() override fun removeObserver(observer: LifecycleObserver)

    /**
     * Sets the current state and notifies the observers.
     *
     * Note that if the `currentState` is the same state as the last call to this method, calling
     * this method has no effect.
     *
     * @param event The event that was received
     */
    public open fun handleLifecycleEvent(event: Event)

    /**
     * The number of observers.
     *
     * @return The number of observers.
     */
    public open val observerCount: Int

    public companion object {
        /**
         * Creates a new LifecycleRegistry for the given provider, that doesn't check that its
         * methods are called on the threads other than main.
         *
         * LifecycleRegistry is not synchronized: if multiple threads access this
         * `LifecycleRegistry`, it must be synchronized externally.
         *
         * Another possible use-case for this method is JVM testing, when main thread is not
         * present.
         */
        @JvmStatic
        @VisibleForTesting
        public fun createUnsafe(owner: LifecycleOwner): LifecycleRegistry
    }
}

/**
 * Checks the [Lifecycle.State] of a component and throws an error if an invalid state transition is
 * detected.
 *
 * @param owner The [LifecycleOwner] holding the [Lifecycle] of the component.
 * @param current The current [Lifecycle.State] of the component.
 * @param next The desired next [Lifecycle.State] of the component.
 * @throws IllegalStateException if the component is in an invalid state for the desired transition.
 */
internal fun checkLifecycleStateTransition(owner: LifecycleOwner?, current: State, next: State) {
    if (current == State.INITIALIZED && next == State.DESTROYED) {
        error(
            "State must be at least '${State.CREATED}' to be moved to '$next' in component $owner"
        )
    }
    if (current == State.DESTROYED && current != next) {
        error("State is '${State.DESTROYED}' and cannot be moved to `$next` in component $owner")
    }
}
