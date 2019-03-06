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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * [CoroutineScope] tied to this [Lifecycle].
 *
 * This scope will be canceled when the [Lifecycle] is destroyed.
 *
 * This scope is bound to  [Dispatchers.Main]
 */
val Lifecycle.coroutineScope: CoroutineScope
    get() {
        while (true) {
            val existing = mInternalScopeRef.get() as LifecycleCoroutineScope?
            if (existing != null) {
                return existing
            }
            val newScope = LifecycleCoroutineScope(
                this,
                SupervisorJob() + Dispatchers.Main
            )
            if (mInternalScopeRef.compareAndSet(null, newScope)) {
                newScope.register()
                return newScope
            }
        }
    }

internal class LifecycleCoroutineScope(
    private val lifecycle: Lifecycle,
    override val coroutineContext: CoroutineContext
) : CoroutineScope, LifecycleEventObserver {
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
                lifecycle.addObserver(this@LifecycleCoroutineScope)
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