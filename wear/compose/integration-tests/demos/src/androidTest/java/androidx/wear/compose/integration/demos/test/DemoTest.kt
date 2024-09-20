/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.wear.compose.integration.demos.test

import android.util.Log
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.wear.compose.integration.demos.DemoActivity
import androidx.wear.compose.integration.demos.WearComposeDemos
import androidx.wear.compose.integration.demos.common.Demo
import androidx.wear.compose.integration.demos.common.DemoCategory
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private val ignoredDemos =
    listOf<String>(
        // Not ignoring any of them \o/
    )

// Run this test on a phone emulator.
// There are issues running on Watch emulators that menu items off screen are not found,
// given the use of ScalingLAZYColumn.
@LargeTest
@RunWith(AndroidJUnit4::class)
class DemoTest {
    // We need to provide the recompose factory first to use new clock.
    @get:Rule val rule = createAndroidComposeRule<DemoActivity>()

    @Test
    fun demoApp_builds() {
        // This test just checks that the demo app builds without crashing and the root screen can
        // be visited.
        val title = AllButIgnoredDemos.demos.first().title
        rule.onNode(hasText(title)).assertExists()
    }

    @Ignore // b/367234726
    @Test
    fun navigateThroughAllDemos() {
        // Compose integration-tests are split into batches due to size,
        // but that's overkill until we have a decent population of tests.
        navigateThroughAllDemos(AllButIgnoredDemos)
    }

    private fun navigateThroughAllDemos(root: DemoCategory, fastForwardClock: Boolean = false) {
        // Keep track of each demo we visit.
        val visitedDemos = mutableListOf<Demo>()

        // Visit all demos.
        root.visitDemos(
            visitedDemos = visitedDemos,
            path = listOf(root),
            fastForwardClock = fastForwardClock
        )

        // Ensure that we visited all the demos we expected to, in the order we expected to.
        assertThat(visitedDemos).isEqualTo(root.allDemos())
    }

    /**
     * DFS traversal of each demo in a [DemoCategory] using [Demo.visit]
     *
     * @param path The path of categories that leads to this demo
     */
    private fun DemoCategory.visitDemos(
        visitedDemos: MutableList<Demo>,
        path: List<DemoCategory>,
        fastForwardClock: Boolean
    ) {
        demos.forEach { demo ->
            visitedDemos.add(demo)
            demo.visit(visitedDemos, path, fastForwardClock)
        }
    }

    /**
     * Visits a [Demo], and then navigates back up to the [DemoCategory] it was inside.
     *
     * If this [Demo] is a [DemoCategory], this will visit sub-[Demo]s first before continuing in
     * the current category.
     *
     * @param path The path of categories that leads to this demo
     */
    private fun Demo.visit(
        visitedDemos: MutableList<Demo>,
        path: List<DemoCategory>,
        fastForwardClock: Boolean
    ) {
        Log.d("TEST", "Visit ${this.navigationTitle(path)}")
        if (fastForwardClock) {
            // Skip through the enter animation of the list screen
            fastForwardClock()
        }

        rule
            .onNode(hasScrollToNodeAction())
            .performScrollToNode(hasText(title) and hasClickAction())
        rule.onNode(hasText(title) and hasClickAction()).performClick()

        if (this is DemoCategory) {
            visitDemos(visitedDemos, path + this, fastForwardClock)
        }

        if (fastForwardClock) {
            // Skip through the enter animation of the visited demo
            fastForwardClock()
        }

        rule.waitForIdle()
        while (rule.onAllNodes(isDialog()).isNotEmpty()) {
            Espresso.pressBack()
            rule.waitForIdle()
        }

        clearFocusFromDemo()
        rule.waitForIdle()

        Espresso.pressBack()
        rule.waitForIdle()

        if (fastForwardClock) {
            // Pump press back
            fastForwardClock(2000)
        }
    }

    private fun fastForwardClock(millis: Long = 5000) {
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(millis)
    }

    private fun SemanticsNodeInteractionCollection.isNotEmpty(): Boolean {
        return fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
    }

    private fun clearFocusFromDemo() {
        with(rule.activity) {
            rule.runOnUiThread {
                if (hostView.hasFocus()) {
                    if (hostView.isFocused) {
                        // One of the Compose components has focus.
                        focusManager.clearFocus(force = true)
                    } else {
                        // A child view has focus. (View interop scenario).
                        // We could also use hostViewGroup.focusedChild?.clearFocus(), but the
                        // interop views might end up being focused if one of them is marked as
                        // focusedByDefault. So we clear focus by requesting focus on the owner.
                        hostView.requestFocus()
                    }
                }
            }
        }
    }
}

private val AllButIgnoredDemos =
    WearComposeDemos.filter { path, demo -> demo.navigationTitle(path) !in ignoredDemos }

private fun Demo.navigationTitle(path: List<DemoCategory>): String {
    return path.plus(this).navigationTitle
}

private val List<Demo>.navigationTitle: String
    get() = if (size == 1) first().title else drop(1).joinToString(" > ")

/**
 * Trims the tree of [Demo]s represented by this [DemoCategory] by cutting all leave demos for which
 * the [predicate] returns `false` and recursively removing all empty categories as a result.
 */
private fun DemoCategory.filter(
    path: List<DemoCategory> = emptyList(),
    predicate: (path: List<DemoCategory>, demo: Demo) -> Boolean
): DemoCategory {
    val newPath = path + this
    return DemoCategory(
        title,
        demos.mapNotNull { demo ->
            when (demo) {
                is DemoCategory -> {
                    demo.filter(newPath, predicate).let { if (it.demos.isEmpty()) null else it }
                }
                else -> {
                    if (predicate(newPath, demo)) demo else null
                }
            }
        }
    )
}

/** Flattened recursive DFS [List] of every demo in [this]. */
fun DemoCategory.allDemos(): List<Demo> {
    val allDemos = mutableListOf<Demo>()
    fun DemoCategory.addAllDemos() {
        demos.forEach { demo ->
            allDemos += demo
            if (demo is DemoCategory) {
                demo.addAllDemos()
            }
        }
    }
    addAllDemos()
    return allDemos
}
