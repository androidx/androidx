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

package androidx.navigation3.scene

import androidx.compose.runtime.Composable
import androidx.compose.ui.util.fastMap
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.NavigationEventHandler
import androidx.navigationevent.compose.NavigationEventState
import androidx.navigationevent.compose.rememberNavigationEventState

/**
 * Remembers and returns a [NavigationEventState] instance for a [SceneState].
 *
 * This composable creates and remembers a [NavigationEventState] object, which holds a
 * [NavigationEventHandler] internally. This is the state object that can be passed to
 * [NavigationBackHandler] (the composable) to "hoist" the state.
 *
 * The state's handler info (currentInfo and backInfo) is kept in sync with the provided
 * [sceneState].
 *
 * @param T the type of the key in the [SceneState].
 * @param sceneState the [SceneState] that this state will track.
 * @return a stable, remembered [NavigationEventState] instance.
 */
@Composable
public fun <T : Any> rememberNavigationEventState(
    sceneState: SceneState<T>
): NavigationEventState<SceneInfo<T>> {
    return rememberNavigationEventState(
        currentInfo = SceneInfo(sceneState.currentScene),
        backInfo = sceneState.previousScenes.fastMap { SceneInfo(it) },
    )
}
