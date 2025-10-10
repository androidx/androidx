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

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import androidx.navigation3.ui.NavDisplay.popTransitionSpec
import kotlin.collections.plus

/** Regular un-animated Dual pane scene */
class TestTwoPaneScene<T : Any>(
    override val key: Any,
    override val entries: List<NavEntry<T>>,
    override val previousEntries: List<NavEntry<T>>,
) : Scene<T> {
    override val content: @Composable (() -> Unit) = {
        val left = entries.first()
        val right = entries.last()
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) { left.Content() }
            Column(Modifier.weight(1f)) { right.Content() }
        }
    }
}

class TestTwoPaneSceneStrategy<T : Any> : SceneStrategy<T> {
    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        if (entries.size < 2) return null
        val lastTwoEntries = entries.takeLast(2)
        return TestTwoPaneScene(
            key = lastTwoEntries.first().contentKey,
            entries = entries.takeLast(2),
            previousEntries = listOf(),
        )
    }
}

/** Animated Dual pane scene for testing transitions */
class TestAnimatedTwoPaneSceneStrategy<T : Any>(
    val durationMillis: Int,
    val overrideEntryAnimations: Boolean = false,
) : SceneStrategy<T> {
    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        if (entries.size < 2) return null
        val lastTwoEntries = entries.takeLast(2)
        return TestAnimatedTwoPaneScene(
            durationMillis = durationMillis,
            overrideEntryAnimations = overrideEntryAnimations,
            key = lastTwoEntries.first().contentKey,
            entries = entries.takeLast(2),
            previousEntries = listOf(),
        )
    }
}

class TestAnimatedTwoPaneScene<T : Any>(
    val durationMillis: Int,
    val overrideEntryAnimations: Boolean,
    override val key: Any,
    override val entries: List<NavEntry<T>>,
    override val previousEntries: List<NavEntry<T>>,
) : Scene<T> {
    override val content: @Composable (() -> Unit) = {
        val left = entries.first()
        val right = entries.last()
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) { left.Content() }
            Column(Modifier.weight(1f)) { right.Content() }
        }
    }

    override val metadata: Map<String, Any>
        get() {
            // define scene metadata transitions
            val sceneTransition =
                slideInHorizontally(tween(durationMillis)) { it / 2 } togetherWith
                    slideOutHorizontally { -it / 2 }
            // build scene metadata map
            val newMetadata =
                NavDisplay.transitionSpec({ sceneTransition }) +
                    popTransitionSpec({ sceneTransition })
            // override NavEntry transitions if necessary
            return if (overrideEntryAnimations) {
                entries.last().metadata + newMetadata
            } else newMetadata + entries.last().metadata
        }
}
