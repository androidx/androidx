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

package androidx.ui.core.pointerinput

import androidx.test.filters.SmallTest
import androidx.ui.core.ConsumedData
import androidx.ui.core.LayoutNode
import androidx.ui.core.Modifier
import androidx.ui.core.Owner
import androidx.ui.core.PointerEventPass
import androidx.ui.core.PointerEventPass.InitialDown
import androidx.ui.core.PointerEventPass.PreUp
import androidx.ui.core.PointerId
import androidx.ui.core.PointerInputChange
import androidx.ui.core.PointerInputData
import androidx.ui.core.PointerInputHandler
import androidx.ui.unit.IntPxPosition
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition
import androidx.ui.unit.Uptime
import androidx.ui.unit.ipx
import androidx.ui.unit.milliseconds
import androidx.ui.unit.px
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

// TODO(shepshapard): Write the following PointerInputEvent to PointerInputChangeEvent tests
// 2 down, 2 move, 2 up, converted correctly
// 3 down, 3 move, 3 up, converted correctly
// down, up, down, up, converted correctly
// 2 down, 1 up, same down, both up, converted correctly
// 2 down, 1 up, new down, both up, converted correctly
// new is up, throws exception

// TODO(shepshapard): Write the following hit testing tests
// 2 down, one hits, target receives correct event
// 2 down, one moves in, one out, 2 up, target receives correct event stream
// down, up, receives down and up
// down, move, up, receives all 3
// down, up, then down and misses, target receives down and up
// down, misses, moves in bounds, up, target does not receive event
// down, hits, moves out of bounds, up, target receives all events

// TODO(shepshapard): Write the following offset testing tests
// 3 simultaneous moves, offsets are correct

// TODO(shepshapard): Write the following pointer input dispatch path tests:
// down, move, up, on 2, hits all 5 passes

@SmallTest
@RunWith(JUnit4::class)
class PointerInputEventProcessor2Test {

    private lateinit var root: LayoutNode
    private lateinit var pointerInputEventProcessor: PointerInputEventProcessor2
    private val testOwner: TestOwner = spy()

    @Before
    fun setup() {
        root = LayoutNode(0, 0, 500, 500)
        root.attach(testOwner)
        pointerInputEventProcessor = PointerInputEventProcessor2(root)
    }

    @Test
    fun process_downMoveUp_convertedCorrectlyAndTraversesAllPassesInCorrectOrder() {

        // Arrange

        val pointerInputHandler: PointerInputHandler = spy(StubPointerInputHandler())
        val layoutNode = LayoutNode(
            0,
            0,
            500,
            500,
            PointerInputModifier(
                TestPointerInputFilter(pointerInputHandler = pointerInputHandler)
            )
        )

        root.insertAt(0, layoutNode)

        val offset = PxPosition(100.px, 200.px)
        val offset2 = PxPosition(300.px, 400.px)

        val events = arrayOf(
            PointerInputEvent(8712, Uptime.Boot + 3.milliseconds, offset, true),
            PointerInputEvent(8712, Uptime.Boot + 11.milliseconds, offset2, true),
            PointerInputEvent(8712, Uptime.Boot + 13.milliseconds, offset2, false)
        )

        val expectedChanges = arrayOf(
            PointerInputChange(
                id = PointerId(8712),
                current = PointerInputData(Uptime.Boot + 3.milliseconds, offset, true),
                previous = PointerInputData(null, null, false),
                consumed = ConsumedData()
            ),
            PointerInputChange(
                id = PointerId(8712),
                current = PointerInputData(Uptime.Boot + 11.milliseconds, offset2, true),
                previous = PointerInputData(Uptime.Boot + 3.milliseconds, offset, true),
                consumed = ConsumedData()
            ),
            PointerInputChange(
                id = PointerId(8712),
                current = PointerInputData(Uptime.Boot + 13.milliseconds, offset2, false),
                previous = PointerInputData(Uptime.Boot + 11.milliseconds, offset2, true),
                consumed = ConsumedData()
            )
        )

        // Act

        events.forEach { pointerInputEventProcessor.process(it) }

        // Assert

        inOrder(pointerInputHandler) {
            for (expected in expectedChanges) {
                for (pass in PointerEventPass.values()) {
                    verify(pointerInputHandler).invoke(
                        eq(listOf(expected)),
                        eq(pass),
                        any()
                    )
                }
            }
        }
    }

    @Test
    fun process_downHits_targetReceives() {

        // Arrange

        val childOffset = PxPosition(100.px, 200.px)
        val pointerInputHandler: PointerInputHandler = spy(StubPointerInputHandler())
        val layoutNode = LayoutNode(
            100, 200, 301, 401,
            PointerInputModifier(
                TestPointerInputFilter(pointerInputHandler = pointerInputHandler)
            )
        )

        root.insertAt(0, layoutNode)

        val offsets = arrayOf(
            PxPosition(100.px, 200.px),
            PxPosition(300.px, 200.px),
            PxPosition(100.px, 400.px),
            PxPosition(300.px, 400.px)
        )

        val events = Array(4) { index ->
            PointerInputEvent(index, Uptime.Boot + 5.milliseconds, offsets[index], true)
        }

        val expectedChanges = Array(4) { index ->
            PointerInputChange(
                id = PointerId(index.toLong()),
                current = PointerInputData(
                    Uptime.Boot + 5.milliseconds,
                    offsets[index] - childOffset,
                    true
                ),
                previous = PointerInputData(null, null, false),
                consumed = ConsumedData()
            )
        }

        // Act

        events.forEach {
            pointerInputEventProcessor.process(it)
        }

        // Assert

        verify(pointerInputHandler, times(4)).invoke(
            any(),
            eq(InitialDown),
            any()
        )
        for (expected in expectedChanges) {
            verify(pointerInputHandler).invoke(
                eq(listOf(expected)),
                eq(InitialDown),
                any()
            )
        }
    }

    @Test
    fun process_downMisses_targetDoesNotReceive() {

        // Arrange

        val pointerInputHandler: PointerInputHandler = spy(StubPointerInputHandler())
        val layoutNode = LayoutNode(
            100, 200, 301, 401,
            PointerInputModifier(
                TestPointerInputFilter(pointerInputHandler = pointerInputHandler)
            )
        )

        root.insertAt(0, layoutNode)

        val offsets = arrayOf(
            PxPosition(99.px, 200.px),
            PxPosition(99.px, 400.px),
            PxPosition(100.px, 199.px),
            PxPosition(100.px, 401.px),
            PxPosition(300.px, 199.px),
            PxPosition(300.px, 401.px),
            PxPosition(301.px, 200.px),
            PxPosition(301.px, 400.px)
        )

        val events = Array(8) { index ->
            PointerInputEvent(index, Uptime.Boot + 0.milliseconds, offsets[index], true)
        }

        // Act

        events.forEach {
            pointerInputEventProcessor.process(it)
        }

        // Assert

        verify(pointerInputHandler, never()).invoke(any(), any(), any())
    }

    @Test
    fun process_downHits3of3_all3PointerNodesReceive() {
        process_partialTreeHits(3)
    }

    @Test
    fun process_downHits2of3_correct2PointerNodesReceive() {
        process_partialTreeHits(2)
    }

    @Test
    fun process_downHits1of3_onlyCorrectPointerNodesReceives() {
        process_partialTreeHits(1)
    }

