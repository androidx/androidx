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

package androidx.navigation3.ui.samples

import androidx.annotation.Sampled
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.NavDisplay.popTransitionSpec
import androidx.navigation3.ui.NavDisplay.predictivePopTransitionSpec
import kotlinx.serialization.Serializable

@Serializable private object A : NavKey

@Serializable private object B : NavKey

@Serializable private object C : NavKey

@Sampled
@Composable
fun SceneDefaultTransitionsSample() {
    val backStack = rememberNavBackStack(A)
    NavDisplay(
        backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
        // NavDisplay default transitions slide vertically
        transitionSpec = { slideVertical },
        popTransitionSpec = { slideVertical },
        predictivePopTransitionSpec = { slideVertical },
        // but the Scene provides default transitions that slide horizontally
        sceneStrategy = DefaultSceneTransitionsSceneStrategy(),
        entryProvider =
            entryProvider {
                entry<A> { BlueBox("A") { backStack.add(B) } }
                entry<B> { RedBox("B") }
            },
    )
}

private class DefaultSceneTransitionsSceneStrategy<T : Any>() : SceneStrategy<T> {
    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        if (entries.isEmpty()) return null
        return DefaultSceneTransitionsScene(entries.last(), entries.dropLast(1))
    }
}

private class DefaultSceneTransitionsScene<T : Any>(
    val currentEntry: NavEntry<T>,
    override val previousEntries: List<NavEntry<T>>,
) : Scene<T> {
    override val key = currentEntry.contentKey
    override val entries: List<NavEntry<T>> = listOf(currentEntry)
    override val content: @Composable (() -> Unit) = { currentEntry.Content() }
    // overrides the default animations passed to NavDisplay
    override val metadata: Map<String, Any> = sceneTransitions
}

@Sampled
@Composable
fun SceneOverrideEntryTransitionsSample() {
    val backStack = rememberNavBackStack(A)
    NavDisplay(
        backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
        // the Scene overrides the NavEntry's slide vertical with slide horizontal transitions
        sceneStrategy = SceneOverrideEntryTransitionsSceneStrategy(),
        entryProvider =
            entryProvider {
                entry<A> { BlueBox("A") { backStack.add(B) } }
                // the entry defines slide vertical transitions
                entry<B>(
                    metadata =
                        NavDisplay.transitionSpec({ slideVertical }) +
                            popTransitionSpec({ slideVertical }) +
                            predictivePopTransitionSpec({ slideVertical })
                ) {
                    RedBox("B")
                }
            },
    )
}

private class SceneOverrideEntryTransitionsSceneStrategy<T : Any>() : SceneStrategy<T> {
    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        if (entries.isEmpty()) return null
        return SceneOverrideEntryTransitionsScene(entries.last(), entries.dropLast(1))
    }
}

private class SceneOverrideEntryTransitionsScene<T : Any>(
    val currentEntry: NavEntry<T>,
    override val previousEntries: List<NavEntry<T>>,
) : Scene<T> {
    override val key = currentEntry.contentKey
    override val entries: List<NavEntry<T>> = listOf(currentEntry)
    override val content: @Composable (() -> Unit) = { currentEntry.Content() }
    // super.metadata defaults to [entries.last()]'s metadata
    // so this overrides the last entry's transitions
    override val metadata: Map<String, Any> = super.metadata + sceneTransitions
}

private val sceneTransitions =
    NavDisplay.transitionSpec({ slideHorizontal }) +
        popTransitionSpec({ slideHorizontal }) +
        predictivePopTransitionSpec({ slideHorizontal })

private val duration = 5000
private val animSpec: FiniteAnimationSpec<IntOffset> =
    tween(duration, easing = LinearOutSlowInEasing)
private val slideHorizontal =
    slideInHorizontally(animSpec) { it / 2 } togetherWith slideOutHorizontally(animSpec) { -it / 2 }
private val slideVertical =
    slideInVertically(animSpec) { it / 2 } togetherWith slideOutVertically(animSpec) { -it / 2 }

@Composable
fun BlueBox(text: String, onClick: (() -> Unit)?) {
    Box(
        Modifier.fillMaxSize().background(Color(0.2f, 0.2f, 1.0f, 1.0f)).border(10.dp, Color.Blue),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BasicText(text, Modifier.size(50.dp), style = TextStyle(textAlign = TextAlign.Center))
            if (onClick != null) {
                Button(onClick = onClick) { Text("Click to navigate") }
            }
        }
    }
}

@Composable
fun RedBox(text: String) {
    Box(
        Modifier.fillMaxSize().background(Color(1.0f, 0.3f, 0.3f, 1.0f)).border(10.dp, Color.Red),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text, Modifier.size(50.dp), style = TextStyle(textAlign = TextAlign.Center))
    }
}
