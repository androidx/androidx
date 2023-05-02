/*
 * Copyright 2023 The Android Open Source Project
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

/**
 * Schedule an effect to run when the [Lifecycle] receives a specific [Lifecycle.Event].
 *
 * Using a [LifecycleEventObserver] to listen for when [LifecycleEventEffect] enters
 * the composition, [onEvent] will be launched when receiving the specified [event].
 *
 * This function should **not** be used to listen for [Lifecycle.Event.ON_DESTROY] because
 * Compose stops recomposing after receiving a [Lifecycle.Event.ON_STOP] and will never be
 * aware of an ON_DESTROY to launch [onEvent].
 *
 * This function should also **not** be used to launch tasks in response to callback
 * events by way of storing callback data as a [Lifecycle.State] in a [MutableState].
 * Instead, see [currentStateAsState] to obtain a [State<Lifecycle.State>][State]
 * that may be used to launch jobs in response to state changes.
 *
 * @param event The [Lifecycle.Event] to listen for
 * @param lifecycleOwner The lifecycle owner to attach an observer
 * @param onEvent The effect to be launched when we receive an [event] callback
 *
 * @throws IllegalArgumentException if attempting to listen for [Lifecycle.Event.ON_DESTROY]
 */
@Composable
fun LifecycleEventEffect(
    event: Lifecycle.Event,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    onEvent: () -> Unit
) {
    if (event == Lifecycle.Event.ON_DESTROY) {
        throw IllegalArgumentException("LifecycleEventEffect cannot be used to " +
            "listen for Lifecycle.Event.ON_DESTROY, since Compose disposes of the " +
            "composition before ON_DESTROY observers are invoked.")
    }

    // Safely update the current `onEvent` lambda when a new one is provided
    val currentOnEvent by rememberUpdatedState(onEvent)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, e ->
            if (e == event) {
                currentOnEvent()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}