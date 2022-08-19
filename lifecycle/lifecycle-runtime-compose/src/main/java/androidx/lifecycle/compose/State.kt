/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.withContext

/**
 * Represents its latest value via [State] in a lifecycle-aware manner.
 *
 * Warning: [Lifecycle.State.INITIALIZED] is not allowed in this API. Passing it as a
 * parameter will throw an [IllegalArgumentException].
 *
 * @sample androidx.lifecycle.compose.samples.UpdatedStateWithLifecycle
 *
 * @param initialValue The initial value given to the returned [State.value].
 * @param lifecycleOwner [LifecycleOwner] whose `lifecycle` is used to re-updating this [State].
 * @param minActiveState [Lifecycle.State] in which the value gets updated. The update will stop
 * if the lifecycle falls below that state, and will restart if it's in that state again.
 * @param context [CoroutineContext] to use for updating value.
 * @param updater Runs block to produce new value to [State].
 */
@ExperimentalLifecycleComposeApi
@Composable
fun <T> rememberUpdatedStateWithLifecycle(
    initialValue: T,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    context: CoroutineContext = EmptyCoroutineContext,
    updater: () -> T,
): State<T> = rememberUpdatedStateWithLifecycle(
    initialValue = initialValue,
    lifecycle = lifecycleOwner.lifecycle,
    minActiveState = minActiveState,
    context = context,
    updater = updater,
)

/**
 * Represents its latest value via [State] in a lifecycle-aware manner.
 *
 * Warning: [Lifecycle.State.INITIALIZED] is not allowed in this API. Passing it as a
 * parameter will throw an [IllegalArgumentException].
 *
 * @sample androidx.lifecycle.compose.samples.UpdatedStateWithLifecycle
 *
 * @param initialValue The initial value given to the returned [State.value].
 * @param lifecycle [Lifecycle] is used to re-updating this [State].
 * @param minActiveState [Lifecycle.State] in which the value gets updated. The update will stop
 * if the lifecycle falls below that state, and will restart if it's in that state again.
 * @param context [CoroutineContext] to use for updating value.
 * @param updater Runs block to produce new value to [State].
 */
@ExperimentalLifecycleComposeApi
@Composable
fun <T> rememberUpdatedStateWithLifecycle(
    initialValue: T,
    lifecycle: Lifecycle,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    context: CoroutineContext = EmptyCoroutineContext,
    updater: () -> T,
): State<T> = produceState(initialValue, lifecycle, minActiveState, context, updater) {
    lifecycle.repeatOnLifecycle(minActiveState) {
        if (context == EmptyCoroutineContext) {
            this@produceState.value = updater()
        } else withContext(context) {
            this@produceState.value = updater()
        }
    }
}
