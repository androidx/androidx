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

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

/**
 * Executes a Composable [transform] block in a headless Composition and returns its result as a
 * [State].
 *
 * This function bridges reactive Compose state (or Flows collected as state) into a continuously
 * updated [State] object outside a traditional Compose UI hierarchy. The [transform] block is
 * automatically recomposed whenever any Compose state it reads is updated, and the newly returned
 * value is pushed into the resulting [State].
 *
 * @param context The [CoroutineContext] to use for the composition's recomposer.
 * @param scope The [CoroutineScope] in which the recomposer and frame clock will run.
 * @param defaultValue The initial value to populate the returned [State] before the first
 *   composition completes.
 * @param onUpdate The [Composable] block that computes the value of type [R].
 * @return A [State] containing the latest result of the [transform] block.
 */
public fun <R> transform(
    defaultValue: R,
    context: CoroutineContext,
    scope: CoroutineScope = CoroutineScope(context),
    onUpdate: @Composable () -> R,
): State<R> {
    GlobalSnapshotManager.ensureStarted()
    // TODO: Figure out the appropriate frame clock
    val clockContext = GatedFrameClock(scope, context)
    val finalContext = context + clockContext

    // TODO: Determine whether a single recomposer is the correct approach
    val recomposer = Recomposer(finalContext)
    val composition = Composition(UnitApplier, recomposer)
    scope.launch(finalContext, start = CoroutineStart.UNDISPATCHED) {
        try {
            recomposer.runRecomposeAndApplyChanges()
        } finally {
            composition.dispose()
        }
    }

    val state = mutableStateOf(defaultValue)

    composition.setContent { state.value = onUpdate() }

    return state
}

/**
 * A Composable [transform] that remembers the resulting [State] and ties the headless composition
 * to the current [CoroutineScope] provided by the Compose lifecycle.
 *
 * @param defaultValue The initial value to populate the returned [State] before the first
 *   composition completes.
 * @param onUpdate The [Composable] block that computes the value of type [R].
 * @return A [State] containing the latest result of the [transform] block.
 */
@Composable
public fun <R> transform(defaultValue: R, onUpdate: @Composable () -> R): State<R> {
    val scope = rememberCoroutineScope()
    return remember(scope) {
        transform(
            defaultValue = defaultValue,
            context = scope.coroutineContext,
            scope = scope,
            onUpdate = onUpdate,
        )
    }
}

private object UnitApplier : AbstractApplier<Unit>(Unit) {
    override fun insertBottomUp(index: Int, instance: Unit) {}

    override fun insertTopDown(index: Int, instance: Unit) {}

    override fun move(from: Int, to: Int, count: Int) {}

    override fun remove(index: Int, count: Int) {}

    override fun onClear() {}
}
