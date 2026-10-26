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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.snapshots.Snapshot
import androidx.savedstate.SavedStateContainer

@Stable
internal class Transformation<T>(
    initialValue: T,
    container: SavedStateContainer,
    private val onUpdate: @Composable (T) -> T,
) {
    private var scope: RunnableTransformScope? = null
    private val saveableStateRegistry = SaveableStateRegistry(container) { true }

    internal val key: Any =
        (saveableStateRegistry.consumeRestored(TRANSFORM_KEY) as? Int) ?: hashCode()

    private val keyEntry = saveableStateRegistry.registerProvider(TRANSFORM_KEY) { key }

    internal val state =
        TransformState(initialValue) {
            // this is when this particular composition is disposed
            keyEntry.unregister()
            scope?.remove(this)
            scope = null
        }

    @Composable
    operator fun invoke() {
        CompositionLocalProvider(LocalSaveableStateRegistry provides saveableStateRegistry) {
            state.value = onUpdate(Snapshot.withoutReadObservation { state.value })
        }
    }

    internal fun installIn(transformScope: RunnableTransformScope) {
        check(scope == null) { "Attempted to install transformation twice" }
        scope = transformScope
    }
}

private const val TRANSFORM_KEY = "androidx.appstate.transform_TRANSFORMATION_KEY"