    private fun process_partialTreeHits(numberOfChildrenHit: Int) {
        // Arrange

        val childPointerInputHandler: PointerInputHandler = spy(StubPointerInputHandler())
        val middlePointerInputHandler: PointerInputHandler = spy(StubPointerInputHandler())
        val parentPointerInputHandler: PointerInputHandler = spy(StubPointerInputHandler())

        val childLayoutNode =
            LayoutNode(
                100, 100, 200, 200,
                PointerInputModifier(
                    TestPointerInputFilter(pointerInputHandler = childPointerInputHandler)
                )
            )
        val middleLayoutNode: LayoutNode =
            LayoutNode(
                100, 100, 400, 400,
                PointerInputModifier(
                    TestPointerInputFilter(pointerInputHandler = middlePointerInputHandler)
                )
            ).apply {
                insertAt(0, childLayoutNode)
            }
        val parentLayoutNode: LayoutNode =
            LayoutNode(
                0, 0, 500, 500,
                PointerInputModifier(
                    TestPointerInputFilter(pointerInputHandler = parentPointerInputHandler)
                )
            ).apply {
                insertAt(0, middleLayoutNode)
            }
        root.insertAt(0, parentLayoutNode)

        val offset = when (numberOfChildrenHit) {
            3 -> PxPosition(250.px, 250.px)
            2 -> PxPosition(150.px, 150.px)
            1 -> PxPosition(50.px, 50.px)
            else -> throw IllegalStateException()
        }

        val event = PointerInputEvent(0, Uptime.Boot + 5.milliseconds, offset, true)

        // Act

        pointerInputEventProcessor.process(event)

        // Assert

        when (numberOfChildrenHit) {
            3 -> {
                verify(parentPointerInputHandler).invoke(
                    any(),
                    eq(InitialDown),
                    any()
                )
                verify(middlePointerInputHandler).invoke(
                    any(),
                    eq(InitialDown),
                    any()
                )
                verify(childPointerInputHandler).invoke(
                    any(),
                    eq(InitialDown),
                    any()
                )
            }
            2 -> {
                verify(parentPointerInputHandler).invoke(
                    any(),
                    eq(InitialDown),
                    any()
                )
                verify(middlePointerInputHandler).invoke(
                    any(),
                    eq(InitialDown),
                    any()
                )
                verify(childPointerInputHandler, never()).invoke(
                    any(),
                    any(),
                    any()
                )
            }
            1 -> {
                verify(parentPointerInputHandler).invoke(
                    any(),
                    eq(InitialDown),
                    any()
                )
                verify(middlePointerInputHandler, never()).invoke(
                    any(),
                    any(),
                    any()
                )
                verify(childPointerInputHandler, never()).invoke(
                    any(),
                    any(),
                    any()
                )
            }
            else -> throw IllegalStateException()
        }
    }

    @Test
    fun process_modifiedChange_isPassedToNext() {

        // Arrange

        val input = PointerInputChange(
            id = PointerId(0),
            current = PointerInputData(
                Uptime.Boot + 5.milliseconds,
                PxPosition(100.px, 0.px),
                true
            ),
            previous = PointerInputData(Uptime.Boot + 3.milliseconds, PxPosition(0.px, 0.px), true),
            consumed = ConsumedData(positionChange = PxPosition(0.px, 0.px))
        )
        val output = PointerInputChange(
            id = PointerId(0),
            current = PointerInputData(
                Uptime.Boot + 5.milliseconds,
                PxPosition(100.px, 0.px),
                true
            ),
            previous = PointerInputData(Uptime.Boot + 3.milliseconds, PxPosition(0.px, 0.px), true),
            consumed = ConsumedData(positionChange = PxPosition(13.px, 0.px))
        )

        val pointerInputHandler: PointerInputHandler =
            spy(StubPointerInputHandler { changes, pass, _ ->
                if (changes == listOf(input) &&
                    pass == InitialDown
                ) {
                    listOf(output)
                } else {
                    changes
                }
            })
        val layoutNode = LayoutNode(
            0, 0, 500, 500,
            PointerInputModifier(
                TestPointerInputFilter(pointerInputHandler = pointerInputHandler)
            )
        )

        root.insertAt(0, layoutNode)

        val down = PointerInputEvent(
            0,
            Uptime.Boot + 3.milliseconds,
            PxPosition(0.px, 0.px),
            true
        )
        val move = PointerInputEvent(
            0,
            Uptime.Boot + 5.milliseconds,
            PxPosition(100.px, 0.px),
            true
        )

        // Act

        pointerInputEventProcessor.process(down)
        reset(pointerInputHandler)
        pointerInputEventProcessor.process(move)

        // Assert

        verify(pointerInputHandler)
            .invoke(eq(listOf(input)), eq(InitialDown), any())
        verify(pointerInputHandler)
            .invoke(eq(listOf(output)), eq(PreUp), any())
    }

    @Test
    fun process_nodesAndAdditionalOffsetIncreasinglyInset_dispatchInfoIsCorrect() {
        process_dispatchInfoIsCorrect(
            0, 0, 100, 100,
            2, 11, 100, 100,
            23, 31, 100, 100,
            43, 51,
            99, 99
        )
    }

    @Test
    fun process_nodesAndAdditionalOffsetIncreasinglyOutset_dispatchInfoIsCorrect() {
        process_dispatchInfoIsCorrect(
            0, 0, 100, 100,
            -2, -11, 100, 100,
            -23, -31, 100, 100,
            -43, -51,
            1, 1
        )
    }

    @Test
    fun process_nodesAndAdditionalOffsetNotOffset_dispatchInfoIsCorrect() {
        process_dispatchInfoIsCorrect(
            0, 0, 100, 100,
            0, 0, 100, 100,
            0, 0, 100, 100,
            0, 0,
            50, 50
        )
    }

    @Suppress("SameParameterValue")
    private fun process_dispatchInfoIsCorrect(
        pX1: Int,
        pY1: Int,
        pX2: Int,
        pY2: Int,
        mX1: Int,
        mY1: Int,
        mX2: Int,
        mY2: Int,
        cX1: Int,
        cY1: Int,
        cX2: Int,
        cY2: Int,
        aOX: Int,
        aOY: Int,
        pointerX: Int,
        pointerY: Int
    ) {

        // Arrange

        val childPointerInputHandler: PointerInputHandler = spy(StubPointerInputHandler())
        val middlePointerInputHandler: PointerInputHandler = spy(StubPointerInputHandler())
        val parentPointerInputHandler: PointerInputHandler = spy(StubPointerInputHandler())

        val childOffset = PxPosition(cX1.px, cY1.px)
        val childLayoutNode = LayoutNode(
            cX1, cY1, cX2, cY2,
            PointerInputModifier(
                TestPointerInputFilter(pointerInputHandler = childPointerInputHandler)
            )
        )
        val middleOffset = PxPosition(mX1.px, mY1.px)
        val middleLayoutNode: LayoutNode = LayoutNode(
            mX1, mY1, mX2, mY2,
            PointerInputModifier(
                TestPointerInputFilter(pointerInputHandler = middlePointerInputHandler)
            )
        ).apply {
            insertAt(0, childLayoutNode)
        }
        val parentLayoutNode: LayoutNode = LayoutNode(
            pX1, pY1, pX2, pY2,
            PointerInputModifier(
                TestPointerInputFilter(pointerInputHandler = parentPointerInputHandler)
            )
        ).apply {
            insertAt(0, middleLayoutNode)
        }

        testOwner.position = IntPxPosition(aOX.ipx, aOY.ipx)

        root.insertAt(0, parentLayoutNode)

        val additionalOffset = IntPxPosition(aOX.ipx, aOY.ipx)

        val offset = PxPosition(pointerX.px, pointerY.px)

        val down = PointerInputEvent(0, Uptime.Boot + 7.milliseconds, offset, true)

        val pointerInputHandlers = arrayOf(
            parentPointerInputHandler,
            middlePointerInputHandler,
            childPointerInputHandler
        )

        val expectedPointerInputChanges = arrayOf(
            PointerInputChange(
                id = PointerId(0),
                current = PointerInputData(
                    Uptime.Boot + 7.milliseconds,
                    offset - additionalOffset,
                    true
                ),
                previous = PointerInputData(null, null, false),
                consumed = ConsumedData()
            ),
            PointerInputChange(
                id = PointerId(0),
                current = PointerInputData(
                    Uptime.Boot + 7.milliseconds,
                    offset - middleOffset - additionalOffset,
                    true
                ),
                previous = PointerInputData(null, null, false),
                consumed = ConsumedData()
            ),
            PointerInputChange(
                id = PointerId(0),
                current = PointerInputData(
                    Uptime.Boot + 7.milliseconds,
                    offset - middleOffset - childOffset - additionalOffset,
                    true
                ),
                previous = PointerInputData(null, null, false),
                consumed = ConsumedData()
            )
        )

        val expectedSizes = arrayOf(
            IntPxSize(pX2.ipx - pX1.ipx, pY2.ipx - pY1.ipx),
            IntPxSize(mX2.ipx - mX1.ipx, mY2.ipx - mY1.ipx),
            IntPxSize(cX2.ipx - cX1.ipx, cY2.ipx - cY1.ipx)
        )

        // Act

        pointerInputEventProcessor.process(down)

        // Assert

        for (pass in PointerEventPass.values()) {
            for (i in pointerInputHandlers.indices) {
                verify(pointerInputHandlers[i]).invoke(
                    listOf(expectedPointerInputChanges[i]),
                    pass,
                    expectedSizes[i]
                )
            }
        }
    }

