/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.foundation.text.selection

import android.os.Build
import androidx.compose.foundation.internal.ClipboardUtils
import androidx.compose.foundation.text.HandleState
import androidx.compose.foundation.text.LegacyTextFieldState
import androidx.compose.foundation.text.TextDelegate
import androidx.compose.foundation.text.TextLayoutResultProxy
import androidx.compose.foundation.text.contextmenu.test.ContextMenuFlagFlipperRunner
import androidx.compose.foundation.text.contextmenu.test.ContextMenuFlagSuppress
import androidx.compose.foundation.text.contextmenu.test.FakeToolbarRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.MultiParagraph
import androidx.compose.ui.text.TextLayoutInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.packFloats
import androidx.compose.ui.util.packInts
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.stubbing.Answer

@RunWith(ContextMenuFlagFlipperRunner::class)
class TextFieldSelectionManagerTest {
    private val text = "Hello World"
    private val textAnnotatedString = AnnotatedString(text)
    private val density = Density(density = 1f)
    private val offsetMapping = OffsetMapping.Identity
    private val maxLines = 2
    private var value = TextFieldValue(text)

    private var onValueChangeInvocationCount = 0
    private val onValueChangeLambda: (TextFieldValue) -> Unit = { newValue ->
        onValueChangeInvocationCount++
        value = newValue
    }

    private lateinit var state: LegacyTextFieldState

    private val dragBeginPosition = Offset.Zero
    private val dragDistance = Offset(300f, 15f)
    private val beginOffset = 0
    private val dragOffset = text.indexOf('r')
    private val fakeTextRange = TextRange(0, "Hello".length)
    private val dragTextRange = TextRange("Hello".length + 1, text.length)
    private val layoutResult: TextLayoutResult = mock()
    private val layoutResultProxy: TextLayoutResultProxy = mock()
    private lateinit var manager: TextFieldSelectionManager

    private val clipboard = mock<Clipboard>()
    private val textToolbar = mock<TextToolbar>()
    private val hapticFeedback = mock<HapticFeedback>()
    private val focusRequester = mock<FocusRequester>()
    private val multiParagraph = mock<MultiParagraph>()

    private val fakeToolbarRequester = FakeToolbarRequester()

    @Before
    fun setup() {
        manager = TextFieldSelectionManager()
        manager.offsetMapping = offsetMapping
        manager.onValueChange = onValueChangeLambda
        manager.value = value
        manager.clipboard = clipboard
        manager.textToolbar = textToolbar
        manager.hapticFeedBack = hapticFeedback
        manager.focusRequester = focusRequester
        manager.coroutineScope = null
        manager.toolbarRequester = fakeToolbarRequester
        onValueChangeInvocationCount = 0

        whenever(layoutResult.layoutInput)
            .thenReturn(
                TextLayoutInput(
                    text = textAnnotatedString,
                    style = TextStyle.Default,
                    placeholders = mock(),
                    maxLines = maxLines,
                    softWrap = true,
                    overflow = TextOverflow.Ellipsis,
                    density = density,
                    layoutDirection = LayoutDirection.Ltr,
                    fontFamilyResolver = mock(),
                    constraints = Constraints(),
                )
            )

        whenever(layoutResult.lineCount).thenReturn(maxLines)
        whenever(layoutResult.getWordBoundary(beginOffset))
            .thenAnswer(TextRangeAnswer(fakeTextRange))
        whenever(layoutResult.getWordBoundary(dragOffset))
            .thenAnswer(TextRangeAnswer(dragTextRange))
        whenever(layoutResult.getBidiRunDirection(any())).thenReturn(ResolvedTextDirection.Ltr)
        whenever(layoutResult.getBoundingBox(any())).thenReturn(Rect.Zero)
        whenever(layoutResult.multiParagraph).thenReturn(multiParagraph)
        // left or right handle drag
        whenever(layoutResult.getOffsetForPosition(dragBeginPosition)).thenReturn(beginOffset)
        whenever(layoutResult.getOffsetForPosition(dragBeginPosition + dragDistance))
            .thenReturn(dragOffset)
        // touch drag
        whenever(layoutResultProxy.getOffsetForPosition(dragBeginPosition, false))
            .thenReturn(beginOffset)
        whenever(layoutResultProxy.getOffsetForPosition(dragBeginPosition, true))
            .thenReturn(beginOffset)
        whenever(layoutResultProxy.getOffsetForPosition(dragBeginPosition + dragDistance, false))
            .thenReturn(dragOffset)
        whenever(layoutResultProxy.getOffsetForPosition(dragBeginPosition + dragDistance, true))
            .thenReturn(dragOffset)

        whenever(
                layoutResultProxy.translateInnerToDecorationCoordinates(matchesOffset(dragDistance))
            )
            .thenAnswer(OffsetAnswer(dragDistance))

        whenever(layoutResultProxy.value).thenReturn(layoutResult)

        val textDelegate = mock<TextDelegate> { on { this.text }.thenReturn(textAnnotatedString) }

        state =
            LegacyTextFieldState(
                textDelegate = textDelegate,
                recomposeScope = mock(),
                keyboardController = null,
            )
        state.layoutResult = layoutResultProxy
        manager.state = state
        whenever(state.textDelegate.density).thenReturn(density)
    }

