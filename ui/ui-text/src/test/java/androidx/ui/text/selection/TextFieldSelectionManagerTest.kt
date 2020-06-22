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

package androidx.ui.text.selection

import androidx.test.filters.SmallTest
import androidx.ui.core.Constraints
import androidx.ui.core.LayoutDirection
import androidx.ui.geometry.Offset
import androidx.ui.input.OffsetMap
import androidx.ui.input.TextFieldValue
import androidx.ui.text.AnnotatedString
import androidx.ui.text.TextFieldState
import androidx.ui.text.TextLayoutInput
import androidx.ui.text.TextRange
import androidx.ui.text.TextStyle
import androidx.ui.text.style.TextDirection
import androidx.ui.text.style.TextOverflow
import androidx.ui.unit.Density
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class TextFieldSelectionManagerTest {
    private val text = "Hello World"
    private val density = Density(density = 1f)
    private val offsetMap = OffsetMap.identityOffsetMap
    private var value = TextFieldValue(text)
    private val lambda: (TextFieldValue) -> Unit = { value = it }
    private val state = TextFieldState(mock())

    private val dragBeginPosition = Offset.Zero
    private val dragDistance = Offset(300f, 15f)
    private val beginOffset = 0
    private val dragOffset = text.indexOf('W')
    private val longPressTextRange = TextRange(0, "Hello".length)
    private val dragTextRange = TextRange("Hello".length + 1, text.length)

    private val manager = TextFieldSelectionManager()

    @Before
    fun setup() {
        manager.offsetMap = offsetMap
        manager.onValueChange = lambda
        manager.state = state
        manager.value = value

        state.layoutResult = mock()
        whenever(state.layoutResult!!.layoutInput).thenReturn(
            TextLayoutInput(
                text = AnnotatedString(text),
                style = TextStyle.Default,
                placeholders = mock(),
                maxLines = 2,
                softWrap = true,
                overflow = TextOverflow.Ellipsis,
                density = density,
                layoutDirection = LayoutDirection.Ltr,
                resourceLoader = mock(),
                constraints = Constraints()
            )
        )
        whenever(state.layoutResult!!.getOffsetForPosition(dragBeginPosition)).thenReturn(
            beginOffset
        )
        whenever(state.layoutResult!!.getOffsetForPosition(dragDistance)).thenReturn(dragOffset)
        whenever(state.layoutResult!!.getWordBoundary(beginOffset)).thenReturn(longPressTextRange)
        whenever(state.layoutResult!!.getWordBoundary(dragOffset)).thenReturn(dragTextRange)
        whenever(state.layoutResult!!.getBidiRunDirection(any())).thenReturn(TextDirection.Ltr)
    }

    @Test
    fun TextFieldSelectionManager_init() {
        assertThat(manager.offsetMap).isEqualTo(offsetMap)
        assertThat(manager.onValueChange).isEqualTo(lambda)
        assertThat(manager.state).isEqualTo(state)
        assertThat(manager.value).isEqualTo(value)
    }

    @Test
    fun TextFieldSelectionManager_longPressDragObserver_onLongPress() {
        manager.longPressDragObserver.onLongPress(dragBeginPosition)

        assertThat(state.selectionIsOn).isTrue()
        assertThat(value.selection).isEqualTo(longPressTextRange)
    }

    @Test
    fun TextFieldSelectionManager_longPressDragObserver_onDrag() {
        manager.longPressDragObserver.onLongPress(dragBeginPosition)
        manager.longPressDragObserver.onDrag(dragDistance)

        assertThat(value.selection).isEqualTo(TextRange(0, text.length))
    }
}