/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.text

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.PathSegment
import androidx.compose.ui.text.FontTestData.Companion.BASIC_MEASURE_FONT
import androidx.compose.ui.text.font.toFontFamily
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlin.math.abs
import kotlin.math.ceil
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class MultiParagraphBoundingBoxesForRangeTest {
    private val fontFamilyMeasureFont = BASIC_MEASURE_FONT.toFontFamily()
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val defaultDensity = Density(density = 1f)
    private val ltrLocaleList = LocaleList("en")
    private val rtlLocaleList = LocaleList("ar")

    @Test
    fun getBoundingBoxesForRange_emptyRange_returnsEmptyList() {
        val layoutResult = simpleTextLayoutResult(text = "Hello World", fontSize = 10.sp)
        assertThat(layoutResult.getBoundingBoxesForRange(3, 3)).isEmpty()
    }

    @Test
    fun getBoundingBoxesForRange_singleLineLtr_returnsSingleMergedBox() {
        val fontSize = 10.sp
        val layoutResult = simpleTextLayoutResult(text = "abcdef", fontSize = fontSize)

        val boxes = layoutResult.getBoundingBoxesForRange(1, 4)
        assertThat(boxes).hasSize(1)
        val box = boxes[0]
        assertThat(box.top).isEqualTo(layoutResult.getLineTop(0))
        assertThat(box.bottom).isEqualTo(layoutResult.getLineBottom(0))
        assertThat(box.left).isWithin(0.5f).of(10f)
        assertThat(box.right).isWithin(0.5f).of(40f)
    }

    @Test
    fun calculateBoundingBoxList_reusesProvidedTargetList() {
        val layoutResult = simpleTextLayoutResult(text = "abcdef", fontSize = 10.sp)
        val targetList = arrayListOf(Rect(0f, 0f, 1f, 1f))

        val returned =
            layoutResult.calculateBoundingBoxList(
                start = 1,
                end = 4,
                target = targetList,
            )

        assertThat(returned).isSameInstanceAs(targetList)
        assertThat(targetList).hasSize(1)
        assertThat(targetList[0].left).isWithin(0.5f).of(10f)
        assertThat(targetList[0].right).isWithin(0.5f).of(40f)
    }

    @Test
    fun getBoundingBoxesForRange_singleLineRtl_returnsSingleMergedBox() {
        val fontSize = 10.sp
        val layoutResult =
            simpleTextLayoutResult(
                text = AnnotatedString("\u05D0\u05D1\u05D2\u05D3"),
                style = TextStyle(textDirection = TextDirection.Rtl),
                fontSize = fontSize,
                width = 100f,
                localeList = rtlLocaleList,
            )

        val boxes = layoutResult.getBoundingBoxesForRange(1, 3)
        assertThat(boxes).hasSize(1)
        val box = boxes[0]
        assertThat(box.top).isEqualTo(layoutResult.getLineTop(0))
        assertThat(box.bottom).isEqualTo(layoutResult.getLineBottom(0))
        assertThat(box.left).isWithin(0.5f).of(70f)
        assertThat(box.right).isWithin(0.5f).of(90f)
    }

    @Test
    fun getBoundingBoxesForRange_mixedBidiBoundary_returnsDisjointVisualBoxesInLtrOrder() {
        // LTR paragraph with embedded RTL word: "abc \u05D0\u05D1\u05D2 def"
        // Range [2, 6) covers 'c', ' ', '\u05D0', '\u05D1' which are visually separated by '\u05D2'
        val text = "abc \u05D0\u05D1\u05D2 def"
        val layoutResult =
            simpleTextLayoutResult(
                text = AnnotatedString(text),
                style = TextStyle(fontSize = 20.sp, textDirection = TextDirection.Ltr),
                width = 1000f,
                localeList = ltrLocaleList,
            )

        val boxes = layoutResult.getBoundingBoxesForRange(2, 6)
        assertThat(boxes.size).isAtLeast(2)
        for (i in 0 until boxes.size - 1) {
            assertThat(boxes[i].right).isAtMost(boxes[i + 1].left)
        }
    }

    @Test
    fun getBoundingBoxesForRange_withWordJoiners_coalescesIntoSingleContinuousBox() {
        val fontSize = 10.sp
        val text = "ab\u2060cd\u2060ef"
        val layoutResult = simpleTextLayoutResult(text = text, fontSize = fontSize)

        val boxes = layoutResult.getBoundingBoxesForRange(0, text.length)
        assertThat(boxes).hasSize(1)
        assertThat(boxes[0].left).isWithin(0.5f).of(0f)
        assertThat(boxes[0].right).isWithin(0.5f).of(60f)
    }

    @Test
    fun getBoundingBoxesForRange_multiLineWrapping_excludesTrailingWhitespace() {
        val fontSize = 10.sp
        // "abc   def": 3 chars (30px) + 3 spaces + "def" wrapped at width = 40px
        val layoutResult =
            simpleTextLayoutResult(
                text = "abc   def",
                fontSize = fontSize,
                width = 40f,
            )

        assertThat(layoutResult.lineCount).isEqualTo(2)
        val boxes = layoutResult.getBoundingBoxesForRange(0, 9)
        assertThat(boxes).hasSize(2)

        // First line box should tightly wrap "abc" (width ~30px), excluding the trailing 3 spaces
        assertThat(boxes[0].top).isEqualTo(layoutResult.getLineTop(0))
        assertThat(boxes[0].bottom).isEqualTo(layoutResult.getLineBottom(0))
        assertThat(boxes[0].left).isWithin(0.5f).of(0f)
        assertThat(boxes[0].right).isWithin(1.0f).of(30f)

        // Second line box wraps "def"
        assertThat(boxes[1].top).isEqualTo(layoutResult.getLineTop(1))
        assertThat(boxes[1].bottom).isEqualTo(layoutResult.getLineBottom(1))
        assertThat(boxes[1].left).isWithin(0.5f).of(0f)
        assertThat(boxes[1].right).isWithin(1.0f).of(30f)
    }

    @Test
    fun getBoundingBoxesForRange_ellipsizedLines_clampsToVisibleCharacters() {
        val fontSize = 10.sp
        val layoutResult =
            simpleTextLayoutResult(
                text = AnnotatedString("abcdefghij"),
                fontSize = fontSize,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                width = 50f,
                localeList = ltrLocaleList,
            )

        assertThat(layoutResult.isLineEllipsized(0)).isTrue()
        val visibleEnd = layoutResult.getLineEnd(0, visibleEnd = true)

        val boxesFull = layoutResult.getBoundingBoxesForRange(0, 10)
        val boxesVisible = layoutResult.getBoundingBoxesForRange(0, visibleEnd)
        assertThat(boxesFull).isEqualTo(boxesVisible)

        // Range completely inside the ellipsized-out tail returns empty
        val boxesEllipsizedTail = layoutResult.getBoundingBoxesForRange(visibleEnd, 10)
        assertThat(boxesEllipsizedTail).isEmpty()
    }

    @Test
    fun getBoundingBoxesForRange_multiParagraph_translatesVerticalCoordinatesToGlobal() {
        val fontSize = 10.sp
        val annotatedString = buildAnnotatedString {
            withStyle(ParagraphStyle()) { append("abc") }
            withStyle(ParagraphStyle()) { append("defg") }
        }
        val layoutResult = simpleTextLayoutResult(text = annotatedString, fontSize = fontSize)

        assertThat(layoutResult.lineCount).isEqualTo(2)
        val boxes = layoutResult.getBoundingBoxesForRange(1, 5)
        assertThat(boxes).hasSize(2)

        assertThat(boxes[0].top).isEqualTo(layoutResult.getLineTop(0))
        assertThat(boxes[0].bottom).isEqualTo(layoutResult.getLineBottom(0))
        assertThat(boxes[0].left).isWithin(0.5f).of(10f)
        assertThat(boxes[0].right).isWithin(0.5f).of(30f)

        assertThat(boxes[1].top).isEqualTo(layoutResult.getLineTop(1))
        assertThat(boxes[1].bottom).isEqualTo(layoutResult.getLineBottom(1))
        assertThat(boxes[1].left).isWithin(0.5f).of(0f)
        assertThat(boxes[1].right).isWithin(0.5f).of(20f)
    }

    @Test
    fun getBoundingBoxesForRange_multiParagraphTruncatedByMaxLines_doesNotCrash() {
        val fontSize = 10.sp
        val annotatedString = buildAnnotatedString {
            withStyle(ParagraphStyle()) { append("first") }
            withStyle(ParagraphStyle()) { append("second") }
        }
        val layoutResult =
            simpleTextLayoutResult(
                text = annotatedString,
                fontSize = fontSize,
                maxLines = 1,
            )

        assertThat(layoutResult.lineCount).isEqualTo(1)
        // Range completely inside the truncated second paragraph returns emptyList without crashing
        assertThat(layoutResult.getBoundingBoxesForRange(5, 11)).isEmpty()
        assertThat(layoutResult.getBoundingBoxesForRange(7, 10)).isEmpty()

        // Range spanning from the first paragraph into the truncated second paragraph clamps to
        // the first paragraph's visible characters
        val boxesSpanning = layoutResult.getBoundingBoxesForRange(2, 9)
        val boxesFirstParagraphOnly = layoutResult.getBoundingBoxesForRange(2, 5)
        assertThat(boxesSpanning).isEqualTo(boxesFirstParagraphOnly)
    }

    @Test(expected = IllegalArgumentException::class)
    fun getBoundingBoxesForRange_negativeStart_throwsIllegalArgumentException() {
        val layoutResult = simpleTextLayoutResult(text = "abcdef", fontSize = 10.sp)
        layoutResult.getBoundingBoxesForRange(-1, 3)
    }

    @Test(expected = IllegalArgumentException::class)
    fun getBoundingBoxesForRange_startGreaterThanEnd_throwsIllegalArgumentException() {
        val layoutResult = simpleTextLayoutResult(text = "abcdef", fontSize = 10.sp)
        layoutResult.getBoundingBoxesForRange(4, 2)
    }

    @Test(expected = IllegalArgumentException::class)
    fun getBoundingBoxesForRange_endGreaterThanLength_throwsIllegalArgumentException() {
        val layoutResult = simpleTextLayoutResult(text = "abcdef", fontSize = 10.sp)
        layoutResult.getBoundingBoxesForRange(0, 7)
    }

    @Test(expected = IllegalArgumentException::class)
    fun fillPathForRange_invalidRange_throwsIllegalArgumentException() {
        val layoutResult = simpleTextLayoutResult(text = "abcdef", fontSize = 10.sp)
        layoutResult.fillPathForRange(start = 5, end = 2, target = Path())
    }

    @Test
    fun calculatePathFromBoundingBoxes_onAndroidPath_mergesAndClonesMultiLineBoxes() {
        val layoutResult = simpleTextLayoutResult(text = "abcdefghijkl\nabcdefgh", fontSize = 10.sp)
        val mergedPath =
            layoutResult.calculatePathFromBoundingBoxes(
                start = 0,
                end = layoutResult.layoutInput.text.length,
                cornerRadius = CornerRadius(3f, 3f),
                mergeLines = true,
            )
        assertThat(mergedPath.isEmpty).isFalse()
        assertThat(mergedPath.getBounds()).isEqualTo(Rect(0f, 0f, 120f, 20f))

        val clonedPath =
            layoutResult.calculatePathFromBoundingBoxes(
                start = 0,
                end = layoutResult.layoutInput.text.length,
                cornerRadius = CornerRadius(3f, 3f),
                mergeLines = false,
            )
        assertThat(clonedPath.isEmpty).isFalse()
        assertThat(clonedPath.getBounds()).isEqualTo(Rect(0f, 0f, 120f, 20f))
    }

    @Test
    fun textLayoutResult_fillPathForRange_mergesMultiLineContourAndPopulatesTarget() {
        val layoutResult =
            simpleTextLayoutResult(
                text = "abc def ghi",
                style = TextStyle(lineHeight = 24.sp),
                fontSize = 10.sp,
                width = 35f,
            )
        val boxes = layoutResult.getBoundingBoxesForRange(0, 11)
        val target = Path()
        layoutResult.fillPathForRange(
            start = 0,
            end = 11,
            target = target,
            cornerRadius = CornerRadius(4f, 4f),
            mergeLines = true,
        )

        assertThat(target.isEmpty).isFalse()
        val bounds = target.getBounds()
        assertThat(bounds.top).isWithin(0.5f).of(boxes.first().top)
        assertThat(bounds.bottom).isWithin(0.5f).of(boxes.last().bottom)
    }

    @Test
    fun textLayoutResult_fillPathForRange_reusesAndClearsDirtyTargetPath() {
        val layoutResult = simpleTextLayoutResult(text = "abcdef", fontSize = 10.sp)
        val target =
            Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(500f, 500f, 800f, 800f), Path.Direction.Clockwise)
            }

        layoutResult.fillPathForRange(start = 1, end = 3, target = target, mergeLines = true)
        assertThat(target.getBounds().left).isWithin(0.5f).of(10f)
        assertThat(target.getBounds().right).isWithin(0.5f).of(30f)
        assertThat(target.fillType).isEqualTo(PathFillType.EvenOdd)

        // Calling again with an empty range rewinds the reused target Path while preserving
        // fillType
        layoutResult.fillPathForRange(start = 2, end = 2, target = target, mergeLines = true)
        assertThat(target.isEmpty).isTrue()
        assertThat(target.fillType).isEqualTo(PathFillType.EvenOdd)
    }

    @Test
    fun fillPathForRange_mergeLinesFalse_matchesGetPathForRange_singleLineSubstring() {
        val layoutResult = simpleTextLayoutResult(text = "abcdefgh", fontSize = 10.sp)
        val fillPath = Path()
        layoutResult.fillPathForRange(
            start = 2,
            end = 6,
            target = fillPath,
            mergeLines = false,
        )
        val rangePath = layoutResult.getPathForRange(2, 6)

        assertPathsEquivalent(fillPath, rangePath)
    }

    @Test
    fun fillPathForRange_mergeLinesFalse_matchesGetPathForRange_multiLineSubstring() {
        // Soft-wrap "abcdefghijkl" (12 chars * 10px) at width = 40f onto 3 lines of 4 chars each
        val layoutResult =
            simpleTextLayoutResult(text = "abcdefghijkl", fontSize = 10.sp, width = 40f)
        assertThat(layoutResult.lineCount).isEqualTo(3)

        val fillPath = Path()
        layoutResult.fillPathForRange(
            start = 1,
            end = 11,
            target = fillPath,
            mergeLines = false,
        )
        val rangePath = layoutResult.getPathForRange(1, 11)

        assertPathsEquivalent(fillPath, rangePath)
    }

    @Test
    fun fillPathForRange_mergeLinesFalse_matchesGetPathForRange_emptyRange() {
        val layoutResult = simpleTextLayoutResult(text = "abcdefgh", fontSize = 10.sp)
        val fillPath = Path()
        layoutResult.fillPathForRange(
            start = 4,
            end = 4,
            target = fillPath,
            mergeLines = false,
        )
        val rangePath = layoutResult.getPathForRange(4, 4)

        assertThat(fillPath.isEmpty).isTrue()
        assertThat(rangePath.isEmpty).isTrue()
    }

    @Test
    fun fillPathForRange_mergeLinesFalse_matchesGetPathForRange_multiParagraph() {
        val annotatedString = buildAnnotatedString {
            withStyle(ParagraphStyle()) { append("abcd") }
            withStyle(ParagraphStyle()) { append("efgh") }
        }
        val layoutResult = simpleTextLayoutResult(text = annotatedString, fontSize = 10.sp)

        val fillPath = Path()
        layoutResult.fillPathForRange(
            start = 1,
            end = 7,
            target = fillPath,
            mergeLines = false,
        )
        val rangePath = layoutResult.getPathForRange(1, 7)

        assertPathsEquivalent(fillPath, rangePath)
    }

    @Test
    fun fillPathForRange_mergeLinesFalse_matchesGetPathForRange_customLineHeight() {
        val layoutResult =
            simpleTextLayoutResult(
                text = "abcdefgh",
                style = TextStyle(lineHeight = 24.sp),
                fontSize = 10.sp,
                width = 40f,
            )

        val fillPath = Path()
        layoutResult.fillPathForRange(
            start = 1,
            end = 7,
            target = fillPath,
            mergeLines = false,
        )
        val rangePath = layoutResult.getPathForRange(1, 7)

        assertPathsEquivalent(fillPath, rangePath)
    }

    @Test
    fun fillPathForRange_mergeLinesFalse_matchesGetPathForRange_bidiMixedDirection() {
        // LTR + RTL without spaces at range boundary: "abc\u05D0\u05D1\u05D2def"
        // Range [1, 5) spans 'b', 'c', '\u05D0', '\u05D1' (two disjoint visual runs)
        val text = "abc\u05D0\u05D1\u05D2def"
        val layoutResult =
            simpleTextLayoutResult(
                text = AnnotatedString(text),
                style = TextStyle(fontSize = 10.sp, textDirection = TextDirection.Ltr),
                width = 1000f,
                localeList = ltrLocaleList,
            )

        val fillPath = Path()
        layoutResult.fillPathForRange(
            start = 1,
            end = 5,
            target = fillPath,
            mergeLines = false,
        )
        val rangePath = layoutResult.getPathForRange(1, 5)

        assertPathsEquivalent(fillPath, rangePath)
    }

    @Test
    fun fillPathForRange_ellipticalCornerRadius_preservesHorizontalRadiusWhenVerticalIsSmaller() {
        // With fontFamilyMeasureFont at 20.sp (density 1f):
        // Line 0 ("aaaaaa"): [0, 0, 120, 20] (maxRx = 60, right step maxRy = 10)
        // Line 1 ("aaaa"):   [0, 20, 80, 40] (step seam width = 40 -> maxRx = 20, maxRy = 10)
        val layoutResult = simpleTextLayoutResult(text = "aaaaaa\naaaa", fontSize = 20.sp)

        val path = Path()
        layoutResult.fillPathForRange(
            start = 0,
            end = layoutResult.layoutInput.text.length,
            target = path,
            cornerRadius = CornerRadius(18f, 6f),
            mergeLines = true,
        )

        // All 6 polygon corners (5 convex outer corners + 1 concave inner step corner) have
        // maxRx >= 20f and maxRy >= 10f, so rx = 18f and ry = 6f must both be preserved in full
        // on every Quadratic corner segment rather than clamping rx down to maxRy (10f) or ry (6f).
        val cornerCurves =
            path.iterator().asSequence().filter { it.type == PathSegment.Type.Quadratic }.toList()
        assertThat(cornerCurves).hasSize(6)
        for (curve in cornerCurves) {
            val rx = abs(curve.points[4] - curve.points[0])
            val ry = abs(curve.points[5] - curve.points[1])
            assertThat(rx).isWithin(0.01f).of(18f)
            assertThat(ry).isWithin(0.01f).of(6f)
        }
    }

    @Test
    fun fillPathForRange_ellipticalCornerRadiusOverflowingVerticalEdge_scalesUniformlyPreservingAspectRatio() {
        // Polygon vertices clockwise:
        // v0=(0,0) [maxRx=60, maxRy=20], v1=(120,0) [maxRx=60, maxRy=10],
        // v2=(120,20) [maxRx=20, maxRy=10], v3=(80,20) [maxRx=20, maxRy=10],
        // v4=(80,40) [maxRx=40, maxRy=10], v5=(0,40) [maxRx=40, maxRy=20].
        val layoutResult = simpleTextLayoutResult(text = "aaaaaa\naaaa", fontSize = 20.sp)

        // 1. Vertical overflow: CornerRadius(24f, 16f) has 3:2 aspect ratio.
        // At v0 & v5 (maxRy = 20f), scale = 1f -> (rx = 24f, ry = 16f).
        // At v1..v4 (maxRy = 10f < 16f), scale = 10f / 16f = 0.625f -> (rx = 15f, ry = 10f).
        val verticalOverflowPath = Path()
        layoutResult.fillPathForRange(
            start = 0,
            end = layoutResult.layoutInput.text.length,
            target = verticalOverflowPath,
            cornerRadius = CornerRadius(24f, 16f),
            mergeLines = true,
        )
        val verticalCurves =
            verticalOverflowPath
                .iterator()
                .asSequence()
                .filter { it.type == PathSegment.Type.Quadratic }
                .toList()
        assertThat(verticalCurves).hasSize(6)
        for ((index, curve) in verticalCurves.withIndex()) {
            val rx = abs(curve.points[4] - curve.points[0])
            val ry = abs(curve.points[5] - curve.points[1])
            assertThat(rx / ry).isWithin(0.01f).of(1.5f)
            if (index == 0 || index == 5) {
                assertThat(rx).isWithin(0.01f).of(24f)
                assertThat(ry).isWithin(0.01f).of(16f)
            } else {
                assertThat(rx).isWithin(0.01f).of(15f)
                assertThat(ry).isWithin(0.01f).of(10f)
            }
        }

        // 2. Horizontal overflow at step seam: CornerRadius(30f, 8f) has 15:4 aspect ratio.
        // At v2 & v3 (maxRx = 20f < 30f, maxRy = 10f), scale = 20f / 30f = 2/3 -> (rx = 20f, ry =
        // 16f / 3f).
        val horizontalOverflowPath = Path()
        layoutResult.fillPathForRange(
            start = 0,
            end = layoutResult.layoutInput.text.length,
            target = horizontalOverflowPath,
            cornerRadius = CornerRadius(30f, 8f),
            mergeLines = true,
        )
        val horizontalCurves =
            horizontalOverflowPath
                .iterator()
                .asSequence()
                .filter { it.type == PathSegment.Type.Quadratic }
                .toList()
        assertThat(horizontalCurves).hasSize(6)
        for ((index, curve) in horizontalCurves.withIndex()) {
            val rx = abs(curve.points[4] - curve.points[0])
            val ry = abs(curve.points[5] - curve.points[1])
            assertThat(rx / ry).isWithin(0.01f).of(3.75f)
            if (index == 2 || index == 3) {
                assertThat(rx).isWithin(0.01f).of(20f)
                assertThat(ry).isWithin(0.01f).of(16f / 3f)
            } else {
                assertThat(rx).isWithin(0.01f).of(30f)
                assertThat(ry).isWithin(0.01f).of(8f)
            }
        }
    }

    @Test
    fun fillPathForRange_reusedAcrossDifferentRanges_doesNotLeakScratchState() {
        val text = "abcdef\nabcd\nabc\u05D0\u05D1\u05D2def"
        val reusedLayout =
            simpleTextLayoutResult(
                text = AnnotatedString(text),
                style = TextStyle(fontSize = 10.sp, textDirection = TextDirection.Ltr),
                localeList = ltrLocaleList,
            )
        val reusedPath = Path()

        // 1. Multi-line merged range populate pass (grows scratch rectCoords + vertices)
        reusedLayout.fillPathForRange(
            start = 0,
            end = 11,
            target = reusedPath,
            cornerRadius = CornerRadius(4f),
            mergeLines = true,
        )
        assertThat(reusedPath.isEmpty).isFalse()

        // 2. Empty range resets target cleanly without leaking previous vertices
        reusedLayout.fillPathForRange(start = 4, end = 4, target = reusedPath)
        assertThat(reusedPath.isEmpty).isTrue()

        // 3. Interleave getBoundingBoxesForRange + BiDi unmerged + smaller single/multi-line ranges
        // and verify each matches a brand-new TextLayoutResult + fresh Path instance.
        val queries =
            listOf(
                Triple(13, 17, false),
                Triple(0, 4, true),
                Triple(2, 10, true),
                Triple(0, text.length, false),
            )
        for ((start, end, mergeLines) in queries) {
            val boxesFromReused = reusedLayout.getBoundingBoxesForRange(start, end)
            reusedLayout.fillPathForRange(
                start = start,
                end = end,
                target = reusedPath,
                cornerRadius = CornerRadius(4f, 2f),
                mergeLines = mergeLines,
            )

            val freshLayout =
                simpleTextLayoutResult(
                    text = AnnotatedString(text),
                    style = TextStyle(fontSize = 10.sp, textDirection = TextDirection.Ltr),
                    localeList = ltrLocaleList,
                )
            assertThat(boxesFromReused).isEqualTo(freshLayout.getBoundingBoxesForRange(start, end))
            val freshPath = Path()
            freshLayout.fillPathForRange(
                start = start,
                end = end,
                target = freshPath,
                cornerRadius = CornerRadius(4f, 2f),
                mergeLines = mergeLines,
            )
            assertThat(Path.combine(PathOperation.Xor, reusedPath, freshPath).isEmpty).isTrue()
        }

        // 4. Exercise >64 distinct (start, end) ranges to verify bounded cache eviction
        val longText = "a".repeat(80)
        val longReusedLayout = simpleTextLayoutResult(text = longText, fontSize = 10.sp)
        val longFreshLayout = simpleTextLayoutResult(text = longText, fontSize = 10.sp)
        for (end in 1..70) {
            longReusedLayout.fillPathForRange(start = 0, end = end, target = reusedPath)
        }
        longReusedLayout.fillPathForRange(start = 2, end = 9, target = reusedPath)
        val expectedAfterEviction = Path()
        longFreshLayout.fillPathForRange(start = 2, end = 9, target = expectedAfterEviction)
        assertThat(Path.combine(PathOperation.Xor, reusedPath, expectedAfterEviction).isEmpty)
            .isTrue()
    }

    private fun assertPathsEquivalent(actual: Path, expected: Path) {
        val actualBounds = actual.getBounds()
        val expectedBounds = expected.getBounds()
        assertThat(actualBounds.left).isWithin(1.0f).of(expectedBounds.left)
        assertThat(actualBounds.top).isWithin(1.0f).of(expectedBounds.top)
        assertThat(actualBounds.right).isWithin(1.0f).of(expectedBounds.right)
        assertThat(actualBounds.bottom).isWithin(1.0f).of(expectedBounds.bottom)

        val xorPath = Path.combine(PathOperation.Xor, actual, expected)
        val xorBounds = xorPath.getBounds()
        // Any symmetric difference between the two paths must have negligible area (at most 1px
        // rounding along an edge).
        val diffArea = xorBounds.width * xorBounds.height
        val totalArea = expectedBounds.width * expectedBounds.height
        assertThat(diffArea).isAtMost(totalArea * 0.05f + 1f)
    }

    @Test
    fun calculatePath_withTextLayoutResultLineIndices_mergesOnlyConsecutiveLines() {
        // Line 0 ("abcdef", 60px), Line 1 (empty "\n"), Line 2 ("abcd", 40px)
        val layoutResult = simpleTextLayoutResult(text = "abcdef\n\nabcd", fontSize = 10.sp)

        // Even though only lines 0 and 2 have boxes, they are non-consecutive (separated by line 1)
        // so mergeLines = true keeps them as two separate contours with an empty gap at line 1.
        val nonConsecutivePath =
            layoutResult.calculatePathFromBoundingBoxes(
                start = 0,
                end = layoutResult.layoutInput.text.length,
                mergeLines = true,
            )
        val gapProbe = Path().apply { addRect(Rect(0f, 12f, 40f, 18f)) }
        val intersection =
            Path().apply { op(nonConsecutivePath, gapProbe, PathOperation.Intersect) }
        assertThat(intersection.isEmpty).isTrue()
    }

    @Test
    fun calculateBoundingBoxList_beyondLaidOutParagraphs_returnsEmpty() {
        // Two paragraphs ("01234\n56789") constrained to maxLines = 1 drops the second paragraph
        // so paragraphInfoList.last().endIndex < layoutInput.text.length.
        val layoutResult =
            simpleTextLayoutResult(
                text = "01234\n56789",
                fontSize = 10.sp,
                maxLines = 1,
            )
        assertThat(layoutResult.calculateBoundingBoxList(2, 2)).isEmpty()
        assertThat(layoutResult.calculateBoundingBoxList(6, 11)).isEmpty()
    }

    private fun simpleTextLayoutResult(
        text: String,
        style: TextStyle? = null,
        fontSize: TextUnit = TextUnit.Unspecified,
        maxLines: Int = Int.MAX_VALUE,
        overflow: TextOverflow = TextOverflow.Clip,
        width: Float = Float.MAX_VALUE,
        localeList: LocaleList? = null,
    ): TextLayoutResult =
        simpleTextLayoutResult(
            text = AnnotatedString(text),
            style = style,
            fontSize = fontSize,
            maxLines = maxLines,
            overflow = overflow,
            width = width,
            localeList = localeList,
        )

    private fun simpleTextLayoutResult(
        text: AnnotatedString,
        style: TextStyle? = null,
        fontSize: TextUnit = TextUnit.Unspecified,
        maxLines: Int = Int.MAX_VALUE,
        overflow: TextOverflow = TextOverflow.Clip,
        width: Float = Float.MAX_VALUE,
        localeList: LocaleList? = null,
    ): TextLayoutResult {
        val mergedStyle =
            TextStyle(
                    fontFamily = fontFamilyMeasureFont,
                    fontSize = fontSize,
                    localeList = localeList,
                )
                .merge(style)
        val maxWidth = if (width == Float.MAX_VALUE) Constraints.Infinity else ceil(width).toInt()
        val constraints = Constraints(maxWidth = maxWidth)
        val fontFamilyResolver = UncachedFontFamilyResolver(context)
        val multiParagraph =
            MultiParagraph(
                annotatedString = text,
                style = mergedStyle,
                maxLines = maxLines,
                constraints = constraints,
                density = defaultDensity,
                fontFamilyResolver = fontFamilyResolver,
                defaultLocaleList = ltrLocaleList,
                overflow = overflow,
            )
        return TextLayoutResult(
            layoutInput =
                TextLayoutInput(
                    text = text,
                    style = mergedStyle,
                    placeholders = emptyList(),
                    maxLines = maxLines,
                    softWrap = true,
                    overflow = overflow,
                    density = defaultDensity,
                    layoutDirection = LayoutDirection.Ltr,
                    fontFamilyResolver = fontFamilyResolver,
                    defaultLocaleList = ltrLocaleList,
                    constraints = constraints,
                ),
            multiParagraph = multiParagraph,
            size = IntSize(ceil(multiParagraph.width).toInt(), ceil(multiParagraph.height).toInt()),
        )
    }
}
