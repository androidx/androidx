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

package androidx.compose.material3.adaptive.layout

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.Ruler
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PaneMarginsModifierTest {
    @Test
    fun unspecifiedPaneMargins_alwaysUseMeasuredValue() {
        assertThat(with(PaneMargins.Unspecified) { MockPlacementScope().getPaneLeft(50) })
            .isEqualTo(50)
        assertThat(with(PaneMargins.Unspecified) { MockPlacementScope().getPaneTop(70) })
            .isEqualTo(70)
        assertThat(
                with(PaneMargins.Unspecified) {
                    MockPlacementScope().getPaneRight(30, MockLayoutWidth)
                }
            )
            .isEqualTo(30)
        assertThat(
                with(PaneMargins.Unspecified) {
                    MockPlacementScope().getPaneBottom(60, MockLayoutHeight)
                }
            )
            .isEqualTo(60)
    }

    @Test
    fun getPaneTop_noMarginsSet_useMeasuredTop() {
        val mockPaneMargins =
            PaneMarginsImpl(PaddingValues(), emptyList(), MockDensity, MockLayoutDirection)
        assertThat(with(mockPaneMargins) { MockPlacementScope().getPaneTop(50) }).isEqualTo(50)
    }

    @Test
    fun getPaneTop_noWindowInsets_useFixedMargins() {
        val mockPaneMargins =
            PaneMarginsImpl(
                PaddingValues(0.dp, 100.dp, 0.dp, 0.dp),
                emptyList(),
                MockDensity,
                MockLayoutDirection
            )
        assertThat(with(mockPaneMargins) { MockPlacementScope().getPaneTop(0) }).isEqualTo(100)
    }

    @Test
    fun getPaneTop_multipleWindowInsets_useLargestOne() {
        val mockPaneMargins =
            PaneMarginsImpl(
                PaddingValues(),
                listOf(MockWindowInsetRulers1, MockWindowInsetRulers2, MockWindowInsetRulers3),
                MockDensity,
                MockLayoutDirection
            )
        assertThat(
                with(mockPaneMargins) {
                    MockPlacementScope(mockInset1Top = 30, mockInset2Top = 60, mockInset3Top = 10)
                        .getPaneTop(0)
                }
            )
            .isEqualTo(60)
    }

    @Test
    fun getPaneTop_withFixedMarginsAndWindowInsets_useLargestOne() {
        val mockPaneMargins =
            PaneMarginsImpl(
                PaddingValues(0.dp, 100.dp, 0.dp, 0.dp),
                listOf(MockWindowInsetRulers1, MockWindowInsetRulers2, MockWindowInsetRulers3),
                MockDensity,
                MockLayoutDirection
            )
        assertThat(
                with(mockPaneMargins) {
                    MockPlacementScope(mockInset1Top = 30, mockInset2Top = 60, mockInset3Top = 10)
                        .getPaneTop(0)
                }
            )
            .isEqualTo(100)
    }

    @Test
    fun getPaneTop_whenMeasuredTopIsLarger_useMeasuredTop() {
        val mockPaneMargins =
            PaneMarginsImpl(
                PaddingValues(0.dp, 100.dp, 0.dp, 0.dp),
                listOf(MockWindowInsetRulers1, MockWindowInsetRulers2, MockWindowInsetRulers3),
                MockDensity,
                MockLayoutDirection
            )
        assertThat(
                with(mockPaneMargins) {
                    MockPlacementScope(mockInset1Top = 30, mockInset2Top = 60, mockInset3Top = 10)
                        .getPaneTop(140)
                }
            )
            .isEqualTo(140)
    }

    @Test
    fun getPaneBottom_noMarginsSet_useMeasuredBottom() {
        val mockPaneMargins =
            PaneMarginsImpl(PaddingValues(), emptyList(), MockDensity, MockLayoutDirection)
        assertThat(
                with(mockPaneMargins) { MockPlacementScope().getPaneBottom(850, MockLayoutHeight) }
            )
            .isEqualTo(850)
    }

    @Test
    fun getPaneBottom_noWindowInsets_useFixedMargins() {
        val mockPaneMargins =
            PaneMarginsImpl(
                PaddingValues(0.dp, 0.dp, 0.dp, 100.dp),
                emptyList(),
                MockDensity,
                MockLayoutDirection
            )
        assertThat(
                with(mockPaneMargins) { MockPlacementScope().getPaneBottom(1024, MockLayoutHeight) }
            )
            .isEqualTo(MockLayoutHeight - 100)
    }

    @Test
    fun getPaneBottom_multipleWindowInsets_useSmallestOne() {
        val mockPaneMargins =
            PaneMarginsImpl(
                PaddingValues(),
                listOf(MockWindowInsetRulers1, MockWindowInsetRulers2, MockWindowInsetRulers3),
                MockDensity,
                MockLayoutDirection
            )
        assertThat(
                with(mockPaneMargins) {
                    MockPlacementScope(
                            mockInset1Bottom = 930,
                            mockInset2Bottom = 960,
                            mockInset3Bottom = 910
                        )
                        .getPaneBottom(1024, MockLayoutHeight)
                }
            )
            .isEqualTo(910)
    }

    @Test
    fun getPaneBottom_withFixedMarginsAndWindowInsets_useSmallestOne() {
        val mockPaneMargins =
            PaneMarginsImpl(
                PaddingValues(0.dp, 0.dp, 0.dp, 200.dp),
                listOf(MockWindowInsetRulers1, MockWindowInsetRulers2, MockWindowInsetRulers3),
                MockDensity,
                MockLayoutDirection
            )
        assertThat(
                with(mockPaneMargins) {
                    MockPlacementScope(
                            mockInset1Bottom = 930,
                            mockInset2Bottom = 960,
                            mockInset3Bottom = 910
                        )
                        .getPaneBottom(1024, MockLayoutHeight)
                }
            )
            .isEqualTo(MockLayoutHeight - 200)
    }

    @Test
    fun getPaneBottom_whenMeasuredBottomIsSmaller_useMeasuredBottom() {
        val mockPaneMargins =
            PaneMarginsImpl(
                PaddingValues(0.dp, 0.dp, 0.dp, 200.dp),
                listOf(MockWindowInsetRulers1, MockWindowInsetRulers2, MockWindowInsetRulers3),
                MockDensity,
                MockLayoutDirection
            )
        assertThat(
                with(mockPaneMargins) {
                    MockPlacementScope(
                            mockInset1Bottom = 930,
                            mockInset2Bottom = 960,
                            mockInset3Bottom = 910
                        )
                        .getPaneBottom(800, MockLayoutHeight)
                }
            )
            .isEqualTo(800)
    }

    @Test
    fun getPaneLeft_noMarginsSet_useMeasuredLeft() {
        val mockPaneMargins =
            PaneMarginsImpl(PaddingValues(), emptyList(), MockDensity, MockLayoutDirection)
        assertThat(with(mockPaneMargins) { MockPlacementScope().getPaneLeft(50) }).isEqualTo(50)
    }

    @Test
    fun getPaneLeft_noWindowInsets_useFixedMargins() {
        val mockPaneMargins =
            PaneMarginsImpl(
                PaddingValues(100.dp, 0.dp, 0.dp, 0.dp),
                emptyList(),
                MockDensity,
                MockLayoutDirection
            )
        assertThat(with(mockPaneMargins) { MockPlacementScope().getPaneLeft(0) }).isEqualTo(100)
    }

    @Test
    fun getPaneLeft_withRtlDirection_usePaddingEnd() {
        val mockPaneMargins =
            PaneMarginsImpl(
                PaddingValues(0.dp, 0.dp, 110.dp, 0.dp),
                emptyList(),
                MockDensity,
                LayoutDirection.Rtl
            )
        assertThat(with(mockPaneMargins) { MockPlacementScope().getPaneLeft(0) }).isEqualTo(110)
    }

    @Test
    fun getPaneLeft_multipleWindowInsets_useLargestOne() {
        val mockPaneMargins =
            PaneMarginsImpl(
                PaddingValues(),
                listOf(MockWindowInsetRulers1, MockWindowInsetRulers2, MockWindowInsetRulers3),
                MockDensity,
                MockLayoutDirection
            )
        assertThat(
                with(mockPaneMargins) {
                    MockPlacementScope(
                            mockInset1Left = 30,
                            mockInset2Left = 60,
                            mockInset3Left = 10
                        )
                        .getPaneLeft(0)
                }
            )
            .isEqualTo(60)
    }

    @Test
    fun getPaneLeft_withFixedMarginsAndWindowInsets_useLargestOne() {
        val mockPaneMargins =
            PaneMarginsImpl(
                PaddingValues(100.dp, 0.dp, 0.dp, 0.dp),
                listOf(MockWindowInsetRulers1, MockWindowInsetRulers2, MockWindowInsetRulers3),
                MockDensity,
                MockLayoutDirection
            )
        assertThat(
                with(mockPaneMargins) {
                    MockPlacementScope(
                            mockInset1Left = 30,
                            mockInset2Left = 60,
                            mockInset3Left = 10
                        )
                        .getPaneLeft(0)
                }
            )
            .isEqualTo(100)
    }

    @Test
    fun getPaneLeft_whenMeasuredLeftIsLarger_useMeasuredLeft() {
        val mockPaneMargins =
            PaneMarginsImpl(
                PaddingValues(100.dp, 0.dp, 0.dp, 0.dp),
                listOf(MockWindowInsetRulers1, MockWindowInsetRulers2, MockWindowInsetRulers3),
                MockDensity,
                MockLayoutDirection
            )
        assertThat(
                with(mockPaneMargins) {
                    MockPlacementScope(
                            mockInset1Left = 30,
                            mockInset2Left = 60,
                            mockInset3Left = 10
                        )
                        .getPaneLeft(140)
                }
            )
            .isEqualTo(140)
    }

    @Test
    fun getPaneRight_noMarginsSet_useMeasuredRight() {
        val mockPaneMargins =
            PaneMarginsImpl(PaddingValues(), emptyList(), MockDensity, MockLayoutDirection)
        assertThat(
                with(mockPaneMargins) { MockPlacementScope().getPaneRight(850, MockLayoutWidth) }
            )
            .isEqualTo(850)
    }

    @Test
    fun getPaneRight_noWindowInsets_useFixedMargins() {
        val mockPaneMargins =
            PaneMarginsImpl(
                PaddingValues(0.dp, 0.dp, 100.dp, 0.dp),
                emptyList(),
                MockDensity,
                MockLayoutDirection
            )
        assertThat(
                with(mockPaneMargins) { MockPlacementScope().getPaneRight(1280, MockLayoutWidth) }
            )
            .isEqualTo(MockLayoutWidth - 100)
    }

    @Test
    fun getPaneRight_withRtlDirection_usePaddingStart() {
        val mockPaneMargins =
            PaneMarginsImpl(
                PaddingValues(110.dp, 0.dp, 0.dp, 0.dp),
                emptyList(),
                MockDensity,
                LayoutDirection.Rtl
            )
        assertThat(
                with(mockPaneMargins) { MockPlacementScope().getPaneRight(1280, MockLayoutWidth) }
            )
            .isEqualTo(MockLayoutWidth - 110)
    }

    @Test
    fun getPaneRight_multipleWindowInsets_useSmallestOne() {
        val mockPaneMargins =
            PaneMarginsImpl(
                PaddingValues(),
                listOf(MockWindowInsetRulers1, MockWindowInsetRulers2, MockWindowInsetRulers3),
                MockDensity,
                MockLayoutDirection
            )
        assertThat(
                with(mockPaneMargins) {
                    MockPlacementScope(
                            mockInset1Right = 930,
                            mockInset2Right = 960,
                            mockInset3Right = 910
                        )
                        .getPaneRight(1280, MockLayoutWidth)
                }
            )
            .isEqualTo(910)
    }

    @Test
    fun getPaneRight_withFixedMarginsAndWindowInsets_useSmallestOne() {
        val mockPaneMargins =
            PaneMarginsImpl(
                PaddingValues(0.dp, 0.dp, 200.dp, 0.dp),
                listOf(MockWindowInsetRulers1, MockWindowInsetRulers2, MockWindowInsetRulers3),
                MockDensity,
                MockLayoutDirection
            )
        assertThat(
                with(mockPaneMargins) {
                    MockPlacementScope(
                            mockInset1Right = 930,
                            mockInset2Right = 960,
                            mockInset3Right = 910
                        )
                        .getPaneRight(1280, MockLayoutWidth)
                }
            )
            .isEqualTo(910)
    }

    @Test
    fun getPaneRight_whenMeasuredRightIsSmaller_useMeasuredRight() {
        val mockPaneMargins =
            PaneMarginsImpl(
                PaddingValues(0.dp, 0.dp, 200.dp, 0.dp),
                listOf(MockWindowInsetRulers1, MockWindowInsetRulers2, MockWindowInsetRulers3),
                MockDensity,
                MockLayoutDirection
            )
        assertThat(
                with(mockPaneMargins) {
                    MockPlacementScope(
                            mockInset1Right = 930,
                            mockInset2Right = 960,
                            mockInset3Right = 910
                        )
                        .getPaneRight(800, MockLayoutWidth)
                }
            )
            .isEqualTo(800)
    }
}

