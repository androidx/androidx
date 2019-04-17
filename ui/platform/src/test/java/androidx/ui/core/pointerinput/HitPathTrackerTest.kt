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
import androidx.ui.core.PointerEventPass
import androidx.ui.core.PointerInputNode
import androidx.ui.core.add
import androidx.ui.core.consumeDownChange
import androidx.ui.core.consumePositionChange
import androidx.ui.core.millisecondsToTimestamp
import androidx.ui.core.positionChange
import androidx.ui.core.px
import androidx.ui.testutils.down
import androidx.ui.testutils.moveTo
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito

@SmallTest
@RunWith(JUnit4::class)
class HitPathTrackerTest {

    private lateinit var compositionRoot: LayoutNode
    private lateinit var hitResult: HitPathTracker
    private val mockOwner = Mockito.mock(Owner::class.java)

    @Before
    fun setup() {
        compositionRoot = LayoutNode()
        compositionRoot.attach(mockOwner)
        hitResult = HitPathTracker()
    }

    @Test
    fun addHitPath_emptyHitResult_resultIsCorrect() {
        val pin1 = PointerInputNode()
        val pin2 = PointerInputNode()
        val pin3 = PointerInputNode()

        hitResult.addHitPath(1, listOf(pin1, pin2, pin3))

        val expectedRoot = Node().apply {
            children.add(Node(pin1).apply {
                pointerIds.add(1)
                children.add(Node(pin2).apply {
                    pointerIds.add(1)
                    children.add(Node(pin3).apply {
                        pointerIds.add(1)
                    })
                })
            })
        }
        assertThat(areEqual(hitResult.root, expectedRoot)).isTrue()
    }

    @Test
    fun addHitPath_existingNonMatchingTree_resultIsCorrect() {
        val pin1 = PointerInputNode()
        val pin2 = PointerInputNode()
        val pin3 = PointerInputNode()
        val pin4 = PointerInputNode()
        val pin5 = PointerInputNode()
        val pin6 = PointerInputNode()
        hitResult.addHitPath(1, listOf(pin1, pin2, pin3))

        hitResult.addHitPath(2, listOf(pin4, pin5, pin6))

        val expectedRoot = Node(null).apply {
            children.add(Node(pin1).apply {
                pointerIds.add(1)
                children.add(Node(pin2).apply {
                    pointerIds.add(1)
                    children.add(Node(pin3).apply {
                        pointerIds.add(1)
                    })
                })
            })
            children.add(Node(pin4).apply {
                pointerIds.add(2)
                children.add(Node(pin5).apply {
                    pointerIds.add(2)
                    children.add(Node(pin6).apply {
                        pointerIds.add(2)
                    })
                })
            })
        }
        assertThat(areEqual(hitResult.root, expectedRoot)).isTrue()
    }

    @Test
    fun addHitPath_completeMatchingTree_resultIsCorrect() {
        val pin1 = PointerInputNode()
        val pin2 = PointerInputNode()
        val pin3 = PointerInputNode()
        hitResult.addHitPath(1, listOf(pin1, pin2, pin3))

        hitResult.addHitPath(2, listOf(pin1, pin2, pin3))

        val expectedRoot = Node(null).apply {
            children.add(Node(pin1).apply {
                pointerIds.add(1)
                pointerIds.add(2)
                children.add(Node(pin2).apply {
                    pointerIds.add(1)
                    pointerIds.add(2)
                    children.add(Node(pin3).apply {
                        pointerIds.add(1)
                        pointerIds.add(2)
                    })
                })
            })
        }
        assertThat(areEqual(hitResult.root, expectedRoot)).isTrue()
    }

    @Test
    fun addHitPath_partiallyMatchingTree_resultIsCorrect() {
        val pin1 = PointerInputNode()
        val pin2 = PointerInputNode()
        val pin3 = PointerInputNode()

        val pin4 = PointerInputNode()
        val pin5 = PointerInputNode()
        hitResult.addHitPath(1, listOf(pin1, pin2, pin3))

        hitResult.addHitPath(2, listOf(pin1, pin4, pin5))

        val expectedRoot = Node(null).apply {
            children.add(Node(pin1).apply {
                pointerIds.add(1)
                pointerIds.add(2)
                children.add(Node(pin2).apply {
                    pointerIds.add(1)
                    children.add(Node(pin3).apply {
                        pointerIds.add(1)
                    })
                })
                children.add(Node(pin4).apply {
                    pointerIds.add(2)
                    children.add(Node(pin5).apply {
                        pointerIds.add(2)
                    })
                })
            })
        }
        assertThat(areEqual(hitResult.root, expectedRoot)).isTrue()
    }

    @Test
    fun dispatchChanges_noNodes_doesNotCrash() {
        hitResult.dispatchChanges(listOf(down(0)), PointerEventPass.InitialDown)
    }

    @Test
    fun dispatchChanges_hitResultHasSingleMatch_pointerInputHandlerCalled() {
        val pin1 = PointerInputNode()
        pin1.pointerInputHandler = spy(MyPointerInputHandler())
        hitResult.addHitPath(13, listOf(pin1))

        hitResult.dispatchChanges(listOf(down(13)), PointerEventPass.InitialDown)

        verify(pin1.pointerInputHandler).invoke(down(13), PointerEventPass.InitialDown)
        verifyNoMoreInteractions(pin1.pointerInputHandler)
    }

    @Test
    fun dispatchChanges_hitResultHasMultipleMatches_pointerInputHandlersCalledInCorrectOrder() {
        val pin1 = PointerInputNode()
        val pin2 = PointerInputNode()
        val pin3 = PointerInputNode()
        pin1.pointerInputHandler = spy(MyPointerInputHandler())
        pin2.pointerInputHandler = spy(MyPointerInputHandler())
        pin3.pointerInputHandler = spy(MyPointerInputHandler())
        hitResult.addHitPath(13, listOf(pin1, pin2, pin3))

        hitResult.dispatchChanges(listOf(down(13)), PointerEventPass.InitialDown)

        inOrder(pin1.pointerInputHandler, pin2.pointerInputHandler, pin3.pointerInputHandler) {
            verify(pin1.pointerInputHandler).invoke(down(13), PointerEventPass.InitialDown)
            verify(pin2.pointerInputHandler).invoke(down(13), PointerEventPass.InitialDown)
            verify(pin3.pointerInputHandler).invoke(down(13), PointerEventPass.InitialDown)
        }
        verifyNoMoreInteractions(
            pin1.pointerInputHandler,
            pin2.pointerInputHandler,
            pin3.pointerInputHandler
        )
    }

    @Test
    fun dispatchChanges_hasDownAndUpPath_pointerInputHandlersCalledInCorrectOrder() {
        val pin1 = PointerInputNode()
        val pin2 = PointerInputNode()
        val pin3 = PointerInputNode()
        pin1.pointerInputHandler = spy(MyPointerInputHandler())
        pin2.pointerInputHandler = spy(MyPointerInputHandler())
        pin3.pointerInputHandler = spy(MyPointerInputHandler())
        hitResult.addHitPath(13, listOf(pin1, pin2, pin3))

        hitResult.dispatchChanges(
            listOf(down(13)),
            PointerEventPass.InitialDown,
            PointerEventPass.PreUp
        )

        inOrder(pin1.pointerInputHandler, pin2.pointerInputHandler, pin3.pointerInputHandler) {
            verify(pin1.pointerInputHandler).invoke(down(13), PointerEventPass.InitialDown)
            verify(pin2.pointerInputHandler).invoke(down(13), PointerEventPass.InitialDown)
            verify(pin3.pointerInputHandler).invoke(down(13), PointerEventPass.InitialDown)
            verify(pin3.pointerInputHandler).invoke(down(13), PointerEventPass.PreUp)
            verify(pin2.pointerInputHandler).invoke(down(13), PointerEventPass.PreUp)
            verify(pin1.pointerInputHandler).invoke(down(13), PointerEventPass.PreUp)
        }
        verifyNoMoreInteractions(
            pin1.pointerInputHandler,
            pin2.pointerInputHandler,
            pin3.pointerInputHandler
        )
    }

