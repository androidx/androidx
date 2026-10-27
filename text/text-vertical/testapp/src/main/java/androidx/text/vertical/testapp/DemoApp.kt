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

package androidx.text.vertical.testapp

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/** The test tag of the app bar title. */
internal const val DEMO_APP_BAR_TITLE_TAG = "demo_app_bar_title"

/**
 * The back stack of [DemoApp].
 *
 * @param initialBackStack the demos from the root category to the demo that shows first
 */
class DemoAppState(initialBackStack: List<Demo>) {
    private val backStack = mutableStateListOf<Demo>().apply { addAll(initialBackStack) }

    /** The demo that shows now. */
    val currentDemo: Demo
        get() = backStack.last()

    /** Whether a parent category sits below [currentDemo]. */
    val canNavigateUp: Boolean
        get() = backStack.size > 1

    /** The app bar title: the root title, or the path below the root joined with " > ". */
    val title: String
        get() =
            if (canNavigateUp) backStack.drop(1).joinToString(" > ") { it.title }
            else currentDemo.title

    /** Pushes [demo] and shows it. */
    fun navigateTo(demo: Demo) {
        backStack.add(demo)
    }

    /** Pops [currentDemo] and shows its parent. Does nothing at the root. */
    fun popBackStack() {
        if (canNavigateUp) backStack.removeAt(backStack.lastIndex)
    }

    companion object {
        /**
         * Saves the back stack as titles and restores it by walking the children of [root].
         *
         * Leaf titles repeat across categories, so the walk starts at [root] and follows one child
         * per saved title.
         */
        fun Saver(root: DemoCategory): Saver<DemoAppState, *> =
            listSaver(
                save = { it.backStack.map(Demo::title) },
                restore = { titles ->
                    val backStack = mutableListOf<Demo>(root)
                    titles.drop(1).fold<String, Demo>(root) { parent, title ->
                        (parent as DemoCategory)
                            .demos
                            .first { it.title == title }
                            .also {
                                backStack.add(it)
                            }
                    }
                    DemoAppState(backStack)
                },
            )
    }
}

/**
 * Remembers a [DemoAppState] that survives rotation.
 *
 * @param root the root category of the demo tree
 * @param initialBackStack the demos from [root] to the demo that shows first. It must start with
 *   [root]. Use [findDemoPath] to build it from a demo name.
 */
@Composable
fun rememberDemoAppState(
    root: DemoCategory,
    initialBackStack: List<Demo> = listOf(root),
): DemoAppState =
    rememberSaveable(saver = DemoAppState.Saver(root)) { DemoAppState(initialBackStack) }

/**
 * Shows a category as a list of demos, or a demo as full screen content.
 *
 * The app bar shows the path to the current demo and a back arrow below the root. The system Back
 * button pops the back stack as well.
 *
 * @param state the back stack to show
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoApp(state: DemoAppState) {
    BackHandler(enabled = state.canNavigateUp) { state.popBackStack() }
    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(state.title, Modifier.testTag(DEMO_APP_BAR_TITLE_TAG)) },
                    navigationIcon = {
                        if (state.canNavigateUp) {
                            IconButton(onClick = state::popBackStack) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_arrow_back),
                                    contentDescription = "Back",
                                )
                            }
                        }
                    },
                )
            }
        ) { innerPadding ->
            Surface(
                modifier =
                    Modifier.fillMaxSize().consumeWindowInsets(innerPadding).padding(innerPadding),
                color = MaterialTheme.colorScheme.background,
            ) {
                when (val demo = state.currentDemo) {
                    is ComposableDemo -> demo.content()
                    is DemoCategory -> DemoList(demo, state::navigateTo)
                }
            }
        }
    }
}

/** Lists the demos of [category]. A tap calls [onNavigate] with the tapped demo. */
@Composable
private fun DemoList(category: DemoCategory, onNavigate: (Demo) -> Unit) {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        category.demos.forEach { demo ->
            Box(
                modifier =
                    Modifier.heightIn(min = 48.dp)
                        .fillMaxWidth()
                        .clickable { onNavigate(demo) }
                        .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(demo.title)
            }
        }
    }
}
