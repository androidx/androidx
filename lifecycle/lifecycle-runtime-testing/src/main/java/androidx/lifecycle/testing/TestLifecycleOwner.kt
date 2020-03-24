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

package androidx.lifecycle.testing

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * Create a [LifecycleOwner] that allows changing the state via the
 * [handleLifecycleEvent] method or [currentState] property.
 *
 * Under the hood, this uses a [LifecycleRegistry]. However, it uses
 * [Dispatchers.Main.immediate][kotlinx.coroutines.MainCoroutineDispatcher.immediate] as the
 * default [coroutineDispatcher] to ensure that all
 * mutations to the [current state][currentState] are run on that dispatcher, no
 * matter what thread you mutate the state from.
 *
 * @param initialState The initial [Lifecycle.State].
 */
class TestLifecycleOwner @JvmOverloads constructor(
    initialState: Lifecycle.State = Lifecycle.State.STARTED,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
) : LifecycleOwner {
    private val lifecycleRegistry = LifecycleRegistry(this).apply {
        currentState = initialState
    }
    override fun getLifecycle() = lifecycleRegistry

    /**
     * Update the [currentState] by moving it to the state directly after the given [event].
     * This is safe to call on any thread.
     */
    fun handleLifecycleEvent(event: Lifecycle.Event) {
        runBlocking(coroutineDispatcher) {
            lifecycleRegistry.handleLifecycleEvent(event)
        }
    }
    /**
     * The current [Lifecycle.State] of this owner. This is safe to mutate on any thread.
     */
    var currentState: Lifecycle.State
        get() = runBlocking(coroutineDispatcher) {
            lifecycleRegistry.currentState
        }
        set(value) {
            runBlocking(coroutineDispatcher) {
                lifecycleRegistry.currentState = value
            }
        }

    /**
     * Get the number of observers.
     */
    val observerCount get() = lifecycleRegistry.observerCount
}