    /**
     * This test creates a layout of this shape:
     *
     *  -------------
     *  |     |     |
     *  |  t  |     |
     *  |     |     |
     *  |-----|     |
     *  |           |
     *  |     |-----|
     *  |     |     |
     *  |     |  t  |
     *  |     |     |
     *  -------------
     *
     * Where there is one child in the top right, and one in the bottom left, and 2 down touches,
     * one in the top left and one in the bottom right.
     */
    @Test
    fun process_2DownOn2DifferentPointerNodes_hitAndDispatchInfoAreCorrect() {

        // Arrange

        val childPointerInputHandler1: PointerInputHandler = spy(StubPointerInputHandler())
        val childPointerInputHandler2: PointerInputHandler = spy(StubPointerInputHandler())

        val childLayoutNode1 =
            LayoutNode(
                0, 0, 50, 50,
                PointerInputModifier(
                    TestPointerInputFilter(pointerInputHandler = childPointerInputHandler1)
                )
            )
        val childLayoutNode2 =
            LayoutNode(
                50, 50, 100, 100,
                PointerInputModifier(
                    TestPointerInputFilter(pointerInputHandler = childPointerInputHandler2)
                )
            )
        root.apply {
            insertAt(0, childLayoutNode1)
            insertAt(0, childLayoutNode2)
        }

        val offset1 = PxPosition(25.px, 25.px)
        val offset2 = PxPosition(75.px, 75.px)

        val down = PointerInputEvent(
            Uptime.Boot + 5.milliseconds,
            listOf(
                PointerInputEventData(0, Uptime.Boot + 5.milliseconds, offset1, true),
                PointerInputEventData(1, Uptime.Boot + 5.milliseconds, offset2, true)
            )
        )

        val expectedChange1 = PointerInputChange(
            id = PointerId(0),
            current = PointerInputData(Uptime.Boot + 5.milliseconds, offset1, true),
            previous = PointerInputData(null, null, false),
            consumed = ConsumedData()
        )
        val expectedChange2 = PointerInputChange(
            id = PointerId(1),
            current = PointerInputData(
                Uptime.Boot + 5.milliseconds,
                offset2 - PxPosition(50.px, 50.px),
                true
            ),
            previous = PointerInputData(null, null, false),
            consumed = ConsumedData()
        )

        // Act

        pointerInputEventProcessor.process(down)

        // Assert

        for (pointerEventPass in PointerEventPass.values()) {
            verify(childPointerInputHandler1)
                .invoke(
                    listOf(expectedChange1),
                    pointerEventPass,
                    IntPxSize(50.ipx, 50.ipx)
                )
            verify(childPointerInputHandler2)
                .invoke(
                    listOf(expectedChange2),
                    pointerEventPass,
                    IntPxSize(50.ipx, 50.ipx)
                )
        }
        verifyNoMoreInteractions(
            childPointerInputHandler1,
            childPointerInputHandler2
        )
    }

    /**
     * This test creates a layout of this shape:
     *
     *  ---------------
     *  | t      |    |
     *  |        |    |
     *  |  |-------|  |
     *  |  | t     |  |
     *  |  |       |  |
     *  |  |       |  |
     *  |--|  |-------|
     *  |  |  | t     |
     *  |  |  |       |
     *  |  |  |       |
     *  |  |--|       |
     *  |     |       |
     *  ---------------
     *
     * There are 3 staggered children and 3 down events, the first is on child 1, the second is on
     * child 2 in a space that overlaps child 1, and the third is in a space that overlaps both
     * child 2.
     */
    @Test
    fun process_3DownOnOverlappingPointerNodes_hitAndDispatchInfoAreCorrect() {

        val childPointerInputHandler1: PointerInputHandler = spy(StubPointerInputHandler())
        val childPointerInputHandler2: PointerInputHandler = spy(StubPointerInputHandler())
        val childPointerInputHandler3: PointerInputHandler = spy(StubPointerInputHandler())

        val childLayoutNode1 = LayoutNode(
            0, 0, 100, 100,
            PointerInputModifier(
                TestPointerInputFilter(pointerInputHandler = childPointerInputHandler1)
            )
        )
        val childLayoutNode2 = LayoutNode(
            50, 50, 150, 150,
            PointerInputModifier(
                TestPointerInputFilter(pointerInputHandler = childPointerInputHandler2)
            )
        )
        val childLayoutNode3 = LayoutNode(
            100, 100, 200, 200,
            PointerInputModifier(
                TestPointerInputFilter(pointerInputHandler = childPointerInputHandler3)
            )
        )

        root.apply {
            insertAt(0, childLayoutNode1)
            insertAt(1, childLayoutNode2)
            insertAt(2, childLayoutNode3)
        }

        val offset1 = PxPosition(25.px, 25.px)
        val offset2 = PxPosition(75.px, 75.px)
        val offset3 = PxPosition(125.px, 125.px)

        val down = PointerInputEvent(
            Uptime.Boot + 5.milliseconds,
            listOf(
                PointerInputEventData(0, Uptime.Boot + 5.milliseconds, offset1, true),
                PointerInputEventData(1, Uptime.Boot + 5.milliseconds, offset2, true),
                PointerInputEventData(2, Uptime.Boot + 5.milliseconds, offset3, true)
            )
        )

        val expectedChange1 = PointerInputChange(
            id = PointerId(0),
            current = PointerInputData(Uptime.Boot + 5.milliseconds, offset1, true),
            previous = PointerInputData(null, null, false),
            consumed = ConsumedData()
        )
        val expectedChange2 = PointerInputChange(
            id = PointerId(1),
            current = PointerInputData(
                Uptime.Boot + 5.milliseconds,
                offset2 - PxPosition(50.px, 50.px),
                true
            ),
            previous = PointerInputData(null, null, false),
            consumed = ConsumedData()
        )
        val expectedChange3 = PointerInputChange(
            id = PointerId(2),
            current = PointerInputData(
                Uptime.Boot + 5.milliseconds,
                offset3 - PxPosition(100.px, 100.px),
                true
            ),
            previous = PointerInputData(null, null, false),
            consumed = ConsumedData()
        )

        // Act

        pointerInputEventProcessor.process(down)

        // Assert

        for (pointerEventPass in PointerEventPass.values()) {
            verify(childPointerInputHandler1)
                .invoke(listOf(expectedChange1), pointerEventPass, IntPxSize(100.ipx, 100.ipx))
            verify(childPointerInputHandler2)
                .invoke(listOf(expectedChange2), pointerEventPass, IntPxSize(100.ipx, 100.ipx))
            verify(childPointerInputHandler3)
                .invoke(listOf(expectedChange3), pointerEventPass, IntPxSize(100.ipx, 100.ipx))
        }
        verifyNoMoreInteractions(
            childPointerInputHandler1,
            childPointerInputHandler2,
            childPointerInputHandler3
        )
    }

    /**
     * This test creates a layout of this shape:
     *
     *  ---------------
     *  |             |
     *  |      t      |
     *  |             |
     *  |  |-------|  |
     *  |  |       |  |
     *  |  |   t   |  |
     *  |  |       |  |
     *  |  |-------|  |
     *  |             |
     *  |      t      |
     *  |             |
     *  ---------------
     *
     * There are 3 staggered children and 3 down events, the first is on child 1, the second is on
     * child 2 in a space that overlaps child 1, and the third is in a space that overlaps both
     * child 2.
     */
    @Test
    fun process_3DownOnFloatingPointerNodeV_hitAndDispatchInfoAreCorrect() {

        val childPointerInputHandler1: PointerInputHandler = spy(StubPointerInputHandler())
        val childPointerInputHandler2: PointerInputHandler = spy(StubPointerInputHandler())

        val childLayoutNode1 = LayoutNode(
            0, 0, 100, 150,
            PointerInputModifier(
                TestPointerInputFilter(pointerInputHandler = childPointerInputHandler1)
            )
        )
        val childLayoutNode2 = LayoutNode(
            25, 50, 75, 100,
            PointerInputModifier(
                TestPointerInputFilter(pointerInputHandler = childPointerInputHandler2)
            )
        )

        root.apply {
            insertAt(0, childLayoutNode1)
            insertAt(1, childLayoutNode2)
        }

        val offset1 = PxPosition(50.px, 25.px)
        val offset2 = PxPosition(50.px, 75.px)
        val offset3 = PxPosition(50.px, 125.px)

        val down = PointerInputEvent(
            Uptime.Boot + 7.milliseconds,
            listOf(
                PointerInputEventData(0, Uptime.Boot + 7.milliseconds, offset1, true),
                PointerInputEventData(1, Uptime.Boot + 7.milliseconds, offset2, true),
                PointerInputEventData(2, Uptime.Boot + 7.milliseconds, offset3, true)
            )
        )

        val expectedChange1 = PointerInputChange(
            id = PointerId(0),
            current = PointerInputData(Uptime.Boot + 7.milliseconds, offset1, true),
            previous = PointerInputData(null, null, false),
            consumed = ConsumedData()
        )
        val expectedChange2 = PointerInputChange(
            id = PointerId(1),
            current = PointerInputData(
                Uptime.Boot + 7.milliseconds,
                offset2 - PxPosition(25.px, 50.px),
                true
            ),
            previous = PointerInputData(null, null, false),
            consumed = ConsumedData()
        )
        val expectedChange3 = PointerInputChange(
            id = PointerId(2),
            current = PointerInputData(Uptime.Boot + 7.milliseconds, offset3, true),
            previous = PointerInputData(null, null, false),
            consumed = ConsumedData()
        )

        // Act

        pointerInputEventProcessor.process(down)

        // Assert

        for (pointerEventPass in PointerEventPass.values()) {
            verify(childPointerInputHandler1)
                .invoke(
                    listOf(expectedChange1, expectedChange3),
                    pointerEventPass,
                    IntPxSize(100.ipx, 150.ipx)
                )
            verify(childPointerInputHandler2)
                .invoke(
                    listOf(expectedChange2),
                    pointerEventPass,
                    IntPxSize(50.ipx, 50.ipx)
                )
        }
        verifyNoMoreInteractions(
            childPointerInputHandler1,
            childPointerInputHandler2
        )
    }

