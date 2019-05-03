/*
 * Copyright 2019 The Android Open Source Project
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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * [CoroutineScope] tied to this [Lifecycle].
 *
 * This scope will be cancelled when the [Lifecycle] is destroyed.
 *
 * This scope is bound to [Dispatchers.Main]
 */
val Lifecycle.coroutineScope: LifecycleCoroutineScope
    get() {
        while (true) {
            val existing = mInternalScopeRef.get() as LifecycleCoroutineScopeImpl?
            if (existing != null) {
                return existing
            }
            val newScope = LifecycleCoroutineScopeImpl(
                this,
                SupervisorJob() + Dispatchers.Main
            )
            if (mInternalScopeRef.compareAndSet(null, newScope)) {
                newScope.register()
                return newScope
            }
        }
    }

/**
 * [CoroutineScope] tied to a [Lifecycle] and [Dispatchers.Main]
 *
 * This scope will be cancelled when the [Lifecycle] is destroyed.
 *
 * This scope provides specialised versions of `launch`: [launchWhenCreated], [launchWhenStarted],
 * [launchWhenResumed]
 */
abstract class LifecycleCoroutineScope internal constructor() : CoroutineScope {
    internal abstract val lifecycle: Lifecycle

    /**
     * Launches and runs the given block when the [Lifecycle] controlling this
     * [LifecycleCoroutineScope] is at least in [Lifecycle.State.CREATED] state.
     *
     * The returned [Job] will be cancelled when the [Lifecycle] is destroyed.
     * @see Lifecycle.whenCreated
     * @see Lifecycle.coroutineScope
     */
    fun launchWhenCreated(block: suspend CoroutineScope.() -> Unit): Job = launch {
        lifecycle.whenCreated(block)
    }

    /**
     * Launches and runs the given block when the [Lifecycle] controlling this
     * [LifecycleCoroutineScope] is at least in [Lifecycle.State.STARTED] state.
     *
     * The returned [Job] will be cancelled when the [Lifecycle] is destroyed.
     * @see Lifecycle.whenStarted
     * @see Lifecycle.coroutineScope
     */

    fun launchWhenStarted(block: suspend CoroutineScope.() -> Unit): Job = launch {
        lifecycle.whenStarted(block)
    }

    /**
     * Launches and runs the given block when the [Lifecycle] controlling this
     * [LifecycleCoroutineScope] is at least in [Lifecycle.State.RESUMED] state.
     *
     * The returned [Job] will be cancelled when the [Lifecycle] is destroyed.
     * @see Lifecycle.whenResumed
     * @see Lifecycle.coroutineScope
     */
    fun launchWhenResumed(block: suspend CoroutineScope.() -> Unit): Job = launch {
        lifecycle.whenResumed(block)
    }
}

internal class LifecycleCoroutineScopeImpl(
    override val lifecycle: Lifecycle,
    override val coroutineContext: CoroutineContext
) : LifecycleCoroutineScope(), LifecycleEventObserver {
    init {
        // in case we are initialized on a non-main thread, make a best effort check before
        // we return the scope. This is not sync but if developer is launching on a non-main
        // dispatcher, they cannot be 100% sure anyways.
        if (lifecycle.currentState == Lifecycle.State.DESTROYED) {
            coroutineContext.cancel()
        }
    }

    fun register() {
        // TODO use Main.Immediate once it is graduated out of experimental.
        launch(Dispatchers.Main) {
            if (lifecycle.currentState >= Lifecycle.State.INITIALIZED) {
                lifecycle.addObserver(this@LifecycleCoroutineScopeImpl)
            } else {
                coroutineContext.cancel()
            }
        }
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (lifecycle.currentState <= Lifecycle.State.DESTROYED) {
            lifecycle.removeObserver(this)
            coroutineContext.cancel()
        }
    }
}