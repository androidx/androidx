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
 * @sample androidx.lifecycle.compose.samples.lifecycleEventEffectSample
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
 * (onStopOrDispose clause)[LifecycleStartStopEffectScope.onStopOrDispose]:
 *
 * LifecycleStartEffect(lifecycleOwner) {
 *     // add ON_START effect here
 *
 *     onStopOrDispose {
 *         // add clean up for work kicked off in the ON_START effect here
 *     }
 * }
 *
 * @sample androidx.lifecycle.compose.samples.lifecycleStartEffectSample
 *
 * A [LifecycleStartEffect] **must** include an
 * [onStopOrDispose][LifecycleStartStopEffectScope.onStopOrDispose] clause as the final
 * statement in its [effects] block. If your operation does not require an effect for
 * _both_ [Lifecycle.Event.ON_START] and [Lifecycle.Event.ON_STOP], a [LifecycleEventEffect]
 * should be used instead.
 *
 * This function uses a [LifecycleEventObserver] to listen for when [LifecycleStartEffect]
 * enters the composition and the effects will be launched when receiving a
 * [Lifecycle.Event.ON_START] or [Lifecycle.Event.ON_STOP] event, respectively. If the
 * [LifecycleStartEffect] leaves the composition prior to receiving an [Lifecycle.Event.ON_STOP]
 * event, [onStopOrDispose][LifecycleStartStopEffectScope.onStopOrDispose] will be called to
 * clean up the work that was kicked off in the ON_START effect.
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
    effects: LifecycleStartStopEffectScope.() -> LifecycleStopOrDisposeEffectResult
) {
    val lifecycleStartStopEffectScope = LifecycleStartStopEffectScope(lifecycleOwner.lifecycle)

    // Safely update the current `onStart` lambda when a new one is provided
    val currentEffects by rememberUpdatedState(effects)
    DisposableEffect(lifecycleOwner) {
        var effectResult: LifecycleStopOrDisposeEffectResult? = null
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> with(lifecycleStartStopEffectScope) {
                        effectResult = currentEffects()
                    }

                Lifecycle.Event.ON_STOP -> effectResult?.runStopOrDisposeEffect()

                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            effectResult?.runStopOrDisposeEffect()
        }
    }
}

/**
 * Interface used for [LifecycleStartEffect] to run the effect within the onStopOrDispose
 * clause when an (ON_STOP)[Lifecycle.Event.ON_STOP] event is received or when cleanup is
 * needed for the work that was kicked off in the ON_START effect.
 */
interface LifecycleStopOrDisposeEffectResult {
    fun runStopOrDisposeEffect()
}

/**
 * Receiver scope for [LifecycleStartEffect] that offers the [onStopOrDispose] clause to
 * couple the ON_START effect. This should be the last statement in any call to
 * [LifecycleStartEffect].
 *
 * This scope is also a [LifecycleOwner] to allow access to the
 * (lifecycle)[LifecycleStartStopEffectScope.lifecycle] within the [onStopOrDispose] clause.
 *
 * @param lifecycle The lifecycle being observed by this receiver scope
 */
class LifecycleStartStopEffectScope(override val lifecycle: Lifecycle) : LifecycleOwner {
    /**
     * Provide the [onStopOrDisposeEffect] to the [LifecycleStartEffect] to run when the
     * observer receives an (ON_STOP)[Lifecycle.Event.ON_STOP] event or must undergo cleanup.
     */
    inline fun onStopOrDispose(
        crossinline onStopOrDisposeEffect: LifecycleOwner.() -> Unit
    ): LifecycleStopOrDisposeEffectResult = object : LifecycleStopOrDisposeEffectResult {
        override fun runStopOrDisposeEffect() {
            onStopOrDisposeEffect()
        }
    }
}

/**
 * Schedule a pair of effects to run when the [Lifecycle] receives either a
 * [Lifecycle.Event.ON_RESUME] or [Lifecycle.Event.ON_PAUSE]. The ON_RESUME effect
 * will be the body of the [effects] block and the ON_PAUSE effect will be within
 * the (onPauseOrDispose clause)[LifecycleResumePauseEffectScope.onPauseOrDispose]:
 *
 * LifecycleResumeEffect(lifecycleOwner) {
 *     // add ON_RESUME effect here
 *
 *     onPauseOrDispose {
 *         // add clean up for work kicked off in the ON_RESUME effect here
 *     }
 * }
 *
 * @sample androidx.lifecycle.compose.samples.lifecycleResumeEffectSample
 *
 * A [LifecycleResumeEffect] **must** include an
 * [onPauseOrDispose][LifecycleResumePauseEffectScope.onPauseOrDispose] clause as
 * the final statement in its [effects] block. If your operation does not require
 * an effect for _both_ [Lifecycle.Event.ON_RESUME] and [Lifecycle.Event.ON_PAUSE],
 * a [LifecycleEventEffect] should be used instead.
 *
 * This function uses a [LifecycleEventObserver] to listen for when [LifecycleResumeEffect]
 * enters the composition and the effects will be launched when receiving a
 * [Lifecycle.Event.ON_RESUME] or [Lifecycle.Event.ON_PAUSE] event, respectively. If the
 * [LifecycleResumeEffect] leaves the composition prior to receiving an [Lifecycle.Event.ON_PAUSE]
 * event, [onPauseOrDispose][LifecycleResumePauseEffectScope.onPauseOrDispose] will be called
 * to clean up the work that was kicked off in the ON_RESUME effect.
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
    val lifecycleResumePauseEffectScope =
        LifecycleResumePauseEffectScope(lifecycleOwner.lifecycle)

    // Safely update the current `onResume` lambda when a new one is provided
    val currentEffects by rememberUpdatedState(effects)
    DisposableEffect(lifecycleOwner) {
        var effectResult: LifecyclePauseEffectResult? = null
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> with(lifecycleResumePauseEffectScope) {
                        effectResult = currentEffects()
                    }

                Lifecycle.Event.ON_PAUSE -> effectResult?.runPauseOrOnDisposeEffect()

                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            effectResult?.runPauseOrOnDisposeEffect()
        }
    }
}

/**
 * Interface used for [LifecycleResumeEffect] to run the effect within the onPauseOrDispose
 * clause when an (ON_PAUSE)[Lifecycle.Event.ON_PAUSE] event is received or when cleanup is
 *  * needed for the work that was kicked off in the ON_RESUME effect.
 */
interface LifecyclePauseEffectResult {
    fun runPauseOrOnDisposeEffect()
}

/**
 * Receiver scope for [LifecycleResumeEffect] that offers the [onPauseOrDispose] clause to
 * couple the ON_RESUME effect. This should be the last statement in any call to
 * [LifecycleResumeEffect].
 *
 * This scope is also a [LifecycleOwner] to allow access to the
 * (lifecycle)[LifecycleResumePauseEffectScope.lifecycle] within the [onPauseOrDispose] clause.
 *
 * @param lifecycle The lifecycle being observed by this receiver scope
 */
class LifecycleResumePauseEffectScope(override val lifecycle: Lifecycle) : LifecycleOwner {
    /**
     * Provide the [onPauseOrDisposeEffect] to the [LifecycleResumeEffect] to run when the observer
     * receives an (ON_PAUSE)[Lifecycle.Event.ON_PAUSE] event or must undergo cleanup.
     */
    inline fun onPauseOrDispose(
        crossinline onPauseOrDisposeEffect: LifecycleOwner.() -> Unit
    ): LifecyclePauseEffectResult = object : LifecyclePauseEffectResult {
        override fun runPauseOrOnDisposeEffect() {
            onPauseOrDisposeEffect()
        }
    }
}
