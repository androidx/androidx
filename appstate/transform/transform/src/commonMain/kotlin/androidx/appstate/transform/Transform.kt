/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.appstate.transform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * This is a convenience function for [TransformScope.launchTransform] that runs the transform
 * within the given [CoroutineScope].
 *
 * This function bridges reactive Compose state (or Flows collected as state) into a continuously
 * updated [State] object outside a traditional Compose UI hierarchy. The [transform] block is
 * automatically recomposed whenever any Compose state it reads is updated, and the newly returned
 * value is pushed into the resulting [State].
 *
 * @param initialValue The initial value to populate the returned [State] before the first
 *   composition completes.
 * @param scope The [CoroutineScope] in which the recomposer and frame clock will run.
 * @param dispatcher The [CoroutineDispatcher] thread this transform should run on.
 * @param onUpdate The [Composable] block that computes the value of type [R].
 * @return A [State] containing the latest result of the [transform] block.
 */
public fun <R> transform(
    initialValue: R,
    scope: CoroutineScope,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    onUpdate: @Composable (R) -> R,
): State<R> {
    val transformScope = TransformScope() as TransformScopeImpl
    val transformation = transformScope.launchTransform(initialValue, onUpdate)
    scope.launch(dispatcher, start = CoroutineStart.UNDISPATCHED) {
        try {
            transformScope.run()
        } finally {
            transformation.dispose()
        }
    }
    return transformation
}

/**
 * A Composable [transform] that remembers the resulting [State] and ties the headless composition
 * to the current [CoroutineScope] provided by the Compose lifecycle.
 *
 * @param initialValue The initial value to populate the returned [State] before the first
 *   composition completes.
 * @param dispatcher The [CoroutineDispatcher] thread this transform should run on.
 * @param onUpdate The [Composable] block that computes the value of type [R].
 * @return A [State] containing the latest result of the [transform] block.
 */
@Composable
public fun <R> transform(
    initialValue: R,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    onUpdate: @Composable (R) -> R,
): State<R> {
    val scope = rememberCoroutineScope()
    return remember(scope) { transform(initialValue, scope, dispatcher, onUpdate) }
}
