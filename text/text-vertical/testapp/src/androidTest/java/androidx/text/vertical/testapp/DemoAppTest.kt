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

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The title of the root category. */
private const val ROOT_TITLE = "Vertical Text Demos"

/** Tests the demo registry, [findDemoPath], and the navigation of [DemoApp]. */
@MediumTest
@RunWith(AndroidJUnit4::class)
class DemoAppTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun siblingTitles_areUnique() {
        assertSiblingTitlesUnique(VerticalTextDemos)
    }

    @Test
    fun findDemoPath_slashPath_returnsAncestorsFromRoot() {
        val path = findDemoPath(VerticalTextDemos, "HorizontalText/CJK font")

        assertThat(path.titles())
            .containsExactly(ROOT_TITLE, "HorizontalText", "CJK font")
            .inOrder()
    }

    @Test
    fun findDemoPath_breadcrumbPath_returnsAncestorsFromRoot() {
        val path = findDemoPath(VerticalTextDemos, "HorizontalText > CJK font")

        assertThat(path.titles())
            .containsExactly(ROOT_TITLE, "HorizontalText", "CJK font")
            .inOrder()
    }

    @Test
    fun findDemoPath_uniqueTitle_returnsNestedPath() {
        assertThat(findDemoPath(VerticalTextDemos, "CJK font").titles())
            .containsExactly(ROOT_TITLE, "HorizontalText", "CJK font")
            .inOrder()
        assertThat(findDemoPath(VerticalTextDemos, "Subtitle cue").titles())
            .containsExactly(ROOT_TITLE, "Subtitle cue")
            .inOrder()
        assertThat(findDemoPath(VerticalTextDemos, "VerticalText").titles())
            .containsExactly(ROOT_TITLE, "VerticalText")
            .inOrder()
    }

    @Test
    fun findDemoPath_uniqueSubstring_returnsNestedPath() {
        val path = findDemoPath(VerticalTextDemos, "CJK")

        assertThat(path.titles())
            .containsExactly(ROOT_TITLE, "HorizontalText", "CJK font")
            .inOrder()
    }

    @Test
    fun findDemoPath_repeatedTitle_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            findDemoPath(VerticalTextDemos, "Long text")
        }
    }

    @Test
    fun findDemoPath_unknownTitle_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            findDemoPath(VerticalTextDemos, "Nope")
        }
    }

    @Test
    fun demoApp_categoryClick_showsDemoAndBackReturns() {
        rule.setContent { DemoApp(rememberDemoAppState(VerticalTextDemos)) }

        backButton().assertDoesNotExist()
        rule.onNodeWithText("VerticalText").performClick()
        rule.onNodeWithText("Long text").performClick()
        appBarTitle().assertTextEquals("VerticalText > Long text")

        backButton().performClick()

        appBarTitle().assertTextEquals("VerticalText")
    }

    @Test
    fun demoApp_systemBack_popsToParent() {
        rule.setContent { DemoApp(rememberDemoAppState(VerticalTextDemos)) }
        rule.onNodeWithText("VerticalText").performClick()
        rule.onNodeWithText("Long text").performClick()

        Espresso.pressBack()

        appBarTitle().assertTextEquals("VerticalText")
    }

    @Test
    fun demoApp_deepLink_keepsAncestorsAfterRecreation() {
        val restorationTester = StateRestorationTester(rule)
        restorationTester.setContent {
            val initialBackStack = findDemoPath(VerticalTextDemos, "HorizontalText/CJK font")
            DemoApp(rememberDemoAppState(VerticalTextDemos, initialBackStack))
        }
        backButton().performClick()

        restorationTester.emulateSavedInstanceStateRestore()

        appBarTitle().assertTextEquals("HorizontalText")
        backButton().performClick()
        appBarTitle().assertTextEquals(ROOT_TITLE)
    }

    /** Asserts that no two children of [category] share a title, at every depth. */
    private fun assertSiblingTitlesUnique(category: DemoCategory) {
        assertThat(category.demos.titles()).containsNoDuplicates()
        category.demos.filterIsInstance<DemoCategory>().forEach { assertSiblingTitlesUnique(it) }
    }

    /** Returns the titles of the demos in this list. */
    private fun List<Demo>.titles(): List<String> = map { it.title }

    /** Returns the title of the app bar. */
    private fun appBarTitle(): SemanticsNodeInteraction = rule.onNodeWithTag(DEMO_APP_BAR_TITLE_TAG)

    /** Returns the back button of the app bar. */
    private fun backButton(): SemanticsNodeInteraction = rule.onNodeWithContentDescription("Back")
}