    @Test
    fun dispatchChanges_2IndependentBranchesFromRoot_eventsSplitCorrectlyAndCallOrderCorrect() {
        val pin1 = PointerInputNode()
        val pin2 = PointerInputNode()
        val pin3 = PointerInputNode()
        val pin4 = PointerInputNode()
        pin1.pointerInputHandler = spy(MyPointerInputHandler())
        pin2.pointerInputHandler = spy(MyPointerInputHandler())
        pin3.pointerInputHandler = spy(MyPointerInputHandler())
        pin4.pointerInputHandler = spy(MyPointerInputHandler())
        hitResult.addHitPath(3, listOf(pin1, pin2))
        hitResult.addHitPath(5, listOf(pin3, pin4))
        val event1 = down(3)
        val event2 = down(5).moveTo(10L.millisecondsToTimestamp(), 7f, 9f)

        hitResult.dispatchChanges(
            listOf(event1, event2),
            PointerEventPass.InitialDown,
            PointerEventPass.PreUp
        )

        inOrder(pin1.pointerInputHandler, pin2.pointerInputHandler) {
            verify(pin1.pointerInputHandler).invoke(event1, PointerEventPass.InitialDown)
            verify(pin2.pointerInputHandler).invoke(event1, PointerEventPass.InitialDown)
            verify(pin2.pointerInputHandler).invoke(event1, PointerEventPass.PreUp)
            verify(pin1.pointerInputHandler).invoke(event1, PointerEventPass.PreUp)
        }
        inOrder(pin3.pointerInputHandler, pin4.pointerInputHandler) {
            verify(pin3.pointerInputHandler).invoke(event2, PointerEventPass.InitialDown)
            verify(pin4.pointerInputHandler).invoke(event2, PointerEventPass.InitialDown)
            verify(pin4.pointerInputHandler).invoke(event2, PointerEventPass.PreUp)
            verify(pin3.pointerInputHandler).invoke(event2, PointerEventPass.PreUp)
        }
        verifyNoMoreInteractions(
            pin1.pointerInputHandler,
            pin2.pointerInputHandler,
            pin3.pointerInputHandler,
            pin4.pointerInputHandler
        )
    }

    @Test
    fun dispatchChanges_2BranchesWithSharedParent_eventsSplitCorrectlyAndCallOrderCorrect() {
        val parent = PointerInputNode()
        val child1 = PointerInputNode()
        val child2 = PointerInputNode()
        parent.pointerInputHandler = spy(MyPointerInputHandler())
        child1.pointerInputHandler = spy(MyPointerInputHandler())
        child2.pointerInputHandler = spy(MyPointerInputHandler())
        hitResult.addHitPath(3, listOf(parent, child1))
        hitResult.addHitPath(5, listOf(parent, child2))
        val event1 = down(3)
        val event2 = down(5).moveTo(10L.millisecondsToTimestamp(), 7f, 9f)

        hitResult.dispatchChanges(
            listOf(event1, event2),
            PointerEventPass.InitialDown,
            PointerEventPass.PreUp
        )

        // Verifies that event1 traverses in the correct order.
        inOrder(
            parent.pointerInputHandler,
            child1.pointerInputHandler
        ) {
            verify(parent.pointerInputHandler).invoke(event1, PointerEventPass.InitialDown)
            verify(child1.pointerInputHandler).invoke(event1, PointerEventPass.InitialDown)
            verify(child1.pointerInputHandler).invoke(event1, PointerEventPass.PreUp)
            verify(parent.pointerInputHandler).invoke(event1, PointerEventPass.PreUp)
        }

        // Verifies that event2 traverses in the correct order.
        inOrder(
            parent.pointerInputHandler,
            child2.pointerInputHandler
        ) {
            verify(parent.pointerInputHandler).invoke(event2, PointerEventPass.InitialDown)
            verify(child2.pointerInputHandler).invoke(event2, PointerEventPass.InitialDown)
            verify(child2.pointerInputHandler).invoke(event2, PointerEventPass.PreUp)
            verify(parent.pointerInputHandler).invoke(event2, PointerEventPass.PreUp)
        }

        // Verifies that the parent gets event1 before the child2 gets event2 on the way down, and
        // that child2 gets event2 before the parent gets event1 on the way up.
        inOrder(
            parent.pointerInputHandler,
            child2.pointerInputHandler
        ) {
            verify(parent.pointerInputHandler).invoke(event1, PointerEventPass.InitialDown)
            verify(child2.pointerInputHandler).invoke(event2, PointerEventPass.InitialDown)
            verify(child2.pointerInputHandler).invoke(event2, PointerEventPass.PreUp)
            verify(parent.pointerInputHandler).invoke(event1, PointerEventPass.PreUp)
        }

        // Verifies that the parent gets event2 before the child1 gets event1 on the way down, and
        // that child1 gets event1 before the parent gets event2 on the way up.
        inOrder(
            parent.pointerInputHandler,
            child1.pointerInputHandler
        ) {
            verify(parent.pointerInputHandler).invoke(event2, PointerEventPass.InitialDown)
            verify(child1.pointerInputHandler).invoke(event1, PointerEventPass.InitialDown)
            verify(child1.pointerInputHandler).invoke(event1, PointerEventPass.PreUp)
            verify(parent.pointerInputHandler).invoke(event2, PointerEventPass.PreUp)
        }

        verifyNoMoreInteractions(
            parent.pointerInputHandler,
            child1.pointerInputHandler,
            child2.pointerInputHandler
        )
    }

    @Test
    fun dispatchChanges_2PointersShareCompletePath_eventsSplitCorrectlyAndCallOrderCorrect() {
        val child1 = PointerInputNode()
        val child2 = PointerInputNode()
        child1.pointerInputHandler = spy(MyPointerInputHandler())
        child2.pointerInputHandler = spy(MyPointerInputHandler())
        hitResult.addHitPath(3, listOf(child1, child2))
        hitResult.addHitPath(5, listOf(child1, child2))
        val event1 = down(3)
        val event2 = down(5).moveTo(10L.millisecondsToTimestamp(), 7f, 9f)

        hitResult.dispatchChanges(
            listOf(event1, event2),
            PointerEventPass.InitialDown,
            PointerEventPass.PreUp
        )

        // Verifies that event1 traverses in the correct order
        inOrder(
            child1.pointerInputHandler,
            child2.pointerInputHandler
        ) {
            verify(child1.pointerInputHandler).invoke(event1, PointerEventPass.InitialDown)
            verify(child2.pointerInputHandler).invoke(event1, PointerEventPass.InitialDown)
            verify(child2.pointerInputHandler).invoke(event1, PointerEventPass.PreUp)
            verify(child1.pointerInputHandler).invoke(event1, PointerEventPass.PreUp)
        }

        // Verifies that event2 traverses in the correct order
        inOrder(
            child1.pointerInputHandler,
            child2.pointerInputHandler
        ) {
            verify(child1.pointerInputHandler).invoke(event2, PointerEventPass.InitialDown)
            verify(child2.pointerInputHandler).invoke(event2, PointerEventPass.InitialDown)
            verify(child2.pointerInputHandler).invoke(event2, PointerEventPass.PreUp)
            verify(child1.pointerInputHandler).invoke(event2, PointerEventPass.PreUp)
        }

        // Verifies that child1 gets event1 before child2 gets event2 on the way down, and
        // that child2 gets event2 before the child1 gets event1 on the way up.
        inOrder(
            child1.pointerInputHandler,
            child2.pointerInputHandler
        ) {
            verify(child1.pointerInputHandler).invoke(event1, PointerEventPass.InitialDown)
            verify(child2.pointerInputHandler).invoke(event2, PointerEventPass.InitialDown)
            verify(child2.pointerInputHandler).invoke(event2, PointerEventPass.PreUp)
            verify(child1.pointerInputHandler).invoke(event1, PointerEventPass.PreUp)
        }

        // Verifies that the child1 gets event2 before child2 gets event1 on the way down, and
        // that child2 gets event1 before the child1 gets event2 on the way up.
        inOrder(
            child1.pointerInputHandler,
            child2.pointerInputHandler
        ) {
            verify(child1.pointerInputHandler).invoke(event2, PointerEventPass.InitialDown)
            verify(child2.pointerInputHandler).invoke(event1, PointerEventPass.InitialDown)
            verify(child2.pointerInputHandler).invoke(event1, PointerEventPass.PreUp)
            verify(child1.pointerInputHandler).invoke(event2, PointerEventPass.PreUp)
        }

        verifyNoMoreInteractions(
            child1.pointerInputHandler,
            child2.pointerInputHandler
        )
    }

    @Test
    fun dispatchChanges_noNodes_nothingChanges() {
        val result = hitResult.dispatchChanges(listOf(down(5)), PointerEventPass.InitialDown)

        assertThat(result).isEqualTo(listOf(down(5)))
    }

    @Test
    fun dispatchChanges_hitResultHasSingleMatch_changesAreUpdatedCorrectly() {
        val pin1 = PointerInputNode()
        pin1.pointerInputHandler = spy(MyPointerInputHandler().apply {
            modifyBlock = { change, _ ->
                change.consumeDownChange()
            }
        })
        hitResult.addHitPath(13, listOf(pin1))

        val result = hitResult.dispatchChanges(listOf(down(13)), PointerEventPass.InitialDown)

        assertThat(result).isEqualTo(listOf(down(13).consumeDownChange()))
    }

