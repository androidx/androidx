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

package androidx.compose.ui.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.FixedSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.node.Ref
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class RtlLayoutTest {
    @get:Rule val rule = createAndroidComposeRule<TestActivity>(StandardTestDispatcher())
    internal lateinit var density: Density
    internal lateinit var position: Array<Ref<Offset>>
    private val size = 100

    @Before
    fun setup() {
        density = Density(rule.activity)
        position = Array(3) { Ref<Offset>() }
    }

    @Test
    fun customLayout_absolutePositioning() =
        with(density) {
            rule.setContent { CustomLayout(true, LayoutDirection.Ltr) }

            rule.waitForIdle()
            assertEquals(Offset(0f, 0f), position[0].value)
            assertEquals(Offset(size.toFloat(), size.toFloat()), position[1].value)
            assertEquals(Offset((size * 2).toFloat(), (size * 2).toFloat()), position[2].value)
        }

    @Test
    fun customLayout_absolutePositioning_rtl() =
        with(density) {
            rule.setContent { CustomLayout(true, LayoutDirection.Rtl) }

            rule.waitForIdle()
            assertEquals(Offset(0f, 0f), position[0].value)
            assertEquals(Offset(size.toFloat(), size.toFloat()), position[1].value)
            assertEquals(Offset((size * 2).toFloat(), (size * 2).toFloat()), position[2].value)
        }

    @Test
    fun customLayout_positioning() =
        with(density) {
            rule.setContent { CustomLayout(false, LayoutDirection.Ltr) }

            rule.waitForIdle()
            assertEquals(Offset(0f, 0f), position[0].value)
            assertEquals(Offset(size.toFloat(), size.toFloat()), position[1].value)
            assertEquals(Offset((size * 2).toFloat(), (size * 2).toFloat()), position[2].value)
        }

    @Test
    fun customLayout_positioning_rtl() =
        with(density) {
            rule.setContent { CustomLayout(false, LayoutDirection.Rtl) }

            rule.waitForIdle()
            assertEquals(Offset((size * 2).toFloat(), 0f), position[0].value)
            assertEquals(Offset(size.toFloat(), size.toFloat()), position[1].value)
            assertEquals(Offset(0f, (size * 2).toFloat()), position[2].value)
        }

    @Test
    fun customLayout_updatingDirectionCausesRemeasure() {
        val direction = mutableStateOf(LayoutDirection.Rtl)
        var actualDirection: LayoutDirection? = null

        rule.setContent {
            val children =
                @Composable {
                    Layout({}) { _, _ ->
                        actualDirection = layoutDirection
                        layout(100, 100) {}
                    }
                }
            CompositionLocalProvider(LocalLayoutDirection provides direction.value) {
                Layout(children) { measurables, constraints ->
                    layout(100, 100) {
                        measurables.first().measure(constraints).placeRelative(0, 0)
                    }
                }
            }
        }
        rule.waitForIdle()
        assertEquals(LayoutDirection.Rtl, actualDirection)

        rule.runOnUiThread { direction.value = LayoutDirection.Ltr }

        rule.waitForIdle()
        assertEquals(LayoutDirection.Ltr, actualDirection)
    }

    @Test
    fun testModifiedLayoutDirection_inMeasureScope() {
        val resultLayoutDirection = Ref<LayoutDirection>()

        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Layout(content = {}) { _, _ ->
                    resultLayoutDirection.value = layoutDirection
                    layout(0, 0) {}
                }
            }
        }

        rule.waitForIdle()
        assertTrue(LayoutDirection.Rtl == resultLayoutDirection.value)
    }

    @Test
    fun testModifiedLayoutDirection_inIntrinsicsMeasure() {
        var resultLayoutDirection: LayoutDirection? = null

        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                val measurePolicy =
                    object : MeasurePolicy {
                        override fun MeasureScope.measure(
                            measurables: List<Measurable>,
                            constraints: Constraints,
                        ) = layout(0, 0) {}

                        override fun IntrinsicMeasureScope.minIntrinsicWidth(
                            measurables: List<IntrinsicMeasurable>,
                            height: Int,
                        ) = 0

                        override fun IntrinsicMeasureScope.minIntrinsicHeight(
                            measurables: List<IntrinsicMeasurable>,
                            width: Int,
                        ) = 0

                        override fun IntrinsicMeasureScope.maxIntrinsicWidth(
                            measurables: List<IntrinsicMeasurable>,
                            height: Int,
                        ): Int {
                            resultLayoutDirection = this.layoutDirection
                            return 0
                        }

                        override fun IntrinsicMeasureScope.maxIntrinsicHeight(
                            measurables: List<IntrinsicMeasurable>,
                            width: Int,
                        ) = 0
                    }
                Layout(
                    content = {},
                    modifier = Modifier.width(IntrinsicSize.Max),
                    measurePolicy = measurePolicy,
                )
            }
        }

        rule.waitForIdle()
        Assert.assertNotNull(resultLayoutDirection)
        assertTrue(LayoutDirection.Rtl == resultLayoutDirection)
    }

    @Test
    fun testRestoreLocaleLayoutDirection() {
        val resultLayoutDirection = Ref<LayoutDirection>()

        rule.setContent {
            val initialLayoutDirection = LocalLayoutDirection.current
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Box {
                    CompositionLocalProvider(LocalLayoutDirection provides initialLayoutDirection) {
                        Layout({}) { _, _ ->
                            resultLayoutDirection.value = layoutDirection
                            layout(0, 0) {}
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        assertEquals(LayoutDirection.Ltr, resultLayoutDirection.value)
    }

    @Test
    fun testChildGetsPlacedWithinContainerWithPaddingAndMinimumTouchTarget() {
        // copy-pasted from TouchTarget.kt (internal in material module)
        class MinimumTouchTargetModifier(val size: DpSize = DpSize(48.dp, 48.dp)) : LayoutModifier {
            override fun MeasureScope.measure(
                measurable: Measurable,
                constraints: Constraints,
            ): MeasureResult {
                val placeable = measurable.measure(constraints)
                val width = maxOf(placeable.width, size.width.roundToPx())
                val height = maxOf(placeable.height, size.height.roundToPx())
                return layout(width, height) {
                    val centerX = ((width - placeable.width) / 2f).roundToInt()
                    val centerY = ((height - placeable.height) / 2f).roundToInt()
                    placeable.place(centerX, centerY)
                }
            }

            override fun equals(other: Any?): Boolean {
                val otherModifier = other as? MinimumTouchTargetModifier ?: return false
                return size == otherModifier.size
            }

            override fun hashCode(): Int = size.hashCode()
        }

        var outerLC: LayoutCoordinates? = null
        var innerLC: LayoutCoordinates? = null
        var density: Density? = null

        val rowWidth = 200.dp
        val outerBoxWidth = 56.dp
        val padding = 16.dp

        rule.setContent {
            density = LocalDensity.current
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Row(modifier = Modifier.width(rowWidth)) {
                    Box(
                        modifier =
                            Modifier.onGloballyPositioned { outerLC = it }
                                .size(outerBoxWidth)
                                .background(color = Color.Red)
                                .padding(horizontal = padding)
                                .then(MinimumTouchTargetModifier())
                    ) {
                        Box(
                            modifier =
                                Modifier.onGloballyPositioned { innerLC = it }
                                    .size(30.dp)
                                    .background(color = Color.Gray)
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        val (innerOffset, innerWidth) = with(innerLC!!) { localToWindow(Offset.Zero) to size.width }
        val (outerOffset, outerWidth) = with(outerLC!!) { localToWindow(Offset.Zero) to size.width }
        assertTrue(innerWidth < outerWidth)
        assertTrue(innerOffset.x > outerOffset.x)
        assertTrue(innerWidth + innerOffset.x < outerWidth + outerOffset.x)

        with(density!!) {
            assertEquals(outerOffset.x.roundToInt(), rowWidth.roundToPx() - outerWidth)
            val paddingPx = padding.roundToPx()
            // OuterBoxLeftEdge_padding-16dp_InnerBoxLeftEdge
            assertTrue(abs(outerOffset.x + paddingPx - innerOffset.x) <= 1.0)
            // InnerBoxRightEdge_padding-16dp_OuterRightEdge
            val outerRightEdge = outerOffset.x + outerWidth
            assertTrue(abs(outerRightEdge - paddingPx - (innerOffset.x + innerWidth)) <= 1)
        }
    }

    @Composable
    private fun CustomLayout(absolutePositioning: Boolean, testLayoutDirection: LayoutDirection) {
        CompositionLocalProvider(LocalLayoutDirection provides testLayoutDirection) {
            Layout(
                content = {
                    FixedSize(size, modifier = Modifier.saveLayoutInfo(position[0]))
                    FixedSize(size, modifier = Modifier.saveLayoutInfo(position[1]))
                    FixedSize(size, modifier = Modifier.saveLayoutInfo(position[2]))
                }
            ) { measurables, constraints ->
                val placeables = measurables.map { it.measure(constraints) }
                val width = placeables.fold(0) { sum, p -> sum + p.width }
                val height = placeables.fold(0) { sum, p -> sum + p.height }
                layout(width, height) {
                    var x = 0
                    var y = 0
                    for (placeable in placeables) {
                        if (absolutePositioning) {
                            placeable.place(x, y)
                        } else {
                            placeable.placeRelative(x, y)
                        }
                        x += placeable.width
                        y += placeable.height
                    }
                }
            }
        }
    }

    private fun Modifier.saveLayoutInfo(position: Ref<Offset>): Modifier = onGloballyPositioned {
        position.value = it.localToRoot(Offset(0f, 0f))
    }
}
