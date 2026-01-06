/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.compose.lifecycle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.DisposableEffectResult
import androidx.compose.runtime.DisposableEffectScope
import androidx.compose.runtime.NonRestartableComposable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.CoroutineScope

// This deprecated-error function shadows the varargs overload so that the varargs version
// is not used without key parameters.
@Deprecated(DisposableEffectNoParamError, level = DeprecationLevel.ERROR)
@Composable
fun DisposableEffectWithLifecycle(block: suspend CoroutineScope.() -> Unit) {
    error(DisposableEffectNoParamError)
}

private const val DisposableEffectNoParamError =
    "DisposableEffectWithLifecycle must provide one or more 'key' parameters that define the " +
        "identity of the DisposableEffect and determine when its previous effect coroutine should" +
        " be cancelled and a new effect Disposable for the new key."

/**
 * A [DisposableEffect] that is lifecycle-aware.
 *
 * This effect is triggered every time the [lifecycle] reaches the [minActiveState], and will be
 * **re-launched** whenever [DisposableEffectWithLifecycle] is recomposed with a different [key1],
 * [lifecycle] or [minActiveState].
 *
 * @see androidx.compose.runtime.DisposableEffect
 */
@Composable
@NonRestartableComposable
fun DisposableEffectWithLifecycle(
    key1: Any?,
    lifecycle: Lifecycle = LocalLifecycleOwner.current.lifecycle,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    effect: DisposableEffectScope.() -> DisposableEffectResult,
) {
    DisposableEffect(key1, lifecycle, minActiveState) {
        disposableEffectWithLifecycle(lifecycle, minActiveState, effect)
    }
}

/**
 * A [DisposableEffect] that is lifecycle-aware.
 *
 * This effect is triggered every time the [lifecycle] reaches the [minActiveState], and will be
 * **re-launched** whenever [DisposableEffectWithLifecycle] is recomposed with a different [key1],
 * [key2], [lifecycle] or [minActiveState].
 *
 * @see androidx.compose.runtime.DisposableEffect
 */
@Composable
@NonRestartableComposable
fun DisposableEffectWithLifecycle(
    key1: Any?,
    key2: Any?,
    lifecycle: Lifecycle = LocalLifecycleOwner.current.lifecycle,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    effect: DisposableEffectScope.() -> DisposableEffectResult,
) {
    DisposableEffect(key1, key2, lifecycle, minActiveState) {
        disposableEffectWithLifecycle(lifecycle, minActiveState, effect)
    }
}

/**
 * A [DisposableEffect] that is lifecycle-aware.
 *
 * This effect is triggered every time the [lifecycle] reaches the [minActiveState], and will be
 * **re-launched** whenever [DisposableEffectWithLifecycle] is recomposed with a different [key1],
 * [key2], [lifecycle] or [minActiveState].
 *
 * @see androidx.compose.runtime.DisposableEffect
 */
@Composable
@NonRestartableComposable
fun DisposableEffectWithLifecycle(
    key1: Any?,
    key2: Any?,
    key3: Any?,
    lifecycle: Lifecycle = LocalLifecycleOwner.current.lifecycle,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    effect: DisposableEffectScope.() -> DisposableEffectResult,
) {
    DisposableEffect(key1, key2, key3, lifecycle, minActiveState) {
        disposableEffectWithLifecycle(lifecycle, minActiveState, effect)
    }
}

/**
 * A [DisposableEffect] that is lifecycle-aware.
 *
 * This effect is triggered every time the [lifecycle] reaches the [minActiveState], and will be
 * **re-launched** whenever [DisposableEffectWithLifecycle] is recomposed with a different [key1],
 * [key2], [key3], [lifecycle] or [minActiveState].
 *
 * @see androidx.compose.runtime.DisposableEffect
 */
@Composable
@NonRestartableComposable
fun DisposableEffectWithLifecycle(
    vararg keys: Any?,
    lifecycle: Lifecycle = LocalLifecycleOwner.current.lifecycle,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    effect: DisposableEffectScope.() -> DisposableEffectResult,
) {
    DisposableEffect(keys, lifecycle, minActiveState) {
        disposableEffectWithLifecycle(lifecycle, minActiveState, effect)
    }
}

private fun DisposableEffectScope.disposableEffectWithLifecycle(
    lifecycle: Lifecycle,
    minActiveState: Lifecycle.State,
    effect: DisposableEffectScope.() -> DisposableEffectResult,
): DisposableEffectResult {
    var effectResult: DisposableEffectResult? = null

    fun maybeLaunch() {
        if (effectResult != null) return
        effectResult = effect()
    }

    fun maybeDispose() {
        effectResult?.dispose()
        effectResult = null
    }

    fun update() {
        if (lifecycle.currentState.isAtLeast(minActiveState)) {
            maybeLaunch()
        } else {
            maybeDispose()
        }
    }

    val observer = LifecycleEventObserver { _, _ -> update() }
    lifecycle.addObserver(observer)
    update()

    return onDispose {
        lifecycle.removeObserver(observer)
        maybeDispose()
    }
}
