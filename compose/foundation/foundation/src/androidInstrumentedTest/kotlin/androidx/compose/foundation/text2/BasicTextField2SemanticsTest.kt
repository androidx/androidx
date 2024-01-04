package androidx.compose.foundation.text2

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.fetchTextLayoutResult
import androidx.compose.foundation.text.selection.isSelectionHandle
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.foundation.text2.input.internal.selection.FakeClipboardManager
import androidx.compose.foundation.text2.input.placeCursorAtEnd
import androidx.compose.foundation.text2.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
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
import androidx.compose.ui.test.performTextReplacement
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
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.PasteText))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Password))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.SetSelection))
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
    fun semantics_setTextAction() {
        val state = TextFieldState()
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier.testTag(Tag)
            )
        }

        rule.onNodeWithTag(Tag)
            .assert(isNotFocused())
            .performTextReplacement("Hello")
        rule.onNodeWithTag(Tag)
            .assert(isFocused())
            .assertTextEquals("Hello")

        assertThat(state.text.toString()).isEqualTo("Hello")
    }

    @Test
    fun semantics_performSetTextAction_whenReadOnly() {
        val state = TextFieldState("", initialSelectionInChars = TextRange(1))
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier.testTag(Tag),
                readOnly = true
            )
        }

        rule.onNodeWithTag(Tag)
            .performTextReplacement("hello")

        assertThat(state.text.toString()).isEqualTo("")
    }

    @Test
    fun semantics_setTextAction_appliesFilter() {
        val state = TextFieldState()
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier.testTag(Tag),
                inputTransformation = { _, changes ->
                    if (changes.length > 1) {
                        val newText = changes.asCharSequence().asSequence().joinToString("-")
                        changes.replace(0, changes.length, newText)
                    }
                }
            )
        }

        rule.onNodeWithTag(Tag)
            .assert(isNotFocused())
            .performTextReplacement("Hello")
        rule.onNodeWithTag(Tag)
            .assert(isFocused())
            .assertTextEquals("H-e-l-l-o")

        assertThat(state.text.toString()).isEqualTo("H-e-l-l-o")
    }

    @Test
    fun semantics_performTextInputAction() {
        val state = TextFieldState("Hello", initialSelectionInChars = TextRange(1))
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier.testTag(Tag)
            )
        }

        rule.onNodeWithTag(Tag)
            .assert(isNotFocused())
            .performTextInput("a")
        rule.onNodeWithTag(Tag)
            .assert(isFocused())
            .assertTextEquals("Haello")

        assertThat(state.text.toString()).isEqualTo("Haello")
    }

    @Test
    fun semantics_performTextInputAction_whenReadOnly() {
        val state = TextFieldState("", initialSelectionInChars = TextRange(1))
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier.testTag(Tag),
                readOnly = true
            )
        }

        rule.onNodeWithTag(Tag)
            .performTextInput("hello")

        assertThat(state.text.toString()).isEqualTo("")
    }

    @Test
    fun semantics_performTextInputAction_appliesFilter() {
        val state = TextFieldState("Hello", initialSelectionInChars = TextRange(1))
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier.testTag(Tag),
                inputTransformation = { _, changes ->
                    val newChange = changes.asCharSequence().replace(Regex("a"), "")
                    changes.replace(0, changes.length, newChange)
                }
            )
        }

        rule.onNodeWithTag(Tag)
            .assert(isNotFocused())
            .performTextInput("abc")
        rule.onNodeWithTag(Tag)
            .assert(isFocused())
            .assertTextEquals("Hbcello")

        assertThat(state.text.toString()).isEqualTo("Hbcello")
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

        state.setTextAndPlaceCursorAtEnd("hello2")

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
        val state = TextFieldState("hello", initialSelectionInChars = TextRange.Zero)
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

        state.edit {
            selectCharsIn(TextRange(2))
        }

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

        rule.onNodeWithTag(Tag).performTextInputSelection(TextRange(2, 3))

        rule.onNode(isSelectionHandle(Handle.SelectionStart)).assertIsDisplayed()
        rule.onNode(isSelectionHandle(Handle.SelectionEnd)).assertIsDisplayed()

        rule.runOnIdle {
            assertThat(state.text.selectionInChars).isEqualTo(TextRange(2, 3))
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun inputSelection_changesSelectionState_appliesFilter() {
        val state = TextFieldState("hello", initialSelectionInChars = TextRange(5))
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier.testTag(Tag),
                inputTransformation = { _, changes ->
                    changes.revertAllChanges()
                }
            )
        }

        rule.onNodeWithTag(Tag).performTextInputSelection(TextRange(2))

        rule.runOnIdle {
            assertThat(state.text.selectionInChars).isEqualTo(TextRange(5))
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
        val state1 = TextFieldState("hello", initialSelectionInChars = TextRange.Zero)
        val state2 = TextFieldState("world", initialSelectionInChars = TextRange(2))
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

    @Test
    fun semantics_paste_notAvailable_whenDisabledOrReadOnly() {
        val state = TextFieldState("World!", initialSelectionInChars = TextRange(0))
        var enabled by mutableStateOf(false)
        var readOnly by mutableStateOf(false)
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier.testTag(Tag),
                enabled = enabled,
                readOnly = readOnly
            )
        }

        rule.onNodeWithTag(Tag).assert(SemanticsMatcher.keyNotDefined(SemanticsActions.PasteText))

        enabled = true
        readOnly = true

        rule.waitForIdle()

        rule.onNodeWithTag(Tag).assert(SemanticsMatcher.keyNotDefined(SemanticsActions.PasteText))

        enabled = true
        readOnly = false

        rule.waitForIdle()

        rule.onNodeWithTag(Tag).assert(SemanticsMatcher.keyIsDefined(SemanticsActions.PasteText))
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun semantics_paste() {
        val state = TextFieldState("Here World!")
        val clipboardManager = FakeClipboardManager("Hello")
        rule.setContent {
            CompositionLocalProvider(LocalClipboardManager provides clipboardManager) {
                BasicTextField2(
                    state = state,
                    modifier = Modifier.testTag(Tag)
                )
            }
        }

        rule.onNodeWithTag(Tag).performTextInputSelection(TextRange(0, 4))
        rule.onNodeWithTag(Tag).performSemanticsAction(SemanticsActions.PasteText)

        rule.runOnIdle {
            assertThat(state.text.selectionInChars).isEqualTo(TextRange(5))
            assertThat(state.text.toString()).isEqualTo("Hello World!")
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun semantics_paste_appliesFilter() {
        val state = TextFieldState("Here World!")
        val clipboardManager = FakeClipboardManager("Hello")
        rule.setContent {
            CompositionLocalProvider(LocalClipboardManager provides clipboardManager) {
                BasicTextField2(
                    state = state,
                    modifier = Modifier.testTag(Tag),
                    inputTransformation = { _, changes ->
                        // remove all 'l' characters
                        if (changes.changes.changeCount != 0) {
                            val newChange = changes.asCharSequence().replace(Regex("l"), "")
                            changes.replace(0, changes.length, newChange)
                            changes.placeCursorAtEnd()
                        }
                    }
                )
            }
        }

        rule.onNodeWithTag(Tag).performTextInputSelection(TextRange(0, 4))
        rule.onNodeWithTag(Tag).performSemanticsAction(SemanticsActions.PasteText)

        rule.runOnIdle {
            assertThat(state.text.selectionInChars).isEqualTo(TextRange(9))
            assertThat(state.text.toString()).isEqualTo("Heo Word!")
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun semantics_copy() {
        val state = TextFieldState("Hello World!")
        val clipboardManager = FakeClipboardManager()
        rule.setContent {
            CompositionLocalProvider(LocalClipboardManager provides clipboardManager) {
                BasicTextField2(
                    state = state,
                    modifier = Modifier.testTag(Tag)
                )
            }
        }

        rule.onNodeWithTag(Tag).performTextInputSelection(TextRange(0, 5))
        rule.onNodeWithTag(Tag).performSemanticsAction(SemanticsActions.CopyText)

        rule.runOnIdle {
            assertThat(clipboardManager.getText()?.toString()).isEqualTo("Hello")
        }
    }

    @Test
    fun semantics_copy_disabled_whenSelectionCollapsed() {
        val state = TextFieldState("Hello World!")
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier.testTag(Tag)
            )
        }

        rule.onNodeWithTag(Tag).assert(SemanticsMatcher.keyNotDefined(SemanticsActions.CopyText))
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun semantics_copy_appliesFilter() {
        val state = TextFieldState("Hello World!", initialSelectionInChars = TextRange(0, 5))
        val clipboardManager = FakeClipboardManager()
        rule.setContent {
            CompositionLocalProvider(LocalClipboardManager provides clipboardManager) {
                BasicTextField2(
                    state = state,
                    modifier = Modifier.testTag(Tag),
                    inputTransformation = { original, changes ->
                        // reject copy action collapsing the selection
                        if (changes.selectionInChars != original.selectionInChars) {
                            changes.revertAllChanges()
                        }
                    }
                )
            }
        }

        rule.onNodeWithTag(Tag).performSemanticsAction(SemanticsActions.CopyText)

        rule.runOnIdle {
            assertThat(state.text.selectionInChars).isEqualTo(TextRange(0, 5))
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun semantics_cut() {
        val state = TextFieldState("Hello World!", initialSelectionInChars = TextRange(0, 5))
        val clipboardManager = FakeClipboardManager()
        rule.setContent {
            CompositionLocalProvider(LocalClipboardManager provides clipboardManager) {
                BasicTextField2(
                    state = state,
                    modifier = Modifier.testTag(Tag)
                )
            }
        }

        rule.onNodeWithTag(Tag).performSemanticsAction(SemanticsActions.CutText)

        rule.runOnIdle {
            assertThat(state.text.toString()).isEqualTo(" World!")
            assertThat(state.text.selectionInChars).isEqualTo(TextRange(0))
            assertThat(clipboardManager.getText()?.toString()).isEqualTo("Hello")
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun semantics_cut_appliesFilter() {
        val state = TextFieldState("Hello World!", initialSelectionInChars = TextRange(0, 5))
        val clipboardManager = FakeClipboardManager()
        rule.setContent {
            CompositionLocalProvider(LocalClipboardManager provides clipboardManager) {
                BasicTextField2(
                    state = state,
                    modifier = Modifier.testTag(Tag),
                    inputTransformation = { _, changes ->
                        changes.revertAllChanges()
                    }
                )
            }
        }

        rule.onNodeWithTag(Tag).performSemanticsAction(SemanticsActions.CutText)

        rule.runOnIdle {
            assertThat(state.text.toString()).isEqualTo("Hello World!")
            assertThat(state.text.selectionInChars).isEqualTo(TextRange(0, 5))
            assertThat(clipboardManager.getText()?.toString()).isEqualTo("Hello")
        }
    }

    @Test
    fun semantics_cut_notAvailable_whenDisabledOrReadOnly() {
        val state = TextFieldState("World!", initialSelectionInChars = TextRange(0, 1))
        var enabled by mutableStateOf(false)
        var readOnly by mutableStateOf(false)
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier.testTag(Tag),
                enabled = enabled,
                readOnly = readOnly
            )
        }

        rule.onNodeWithTag(Tag).assert(SemanticsMatcher.keyNotDefined(SemanticsActions.CutText))

        enabled = true
        readOnly = true

        rule.waitForIdle()

        rule.onNodeWithTag(Tag).assert(SemanticsMatcher.keyNotDefined(SemanticsActions.CutText))

        enabled = true
        readOnly = false

        rule.waitForIdle()

        rule.onNodeWithTag(Tag).assert(SemanticsMatcher.keyIsDefined(SemanticsActions.CutText))
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
