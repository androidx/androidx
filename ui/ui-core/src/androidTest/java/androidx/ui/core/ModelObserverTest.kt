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
import androidx.compose.MutableState
import androidx.compose.frames.commit
import androidx.compose.frames.open
import androidx.compose.mutableStateOf
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(JUnit4::class)
class ModelObserverTest {

    @Test
    fun modelChangeTriggersCallback() {
        val node = "Hello World"
        val countDownLatch = CountDownLatch(1)

        val model = mutableStateOf(0)
        val modelObserver = ModelObserver { it() }
        modelObserver.enableModelUpdatesObserving(true)

        open() // open the frame

        val onCommitListener: (String) -> Unit = { affectedNode ->
            assertEquals(node, affectedNode)
            assertEquals(1, countDownLatch.count)
            countDownLatch.countDown()
        }

        modelObserver.observeReads(node, onCommitListener) {
            // read the value
            model.value
        }

        model.value++
        commit() // close the frame

        modelObserver.enableModelUpdatesObserving(false)
        assertTrue(countDownLatch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun allThreeStagesWorksTogether() {
        val str = "Hello World"
        val measureNode = LayoutNode()
        val layoutNode = LayoutNode()
        val drawLatch = CountDownLatch(1)
        val measureLatch = CountDownLatch(1)
        val layoutLatch = CountDownLatch(1)
        val drawModel = mutableStateOf(0)
        val measureModel = mutableStateOf(0)
        val layoutModel = mutableStateOf(0)

        val onCommitDrawListener: (String) -> Unit = { affectedNode ->
            assertEquals(str, affectedNode)
            assertEquals(1, drawLatch.count)
            drawLatch.countDown()
        }
        val onCommitMeasureListener: (LayoutNode) -> Unit = { affectedNode ->
            assertEquals(measureNode, affectedNode)
            assertEquals(1, measureLatch.count)
            measureLatch.countDown()
        }
        val onCommitLayoutListener: (LayoutNode) -> Unit = { affectedNode ->
            assertEquals(layoutNode, affectedNode)
            assertEquals(1, layoutLatch.count)
            layoutLatch.countDown()
        }
        val modelObserver = ModelObserver { it() }

        modelObserver.enableModelUpdatesObserving(true)

        open() // open the frame

        modelObserver.observeReads(layoutNode, onCommitLayoutListener) {
            layoutModel.value
        }

        modelObserver.observeReads(measureNode, onCommitMeasureListener) {
            measureModel.value
        }

        modelObserver.observeReads(str, onCommitDrawListener) {
            drawModel.value
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
        val layoutModel1 = mutableStateOf(0)
        val layoutModel2 = mutableStateOf(0)
        val measureModel = mutableStateOf(0)

        val onCommitMeasureListener: (LayoutNode) -> Unit = { affectedNode ->
            assertEquals(affectedNode, measureNode)
            assertEquals(measureLatch.count, 1)
            measureLatch.countDown()
        }
        val onCommitLayoutListener: (LayoutNode) -> Unit = { affectedNode ->
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
        val modelObserver = ModelObserver { it() }

        modelObserver.enableModelUpdatesObserving(true)

        open() // open the frame

        modelObserver.observeReads(layoutNode1, onCommitLayoutListener) {
            layoutModel1.value
            modelObserver.observeReads(layoutNode2, onCommitLayoutListener) {
                layoutModel2.value
                modelObserver.observeReads(measureNode, onCommitMeasureListener) {
                    measureModel.value
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
        val node = "Hello"
        val countDownLatch = CountDownLatch(1)

        val model = mutableStateOf(0)
        val onCommitListener: (String) -> Unit = { _ ->
            assertEquals(1, countDownLatch.count)
            countDownLatch.countDown()
        }
        val modelObserver = ModelObserver { it() }

        modelObserver.enableModelUpdatesObserving(true)

        open() // open the frame

        modelObserver.observeReads(node, onCommitListener) {
            // switch to the next frame.
            // this will be done by subcomposition, for example.
            FrameManager.nextFrame()
            // read the value
            model.value
        }

        model.value++
        commit() // close the frame

        modelObserver.enableModelUpdatesObserving(false)
        assertTrue(countDownLatch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun pauseStopsObserving() {
        val node = LayoutNode()
        var commits = 0

        runSimpleTest { modelObserver, model ->
            modelObserver.observeReads(node, { _ -> commits++ }) {
                modelObserver.pauseObservingReads {
                    model.value
                }
            }
        }

        assertEquals(0, commits)
    }

    @Test
    fun nestedPauseStopsObserving() {
        val node = LayoutNode()
        var commits = 0

        runSimpleTest { modelObserver, model ->
            modelObserver.observeReads(node, { _ -> commits++ }) {
                modelObserver.pauseObservingReads {
                    modelObserver.pauseObservingReads {
                        model.value
                    }
                    model.value
                }
            }
        }

        assertEquals(0, commits)
    }

    @Test
    fun simpleObserving() {
        val node = LayoutNode()
        var commits = 0

        runSimpleTest { modelObserver, model ->
            modelObserver.observeReads(node, { _ -> commits++ }) {
                model.value
            }
        }

        assertEquals(1, commits)
    }

    @Test
    fun observeWithinPause() {
        val node = LayoutNode()
        var commits = 0
        var commits2 = 0

        runSimpleTest { modelObserver, model ->
            modelObserver.observeReads(node, { _ -> commits++ }) {
                modelObserver.pauseObservingReads {
                    modelObserver.observeReads(node, { _ -> commits2++ }) {
                        model.value
                    }
                }
            }
        }
        assertEquals(0, commits)
        assertEquals(1, commits2)
    }

    private fun runSimpleTest(
        block: (modelObserver: ModelObserver, model: MutableState<Int>) -> Unit
    ) {
        val modelObserver = ModelObserver { it() }
        val model = mutableStateOf(0)

        modelObserver.enableModelUpdatesObserving(true)
        try {
            open() // open the frame
            block(modelObserver, model)
            model.value++
            commit() // close the frame
        } finally {
            modelObserver.enableModelUpdatesObserving(false)
        }
    }
}