    @Test
    fun TextFieldSelectionManager_init() {
        assertThat(manager.offsetMapping).isEqualTo(offsetMapping)
        val textFieldValue = TextFieldValue(text = text)
        manager.onValueChange.invoke(textFieldValue)
        assertThat(onValueChangeInvocationCount).isEqualTo(1)
        assertThat(value).isEqualTo(textFieldValue)
        assertThat(manager.state).isEqualTo(state)
        assertThat(manager.value).isEqualTo(value)
    }

    @Test
    fun TextFieldSelectionManager_touchSelectionObserver_onLongPress() {
        whenever(layoutResultProxy.isPositionOnText(dragBeginPosition)).thenReturn(true)

        manager.touchSelectionObserver.onStart(dragBeginPosition)

        assertThat(state.handleState).isEqualTo(HandleState.None)
        assertThat(state.showFloatingToolbar).isFalse()
        assertThat(value.selection).isEqualTo(fakeTextRange)
        verify(hapticFeedback, times(1)).performHapticFeedback(HapticFeedbackType.TextHandleMove)

        verify(focusRequester, times(1)).requestFocus()
    }

    @Test
    fun TextFieldSelectionManager_touchSelectionObserver_onLongPress_blank() {
        // Setup
        val fakeLineEnd = text.length
        whenever(layoutResultProxy.isPositionOnText(dragBeginPosition)).thenReturn(false)
        whenever(layoutResultProxy.getOffsetForPosition(dragBeginPosition)).thenReturn(fakeLineEnd)

        // Act
        manager.touchSelectionObserver.onStart(dragBeginPosition)

        // Assert
        assertThat(state.handleState).isEqualTo(HandleState.None)
        assertThat(state.showFloatingToolbar).isFalse()
        assertThat(value.selection).isEqualTo(TextRange(fakeLineEnd))
        verify(hapticFeedback, times(1)).performHapticFeedback(HapticFeedbackType.TextHandleMove)

        verify(focusRequester, times(1)).requestFocus()
    }

