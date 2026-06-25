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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope

/**
 * Suspend function that executes a Composable [onUpdate] block in a headless Composition.
 *
 * This function bridges reactive Compose state (or Flows collected as state) into a continuously
 * updated [State] object outside of a traditional Compose UI hierarchy. The [onUpdate] block is
 * automatically recomposed whenever any Compose state it reads is updated.
 *
 * @param dispatcher The [CoroutineDispatcher] thread this listener should run on.
 * @param onUpdate The [Composable] block that will be recomposed when its state dependencies
 *   change.
 */
public suspend fun listener(
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    onUpdate: @Composable () -> Unit,
) {
    coroutineScope {
        transform(defaultValue = Unit, scope = this, dispatcher = dispatcher, onUpdate)
    }
}
