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

package androidx.compose.remote.core

import androidx.compose.remote.core.layout.ApplyClick
import androidx.compose.remote.core.layout.CaptureAnimatedState
import androidx.compose.remote.core.layout.CaptureComponentTree
import androidx.compose.remote.core.layout.Color
import androidx.compose.remote.core.layout.MockRemoteContext
import androidx.compose.remote.core.layout.TestOperation
import androidx.compose.remote.core.layout.TestParameters
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.actions.ValueIntegerExpressionChange
import org.junit.Assert
import org.junit.Test

class LayoutStateTransitionTest : BaseLayoutTest() {
    init {
        GENERATE_GOLD_FILES = false
    }

    @Test
    fun testRcStateLayoutToggleDemoLifecycle() {
        var doc: CoreDocument? = null
        var context: MockRemoteContext? = null

        val ops =
            arrayListOf<TestOperation>(
                TestLayout {
                    val stateVar = writer.addInteger(0)
                    val toggleExpr =
                        integerExpression(
                            stateVar,
                            1L,
                            Rc.IntegerExpression.L_ADD,
                            2L,
                            Rc.IntegerExpression.L_MOD,
                        )
                    val toggleAction = ValueIntegerExpressionChange(stateVar, toggleExpr)

                    column(Modifier.fillMaxSize()) {
                        stateLayout(Modifier.fillMaxWidth().height(100f), stateVar) {
                            row(
                                Modifier.fillMaxWidth().height(100f),
                                horizontal = RowLayout.START,
                            ) {
                                box(Modifier.size(50f).animationSpec(100).background(Color.RED))
                            }
                            row(Modifier.fillMaxWidth().height(100f), horizontal = RowLayout.END) {
                                box(Modifier.size(50f).animationSpec(100).background(Color.RED))
                            }
                        }
                        box(
                            Modifier.size(100f).onClick(toggleAction),
                            BoxLayout.CENTER,
                            BoxLayout.CENTER,
                        )
                    }
                },
                object : TestOperation() {
                    override fun apply(
                        c: RemoteContext,
                        d: CoreDocument,
                        tp: TestParameters,
                        cm: MutableList<Map<String, Any>>?,
                    ): Boolean {
                        context = c as MockRemoteContext
                        doc = d
                        return false
                    }
                },
            )

        checkLayout(400, 300, 7, RcProfiles.PROFILE_ANDROIDX, "ToggleDemoLifecycle", ops)

        val d = doc!!
        val c = context!!
        c.setAnimationEnabled(true)

        val box0 = d.getComponent(-9)!!

        // Click toggle button at (50, 150)
        d.onClick(c, 50f, 150f)

        // Frame 1 of draw (at t = 1000ms)
        c.currentTime = 1000L
        d.paint(c, 0)
        println("Frame 1: box0.x=${box0.x}, needsMeasure=${d.needsMeasure()}")

        // Frame 2 of draw (at t = 1016ms)
        c.currentTime = 1016L
        if (d.needsMeasure()) {
            d.measure(c, 0f, 400f, 0f, 300f)
        }
        d.paint(c, 0)
        println("Frame 2: box0.x=${box0.x}, needsMeasure=${d.needsMeasure()}")
    }

    @Test
    fun testNestedStateLayoutTransition() {
        val ops =
            arrayListOf<TestOperation>(
                TestLayout {
                    val stateVar = writer.addInteger(0)
                    val toggleExpr =
                        integerExpression(
                            stateVar,
                            1L,
                            Rc.IntegerExpression.L_ADD,
                            2L,
                            Rc.IntegerExpression.L_MOD,
                        )
                    val toggleAction = ValueIntegerExpressionChange(stateVar, toggleExpr)

                    column(Modifier.fillMaxSize()) {
                        stateLayout(Modifier.fillMaxWidth().height(100f), stateVar) {
                            // State 0: Row on left
                            row(
                                Modifier.fillMaxWidth().height(100f),
                                horizontal = RowLayout.START,
                            ) {
                                box(Modifier.size(50f).animationSpec(100).background(Color.RED))
                            }

                            // State 1: Row at end
                            row(Modifier.fillMaxWidth().height(100f), horizontal = RowLayout.END) {
                                box(Modifier.size(50f).animationSpec(100).background(Color.RED))
                            }
                        }

                        box(
                            Modifier.size(100f).onClick(toggleAction),
                            BoxLayout.CENTER,
                            BoxLayout.CENTER,
                        )
                    }
                },
                CaptureComponentTree(),
                ApplyClick(50f, 150f),
                CaptureAnimatedState(0),
                CaptureAnimatedState(150),
                CaptureAnimatedState(300),
            )
        checkLayout(400, 300, 7, RcProfiles.PROFILE_ANDROIDX, "StateLayoutTransition", ops)
    }