    /**
     * This test creates a layout of this shape:
     *
     *  -----------------
     *  |               |
     *  |   |-------|   |
     *  |   |       |   |
     *  | t |   t   | t |
     *  |   |       |   |
     *  |   |-------|   |
     *  |               |
     *  -----------------
     *
     * There are 3 staggered children and 3 down events, the first is on child 1, the second is on
     * child 2 in a space that overlaps child 1, and the third is in a space that overlaps both
     * child 2.
     */
    @Test
    fun process_3DownOnFloatingPointerNodeH_hitAndDispatchInfoAreCorrect() {
        val childPointerInputHandler1: PointerInputHandler = spy(StubPointerInputHandler())
        val childPointerInputHandler2: PointerInputHandler = spy(StubPointerInputHandler())

        val childLayoutNode1 = LayoutNode(
            0, 0, 150, 100,
            PointerInputModifier(
                TestPointerInputFilter(pointerInputHandler = childPointerInputHandler1)
            )
        )
        val childLayoutNode2 = LayoutNode(
            50, 25, 100, 75,
            PointerInputModifier(
                TestPointerInputFilter(pointerInputHandler = childPointerInputHandler2)
            )
        )

        root.apply {
            insertAt(0, childLayoutNode1)
            insertAt(1, childLayoutNode2)
        }

        val offset1 = PxPosition(25.px, 50.px)
        val offset2 = PxPosition(75.px, 50.px)
        val offset3 = PxPosition(125.px, 50.px)

        val down = PointerInputEvent(
            Uptime.Boot + 11.milliseconds,
            listOf(
                PointerInputEventData(0, Uptime.Boot + 11.milliseconds, offset1, true),
                PointerInputEventData(1, Uptime.Boot + 11.milliseconds, offset2, true),
                PointerInputEventData(2, Uptime.Boot + 11.milliseconds, offset3, true)
            )
        )

        val expectedChange1 = PointerInputChange(
            id = PointerId(0),
            current = PointerInputData(Uptime.Boot + 11.milliseconds, offset1, true),
            previous = PointerInputData(null, null, false),
            consumed = ConsumedData()
        )
        val expectedChange2 = PointerInputChange(
            id = PointerId(1),
            current = PointerInputData(
                Uptime.Boot + 11.milliseconds,
                offset2 - PxPosition(50.px, 25.px),
                true
            ),
            previous = PointerInputData(null, null, false),
            consumed = ConsumedData()
        )
        val expectedChange3 = PointerInputChange(
            id = PointerId(2),
            current = PointerInputData(Uptime.Boot + 11.milliseconds, offset3, true),
            previous = PointerInputData(null, null, false),
            consumed = ConsumedData()
        )

        // Act

        pointerInputEventProcessor.process(down)

        // Assert

        for (pointerEventPass in PointerEventPass.values()) {
            verify(childPointerInputHandler1)
                .invoke(
                    listOf(expectedChange1, expectedChange3),
                    pointerEventPass,
                    IntPxSize(150.ipx, 100.ipx)
                )
            verify(childPointerInputHandler2)
                .invoke(
                    listOf(expectedChange2),
                    pointerEventPass,
                    IntPxSize(50.ipx, 50.ipx)
                )
        }
        verifyNoMoreInteractions(
            childPointerInputHandler1,
            childPointerInputHandler2
        )
    }

    /**
     * This test creates a layout of this shape:
     *     0   1   2   3   4
     *   .........   .........
     * 0 .     t .   . t     .
     *   .   |---|---|---|   .
     * 1 . t | t |   | t | t .
     *   ....|---|   |---|....
     * 2     |           |
     *   ....|---|   |---|....
     * 3 . t | t |   | t | t .
     *   .   |---|---|---|   .
     * 4 .     t .   . t     .
     *   .........   .........
     *
     * 4 LayoutNodes with PointerInputModifiers that are clipped by their parent LayoutNode. 4
     * touches touch just inside the parent LayoutNode and inside the child LayoutNodes. 8
     * touches touch just outside the parent LayoutNode but inside the child LayoutNodes.
     *
     * Because LayoutNodes clip the bounds where children LayoutNodes can be hit, all 8 should miss,
     * but the other 4 touches are inside both, so hit.
     */
    @Test
    fun process_4DownInClippedAreaOfLnsWithPims_onlyCorrectPointersHit() {

        // Arrange

        val singlePointerInputHandler: PointerInputHandler = spy(StubPointerInputHandler())

        val layoutNode1 = LayoutNode(
            -1, -1, 1, 1,
            PointerInputModifier(
                TestPointerInputFilter(pointerInputHandler = singlePointerInputHandler)
            )
        )
        val layoutNode2 = LayoutNode(
            2, -1, 4, 1,
            PointerInputModifier(
                TestPointerInputFilter(pointerInputHandler = singlePointerInputHandler)
            )
        )
        val layoutNode3 = LayoutNode(
            -1, 2, 1, 4,
            PointerInputModifier(
                TestPointerInputFilter(pointerInputHandler = singlePointerInputHandler)
            )
        )
        val layoutNode4 = LayoutNode(
            2, 2, 4, 4,
            PointerInputModifier(
                TestPointerInputFilter(pointerInputHandler = singlePointerInputHandler)
            )
        )

        val parentLayoutNode = LayoutNode(1, 1, 4, 4).apply {
            insertAt(0, layoutNode1)
            insertAt(1, layoutNode2)
            insertAt(2, layoutNode3)
            insertAt(3, layoutNode4)
        }
        root.apply {
            insertAt(0, parentLayoutNode)
        }
        val offsetsThatHit =
            listOf(
                PxPosition(1.px, 1.px),
                PxPosition(3.px, 1.px),
                PxPosition(1.px, 3.px),
                PxPosition(3.px, 3.px)
            )
        val offsetsThatMiss =
            listOf(
                PxPosition(1.px, 0.px),
                PxPosition(3.px, 0.px),
                PxPosition(0.px, 1.px),
                PxPosition(4.px, 1.px),
                PxPosition(0.px, 3.px),
                PxPosition(4.px, 3.px),
                PxPosition(1.px, 4.px),
                PxPosition(3.px, 4.px)
            )
        val allOffsets = offsetsThatHit + offsetsThatMiss
        val pointerInputEvent =
            PointerInputEvent(
                Uptime.Boot + 11.milliseconds,
                (allOffsets.indices).map {
                    PointerInputEventData(it, Uptime.Boot + 11.milliseconds, allOffsets[it], true)
                }
            )

        // Act

        pointerInputEventProcessor.process(pointerInputEvent)

        // Assert

        val expectedChanges =
            (offsetsThatHit.indices).map {
                PointerInputChange(
                    id = PointerId(it.toLong()),
                    current = PointerInputData(
                        Uptime.Boot + 11.milliseconds,
                        PxPosition(
                            if (offsetsThatHit[it].x == 1.px) 1.px else 0.px,
                            if (offsetsThatHit[it].y == 1.px) 1.px else 0.px
                        ),
                        true
                    ),
                    previous = PointerInputData(null, null, false),
                    consumed = ConsumedData()
                )
            }
        PointerEventPass.values().forEach { pointerEventPass ->
            expectedChanges.forEach { change ->
                verify(singlePointerInputHandler).invoke(
                    eq(listOf(change)),
                    eq(pointerEventPass),
                    any()
                )
            }
        }
        verifyNoMoreInteractions(
            singlePointerInputHandler
        )
    }

