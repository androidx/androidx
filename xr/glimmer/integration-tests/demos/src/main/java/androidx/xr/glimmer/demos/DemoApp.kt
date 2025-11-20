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

package androidx.xr.glimmer.demos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.ListItem
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.list.VerticalList

@Composable
fun DemoApp(demoAppState: DemoAppState) {
    val overlayOnBackground = OverlayOnBackgroundSetting.asState().value
    GlimmerTheme {
        Column(
            Modifier.demoBackground(overlayOnBackground)
                .windowInsetsPadding(WindowInsets.systemBars)
        ) {
            NavDisplay(backStack = demoAppState.backstack, onBack = demoAppState::popBackstack) {
                demo ->
                NavEntry(key = demo, contentKey = demo.title) {
                    DisplayDemo(it, demoAppState::navigateToDemo)
                }
            }
        }
    }
}

class DemoAppState(initialBackstack: List<Demo>) {
    private val _backstack = mutableStateListOf<Demo>().apply { addAll(initialBackstack) }

    val backstack: List<Demo>
        get() = _backstack

    fun navigateToDemo(demo: Demo) {
        _backstack.add(demo)
    }

    fun popBackstack() {
        _backstack.removeLastOrNull()
    }

    companion object {
        fun Saver(rootDemo: Demo): Saver<DemoAppState, *> =
            listSaver(
                save = { it._backstack.map(Demo::title) },
                restore = { restored ->
                    require(restored.isNotEmpty()) { "no restored items" }
                    val backStack =
                        restored.mapTo(mutableListOf()) {
                            requireNotNull(findDemo(rootDemo, it)) { "could not find demo" }
                        }
                    DemoAppState(initialBackstack = backStack)
                },
            )

        private fun findDemo(demo: Demo, title: String): Demo? {
            if (demo.title == title) return demo
            if (demo is DemoCategory) {
                demo.demos.forEach { child ->
                    findDemo(child, title)?.let {
                        return it
                    }
                }
            }
            return null
        }
    }
}

@Composable
fun rememberDemoAppState(rootDemo: Demo): DemoAppState =
    rememberSaveable(saver = DemoAppState.Saver(rootDemo)) { DemoAppState(listOf(rootDemo)) }

@Composable
private fun DisplayDemo(demo: Demo, onNavigate: (Demo) -> Unit) {
    when (demo) {
        is ComposableDemo -> demo.content()
        is DemoCategory -> DisplayDemoCategory(demo, onNavigate)
    }
}

@Composable
private fun DisplayDemoCategory(category: DemoCategory, onNavigate: (Demo) -> Unit) {
    VerticalList(
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(category.demos.size) { index ->
            val demo = category.demos[index]
            ListItem(onClick = { onNavigate(demo) }) { Text(demo.title) }
        }
    }
}

@Composable
private fun Modifier.demoBackground(overlayOnBackground: Boolean) =
    this.then(
        if (overlayOnBackground) {
            Modifier.background(
                    brush = Brush.linearGradient(0f to Color(0xFF375672), 1f to Color(0xFF6D6637))
                )
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                    blendMode = BlendMode.Screen
                }
        } else {
            Modifier.background(GlimmerTheme.colors.surface)
        }
    )