    @Test
    fun testDirectStateTransitionInterpolation() {
        var stateVarId = 0
        var doc: CoreDocument? = null
        var context: MockRemoteContext? = null

        val ops =
            arrayListOf<TestOperation>(
                TestLayout {
                    val sv = writer.addInteger(0)
                    stateVarId = (sv - 0x100000000L).toInt()
                    column(Modifier.fillMaxSize()) {
                        stateLayout(Modifier.fillMaxWidth().height(100f), sv) {
                            row(
                                Modifier.fillMaxWidth().height(100f),
                                horizontal = RowLayout.START,
                            ) {
                                box(Modifier.size(50f).animationSpec(100).background(Color.RED))
                            }
                            row(Modifier.fillMaxWidth().height(100f), horizontal = RowLayout.END) {
                                box(Modifier.size(50f).animationSpec(100).background(Color.RED))
                            }
                        }
                    }
                },
                object : TestOperation() {
                    override fun apply(
                        c: RemoteContext,
                        d: CoreDocument,
                        tp: TestParameters,
                        cm: MutableList<Map<String, Any>>?,
                    ): Boolean {
                        context = c as MockRemoteContext
                        doc = d
                        return false
                    }
                },
            )

        checkLayout(400, 300, 7, RcProfiles.PROFILE_ANDROIDX, "DirectStateTransition", ops)

        val d = doc!!
        val c = context!!
        c.setAnimationEnabled(true)

        val box0 = d.getComponent(-9)!! // Box in state 0
        println("Initial Box X: ${box0.x}")

        c.overrideInteger(stateVarId, 1)
        println("c.getInteger($stateVarId) = ${c.getInteger(stateVarId)}")
        val startTime = c.currentTime + 10L
        c.currentTime = startTime

        val stateLayout =
            d.getComponent(-5)
                as androidx.compose.remote.core.operations.layout.managers.StateLayout
        val box1 = d.getComponent(-12)!!
        println("stateVarId: $stateVarId")
        println("StateLayout measuredLayoutIndex: ${stateLayout.measuredLayoutIndex}")
        println("box0 animateMeasure: ${box0.mAnimateMeasure}")
        println("box1 animateMeasure: ${box1.mAnimateMeasure}")

        // Trigger paint (detects state change, invalidates measure)
        d.paint(c, 0)
        println("After paint, stateLayout currentLayoutIndex: ${stateLayout.measuredLayoutIndex}")
        // Trigger measure & layout
        d.measure(c, 0f, 400f, 0f, 300f)
        println("After measure, box0 animateMeasure: ${box0.mAnimateMeasure}")
        println("After measure, box1 animateMeasure: ${box1.mAnimateMeasure}")

        // At t = 0ms of animation
        d.paint(c, 0)
        val x0 = box0.x
        println("Box X at t=0ms: $x0")

        // At t = 150ms
        c.currentTime = startTime + 150L
        d.paint(c, 0)
        val x150 = box0.x
        println("Box X at t=150ms: $x150, box0 animateMeasure: ${box0.mAnimateMeasure}")

        // At t = 300ms
        c.currentTime = startTime + 300L
        d.paint(c, 0)
        val x300 = box0.x
        println("Box X at t=300ms: $x300")

        // At t = 600ms
        c.currentTime = startTime + 600L
        d.paint(c, 0)
        val x600 = box0.x
        println("Box X at t=600ms: $x600")
        println("box0 visibility at t=600ms: ${box0.mVisibility}, isGone: ${box0.isGone()}")

        Assert.assertTrue("x0 should be < 1f but was $x0", x0 < 1f)
        Assert.assertTrue("x150 should be between 200f and 345f but was $x150", x150 in 200f..345f)
        Assert.assertEquals(350f, x300, 1f)
    }

    @Test
    fun testFirstTransitionJumpDueToMFirstLayout() {
        var stateVarId = -1
        var doc: CoreDocument? = null
        var context: MockRemoteContext? = null

        val ops =
            arrayListOf<TestOperation>(
                TestLayout {
                    val sv = writer.addInteger(0)
                    stateVarId = (sv - 0x100000000L).toInt()
                    val toggleExpr =
                        integerExpression(
                            sv,
                            1L,
                            Rc.IntegerExpression.L_ADD,
                            2L,
                            Rc.IntegerExpression.L_MOD,
                        )
                    val toggleAction = ValueIntegerExpressionChange(sv, toggleExpr)

                    column(Modifier.fillMaxSize()) {
                        stateLayout(Modifier.fillMaxWidth().height(120f), sv) {
                            row(
                                Modifier.fillMaxWidth().height(100f),
                                horizontal = RowLayout.START,
                            ) {
                                box(Modifier.size(50f).animationSpec(100).background(Color.RED))
                            }
                            row(Modifier.fillMaxWidth().height(100f), horizontal = RowLayout.END) {
                                box(Modifier.size(50f).animationSpec(100).background(Color.GREEN))
                            }
                        }
                        box(
                            Modifier.size(100f).onClick(toggleAction),
                            BoxLayout.CENTER,
                            BoxLayout.CENTER,
                        )
                    }
                },
                object : TestOperation() {
                    override fun apply(
                        c: RemoteContext,
                        d: CoreDocument,
                        tp: TestParameters,
                        cm: MutableList<Map<String, Any>>?,
                    ): Boolean {
                        context = c as MockRemoteContext
                        doc = d
                        return false
                    }
                },
            )

        checkLayout(
            400,
            300,
            7,
            RcProfiles.PROFILE_ANDROIDX,
            "testFirstTransitionJumpDueToMFirstLayout",
            ops,
        )

        val d = doc!!
        val c = context!!
        c.setAnimationEnabled(true)
        c.currentTime = 1000L

        // Initial render in State 0
        d.paint(c, 0)
        d.measure(c, 0f, 400f, 0f, 300f)
        d.paint(c, 0)

        // Find state 0 box (c1, red) and state 1 box (c2, green)
        // In doc, let's inspect mFirstLayout for state 1 box (c2) before click
        println("Document hierarchy:\n${d.displayHierarchy()}")
        var stateLayout: androidx.compose.remote.core.operations.layout.managers.StateLayout? = null
        for (i in -1 downTo -50) {
            val comp = d.getComponent(i)
            if (comp is androidx.compose.remote.core.operations.layout.managers.StateLayout) {
                stateLayout = comp
                break
            }
        }
        val sl = stateLayout!!
        val state1Row =
            sl.getLayout(1) as androidx.compose.remote.core.operations.layout.LayoutComponent
        val c2 = state1Row.getChildrenComponents()[0]

        Assert.assertTrue("c2.mFirstLayout should be true before Click 1", c2.mFirstLayout)

        // CLICK 1: State 0 -> State 1
        d.onClick(c, 50f, 150f)
        c.currentTime = 1016L
        d.measure(c, 0f, 400f, 0f, 300f)
        d.paint(c, 0)

        Assert.assertTrue(
            "c2.x should be near 0 at start of animation, but was ${c2.x}",
            c2.x < 50f,
        )

        println(
            "Click 1 (State 0 -> State 1) after measure&paint: c2.mAnimateMeasure = ${c2.mAnimateMeasure}, c2.x = ${c2.x}"
        )

        // CLICK 2: State 1 -> State 0
        c.currentTime = 3000L
        d.measure(c, 0f, 400f, 0f, 300f)
        d.paint(c, 0)

        val c1 =
            (sl.getLayout(0) as androidx.compose.remote.core.operations.layout.LayoutComponent)
                .getChildrenComponents()[0]
        println("Before Click 2: c1.mFirstLayout = ${c1.mFirstLayout}, c1.x = ${c1.x}")

        d.onClick(c, 50f, 150f)
        c.currentTime = 3016L
        d.measure(c, 0f, 400f, 0f, 300f)
        d.paint(c, 0)

        println(
            "Click 2 (State 1 -> State 0) after measure&paint: c1.mAnimateMeasure = ${c1.mAnimateMeasure}, c1.x = ${c1.x}"
        )

        // Advance time for Click 2 to finish
        c.currentTime = 5000L
        d.measure(c, 0f, 400f, 0f, 300f)
        d.paint(c, 0)

        // CLICK 3: State 0 -> State 1
        d.onClick(c, 50f, 150f)
        c.currentTime = 5016L
        d.measure(c, 0f, 400f, 0f, 300f)
        d.paint(c, 0)

        println(
            "Click 3 (State 0 -> State 1) after measure&paint: c2.mAnimateMeasure = ${c2.mAnimateMeasure}, c2.x = ${c2.x}"
        )
    }

