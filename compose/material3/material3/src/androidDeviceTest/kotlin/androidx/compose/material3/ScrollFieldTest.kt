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

package androidx.compose.material3

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ScrollFieldTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun scrollField_initialState() {
        val itemCount = 10
        val initialIndex = 3
        lateinit var state: ScrollFieldState
        rule.setContent {
            state = rememberScrollFieldState(itemCount = itemCount, index = initialIndex)
            ScrollField(state = state, contentDescription = null)
        }

        assertThat(state.selectedOption).isEqualTo(initialIndex)

        rule
            .onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "3"))
    }

    @Test
    fun scrollField_selectOption() {
        val itemCount = 10
        lateinit var state: ScrollFieldState
        rule.setContent {
            state = rememberScrollFieldState(itemCount = itemCount, index = 0)
            ScrollField(state = state, contentDescription = null)
        }

        rule.onNodeWithText("01", useUnmergedTree = true).performClick()
        rule.waitForIdle()

        assertThat(state.selectedOption).isEqualTo(1)

        rule
            .onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "1"))
    }

    @Test
    fun scrollField_setProgressSemantics() {
        val itemCount = 10
        lateinit var state: ScrollFieldState
        rule.setContent {
            state = rememberScrollFieldState(itemCount = itemCount, index = 3)
            ScrollField(state = state, contentDescription = null)
        }

        val scrollFieldNode =
            rule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))

        val rangeInfo =
            scrollFieldNode
                .fetchSemanticsNode()
                .config
                .getOrNull(SemanticsProperties.ProgressBarRangeInfo)
        assertThat(rangeInfo).isNotNull()
        val initialProgress = rangeInfo!!.current

        scrollFieldNode.performSemanticsAction(SemanticsActions.SetProgress) {
            it(initialProgress + 1f)
        }
        rule.waitForIdle()

        assertThat(state.selectedOption).isEqualTo(4)
        scrollFieldNode.assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "4")
        )

        scrollFieldNode.performSemanticsAction(SemanticsActions.SetProgress) { it(initialProgress) }
        rule.waitForIdle()
        assertThat(state.selectedOption).isEqualTo(3)
        scrollFieldNode.assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "3")
        )
    }

    @Test
    fun scrollField_customAccessibilityDescription() {
        val itemCount = 10
        lateinit var state: ScrollFieldState
        rule.setContent {
            state = rememberScrollFieldState(itemCount = itemCount, index = 5)
            ScrollField(
                state = state,
                contentDescription = null,
                fieldAccessibilityDescription = { index -> "Option $index" },
            )
        }

        rule
            .onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Option 5"))
    }

    @Test
    fun scrollFieldState_targetOption() {
        val itemCount = 10
        lateinit var state: ScrollFieldState
        rule.setContent {
            state = rememberScrollFieldState(itemCount = itemCount, index = 0)
            ScrollField(state = state, contentDescription = null)
        }

        assertThat(state.targetOption).isEqualTo(0)

        // Initiate a scroll
        rule.mainClock.autoAdvance = false
        val scrollFieldNode =
            rule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))

        val rangeInfo =
            scrollFieldNode
                .fetchSemanticsNode()
                .config
                .getOrNull(SemanticsProperties.ProgressBarRangeInfo)

        // Use SetProgress to scroll forward by 1 page (which should result in option 1)
        scrollFieldNode.performSemanticsAction(SemanticsActions.SetProgress) {
            it(rangeInfo!!.current + 1f)
        }

        // Advance the clock slightly to start the animation but not finish it
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()

        // targetOption should instantly update to 1, even if the animation hasn't settled yet
        assertThat(state.targetOption).isEqualTo(1)

        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        assertThat(state.targetOption).isEqualTo(1)
        assertThat(state.selectedOption).isEqualTo(1)
    }

    @Test
    fun scrollField_keyboardNavigation() {
        val itemCount = 10
        lateinit var state: ScrollFieldState
        rule.setContent {
            state = rememberScrollFieldState(itemCount = itemCount, index = 3)
            ScrollField(state = state, contentDescription = null)
        }

        val scrollFieldNode =
            rule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))

        scrollFieldNode.requestFocus()
        scrollFieldNode.assertIsFocused()
        rule.waitForIdle()

        scrollFieldNode.performKeyInput { pressKey(Key.DirectionDown) }
        rule.waitForIdle()

        assertThat(state.selectedOption).isEqualTo(4)

        scrollFieldNode.performKeyInput { pressKey(Key.DirectionUp) }
        rule.waitForIdle()

        assertThat(state.selectedOption).isEqualTo(3)
    }

    @Test
    fun scrollField_contentDescription() {
        val itemCount = 10
        lateinit var state: ScrollFieldState
        rule.setContent {
            state = rememberScrollFieldState(itemCount = itemCount, index = 5)
            ScrollField(state = state, contentDescription = "Test description")
        }

        rule
            .onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            .assertContentDescriptionEquals("Test description")
    }
}
