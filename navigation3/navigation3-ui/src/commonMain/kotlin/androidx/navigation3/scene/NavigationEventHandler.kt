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
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.NavigationEventState

/**
 * A composable that handles back navigation gestures for a [SceneState], driven by a
 * [NavigationEventState].
 *
 * This is a convenience wrapper around the core
 * [androidx.navigationevent.compose.NavigationBackHandler] that automatically handles the
 * [NavigationEventState] based on the provided [sceneState].
 *
 * @param sceneState the [SceneState] that this handler is associated with.
 * @param state the hoisted [NavigationEventState] (returned from [rememberNavigationEventState]) to
 *   be registered.
 * @param onBack called when a back navigation gesture completes and navigation occurs.
 */
@Composable
public fun <T : Any> NavigationBackHandler(
    sceneState: SceneState<T>,
    state: NavigationEventState<SceneInfo<T>> = rememberNavigationEventState(sceneState),
    onBack: () -> Unit,
) {
    NavigationBackHandler(
        state = state,
        isBackEnabled = sceneState.currentScene.previousEntries.isNotEmpty(),
        onBackCompleted = {
            // If 'enabled' becomes stale (e.g., it was set to false but a gesture was
            // dispatched in the same frame), this may result in no entries being popped
            // due to 'entries.size' being smaller than 'scene.previousEntries.size'
            // but that's preferable to crashing with an 'IndexOutOfBoundsException'
            repeat(sceneState.entries.size - sceneState.currentScene.previousEntries.size) {
                onBack()
            }
        },
    )
}
