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

/**
 * Schedule a pair of effects to run when the [Lifecycle] receives either a
 * [Lifecycle.Event.ON_START] or [Lifecycle.Event.ON_STOP]. The ON_START effect will
 * be the body of the [effects] block and the ON_STOP effect will be within the
 * (onStop clause)[LifecycleStartStopEffectScope.onStop]:
 *
 * LifecycleStartEffect(lifecycleOwner) {
 *     // add ON_START effect work here
 *
 *     onStop {
 *         // add ON_STOP effect work here
 *     }
 * }
 *
 * A [LifecycleStartEffect] **must** include an [onStop][LifecycleStartStopEffectScope.onStop]
 * clause as the final statement in its [effects] block. If your operation does not require
 * an effect for both ON_START and ON_STOP, a [LifecycleEventEffect] should be used instead.
 *
 * This function uses a [LifecycleEventObserver] to listen for when [LifecycleStartEffect]
 * enters the composition and the effects will be launched when receiving a
 * [Lifecycle.Event.ON_START] or [Lifecycle.Event.ON_STOP] event, respectively.
 *
 * This function should **not** be used to launch tasks in response to callback
 * events by way of storing callback data as a [Lifecycle.State] in a [MutableState].
 * Instead, see [currentStateAsState] to obtain a [State<Lifecycle.State>][State]
 * that may be used to launch jobs in response to state changes.
 *
 * @param lifecycleOwner The lifecycle owner to attach an observer
 * @param effects The effects to be launched when we receive the respective event callbacks
 */
@Composable
fun LifecycleStartEffect(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    effects: LifecycleStartStopEffectScope.() -> LifecycleStopEffectResult
) {
    val lifecycleStartStopEffectScope = LifecycleStartStopEffectScope()
    // Safely update the current `onStart` lambda when a new one is provided
    val currentEffects by rememberUpdatedState(effects)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START ->
                    lifecycleStartStopEffectScope.currentEffects()

                Lifecycle.Event.ON_STOP ->
                    lifecycleStartStopEffectScope.currentEffects().runStopEffect()

                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

/**
 * Interface used for [LifecycleStartEffect] to run the effect within the onStop
 * clause when an (ON_STOP)[Lifecycle.Event.ON_STOP] event is received.
 */
interface LifecycleStopEffectResult {
    fun runStopEffect()
}

/**
 * Receiver scope for [LifecycleStartEffect] that offers the [onStop] clause to
 * couple the ON_START effect. This should be the last statement in any call to
 * [LifecycleStartEffect].
 */
class LifecycleStartStopEffectScope {
    /**
     * Provide the [onStopEffect] to the [LifecycleStartEffect] to run when the observer
     * receives an (ON_STOP)[Lifecycle.Event.ON_STOP] event.
     */
    inline fun onStop(
        crossinline onStopEffect: () -> Unit
    ): LifecycleStopEffectResult = object : LifecycleStopEffectResult {
        override fun runStopEffect() {
            onStopEffect()
        }
    }
}

/**
 * Schedule a pair of effects to run when the [Lifecycle] receives either a
 * [Lifecycle.Event.ON_RESUME] or [Lifecycle.Event.ON_PAUSE]. The ON_RESUME effect
 * will be the body of the [effects] block and the ON_PAUSE effect will be within the
 * (onPause clause)[LifecycleResumePauseEffectScope.onPause]:
 *
 * LifecycleResumeEffect(lifecycleOwner) {
 *     // add ON_RESUME effect work here
 *
 *     onPause {
 *         // add ON_PAUSE effect work here
 *     }
 * }
 *
 * A [LifecycleResumeEffect] **must** include an [onPause][LifecycleResumePauseEffectScope.onPause]
 * clause as the final statement in its [effects] block. If your operation does not require
 * an effect for both ON_RESUME and ON_PAUSE, a [LifecycleEventEffect] should be used instead.
 *
 * This function uses a [LifecycleEventObserver] to listen for when [LifecycleResumeEffect]
 * enters the composition and the effects will be launched when receiving a
 * [Lifecycle.Event.ON_RESUME] or [Lifecycle.Event.ON_PAUSE] event, respectively.
 *
 * This function should **not** be used to launch tasks in response to callback
 * events by way of storing callback data as a [Lifecycle.State] in a [MutableState].
 * Instead, see [currentStateAsState] to obtain a [State<Lifecycle.State>][State]
 * that may be used to launch jobs in response to state changes.
 *
 * @param lifecycleOwner The lifecycle owner to attach an observer
 * @param effects The effects to be launched when we receive the respective event callbacks
 */
@Composable
fun LifecycleResumeEffect(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    effects: LifecycleResumePauseEffectScope.() -> LifecyclePauseEffectResult
) {
    val lifecycleResumePauseEffectScope = LifecycleResumePauseEffectScope()
    // Safely update the current `onResume` lambda when a new one is provided
    val currentEffects by rememberUpdatedState(effects)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME ->
                    lifecycleResumePauseEffectScope.currentEffects()

                Lifecycle.Event.ON_PAUSE ->
                    lifecycleResumePauseEffectScope.currentEffects().runPauseEffect()

                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

/**
 * Interface used for [LifecycleResumeEffect] to run the effect within the onPause
 * clause when an (ON_PAUSE)[Lifecycle.Event.ON_PAUSE] event is received.
 */
interface LifecyclePauseEffectResult {
    fun runPauseEffect()
}

/**
 * Receiver scope for [LifecycleResumeEffect] that offers the [onPause] clause to
 * couple the ON_RESUME effect. This should be the last statement in any call to
 * [LifecycleResumeEffect].
 */
class LifecycleResumePauseEffectScope {
    /**
     * Provide the [onPauseEffect] to the [LifecycleResumeEffect] to run when the observer
     * receives an (ON_PAUSE)[Lifecycle.Event.ON_PAUSE] event.
     */
    inline fun onPause(
        crossinline onPauseEffect: () -> Unit
    ): LifecyclePauseEffectResult = object : LifecyclePauseEffectResult {
        override fun runPauseEffect() {
            onPauseEffect()
        }
    }
}
