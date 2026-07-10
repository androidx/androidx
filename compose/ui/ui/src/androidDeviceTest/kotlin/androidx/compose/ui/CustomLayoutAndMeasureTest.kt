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

package androidx.compose.ui

import android.os.Build
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.Ref
import androidx.compose.ui.platform.AndroidOwnerExtraAssertionsRule
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toOffset
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class CustomLayoutAndMeasureTest {
    @get:Rule val rule = createAndroidComposeRule<TestActivity>(StandardTestDispatcher())
    @get:Rule val excessiveAssertions = AndroidOwnerExtraAssertionsRule()
    private lateinit var activity: TestActivity
    private lateinit var density: Density

    @Before
    fun setup() {
        activity = rule.activity
        activity.hasFocusLatch.await(5, TimeUnit.SECONDS)
        density = Density(activity)
    }

    @Test
    fun multiChildLayoutTest() {
        val childrenCount = 3
        val childConstraints =
            arrayOf(Constraints(), Constraints.fixedWidth(50), Constraints.fixedHeight(50))
        val headerChildrenCount = 1
        val footerChildrenCount = 2

        rule.setContent {
            val header =
                @Composable {
                    Layout(
                        measurePolicy = { _, constraints ->
                            assertEquals(childConstraints[0], constraints)
                            layout(0, 0) {}
                        },
                        content = {},
                        modifier = Modifier.layoutId("header"),
                    )
                }
            val footer =
                @Composable {
                    Layout(
                        measurePolicy = { _, constraints ->
                            assertEquals(childConstraints[1], constraints)
                            layout(0, 0) {}
                        },
                        content = {},
                        modifier = Modifier.layoutId("footer"),
                    )
                    Layout(
                        measurePolicy = { _, constraints ->
                            assertEquals(childConstraints[2], constraints)
                            layout(0, 0) {}
                        },
                        content = {},
                        modifier = Modifier.layoutId("footer"),
                    )
                }

            Layout({
                header()
                footer()
            }) { measurables, _ ->
                assertEquals(childrenCount, measurables.size)
                measurables.forEachIndexed { index, measurable ->
                    measurable.measure(childConstraints[index])
                }
                val measurablesHeader = measurables.filter { it.layoutId == "header" }
                val measurablesFooter = measurables.filter { it.layoutId == "footer" }
                assertEquals(headerChildrenCount, measurablesHeader.size)
                assertSame(measurables[0], measurablesHeader[0])
                assertEquals(footerChildrenCount, measurablesFooter.size)
                assertSame(measurables[1], measurablesFooter[0])
                assertSame(measurables[2], measurablesFooter[1])
                layout(0, 0) {}
            }
        }
        rule.waitForIdle()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun measureInLayoutDoesNotAffectParentSize() {
        val white = Color(0xFFFFFFFF)
        val blue = Color(0xFF000080)
        val model = SquareModel(outerColor = blue, innerColor = white)
        var measureCalls = 0
        var layoutCalls = 0

        rule.setContent {
            Layout(
                modifier = remember { Modifier.drawBehind { drawRect(model.outerColor) } },
                content = {
                    AtLeastSize(
                        size = model.size,
                        modifier = Modifier.drawBehind { drawRect(model.innerColor) },
                    )
                },
                measurePolicy =
                    remember {
                        MeasurePolicy { measurables, constraints ->
                            measureCalls++
                            layout(30, 30) {
                                layoutCalls++
                                val placeable = measurables[0].measure(constraints)
                                placeable.place(
                                    (30 - placeable.width) / 2,
                                    (30 - placeable.height) / 2,
                                )
                            }
                        }
                    },
            )
        }

        validateSquareColors(outerColor = blue, innerColor = white, size = 10)

        layoutCalls = 0
        measureCalls = 0
        rule.runOnIdle { model.size = 20 }

        validateSquareColors(outerColor = blue, innerColor = white, size = 20, totalSize = 30)
        assertEquals(0, measureCalls)
        assertEquals(1, layoutCalls)
    }

    @Test
    fun testLayout_whenMeasuringIsDoneDuringPlacing() {
        @Composable
        fun FixedSizeRow(width: Int, height: Int, content: @Composable () -> Unit) {
            Layout(
                content = content,
                measurePolicy = { measurables, constraints ->
                    val resolvedWidth = constraints.constrainWidth(width)
                    val resolvedHeight = constraints.constrainHeight(height)
                    layout(resolvedWidth, resolvedHeight) {
                        val childConstraints =
                            Constraints(0, Constraints.Infinity, resolvedHeight, resolvedHeight)
                        var left = 0
                        for (measurable in measurables) {
                            val placeable = measurable.measure(childConstraints)
                            if (left + placeable.width > width) {
                                break
                            }
                            placeable.place(left, 0)
                            left += placeable.width
                        }
                    }
                },
            )
        }

        @Composable
        fun FixedWidthBox(
            width: Int,
            measured: Ref<Boolean?>,
            laidOut: Ref<Boolean?>,
            drawn: Ref<Boolean?>,
        ) {
            Layout(
                content = {},
                modifier = Modifier.drawBehind { drawn.value = true },
                measurePolicy = { _, constraints ->
                    measured.value = true
                    val resolvedWidth = constraints.constrainWidth(width)
                    val resolvedHeight = constraints.minHeight
                    layout(resolvedWidth, resolvedHeight) { laidOut.value = true }
                },
            )
        }

        val childrenCount = 5
        val measured = Array(childrenCount) { Ref<Boolean?>() }
        val laidOut = Array(childrenCount) { Ref<Boolean?>() }
        val drawn = Array(childrenCount) { Ref<Boolean?>() }
        rule.setContent {
            Align {
                FixedSizeRow(width = 90, height = 40) {
                    for (i in 0 until childrenCount) {
                        FixedWidthBox(
                            width = 30,
                            measured = measured[i],
                            laidOut = laidOut[i],
                            drawn = drawn[i],
                        )
                    }
                }
            }
        }
        rule.runOnIdle {
            for (i in 0 until childrenCount) {
                assertEquals(i <= 3, measured[i].value ?: false)
                assertEquals(i <= 2, laidOut[i].value ?: false)
                assertEquals(i <= 2, drawn[i].value ?: false)
            }
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testRelayoutOnNewChild() {
        val drawChild = mutableStateOf(false)

        val outerColor = Color(0xFF000080)
        val innerColor = Color(0xFFFFFFFF)
        rule.setContent {
            AtLeastSize(size = 30, modifier = Modifier.fillColor(outerColor)) {
                if (drawChild.value) {
                    Padding(size = 20) {
                        AtLeastSize(size = 20, modifier = Modifier.fillColor(innerColor)) {}
                    }
                }
            }
        }

        // The padded area doesn't draw
        validateSquareColors(outerColor = outerColor, innerColor = outerColor, size = 10)

        rule.runOnIdle { drawChild.value = true }

        validateSquareColors(outerColor = outerColor, innerColor = innerColor, size = 20)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testRedrawOnRemovedChild() {
        val drawChild = mutableStateOf(true)

        val outerColor = Color(0xFF000080)
        val innerColor = Color(0xFFFFFFFF)
        rule.setContent {
            AtLeastSize(size = 30, modifier = Modifier.drawBehind { drawRect(outerColor) }) {
                AtLeastSize(size = 30) {
                    if (drawChild.value) {
                        Padding(size = 10) {
                            AtLeastSize(
                                size = 10,
                                modifier = Modifier.drawBehind { drawRect(innerColor) },
                            )
                        }
                    }
                }
            }
        }

        validateSquareColors(outerColor = outerColor, innerColor = innerColor, size = 10)

        rule.runOnIdle { drawChild.value = false }

        // The padded area doesn't draw
        validateSquareColors(outerColor = outerColor, innerColor = outerColor, size = 10)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testRelayoutOnRemovedChild() {
        val drawChild = mutableStateOf(true)

        val outerColor = Color(0xFF000080)
        val innerColor = Color(0xFFFFFFFF)
        rule.setContent {
            AtLeastSize(size = 30, modifier = Modifier.drawBehind { drawRect(outerColor) }) {
                Padding(size = 20) {
                    if (drawChild.value) {
                        AtLeastSize(
                            size = 20,
                            modifier = Modifier.drawBehind { drawRect(innerColor) },
                        )
                    }
                }
            }
        }

        validateSquareColors(outerColor = outerColor, innerColor = innerColor, size = 20)

        rule.runOnIdle { drawChild.value = false }

        // The padded area doesn't draw
        validateSquareColors(outerColor = outerColor, innerColor = outerColor, size = 10)
    }

    @Test
    fun testLayoutBeforeDraw_forRecomposingNodesNotAffectingRootSize() {
        val offset = mutableStateOf(0)
        var laidOut = false
        rule.setContent {
            val container =
                @Composable { content: @Composable () -> Unit ->
                    // This simulates a Container optimisation, when the child does not
                    // affect parent size.
                    Layout(content) { measurables, constraints ->
                        layout(30, 30) { measurables[0].measure(constraints).place(0, 0) }
                    }
                }
            val recomposingChild =
                @Composable { content: @Composable (Int) -> Unit ->
                    // This simulates a child that recomposes, for example due to a transition.
                    content(offset.value)
                }
            val assumeLayoutBeforeDraw =
                @Composable { value: Int ->
                    // This assumes a layout was done before the draw pass.
                    Layout(
                        content = {},
                        modifier =
                            Modifier.drawBehind {
                                assertEquals(offset.value, value)
                                assertTrue(laidOut)
                            },
                    ) { _, _ ->
                        laidOut = true
                        layout(0, 0) {}
                    }
                }

            container { recomposingChild { assumeLayoutBeforeDraw(it) } }
        }

        rule.runOnIdle { offset.value = 10 }
        rule.waitForIdle()
    }

    @Test
    fun testZeroSizeCanRelayout() {
        val model = SquareModel(size = 0)
        var modelMeasuredSize = -1
        rule.setContent {
            Layout(content = {}) { _, _ ->
                modelMeasuredSize = model.size
                layout(model.size, model.size) {}
            }
        }

        rule.runOnIdle {
            assertEquals(0, modelMeasuredSize)
            model.size = 10
        }
        rule.runOnIdle { assertEquals(10, modelMeasuredSize) }
    }

    @Test
    fun testZeroSizeCanRelayout_child() {
        val model = SquareModel(size = 0)
        var layoutSize = -1
        rule.setContent {
            Layout(
                content = {
                    Layout(content = {}) { _, _ ->
                        layoutSize = model.size
                        layout(model.size, model.size) {}
                    }
                }
            ) { measurables, constraints ->
                val placeable = measurables[0].measure(constraints)
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
        }

        rule.runOnIdle {
            assertEquals(0, layoutSize)
            model.size = 10
        }
        rule.runOnIdle { assertEquals(10, layoutSize) }
    }

    @Test
    fun testZeroSizeCanRelayout_childRepaintBoundary() {
        val model = SquareModel(size = 0)
        var layoutSize = -1
        rule.setContent {
            Layout(
                content = {
                    Layout(modifier = Modifier.graphicsLayer(), content = {}) { _, _ ->
                        layoutSize = model.size
                        layout(model.size, model.size) {}
                    }
                }
            ) { measurables, constraints ->
                val placeable = measurables[0].measure(constraints)
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
        }

        rule.runOnIdle {
            assertEquals(0, layoutSize)
            model.size = 10
        }
        rule.runOnIdle { assertEquals(10, layoutSize) }
    }

    @Test
    fun layoutModifier_testLayoutDirection() {
        val layoutDirection = Ref<LayoutDirection>()

        val layoutModifier =
            object : LayoutModifier {
                override fun MeasureScope.measure(
                    measurable: Measurable,
                    constraints: Constraints,
                ): MeasureResult {
                    layoutDirection.value = this.layoutDirection
                    return layout(0, 0) {}
                }
            }
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                FixedSize(size = 50, modifier = layoutModifier)
            }
        }
        rule.waitForIdle()
        assertEquals(LayoutDirection.Rtl, layoutDirection.value)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun layoutModifier_redrawsCorrectlyWhenOnlyNonModifiedSizeChanges() {
        val blue = Color(0xFF000080)
        val green = Color(0xFF00FF00)
        val offset = mutableStateOf(10)

        rule.setContent {
            FixedSize(30, modifier = Modifier.drawBehind { drawRect(green) }) {
                FixedSize(
                    offset.value,
                    modifier = AlignTopLeft.graphicsLayer().drawBehind { drawRect(blue) },
                ) {}
            }
        }
        validateSquareColors(outerColor = green, innerColor = blue, size = 10, offset = -10)

        rule.runOnIdle { offset.value = 20 }
        validateSquareColors(
            outerColor = green,
            innerColor = blue,
            size = 20,
            offset = -5,
            totalSize = 30,
        )
    }

    @Test
    fun layoutModifier_convenienceApi() {
        val size = 100
        val offset = 15
        var resultCoordinates: LayoutCoordinates? = null

        rule.setContent {
            FixedSize(
                size = size,
                modifier =
                    Modifier.layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)
                            layout(placeable.width, placeable.height) {
                                placeable.place(offset, offset)
                            }
                        }
                        .onGloballyPositioned { resultCoordinates = it },
            )
        }

        rule.runOnIdle {
            assertEquals(size, resultCoordinates?.size?.height)
            assertEquals(size, resultCoordinates?.size?.width)
            assertEquals(IntOffset(offset, offset).toOffset(), resultCoordinates!!.positionInRoot())
        }
    }

    @Test
    fun layoutModifier_convenienceApi_equivalent() {
        val size = 100
        val offset = 15
        val latch = CountDownLatch(2)

        var convenienceCoordinates: LayoutCoordinates? = null
        var coordinates: LayoutCoordinates? = null

        rule.setContent {
            FixedSize(
                size = size,
                modifier =
                    Modifier.layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)
                            layout(placeable.width, placeable.height) {
                                placeable.place(offset, offset)
                            }
                        }
                        .onGloballyPositioned {
                            convenienceCoordinates = it
                            latch.countDown()
                        },
            )

            val layoutModifier =
                object : LayoutModifier {
                    override fun MeasureScope.measure(
                        measurable: Measurable,
                        constraints: Constraints,
                    ): MeasureResult {
                        val placeable = measurable.measure(constraints)
                        return layout(placeable.width, placeable.height) {
                            placeable.place(offset, offset)
                        }
                    }
                }
            FixedSize(
                size = size,
                modifier =
                    layoutModifier.onGloballyPositioned {
                        coordinates = it
                        latch.countDown()
                    },
            )
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))

        rule.runOnIdle {
            assertEquals(coordinates?.size?.height, convenienceCoordinates?.size?.height)
            assertEquals(coordinates?.size?.width, convenienceCoordinates?.size?.width)
            assertEquals(coordinates?.positionInRoot(), convenienceCoordinates?.positionInRoot())
        }
    }

    @Test
    fun requestRemeasureForAlreadyMeasuredChildWhileTheParentIsStillMeasuring() {
        var lastLayoutValue = false
        rule.setContent {
            Layout(
                content = {
                    val state = remember { mutableStateOf(false) }
                    Layout(content = {}, modifier = Modifier.drawBehind {}) { _, _ ->
                        lastLayoutValue = state.value
                        // this registers the value read
                        if (!state.value) {
                            // change the value right inside the measure block
                            // it will cause one more remeasure pass as we also read this value
                            state.value = true
                        }
                        layout(100, 100) {}
                    }
                    FixedSize(30, content = {})
                }
            ) { measurables, constraints ->
                val (first, second) = measurables
                val firstPlaceable = first.measure(constraints)
                // switch frame, as inside the measure block we changed the model value
                // this will trigger requestRemeasure on this first layout
                Snapshot.sendApplyNotifications()
                val secondPlaceable = second.measure(constraints)
                layout(30, 30) {
                    firstPlaceable.place(0, 0)
                    secondPlaceable.place(0, 0)
                }
            }
        }
        rule.runOnIdle { assertTrue(lastLayoutValue) }
    }

    @Test
    fun placeableMeasuredSize() =
        with(density) {
            val realSize = 100.dp
            val constrainedSize = 50.dp
            var measuredSize = IntSize.Zero
            var placeableSize = IntSize.Zero
            rule.setContent {
                Layout(content = { Box(Modifier.requiredSize(realSize)) }) { measurables, _ ->
                    val placeable =
                        measurables[0].measure(
                            Constraints.fixed(
                                constrainedSize.roundToPx(),
                                constrainedSize.roundToPx(),
                            )
                        )
                    measuredSize = IntSize(placeable.measuredWidth, placeable.measuredHeight)
                    placeableSize = IntSize(placeable.width, placeable.height)
                    assertEquals(realSize.roundToPx(), placeable.measuredWidth)
                    assertEquals(realSize.roundToPx(), placeable.measuredHeight)
                    assertEquals(constrainedSize.roundToPx(), placeable.width)
                    assertEquals(constrainedSize.roundToPx(), placeable.height)
                    layout(1, 1) {}
                }
            }
            rule.runOnIdle {
                assertEquals(realSize.roundToPx(), measuredSize.width)
                assertEquals(realSize.roundToPx(), measuredSize.height)
                assertEquals(constrainedSize.roundToPx(), placeableSize.width)
                assertEquals(constrainedSize.roundToPx(), placeableSize.height)
            }
        }

    @Test
    fun noRemeasureWhenWeStopUsingStateInMeasuring() =
        with(density) {
            val counter = mutableStateOf(0)
            var parentRemeasures = 0
            val measurePolicy =
                mutableStateOf(
                    MeasurePolicy { measurables, constraints ->
                        counter.value
                        parentRemeasures++
                        measurables.first().measure(constraints)
                        layout(1, 1) {}
                    }
                )
            rule.setContent {
                Layout(
                    content = {
                        Layout(content = {}) { _, _ ->
                            counter.value
                            layout(1, 1) {}
                        }
                    },
                    measurePolicy = measurePolicy.value,
                )
            }

            rule.runOnIdle { assertEquals(1, parentRemeasures) }

            measurePolicy.value = MeasurePolicy { measurables, constraints ->
                // not using counter anymore
                parentRemeasures++
                measurables.first().measure(constraints)
                layout(1, 1) {}
            }

            rule.runOnIdle { assertEquals(2, parentRemeasures) }

            counter.value = 1

            rule.runOnIdle { assertEquals(2, parentRemeasures) }
        }

    @Test
    fun updatingModifierIsNotCausingParentsRelayout() {
        var parentLayoutsCount = 0
        var modifier by mutableStateOf(Modifier.layout(onLayout = { println("1") }))
        val parentMeasurePolicy = MeasurePolicy { measurables, constraints ->
            val placeable = measurables.first().measure(constraints)
            layout(placeable.width, placeable.height) {
                parentLayoutsCount++
                placeable.place(0, 0)
            }
        }
        rule.setContent {
            Layout(
                content = { Layout({}, modifier) { _, _ -> layout(10, 10) {} } },
                measurePolicy = parentMeasurePolicy,
            )
        }
        rule.runOnIdle {
            assertEquals(1, parentLayoutsCount)
            modifier = Modifier.layout(onLayout = { println("2") })
        }

        rule.runOnIdle { assertEquals(1, parentLayoutsCount) }
    }

    @Test
    fun instancesKeepDelegates() {
        var color by mutableStateOf(Color.Red)
        var size by mutableStateOf(30)
        var m: Measurable? = null
        val layoutCaptureModifier =
            object : LayoutModifier {
                override fun MeasureScope.measure(
                    measurable: Measurable,
                    constraints: Constraints,
                ): MeasureResult {
                    m = measurable
                    val p = measurable.measure(constraints)
                    return layout(p.width, p.height) { p.place(0, 0) }
                }
            }
        rule.setContent {
            FixedSize(size = size, modifier = layoutCaptureModifier.background(color)) {}
        }
        rule.waitForIdle()
        val firstMeasurable = m

        rule.runOnIdle {
            m = null
            size = 40
            color = Color.Blue
        }

        rule.waitForIdle()
        assertNotNull(m)
        assertSame(firstMeasurable, m)
    }

    @Test
    fun replaceMultiImplementationModifier() {
        var color by mutableStateOf(Color.Red)
        var m: Measurable? = null

        class SpecialModifier : DrawModifier, LayoutModifier {
            override fun ContentDrawScope.draw() {
                drawContent()
            }

            override fun MeasureScope.measure(
                measurable: Measurable,
                constraints: Constraints,
            ): MeasureResult {
                val placeable = measurable.measure(constraints)
                return layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
        }

        val layoutCaptureModifier =
            object : LayoutModifier {
                override fun MeasureScope.measure(
                    measurable: Measurable,
                    constraints: Constraints,
                ): MeasureResult {
                    m = measurable
                    val p = measurable.measure(constraints)
                    return layout(p.width, p.height) { p.place(0, 0) }
                }
            }
        rule.setContent {
            FixedSize(30, layoutCaptureModifier.then(SpecialModifier()).background(color)) {}
        }
        rule.waitForIdle()
        val firstMeasurable = m

        rule.runOnIdle {
            m = null
            color = Color.Blue
        }

        rule.waitForIdle()
        // The new instance's measurable is the same.
        assertNotNull(m)
        assertSame(firstMeasurable, m)
    }

    @Test
    fun modifiers_validateCorrectSizes() {
        val layoutModifier =
            object : LayoutModifier {
                override fun MeasureScope.measure(
                    measurable: Measurable,
                    constraints: Constraints,
                ): MeasureResult {
                    val placeable = measurable.measure(constraints)
                    return layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                }
            }
        val parentDataModifier =
            object : ParentDataModifier {
                override fun Density.modifyParentData(parentData: Any?) = parentData
            }
        val size = 50

        val childSizes = arrayOfNulls<IntSize>(2)
        rule.setContent {
            Layout(
                content = {
                    FixedSize(size, layoutModifier)
                    FixedSize(size, parentDataModifier)
                },
                measurePolicy = { measurables, constraints ->
                    for (i in measurables.indices) {
                        val child = measurables[i]
                        val placeable = child.measure(constraints)
                        childSizes[i] = IntSize(placeable.width, placeable.height)
                    }
                    layout(0, 0) {}
                },
            )
        }
        rule.waitForIdle()
        assertEquals(IntSize(size, size), childSizes[0]!!)
        assertEquals(IntSize(size, size), childSizes[1]!!)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun validateSquareColors(
        outerColor: Color,
        innerColor: Color,
        size: Int,
        offset: Int = 0,
        totalSize: Int = size * 3,
    ) {
        rule.validateSquareColors(outerColor, innerColor, size, offset, totalSize)
    }

    private fun Modifier.fillColor(color: Color): Modifier = drawBehind { drawRect(color) }

    private fun Modifier.layout(onLayout: () -> Unit) = layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            onLayout()
            placeable.place(0, 0)
        }
    }
}