    @Test
    fun testPostAnimationComponentVisibility() {
        var stateVarId = -1
        var doc: CoreDocument? = null
        var context: MockRemoteContext? = null

        val ops =
            arrayListOf<TestOperation>(
                TestLayout {
                    val sv = writer.addInteger(0)
                    stateVarId = (sv - 0x100000000L).toInt()
                    column(Modifier.fillMaxSize()) {
                        stateLayout(Modifier.fillMaxWidth().height(100f), sv) {
                            row(
                                Modifier.fillMaxWidth().height(100f),
                                horizontal = RowLayout.START,
                            ) {
                                box(Modifier.size(50f).animationSpec(100).background(Color.RED))
                            }
                            row(Modifier.fillMaxWidth().height(100f), horizontal = RowLayout.END) {
                                box(Modifier.size(50f).animationSpec(100).background(Color.RED))
                            }
                        }
                    }
                },
                object : TestOperation() {
                    override fun apply(
                        c: RemoteContext,
                        d: CoreDocument,
                        tp: TestParameters,
                        cm: MutableList<Map<String, Any>>?,
                    ): Boolean {
                        context = c as MockRemoteContext
                        doc = d
                        return false
                    }
                },
            )

        checkLayout(400, 300, 7, RcProfiles.PROFILE_ANDROIDX, "PostAnimationVisibility", ops)

        val d = doc!!
        val c = context!!
        c.setAnimationEnabled(true)
        val box0 = d.getComponent(-9)!!

        // Trigger transition 0 -> 1
        c.overrideInteger(stateVarId, 1)
        val startTime = c.currentTime + 10L
        c.currentTime = startTime
        d.paint(c, 0)
        d.measure(c, 0f, 400f, 0f, 300f)

        // At t = 600ms (animation completed 300ms ago)
        c.currentTime = startTime + 600L
        d.paint(c, 0)

        Assert.assertEquals(350f, box0.x, 1f)
        Assert.assertFalse("box0 should NOT be gone after animation completes", box0.isGone)
    }

    @Test
    fun testRowToColumnMultiElementTransition() {
        var stateVarId = -1
        var doc: CoreDocument? = null
        var context: MockRemoteContext? = null

        val ops =
            arrayListOf<TestOperation>(
                TestLayout {
                    val sv = writer.addInteger(0)
                    stateVarId = (sv - 0x100000000L).toInt()
                    column(Modifier.fillMaxSize()) {
                        stateLayout(Modifier.fillMaxSize(), sv) {
                            row(Modifier.fillMaxWidth().height(100f)) {
                                box(Modifier.size(50f).animationSpec(100).background(Color.RED))
                                box(Modifier.size(50f).animationSpec(101).background(Color.GREEN))
                                box(Modifier.size(50f).animationSpec(102).background(Color.BLUE))
                            }
                            column(Modifier.width(100f).fillMaxHeight()) {
                                box(Modifier.size(50f).animationSpec(100).background(Color.RED))
                                box(Modifier.size(50f).animationSpec(101).background(Color.GREEN))
                                box(Modifier.size(50f).animationSpec(102).background(Color.BLUE))
                            }
                        }
                    }
                },
                object : TestOperation() {
                    override fun apply(
                        c: RemoteContext,
                        d: CoreDocument,
                        tp: TestParameters,
                        cm: MutableList<Map<String, Any>>?,
                    ): Boolean {
                        context = c as MockRemoteContext
                        doc = d
                        return false
                    }
                },
            )

        checkLayout(400, 300, 7, RcProfiles.PROFILE_ANDROIDX, "RowToColumnTransition", ops)

        val d = doc!!
        val c = context!!
        c.setAnimationEnabled(true)

        val b0 = d.getComponent(-9)!!
        val b1 = d.getComponent(-10)!!
        val b2 = d.getComponent(-11)!!

        Assert.assertEquals(0f, b0.x, 1f)
        Assert.assertEquals(0f, b0.y, 1f)

        c.overrideInteger(stateVarId, 1)
        val startTime = c.currentTime + 10L
        c.currentTime = startTime
        d.paint(c, 0)
        d.measure(c, 0f, 400f, 0f, 300f)

        c.currentTime = startTime + 150L
        d.paint(c, 0)
        val gAnimX = b1.mAnimateMeasure?.getX() ?: b1.x
        val gAnimY = b1.mAnimateMeasure?.getY() ?: b1.y
        Assert.assertTrue("Green box X should interpolate leftward", gAnimX in 0f..50f)

        c.currentTime = startTime + 600L
        d.paint(c, 0)
        Assert.assertEquals(0f, b1.x, 1f)
        Assert.assertEquals(50f, b1.y, 1f)

        Assert.assertFalse("Red box should not be gone", b0.isGone)
        Assert.assertFalse("Green box should not be gone", b1.isGone)
        Assert.assertFalse("Blue box should not be gone", b2.isGone)
    }

