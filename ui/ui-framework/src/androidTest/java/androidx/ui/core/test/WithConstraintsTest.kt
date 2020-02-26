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

package androidx.ui.core.test

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.Composable
import androidx.compose.emptyContent
import androidx.compose.remember
import androidx.compose.state
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.rule.ActivityTestRule
import androidx.ui.core.Constraints
import androidx.ui.core.Layout
import androidx.ui.core.MeasureBlock
import androidx.ui.core.Modifier
import androidx.ui.core.OnPositioned
import androidx.ui.core.Ref
import androidx.ui.core.WithConstraints
import androidx.ui.core.draw
import androidx.ui.core.setContent
import androidx.ui.framework.test.TestActivity
import androidx.ui.graphics.Color
import androidx.ui.graphics.Paint
import androidx.ui.graphics.vector.DrawVector
import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.ipx
import androidx.ui.unit.px
import androidx.ui.unit.toRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(JUnit4::class)
class WithConstraintsTest {

    @get:Rule
    val rule = ActivityTestRule<TestActivity>(
        TestActivity::class.java
    )
    @get:Rule
    val excessiveAssertions = AndroidOwnerExtraAssertionsRule()

    private lateinit var activity: TestActivity
    private lateinit var drawLatch: CountDownLatch

    @Before
    fun setup() {
        activity = rule.activity
        activity.hasFocusLatch.await(5, TimeUnit.SECONDS)
        drawLatch = CountDownLatch(1)
    }

    @Test
    fun withConstraintsTest() {
        val size = 20.ipx

        val countDownLatch = CountDownLatch(1)
        val topConstraints = Ref<Constraints>()
        val paddedConstraints = Ref<Constraints>()
        val firstChildConstraints = Ref<Constraints>()
        val secondChildConstraints = Ref<Constraints>()
        rule.runOnUiThreadIR {
            activity.setContentInFrameLayout {
                WithConstraints { constraints ->
                    topConstraints.value = constraints
                    Padding(size = size) {
                        val drawModifier = draw { _, _ ->
                            countDownLatch.countDown()
                        }
                        WithConstraints(drawModifier) { constraints ->
                            paddedConstraints.value = constraints
                            Layout(measureBlock = { _, childConstraints ->
                                firstChildConstraints.value = childConstraints
                                layout(size, size) { }
                            }, children = { })
                            Layout(measureBlock = { _, chilConstraints ->
                                secondChildConstraints.value = chilConstraints
                                layout(size, size) { }
                            }, children = { })
                        }
                    }
                }
            }
        }
        assertTrue(countDownLatch.await(1, TimeUnit.SECONDS))

        val expectedPaddedConstraints = Constraints(
            0.ipx,
            topConstraints.value!!.maxWidth - size * 2,
            0.ipx,
            topConstraints.value!!.maxHeight - size * 2
        )
        assertEquals(expectedPaddedConstraints, paddedConstraints.value)
        assertEquals(paddedConstraints.value, firstChildConstraints.value)
        assertEquals(paddedConstraints.value, secondChildConstraints.value)
    }

    @Suppress("UNUSED_ANONYMOUS_PARAMETER")
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun withConstraints_layoutListener() {
        val green = Color.Green
        val white = Color.White
        val model = SquareModel(size = 20.ipx, outerColor = green, innerColor = white)

        rule.runOnUiThreadIR {
            activity.setContentInFrameLayout {
                WithConstraints { constraints ->
                    val outerModifier = draw { canvas, size ->
                        val paint = Paint()
                        paint.color = model.outerColor
                        canvas.drawRect(size.toRect(), paint)
                    }
                    Layout(children = {
                        val innerModifier = draw { canvas, size ->
                            drawLatch.countDown()
                            val paint = Paint()
                            paint.color = model.innerColor
                            canvas.drawRect(size.toRect(), paint)
                        }
                        Layout(
                            children = {},
                            modifier = innerModifier
                        ) { measurables, constraints2 ->
                            layout(model.size, model.size) {}
                        }
                    }, modifier = outerModifier) { measurables, constraints3 ->
                        val placeable = measurables[0].measure(
                            Constraints.fixed(
                                model.size,
                                model.size
                            )
                        )
                        layout(model.size * 3, model.size * 3) {
                            placeable.place(model.size, model.size)
                        }
                    }
                }
            }
        }
        takeScreenShot(60).apply {
            assertRect(color = white, size = 20)
            assertRect(color = green, holeSize = 20)
        }

        drawLatch = CountDownLatch(1)
        rule.runOnUiThreadIR {
            model.size = 10.ipx
        }

        takeScreenShot(30).apply {
            assertRect(color = white, size = 10)
            assertRect(color = green, holeSize = 10)
        }
    }

