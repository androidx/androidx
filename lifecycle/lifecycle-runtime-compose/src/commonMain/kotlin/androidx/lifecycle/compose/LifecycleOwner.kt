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

package androidx.lifecycle.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.Lifecycle.Event.ON_RESUME
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.Lifecycle.State.DESTROYED
import androidx.lifecycle.Lifecycle.State.RESUMED
import androidx.lifecycle.Lifecycle.State.STARTED
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

/**
 * Creates a new [LifecycleOwner] with a lifecycle that is a child of the parent composition's
 * lifecycle.
 *
 * This is useful for creating components (e.g., a map view) that need a lifecycle shorter than the
 * host screen. The child lifecycle will never be in a state greater than its parent, and it can be
 * further restricted by the [maxLifecycle] parameter. For example, you can ensure a component's
 * lifecycle never goes beyond [STARTED], even if the parent Fragment is [RESUMED].
 *
 * When the composable leaves the composition, the child lifecycle will be moved to [DESTROYED].
 * This ensures the child is properly cleaned up even if it is referenced outside the composition.
 *
 * **Null parent:** If [parent] is **EXPLICITLY** `null`, this creates a root lifecycle that runs
 * independently and manages its own state.
 *
 * @param maxLifecycle The maximum [Lifecycle.State] this child lifecycle is allowed to enter.
 *   Defaults to [RESUMED].
 * @param parent The [LifecycleOwner] to use as the parent, or null if it is a root. Defaults to the
 *   [LocalLifecycleOwner].
 * @param content The composable content that will be scoped to the new child lifecycle.
 */
@Composable
public fun LifecycleOwner(
    maxLifecycle: State = RESUMED,
    parent: LifecycleOwner? = LocalLifecycleOwner.current,
    content: @Composable () -> Unit,
) {
    val localLifecycleOwner = remember(parent) { ComposeLifecycleOwner() }

    // Pass LifecycleEvents from the parent down to the child.
    DisposableEffect(localLifecycleOwner, parent) {
        val observer = LifecycleEventObserver { _, event ->
            // Connect parent's events to the child lifecycle.
            localLifecycleOwner.handleLifecycleEvent(event)
        }

        // Add observer only if there is a parent.
        parent?.lifecycle?.addObserver(observer)

        if (parent == null) {
            // No parent: manually mark this lifecycle as RESUMED.
            localLifecycleOwner.handleLifecycleEvent(event = ON_RESUME)
        }

        onDispose {
            // Remove observer if it was added (has a parent).
            parent?.lifecycle?.removeObserver(observer)

            // Manually dispatch ON_DESTROY. This ensures that any code holding a reference to this
            // from outside a composition is notified that it has been permanently destroyed.
            localLifecycleOwner.handleLifecycleEvent(event = ON_DESTROY)
        }
    }

    // Ensure that the child lifecycle is capped at the maxLifecycle.
    LaunchedEffect(localLifecycleOwner, maxLifecycle) {
        localLifecycleOwner.maxLifecycleState = maxLifecycle
    }

    // Now install the LifecycleOwner as a composition local.
    CompositionLocalProvider(LocalLifecycleOwner provides localLifecycleOwner, content = content)
}

/**
 * A private [LifecycleOwner] that is controlled by a parent's lifecycle and capped by a maximum
 * state.
 */
private class ComposeLifecycleOwner : LifecycleOwner {

    private val lifecycleRegistry = LifecycleRegistry(provider = this)

    override val lifecycle
        get() = lifecycleRegistry

    // Tracks the last known state from the parent lifecycle.
    private var parentLifecycleState: State = State.INITIALIZED

    // The maximum state this lifecycle can enter.
    var maxLifecycleState: State = State.INITIALIZED
        set(value) {
            field = value
            updateLifecycleState()
        }

    fun handleLifecycleEvent(event: Lifecycle.Event) {
        parentLifecycleState = event.targetState
        updateLifecycleState()
    }

    private fun updateLifecycleState() {
        // The child's state is capped at the minimum of the parent's state and the max state.
        // For example, if parent is RESUMED and max is STARTED, the child state becomes STARTED.
        lifecycleRegistry.currentState =
            if (parentLifecycleState.ordinal < maxLifecycleState.ordinal) {
                parentLifecycleState
            } else {
                maxLifecycleState
            }
    }
}