    /**
     * This test creates a layout of this shape:
     *
     *   |---|
     *   |tt |
     *   |t  |
     *   |---|t
     *       tt
     *
     *   But where the additional offset suggest something more like this shape.
     *
     *   tt
     *   t|---|
     *    |  t|
     *    | tt|
     *    |---|
     *
     *   Without the additional offset, it would be expected that only the top left 3 pointers would
     *   hit, but with the additional offset, only the bottom right 3 hit.
     */
    @Test
    fun process_rootIsOffset_onlyCorrectPointersHit() {

        // Arrange
        val singlePointerInputHandler: PointerInputHandler = spy(StubPointerInputHandler())
        val layoutNode = LayoutNode(
            0, 0, 2, 2,
            PointerInputModifier(
                TestPointerInputFilter(pointerInputHandler = singlePointerInputHandler)
            )
        )
        root.apply {
            insertAt(0, layoutNode)
        }
        val offsetsThatHit =
            listOf(
                PxPosition(2.px, 2.px),
                PxPosition(2.px, 1.px),
                PxPosition(1.px, 2.px)
            )
        val offsetsThatMiss =
            listOf(
                PxPosition(0.px, 0.px),
                PxPosition(0.px, 1.px),
                PxPosition(1.px, 0.px)
            )
        val allOffsets = offsetsThatHit + offsetsThatMiss
        val pointerInputEvent =
            PointerInputEvent(
                Uptime.Boot + 11.milliseconds,
                (allOffsets.indices).map {
                    PointerInputEventData(it, Uptime.Boot + 11.milliseconds, allOffsets[it], true)
                }
            )
        testOwner.position = IntPxPosition(1.ipx, 1.ipx)

        // Act

        pointerInputEventProcessor.process(pointerInputEvent)

        // Assert

        val expectedChanges =
            (offsetsThatHit.indices).map {
                PointerInputChange(
                    id = PointerId(it.toLong()),
                    current = PointerInputData(
                        Uptime.Boot + 11.milliseconds,
                        offsetsThatHit[it] - PxPosition(1.px, 1.px),
                        true
                    ),
                    previous = PointerInputData(null, null, false),
                    consumed = ConsumedData()
                )
            }
        PointerEventPass.values().forEach { pointerEventPass ->
            verify(singlePointerInputHandler).invoke(
                eq(expectedChanges),
                eq(pointerEventPass),
                any()
            )
        }
        verifyNoMoreInteractions(
            singlePointerInputHandler
        )
    }

    @Test
    fun process_downOn3NestedPointerInputModifiers_hitAndDispatchInfoAreCorrect() {

        val pointerInputHandler1: PointerInputHandler = spy(StubPointerInputHandler())
        val pointerInputHandler2: PointerInputHandler = spy(StubPointerInputHandler())
        val pointerInputHandler3: PointerInputHandler = spy(StubPointerInputHandler())

        val modifier =
            PointerInputModifier(
                TestPointerInputFilter(pointerInputHandler = pointerInputHandler1)
            ) +
                    PointerInputModifier(
                        TestPointerInputFilter(pointerInputHandler = pointerInputHandler2)
                    ) +
                    PointerInputModifier(
                        TestPointerInputFilter(pointerInputHandler = pointerInputHandler3)
                    )

        val layoutNode = LayoutNode(
            25, 50, 75, 100,
            modifier
        )

        root.apply {
            insertAt(0, layoutNode)
        }

        val offset1 = PxPosition(50.px, 75.px)

        val down = PointerInputEvent(
            Uptime.Boot + 7.milliseconds,
            listOf(
                PointerInputEventData(0, Uptime.Boot + 7.milliseconds, offset1, true)
            )
        )

        val expectedChange = PointerInputChange(
            id = PointerId(0),
            current = PointerInputData(
                Uptime.Boot + 7.milliseconds,
                offset1 - PxPosition(25.px, 50.px),
                true
            ),
            previous = PointerInputData(null, null, false),
            consumed = ConsumedData()
        )

        // Act

        pointerInputEventProcessor.process(down)

        // Assert

        for (pointerEventPass in PointerEventPass.values()) {
            verify(pointerInputHandler1)
                .invoke(
                    listOf(expectedChange),
                    pointerEventPass,
                    IntPxSize(50.ipx, 50.ipx)
                )
            verify(pointerInputHandler2)
                .invoke(
                    listOf(expectedChange),
                    pointerEventPass,
                    IntPxSize(50.ipx, 50.ipx)
                )
            verify(pointerInputHandler3)
                .invoke(
                    listOf(expectedChange),
                    pointerEventPass,
                    IntPxSize(50.ipx, 50.ipx)
                )
        }
        verifyNoMoreInteractions(pointerInputHandler1, pointerInputHandler2, pointerInputHandler3)
    }

    @Test
    fun process_downOnDeeplyNestedPointerInputModifier_hitAndDispatchInfoAreCorrect() {

        val pointerInputHandler1: PointerInputHandler = spy(StubPointerInputHandler())

        val layoutNode1 =
            LayoutNode(
                1, 5, 500, 500,
                PointerInputModifier(
                    TestPointerInputFilter(pointerInputHandler = pointerInputHandler1)
                )
            )
        val layoutNode2: LayoutNode = LayoutNode(2, 6, 500, 500).apply {
            insertAt(0, layoutNode1)
        }
        val layoutNode3: LayoutNode = LayoutNode(3, 7, 500, 500).apply {
            insertAt(0, layoutNode2)
        }
        val layoutNode4: LayoutNode = LayoutNode(4, 8, 500, 500).apply {
            insertAt(0, layoutNode3)
        }
        root.apply {
            insertAt(0, layoutNode4)
        }

        val offset1 = PxPosition(499.px, 499.px)

        val downEvent = PointerInputEvent(
            Uptime.Boot + 7.milliseconds,
            listOf(
                PointerInputEventData(0, Uptime.Boot + 7.milliseconds, offset1, true)
            )
        )

        val expectedChange = PointerInputChange(
            id = PointerId(0),
            current = PointerInputData(
                Uptime.Boot + 7.milliseconds,
                offset1 - PxPosition(1.px + 2.px + 3.px + 4.px, 5.px + 6.px + 7.px + 8.px),
                true
            ),
            previous = PointerInputData(null, null, false),
            consumed = ConsumedData()
        )

        // Act

        pointerInputEventProcessor.process(downEvent)

        // Assert

        for (pointerEventPass in PointerEventPass.values()) {
            verify(pointerInputHandler1)
                .invoke(
                    listOf(expectedChange),
                    pointerEventPass,
                    IntPxSize(499.ipx, 495.ipx)
                )
        }
        verifyNoMoreInteractions(pointerInputHandler1)
    }

