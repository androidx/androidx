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

package androidx.appstate.dataflow.ui

import androidx.appstate.dataflow.DataKey
import androidx.appstate.dataflow.DataflowRegistry
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember

/**
 * Subscribes to data of type [S] for [key] and provides it to [content].
 *
 * Recomposes [content] whenever the reducer for [key] in this [DataflowRegistry] produces new data.
 *
 * Automatically disposes the subscription when this composable leaves the composition.
 *
 * [S] - the type of data produced for [key]
 *
 * @param key [DataKey] identifying the data in this [DataflowRegistry]
 * @param content composable called with the latest [S] data
 */
@Composable
public fun <S : Any> DataflowRegistry.UiContent(
    key: DataKey<S>,
    content: @Composable (data: S) -> Unit,
) {
    val disposableState = remember(key, this) { getState(key) }
    DisposableEffect(disposableState) {
        onDispose {
            disposableState.dispose()
        }
    }
    content(disposableState.value)
}