    @Test
    fun TextFieldSelectionManager_touchSelectionObserver_onDrag() {
        whenever(layoutResultProxy.isPositionOnText(dragBeginPosition)).thenReturn(true)

        manager.touchSelectionObserver.onStart(dragBeginPosition)
        manager.touchSelectionObserver.onDrag(dragDistance)

        assertThat(state.handleState).isEqualTo(HandleState.None)
        assertThat(value.selection).isEqualTo(TextRange(0, text.length))
        assertThat(state.showFloatingToolbar).isFalse()
        verify(hapticFeedback, times(2)).performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    @Test
    fun TextFieldSelectionManager_touchSelectionObserver_onStop() {
        whenever(layoutResultProxy.isPositionOnText(dragBeginPosition)).thenReturn(true)

        manager.touchSelectionObserver.onStart(dragBeginPosition)
        manager.touchSelectionObserver.onDrag(dragDistance)
        manager.value = value
        manager.touchSelectionObserver.onStop()

        assertThat(state.handleState).isEqualTo(HandleState.Selection)
        assertThat(value.selection).isEqualTo(TextRange(0, text.length))
        assertThat(state.showFloatingToolbar).isTrue()
        verify(hapticFeedback, times(2)).performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    @Test
    fun TextFieldSelectionManager_handleDragObserver_onDown_startHandle() {
        manager.handleDragObserver(isStartHandle = true).onDown(Offset.Zero)

        assertThat(manager.draggingHandle).isNotNull()
        assertThat(state.showFloatingToolbar).isFalse()
        assertThat(onValueChangeInvocationCount).isEqualTo(0)
        verify(hapticFeedback, times(0)).performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    @Test
    fun TextFieldSelectionManager_handleDragObserver_onDown_endHandle() {
        manager.handleDragObserver(isStartHandle = false).onDown(Offset.Zero)

        assertThat(manager.draggingHandle).isNotNull()
        assertThat(state.showFloatingToolbar).isFalse()
        assertThat(onValueChangeInvocationCount).isEqualTo(0)
        verify(hapticFeedback, times(0)).performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    @Test
    fun TextFieldSelectionManager_handleDragObserver_onDrag_startHandle() {
        manager.value = TextFieldValue(text = text, selection = TextRange(0, "Hello".length))

        manager.handleDragObserver(isStartHandle = true).onDrag(dragDistance)

        assertThat(state.showFloatingToolbar).isFalse()
        assertThat(value.selection).isEqualTo(TextRange(text.length, "Hello".length))
        verify(hapticFeedback, times(1)).performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    @Test
    fun TextFieldSelectionManager_handleDragObserver_onDrag_endHandle() {
        manager.value = TextFieldValue(text = text, selection = TextRange(0, "Hello".length))

        manager.handleDragObserver(isStartHandle = false).onDrag(dragDistance)

        assertThat(state.showFloatingToolbar).isFalse()
        assertThat(value.selection).isEqualTo(TextRange(0, dragOffset))
        verify(hapticFeedback, times(1)).performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    @Test
    fun TextFieldSelectionManager_handleDragObserver_onStop() {
        manager.handleDragObserver(false).onStart(Offset.Zero)
        manager.handleDragObserver(false).onDrag(Offset.Zero)

        verify(hapticFeedback, times(1)).performHapticFeedback(HapticFeedbackType.TextHandleMove)

        manager.handleDragObserver(false).onStop()

        assertThat(manager.draggingHandle).isNull()
        assertThat(state.showFloatingToolbar).isTrue()
        verify(hapticFeedback, times(1)).performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    @Test
    fun TextFieldSelectionManager_cursorDragObserver_onStart() {
        manager.cursorDragObserver().onStart(Offset.Zero)

        assertThat(manager.draggingHandle).isNotNull()
        assertThat(state.showFloatingToolbar).isFalse()
        assertThat(onValueChangeInvocationCount).isEqualTo(0)
        verify(hapticFeedback, times(0)).performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    @Test
    fun TextFieldSelectionManager_cursorDragObserver_onDrag() {
        manager.value = TextFieldValue(text = text, selection = TextRange(0, "Hello".length))

        manager.cursorDragObserver().onDrag(dragDistance)

        assertThat(state.showFloatingToolbar).isFalse()
        assertThat(value.selection).isEqualTo(TextRange(dragOffset, dragOffset))
        verify(hapticFeedback, times(1)).performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    @Test
    fun TextFieldSelectionManager_cursorDragObserver_onDrag_withVisualTransformation() {
        // there is a placeholder after every other char in the original value
        val offsetMapping =
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int) = 2 * offset

                override fun transformedToOriginal(offset: Int) = offset / 2
            }
        manager.value = TextFieldValue(text = "H*e*l*l*o* *W*o*r*l*d", selection = TextRange(0, 0))
        manager.offsetMapping = offsetMapping
        manager.visualTransformation = VisualTransformation { original ->
            TransformedText(
                AnnotatedString(original.indices.map { original[it] }.joinToString("*")),
                offsetMapping,
            )
        }

        manager.cursorDragObserver().onDrag(dragDistance)

        assertThat(value.selection).isEqualTo(TextRange(dragOffset / 2, dragOffset / 2))
    }

    @Test
    fun TextFieldSelectionManager_cursorDragObserver_onStop() {
        manager.handleDragObserver(false).onStart(Offset.Zero)
        manager.handleDragObserver(false).onDrag(Offset.Zero)

        verify(hapticFeedback, times(1)).performHapticFeedback(HapticFeedbackType.TextHandleMove)

        manager.cursorDragObserver().onStop()

        assertThat(manager.draggingHandle).isNull()
        assertThat(state.showFloatingToolbar).isFalse()
        verify(hapticFeedback, times(1)).performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    @ContextMenuFlagSuppress(suppressedFlagValue = true)
    @Test
    fun TextFieldSelectionManager_deselect() {
        whenever(textToolbar.status).thenReturn(TextToolbarStatus.Shown)
        manager.value = TextFieldValue(text = text, selection = TextRange(0, "Hello".length))

        manager.deselect()

        verify(textToolbar, times(1)).hide()
        assertThat(value.selection).isEqualTo(TextRange("Hello".length))
        assertThat(state.handleState).isEqualTo(HandleState.None)
    }

    @ContextMenuFlagSuppress(suppressedFlagValue = false)
    @Test
    fun TextFieldSelectionManager_deselect_newContextMenu() {
        manager.value = TextFieldValue(text = text, selection = TextRange(0, "Hello".length))

        manager.deselect()

        assertThat(fakeToolbarRequester.shown).isFalse()
        assertThat(fakeToolbarRequester.hideCount).isEqualTo(1)
        assertThat(value.selection).isEqualTo(TextRange("Hello".length))
        assertThat(state.handleState).isEqualTo(HandleState.None)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun autofill_selection_collapse() {
        manager.value = TextFieldValue(text = text, selection = TextRange(4, 4))
        val mockLambda: () -> Unit = mock()
        val manager = TextFieldSelectionManager().apply { requestAutofillAction = mockLambda }

        manager.autofill()

        verify(mockLambda, times(1)).invoke()
        assertThat(state.handleState).isEqualTo(HandleState.None)
    }

    @Test
    fun copy_selection_collapse() = runTestWithCoroutineScope {
        manager.value = TextFieldValue(text = text, selection = TextRange(4, 4))

        manager.copy()

        verify(clipboard, times(0)).setClipEntry(any())
    }

    @Test
    fun copy_selection_not_null() = runTestWithCoroutineScope {
        Mockito.mockStatic(ClipboardUtils::class.java).use { mockedClipboardUtils ->
            val clipEntry = mock<ClipEntry>()
            mockedClipboardUtils
                .`when`<ClipEntry> { ClipboardUtils.toClipEntry(any()) }
                .thenReturn(clipEntry)

            manager.value = TextFieldValue(text = text, selection = TextRange(0, "Hello".length))

            manager.copy()

            mockedClipboardUtils.verify { ClipboardUtils.toClipEntry(eq(AnnotatedString("Hello"))) }
            verify(clipboard, times(1)).setClipEntry(clipEntry)
            assertThat(value.selection).isEqualTo(TextRange("Hello".length, "Hello".length))
            assertThat(state.handleState).isEqualTo(HandleState.None)
        }
    }

    @Test
    fun copy_selection_reversed() = runTestWithCoroutineScope {
        Mockito.mockStatic(ClipboardUtils::class.java).use { mockedClipboardUtils ->
            val clipEntry = mock<ClipEntry>()
            mockedClipboardUtils
                .`when`<ClipEntry> { ClipboardUtils.toClipEntry(any()) }
                .thenReturn(clipEntry)

            manager.value =
                TextFieldValue(text = text, selection = TextRange("Hello".length, "He".length))

            manager.copy()

            mockedClipboardUtils.verify { ClipboardUtils.toClipEntry(eq(AnnotatedString("llo"))) }
            verify(clipboard, times(1)).setClipEntry(clipEntry)
            assertThat(value.selection).isEqualTo(TextRange("Hello".length, "Hello".length))
            assertThat(state.handleState).isEqualTo(HandleState.None)
        }
    }

    @Test
    fun paste_clipBoardManager_null() = runTestWithCoroutineScope {
        manager.clipboard = null

        manager.paste()

        assertThat(onValueChangeInvocationCount).isEqualTo(0)
    }

    @Test
    fun paste_clipBoardManager_empty() = runTestWithCoroutineScope {
        whenever(clipboard.getClipEntry()).thenReturn(null)

        manager.paste()

        assertThat(onValueChangeInvocationCount).isEqualTo(0)
    }

    @Test
    fun paste_clipBoardManager_not_empty() = runTestWithCoroutineScope {
        Mockito.mockStatic(ClipboardUtils::class.java).use { mockedClipboardUtils ->
            val clipEntry = mock<ClipEntry>()
            mockedClipboardUtils
                .`when`<AnnotatedString?> { ClipboardUtils.readAnnotatedString(clipEntry) }
                .thenReturn(AnnotatedString("Hello"))

            whenever(clipboard.getClipEntry()).thenReturn(clipEntry)
            manager.value =
                TextFieldValue(text = text, selection = TextRange("Hel".length, "Hello Wo".length))

            manager.paste()

            assertThat(value.text).isEqualTo("HelHellorld")
            assertThat(value.selection).isEqualTo(TextRange("Hello Wo".length, "Hello Wo".length))
            assertThat(state.handleState).isEqualTo(HandleState.None)
        }
    }

    @Test
    fun paste_selection_reversed() = runTestWithCoroutineScope {
        Mockito.mockStatic(ClipboardUtils::class.java).use { mockedClipboardUtils ->
            val clipEntry = mock<ClipEntry>()
            mockedClipboardUtils
                .`when`<AnnotatedString?> { ClipboardUtils.readAnnotatedString(clipEntry) }
                .thenReturn(AnnotatedString("i"))

            whenever(clipboard.getClipEntry()).thenReturn(clipEntry)
            manager.value =
                TextFieldValue(text = text, selection = TextRange("Hello".length, "H".length))

            manager.paste()

            assertThat(value.text).isEqualTo("Hi World")
            assertThat(value.selection).isEqualTo(TextRange("Hi".length, "Hi".length))
            assertThat(state.handleState).isEqualTo(HandleState.None)
        }
    }

    @Test
    fun cut_selection_collapse() = runTestWithCoroutineScope {
        manager.value = TextFieldValue(text = text, selection = TextRange(4, 4))

        manager.cut()

        verify(clipboard, times(0)).setClipEntry(any())
    }

    @Test
    fun cut_selection_not_null() = runTestWithCoroutineScope {
        Mockito.mockStatic(ClipboardUtils::class.java).use { mockedClipboardUtils ->
            val clipEntry = mock<ClipEntry>()
            mockedClipboardUtils
                .`when`<ClipEntry> { ClipboardUtils.toClipEntry(any()) }
                .thenReturn(clipEntry)

            manager.value =
                TextFieldValue(
                    text = text + text,
                    selection = TextRange("Hello".length, text.length),
                )

            manager.cut()

            mockedClipboardUtils.verify {
                ClipboardUtils.toClipEntry(eq(AnnotatedString(" World")))
            }

            verify(clipboard, times(1)).setClipEntry(clipEntry)
            assertThat(value.text).isEqualTo("HelloHello World")
            assertThat(value.selection).isEqualTo(TextRange("Hello".length, "Hello".length))
            assertThat(state.handleState).isEqualTo(HandleState.None)
        }
    }

    @Test
    fun cut_selection_reversed() = runTestWithCoroutineScope {
        Mockito.mockStatic(ClipboardUtils::class.java).use { mockedClipboardUtils ->
            val clipEntry = mock<ClipEntry>()
            mockedClipboardUtils
                .`when`<ClipEntry> { ClipboardUtils.toClipEntry(any()) }
                .thenReturn(clipEntry)

            manager.value =
                TextFieldValue(text = text, selection = TextRange("Hello".length, "He".length))

            manager.cut()

            mockedClipboardUtils.verify { ClipboardUtils.toClipEntry(eq(AnnotatedString("llo"))) }

            verify(clipboard, times(1)).setClipEntry(clipEntry)
            assertThat(value.text).isEqualTo("He World")
            assertThat(value.selection).isEqualTo(TextRange("He".length, "He".length))
            assertThat(state.handleState).isEqualTo(HandleState.None)
        }
    }

    @Test
    fun selectAll() {
        manager.value = TextFieldValue(text = text, selection = TextRange(0))

        manager.selectAll()

        assertThat(value.selection).isEqualTo(TextRange(0, text.length))
        assertThat(manager.state).isNotNull()
        val state = manager.state!!
        assertThat(state.handleState).isEqualTo(HandleState.Selection)
        assertThat(state.showFloatingToolbar).isEqualTo(true)
    }

    @Test
    fun selectAll_whenPartiallySelected() {
        manager.value = TextFieldValue(text = text, selection = TextRange(0, 5))

        manager.selectAll()

        assertThat(value.selection).isEqualTo(TextRange(0, text.length))
        assertThat(manager.state).isNotNull()
        val state = manager.state!!
        assertThat(state.handleState).isEqualTo(HandleState.Selection)
        assertThat(state.showFloatingToolbar).isEqualTo(true)
    }

    @ContextMenuFlagSuppress(suppressedFlagValue = true)
    @Test
    fun showSelectionToolbar_trigger_textToolbar_showMenu_noText_inClipboard_not_show_paste() =
        runTestWithCoroutineScope {
            Mockito.mockStatic(ClipboardUtils::class.java).use { mockedClipboardUtils ->
                mockedClipboardUtils
                    .`when`<Boolean> { ClipboardUtils.hasText(clipboard) }
                    .thenReturn(false)

                manager.value =
                    TextFieldValue(
                        text = text + text,
                        selection = TextRange("Hello".length, text.length),
                    )

                manager.showSelectionToolbar()

                verify(textToolbar, times(1))
                    .showMenu(any(), any(), isNull(), any(), anyOrNull(), isNull())
            }
        }

    @ContextMenuFlagSuppress(suppressedFlagValue = true)
    @Test
    fun showSelectionToolbar_trigger_textToolbar_showMenu_hasText_inClipboard_show_paste() =
        runTestWithCoroutineScope {
            Mockito.mockStatic(ClipboardUtils::class.java).use { mockedClipboardUtils ->
                mockedClipboardUtils
                    .`when`<Boolean> { ClipboardUtils.hasText(clipboard) }
                    .thenReturn(true)

                manager.value =
                    TextFieldValue(
                        text = text + text,
                        selection = TextRange("Hello".length, text.length),
                    )

                manager.showSelectionToolbar()

                verify(textToolbar, times(1))
                    .showMenu(anyOrNull(), anyOrNull(), any(), anyOrNull(), anyOrNull(), isNull())
            }
        }

    @ContextMenuFlagSuppress(suppressedFlagValue = true)
    @Test
    fun showSelectionToolbar_trigger_textToolbar_showMenu_selection_collapse_not_show_copy_cut() =
        runTestWithCoroutineScope {
            Mockito.mockStatic(ClipboardUtils::class.java).use { mockedClipboardUtils ->
                val clipEntry = mock<ClipEntry>()
                mockedClipboardUtils
                    .`when`<AnnotatedString?> { ClipboardUtils.readAnnotatedString(clipEntry) }
                    .thenReturn(AnnotatedString(text))
                mockedClipboardUtils
                    .`when`<Boolean> { ClipboardUtils.hasText(clipboard) }
                    .thenReturn(true)

                whenever(clipboard.getClipEntry()).thenReturn(clipEntry)
                manager.value = TextFieldValue(text = text + text, selection = TextRange(0, 0))

                manager.showSelectionToolbar()

                verify(textToolbar, times(1))
                    .showMenu(any(), isNull(), any(), isNull(), anyOrNull(), any())
            }
        }

    @ContextMenuFlagSuppress(suppressedFlagValue = true)
    @Test
    fun showSelectionToolbar_trigger_textToolbar_showMenu_no_text_show_paste_only() =
        runTestWithCoroutineScope {
            Mockito.mockStatic(ClipboardUtils::class.java).use { mockedClipboardUtils ->
                mockedClipboardUtils
                    .`when`<Boolean> { ClipboardUtils.hasText(clipboard) }
                    .thenReturn(true)
                manager.value = TextFieldValue()

                manager.showSelectionToolbar()

                verify(textToolbar, times(1))
                    .showMenu(any(), isNull(), any(), isNull(), isNull(), any())
            }
        }

    @ContextMenuFlagSuppress(suppressedFlagValue = true)
    @Test
    fun showSelectionToolbar_trigger_textToolbar_no_menu() = runTestWithCoroutineScope {
        Mockito.mockStatic(ClipboardUtils::class.java).use { mockedClipboardUtils ->
            mockedClipboardUtils
                .`when`<Boolean> { ClipboardUtils.hasText(clipboard) }
                .thenReturn(false)
            manager.value = TextFieldValue()

            manager.showSelectionToolbar()

            verify(textToolbar, times(1))
                .showMenu(any(), isNull(), isNull(), isNull(), isNull(), any())
        }
    }

    @ContextMenuFlagSuppress(suppressedFlagValue = true)
    @Test
    fun showSelectionToolbar_passwordTextField_not_show_copy_cut() = runTestWithCoroutineScope {
        Mockito.mockStatic(ClipboardUtils::class.java).use { mockedClipboardUtils ->
            mockedClipboardUtils
                .`when`<Boolean> { ClipboardUtils.hasText(clipboard) }
                .thenReturn(true)

            manager.visualTransformation = PasswordVisualTransformation()
            manager.value = TextFieldValue(text, TextRange(0, 5))

            manager.showSelectionToolbar()

            verify(textToolbar, times(1))
                .showMenu(any(), isNull(), any(), isNull(), anyOrNull(), isNull())
        }
    }

    @ContextMenuFlagSuppress(suppressedFlagValue = false)
    @Test
    fun showSelectionToolbar_trigger_textToolbar_showMenu_noText_inClipboard_not_show_paste_newContextMenu() =
        runTestWithCoroutineScope {
            manager.value =
                TextFieldValue(
                    text = text + text,
                    selection = TextRange("Hello".length, text.length),
                )

            manager.showSelectionToolbar()

            assertThat(fakeToolbarRequester.shown).isTrue()
            assertThat(fakeToolbarRequester.showCount).isEqualTo(1)
        }

    @ContextMenuFlagSuppress(suppressedFlagValue = false)
    @Test
    fun showSelectionToolbar_trigger_textToolbar_showMenu_hasText_inClipboard_show_paste_newContextMenu() =
        runTestWithCoroutineScope {
            Mockito.mockStatic(ClipboardUtils::class.java).use { mockedClipboardUtils ->
                mockedClipboardUtils
                    .`when`<Boolean> { ClipboardUtils.hasText(clipboard) }
                    .thenReturn(true)

                manager.value =
                    TextFieldValue(
                        text = text + text,
                        selection = TextRange("Hello".length, text.length),
                    )

                manager.showSelectionToolbar()

                assertThat(fakeToolbarRequester.shown).isTrue()
                assertThat(fakeToolbarRequester.showCount).isEqualTo(1)
            }
        }

    @ContextMenuFlagSuppress(suppressedFlagValue = false)
    @Test
    fun showSelectionToolbar_trigger_textToolbar_showMenu_selection_collapse_not_show_copy_cut_newContextMenu() =
        runTestWithCoroutineScope {
            Mockito.mockStatic(ClipboardUtils::class.java).use { mockedClipboardUtils ->
                mockedClipboardUtils
                    .`when`<Boolean> { ClipboardUtils.hasText(clipboard) }
                    .thenReturn(true)
                manager.value = TextFieldValue(text = text + text, selection = TextRange(0, 0))

                manager.showSelectionToolbar()

                assertThat(fakeToolbarRequester.shown).isTrue()
                assertThat(fakeToolbarRequester.showCount).isEqualTo(1)
            }
        }

    @ContextMenuFlagSuppress(suppressedFlagValue = false)
    @Test
    fun showSelectionToolbar_trigger_textToolbar_showMenu_no_text_show_paste_only_newContextMenu() =
        runTestWithCoroutineScope {
            Mockito.mockStatic(ClipboardUtils::class.java).use { mockedClipboardUtils ->
                mockedClipboardUtils
                    .`when`<Boolean> { ClipboardUtils.hasText(clipboard) }
                    .thenReturn(true)
                manager.value = TextFieldValue()

                manager.showSelectionToolbar()

                assertThat(fakeToolbarRequester.shown).isTrue()
                assertThat(fakeToolbarRequester.showCount).isEqualTo(1)
            }
        }

    @ContextMenuFlagSuppress(suppressedFlagValue = false)
    @Test
    fun showSelectionToolbar_trigger_textToolbar_no_menu_newContextMenu() =
        runTestWithCoroutineScope {
            whenever(clipboard.getClipEntry()).thenReturn(null)
            manager.value = TextFieldValue()

            manager.showSelectionToolbar()

            assertThat(fakeToolbarRequester.shown).isTrue()
            assertThat(fakeToolbarRequester.showCount).isEqualTo(1)
        }

    @ContextMenuFlagSuppress(suppressedFlagValue = false)
    @Test
    fun showSelectionToolbar_passwordTextField_not_show_copy_cut_newContextMenu() =
        runTestWithCoroutineScope {
            Mockito.mockStatic(ClipboardUtils::class.java).use { mockedClipboardUtils ->
                mockedClipboardUtils
                    .`when`<Boolean> { ClipboardUtils.hasText(clipboard) }
                    .thenReturn(true)

                manager.visualTransformation = PasswordVisualTransformation()
                manager.value = TextFieldValue(text, TextRange(0, 5))

                manager.showSelectionToolbar()

                assertThat(fakeToolbarRequester.shown).isTrue()
                assertThat(fakeToolbarRequester.showCount).isEqualTo(1)
            }
        }

    @Test
    fun isTextChanged_text_changed_return_true() {
        manager.touchSelectionObserver.onStart(dragBeginPosition)
        manager.value = TextFieldValue(text + text)

        assertThat(manager.isTextChanged()).isTrue()
    }

    @Test
    fun isTextChanged_text_unchange_return_false() {
        manager.touchSelectionObserver.onStart(dragBeginPosition)

        assertThat(manager.isTextChanged()).isFalse()
    }

    @Test
    fun getHandleLineHeight_valid() {
        val selection = TextRange(1, text.length - 1)
        manager.value = TextFieldValue(text = text, selection = selection)
        val selectionStartLineHeight = 10f
        val selectionEndLineHeight = 20f

        whenever(multiParagraph.getLineForOffset(selection.start)).thenReturn(0)
        whenever(multiParagraph.getLineForOffset(selection.end)).thenReturn(1)
        whenever(multiParagraph.getLineHeight(0)).thenReturn(selectionStartLineHeight)
        whenever(multiParagraph.getLineHeight(1)).thenReturn(selectionEndLineHeight)
        whenever(multiParagraph.getLineEnd(0)).thenReturn(selection.start + 1)
        whenever(multiParagraph.getLineEnd(1)).thenReturn(selection.end + 1)
        whenever(multiParagraph.lineCount).thenReturn(2)
        whenever(multiParagraph.maxLines).thenReturn(2)

        assertThat(manager.getHandleLineHeight(isStartHandle = true))
            .isEqualTo(selectionStartLineHeight)
        assertThat(manager.getHandleLineHeight(isStartHandle = false))
            .isEqualTo(selectionEndLineHeight)
    }

    @Test
    fun getHandleLineHeight_selection_out_of_lines_limit_return_zero() {
        val selection = TextRange(1, text.length - 1)
        manager.value = TextFieldValue(text = text, selection = selection)
        val selectionEndLineHeight = 20f

        whenever(multiParagraph.getLineForOffset(selection.start)).thenReturn(2)
        whenever(multiParagraph.getLineForOffset(selection.end)).thenReturn(3)
        whenever(multiParagraph.getLineHeight(any())).thenReturn(selectionEndLineHeight)
        whenever(multiParagraph.getLineEnd(2)).thenReturn(text.length)
        whenever(multiParagraph.getLineEnd(3)).thenReturn(text.length)
        whenever(multiParagraph.lineCount).thenReturn(2)
        whenever(multiParagraph.maxLines).thenReturn(2)

        assertThat(manager.getHandleLineHeight(isStartHandle = true)).isZero()
        assertThat(manager.getHandleLineHeight(isStartHandle = false)).isZero()
    }

    private fun runTestWithCoroutineScope(testBody: suspend TestScope.() -> Unit) = runTest {
        manager.coroutineScope = this
        testBody()
    }
}

// This class is a workaround for the bug that mockito can't stub a method returning inline class.
// (https://github.com/nhaarman/mockito-kotlin/issues/309).
internal class TextRangeAnswer(private val textRange: TextRange) : Answer<Any> {
    override fun answer(invocation: InvocationOnMock?): Any =
        packInts(textRange.start, textRange.end)
}

internal class OffsetAnswer(private val offset: Offset) : Answer<Any> {
    override fun answer(invocation: InvocationOnMock?): Any = packFloats(offset.x, offset.y)
}

// Another workaround for matching an Offset
// (https://github.com/nhaarman/mockito-kotlin/issues/309).
private fun matchesOffset(offset: Offset): Offset =
    Mockito.argThat { arg: Any ->
        if (arg is Long) {
            arg == packFloats(offset.x, offset.y)
        } else {
            arg == offset
        }
    } as Offset? ?: offset
