/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.navigationevent.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventState
import kotlinx.coroutines.CoroutineScope

/**
 * Remembers and returns the current [NavigationEventState] for a specific [NavigationEventInfo]
 * type.
 *
 * This is a convenience wrapper around [NavigationEventDispatcher.getState]. It:
 * - Reads the dispatcher from [LocalNavigationEventDispatcherOwner].
 * - Creates a [CoroutineScope] via [rememberCoroutineScope].
 * - Subscribes to the dispatcher state for `T` and exposes it as Compose state via
 *   [collectAsState].
 *
 * Use this when a composable needs to observe navigation events of a single info type `T` (for
 * example, to react to a predictive back gesture). The returned value participates in
 * recomposition: the composable will recompose whenever the underlying state for `T` changes.
 *
 * The initial value is `Idle(initialInfo)`, and filtering is type-based (only states whose
 * `currentInfo` is of type `T` are observed).
 *
 * @param T the [NavigationEventInfo] subtype to observe.
 * @param initialInfo the initial `T` used to seed the observation (provides the initial `Idle`
 *   value).
 * @return the current [NavigationEventState] for `T`.
 * @throws IllegalStateException if no [NavigationEventDispatcherOwner] is provided via
 *   [LocalNavigationEventDispatcherOwner].
 * @see NavigationEventDispatcher.getState
 * @see LocalNavigationEventDispatcherOwner
 */
@Composable
public inline fun <reified T : NavigationEventInfo> rememberNavigationEventState(
    initialInfo: T
): NavigationEventState<T> {
    val dispatcher =
        checkNotNull(LocalNavigationEventDispatcherOwner.current) {
                "No NavigationEventDispatcher was provided via LocalNavigationEventDispatcherOwner"
            }
            .navigationEventDispatcher

    val scope = rememberCoroutineScope()
    val state by remember { dispatcher.getState(scope, initialInfo) }.collectAsState()
    return state
}