    @Test
    fun dispatchChanges_hitResultHasMultipleMatchesAndDownAndUpPaths_changesAreUpdatedCorrectly() {
        val pin1 = PointerInputNode()
        val pin2 = PointerInputNode()
        val pin3 = PointerInputNode()
        pin1.pointerInputHandler = spy(MyPointerInputHandler().apply {
            modifyBlock = { change, pass ->
                val yConsume = if (pass == PointerEventPass.InitialDown) 2f else 64f
                change.consumePositionChange(0.px, yConsume.px)
            }
        })
        pin2.pointerInputHandler = spy(MyPointerInputHandler().apply {
            modifyBlock = { change, pass ->
                val yConsume = if (pass == PointerEventPass.InitialDown) 4f else 32f
                change.consumePositionChange(0.px, yConsume.px)
            }
        })
        pin3.pointerInputHandler = spy(MyPointerInputHandler().apply {
            modifyBlock = { change, pass ->
                val yConsume = if (pass == PointerEventPass.InitialDown) 8f else 16f
                change.consumePositionChange(0.px, yConsume.px)
            }
        })
        hitResult.addHitPath(13, listOf(pin1, pin2, pin3))
        val change = down(13).moveTo(10L.millisecondsToTimestamp(), 0f, 130f)

        val result = hitResult.dispatchChanges(
            listOf(change),
            PointerEventPass.InitialDown,
            PointerEventPass.PreUp
        )

        verify(pin1.pointerInputHandler).invoke(
            change, PointerEventPass.InitialDown
        )
        verify(pin2.pointerInputHandler).invoke(
            change.consumePositionChange(0.px, 2.px),
            PointerEventPass.InitialDown
        )
        verify(pin3.pointerInputHandler).invoke(
            change.consumePositionChange(0.px, 6.px), // 2 + 4
            PointerEventPass.InitialDown
        )
        verify(pin3.pointerInputHandler).invoke(
            change.consumePositionChange(0.px, 14.px), // 2 + 4 + 8
            PointerEventPass.PreUp
        )
        verify(pin2.pointerInputHandler).invoke(
            change.consumePositionChange(0.px, 30.px), // 2 + 4 + 8 + 16
            PointerEventPass.PreUp
        )
        verify(pin1.pointerInputHandler).invoke(
            change.consumePositionChange(0.px, 62.px), // 2 + 4 + 8 + 16 + 32
            PointerEventPass.PreUp
        )
        assertThat(result)
            .isEqualTo(listOf(change.consumePositionChange(0.px, 126.px))) // 2 + 4 + 8 + 16 + 32 + 64
    }

    @Test
    fun dispatchChanges_2IndependentBranchesFromRoot_changesAreUpdatedCorrectly() {
        val pin1 = PointerInputNode()
        val pin2 = PointerInputNode()
        val pin3 = PointerInputNode()
        val pin4 = PointerInputNode()
        pin1.pointerInputHandler = spy(MyPointerInputHandler().apply {
            modifyBlock = { change, pass ->
                val yConsume = if (pass == PointerEventPass.InitialDown) 2f else 12f
                change.consumePositionChange(0.px, yConsume.px)
            }
        })
        pin2.pointerInputHandler = spy(MyPointerInputHandler().apply {
            modifyBlock = { change, pass ->
                val yConsume = if (pass == PointerEventPass.InitialDown) 3f else 6f
                change.consumePositionChange(0.px, yConsume.px)
            }
        })
        pin3.pointerInputHandler = spy(MyPointerInputHandler().apply {
            modifyBlock = { change, pass ->
                val yConsume = if (pass == PointerEventPass.InitialDown) -2f else -12f
                change.consumePositionChange(0.px, yConsume.px)
            }
        })
        pin4.pointerInputHandler = spy(MyPointerInputHandler().apply {
            modifyBlock = { change, pass ->
                val yConsume = if (pass == PointerEventPass.InitialDown) -3f else -6f
                change.consumePositionChange(0.px, yConsume.px)
            }
        })
        hitResult.addHitPath(3, listOf(pin1, pin2))
        hitResult.addHitPath(5, listOf(pin3, pin4))
        val event1 = down(3).moveTo(10L.millisecondsToTimestamp(), 0f, 24f)
        val event2 = down(5).moveTo(10L.millisecondsToTimestamp(), 0f, -24f)

        val result = hitResult.dispatchChanges(
            listOf(event1, event2),
            PointerEventPass.InitialDown,
            PointerEventPass.PreUp
        )

        verify(pin1.pointerInputHandler).invoke(
            event1,
            PointerEventPass.InitialDown
        )
        verify(pin2.pointerInputHandler).invoke(
            event1.consumePositionChange(0.px, 2.px),
            PointerEventPass.InitialDown
        )
        verify(pin2.pointerInputHandler).invoke(
            event1.consumePositionChange(0.px, 5.px),
            PointerEventPass.PreUp
        )
        verify(pin1.pointerInputHandler).invoke(
            event1.consumePositionChange(0.px, 11.px),
            PointerEventPass.PreUp
        )

        verify(pin3.pointerInputHandler).invoke(
            event2,
            PointerEventPass.InitialDown
        )
        verify(pin4.pointerInputHandler).invoke(
            event2.consumePositionChange(0.px, -2.px),
            PointerEventPass.InitialDown
        )
        verify(pin4.pointerInputHandler).invoke(
            event2.consumePositionChange(0.px, -5.px),
            PointerEventPass.PreUp
        )
        verify(pin3.pointerInputHandler).invoke(
            event2.consumePositionChange(0.px, (-11).px),
            PointerEventPass.PreUp
        )

        assertThat(result).hasSize(2)
        assertThat(result).contains(event1.consumePositionChange(0.px, 23.px))
        assertThat(result).contains(event2.consumePositionChange(0.px, (-23).px))
    }

    @Test
    fun dispatchChanges_2BranchesWithSharedParent_changesAreUpdatedCorrectly() {
        val parent = PointerInputNode()
        val child1 = PointerInputNode()
        val child2 = PointerInputNode()
        parent.pointerInputHandler = spy(MyPointerInputHandler().apply {
            modifyBlock = { change, pass ->
                val yConsume = if (pass == PointerEventPass.InitialDown) 2 else 3
                change.consumePositionChange(
                    0.px,
                    (change.positionChange().y.value.toInt() / yConsume).px
                )
            }
        })
        child1.pointerInputHandler = spy(MyPointerInputHandler().apply {
            modifyBlock = { change, pass ->
                val yConsume = if (pass == PointerEventPass.InitialDown) 5 else 7
                change.consumePositionChange(
                    0.px,
                    (change.positionChange().y.value.toInt() / yConsume).px
                )
            }
        })
        child2.pointerInputHandler = spy(MyPointerInputHandler().apply {
            modifyBlock = { change, pass ->
                val yConsume = if (pass == PointerEventPass.InitialDown) 11 else 13
                change.consumePositionChange(
                    0.px,
                    (change.positionChange().y.value.toInt() / yConsume).px
                )
            }
        })
        hitResult.addHitPath(3, listOf(parent, child1))
        hitResult.addHitPath(5, listOf(parent, child2))
        val event1 = down(3).moveTo(10L.millisecondsToTimestamp(), 0f, 1000f)
        val event2 = down(5).moveTo(10L.millisecondsToTimestamp(), 0f, -1000f)

        val result = hitResult.dispatchChanges(
            listOf(event1, event2),
            PointerEventPass.InitialDown,
            PointerEventPass.PreUp
        )

        verify(parent.pointerInputHandler).invoke(
            event1,
            PointerEventPass.InitialDown
        )
        verify(child1.pointerInputHandler).invoke(
            event1.consumePositionChange(0.px, 500.px),
            PointerEventPass.InitialDown
        )
        verify(child1.pointerInputHandler).invoke(
            event1.consumePositionChange(0.px, 600.px),
            PointerEventPass.PreUp
        )
        verify(parent.pointerInputHandler).invoke(
            event1.consumePositionChange(0.px, 657.px),
            PointerEventPass.PreUp
        )

        verify(parent.pointerInputHandler).invoke(
            event2,
            PointerEventPass.InitialDown
        )
        verify(child2.pointerInputHandler).invoke(
            event2.consumePositionChange(0.px, -500.px),
            PointerEventPass.InitialDown
        )
        verify(child2.pointerInputHandler).invoke(
            event2.consumePositionChange(0.px, -545.px),
            PointerEventPass.PreUp
        )
        verify(parent.pointerInputHandler).invoke(
            event2.consumePositionChange(0.px, -580.px),
            PointerEventPass.PreUp
        )

        assertThat(result).hasSize(2)
        assertThat(result).contains(event1.consumePositionChange(0.px, 771.px))
        assertThat(result).contains(event2.consumePositionChange(0.px, -720.px))
    }

