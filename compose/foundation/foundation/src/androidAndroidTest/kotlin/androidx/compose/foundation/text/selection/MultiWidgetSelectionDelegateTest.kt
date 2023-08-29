/*
 * Copyright 2019 The Android Open Source Project
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
import androidx.compose.foundation.text.InternalFoundationTextApi
import androidx.compose.foundation.text.TEST_FONT_FAMILY
import androidx.compose.foundation.text.TextDelegate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
@MediumTest
class MultiWidgetSelectionDelegateTest {

    private val fontFamily = TEST_FONT_FAMILY
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val defaultDensity = Density(density = 1f)
    private val fontFamilyResolver = createFontFamilyResolver(context)

    @Test
    fun getHandlePosition_StartHandle_invalid() {
        val text = "hello world\n"
        val fontSize = 20.sp

        val layoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectableId = 1L
        val selectable = MultiWidgetSelectionDelegate(
            selectableId = selectableId,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        val selectableInvalidId = 2L
        val startOffset = text.indexOf('h')
        val endOffset = text.indexOf('o')

        val selection = Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = startOffset,
                selectableId = selectableInvalidId
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = endOffset,
                selectableId = selectableInvalidId
            ),
            handlesCrossed = false
        )

        // Act.
        val coordinates = selectable.getHandlePosition(
            selection = selection,
            isStartHandle = true
        )

        // Assert.
        assertThat(coordinates).isEqualTo(Offset.Zero)
    }

    @Test
    fun getHandlePosition_EndHandle_invalid() {
        val text = "hello world\n"
        val fontSize = 20.sp

        val layoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectableId = 1L
        val selectable = MultiWidgetSelectionDelegate(
            selectableId = selectableId,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        val selectableInvalidId = 2L
        val startOffset = text.indexOf('h')
        val endOffset = text.indexOf('o')

        val selection = Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = startOffset,
                selectableId = selectableInvalidId
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = endOffset,
                selectableId = selectableInvalidId
            ),
            handlesCrossed = false
        )

        // Act.
        val coordinates = selectable.getHandlePosition(
            selection = selection,
            isStartHandle = false
        )

        // Assert.
        assertThat(coordinates).isEqualTo(Offset.Zero)
    }

    @Test
    fun getHandlePosition_StartHandle_not_cross_ltr() {
        val text = "hello world\n"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }

        val layoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectableId = 1L
        val selectable = MultiWidgetSelectionDelegate(
            selectableId = selectableId,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        val startOffset = text.indexOf('h')
        val endOffset = text.indexOf('o')

        val selection = Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = startOffset,
                selectableId = selectableId
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = endOffset,
                selectableId = selectableId
            ),
            handlesCrossed = false
        )

        // Act.
        val coordinates = selectable.getHandlePosition(
            selection = selection,
            isStartHandle = true
        )

        // Assert.
        assertThat(coordinates).isEqualTo(
            Offset((fontSizeInPx * startOffset), fontSizeInPx)
        )
    }

    @Test
    fun getHandlePosition_StartHandle_cross_ltr() {
        val text = "hello world\n"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }

        val layoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectableId = 1L
        val selectable = MultiWidgetSelectionDelegate(
            selectableId = selectableId,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        val startOffset = text.indexOf('o')
        val endOffset = text.indexOf('h')

        val selection = Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = startOffset,
                selectableId = selectableId
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = endOffset,
                selectableId = selectableId
            ),
            handlesCrossed = true
        )

        // Act.
        val coordinates = selectable.getHandlePosition(
            selection = selection,
            isStartHandle = true
        )

        // Assert.
        assertThat(coordinates).isEqualTo(
            Offset((fontSizeInPx * startOffset), fontSizeInPx)
        )
    }

    @Test
    fun getHandlePosition_StartHandle_not_cross_rtl() {
        val text = "\u05D0\u05D1\u05D2 \u05D3\u05D4\u05D5\n"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }

        val layoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectableId = 1L
        val selectable = MultiWidgetSelectionDelegate(
            selectableId = selectableId,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        val startOffset = text.indexOf('\u05D1')
        val endOffset = text.indexOf('\u05D5')

        val selection = Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = startOffset,
                selectableId = selectableId
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = endOffset,
                selectableId = selectableId
            ),
            handlesCrossed = false
        )

        // Act.
        val coordinates = selectable.getHandlePosition(
            selection = selection,
            isStartHandle = true
        )

        // Assert.
        assertThat(coordinates).isEqualTo(
            Offset((fontSizeInPx * (text.length - 1 - startOffset)), fontSizeInPx)
        )
    }

    @Test
    fun getHandlePosition_StartHandle_cross_rtl() {
        val text = "\u05D0\u05D1\u05D2 \u05D3\u05D4\u05D5\n"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }

        val layoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectableId = 1L
        val selectable = MultiWidgetSelectionDelegate(
            selectableId = selectableId,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        val startOffset = text.indexOf('\u05D5')
        val endOffset = text.indexOf('\u05D1')

        val selection = Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = startOffset,
                selectableId = selectableId
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = endOffset,
                selectableId = selectableId
            ),
            handlesCrossed = true
        )

        // Act.
        val coordinates = selectable.getHandlePosition(
            selection = selection,
            isStartHandle = true
        )

        // Assert.
        assertThat(coordinates).isEqualTo(
            Offset((fontSizeInPx * (text.length - 1 - startOffset)), fontSizeInPx)
        )
    }

    @Test
    fun getHandlePosition_StartHandle_not_cross_bidi() {
        val textLtr = "Hello"
        val textRtl = "\u05D0\u05D1\u05D2"
        val text = textLtr + textRtl
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }

        val layoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectableId = 1L
        val selectable = MultiWidgetSelectionDelegate(
            selectableId = selectableId,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        val startOffset = text.indexOf('\u05D0')
        val endOffset = text.indexOf('\u05D2')

        val selection = Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = startOffset,
                selectableId = selectableId
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = endOffset,
                selectableId = selectableId
            ),
            handlesCrossed = false
        )

        // Act.
        val coordinates = selectable.getHandlePosition(
            selection = selection,
            isStartHandle = true
        )

        // Assert.
        assertThat(coordinates).isEqualTo(
            Offset((fontSizeInPx * (text.length)), fontSizeInPx)
        )
    }

    @Test
    fun getHandlePosition_StartHandle_cross_bidi() {
        val textLtr = "Hello"
        val textRtl = "\u05D0\u05D1\u05D2"
        val text = textLtr + textRtl
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }

        val layoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectableId = 1L
        val selectable = MultiWidgetSelectionDelegate(
            selectableId = selectableId,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        val startOffset = text.indexOf('\u05D0')
        val endOffset = text.indexOf('H')

        val selection = Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = startOffset,
                selectableId = selectableId
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = endOffset,
                selectableId = selectableId
            ),
            handlesCrossed = true
        )

        // Act.
        val coordinates = selectable.getHandlePosition(
            selection = selection,
            isStartHandle = true
        )

        // Assert.
        assertThat(coordinates).isEqualTo(
            Offset((fontSizeInPx * (textLtr.length)), fontSizeInPx)
        )
    }

    @Test
    fun getHandlePosition_EndHandle_not_cross_ltr() {
        val text = "hello world\n"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }

        val layoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectableId = 1L
        val selectable = MultiWidgetSelectionDelegate(
            selectableId = selectableId,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        val startOffset = text.indexOf('h')
        val endOffset = text.indexOf('o')

        val selection = Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = startOffset,
                selectableId = selectableId
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = endOffset,
                selectableId = selectableId
            ),
            handlesCrossed = false
        )

        // Act.
        val coordinates = selectable.getHandlePosition(
            selection = selection,
            isStartHandle = false
        )

        // Assert.
        assertThat(coordinates).isEqualTo(
            Offset((fontSizeInPx * endOffset), fontSizeInPx)
        )
    }

    @Test
    fun getHandlePosition_EndHandle_not_cross_ltr_overflowed() {
        val text = "hello\nworld"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }

        val layoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity,
            maxLines = 1
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectableId = 1L
        val selectable = MultiWidgetSelectionDelegate(
            selectableId = selectableId,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        val startOffset = text.indexOf('h')
        val endOffset = text.indexOf('r')

        val selection = Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = startOffset,
                selectableId = selectableId
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = endOffset,
                selectableId = selectableId
            ),
            handlesCrossed = false
        )

        // Act.
        val coordinates = selectable.getHandlePosition(
            selection = selection,
            isStartHandle = false
        )

        // Assert.
        assertThat(coordinates).isEqualTo(
            Offset(fontSizeInPx * 5, fontSizeInPx) // the last offset in the first line
        )
    }

    @Test
    fun getHandlePosition_EndHandle_cross_ltr() {
        val text = "hello world\n"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }

        val layoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectableId = 1L
        val selectable = MultiWidgetSelectionDelegate(
            selectableId = selectableId,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        val startOffset = text.indexOf('o')
        val endOffset = text.indexOf('h')

        val selection = Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = startOffset,
                selectableId = selectableId
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = endOffset,
                selectableId = selectableId
            ),
            handlesCrossed = true
        )

        // Act.
        val coordinates = selectable.getHandlePosition(
            selection = selection,
            isStartHandle = false
        )

        // Assert.
        assertThat(coordinates).isEqualTo(
            Offset((fontSizeInPx * endOffset), fontSizeInPx)
        )
    }

    @Test
    fun getHandlePosition_EndHandle_cross_ltr_overflowed() {
        val text = "hello\nworld"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }

        val layoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity,
            maxLines = 1
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectableId = 1L
        val selectable = MultiWidgetSelectionDelegate(
            selectableId = selectableId,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        val startOffset = text.indexOf('r')
        val endOffset = text.indexOf('w')

        val selection = Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = startOffset,
                selectableId = selectableId
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = endOffset,
                selectableId = selectableId
            ),
            handlesCrossed = true
        )

        // Act.
        val coordinates = selectable.getHandlePosition(
            selection = selection,
            isStartHandle = false
        )

        // Assert.
        assertThat(coordinates).isEqualTo(
            Offset((fontSizeInPx * 5), fontSizeInPx)
        )
    }

    @Test
    fun getHandlePosition_EndHandle_not_cross_rtl() {
        val text = "\u05D0\u05D1\u05D2 \u05D3\u05D4\u05D5\n"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }

        val layoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectableId = 1L
        val selectable = MultiWidgetSelectionDelegate(
            selectableId = selectableId,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        val startOffset = text.indexOf('\u05D1')
        val endOffset = text.indexOf('\u05D5')

        val selection = Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = startOffset,
                selectableId = selectableId
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = endOffset,
                selectableId = selectableId
            ),
            handlesCrossed = false
        )

        // Act.
        val coordinates = selectable.getHandlePosition(
            selection = selection,
            isStartHandle = false
        )

        // Assert.
        assertThat(coordinates).isEqualTo(
            Offset((fontSizeInPx * (text.length - 1 - endOffset)), fontSizeInPx)
        )
    }

    @Test
    fun getHandlePosition_EndHandle_cross_rtl() {
        val text = "\u05D0\u05D1\u05D2 \u05D3\u05D4\u05D5\n"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }

        val layoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectableId = 1L
        val selectable = MultiWidgetSelectionDelegate(
            selectableId = selectableId,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        val startOffset = text.indexOf('\u05D5')
        val endOffset = text.indexOf('\u05D1')

        val selection = Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = startOffset,
                selectableId = selectableId
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = endOffset,
                selectableId = selectableId
            ),
            handlesCrossed = true
        )

        // Act.
        val coordinates = selectable.getHandlePosition(
            selection = selection,
            isStartHandle = false
        )

        // Assert.
        assertThat(coordinates).isEqualTo(
            Offset((fontSizeInPx * (text.length - 1 - endOffset)), fontSizeInPx)
        )
    }

    @Test
    fun getHandlePosition_EndHandle_not_cross_rtl_overflowed() {
        val text = "\u05D0\u05D1\u05D2\n\u05D3\u05D4\u05D5"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }

        val layoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity,
            maxLines = 1
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectableId = 1L
        val selectable = MultiWidgetSelectionDelegate(
            selectableId = selectableId,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        val startOffset = text.indexOf('\u05D1')
        val endOffset = text.indexOf('\u05D5')

        val selection = Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Rtl,
                offset = startOffset,
                selectableId = selectableId
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Rtl,
                offset = endOffset,
                selectableId = selectableId
            ),
            handlesCrossed = false
        )

        // Act.
        val coordinates = selectable.getHandlePosition(
            selection = selection,
            isStartHandle = false
        )

        // Assert.
        assertThat(coordinates).isEqualTo(Offset(0f, fontSizeInPx))
    }

    @Test
    fun getHandlePosition_EndHandle_cross_rtl_overflowed() {
        val text = "\u05D0\u05D1\u05D2\n\u05D3\u05D4\u05D5"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }

        val layoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity,
            maxLines = 1
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectableId = 1L
        val selectable = MultiWidgetSelectionDelegate(
            selectableId = selectableId,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        val startOffset = text.indexOf('\u05D5')
        val endOffset = text.indexOf('\u05D3')

        val selection = Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Rtl,
                offset = startOffset,
                selectableId = selectableId
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Rtl,
                offset = endOffset,
                selectableId = selectableId
            ),
            handlesCrossed = true
        )

        // Act.
        val coordinates = selectable.getHandlePosition(
            selection = selection,
            isStartHandle = false
        )

        // Assert.
        assertThat(coordinates).isEqualTo(Offset((fontSizeInPx * 3), fontSizeInPx))
    }

    @Test
    fun getHandlePosition_EndHandle_not_cross_bidi() {
        val textLtr = "Hello"
        val textRtl = "\u05D0\u05D1\u05D2"
        val text = textLtr + textRtl
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }

        val layoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectableId = 1L
        val selectable = MultiWidgetSelectionDelegate(
            selectableId = selectableId,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        val startOffset = text.indexOf('e')
        val endOffset = text.indexOf('\u05D0')

        val selection = Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = startOffset,
                selectableId = selectableId
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = endOffset,
                selectableId = selectableId
            ),
            handlesCrossed = false
        )

        // Act.
        val coordinates = selectable.getHandlePosition(
            selection = selection,
            isStartHandle = false
        )

        // Assert.
        assertThat(coordinates).isEqualTo(
            Offset((fontSizeInPx * (textLtr.length)), fontSizeInPx)
        )
    }

    @Test
    fun getHandlePosition_EndHandle_cross_bidi() {
        val textLtr = "Hello"
        val textRtl = "\u05D0\u05D1\u05D2"
        val text = textLtr + textRtl
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }
        val layoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectableId = 1L
        val selectable = MultiWidgetSelectionDelegate(
            selectableId = selectableId,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        val startOffset = text.indexOf('\u05D2')
        val endOffset = text.indexOf('\u05D0')

        val selection = Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = startOffset,
                selectableId = selectableId
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = endOffset,
                selectableId = selectableId
            ),
            handlesCrossed = true
        )

        // Act.
        val coordinates = selectable.getHandlePosition(
            selection = selection,
            isStartHandle = false
        )

        // Assert.
        assertThat(coordinates).isEqualTo(
            Offset((fontSizeInPx * (text.length)), fontSizeInPx)
        )
    }

    @Test
    fun getText_textLayoutResult_Null_Return_Empty_AnnotatedString() {
        val layoutResult = null

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectable = MultiWidgetSelectionDelegate(
            0,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        assertThat(selectable.getText()).isEqualTo(AnnotatedString(""))
    }

    @Test
    fun getText_textLayoutResult_NotNull_Return_AnnotatedString() {
        val textLtr = "Hello"
        val textRtl = "\u05D0\u05D1\u05D2"
        val text = textLtr + textRtl
        val fontSize = 20.sp
        val spanStyle = SpanStyle(fontSize = fontSize, fontFamily = fontFamily)

        val layoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectable = MultiWidgetSelectionDelegate(
            0,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        assertThat(selectable.getText()).isEqualTo(AnnotatedString(text, spanStyle))
    }

    @Test
    fun getBoundingBox_valid() {
        val text = "hello\nworld\n"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }

        val layoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectable = MultiWidgetSelectionDelegate(
            1,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        val textOffset = text.indexOf('w')

        // Act.
        val box = selectable.getBoundingBox(textOffset)

        // Assert.
        assertThat(box.left).isZero()
        assertThat(box.right).isEqualTo(fontSizeInPx)
        assertThat(box.top).isEqualTo(fontSizeInPx)
        assertThat(box.bottom).isEqualTo(2 * fontSizeInPx)
    }

    @Test
    fun getBoundingBox_zero_length_text_return_zero_rect() {
        val text = ""
        val fontSize = 20.sp

        val layoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectable = MultiWidgetSelectionDelegate(
            0,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        // Act.
        val box = selectable.getBoundingBox(0)

        // Assert.
        assertThat(box).isEqualTo(Rect.Zero)
    }

    @Test
    fun getBoundingBox_negative_offset_should_return_zero_rect() {
        val text = "hello\nworld\n"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }

        val layoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectable = MultiWidgetSelectionDelegate(
            0,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        // Act.
        val box = selectable.getBoundingBox(-2)

        // Assert.
        assertThat(box.left).isZero()
        assertThat(box.right).isEqualTo(fontSizeInPx)
        assertThat(box.top).isZero()
        assertThat(box.bottom).isEqualTo(fontSizeInPx)
    }

    @Test
    fun getBoundingBox_offset_larger_than_range_should_return_largest() {
        val text = "hello\nworld"
        val fontSize = 20.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }

        val layoutResult = simpleTextLayout(
            text = text,
            fontSize = fontSize,
            density = defaultDensity
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectable = MultiWidgetSelectionDelegate(
            selectableId = 1,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        // Act.
        val box = selectable.getBoundingBox(text.indexOf('d') + 5)

        // Assert.
        assertThat(box.left).isEqualTo(4 * fontSizeInPx)
        assertThat(box.right).isEqualTo(5 * fontSizeInPx)
        assertThat(box.top).isEqualTo(fontSizeInPx)
        assertThat(box.bottom).isEqualTo(2 * fontSizeInPx)
    }

    @Test
    fun getRangeOfLineContaining_zeroOffset() {
        val text = "hello\nworld\n"

        val layoutResult = simpleTextLayout(
            text = text,
            density = defaultDensity
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectable = MultiWidgetSelectionDelegate(
            1,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        // Act.
        val lineRange = selectable.getRangeOfLineContaining(0)

        // Assert.
        assertThat(lineRange).isEqualTo(TextRange(0, 5))
    }

    @Test
    fun getRangeOfLineContaining_secondLine() {
        val text = "hello\nworld\n"

        val layoutResult = simpleTextLayout(
            text = text,
            density = defaultDensity
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectable = MultiWidgetSelectionDelegate(
            1,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        // Act.
        val lineRange = selectable.getRangeOfLineContaining(7)

        // Assert.
        assertThat(lineRange).isEqualTo(TextRange(6, 11))
    }

    @Test
    fun getRangeOfLineContaining_negativeOffset_returnsFirstLine() {
        val text = "hello\nworld\n"

        val layoutResult = simpleTextLayout(
            text = text,
            density = defaultDensity
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectable = MultiWidgetSelectionDelegate(
            1,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        // Act.
        val lineRange = selectable.getRangeOfLineContaining(-1)

        // Assert.
        assertThat(lineRange).isEqualTo(TextRange(0, 5))
    }

    @Test
    fun getRangeOfLineContaining_offsetPastTextLength_returnsLastLine() {
        val text = "hello\nworld\n"

        val layoutResult = simpleTextLayout(
            text = text,
            density = defaultDensity
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectable = MultiWidgetSelectionDelegate(
            1,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        // Act.
        val lineRange = selectable.getRangeOfLineContaining(Int.MAX_VALUE)

        // Assert.
        assertThat(lineRange).isEqualTo(TextRange(6, 11))
    }

    @Test
    fun getRangeOfLineContaining_offsetAtNewline_returnsPreviousLine() {
        val text = "hello\nworld\n"

        val layoutResult = simpleTextLayout(
            text = text,
            density = defaultDensity
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectable = MultiWidgetSelectionDelegate(
            1,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        // Act.
        val lineRange = selectable.getRangeOfLineContaining(5)

        // Assert.
        assertThat(lineRange).isEqualTo(TextRange(0, 5))
    }

    @Test
    fun getRangeOfLineContaining_emptyString_returnsEmptyRange() {
        val text = ""

        val layoutResult = simpleTextLayout(
            text = text,
            density = defaultDensity
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectable = MultiWidgetSelectionDelegate(
            1,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        // Act.
        val lineRange = selectable.getRangeOfLineContaining(5)

        // Assert.
        assertThat(lineRange).isEqualTo(TextRange.Zero)
    }

    @Test
    fun getRangeOfLineContaining_emptyLine_returnsEmptyNonZeroRange() {
        val text = "hello\n\nworld"

        val layoutResult = simpleTextLayout(
            text = text,
            density = defaultDensity
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectable = MultiWidgetSelectionDelegate(
            1,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        // Act.
        val lineRange = selectable.getRangeOfLineContaining(6)

        // Assert.
        assertThat(lineRange).isEqualTo(TextRange(6, 6))
    }

    @Test
    fun getRangeOfLineContaining_overflowed_returnsLastVisibleLine() {
        val text = "hello\nworld"

        val layoutResult = simpleTextLayout(
            text = text,
            density = defaultDensity,
            maxLines = 1
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectable = MultiWidgetSelectionDelegate(
            1,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        // Act.
        val lineRange = selectable.getRangeOfLineContaining(6)

        // Assert.
        assertThat(lineRange).isEqualTo(TextRange(0, 5))
    }

    @Test
    fun getRangeOfLineContaining_overflowedDueToMaxHeight_returnsLastVisibleLine() {
        val text = "hello\nworld"
        val fontSize = 20.sp

        val layoutResult = simpleTextLayout(
            text = text,
            density = defaultDensity,
            fontSize = fontSize,
            constraints = Constraints(maxHeight = with(defaultDensity) { fontSize.roundToPx() } * 1)
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectable = MultiWidgetSelectionDelegate(
            1,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        // Act.
        val lineRange = selectable.getRangeOfLineContaining(6)

        // Assert.
        assertThat(lineRange).isEqualTo(TextRange(0, 5))
    }

    @Test
    fun getLastVisibleOffset_everythingVisible_returnsTextLength() {
        val text = "hello\nworld"

        val layoutResult = constrainedTextLayout(
            text = text
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectable = MultiWidgetSelectionDelegate(
            1,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        val lastVisibleOffset = selectable.getLastVisibleOffset()

        assertThat(lastVisibleOffset).isEqualTo(text.length)
    }

    @Test
    fun getLastVisibleOffset_changesWhenTextLayoutChanges() {
        val text = "hello\nworld"

        var layoutResult = constrainedTextLayout(text = text)

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectable = MultiWidgetSelectionDelegate(
            1,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        assertThat(selectable.getLastVisibleOffset()).isEqualTo(text.length)

        layoutResult = constrainedTextLayout(text = "$text$text")

        assertThat(selectable.getLastVisibleOffset()).isEqualTo(text.length * 2)
    }

    // start = maxLines 1
    // start = clip
    // start = enabled soft wrap
    @Test
    fun getLastVisibleOffset_maxLines1_clip_enabledSoftwrap_multiLineContent() {
        val text = "hello\nworld"

        val layoutResult = constrainedTextLayout(
            text = text,
            overflow = TextOverflow.Clip,
            maxLines = 1,
            softWrap = true
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectable = MultiWidgetSelectionDelegate(
            1,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        val lastVisibleOffset = selectable.getLastVisibleOffset()

        assertThat(lastVisibleOffset).isEqualTo(5)
    }

    @Test
    fun getLastVisibleOffset_maxLines1_clip_enabledSoftwrap_singleLineContent() {
        val text = "hello world"

        val layoutResult = constrainedTextLayout(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            softWrap = true,
            widthInCharacters = 10
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectable = MultiWidgetSelectionDelegate(
            1,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        val lastVisibleOffset = selectable.getLastVisibleOffset()

        assertThat(lastVisibleOffset).isEqualTo(5)
    }

    // start = disabled soft wrap
    @Test
    fun getLastVisibleOffset_maxLines1_clip_disabledSoftwrap_multiLineContent() {
        val text = "hello\nworld"

        val layoutResult = constrainedTextLayout(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            softWrap = false
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectable = MultiWidgetSelectionDelegate(
            1,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        val lastVisibleOffset = selectable.getLastVisibleOffset()

        assertThat(lastVisibleOffset).isEqualTo(5)
    }

    @Test
    fun getLastVisibleOffset_maxLines1_clip_disabledSoftwrap_singleLineContent() {
        val text = "hello world ".repeat(10)

        val layoutResult = constrainedTextLayout(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            softWrap = false,
            widthInCharacters = 10
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectable = MultiWidgetSelectionDelegate(
            1,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        val lastVisibleOffset = selectable.getLastVisibleOffset()

        assertThat(lastVisibleOffset).isEqualTo(text.length - 1) // ignore last whitespace
    }

    // start = ellipsis
    // start = enabled soft wrap
    @Test
    fun getLastVisibleOffset_maxLines1_ellipsis_enabledSoftwrap_multiLineContent() {
        val text = "hello\nworld"

        val layoutResult = constrainedTextLayout(
            text = text,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            softWrap = true,
            widthInCharacters = 4
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectable = MultiWidgetSelectionDelegate(
            1,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        val lastVisibleOffset = selectable.getLastVisibleOffset()

        assertThat(lastVisibleOffset).isEqualTo(3)
    }

    // TODO(b/270441925); Last visible offset calculated using getLineVisibleEnd. It returns//   a different result below API 26.
    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun getLastVisibleOffset_maxLines1_ellipsis_enabledSoftwrap_singleLineContent() {
        val text = "hello world ".repeat(10)

        val layoutResult = constrainedTextLayout(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = true,
            widthInCharacters = 10
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectable = MultiWidgetSelectionDelegate(
            1,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        val lastVisibleOffset = selectable.getLastVisibleOffset()

        assertThat(lastVisibleOffset).isEqualTo(9)
    }

    // start = disabled soft wrap
    @Test
    fun getLastVisibleOffset_maxLines1_ellipsis_disabledSoftwrap_multiLineContent() {
        val text = "hello\nworld"

        val layoutResult = constrainedTextLayout(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
            widthInCharacters = 5
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectable = MultiWidgetSelectionDelegate(
            1,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        val lastVisibleOffset = selectable.getLastVisibleOffset()

        // first line will include an ellipsis before line break
        assertThat(lastVisibleOffset).isEqualTo(4)
    }

    @Test
    fun getLastVisibleOffset_maxLines1_ellipsis_disabledSoftwrap_singleLineContent() {
        val text = "hello world ".repeat(10)

        val layoutResult = constrainedTextLayout(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
            widthInCharacters = 20
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectable = MultiWidgetSelectionDelegate(
            1,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        val lastVisibleOffset = selectable.getLastVisibleOffset()

        // the way text layout is calculated with ellipsis is vastly different before and
        // after API 23. Last visible offset logic cannot be unified below API 23.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            assertThat(lastVisibleOffset).isEqualTo(19)
        } else {
            assertThat(lastVisibleOffset).isEqualTo(17)
        }
    }

    // start = height constrained
    // start = clip
    // start = enabled soft wrap
    @Test
    fun getLastVisibleOffset_limitHeight_clip_enabledSoftwrap_multiLineContent() {
        val text = "hello\nworld"

        val layoutResult = constrainedTextLayout(
            text = text,
            overflow = TextOverflow.Clip,
            softWrap = true,
            maxHeightInLines = 1
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectable = MultiWidgetSelectionDelegate(
            1,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        val lastVisibleOffset = selectable.getLastVisibleOffset()

        assertThat(lastVisibleOffset).isEqualTo(5)
    }

    @Test
    fun getLastVisibleOffset_limitHeight_clip_enabledSoftwrap_singleLineContent() {
        val text = "helloworld helloworld helloworld"

        val layoutResult = constrainedTextLayout(
            text = text,
            overflow = TextOverflow.Clip,
            softWrap = true,
            widthInCharacters = 10,
            maxHeightInLines = 2
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectable = MultiWidgetSelectionDelegate(
            1,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        val lastVisibleOffset = selectable.getLastVisibleOffset()

        assertThat(lastVisibleOffset).isEqualTo(21)
    }

    // start = disabled soft wrap
    @Test
    fun getLastVisibleOffset_limitHeight_clip_disabledSoftwrap_multiLineContent() {
        val text = "hello\nworld"

        val layoutResult = constrainedTextLayout(
            text = text,
            overflow = TextOverflow.Clip,
            softWrap = false,
            maxHeightInLines = 1
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectable = MultiWidgetSelectionDelegate(
            1,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        val lastVisibleOffset = selectable.getLastVisibleOffset()

        assertThat(lastVisibleOffset).isEqualTo(5)
    }

    @Test
    fun getLastVisibleOffset_limitHeight_clip_disabledSoftwrap_singleLineContent() {
        val text = "hello world ".repeat(10)

        val layoutResult = constrainedTextLayout(
            text = text,
            overflow = TextOverflow.Clip,
            softWrap = false,
            widthInCharacters = 10,
            maxHeightInLines = 1
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectable = MultiWidgetSelectionDelegate(
            1,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        val lastVisibleOffset = selectable.getLastVisibleOffset()

        assertThat(lastVisibleOffset).isEqualTo(text.length - 1) // ignores last whitespace
    }

    // start = ellipsis
    // start = enabled soft wrap
    @Test
    fun getLastVisibleOffset_limitHeight_ellipsis_enabledSoftwrap_multiLineContent() {
        val text = "hello\nworld\nhello\nworld"

        val layoutResult = constrainedTextLayout(
            text = text,
            overflow = TextOverflow.Ellipsis,
            softWrap = true,
            widthInCharacters = 10,
            maxHeightInLines = 2,
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectable = MultiWidgetSelectionDelegate(
            1,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        val lastVisibleOffset = selectable.getLastVisibleOffset()

        assertThat(lastVisibleOffset).isEqualTo(11)
    }

    @Test
    fun getLastVisibleOffset_limitHeight_ellipsis_enabledSoftwrap_singleLineContent() {
        val text = "hello world ".repeat(10)

        val layoutResult = constrainedTextLayout(
            text = text,
            overflow = TextOverflow.Ellipsis,
            softWrap = true,
            widthInCharacters = 10,
            maxHeightInLines = 1
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectable = MultiWidgetSelectionDelegate(
            1,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        val lastVisibleOffset = selectable.getLastVisibleOffset()

        // the way text layout is calculated with ellipsis is vastly different before and
        // after API 23. Last visible offset logic cannot be unified below API 23.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            assertThat(lastVisibleOffset).isEqualTo(9)
        } else {
            assertThat(lastVisibleOffset).isEqualTo(5)
        }
    }

    // start = disabled soft wrap
    @Test
    fun getLastVisibleOffset_limitHeight_ellipsis_disabledSoftwrap_multiLineContent() {
        val text = "hello\nworld"

        val layoutResult = constrainedTextLayout(
            text = text,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
            maxHeightInLines = 1
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectable = MultiWidgetSelectionDelegate(
            1,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        val lastVisibleOffset = selectable.getLastVisibleOffset()

        // first line will include an ellipsis before line break
        assertThat(lastVisibleOffset).isEqualTo(5)
    }

    @Test
    fun getLastVisibleOffset_limitHeight_ellipsis_disabledSoftwrap_singleLineContent() {
        val text = "hello world ".repeat(10)

        val layoutResult = constrainedTextLayout(
            text = text,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
            widthInCharacters = 20,
            maxHeightInLines = 1
        )

        val layoutCoordinates = mock<LayoutCoordinates>()
        whenever(layoutCoordinates.isAttached).thenReturn(true)

        val selectable = MultiWidgetSelectionDelegate(
            1,
            coordinatesCallback = { layoutCoordinates },
            layoutResultCallback = { layoutResult }
        )

        val lastVisibleOffset = selectable.getLastVisibleOffset()

        // the way text layout is calculated with ellipsis is vastly different before and
        // after API 23. Last visible offset logic cannot be unified below API 23.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            assertThat(lastVisibleOffset).isEqualTo(19)
        } else {
            assertThat(lastVisibleOffset).isEqualTo(17)
        }
    }

    @OptIn(InternalFoundationTextApi::class)
    private fun simpleTextLayout(
        text: String = "",
        fontSize: TextUnit = TextUnit.Unspecified,
        density: Density,
        maxLines: Int = Int.MAX_VALUE,
        constraints: Constraints = Constraints()
    ): TextLayoutResult {
        val spanStyle = SpanStyle(fontSize = fontSize, fontFamily = fontFamily)
        val annotatedString = AnnotatedString(text, spanStyle)
        return TextDelegate(
            text = annotatedString,
            style = TextStyle(),
            density = density,
            maxLines = maxLines,
            fontFamilyResolver = fontFamilyResolver
        ).layout(constraints, LayoutDirection.Ltr)
    }

    @OptIn(InternalFoundationTextApi::class)
    private fun constrainedTextLayout(
        text: String = "",
        fontSize: TextUnit = 20.sp,
        density: Density = defaultDensity,
        maxLines: Int = Int.MAX_VALUE,
        overflow: TextOverflow = TextOverflow.Clip,
        softWrap: Boolean = true,
        widthInCharacters: Int = 20,
        maxHeightInLines: Int = Int.MAX_VALUE
    ): TextLayoutResult {
        val spanStyle = SpanStyle(fontSize = fontSize, fontFamily = fontFamily)
        val annotatedString = AnnotatedString(text, spanStyle)
        val width = with(density) { fontSize.roundToPx() } * widthInCharacters
        val constraints = Constraints(
            minWidth = width,
            maxWidth = width,
            maxHeight = if (maxHeightInLines == Int.MAX_VALUE) {
                Int.MAX_VALUE
            } else {
                with(density) { fontSize.roundToPx() } * maxHeightInLines
            }
        )
        return TextDelegate(
            text = annotatedString,
            style = TextStyle(),
            density = density,
            fontFamilyResolver = fontFamilyResolver,
            maxLines = maxLines,
            overflow = overflow,
            softWrap = softWrap
        ).layout(constraints, LayoutDirection.Ltr)
    }
}
