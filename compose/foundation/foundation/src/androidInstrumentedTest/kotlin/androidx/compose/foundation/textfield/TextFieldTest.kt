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

@file:Suppress("Deprecation")

package androidx.compose.foundation.textfield

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.internal.readAnnotatedString
import androidx.compose.foundation.internal.toClipEntry
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.FocusedWindowTest
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.computeSizeForDefaultText
import androidx.compose.foundation.text.contextmenu.internal.ProvidePlatformTextContextMenuToolbar
import androidx.compose.foundation.text.contextmenu.test.ContextMenuFlagFlipperRunner
import androidx.compose.foundation.text.contextmenu.test.ContextMenuFlagSuppress
import androidx.compose.foundation.text.contextmenu.test.SpyTextActionModeCallback
import androidx.compose.foundation.text.contextmenu.test.assertShown
import androidx.compose.foundation.text.input.InputMethodInterceptor
import androidx.compose.foundation.text.selection.fetchTextLayoutResult
import androidx.compose.foundation.text.selection.isSelectionHandle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertPixelColor
import androidx.compose.testutils.assertShape
import androidx.compose.testutils.expectError
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.NativeClipboard
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasImeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.isNotFocused
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.UrlAnnotation
import androidx.compose.ui.text.VerbatimTtsAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.toFontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TextFieldValue.Companion.Saver
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.text.withAnnotation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.fonts.R
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@MediumTest
@RunWith(ContextMenuFlagFlipperRunner::class)
class TextFieldTest : FocusedWindowTest {
    @get:Rule val rule = createComposeRule()

    private val inputMethodInterceptor = InputMethodInterceptor(rule)
    private val Tag = "textField"

    // This sample font provides the following features:
    // 1. The width of most of visible characters equals to font size.
    // 2. The LTR/RTL characters are rendered as ▶/◀.
    // 3. The fontMetrics passed to TextPaint has descend - ascend equal to 1.2 * fontSize.
    private val measureFontFamily =
        Font(resId = R.font.sample_font, weight = FontWeight.Normal, style = FontStyle.Normal)
            .toFontFamily()

    @Test
    fun textField_focusInSemantics() {
        var isFocused = false
        rule.setContent {
            val state = remember { mutableStateOf("") }
            BasicTextField(
                value = state.value,
                modifier = Modifier.fillMaxSize().onFocusChanged { isFocused = it.isFocused },
                onValueChange = { state.value = it },
            )
        }

        rule.onNode(hasSetTextAction()).performClick()

        rule.runOnIdle { assertThat(isFocused).isTrue() }
    }

    @Test
    fun textField_commitTexts() {
        var state by mutableStateOf("")
        inputMethodInterceptor.setContent {
            BasicTextField(
                value = state,
                modifier = Modifier.fillMaxSize(),
                onValueChange = { state = it },
            )
        }
        rule.onNode(hasSetTextAction()).performClick()

        with(inputMethodInterceptor) {
            withInputConnection { commitText("1", 1) }
            withInputConnection { commitText("a", 1) }
            withInputConnection { commitText("2", 1) }
            withInputConnection { commitText("b", 1) }
            withInputConnection { commitText("3", 1) }
        }

        rule.runOnIdle { assertThat(state).isEqualTo("1a2b3") }
    }

    @Test
    fun textField_commitTexts_state_may_not_set() {
        var state by mutableStateOf("")
        inputMethodInterceptor.setContent {
            BasicTextField(
                value = state,
                modifier = Modifier.fillMaxSize(),
                onValueChange = { value ->
                    if (value.all { it.isDigit() }) {
                        state = value
                    }
                },
            )
        }

        rule.onNode(hasSetTextAction()).performClick()

        // Performs input events "1", "a", "2", "b", "3". Only numbers should remain.
        with(inputMethodInterceptor) {
            withInputConnection { commitText("1", 1) }
            withInputConnection { commitText("a", 1) }
            withInputConnection { commitText("2", 1) }
            withInputConnection { commitText("b", 1) }
            withInputConnection { commitText("3", 1) }
        }

        rule.runOnIdle { assertThat(state).isEqualTo("123") }
    }

    @Test
    fun textField_onTextLayoutCallback() {
        val onTextLayout: (TextLayoutResult) -> Unit = mock()
        var state by mutableStateOf("")
        inputMethodInterceptor.setContent {
            BasicTextField(
                value = state,
                modifier = Modifier.fillMaxSize(),
                onValueChange = { state = it },
                onTextLayout = onTextLayout,
            )
        }

        rule.onNode(hasSetTextAction()).performClick()

        // Performs input events "1", "2", "3".
        with(inputMethodInterceptor) {
            withInputConnection { commitText("1", 1) }
            withInputConnection { commitText("2", 1) }
            withInputConnection { commitText("3", 1) }
        }

        rule.runOnIdle { assertThat(state).isEqualTo("123") }
    }

    @Test
    fun textFieldInRow_fixedElementIsVisible() {
        val parentSize = 300.dp
        val boxSize = 50.dp
        var size: Int? = null
        rule.setContent {
            Box(Modifier.size(parentSize)) {
                Row {
                    BasicTextField(
                        value = "",
                        onValueChange = {},
                        modifier = Modifier.weight(1f).onGloballyPositioned { size = it.size.width },
                    )
                    Box(Modifier.size(boxSize))
                }
            }
        }

        with(rule.density) {
            assertThat(size).isEqualTo(parentSize.roundToPx() - boxSize.roundToPx())
        }
    }

