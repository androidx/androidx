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

@file:Suppress("Deprecation")

package androidx.ui.core.test

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.Composable
import androidx.compose.State
import androidx.compose.emptyContent
import androidx.compose.getValue
import androidx.compose.mutableStateOf
import androidx.compose.onDispose
import androidx.compose.remember
import androidx.compose.setValue
import androidx.compose.state
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.rule.ActivityTestRule
import androidx.ui.core.Constraints
import androidx.ui.core.DensityAmbient
import androidx.ui.core.Layout
import androidx.ui.core.LayoutDirection
import androidx.ui.core.LayoutModifier
import androidx.ui.core.Measurable
import androidx.ui.core.MeasureBlock
import androidx.ui.core.MeasureScope
import androidx.ui.core.Modifier
import androidx.ui.core.Ref
import androidx.ui.core.WithConstraints
import androidx.ui.core.drawBehind
import androidx.ui.core.onPositioned
import androidx.ui.core.paint
import androidx.ui.core.setContent
import androidx.ui.framework.test.TestActivity
import androidx.ui.graphics.Color
import androidx.ui.graphics.vector.VectorPainter
import androidx.ui.layout.Constraints
import androidx.ui.layout.DpConstraints
import androidx.ui.layout.ltr
import androidx.ui.layout.rtl
import androidx.ui.unit.Density
import androidx.ui.unit.Dp
import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
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
            activity.setContent {
                WithConstraints {
                    topConstraints.value = constraints
                    Padding(size = size) {
                        val drawModifier = Modifier.drawBehind {
                            countDownLatch.countDown()
                        }
                        WithConstraints(drawModifier) {
                            paddedConstraints.value = constraints
                            Layout(measureBlock = { _, childConstraints, _ ->
                                firstChildConstraints.value = childConstraints
                                layout(size, size) { }
                            }, children = { })
                            Layout(measureBlock = { _, chilConstraints, _ ->
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
            activity.setContent {
                WithConstraints {
                    val outerModifier = Modifier.drawBehind {
                        drawRect(model.outerColor)
                    }
                    Layout(children = {
                        val innerModifier = Modifier.drawBehind {
                            drawLatch.countDown()
                            drawRect(model.innerColor)
                        }
                        Layout(
                            children = {},
                            modifier = innerModifier
                        ) { measurables, constraints2, _ ->
                            layout(model.size, model.size) {}
                        }
                    }, modifier = outerModifier) { measurables, constraints3, _ ->
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
        val offset = mutableStateOf(0.ipx)
        rule.runOnUiThreadIR {
            activity.setContent {
                Scroller(
                    modifier = countdownLatchBackgroundModifier(Color.Yellow),
                    onScrollPositionChanged = { position, _ ->
                        offset.value = position
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
        val model = mutableStateOf(0)
        var latch = CountDownLatch(1)

        rule.runOnUiThreadIR {
            activity.setContent {
                WithConstraints {
                    // this block is called as a subcomposition from LayoutNode.measure()
                    // VectorPainter introduces additional subcomposition which is closing the
                    // current frame and opens a new one. our model reads during measure()
                    // wasn't possible to survide Frames swicth previously so the model read
                    // within the child Layout wasn't recorded
                    val background = Modifier.paint(
                        VectorPainter(
                            name = "testPainter",
                            defaultWidth = 10.dp,
                            defaultHeight = 10.dp
                        ) { _, _ ->
                            /* intentionally empty */
                        }
                    )
                    Layout(modifier = background, children = {}) { _, _, _ ->
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
        val model = mutableStateOf(0)
        var latch = CountDownLatch(1)
        var recompositionsCount1 = 0
        var recompositionsCount2 = 0

        rule.runOnUiThreadIR {
            activity.setContent {
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
        val model = mutableStateOf(50.ipx)
        var latch = CountDownLatch(1)
        var actualConstraints: Constraints? = null

        rule.runOnUiThreadIR {
            activity.setContent {
                ChangingConstraintsLayout(model) {
                    WithConstraints {
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
    fun updateLayoutDirectionRecomposingWithConstraints() {
        val direction = mutableStateOf(LayoutDirection.Rtl)
        var latch = CountDownLatch(1)
        var actualDirection: LayoutDirection? = null

        rule.runOnUiThreadIR {
            activity.setContent {
                ChangingLayoutDirectionLayout(direction) {
                    WithConstraints {
                        actualDirection = layoutDirection
                        assertEquals(1, latch.count)
                        latch.countDown()
                        Container(width = 100.ipx, height = 100.ipx, children = emptyContent())
                    }
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(LayoutDirection.Rtl, actualDirection)

        latch = CountDownLatch(1)
        rule.runOnUiThread { direction.value = LayoutDirection.Ltr }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(LayoutDirection.Ltr, actualDirection)
    }

    @Test
    fun withConstsraintsBehavesAsWrap() {
        val size = mutableStateOf(50.ipx)
        var withConstLatch = CountDownLatch(1)
        var childLatch = CountDownLatch(1)
        var withConstSize: IntPxSize? = null
        var childSize: IntPxSize? = null

        rule.runOnUiThreadIR {
            activity.setContent {
                Container(width = 200.ipx, height = 200.ipx) {
                    WithConstraints(modifier = Modifier.onPositioned {
                        // OnPositioned can be fired multiple times with the same value
                        // for example when requestLayout() was triggered on ComposeView.
                        // if we called twice, let's make sure we got the correct values.
                        assertTrue(withConstSize == null || withConstSize == it.size)
                        withConstSize = it.size
                        withConstLatch.countDown()
                    }) {
                        Container(width = size.value, height = size.value,
                            modifier = Modifier.onPositioned {
                                // OnPositioned can be fired multiple times with the same value
                                // for example when requestLayout() was triggered on ComposeView.
                                // if we called twice, let's make sure we got the correct values.
                                assertTrue(childSize == null || childSize == it.size)
                                childSize = it.size
                                childLatch.countDown()
                            }) {
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
        val model = mutableStateOf(100.ipx)

        rule.runOnUiThreadIR {
            activity.setContent {
                Container(100.ipx, 100.ipx, backgroundModifier(Color.Red)) {
                    ChangingConstraintsLayout(model) {
                        WithConstraints {
                            val receivedConstraints = constraints
                            Container(100.ipx, 100.ipx, infiniteConstraints) {
                                Container(100.ipx, 100.ipx) {
                                    Layout(
                                        {},
                                        countdownLatchBackgroundModifier(Color.Yellow)
                                    ) { _, _, _ ->
                                        // the same as the value inside ValueModel
                                        val size = receivedConstraints.maxWidth
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
            activity.setContent {
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
                        }) { _, _, _ ->
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
        val model = mutableStateOf(100.ipx)
        drawLatch = CountDownLatch(2)

        rule.runOnUiThreadIR {
            activity.setContent {
                Container(
                    100.ipx, 100.ipx,
                    modifier = countdownLatchBackgroundModifier(Color.Red)
                ) {
                    // this component changes the constraints which triggers subcomposition
                    // within onMeasure block
                    ChangingConstraintsLayout(model) {
                        WithConstraints {
                            if (constraints.maxWidth == 100.ipx) {
                                // we will stop emmitting this layouts after constraints change
                                // Additional Container is needed so the Layout will be
                                // marked as not affecting parent size which means the Layout
                                // will be added into relayoutNodes List separately
                                Container(100.ipx, 100.ipx) {
                                    Layout(
                                        children = {},
                                        modifier = countdownLatchBackgroundModifier(Color.Yellow)
                                    ) { _, _, _ ->
                                        layout(model.value, model.value) {}
                                    }
                                }
                            }
                        }
                        Container(100.ipx, 100.ipx, Modifier, emptyContent())
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
                val drawModifier = Modifier.drawBehind {
                    // this verifies the layout was remeasured before being drawn
                    assertTrue(lastLayoutValue)
                    drawlatch.countDown()
                }
                Layout(children = {}, modifier = drawModifier) { _, _, _ ->
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
                val children = @Composable {
                    Layout(children = {
                        WithConstraints {
                            assertEquals(1, innerComposeLatch.count)
                            innerComposeLatch.countDown()
                            Layout(children = emptyContent()) { _, _, _ ->
                                assertEquals(1, innerMeasureLatch.count)
                                innerMeasureLatch.countDown()
                                layout(100.ipx, 100.ipx) {
                                    assertEquals(1, innerLayoutLatch.count)
                                    innerLayoutLatch.countDown()
                                }
                            }
                        }
                    }) { measurables, constraints, _ ->
                        assertEquals(1, outerMeasureLatch.count)
                        outerMeasureLatch.countDown()
                        layout(100.ipx, 100.ipx) {
                            assertEquals(1, outerLayoutLatch.count)
                            outerLayoutLatch.countDown()
                            measurables.forEach { it.measure(constraints).place(0.ipx, 0.ipx) }
                        }
                    }
                }

                Layout(children) { measurables, _, _ ->
                    layout(100.ipx, 100.ipx) {
                        // we fix the constraints used by children so if the constraints given
                        // by the android view will change it would not affect the test
                        val constraints = Constraints(maxWidth = 100.ipx, maxHeight = 100.ipx)
                        measurables.first().measure(constraints).place(0.ipx, 0.ipx)
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
                        ) { _, _, _ ->
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

    @Test
    fun withConstraints_getsCorrectLayoutDirection() {
        var latch = CountDownLatch(1)
        var resultLayoutDirection: LayoutDirection? = null
        rule.runOnUiThreadIR {
            activity.setContent {
                Layout(
                    children = @Composable {
                        WithConstraints {
                            resultLayoutDirection = layoutDirection
                        }
                    },
                    modifier = layoutDirectionModifier(LayoutDirection.Rtl)
                ) { m, c, _ ->
                    val p = m.first().measure(c)
                    layout(0.ipx, 0.ipx) {
                        p.place(PxPosition.Origin)
                        latch.countDown()
                    }
                }
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(LayoutDirection.Rtl, resultLayoutDirection)
    }

    @Test
    fun withConstraints_layoutDirectionSetByModifier() {
        var latch = CountDownLatch(1)
        var resultLayoutDirection: LayoutDirection? = null
        rule.runOnUiThreadIR {
            activity.setContent {
                Layout(
                    children = @Composable {
                        WithConstraints(
                            modifier = layoutDirectionModifier(LayoutDirection.Rtl)
                        ) {
                            resultLayoutDirection = layoutDirection
                            latch.countDown()
                        }
                    },
                    modifier = layoutDirectionModifier(LayoutDirection.Ltr)
                ) { m, c, _ ->
                    val p = m.first().measure(c)
                    layout(0.ipx, 0.ipx) {
                        p.place(PxPosition.Origin)
                        latch.countDown()
                    }
                }
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(LayoutDirection.Rtl, resultLayoutDirection)
    }

    @Test
    fun withConstraintsChildIsMeasuredEvenWithDefaultConstraints() {
        val compositionLatch = CountDownLatch(1)
        val childMeasureLatch = CountDownLatch(1)
        val zeroConstraints = Constraints.fixed(0.ipx, 0.ipx)
        rule.runOnUiThreadIR {
            activity.setContent {
                Layout(measureBlock = { measurables, _, _ ->
                    layout(0.ipx, 0.ipx) {
                        // there was a bug when the child of WithConstraints wasn't marking
                        // needsRemeasure and it was only measured because the constraints
                        // have been changed. to verify needRemeasure is true we measure the
                        // children with the default zero constraints so it will be equals to the
                        // initial constraints
                        measurables.first().measure(zeroConstraints).place(0.ipx, 0.ipx)
                    }
                }, children = {
                    WithConstraints {
                        compositionLatch.countDown()
                        Layout(children = {}) { _, _, _ ->
                            childMeasureLatch.countDown()
                            layout(0.ipx, 0.ipx) {}
                        }
                    }
                })
            }
        }

        assertTrue(compositionLatch.await(1, TimeUnit.SECONDS))
        assertTrue(childMeasureLatch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun onDisposeInsideWithConstraintsCalled() {
        var emit by mutableStateOf(true)
        val composedLatch = CountDownLatch(1)
        val disposedLatch = CountDownLatch(1)
        rule.runOnUiThreadIR {
            activity.setContent {
                if (emit) {
                    WithConstraints {
                        composedLatch.countDown()
                        onDispose {
                            disposedLatch.countDown()
                        }
                    }
                }
            }
        }

        assertTrue(composedLatch.await(1, TimeUnit.SECONDS))

        rule.runOnUiThread {
            emit = false
        }
        assertTrue(disposedLatch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun dpOverloadsHaveCorrectValues() {
        val dpConstraints = DpConstraints(
            minWidth = 5.dp,
            maxWidth = 7.dp,
            minHeight = 9.dp,
            maxHeight = 12.dp
        )
        val latch = CountDownLatch(1)
        var actualMinWidth: Dp = 0.dp
        var actualMaxWidth: Dp = 0.dp
        var actualMinHeight: Dp = 0.dp
        var actualMaxHeight: Dp = 0.dp
        var density: Density? = null
        rule.runOnUiThreadIR {
            activity.setContent {
                Layout(
                    children = @Composable {
                        WithConstraints {
                            density = DensityAmbient.current
                            actualMinWidth = minWidth
                            actualMaxWidth = maxWidth
                            actualMinHeight = minHeight
                            actualMaxHeight = maxHeight
                            latch.countDown()
                        }
                    }
                ) { m, _, _ ->
                    layout(0.ipx, 0.ipx) {
                        m.first().measure(Constraints(dpConstraints)).place(PxPosition.Origin)
                    }
                }
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        with(density!!) {
            assertEquals(dpConstraints.minWidth.toIntPx(), actualMinWidth.toIntPx())
            assertEquals(dpConstraints.maxWidth.toIntPx(), actualMaxWidth.toIntPx())
            assertEquals(dpConstraints.minHeight.toIntPx(), actualMinHeight.toIntPx())
            assertEquals(dpConstraints.maxHeight.toIntPx(), actualMaxHeight.toIntPx())
        }
    }

    private fun takeScreenShot(size: Int): Bitmap {
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        val bitmap = rule.waitAndScreenShot()
        assertEquals(size, bitmap.width)
        assertEquals(size, bitmap.height)
        return bitmap
    }

    private fun countdownLatchBackgroundModifier(color: Color) =
        Modifier.drawBehind {
            drawRect(color)
            drawLatch.countDown()
        }

    private fun layoutDirectionModifier(ld: LayoutDirection) = when (ld) {
        LayoutDirection.Ltr -> Modifier.ltr
        LayoutDirection.Rtl -> Modifier.rtl
    }
}

@Composable
private fun TestLayout(@Suppress("UNUSED_PARAMETER") someInput: Int) {
    Layout(children = {
        WithConstraints {
            NeedsOtherMeasurementComposable(10.ipx)
        }
    }) { measurables, constraints, _ ->
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
    ) { _, _, _ ->
        layout(foo, foo) { }
    }
}

@Composable
fun Container(
    width: IntPx,
    height: IntPx,
    modifier: Modifier = Modifier,
    children: @Composable () ->
    Unit
) {
    Layout(
        children = children,
        modifier = modifier,
        measureBlock = remember<MeasureBlock>(width, height) {
            { measurables, _, _ ->
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
    children: @Composable () -> Unit
) {
    Layout(children = children, measureBlock = remember<MeasureBlock>(width, height) {
        { measurables, _, _ ->
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
private fun ChangingConstraintsLayout(size: State<IntPx>, children: @Composable () -> Unit) {
    Layout(children) { measurables, _, _ ->
        layout(100.ipx, 100.ipx) {
            val constraints = Constraints.fixed(size.value, size.value)
            measurables.first().measure(constraints).place(0.ipx, 0.ipx)
        }
    }
}

@Composable
private fun ChangingLayoutDirectionLayout(
    direction: State<LayoutDirection>,
    children: @Composable () -> Unit
) {
    Layout(children) { measurables, _, _ ->
        layout(100.ipx, 100.ipx) {
            val constraints = Constraints.fixed(100.ipx, 100.ipx)
            measurables.first().measure(constraints, direction.value).place(0.ipx, 0.ipx)
        }
    }
}

fun backgroundModifier(color: Color) = Modifier.drawBehind {
    drawRect(color)
}

val infiniteConstraints = object : LayoutModifier {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
        layoutDirection: LayoutDirection
    ): MeasureScope.MeasureResult {
        val placeable = measurable.measure(Constraints())
        return layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.place(0.ipx, 0.ipx)
        }
    }
}