    @Test
    fun process_downOnComplexPointerAndLayoutNodePath_hitAndDispatchInfoAreCorrect() {

        val pointerInputHandler2: PointerInputHandler = spy(StubPointerInputHandler())
        val pointerInputHandler1: PointerInputHandler = spy(StubPointerInputHandler())
        val pointerInputHandler3: PointerInputHandler = spy(StubPointerInputHandler())
        val pointerInputHandler4: PointerInputHandler = spy(StubPointerInputHandler())

        val layoutNode1 = LayoutNode(
            1, 6, 500, 500,
            PointerInputModifier(
                TestPointerInputFilter(pointerInputHandler = pointerInputHandler1)
            ) + PointerInputModifier(
                TestPointerInputFilter(pointerInputHandler = pointerInputHandler2)
            )
        )
        val layoutNode2: LayoutNode = LayoutNode(2, 7, 500, 500).apply {
            insertAt(0, layoutNode1)
        }
        val layoutNode3 =
            LayoutNode(
                3, 8, 500, 500,
                PointerInputModifier(
                    TestPointerInputFilter(pointerInputHandler = pointerInputHandler3)
                ) + PointerInputModifier(
                    TestPointerInputFilter(pointerInputHandler = pointerInputHandler4)
                )
            ).apply {
                insertAt(0, layoutNode2)
            }

        val layoutNode4: LayoutNode = LayoutNode(4, 9, 500, 500).apply {
            insertAt(0, layoutNode3)
        }
        val layoutNode5: LayoutNode = LayoutNode(5, 10, 500, 500).apply {
            insertAt(0, layoutNode4)
        }
        root.apply {
            insertAt(0, layoutNode5)
        }

        val offset1 = PxPosition(499.px, 499.px)

        val downEvent = PointerInputEvent(
            Uptime.Boot + 3.milliseconds,
            listOf(
                PointerInputEventData(0, Uptime.Boot + 3.milliseconds, offset1, true)
            )
        )

        val expectedChange1 = PointerInputChange(
            id = PointerId(0),
            current = PointerInputData(
                Uptime.Boot + 3.milliseconds,
                offset1 - PxPosition(
                    1.px + 2.px + 3.px + 4.px + 5.px,
                    6.px + 7.px + 8.px + 9.px + 10.px
                ),
                true
            ),
            previous = PointerInputData(null, null, false),
            consumed = ConsumedData()
        )

        val expectedChange2 = PointerInputChange(
            id = PointerId(0),
            current = PointerInputData(
                Uptime.Boot + 3.milliseconds,
                offset1 - PxPosition(3.px + 4.px + 5.px, 8.px + 9.px + 10.px),
                true
            ),
            previous = PointerInputData(null, null, false),
            consumed = ConsumedData()
        )

        // Act

        pointerInputEventProcessor.process(downEvent)

        // Assert

        for (pointerEventPass in PointerEventPass.values()) {
            verify(pointerInputHandler1)
                .invoke(
                    listOf(expectedChange1),
                    pointerEventPass,
                    IntPxSize(499.ipx, 494.ipx)
                )
            verify(pointerInputHandler2)
                .invoke(
                    listOf(expectedChange1),
                    pointerEventPass,
                    IntPxSize(499.ipx, 494.ipx)
                )
            verify(pointerInputHandler3)
                .invoke(
                    listOf(expectedChange2),
                    pointerEventPass,
                    IntPxSize(497.ipx, 492.ipx)
                )
            verify(pointerInputHandler4)
                .invoke(
                    listOf(expectedChange2),
                    pointerEventPass,
                    IntPxSize(497.ipx, 492.ipx)
                )
        }
        verifyNoMoreInteractions(
            pointerInputHandler1,
            pointerInputHandler2,
            pointerInputHandler3,
            pointerInputHandler4
        )
    }

    @Test
    fun process_downOnFullyOverlappingPointerInputModifiers_onlyTopPointerInputModifierReceives() {

        val pointerInputHandler1: PointerInputHandler = spy(StubPointerInputHandler())
        val pointerInputHandler2: PointerInputHandler = spy(StubPointerInputHandler())

        val layoutNode1 = LayoutNode(
            0, 0, 100, 100,
            PointerInputModifier(
                TestPointerInputFilter(pointerInputHandler = pointerInputHandler1)
            )
        )
        val layoutNode2 = LayoutNode(
            0, 0, 100, 100,
            PointerInputModifier(
                TestPointerInputFilter(pointerInputHandler = pointerInputHandler2)
            )
        )

        root.apply {
            insertAt(0, layoutNode1)
            insertAt(1, layoutNode2)
        }

        val down = PointerInputEvent(
            1, Uptime.Boot + 0.milliseconds, PxPosition(50.px, 50.px), true
        )

        // Act

        pointerInputEventProcessor.process(down)

        // Assert
        verify(pointerInputHandler2, times(5)).invoke(any(), any(), any())
        verify(pointerInputHandler1, never()).invoke(any(), any(), any())
    }

    @Test
    fun process_downOnPointerInputModifierInLayoutNodeWithNoSize_downNotReceived() {

        val pointerInputHandler1: PointerInputHandler = spy(StubPointerInputHandler())

        val layoutNode1 = LayoutNode(
            0, 0, 0, 0,
            PointerInputModifier(
                TestPointerInputFilter(pointerInputHandler = pointerInputHandler1)
            )
        )

        root.apply {
            insertAt(0, layoutNode1)
        }

        val down = PointerInputEvent(
            1, Uptime.Boot + 0.milliseconds, PxPosition(0.px, 0.px), true
        )

        // Act
        pointerInputEventProcessor.process(down)

        // Assert
        verify(pointerInputHandler1, never()).invoke(any(), any(), any())
    }

    // Cancel Handlers

    @Test
    fun processCancel_noPointers_doesntCrash() {
        pointerInputEventProcessor.processCancel()
    }

    @Test
    fun processCancel_downThenCancel_pimOnlyReceivesCorrectDownThenCancel() {

        // Arrange

        val pointerInputHandler: PointerInputHandler = spy(StubPointerInputHandler())
        val cancelHandler: () -> Unit = spy()

        val layoutNode = LayoutNode(
            0, 0, 500, 500,
            PointerInputModifier(
                TestPointerInputFilter(
                    pointerInputHandler = pointerInputHandler,
                    cancelHandler = cancelHandler
                )
            )
        )

        root.insertAt(0, layoutNode)

        val pointerInputEvent =
            PointerInputEvent(
                7,
                Uptime.Boot + 5.milliseconds,
                PxPosition(250.px, 250.px),
                true
            )

        val expectedChange =
            PointerInputChange(
                id = PointerId(7),
                current = PointerInputData(
                    Uptime.Boot + 5.milliseconds,
                    PxPosition(250.px, 250.px),
                    true
                ),
                previous = PointerInputData(null, null, false),
                consumed = ConsumedData()
            )

        // Act

        pointerInputEventProcessor.process(pointerInputEvent)
        pointerInputEventProcessor.processCancel()

        // Assert

        inOrder(pointerInputHandler, cancelHandler) {
            for (pass in PointerEventPass.values()) {
                verify(pointerInputHandler).invoke(
                    eq(listOf(expectedChange)),
                    eq(pass),
                    any()
                )
            }
            verify(cancelHandler).invoke()
        }
        verifyNoMoreInteractions(
            pointerInputHandler,
            cancelHandler
        )
    }

    @Test
    fun processCancel_downDownOnSamePimThenCancel_pimOnlyReceivesCorrectChangesThenCancel() {

        // Arrange

        val pointerInputHandler: PointerInputHandler = spy(StubPointerInputHandler())
        val cancelHandler: () -> Unit = spy()

        val layoutNode = LayoutNode(
            0, 0, 500, 500,
            PointerInputModifier(
                TestPointerInputFilter(
                    pointerInputHandler = pointerInputHandler,
                    cancelHandler = cancelHandler
                )
            )
        )

        root.insertAt(0, layoutNode)

        val pointerInputEvent1 =
            PointerInputEvent(
                7,
                Uptime.Boot + 5.milliseconds,
                PxPosition(200.px, 200.px),
                true
            )

        val pointerInputEvent2 =
            PointerInputEvent(
                Uptime.Boot + 10.milliseconds,
                listOf(
                    PointerInputEventData(
                        7,
                        Uptime.Boot + 10.milliseconds,
                        PxPosition(200.px, 200.px),
                        true
                    ),
                    PointerInputEventData(
                        9,
                        Uptime.Boot + 10.milliseconds,
                        PxPosition(300.px, 300.px),
                        true
                    )
                )
            )

        val expectedChanges1 =
            listOf(
                PointerInputChange(
                    id = PointerId(7),
                    current = PointerInputData(
                        Uptime.Boot + 5.milliseconds,
                        PxPosition(200.px, 200.px),
                        true
                    ),
                    previous = PointerInputData(null, null, false),
                    consumed = ConsumedData()
                )
            )

        val expectedChanges2 =
            listOf(
                PointerInputChange(
                    id = PointerId(7),
                    current = PointerInputData(
                        Uptime.Boot + 10.milliseconds,
                        PxPosition(200.px, 200.px),
                        true
                    ),
                    previous = PointerInputData(
                        Uptime.Boot + 5.milliseconds,
                        PxPosition(200.px, 200.px),
                        true
                    ),
                    consumed = ConsumedData()
                ),
                PointerInputChange(
                    id = PointerId(9),
                    current = PointerInputData(
                        Uptime.Boot + 10.milliseconds,
                        PxPosition(300.px, 300.px),
                        true
                    ),
                    previous = PointerInputData(null, null, false),
                    consumed = ConsumedData()
                )
            )

        // Act

        pointerInputEventProcessor.process(pointerInputEvent1)
        pointerInputEventProcessor.process(pointerInputEvent2)
        pointerInputEventProcessor.processCancel()

        // Assert

        inOrder(pointerInputHandler, cancelHandler) {
            for (pass in PointerEventPass.values()) {
                verify(pointerInputHandler).invoke(
                    eq(expectedChanges1),
                    eq(pass),
                    any()
                )
            }
            for (pass in PointerEventPass.values()) {
                verify(pointerInputHandler).invoke(
                    eq(expectedChanges2),
                    eq(pass),
                    any()
                )
            }
            verify(cancelHandler).invoke()
        }
        verifyNoMoreInteractions(
            pointerInputHandler,
            cancelHandler
        )
    }

