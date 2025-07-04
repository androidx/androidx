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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TEST_FONT_FAMILY
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputFilter
import androidx.compose.ui.input.pointer.PointerInputModifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasAnyChild
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.google.common.truth.Truth.assertThat
import kotlin.math.max
import org.junit.Rule
import org.mockito.kotlin.mock

internal abstract class AbstractSelectionContainerTest {
    @get:Rule val rule = createComposeRule().also { it.mainClock.autoAdvance = false }

    protected val textContent = "Text Demo Text"
    protected val fontFamily = TEST_FONT_FAMILY
    protected val selection = mutableStateOf<Selection?>(null)
    protected val fontSize = 20.sp
    protected val log = PointerInputChangeLog()

    protected val tag1 = "tag1"
    protected val tag2 = "tag2"

    protected val hapticFeedback = mock<HapticFeedback>()

    protected fun characterBox(tag: String, offset: Int): Rect {
        val nodePosition = rule.onNodeWithTag(tag).fetchSemanticsNode().positionInRoot
        val textLayoutResult = rule.onNodeWithTag(tag).fetchTextLayoutResult()
        return textLayoutResult.getBoundingBox(offset).translate(nodePosition)
    }

    protected fun SemanticsNodeInteractionsProvider.onSelectionContainer() =
        onNode(isRoot() and hasAnyChild(hasTestTag("selectionContainer")))

    protected fun assertAnchorInfo(
        anchorInfo: Selection.AnchorInfo?,
        resolvedTextDirection: ResolvedTextDirection = ResolvedTextDirection.Ltr,
        offset: Int = 0,
        selectableId: Long = 0,
    ) {
        assertThat(anchorInfo)
            .isEqualTo(Selection.AnchorInfo(resolvedTextDirection, offset, selectableId))
    }

    protected fun createSelectionContainer(
        isRtl: Boolean = false,
        content: (@Composable () -> Unit)? = null,
    ) {
        val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
        rule.setContent {
            CompositionLocalProvider(
                LocalHapticFeedback provides hapticFeedback,
                LocalLayoutDirection provides layoutDirection,
            ) {
                TestParent(Modifier.testTag("selectionContainer").gestureSpy(log)) {
                    SelectionContainer(
                        selection = selection.value,
                        onSelectionChange = { selection.value = it },
                    ) {
                        content?.invoke() ?: TestText(textContent, Modifier.fillMaxSize())
                    }
                }
            }
        }

        rule.waitForIdle()
    }

    @Composable
    protected fun TestText(text: String, modifier: Modifier = Modifier) {
        BasicText(
            text = AnnotatedString(text),
            modifier = modifier,
            style = TextStyle(fontFamily = fontFamily, fontSize = fontSize),
            softWrap = true,
            overflow = TextOverflow.Clip,
            maxLines = Int.MAX_VALUE,
            inlineContent = mapOf(),
            onTextLayout = {},
        )
    }

    @Composable
    protected fun TestButton(
        modifier: Modifier = Modifier,
        onClick: () -> Unit,
        content: @Composable () -> Unit,
    ) {
        Box(
            modifier.clickable(onClick = onClick), // It marks this node as focusable
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

@Composable
fun TestParent(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(modifier = modifier, content = content) { measurables, constraints ->
        val placeables = measurables.map { measurable -> measurable.measure(constraints) }

        val width = placeables.fold(0) { maxWidth, placeable -> max(maxWidth, (placeable.width)) }

        val height = placeables.fold(0) { minWidth, placeable -> max(minWidth, (placeable.height)) }

        layout(width, height) { placeables.forEach { placeable -> placeable.place(0, 0) } }
    }
}

internal class PointerInputChangeLog : (PointerEvent, PointerEventPass) -> Unit {

    val entries = mutableListOf<PointerInputChangeLogEntry>()

    override fun invoke(p1: PointerEvent, p2: PointerEventPass) {
        entries.add(PointerInputChangeLogEntry(p1.changes.map { it }, p2))
    }
}

internal data class PointerInputChangeLogEntry(
    val changes: List<PointerInputChange>,
    val pass: PointerEventPass,
)

private fun Modifier.gestureSpy(
    onPointerInput: (PointerEvent, PointerEventPass) -> Unit
): Modifier = composed {
    val spy = remember { GestureSpy() }
    spy.onPointerInput = onPointerInput
    spy
}

private class GestureSpy : PointerInputModifier {

    lateinit var onPointerInput: (PointerEvent, PointerEventPass) -> Unit

    override val pointerInputFilter =
        object : PointerInputFilter() {
            override fun onPointerEvent(
                pointerEvent: PointerEvent,
                pass: PointerEventPass,
                bounds: IntSize,
            ) {
                onPointerInput(pointerEvent, pass)
            }

            override fun onCancel() {
                // Nothing to implement
            }
        }
}