    @Test
    fun dispatchChanges_2PointersShareCompletePath_changesAreUpdatedCorrectly() {
        val child1 = PointerInputNode()
        val child2 = PointerInputNode()
        child1.pointerInputHandler = spy(MyPointerInputHandler().apply {
            modifyBlock = { change, pass ->
                val yConsume = if (pass == PointerEventPass.InitialDown) 2 else 3
                change.consumePositionChange(
                    0.px,
                    (change.positionChange().y.value.toInt() / yConsume).px
                )
            }
        })
        child2.pointerInputHandler = spy(MyPointerInputHandler().apply {
            modifyBlock = { change, pass ->
                val yConsume = if (pass == PointerEventPass.InitialDown) 5 else 7
                change.consumePositionChange(
                    0.px,
                    (change.positionChange().y.value.toInt() / yConsume).px
                )
            }
        })
        hitResult.addHitPath(3, listOf(child1, child2))
        hitResult.addHitPath(5, listOf(child1, child2))
        val event1 = down(3).moveTo(10L.millisecondsToTimestamp(), 0f, 1000f)
        val event2 = down(5).moveTo(10L.millisecondsToTimestamp(), 0f, -1000f)

        val result = hitResult.dispatchChanges(
            listOf(event1, event2),
            PointerEventPass.InitialDown,
            PointerEventPass.PreUp
        )

        verify(child1.pointerInputHandler).invoke(
            event1,
            PointerEventPass.InitialDown
        )
        verify(child1.pointerInputHandler).invoke(
            event2,
            PointerEventPass.InitialDown
        )
        verify(child2.pointerInputHandler).invoke(
            event1.consumePositionChange(0.px, 500.px),
            PointerEventPass.InitialDown
        )
        verify(child2.pointerInputHandler).invoke(
            event2.consumePositionChange(0.px, -500.px),
            PointerEventPass.InitialDown
        )

        verify(child2.pointerInputHandler).invoke(
            event1.consumePositionChange(0.px, 600.px),
            PointerEventPass.PreUp
        )
        verify(child2.pointerInputHandler).invoke(
            event2.consumePositionChange(0.px, -600.px),
            PointerEventPass.PreUp
        )
        verify(child1.pointerInputHandler).invoke(
            event1.consumePositionChange(0.px, 657.px),
            PointerEventPass.PreUp
        )
        verify(child1.pointerInputHandler).invoke(
            event2.consumePositionChange(0.px, -657.px),
            PointerEventPass.PreUp
        )

        assertThat(result).hasSize(2)
        assertThat(result).contains(event1.consumePositionChange(0.px, 771.px))
        assertThat(result).contains(event2.consumePositionChange(0.px, -771.px))
    }

    @Test
    fun removeDetachedPointerInputNodes_noNodes_hitResultJustHasRootAndDoesNotCrash() {
        val throwable = catchThrowable {
            hitResult.removeDetachedPointerInputNodes()
        }

        assertThat(throwable).isNull()
        assertThat(areEqual(hitResult.root, Node()))
    }

    @Test
    fun removeDetachedPointerInputNodes_complexNothingDetached_nothingRemoved() {

        // Arrange.

        val pin1 = PointerInputNode()

        val pin2 = PointerInputNode()
        val pin3 = PointerInputNode().apply {
            emitInsertAt(0, pin2)
        }

        val pin4 = PointerInputNode()
        val pin5 = PointerInputNode().apply {
            emitInsertAt(0, pin4)
        }
        val pin6 = PointerInputNode().apply {
            emitInsertAt(0, pin5)
        }

        val pin7 = PointerInputNode()
        val pin8 = PointerInputNode()
        val layoutNode = LayoutNode().apply {
            emitInsertAt(0, pin7)
            emitInsertAt(1, pin8)
        }
        val pin9 = PointerInputNode().apply {
            emitInsertAt(0, layoutNode)
        }

        compositionRoot.emitInsertAt(0, pin1)
        compositionRoot.emitInsertAt(1, pin3)
        compositionRoot.emitInsertAt(2, pin6)
        compositionRoot.emitInsertAt(3, pin9)

        hitResult.addHitPath(1, listOf(pin1))
        hitResult.addHitPath(2, listOf(pin3, pin2))
        hitResult.addHitPath(3, listOf(pin6, pin5, pin4))
        hitResult.addHitPath(4, listOf(pin9, pin7))
        hitResult.addHitPath(5, listOf(pin9, pin8))

        // Act.

        hitResult.removeDetachedPointerInputNodes()

        // Assert.

        val expectedRoot = Node().apply {
            children.add(Node(pin1).apply {
                pointerIds.add(1)
            })
            children.add(Node(pin3).apply {
                pointerIds.add(2)
                children.add(Node(pin2).apply {
                    pointerIds.add(2)
                })
            })
            children.add(Node(pin6).apply {
                pointerIds.add(3)
                children.add(Node(pin5).apply {
                    pointerIds.add(3)
                    children.add(Node(pin4).apply {
                        pointerIds.add(3)
                    })
                })
            })
            children.add(Node(pin9).apply {
                pointerIds.add(4)
                pointerIds.add(5)
                children.add(Node(pin7).apply {
                    pointerIds.add(4)
                })
                children.add(Node(pin8).apply {
                    pointerIds.add(5)
                })
            })
        }
        assertThat(areEqual(hitResult.root, expectedRoot)).isTrue()
    }

    //  compositionRoot, root -> middle -> leaf
    @Test
    fun removeDetachedPointerInputNodes_1PathRootDetached_fullPathRemoved() {
        val leaf = PointerInputNode()
        val middle = PointerInputNode().apply {
            emitInsertAt(0, leaf)
        }
        val root = PointerInputNode().apply {
            emitInsertAt(0, middle)
        }
        hitResult.addHitPath(0, listOf(root, middle, leaf))

        hitResult.removeDetachedPointerInputNodes()

        assertThat(areEqual(hitResult.root, Node())).isTrue()
    }

    //  compositionRoot -> root, middle -> child
    @Test
    fun removeDetachedPointerInputNodes_1PathMiddleDetached_itAndAncestorsRemoved() {
        val child = PointerInputNode()
        val middle = PointerInputNode().apply {
            emitInsertAt(0, child)
        }
        val root = PointerInputNode()
        compositionRoot.add(root)
        hitResult.addHitPath(0, listOf(root, middle, child))

        hitResult.removeDetachedPointerInputNodes()

        val expectedRoot = Node().apply {
            children.add(Node(root).apply {
                pointerIds.add(0)
            })
        }

        assertThat(areEqual(hitResult.root, expectedRoot)).isTrue()
    }

    //  compositionRoot -> root -> middle, leaf
    @Test
    fun removeDetachedPointerInputNodes_1PathLeafDetached_justLeafRemoved() {
        val leaf = PointerInputNode()
        val middle = PointerInputNode()
        val root = PointerInputNode().apply {
            emitInsertAt(0, middle)
        }
        compositionRoot.add(root)
        hitResult.addHitPath(0, listOf(root, middle, leaf))

        hitResult.removeDetachedPointerInputNodes()

        val expectedRoot = Node().apply {
            children.add(Node(root).apply {
                pointerIds.add(0)
                children.add(Node(middle).apply {
                    pointerIds.add(0)
                })
            })
        }

        assertThat(areEqual(hitResult.root, expectedRoot)).isTrue()
    }

    //  compositionRoot -> root1 -> middle1 -> leaf1
    //  compositionRoot -> root2 -> middle2 -> leaf2
    //  compositionRoot, root3 -> middle3 -> leaf3
    @Test
    fun removeDetachedPointerInputNodes_3Roots1Detached_correctRootAndAncestorsRemoved() {
        val leaf1 = PointerInputNode()
        val middle1 = PointerInputNode().apply {
            emitInsertAt(0, leaf1)
        }
        val root1 = PointerInputNode().apply {
            emitInsertAt(0, middle1)
        }

        val leaf2 = PointerInputNode()
        val middle2 = PointerInputNode().apply {
            emitInsertAt(0, leaf2)
        }
        val root2 = PointerInputNode().apply {
            emitInsertAt(0, middle2)
        }

        val leaf3 = PointerInputNode()
        val middle3 = PointerInputNode().apply {
            emitInsertAt(0, leaf3)
        }
        val root3 = PointerInputNode().apply {
            emitInsertAt(0, middle3)
        }

        compositionRoot.emitInsertAt(0, root1)
        compositionRoot.emitInsertAt(1, root2)

        hitResult.addHitPath(3, listOf(root1, middle1, leaf1))
        hitResult.addHitPath(5, listOf(root2, middle2, leaf2))
        hitResult.addHitPath(7, listOf(root3, middle3, leaf3))

        hitResult.removeDetachedPointerInputNodes()

        val expectedRoot = Node().apply {
            children.add(Node(root1).apply {
                pointerIds.add(3)
                children.add(Node(middle1).apply {
                    pointerIds.add(3)
                    children.add(Node(leaf1).apply {
                        pointerIds.add(3)
                    })
                })
            })
            children.add(Node(root2).apply {
                pointerIds.add(5)
                children.add(Node(middle2).apply {
                    pointerIds.add(5)
                    children.add(Node(leaf2).apply {
                        pointerIds.add(5)
                    })
                })
            })
        }

        assertThat(areEqual(hitResult.root, expectedRoot)).isTrue()
    }

