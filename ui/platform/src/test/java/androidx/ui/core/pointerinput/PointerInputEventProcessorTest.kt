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
import androidx.ui.core.LayoutNode
import androidx.ui.core.Owner
import androidx.ui.core.PointerInputNode
import androidx.ui.core.Timestamp
import androidx.ui.core.ipx
import androidx.ui.core.pointerinput.PointerEventPass.InitialDown
import androidx.ui.core.pointerinput.PointerEventPass.PreUp
import androidx.ui.engine.geometry.Offset
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
// 2 Directly Nested PointerInputNodes inside offset LayoutNode, offset is correct
// PointerInputNode inside 3 nested parents, offset is correct
// 2 LayoutNodes in between PointerInputNodes, offset is correct
// 3 simultaneous moves, offsets are correct

// TODO(shepshapard): Write hit test tests related to siblings (once the functionality has been
// written).

// TODO(shepshapard): Write the following pointer input dispatch path tests:
// down, move, up, on 2, hits all 5 passes
// 2 down, hits for each individually (TODO, this will change)

// TODO(shepshapard): These tests shouldn't require Android to run, but currently do given Crane
// currently relies on Android Context.
@SmallTest
@RunWith(JUnit4::class)
class PointerInputEventProcessorTest {

    private val mMockOwner = mock(Owner::class.java)
    private lateinit var mTrackerList:
            MutableList<Triple<PointerInputNode, PointerEventPass, PointerInputChange>>

    @Before
    fun setup() {
        mTrackerList = mutableListOf()
    }

