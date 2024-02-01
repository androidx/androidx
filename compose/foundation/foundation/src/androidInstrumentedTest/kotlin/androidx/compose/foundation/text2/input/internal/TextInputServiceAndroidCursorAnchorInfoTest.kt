/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.text2.input.internal

import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.CursorAnchorInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.InputConnection
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text2.input.ComposeInputMethodManagerTestRule
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.runtime.snapshots.ObserverHandle
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.PlatformTextInputSession
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFoundationApi::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
internal class TextInputServiceAndroidCursorAnchorInfoTest {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val composeImmRule = ComposeInputMethodManagerTestRule().apply {
        setFactory { composeImm }
    }

    private val context = InstrumentationRegistry.getInstrumentation().context
    private val defaultDensity = Density(density = 1f)
    private val builder = CursorAnchorInfo.Builder()
    private val androidMatrix = android.graphics.Matrix()
    private val reportedCursorAnchorInfos = mutableListOf<CursorAnchorInfo>()

    private val composeImm = object : ComposeInputMethodManager {
        override fun updateCursorAnchorInfo(info: CursorAnchorInfo) {
            reportedCursorAnchorInfos += info
        }

        override fun restartInput() {}
        override fun showSoftInput() {}
        override fun hideSoftInput() {}
        override fun updateExtractedText(token: Int, extractedText: ExtractedText) {}
        override fun updateSelection(
            selectionStart: Int,
            selectionEnd: Int,
            compositionStart: Int,
            compositionEnd: Int
        ) {
        }

        override fun sendKeyEvent(event: KeyEvent) {}
    }

    private lateinit var inputConnection: InputConnection
    private val session = object : PlatformTextInputSession {
        override val view = View(InstrumentationRegistry.getInstrumentation().context)
        override suspend fun startInputMethod(
            request: PlatformTextInputMethodRequest
        ): Nothing {
            inputConnection = request.createInputConnection(EditorInfo())
            awaitCancellation()
        }
    }

    private val windowOffset = Offset(12f, 34f)
    private val layoutState = TextLayoutState().apply {
        coreNodeCoordinates = TestLayoutCoordinates(windowOffset = windowOffset, isAttached = true)
        decoratorNodeCoordinates =
            TestLayoutCoordinates(windowOffset = windowOffset, isAttached = true)
    }

    @Test
    fun requestCursorUpdates_immediate() = runTest {
        val textFieldState = TextFieldState("abc", initialSelectionInChars = TextRange(2))
        backgroundScope.startFakeTextInputSession(textFieldState)

        // This requests a single update, immediately, with no future monitoring.
        inputConnection.requestCursorUpdates(InputConnection.CURSOR_UPDATE_IMMEDIATE)

        // Immediate update.
        val expectedInfo = builder.build(
            text = textFieldState.text,
            selection = textFieldState.text.selectionInChars,
            composition = textFieldState.text.compositionInChars,
            textLayoutResult = layoutState.layoutResult!!,
            matrix = getAndroidMatrix(windowOffset),
            innerTextFieldBounds = Rect.Zero,
            decorationBoxBounds = Rect.Zero,
        )
        assertThat(reportedCursorAnchorInfos).containsExactly(expectedInfo)
        reportedCursorAnchorInfos.clear()

        // Trigger new layout.
        layoutState.coreNodeCoordinates =
            TestLayoutCoordinates(windowOffset = Offset(67f, 89f), isAttached = true)

        // No further updates since monitoring is off
        assertThat(reportedCursorAnchorInfos).isEmpty()
    }

    @Test
    fun requestCursorUpdates_immediate_beforeUpdateTextLayoutResult() = runTest {
        val textFieldState = TextFieldState("abc", initialSelectionInChars = TextRange(2))
        val transformedState = TransformedTextFieldState(
            textFieldState = textFieldState,
            inputTransformation = null,
            codepointTransformation = null
        )

        backgroundScope.launch(Dispatchers.Unconfined) {
            session.platformSpecificTextInputSession(
                state = transformedState,
                layoutState = layoutState,
                imeOptions = ImeOptions.Default,
                receiveContentConfiguration = null,
                onImeAction = null,
            )
        }

        // This requests a single update, immediately, with no future monitoring.
        inputConnection.requestCursorUpdates(InputConnection.CURSOR_UPDATE_IMMEDIATE)

        assertThat(reportedCursorAnchorInfos).isEmpty()
    }

    @Test
    fun requestCursorUpdates_monitor() = runTest {
        val textFieldState = TextFieldState("abc", initialSelectionInChars = TextRange(2))
        backgroundScope.startFakeTextInputSession(textFieldState)

        // This requests a single update, immediately, with no future monitoring.
        inputConnection.requestCursorUpdates(InputConnection.CURSOR_UPDATE_MONITOR)

        // No immediate update.
        assertThat(reportedCursorAnchorInfos).isEmpty()

        // Trigger new layout.
        layoutState.coreNodeCoordinates =
            TestLayoutCoordinates(windowOffset = Offset(67f, 89f), isAttached = true)

        // Monitoring update.
        val expectedInfo = builder.build(
            text = textFieldState.text,
            selection = textFieldState.text.selectionInChars,
            composition = textFieldState.text.compositionInChars,
            textLayoutResult = layoutState.layoutResult!!,
            matrix = getAndroidMatrix(Offset(67f, 89f)),
            innerTextFieldBounds = Rect.Zero,
            decorationBoxBounds = Rect.Zero,
        )
        assertThat(reportedCursorAnchorInfos).containsExactly(expectedInfo)
    }