    @Test
    fun processCancel_downOn2DifferentPimsThenCancel_pimsOnlyReceiveCorrectDownsThenCancel() {

        // Arrange

        val pointerInputHandler1: PointerInputHandler = spy(StubPointerInputHandler())
        val cancelHandler1: () -> Unit = spy()
        val layoutNode1 = LayoutNode(
            0, 0, 199, 199,
            PointerInputModifier(
                TestPointerInputFilter(
                    pointerInputHandler = pointerInputHandler1,
                    cancelHandler = cancelHandler1
                )
            )
        )

        val pointerInputHandler2: PointerInputHandler = spy(StubPointerInputHandler())
        val cancelHandler2: () -> Unit = spy()
        val layoutNode2 = LayoutNode(
            200, 200, 399, 399,
            PointerInputModifier(
                TestPointerInputFilter(
                    pointerInputHandler = pointerInputHandler2,
                    cancelHandler = cancelHandler2
                )
            )
        )

        root.insertAt(0, layoutNode1)
        root.insertAt(1, layoutNode2)

        val pointerInputEventData1 =
            PointerInputEventData(
                7,
                Uptime.Boot + 5.milliseconds,
                PxPosition(100.px, 100.px),
                true
            )

        val pointerInputEventData2 =
            PointerInputEventData(
                9,
                Uptime.Boot + 5.milliseconds,
                PxPosition(300.px, 300.px),
                true
            )

        val pointerInputEvent = PointerInputEvent(
            Uptime.Boot + 5.milliseconds,
            listOf(pointerInputEventData1, pointerInputEventData2)
        )

        val expectedChange1 =
            PointerInputChange(
                id = PointerId(7),
                current = PointerInputData(
                    Uptime.Boot + 5.milliseconds,
                    PxPosition(100.px, 100.px),
                    true
                ),
                previous = PointerInputData(null, null, false),
                consumed = ConsumedData()
            )

        val expectedChange2 =
            PointerInputChange(
                id = PointerId(9),
                current = PointerInputData(
                    Uptime.Boot + 5.milliseconds,
                    PxPosition(100.px, 100.px),
                    true
                ),
                previous = PointerInputData(null, null, false),
                consumed = ConsumedData()
            )

        // Act

        pointerInputEventProcessor.process(pointerInputEvent)
        pointerInputEventProcessor.processCancel()

        // Assert

        inOrder(pointerInputHandler1, cancelHandler1) {
            for (pass in PointerEventPass.values()) {
                verify(pointerInputHandler1).invoke(
                    eq(listOf(expectedChange1)),
                    eq(pass),
                    any()
                )
            }
            verify(cancelHandler1).invoke()
        }
        inOrder(pointerInputHandler2, cancelHandler2) {
            for (pass in PointerEventPass.values()) {
                verify(pointerInputHandler2).invoke(
                    eq(listOf(expectedChange2)),
                    eq(pass),
                    any()
                )
            }
            verify(cancelHandler2).invoke()
        }
        verifyNoMoreInteractions(
            pointerInputHandler1,
            cancelHandler1,
            pointerInputHandler2,
            cancelHandler2
        )
    }

    @Test
    fun processCancel_downMoveCancel_pimOnlyReceivesCorrectDownMoveCancel() {

        // Arrange

        val pointerInputHandler: PointerInputHandler = spy(StubPointerInputHandler())
        val cancelHandler: () -> Unit = spy()
        val layoutNode = LayoutNode(
            0, 0, 500, 500,
            PointerInputModifier(
                TestPointerInputFilter(
                    pointerInputHandler = pointerInputHandler,
                    cancelHandler = cancelHandler
                )
            )
        )

        root.insertAt(0, layoutNode)

        val down =
            PointerInputEvent(
                7,
                Uptime.Boot + 5.milliseconds,
                PxPosition(200.px, 200.px),
                true
            )

        val move =
            PointerInputEvent(
                7,
                Uptime.Boot + 10.milliseconds,
                PxPosition(300.px, 300.px),
                true
            )

        val expectedDown =
            PointerInputChange(
                id = PointerId(7),
                current = PointerInputData(
                    Uptime.Boot + 5.milliseconds,
                    PxPosition(200.px, 200.px),
                    true
                ),
                previous = PointerInputData(null, null, false),
                consumed = ConsumedData()
            )

        val expectedMove =
            PointerInputChange(
                id = PointerId(7),
                current = PointerInputData(
                    Uptime.Boot + 10.milliseconds,
                    PxPosition(300.px, 300.px),
                    true
                ),
                previous = PointerInputData(
                    Uptime.Boot + 5.milliseconds,
                    PxPosition(200.px, 200.px),
                    true
                ),
                consumed = ConsumedData()
            )

        // Act

        pointerInputEventProcessor.process(down)
        pointerInputEventProcessor.process(move)
        pointerInputEventProcessor.processCancel()

        // Assert

        inOrder(pointerInputHandler, cancelHandler) {
            for (pass in PointerEventPass.values()) {
                verify(pointerInputHandler).invoke(
                    eq(listOf(expectedDown)),
                    eq(pass),
                    any()
                )
            }
            for (pass in PointerEventPass.values()) {
                verify(pointerInputHandler).invoke(
                    eq(listOf(expectedMove)),
                    eq(pass),
                    any()
                )
            }
            verify(cancelHandler).invoke()
        }
        verifyNoMoreInteractions(
            pointerInputHandler,
            cancelHandler
        )
    }

    @Test
    fun processCancel_downCancelMoveUp_pimOnlyReceivesCorrectDownCancel() {

        // Arrange

        val pointerInputHandler: PointerInputHandler = spy(StubPointerInputHandler())
        val cancelHandler: () -> Unit = spy()
        val layoutNode = LayoutNode(
            0, 0, 500, 500,
            PointerInputModifier(
                TestPointerInputFilter(
                    pointerInputHandler = pointerInputHandler,
                    cancelHandler = cancelHandler
                )
            )
        )

        root.insertAt(0, layoutNode)

        val down =
            PointerInputEvent(
                7,
                Uptime.Boot + 5.milliseconds,
                PxPosition(200.px, 200.px),
                true
            )

        val expectedDown =
            PointerInputChange(
                id = PointerId(7),
                current = PointerInputData(
                    Uptime.Boot + 5.milliseconds,
                    PxPosition(200.px, 200.px),
                    true
                ),
                previous = PointerInputData(null, null, false),
                consumed = ConsumedData()
            )

        // Act

        pointerInputEventProcessor.process(down)
        pointerInputEventProcessor.processCancel()

        // Assert

        inOrder(pointerInputHandler, cancelHandler) {
            for (pass in PointerEventPass.values()) {
                verify(pointerInputHandler).invoke(
                    eq(listOf(expectedDown)),
                    eq(pass),
                    any()
                )
            }
            verify(cancelHandler).invoke()
        }
        verifyNoMoreInteractions(
            pointerInputHandler,
            cancelHandler
        )
    }

    @Test
    fun processCancel_downCancelDown_pimOnlyReceivesCorrectDownCancelDown() {

        // Arrange

        val pointerInputHandler: PointerInputHandler = spy(StubPointerInputHandler())
        val cancelHandler: () -> Unit = spy()
        val layoutNode = LayoutNode(
            0, 0, 500, 500,
            PointerInputModifier(
                TestPointerInputFilter(
                    pointerInputHandler = pointerInputHandler,
                    cancelHandler = cancelHandler
                )
            )
        )

        root.insertAt(0, layoutNode)

        val down1 =
            PointerInputEvent(
                7,
                Uptime.Boot + 5.milliseconds,
                PxPosition(200.px, 200.px),
                true
            )

        val down2 =
            PointerInputEvent(
                7,
                Uptime.Boot + 10.milliseconds,
                PxPosition(200.px, 200.px),
                true
            )

        val expectedDown1 =
            PointerInputChange(
                id = PointerId(7),
                current = PointerInputData(
                    Uptime.Boot + 5.milliseconds,
                    PxPosition(200.px, 200.px),
                    true
                ),
                previous = PointerInputData(null, null, false),
                consumed = ConsumedData()
            )

        val expectedDown2 =
            PointerInputChange(
                id = PointerId(7),
                current = PointerInputData(
                    Uptime.Boot + 10.milliseconds,
                    PxPosition(200.px, 200.px),
                    true
                ),
                previous = PointerInputData(null, null, false),
                consumed = ConsumedData()
            )

        // Act

        pointerInputEventProcessor.process(down1)
        pointerInputEventProcessor.processCancel()
        pointerInputEventProcessor.process(down2)

        // Assert

        inOrder(pointerInputHandler, cancelHandler) {
            for (pass in PointerEventPass.values()) {
                verify(pointerInputHandler).invoke(
                    eq(listOf(expectedDown1)),
                    eq(pass),
                    any()
                )
            }
            verify(cancelHandler).invoke()
            for (pass in PointerEventPass.values()) {
                verify(pointerInputHandler).invoke(
                    eq(listOf(expectedDown2)),
                    eq(pass),
                    any()
                )
            }
        }
        verifyNoMoreInteractions(
            pointerInputHandler,
            cancelHandler
        )
    }

