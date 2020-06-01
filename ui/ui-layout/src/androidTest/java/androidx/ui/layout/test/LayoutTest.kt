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

package androidx.ui.layout.test

import android.app.Activity
import android.os.Handler
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.compose.Composable
import androidx.compose.Recomposer
import androidx.test.rule.ActivityTestRule
import androidx.ui.core.Alignment
import androidx.ui.core.AlignmentLine
import androidx.ui.core.AndroidOwner
import androidx.ui.core.Constraints
import androidx.ui.core.Layout
import androidx.ui.core.LayoutDirection
import androidx.ui.core.Modifier
import androidx.ui.core.Placeable
import androidx.ui.core.Ref
import androidx.ui.core.enforce
import androidx.ui.core.hasFixedHeight
import androidx.ui.core.hasFixedWidth
import androidx.ui.core.offset
import androidx.ui.core.onPositioned
import androidx.ui.core.setContent
import androidx.ui.layout.Arrangement
import androidx.ui.layout.Constraints
import androidx.ui.layout.DpConstraints
import androidx.ui.layout.InnerPadding
import androidx.ui.unit.Density
import androidx.ui.unit.Dp
import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition
import androidx.ui.unit.PxSize
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
import androidx.ui.unit.isFinite
import androidx.ui.unit.max
import androidx.ui.unit.px
import androidx.ui.unit.round
import androidx.ui.unit.toPx
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

open class LayoutTest {
    @get:Rule
    val activityTestRule = ActivityTestRule<TestActivity>(
        TestActivity::class.java
    )
    lateinit var activity: TestActivity
    lateinit var handler: Handler
    internal lateinit var density: Density

    @Before
    fun setup() {
        activity = activityTestRule.activity
        density = Density(activity)
        activity.hasFocusLatch.await(5, TimeUnit.SECONDS)

        // Kotlin IR compiler doesn't seem too happy with auto-conversion from
        // lambda to Runnable, so separate it here
        val runnable: Runnable = object : Runnable {
            override fun run() {
                handler = Handler()
            }
        }
        activityTestRule.runOnUiThread(runnable)
    }

    internal fun show(composable: @Composable () -> Unit) {
        val runnable: Runnable = object : Runnable {
            override fun run() {
                activity.setContent(Recomposer.current(), composable)
            }
        }
        activityTestRule.runOnUiThread(runnable)
    }

    internal fun findOwnerView(): View {
        return findOwner(activity).view
    }

    internal fun findOwner(activity: Activity): AndroidOwner {
        val contentViewGroup = activity.findViewById<ViewGroup>(android.R.id.content)
        return findOwner(contentViewGroup)!!
    }

    internal fun findOwner(parent: ViewGroup): AndroidOwner? {
        for (index in 0 until parent.childCount) {
            val child = parent.getChildAt(index)
            if (child is AndroidOwner) {
                return child
            } else if (child is ViewGroup) {
                val owner = findOwner(child)
                if (owner != null) {
                    return owner
                }
            }
        }
        return null
    }

    internal fun waitForDraw(view: View) {
        val viewDrawLatch = CountDownLatch(1)
        val listener = object : ViewTreeObserver.OnDrawListener {
            override fun onDraw() {
                viewDrawLatch.countDown()
            }
        }
        view.post(object : Runnable {
            override fun run() {
                view.viewTreeObserver.addOnDrawListener(listener)
                view.invalidate()
            }
        })
        assertTrue(viewDrawLatch.await(1, TimeUnit.SECONDS))
    }

    internal fun Modifier.saveLayoutInfo(
        size: Ref<IntPxSize>,
        position: Ref<PxPosition>,
        positionedLatch: CountDownLatch
    ): Modifier = this.onPositioned { coordinates ->
        size.value = IntPxSize(coordinates.size.width, coordinates.size.height)
        position.value = coordinates.localToGlobal(PxPosition(0f, 0f))
        positionedLatch.countDown()
    }