    @Test
    fun requestCursorUpdates_immediateAndMonitor() = runTest {
        val textFieldState = TextFieldState("abc", initialSelectionInChars = TextRange(2))
        backgroundScope.startFakeTextInputSession(textFieldState)

        inputConnection.requestCursorUpdates(
            InputConnection.CURSOR_UPDATE_IMMEDIATE or InputConnection.CURSOR_UPDATE_MONITOR
        )

        // Immediate update.
        val expectedInfo = builder.build(
            text = textFieldState.text,
            selection = textFieldState.text.selectionInChars,
            composition = textFieldState.text.compositionInChars,
            textLayoutResult = layoutState.layoutResult!!,
            matrix = getAndroidMatrix(windowOffset),
            innerTextFieldBounds = Rect.Zero,
            decorationBoxBounds = Rect.Zero,
        )
        assertThat(reportedCursorAnchorInfos).containsExactly(expectedInfo)
        reportedCursorAnchorInfos.clear()

        // Trigger new layout.
        layoutState.coreNodeCoordinates =
            TestLayoutCoordinates(windowOffset = Offset(67f, 89f), isAttached = true)

        // Monitoring update.
        val expectedInfo2 = builder.build(
            text = textFieldState.text,
            selection = textFieldState.text.selectionInChars,
            composition = textFieldState.text.compositionInChars,
            textLayoutResult = layoutState.layoutResult!!,
            matrix = getAndroidMatrix(Offset(67f, 89f)),
            innerTextFieldBounds = Rect.Zero,
            decorationBoxBounds = Rect.Zero,
        )
        assertThat(reportedCursorAnchorInfos).containsExactly(expectedInfo2)
    }

    @Test
    fun requestCursorUpdates_cancel() = runTest {
        val textFieldState = TextFieldState("abc", initialSelectionInChars = TextRange(2))
        backgroundScope.startFakeTextInputSession(textFieldState)

        inputConnection.requestCursorUpdates(
            InputConnection.CURSOR_UPDATE_IMMEDIATE or InputConnection.CURSOR_UPDATE_MONITOR
        )

        // Immediate update.
        val expectedInfo = builder.build(
            text = textFieldState.text,
            selection = textFieldState.text.selectionInChars,
            composition = textFieldState.text.compositionInChars,
            textLayoutResult = layoutState.layoutResult!!,
            matrix = getAndroidMatrix(windowOffset),
            innerTextFieldBounds = Rect.Zero,
            decorationBoxBounds = Rect.Zero,
        )
        assertThat(reportedCursorAnchorInfos).containsExactly(expectedInfo)
        reportedCursorAnchorInfos.clear()

        // Cancel monitoring.
        inputConnection.requestCursorUpdates(0)

        // Trigger new layout.
        layoutState.coreNodeCoordinates =
            TestLayoutCoordinates(windowOffset = Offset(67f, 89f), isAttached = true)

        // No update after cancel update.
        assertThat(reportedCursorAnchorInfos).isEmpty()
    }

    private fun runTest(testBody: suspend TestScope.() -> Unit) {
        kotlinx.coroutines.test.runTest {
            // Bootstrap the snapshot observation system.
            lateinit var handle: ObserverHandle
            try {
                handle = Snapshot.registerGlobalWriteObserver {
                    Snapshot.sendApplyNotifications()
                }
                testBody()
            } finally {
                handle.dispose()
            }
        }
    }

    private fun CoroutineScope.startFakeTextInputSession(textFieldState: TextFieldState) {
        val transformedState = TransformedTextFieldState(
            textFieldState = textFieldState,
            inputTransformation = null,
            codepointTransformation = null
        )

        launch(Dispatchers.Unconfined) {
            session.platformSpecificTextInputSession(
                state = transformedState,
                layoutState = layoutState,
                imeOptions = ImeOptions.Default,
                receiveContentConfiguration = null,
                onImeAction = null,
            )
        }

        layoutState.updateNonMeasureInputs(
            textFieldState = transformedState,
            textStyle = TextStyle.Default,
            singleLine = false,
            softWrap = false
        )
        layoutState.layoutWithNewMeasureInputs(
            density = defaultDensity,
            fontFamilyResolver = createFontFamilyResolver(context, coroutineContext),
            layoutDirection = LayoutDirection.Ltr,
            constraints = Constraints()
        )
    }

    private fun getAndroidMatrix(offset: Offset) =
        androidMatrix.apply { setTranslate(offset.x, offset.y) }

    private open class TestLayoutCoordinates(
        val windowOffset: Offset = Offset.Zero,
        override var size: IntSize = IntSize.Zero,
        override var providedAlignmentLines: Set<AlignmentLine> = emptySet(),
        override var parentLayoutCoordinates: LayoutCoordinates? = null,
        override var parentCoordinates: LayoutCoordinates? = null,
        override var isAttached: Boolean = false,
    ) : LayoutCoordinates {
        override fun get(alignmentLine: AlignmentLine): Int = AlignmentLine.Unspecified
        override fun windowToLocal(relativeToWindow: Offset): Offset =
            relativeToWindow - windowOffset
        override fun localToWindow(relativeToLocal: Offset): Offset = relativeToLocal + windowOffset
        override fun localToRoot(relativeToLocal: Offset): Offset = relativeToLocal
        override fun localPositionOf(
            sourceCoordinates: LayoutCoordinates,
            relativeToSource: Offset
        ): Offset = relativeToSource

        override fun localBoundingBoxOf(
            sourceCoordinates: LayoutCoordinates,
            clipBounds: Boolean
        ): Rect = Rect.Zero

        override fun transformToScreen(matrix: Matrix) {
            matrix.translate(windowOffset.x, windowOffset.y, 0f)
        }
    }
}
