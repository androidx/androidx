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
import kotlin.jvm.JvmOverloads
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Create a [LifecycleOwner] that allows changing the state via the [handleLifecycleEvent] method or
 * [currentState] property.
 *
 * Under the hood, this uses a [LifecycleRegistry]. However, it uses
 * [Dispatchers.Main.immediate][kotlinx.coroutines.MainCoroutineDispatcher.immediate] as the default
 * [coroutineDispatcher] to ensure that all mutations to the [current state][currentState] are run
 * on that dispatcher, no matter what thread you mutate the state from.
 *
 * @param initialState The initial [Lifecycle.State].
 * @param coroutineDispatcher A [CoroutineDispatcher] to use when dispatching work from this class.
 */
public class TestLifecycleOwner
@JvmOverloads
constructor(
    initialState: Lifecycle.State = Lifecycle.State.STARTED,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : LifecycleOwner {
    // it is in test artifact
    private val lifecycleRegistry =
        LifecycleRegistry.createUnsafe(this).apply { currentState = initialState }

    override val lifecycle: LifecycleRegistry
        get() = lifecycleRegistry

    /**
     * Update the [currentState] by moving it to the state directly after the given [event]. This is
     * safe to mutate on any thread, but will block that thread during execution.
     */
    public fun handleLifecycleEvent(event: Lifecycle.Event) {
        runBlockingIfPossible(coroutineDispatcher) { lifecycleRegistry.handleLifecycleEvent(event) }
    }

    /**
     * The current [Lifecycle.State] of this owner. This is safe to call on any thread but is
     * thread-blocking and should not be called from within a coroutine (use [setCurrentState]
     * instead).
     */
    public var currentState: Lifecycle.State
        get() = runBlockingIfPossible(coroutineDispatcher) { lifecycleRegistry.currentState }
        set(value) {
            runBlockingIfPossible(coroutineDispatcher) { lifecycleRegistry.currentState = value }
        }

    /**
     * Updates the [currentState]. This suspending function is safe to call on any thread and will
     * not block that thread. If the state should be updated from outside of a suspending function,
     * use [currentState] property syntax instead.
     */
    public suspend fun setCurrentState(state: Lifecycle.State) {
        withContext(coroutineDispatcher) { lifecycleRegistry.currentState = state }
    }

    /** Get the number of observers. */
    public val observerCount: Int
        get() = lifecycleRegistry.observerCount
}

/**
 * Executes the given block, blocking the current thread if the target platform supports it (e.g.,
 * JVM, Native).
 *
 * On single-threaded platforms like Kotlin/JS and Wasm, `runBlocking` is not supported as it would
 * freeze the only available thread. On these targets, this function will simply execute the block
 * directly without blocking.
 */
internal expect fun <T> runBlockingIfPossible(dispatcher: CoroutineDispatcher, block: () -> T): T
