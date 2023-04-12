package androidx.compose.foundation.text2

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.fetchTextLayoutResult
import androidx.compose.foundation.text2.input.TextFieldCharSequence
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasImeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.isNotFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFoundationApi::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
class BasicTextField2SemanticsTest {
    @get:Rule
    val rule = createComposeRule()

    private val Tag = "TextField"

    @Test
    fun defaultSemantics() {
        rule.setContent {
            BasicTextField2(
                modifier = Modifier.testTag(Tag),
                state = remember { TextFieldState() },
                decorationBox = {
                    Column {
                        BasicText("label")
                        it()
                    }
                }
            )
        }

        rule.onNodeWithTag(Tag)
            .assertEditableTextEquals("")
            .assertTextEquals("label", includeEditableText = false)
            .assertHasClickAction()
            .assert(hasSetTextAction())
            .assert(hasImeAction(ImeAction.Default))
            .assert(isNotFocused())
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.TextSelectionRange,
                    TextRange.Zero
                )
            )
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.SetText))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Password))
            // TODO(halilibo): enable after selection work is completed.
            // .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.SetSelection))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult))

        val textLayoutResults = mutableListOf<TextLayoutResult>()
        rule.onNodeWithTag(Tag)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(textLayoutResults) }
        assert(textLayoutResults.size == 1) { "TextLayoutResult is null" }
    }

    @Test
    fun semantics_enabledStatus() {
        var enabled by mutableStateOf(true)
        rule.setContent {
            val state = remember { TextFieldState() }
            BasicTextField2(
                state = state,
                modifier = Modifier.testTag(Tag),
                enabled = enabled
            )
        }

        rule.onNodeWithTag(Tag)
            .assert(isEnabled())

        enabled = false
        rule.waitForIdle()

        rule.onNodeWithTag(Tag)
            .assert(isNotEnabled())
    }

    @Test
    fun semantics_clickAction() {
        rule.setContent {
            val state = remember { TextFieldState() }
            BasicTextField2(
                state = state,
                modifier = Modifier.testTag(Tag)
            )
        }

        rule.onNodeWithTag(Tag)
            .assert(isNotFocused())
            .performSemanticsAction(SemanticsActions.OnClick)
        rule.onNodeWithTag(Tag)
            .assert(isFocused())
    }

    @Test
    fun semantics_imeOption() {
        rule.setContent {
            val state = remember { TextFieldState() }
            BasicTextField2(
                state = state,
                modifier = Modifier.testTag(Tag),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )
        }

        rule.onNodeWithTag(Tag).assert(hasImeAction(ImeAction.Search))
    }

    @Test
    fun contentSemanticsAreSet_inTheFirstComposition() {
        val state = TextFieldState("hello")
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier.testTag(Tag)
            )
        }

        rule.onNodeWithTag(Tag).assertTextEquals("hello")
    }

    @Test
    fun contentSemanticsAreSet_afterRecomposition() {
        val state = TextFieldState("hello")
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier.testTag(Tag)
            )
        }

        rule.onNodeWithTag(Tag).assertTextEquals("hello")

        state.editProcessor.reset(TextFieldCharSequence("hello2"))

        rule.onNodeWithTag(Tag).assertTextEquals("hello2")
    }

    @Test
    fun selectionSemanticsAreSet_inTheFirstComposition() {
        val state = TextFieldState("hello", initialSelectionInChars = TextRange(2))
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier.testTag(Tag)
            )
        }

        with(rule.onNodeWithTag(Tag)) {
            assertTextEquals("hello")
            assertSelection(TextRange(2))
        }
    }

    @Test
    fun selectionSemanticsAreSet_afterRecomposition() {
        val state = TextFieldState("hello")
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier.testTag(Tag)
            )
        }

        with(rule.onNodeWithTag(Tag)) {
            assertTextEquals("hello")
            assertSelection(TextRange.Zero)
        }

        state.editProcessor.reset(TextFieldCharSequence("hello", selection = TextRange(2)))

        with(rule.onNodeWithTag(Tag)) {
            assertTextEquals("hello")
            assertSelection(TextRange(2))
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun inputSelection_changesSelectionState() {
        val state = TextFieldState("hello")
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier.testTag(Tag)
            )
        }

        rule.onNodeWithTag(Tag).performTextInputSelection(TextRange(2))

        rule.runOnIdle {
            assertThat(state.text.selectionInChars).isEqualTo(TextRange(2))
        }
    }

    @Test
    fun textLayoutResultSemanticsAreSet_inTheFirstComposition() {
        val state = TextFieldState("hello")
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier
                    .testTag(Tag)
            )
        }

        rule.onNodeWithTag(Tag).assertTextEquals("hello")
        assertThat(rule.onNodeWithTag(Tag).fetchTextLayoutResult().layoutInput.text.text)
            .isEqualTo("hello")
    }

    @Test
    fun textLayoutResultSemanticsAreUpdated_afterRecomposition() {
        val state = TextFieldState()
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier
                    .testTag(Tag)
            )
        }

        rule.onNodeWithTag(Tag).assertTextEquals("")
        rule.onNodeWithTag(Tag).performTextInput("hello")
        assertThat(rule.onNodeWithTag(Tag).fetchTextLayoutResult().layoutInput.text.text)
            .isEqualTo("hello")
    }

    @Test
    fun semanticsAreSet_afterStateObjectChanges() {
        val state1 = TextFieldState("hello")
        val state2 = TextFieldState("world", TextRange(2))
        var chosenState by mutableStateOf(true)
        rule.setContent {
            BasicTextField2(
                state = if (chosenState) state1 else state2,
                modifier = Modifier.testTag(Tag)
            )
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

    private fun SemanticsNodeInteraction.assertEditableTextEquals(
        value: String
    ): SemanticsNodeInteraction =
        assert(
            SemanticsMatcher("${SemanticsProperties.EditableText.name} = '$value'") {
                it.config.getOrNull(SemanticsProperties.EditableText)?.text.equals(value)
            }
        )
}