    @Test
    fun process_layoutNodeRemovedDuringInput_correctPointerInputChangesReceived() {

        // Arrange

        val childPointerInputHandler: PointerInputHandler = spy(StubPointerInputHandler())
        val childLayoutNode = LayoutNode(
            0, 0, 100, 100,
            PointerInputModifier(
                TestPointerInputFilter(
                    pointerInputHandler = childPointerInputHandler
                )
            )
        )

        val parentPointerInputHandler: PointerInputHandler = spy(StubPointerInputHandler())
        val parentLayoutNode: LayoutNode = LayoutNode(
            0, 0, 100, 100,
            PointerInputModifier(
                TestPointerInputFilter(
                    pointerInputHandler = parentPointerInputHandler
                )
            )
        ).apply {
            insertAt(0, childLayoutNode)
        }

        root.insertAt(0, parentLayoutNode)

        val offset = PxPosition(50.px, 50.px)

        val down = PointerInputEvent(0, Uptime.Boot + 7.milliseconds, offset, true)
        val up = PointerInputEvent(0, Uptime.Boot + 11.milliseconds, null, false)

        val expectedDownChange = PointerInputChange(
            id = PointerId(0),
            current = PointerInputData(Uptime.Boot + 7.milliseconds, offset, true),
            previous = PointerInputData(null, null, false),
            consumed = ConsumedData()
        )

        val expectedUpChange = PointerInputChange(
            id = PointerId(0),
            current = PointerInputData(Uptime.Boot + 11.milliseconds, null, false),
            previous = PointerInputData(Uptime.Boot + 7.milliseconds, offset, true),
            consumed = ConsumedData()
        )

        // Act

        pointerInputEventProcessor.process(down)
        parentLayoutNode.removeAt(0, 1)
        pointerInputEventProcessor.process(up)

        // Assert

        PointerEventPass.values().forEach {
            verify(parentPointerInputHandler)
                .invoke(eq(listOf(expectedDownChange)), eq(it), any())
            verify(childPointerInputHandler)
                .invoke(eq(listOf(expectedDownChange)), eq(it), any())
            verify(parentPointerInputHandler)
                .invoke(eq(listOf(expectedUpChange)), eq(it), any())
        }
        verifyNoMoreInteractions(parentPointerInputHandler)
        verifyNoMoreInteractions(childPointerInputHandler)
    }

    @Test
    fun process_layoutNodeRemovedDuringInput_cancelDispatchedToCorrectPointerInputModifier() {

        // Arrange

        val childCancelHandler: () -> Unit = spy()
        val childLayoutNode = LayoutNode(
            0, 0, 100, 100,
            PointerInputModifier(
                TestPointerInputFilter(
                    cancelHandler = childCancelHandler
                )
            )
        )

        val parentCancelHandler: () -> Unit = spy()
        val parentLayoutNode: LayoutNode = LayoutNode(
            0, 0, 100, 100,
            PointerInputModifier(
                TestPointerInputFilter(
                    cancelHandler = parentCancelHandler
                )
            )
        ).apply {
            insertAt(0, childLayoutNode)
        }

        root.insertAt(0, parentLayoutNode)

        val down =
            PointerInputEvent(0, Uptime.Boot + 7.milliseconds, PxPosition(50.px, 50.px), true)

        val up = PointerInputEvent(0, Uptime.Boot + 11.milliseconds, null, false)

        // Act

        pointerInputEventProcessor.process(down)
        parentLayoutNode.removeAt(0, 1)
        pointerInputEventProcessor.process(up)

        // Assert
        verify(childCancelHandler).invoke()
        verify(parentCancelHandler, never()).invoke()
    }

    @Test
    fun process_pointerInputModifierRemovedDuringInput_correctPointerInputChangesReceived() {

        // Arrange

        val childPointerInputHandler: PointerInputHandler = spy(StubPointerInputHandler())
        val childLayoutNode = LayoutNode(
            0, 0, 100, 100,
            PointerInputModifier(
                TestPointerInputFilter(
                    pointerInputHandler = childPointerInputHandler
                )
            )
        )

        val parentPointerInputHandler: PointerInputHandler = spy(StubPointerInputHandler())
        val parentLayoutNode: LayoutNode = LayoutNode(
            0, 0, 100, 100,
            PointerInputModifier(
                TestPointerInputFilter(
                    pointerInputHandler = parentPointerInputHandler
                )
            )
        ).apply {
            insertAt(0, childLayoutNode)
        }

        root.insertAt(0, parentLayoutNode)

        val offset = PxPosition(50.px, 50.px)

        val down = PointerInputEvent(0, Uptime.Boot + 7.milliseconds, offset, true)
        val up = PointerInputEvent(0, Uptime.Boot + 11.milliseconds, null, false)

        val expectedDownChange = PointerInputChange(
            id = PointerId(0),
            current = PointerInputData(Uptime.Boot + 7.milliseconds, offset, true),
            previous = PointerInputData(null, null, false),
            consumed = ConsumedData()
        )

        val expectedUpChange = PointerInputChange(
            id = PointerId(0),
            current = PointerInputData(Uptime.Boot + 11.milliseconds, null, false),
            previous = PointerInputData(Uptime.Boot + 7.milliseconds, offset, true),
            consumed = ConsumedData()
        )

        // Act

        pointerInputEventProcessor.process(down)
        childLayoutNode.modifier = Modifier.None
        pointerInputEventProcessor.process(up)

        // Assert

        PointerEventPass.values().forEach {
            verify(parentPointerInputHandler)
                .invoke(eq(listOf(expectedDownChange)), eq(it), any())
            verify(childPointerInputHandler)
                .invoke(eq(listOf(expectedDownChange)), eq(it), any())
            verify(parentPointerInputHandler)
                .invoke(eq(listOf(expectedUpChange)), eq(it), any())
        }
        verifyNoMoreInteractions(parentPointerInputHandler)
        verifyNoMoreInteractions(childPointerInputHandler)
    }

    @Test
    fun process_pointerInputModifierRemovedDuringInput_cancelDispatchedToCorrectPim() {

        // Arrange

        val childCancelHandler: () -> Unit = spy()
        val childLayoutNode = LayoutNode(
            0, 0, 100, 100,
            PointerInputModifier(
                TestPointerInputFilter(
                    cancelHandler = childCancelHandler
                )
            )
        )

        val parentCancelHandler: () -> Unit = spy()
        val parentLayoutNode: LayoutNode = LayoutNode(
            0, 0, 100, 100,
            PointerInputModifier(
                TestPointerInputFilter(
                    cancelHandler = parentCancelHandler
                )
            )
        ).apply {
            insertAt(0, childLayoutNode)
        }

        root.insertAt(0, parentLayoutNode)

        val down =
            PointerInputEvent(0, Uptime.Boot + 7.milliseconds, PxPosition(50.px, 50.px), true)

        val up = PointerInputEvent(0, Uptime.Boot + 11.milliseconds, null, false)

        // Act

        pointerInputEventProcessor.process(down)
        childLayoutNode.modifier = Modifier.None
        pointerInputEventProcessor.process(up)

        // Assert
        verify(childCancelHandler).invoke()
        verify(parentCancelHandler, never()).invoke()
    }
}

abstract class TestOwner : Owner {
    var position = IntPxPosition.Origin
    override fun calculatePosition() = position
}

class TestPointerInputFilter(
    override val pointerInputHandler: PointerInputHandler = { changes, _, _ -> changes },
    override val cancelHandler: () -> Unit = {}
) : PointerInputFilter()