    @Test
    fun testInterruptedMidFlightTransition() {
        var stateVarId = -1
        var doc: CoreDocument? = null
        var context: MockRemoteContext? = null

        val ops =
            arrayListOf<TestOperation>(
                TestLayout {
                    val sv = writer.addInteger(0)
                    stateVarId = (sv - 0x100000000L).toInt()
                    column(Modifier.fillMaxSize()) {
                        stateLayout(Modifier.fillMaxWidth().height(100f), sv) {
                            row(
                                Modifier.fillMaxWidth().height(100f),
                                horizontal = RowLayout.START,
                            ) {
                                box(Modifier.size(50f).animationSpec(100).background(Color.RED))
                            }
                            row(Modifier.fillMaxWidth().height(100f), horizontal = RowLayout.END) {
                                box(Modifier.size(50f).animationSpec(100).background(Color.RED))
                            }
                        }
                    }
                },
                object : TestOperation() {
                    override fun apply(
                        c: RemoteContext,
                        d: CoreDocument,
                        tp: TestParameters,
                        cm: MutableList<Map<String, Any>>?,
                    ): Boolean {
                        context = c as MockRemoteContext
                        doc = d
                        return false
                    }
                },
            )

        checkLayout(400, 300, 7, RcProfiles.PROFILE_ANDROIDX, "InterruptedTransition", ops)

        val d = doc!!
        val c = context!!
        c.setAnimationEnabled(true)
        val box0 = d.getComponent(-9)!!

        c.overrideInteger(stateVarId, 1)
        var startTime = c.currentTime + 10L
        c.currentTime = startTime
        d.paint(c, 0)
        d.measure(c, 0f, 400f, 0f, 300f)

        c.currentTime = startTime + 150L
        d.paint(c, 0)
        val midX = box0.mAnimateMeasure?.getX() ?: box0.x
        Assert.assertTrue("midX should be > 200f", midX > 200f)

        c.overrideInteger(stateVarId, 0)
        startTime = c.currentTime + 10L
        c.currentTime = startTime
        d.paint(c, 0)
        d.measure(c, 0f, 400f, 0f, 300f)

        c.currentTime = startTime + 50L
        d.paint(c, 0)
        val am = box0.mAnimateMeasure
        val revX = am?.getX() ?: box0.x
        Assert.assertTrue("Reverse mid-flight X should be <= 350f", revX in 0f..350f)

        c.currentTime = startTime + 600L
        d.paint(c, 0)
        Assert.assertEquals(0f, box0.x, 1f)
        Assert.assertFalse("box0 should not be gone", box0.isGone)
    }

    @Test
    fun testMultipleStateTransitions() {
        var stateVarId = -1
        var doc: CoreDocument? = null
        var context: MockRemoteContext? = null

        val ops =
            arrayListOf<TestOperation>(
                TestLayout {
                    val sv = writer.addInteger(0)
                    stateVarId = (sv - 0x100000000L).toInt()
                    column(Modifier.fillMaxSize()) {
                        stateLayout(Modifier.fillMaxWidth().height(100f), sv) {
                            row(
                                Modifier.fillMaxWidth().height(100f),
                                horizontal = RowLayout.START,
                            ) {
                                box(Modifier.size(50f).animationSpec(100).background(Color.RED))
                            }
                            row(Modifier.fillMaxWidth().height(100f), horizontal = RowLayout.END) {
                                box(Modifier.size(50f).animationSpec(100).background(Color.RED))
                            }
                        }
                    }
                },
                object : TestOperation() {
                    override fun apply(
                        c: RemoteContext,
                        d: CoreDocument,
                        tp: TestParameters,
                        cm: MutableList<Map<String, Any>>?,
                    ): Boolean {
                        context = c as MockRemoteContext
                        doc = d
                        return false
                    }
                },
            )

        checkLayout(400, 300, 7, RcProfiles.PROFILE_ANDROIDX, "MultipleStateTransitions", ops)

        val d = doc!!
        val c = context!!
        c.setAnimationEnabled(true)

        val box0 = d.getComponent(-9)!!

        // Transition 1: 0 -> 1
        c.overrideInteger(stateVarId, 1)
        var startTime = c.currentTime + 10L
        c.currentTime = startTime
        d.paint(c, 0)
        d.measure(c, 0f, 400f, 0f, 300f)

        c.currentTime = startTime + 150L
        d.paint(c, 0)
        println("T1 (0->1) at 150ms: ${box0.x}")

        c.currentTime = startTime + 600L
        d.paint(c, 0)
        println("T1 (0->1) at 600ms: ${box0.x}")
        Assert.assertEquals(350f, box0.x, 1f)

        // Transition 2: 1 -> 0
        c.overrideInteger(stateVarId, 0)
        startTime = c.currentTime + 10L
        c.currentTime = startTime
        d.paint(c, 0)
        d.measure(c, 0f, 400f, 0f, 300f)

        c.currentTime = startTime + 150L
        d.paint(c, 0)
        println("T2 (1->0) at 150ms: ${box0.x}")

        c.currentTime = startTime + 600L
        d.paint(c, 0)
        println("T2 (1->0) at 600ms: ${box0.x}")
        Assert.assertEquals(0f, box0.x, 1f)

        // Transition 3: 0 -> 1 again
        c.overrideInteger(stateVarId, 1)
        startTime = c.currentTime + 10L
        c.currentTime = startTime
        d.paint(c, 0)
        d.measure(c, 0f, 400f, 0f, 300f)

        c.currentTime = startTime + 150L
        d.paint(c, 0)
        println("T3 (0->1) at 150ms: ${box0.x}")

        c.currentTime = startTime + 600L
        d.paint(c, 0)
        println("T3 (0->1) at 600ms: ${box0.x}")
        Assert.assertEquals(350f, box0.x, 1f)
    }

