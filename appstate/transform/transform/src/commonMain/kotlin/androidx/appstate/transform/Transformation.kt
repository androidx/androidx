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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshots.Snapshot

@Stable
internal class Transformation<T>(
    initialValue: T,
    private val onUpdate: @Composable (T) -> T,
) {
    private var scope: TransformScopeImpl? = null

    internal val state =
        TransformState(initialValue) {
            // this is when this particular composition is disposed
            scope?.remove(this)
            scope = null
        }

    @Composable
    operator fun invoke() {
        state.value = onUpdate(Snapshot.withoutReadObservation { state.value })
    }

    internal fun installIn(transformScope: TransformScopeImpl) {
        check(scope == null) { "Attempted to install transformation twice" }
        scope = transformScope
    }
}
