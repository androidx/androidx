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

package androidx.ui.core.pointerinput

import androidx.test.filters.SmallTest
import androidx.ui.core.ConsumedData
import androidx.ui.core.DrawNode
import androidx.ui.core.LayoutNode
import androidx.ui.core.Owner
import androidx.ui.core.PointerEventPass
import androidx.ui.core.PointerEventPass.InitialDown
import androidx.ui.core.PointerEventPass.PreUp
import androidx.ui.core.PointerId
import androidx.ui.core.PointerInputChange
import androidx.ui.core.PointerInputData
import androidx.ui.core.PointerInputNode
import androidx.ui.core.SemanticsComponentNode
import androidx.ui.unit.IntPxPosition
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition
import androidx.ui.unit.Uptime
import androidx.ui.unit.ipx
import androidx.ui.unit.milliseconds
import androidx.ui.unit.px
import androidx.ui.core.semantics.SemanticsConfiguration
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.mock

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
class PointerInputEventProcessorTest {

    private lateinit var root: LayoutNode
    private lateinit var pointerInputEventProcessor: PointerInputEventProcessor
    private val mockOwner = mock(Owner::class.java)
    private lateinit var mTrackerList:
            MutableList<Triple<PointerInputNode, PointerEventPass, PointerInputChange>>

    @Before
    fun setup() {
        root = LayoutNode(0, 0, 500, 500)
        root.attach(mockOwner)
        pointerInputEventProcessor = PointerInputEventProcessor(root)
        mTrackerList = mutableListOf()
    }

    @Test
    fun process_downMoveUp_convertedCorrectlyAndTraversesAllPassesInCorrectOrder() {

        // Arrange

        val pointerInputNode: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, LayoutNode(0, 0, 500, 500))
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        root.emitInsertAt(0, pointerInputNode)

        val offset = PxPosition(100.px, 200.px)
        val offset2 = PxPosition(300.px, 400.px)

        val events = arrayOf(
            PointerInputEvent(8712, Uptime.Boot + 3.milliseconds, offset, true),
            PointerInputEvent(8712, Uptime.Boot + 11.milliseconds, offset2, true),
            PointerInputEvent(8712, Uptime.Boot + 13.milliseconds, offset2, false)
        )

        val expectedChanges = arrayOf(
            PointerInputChange(
                id = PointerId(8712, Uptime.Boot),
                current = PointerInputData(Uptime.Boot + 3.milliseconds, offset, true),
                previous = PointerInputData(null, null, false),
                consumed = ConsumedData()
            ),
            PointerInputChange(
                id = PointerId(8712, Uptime.Boot),
                current = PointerInputData(Uptime.Boot + 11.milliseconds, offset2, true),
                previous = PointerInputData(Uptime.Boot + 3.milliseconds, offset, true),
                consumed = ConsumedData()
            ),
            PointerInputChange(
                id = PointerId(8712, Uptime.Boot),
                current = PointerInputData(Uptime.Boot + 13.milliseconds, offset2, false),
                previous = PointerInputData(Uptime.Boot + 11.milliseconds, offset2, true),
                consumed = ConsumedData()
            )
        )

        // Act

        events.forEach { pointerInputEventProcessor.process(it, IntPxPosition.Origin) }

        // Assert

        inOrder(pointerInputNode.pointerInputHandler) {
            for (expected in expectedChanges) {
                for (pass in PointerEventPass.values()) {
                    verify(pointerInputNode.pointerInputHandler).invoke(
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
        val pointerInputNode: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, LayoutNode(100, 200, 301, 401))
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        root.emitInsertAt(0, pointerInputNode)

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
                id = PointerId(index, Uptime.Boot),
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
            pointerInputEventProcessor.process(it, IntPxPosition.Origin)
        }

        // Assert

        verify(pointerInputNode.pointerInputHandler, times(4)).invoke(
            any(),
            eq(InitialDown),
            any()
        )
        for (expected in expectedChanges) {
            verify(pointerInputNode.pointerInputHandler).invoke(
                eq(listOf(expected)),
                eq(InitialDown),
                any()
            )
        }
    }

    @Test
    fun process_downMisses_targetDoesNotReceive() {

        // Arrange

        val pointerInputNode: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, LayoutNode(100, 200, 301, 401))
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        root.emitInsertAt(0, pointerInputNode)

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
            pointerInputEventProcessor.process(it, IntPxPosition.Origin)
        }

        // Assert

        verify(pointerInputNode.pointerInputHandler, never()).invoke(any(), any(), any())
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