    @Test
    fun testRowToColumnWithWeightTransition() {
        var stateVarId = -1
        var doc: CoreDocument? = null
        var context: MockRemoteContext? = null

        val ops =
            arrayListOf<TestOperation>(
                TestLayout {
                    val sv = writer.addInteger(0)
                    stateVarId = (sv - 0x100000000L).toInt()
                    column(Modifier.fillMaxSize()) {
                        stateLayout(Modifier.fillMaxWidth().height(260f), sv) {
                            // State 0: Row with horizontalWeight(1f) on middle box
                            row(
                                Modifier.fillMaxWidth().height(260f),
                                horizontal = RowLayout.SPACE_EVENLY,
                                vertical = RowLayout.CENTER,
                            ) {
                                box(Modifier.size(60f).animationSpec(100).background(Color.RED))
                                box(
                                    Modifier.size(60f)
                                        .horizontalWeight(1f)
                                        .animationSpec(101)
                                        .background(Color.GREEN)
                                )
                                box(Modifier.size(60f).animationSpec(102).background(Color.BLUE))
                            }

                            // State 1: Column with size(120f) on middle box
                            column(
                                Modifier.fillMaxWidth().height(260f),
                                horizontal = ColumnLayout.CENTER,
                                vertical = ColumnLayout.SPACE_EVENLY,
                            ) {
                                box(Modifier.size(60f).animationSpec(100).background(Color.RED))
                                box(Modifier.size(120f).animationSpec(101).background(Color.GREEN))
                                box(Modifier.size(60f).animationSpec(102).background(Color.BLUE))
                            }
                        }
                    }
                },
                object : TestOperation() {
                    override fun apply(
                        c: RemoteContext,
                        d: CoreDocument,
                        tp: TestParameters,
                        cm: MutableList<Map<String, Any>>?,
                    ): Boolean {
                        context = c as MockRemoteContext
                        doc = d
                        return false
                    }
                },
            )

        checkLayout(
            400,
            300,
            7,
            RcProfiles.PROFILE_ANDROIDX,
            "RowToColumnWithWeightTransition",
            ops,
        )

        val d = doc!!
        val c = context!!
        c.setAnimationEnabled(true)

        val b0 = d.getComponent(-9)!!
        val b1 = d.getComponent(-10)!!
        val b2 = d.getComponent(-11)!!

        // In State 0 (Row with weight on b1):
        // Total width = 400. b0=60, b2=60. SPACE_EVENLY gives 4 gaps of 0.
        // b1 (weight 1f) gets remaining width = 280.
        println(
            "State 0: b0(x=${b0.x}, w=${b0.width}), b1(x=${b1.x}, w=${b1.width}), b2(x=${b2.x}, w=${b2.width})"
        )

        // Switch to State 1 (Column)
        c.overrideInteger(stateVarId, 1)
        var startTime = c.currentTime + 10L
        c.currentTime = startTime
        d.paint(c, 0)
        d.measure(c, 0f, 400f, 0f, 300f)

        c.currentTime = startTime + 600L
        d.paint(c, 0)

        println("State 1 at 600ms: b0(x=${b0.x}, y=${b0.y}, w=${b0.width}, h=${b0.height})")
        println("State 1 at 600ms: b1(x=${b1.x}, y=${b1.y}, w=${b1.width}, h=${b1.height})")
        println("State 1 at 600ms: b2(x=${b2.x}, y=${b2.y}, w=${b2.width}, h=${b2.height})")

        // In State 1 (Column):
        // Total height = 260. b0=60, b1=120, b2=60. Total children height = 240.
        // SPACE_EVENLY gap = (260 - 240) / 4 = 5.
        // b0: y = 5, x = (400-60)/2 = 170
        // b1: y = 5 + 60 + 5 = 70, x = (400-120)/2 = 140, w = 120, h = 120
        // b2: y = 70 + 120 + 5 = 195, x = (400-60)/2 = 170

        Assert.assertEquals("b1 width in State 1 Column should be 120", 120f, b1.width, 1f)
        Assert.assertEquals("b1 height in State 1 Column should be 120", 120f, b1.height, 1f)
        Assert.assertEquals("b1 X in State 1 Column should be 140", 140f, b1.x, 1f)
        Assert.assertEquals("b1 Y in State 1 Column should be 70", 70f, b1.y, 1f)

        Assert.assertEquals("b2 Y in State 1 Column should be 195", 195f, b2.y, 1f)
    }

    @Test
    fun testRcStateLayoutToggleDemoFirstTransition() {
        var stateVarId = -1
        var doc: CoreDocument? = null
        var context: MockRemoteContext? = null

        val ops =
            arrayListOf<TestOperation>(
                TestLayout {
                    val sv = writer.addInteger(0)
                    stateVarId = (sv - 0x100000000L).toInt()
                    column(Modifier.fillMaxSize()) {
                        stateLayout(Modifier.fillMaxWidth().height(120f), sv) {
                            row(
                                Modifier.fillMaxWidth().height(100f),
                                horizontal = RowLayout.START,
                            ) {
                                box(Modifier.size(50f).animationSpec(100).background(Color.RED))
                            }
                            row(Modifier.fillMaxWidth().height(100f), horizontal = RowLayout.END) {
                                box(Modifier.size(50f).animationSpec(100).background(Color.RED))
                            }
                        }
                    }
                },
                object : TestOperation() {
                    override fun apply(
                        c: RemoteContext,
                        d: CoreDocument,
                        tp: TestParameters,
                        cm: MutableList<Map<String, Any>>?,
                    ): Boolean {
                        context = c as MockRemoteContext
                        doc = d
                        return false
                    }
                },
            )

        checkLayout(
            400,
            300,
            7,
            RcProfiles.PROFILE_ANDROIDX,
            "RcStateLayoutToggleDemoFirstTransition",
            ops,
        )

        val d = doc!!
        val c = context!!
        c.setAnimationEnabled(true)

        val b0 = d.getComponent(-9)!!

        // Initially in State 0:
        Assert.assertEquals(0f, b0.x, 1f)

        // First transition: 0 -> 1
        c.overrideInteger(stateVarId, 1)
        val startTime = c.currentTime + 10L
        c.currentTime = startTime
        d.paint(c, 0)
        d.measure(c, 0f, 400f, 0f, 300f)

        c.currentTime = startTime + 150L
        d.paint(c, 0)
        val midX = b0.mAnimateMeasure?.getX() ?: b0.x
        println("First transition (0->1) midX at 150ms: $midX, b0.x=${b0.x}")

        Assert.assertTrue(
            "First transition (0->1) at 150ms should be animating mid-flight",
            midX in 50f..345f,
        )
    }