    /**
     * WithConstraints will cause a requestLayout during layout in some circumstances.
     * The test here is the minimal example from a bug.
     */
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun requestLayoutDuringLayout() {
        val offset = OffsetModel(0.ipx)
        rule.runOnUiThreadIR {
            activity.setContentInFrameLayout {
                Scroller(
                    modifier = countdownLatchBackgroundModifier(Color.Yellow),
                    onScrollPositionChanged = { position, _ ->
                        offset.offset = position
                    },
                    offset = offset
                ) {
                    // Need to pass some param here to a separate function or else it works fine
                    TestLayout(5)
                }
            }
        }

        takeScreenShot(30).apply {
            assertRect(color = Color.Red, size = 10)
            assertRect(color = Color.Yellow, holeSize = 10)
        }
    }

    @Test
    fun subcomposionInsideWithConstraintsDoesntAffectModelReadsObserving() {
        val model = ValueModel(0)
        var latch = CountDownLatch(1)

        rule.runOnUiThreadIR {
            activity.setContent {
                WithConstraints {
                    // this block is called as a subcomposition from LayoutNode.measure()
                    // DrawVector introduces additional subcomposition which is closing the
                    // current frame and opens a new one. our model reads during measure()
                    // wasn't possible to survide Frames swicth previously so the model read
                    // within the child Layout wasn't recorded
                    DrawVector(100.px, 100.px) { _, _ -> }
                    Layout({}) { _, _ ->
                        // read the model
                        model.value
                        latch.countDown()
                        layout(10.ipx, 10.ipx) {}
                    }
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))

        latch = CountDownLatch(1)
        rule.runOnUiThread { model.value++ }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun withConstraintCallbackIsNotExecutedWithInnerRecompositions() {
        val model = ValueModel(0)
        var latch = CountDownLatch(1)
        var recompositionsCount1 = 0
        var recompositionsCount2 = 0

        rule.runOnUiThreadIR {
            activity.setContentInFrameLayout {
                WithConstraints {
                    recompositionsCount1++
                    Container(100.ipx, 100.ipx) {
                        model.value // model read
                        recompositionsCount2++
                        latch.countDown()
                    }
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))

        latch = CountDownLatch(1)
        rule.runOnUiThread { model.value++ }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(1, recompositionsCount1)
        assertEquals(2, recompositionsCount2)
    }

    @Test
    fun updateConstraintsRecomposingWithConstraints() {
        val model = ValueModel(50.ipx)
        var latch = CountDownLatch(1)
        var actualConstraints: Constraints? = null

        rule.runOnUiThreadIR {
            activity.setContentInFrameLayout {
                ChangingConstraintsLayout(model) {
                    WithConstraints { constraints ->
                        actualConstraints = constraints
                        assertEquals(1, latch.count)
                        latch.countDown()
                        Container(width = 100.ipx, height = 100.ipx, children = emptyContent())
                    }
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(Constraints.fixed(50.ipx, 50.ipx), actualConstraints)

        latch = CountDownLatch(1)
        rule.runOnUiThread { model.value = 100.ipx }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(Constraints.fixed(100.ipx, 100.ipx), actualConstraints)
    }

    @Test
    fun withConstsraintsBehavesAsWrap() {
        val size = ValueModel(50.ipx)
        var withConstLatch = CountDownLatch(1)
        var childLatch = CountDownLatch(1)
        var withConstSize: IntPxSize? = null
        var childSize: IntPxSize? = null

        rule.runOnUiThreadIR {
            activity.setContentInFrameLayout {
                Container(width = 200.ipx, height = 200.ipx) {
                    WithConstraints {
                        OnPositioned {
                            // OnPositioned can be fired multiple times with the same value
                            // for example when requestLayout() was triggered on ComposeView.
                            // if we called twice, let's make sure we got the correct values.
                            assertTrue(withConstSize == null || withConstSize == it.size)
                            withConstSize = it.size
                            withConstLatch.countDown()
                        }
                        Container(width = size.value, height = size.value) {
                            OnPositioned {
                                // OnPositioned can be fired multiple times with the same value
                                // for example when requestLayout() was triggered on ComposeView.
                                // if we called twice, let's make sure we got the correct values.
                                assertTrue(childSize == null || childSize == it.size)
                                childSize = it.size
                                childLatch.countDown()
                            }
                        }
                    }
                }
            }
        }
        assertTrue(withConstLatch.await(1, TimeUnit.SECONDS))
        assertTrue(childLatch.await(1, TimeUnit.SECONDS))
        var expectedSize = IntPxSize(50.ipx, 50.ipx)
        assertEquals(expectedSize, withConstSize)
        assertEquals(expectedSize, childSize)

        withConstSize = null
        childSize = null
        withConstLatch = CountDownLatch(1)
        childLatch = CountDownLatch(1)
        rule.runOnUiThread { size.value = 100.ipx }

        assertTrue(withConstLatch.await(1, TimeUnit.SECONDS))
        assertTrue(childLatch.await(1, TimeUnit.SECONDS))
        expectedSize = IntPxSize(100.ipx, 100.ipx)
        assertEquals(expectedSize, withConstSize)
        assertEquals(expectedSize, childSize)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun withConstraintsIsNotSwallowingInnerRemeasureRequest() {
        val model = ValueModel(100.ipx)

        rule.runOnUiThreadIR {
            activity.setContentInFrameLayout {
                Container(100.ipx, 100.ipx, backgroundModifier(Color.Red)) {
                    ChangingConstraintsLayout(model) {
                        WithConstraints { constraints ->
                            Container(100.ipx, 100.ipx) {
                                Container(100.ipx, 100.ipx) {
                                    Layout(
                                        {},
                                        countdownLatchBackgroundModifier(Color.Yellow)
                                    ) { _, _ ->
                                        // the same as the value inside ValueModel
                                        val size = constraints.maxWidth
                                        layout(size, size) {}
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        takeScreenShot(100).apply {
            assertRect(color = Color.Yellow)
        }

        drawLatch = CountDownLatch(1)
        rule.runOnUiThread {
            model.value = 50.ipx
        }

        takeScreenShot(100).apply {
            assertRect(color = Color.Red, holeSize = 50)
            assertRect(color = Color.Yellow, size = 50)
        }
    }

    @Test
    fun updateModelInMeasuringAndReadItInCompositionWorksInsideWithConstraints() {
        val latch = CountDownLatch(1)
        rule.runOnUiThread {
            activity.setContentInFrameLayout {
                Container(width = 100.ipx, height = 100.ipx) {
                    WithConstraints {
                        // this replicates the popular pattern we currently use
                        // where we save some data calculated in the measuring block
                        // and then use it in the next composition frame
                        var model by state { false }
                        Layout({
                            if (model) {
                                latch.countDown()
                            }
                        }) { _, _ ->
                            if (!model) {
                                model = true
                            }
                            layout(100.ipx, 100.ipx) {}
                        }
                    }
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun removeLayoutNodeFromWithConstraintsDuringOnMeasure() {
        val model = ValueModel(100.ipx)
        drawLatch = CountDownLatch(2)

        rule.runOnUiThreadIR {
            activity.setContentInFrameLayout {
                Container(
                    100.ipx, 100.ipx,
                    modifier = countdownLatchBackgroundModifier(Color.Red)
                ) {
                    // this component changes the constraints which triggers subcomposition
                    // within onMeasure block
                    ChangingConstraintsLayout(model) {
                        WithConstraints { constraints ->
                            if (constraints.maxWidth == 100.ipx) {
                                // we will stop emmitting this layouts after constraints change
                                // Additional Container is needed so the Layout will be
                                // marked as not affecting parent size which means the Layout
                                // will be added into relayoutNodes List separately
                                Container(100.ipx, 100.ipx) {
                                    Layout(
                                        children = {},
                                        modifier = countdownLatchBackgroundModifier(Color.Yellow)
                                    ) { _, _ ->
                                        layout(model.value, model.value) {}
                                    }
                                }
                            }
                        }
                        Container(100.ipx, 100.ipx, Modifier.None, emptyContent())
                    }
                }
            }
        }
        takeScreenShot(100).apply {
            assertRect(color = Color.Yellow)
        }

        drawLatch = CountDownLatch(1)
        rule.runOnUiThread {
            model.value = 50.ipx
        }

        takeScreenShot(100).apply {
            assertRect(color = Color.Red)
        }
    }

    @Test
    fun withConstraintsSiblingWhichIsChangingTheModelInsideMeasureBlock() {
        // WithConstraints is calling FrameManager.nextFrame() after composition
        // so this code was causing an issue as the model value change is triggering
        // remeasuring while our parent is measuring right now and this child was
        // already measured
        val drawlatch = CountDownLatch(1)
        rule.runOnUiThread {
            activity.setContent {
                val state = state { false }
                var lastLayoutValue: Boolean = false
                val drawModifier = draw { _, _ ->
                    // this verifies the layout was remeasured before being drawn
                    assertTrue(lastLayoutValue)
                    drawlatch.countDown()
                }
                Layout(children = {}, modifier = drawModifier) { _, _ ->
                    lastLayoutValue = state.value
                    // this registers the value read
                    if (!state.value) {
                        // change the value right inside the measure block
                        // it will cause one more remeasure pass as we also read this value
                        state.value = true
                    }
                    layout(100.ipx, 100.ipx) {}
                }
                WithConstraints {}
            }
        }
        assertTrue(drawlatch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun allTheStepsCalledExactlyOnce() {
        val outerComposeLatch = CountDownLatch(1)
        val outerMeasureLatch = CountDownLatch(1)
        val outerLayoutLatch = CountDownLatch(1)
        val innerComposeLatch = CountDownLatch(1)
        val innerMeasureLatch = CountDownLatch(1)
        val innerLayoutLatch = CountDownLatch(1)
        rule.runOnUiThread {
            activity.setContent {
                assertEquals(1, outerComposeLatch.count)
                outerComposeLatch.countDown()
                Layout(children = {
                    WithConstraints {
                        assertEquals(1, innerComposeLatch.count)
                        innerComposeLatch.countDown()
                        Layout(children = emptyContent()) { _, _ ->
                            assertEquals(1, innerMeasureLatch.count)
                            innerMeasureLatch.countDown()
                            layout(100.ipx, 100.ipx) {
                                assertEquals(1, innerLayoutLatch.count)
                                innerLayoutLatch.countDown()
                            }
                        }
                    }
                }) { measurables, constraints ->
                    assertEquals(1, outerMeasureLatch.count)
                    outerMeasureLatch.countDown()
                    layout(100.ipx, 100.ipx) {
                        assertEquals(1, outerLayoutLatch.count)
                        outerLayoutLatch.countDown()
                        measurables.forEach { it.measure(constraints).place(0.ipx, 0.ipx) }
                    }
                }
            }
        }
        assertTrue(outerComposeLatch.await(1, TimeUnit.SECONDS))
        assertTrue(outerMeasureLatch.await(1, TimeUnit.SECONDS))
        assertTrue(outerLayoutLatch.await(1, TimeUnit.SECONDS))
        assertTrue(innerComposeLatch.await(1, TimeUnit.SECONDS))
        assertTrue(innerMeasureLatch.await(1, TimeUnit.SECONDS))
        assertTrue(innerLayoutLatch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun triggerRootRemeasureWhileRootIsLayouting() {
        rule.runOnUiThread {
            activity.setContent {
                val state = state { 0 }
                ContainerChildrenAffectsParentSize(100.ipx, 100.ipx) {
                    WithConstraints {
                        Layout(
                            children = {},
                            modifier = countdownLatchBackgroundModifier(Color.Transparent)
                        ) { _, _ ->
                            // read and write once inside measureBlock
                            if (state.value == 0) {
                                state.value = 1
                            }
                            layout(100.ipx, 100.ipx) {}
                        }
                    }
                    Container(100.ipx, 100.ipx) {
                        WithConstraints {}
                    }
                }
            }
        }

        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        // before the fix this was failing our internal assertions in AndroidOwner
        // so nothing else to assert, apart from not crashing
    }

    private fun takeScreenShot(size: Int): Bitmap {
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        val bitmap = rule.waitAndScreenShot()
        assertEquals(size, bitmap.width)
        assertEquals(size, bitmap.height)
        return bitmap
    }

    private fun countdownLatchBackgroundModifier(color: Color) = draw {
        canvas, size ->
        val paint = Paint()
        paint.color = color
        canvas.drawRect(size.toRect(), paint)
        drawLatch.countDown()
    }
}

@Composable
private fun TestLayout(@Suppress("UNUSED_PARAMETER") someInput: Int) {
    Layout(children = {
        WithConstraints {
            NeedsOtherMeasurementComposable(10.ipx)
        }
    }) { measurables, constraints ->
        val withConstraintsPlaceable = measurables[0].measure(constraints)

        layout(30.ipx, 30.ipx) {
            withConstraintsPlaceable.place(10.ipx, 10.ipx)
        }
    }
}

@Composable
private fun NeedsOtherMeasurementComposable(foo: IntPx) {
    Layout(
        children = {},
        modifier = backgroundModifier(Color.Red)
    ) { _, _ ->
        layout(foo, foo) { }
    }
}

@Composable
fun Container(
    width: IntPx,
    height: IntPx,
    modifier: Modifier = Modifier.None,
    children: @Composable() () ->
    Unit
) {
    Layout(
        children = children,
        modifier = modifier,
        measureBlock = remember<MeasureBlock>(width, height) {
            { measurables, _ ->
                val constraint = Constraints(maxWidth = width, maxHeight = height)
                layout(width, height) {
                    measurables.forEach {
                        val placeable = it.measure(constraint)
                        placeable.place(
                            (width - placeable.width) / 2,
                            (height - placeable.height) / 2
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun ContainerChildrenAffectsParentSize(
    width: IntPx,
    height: IntPx,
    children: @Composable() () -> Unit
) {
    Layout(children = children, measureBlock = remember<MeasureBlock>(width, height) {
        { measurables, _ ->
            val constraint = Constraints(maxWidth = width, maxHeight = height)
            val placeables = measurables.map { it.measure(constraint) }
            layout(width, height) {
                placeables.forEach {
                    it.place((width - width) / 2, (height - height) / 2)
                }
            }
        }
    })
}

@Composable
private fun ChangingConstraintsLayout(size: ValueModel<IntPx>, children: @Composable() () -> Unit) {
    Layout(children) { measurables, _ ->
        layout(100.ipx, 100.ipx) {
            val constraints = Constraints.fixed(size.value, size.value)
            measurables.first().measure(constraints).place(0.ipx, 0.ipx)
        }
    }
}

fun backgroundModifier(color: Color) = draw { canvas, size ->
    val paint = Paint()
    paint.color = color
    canvas.drawRect(size.toRect(), paint)
}