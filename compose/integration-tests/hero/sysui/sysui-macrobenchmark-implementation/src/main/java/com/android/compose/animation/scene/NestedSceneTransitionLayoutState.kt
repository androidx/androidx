/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.compose.animation.scene

import androidx.compose.ui.util.fastForEach

/**
 * An implementation of [SceneTransitionLayoutState] for nested STLs that takes ancestors into
 * account in its state check functions.
 */
internal class NestedSceneTransitionLayoutState(
    internal val ancestors: List<SceneTransitionLayoutState>,
    internal val delegate: SceneTransitionLayoutState,
) : SceneTransitionLayoutState by delegate {
    init {
        check(ancestors.isNotEmpty()) {
            "NestedSceneTransitionLayoutState should not be used for non nested STLs"
        }
    }

    override fun isIdle(content: ContentKey?): Boolean {
        var foundContent = content == null
        forEachState { state ->
            if (!state.isIdle()) return false
            foundContent = foundContent || state.isIdle(content)
        }
        return foundContent
    }

    override fun isTransitioning(from: ContentKey?, to: ContentKey?): Boolean {
        forEachState { state -> if (state.isTransitioning(from, to)) return true }
        return false
    }

    override fun isTransitioningBetween(content: ContentKey, other: ContentKey): Boolean {
        forEachState { state -> if (state.isTransitioningBetween(content, other)) return true }
        return false
    }

    override fun isTransitioningFromOrTo(content: ContentKey): Boolean {
        forEachState { state -> if (state.isTransitioningFromOrTo(content)) return true }
        return false
    }

    override fun isCurrentScene(content: SceneKey): Boolean {
        forEachState { state -> if (state.isCurrentScene(content)) return true }
        return false
    }

    override fun isInCurrentOverlays(content: OverlayKey): Boolean {
        forEachState { state -> if (state.isInCurrentOverlays(content)) return true }
        return false
    }

    private inline fun forEachState(action: (SceneTransitionLayoutState) -> Unit) {
        action(delegate)
        ancestors.fastForEach { action(it) }
    }
}
