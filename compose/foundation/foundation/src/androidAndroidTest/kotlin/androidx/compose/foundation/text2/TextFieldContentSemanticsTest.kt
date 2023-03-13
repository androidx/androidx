@file:OptIn(ExperimentalFoundationApi::class)

package androidx.compose.foundation.text2

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class TextFieldContentSemanticsTest {
    @get:Rule
    val rule = createComposeRule()

    private val Tag = "TextField"

    @Test
    fun contentSemanticsAreSet_inTheFirstComposition() {
        val state = TextFieldState(TextFieldValue("hello"))
        rule.setContent {
            Box(modifier = Modifier.testTag(Tag).then(TextFieldContentSemanticsElement(state)))
        }

        rule.onNodeWithTag(Tag).assertTextEquals("hello")
    }

    @Test
    fun contentSemanticsAreSet_afterRecomposition() {
        val state = TextFieldState(TextFieldValue("hello"))
        rule.setContent {
            Box(modifier = Modifier.testTag(Tag).then(TextFieldContentSemanticsElement(state)))
        }

        rule.onNodeWithTag(Tag).assertTextEquals("hello")

        state.editProcessor.reset(TextFieldValue("hello2"))

        rule.onNodeWithTag(Tag).assertTextEquals("hello2")
    }

    @Test
    fun selectionSemanticsAreSet_inTheFirstComposition() {
        val state = TextFieldState(TextFieldValue("hello", selection = TextRange(2)))
        rule.setContent {
            Box(modifier = Modifier.testTag(Tag).then(TextFieldContentSemanticsElement(state)))
        }

        with(rule.onNodeWithTag(Tag)) {
            assertTextEquals("hello")
            assertSelection(TextRange(2))
        }
    }

    @Test
    fun selectionSemanticsAreSet_afterRecomposition() {
        val state = TextFieldState(TextFieldValue("hello"))
        rule.setContent {
            Box(modifier = Modifier.testTag(Tag).then(TextFieldContentSemanticsElement(state)))
        }

        with(rule.onNodeWithTag(Tag)) {
            assertTextEquals("hello")
            assertSelection(TextRange.Zero)
        }

        state.editProcessor.reset(TextFieldValue("hello", selection = TextRange(2)))

        with(rule.onNodeWithTag(Tag)) {
            assertTextEquals("hello")
            assertSelection(TextRange(2))
        }
    }

    @Test
    fun semanticsAreSet_afterStateObjectChanges() {
        val state1 = TextFieldState(TextFieldValue("hello"))
        val state2 = TextFieldState(TextFieldValue("world", TextRange(2)))
        var chosenState by mutableStateOf(true)
        rule.setContent {
            Box(modifier = Modifier.testTag(Tag).then(TextFieldContentSemanticsElement(
                if (chosenState) state1 else state2
            )))
        }

        with(rule.onNodeWithTag(Tag)) {
            assertTextEquals("hello")
            assertSelection(TextRange.Zero)
        }

        chosenState = false

        with(rule.onNodeWithTag(Tag)) {
            assertTextEquals("world")
            assertSelection(TextRange(2))
        }
    }

    private fun SemanticsNodeInteraction.assertSelection(expected: TextRange) {
        val selection = fetchSemanticsNode().config
            .getOrNull(SemanticsProperties.TextSelectionRange)
        assertThat(selection).isEqualTo(expected)
    }
}
