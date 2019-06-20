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
import androidx.ui.core.IntPxPosition
import androidx.ui.core.LayoutNode
import androidx.ui.core.Owner
import androidx.ui.core.PointerEventPass
import androidx.ui.core.PointerInputChange
import androidx.ui.core.PointerInputData
import androidx.ui.core.PointerInputNode
import androidx.ui.core.PxPosition
import androidx.ui.core.SemanticsComponentNode
import androidx.ui.core.add
import androidx.ui.core.consumeDownChange
import androidx.ui.core.consumePositionChange
import androidx.ui.core.ipx
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

        verify(pin1.pointerInputHandler).invoke(listOf(down(13)), PointerEventPass.InitialDown)
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
            verify(pin1.pointerInputHandler).invoke(listOf(down(13)), PointerEventPass.InitialDown)
            verify(pin2.pointerInputHandler).invoke(listOf(down(13)), PointerEventPass.InitialDown)
            verify(pin3.pointerInputHandler).invoke(listOf(down(13)), PointerEventPass.InitialDown)
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
            verify(pin1.pointerInputHandler).invoke(listOf(down(13)), PointerEventPass.InitialDown)
            verify(pin2.pointerInputHandler).invoke(listOf(down(13)), PointerEventPass.InitialDown)
            verify(pin3.pointerInputHandler).invoke(listOf(down(13)), PointerEventPass.InitialDown)
            verify(pin3.pointerInputHandler).invoke(listOf(down(13)), PointerEventPass.PreUp)
            verify(pin2.pointerInputHandler).invoke(listOf(down(13)), PointerEventPass.PreUp)
            verify(pin1.pointerInputHandler).invoke(listOf(down(13)), PointerEventPass.PreUp)
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
            verify(pin1.pointerInputHandler).invoke(listOf(event1), PointerEventPass.InitialDown)
            verify(pin2.pointerInputHandler).invoke(listOf(event1), PointerEventPass.InitialDown)
            verify(pin2.pointerInputHandler).invoke(listOf(event1), PointerEventPass.PreUp)
            verify(pin1.pointerInputHandler).invoke(listOf(event1), PointerEventPass.PreUp)
        }
        inOrder(pin3.pointerInputHandler, pin4.pointerInputHandler) {
            verify(pin3.pointerInputHandler).invoke(listOf(event2), PointerEventPass.InitialDown)
            verify(pin4.pointerInputHandler).invoke(listOf(event2), PointerEventPass.InitialDown)
            verify(pin4.pointerInputHandler).invoke(listOf(event2), PointerEventPass.PreUp)
            verify(pin3.pointerInputHandler).invoke(listOf(event2), PointerEventPass.PreUp)
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

        // Verifies that the events traverse between parent and child1 in the correct order.
        inOrder(
            parent.pointerInputHandler,
            child1.pointerInputHandler
        ) {
            verify(parent.pointerInputHandler).invoke(
                listOf(event1, event2),
                PointerEventPass.InitialDown
            )
            verify(child1.pointerInputHandler).invoke(listOf(event1), PointerEventPass.InitialDown)
            verify(child1.pointerInputHandler).invoke(listOf(event1), PointerEventPass.PreUp)
            verify(parent.pointerInputHandler).invoke(
                listOf(event1, event2),
                PointerEventPass.PreUp
            )
        }

        // Verifies that the events traverse between parent and child2 in the correct order.
        inOrder(
            parent.pointerInputHandler,
            child2.pointerInputHandler
        ) {
            verify(parent.pointerInputHandler).invoke(
                listOf(event1, event2),
                PointerEventPass.InitialDown
            )
            verify(child2.pointerInputHandler).invoke(listOf(event2), PointerEventPass.InitialDown)
            verify(child2.pointerInputHandler).invoke(listOf(event2), PointerEventPass.PreUp)
            verify(parent.pointerInputHandler).invoke(
                listOf(event1, event2),
                PointerEventPass.PreUp
            )
        }

        verifyNoMoreInteractions(
            parent.pointerInputHandler,
            child1.pointerInputHandler,
            child2.pointerInputHandler
        )
    }

    @Test
    fun dispatchChanges_2PointersShareCompletePath_eventsDoNotSplitAndCallOrderCorrect() {
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

        // Verify that order is correct for child1.
        inOrder(
            child1.pointerInputHandler
        ) {
            verify(child1.pointerInputHandler).invoke(
                listOf(event1, event2),
                PointerEventPass.InitialDown
            )
            verify(child1.pointerInputHandler).invoke(
                listOf(event1, event2),
                PointerEventPass.PreUp
            )
        }

        // Verify that order is correct for child2.
        inOrder(
            child2.pointerInputHandler
        ) {
            verify(child2.pointerInputHandler).invoke(
                listOf(event1, event2),
                PointerEventPass.InitialDown
            )
            verify(child2.pointerInputHandler).invoke(
                listOf(event1, event2),
                PointerEventPass.PreUp
            )
        }

        // Verify that first pass hits child1 before second pass hits child2
        inOrder(
            child1.pointerInputHandler,
            child2.pointerInputHandler
        ) {
            verify(child1.pointerInputHandler).invoke(
                listOf(event1, event2),
                PointerEventPass.InitialDown
            )
            verify(child2.pointerInputHandler).invoke(
                listOf(event1, event2),
                PointerEventPass.PreUp
            )
        }

        // Verify that first pass hits child2 before second pass hits child1
        inOrder(
            child1.pointerInputHandler,
            child2.pointerInputHandler
        ) {
            verify(child2.pointerInputHandler).invoke(
                listOf(event1, event2),
                PointerEventPass.InitialDown
            )
            verify(child1.pointerInputHandler).invoke(
                listOf(event1, event2),
                PointerEventPass.PreUp
            )
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
            modifyBlock = { changes, _ ->
                changes.map { it.consumeDownChange() }
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
            modifyBlock = { changes, pass ->
                val yConsume = if (pass == PointerEventPass.InitialDown) 2f else 64f
                changes.map { it.consumePositionChange(0.px, yConsume.px) }
            }
        })
        pin2.pointerInputHandler = spy(MyPointerInputHandler().apply {
            modifyBlock = { changes, pass ->
                val yConsume = if (pass == PointerEventPass.InitialDown) 4f else 32f
                changes.map { it.consumePositionChange(0.px, yConsume.px) }
            }
        })
        pin3.pointerInputHandler = spy(MyPointerInputHandler().apply {
            modifyBlock = { changes, pass ->
                val yConsume = if (pass == PointerEventPass.InitialDown) 8f else 16f
                changes.map { it.consumePositionChange(0.px, yConsume.px) }
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
            listOf(change), PointerEventPass.InitialDown
        )
        verify(pin2.pointerInputHandler).invoke(
            listOf(change.consumePositionChange(0.px, 2.px)),
            PointerEventPass.InitialDown
        )
        verify(pin3.pointerInputHandler).invoke(
            listOf(change.consumePositionChange(0.px, 6.px)), // 2 + 4
            PointerEventPass.InitialDown
        )
        verify(pin3.pointerInputHandler).invoke(
            listOf(change.consumePositionChange(0.px, 14.px)), // 2 + 4 + 8
            PointerEventPass.PreUp
        )
        verify(pin2.pointerInputHandler).invoke(
            listOf(change.consumePositionChange(0.px, 30.px)), // 2 + 4 + 8 + 16
            PointerEventPass.PreUp
        )
        verify(pin1.pointerInputHandler).invoke(
            listOf(change.consumePositionChange(0.px, 62.px)), // 2 + 4 + 8 + 16 + 32
            PointerEventPass.PreUp
        )
        assertThat(result)
            .isEqualTo(
                listOf(
                    change.consumePositionChange(
                        0.px,
                        126.px
                    )
                )
            ) // 2 + 4 + 8 + 16 + 32 + 64
    }

    @Test
    fun dispatchChanges_2IndependentBranchesFromRoot_changesAreUpdatedCorrectly() {
        val pin1 = PointerInputNode()
        val pin2 = PointerInputNode()
        val pin3 = PointerInputNode()
        val pin4 = PointerInputNode()
        pin1.pointerInputHandler = spy(MyPointerInputHandler().apply {
            modifyBlock = { changes, pass ->
                val yConsume = if (pass == PointerEventPass.InitialDown) 2f else 12f
                changes.map { it.consumePositionChange(0.px, yConsume.px) }
            }
        })
        pin2.pointerInputHandler = spy(MyPointerInputHandler().apply {
            modifyBlock = { changes, pass ->
                val yConsume = if (pass == PointerEventPass.InitialDown) 3f else 6f
                changes.map { it.consumePositionChange(0.px, yConsume.px) }
            }
        })
        pin3.pointerInputHandler = spy(MyPointerInputHandler().apply {
            modifyBlock = { changes, pass ->
                val yConsume = if (pass == PointerEventPass.InitialDown) -2f else -12f
                changes.map { it.consumePositionChange(0.px, yConsume.px) }
            }
        })
        pin4.pointerInputHandler = spy(MyPointerInputHandler().apply {
            modifyBlock = { changes, pass ->
                val yConsume = if (pass == PointerEventPass.InitialDown) -3f else -6f
                changes.map { it.consumePositionChange(0.px, yConsume.px) }
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
            listOf(event1),
            PointerEventPass.InitialDown
        )
        verify(pin2.pointerInputHandler).invoke(
            listOf(event1.consumePositionChange(0.px, 2.px)),
            PointerEventPass.InitialDown
        )
        verify(pin2.pointerInputHandler).invoke(
            listOf(event1.consumePositionChange(0.px, 5.px)),
            PointerEventPass.PreUp
        )
        verify(pin1.pointerInputHandler).invoke(
            listOf(event1.consumePositionChange(0.px, 11.px)),
            PointerEventPass.PreUp
        )

        verify(pin3.pointerInputHandler).invoke(
            listOf(event2),
            PointerEventPass.InitialDown
        )
        verify(pin4.pointerInputHandler).invoke(
            listOf(event2.consumePositionChange(0.px, -2.px)),
            PointerEventPass.InitialDown
        )
        verify(pin4.pointerInputHandler).invoke(
            listOf(event2.consumePositionChange(0.px, -5.px)),
            PointerEventPass.PreUp
        )
        verify(pin3.pointerInputHandler).invoke(
            listOf(event2.consumePositionChange(0.px, (-11).px)),
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
            modifyBlock = { changes, pass ->
                val yConsume = if (pass == PointerEventPass.InitialDown) 2 else 3
                changes.map {
                    it.consumePositionChange(
                        0.px,
                        (it.positionChange().y.value.toInt() / yConsume).px
                    )
                }
            }
        }
        )
        child1.pointerInputHandler = spy(MyPointerInputHandler().apply
        {
            modifyBlock = { changes, pass ->
                val yConsume = if (pass == PointerEventPass.InitialDown) 5 else 7
                changes.map {
                    it.consumePositionChange(
                        0.px,
                        (it.positionChange().y.value.toInt() / yConsume).px
                    )
                }
            }
        })
        child2.pointerInputHandler = spy(MyPointerInputHandler().apply
        {
            modifyBlock = { changes, pass ->
                val yConsume = if (pass == PointerEventPass.InitialDown) 11 else 13
                changes.map {
                    it.consumePositionChange(
                        0.px,
                        (it.positionChange().y.value.toInt() / yConsume).px
                    )
                }
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
            listOf(event1, event2),
            PointerEventPass.InitialDown
        )
        verify(child1.pointerInputHandler).invoke(
            listOf(event1.consumePositionChange(0.px, 500.px)),
            PointerEventPass.InitialDown
        )
        verify(child2.pointerInputHandler).invoke(
            listOf(event2.consumePositionChange(0.px, -500.px)),
            PointerEventPass.InitialDown
        )
        verify(child1.pointerInputHandler).invoke(
            listOf(event1.consumePositionChange(0.px, 600.px)),
            PointerEventPass.PreUp
        )
        verify(child2.pointerInputHandler).invoke(
            listOf(event2.consumePositionChange(0.px, -545.px)),
            PointerEventPass.PreUp
        )
        verify(parent.pointerInputHandler).invoke(
            listOf(
                event1.consumePositionChange(0.px, 657.px),
                event2.consumePositionChange(0.px, -580.px)
            ),
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
            modifyBlock = { changes, pass ->
                val yConsume = if (pass == PointerEventPass.InitialDown) 2 else 3
                changes.map {
                    it.consumePositionChange(
                        0.px,
                        (it.positionChange().y.value.toInt() / yConsume).px
                    )
                }
            }
        })
        child2.pointerInputHandler = spy(MyPointerInputHandler().apply {
            modifyBlock = { changes, pass ->
                val yConsume = if (pass == PointerEventPass.InitialDown) 5 else 7
                changes.map {
                    it.consumePositionChange(
                        0.px,
                        (it.positionChange().y.value.toInt() / yConsume).px
                    )
                }
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
            listOf(event1, event2),
            PointerEventPass.InitialDown
        )
        verify(child2.pointerInputHandler).invoke(
            listOf(
                event1.consumePositionChange(0.px, 500.px),
                event2.consumePositionChange(0.px, -500.px)
            ),
            PointerEventPass.InitialDown
        )

        verify(child2.pointerInputHandler).invoke(
            listOf(
                event1.consumePositionChange(0.px, 600.px),
                event2.consumePositionChange(0.px, -600.px)
            ),
            PointerEventPass.PreUp
        )
        verify(child1.pointerInputHandler).invoke(
            listOf(
                event1.consumePositionChange(0.px, 657.px),
                event2.consumePositionChange(0.px, -657.px)
            ),
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

    // compositionRoot, root1 -> middle1 -> leaf1
    // compositionRoot -> root2, middle2, leaf2
    // compositionRoot -> root3 -> middle3, leaf3
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

    // compositionRoot, root ->
    //   layoutNode -> middle1 -> leaf1
    //   layoutNode -> middle2 -> leaf2
    //   layoutNode -> middle3 -> leaf3
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

    // compositionRoot -> root ->
    //   layoutNode -> middle1 -> leaf1
    //   layoutNode -> middle2 -> leaf2
    //   layoutNode, middle3 -> leaf3
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

    // compositionRoot -> root ->
    //   layoutNode, middle1 -> leaf1
    //   layoutNode, middle2 -> leaf2
    //   layoutNode -> middle3 -> leaf3
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

    // compositionRoot -> root ->
    //   layoutNode, middle1 -> leaf1
    //   layoutNode, middle2 -> leaf2
    //   layoutNode, middle3 -> leaf3
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

    // compositionRoot -> root -> middle ->
    //   layoutNode -> leaf1
    //   layoutNode, leaf2
    //   layoutNode -> leaf3
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

    // compositionRoot -> root -> middle ->
    //   layoutNode, leaf1
    //   layoutNode -> leaf2
    //   layoutNode, leaf3
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

    // compositionRoot -> root -> middle ->
    //   layoutNode, leaf1
    //   layoutNode, leaf2
    //   layoutNode, leaf3
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

    @Test
    fun removePointerInputNodesWithNoLayoutNodeDescendants_noNodes_hitResultJustHasRootNoCrash() {
        val throwable = catchThrowable {
            hitResult.removePointerInputNodesWithNoLayoutNodeDescendants()
        }

        assertThat(throwable).isNull()
        assertThat(areEqual(hitResult.root, Node()))
    }

    // PointerInputNode
    @Test
    fun removePointerInputNodesWithNoLayoutNodeDescendants_justPin_pinRemoved() {
        val pointerInputNode = PointerInputNode()
        hitResult.addHitPath(0, listOf(pointerInputNode))

        hitResult.removePointerInputNodesWithNoLayoutNodeDescendants()

        assertThat(areEqual(hitResult.root, Node())).isTrue()
    }

    // PointerInputNode -> DrawNode
    @Test
    fun removePointerInputNodesWithNoLayoutNodeDescendants_dnInPin_pinRemoved() {
        val drawNode = DrawNode()
        val pointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, drawNode)
        }
        hitResult.addHitPath(0, listOf(pointerInputNode))

        hitResult.removePointerInputNodesWithNoLayoutNodeDescendants()

        assertThat(areEqual(hitResult.root, Node())).isTrue()
    }

    // PointerInputNode -> SemanticsNode
    @Test
    fun removePointerInputNodesWithNoLayoutNodeDescendants_snInPin_pinRemoved() {
        val semanticsNode = SemanticsComponentNode()
        val pointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, semanticsNode)
        }
        hitResult.addHitPath(0, listOf(pointerInputNode))

        hitResult.removePointerInputNodesWithNoLayoutNodeDescendants()

        assertThat(areEqual(hitResult.root, Node())).isTrue()
    }

    // PointerInputNode A -> PointerInputNode B -> SemanticsNode
    @Test
    fun removePointerInputNodesWithNoLayoutNodeDescendants_smInPinInPin_pinsRemoved() {
        val semanticsNode = SemanticsComponentNode()
        val pointerInputNodeB = PointerInputNode().apply {
            emitInsertAt(0, semanticsNode)
        }
        val pointerInputNodeA = PointerInputNode().apply {
            emitInsertAt(0, pointerInputNodeB)
        }
        hitResult.addHitPath(0, listOf(pointerInputNodeA, pointerInputNodeB))

        hitResult.removePointerInputNodesWithNoLayoutNodeDescendants()

        assertThat(areEqual(hitResult.root, Node())).isTrue()
    }

    // PointerInputNode A -> PointerInputNode B -> DrawNode
    @Test
    fun removePointerInputNodesWithNoLayoutNodeDescendants_dnInPinInPin_pisnRemoved() {
        val drawnode = DrawNode()
        val pointerInputNodeB = PointerInputNode().apply {
            emitInsertAt(0, drawnode)
        }
        val pointerInputNodeA = PointerInputNode().apply {
            emitInsertAt(0, pointerInputNodeB)
        }
        hitResult.addHitPath(0, listOf(pointerInputNodeA, pointerInputNodeB))

        hitResult.removePointerInputNodesWithNoLayoutNodeDescendants()

        assertThat(areEqual(hitResult.root, Node())).isTrue()
    }

    // PointerInputNode -> LayoutNode
    @Test
    fun removePointerInputNodesWithNoLayoutNodeDescendants_pinWithLn_nothingRemoved() {
        val layoutNode = LayoutNode(0, 0, 100, 100)
        val pointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, layoutNode)
        }
        hitResult.addHitPath(0, listOf(pointerInputNode))

        hitResult.removePointerInputNodesWithNoLayoutNodeDescendants()

        val expectedRoot = Node().apply {
            children.add(Node(pointerInputNode).apply { pointerIds.add(0) })
        }
        assertThat(areEqual(hitResult.root, expectedRoot)).isTrue()
    }

    // PointerInputNode A -> PointerInputNode B -> LayoutNode
    @Test
    fun removePointerInputNodesWithNoLayoutNodeDescendants_lnInPinInPin_nothingRemoved() {
        val layoutNode = LayoutNode(0, 0, 100, 100)
        val pointerInputNodeB = PointerInputNode().apply {
            emitInsertAt(0, layoutNode)
        }
        val pointerInputNodeA = PointerInputNode().apply {
            emitInsertAt(0, pointerInputNodeB)
        }
        hitResult.addHitPath(0, listOf(pointerInputNodeA, pointerInputNodeB))

        hitResult.removePointerInputNodesWithNoLayoutNodeDescendants()

        val expectedRoot = Node().apply {
            children.add(Node(pointerInputNodeA).apply {
                pointerIds.add(0)
                children.add(Node(pointerInputNodeB).apply {
                    pointerIds.add(0)
                })
            })
        }

        assertThat(areEqual(hitResult.root, expectedRoot)).isTrue()
    }

    // PointerInputNode A -> LayoutNode -> PointerInputNode B
    @Test
    fun removePointerInputNodesWithNoLayoutNodeDescendants_PinInLnInPin_childPinRemoved() {
        val pointerInputNodeB = PointerInputNode()
        val layoutNode = LayoutNode(0, 0, 100, 100).apply {
            emitInsertAt(0, pointerInputNodeB)
        }
        val pointerInputNodeA = PointerInputNode().apply {
            emitInsertAt(0, layoutNode)
        }
        hitResult.addHitPath(0, listOf(pointerInputNodeA, pointerInputNodeB))

        hitResult.removePointerInputNodesWithNoLayoutNodeDescendants()

        val expectedRoot = Node().apply {
            children.add(Node(pointerInputNodeA).apply {
                pointerIds.add(0)
            })
        }

        assertThat(areEqual(hitResult.root, expectedRoot)).isTrue()
    }

    // PointerInputNode A -> PointerInputNode B -> LayoutNode
    // PointerInputNode A -> PointerInputNode C
    @Test
    fun removePointerInputNodesWithNoLayoutNodeDescendants_2BranchesOneHasLn_otherBranchRemoved() {
        val layoutNode = LayoutNode(0, 0, 100, 100)
        val pointerInputNodeB = PointerInputNode().apply {
            emitInsertAt(0, layoutNode)
        }
        val pointerInputNodeC = PointerInputNode()
        val pointerInputNodeA = PointerInputNode().apply {
            emitInsertAt(0, pointerInputNodeB)
            emitInsertAt(0, pointerInputNodeC)
        }
        hitResult.addHitPath(0, listOf(pointerInputNodeA, pointerInputNodeB))
        hitResult.addHitPath(1, listOf(pointerInputNodeA, pointerInputNodeC))

        hitResult.removePointerInputNodesWithNoLayoutNodeDescendants()

        val expectedRoot = Node().apply {
            children.add(Node(pointerInputNodeA).apply {
                pointerIds.add(0)
                pointerIds.add(1)
                children.add(Node(pointerInputNodeB).apply {
                    pointerIds.add(0)
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

    @Test
    fun refreshOffsets_layoutNodesNotOffset_changeTranslatedCorrectly() {
        refreshOffsets_changeTranslatedCorrectly(
            0, 0, 100, 100,
            0, 0, 100, 100,
            0, 0, 100, 100,
            50, 50
        )
    }

    @Test
    fun refreshOffsets_layoutNodesIncreasinglyInset_changeTranslatedCorrectly() {
        refreshOffsets_changeTranslatedCorrectly(
            0, 0, 100, 100,
            2, 11, 100, 100,
            23, 31, 100, 100,
            99, 99
        )
    }

    @Test
    fun refreshOffsets_layoutNodesIncreasinglyOutset_changeTranslatedCorrectly() {
        refreshOffsets_changeTranslatedCorrectly(
            0, 0, 100, 100,
            -2, -11, 100, 100,
            -23, -31, 100, 100,
            1, 1
        )
    }

    // ParentLn -> ParentPin -> MiddleLn -> MiddlePin -> ChildLn -> ChildPin
    private fun refreshOffsets_changeTranslatedCorrectly(
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

        val childOffset = PxPosition(cX1.px, cY1.px)
        val childLayoutNode = LayoutNode(cX1, cY1, cX2, cY2)
        val childPointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, childLayoutNode)
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        val middleOffset = PxPosition(mX1.px, mY1.px)
        val middleLayoutNode = LayoutNode(mX1, mY1, mX2, mY2).apply {
            emitInsertAt(0, childPointerInputNode)
        }
        val middlePointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, middleLayoutNode)
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        val parentLayoutNode = LayoutNode(pX1, pY1, pX2, pY2).apply {
            emitInsertAt(0, middlePointerInputNode)
        }
        val parentPointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, parentLayoutNode)
            pointerInputHandler = spy(MyPointerInputHandler())
        }

        hitResult.addHitPath(
            3,
            listOf(parentPointerInputNode, middlePointerInputNode, childPointerInputNode)
        )

        val offset = PxPosition(pointerX.px, pointerY.px)

        // Act

        hitResult.refreshOffsets()

        // Assert

        hitResult.dispatchChanges(
            listOf(down(3, 7L.millisecondsToTimestamp(), pointerX.toFloat(), pointerY.toFloat())),
            PointerEventPass.InitialDown, PointerEventPass.PreUp
        )

        val pointerInputNodes = arrayOf(
            parentPointerInputNode,
            middlePointerInputNode,
            childPointerInputNode
        )

        val expectedPointerInputChanges = arrayOf(
            PointerInputChange(
                id = 3,
                current = PointerInputData(7L.millisecondsToTimestamp(), offset, true),
                previous = PointerInputData(null, null, false),
                consumed = ConsumedData()
            ),
            PointerInputChange(
                id = 3,
                current = PointerInputData(
                    7L.millisecondsToTimestamp(),
                    offset - middleOffset,
                    true
                ),
                previous = PointerInputData(null, null, false),
                consumed = ConsumedData()
            ),
            PointerInputChange(
                id = 3,
                current = PointerInputData(
                    7L.millisecondsToTimestamp(),
                    offset - middleOffset - childOffset,
                    true
                ),
                previous = PointerInputData(null, null, false),
                consumed = ConsumedData()
            )
        )

        for (pass in listOf(PointerEventPass.InitialDown, PointerEventPass.PreUp)) {
            for (i in 0 until pointerInputNodes.size) {
                verify(pointerInputNodes[i].pointerInputHandler).invoke(
                    listOf(expectedPointerInputChanges[i]),
                    pass
                )
            }
        }
        for (pointerInputNode in pointerInputNodes) {
            verifyNoMoreInteractions(pointerInputNode.pointerInputHandler)
        }
    }

    // ParentLn -> ParentPin -> ChildLn -> ChildPin
    //
    // ParentLn(3, 5), ChildLn(-7, -11)
    // Move to
    // ParentLn(-13, -17), ChildLn(19, 29)
    @Test
    fun refreshOffsets_layoutNodesMove_changeTranslatedCorrectly() {

        // Arrange

        val originalChildOffset = IntPxPosition(3.ipx, 5.ipx)
        val originalParentOffset = IntPxPosition(-7.ipx, -11.ipx)
        val newChildOffset = IntPxPosition(19.ipx, 29.ipx)
        val newParentOffset = IntPxPosition(-13.ipx, -17.ipx)
        val pointerOffset = PxPosition(5.px, 7.px)

        val childLayoutNode = LayoutNode(originalChildOffset)
        val childPointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, childLayoutNode)
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        val parentLayoutNode = LayoutNode(originalParentOffset).apply {
            emitInsertAt(0, childPointerInputNode)
        }
        val parentPointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, parentLayoutNode)
            pointerInputHandler = spy(MyPointerInputHandler())
        }

        hitResult.addHitPath(3, listOf(parentPointerInputNode, childPointerInputNode))

        hitResult.refreshOffsets()

        childLayoutNode.moveTo(newChildOffset.x, newChildOffset.y)
        parentLayoutNode.moveTo(newParentOffset.x, newParentOffset.y)

        // Act

        hitResult.refreshOffsets()

        // Assert

        val pointerInputNodes = arrayOf(parentPointerInputNode, childPointerInputNode)

        val expectedPointerInputChanges = arrayOf(
            PointerInputChange(
                id = 3,
                current = PointerInputData(
                    7L.millisecondsToTimestamp(),
                    pointerOffset - newParentOffset,
                    true
                ),
                previous = PointerInputData(null, null, false),
                consumed = ConsumedData()
            ),
            PointerInputChange(
                id = 3,
                current = PointerInputData(
                    7L.millisecondsToTimestamp(),
                    pointerOffset - newParentOffset - newChildOffset,
                    true
                ),
                previous = PointerInputData(null, null, false),
                consumed = ConsumedData()
            )
        )

        hitResult.dispatchChanges(
            listOf(
                down(
                    3,
                    7L.millisecondsToTimestamp(),
                    pointerOffset.x.value,
                    pointerOffset.y.value
                )
            ),
            PointerEventPass.InitialDown, PointerEventPass.PreUp
        )

        for (pass in listOf(PointerEventPass.InitialDown, PointerEventPass.PreUp)) {
            for (i in 0 until pointerInputNodes.size) {
                verify(pointerInputNodes[i].pointerInputHandler).invoke(
                    listOf(expectedPointerInputChanges[i]),
                    pass
                )
            }
        }
        for (pointerInputNode in pointerInputNodes) {
            verifyNoMoreInteractions(pointerInputNode.pointerInputHandler)
        }
    }

    // Ln -> Pin -> Ln -> Pin
    // Ln -> Pin -> Ln -> Pin
    @Test
    fun refreshOffsets_2IndependentPaths_changeTranslatedCorrectly() {

        // Arrange

        val child1Offset = IntPxPosition(3.ipx, 5.ipx)
        val parent1Offset = IntPxPosition(-7.ipx, -11.ipx)
        val child2Offset = IntPxPosition(-13.ipx, -17.ipx)
        val parent2Offset = IntPxPosition(19.ipx, 27.ipx)
        val pointer1Offset = PxPosition(5.px, 7.px)
        val pointer2Offset = PxPosition(11.px, 13.px)

        val childLayoutNode1 = LayoutNode(child1Offset)
        val childPointerInputNode1 = PointerInputNode().apply {
            emitInsertAt(0, childLayoutNode1)
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        val parentLayoutNode1 = LayoutNode(parent1Offset).apply {
            emitInsertAt(0, childPointerInputNode1)
        }
        val parentPointerInputNode1 = PointerInputNode().apply {
            emitInsertAt(0, parentLayoutNode1)
            pointerInputHandler = spy(MyPointerInputHandler())
        }

        val childLayoutNode2 = LayoutNode(child2Offset)
        val childPointerInputNode2 = PointerInputNode().apply {
            emitInsertAt(0, childLayoutNode2)
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        val parentLayoutNode2 = LayoutNode(parent2Offset).apply {
            emitInsertAt(0, childPointerInputNode2)
        }
        val parentPointerInputNode2 = PointerInputNode().apply {
            emitInsertAt(0, parentLayoutNode2)
            pointerInputHandler = spy(MyPointerInputHandler())
        }

        hitResult.addHitPath(3, listOf(parentPointerInputNode1, childPointerInputNode1))
        hitResult.addHitPath(5, listOf(parentPointerInputNode2, childPointerInputNode2))

        // Act

        hitResult.refreshOffsets()

        // Assert

        val pointerInputNodes1 = arrayOf(parentPointerInputNode1, childPointerInputNode1)

        val expectedPointerInputChanges1 = arrayOf(
            PointerInputChange(
                id = 3,
                current = PointerInputData(
                    7L.millisecondsToTimestamp(),
                    pointer1Offset - parent1Offset,
                    true
                ),
                previous = PointerInputData(null, null, false),
                consumed = ConsumedData()
            ),
            PointerInputChange(
                id = 3,
                current = PointerInputData(
                    7L.millisecondsToTimestamp(),
                    pointer1Offset - parent1Offset - child1Offset,
                    true
                ),
                previous = PointerInputData(null, null, false),
                consumed = ConsumedData()
            )
        )

        val pointerInputNodes2 = arrayOf(parentPointerInputNode2, childPointerInputNode2)

        val expectedPointerInputChanges2 = arrayOf(
            PointerInputChange(
                id = 5,
                current = PointerInputData(
                    7L.millisecondsToTimestamp(),
                    pointer2Offset - parent2Offset,
                    true
                ),
                previous = PointerInputData(null, null, false),
                consumed = ConsumedData()
            ),
            PointerInputChange(
                id = 5,
                current = PointerInputData(
                    7L.millisecondsToTimestamp(),
                    pointer2Offset - parent2Offset - child2Offset,
                    true
                ),
                previous = PointerInputData(null, null, false),
                consumed = ConsumedData()
            )
        )

        hitResult.dispatchChanges(
            listOf(
                down(
                    3,
                    7L.millisecondsToTimestamp(),
                    pointer1Offset.x.value,
                    pointer1Offset.y.value
                ),
                down(
                    5,
                    7L.millisecondsToTimestamp(),
                    pointer2Offset.x.value,
                    pointer2Offset.y.value
                )
            ),
            PointerEventPass.InitialDown, PointerEventPass.PreUp
        )

        for (pass in listOf(PointerEventPass.InitialDown, PointerEventPass.PreUp)) {
            for (i in 0 until pointerInputNodes1.size) {
                verify(pointerInputNodes1[i].pointerInputHandler).invoke(
                    listOf(expectedPointerInputChanges1[i]),
                    pass
                )
            }
            for (i in 0 until pointerInputNodes2.size) {
                verify(pointerInputNodes2[i].pointerInputHandler).invoke(
                    listOf(expectedPointerInputChanges2[i]),
                    pass
                )
            }
        }
        for (pointerInputNode in pointerInputNodes1) {
            verifyNoMoreInteractions(pointerInputNode.pointerInputHandler)
        }
        for (pointerInputNode in pointerInputNodes2) {
            verifyNoMoreInteractions(pointerInputNode.pointerInputHandler)
        }
    }

    // Ln -> Pin
    //   -> Ln -> Pin
    //   -> Ln -> Pin
    @Test
    fun refreshOffsets_2SplittingPaths_changeTranslatedCorrectly() {

        // Arrange

        val child1Offset = IntPxPosition(3.ipx, 5.ipx)
        val child2Offset = IntPxPosition(-13.ipx, -17.ipx)
        val parentOffset = IntPxPosition(-7.ipx, 11.ipx)
        val pointer1Offset = PxPosition(5.px, 7.px)
        val pointer2Offset = PxPosition(11.px, 13.px)

        val childLayoutNode1 = LayoutNode(child1Offset)
        val childPointerInputNode1: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, childLayoutNode1)
            pointerInputHandler = spy(MyPointerInputHandler())
        }

        val childLayoutNode2 = LayoutNode(child2Offset)
        val childPointerInputNode2: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, childLayoutNode2)
            pointerInputHandler = spy(MyPointerInputHandler())
        }

        val parentLayoutNode: LayoutNode = LayoutNode(parentOffset).apply {
            emitInsertAt(0, childPointerInputNode1)
            emitInsertAt(1, childPointerInputNode2)
        }
        val parentPointerInputNode: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, parentLayoutNode)
            pointerInputHandler = spy(MyPointerInputHandler())
        }

        hitResult.addHitPath(3, listOf(parentPointerInputNode, childPointerInputNode1))
        hitResult.addHitPath(5, listOf(parentPointerInputNode, childPointerInputNode2))

        // Act

        hitResult.refreshOffsets()

        // Assert

        val parentExpectedPointerInputChanges = listOf(
            PointerInputChange(
                id = 3,
                current = PointerInputData(
                    7L.millisecondsToTimestamp(),
                    pointer1Offset - parentOffset,
                    true
                ),
                previous = PointerInputData(null, null, false),
                consumed = ConsumedData()
            ),
            PointerInputChange(
                id = 5,
                current = PointerInputData(
                    7L.millisecondsToTimestamp(),
                    pointer2Offset - parentOffset,
                    true
                ),
                previous = PointerInputData(null, null, false),
                consumed = ConsumedData()
            )
        )

        val child1ExpectedPointerInputChanges = listOf(
            PointerInputChange(
                id = 3,
                current = PointerInputData(
                    7L.millisecondsToTimestamp(),
                    pointer1Offset - parentOffset - child1Offset,
                    true
                ),
                previous = PointerInputData(null, null, false),
                consumed = ConsumedData()
            )
        )

        val child2ExpectedPointerInputChanges = listOf(
            PointerInputChange(
                id = 5,
                current = PointerInputData(
                    7L.millisecondsToTimestamp(),
                    pointer2Offset - parentOffset - child2Offset,
                    true
                ),
                previous = PointerInputData(null, null, false),
                consumed = ConsumedData()
            )
        )

        hitResult.dispatchChanges(
            listOf(
                down(
                    3,
                    7L.millisecondsToTimestamp(),
                    pointer1Offset.x.value,
                    pointer1Offset.y.value
                ),
                down(
                    5,
                    7L.millisecondsToTimestamp(),
                    pointer2Offset.x.value,
                    pointer2Offset.y.value
                )
            ),
            PointerEventPass.InitialDown, PointerEventPass.PreUp
        )

        verify(parentPointerInputNode.pointerInputHandler).invoke(
            parentExpectedPointerInputChanges, PointerEventPass.InitialDown
        )
        verify(childPointerInputNode1.pointerInputHandler).invoke(
            child1ExpectedPointerInputChanges, PointerEventPass.InitialDown
        )
        verify(childPointerInputNode2.pointerInputHandler).invoke(
            child2ExpectedPointerInputChanges, PointerEventPass.InitialDown
        )
        verify(childPointerInputNode1.pointerInputHandler).invoke(
            child1ExpectedPointerInputChanges, PointerEventPass.PreUp
        )
        verify(childPointerInputNode2.pointerInputHandler).invoke(
            child2ExpectedPointerInputChanges, PointerEventPass.PreUp
        )
        verify(parentPointerInputNode.pointerInputHandler).invoke(
            parentExpectedPointerInputChanges, PointerEventPass.PreUp
        )

        verifyNoMoreInteractions(parentPointerInputNode.pointerInputHandler)
        verifyNoMoreInteractions(childPointerInputNode1.pointerInputHandler)
        verifyNoMoreInteractions(childPointerInputNode2.pointerInputHandler)
    }

    // Ln -> Ln -> Pin -> Ln
    @Test
    fun refreshOffsets_firstPointerInputNodeIsUnder2LayoutNodes_changeTranslatedCorrectly() {

        // Arrange

        val parentOffset1 = IntPxPosition(1.ipx, 2.ipx)
        val parentOffset2 = IntPxPosition(3.ipx, 4.ipx)
        val childOffset = IntPxPosition(5.ipx, 6.ipx)

        val pointerOffset = PxPosition(5.px, 7.px)

        val childLayoutNode = LayoutNode(childOffset)
        val pointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, childLayoutNode)
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        val parentLayoutNode2 = LayoutNode(parentOffset2).apply {
            emitInsertAt(0, pointerInputNode)
        }
        @Suppress("UNUSED_VARIABLE")
        val parentLayoutNode1 = LayoutNode(parentOffset1).apply {
            emitInsertAt(0, parentLayoutNode2)
        }

        hitResult.addHitPath(3, listOf(pointerInputNode))

        // Act

        hitResult.refreshOffsets()

        // Assert

        val expectedPointerInputChange =
            PointerInputChange(
                id = 3,
                current = PointerInputData(
                    7L.millisecondsToTimestamp(),
                    pointerOffset - parentOffset1 - parentOffset2 - childOffset,
                    true
                ),
                previous = PointerInputData(null, null, false),
                consumed = ConsumedData()
            )

        hitResult.dispatchChanges(
            listOf(
                down(
                    3,
                    7L.millisecondsToTimestamp(),
                    pointerOffset.x.value,
                    pointerOffset.y.value
                )
            ),
            PointerEventPass.InitialDown, PointerEventPass.PreUp
        )

        for (pass in listOf(PointerEventPass.InitialDown, PointerEventPass.PreUp)) {
            verify(pointerInputNode.pointerInputHandler).invoke(
                listOf(expectedPointerInputChange),
                pass
            )
        }
        verifyNoMoreInteractions(pointerInputNode.pointerInputHandler)
    }

    // Ln -> Ln -> Pin -> Ln -> Ln -> Pin -> Ln
    @Test
    fun refreshOffsets_2LayoutNodes1Pin2LayoutNodes1Pin1LayoutNode_changeTranslatedCorrectly() {

        // Arrange

        val parentOffset1 = IntPxPosition(1.ipx, 2.ipx)
        val parentOffset2 = IntPxPosition(3.ipx, 4.ipx)
        val parentOffset3 = IntPxPosition(5.ipx, 6.ipx)
        val childOffset1 = IntPxPosition(7.ipx, 8.ipx)
        val childOffset2 = IntPxPosition(9.ipx, 10.ipx)
        val pointerOffset = PxPosition(5.px, 7.px)

        val childLayoutNode2 = LayoutNode(childOffset2)
        val childPointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, childLayoutNode2)
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        val childLayoutNode1 = LayoutNode(childOffset1).apply {
            emitInsertAt(0, childPointerInputNode)
        }
        val parentLayoutNode3 = LayoutNode(parentOffset3).apply {
            emitInsertAt(0, childLayoutNode1)
        }
        val parentPointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, parentLayoutNode3)
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        val parentLayoutNode2 = LayoutNode(parentOffset2).apply {
            emitInsertAt(0, parentPointerInputNode)
        }
        @Suppress("UNUSED_VARIABLE")
        val parentLayoutNode1 = LayoutNode(parentOffset1).apply {
            emitInsertAt(0, parentLayoutNode2)
        }

        hitResult.addHitPath(3, listOf(parentPointerInputNode, childPointerInputNode))

        // Act

        hitResult.refreshOffsets()

        // Assert

        val pointerInputNodes = arrayOf(parentPointerInputNode, childPointerInputNode)

        val expectedPointerInputChanges = arrayOf(
            PointerInputChange(
                id = 3,
                current = PointerInputData(
                    7L.millisecondsToTimestamp(),
                    pointerOffset - parentOffset1 - parentOffset2 - parentOffset3,
                    true
                ),
                previous = PointerInputData(null, null, false),
                consumed = ConsumedData()
            ),
            PointerInputChange(
                id = 3,
                current = PointerInputData(
                    7L.millisecondsToTimestamp(),
                    pointerOffset - parentOffset1 - parentOffset2 - parentOffset3 -
                            childOffset1 - childOffset2,
                    true
                ),
                previous = PointerInputData(null, null, false),
                consumed = ConsumedData()
            )
        )

        hitResult.dispatchChanges(
            listOf(
                down(
                    3,
                    7L.millisecondsToTimestamp(),
                    pointerOffset.x.value,
                    pointerOffset.y.value
                )
            ),
            PointerEventPass.InitialDown, PointerEventPass.PreUp
        )

        for (pass in listOf(PointerEventPass.InitialDown, PointerEventPass.PreUp)) {
            for (i in 0 until pointerInputNodes.size) {
                verify(pointerInputNodes[i].pointerInputHandler).invoke(
                    listOf(expectedPointerInputChanges[i]),
                    pass
                )
            }
        }
        for (pointerInputNode in pointerInputNodes) {
            verifyNoMoreInteractions(pointerInputNode.pointerInputHandler)
        }
    }

    // layout node grandchild does not affect pin position

    @Test
    fun refreshOffsets_pinWith2LayoutNodes_neitherLayoutNodeInTopLeft() {
        refreshOffsets_pinWith2LayoutNodes(
            300, 100, 400, 200,
            100, 300, 200, 400,
            250, 350,
            150, 250
        )
    }

    @Test
    fun refreshOffsets_pinWith2LayoutNodes_neitherLayoutNodeHasSize() {
        refreshOffsets_pinWith2LayoutNodes(
            300, 100, 300, 100,
            100, 300, 100, 300,
            250, 350,
            150, 250
        )
    }

    @Test
    fun refreshOffsets_pinWith2LayoutNodes_resultingOffestIsNegative() {
        refreshOffsets_pinWith2LayoutNodes(
            300, 100, 400, 200,
            100, 300, 200, 400,
            75, 25,
            -25, -75
        )
    }

    private fun refreshOffsets_pinWith2LayoutNodes(
        layoutNodeAX1: Int,
        layoutNodeAY1: Int,
        layoutNodeAX2: Int,
        layoutNodeAY2: Int,
        layoutNodeBX1: Int,
        layoutNodeBY1: Int,
        layoutNodeBX2: Int,
        layoutNodeBY2: Int,
        pointerX: Int,
        pointerY: Int,
        pointerXExpected: Int,
        pointerYExpected: Int
    ) {

        // Arrange
        val pointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, LayoutNode(layoutNodeAX1, layoutNodeAY1, layoutNodeAX2, layoutNodeAY2))
            emitInsertAt(1, LayoutNode(layoutNodeBX1, layoutNodeBY1, layoutNodeBX2, layoutNodeBY2))
            pointerInputHandler = spy(MyPointerInputHandler())
        }

        hitResult.addHitPath(3, listOf(pointerInputNode))

        // Act

        hitResult.refreshOffsets()

        // Assert

        val expectedPointerInputChange =
            PointerInputChange(
                id = 3,
                current = PointerInputData(
                    7L.millisecondsToTimestamp(),
                    PxPosition(pointerXExpected.px, pointerYExpected.px),
                    true
                ),
                previous = PointerInputData(null, null, false),
                consumed = ConsumedData()
            )

        hitResult.dispatchChanges(
            listOf(
                down(
                    3,
                    7L.millisecondsToTimestamp(),
                    pointerX.toFloat(),
                    pointerY.toFloat()
                )
            ),
            PointerEventPass.InitialDown, PointerEventPass.PreUp
        )

        for (pass in listOf(PointerEventPass.InitialDown, PointerEventPass.PreUp)) {
            verify(pointerInputNode.pointerInputHandler).invoke(
                listOf(expectedPointerInputChange),
                pass
            )
        }
        verifyNoMoreInteractions(pointerInputNode.pointerInputHandler)
    }

    @Test
    fun refreshOffsets_offsetsAreRefreshedViaDirectLayoutNodeAncestors() {

        // Arrange
        val pointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, LayoutNode(100, 200, 200, 300).apply {
                emitInsertAt(0, LayoutNode(-20, -40, -60, -80))
            })
            pointerInputHandler = spy(MyPointerInputHandler())
        }

        hitResult.addHitPath(3, listOf(pointerInputNode))

        // Act

        hitResult.refreshOffsets()

        // Assert

        val expectedPointerInputChange =
            PointerInputChange(
                id = 3,
                current = PointerInputData(
                    7L.millisecondsToTimestamp(),
                    PxPosition(50.px, 50.px),
                    true
                ),
                previous = PointerInputData(null, null, false),
                consumed = ConsumedData()
            )

        hitResult.dispatchChanges(
            listOf(
                down(
                    3,
                    7L.millisecondsToTimestamp(),
                    150f,
                    250f
                )
            ),
            PointerEventPass.InitialDown, PointerEventPass.PreUp
        )

        for (pass in listOf(PointerEventPass.InitialDown, PointerEventPass.PreUp)) {
            verify(pointerInputNode.pointerInputHandler).invoke(
                listOf(expectedPointerInputChange),
                pass
            )
        }
        verifyNoMoreInteractions(pointerInputNode.pointerInputHandler)
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