private val MockDensity = Density(1f)
private val MockLayoutDirection = LayoutDirection.Ltr
private const val MockLayoutWidth = 1280
private const val MockLayoutHeight = 1024

private val MockWindowInsetRulers1 = WindowInsetsRulers()
private val MockWindowInsetRulers2 = WindowInsetsRulers()
private val MockWindowInsetRulers3 = WindowInsetsRulers()

private class MockPlacementScope(
    val mockInset1Left: Int = 0,
    val mockInset1Top: Int = 0,
    val mockInset1Right: Int = 0,
    val mockInset1Bottom: Int = 0,
    val mockInset2Left: Int = 0,
    val mockInset2Top: Int = 0,
    val mockInset2Right: Int = 0,
    val mockInset2Bottom: Int = 0,
    val mockInset3Left: Int = 0,
    val mockInset3Top: Int = 0,
    val mockInset3Right: Int = 0,
    val mockInset3Bottom: Int = 0,
) : Placeable.PlacementScope() {
    override val parentWidth = MockLayoutWidth
    override val parentLayoutDirection = MockLayoutDirection

    override fun Ruler.current(defaultValue: Float): Float =
        when (this) {
            MockWindowInsetRulers1.left -> mockInset1Left.toFloat()
            MockWindowInsetRulers1.top -> mockInset1Top.toFloat()
            MockWindowInsetRulers1.right -> mockInset1Right.toFloat()
            MockWindowInsetRulers1.bottom -> mockInset1Bottom.toFloat()
            MockWindowInsetRulers2.left -> mockInset2Left.toFloat()
            MockWindowInsetRulers2.top -> mockInset2Top.toFloat()
            MockWindowInsetRulers2.right -> mockInset2Right.toFloat()
            MockWindowInsetRulers2.bottom -> mockInset2Bottom.toFloat()
            MockWindowInsetRulers3.left -> mockInset3Left.toFloat()
            MockWindowInsetRulers3.top -> mockInset3Top.toFloat()
            MockWindowInsetRulers3.right -> mockInset3Right.toFloat()
            MockWindowInsetRulers3.bottom -> mockInset3Bottom.toFloat()
            else -> 0f
        }
}
