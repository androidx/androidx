/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.remote.creation.compose.capture.headless

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Lightweight, thread-safe [LifecycleOwner] for headless document capture.
 *
 * `androidx.lifecycle.LifecycleRegistry` enforces main-thread access (`enforceMainThreadIfNeeded`),
 * which throws [IllegalStateException] when a capture runs on a background thread (e.g. a Binder
 * thread or [kotlinx.coroutines.Dispatchers.Default]). See also
 * `androidx.lifecycle.LifecycleRegistry.createUnsafe` as prior art for bypassing main-thread
 * enforcement in off-main-thread / headless hosts. This implementation avoids main-thread
 * enforcement and uses per-registration tokens with reference-identity matching so observers can
 * safely remove themselves during event dispatch from any thread.
 *
 * Callbacks are dispatched while holding the owner's lock. This deliberately serializes all
 * lifecycle callbacks, matching the single-threaded delivery guarantee observers get from
 * `LifecycleRegistry` on the main thread, at the cost that a callback must not block on another
 * thread that is itself adding or removing an observer on this owner. As with `LifecycleRegistry`,
 * downward events (`ON_PAUSE` .. `ON_DESTROY`) are delivered in reverse registration order.
 */
internal class HeadlessLifecycleOwner(private val savedState: SavedState? = null) :
    LifecycleOwner, SavedStateRegistryOwner {
    private class Registration(val observer: LifecycleObserver) {
        @Volatile var active = true

        fun dispatch(owner: LifecycleOwner, event: Lifecycle.Event) {
            if (observer is DefaultLifecycleObserver) {
                when (event) {
                    Lifecycle.Event.ON_CREATE -> observer.onCreate(owner)
                    Lifecycle.Event.ON_START -> observer.onStart(owner)
                    Lifecycle.Event.ON_RESUME -> observer.onResume(owner)
                    Lifecycle.Event.ON_PAUSE -> observer.onPause(owner)
                    Lifecycle.Event.ON_STOP -> observer.onStop(owner)
                    Lifecycle.Event.ON_DESTROY -> observer.onDestroy(owner)
                    Lifecycle.Event.ON_ANY -> {}
                }
            }
            // Re-check: a DefaultLifecycleObserver callback above may have removed this observer.
            if (active && observer is LifecycleEventObserver) {
                observer.onStateChanged(owner, event)
            }
        }
    }

    private val lock = Any()
    @Volatile private var state = Lifecycle.State.INITIALIZED
    private val registrations = CopyOnWriteArrayList<Registration>()

    override val lifecycle: Lifecycle =
        object : Lifecycle() {
            override val currentState: State
                get() = state

            override fun addObserver(observer: LifecycleObserver) {
                synchronized(lock) {
                    if (state == State.DESTROYED) {
                        return
                    }
                    val registration = Registration(observer)
                    registrations.add(registration)
                    // Only bring the observer up to the current state, which is below RESUMED if
                    // it is added from a callback while destroy() is in progress.
                    for (event in arrayOf(Event.ON_CREATE, Event.ON_START, Event.ON_RESUME)) {
                        if (!registration.active || event.targetState > state) break
                        registration.dispatch(this@HeadlessLifecycleOwner, event)
                    }
                }
            }

            override fun removeObserver(observer: LifecycleObserver) {
                synchronized(lock) {
                    for (registration in registrations.toTypedArray()) {
                        if (registration.observer === observer) {
                            registration.active = false
                            registrations.remove(registration)
                            break
                        }
                    }
                }
            }
        }

    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    init {
        savedStateRegistryController.performRestore(savedState)
        synchronized(lock) {
            for (event in
                arrayOf(
                    Lifecycle.Event.ON_CREATE,
                    Lifecycle.Event.ON_START,
                    Lifecycle.Event.ON_RESUME,
                )) {
                state = event.targetState
                for (registration in registrations.toTypedArray()) {
                    if (registration.active) registration.dispatch(this, event)
                }
            }
        }
    }

    fun performSave() {
        val outState = savedState ?: return
        savedStateRegistryController.performSave(outState)
    }

    /**
     * Moves to [Lifecycle.State.DESTROYED] one event at a time. For each of `ON_PAUSE`, `ON_STOP`
     * and `ON_DESTROY`, [state] is first set to the event's target state and then the event is
     * delivered to every observer in reverse registration order, so observers see `STARTED` in
     * `onPause`, `CREATED` in `onStop` and `DESTROYED` in `onDestroy`.
     */
    fun destroy() {
        synchronized(lock) {
            for (event in
                arrayOf(
                    Lifecycle.Event.ON_PAUSE,
                    Lifecycle.Event.ON_STOP,
                    Lifecycle.Event.ON_DESTROY,
                )) {
                if (state <= event.targetState) continue
                state = event.targetState
                for (registration in registrations.toTypedArray().reversedArray()) {
                    if (registration.active) registration.dispatch(this, event)
                }
            }
            registrations.clear()
        }
    }
}