    //  compositionRoot -> root1, middle1 -> leaf1
    //  compositionRoot -> root2 -> middle2 -> leaf2
    //  compositionRoot -> root3 -> middle3 -> leaf3
    @Test
    fun removeDetachedPointerInputNodes_3Roots1MiddleDetached_correctMiddleAndAncestorsRemoved() {
        val leaf1 = PointerInputNode()
        val middle1 = PointerInputNode().apply {
            emitInsertAt(0, leaf1)
        }
        val root1 = PointerInputNode()

        val leaf2 = PointerInputNode()
        val middle2 = PointerInputNode().apply {
            emitInsertAt(0, leaf2)
        }
        val root2 = PointerInputNode().apply {
            emitInsertAt(0, middle2)
        }

        val leaf3 = PointerInputNode()
        val middle3 = PointerInputNode().apply {
            emitInsertAt(0, leaf3)
        }
        val root3 = PointerInputNode().apply {
            emitInsertAt(0, middle3)
        }

        compositionRoot.emitInsertAt(0, root1)
        compositionRoot.emitInsertAt(1, root2)
        compositionRoot.emitInsertAt(2, root3)

        hitResult.addHitPath(3, listOf(root1, middle1, leaf1))
        hitResult.addHitPath(5, listOf(root2, middle2, leaf2))
        hitResult.addHitPath(7, listOf(root3, middle3, leaf3))

        hitResult.removeDetachedPointerInputNodes()

        val expectedRoot = Node().apply {
            children.add(Node(root1).apply {
                pointerIds.add(3)
            })
            children.add(Node(root2).apply {
                pointerIds.add(5)
                children.add(Node(middle2).apply {
                    pointerIds.add(5)
                    children.add(Node(leaf2).apply {
                        pointerIds.add(5)
                    })
                })
            })
            children.add(Node(root3).apply {
                pointerIds.add(7)
                children.add(Node(middle3).apply {
                    pointerIds.add(7)
                    children.add(Node(leaf3).apply {
                        pointerIds.add(7)
                    })
                })
            })
        }

        assertThat(areEqual(hitResult.root, expectedRoot)).isTrue()
    }

    //  compositionRoot -> root1 -> middle1 -> leaf1
    //  compositionRoot -> root2 -> middle2, leaf2
    //  compositionRoot -> root3 -> middle3 -> leaf3
    @Test
    fun removeDetachedPointerInputNodes_3Roots1LeafDetached_correctLeafRemoved() {
        val leaf1 = PointerInputNode()
        val middle1 = PointerInputNode().apply {
            emitInsertAt(0, leaf1)
        }
        val root1 = PointerInputNode().apply {
            emitInsertAt(0, middle1)
        }

        val leaf2 = PointerInputNode()
        val middle2 = PointerInputNode()
        val root2 = PointerInputNode().apply {
            emitInsertAt(0, middle2)
        }

        val leaf3 = PointerInputNode()
        val middle3 = PointerInputNode().apply {
            emitInsertAt(0, leaf3)
        }
        val root3 = PointerInputNode().apply {
            emitInsertAt(0, middle3)
        }

        compositionRoot.emitInsertAt(0, root1)
        compositionRoot.emitInsertAt(1, root2)
        compositionRoot.emitInsertAt(2, root3)

        hitResult.addHitPath(3, listOf(root1, middle1, leaf1))
        hitResult.addHitPath(5, listOf(root2, middle2, leaf2))
        hitResult.addHitPath(7, listOf(root3, middle3, leaf3))

        hitResult.removeDetachedPointerInputNodes()

        val expectedRoot = Node().apply {
            children.add(Node(root1).apply {
                pointerIds.add(3)
                children.add(Node(middle1).apply {
                    pointerIds.add(3)
                    children.add(Node(leaf1).apply {
                        pointerIds.add(3)
                    })
                })
            })
            children.add(Node(root2).apply {
                pointerIds.add(5)
                children.add(Node(middle2).apply {
                    pointerIds.add(5)
                })
            })
            children.add(Node(root3).apply {
                pointerIds.add(7)
                children.add(Node(middle3).apply {
                    pointerIds.add(7)
                    children.add(Node(leaf3).apply {
                        pointerIds.add(7)
                    })
                })
            })
        }

        assertThat(areEqual(hitResult.root, expectedRoot)).isTrue()
    }

    //  compositionRoot, root1 -> middle1 -> leaf1
    //  compositionRoot -> root2 -> middle2 -> leaf2
    //  compositionRoot, root3 -> middle3 -> leaf3
    @Test
    fun removeDetachedPointerInputNodes_3Roots2Detached_correct2RootsAndAncestorsRemoved() {
        val leaf1 = PointerInputNode()
        val middle1 = PointerInputNode().apply {
            emitInsertAt(0, leaf1)
        }
        val root1 = PointerInputNode().apply {
            emitInsertAt(0, middle1)
        }

        val leaf2 = PointerInputNode()
        val middle2 = PointerInputNode().apply {
            emitInsertAt(0, leaf2)
        }
        val root2 = PointerInputNode().apply {
            emitInsertAt(0, middle2)
        }

        val leaf3 = PointerInputNode()
        val middle3 = PointerInputNode().apply {
            emitInsertAt(0, leaf3)
        }
        val root3 = PointerInputNode().apply {
            emitInsertAt(0, middle3)
        }

        compositionRoot.emitInsertAt(0, root2)

        hitResult.addHitPath(3, listOf(root1, middle1, leaf1))
        hitResult.addHitPath(5, listOf(root2, middle2, leaf2))
        hitResult.addHitPath(7, listOf(root3, middle3, leaf3))

        hitResult.removeDetachedPointerInputNodes()

        val expectedRoot = Node().apply {
            children.add(Node(root2).apply {
                pointerIds.add(5)
                children.add(Node(middle2).apply {
                    pointerIds.add(5)
                    children.add(Node(leaf2).apply {
                        pointerIds.add(5)
                    })
                })
            })
        }

        assertThat(areEqual(hitResult.root, expectedRoot)).isTrue()
    }

    //  compositionRoot -> root1, middle1 -> leaf1
    //  compositionRoot -> root2, middle2 -> leaf2
    //  compositionRoot -> root3 -> middle3 -> leaf3
    @Test
    fun removeDetachedPointerInputNodes_3Roots2MiddlesDetached_correct2NodesAndAncestorsRemoved() {
        val leaf1 = PointerInputNode()
        val middle1 = PointerInputNode().apply {
            emitInsertAt(0, leaf1)
        }
        val root1 = PointerInputNode()

        val leaf2 = PointerInputNode()
        val middle2 = PointerInputNode().apply {
            emitInsertAt(0, leaf2)
        }
        val root2 = PointerInputNode()

        val leaf3 = PointerInputNode()
        val middle3 = PointerInputNode().apply {
            emitInsertAt(0, leaf3)
        }
        val root3 = PointerInputNode().apply {
            emitInsertAt(0, middle3)
        }

        compositionRoot.emitInsertAt(0, root1)
        compositionRoot.emitInsertAt(1, root2)
        compositionRoot.emitInsertAt(2, root3)

        hitResult.addHitPath(3, listOf(root1, middle1, leaf1))
        hitResult.addHitPath(5, listOf(root2, middle2, leaf2))
        hitResult.addHitPath(7, listOf(root3, middle3, leaf3))

        hitResult.removeDetachedPointerInputNodes()

        val expectedRoot = Node().apply {
            children.add(Node(root1).apply {
                pointerIds.add(3)
            })
            children.add(Node(root2).apply {
                pointerIds.add(5)
            })
            children.add(Node(root3).apply {
                pointerIds.add(7)
                children.add(Node(middle3).apply {
                    pointerIds.add(7)
                    children.add(Node(leaf3).apply {
                        pointerIds.add(7)
                    })
                })
            })
        }

        assertThat(areEqual(hitResult.root, expectedRoot)).isTrue()
    }

