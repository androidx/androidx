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

package androidx.lifecycle.compose.samples

import androidx.annotation.Sampled
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.lifecycle.compose.rememberLifecycleOwner

/**
 * Shows the current Lifecycle state. Used by the samples below to make changes to the Lifecycle
 * easy to see.
 */
@Composable
private fun LifecycleAwareText() {
    // Recompose when the Lifecycle state changes.
    val lifecycleState = LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    Text(text = "Current lifecycle state is ${lifecycleState.value}")
}

@Sampled
@Composable
fun ComposableWithMaxLifecycle() {
    // Create a LifecycleOwner tied to this composition and cap it at STARTED.
    // Use this when a child should never go to RESUMED, even if the parent does.
    val cappedLifecycleOwner = rememberLifecycleOwner(maxLifecycle = Lifecycle.State.STARTED)

    // Make children see the capped Lifecycle instead of the parent's.
    CompositionLocalProvider(LocalLifecycleOwner provides cappedLifecycleOwner) {
        // Will only report CREATED or STARTED, never RESUMED.
        LifecycleAwareText()
    }
}

@Sampled
@Composable
fun ComposableWithParentNull() {
    // Create a LifecycleOwner with no parent.
    // Use this when a child needs its own independent Lifecycle, managed only
    // by whether it is in the composition. Parent state changes do not affect it.
    val detachedLifecycleOwner =
        rememberLifecycleOwner(parent = null, maxLifecycle = Lifecycle.State.STARTED)

    CompositionLocalProvider(LocalLifecycleOwner provides detachedLifecycleOwner) {
        // Stays STARTED until this composable leaves the composition,
        // even if the parent moves to a lower state.
        LifecycleAwareText()
    }
}