    internal fun testIntrinsics(
        vararg layouts: @Composable () -> Unit,
        test: ((IntPx) -> IntPx, (IntPx) -> IntPx, (IntPx) -> IntPx, (IntPx) -> IntPx) -> Unit
    ) {
        layouts.forEach { layout ->
            val layoutLatch = CountDownLatch(1)
            show {
                Layout(
                    layout,
                    minIntrinsicWidthMeasureBlock = { _, _, _ -> 0.ipx },
                    minIntrinsicHeightMeasureBlock = { _, _, _ -> 0.ipx },
                    maxIntrinsicWidthMeasureBlock = { _, _, _ -> 0.ipx },
                    maxIntrinsicHeightMeasureBlock = { _, _, _ -> 0.ipx }
                ) { measurables, _, layoutDirection ->
                    val measurable = measurables.first()
                    test(
                        { h -> measurable.minIntrinsicWidth(h, layoutDirection) },
                        { w -> measurable.minIntrinsicHeight(w, layoutDirection) },
                        { h -> measurable.maxIntrinsicWidth(h, layoutDirection) },
                        { w -> measurable.maxIntrinsicHeight(w, layoutDirection) }
                    )
                    layoutLatch.countDown()
                    layout(0.ipx, 0.ipx) {}
                }
            }
            assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))
        }
    }

    @Composable
    internal fun FixedSizeLayout(
        width: IntPx,
        height: IntPx,
        alignmentLines: Map<AlignmentLine, IntPx>
    ) {
        Layout({}) { _, constraints, _ ->
            layout(
                width.coerceIn(constraints.minWidth, constraints.maxWidth),
                height.coerceIn(constraints.minHeight, constraints.maxHeight),
                alignmentLines
            ) {}
        }
    }

    @Composable
    internal fun WithInfiniteConstraints(children: @Composable () -> Unit) {
        Layout(children) { measurables, _, _ ->
            val placeables = measurables.map { it.measure(Constraints()) }
            layout(0.ipx, 0.ipx) {
                placeables.forEach { it.place(0.ipx, 0.ipx) }
            }
        }
    }

    @Composable
    internal fun ConstrainedBox(
        constraints: DpConstraints,
        modifier: Modifier = Modifier,
        children: @Composable () -> Unit
    ) {
        Layout(
            children,
            modifier = modifier,
            minIntrinsicWidthMeasureBlock = { measurables, h, _ ->
                val width = measurables.firstOrNull()?.minIntrinsicWidth(h) ?: 0.ipx
                width.coerceIn(constraints.minWidth.toIntPx(), constraints.maxWidth.toIntPx())
            },
            minIntrinsicHeightMeasureBlock = { measurables, w, _ ->
                val height = measurables.firstOrNull()?.minIntrinsicHeight(w) ?: 0.ipx
                height.coerceIn(constraints.minHeight.toIntPx(), constraints.maxHeight.toIntPx())
            },
            maxIntrinsicWidthMeasureBlock = { measurables, h, _ ->
                val width = measurables.firstOrNull()?.maxIntrinsicWidth(h) ?: 0.ipx
                width.coerceIn(constraints.minWidth.toIntPx(), constraints.maxWidth.toIntPx())
            },
            maxIntrinsicHeightMeasureBlock = { measurables, w, _ ->
                val height = measurables.firstOrNull()?.maxIntrinsicHeight(w) ?: 0.ipx
                height.coerceIn(constraints.minHeight.toIntPx(), constraints.maxHeight.toIntPx())
            }
        ) { measurables, incomingConstraints, _ ->
            val measurable = measurables.firstOrNull()
            val childConstraints = Constraints(constraints).enforce(incomingConstraints)
            val placeable = measurable?.measure(childConstraints)

            val layoutWidth = placeable?.width ?: childConstraints.minWidth
            val layoutHeight = placeable?.height ?: childConstraints.minHeight
            layout(layoutWidth, layoutHeight) {
                placeable?.place(IntPx.Zero, IntPx.Zero)
            }
        }
    }

    internal fun assertEquals(expected: PxSize?, actual: PxSize?) {
        assertNotNull("Null expected size", expected)
        expected as PxSize
        assertNotNull("Null actual size", actual)
        actual as PxSize

        assertEquals(
            "Expected width ${expected.width} but obtained ${actual.width}",
            expected.width,
            actual.width,
            0f
        )
        assertEquals(
            "Expected height ${expected.height} but obtained ${actual.height}",
            expected.height,
            actual.height,
            0f
        )
        if (actual.width != actual.width.toInt().toFloat()) {
            fail("Expected integer width")
        }
        if (actual.height != actual.height.toInt().toFloat()) {
            fail("Expected integer height")
        }
    }

    internal fun assertEquals(expected: PxPosition?, actual: PxPosition?) {
        assertNotNull("Null expected position", expected)
        expected as PxPosition
        assertNotNull("Null actual position", actual)
        actual as PxPosition

        assertEquals(
            "Expected x ${expected.x} but obtained ${actual.x}",
            expected.x,
            actual.x,
            0f
        )
        assertEquals(
            "Expected y ${expected.y} but obtained ${actual.y}",
            expected.y,
            actual.y,
            0f
        )
        if (actual.x != actual.x.toInt().toFloat()) {
            fail("Expected integer x coordinate")
        }
        if (actual.y != actual.y.toInt().toFloat()) {
            fail("Expected integer y coordinate")
        }
    }

    internal fun assertEquals(expected: IntPx, actual: IntPx) {
        assertEquals(
            "Expected $expected but obtained $actual",
            expected.value.toFloat(),
            actual.value.toFloat(),
            0f
        )
    }

    internal val customVerticalArrangement = object : Arrangement.Vertical {
        override fun arrange(
            totalSize: IntPx,
            size: List<IntPx>,
            layoutDirection: LayoutDirection
        ): List<IntPx> {
            val positions = mutableListOf<IntPx>()
            var current = 0.px
            val usedSpace = size.fold(0.ipx) { sum, e -> sum + e }
            val step = if (size.size < 2) {
                0.px
            } else {
                (totalSize - usedSpace).toPx() * 2 / (size.lastIndex * size.size)
            }
            size.forEachIndexed { i, childSize ->
                current += step * i
                positions.add(current.round())
                current += childSize.toPx()
            }
            return positions
        }
    }

    internal val customHorizontalArrangement = object : Arrangement.Horizontal {
        override fun arrange(
            totalSize: IntPx,
            size: List<IntPx>,
            layoutDirection: LayoutDirection
        ): List<IntPx> {
            val positions = mutableListOf<IntPx>()
            var current = 0.px
            if (layoutDirection == LayoutDirection.Rtl) {
                size.forEach {
                    positions.add(current.round())
                    current += it
                }
            } else {
                val usedSpace = size.fold(0.ipx) { sum, e -> sum + e }
                val step = if (size.size < 2) {
                    0.px
                } else {
                    (totalSize - usedSpace).toPx() * 2 / (size.lastIndex * size.size)
                }
                size.forEachIndexed { i, childSize ->
                    current += step * i
                    positions.add(current.round())
                    current += childSize.toPx()
                }
            }
            return positions
        }
    }

    @Composable
    internal fun Container(
        modifier: Modifier = Modifier,
        padding: InnerPadding = InnerPadding(0.dp),
        alignment: Alignment = Alignment.Center,
        expanded: Boolean = false,
        constraints: DpConstraints = DpConstraints(),
        width: Dp? = null,
        height: Dp? = null,
        children: @Composable () -> Unit
    ) {
        Layout(children, modifier) { measurables, incomingConstraints, _ ->
            val containerConstraints = Constraints(constraints)
                .copy(
                    width?.toIntPx() ?: constraints.minWidth.toIntPx(),
                    width?.toIntPx() ?: constraints.maxWidth.toIntPx(),
                    height?.toIntPx() ?: constraints.minHeight.toIntPx(),
                    height?.toIntPx() ?: constraints.maxHeight.toIntPx()
                ).enforce(incomingConstraints)
            val totalHorizontal = padding.start.toIntPx() + padding.end.toIntPx()
            val totalVertical = padding.top.toIntPx() + padding.bottom.toIntPx()
            val childConstraints = containerConstraints
                .copy(minWidth = 0.ipx, minHeight = 0.ipx)
                .offset(-totalHorizontal, -totalVertical)
            var placeable: Placeable? = null
            val containerWidth = if ((containerConstraints.hasFixedWidth || expanded) &&
                containerConstraints.maxWidth.isFinite()
            ) {
                containerConstraints.maxWidth
            } else {
                placeable = measurables.firstOrNull()?.measure(childConstraints)
                max((placeable?.width ?: 0.ipx) + totalHorizontal, containerConstraints.minWidth)
            }
            val containerHeight = if ((containerConstraints.hasFixedHeight || expanded) &&
                containerConstraints.maxHeight.isFinite()
            ) {
                containerConstraints.maxHeight
            } else {
                if (placeable == null) {
                    placeable = measurables.firstOrNull()?.measure(childConstraints)
                }
                max((placeable?.height ?: 0.ipx) + totalVertical, containerConstraints.minHeight)
            }
            layout(containerWidth, containerHeight) {
                val p = placeable ?: measurables.firstOrNull()?.measure(childConstraints)
                p?.let {
                    val position = alignment.align(
                        IntPxSize(
                            containerWidth - it.width - totalHorizontal,
                            containerHeight - it.height - totalVertical
                        )
                    )
                    it.place(
                        padding.start.toIntPx() + position.x,
                        padding.top.toIntPx() + position.y
                    )
                }
            }
        }
    }
}