    //  compositionRoot -> root1 -> middle1 -> leaf1
    //  compositionRoot -> root2 -> middle2, leaf2
    //  compositionRoot -> root3 -> middle3, leaf3
    @Test
    fun removeDetachedPointerInputNodes_3Roots2LeafsDetached_correct2LeafsRemoved() {
        val leaf1 = PointerInputNode()
        val middle1 = PointerInputNode().apply {
            emitInsertAt(0, leaf1)
        }
        val root1 = PointerInputNode().apply {
            emitInsertAt(0, middle1)
        }

        val leaf2 = PointerInputNode()
        val middle2 = PointerInputNode()
        val root2 = PointerInputNode().apply {
            emitInsertAt(0, middle2)
        }

        val leaf3 = PointerInputNode()
        val middle3 = PointerInputNode()
        val root3 = PointerInputNode().apply {
            emitInsertAt(0, middle3)
        }

        compositionRoot.emitInsertAt(0, root1)
        compositionRoot.emitInsertAt(1, root2)
        compositionRoot.emitInsertAt(2, root3)

        hitResult.addHitPath(3, listOf(root1, middle1, leaf1))
        hitResult.addHitPath(5, listOf(root2, middle2, leaf2))
        hitResult.addHitPath(7, listOf(root3, middle3, leaf3))

        hitResult.removeDetachedPointerInputNodes()

        val expectedRoot = Node().apply {
            children.add(Node(root1).apply {
                pointerIds.add(3)
                children.add(Node(middle1).apply {
                    pointerIds.add(3)
                    children.add(Node(leaf1).apply {
                        pointerIds.add(3)
                    })
                })
            })
            children.add(Node(root2).apply {
                pointerIds.add(5)
                children.add(Node(middle2).apply {
                    pointerIds.add(5)
                })
            })
            children.add(Node(root3).apply {
                pointerIds.add(7)
                children.add(Node(middle3).apply {
                    pointerIds.add(7)
                })
            })
        }

        assertThat(areEqual(hitResult.root, expectedRoot)).isTrue()
    }

    //  compositionRoot, root1 -> middle1 -> leaf1
    //  compositionRoot, root2 -> middle2 -> leaf2
    //  compositionRoot, root3 -> middle3 -> leaf3
    @Test
    fun removeDetachedPointerInputNodes_3Roots3Detached_all3RootsAndAncestorsRemoved() {
        val leaf1 = PointerInputNode()
        val middle1 = PointerInputNode().apply {
            emitInsertAt(0, leaf1)
        }
        val root1 = PointerInputNode().apply {
            emitInsertAt(0, middle1)
        }

        val leaf2 = PointerInputNode()
        val middle2 = PointerInputNode().apply {
            emitInsertAt(0, leaf2)
        }
        val root2 = PointerInputNode().apply {
            emitInsertAt(0, middle2)
        }

        val leaf3 = PointerInputNode()
        val middle3 = PointerInputNode().apply {
            emitInsertAt(0, leaf3)
        }
        val root3 = PointerInputNode().apply {
            emitInsertAt(0, middle3)
        }

        hitResult.addHitPath(3, listOf(root1, middle1, leaf1))
        hitResult.addHitPath(5, listOf(root2, middle2, leaf2))
        hitResult.addHitPath(7, listOf(root3, middle3, leaf3))

        hitResult.removeDetachedPointerInputNodes()

        val expectedRoot = Node()

        assertThat(areEqual(hitResult.root, expectedRoot)).isTrue()
    }

    //  compositionRoot -> root1, middle1 -> leaf1
    //  compositionRoot -> root2, middle2 -> leaf2
    //  compositionRoot -> root3, middle3 -> leaf3
    @Test
    fun removeDetachedPointerInputNodes_3Roots3MiddlesDetached_all3MiddlesAndAncestorsRemoved() {
        val leaf1 = PointerInputNode()
        val middle1 = PointerInputNode().apply {
            emitInsertAt(0, leaf1)
        }
        val root1 = PointerInputNode()

        val leaf2 = PointerInputNode()
        val middle2 = PointerInputNode().apply {
            emitInsertAt(0, leaf2)
        }
        val root2 = PointerInputNode()

        val leaf3 = PointerInputNode()
        val middle3 = PointerInputNode().apply {
            emitInsertAt(0, leaf3)
        }
        val root3 = PointerInputNode()

        compositionRoot.emitInsertAt(0, root1)
        compositionRoot.emitInsertAt(1, root2)
        compositionRoot.emitInsertAt(2, root3)

        hitResult.addHitPath(3, listOf(root1, middle1, leaf1))
        hitResult.addHitPath(5, listOf(root2, middle2, leaf2))
        hitResult.addHitPath(7, listOf(root3, middle3, leaf3))

        hitResult.removeDetachedPointerInputNodes()

        val expectedRoot = Node().apply {
            children.add(Node(root1).apply {
                pointerIds.add(3)
            })
            children.add(Node(root2).apply {
                pointerIds.add(5)
            })
            children.add(Node(root3).apply {
                pointerIds.add(7)
            })
        }

        assertThat(areEqual(hitResult.root, expectedRoot)).isTrue()
    }

    //  compositionRoot -> root1 -> middle1, leaf1
    //  compositionRoot -> root2 -> middle2, leaf2
    //  compositionRoot -> root3 -> middle3, leaf3
    @Test
    fun removeDetachedPointerInputNodes_3Roots3LeafsDetached_all3LeafsRemoved() {
        val leaf1 = PointerInputNode()
        val middle1 = PointerInputNode()
        val root1 = PointerInputNode().apply {
            emitInsertAt(0, middle1)
        }

        val leaf2 = PointerInputNode()
        val middle2 = PointerInputNode()
        val root2 = PointerInputNode().apply {
            emitInsertAt(0, middle2)
        }

        val leaf3 = PointerInputNode()
        val middle3 = PointerInputNode()
        val root3 = PointerInputNode().apply {
            emitInsertAt(0, middle3)
        }

        compositionRoot.emitInsertAt(0, root1)
        compositionRoot.emitInsertAt(1, root2)
        compositionRoot.emitInsertAt(2, root3)

        hitResult.addHitPath(3, listOf(root1, middle1, leaf1))
        hitResult.addHitPath(5, listOf(root2, middle2, leaf2))
        hitResult.addHitPath(7, listOf(root3, middle3, leaf3))

        hitResult.removeDetachedPointerInputNodes()

        val expectedRoot = Node().apply {
            children.add(Node(root1).apply {
                pointerIds.add(3)
                children.add(Node(middle1).apply {
                    pointerIds.add(3)
                })
            })
            children.add(Node(root2).apply {
                pointerIds.add(5)
                children.add(Node(middle2).apply {
                    pointerIds.add(5)
                })
            })
            children.add(Node(root3).apply {
                pointerIds.add(7)
                children.add(Node(middle3).apply {
                    pointerIds.add(7)
                })
            })
        }

        assertThat(areEqual(hitResult.root, expectedRoot)).isTrue()
    }

    //  compositionRoot, root1 -> middle1 -> leaf1
    //  compositionRoot -> root2, middle2, leaf2
    //  compositionRoot -> root3 -> middle3, leaf3
    @Test
    fun removeDetachedPointerInputNodes_3RootsStaggeredDetached_correctPathsRemoved() {
        val leaf1 = PointerInputNode()
        val middle1 = PointerInputNode().apply {
            emitInsertAt(0, leaf1)
        }
        val root1 = PointerInputNode().apply {
            emitInsertAt(0, middle1)
        }

        val leaf2 = PointerInputNode()
        val middle2 = PointerInputNode().apply {
            emitInsertAt(0, leaf2)
        }
        val root2 = PointerInputNode()

        val leaf3 = PointerInputNode()
        val middle3 = PointerInputNode()
        val root3 = PointerInputNode().apply {
            emitInsertAt(0, middle3)
        }

        compositionRoot.emitInsertAt(0, root2)
        compositionRoot.emitInsertAt(1, root3)

        hitResult.addHitPath(3, listOf(root1, middle1, leaf1))
        hitResult.addHitPath(5, listOf(root2, middle2, leaf2))
        hitResult.addHitPath(7, listOf(root3, middle3, leaf3))

        hitResult.removeDetachedPointerInputNodes()

        val expectedRoot = Node().apply {
            children.add(Node(root2).apply {
                pointerIds.add(5)
            })
            children.add(Node(root3).apply {
                pointerIds.add(7)
                children.add(Node(middle3).apply {
                    pointerIds.add(7)
                })
            })
        }

        assertThat(areEqual(hitResult.root, expectedRoot)).isTrue()
    }

    //  compositionRoot, root -> layoutNode -> middle1 -> leaf1
    //                           layoutNode -> middle2 -> leaf2
    //                           layoutNode -> middle3 -> leaf3
    @Test
    fun removeDetachedPointerInputNodes_rootWith3MiddlesDetached_allRemoved() {
        val leaf1 = PointerInputNode()
        val middle1 = PointerInputNode().apply {
            emitInsertAt(0, leaf1)
        }

        val leaf2 = PointerInputNode()
        val middle2 = PointerInputNode().apply {
            emitInsertAt(0, leaf2)
        }

        val leaf3 = PointerInputNode()
        val middle3 = PointerInputNode().apply {
            emitInsertAt(0, leaf3)
        }

        val layoutNode = LayoutNode().apply {
            emitInsertAt(0, middle1)
            emitInsertAt(1, middle2)
            emitInsertAt(2, middle3)
        }

        val root = PointerInputNode().apply {
            emitInsertAt(0, layoutNode)
        }

        hitResult.addHitPath(3, listOf(root, middle1, leaf1))
        hitResult.addHitPath(5, listOf(root, middle2, leaf2))
        hitResult.addHitPath(7, listOf(root, middle3, leaf3))

        hitResult.removeDetachedPointerInputNodes()

        val expectedRoot = Node()

        assertThat(areEqual(hitResult.root, expectedRoot)).isTrue()
    }