    @Test
    fun textFieldValue_saverRestoresState() {
        var state: MutableState<TextFieldValue>? = null

        val restorationTester = StateRestorationTester(rule)
        restorationTester.setContent {
            state = rememberSaveable(stateSaver = Saver) { mutableStateOf(TextFieldValue()) }
        }

        rule.runOnIdle {
            state!!.value = TextFieldValue("test", TextRange(1, 2))

            // we null it to ensure recomposition happened
            state = null
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnIdle {
            assertThat(state!!.value).isEqualTo(TextFieldValue("test", TextRange(1, 2)))
        }
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun textFieldValue_saverRestoresState_withAnnotatedString() {
        var state: MutableState<TextFieldValue>? = null
        val annotatedString = buildAnnotatedString {
            withStyle(ParagraphStyle(textAlign = TextAlign.Justify)) { append("1") }
            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append("2") }
            withAnnotation(tag = "Tag1", annotation = "Annotation1") { append("3") }
            withAnnotation(VerbatimTtsAnnotation("verbatim1")) { append("4") }
            withAnnotation(tag = "Tag2", annotation = "Annotation2") { append("5") }
            withAnnotation(VerbatimTtsAnnotation("verbatim2")) { append("6") }
            withAnnotation(UrlAnnotation("url1")) { append("7") }
            withAnnotation(UrlAnnotation("url2")) { append("8") }
            withStyle(
                SpanStyle(
                    color = Color.Red,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic,
                    fontSynthesis = FontSynthesis.All,
                    fontFeatureSettings = "feature settings",
                    letterSpacing = 2.em,
                    baselineShift = BaselineShift.Superscript,
                    textGeometricTransform = TextGeometricTransform(2f, 3f),
                    localeList =
                        LocaleList(Locale("sr-Latn-SR"), Locale("sr-Cyrl-SR"), Locale.current),
                    background = Color.Blue,
                    textDecoration = TextDecoration.LineThrough,
                    shadow = Shadow(color = Color.Red, offset = Offset(2f, 2f), blurRadius = 4f),
                )
            ) {
                append("7")
            }
            withStyle(
                ParagraphStyle(
                    textAlign = TextAlign.Justify,
                    textDirection = TextDirection.Rtl,
                    lineHeight = 10.sp,
                    textIndent = TextIndent(firstLine = 2.sp, restLine = 3.sp),
                )
            ) {
                append("8")
            }
        }
        val newTextFieldValue = TextFieldValue(annotatedString, TextRange(1, 2))

        val restorationTester = StateRestorationTester(rule)
        restorationTester.setContent {
            state = rememberSaveable(stateSaver = Saver) { mutableStateOf(TextFieldValue()) }
        }

        rule.runOnIdle {
            state!!.value = newTextFieldValue
            // we null it to ensure recomposition happened
            state = null
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnIdle { assertThat(state!!.value).isEqualTo(newTextFieldValue) }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun textFieldNotFocused_cursorNotRendered() {
        rule.setContent {
            BasicTextField(
                value = "",
                onValueChange = {},
                textStyle = TextStyle(color = Color.White),
                modifier = Modifier.size(10.dp, 20.dp).background(color = Color.White),
                cursorBrush = SolidColor(Color.Blue),
            )
        }

        rule
            .onNode(hasSetTextAction())
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = RectangleShape,
                shapeColor = Color.White,
                backgroundColor = Color.White,
                antiAliasingGap = 0.0f,
            )
    }

    @Test
    fun defaultSemantics() {
        rule.setContent {
            BasicTextField(
                modifier = Modifier.testTag(Tag),
                value = "",
                onValueChange = {},
                decorationBox = {
                    Column {
                        BasicText("label")
                        it()
                    }
                },
            )
        }

        rule
            .onNodeWithTag(Tag)
            .assertEditableTextEquals("")
            .assertTextEquals("label", includeEditableText = false)
            .assertHasClickAction()
            .assert(hasSetTextAction())
            .assert(hasImeAction(ImeAction.Default))
            .assert(isNotFocused())
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.TextSelectionRange, TextRange.Zero)
            )
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.SetText))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.SetSelection))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult))

        val textLayoutResults = mutableListOf<TextLayoutResult>()
        rule.onNodeWithTag(Tag).performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
            it(textLayoutResults)
        }
        assert(textLayoutResults.size == 1) { "TextLayoutResult is null" }
    }

    @Test
    fun semantics_setTextAction_throwsAssertionErrorWhenReadOnly() {
        rule.setContent {
            var value by remember { mutableStateOf("") }
            BasicTextField(
                modifier = Modifier.testTag(Tag),
                value = value,
                onValueChange = { value = it },
                readOnly = true,
            )
        }

        expectError<AssertionError>(expectedMessage = "Failed to perform text input.*") {
            rule.onNodeWithTag(Tag).performTextReplacement("hello")
        }
        rule.onNodeWithTag(Tag).assertEditableTextEquals("")
    }

    @Test
    fun semantics_setTextAction_throwsWhenDisabled() {
        rule.setContent {
            var value by remember { mutableStateOf("") }
            BasicTextField(
                modifier = Modifier.testTag(Tag),
                value = value,
                onValueChange = { value = it },
                enabled = false,
            )
        }

        assertFailsWith<AssertionError> { rule.onNodeWithTag(Tag).performTextReplacement("hello") }
    }

    @Test
    fun semantics_insertTextAction_throwsAssertionErrorWhenReadOnly() {
        rule.setContent {
            var value by remember { mutableStateOf("") }
            BasicTextField(
                modifier = Modifier.testTag(Tag),
                value = value,
                onValueChange = { value = it },
                readOnly = true,
            )
        }

        expectError<AssertionError>(expectedMessage = "Failed to perform text input.*") {
            rule.onNodeWithTag(Tag).performTextInput("hello")
        }

        rule.onNodeWithTag(Tag).assertEditableTextEquals("")
    }

    @Test
    fun semantics_insertTextAction_throwsWhenDisabled() {
        rule.setContent {
            var value by remember { mutableStateOf("") }
            BasicTextField(
                modifier = Modifier.testTag(Tag),
                value = value,
                onValueChange = { value = it },
                enabled = false,
            )
        }

        assertFailsWith<AssertionError> { rule.onNodeWithTag(Tag).performTextInput("hello") }
    }

    @Test
    fun semantics_clickAction() {
        rule.setContent {
            var value by remember { mutableStateOf("") }
            BasicTextField(
                modifier = Modifier.testTag(Tag),
                value = value,
                onValueChange = { value = it },
            )
        }

        rule
            .onNodeWithTag(Tag)
            .assert(isNotFocused())
            .performSemanticsAction(SemanticsActions.OnClick)
        rule.onNodeWithTag(Tag).assert(isFocused())
    }

    @Test
    fun semantics_imeEnterAction() {
        var done = false
        rule.setContent {
            var value by remember { mutableStateOf("") }
            BasicTextField(
                modifier = Modifier.testTag(Tag),
                value = value,
                onValueChange = { value = it },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { done = true }),
            )
        }

        rule.onNodeWithTag(Tag).requestFocus().assertIsFocused()

        rule.runOnIdle { assertThat(done).isFalse() }

        rule.onNodeWithTag(Tag).performImeAction()

        rule.runOnIdle { assertThat(done).isTrue() }
    }

    @Test
    fun semantics_defaultImeEnterAction() {
        rule.setContent {
            var value by remember { mutableStateOf("") }
            BasicTextField(
                modifier = Modifier.testTag(Tag),
                value = value,
                onValueChange = { value = it },
                keyboardActions = KeyboardActions(),
            )
        }

        rule.onNodeWithTag(Tag).requestFocus().assertIsFocused()

        val error = assertFailsWith<AssertionError> { rule.onNodeWithTag(Tag).performImeAction() }
        assertThat(error)
            .hasMessageThat()
            .startsWith(
                "Failed to perform IME action.\n" +
                    "Failed to assert the following: (NOT (ImeAction = 'Default'))\n" +
                    "Semantics of the node:"
            )
    }

    @Test
    fun semantics_setTextSetSelectionActions() {
        rule.setContent {
            var value by remember { mutableStateOf("") }
            BasicTextField(
                modifier = Modifier.testTag(Tag),
                value = value,
                onValueChange = { value = it },
            )
        }

        val hello = AnnotatedString("Hello")
        rule
            .onNodeWithTag(Tag)
            .assertEditableTextEquals("")
            .performSemanticsAction(SemanticsActions.SetText) { it(hello) }
            .assertEditableTextEquals(hello.text)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.TextSelectionRange,
                    TextRange(hello.length),
                )
            )

        rule
            .onNodeWithTag(Tag)
            .performSemanticsAction(SemanticsActions.SetSelection) { it(1, 3, true) }
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.TextSelectionRange,
                    TextRange(1, 3),
                )
            )
    }

    @Test
    fun semantics_setTextSetSelection_transformed_invalidIndex() {
        rule.setContent {
            var value by remember { mutableStateOf("Hello") }
            BasicTextField(
                modifier = Modifier.testTag(Tag),
                value = value,
                onValueChange = { value = it },
            )
        }

        rule
            .onNodeWithTag(Tag)
            .performSemanticsAction(SemanticsActions.SetSelection) { it(0, Int.MAX_VALUE, false) }
            .assert(
                // invalid selection should be ignored.
                SemanticsMatcher.expectValue(
                    SemanticsProperties.TextSelectionRange,
                    TextRange(0, 0),
                )
            )
    }

    @Test
    fun semantics_setTextSetSelection_original_invalidIndex() {
        rule.setContent {
            var value by remember { mutableStateOf("Hello") }
            BasicTextField(
                modifier = Modifier.testTag(Tag),
                value = value,
                onValueChange = { value = it },
            )
        }

        rule
            .onNodeWithTag(Tag)
            .performSemanticsAction(SemanticsActions.SetSelection) { it(0, Int.MAX_VALUE, true) }
            .assert(
                // invalid selection should be ignored.
                SemanticsMatcher.expectValue(
                    SemanticsProperties.TextSelectionRange,
                    TextRange(0, 0),
                )
            )
    }

    @Test
    fun setImeAction_isReflectedInSemantics() {
        rule.setContent {
            BasicTextField(
                value = "",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                onValueChange = {},
            )
        }

        rule.onNode(hasSetTextAction()).assert(hasImeAction(ImeAction.Search))
    }

    @Test
    fun semantics_copyTextAction() {
        val text = "Hello World"
        var value by mutableStateOf(TextFieldValue(text, TextRange(0, 5)))

        rule.setContent {
            BasicTextField(
                modifier = Modifier.testTag(Tag),
                value = value,
                onValueChange = { value = it },
            )
        }

        rule.onNodeWithTag(Tag).performSemanticsAction(SemanticsActions.CopyText) { it() }

        rule.runOnIdle { assertThat(value.selection).isEqualTo(TextRange(5, 5)) }
    }

    @Test
    fun semantics_pasteTextAction() {
        val text = "Hello World"
        var value by mutableStateOf(TextFieldValue(text, TextRange(0, 6)))

        rule.setContent {
            BasicTextField(
                modifier = Modifier.testTag(Tag),
                value = value,
                onValueChange = { value = it },
            )
        }

        // copy text to the clipboard
        rule.onNodeWithTag(Tag).performSemanticsAction(SemanticsActions.CopyText) { it() }
        rule.runOnIdle {
            assertThat(value.selection.collapsed).isTrue()
            assertThat(value.selection.start).isEqualTo(6)
        }

        // paste text from the clipboard
        rule.onNodeWithTag(Tag).performSemanticsAction(SemanticsActions.PasteText) { it() }
        rule.runOnIdle { assertThat(value.text).isEqualTo("Hello Hello World") }
    }

    @Test
    fun semantics_cutTextAction() {
        val text = "Hello World"
        var value by mutableStateOf(TextFieldValue(text, TextRange(0, 6)))

        rule.setContent {
            BasicTextField(
                modifier = Modifier.testTag(Tag),
                value = value,
                onValueChange = { value = it },
            )
        }

        rule.onNodeWithTag(Tag).performSemanticsAction(SemanticsActions.CutText) { it() }

        rule.runOnIdle {
            assertThat(value.text).isEqualTo("World")
            assertThat(value.selection).isEqualTo(TextRange(0, 0))
        }
    }

    @Test
    fun semantics_passwordTextField_noCopyCutActions() {
        rule.setContent {
            BasicTextField(
                modifier = Modifier.testTag(Tag),
                value = TextFieldValue("Hello", TextRange(0, 3)),
                onValueChange = {},
                visualTransformation = PasswordVisualTransformation(),
            )
        }

        rule
            .onNodeWithTag(Tag)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.CopyText))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.CutText))
    }

    @Test
    fun semantics_transformedText() {
        rule.setContent {
            BasicTextField(
                modifier = Modifier.testTag(Tag),
                value = TextFieldValue("Hello"),
                onValueChange = {},
                visualTransformation = { text ->
                    TransformedText(text.toUpperCase(LocaleList("en-US")), OffsetMapping.Identity)
                },
            )
        }

        rule.onNodeWithTag(Tag).assertTextEquals("HELLO")
    }

    @ContextMenuFlagSuppress(suppressedFlagValue = true)
    @LargeTest
    @Test
    fun semantics_longClick() {
        val text = "Hello World"
        var value by mutableStateOf(TextFieldValue(text, TextRange(text.length)))
        var toolbar: TextToolbar? = null

        rule.setTextFieldTestContent {
            toolbar = LocalTextToolbar.current
            BasicTextField(
                modifier = Modifier.testTag(Tag),
                value = value,
                onValueChange = { value = it },
            )
        }

        rule.runOnIdle { assertThat(toolbar?.status).isEqualTo(TextToolbarStatus.Hidden) }

        rule.onNodeWithTag(Tag).performSemanticsAction(SemanticsActions.OnLongClick) { it() }

        rule.runOnIdle { assertThat(toolbar?.status).isEqualTo(TextToolbarStatus.Shown) }
    }

    @ContextMenuFlagSuppress(suppressedFlagValue = false)
    @LargeTest
    @Test
    fun semantics_longClick_newContextMenu() {
        val text = "Hello World"
        var value by mutableStateOf(TextFieldValue(text, TextRange(text.length)))
        val spyTextActionModeCallback = SpyTextActionModeCallback()

        rule.setTextFieldTestContent {
            ProvidePlatformTextContextMenuToolbar(
                callbackInjector = { spyTextActionModeCallback.apply { delegate = it } }
            ) {
                BasicTextField(
                    modifier = Modifier.testTag(Tag),
                    value = value,
                    onValueChange = { value = it },
                )
            }
        }

        spyTextActionModeCallback.assertShown(false)

        rule.onNodeWithTag(Tag).performSemanticsAction(SemanticsActions.OnLongClick) { it() }
        rule.waitForIdle()

        spyTextActionModeCallback.assertShown(true)
    }

    @Test
    fun stringOverrideTextField_canDeleteLastSymbol() {
        var lastSeenText = ""
        rule.setContent {
            var text by remember { mutableStateOf("") }
            BasicTextField(
                value = text,
                onValueChange = {
                    text = it
                    lastSeenText = it
                },
                modifier = Modifier.testTag(Tag),
            )
        }

        rule.onNodeWithTag(Tag).performTextInput("A")

        rule.runOnIdle { assertThat(lastSeenText).isEqualTo("A") }

        rule.onNodeWithTag(Tag).performTextClearance()

        rule.runOnIdle { assertThat(lastSeenText).isEqualTo("") }
    }

    @Test
    fun decorationBox_clickable() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        rule.setContent {
            scope = rememberCoroutineScope()
            Column {
                BasicTextField(
                    value = "test",
                    onValueChange = {},
                    textStyle = TextStyle(fontSize = 2.sp),
                    modifier = Modifier.requiredHeight(100.dp).fillMaxWidth(),
                    decorationBox = {
                        // the core text field is at the very bottom
                        Column {
                            BasicText("Label", Modifier.testTag("label"))
                            Spacer(Modifier.weight(1f))
                            it()
                        }
                    },
                    interactionSource = interactionSource,
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope!!.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        // click outside core text field area
        rule.onNodeWithTag("label", useUnmergedTree = true).performTouchInput { click(Offset.Zero) }

        rule.runOnIdle {
            // Not asserting total size as we have other interactions here too
            assertThat(interactions.filterIsInstance<FocusInteraction.Focus>()).hasSize(1)
        }
    }

    @Test
    fun textField_stringOverload_callsOnValueChange_whenTextChange() {
        var onValueChangeCalled = false

        rule.setContent {
            val state = remember { mutableStateOf("abc") }
            BasicTextField(
                modifier = Modifier.testTag(Tag),
                value = state.value,
                onValueChange = {
                    onValueChangeCalled = true
                    state.value = it
                },
            )
        }

        rule.onNodeWithTag(Tag).performClick().performTextInputSelection(TextRange(0, 0))

        // reset
        rule.runOnIdle { onValueChangeCalled = false }

        // change selection
        rule.onNodeWithTag(Tag).performTextInputSelection(TextRange(1, 1))

        rule.runOnIdle { assertThat(onValueChangeCalled).isFalse() }

        // change text
        rule.onNodeWithTag(Tag).performTextInput("d")

        rule.runOnIdle { assertThat(onValueChangeCalled).isTrue() }
    }

    @Test
    @Ignore // b/184750119
    fun textField_callsOnValueChange_whenTextFieldValueChange() {
        var onValueChangeCalled = false
        var lastSeenTextFieldValue = TextFieldValue()

        rule.setContent {
            val state = remember { mutableStateOf(TextFieldValue("abc")) }
            BasicTextField(
                modifier = Modifier.testTag(Tag),
                value = state.value,
                onValueChange = {
                    onValueChangeCalled = true
                    lastSeenTextFieldValue = it
                    state.value = it
                },
            )
        }

        rule.onNodeWithTag(Tag).performClick().performTextInputSelection(TextRange(0, 0))

        // reset flag since click might change selection
        rule.runOnIdle { onValueChangeCalled = false }

        rule.onNodeWithTag(Tag).performTextInputSelection(TextRange(1, 1))

        // selection changed
        rule.runOnIdle {
            assertWithMessage("$lastSeenTextFieldValue").that(onValueChangeCalled).isTrue()
            // reset flag
            onValueChangeCalled = false
        }
        rule.waitUntil { onValueChangeCalled == false }

        // set selection to same value, no change should occur
        rule.onNodeWithTag(Tag).performTextInputSelection(TextRange(1, 1))

        rule.runOnIdle {
            assertWithMessage("$lastSeenTextFieldValue").that(onValueChangeCalled).isFalse()
        }

        rule.onNodeWithTag(Tag).performTextInput("d")

        rule.runOnIdle {
            assertWithMessage("$lastSeenTextFieldValue").that(onValueChangeCalled).isTrue()
        }
    }

    @Test
    fun textField_stringOverload_doesNotCallOnValueChange_whenCompositionUpdatesOnly() {
        var callbackCounter = 0

        rule.setContent {
            val focusManager = LocalFocusManager.current
            val text = remember { mutableStateOf("A") }

            BasicTextField(
                value = text.value,
                onValueChange = {
                    callbackCounter += 1
                    text.value = it

                    // causes TextFieldValue's composition clearing
                    focusManager.clearFocus(true)
                },
                modifier = Modifier.testTag("tag"),
            )
        }

        rule.onNodeWithTag("tag").performClick()
        rule.waitForIdle()

        rule.onNodeWithTag("tag").performTextClearance()

        rule.runOnIdle { assertThat(callbackCounter).isEqualTo(1) }
    }

    @Test
    fun textField_stringOverload_doesNotCallOnValueChange_ifSelectionInherentlyChanges() {
        var callbackCounter = 0
        val text = mutableStateOf("ABCDE")

        rule.setContent {
            BasicTextField(
                value = text.value,
                onValueChange = {
                    callbackCounter += 1
                    text.value = it
                },
                modifier = Modifier.testTag("tag"),
            )
        }

        rule.onNodeWithTag("tag").performTextInputSelection(TextRange(0, 4))
        rule.waitForIdle()

        text.value = ""
        rule.waitForIdle()

        text.value = "ABCDE"

        rule.runOnIdle { assertThat(callbackCounter).isEqualTo(0) }
    }

    @Test
    fun textField_stringOverload_doesNotCallOnValueChange_whenCompositionUpdatesOnly_semantics() {
        var callbackCounter = 0

        rule.setContent {
            val focusManager = LocalFocusManager.current
            val text = remember { mutableStateOf("A") }

            BasicTextField(
                value = text.value,
                onValueChange = {
                    callbackCounter += 1
                    text.value = it

                    // causes TextFieldValue's composition clearing
                    focusManager.clearFocus(true)
                },
                modifier = Modifier.testTag("tag"),
            )
        }

        rule.onNodeWithTag("tag").performClick()
        rule.waitForIdle()

        rule.onNodeWithTag("tag").performSemanticsAction(SemanticsActions.SetText) {
            it(AnnotatedString(""))
        }

        rule.runOnIdle { assertThat(callbackCounter).isEqualTo(1) }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun textField_textAlignCenter_defaultWidth() {
        val fontSize = 50
        val density = Density(1f, 1f)
        val textStyle =
            TextStyle(
                textAlign = TextAlign.Center,
                color = Color.Black,
                fontFamily = measureFontFamily,
                fontSize = fontSize.sp,
            )
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides density) {
                BasicTextField(
                    modifier = Modifier.testTag(Tag),
                    value = "H",
                    onValueChange = {},
                    textStyle = textStyle,
                    singleLine = true,
                )
            }
        }

        rule.waitForIdle()
        rule.onNodeWithTag(Tag).captureToImage().assertCentered(fontSize)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun textField_textAlignCenter_widthSmallerThanDefaultWidth() {
        val fontSize = 50
        val density = Density(1f, 1f)
        val textStyle =
            TextStyle(
                textAlign = TextAlign.Center,
                color = Color.Black,
                fontFamily = measureFontFamily,
                fontSize = fontSize.sp,
            )
        rule.setContent {
            val fontFamilyResolver = LocalFontFamilyResolver.current
            val defaultWidth =
                computeSizeForDefaultText(
                        style = textStyle,
                        density = density,
                        fontFamilyResolver = fontFamilyResolver,
                        maxLines = 1,
                    )
                    .width

            CompositionLocalProvider(LocalDensity provides density) {
                BasicTextField(
                    modifier = Modifier.testTag(Tag).width(defaultWidth.dp / 2),
                    value = "H",
                    onValueChange = {},
                    textStyle = textStyle,
                    singleLine = true,
                )
            }
        }

        rule.waitForIdle()
        rule.onNodeWithTag(Tag).captureToImage().assertCentered(fontSize)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun textField_textAlignCenter_widthLargerThanDefaultWidth() {
        val fontSize = 50
        val density = Density(1f, 1f)
        val textStyle =
            TextStyle(
                textAlign = TextAlign.Center,
                color = Color.Black,
                fontFamily = measureFontFamily,
                fontSize = fontSize.sp,
            )
        rule.setContent {
            val fontFamilyResolver = LocalFontFamilyResolver.current
            val defaultWidth =
                computeSizeForDefaultText(
                        style = textStyle,
                        density = density,
                        fontFamilyResolver = fontFamilyResolver,
                        maxLines = 1,
                    )
                    .width

            CompositionLocalProvider(LocalDensity provides density) {
                BasicTextField(
                    modifier = Modifier.testTag(Tag).width(defaultWidth.dp * 2),
                    value = "H",
                    onValueChange = {},
                    textStyle = textStyle,
                    singleLine = true,
                )
            }
        }

        rule.waitForIdle()
        rule.onNodeWithTag(Tag).captureToImage().assertCentered(fontSize)
    }

    @Test
    fun textField_cursorAppearsOnTouch_thenDisappearsWhenTyping() {
        rule.setTextFieldTestContent {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                BasicTextField(
                    value = "test",
                    onValueChange = {},
                    // center so that the click places the cursor in the middle of the word
                    textStyle = TextStyle(textAlign = TextAlign.Center),
                    modifier = Modifier.testTag(Tag),
                )
            }
        }

        rule.onNode(isSelectionHandle(Handle.Cursor)).assertDoesNotExist()

        rule.onNodeWithTag(Tag).performTouchInput { click() }
        rule.onNode(isSelectionHandle(Handle.Cursor)).assertExists()

        rule.onNodeWithTag(Tag).performTextInput("t")
        rule.onNode(isSelectionHandle(Handle.Cursor)).assertDoesNotExist()
    }

    @Test
    fun whenSelectedTextIsRemoved_SelectionCoerces() {
        val textFieldValue = mutableStateOf("Hello")
        rule.setContent {
            BasicTextField(
                value = textFieldValue.value,
                onValueChange = { textFieldValue.value = it },
                modifier = Modifier.testTag(Tag).wrapContentSize(),
            )
        }
        val textNode = rule.onNodeWithTag(Tag)
        textNode.performTextInputSelection(TextRange(0, 4))
        textFieldValue.value = ""

        rule.waitForIdle()
        val expected = TextRange(0, 0)
        val actual =
            textNode.fetchSemanticsNode().config.getOrNull(SemanticsProperties.TextSelectionRange)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun whenPartiallySelectedTextIsRemoved_SelectionCoercesToEdges() {
        val textFieldValue = mutableStateOf("Hello World!")
        inputMethodInterceptor.setContent {
            BasicTextField(
                value = textFieldValue.value,
                onValueChange = { textFieldValue.value = it },
                modifier = Modifier.testTag(Tag).wrapContentSize(),
            )
        }
        val textNode = rule.onNodeWithTag(Tag)
        textNode.performTextInputSelection(TextRange(2, 8))
        textFieldValue.value = "Hello"

        rule.waitForIdle()

        val expected = TextRange(2, 5)
        val actual =
            textNode.fetchSemanticsNode().config.getOrNull(SemanticsProperties.TextSelectionRange)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun whenSelectedTextIsRemoved_addedLater_SelectionDoesNotRemain() {
        val textFieldValue = mutableStateOf("Hello")
        rule.setContent {
            BasicTextField(
                value = textFieldValue.value,
                onValueChange = { textFieldValue.value = it },
                modifier = Modifier.testTag(Tag).wrapContentSize(),
            )
        }
        val textNode = rule.onNodeWithTag(Tag)
        textNode.performTextInputSelection(TextRange(0, 4))
        rule.waitForIdle()

        textFieldValue.value = ""
        rule.waitForIdle()

        textNode.assertTextEquals("")

        textFieldValue.value = "Hello"
        rule.waitForIdle()

        val expected = TextRange.Zero
        val actual =
            textNode.fetchSemanticsNode().config.getOrNull(SemanticsProperties.TextSelectionRange)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun whenSelectedTextIsPartiallyRemoved_addedLater_SelectionRemainsPartially() {
        val textFieldValue = mutableStateOf("Hello")
        inputMethodInterceptor.setContent {
            BasicTextField(
                value = textFieldValue.value,
                onValueChange = { textFieldValue.value = it },
                modifier = Modifier.testTag(Tag).wrapContentSize(),
            )
        }
        val textNode = rule.onNodeWithTag(Tag)
        textNode.performTextInputSelection(TextRange(0, 4))
        rule.waitForIdle()

        textFieldValue.value = "He"
        rule.waitForIdle()

        textNode.assertTextEquals("He")

        textFieldValue.value = "Hello"
        rule.waitForIdle()

        val expected = TextRange(0, 2)
        val actual =
            textNode.fetchSemanticsNode().config.getOrNull(SemanticsProperties.TextSelectionRange)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun whenSelectedTextIsRemovedByIME_SelectionDoesNotRevert() {
        // hard to find a descriptive name. Take a look at
        // `whenSelectedTextIsRemoved_addedLater_SelectionRemains` to understand this case better.

        val textFieldValue = mutableStateOf("Hello")
        rule.setContent {
            BasicTextField(
                value = textFieldValue.value,
                onValueChange = { textFieldValue.value = it },
                modifier = Modifier.testTag(Tag).wrapContentSize(),
            )
        }
        val textNode = rule.onNodeWithTag(Tag)
        textNode.performTextClearance()

        rule.waitForIdle()
        textNode.assertTextEquals("")
        val selection =
            textNode.fetchSemanticsNode().config.getOrNull(SemanticsProperties.TextSelectionRange)
        assertThat(selection).isEqualTo(TextRange(0))

        textFieldValue.value = "Hello"

        rule.waitForIdle()
        val actual =
            textNode.fetchSemanticsNode().config.getOrNull(SemanticsProperties.TextSelectionRange)
        assertThat(actual).isEqualTo(TextRange(0))
    }

    // Regression test for b/311834126
    @Test
    fun whenPastingTextThatIncreasesEndOffset_noCrashAndCursorAtEndOfPastedText() = runTest {
        val longText = "Text".repeat(4)
        val shortText = "Text".repeat(2)

        val mockedNativeClipboard = mock<NativeClipboard>()
        var tfv by mutableStateOf(TextFieldValue(shortText))
        val clipboard =
            object : Clipboard {
                var contents: AnnotatedString? = null

                override suspend fun getClipEntry(): ClipEntry? {
                    return contents?.toClipEntry()
                }

                override suspend fun setClipEntry(clipEntry: ClipEntry?) {
                    contents = clipEntry?.readAnnotatedString()
                }

                override val nativeClipboard: NativeClipboard
                    get() = mockedNativeClipboard
            }
        rule.setTextFieldTestContent {
            CompositionLocalProvider(LocalClipboard provides clipboard) {
                BasicTextField(
                    value = tfv,
                    onValueChange = { tfv = it },
                    modifier = Modifier.testTag(Tag),
                )
            }
        }
        clipboard.setClipEntry(AnnotatedString(longText).toClipEntry())
        rule.waitForIdle()

        val node = rule.onNodeWithTag(Tag)
        node.performTouchInput { longClick(center) }
        rule.waitForIdle()

        node.performSemanticsAction(SemanticsActions.PasteText) { it() }
        rule.waitForIdle()

        val expectedTfv = TextFieldValue(text = longText, selection = TextRange(longText.length))
        assertThat(tfv.text).isEqualTo(expectedTfv.text)
        assertThat(tfv.selection).isEqualTo(expectedTfv.selection)
    }

    @Test
    fun decorationBoxIntrinsics() {
        var size: IntSize? = null
        var dividerSize: IntSize? = null
        rule.setContent {
            Box(Modifier.onGloballyPositioned { size = it.size }) {
                Row(Modifier.height(IntrinsicSize.Min)) {
                    Box(
                        modifier =
                            Modifier.fillMaxHeight()
                                .width(10.dp)
                                .onGloballyPositioned { dividerSize = it.size }
                                .drawBehind { drawRect(Color.Gray) }
                    )
                    BasicTextField(
                        value = "",
                        onValueChange = {},
                        decorationBox = { content ->
                            Box(
                                modifier = Modifier.border(1.dp, Color.Magenta).padding(4.dp),
                                propagateMinConstraints = true,
                            ) {
                                content()
                            }
                        },
                    )
                }
            }
        }

        rule.runOnIdle {
            assertThat(dividerSize).isNotNull()
            assertThat(size).isNotNull()
            assertThat(dividerSize!!.height).isEqualTo(size!!.height)
        }
    }

    @Test
    fun decorationBox_togglingInnerTextField() {
        var value by mutableStateOf("")
        val decorationTag = "decorationTag"
        rule.setContent {
            Column {
                BasicTextField(
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier.fillMaxWidth().testTag(Tag),
                    decorationBox = {
                        // the core text field is at the very bottom
                        if (value.isEmpty()) {
                            BasicText("test", modifier = Modifier.testTag(decorationTag))
                        } else {
                            it()
                        }
                    },
                )
            }
        }

        rule.onNodeWithTag(decorationTag, true).assertExists()

        rule.onNode(hasSetTextAction()).performTextInput("hello")

        rule.onNodeWithTag(decorationTag, true).assertDoesNotExist()

        rule.onNode(hasSetTextAction()).performTextClearance()

        rule.onNodeWithTag(decorationTag, true).assertExists()

        rule.onNode(hasSetTextAction()).performTextInput("hello2")

        rule.onNodeWithTag(decorationTag, true).assertDoesNotExist()
    }

    // Regression test for b/311007530
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun whenToggleReadOnly_onEditedTextField_noChangeNorCrash() {
        val tag = "tag"

        val text = "text"
        val tfv = TextFieldValue(text = text, selection = TextRange(text.length))

        val textAfterBackspace = text.run { substring(0, length - 1) }
        val tfvAfterBackspace =
            TextFieldValue(
                text = textAfterBackspace,
                selection = TextRange(textAfterBackspace.length),
            )

        var value by mutableStateOf(tfv)
        var readOnly by mutableStateOf(false)
        rule.setTextFieldTestContent {
            BasicTextField(
                value = value,
                onValueChange = { value = it },
                readOnly = readOnly,
                modifier = Modifier.testTag(tag),
            )
        }
        val node = rule.onNodeWithTag(tag)
        // gain focus and place cursor at end of text
        node.performTouchInput { click(centerRight - Offset(5f, 0f)) }
        rule.waitForIdle()
        assertThat(value.text).isEqualTo(tfv.text)
        assertThat(value.selection).isEqualTo(tfv.selection)

        node.performKeyInput { keyDown(Key.Backspace) }
        rule.waitForIdle()
        assertThat(value.text).isEqualTo(tfvAfterBackspace.text)
        assertThat(value.selection).isEqualTo(tfvAfterBackspace.selection)

        rule.runOnUiThread { readOnly = true }
        rule.waitForIdle()
        assertThat(value.text).isEqualTo(tfvAfterBackspace.text)
        assertThat(value.selection).isEqualTo(tfvAfterBackspace.selection)

        rule.runOnUiThread { readOnly = false }
        rule.waitForIdle()
        assertThat(value.text).isEqualTo(tfvAfterBackspace.text)
        assertThat(value.selection).isEqualTo(tfvAfterBackspace.selection)
    }

    // Regression test for b/322835187
    @Test
    fun whenToggleReadOnly_onTypedTextField_noChangeNorCrash() {
        val tag = "tag"

        var value by mutableStateOf(TextFieldValue())
        var readOnly by mutableStateOf(false)
        rule.setTextFieldTestContent {
            BasicTextField(
                value = value,
                onValueChange = { value = it },
                readOnly = readOnly,
                modifier = Modifier.testTag(tag),
            )
        }
        rule.onNodeWithTag(tag).requestFocus().performTextInput("Hello")

        rule.runOnIdle {
            assertThat(value.text).isEqualTo("Hello")
            assertThat(value.selection).isEqualTo(TextRange(5))
        }

        rule.runOnUiThread { readOnly = true }
        rule.waitForIdle()
        rule.runOnUiThread { readOnly = false }

        rule.runOnIdle {
            assertThat(value.text).isEqualTo("Hello")
            assertThat(value.selection).isEqualTo(TextRange(5))
        }
    }

    // Regression test for b/322851615
    @Test
    fun whenRemeasureInnerTextField_andNotDecorationBox_firstTapPlacesCursorAtCorrectOffset() {
        var outerTextFieldRecomposed: Boolean
        val innerDirection = mutableStateOf(LayoutDirection.Ltr)
        val tfvState = mutableStateOf(TextFieldValue(text = "text"))
        rule.setTextFieldTestContent {
            outerTextFieldRecomposed = true
            BasicTextField(
                value = tfvState.value,
                onValueChange = { tfvState.value = it },
                modifier = Modifier.testTag(Tag),
                decorationBox = { innerTextField ->
                    CompositionLocalProvider(
                        value = LocalLayoutDirection provides innerDirection.value,
                        content = innerTextField,
                    )
                },
            )
        }

        // For this test to work, we need to re-measure the inner part of the text field without
        // causing a re-layout of the core text root box. We do this by changing the layout
        // direction within the decoration box which will cause a re-layout of only the inner
        // text field.
        outerTextFieldRecomposed = false
        innerDirection.value = LayoutDirection.Rtl
        rule.waitForIdle()
        innerDirection.value = LayoutDirection.Ltr
        rule.waitForIdle()

        // Failing here indicates that the strategy in use to put the text field into a
        // state that could cause the regression has failed, making this test pointless.
        // If a CL makes this test fail in pre-submit, this test can likely be marked as ignored,
        // and then we can later come up with a new way to make this test, if necessary.
        assertWithMessage("Outer text field should not recompose.")
            .that(outerTextFieldRecomposed)
            .isFalse()

        val targetOffset = 2

        val node = rule.onNodeWithTag(Tag)
        val textLayoutResult = node.fetchTextLayoutResult()
        val offset = textLayoutResult.getBoundingBox(targetOffset).centerLeft

        node.performTouchInput { click(offset) }
        rule.waitForIdle()

        assertThat(tfvState.value.selection).isEqualTo(TextRange(targetOffset))
    }

    @Test
    fun doesNotStopBeingTextEditor_whenWindowFocusLost() {
        var windowFocus by mutableStateOf(true)
        inputMethodInterceptor.setContent {
            CompositionLocalProvider(
                LocalWindowInfo provides
                    object : WindowInfo {
                        override val isWindowFocused: Boolean
                            get() = windowFocus
                    }
            ) {
                BasicTextField("", {}, Modifier.testTag(Tag))
            }
        }
        rule.onNodeWithTag(Tag).requestFocus()
        rule.runOnIdle { windowFocus = false }
        inputMethodInterceptor.assertSessionActive()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun singleLineTextField_enterIsNotConsumed_withDefaultKeyboardAction() {
        var keyDownReceived = false
        var keyUpReceived = false
        rule.setTextFieldTestContent {
            var value by remember { mutableStateOf("") }
            Box(
                Modifier.onKeyEvent {
                    if (it.key == Key.Enter) {
                        when (it.type) {
                            KeyEventType.KeyDown -> keyDownReceived = true
                            KeyEventType.KeyUp -> keyUpReceived = true
                        }
                    }
                    false
                }
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = { value = it },
                    singleLine = true,
                    modifier = Modifier.testTag(Tag),
                )
            }
        }
        rule.onNodeWithTag(Tag).apply {
            requestFocus()
            performKeyInput { pressKey(Key.Enter) }
        }

        assertTrue(keyDownReceived)
        assertTrue(keyUpReceived)
    }
}

private fun SemanticsNodeInteraction.assertEditableTextEquals(
    value: String
): SemanticsNodeInteraction =
    assert(
        SemanticsMatcher("${SemanticsProperties.EditableText.name} = '$value'") {
            it.config.getOrNull(SemanticsProperties.EditableText)?.text.equals(value)
        }
    )

private fun ImageBitmap.assertCentered(excludedWidth: Int) {
    val pixel = toPixelMap()
    for (y in 0 until height) {
        for (x in 0 until (width - excludedWidth) / 2) {
            val leftPixel = pixel[x, y]
            pixel.assertPixelColor(leftPixel, width - 1 - x, y)
        }
    }
}
