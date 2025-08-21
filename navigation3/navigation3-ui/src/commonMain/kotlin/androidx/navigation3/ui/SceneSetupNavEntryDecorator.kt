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

package androidx.navigation3.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.key
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.navEntryDecorator

/** Returns a [SceneSetupNavEntryDecorator] that is remembered across recompositions. */
@Composable
public fun rememberSceneSetupNavEntryDecorator(): NavEntryDecorator<Any> = remember {
    SceneSetupNavEntryDecorator()
}

/**
 * A [NavEntryDecorator] that wraps each entry in a [movableContentOf] to allow nav displays to
 * arbitrarily place entries in different places in the composable call hierarchy and ensures that
 * the same entry content is not composed multiple times in different places of the hierarchy by
 * different scenes.
 *
 * This should likely be the first [NavEntryDecorator] to ensure that other [NavEntryDecorator]
 * calls that are stateful are moved properly inside the [movableContentOf].
 */
public fun SceneSetupNavEntryDecorator(): NavEntryDecorator<Any> {
    val movableContentMap: MutableMap<Any, @Composable (@Composable () -> Unit) -> Unit> =
        mutableStateMapOf()
    return navEntryDecorator(onPop = { contentKey -> movableContentMap.remove(contentKey) }) { entry
        ->
        val contentKey = entry.contentKey
        // If we should not be rendering this entry here in the current scene, we skip calling
        // entry.Content and all nested content wrappers. If this is the case here, then it means
        // that this entry is being rendered by a different scene somewhere else.
        if (LocalEntriesToRenderInCurrentScene.current.contains(contentKey)) {
            key(contentKey) {
                // In case the key is removed from the backstack while this is still
                // being rendered, we remember the movableContent directly to allow
                // rendering it while we are animating out.
                val movableContent = remember {
                    // Get or put a movableContentOf for this content key
                    // This represents a "slot" that can be moved around, specifically to be
                    // rendered in a different place in the UI callstack hierarchy while
                    // maintaining all internal state
                    movableContentMap.getOrPut(contentKey) {
                        // We don't capture entry.Content() here, as that could result in a stale
                        // entry.Content() call as we want to create a movableContentOf only
                        // once for each entry. Instead, we pass through the entry's content as
                        // a composable here to be invoked below
                        movableContentOf { content -> content() }
                    }
                }

                // Finally, render the entry content via the movableContentOf
                movableContent { entry.Content() }
            }
        }
    }
}

/**
 * The entry keys to render in the current [Scene], in the sense of the target of the animation for
 * an [androidx.compose.animation.AnimatedContent] that is transitioning between different scenes.
 */
public val LocalEntriesToRenderInCurrentScene: ProvidableCompositionLocal<Set<Any>> =
    compositionLocalOf {
        throw IllegalStateException(
            "Unexpected access to LocalEntriesToRenderInCurrentScene. You should only " +
                "access LocalEntriesToRenderInCurrentScene inside a NavEntry passed " +
                "to NavDisplay."
        )
    }