    //  compositionRoot -> root -> layoutNode -> middle1 -> leaf1
    //                             layoutNode -> middle2 -> leaf2
    //                             layoutNode, middle3 -> leaf3
    @Test
    fun removeDetachedPointerInputNodes_rootWith3Middles1Detached_correctMiddleRemoved() {
        val leaf1 = PointerInputNode()
        val middle1 = PointerInputNode().apply {
            emitInsertAt(0, leaf1)
        }

        val leaf2 = PointerInputNode()
        val middle2 = PointerInputNode().apply {
            emitInsertAt(0, leaf2)
        }

        val leaf3 = PointerInputNode()
        val middle3 = PointerInputNode().apply {
            emitInsertAt(0, leaf3)
        }

        val layoutNode = LayoutNode().apply {
            emitInsertAt(0, middle1)
            emitInsertAt(1, middle2)
        }

        val root = PointerInputNode().apply {
            emitInsertAt(0, layoutNode)
        }

        compositionRoot.emitInsertAt(0, root)

        hitResult.addHitPath(3, listOf(root, middle1, leaf1))
        hitResult.addHitPath(5, listOf(root, middle2, leaf2))
        hitResult.addHitPath(7, listOf(root, middle3, leaf3))

        hitResult.removeDetachedPointerInputNodes()

        val expectedRoot = Node().apply {
            children.add(Node(root).apply {
                pointerIds.add(3)
                pointerIds.add(5)
                pointerIds.add(7)
                children.add(Node(middle1).apply {
                    pointerIds.add(3)
                    children.add(Node(leaf1).apply {
                        pointerIds.add(3)
                    })
                })
                children.add(Node(middle2).apply {
                    pointerIds.add(5)
                    children.add(Node(leaf2).apply {
                        pointerIds.add(5)
                    })
                })
            })
        }

        assertThat(areEqual(hitResult.root, expectedRoot)).isTrue()
    }

    //  compositionRoot -> root -> layoutNode, middle1 -> leaf1
    //                             layoutNode, middle2 -> leaf2
    //                             layoutNode -> middle3 -> leaf3
    @Test
    fun removeDetachedPointerInputNodes_rootWith3Middles2Detached_correctMiddlesRemoved() {
        val leaf1 = PointerInputNode()
        val middle1 = PointerInputNode().apply {
            emitInsertAt(0, leaf1)
        }

        val leaf2 = PointerInputNode()
        val middle2 = PointerInputNode().apply {
            emitInsertAt(0, leaf2)
        }

        val leaf3 = PointerInputNode()
        val middle3 = PointerInputNode().apply {
            emitInsertAt(0, leaf3)
        }

        val layoutNode = LayoutNode().apply {
            emitInsertAt(0, middle3)
        }

        val root = PointerInputNode().apply {
            emitInsertAt(0, layoutNode)
        }

        compositionRoot.emitInsertAt(0, root)

        hitResult.addHitPath(3, listOf(root, middle1, leaf1))
        hitResult.addHitPath(5, listOf(root, middle2, leaf2))
        hitResult.addHitPath(7, listOf(root, middle3, leaf3))

        hitResult.removeDetachedPointerInputNodes()

        val expectedRoot = Node().apply {
            children.add(Node(root).apply {
                pointerIds.add(3)
                pointerIds.add(5)
                pointerIds.add(7)
                children.add(Node(middle3).apply {
                    pointerIds.add(7)
                    children.add(Node(leaf3).apply {
                        pointerIds.add(7)
                    })
                })
            })
        }

        assertThat(areEqual(hitResult.root, expectedRoot)).isTrue()
    }

    //  compositionRoot -> root -> layoutNode, middle1 -> leaf1
    //                             layoutNode, middle2 -> leaf2
    //                             layoutNode, middle3 -> leaf3
    @Test
    fun removeDetachedPointerInputNodes_rootWith3MiddlesAllDetached_allMiddlesRemoved() {
        val leaf1 = PointerInputNode()
        val middle1 = PointerInputNode().apply {
            emitInsertAt(0, leaf1)
        }

        val leaf2 = PointerInputNode()
        val middle2 = PointerInputNode().apply {
            emitInsertAt(0, leaf2)
        }

        val leaf3 = PointerInputNode()
        val middle3 = PointerInputNode().apply {
            emitInsertAt(0, leaf3)
        }

        val layoutNode = LayoutNode()

        val root = PointerInputNode().apply {
            emitInsertAt(0, layoutNode)
        }

        compositionRoot.emitInsertAt(0, root)

        hitResult.addHitPath(3, listOf(root, middle1, leaf1))
        hitResult.addHitPath(5, listOf(root, middle2, leaf2))
        hitResult.addHitPath(7, listOf(root, middle3, leaf3))

        hitResult.removeDetachedPointerInputNodes()

        val expectedRoot = Node().apply {
            children.add(Node(root).apply {
                pointerIds.add(3)
                pointerIds.add(5)
                pointerIds.add(7)
            })
        }

        assertThat(areEqual(hitResult.root, expectedRoot)).isTrue()
    }

    //  compositionRoot -> root -> middle -> layoutNode -> leaf1
    //                                       layoutNode, leaf2
    //                                       layoutNode -> leaf3
    @Test
    fun removeDetachedPointerInputNodes_middleWith3Leafs1Detached_correctLeafRemoved() {
        val leaf1 = PointerInputNode()
        val leaf2 = PointerInputNode()
        val leaf3 = PointerInputNode()

        val layoutNode = LayoutNode().apply {
            emitInsertAt(0, leaf1)
            emitInsertAt(1, leaf3)
        }

        val middle = PointerInputNode().apply {
            emitInsertAt(0, layoutNode)
        }

        val root = PointerInputNode().apply {
            emitInsertAt(0, middle)
        }

        compositionRoot.emitInsertAt(0, root)

        hitResult.addHitPath(3, listOf(root, middle, leaf1))
        hitResult.addHitPath(5, listOf(root, middle, leaf2))
        hitResult.addHitPath(7, listOf(root, middle, leaf3))

        hitResult.removeDetachedPointerInputNodes()

        val expectedRoot = Node().apply {
            children.add(Node(root).apply {
                pointerIds.add(3)
                pointerIds.add(5)
                pointerIds.add(7)
                children.add(Node(middle).apply {
                    pointerIds.add(3)
                    pointerIds.add(5)
                    pointerIds.add(7)
                    children.add(Node(leaf1).apply {
                        pointerIds.add(3)
                    })
                    children.add(Node(leaf3).apply {
                        pointerIds.add(7)
                    })
                })
            })
        }

        assertThat(areEqual(hitResult.root, expectedRoot)).isTrue()
    }

    //  compositionRoot -> root -> middle -> layoutNode, leaf1
    //                                       layoutNode -> leaf2
    //                                       layoutNode, leaf3
    @Test
    fun removeDetachedPointerInputNodes_middleWith3Leafs2Detached_correctLeafsRemoved() {
        val leaf1 = PointerInputNode()
        val leaf2 = PointerInputNode()
        val leaf3 = PointerInputNode()

        val layoutNode = LayoutNode().apply {
            emitInsertAt(0, leaf2)
        }

        val middle = PointerInputNode().apply {
            emitInsertAt(0, layoutNode)
        }

        val root = PointerInputNode().apply {
            emitInsertAt(0, middle)
        }

        compositionRoot.emitInsertAt(0, root)

        hitResult.addHitPath(3, listOf(root, middle, leaf1))
        hitResult.addHitPath(5, listOf(root, middle, leaf2))
        hitResult.addHitPath(7, listOf(root, middle, leaf3))

        hitResult.removeDetachedPointerInputNodes()

        val expectedRoot = Node().apply {
            children.add(Node(root).apply {
                pointerIds.add(3)
                pointerIds.add(5)
                pointerIds.add(7)
                children.add(Node(middle).apply {
                    pointerIds.add(3)
                    pointerIds.add(5)
                    pointerIds.add(7)
                    children.add(Node(leaf2).apply {
                        pointerIds.add(5)
                    })
                })
            })
        }

        assertThat(areEqual(hitResult.root, expectedRoot)).isTrue()
    }

