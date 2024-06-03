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

package androidx.window.embedding

import android.graphics.Rect
import androidx.window.core.Bounds
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowLayoutInfo
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/** Unit tests for [EmbeddingBounds] */
class EmbeddingBoundsTests {

    private val taskBounds = Bounds(0, 0, 10, 10)

    private val layoutInfoWithoutHinge = WindowLayoutInfo(emptyList())

    private val layoutInfoWithTwoHinges =
        WindowLayoutInfo(
            listOf(
                TestFoldingFeature(Bounds(left = 4, top = 0, right = 6, bottom = 10)),
                TestFoldingFeature(Bounds(left = 0, top = 4, right = 10, bottom = 6)),
            )
        )

    private val layoutInfoWithVerticalHinge =
        WindowLayoutInfo(
            listOf(TestFoldingFeature(Bounds(left = 4, top = 0, right = 6, bottom = 10)))
        )

    private val layoutInfoWithHorizontalHinge =
        WindowLayoutInfo(
            listOf(TestFoldingFeature(Bounds(left = 0, top = 4, right = 10, bottom = 6)))
        )

    @Test
    fun testTranslateBOUNDS_EXPANDED_returnEmptyBounds() {
        assertThat(
                EmbeddingBounds.translateEmbeddingBounds(
                        EmbeddingBounds.BOUNDS_EXPANDED,
                        taskBounds,
                        layoutInfoWithVerticalHinge
                    )
                    .isZero
            )
            .isTrue()
    }

    @Test
    fun testTranslateBoundsMatchParentTask_returnEmptyBounds() {
        assertThat(
                EmbeddingBounds.translateEmbeddingBounds(
                        EmbeddingBounds(
                            EmbeddingBounds.Alignment.ALIGN_TOP,
                            width = EmbeddingBounds.Dimension.pixel(taskBounds.width),
                            height = EmbeddingBounds.Dimension.pixel(taskBounds.height),
                        ),
                        taskBounds,
                        layoutInfoWithVerticalHinge
                    )
                    .isZero
            )
            .isTrue()
    }

    @Test
    fun testTranslateBoundsLargerThanParentTask_returnEmptyBounds() {
        assertThat(
                EmbeddingBounds.translateEmbeddingBounds(
                        EmbeddingBounds(
                            EmbeddingBounds.Alignment.ALIGN_TOP,
                            width = EmbeddingBounds.Dimension.pixel(taskBounds.width + 1),
                            height = EmbeddingBounds.Dimension.pixel(taskBounds.height + 1),
                        ),
                        taskBounds,
                        layoutInfoWithVerticalHinge
                    )
                    .isZero
            )
            .isTrue()
    }

    @Test
    fun testTranslateBOUNDS_HINGE_LEFT() {
        val fallbackBoundsHingeLeft = Bounds(left = 0, top = 0, right = 5, bottom = 10)

        assertWithMessage("Must fallback to the left half on device without hinge")
            .that(
                EmbeddingBounds.translateEmbeddingBounds(
                    EmbeddingBounds.BOUNDS_HINGE_LEFT,
                    taskBounds,
                    layoutInfoWithoutHinge,
                )
            )
            .isEqualTo(fallbackBoundsHingeLeft)

        assertWithMessage("Must fallback to the left half on device with multiple hinges")
            .that(
                EmbeddingBounds.translateEmbeddingBounds(
                    EmbeddingBounds.BOUNDS_HINGE_LEFT,
                    taskBounds,
                    layoutInfoWithTwoHinges,
                )
            )
            .isEqualTo(fallbackBoundsHingeLeft)

        assertWithMessage("Must fallback to the left half on device with a horizontal hinge")
            .that(
                EmbeddingBounds.translateEmbeddingBounds(
                    EmbeddingBounds.BOUNDS_HINGE_LEFT,
                    taskBounds,
                    layoutInfoWithHorizontalHinge,
                )
            )
            .isEqualTo(fallbackBoundsHingeLeft)

        assertWithMessage("Must report bounds on the left of hinge on device with a vertical hinge")
            .that(
                EmbeddingBounds.translateEmbeddingBounds(
                    EmbeddingBounds.BOUNDS_HINGE_LEFT,
                    taskBounds,
                    layoutInfoWithVerticalHinge,
                )
            )
            .isEqualTo(Bounds(left = 0, top = 0, right = 4, bottom = 10))
    }

