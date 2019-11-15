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
import androidx.ui.core.IntPxSize
import androidx.ui.core.LayoutNode
import androidx.ui.core.Owner
import androidx.ui.core.PointerEventPass
import androidx.ui.core.PointerInputChange
import androidx.ui.core.PointerInputData
import androidx.ui.core.PointerInputNode
import androidx.ui.core.PxPosition
import androidx.ui.core.SemanticsComponentNode
import androidx.ui.core.Uptime
import androidx.ui.core.add
import androidx.ui.core.consumeDownChange
import androidx.ui.core.consumePositionChange
import androidx.ui.core.ipx
import androidx.ui.core.milliseconds
import androidx.ui.core.positionChange
import androidx.ui.core.px
import androidx.ui.testutils.down
import androidx.ui.testutils.moveTo
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
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

        verify(pin1.pointerInputHandler).invoke(
            eq(listOf(down(13))),
            eq(PointerEventPass.InitialDown),
            any()
        )
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
            verify(pin1.pointerInputHandler).invoke(
                eq(listOf(down(13))),
                eq(PointerEventPass.InitialDown),
                any()
            )
            verify(pin2.pointerInputHandler).invoke(
                eq(listOf(down(13))),
                eq(PointerEventPass.InitialDown),
                any()
            )
            verify(pin3.pointerInputHandler).invoke(
                eq(listOf(down(13))),
                eq(PointerEventPass.InitialDown),
                any()
            )
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
            verify(pin1.pointerInputHandler).invoke(
                eq(listOf(down(13))),
                eq(PointerEventPass.InitialDown),
                any()
            )
            verify(pin2.pointerInputHandler).invoke(
                eq(listOf(down(13))),
                eq(PointerEventPass.InitialDown),
                any()
            )
            verify(pin3.pointerInputHandler).invoke(
                eq(listOf(down(13))),
                eq(PointerEventPass.InitialDown),
                any()
            )
            verify(pin3.pointerInputHandler).invoke(
                eq(listOf(down(13))),
                eq(PointerEventPass.PreUp),
                any()
            )
            verify(pin2.pointerInputHandler).invoke(
                eq(listOf(down(13))),
                eq(PointerEventPass.PreUp),
                any()
            )
            verify(pin1.pointerInputHandler).invoke(
                eq(listOf(down(13))),
                eq(PointerEventPass.PreUp),
                any()
            )
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
        val event2 = down(5).moveTo(10.milliseconds, 7f, 9f)

        hitResult.dispatchChanges(
            listOf(event1, event2),
            PointerEventPass.InitialDown,
            PointerEventPass.PreUp
        )

        inOrder(pin1.pointerInputHandler, pin2.pointerInputHandler) {
            verify(pin1.pointerInputHandler).invoke(
                eq(listOf(event1)),
                eq(PointerEventPass.InitialDown),
                any()
            )
            verify(pin2.pointerInputHandler).invoke(
                eq(listOf(event1)),
                eq(PointerEventPass.InitialDown),
                any()
            )
            verify(pin2.pointerInputHandler).invoke(
                eq(listOf(event1)),
                eq(PointerEventPass.PreUp),
                any()
            )
            verify(pin1.pointerInputHandler).invoke(
                eq(listOf(event1)),
                eq(PointerEventPass.PreUp),
                any()
            )
        }
        inOrder(pin3.pointerInputHandler, pin4.pointerInputHandler) {
            verify(pin3.pointerInputHandler).invoke(
                eq(listOf(event2)),
                eq(PointerEventPass.InitialDown),
                any()
            )
            verify(pin4.pointerInputHandler).invoke(
                eq(listOf(event2)),
                eq(PointerEventPass.InitialDown),
                any()
            )
            verify(pin4.pointerInputHandler).invoke(
                eq(listOf(event2)),
                eq(PointerEventPass.PreUp),
                any()
            )
            verify(pin3.pointerInputHandler).invoke(
                eq(listOf(event2)),
                eq(PointerEventPass.PreUp),
                any()
            )
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
        val event2 = down(5).moveTo(10.milliseconds, 7f, 9f)

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
                eq(listOf(event1, event2)),
                eq(PointerEventPass.InitialDown),
                any()
            )
            verify(child1.pointerInputHandler).invoke(
                eq(listOf(event1)),
                eq(PointerEventPass.InitialDown),
                any()
            )
            verify(child1.pointerInputHandler).invoke(
                eq(listOf(event1)),
                eq(PointerEventPass.PreUp),
                any()
            )
            verify(parent.pointerInputHandler).invoke(
                eq(listOf(event1, event2)),
                eq(PointerEventPass.PreUp),
                any()
            )
        }

        // Verifies that the events traverse between parent and child2 in the correct order.
        inOrder(
            parent.pointerInputHandler,
            child2.pointerInputHandler
        ) {
            verify(parent.pointerInputHandler).invoke(
                eq(listOf(event1, event2)),
                eq(PointerEventPass.InitialDown),
                any()
            )
            verify(child2.pointerInputHandler).invoke(
                eq(listOf(event2)),
                eq(PointerEventPass.InitialDown),
                any()
            )
            verify(child2.pointerInputHandler).invoke(
                eq(listOf(event2)),
                eq(PointerEventPass.PreUp),
                any()
            )
            verify(parent.pointerInputHandler).invoke(
                eq(listOf(event1, event2)),
                eq(PointerEventPass.PreUp),
                any()
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
        val event2 = down(5).moveTo(10.milliseconds, 7f, 9f)

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
                eq(listOf(event1, event2)),
                eq(PointerEventPass.InitialDown),
                any()
            )
            verify(child1.pointerInputHandler).invoke(
                eq(listOf(event1, event2)),
                eq(PointerEventPass.PreUp),
                any()
            )
        }

        // Verify that order is correct for child2.
        inOrder(
            child2.pointerInputHandler
        ) {
            verify(child2.pointerInputHandler).invoke(
                eq(listOf(event1, event2)),
                eq(PointerEventPass.InitialDown),
                any()
            )
            verify(child2.pointerInputHandler).invoke(
                eq(listOf(event1, event2)),
                eq(PointerEventPass.PreUp),
                any()
            )
        }

        // Verify that first pass hits child1 before second pass hits child2
        inOrder(
            child1.pointerInputHandler,
            child2.pointerInputHandler
        ) {
            verify(child1.pointerInputHandler).invoke(
                eq(listOf(event1, event2)),
                eq(PointerEventPass.InitialDown),
                any()
            )
            verify(child2.pointerInputHandler).invoke(
                eq(listOf(event1, event2)),
                eq(PointerEventPass.PreUp),
                any()
            )
        }

        // Verify that first pass hits child2 before second pass hits child1
        inOrder(
            child1.pointerInputHandler,
            child2.pointerInputHandler
        ) {
            verify(child2.pointerInputHandler).invoke(
                eq(listOf(event1, event2)),
                eq(PointerEventPass.InitialDown),
                any()
            )
            verify(child1.pointerInputHandler).invoke(
                eq(listOf(event1, event2)),
                eq(PointerEventPass.PreUp),
                any()
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
            modifyBlock = { changes, _, _ ->
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
            modifyBlock = { changes, pass, _ ->
                val yConsume = if (pass == PointerEventPass.InitialDown) 2f else 64f
                changes.map { it.consumePositionChange(0.px, yConsume.px) }
            }
        })
        pin2.pointerInputHandler = spy(MyPointerInputHandler().apply {
            modifyBlock = { changes, pass, _ ->
                val yConsume = if (pass == PointerEventPass.InitialDown) 4f else 32f
                changes.map { it.consumePositionChange(0.px, yConsume.px) }
            }
        })
        pin3.pointerInputHandler = spy(MyPointerInputHandler().apply {
            modifyBlock = { changes, pass, _ ->
                val yConsume = if (pass == PointerEventPass.InitialDown) 8f else 16f
                changes.map { it.consumePositionChange(0.px, yConsume.px) }
            }
        })
        hitResult.addHitPath(13, listOf(pin1, pin2, pin3))
        val change = down(13).moveTo(10.milliseconds, 0f, 130f)

        val result = hitResult.dispatchChanges(
            listOf(change),
            PointerEventPass.InitialDown,
            PointerEventPass.PreUp
        )

        verify(pin1.pointerInputHandler).invoke(
            eq(listOf(change)), eq(PointerEventPass.InitialDown), any()
        )
        verify(pin2.pointerInputHandler).invoke(
            eq(listOf(change.consumePositionChange(0.px, 2.px))),
            eq(PointerEventPass.InitialDown),
            any()
        )
        verify(pin3.pointerInputHandler).invoke(
            eq(listOf(change.consumePositionChange(0.px, 6.px))), // 2 + 4
            eq(PointerEventPass.InitialDown),
            any()
        )
        verify(pin3.pointerInputHandler).invoke(
            eq(listOf(change.consumePositionChange(0.px, 14.px))), // 2 + 4 + 8
            eq(PointerEventPass.PreUp),
            any()
        )
        verify(pin2.pointerInputHandler).invoke(
            eq(listOf(change.consumePositionChange(0.px, 30.px))), // 2 + 4 + 8 + 16
            eq(PointerEventPass.PreUp),
            any()
        )
        verify(pin1.pointerInputHandler).invoke(
            eq(listOf(change.consumePositionChange(0.px, 62.px))), // 2 + 4 + 8 + 16 + 32
            eq(PointerEventPass.PreUp),
            any()
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
            modifyBlock = { changes, pass, _ ->
                val yConsume = if (pass == PointerEventPass.InitialDown) 2f else 12f
                changes.map { it.consumePositionChange(0.px, yConsume.px) }
            }
        })
        pin2.pointerInputHandler = spy(MyPointerInputHandler().apply {
            modifyBlock = { changes, pass, _ ->
                val yConsume = if (pass == PointerEventPass.InitialDown) 3f else 6f
                changes.map { it.consumePositionChange(0.px, yConsume.px) }
            }
        })
        pin3.pointerInputHandler = spy(MyPointerInputHandler().apply {
            modifyBlock = { changes, pass, _ ->
                val yConsume = if (pass == PointerEventPass.InitialDown) -2f else -12f
                changes.map { it.consumePositionChange(0.px, yConsume.px) }
            }
        })
        pin4.pointerInputHandler = spy(MyPointerInputHandler().apply {
            modifyBlock = { changes, pass, _ ->
                val yConsume = if (pass == PointerEventPass.InitialDown) -3f else -6f
                changes.map { it.consumePositionChange(0.px, yConsume.px) }
            }
        })
        hitResult.addHitPath(3, listOf(pin1, pin2))
        hitResult.addHitPath(5, listOf(pin3, pin4))
        val event1 = down(3).moveTo(10.milliseconds, 0f, 24f)
        val event2 = down(5).moveTo(10.milliseconds, 0f, -24f)

        val result = hitResult.dispatchChanges(
            listOf(event1, event2),
            PointerEventPass.InitialDown,
            PointerEventPass.PreUp
        )

        verify(pin1.pointerInputHandler).invoke(
            eq(listOf(event1)),
            eq(PointerEventPass.InitialDown),
            any()
        )
        verify(pin2.pointerInputHandler).invoke(
            eq(listOf(event1.consumePositionChange(0.px, 2.px))),
            eq(PointerEventPass.InitialDown),
            any()
        )
        verify(pin2.pointerInputHandler).invoke(
            eq(listOf(event1.consumePositionChange(0.px, 5.px))),
            eq(PointerEventPass.PreUp),
            any()
        )
        verify(pin1.pointerInputHandler).invoke(
            eq(listOf(event1.consumePositionChange(0.px, 11.px))),
            eq(PointerEventPass.PreUp),
            any()
        )

        verify(pin3.pointerInputHandler).invoke(
            eq(listOf(event2)),
            eq(PointerEventPass.InitialDown),
            any()
        )
        verify(pin4.pointerInputHandler).invoke(
            eq(listOf(event2.consumePositionChange(0.px, -2.px))),
            eq(PointerEventPass.InitialDown),
            any()
        )
        verify(pin4.pointerInputHandler).invoke(
            eq(listOf(event2.consumePositionChange(0.px, -5.px))),
            eq(PointerEventPass.PreUp),
            any()
        )
        verify(pin3.pointerInputHandler).invoke(
            eq(listOf(event2.consumePositionChange(0.px, (-11).px))),
            eq(PointerEventPass.PreUp),
            any()
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
            modifyBlock = { changes, pass, _ ->
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
            modifyBlock = { changes, pass, _ ->
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
            modifyBlock = { changes, pass, _ ->
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
        val event1 = down(3).moveTo(10.milliseconds, 0f, 1000f)
        val event2 = down(5).moveTo(10.milliseconds, 0f, -1000f)

        val result = hitResult.dispatchChanges(
            listOf(event1, event2),
            PointerEventPass.InitialDown,
            PointerEventPass.PreUp
        )

        verify(parent.pointerInputHandler).invoke(
            eq(listOf(event1, event2)),
            eq(PointerEventPass.InitialDown),
            any()
        )
        verify(child1.pointerInputHandler).invoke(
            eq(listOf(event1.consumePositionChange(0.px, 500.px))),
            eq(PointerEventPass.InitialDown),
            any()
        )
        verify(child2.pointerInputHandler).invoke(
            eq(listOf(event2.consumePositionChange(0.px, -500.px))),
            eq(PointerEventPass.InitialDown),
            any()
        )
        verify(child1.pointerInputHandler).invoke(
            eq(listOf(event1.consumePositionChange(0.px, 600.px))),
            eq(PointerEventPass.PreUp),
            any()
        )
        verify(child2.pointerInputHandler).invoke(
            eq(listOf(event2.consumePositionChange(0.px, -545.px))),
            eq(PointerEventPass.PreUp),
            any()
        )
        verify(parent.pointerInputHandler).invoke(
            eq(
                listOf(
                    event1.consumePositionChange(0.px, 657.px),
                    event2.consumePositionChange(0.px, -580.px)
                )
            ),
            eq(PointerEventPass.PreUp),
            any()
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
            modifyBlock = { changes, pass, _ ->
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
            modifyBlock = { changes, pass, _ ->
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
        val event1 = down(3).moveTo(10.milliseconds, 0f, 1000f)
        val event2 = down(5).moveTo(10.milliseconds, 0f, -1000f)

        val result = hitResult.dispatchChanges(
            listOf(event1, event2),
            PointerEventPass.InitialDown,
            PointerEventPass.PreUp
        )

        verify(child1.pointerInputHandler).invoke(
            eq(listOf(event1, event2)),
            eq(PointerEventPass.InitialDown),
            any()
        )
        verify(child2.pointerInputHandler).invoke(
            eq(
                listOf(
                    event1.consumePositionChange(0.px, 500.px),
                    event2.consumePositionChange(0.px, -500.px)
                )
            ),
            eq(PointerEventPass.InitialDown),
            any()
        )

        verify(child2.pointerInputHandler).invoke(
            eq(
                listOf(
                    event1.consumePositionChange(0.px, 600.px),
                    event2.consumePositionChange(0.px, -600.px)
                )
            ),
            eq(PointerEventPass.PreUp),
            any()
        )
        verify(child1.pointerInputHandler).invoke(
            eq(
                listOf(
                    event1.consumePositionChange(0.px, 657.px),
                    event2.consumePositionChange(0.px, -657.px)
                )
            ),
            eq(PointerEventPass.PreUp),
            any()
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
    fun refreshOffsets_nothingOffset_changeTranslatedAndSizesReportedCorrectly() {
        refreshOffsets_changeTranslatedAndSizesReportedCorrectly(
            0, 0, 100, 100,
            0, 0, 100, 100,
            0, 0, 100, 100,
            0, 0,
            50, 50
        )
    }

    @Test
    fun refreshOffsets_layoutNodesIncreasinglyInset_changeTranslatedAndSizesReportedCorrectly() {
        refreshOffsets_changeTranslatedAndSizesReportedCorrectly(
            0, 0, 100, 100,
            2, 11, 100, 100,
            23, 31, 100, 100,
            0, 0,
            99, 99
        )
    }

    @Test
    fun refreshOffsets_layoutNodesIncreasinglyOutset_changeTranslatedAndSizesReportedCorrectly() {
        refreshOffsets_changeTranslatedAndSizesReportedCorrectly(
            0, 0, 100, 100,
            -2, -11, 100, 100,
            -23, -31, 100, 100,
            0, 0,
            1, 1
        )
    }

    @Test
    fun refreshOffsets_additionalOffset_changeTranslatedAndSizesReportedCorrectly() {
        refreshOffsets_changeTranslatedAndSizesReportedCorrectly(
            0, 0, 100, 100,
            0, 0, 100, 100,
            0, 0, 100, 100,
            41, 51,
            50, 50
        )
    }

    @Test
    fun refreshOffsets_allIncreasinglyInset_changeTranslatedAndSizesReportedCorrectly() {
        refreshOffsets_changeTranslatedAndSizesReportedCorrectly(
            0, 0, 100, 100,
            2, 11, 100, 100,
            23, 31, 100, 100,
            41, 51,
            99, 99
        )
    }

    @Test
    fun refreshOffsets_allIncreasinglyOutset_changeTranslatedAndSizesReportedCorrectly() {
        refreshOffsets_changeTranslatedAndSizesReportedCorrectly(
            0, 0, 100, 100,
            -2, -11, 100, 100,
            -23, -31, 100, 100,
            -41, -51,
            1, 1
        )
    }

    // ParentLn -> ParentPin -> MiddleLn -> MiddlePin -> ChildLn -> ChildPin
    @Suppress("SameParameterValue")
    private fun refreshOffsets_changeTranslatedAndSizesReportedCorrectly(
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
        additionalOffsetX: Int,
        additionalOffsetY: Int,
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
        val additionalOffset = IntPxPosition(additionalOffsetX.ipx, additionalOffsetY.ipx)

        // Act

        hitResult.refreshOffsets(additionalOffset)

        // Assert

        hitResult.dispatchChanges(
            listOf(down(3, 7.milliseconds, pointerX.toFloat(), pointerY.toFloat())),
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
                current = PointerInputData(
                    Uptime.Boot + 7.milliseconds,
                    offset - additionalOffset,
                    true
                ),
                previous = PointerInputData(null, null, false),
                consumed = ConsumedData()
            ),
            PointerInputChange(
                id = 3,
                current = PointerInputData(
                    Uptime.Boot + 7.milliseconds,
                    offset - middleOffset - additionalOffset,
                    true
                ),
                previous = PointerInputData(null, null, false),
                consumed = ConsumedData()
            ),
            PointerInputChange(
                id = 3,
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
            IntPxSize((pX2 - pX1).ipx, (pY2 - pY1).ipx),
            IntPxSize((mX2 - mX1).ipx, (mY2 - mY1).ipx),
            IntPxSize((cX2 - cX1).ipx, (cY2 - cY1).ipx)
        )

        for (pass in listOf(PointerEventPass.InitialDown, PointerEventPass.PreUp)) {
            for (i in pointerInputNodes.indices) {
                verify(pointerInputNodes[i].pointerInputHandler).invoke(
                    listOf(expectedPointerInputChanges[i]),
                    pass,
                    expectedSizes[i]
                )
            }
        }
        for (pointerInputNode in pointerInputNodes) {
            verifyNoMoreInteractions(pointerInputNode.pointerInputHandler)
        }
    }

    @Test
    fun refreshOffsets_translateLayoutNodes_changeTranslatedAndSizesReportedCorrectly() {
        refreshOffsets_changeTranslatedAndSizesReportedCorrectly(
            Box(0, 0, 500, 500),
            Box(100, 100, 400, 400),
            Point(0, 0),
            Box(50, -50, 550, 450), // Translate by 50, -50
            Box(105, 95, 405, 395), // Translate by 5, -5
            Point(0, 0),
            250, 250
        )
    }

    @Test
    fun refreshOffsets_resizeLayoutNodes_changeTranslatedAndSizesReportedCorrectly() {
        refreshOffsets_changeTranslatedAndSizesReportedCorrectly(
            Box(0, 0, 500, 500),
            Box(100, 100, 400, 400),
            Point(0, 0),
            Box(0, 0, 450, 550), // Add to size by -50, 50
            Box(100, 100, 395, 405), // Add to size by -5, 5
            Point(0, 0),
            250, 250
        )
    }

    @Test
    fun refreshOffsets_translateAndResizeLayoutNodes_changeTranslatedAndSizesReportedCorrectly() {
        refreshOffsets_changeTranslatedAndSizesReportedCorrectly(
            Box(0, 0, 500, 500),
            Box(100, 100, 400, 400),
            Point(0, 0),
            Box(-50, 50, 550, 450), // Centered, scale by 100, -100
            Box(105, 95, 395, 405), // Centered, scale by -10, 10
            Point(0, 0),
            250, 250
        )
    }

    @Test
    fun refreshOffsets_updateOffsets_changeTranslatedAndSizesReportedCorrectly() {
        refreshOffsets_changeTranslatedAndSizesReportedCorrectly(
            Box(0, 0, 500, 500),
            Box(100, 100, 400, 400),
            Point(0, 0),
            Box(0, 0, 500, 500),
            Box(100, 100, 400, 400),
            Point(5, 15),
            250, 250
        )
    }

    @Test
    fun refreshOffsets_translateAll_changeTranslatedAndSizesReportedCorrectly() {
        refreshOffsets_changeTranslatedAndSizesReportedCorrectly(
            Box(0, 0, 500, 500),
            Box(100, 100, 400, 400),
            Point(0, 0),
            Box(50, -50, 550, 450), // Translate by 50, -50
            Box(105, 95, 405, 395), // Translate by 5, -5
            Point(100, -100), // updated by 100, -100
            250, 250
        )
    }

    @Test
    fun refreshOffsets_resizeLayoutNodesUpdateOffsets_changeTranslatedAndSizesReportedCorrectly() {
        refreshOffsets_changeTranslatedAndSizesReportedCorrectly(
            Box(0, 0, 500, 500),
            Box(100, 100, 400, 400),
            Point(0, 0),
            Box(0, 0, 450, 550), // Add to size by -50, 50
            Box(100, 100, 395, 405), // Add to size by -5, 5
            Point(100, -100),
            250, 250
        )
    }

    @Test
    fun refreshOffsets_translateAllResizeLayoutNodes_changeTranslatedAndSizesReportedCorrectly() {
        refreshOffsets_changeTranslatedAndSizesReportedCorrectly(
            Box(0, 0, 500, 500),
            Box(100, 100, 400, 400),
            Point(0, 0),
            Box(-50, 50, 550, 450), // Centered, scale by 100, -100
            Box(105, 95, 395, 405), // Centered, scale by -10, 10
            Point(100, -100),
            250, 250
        )
    }

    // ParentLn -> ParentPin -> ChildLn -> ChildPin
    @Suppress("SameParameterValue")
    private fun refreshOffsets_changeTranslatedAndSizesReportedCorrectly(
        p1: Box,
        c1: Box,
        aO1: Point,
        p2: Box,
        c2: Box,
        aO2: Point,
        pointerX: Int,
        pointerY: Int
    ) {

        // Arrange

        val pointerPosition = PxPosition(pointerX.px, pointerY.px)
        val parentOffset2 = PxPosition(p2.left.px, p2.top.px)
        val childOffset2 = PxPosition(c2.left.px, c2.top.px)
        val additionalOffset2 = IntPxPosition(aO2.x.ipx, aO2.y.ipx)
        val parentSize2 = IntPxSize(p2.right.ipx - p2.left.ipx, p2.bottom.ipx - p2.top.ipx)
        val childSize2 = IntPxSize(c2.right.ipx - c2.left.ipx, c2.bottom.ipx - c2.top.ipx)

        val childLayoutNode = LayoutNode(c1.left, c1.top, c1.right, c1.bottom)
        val childPointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, childLayoutNode)
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        val parentLayoutNode = LayoutNode(p1.left, p1.top, p1.right, p1.bottom).apply {
            emitInsertAt(0, childPointerInputNode)
        }
        val parentPointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, parentLayoutNode)
            pointerInputHandler = spy(MyPointerInputHandler())
        }

        hitResult.addHitPath(3, listOf(parentPointerInputNode, childPointerInputNode))

        hitResult.refreshOffsets(IntPxPosition(aO1.x.ipx, aO1.y.ipx))

        childLayoutNode.place(c2.left.ipx, c2.top.ipx)
        childLayoutNode
            .resize(c2.right.ipx - c2.left.ipx, c2.bottom.ipx - c2.top.ipx)
        parentLayoutNode.place(p2.left.ipx, p2.top.ipx)
        parentLayoutNode
            .resize(p2.right.ipx - p2.left.ipx, p2.bottom.ipx - p2.top.ipx)

        // Act

        hitResult.refreshOffsets(additionalOffset2)

        // Assert

        val pointerInputNodes = arrayOf(parentPointerInputNode, childPointerInputNode)

        val expectedPointerInputChanges = arrayOf(
            PointerInputChange(
                id = 3,
                current = PointerInputData(
                    Uptime.Boot + 7.milliseconds,
                    pointerPosition - parentOffset2 - additionalOffset2,
                    true
                ),
                previous = PointerInputData(null, null, false),
                consumed = ConsumedData()
            ),
            PointerInputChange(
                id = 3,
                current = PointerInputData(
                    Uptime.Boot + 7.milliseconds,
                    pointerPosition - parentOffset2 - childOffset2 - additionalOffset2,
                    true
                ),
                previous = PointerInputData(null, null, false),
                consumed = ConsumedData()
            )
        )

        val expectedSizes = arrayOf(
            parentSize2,
            childSize2
        )

        hitResult.dispatchChanges(
            listOf(
                down(
                    3,
                    7.milliseconds,
                    pointerX.toFloat(),
                    pointerY.toFloat()
                )
            ),
            PointerEventPass.InitialDown, PointerEventPass.PreUp
        )

        for (pass in listOf(PointerEventPass.InitialDown, PointerEventPass.PreUp)) {
            for (i in pointerInputNodes.indices) {
                verify(pointerInputNodes[i].pointerInputHandler).invoke(
                    listOf(expectedPointerInputChanges[i]),
                    pass,
                    expectedSizes[i]
                )
            }
        }
        for (pointerInputNode in pointerInputNodes) {
            verifyNoMoreInteractions(pointerInputNode.pointerInputHandler)
        }
    }

    // Pin -> Ln -> Pin -> Ln
    // Pin -> Ln -> Pin -> Ln
    @Test
    fun refreshOffsets_2IndependentPaths_changeTranslatedAndSizeReportedCorrectly() {

        // Arrange

        val child1Offset = IntPxPosition(3.ipx, 5.ipx)
        val child1Size = IntPxSize(3.ipx, 5.ipx)
        val parent1Offset = IntPxPosition(-7.ipx, -11.ipx)
        val parent1Size = IntPxSize(7.ipx, 11.ipx)
        val child2Offset = IntPxPosition(-13.ipx, -17.ipx)
        val child2Size = IntPxSize(13.ipx, 17.ipx)
        val parent2Offset = IntPxPosition(19.ipx, 27.ipx)
        val parent2Size = IntPxSize(19.ipx, 27.ipx)
        val pointer1Offset = PxPosition(5.px, 7.px)
        val pointer2Offset = PxPosition(11.px, 13.px)

        val childLayoutNode1 = LayoutNode(child1Offset, child1Size)
        val childPointerInputNode1 = PointerInputNode().apply {
            emitInsertAt(0, childLayoutNode1)
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        val parentLayoutNode1 = LayoutNode(parent1Offset, parent1Size).apply {
            emitInsertAt(0, childPointerInputNode1)
        }
        val parentPointerInputNode1 = PointerInputNode().apply {
            emitInsertAt(0, parentLayoutNode1)
            pointerInputHandler = spy(MyPointerInputHandler())
        }

        val childLayoutNode2 = LayoutNode(child2Offset, child2Size)
        val childPointerInputNode2 = PointerInputNode().apply {
            emitInsertAt(0, childLayoutNode2)
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        val parentLayoutNode2 = LayoutNode(parent2Offset, parent2Size).apply {
            emitInsertAt(0, childPointerInputNode2)
        }
        val parentPointerInputNode2 = PointerInputNode().apply {
            emitInsertAt(0, parentLayoutNode2)
            pointerInputHandler = spy(MyPointerInputHandler())
        }

        hitResult.addHitPath(3, listOf(parentPointerInputNode1, childPointerInputNode1))
        hitResult.addHitPath(5, listOf(parentPointerInputNode2, childPointerInputNode2))

        val additionalOffset = IntPxPosition(29.ipx, 31.ipx)

        // Act

        hitResult.refreshOffsets(additionalOffset)

        // Assert

        val pointerInputNodes1 = arrayOf(parentPointerInputNode1, childPointerInputNode1)

        val expectedPointerInputChanges1 = arrayOf(
            PointerInputChange(
                id = 3,
                current = PointerInputData(
                    Uptime.Boot + 7.milliseconds,
                    pointer1Offset - parent1Offset - additionalOffset,
                    true
                ),
                previous = PointerInputData(null, null, false),
                consumed = ConsumedData()
            ),
            PointerInputChange(
                id = 3,
                current = PointerInputData(
                    Uptime.Boot + 7.milliseconds,
                    pointer1Offset - parent1Offset - child1Offset - additionalOffset,
                    true
                ),
                previous = PointerInputData(null, null, false),
                consumed = ConsumedData()
            )
        )

        val expectedSizes1 = arrayOf(
            parent1Size,
            child1Size
        )

        val pointerInputNodes2 = arrayOf(parentPointerInputNode2, childPointerInputNode2)

        val expectedPointerInputChanges2 = arrayOf(
            PointerInputChange(
                id = 5,
                current = PointerInputData(
                    Uptime.Boot + 7.milliseconds,
                    pointer2Offset - parent2Offset - additionalOffset,
                    true
                ),
                previous = PointerInputData(null, null, false),
                consumed = ConsumedData()
            ),
            PointerInputChange(
                id = 5,
                current = PointerInputData(
                    Uptime.Boot + 7.milliseconds,
                    pointer2Offset - parent2Offset - child2Offset - additionalOffset,
                    true
                ),
                previous = PointerInputData(null, null, false),
                consumed = ConsumedData()
            )
        )

        val expectedSizes2 = arrayOf(
            parent2Size,
            child2Size
        )

        hitResult.dispatchChanges(
            listOf(
                down(
                    3,
                    7.milliseconds,
                    pointer1Offset.x.value,
                    pointer1Offset.y.value
                ),
                down(
                    5,
                    7.milliseconds,
                    pointer2Offset.x.value,
                    pointer2Offset.y.value
                )
            ),
            PointerEventPass.InitialDown, PointerEventPass.PreUp
        )

        for (pass in listOf(PointerEventPass.InitialDown, PointerEventPass.PreUp)) {
            for (i in pointerInputNodes1.indices) {
                verify(pointerInputNodes1[i].pointerInputHandler).invoke(
                    listOf(expectedPointerInputChanges1[i]),
                    pass,
                    expectedSizes1[i]
                )
            }
            for (i in pointerInputNodes2.indices) {
                verify(pointerInputNodes2[i].pointerInputHandler).invoke(
                    listOf(expectedPointerInputChanges2[i]),
                    pass,
                    expectedSizes2[i]
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

    // Pin -> Ln
    //   -> Pin -> Ln
    //   -> Pin -> Ln
    @Test
    fun refreshOffsets_2SplittingPaths_changeTranslatedCorrectly() {

        // Arrange

        val child1Offset = IntPxPosition(3.ipx, 5.ipx)
        val child1Size = IntPxSize(3.ipx, 5.ipx)
        val child2Offset = IntPxPosition(-13.ipx, -17.ipx)
        val child2Size = IntPxSize(13.ipx, 17.ipx)
        val parentOffset = IntPxPosition(-7.ipx, 11.ipx)
        val parentSize = IntPxSize(7.ipx, 11.ipx)
        val pointer1Offset = PxPosition(5.px, 7.px)
        val pointer2Offset = PxPosition(11.px, 13.px)

        val childLayoutNode1 = LayoutNode(child1Offset, child1Size)
        val childPointerInputNode1: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, childLayoutNode1)
            pointerInputHandler = spy(MyPointerInputHandler())
        }

        val childLayoutNode2 = LayoutNode(child2Offset, child2Size)
        val childPointerInputNode2: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, childLayoutNode2)
            pointerInputHandler = spy(MyPointerInputHandler())
        }

        val parentLayoutNode: LayoutNode = LayoutNode(parentOffset, parentSize).apply {
            emitInsertAt(0, childPointerInputNode1)
            emitInsertAt(1, childPointerInputNode2)
        }
        val parentPointerInputNode: PointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, parentLayoutNode)
            pointerInputHandler = spy(MyPointerInputHandler())
        }

        hitResult.addHitPath(3, listOf(parentPointerInputNode, childPointerInputNode1))
        hitResult.addHitPath(5, listOf(parentPointerInputNode, childPointerInputNode2))

        val additionalOffset = IntPxPosition(29.ipx, 31.ipx)

        // Act

        hitResult.refreshOffsets(additionalOffset)

        // Assert

        val parentExpectedPointerInputChanges = listOf(
            PointerInputChange(
                id = 3,
                current = PointerInputData(
                    Uptime.Boot + 7.milliseconds,
                    pointer1Offset - parentOffset - additionalOffset,
                    true
                ),
                previous = PointerInputData(null, null, false),
                consumed = ConsumedData()
            ),
            PointerInputChange(
                id = 5,
                current = PointerInputData(
                    Uptime.Boot + 7.milliseconds,
                    pointer2Offset - parentOffset - additionalOffset,
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
                    Uptime.Boot + 7.milliseconds,
                    pointer1Offset - parentOffset - child1Offset - additionalOffset,
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
                    Uptime.Boot + 7.milliseconds,
                    pointer2Offset - parentOffset - child2Offset - additionalOffset,
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
                    7.milliseconds,
                    pointer1Offset.x.value,
                    pointer1Offset.y.value
                ),
                down(
                    5,
                    7.milliseconds,
                    pointer2Offset.x.value,
                    pointer2Offset.y.value
                )
            ),
            PointerEventPass.InitialDown, PointerEventPass.PreUp
        )

        verify(parentPointerInputNode.pointerInputHandler).invoke(
            parentExpectedPointerInputChanges, PointerEventPass.InitialDown, parentSize
        )
        verify(childPointerInputNode1.pointerInputHandler).invoke(
            child1ExpectedPointerInputChanges, PointerEventPass.InitialDown, child1Size
        )
        verify(childPointerInputNode2.pointerInputHandler).invoke(
            child2ExpectedPointerInputChanges, PointerEventPass.InitialDown, child2Size
        )
        verify(childPointerInputNode1.pointerInputHandler).invoke(
            child1ExpectedPointerInputChanges, PointerEventPass.PreUp, child1Size
        )
        verify(childPointerInputNode2.pointerInputHandler).invoke(
            child2ExpectedPointerInputChanges, PointerEventPass.PreUp, child2Size
        )
        verify(parentPointerInputNode.pointerInputHandler).invoke(
            parentExpectedPointerInputChanges, PointerEventPass.PreUp, parentSize
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
        val parentSize1 = IntPxSize(10.ipx, 20.ipx)
        val parentOffset2 = IntPxPosition(3.ipx, 4.ipx)
        val parentSize2 = IntPxSize(30.ipx, 40.ipx)
        val childOffset = IntPxPosition(5.ipx, 6.ipx)
        val childSize = IntPxSize(5.ipx, 10.ipx)

        val pointerOffset = PxPosition(5.px, 7.px)

        val childLayoutNode = LayoutNode(childOffset, childSize)
        val pointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, childLayoutNode)
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        val parentLayoutNode2 = LayoutNode(parentOffset2, parentSize2).apply {
            emitInsertAt(0, pointerInputNode)
        }
        @Suppress("UNUSED_VARIABLE")
        val parentLayoutNode1 = LayoutNode(parentOffset1, parentSize1).apply {
            emitInsertAt(0, parentLayoutNode2)
        }

        hitResult.addHitPath(3, listOf(pointerInputNode))

        val additionalOffset = IntPxPosition(29.ipx, 31.ipx)

        // Act

        hitResult.refreshOffsets(additionalOffset)

        // Assert

        val expectedPointerInputChange =
            PointerInputChange(
                id = 3,
                current = PointerInputData(
                    Uptime.Boot + 7.milliseconds,
                    pointerOffset - parentOffset1 - parentOffset2 - childOffset - additionalOffset,
                    true
                ),
                previous = PointerInputData(null, null, false),
                consumed = ConsumedData()
            )

        hitResult.dispatchChanges(
            listOf(
                down(
                    3,
                    7.milliseconds,
                    pointerOffset.x.value,
                    pointerOffset.y.value
                )
            ),
            PointerEventPass.InitialDown, PointerEventPass.PreUp
        )

        for (pass in listOf(PointerEventPass.InitialDown, PointerEventPass.PreUp)) {
            verify(pointerInputNode.pointerInputHandler).invoke(
                listOf(expectedPointerInputChange),
                pass,
                childSize
            )
        }
        verifyNoMoreInteractions(pointerInputNode.pointerInputHandler)
    }

    // Ln -> Ln -> Pin -> Ln -> Ln -> Pin -> Ln
    @Test
    fun refreshOffsets_2LayoutNodes1Pin2LayoutNodes1Pin1LayoutNode_changeTranslatedCorrectly() {

        // Arrange

        val parentOffset1 = IntPxPosition(1.ipx, 2.ipx)
        val parentSize1 = IntPxSize(10.ipx, 20.ipx)
        val parentOffset2 = IntPxPosition(3.ipx, 4.ipx)
        val parentSize2 = IntPxSize(20.ipx, 40.ipx)
        val parentOffset3 = IntPxPosition(5.ipx, 6.ipx)
        val parentSize3 = IntPxSize(30.ipx, 60.ipx)
        val childOffset1 = IntPxPosition(7.ipx, 8.ipx)
        val childSize1 = IntPxSize(5.ipx, 10.ipx)
        val childOffset2 = IntPxPosition(9.ipx, 10.ipx)
        val childSize2 = IntPxSize(6.ipx, 8.ipx)
        val pointerOffset = PxPosition(5.px, 7.px)

        val childLayoutNode2 = LayoutNode(childOffset2, childSize2)
        val childPointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, childLayoutNode2)
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        val childLayoutNode1 = LayoutNode(childOffset1, childSize1).apply {
            emitInsertAt(0, childPointerInputNode)
        }
        val parentLayoutNode3 = LayoutNode(parentOffset3, parentSize3).apply {
            emitInsertAt(0, childLayoutNode1)
        }
        val parentPointerInputNode = PointerInputNode().apply {
            emitInsertAt(0, parentLayoutNode3)
            pointerInputHandler = spy(MyPointerInputHandler())
        }
        val parentLayoutNode2 = LayoutNode(parentOffset2, parentSize2).apply {
            emitInsertAt(0, parentPointerInputNode)
        }
        @Suppress("UNUSED_VARIABLE")
        val parentLayoutNode1 = LayoutNode(parentOffset1, parentSize1).apply {
            emitInsertAt(0, parentLayoutNode2)
        }

        hitResult.addHitPath(3, listOf(parentPointerInputNode, childPointerInputNode))

        val additionalOffset = IntPxPosition(29.ipx, 31.ipx)

        // Act

        hitResult.refreshOffsets(additionalOffset)

        // Assert

        val pointerInputNodes = arrayOf(parentPointerInputNode, childPointerInputNode)

        val expectedPointerInputChanges = arrayOf(
            PointerInputChange(
                id = 3,
                current = PointerInputData(
                    Uptime.Boot + 7.milliseconds,
                    pointerOffset - parentOffset1 - parentOffset2 - parentOffset3 -
                            additionalOffset,
                    true
                ),
                previous = PointerInputData(null, null, false),
                consumed = ConsumedData()
            ),
            PointerInputChange(
                id = 3,
                current = PointerInputData(
                    Uptime.Boot + 7.milliseconds,
                    pointerOffset - parentOffset1 - parentOffset2 - parentOffset3 -
                            childOffset1 - childOffset2 - additionalOffset,
                    true
                ),
                previous = PointerInputData(null, null, false),
                consumed = ConsumedData()
            )
        )

        val expectedSizes = arrayOf(
            parentSize3,
            childSize2
        )

        hitResult.dispatchChanges(
            listOf(
                down(
                    3,
                    7.milliseconds,
                    pointerOffset.x.value,
                    pointerOffset.y.value
                )
            ),
            PointerEventPass.InitialDown, PointerEventPass.PreUp
        )

        for (pass in listOf(PointerEventPass.InitialDown, PointerEventPass.PreUp)) {
            for (i in pointerInputNodes.indices) {
                verify(pointerInputNodes[i].pointerInputHandler).invoke(
                    listOf(expectedPointerInputChanges[i]),
                    pass,
                    expectedSizes[i]
                )
            }
        }
        for (pointerInputNode in pointerInputNodes) {
            verifyNoMoreInteractions(pointerInputNode.pointerInputHandler)
        }
    }

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

    @Suppress("SameParameterValue")
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

        hitResult.refreshOffsets(IntPxPosition.Origin)

        // Assert

        val expectedPointerInputChange =
            PointerInputChange(
                id = 3,
                current = PointerInputData(
                    Uptime.Boot + 7.milliseconds,
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
                    7.milliseconds,
                    pointerX.toFloat(),
                    pointerY.toFloat()
                )
            ),
            PointerEventPass.InitialDown, PointerEventPass.PreUp
        )

        for (pass in listOf(PointerEventPass.InitialDown, PointerEventPass.PreUp)) {
            verify(pointerInputNode.pointerInputHandler).invoke(
                eq(listOf(expectedPointerInputChange)),
                eq(pass),
                any()
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

        hitResult.refreshOffsets(IntPxPosition.Origin)

        // Assert

        val expectedPointerInputChange =
            PointerInputChange(
                id = 3,
                current = PointerInputData(
                    Uptime.Boot + 7.milliseconds,
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
                    7.milliseconds,
                    150f,
                    250f
                )
            ),
            PointerEventPass.InitialDown, PointerEventPass.PreUp
        )

        for (pass in listOf(PointerEventPass.InitialDown, PointerEventPass.PreUp)) {
            verify(pointerInputNode.pointerInputHandler).invoke(
                eq(listOf(expectedPointerInputChange)),
                eq(pass),
                any()
            )
        }
        verifyNoMoreInteractions(pointerInputNode.pointerInputHandler)
    }

    @Test
    fun dispatchCancel_nothingTracked_doesNotCrash() {
        hitResult.dispatchCancel()
    }

    // Pin -> Ln
    @Test
    fun dispatchCancel_singlePin_cancelHandlerIsCalled() {
        val pointerInputNode = PointerInputNode().apply {
            cancelHandler = spy(MyCancelHandler())
        }
        hitResult.addHitPath(3, listOf(pointerInputNode))

        hitResult.dispatchCancel()

        verify(pointerInputNode.cancelHandler).invoke()
    }

    // Pin -> Pin -> Pin
    @Test
    fun dispatchCancel_3Pins_cancelHandlersCalledOnceInOrder() {
        val pointerInputNodeChild = PointerInputNode()
        val pointerInputNodeMiddle = PointerInputNode()
        val pointerInputNodeParent = PointerInputNode()
        pointerInputNodeChild.cancelHandler = spy(MyCancelHandler())
        pointerInputNodeMiddle.cancelHandler = spy(MyCancelHandler())
        pointerInputNodeParent.cancelHandler = spy(MyCancelHandler())
        hitResult.addHitPath(3,
            listOf(pointerInputNodeParent, pointerInputNodeMiddle, pointerInputNodeChild)
        )

        hitResult.dispatchCancel()

        inOrder(
            pointerInputNodeParent.cancelHandler,
            pointerInputNodeMiddle.cancelHandler,
            pointerInputNodeChild.cancelHandler
        ) {
            verify(pointerInputNodeChild.cancelHandler).invoke()
            verify(pointerInputNodeMiddle.cancelHandler).invoke()
            verify(pointerInputNodeParent.cancelHandler).invoke()
        }
    }

    // PIN -> PIN
    // PIN -> PIN
    @Test
    fun dispatchCancel_2IndependentPathsFromRoot_cancelHandlersCalledOnceInOrder() {
        val pinParent1 = PointerInputNode()
        val pinChild1 = PointerInputNode()
        val pinParent2 = PointerInputNode()
        val pinChild2 = PointerInputNode()
        pinParent1.cancelHandler = spy(MyCancelHandler())
        pinChild1.cancelHandler = spy(MyCancelHandler())
        pinParent2.cancelHandler = spy(MyCancelHandler())
        pinChild2.cancelHandler = spy(MyCancelHandler())
        hitResult.addHitPath(3, listOf(pinParent1, pinChild1))
        hitResult.addHitPath(5, listOf(pinParent2, pinChild2))

        hitResult.dispatchCancel()

        inOrder(pinParent1.cancelHandler, pinChild1.cancelHandler) {
            verify(pinChild1.cancelHandler).invoke()
            verify(pinParent1.cancelHandler).invoke()
        }
        inOrder(pinParent2.cancelHandler, pinChild2.cancelHandler) {
            verify(pinChild2.cancelHandler).invoke()
            verify(pinParent2.cancelHandler).invoke()
        }
        verifyNoMoreInteractions(
            pinParent1.cancelHandler,
            pinChild1.cancelHandler,
            pinParent2.cancelHandler,
            pinChild2.cancelHandler
        )
    }

    // PIN -> PIN
    //     -> PIN
    @Test
    fun dispatchCancel_2BranchingPaths_cancelHandlersCalledOnceInOrder() {
        val pinParent = PointerInputNode()
        val pinChild1 = PointerInputNode()
        val pinChild2 = PointerInputNode()
        pinParent.cancelHandler = spy(MyCancelHandler())
        pinChild1.cancelHandler = spy(MyCancelHandler())
        pinChild2.cancelHandler = spy(MyCancelHandler())
        hitResult.addHitPath(3, listOf(pinParent, pinChild1))
        hitResult.addHitPath(5, listOf(pinParent, pinChild2))

        hitResult.dispatchCancel()

        inOrder(pinParent.cancelHandler, pinChild1.cancelHandler) {
            verify(pinChild1.cancelHandler).invoke()
            verify(pinParent.cancelHandler).invoke()
        }
        inOrder(pinParent.cancelHandler, pinChild2.cancelHandler) {
            verify(pinChild2.cancelHandler).invoke()
            verify(pinParent.cancelHandler).invoke()
        }
        verifyNoMoreInteractions(
            pinParent.cancelHandler,
            pinChild1.cancelHandler,
            pinChild2.cancelHandler
        )
    }

    @Test
    fun clear_nothingTracked_doesNotCrash() {
        hitResult.clear()
    }

    // Pin -> Ln
    @Test
    fun clear_singlePin_cleared() {
        val pointerInputNode = PointerInputNode()
        hitResult.addHitPath(3, listOf(pointerInputNode))

        hitResult.clear()

        assertThat(areEqual(hitResult.root, Node())).isTrue()
    }

    // Pin -> Pin -> Pin
    @Test
    fun clear_3Pins_cleared() {
        val pointerInputNodeChild = PointerInputNode()
        val pointerInputNodeMiddle = PointerInputNode()
        val pointerInputNodeParent = PointerInputNode()
        hitResult.addHitPath(3,
            listOf(pointerInputNodeParent, pointerInputNodeMiddle, pointerInputNodeChild)
        )

        hitResult.clear()

        assertThat(areEqual(hitResult.root, Node())).isTrue()
    }

    // PIN -> PIN
    // PIN -> PIN
    @Test
    fun clear_2IndependentPathsFromRoot_cleared() {
        val pinParent1 = PointerInputNode()
        val pinChild1 = PointerInputNode()
        val pinParent2 = PointerInputNode()
        val pinChild2 = PointerInputNode()
        hitResult.addHitPath(3, listOf(pinParent1, pinChild1))
        hitResult.addHitPath(5, listOf(pinParent2, pinChild2))

        hitResult.clear()

        assertThat(areEqual(hitResult.root, Node())).isTrue()
    }

    // PIN -> PIN
    //     -> PIN
    @Test
    fun clear_2BranchingPaths_cleared() {
        val pinParent = PointerInputNode()
        val pinChild1 = PointerInputNode()
        val pinChild2 = PointerInputNode()
        hitResult.addHitPath(3, listOf(pinParent, pinChild1))
        hitResult.addHitPath(5, listOf(pinParent, pinChild2))

        hitResult.clear()

        assertThat(areEqual(hitResult.root, Node())).isTrue()
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

private data class Box(val left: Int, val top: Int, val right: Int, val bottom: Int)

private data class Point(val x: Int, val y: Int)