    //  compositionRoot -> root -> middle -> layoutNode, leaf1
    //                                       layoutNode, leaf2
    //                                       layoutNode, leaf3
    @Test
    fun removeDetachedPointerInputNodes_middleWith3LeafsAllDetached_allLeafsRemoved() {
        val leaf1 = PointerInputNode()
        val leaf2 = PointerInputNode()
        val leaf3 = PointerInputNode()

        val layoutNode = LayoutNode()

        val middle = PointerInputNode().apply {
            emitInsertAt(0, layoutNode)
        }

        val root = PointerInputNode().apply {
            emitInsertAt(0, middle)
        }

        compositionRoot.emitInsertAt(0, root)

        hitResult.addHitPath(3, listOf(root, middle, leaf1))
        hitResult.addHitPath(5, listOf(root, middle, leaf2))
        hitResult.addHitPath(7, listOf(root, middle, leaf3))

        hitResult.removeDetachedPointerInputNodes()

        val expectedRoot = Node().apply {
            children.add(Node(root).apply {
                pointerIds.add(3)
                pointerIds.add(5)
                pointerIds.add(7)
                children.add(Node(middle).apply {
                    pointerIds.add(3)
                    pointerIds.add(5)
                    pointerIds.add(7)
                })
            })
        }

        assertThat(areEqual(hitResult.root, expectedRoot)).isTrue()
    }

    // arrange: root(3) -> middle(3) -> leaf(3)
    // act: 3 is removed
    // assert: no path
    @Test
    fun removePointerId_onePathPointerIdRemoved_hitTestResultIsEmpty() {
        val root = PointerInputNode()
        val middle = PointerInputNode()
        val leaf = PointerInputNode()

        hitResult.addHitPath(3, listOf(root, middle, leaf))

        hitResult.removePointerId(3)

        val expectedRoot = Node()

        assertThat(areEqual(hitResult.root, expectedRoot)).isTrue()
    }

    // arrange: root(3) -> middle(3) -> leaf(3)
    // act: 99 is removed
    // assert: root(3) -> middle(3) -> leaf(3)
    @Test
    fun removePointerId_onePathOtherPointerIdRemoved_hitTestResultIsNotChanged() {
        val root = PointerInputNode()
        val middle = PointerInputNode()
        val leaf = PointerInputNode()

        hitResult.addHitPath(3, listOf(root, middle, leaf))

        hitResult.removePointerId(99)

        val expectedRoot = Node().apply {
            children.add(Node(root).apply {
                pointerIds.add(3)
                children.add(Node(middle).apply {
                    pointerIds.add(3)
                    children.add(Node(leaf).apply {
                        pointerIds.add(3)
                    })
                })
            })
        }

        assertThat(areEqual(hitResult.root, expectedRoot)).isTrue()
    }

    // Arrange:
    // root(3) -> middle(3) -> leaf(3)
    // root(5) -> middle(5) -> leaf(5)
    //
    // Act:
    // 5 is removed
    //
    // Act:
    // root(3) -> middle(3) -> leaf(3)
    @Test
    fun removePointerId_2IndependentPaths1PointerIdRemoved_resultContainsRemainingPath() {
        val root1 = PointerInputNode()
        val middle1 = PointerInputNode()
        val leaf1 = PointerInputNode()

        val root2 = PointerInputNode()
        val middle2 = PointerInputNode()
        val leaf2 = PointerInputNode()

        hitResult.addHitPath(3, listOf(root1, middle1, leaf1))
        hitResult.addHitPath(5, listOf(root2, middle2, leaf2))

        hitResult.removePointerId(5)

        val expectedRoot = Node().apply {
            children.add(Node(root1).apply {
                pointerIds.add(3)
                children.add(Node(middle1).apply {
                    pointerIds.add(3)
                    children.add(Node(leaf1).apply {
                        pointerIds.add(3)
                    })
                })
            })
        }

        assertThat(areEqual(hitResult.root, expectedRoot)).isTrue()
    }

    // root(3,5) -> middle(3,5) -> leaf(3,5)
    // 3 is removed
    // root(5) -> middle(5) -> leaf(5)
    @Test
    fun removePointerId_2PathsShareNodes1PointerIdRemoved_resultContainsRemainingPath() {
        val root = PointerInputNode()
        val middle = PointerInputNode()
        val leaf = PointerInputNode()

        hitResult.addHitPath(3, listOf(root, middle, leaf))
        hitResult.addHitPath(5, listOf(root, middle, leaf))

        hitResult.removePointerId(3)

        val expectedRoot = Node().apply {
            children.add(Node(root).apply {
                pointerIds.add(5)
                children.add(Node(middle).apply {
                    pointerIds.add(5)
                    children.add(Node(leaf).apply {
                        pointerIds.add(5)
                    })
                })
            })
        }

        assertThat(areEqual(hitResult.root, expectedRoot)).isTrue()
    }

    // Arrange: root(3,5) -> middle(3,5) -> leaf(3)
    // Act: 3 is removed
    // Assert: root(5) -> middle(5)
    @Test
    fun removePointerId_2PathsShare2NodesLongPathPointerIdRemoved_resultJustHasShortPath() {
        val root = PointerInputNode()
        val middle = PointerInputNode()
        val leaf = PointerInputNode()

        hitResult.addHitPath(3, listOf(root, middle, leaf))
        hitResult.addHitPath(5, listOf(root, middle))

        hitResult.removePointerId(3)

        val expectedRoot = Node().apply {
            children.add(Node(root).apply {
                pointerIds.add(5)
                children.add(Node(middle).apply {
                    pointerIds.add(5)
                })
            })
        }

        assertThat(areEqual(hitResult.root, expectedRoot)).isTrue()
    }

    // Arrange: root(3,5) -> middle(3,5) -> leaf(3)
    // Act: 5 is removed
    // Assert: root(3) -> middle(3) -> leaf(3)
    @Test
    fun removePointerId_2PathsShare2NodesShortPathPointerIdRemoved_resultJustHasLongPath() {
        val root = PointerInputNode()
        val middle = PointerInputNode()
        val leaf = PointerInputNode()

        hitResult.addHitPath(3, listOf(root, middle, leaf))
        hitResult.addHitPath(5, listOf(root, middle))

        hitResult.removePointerId(5)

        val expectedRoot = Node().apply {
            children.add(Node(root).apply {
                pointerIds.add(3)
                children.add(Node(middle).apply {
                    pointerIds.add(3)
                    children.add(Node(leaf).apply {
                        pointerIds.add(3)
                    })
                })
            })
        }

        assertThat(areEqual(hitResult.root, expectedRoot)).isTrue()
    }

    // Arrange: root(3,5) -> middle(3) -> leaf(3)
    // Act: 3 is removed
    // Assert: root(5)
    @Test
    fun removePointerId_2PathsShare1NodeLongPathPointerIdRemoved_resultJustHasShortPath() {
        val root = PointerInputNode()
        val middle = PointerInputNode()
        val leaf = PointerInputNode()

        hitResult.addHitPath(3, listOf(root, middle, leaf))
        hitResult.addHitPath(5, listOf(root))

        hitResult.removePointerId(3)

        val expectedRoot = Node().apply {
            children.add(Node(root).apply {
                pointerIds.add(5)
            })
        }

        assertThat(areEqual(hitResult.root, expectedRoot)).isTrue()
    }

    // Arrange: root(3,5) -> middle(3) -> leaf(3)
    // Act: 5 is removed
    // Assert: root(3) -> middle(3) -> leaf(3)
    @Test
    fun removePointerId_2PathsShare1NodeShortPathPointerIdRemoved_resultJustHasLongPath() {
        val root = PointerInputNode()
        val middle = PointerInputNode()
        val leaf = PointerInputNode()

        hitResult.addHitPath(3, listOf(root, middle, leaf))
        hitResult.addHitPath(5, listOf(root))

        hitResult.removePointerId(5)

        val expectedRoot = Node().apply {
            children.add(Node(root).apply {
                pointerIds.add(3)
                children.add(Node(middle).apply {
                    pointerIds.add(3)
                    children.add(Node(leaf).apply {
                        pointerIds.add(3)
                    })
                })
            })
        }

        assertThat(areEqual(hitResult.root, expectedRoot)).isTrue()
    }

    private fun areEqual(actualNode: Node, expectedNode: Node): Boolean {
        if (actualNode.pointerInputNode != expectedNode.pointerInputNode) {
            return false
        }

        if (actualNode.pointerIds.size != expectedNode.pointerIds.size) {
            return false
        }
        var check = true
        actualNode.pointerIds.forEach {
            check = check && expectedNode.pointerIds.contains(it)
        }
        if (!check) {
            return false
        }

        if (actualNode.children.size != expectedNode.children.size) {
            return false
        }
        for (child in actualNode.children) {
            check = check && expectedNode.children.any {
                areEqual(child, it)
            }
        }

        return check
    }
}