    @Test
    fun testTranslateBOUNDS_HINGE_TOP() {
        val fallbackBoundsHingeTop = Bounds(left = 0, top = 0, right = 10, bottom = 5)

        assertWithMessage("Must fallback to the top half on device without hinge")
            .that(
                EmbeddingBounds.translateEmbeddingBounds(
                    EmbeddingBounds.BOUNDS_HINGE_TOP,
                    taskBounds,
                    layoutInfoWithoutHinge
                )
            )
            .isEqualTo(fallbackBoundsHingeTop)

        assertWithMessage("Must fallback to the top half on device with multiple hinges")
            .that(
                EmbeddingBounds.translateEmbeddingBounds(
                    EmbeddingBounds.BOUNDS_HINGE_TOP,
                    taskBounds,
                    layoutInfoWithTwoHinges,
                )
            )
            .isEqualTo(fallbackBoundsHingeTop)

        assertWithMessage("Must fallback to the top half on device with a vertical hinge")
            .that(
                EmbeddingBounds.translateEmbeddingBounds(
                    EmbeddingBounds.BOUNDS_HINGE_TOP,
                    taskBounds,
                    layoutInfoWithVerticalHinge,
                )
            )
            .isEqualTo(fallbackBoundsHingeTop)

        assertWithMessage(
                "Must report bounds on the top of hinge on device with a horizontal hinge"
            )
            .that(
                EmbeddingBounds.translateEmbeddingBounds(
                    EmbeddingBounds.BOUNDS_HINGE_TOP,
                    taskBounds,
                    layoutInfoWithHorizontalHinge,
                )
            )
            .isEqualTo(Bounds(left = 0, top = 0, right = 10, bottom = 4))
    }

    @Test
    fun testTranslateBOUNDS_HINGE_RIGHT() {
        val fallbackBoundsHingeRight = Bounds(left = 5, top = 0, right = 10, bottom = 10)

        assertWithMessage("Must fallback to the right half on device without hinge")
            .that(
                EmbeddingBounds.translateEmbeddingBounds(
                    EmbeddingBounds.BOUNDS_HINGE_RIGHT,
                    taskBounds,
                    layoutInfoWithoutHinge,
                )
            )
            .isEqualTo(fallbackBoundsHingeRight)

        assertWithMessage("Must fallback to the right half on device with multiple hinges")
            .that(
                EmbeddingBounds.translateEmbeddingBounds(
                    EmbeddingBounds.BOUNDS_HINGE_RIGHT,
                    taskBounds,
                    layoutInfoWithTwoHinges,
                )
            )
            .isEqualTo(fallbackBoundsHingeRight)

        assertWithMessage("Must fallback to the right half on device with a horizontal hinge")
            .that(
                EmbeddingBounds.translateEmbeddingBounds(
                    EmbeddingBounds.BOUNDS_HINGE_RIGHT,
                    taskBounds,
                    layoutInfoWithHorizontalHinge,
                )
            )
            .isEqualTo(fallbackBoundsHingeRight)

        assertWithMessage(
                "Must report bounds on the right of hinge on device with a vertical hinge"
            )
            .that(
                EmbeddingBounds.translateEmbeddingBounds(
                    EmbeddingBounds.BOUNDS_HINGE_RIGHT,
                    taskBounds,
                    layoutInfoWithVerticalHinge,
                )
            )
            .isEqualTo(Bounds(left = 6, top = 0, right = 10, bottom = 10))
    }