    @Test
    fun testFrameByFrameFirstTransition() {
        var stateVarId = -1
        var doc: CoreDocument? = null
        var context: MockRemoteContext? = null

        val ops =
            arrayListOf<TestOperation>(
                TestLayout {
                    val sv = writer.addInteger(0)
                    stateVarId = (sv - 0x100000000L).toInt()
                    column(Modifier.fillMaxSize()) {
                        stateLayout(Modifier.fillMaxWidth().height(120f), sv) {
                            row(
                                Modifier.fillMaxWidth().height(100f),
                                horizontal = RowLayout.START,
                            ) {
                                box(Modifier.size(50f).animationSpec(100).background(Color.RED))
                            }
                            row(Modifier.fillMaxWidth().height(100f), horizontal = RowLayout.END) {
                                box(Modifier.size(50f).animationSpec(100).background(Color.RED))
                            }
                        }
                    }
                },
                object : TestOperation() {
                    override fun apply(
                        c: RemoteContext,
                        d: CoreDocument,
                        tp: TestParameters,
                        cm: MutableList<Map<String, Any>>?,
                    ): Boolean {
                        context = c as MockRemoteContext
                        doc = d
                        return false
                    }
                },
            )

        checkLayout(
            400,
            300,
            7,
            RcProfiles.PROFILE_ANDROIDX,
            "testFrameByFrameFirstTransition",
            ops,
        )

        val d = doc!!
        val c = context!!
        c.setAnimationEnabled(true)

        val b0 = d.getComponent(-9)!!
        val startT = 1000L
        c.currentTime = startT

        // Frame 0 (Initial state 0):
        d.paint(c, 0)
        println("Frame 0 (t=1000ms): b0.x=${b0.x}, repaint=${d.needsRepaint()}")
        Assert.assertEquals(0f, b0.x, 1f)

        // Click to toggle state: 0 -> 1
        c.overrideInteger(stateVarId, 1)

        // Frame 1 (t=1016ms, stateVar is 1, paint detects change, calls invalidateMeasure):
        c.currentTime = startT + 16L
        d.paint(c, 0)
        println(
            "Frame 1 (t=1016ms): b0.x=${b0.x}, needsMeasure=${d.needsMeasure()}, repaint=${d.needsRepaint()}"
        )

        // Frame 2 (t=1032ms, needsMeasure is true so d.layout(c) runs, then paint):
        c.currentTime = startT + 32L
        if (d.needsMeasure()) {
            d.measure(c, 0f, 400f, 0f, 300f)
        }
        d.paint(c, 0)
        val f2x = b0.mAnimateMeasure?.getX() ?: b0.x
        println(
            "Frame 2 (t=1032ms): animX=$f2x, b0.x=${b0.x}, animDone=${b0.mAnimateMeasure?.isDone}, repaint=${d.needsRepaint()}"
        )

        // Frame 3 (t=1048ms):
        c.currentTime = startT + 48L
        if (d.needsMeasure()) {
            d.measure(c, 0f, 400f, 0f, 300f)
        }
        d.paint(c, 0)
        val f3x = b0.mAnimateMeasure?.getX() ?: b0.x
        println(
            "Frame 3 (t=1048ms): animX=$f3x, b0.x=${b0.x}, animDone=${b0.mAnimateMeasure?.isDone}, repaint=${d.needsRepaint()}"
        )

        // Frame 4 (t=1100ms):
        c.currentTime = startT + 100L
        if (d.needsMeasure()) {
            d.measure(c, 0f, 400f, 0f, 300f)
        }
        d.paint(c, 0)
        val f4x = b0.mAnimateMeasure?.getX() ?: b0.x
        println(
            "Frame 4 (t=1100ms): animX=$f4x, b0.x=${b0.x}, animDone=${b0.mAnimateMeasure?.isDone}, repaint=${d.needsRepaint()}"
        )

        // Frame 5 (t=1200ms):
        c.currentTime = startT + 200L
        if (d.needsMeasure()) {
            d.measure(c, 0f, 400f, 0f, 300f)
        }
        d.paint(c, 0)
        val f5x = b0.mAnimateMeasure?.getX() ?: b0.x
        println(
            "Frame 5 (t=1200ms): animX=$f5x, b0.x=${b0.x}, animDone=${b0.mAnimateMeasure?.isDone}, repaint=${d.needsRepaint()}"
        )

        // Frame 6 (t=1600ms, finished):
        c.currentTime = startT + 600L
        if (d.needsMeasure()) {
            d.measure(c, 0f, 400f, 0f, 300f)
        }
        d.paint(c, 0)
        val f6x = b0.mAnimateMeasure?.getX() ?: b0.x

        // Assertions:
    }

    @Test
    fun testThreeStateTransitions0120() {
        var stateVarId = -1
        var doc: CoreDocument? = null
        var context: MockRemoteContext? = null

        val ops =
            arrayListOf<TestOperation>(
                TestLayout {
                    val sv = writer.addInteger(0)
                    stateVarId = (sv - 0x100000000L).toInt()
                    column(Modifier.fillMaxSize()) {
                        stateLayout(Modifier.fillMaxWidth().height(100f), sv) {
                            row(
                                Modifier.fillMaxWidth().height(100f),
                                horizontal = RowLayout.START,
                            ) {
                                box(Modifier.size(50f).animationSpec(100).background(Color.RED))
                            }
                            row(
                                Modifier.fillMaxWidth().height(100f),
                                horizontal = RowLayout.CENTER,
                            ) {
                                box(Modifier.size(50f).animationSpec(100).background(Color.GREEN))
                            }
                            row(Modifier.fillMaxWidth().height(100f), horizontal = RowLayout.END) {
                                box(Modifier.size(50f).animationSpec(100).background(Color.BLUE))
                            }
                        }
                    }
                },
                object : TestOperation() {
                    override fun apply(
                        c: RemoteContext,
                        d: CoreDocument,
                        tp: TestParameters,
                        cm: MutableList<Map<String, Any>>?,
                    ): Boolean {
                        context = c as MockRemoteContext
                        doc = d
                        return false
                    }
                },
            )

        checkLayout(400, 300, 7, RcProfiles.PROFILE_ANDROIDX, "ThreeStateTransitions0120", ops)

        val d = doc!!
        val c = context!!
        c.setAnimationEnabled(true)
        val box0 = d.getComponent(-9)!!

        // Initial state 0
        c.currentTime = 1000L
        d.paint(c, 0)
        d.measure(c, 0f, 400f, 0f, 300f)
        Assert.assertEquals(0f, box0.x, 0.5f)

        // Transition State 0 -> State 1
        c.overrideInteger(stateVarId, 1)
        val startT1 = 1010L
        c.currentTime = startT1
        d.paint(c, 0)
        if (d.needsMeasure()) d.measure(c, 0f, 400f, 0f, 300f)

        c.currentTime = startT1 + 150L
        if (d.needsMeasure()) d.measure(c, 0f, 400f, 0f, 300f)
        d.paint(c, 0)
        val midT1X = box0.mAnimateMeasure?.getX() ?: box0.x
        Assert.assertTrue("Mid x ($midT1X) should be between 10 and 174", midT1X in 10f..174f)

        c.currentTime = startT1 + 600L
        if (d.needsMeasure()) d.measure(c, 0f, 400f, 0f, 300f)
        d.paint(c, 0)
        val endT1X = box0.mAnimateMeasure?.getX() ?: box0.x
        Assert.assertEquals(175f, endT1X, 2f)

        // Transition State 1 -> State 2
        c.overrideInteger(stateVarId, 2)
        val startT2 = startT1 + 700L
        c.currentTime = startT2
        d.paint(c, 0)
        if (d.needsMeasure()) d.measure(c, 0f, 400f, 0f, 300f)

        c.currentTime = startT2 + 150L
        if (d.needsMeasure()) d.measure(c, 0f, 400f, 0f, 300f)
        d.paint(c, 0)
        val midT2X = box0.mAnimateMeasure?.getX() ?: box0.x
        Assert.assertTrue("Mid x ($midT2X) should be between 175 and 349", midT2X in 175f..349f)

        c.currentTime = startT2 + 600L
        if (d.needsMeasure()) d.measure(c, 0f, 400f, 0f, 300f)
        d.paint(c, 0)
        val endT2X = box0.mAnimateMeasure?.getX() ?: box0.x
        Assert.assertEquals(350f, endT2X, 2f)

        // Transition State 2 -> State 0
        c.overrideInteger(stateVarId, 0)
        val startT0 = startT2 + 700L
        c.currentTime = startT0
        d.paint(c, 0)
        if (d.needsMeasure()) d.measure(c, 0f, 400f, 0f, 300f)

        c.currentTime = startT0 + 600L
        if (d.needsMeasure()) d.measure(c, 0f, 400f, 0f, 300f)
        d.paint(c, 0)
        val endT0X = box0.mAnimateMeasure?.getX() ?: box0.x
        Assert.assertEquals(0f, endT0X, 2f)
    }

