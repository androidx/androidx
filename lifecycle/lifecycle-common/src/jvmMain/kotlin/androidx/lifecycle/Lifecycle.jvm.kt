/*
 * Copyright (C) 2017 The Android Open Source Project
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

import androidx.annotation.RestrictTo
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// TODO(b/320544977): Switch back to typealias when RestrictTo supports it. For now, we use an
//  actual class implementation that delegates to the Java class, ensuring that the signatures of
//  the members match the ones expected by the expect declaration
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public actual class AtomicReference<V> actual constructor(initialValue: V) {

    private val base = AtomicReference<V>(initialValue)

    public actual fun get(): V = base.get()

    public actual fun compareAndSet(expectedValue: V, newValue: V): Boolean =
        base.compareAndSet(expectedValue, newValue)
}

/**
 * [CoroutineScope] tied to a [Lifecycle] and
 * [Dispatchers.Main.immediate][kotlinx.coroutines.MainCoroutineDispatcher.immediate]
 *
 * This scope will be cancelled when the [Lifecycle] is destroyed.
 *
 * This scope provides specialised versions of `launch`: [launchWhenCreated], [launchWhenStarted],
 * [launchWhenResumed]
 */
public actual abstract class LifecycleCoroutineScope internal actual constructor() :
    CoroutineScope {
    internal actual abstract val lifecycle: Lifecycle

    /**
     * Launches and runs the given block when the [Lifecycle] controlling this
     * [LifecycleCoroutineScope] is at least in [Lifecycle.State.CREATED] state.
     *
     * The returned [Job] will be cancelled when the [Lifecycle] is destroyed.
     *
     * @see Lifecycle.whenCreated
     * @see Lifecycle.coroutineScope
     */
    @Deprecated(
        message =
            "launchWhenCreated is deprecated as it can lead to wasted resources " +
                "in some cases. Replace with suspending repeatOnLifecycle to run the block whenever " +
                "the Lifecycle state is at least Lifecycle.State.CREATED."
    )
    @Suppress("DEPRECATION")
    public fun launchWhenCreated(block: suspend CoroutineScope.() -> Unit): Job = launch {
        lifecycle.whenCreated(block)
    }

    /**
     * Launches and runs the given block when the [Lifecycle] controlling this
     * [LifecycleCoroutineScope] is at least in [Lifecycle.State.STARTED] state.
     *
     * The returned [Job] will be cancelled when the [Lifecycle] is destroyed.
     *
     * @see Lifecycle.whenStarted
     * @see Lifecycle.coroutineScope
     */
    @Deprecated(
        message =
            "launchWhenStarted is deprecated as it can lead to wasted resources " +
                "in some cases. Replace with suspending repeatOnLifecycle to run the block whenever " +
                "the Lifecycle state is at least Lifecycle.State.STARTED."
    )
    @Suppress("DEPRECATION")
    public fun launchWhenStarted(block: suspend CoroutineScope.() -> Unit): Job = launch {
        lifecycle.whenStarted(block)
    }

    /**
     * Launches and runs the given block when the [Lifecycle] controlling this
     * [LifecycleCoroutineScope] is at least in [Lifecycle.State.RESUMED] state.
     *
     * The returned [Job] will be cancelled when the [Lifecycle] is destroyed.
     *
     * @see Lifecycle.whenResumed
     * @see Lifecycle.coroutineScope
     */
    @Deprecated(
        message =
            "launchWhenResumed is deprecated as it can lead to wasted resources " +
                "in some cases. Replace with suspending repeatOnLifecycle to run the block whenever " +
                "the Lifecycle state is at least Lifecycle.State.RESUMED."
    )
    @Suppress("DEPRECATION")
    public fun launchWhenResumed(block: suspend CoroutineScope.() -> Unit): Job = launch {
        lifecycle.whenResumed(block)
    }
}