    @Test
    fun process_downMoveUp_convertedCorrectlyAndTraversesAllPassesInCorrectOrder() {

        // Arrange

        val pointerInputNode: PointerInputNode = PointerInputNode()
            .apply {
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        val layoutNode: LayoutNode = LayoutNode(0, 0, 500, 500).apply {
            emitInsertAt(0, pointerInputNode)
            attach(mMockOwner)
        }

        val pointerInputEventProcessor = PointerInputEventProcessor(layoutNode)

        val offset = Offset(100f, 200f)
        val offset2 = Offset(300f, 400f)

        val events = arrayOf(
            PointerInputEvent(Timestamp(0), 8712, offset, true),
            PointerInputEvent(Timestamp(0), 8712, offset2, true),
            PointerInputEvent(Timestamp(0), 8712, offset2, false)
        )

        val expectedChanges = arrayOf(
            PointerInputChange(
                id = 8712,
                current = PointerInputData(offset, true),
                previous = PointerInputData(null, false),
                consumed = ConsumedData()
            ),
            PointerInputChange(
                id = 8712,
                current = PointerInputData(offset2, true),
                previous = PointerInputData(offset, true),
                consumed = ConsumedData()
            ),
            PointerInputChange(
                id = 8712,
                current = PointerInputData(offset2, false),
                previous = PointerInputData(offset2, true),
                consumed = ConsumedData()
            )
        )

        // Act

        events.forEach { pointerInputEventProcessor.process(it) }

        // Assert

        inOrder(pointerInputNode.pointerInputHandler) {
            for (expected in expectedChanges) {
                for (pass in PointerEventPass.values()) {
                    verify(pointerInputNode.pointerInputHandler).invoke(expected, pass)
                }
            }
        }
    }

    @Test
    fun process_downHits_targetReceives() {

        // Arrange

        val pointerInputNode: PointerInputNode = PointerInputNode()
            .apply {
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        val childOffset = Offset(dx = 100f, dy = 200f)
        val childLayoutNode: LayoutNode = LayoutNode(100, 200, 301, 401).apply {
            emitInsertAt(0, pointerInputNode)
        }
        val parentLayoutNode: LayoutNode = LayoutNode(0, 0, 500, 500).apply {
            emitInsertAt(0, childLayoutNode)
            attach(mMockOwner)
        }

        val pointerInputEventProcessor = PointerInputEventProcessor(
            parentLayoutNode
        )

        val offsets = arrayOf(
            Offset(100f, 200f),
            Offset(300f, 200f),
            Offset(100f, 400f),
            Offset(300f, 400f)
        )

        val events = Array(4) { index ->
            PointerInputEvent(Timestamp(0), index, offsets[index], true)
        }

        val expectedChanges = Array(4) { index ->
            PointerInputChange(
                id = index,
                current = PointerInputData(offsets[index] - childOffset, true),
                previous = PointerInputData(null, false),
                consumed = ConsumedData()
            )
        }

        // Act

        events.forEach {
            pointerInputEventProcessor.process(it)
        }

        // Assert

        verify(pointerInputNode.pointerInputHandler, times(4)).invoke(any(), eq(InitialDown))
        for (expected in expectedChanges) {
            verify(pointerInputNode.pointerInputHandler).invoke(expected, InitialDown)
        }
    }

    @Test
    fun process_downMisses_targetDoesNotReceive() {

        // Arrange

        val pointerInputNode: PointerInputNode = PointerInputNode()
            .apply {
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        val childLayoutNode: LayoutNode = LayoutNode(100, 200, 301, 401).apply {
            emitInsertAt(0, pointerInputNode)
        }
        val parentLayoutNode: LayoutNode = LayoutNode(0, 0, 500, 500).apply {
            emitInsertAt(0, childLayoutNode)
            attach(mMockOwner)
        }

        val pointerInputEventProcessor = PointerInputEventProcessor(
            parentLayoutNode
        )

        val offsets = arrayOf(
            Offset(99f, 200f),
            Offset(99f, 400f),
            Offset(100f, 199f),
            Offset(100f, 401f),
            Offset(300f, 199f),
            Offset(300f, 401f),
            Offset(301f, 200f),
            Offset(301f, 400f)
        )

        val events = Array(8) { index ->
            PointerInputEvent(Timestamp(0), index, offsets[index], true)
        }

        // Act

        events.forEach {
            pointerInputEventProcessor.process(it)
        }

        // Assert

        verify(pointerInputNode.pointerInputHandler, never()).invoke(any(), any())
    }

    @Test
    fun process_downHits3of3_all3TargetsReceive() {
        process_partialTreeHits(3)
    }

    @Test
    fun process_downHits2of3_correct2TargetsReceive() {
        process_partialTreeHits(2)
    }

    @Test
    fun process_downHits1of3_onlyCorrectTargetReceives() {
        process_partialTreeHits(1)
    }

    private fun process_partialTreeHits(numberOfChildrenHit: Int) {
        // Arrange

        val childPointerInputNode: PointerInputNode = PointerInputNode()
            .apply {
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        val childLayoutNode: LayoutNode = LayoutNode(100, 100, 200, 200).apply {
            emitInsertAt(0, childPointerInputNode)
        }
        val middlePointerInputNode: PointerInputNode = PointerInputNode()
            .apply {
            emitInsertAt(0, childLayoutNode)
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        val middleLayoutNode: LayoutNode = LayoutNode(100, 100, 400, 400).apply {
            emitInsertAt(0, middlePointerInputNode)
        }
        val parentPointerInputNode: PointerInputNode = PointerInputNode()
            .apply {
            emitInsertAt(0, middleLayoutNode)
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        val parentLayoutNode: LayoutNode = LayoutNode(0, 0, 500, 500).apply {
            emitInsertAt(0, parentPointerInputNode)
            attach(mMockOwner)
        }

        val offset = when (numberOfChildrenHit) {
            3 -> Offset(250f, 250f)
            2 -> Offset(150f, 150f)
            1 -> Offset(50f, 50f)
            else -> throw IllegalStateException()
        }

        val pointerInputEventProcessor = PointerInputEventProcessor(parentLayoutNode)

        val event = PointerInputEvent(Timestamp(0), 0, offset, true)

        // Act

        pointerInputEventProcessor.process(event)

        // Assert

        when (numberOfChildrenHit) {
            3 -> {
                verify(parentPointerInputNode.pointerInputHandler).invoke(any(), eq(InitialDown))
                verify(middlePointerInputNode.pointerInputHandler).invoke(any(), eq(InitialDown))
                verify(childPointerInputNode.pointerInputHandler).invoke(any(), eq(InitialDown))
            }
            2 -> {
                verify(parentPointerInputNode.pointerInputHandler).invoke(any(), eq(InitialDown))
                verify(middlePointerInputNode.pointerInputHandler).invoke(any(), eq(InitialDown))
                verify(childPointerInputNode.pointerInputHandler, never()).invoke(any(), any())
            }
            1 -> {
                verify(parentPointerInputNode.pointerInputHandler).invoke(any(), eq(InitialDown))
                verify(middlePointerInputNode.pointerInputHandler, never()).invoke(any(), any())
                verify(childPointerInputNode.pointerInputHandler, never()).invoke(any(), any())
            }
            else -> throw IllegalStateException()
        }
    }

    @Test
    fun process_modifiedPointerInputChange_isPassedToNext() {

        // Arrange

        val input = PointerInputChange(
            id = 0,
            current = PointerInputData(Offset(100f, 0f), true),
            previous = PointerInputData(Offset(0f, 0f), true),
            consumed = ConsumedData(positionChange = Offset(0f, 0f))
        )
        val output = PointerInputChange(
            id = 0,
            current = PointerInputData(Offset(100f, 0f), true),
            previous = PointerInputData(Offset(0f, 0f), true),
            consumed = ConsumedData(positionChange = Offset(13f, 0f))
        )

        val pointerInputNode: PointerInputNode = PointerInputNode()
            .apply {
            pointerInputHandler = spy(MyPointerInputHandler())
            whenever(pointerInputHandler.invoke(input, InitialDown)).thenReturn(output)
        }
        val layoutNode: LayoutNode = LayoutNode(0, 0, 500, 500).apply {
            emitInsertAt(0, pointerInputNode)
            attach(mMockOwner)
        }

        val pointerInputEventProcessor = PointerInputEventProcessor(layoutNode)

        val down = PointerInputEvent(
            Timestamp(0),
            0,
            Offset(0f, 0f),
            true
        )
        val move = PointerInputEvent(
            Timestamp(0),
            0,
            Offset(100f, 0f),
            true
        )

        // Act

        pointerInputEventProcessor.process(down)
        pointerInputEventProcessor.process(move)

        // Assert

        verify(pointerInputNode.pointerInputHandler).invoke(input, InitialDown)
        verify(pointerInputNode.pointerInputHandler).invoke(output, PreUp)
    }

    @Test
    fun process_layoutNodesIncreasinglyInset_pointerInputChangeTranslatedCorrectly() {
        process_pointerInputChangeTranslatedCorrectly(
            0, 0, 100, 100,
            2, 11, 100, 100,
            23, 31, 100, 100,
            99, 99
        )
    }

    @Test
    fun process_layoutNodesIncreasinglyOutset_pointerInputChangeTranslatedCorrectly() {
        process_pointerInputChangeTranslatedCorrectly(
            0, 0, 100, 100,
            -2, -11, 100, 100,
            -23, -31, 100, 100,
            1, 1
        )
    }

    @Test
    fun process_layoutNodesNotOffset_pointerInputChangeTranslatedCorrectly() {
        process_pointerInputChangeTranslatedCorrectly(
            0, 0, 100, 100,
            0, 0, 100, 100,
            0, 0, 100, 100,
            50, 50
        )
    }

    private fun process_pointerInputChangeTranslatedCorrectly(
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
        pointerX: Int,
        pointerY: Int
    ) {

        // Arrange

        val childPointerInputNode: PointerInputNode = PointerInputNode()
            .apply {
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        val childOffset = Offset(dx = cX1.toFloat(), dy = cY1.toFloat())
        val childLayoutNode: LayoutNode = LayoutNode(cX1, cY1, cX2, cY2).apply {
            emitInsertAt(0, childPointerInputNode)
        }
        val middlePointerInputNode: PointerInputNode = PointerInputNode()
            .apply {
            emitInsertAt(0, childLayoutNode)
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        val middleOffset = Offset(dx = mX1.toFloat(), dy = mY1.toFloat())
        val middleLayoutNode: LayoutNode = LayoutNode(mX1, mY1, mX2, mY2).apply {
            emitInsertAt(0, middlePointerInputNode)
        }
        val parentPointerInputNode: PointerInputNode = PointerInputNode()
            .apply {
            emitInsertAt(0, middleLayoutNode)
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        val parentLayoutNode: LayoutNode = LayoutNode(pX1, pY1, pX2, pY2).apply {
            emitInsertAt(0, parentPointerInputNode)
            attach(mMockOwner)
        }

        val pointerInputEventProcessor = PointerInputEventProcessor(parentLayoutNode)

        val offset = Offset(pointerX.toFloat(), pointerY.toFloat())

        val down = PointerInputEvent(Timestamp(0), 0, offset, true)

        val pointerInputNodes = arrayOf(
            parentPointerInputNode,
            middlePointerInputNode,
            childPointerInputNode
        )

        val expectedPointerInputChanges = arrayOf(
            PointerInputChange(
                id = 0,
                current = PointerInputData(offset, true),
                previous = PointerInputData(null, false),
                consumed = ConsumedData()
            ),
            PointerInputChange(
                id = 0,
                current = PointerInputData(offset - middleOffset, true),
                previous = PointerInputData(null, false),
                consumed = ConsumedData()
            ),
            PointerInputChange(
                id = 0,
                current = PointerInputData(offset - middleOffset - childOffset, true),
                previous = PointerInputData(null, false),
                consumed = ConsumedData()
            )
        )

        // Act

        pointerInputEventProcessor.process(down)

        // Assert

        for (pass in PointerEventPass.values())
            for (i in 0..2) {
                verify(pointerInputNodes[i].pointerInputHandler).invoke(
                    expectedPointerInputChanges[i],
                    pass
                )
            }
    }

    /**
     * This test creates a tree of this shape
     *
     *         (LayoutNode)
     *          /        \
     *    (LayoutNode)  (LayoutNode)
     *        /               \
     * (PointerInputNode)  (PointerInputNode)
     *
     * Where 2 child LayoutNodes do not overlap. The test verifies that a PointerInputEvent with
     * 2 down events that hit both of the PointerInputNodes at the same time, results in the correct
     * PointerEventChanges being passed to the PointerInputNodes through all passes.
     */
    @Test
    fun process_binaryTreeHeight2_pointerInputChangeTranslatedCorrectly() {

        // Arrange

        val childPointerInputNode1: PointerInputNode = PointerInputNode()
            .apply {
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        val childLayoutNode1: LayoutNode = LayoutNode(0, 0, 50, 50).apply {
            emitInsertAt(0, childPointerInputNode1)
        }
        val childPointerInputNode2: PointerInputNode = PointerInputNode()
            .apply {
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        val childLayoutNode2: LayoutNode = LayoutNode(50, 50, 100, 100).apply {
            emitInsertAt(0, childPointerInputNode2)
        }
        val parentLayoutNode: LayoutNode = LayoutNode(0, 0, 100, 100).apply {
            emitInsertAt(0, childLayoutNode1)
            emitInsertAt(1, childLayoutNode2)
            attach(mMockOwner)
        }

        val pointerInputEventProcessor = PointerInputEventProcessor(parentLayoutNode)

        val offset1 = Offset(25f, 25f)
        val offset2 = Offset(75f, 75f)

        val down = PointerInputEvent(
            Timestamp(0),
            listOf(
                PointerInputEventData(0, offset1, true),
                PointerInputEventData(1, offset2, true)
            )
        )

        val pointerInputChange1 = PointerInputChange(
            id = 0,
            current = PointerInputData(offset1, true),
            previous = PointerInputData(null, false),
            consumed = ConsumedData()
        )
        val pointerInputChange2 = PointerInputChange(
            id = 1,
            current = PointerInputData(offset2 - Offset(50f, 50f), true),
            previous = PointerInputData(null, false),
            consumed = ConsumedData()
        )

        // Act

        pointerInputEventProcessor.process(down)

        // Assert

        for (pointerEventPass in PointerEventPass.values()) {
            verify(childPointerInputNode1.pointerInputHandler)
                .invoke(pointerInputChange1, pointerEventPass)
            verify(childPointerInputNode2.pointerInputHandler)
                .invoke(pointerInputChange2, pointerEventPass)
        }
    }

    // --- Stopped Here ---

    @Test
    fun process_pointerInputNodeRemovedDuringInput_correctPointerInputChangesReceived() {

        // Arrange

        val childPointerInputNode: PointerInputNode = PointerInputNode()
            .apply {
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        val childLayoutNode: LayoutNode = LayoutNode(0, 0, 100, 100).apply {
            emitInsertAt(0, childPointerInputNode)
        }
        val parentPointerInputNode: PointerInputNode = PointerInputNode()
            .apply {
            emitInsertAt(0, childLayoutNode)
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        val parentLayoutNode: LayoutNode = LayoutNode(0, 0, 100, 100).apply {
            emitInsertAt(0, parentPointerInputNode)
            attach(mMockOwner)
        }

        val offset = Offset(50f, 50f)

        val down = PointerInputEvent(Timestamp(0), 0, offset, true)
        val up = PointerInputEvent(Timestamp(1), 0, null, false)

        val pointerInputEventProcessor = PointerInputEventProcessor(parentLayoutNode)

        val expectedDownChange = PointerInputChange(
            id = 0,
            current = PointerInputData(offset, true),
            previous = PointerInputData(null, false),
            consumed = ConsumedData()
        )

        val expectedUpChange = PointerInputChange(
            id = 0,
            current = PointerInputData(null, false),
            previous = PointerInputData(offset, true),
            consumed = ConsumedData()
        )

        // Act

        pointerInputEventProcessor.process(down)
        parentPointerInputNode.emitRemoveAt(0, 1)
        pointerInputEventProcessor.process(up)

        // Assert

        PointerEventPass.values().forEach {
            verify(parentPointerInputNode.pointerInputHandler)
                .invoke(expectedDownChange, it)
            verify(childPointerInputNode.pointerInputHandler)
                .invoke(expectedDownChange, it)
            verify(parentPointerInputNode.pointerInputHandler)
                .invoke(expectedUpChange, it)
        }
        verifyNoMoreInteractions(parentPointerInputNode.pointerInputHandler)
        verifyNoMoreInteractions(childPointerInputNode.pointerInputHandler)
    }

    // Private helpers

    private fun LayoutNode(x: Int, y: Int, x2: Int, y2: Int) =
        LayoutNode().apply {
            moveTo(x.ipx, y.ipx)
            resize(x2.ipx - x.ipx, y2.ipx - y.ipx)
        }

    private fun PointerInputEventData(
        id: Int,
        position: Offset?,
        down: Boolean
    ): PointerInputEventData {
        val pointerInputData = PointerInputData(position, down)
        return PointerInputEventData(id, pointerInputData)
    }

    private fun PointerInputEvent(
        timeStamp: Timestamp,
        id: Int,
        position: Offset?,
        down: Boolean
    ): PointerInputEvent {
        return PointerInputEvent(timeStamp, listOf(PointerInputEventData(id, position, down)))
    }

    private open inner class MyPointerInputHandler() :
        Function2<PointerInputChange, PointerEventPass, PointerInputChange> {
        override fun invoke(p1: PointerInputChange, p2: PointerEventPass): PointerInputChange {
            return p1
        }
    }
}