    @Test
    fun testThreeStateInterruptedTransition() {
        var stateVarId = -1
        var doc: CoreDocument? = null
        var context: MockRemoteContext? = null

        val ops =
            arrayListOf<TestOperation>(
                TestLayout {
                    val sv = writer.addInteger(0)
                    stateVarId = (sv - 0x100000000L).toInt()
                    column(Modifier.fillMaxSize()) {
                        stateLayout(Modifier.fillMaxWidth().height(100f), sv) {
                            row(
                                Modifier.fillMaxWidth().height(100f),
                                horizontal = RowLayout.START,
                            ) {
                                box(Modifier.size(50f).animationSpec(100).background(Color.RED))
                            }
                            row(
                                Modifier.fillMaxWidth().height(100f),
                                horizontal = RowLayout.CENTER,
                            ) {
                                box(Modifier.size(50f).animationSpec(100).background(Color.GREEN))
                            }
                            row(Modifier.fillMaxWidth().height(100f), horizontal = RowLayout.END) {
                                box(Modifier.size(50f).animationSpec(100).background(Color.BLUE))
                            }
                        }
                    }
                },
                object : TestOperation() {
                    override fun apply(
                        c: RemoteContext,
                        d: CoreDocument,
                        tp: TestParameters,
                        cm: MutableList<Map<String, Any>>?,
                    ): Boolean {
                        context = c as MockRemoteContext
                        doc = d
                        return false
                    }
                },
            )

        checkLayout(400, 300, 7, RcProfiles.PROFILE_ANDROIDX, "ThreeStateInterrupted", ops)

        val d = doc!!
        val c = context!!
        c.setAnimationEnabled(true)
        val box0 = d.getComponent(-9)!!

        c.currentTime = 1000L
        d.paint(c, 0)
        d.measure(c, 0f, 400f, 0f, 300f)

        // Start 0 -> 1
        c.overrideInteger(stateVarId, 1)
        c.currentTime = 1010L
        if (d.needsMeasure()) d.measure(c, 0f, 400f, 0f, 300f)
        d.paint(c, 0)

        // Advance 100ms towards state 1 (x=175)
        c.currentTime = 1110L
        if (d.needsMeasure()) d.measure(c, 0f, 400f, 0f, 300f)
        d.paint(c, 0)
        val posBeforeInterrupt = box0.mAnimateMeasure?.getX() ?: box0.x

        // Interrupt mid-flight by changing state to 2 (x=350)
        c.overrideInteger(stateVarId, 2)
        c.currentTime = 1120L
        if (d.needsMeasure()) d.measure(c, 0f, 400f, 0f, 300f)
        d.paint(c, 0)

        val stateLayout =
            d.getComponent(-5)
                as androidx.compose.remote.core.operations.layout.managers.StateLayout
        val state1Row =
            stateLayout.getLayout(1)
                as androidx.compose.remote.core.operations.layout.LayoutComponent
        val box1 = state1Row.getChildrenComponents()[0]
        val posAtInterrupt = box1.mAnimateMeasure?.getX() ?: box1.x

        // Advance mid-way through state 2 transition (t = 1250L)
        c.currentTime = 1250L
        if (d.needsMeasure()) d.measure(c, 0f, 400f, 0f, 300f)
        d.paint(c, 0)
        val midPos = box1.mAnimateMeasure?.getX() ?: box1.x
        Assert.assertTrue(
            "Mid pos ($midPos) should be >= $posAtInterrupt",
            midPos >= posAtInterrupt,
        )

        // Advance to end of transition
        c.currentTime = 1800L
        if (d.needsMeasure()) d.measure(c, 0f, 400f, 0f, 300f)
        d.paint(c, 0)
        val state2Row =
            stateLayout.getLayout(2)
                as androidx.compose.remote.core.operations.layout.LayoutComponent
        val box2 = state2Row.getChildrenComponents()[0]
        Assert.assertEquals(350f, box2.x, 2f)
    }

