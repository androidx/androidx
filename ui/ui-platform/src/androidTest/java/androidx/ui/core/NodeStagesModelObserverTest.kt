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

package androidx.ui.core

import androidx.compose.FrameManager
import androidx.compose.annotations.Hide
import androidx.compose.frames.AbstractRecord
import androidx.compose.frames.Framed
import androidx.compose.frames.Record
import androidx.compose.frames._created
import androidx.compose.frames.commit
import androidx.compose.frames.open
import androidx.compose.frames.readable
import androidx.compose.frames.writable
import androidx.test.filters.SmallTest
import androidx.ui.core.NodeStagesModelObserver.Stage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(JUnit4::class)
class NodeStagesModelObserverTest {

    @Test
    fun modelChangeTriggersCallback() {
        val node = DrawNode()
        val countDownLatch = CountDownLatch(1)

        val model = State(0)
        val modelObserver = NodeStagesModelObserver { stage, affectedNode ->
            assertEquals(Stage.Draw, stage)
            assertEquals(node, affectedNode)
            assertEquals(1, countDownLatch.count)
            countDownLatch.countDown()
        }

        modelObserver.enableModelUpdatesObserving(true)

        open() // open the frame

        modelObserver.observeReads {
            modelObserver.stage(Stage.Draw, node) {
                // read the value
                model.value
            }
        }

        model.value++
        commit() // close the frame

        modelObserver.enableModelUpdatesObserving(false)
        assertTrue(countDownLatch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun allThreeStagesWorksTogether() {
        val drawNode = DrawNode()
        val measureNode = LayoutNode()
        val layoutNode = LayoutNode()
        val drawLatch = CountDownLatch(1)
        val measureLatch = CountDownLatch(1)
        val layoutLatch = CountDownLatch(1)
        val drawModel = State(0)
        val measureModel = State(0)
        val layoutModel = State(0)

        val modelObserver = NodeStagesModelObserver { stage, affectedNode ->
            when (stage) {
                Stage.Draw -> {
                    assertEquals(drawNode, affectedNode)
                    assertEquals(1, drawLatch.count)
                    drawLatch.countDown()
                }
                Stage.Measure -> {
                    assertEquals(measureNode, affectedNode)
                    assertEquals(1, measureLatch.count)
                    measureLatch.countDown()
                }
                Stage.Layout -> {
                    assertEquals(layoutNode, affectedNode)
                    assertEquals(1, layoutLatch.count)
                    layoutLatch.countDown()
                }
            }
        }

        modelObserver.enableModelUpdatesObserving(true)

        open() // open the frame

        modelObserver.observeReads {
            modelObserver.stage(Stage.Layout, layoutNode) {
                layoutModel.value
            }
            modelObserver.stage(Stage.Measure, measureNode) {
                measureModel.value
            }
            modelObserver.stage(Stage.Draw, drawNode) {
                drawModel.value
            }
        }

        drawModel.value++
        measureModel.value++
        layoutModel.value++
        commit() // close the frame

        modelObserver.enableModelUpdatesObserving(false)
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        assertTrue(measureLatch.await(1, TimeUnit.SECONDS))
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun enclosedStagesCorrectlyObserveChanges() {
        val layoutNode1 = LayoutNode()
        val layoutNode2 = LayoutNode()
        val measureNode = LayoutNode()
        val layoutLatch1 = CountDownLatch(1)
        val layoutLatch2 = CountDownLatch(1)
        val measureLatch = CountDownLatch(1)
        val layoutModel1 = State(0)
        val layoutModel2 = State(0)
        val measureModel = State(0)

        val modelObserver = NodeStagesModelObserver { stage, affectedNode ->
            when (stage) {
                Stage.Layout -> {
                    when (affectedNode) {
                        layoutNode1 -> {
                            assertEquals(1, layoutLatch1.count)
                            layoutLatch1.countDown()
                        }
                        layoutNode2 -> {
                            assertEquals(1, layoutLatch2.count)
                            layoutLatch2.countDown()
                        }
                        measureNode -> {
                            throw IllegalStateException("measureNode called with Stage.Layout")
                        }
                    }
                }
                Stage.Measure -> {
                    assertEquals(affectedNode, measureNode)
                    assertEquals(measureLatch.count, 1)
                    measureLatch.countDown()
                }
                else -> throw IllegalStateException("Unexpected stage $stage")
            }
        }

        modelObserver.enableModelUpdatesObserving(true)

        open() // open the frame

        modelObserver.observeReads {
            modelObserver.stage(Stage.Layout, layoutNode1) {
                layoutModel1.value
                modelObserver.stage(Stage.Layout, layoutNode2) {
                    layoutModel2.value
                    modelObserver.stage(Stage.Measure, measureNode) {
                        measureModel.value
                    }
                }
            }
        }

        layoutModel1.value++
        layoutModel2.value++
        measureModel.value++
        commit() // close the frame

        modelObserver.enableModelUpdatesObserving(false)
        assertTrue(layoutLatch1.await(1, TimeUnit.SECONDS))
        assertTrue(layoutLatch2.await(1, TimeUnit.SECONDS))
        assertTrue(measureLatch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun modelReadTriggersCallbackAfterSwitchingFrameWithinObserveReads() {
        val node = DrawNode()
        val countDownLatch = CountDownLatch(1)

        val model = State(0)
        val modelObserver = NodeStagesModelObserver { _, _ ->
            assertEquals(1, countDownLatch.count)
            countDownLatch.countDown()
        }

        modelObserver.enableModelUpdatesObserving(true)

        open() // open the frame

        modelObserver.observeReads {
            modelObserver.stage(Stage.Draw, node) {
                // switch to the next frame.
                // this will be done by subcomposition, for example.
                FrameManager.nextFrame()
                // read the value
                model.value
            }
        }

        model.value++
        commit() // close the frame

        modelObserver.enableModelUpdatesObserving(false)
        assertTrue(countDownLatch.await(1, TimeUnit.SECONDS))
    }
}

// @Model generation is not enabled for this module and androidx.compose.State is internal
// TODO make State's constructor public and remove the copied code. b/142883125
private class State<T> constructor(value: T) : Framed {

    @Suppress("UNCHECKED_CAST")
    var value: T
        get() = next.readable(this).value
        set(value) {
            next.writable(this).value = value
        }

    private var next: StateRecord<T> =
        StateRecord(value)

    init {
        _created(this)
    }

    // NOTE(lmr): ideally we can compile `State` with our own compiler so that this is not visible
    @Hide
    override val firstFrameRecord: Record
        get() = next

    // NOTE(lmr): ideally we can compile `State` with our own compiler so that this is not visible
    @Hide
    override fun prependFrameRecord(value: Record) {
        value.next = next
        @Suppress("UNCHECKED_CAST")
        next = value as StateRecord<T>
    }

    private class StateRecord<T>(myValue: T) : AbstractRecord() {
        override fun assign(value: Record) {
            @Suppress("UNCHECKED_CAST")
            this.value = (value as StateRecord<T>).value
        }

        override fun create(): Record =
            StateRecord(value)

        var value: T = myValue
    }
}