        val childLayoutNode = LayoutNode(100, 100, 200, 200)
        val childPointerInputNode: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, childLayoutNode)
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        val middleLayoutNode: LayoutNode = LayoutNode(100, 100, 400, 400).apply {
            emitInsertAt(0, childPointerInputNode)
        }
        val middlePointerInputNode: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, middleLayoutNode)
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        val parentLayoutNode: LayoutNode = LayoutNode(0, 0, 500, 500).apply {
            emitInsertAt(0, middlePointerInputNode)
        }
        val parentPointerInputNode: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, parentLayoutNode)
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        root.emitInsertAt(0, parentPointerInputNode)

        val offset = when (numberOfChildrenHit) {
            3 -> PxPosition(250.px, 250.px)
            2 -> PxPosition(150.px, 150.px)
            1 -> PxPosition(50.px, 50.px)
            else -> throw IllegalStateException()
        }

        val event = PointerInputEvent(0, Uptime.Boot + 5.milliseconds, offset, true)

        // Act

        pointerInputEventProcessor.process(event, IntPxPosition.Origin)

        // Assert

        when (numberOfChildrenHit) {
            3 -> {
                verify(parentPointerInputNode.pointerInputHandler).invoke(
                    any(),
                    eq(InitialDown),
                    any()
                )
                verify(middlePointerInputNode.pointerInputHandler).invoke(
                    any(),
                    eq(InitialDown),
                    any()
                )
                verify(childPointerInputNode.pointerInputHandler).invoke(
                    any(),
                    eq(InitialDown),
                    any()
                )
            }
            2 -> {
                verify(parentPointerInputNode.pointerInputHandler).invoke(
                    any(),
                    eq(InitialDown),
                    any()
                )
                verify(middlePointerInputNode.pointerInputHandler).invoke(
                    any(),
                    eq(InitialDown),
                    any()
                )
                verify(childPointerInputNode.pointerInputHandler, never()).invoke(
                    any(),
                    any(),
                    any()
                )
            }
            1 -> {
                verify(parentPointerInputNode.pointerInputHandler).invoke(
                    any(),
                    eq(InitialDown),
                    any()
                )
                verify(middlePointerInputNode.pointerInputHandler, never()).invoke(
                    any(),
                    any(),
                    any()
                )
                verify(childPointerInputNode.pointerInputHandler, never()).invoke(
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
            id = PointerId(0, Uptime.Boot),
            current = PointerInputData(
                Uptime.Boot + 5.milliseconds,
                PxPosition(100.px, 0.px),
                true
            ),
            previous = PointerInputData(Uptime.Boot + 3.milliseconds, PxPosition(0.px, 0.px), true),
            consumed = ConsumedData(positionChange = PxPosition(0.px, 0.px))
        )
        val output = PointerInputChange(
            id = PointerId(0, Uptime.Boot),
            current = PointerInputData(
                Uptime.Boot + 5.milliseconds,
                PxPosition(100.px, 0.px),
                true
            ),
            previous = PointerInputData(Uptime.Boot + 3.milliseconds, PxPosition(0.px, 0.px), true),
            consumed = ConsumedData(positionChange = PxPosition(13.px, 0.px))
        )

        val pointerInputNode: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, LayoutNode(0, 0, 500, 500))
            pointerInputHandler = spy(MyPointerInputHandler())
            whenever(
                pointerInputHandler.invoke(
                    listOf(input),
                    InitialDown,
                    IntPxSize(500.ipx, 500.ipx)
                )
            )
                .thenReturn(
                    listOf(
                        output
                    )
                )
        }
        root.emitInsertAt(0, pointerInputNode)

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

        pointerInputEventProcessor.process(down, IntPxPosition.Origin)
        pointerInputEventProcessor.process(move, IntPxPosition.Origin)

        // Assert

        verify(pointerInputNode.pointerInputHandler)
            .invoke(eq(listOf(input)), eq(InitialDown), any())
        verify(pointerInputNode.pointerInputHandler)
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

        val childOffset = PxPosition(cX1.px, cY1.px)
        val childLayoutNode = LayoutNode(cX1, cY1, cX2, cY2)
        val childPointerInputNode: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, childLayoutNode)
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        val middleOffset = PxPosition(mX1.px, mY1.px)
        val middleLayoutNode: LayoutNode = LayoutNode(mX1, mY1, mX2, mY2).apply {
            emitInsertAt(0, childPointerInputNode)
        }
        val middlePointerInputNode: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, middleLayoutNode)
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        val parentLayoutNode: LayoutNode = LayoutNode(pX1, pY1, pX2, pY2).apply {
            emitInsertAt(0, middlePointerInputNode)
        }
        val parentPointerInputNode: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, parentLayoutNode)
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        root.emitInsertAt(0, parentPointerInputNode)

        val additionalOffset = IntPxPosition(aOX.ipx, aOY.ipx)

        val offset = PxPosition(pointerX.px, pointerY.px)

        val down = PointerInputEvent(0, Uptime.Boot + 7.milliseconds, offset, true)

        val pointerInputNodes = arrayOf(
            parentPointerInputNode,
            middlePointerInputNode,
            childPointerInputNode
        )

        val expectedPointerInputChanges = arrayOf(
            PointerInputChange(
                id = PointerId(0, Uptime.Boot),
                current = PointerInputData(
                    Uptime.Boot + 7.milliseconds,
                    offset - additionalOffset,
                    true
                ),
                previous = PointerInputData(null, null, false),
                consumed = ConsumedData()
            ),
            PointerInputChange(
                id = PointerId(0, Uptime.Boot),
                current = PointerInputData(
                    Uptime.Boot + 7.milliseconds,
                    offset - middleOffset - additionalOffset,
                    true
                ),
                previous = PointerInputData(null, null, false),
                consumed = ConsumedData()
            ),
            PointerInputChange(
                id = PointerId(0, Uptime.Boot),
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

        pointerInputEventProcessor.process(down, additionalOffset)

        // Assert

        for (pass in PointerEventPass.values()) {
            for (i in pointerInputNodes.indices) {
                verify(pointerInputNodes[i].pointerInputHandler).invoke(
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

        val childPointerInputNode1: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, LayoutNode(0, 0, 50, 50))
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        val childPointerInputNode2: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, LayoutNode(50, 50, 100, 100))
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        root.apply {
            emitInsertAt(0, childPointerInputNode1)
            emitInsertAt(0, childPointerInputNode2)
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
            id = PointerId(0, Uptime.Boot),
            current = PointerInputData(Uptime.Boot + 5.milliseconds, offset1, true),
            previous = PointerInputData(null, null, false),
            consumed = ConsumedData()
        )
        val expectedChange2 = PointerInputChange(
            id = PointerId(1, Uptime.Boot),
            current = PointerInputData(
                Uptime.Boot + 5.milliseconds,
                offset2 - PxPosition(50.px, 50.px),
                true
            ),
            previous = PointerInputData(null, null, false),
            consumed = ConsumedData()
        )

        // Act

        pointerInputEventProcessor.process(down, IntPxPosition.Origin)

        // Assert

        for (pointerEventPass in PointerEventPass.values()) {
            verify(childPointerInputNode1.pointerInputHandler)
                .invoke(
                    listOf(expectedChange1),
                    pointerEventPass,
                    IntPxSize(50.ipx, 50.ipx)
                )
            verify(childPointerInputNode2.pointerInputHandler)
                .invoke(
                    listOf(expectedChange2),
                    pointerEventPass,
                    IntPxSize(50.ipx, 50.ipx)
                )
        }
        verifyNoMoreInteractions(
            childPointerInputNode1.pointerInputHandler,
            childPointerInputNode2.pointerInputHandler
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
        val childPointerInputNode1: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, LayoutNode(0, 0, 100, 100))
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        val childPointerInputNode2: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, LayoutNode(50, 50, 150, 150))
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        val childPointerInputNode3: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, LayoutNode(100, 100, 200, 200))
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        root.apply {
            emitInsertAt(0, childPointerInputNode1)
            emitInsertAt(1, childPointerInputNode2)
            emitInsertAt(2, childPointerInputNode3)
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
            id = PointerId(0, Uptime.Boot),
            current = PointerInputData(Uptime.Boot + 5.milliseconds, offset1, true),
            previous = PointerInputData(null, null, false),
            consumed = ConsumedData()
        )
        val expectedChange2 = PointerInputChange(
            id = PointerId(1, Uptime.Boot),
            current = PointerInputData(
                Uptime.Boot + 5.milliseconds,
                offset2 - PxPosition(50.px, 50.px),
                true
            ),
            previous = PointerInputData(null, null, false),
            consumed = ConsumedData()
        )
        val expectedChange3 = PointerInputChange(
            id = PointerId(2, Uptime.Boot),
            current = PointerInputData(
                Uptime.Boot + 5.milliseconds,
                offset3 - PxPosition(100.px, 100.px),
                true
            ),
            previous = PointerInputData(null, null, false),
            consumed = ConsumedData()
        )

        // Act

        pointerInputEventProcessor.process(down, IntPxPosition.Origin)

        // Assert

        for (pointerEventPass in PointerEventPass.values()) {
            verify(childPointerInputNode1.pointerInputHandler)
                .invoke(listOf(expectedChange1), pointerEventPass, IntPxSize(100.ipx, 100.ipx))
            verify(childPointerInputNode2.pointerInputHandler)
                .invoke(listOf(expectedChange2), pointerEventPass, IntPxSize(100.ipx, 100.ipx))
            verify(childPointerInputNode3.pointerInputHandler)
                .invoke(listOf(expectedChange3), pointerEventPass, IntPxSize(100.ipx, 100.ipx))
        }
        verifyNoMoreInteractions(
            childPointerInputNode1.pointerInputHandler,
            childPointerInputNode2.pointerInputHandler,
            childPointerInputNode3.pointerInputHandler
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
        val childPointerInputNode1: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, LayoutNode(0, 0, 100, 150))
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        val childPointerInputNode2: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, LayoutNode(25, 50, 75, 100))
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        root.apply {
            emitInsertAt(0, childPointerInputNode1)
            emitInsertAt(1, childPointerInputNode2)
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
            id = PointerId(0, Uptime.Boot),
            current = PointerInputData(Uptime.Boot + 7.milliseconds, offset1, true),
            previous = PointerInputData(null, null, false),
            consumed = ConsumedData()
        )
        val expectedChange2 = PointerInputChange(
            id = PointerId(1, Uptime.Boot),
            current = PointerInputData(
                Uptime.Boot + 7.milliseconds,
                offset2 - PxPosition(25.px, 50.px),
                true
            ),
            previous = PointerInputData(null, null, false),
            consumed = ConsumedData()
        )
        val expectedChange3 = PointerInputChange(
            id = PointerId(2, Uptime.Boot),
            current = PointerInputData(Uptime.Boot + 7.milliseconds, offset3, true),
            previous = PointerInputData(null, null, false),
            consumed = ConsumedData()
        )

        // Act

        pointerInputEventProcessor.process(down, IntPxPosition.Origin)

        // Assert

        for (pointerEventPass in PointerEventPass.values()) {
            verify(childPointerInputNode1.pointerInputHandler)
                .invoke(
                    listOf(expectedChange1, expectedChange3),
                    pointerEventPass,
                    IntPxSize(100.ipx, 150.ipx)
                )
            verify(childPointerInputNode2.pointerInputHandler)
                .invoke(
                    listOf(expectedChange2),
                    pointerEventPass,
                    IntPxSize(50.ipx, 50.ipx)
                )
        }
        verifyNoMoreInteractions(
            childPointerInputNode1.pointerInputHandler,
            childPointerInputNode2.pointerInputHandler
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
        val childPointerInputNode1: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, LayoutNode(0, 0, 150, 100))
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        val childPointerInputNode2: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, LayoutNode(50, 25, 100, 75))
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        root.apply {
            emitInsertAt(0, childPointerInputNode1)
            emitInsertAt(1, childPointerInputNode2)
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
            id = PointerId(0, Uptime.Boot),
            current = PointerInputData(Uptime.Boot + 11.milliseconds, offset1, true),
            previous = PointerInputData(null, null, false),
            consumed = ConsumedData()
        )
        val expectedChange2 = PointerInputChange(
            id = PointerId(1, Uptime.Boot),
            current = PointerInputData(
                Uptime.Boot + 11.milliseconds,
                offset2 - PxPosition(50.px, 25.px),
                true
            ),
            previous = PointerInputData(null, null, false),
            consumed = ConsumedData()
        )
        val expectedChange3 = PointerInputChange(
            id = PointerId(2, Uptime.Boot),
            current = PointerInputData(Uptime.Boot + 11.milliseconds, offset3, true),
            previous = PointerInputData(null, null, false),
            consumed = ConsumedData()
        )

        // Act

        pointerInputEventProcessor.process(down, IntPxPosition.Origin)

        // Assert

        for (pointerEventPass in PointerEventPass.values()) {
            verify(childPointerInputNode1.pointerInputHandler)
                .invoke(
                    listOf(expectedChange1, expectedChange3),
                    pointerEventPass,
                    IntPxSize(150.ipx, 100.ipx)
                )
            verify(childPointerInputNode2.pointerInputHandler)
                .invoke(
                    listOf(expectedChange2),
                    pointerEventPass,
                    IntPxSize(50.ipx, 50.ipx)
                )
        }
        verifyNoMoreInteractions(
            childPointerInputNode1.pointerInputHandler,
            childPointerInputNode2.pointerInputHandler
        )
    }

    /**
     * This test creates a layout of this shape:
     *
     *    t            t
     *   |---|
     *  t|t  |  t      t t
     *   |   |
     *   |---|
     *
     *    t     t      t
     *
     *              |---|
     *              |   |
     *  t t     t   |  t|t
     *              |---|
     *    t            t
     *
     * One PointerInputNode with 2 child LayoutNodes that are far apart.  Touches happen both
     * inside the bounding box that wraps around the LayoutNodes, and just outside of it.  Those
     * that happen inside all hit, those that happen outside do not.
     */
    @Test
    fun process_ManyPointersOnPinWith2LnsThatAreTopLeftBottomRight_onlyCorrectPointersHit() {

        // Arrange

        val pointerInputNode: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, LayoutNode(100, 100, 200, 200))
            emitInsertAt(1, LayoutNode(300, 300, 400, 400))
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        root.apply {
            emitInsertAt(0, pointerInputNode)
        }
        val offsetsThatHit =
            listOf(
                PxPosition(100.px, 100.px),
                PxPosition(250.px, 100.px),
                PxPosition(399.px, 100.px),
                PxPosition(100.px, 250.px),
                PxPosition(250.px, 250.px),
                PxPosition(399.px, 250.px),
                PxPosition(100.px, 399.px),
                PxPosition(250.px, 399.px),
                PxPosition(399.px, 399.px)
            )
        val offsetsThatMiss =
            listOf(
                PxPosition(100.px, 99.px),
                PxPosition(399.px, 99.px),
                PxPosition(99.px, 100.px),
                PxPosition(400.px, 100.px),
                PxPosition(99.px, 399.px),
                PxPosition(400.px, 399.px),
                PxPosition(100.px, 400.px),
                PxPosition(399.px, 400.px)
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

        pointerInputEventProcessor.process(pointerInputEvent, IntPxPosition.Origin)

        // Assert

        val expectedChanges =
            (offsetsThatHit.indices).map {
                PointerInputChange(
                    id = PointerId(it, Uptime.Boot),
                    current = PointerInputData(
                        Uptime.Boot + 11.milliseconds,
                        offsetsThatHit[it] - PxPosition(100.px, 100.px),
                        true
                    ),
                    previous = PointerInputData(null, null, false),
                    consumed = ConsumedData()
                )
            }
        PointerEventPass.values().forEach { pointerEventPass ->
            verify(pointerInputNode.pointerInputHandler).invoke(
                eq(expectedChanges),
                eq(pointerEventPass),
                any()
            )
        }
        verifyNoMoreInteractions(
            pointerInputNode.pointerInputHandler
        )
    }

    /**
     * This test creates a layout of this shape:
     *
     *    t            t
     *              |---|
     *  t t     t   |  t|t
     *              |   |
     *              |---|
     *
     *    t     t      t
     *
     *   |---|
     *   |   |
     *  t|t  |  t      t t
     *   |---|
     *    t            t
     *
     * One PointerInputNode with 2 child LayoutNodes that are far apart.  Touches happen both
     * inside the bounding box that wraps around the LayoutNodes, and just outside of it.  Those
     * that happen inside all hit, those that happen outside do not.
     */
    @Test
    fun process_ManyPointersOnPinWith2LnsThatAreTopRightBottomLeft_onlyCorrectPointersHit() {

        // Arrange

        val pointerInputNode: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, LayoutNode(300, 100, 400, 200))
            emitInsertAt(1, LayoutNode(100, 300, 200, 400))
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        root.apply {
            emitInsertAt(0, pointerInputNode)
        }
        val offsetsThatHit =
            listOf(
                PxPosition(100.px, 100.px),
                PxPosition(250.px, 100.px),
                PxPosition(399.px, 100.px),
                PxPosition(100.px, 250.px),
                PxPosition(250.px, 250.px),
                PxPosition(399.px, 250.px),
                PxPosition(100.px, 399.px),
                PxPosition(250.px, 399.px),
                PxPosition(399.px, 399.px)
            )
        val offsetsThatMiss =
            listOf(
                PxPosition(100.px, 99.px),
                PxPosition(399.px, 99.px),
                PxPosition(99.px, 100.px),
                PxPosition(400.px, 100.px),
                PxPosition(99.px, 399.px),
                PxPosition(400.px, 399.px),
                PxPosition(100.px, 400.px),
                PxPosition(399.px, 400.px)
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

        pointerInputEventProcessor.process(pointerInputEvent, IntPxPosition.Origin)

        // Assert

        val expectedChanges =
            (offsetsThatHit.indices).map {
                PointerInputChange(
                    id = PointerId(it, Uptime.Boot),
                    current = PointerInputData(
                        Uptime.Boot + 11.milliseconds,
                        offsetsThatHit[it] - PxPosition(100.px, 100.px),
                        true
                    ),
                    previous = PointerInputData(null, null, false),
                    consumed = ConsumedData()
                )
            }
        PointerEventPass.values().forEach { pointerEventPass ->
            verify(pointerInputNode.pointerInputHandler).invoke(
                eq(expectedChanges),
                eq(pointerEventPass),
                any()
            )
        }
        verifyNoMoreInteractions(
            pointerInputNode.pointerInputHandler
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
     * 4 PointerInputNodes around 4 LayoutNodes that are clipped by their parent LayoutNode. 4
     * touches touch just inside the parent LayoutNode and inside the child LayoutNodes. 8
     * touches touch just outside the parent LayoutNode but inside the child LayoutNodes.
     *
     * Because LayoutNodes clip the bounds where children LayoutNodes can be hit, all 8 should miss,
     * but the other 4 touches are inside both, so hit.
     */
    @Test
    fun process_4DownInClippedAreaOfLnsWrappedByPins_onlyCorrectPointersHit() {

        // Arrange

        val singlePointerInputHandler = spy(MyPointerInputHandler())
        val pointerInputNode1 = PointerInputNode().apply {
            emitInsertAt(0, LayoutNode(-1, -1, 1, 1))
            pointerInputHandler = singlePointerInputHandler
        }
        val pointerInputNode2 = PointerInputNode().apply {
            emitInsertAt(0, LayoutNode(2, -1, 4, 1))
            pointerInputHandler = singlePointerInputHandler
        }
        val pointerInputNode3 = PointerInputNode().apply {
            emitInsertAt(0, LayoutNode(-1, 2, 1, 4))
            pointerInputHandler = singlePointerInputHandler
        }
        val pointerInputNode4 = PointerInputNode().apply {
            emitInsertAt(0, LayoutNode(2, 2, 4, 4))
            pointerInputHandler = singlePointerInputHandler
        }
        val parentLayoutNode = LayoutNode(1, 1, 4, 4).apply {
            emitInsertAt(0, pointerInputNode1)
            emitInsertAt(1, pointerInputNode2)
            emitInsertAt(2, pointerInputNode3)
            emitInsertAt(3, pointerInputNode4)
        }
        root.apply {
            emitInsertAt(0, parentLayoutNode)
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

        pointerInputEventProcessor.process(pointerInputEvent, IntPxPosition.Origin)

        // Assert

        val expectedChanges =
            (offsetsThatHit.indices).map {
                PointerInputChange(
                    id = PointerId(it, Uptime.Boot),
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
     *                .....
     *                . B .
     *                .....
     *         t   t
     *        |-----|
     *       t|t   t|t
     *        |  A  |
     *       t|t   t|t
     *        |-----|
     *         t   t
     *  .....
     *  . C .
     *  .....
     *
     * Here we have a LayoutNode (A) that is the parent of a PointerInputNode that is then a parent
     * of LayoutNodes B and C.  4 touches are performed in the corners of (A).
     *
     * Even though B and C are themselves are not touchable because they are laid out outside of the
     * bounds of their parent LayoutNode, the PointerInputNode that wraps them is sized to include
     * the space underneath A, so all 4 touches should hit.
     */
    @Test
    fun process_lnWithPinWith2LnsOutsideOfLayoutBoundsPointerInsidePin_pointersHit() {

        // Arrange

        val pointerInputNode: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, LayoutNode(350, -50, 400, 0))
            emitInsertAt(1, LayoutNode(-50, 350, 0, 400))
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        val layoutNode: LayoutNode = LayoutNode(100, 100, 400, 400).apply {
            emitInsertAt(0, pointerInputNode)
        }
        root.apply {
            emitInsertAt(0, layoutNode)
        }
        val offsetsThatHit =
            listOf(
                PxPosition(100.px, 100.px),
                PxPosition(399.px, 100.px),
                PxPosition(100.px, 399.px),
                PxPosition(399.px, 399.px)
            )
        val offsetsThatMiss =
            listOf(
                PxPosition(100.px, 99.px),
                PxPosition(399.px, 99.px),
                PxPosition(99.px, 100.px),
                PxPosition(400.px, 100.px),
                PxPosition(99.px, 399.px),
                PxPosition(400.px, 399.px),
                PxPosition(100.px, 400.px),
                PxPosition(399.px, 400.px)
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

        pointerInputEventProcessor.process(pointerInputEvent, IntPxPosition.Origin)

        // Assert

        val expectedChanges =
            (offsetsThatHit.indices).map {
                PointerInputChange(
                    id = PointerId(it, Uptime.Boot),
                    current = PointerInputData(
                        Uptime.Boot + 11.milliseconds,
                        offsetsThatHit[it] - PxPosition(50.px, 50.px),
                        true
                    ),
                    previous = PointerInputData(null, null, false),
                    consumed = ConsumedData()
                )
            }
        PointerEventPass.values().forEach { pointerEventPass ->
            verify(pointerInputNode.pointerInputHandler).invoke(
                eq(expectedChanges),
                eq(pointerEventPass),
                any()
            )
        }
        verifyNoMoreInteractions(
            pointerInputNode.pointerInputHandler
        )
    }

    /**
     * This test creates a layout of this shape:
     *
     *    t            t
     *                  *
     *  t t     t      t t
     *
     *
     *
     *    t     t      t
     *
     *
     *
     *  t t     t      t t
     *   *
     *    t            t
     *
     * One PointerInputNode with 2 child LayoutNodes that have no size (represented by *).  Touches
     * happen both inside the bounding box that wraps around the LayoutNodes, and just outside of
     * it.  Those that happen inside all hit, those that happen outside do not.
     */
    @Test
    fun process_manyPointersOnPinWith2LnsWithNoSize_onlyCorrectPointersHit() {

        // Arrange

        val pointerInputNode: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, LayoutNode(400, 100, 400, 100))
            emitInsertAt(1, LayoutNode(100, 400, 100, 400))
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        root.apply {
            emitInsertAt(0, pointerInputNode)
        }
        val offsetsThatHit =
            listOf(
                PxPosition(100.px, 100.px),
                PxPosition(250.px, 100.px),
                PxPosition(399.px, 100.px),
                PxPosition(100.px, 250.px),
                PxPosition(250.px, 250.px),
                PxPosition(399.px, 250.px),
                PxPosition(100.px, 399.px),
                PxPosition(250.px, 399.px),
                PxPosition(399.px, 399.px)
            )
        val offsetsThatMiss =
            listOf(
                PxPosition(100.px, 99.px),
                PxPosition(399.px, 99.px),
                PxPosition(99.px, 100.px),
                PxPosition(400.px, 100.px),
                PxPosition(99.px, 399.px),
                PxPosition(400.px, 399.px),
                PxPosition(100.px, 400.px),
                PxPosition(399.px, 400.px)
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

        pointerInputEventProcessor.process(pointerInputEvent, IntPxPosition.Origin)

        // Assert

        val expectedChanges =
            (offsetsThatHit.indices).map {
                PointerInputChange(
                    id = PointerId(it, Uptime.Boot),
                    current = PointerInputData(
                        Uptime.Boot + 11.milliseconds,
                        offsetsThatHit[it] - PxPosition(100.px, 100.px),
                        true
                    ),
                    previous = PointerInputData(null, null, false),
                    consumed = ConsumedData()
                )
            }
        PointerEventPass.values().forEach { pointerEventPass ->
            verify(pointerInputNode.pointerInputHandler).invoke(
                eq(expectedChanges),
                eq(pointerEventPass),
                any()
            )
        }
        verifyNoMoreInteractions(
            pointerInputNode.pointerInputHandler
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
    fun process_additionalOffsetExists_onlyCorrectPointersHit() {

        // Arrange

        val pointerInputNode: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, LayoutNode(0, 0, 2, 2))
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        root.apply {
            emitInsertAt(0, pointerInputNode)
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

        // Act

        pointerInputEventProcessor.process(pointerInputEvent, IntPxPosition(1.ipx, 1.ipx))

        // Assert

        val expectedChanges =
            (offsetsThatHit.indices).map {
                PointerInputChange(
                    id = PointerId(it, Uptime.Boot),
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
            verify(pointerInputNode.pointerInputHandler).invoke(
                eq(expectedChanges),
                eq(pointerEventPass),
                any()
            )
        }
        verifyNoMoreInteractions(
            pointerInputNode.pointerInputHandler
        )
    }

    @Test
    fun process_downOn3NestedPointerInputNodes_hitAndDispatchInfoAreCorrect() {
        val childPointerInputNode1: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, LayoutNode(25, 50, 75, 100))
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        val childPointerInputNode2: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, childPointerInputNode1)
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        val childPointerInputNode3: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, childPointerInputNode2)
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        root.apply {
            emitInsertAt(0, childPointerInputNode3)
        }

        val offset1 = PxPosition(50.px, 75.px)

        val down = PointerInputEvent(
            Uptime.Boot + 7.milliseconds,
            listOf(
                PointerInputEventData(0, Uptime.Boot + 7.milliseconds, offset1, true)
            )
        )

        val expectedChange = PointerInputChange(
            id = PointerId(0, Uptime.Boot),
            current = PointerInputData(
                Uptime.Boot + 7.milliseconds,
                offset1 - PxPosition(25.px, 50.px),
                true
            ),
            previous = PointerInputData(null, null, false),
            consumed = ConsumedData()
        )

        // Act

        pointerInputEventProcessor.process(down, IntPxPosition.Origin)

        // Assert

        for (pointerEventPass in PointerEventPass.values()) {
            verify(childPointerInputNode1.pointerInputHandler)
                .invoke(
                    listOf(expectedChange),
                    pointerEventPass,
                    IntPxSize(50.ipx, 50.ipx)
                )
            verify(childPointerInputNode2.pointerInputHandler)
                .invoke(
                    listOf(expectedChange),
                    pointerEventPass,
                    IntPxSize(50.ipx, 50.ipx)
                )
            verify(childPointerInputNode3.pointerInputHandler)
                .invoke(
                    listOf(expectedChange),
                    pointerEventPass,
                    IntPxSize(50.ipx, 50.ipx)
                )
        }
        verifyNoMoreInteractions(childPointerInputNode1.pointerInputHandler)
        verifyNoMoreInteractions(childPointerInputNode2.pointerInputHandler)
        verifyNoMoreInteractions(childPointerInputNode3.pointerInputHandler)
    }

    @Test
    fun process_downOnDeeplyNestedPointerInputNode_hitAndDispatchInfoAreCorrect() {
        val layoutNode1 = LayoutNode(1, 5, 500, 500)
        val pointerInputNode: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, layoutNode1)
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        val layoutNode2: LayoutNode = LayoutNode(2, 6, 500, 500).apply {
            emitInsertAt(0, pointerInputNode)
        }
        val layoutNode3: LayoutNode = LayoutNode(3, 7, 500, 500).apply {
            emitInsertAt(0, layoutNode2)
        }
        val layoutNode4: LayoutNode = LayoutNode(4, 8, 500, 500).apply {
            emitInsertAt(0, layoutNode3)
        }
        root.apply {
            emitInsertAt(0, layoutNode4)
        }

        val offset1 = PxPosition(499.px, 499.px)

        val downEvent = PointerInputEvent(
            Uptime.Boot + 7.milliseconds,
            listOf(
                PointerInputEventData(0, Uptime.Boot + 7.milliseconds, offset1, true)
            )
        )

        val expectedChange = PointerInputChange(
            id = PointerId(0, Uptime.Boot),
            current = PointerInputData(
                Uptime.Boot + 7.milliseconds,
                offset1 - PxPosition(1.px + 2.px + 3.px + 4.px, 5.px + 6.px + 7.px + 8.px),
                true
            ),
            previous = PointerInputData(null, null, false),
            consumed = ConsumedData()
        )

        // Act

        pointerInputEventProcessor.process(downEvent, IntPxPosition.Origin)

        // Assert

        for (pointerEventPass in PointerEventPass.values()) {
            verify(pointerInputNode.pointerInputHandler)
                .invoke(
                    listOf(expectedChange),
                    pointerEventPass,
                    IntPxSize(499.ipx, 495.ipx)
                )
        }
        verifyNoMoreInteractions(pointerInputNode.pointerInputHandler)
    }

    @Test
    fun process_downOnComplexPointerAndLayoutNodePath_hitAndDispatchInfoAreCorrect() {
        val layoutNode1 = LayoutNode(1, 6, 500, 500)
        val pointerInputNode1: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, layoutNode1)
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        val pointerInputNode2: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, pointerInputNode1)
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        val layoutNode2: LayoutNode = LayoutNode(2, 7, 500, 500).apply {
            emitInsertAt(0, pointerInputNode2)
        }
        val layoutNode3: LayoutNode = LayoutNode(3, 8, 500, 500).apply {
            emitInsertAt(0, layoutNode2)
        }
        val pointerInputNode3: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, layoutNode3)
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        val pointerInputNode4: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, pointerInputNode3)
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        val layoutNode4: LayoutNode = LayoutNode(4, 9, 500, 500).apply {
            emitInsertAt(0, pointerInputNode4)
        }
        val layoutNode5: LayoutNode = LayoutNode(5, 10, 500, 500).apply {
            emitInsertAt(0, layoutNode4)
        }
        root.apply {
            emitInsertAt(0, layoutNode5)
        }

        val offset1 = PxPosition(499.px, 499.px)

        val downEvent = PointerInputEvent(
            Uptime.Boot + 3.milliseconds,
            listOf(
                PointerInputEventData(0, Uptime.Boot + 3.milliseconds, offset1, true)
            )
        )

        val expectedChange1 = PointerInputChange(
            id = PointerId(0, Uptime.Boot),
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
            id = PointerId(0, Uptime.Boot),
            current = PointerInputData(
                Uptime.Boot + 3.milliseconds,
                offset1 - PxPosition(3.px + 4.px + 5.px, 8.px + 9.px + 10.px),
                true
            ),
            previous = PointerInputData(null, null, false),
            consumed = ConsumedData()
        )

        // Act

        pointerInputEventProcessor.process(downEvent, IntPxPosition.Origin)

        // Assert

        for (pointerEventPass in PointerEventPass.values()) {
            verify(pointerInputNode1.pointerInputHandler)
                .invoke(
                    listOf(expectedChange1),
                    pointerEventPass,
                    IntPxSize(499.ipx, 494.ipx)
                )
            verify(pointerInputNode2.pointerInputHandler)
                .invoke(
                    listOf(expectedChange1),
                    pointerEventPass,
                    IntPxSize(499.ipx, 494.ipx)
                )
            verify(pointerInputNode3.pointerInputHandler)
                .invoke(
                    listOf(expectedChange2),
                    pointerEventPass,
                    IntPxSize(497.ipx, 492.ipx)
                )
            verify(pointerInputNode4.pointerInputHandler)
                .invoke(
                    listOf(expectedChange2),
                    pointerEventPass,
                    IntPxSize(497.ipx, 492.ipx)
                )
        }
        verifyNoMoreInteractions(pointerInputNode1.pointerInputHandler)
        verifyNoMoreInteractions(pointerInputNode2.pointerInputHandler)
        verifyNoMoreInteractions(pointerInputNode3.pointerInputHandler)
        verifyNoMoreInteractions(pointerInputNode4.pointerInputHandler)
    }

    @Test
    fun process_downOnCompletelyOverlappingPointerInputNodes_onlyTopPointerInputNodeReceives() {
        val childPointerInputNode1: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, LayoutNode(0, 0, 100, 100))
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        val childPointerInputNode2: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, LayoutNode(0, 0, 100, 100))
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        root.apply {
            emitInsertAt(0, childPointerInputNode1)
            emitInsertAt(1, childPointerInputNode2)
        }

        val down = PointerInputEvent(
            1, Uptime.Boot + 0.milliseconds, PxPosition(50.px, 50.px), true
        )

        // Act

        pointerInputEventProcessor.process(down, IntPxPosition.Origin)

        // Assert
        verify(childPointerInputNode2.pointerInputHandler, times(5)).invoke(any(), any(), any())
        verify(childPointerInputNode1.pointerInputHandler, never()).invoke(any(), any(), any())
    }

    @Test
    fun process_downOnPointerInputNodeWrappingSemanticsNodeWrappingLayoutNode_downReceived() {
        val semanticsComponentNode: SemanticsComponentNode =
            SemanticsComponentNode(
                1,
                SemanticsConfiguration()
            ).apply {
                emitInsertAt(0, LayoutNode(0, 0, 100, 100))
            }
        val pointerInputNode: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, semanticsComponentNode)
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        root.apply {
            emitInsertAt(0, pointerInputNode)
        }

        val down = PointerInputEvent(
            1, Uptime.Boot + 0.milliseconds, PxPosition(50.px, 50.px), true
        )

        // Act

        pointerInputEventProcessor.process(down, IntPxPosition.Origin)

        // Assert
        verify(pointerInputNode.pointerInputHandler, times(5)).invoke(any(), any(), any())
    }

    @Test
    fun process_downOnPointerInputNodeWrappingDrawNode_downNotReceived() {
        val pointerInputNode: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, DrawNode())
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        root.apply {
            emitInsertAt(0, pointerInputNode)
        }

        val down = PointerInputEvent(
            1, Uptime.Boot + 0.milliseconds, PxPosition(50.px, 50.px), true
        )

        // Act

        pointerInputEventProcessor.process(down, IntPxPosition.Origin)

        // Assert
        verify(pointerInputNode.pointerInputHandler, never()).invoke(any(), any(), any())
    }

    @Test
    fun process_downOnPointerInputNodeWrappingSemanticsNode_downNotReceived() {
        val pointerInputNode: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(
                0,
                SemanticsComponentNode(1, SemanticsConfiguration())
            )
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        root.apply {
            emitInsertAt(0, pointerInputNode)
        }

        val down = PointerInputEvent(
            1, Uptime.Boot + 0.milliseconds, PxPosition(50.px, 50.px), true
        )

        // Act

        pointerInputEventProcessor.process(down, IntPxPosition.Origin)

        // Assert
        verify(pointerInputNode.pointerInputHandler, never()).invoke(any(), any(), any())
    }

    @Test
    fun process_downOnPointerInputNodeWrappingPointerInputNodeNode_downNotReceived() {
        val pointerInputNode: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, PointerInputNode())
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        root.apply {
            emitInsertAt(0, pointerInputNode)
        }

        val down = PointerInputEvent(
            1, Uptime.Boot + 0.milliseconds, PxPosition(50.px, 50.px), true
        )

        // Act

        pointerInputEventProcessor.process(down, IntPxPosition.Origin)

        // Assert
        verify(pointerInputNode.pointerInputHandler, never()).invoke(any(), any(), any())
    }

    @Test
    fun processCancel_noPointers_doesntCrash() {
        pointerInputEventProcessor.processCancel()
    }

    @Test
    fun processCancel_downThenCancel_pinOnlyReceivesCorrectDownThenCancel() {

        // Arrange

        val pointerInputNode: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, LayoutNode(0, 0, 500, 500))
            pointerInputHandler = spy(MyPointerInputHandler())
            cancelHandler = spy(MyCancelHandler())
        }
        root.emitInsertAt(0, pointerInputNode)

        val pointerInputEvent =
            PointerInputEvent(
                7,
                Uptime.Boot + 5.milliseconds,
                PxPosition(250.px, 250.px),
                true
            )

        val expectedChange =
            PointerInputChange(
                id = PointerId(7, Uptime.Boot),
                current = PointerInputData(
                    Uptime.Boot + 5.milliseconds,
                    PxPosition(250.px, 250.px),
                    true
                ),
                previous = PointerInputData(null, null, false),
                consumed = ConsumedData()
            )

        // Act

        pointerInputEventProcessor.process(pointerInputEvent, IntPxPosition.Origin)
        pointerInputEventProcessor.processCancel()

        // Assert

        inOrder(pointerInputNode.pointerInputHandler, pointerInputNode.cancelHandler) {
            for (pass in PointerEventPass.values()) {
                verify(pointerInputNode.pointerInputHandler).invoke(
                    eq(listOf(expectedChange)),
                    eq(pass),
                    any()
                )
            }
            verify(pointerInputNode.cancelHandler).invoke()
        }
        verifyNoMoreInteractions(
            pointerInputNode.pointerInputHandler,
            pointerInputNode.cancelHandler
        )
    }

    @Test
    fun processCancel_downDownOnSamePinThenCancel_pinOnlyReceivesCorrectChangesThenCancel() {

        // Arrange

        val pointerInputNode: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, LayoutNode(0, 0, 500, 500))
            pointerInputHandler = spy(MyPointerInputHandler())
            cancelHandler = spy(MyCancelHandler())
        }
        root.emitInsertAt(0, pointerInputNode)

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
                    id = PointerId(7, Uptime.Boot),
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
                    id = PointerId(7, Uptime.Boot),
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
                    id = PointerId(9, Uptime.Boot),
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

        pointerInputEventProcessor.process(pointerInputEvent1, IntPxPosition.Origin)
        pointerInputEventProcessor.process(pointerInputEvent2, IntPxPosition.Origin)
        pointerInputEventProcessor.processCancel()

        // Assert

        inOrder(pointerInputNode.pointerInputHandler, pointerInputNode.cancelHandler) {
            for (pass in PointerEventPass.values()) {
                verify(pointerInputNode.pointerInputHandler).invoke(
                    eq(expectedChanges1),
                    eq(pass),
                    any()
                )
            }
            for (pass in PointerEventPass.values()) {
                verify(pointerInputNode.pointerInputHandler).invoke(
                    eq(expectedChanges2),
                    eq(pass),
                    any()
                )
            }
            verify(pointerInputNode.cancelHandler).invoke()
        }
        verifyNoMoreInteractions(
            pointerInputNode.pointerInputHandler,
            pointerInputNode.cancelHandler
        )
    }

    @Test
    fun processCancel_downOn2DifferentPinsThenCancel_pinsOnlyReceiveCorrectDownsThenCancel() {

        // Arrange

        val pointerInputNode1: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, LayoutNode(0, 0, 199, 199))
            pointerInputHandler = spy(MyPointerInputHandler())
            cancelHandler = spy(MyCancelHandler())
        }
        val pointerInputNode2: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, LayoutNode(200, 200, 399, 399))
            pointerInputHandler = spy(MyPointerInputHandler())
            cancelHandler = spy(MyCancelHandler())
        }
        root.emitInsertAt(0, pointerInputNode1)
        root.emitInsertAt(1, pointerInputNode2)

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
                id = PointerId(7, Uptime.Boot),
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
                id = PointerId(9, Uptime.Boot),
                current = PointerInputData(
                    Uptime.Boot + 5.milliseconds,
                    PxPosition(100.px, 100.px),
                    true
                ),
                previous = PointerInputData(null, null, false),
                consumed = ConsumedData()
            )

        // Act

        pointerInputEventProcessor.process(pointerInputEvent, IntPxPosition.Origin)
        pointerInputEventProcessor.processCancel()

        // Assert

        inOrder(pointerInputNode1.pointerInputHandler, pointerInputNode1.cancelHandler) {
            for (pass in PointerEventPass.values()) {
                verify(pointerInputNode1.pointerInputHandler).invoke(
                    eq(listOf(expectedChange1)),
                    eq(pass),
                    any()
                )
            }
            verify(pointerInputNode1.cancelHandler).invoke()
        }
        inOrder(pointerInputNode2.pointerInputHandler, pointerInputNode2.cancelHandler) {
            for (pass in PointerEventPass.values()) {
                verify(pointerInputNode2.pointerInputHandler).invoke(
                    eq(listOf(expectedChange2)),
                    eq(pass),
                    any()
                )
            }
            verify(pointerInputNode2.cancelHandler).invoke()
        }
        verifyNoMoreInteractions(
            pointerInputNode1.pointerInputHandler,
            pointerInputNode1.cancelHandler,
            pointerInputNode2.pointerInputHandler,
            pointerInputNode2.cancelHandler
        )
    }

    @Test
    fun processCancel_downMoveCancel_pinOnlyReceivesCorrectDownMoveCancel() {

        // Arrange

        val pointerInputNode: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, LayoutNode(0, 0, 500, 500))
            pointerInputHandler = spy(MyPointerInputHandler())
            cancelHandler = spy(MyCancelHandler())
        }
        root.emitInsertAt(0, pointerInputNode)

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
                id = PointerId(7, Uptime.Boot),
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
                id = PointerId(7, Uptime.Boot),
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

        pointerInputEventProcessor.process(down, IntPxPosition.Origin)
        pointerInputEventProcessor.process(move, IntPxPosition.Origin)
        pointerInputEventProcessor.processCancel()

        // Assert

        inOrder(pointerInputNode.pointerInputHandler, pointerInputNode.cancelHandler) {
            for (pass in PointerEventPass.values()) {
                verify(pointerInputNode.pointerInputHandler).invoke(
                    eq(listOf(expectedDown)),
                    eq(pass),
                    any()
                )
            }
            for (pass in PointerEventPass.values()) {
                verify(pointerInputNode.pointerInputHandler).invoke(
                    eq(listOf(expectedMove)),
                    eq(pass),
                    any()
                )
            }
            verify(pointerInputNode.cancelHandler).invoke()
        }
        verifyNoMoreInteractions(
            pointerInputNode.pointerInputHandler,
            pointerInputNode.cancelHandler
        )
    }

    @Test
    fun processCancel_downCancelMoveUp_pinOnlyReceivesCorrectDownCancel() {

        // Arrange

        val pointerInputNode: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, LayoutNode(0, 0, 500, 500))
            pointerInputHandler = spy(MyPointerInputHandler())
            cancelHandler = spy(MyCancelHandler())
        }
        root.emitInsertAt(0, pointerInputNode)

        val down =
            PointerInputEvent(
                7,
                Uptime.Boot + 5.milliseconds,
                PxPosition(200.px, 200.px),
                true
            )

        val expectedDown =
            PointerInputChange(
                id = PointerId(7, Uptime.Boot),
                current = PointerInputData(
                    Uptime.Boot + 5.milliseconds,
                    PxPosition(200.px, 200.px),
                    true
                ),
                previous = PointerInputData(null, null, false),
                consumed = ConsumedData()
            )

        // Act

        pointerInputEventProcessor.process(down, IntPxPosition.Origin)
        pointerInputEventProcessor.processCancel()

        // Assert

        inOrder(pointerInputNode.pointerInputHandler, pointerInputNode.cancelHandler) {
            for (pass in PointerEventPass.values()) {
                verify(pointerInputNode.pointerInputHandler).invoke(
                    eq(listOf(expectedDown)),
                    eq(pass),
                    any()
                )
            }
            verify(pointerInputNode.cancelHandler).invoke()
        }
        verifyNoMoreInteractions(
            pointerInputNode.pointerInputHandler,
            pointerInputNode.cancelHandler
        )
    }

    @Test
    fun processCancel_downCancelDown_pinOnlyReceivesCorrectDownCancelDown() {

        // Arrange

        val pointerInputNode: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, LayoutNode(0, 0, 500, 500))
            pointerInputHandler = spy(MyPointerInputHandler())
            cancelHandler = spy(MyCancelHandler())
        }
        root.emitInsertAt(0, pointerInputNode)

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
                id = PointerId(7, Uptime.Boot),
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
                id = PointerId(7, Uptime.Boot),
                current = PointerInputData(
                    Uptime.Boot + 10.milliseconds,
                    PxPosition(200.px, 200.px),
                    true
                ),
                previous = PointerInputData(null, null, false),
                consumed = ConsumedData()
            )

        // Act

        pointerInputEventProcessor.process(down1, IntPxPosition.Origin)
        pointerInputEventProcessor.processCancel()
        pointerInputEventProcessor.process(down2, IntPxPosition.Origin)

        // Assert

        inOrder(pointerInputNode.pointerInputHandler, pointerInputNode.cancelHandler) {
            for (pass in PointerEventPass.values()) {
                verify(pointerInputNode.pointerInputHandler).invoke(
                    eq(listOf(expectedDown1)),
                    eq(pass),
                    any()
                )
            }
            verify(pointerInputNode.cancelHandler).invoke()
            for (pass in PointerEventPass.values()) {
                verify(pointerInputNode.pointerInputHandler).invoke(
                    eq(listOf(expectedDown2)),
                    eq(pass),
                    any()
                )
            }
        }
        verifyNoMoreInteractions(
            pointerInputNode.pointerInputHandler,
            pointerInputNode.cancelHandler
        )
    }

    @Test
    fun process_pointerInputNodeRemovedDuringInput_correctPointerInputChangesReceived() {

        // Arrange

        val childLayoutNode = LayoutNode(0, 0, 100, 100)
        val childPointerInputNode: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, childLayoutNode)
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        val parentLayoutNode: LayoutNode = LayoutNode(0, 0, 100, 100).apply {
            emitInsertAt(0, childPointerInputNode)
        }
        val parentPointerInputNode: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, parentLayoutNode)
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        root.emitInsertAt(0, parentPointerInputNode)

        val offset = PxPosition(50.px, 50.px)

        val down = PointerInputEvent(0, Uptime.Boot + 7.milliseconds, offset, true)
        val up = PointerInputEvent(0, Uptime.Boot + 11.milliseconds, null, false)

        val expectedDownChange = PointerInputChange(
            id = PointerId(0, Uptime.Boot),
            current = PointerInputData(Uptime.Boot + 7.milliseconds, offset, true),
            previous = PointerInputData(null, null, false),
            consumed = ConsumedData()
        )

        val expectedUpChange = PointerInputChange(
            id = PointerId(0, Uptime.Boot),
            current = PointerInputData(Uptime.Boot + 11.milliseconds, null, false),
            previous = PointerInputData(Uptime.Boot + 7.milliseconds, offset, true),
            consumed = ConsumedData()
        )

        // Act

        pointerInputEventProcessor.process(down, IntPxPosition.Origin)
        parentLayoutNode.emitRemoveAt(0, 1)
        pointerInputEventProcessor.process(up, IntPxPosition.Origin)

        // Assert

        PointerEventPass.values().forEach {
            verify(parentPointerInputNode.pointerInputHandler)
                .invoke(eq(listOf(expectedDownChange)), eq(it), any())
            verify(childPointerInputNode.pointerInputHandler)
                .invoke(eq(listOf(expectedDownChange)), eq(it), any())
            verify(parentPointerInputNode.pointerInputHandler)
                .invoke(eq(listOf(expectedUpChange)), eq(it), any())
        }
        verifyNoMoreInteractions(parentPointerInputNode.pointerInputHandler)
        verifyNoMoreInteractions(childPointerInputNode.pointerInputHandler)
    }

    @Test
    fun process_pointerInputNodeRemovedDuringInput_cancelDispatchedToCorrectPointerInputNode() {

        // Arrange

        val childLayoutNode = LayoutNode(0, 0, 100, 100)
        val childPointerInputNode: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, childLayoutNode)
            cancelHandler = spy(MyCancelHandler())
        }
        val parentLayoutNode: LayoutNode = LayoutNode(0, 0, 100, 100).apply {
            emitInsertAt(0, childPointerInputNode)
        }
        val parentPointerInputNode: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, parentLayoutNode)
            cancelHandler = spy(MyCancelHandler())
        }
        root.emitInsertAt(0, parentPointerInputNode)

        val down =
            PointerInputEvent(0, Uptime.Boot + 7.milliseconds, PxPosition(50.px, 50.px), true)

        val up = PointerInputEvent(0, Uptime.Boot + 11.milliseconds, null, false)

        // Act

        pointerInputEventProcessor.process(down, IntPxPosition.Origin)
        parentLayoutNode.emitRemoveAt(0, 1)
        pointerInputEventProcessor.process(up, IntPxPosition.Origin)

        // Assert
        verify(childPointerInputNode.cancelHandler).invoke()
        verify(parentPointerInputNode.cancelHandler, never()).invoke()
    }

    @Test
    fun process_childLayoutNodeRemovedDuringInput_correctPointerInputChangesReceived() {

        // Arrange

        val childLayoutNode = LayoutNode(0, 0, 100, 100)
        val childPointerInputNode: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, childLayoutNode)
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        val parentLayoutNode: LayoutNode = LayoutNode(0, 0, 100, 100).apply {
            emitInsertAt(0, childPointerInputNode)
        }
        val parentPointerInputNode: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, parentLayoutNode)
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        root.emitInsertAt(0, parentPointerInputNode)

        val offset = PxPosition(50.px, 50.px)

        val down = PointerInputEvent(0, Uptime.Boot + 7.milliseconds, offset, true)
        val up = PointerInputEvent(0, Uptime.Boot + 11.milliseconds, null, false)

        val expectedDownChange = PointerInputChange(
            id = PointerId(0, Uptime.Boot),
            current = PointerInputData(Uptime.Boot + 7.milliseconds, offset, true),
            previous = PointerInputData(null, null, false),
            consumed = ConsumedData()
        )

        val expectedUpChange = PointerInputChange(
            id = PointerId(0, Uptime.Boot),
            current = PointerInputData(Uptime.Boot + 11.milliseconds, null, false),
            previous = PointerInputData(Uptime.Boot + 7.milliseconds, offset, true),
            consumed = ConsumedData()
        )

        // Act

        pointerInputEventProcessor.process(down, IntPxPosition.Origin)
        childPointerInputNode.emitRemoveAt(0, 1)
        pointerInputEventProcessor.process(up, IntPxPosition.Origin)

        // Assert

        PointerEventPass.values().forEach {
            verify(parentPointerInputNode.pointerInputHandler)
                .invoke(eq(listOf(expectedDownChange)), eq(it), any())
            verify(childPointerInputNode.pointerInputHandler)
                .invoke(eq(listOf(expectedDownChange)), eq(it), any())
            verify(parentPointerInputNode.pointerInputHandler)
                .invoke(eq(listOf(expectedUpChange)), eq(it), any())
        }
        verifyNoMoreInteractions(parentPointerInputNode.pointerInputHandler)
        verifyNoMoreInteractions(childPointerInputNode.pointerInputHandler)
    }

    @Test
    fun process_childLayoutNodeRemovedDuringInput_cancelDispatchedToCorrectPointerInputNode() {

        // Arrange

        val childLayoutNode = LayoutNode(0, 0, 100, 100)
        val childPointerInputNode: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, childLayoutNode)
            cancelHandler = spy(MyCancelHandler())
        }
        val parentLayoutNode: LayoutNode = LayoutNode(0, 0, 100, 100).apply {
            emitInsertAt(0, childPointerInputNode)
        }
        val parentPointerInputNode: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, parentLayoutNode)
            cancelHandler = spy(MyCancelHandler())
        }
        root.emitInsertAt(0, parentPointerInputNode)

        val down =
            PointerInputEvent(0, Uptime.Boot + 7.milliseconds, PxPosition(50.px, 50.px), true)

        val up = PointerInputEvent(0, Uptime.Boot + 11.milliseconds, null, false)

        // Act

        pointerInputEventProcessor.process(down, IntPxPosition.Origin)
        childPointerInputNode.emitRemoveAt(0, 1)
        pointerInputEventProcessor.process(up, IntPxPosition.Origin)

        // Assert
        verify(childPointerInputNode.cancelHandler).invoke()
        verify(parentPointerInputNode.cancelHandler, never()).invoke()
    }
}