    @Test
    fun testResize_anim1_jvm() {
        var doc: CoreDocument? = null
        var context: MockRemoteContext? = null

        val ops =
            arrayListOf<TestOperation>(
                TestLayout { box(Modifier.fillMaxSize().padding(8).background(Color.RED)) },
                object : TestOperation() {
                    override fun apply(
                        c: RemoteContext,
                        d: CoreDocument,
                        tp: TestParameters,
                        cm: MutableList<Map<String, Any>>?,
                    ): Boolean {
                        context = c as MockRemoteContext
                        doc = d
                        return false
                    }
                },
            )

        checkLayout(1000, 1000, 7, RcProfiles.PROFILE_ANDROIDX, "ResizeAnim1", ops)

        val d = doc!!
        val c = context!!
        c.setAnimationEnabled(true)

        // Snapshot 0: 1000x1000, t=0
        c.mWidth = 1000f
        c.mHeight = 1000f
        c.currentTime = 0
        d.paint(c, 0)
        println("SNAPSHOT 0 (t=0, 1000x1000):")
        println(d.displayHierarchy())

        // Snapshot 1: 1200x1300, t=0
        c.mWidth = 1200f
        c.mHeight = 1300f
        c.currentTime = 0
        d.paint(c, 0)
        println("SNAPSHOT 1 (t=0, 1200x1300):")
        println(d.displayHierarchy())

        // Snapshot 2: 1200x1300, t=150
        c.mWidth = 1200f
        c.mHeight = 1300f
        c.currentTime = 150
        d.paint(c, 0)
        println("SNAPSHOT 2 (t=150, 1200x1300):")
        println(d.displayHierarchy())

        // Snapshot 3: 1200x1300, t=300
        c.mWidth = 1200f
        c.mHeight = 1300f
        c.currentTime = 300
        d.paint(c, 0)
        println("SNAPSHOT 3 (t=300, 1200x1300):")
        println(d.displayHierarchy())
    }

    @Test
    fun testResize_anim2_jvm() {
        var doc: CoreDocument? = null
        var context: MockRemoteContext? = null

        val ops =
            arrayListOf<TestOperation>(
                TestLayout {
                    column(
                        Modifier.fillMaxSize(),
                        horizontal = ColumnLayout.CENTER,
                        vertical = ColumnLayout.CENTER,
                    ) {
                        box(Modifier.size(100).background(Color.RED))
                        box(Modifier.size(100).background(Color.GREEN))
                        box(Modifier.size(100).background(Color.BLUE))
                    }
                },
                object : TestOperation() {
                    override fun apply(
                        c: RemoteContext,
                        d: CoreDocument,
                        tp: TestParameters,
                        cm: MutableList<Map<String, Any>>?,
                    ): Boolean {
                        context = c as MockRemoteContext
                        doc = d
                        return false
                    }
                },
            )

        checkLayout(1000, 1000, 7, RcProfiles.PROFILE_ANDROIDX, "ResizeAnim2", ops)

        val d = doc!!
        val c = context!!
        c.setAnimationEnabled(true)

        // Snapshot 0: 1000x1000, t=0
        c.mWidth = 1000f
        c.mHeight = 1000f
        c.currentTime = 0
        d.paint(c, 0)
        println("SNAPSHOT 0 (t=0, 1000x1000):")
        println(d.displayHierarchy())

        // Snapshot 1: 1200x1300, t=0
        c.mWidth = 1200f
        c.mHeight = 1300f
        c.currentTime = 0
        d.paint(c, 0)
        println("SNAPSHOT 1 (t=0, 1200x1300):")
        println(d.displayHierarchy())

        // Snapshot 2: 1200x1300, t=150
        c.mWidth = 1200f
        c.mHeight = 1300f
        c.currentTime = 150
        d.paint(c, 0)
        println("SNAPSHOT 2 (t=150, 1200x1300):")
        println(d.displayHierarchy())

        // Snapshot 3: 1200x1300, t=300
        c.mWidth = 1200f
        c.mHeight = 1300f
        c.currentTime = 300
        d.paint(c, 0)
        println("SNAPSHOT 3 (t=300, 1200x1300):")
        println(d.displayHierarchy())
    }

    @Test
    fun testResize_anim3_jvm() {
        var doc: CoreDocument? = null
        var context: MockRemoteContext? = null

        val ops =
            arrayListOf<TestOperation>(
                TestLayout {
                    column(
                        Modifier.fillMaxSize(),
                        horizontal = ColumnLayout.CENTER,
                        vertical = ColumnLayout.CENTER,
                    ) {
                        box(Modifier.size(100).background(Color.RED))
                        row(
                            Modifier.fillMaxWidth(),
                            horizontal = RowLayout.CENTER,
                            vertical = RowLayout.CENTER,
                        ) {
                            box(Modifier.size(50).background(Color.YELLOW))
                            box(Modifier.size(75).background(Color.YELLOW))
                            box(Modifier.size(50).background(Color.YELLOW))
                        }
                        box(Modifier.size(100).background(Color.GREEN))
                        box(Modifier.size(100).background(Color.BLUE))
                    }
                },
                object : TestOperation() {
                    override fun apply(
                        c: RemoteContext,
                        d: CoreDocument,
                        tp: TestParameters,
                        cm: MutableList<Map<String, Any>>?,
                    ): Boolean {
                        context = c as MockRemoteContext
                        doc = d
                        return false
                    }
                },
            )

        checkLayout(1000, 1000, 7, RcProfiles.PROFILE_ANDROIDX, "ResizeAnim3", ops)

        val d = doc!!
        val c = context!!
        c.setAnimationEnabled(true)

        // Snapshot 0: 1000x1000, t=0
        c.mWidth = 1000f
        c.mHeight = 1000f
        c.currentTime = 0
        d.paint(c, 0)
        println("SNAPSHOT 0 (t=0, 1000x1000):")
        println(d.displayHierarchy())

        // Snapshot 1: 1200x1300, t=0
        c.mWidth = 1200f
        c.mHeight = 1300f
        c.currentTime = 0
        d.paint(c, 0)
        println("SNAPSHOT 1 (t=0, 1200x1300):")
        println(d.displayHierarchy())

        // Snapshot 2: 1200x1300, t=150
        c.mWidth = 1200f
        c.mHeight = 1300f
        c.currentTime = 150
        d.paint(c, 0)
        println("SNAPSHOT 2 (t=150, 1200x1300):")
        println(d.displayHierarchy())

        // Snapshot 3: 1200x1300, t=300
        c.mWidth = 1200f
        c.mHeight = 1300f
        c.currentTime = 300
        d.paint(c, 0)
        println("SNAPSHOT 3 (t=300, 1200x1300):")
        println(d.displayHierarchy())
    }
}
