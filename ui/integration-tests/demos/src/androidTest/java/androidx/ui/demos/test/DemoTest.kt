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

package androidx.ui.demos.test

import androidx.test.espresso.Espresso
import androidx.test.filters.LargeTest
import androidx.ui.demos.AllDemosCategory
import androidx.ui.demos.DemoActivity
import androidx.ui.demos.Tags
import androidx.ui.demos.common.ActivityDemo
import androidx.ui.demos.common.ComposableDemo
import androidx.ui.demos.common.Demo
import androidx.ui.demos.common.DemoCategory
import androidx.ui.demos.common.allDemos
import androidx.ui.demos.common.allLaunchableDemos
import androidx.ui.test.android.AndroidComposeTestRule
import androidx.ui.test.assertLabelEquals
import androidx.ui.test.doClick
import androidx.ui.test.doScrollTo
import androidx.ui.test.find
import androidx.ui.test.findByTag
import androidx.ui.test.findByText
import androidx.ui.test.hasClickAction
import androidx.ui.test.hasText
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

// TODO: expand testing here to include Activity based demos, which are harder to handle
// synchronization between
@LargeTest
@RunWith(JUnit4::class)
class DemoTest {
    @get:Rule
    val composeTestRule = AndroidComposeTestRule<DemoActivity>(disableTransitions = true)

    @Test
    fun testFiltering() {
        assertIsOnRootScreen()
        // Enter filtering mode
        findByTag(Tags.FilterButton).doClick()
        // TODO: use keyboard input APIs when available to actually filter the list
        val testDemo = AllDemosCategory.allLaunchableDemos()
            .filterIsInstance<ComposableDemo>()
            .sortedBy { it.title }
            .first()
        // Click on the first demo
        val demoTitle = testDemo.title
        findByText(demoTitle).doClick()
        assertAppBarHasTitle(demoTitle)
        Espresso.pressBack()
        assertIsOnRootScreen()
    }

    @Test
    fun navigateThroughAllDemos() {
        // Keep track of each demo we visit
        val visitedDemos = mutableListOf<Demo>()

        // Visit all demos, ensuring we start and end up on the root screen
        assertIsOnRootScreen()
        AllDemosCategory.visitDemos(visitedDemos = visitedDemos, path = listOf(AllDemosCategory))
        assertIsOnRootScreen()

        // Ensure that we visited all the demos we expected to, in the order we expected to.
        val nonActivityDemos = AllDemosCategory.allDemos().filter { it !is ActivityDemo<*> }
        assertThat(visitedDemos).isEqualTo(nonActivityDemos)
    }

    /**
     * DFS traversal of each demo in a [DemoCategory] using [Demo.visit]
     *
     * @param path The path of categories that leads to this demo
     */
    private fun DemoCategory.visitDemos(visitedDemos: MutableList<Demo>, path: List<DemoCategory>) {
        demos.filter { it !is ActivityDemo<*> }.forEach { demo ->
            visitedDemos.add(demo)
            demo.visit(visitedDemos, path)
        }
    }

    /**
     * Visits a [Demo], and then navigates back up to the [DemoCategory] it was inside.
     *
     * If this [Demo] is a [DemoCategory], this will visit all sub-[Demo]s first before continuing
     * in the current category.
     *
     * @param path The path of categories that leads to this demo
     */
    private fun Demo.visit(visitedDemos: MutableList<Demo>, path: List<DemoCategory>) {
        val navigationTitle = if (path.size == 1) {
            path.first().title
        } else {
            path.drop(1).joinToString(" > ") { it.title }
        }

        // TODO: b/154796168 remove this when we can check for dialogs and dismiss them
        if (title in listOf("Dialog", "AlertDialog")) return

        find(hasText(title) and hasClickAction())
            .assertExists("Couldn't find \"$title\" in \"$navigationTitle\"")
            .doScrollTo()
            .doClick()

        if (this is DemoCategory) {
            visitDemos(visitedDemos, path + this)
        }

        Espresso.pressBack()

        assertAppBarHasTitle(navigationTitle)
    }
}

/**
 * Asserts that the app bar title matches the root category title, so we are on the root screen.
 */
private fun assertIsOnRootScreen() = assertAppBarHasTitle(AllDemosCategory.title)

/**
 * Asserts that the app bar title matches the given [title].
 */
private fun assertAppBarHasTitle(title: String) =
    findByTag(Tags.AppBarTitle).assertLabelEquals(title)
