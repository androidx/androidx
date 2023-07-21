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

package androidx.compose.foundation.text.modifiers

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.ceilToIntPx
import androidx.compose.foundation.text.selection.Selectable
import androidx.compose.foundation.text.selection.Selection
import androidx.compose.foundation.text.selection.Selection.AnchorInfo
import androidx.compose.foundation.text.selection.SelectionAdjustment
import androidx.compose.foundation.text.selection.SelectionRegistrar
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class SelectionControllerTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun drawWithClip_doesClip() {
        val canvasSize = 10.dp
        val pathSize = 10_000f
        val path = Path().also {
            it.addRect(Rect(0f, 0f, pathSize, pathSize))
        }

        val subject = SelectionController(
            FixedSelectionFake(0, 1000, 200),
            Color.White,
            params = FakeParams(
                path, true
            )
        )
        var size: Size? = null

        rule.setContent {
            Box(Modifier.fillMaxSize().drawBehind { drawRect(Color.Black) }) {
                Canvas(Modifier.size(canvasSize)) {
                    size = this.size
                    subject.draw(this)
                }
            }
        }

        rule.waitForIdle()
        assertClipped(size!!, true)
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun drawWithOut_doesNotClip() {
        val canvasSize = 10.dp
        val pathSize = 10_000f
        val path = Path().also {
            it.addRect(Rect(0f, 0f, pathSize, pathSize))
        }

        val subject = SelectionController(
            FixedSelectionFake(0, 1000, 200),
            Color.White,
            params = FakeParams(
                path, false
            )
        )
        var size: Size? = null

        rule.setContent {
            Box(Modifier.fillMaxSize().drawBehind { drawRect(Color.Black) }) {
                Canvas(Modifier.size(canvasSize)) {
                    size = this.size
                    drawRect(Color.Black)
                    subject.draw(this)
                }
            }
        }

        rule.waitForIdle()
        assertClipped(size!!, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun assertClipped(size: Size, isClipped: Boolean) {
        val expectedColor = if (isClipped) { Color.Black } else { Color.White }
        rule.onRoot().captureToImage().asAndroidBitmap().apply {
            Assert.assertEquals(
                expectedColor.toArgb(),
                getPixel(
                    size.width.ceilToIntPx() + 5,
                    size.height.ceilToIntPx() + 5
                )
            )
        }
    }
}

/**
 * Fake that always has selection
 */
private class FixedSelectionFake(
    val start: Int,
    val end: Int,
    val lastVisible: Int
) : SelectionRegistrar {

    var selectableId = 0L
    var allSelectables = mutableListOf<Long>()

    override val subselections: Map<Long, Selection>
        get() = allSelectables.associateWith { selectionId ->
            Selection(
                AnchorInfo(ResolvedTextDirection.Ltr, start, selectionId),
                AnchorInfo(ResolvedTextDirection.Ltr, end, selectionId)
            )
        }

    override fun subscribe(selectable: Selectable): Selectable {
        return FakeSelectableWithLastVisibleOffset(selectable.selectableId, lastVisible)
    }

    override fun unsubscribe(selectable: Selectable) {
        // nothing
    }

    override fun nextSelectableId(): Long {
        return selectableId++.also {
            allSelectables.add(it)
        }
    }

    override fun notifyPositionChange(selectableId: Long) {
        FAKE("Not yet implemented")
    }

    override fun notifySelectionUpdateStart(
        layoutCoordinates: LayoutCoordinates,
        startPosition: Offset,
        adjustment: SelectionAdjustment
    ) {
        FAKE("Selection not editable")
    }

    override fun notifySelectionUpdateSelectAll(selectableId: Long) {
        FAKE()
    }

    override fun notifySelectionUpdate(
        layoutCoordinates: LayoutCoordinates,
        newPosition: Offset,
        previousPosition: Offset,
        isStartHandle: Boolean,
        adjustment: SelectionAdjustment
    ): Boolean {
        FAKE("Selection not editable")
    }

    override fun notifySelectionUpdateEnd() {
        FAKE("Selection not editable")
    }

    override fun notifySelectableChange(selectableId: Long) {
        FAKE("Selection not editable")
    }
}

private class FakeSelectableWithLastVisibleOffset(
    override val selectableId: Long,
    private val lastVisible: Int
) : Selectable {
    override fun updateSelection(
        startHandlePosition: Offset,
        endHandlePosition: Offset,
        previousHandlePosition: Offset?,
        isStartHandle: Boolean,
        containerLayoutCoordinates: LayoutCoordinates,
        adjustment: SelectionAdjustment,
        previousSelection: Selection?
    ): Pair<Selection?, Boolean> {
        FAKE()
    }

    override fun getSelectAllSelection(): Selection? {
        FAKE()
    }

    override fun getHandlePosition(selection: Selection, isStartHandle: Boolean): Offset {
        FAKE()
    }

    override fun getLayoutCoordinates(): LayoutCoordinates? {
        FAKE()
    }

    override fun getText(): AnnotatedString {
        FAKE()
    }

    override fun getBoundingBox(offset: Int): Rect {
        FAKE()
    }

    override fun getRangeOfLineContaining(offset: Int): TextRange {
        FAKE()
    }

    override fun getLastVisibleOffset(): Int {
        return lastVisible
    }
}

private class FakeParams(
    val path: Path,
    override val shouldClip: Boolean
) : StaticTextSelectionParams(null, null) {

    override fun getPathForRange(start: Int, end: Int): Path? {
        return path
    }
}

private fun FAKE(reason: String = "Unsupported fake method on fake"): Nothing =
    throw NotImplementedError("No support in fake: $reason")