    @Test
    fun testTranslateBOUNDS_HINGE_BOTTOM() {
        val fallbackBoundsHingeBottom = Bounds(left = 0, top = 5, right = 10, bottom = 10)

        assertWithMessage("Must fallback to the bottom half on device without hinge")
            .that(
                EmbeddingBounds.translateEmbeddingBounds(
                    EmbeddingBounds.BOUNDS_HINGE_BOTTOM,
                    taskBounds,
                    layoutInfoWithoutHinge,
                )
            )
            .isEqualTo(fallbackBoundsHingeBottom)

        assertWithMessage("Must fallback to the bottom half on device with multiple hinges")
            .that(
                EmbeddingBounds.translateEmbeddingBounds(
                    EmbeddingBounds.BOUNDS_HINGE_BOTTOM,
                    taskBounds,
                    layoutInfoWithTwoHinges,
                )
            )
            .isEqualTo(fallbackBoundsHingeBottom)

        assertWithMessage("Must fallback to the bottom half on device with a vertical hinge")
            .that(
                EmbeddingBounds.translateEmbeddingBounds(
                    EmbeddingBounds.BOUNDS_HINGE_BOTTOM,
                    taskBounds,
                    layoutInfoWithVerticalHinge,
                )
            )
            .isEqualTo(fallbackBoundsHingeBottom)

        assertWithMessage(
                "Must report bounds on the bottom of hinge on device with a horizontal hinge"
            )
            .that(
                EmbeddingBounds.translateEmbeddingBounds(
                    EmbeddingBounds.BOUNDS_HINGE_BOTTOM,
                    taskBounds,
                    layoutInfoWithHorizontalHinge,
                )
            )
            .isEqualTo(Bounds(left = 0, top = 6, right = 10, bottom = 10))
    }

    @Test
    fun testTranslateShrunkLeftBounds() {
        assertThat(
                EmbeddingBounds.translateEmbeddingBounds(
                    EmbeddingBounds(
                        EmbeddingBounds.Alignment.ALIGN_LEFT,
                        EmbeddingBounds.Dimension.ratio(0.7f),
                        EmbeddingBounds.Dimension.pixel(8),
                    ),
                    taskBounds,
                    layoutInfoWithoutHinge,
                )
            )
            .isEqualTo(Bounds(left = 0, top = 1, right = 7, bottom = 9))
    }

    @Test
    fun testTranslateShrunkTopBounds() {
        assertThat(
                EmbeddingBounds.translateEmbeddingBounds(
                    EmbeddingBounds(
                        EmbeddingBounds.Alignment.ALIGN_TOP,
                        EmbeddingBounds.Dimension.pixel(8),
                        EmbeddingBounds.Dimension.ratio(0.5f),
                    ),
                    taskBounds,
                    layoutInfoWithoutHinge,
                )
            )
            .isEqualTo(Bounds(left = 1, top = 0, right = 9, bottom = 5))
    }

    @Test
    fun testTranslateShrunkBottomBounds() {
        assertThat(
                EmbeddingBounds.translateEmbeddingBounds(
                    EmbeddingBounds(
                        EmbeddingBounds.Alignment.ALIGN_BOTTOM,
                        EmbeddingBounds.Dimension.DIMENSION_HINGE,
                        EmbeddingBounds.Dimension.DIMENSION_EXPANDED,
                    ),
                    taskBounds,
                    layoutInfoWithoutHinge,
                )
            )
            .isEqualTo(Bounds(left = 2, top = 0, right = 7, bottom = 10))
    }

    @Test
    fun testTranslateShrunkRightBounds() {
        assertThat(
                EmbeddingBounds.translateEmbeddingBounds(
                    EmbeddingBounds(
                        EmbeddingBounds.Alignment.ALIGN_RIGHT,
                        EmbeddingBounds.Dimension.DIMENSION_HINGE,
                        EmbeddingBounds.Dimension.pixel(4),
                    ),
                    taskBounds,
                    layoutInfoWithVerticalHinge,
                )
            )
            .isEqualTo(Bounds(left = 6, top = 3, right = 10, bottom = 7))
    }

    private class TestFoldingFeature(val rawBounds: Bounds) : FoldingFeature {
        override val bounds: Rect
            get() =
                mock<Rect>().apply {
                    left = rawBounds.left
                    top = rawBounds.top
                    right = rawBounds.right
                    bottom = rawBounds.bottom
                    doReturn(rawBounds.width).whenever(this).width()
                    doReturn(rawBounds.height).whenever(this).height()
                }

        override val isSeparating: Boolean
            get() = true

        override val occlusionType: FoldingFeature.OcclusionType
            get() = FoldingFeature.OcclusionType.FULL

        override val orientation: FoldingFeature.Orientation
            get() =
                if (rawBounds.width > rawBounds.height) {
                    FoldingFeature.Orientation.HORIZONTAL
                } else {
                    FoldingFeature.Orientation.VERTICAL
                }

        override val state: FoldingFeature.State
            get() = FoldingFeature.State.FLAT
    }
}
