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

@file:Suppress("DEPRECATION") // https://youtrack.jetbrains.com/issue/CMP-6181

package androidx.compose.ui.input.mouse

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.scene.LocalComposeScene
import androidx.compose.ui.scene.lastKnownPointerPosition
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.use
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.Executors
import kotlin.random.Random
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Test

@OptIn(ExperimentalComposeUiApi::class, ExperimentalTestApi::class)
class MouseMoveTest {
    @Test
    fun `inside window`() = ImageComposeScene(
        width = 100,
        height = 100,
        density = Density(2f)
    ).use { scene ->
        val collector = EventCollector()

        scene.setContent {
            Box(
                modifier = Modifier
                    .collectPointerEvents(collector)
                    .size(10.dp, 20.dp)
            )
        }

        scene.sendPointerEvent(PointerEventType.Enter, Offset(0f, 0f))
        scene.sendPointerEvent(PointerEventType.Move, Offset(10f, 20f))
        collector.assertCounts(enter = 1, exit = 0, move = 1)

        scene.sendPointerEvent(PointerEventType.Move, Offset(10f, 15f))
        collector.assertCounts(enter = 1, exit = 0, move = 2)

        scene.sendPointerEvent(PointerEventType.Move, Offset(30f, 30f))
        collector.assertCounts(enter = 1, exit = 1, move = 2)
    }

    @Test
    fun `window enter`() = ImageComposeScene(
        width = 100,
        height = 100,
        density = Density(2f)
    ).use { scene ->
        val collector = EventCollector()

        scene.setContent {
            Box(
                modifier = Modifier
                    .collectPointerEvents(collector)
                    .size(10.dp, 20.dp)
            )
        }

        scene.sendPointerEvent(PointerEventType.Enter, Offset(10f, 20f))
        collector.assertCounts(enter = 1, exit = 0, move = 0)

        scene.sendPointerEvent(PointerEventType.Exit, Offset(-1f, -1f))
        collector.assertCounts(enter = 1, exit = 1, move = 0)
    }

    @Test
    fun `move between two components`() = ImageComposeScene(
        width = 100,
        height = 100
    ).use { scene ->
        val collector1 = EventCollector()
        val collector2 = EventCollector()

        scene.setContent {
            Column {
                Box(
                    modifier = Modifier
                        .collectPointerEvents(collector1)
                        .size(10.dp, 20.dp)
                )
                Box(
                    modifier = Modifier
                        .collectPointerEvents(collector2)
                        .size(10.dp, 20.dp)
                )
            }
        }

        scene.sendPointerEvent(PointerEventType.Enter, Offset(0f, 0f))
        collector1.assertCounts(enter = 1, exit = 0, move = 0)
        collector2.assertCounts(enter = 0, exit = 0, move = 0)

        scene.sendPointerEvent(PointerEventType.Move, Offset(0f, 19f))
        collector1.assertCounts(enter = 1, exit = 0, move = 1)
        collector2.assertCounts(enter = 0, exit = 0, move = 0)

        scene.sendPointerEvent(PointerEventType.Move, Offset(0f, 20f))
        collector1.assertCounts(enter = 1, exit = 1, move = 1)
        collector2.assertCounts(enter = 1, exit = 0, move = 0)

        scene.sendPointerEvent(PointerEventType.Move, Offset(0f, 19f))
        collector1.assertCounts(enter = 2, exit = 1, move = 1)
        collector2.assertCounts(enter = 1, exit = 1, move = 0)
    }

    @Test
    fun `move between two overlapped components`() = ImageComposeScene(
        width = 200,
        height = 200
    ).use { scene ->
        val collector1 = EventCollector()
        val collector2 = EventCollector()

        scene.setContent {
            Box(
                modifier = Modifier
                    .collectPointerEvents(collector1)
                    .size(100.dp, 200.dp)
            )
            Box(
                modifier = Modifier
                    .collectPointerEvents(collector2)
                    .size(10.dp, 20.dp)
            )
        }

        scene.sendPointerEvent(PointerEventType.Enter, Offset(0f, 0f))
        collector1.assertCounts(enter = 0, exit = 0, move = 0)
        collector2.assertCounts(enter = 1, exit = 0, move = 0)

        scene.sendPointerEvent(PointerEventType.Move, Offset(9f, 0f))
        collector1.assertCounts(enter = 0, exit = 0, move = 0)
        collector2.assertCounts(enter = 1, exit = 0, move = 1)

        scene.sendPointerEvent(PointerEventType.Move, Offset(10f, 0f))
        collector1.assertCounts(enter = 1, exit = 0, move = 0)
        collector2.assertCounts(enter = 1, exit = 1, move = 1)

        scene.sendPointerEvent(PointerEventType.Move, Offset(9f, 0f))
        collector1.assertCounts(enter = 1, exit = 1, move = 0)
        collector2.assertCounts(enter = 2, exit = 1, move = 1)
    }

    @Test
    fun `move between two nested components`() = ImageComposeScene(
        width = 200,
        height = 200
    ).use { scene ->
        val collector1 = EventCollector()
        val collector2 = EventCollector()

        scene.setContent {
            Box(
                modifier = Modifier
                    .collectPointerEvents(collector1)
                    .size(100.dp, 200.dp)
            ) {
                Box(
                    modifier = Modifier
                        .collectPointerEvents(collector2)
                        .size(10.dp, 20.dp)
                )
            }
        }

        scene.sendPointerEvent(PointerEventType.Enter, Offset(0f, 0f))
        collector1.assertCounts(enter = 1, exit = 0, move = 0)
        collector2.assertCounts(enter = 1, exit = 0, move = 0)

        scene.sendPointerEvent(PointerEventType.Move, Offset(9f, 0f))
        collector1.assertCounts(enter = 1, exit = 0, move = 1)
        collector2.assertCounts(enter = 1, exit = 0, move = 1)

        scene.sendPointerEvent(PointerEventType.Move, Offset(10f, 0f))
        collector1.assertCounts(enter = 1, exit = 0, move = 2)
        collector2.assertCounts(enter = 1, exit = 1, move = 1)

        scene.sendPointerEvent(PointerEventType.Move, Offset(9f, 0f))
        collector1.assertCounts(enter = 1, exit = 0, move = 3)
        collector2.assertCounts(enter = 2, exit = 1, move = 1)

        scene.sendPointerEvent(PointerEventType.Move, Offset(-1f, -1f))
        collector1.assertCounts(enter = 1, exit = 1, move = 3)
        collector2.assertCounts(enter = 2, exit = 2, move = 1)
    }

    @Test
    @OptIn(ExperimentalComposeUiApi::class)
    fun `shouldn't send a move events with same position`() = ImageComposeScene(
        width = 100,
        height = 100
    ).use { scene ->
        val collector = EventCollector()
        var pressCount = 0

        scene.setContent {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .collectPointerEvents(collector)
                    .onPointerEvent(PointerEventType.Press) { pressCount++ }
            )
        }

        scene.sendPointerEvent(PointerEventType.Move, Offset(0f, 0f))
        collector.assertCounts(enter = 1, exit = 0, move = 0)

        scene.sendPointerEvent(PointerEventType.Move, Offset(0f, 0f))
        collector.assertCounts(enter = 1, exit = 0, move = 0)

        scene.sendPointerEvent(PointerEventType.Move, Offset(0f, 0f))
        collector.assertCounts(enter = 1, exit = 0, move = 0)
    }

    @Test
    fun `should send a press with same position`() = ImageComposeScene(
        width = 100,
        height = 100
    ).use { scene ->
        val collector = EventCollector()

        scene.setContent {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .collectPointerEvents(collector)
            )
        }

        scene.sendPointerEvent(PointerEventType.Move, Offset(0f, 0f))
        collector.assertCounts(enter = 1, exit = 0, move = 0, press = 0)

        scene.sendPointerEvent(PointerEventType.Press, Offset(0f, 0f))
        collector.assertCounts(enter = 1, exit = 0, move = 0, press = 1)

        scene.sendPointerEvent(PointerEventType.Move, Offset(0f, 0f))
        collector.assertCounts(enter = 1, exit = 0, move = 0, press = 1)
    }

    @Test
    @OptIn(ExperimentalComposeUiApi::class)
    fun `should send a synthetic move when component moves`() = ImageComposeScene(
        width = 100,
        height = 100
    ).use { scene ->
        val collector = EventCollector()
        
        var x by mutableStateOf(0)
        
        scene.setContent {
            Box(
                modifier = Modifier
                    .offset(x.dp, 0.dp)
                    .size(10.dp)
                    .collectPointerEvents(collector)
                    .onPointerEvent(PointerEventType.Move) { println("Move ${it.changes.first().position}") }
                    .onPointerEvent(PointerEventType.Enter) { println("Enter ${it.changes.first().position}") }
                    .onPointerEvent(PointerEventType.Exit) { println("Exit ${it.changes.first().position}") }
            )
        }

        scene.sendPointerEvent(PointerEventType.Move, Offset(0f, 0f))
        collector.assertCounts(enter = 1, exit = 0, move = 0)
        collector.assertPositions(move = null)

        x = -2
        scene.render()
        collector.assertCounts(enter = 1, exit = 0, move = 1)
        collector.assertPositions(move = Offset(2f, 0f))
    }

    @Test
    fun `hover on scroll`() = runSkikoComposeUiTest(
        size = Size(100f, 100f),
    ) {
        val collector1 = EventCollector()
        val collector2 = EventCollector()

        setContent {
            val state = rememberScrollState()
            Column(Modifier.size(10.dp).verticalScroll(state)) {
                Box(
                    modifier = Modifier
                        .collectPointerEvents(collector1)
                        .size(10.dp, 10.dp)
                )
                Box(
                    modifier = Modifier
                        .collectPointerEvents(collector2)
                        .size(10.dp, 10.dp)
                )
            }
        }

        scene.sendPointerEvent(PointerEventType.Enter, Offset(0f, 0f))
        collector1.assertCounts(enter = 1, exit = 0)
        collector2.assertCounts(enter = 0, exit = 0)

        scene.sendPointerEvent(PointerEventType.Scroll, Offset(0f, 0f), scrollDelta = Offset(0f, 10000f))
        waitForIdle()
        collector1.assertCounts(enter = 1, exit = 1)
        collector2.assertCounts(enter = 1, exit = 0)

        scene.sendPointerEvent(PointerEventType.Scroll, Offset(0f, 0f), scrollDelta = Offset(0f, -10000f))
        waitForIdle()
        collector1.assertCounts(enter = 2, exit = 1)
        collector2.assertCounts(enter = 1, exit = 1)
    }

    @Test
    fun `hover on scroll in lazy list`() = runSkikoComposeUiTest(
        size = Size(100f, 100f),
    ) {
        val collector1 = EventCollector()
        val collector2 = EventCollector()

        setContent {
            LazyColumn(Modifier.size(12.dp)) {
                items(2) {
                    Box(
                        modifier = Modifier
                            .collectPointerEvents(if (it == 0) collector1 else collector2)
                            .size(10.dp, 10.dp)
                    )
                }
            }
        }

        scene.sendPointerEvent(PointerEventType.Enter, Offset(5f, 5f))
        collector1.assertCounts(enter = 1, exit = 0)
        collector2.assertCounts(enter = 0, exit = 0)

        scene.sendPointerEvent(PointerEventType.Scroll, Offset(5f, 5f), scrollDelta = Offset(0f, 10000f))
        waitForIdle()
        collector1.assertCounts(enter = 1, exit = 1)
        collector2.assertCounts(enter = 1, exit = 0)

        scene.sendPointerEvent(PointerEventType.Scroll, Offset(5f, 5f), scrollDelta = Offset(0f, -10000f))
        waitForIdle()
        collector1.assertCounts(enter = 2, exit = 1)
        collector2.assertCounts(enter = 1, exit = 1)
    }

    // bug after reverting 1b9c0419
    // enter/exit haven't properly fired when we hover another component
    @Test
    fun `consuming move events with same position shouldn't affect hover()`() = ImageComposeScene(
        width = 100,
        height = 100
    ).use { scene ->
        val collector1 = EventCollector()
        val collector2 = EventCollector()

        scene.setContent {
            Row {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .collectPointerEvents(collector1)
                )

                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .collectPointerEvents(collector2)
                )
            }
        }

        scene.sendPointerEvent(PointerEventType.Enter, Offset(0f, 0f))
        scene.sendPointerEvent(PointerEventType.Move, Offset(0f, 0f))
        collector1.assertCounts(enter = 1, exit = 0, move = 0)
        collector2.assertCounts(enter = 0, exit = 0, move = 0)

        scene.sendPointerEvent(PointerEventType.Move, Offset(10f, 0f))
        collector1.assertCounts(enter = 1, exit = 1, move = 0)
        collector2.assertCounts(enter = 1, exit = 0, move = 0)

        scene.sendPointerEvent(PointerEventType.Move, Offset(10f, 0f))
        collector1.assertCounts(enter = 1, exit = 1, move = 0)
        collector2.assertCounts(enter = 1, exit = 0, move = 0)

        scene.sendPointerEvent(PointerEventType.Move, Offset(9f, 0f))
        collector1.assertCounts(enter = 2, exit = 1, move = 0)
        collector2.assertCounts(enter = 1, exit = 1, move = 0)
    }

    // bug https://youtrack.jetbrains.com/issue/CMP-2147
    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun `move between two components with an intermediate render`() {
        @Composable
        fun Modifier.hoverable(hoveredState: MutableState<Boolean>) = this
            .onPointerEvent(PointerEventType.Enter) {
                hoveredState.value = true
            }
            .onPointerEvent(PointerEventType.Exit) {
                hoveredState.value = false
            }

        @Composable
        fun Item(hoveredState: MutableState<Boolean>) {
            Box(
                Modifier
                    .hoverable(hoveredState)
                    .background(if (hoveredState.value) Color.Cyan else Color.Transparent)
                    .size(10.dp, 20.dp)
            )
        }

        val context = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        runBlocking(context) {
            ImageComposeScene(
                width = 100,
                height = 100,
                coroutineContext = context
            ).use { scene ->
                val isHovered1 = mutableStateOf(false)
                val isHovered2 = mutableStateOf(false)

                scene.setContent {
                    Column {
                        Item(isHovered1)
                        Item(isHovered2)
                    }
                }

                while (scene.hasInvalidations()) {
                    yield()
                    scene.render()
                }

                scene.sendPointerEvent(PointerEventType.Enter, Offset(0f, 0f))
                assertThat(isHovered1.value).isTrue()
                assertThat(isHovered2.value).isFalse()

                scene.sendPointerEvent(PointerEventType.Move, Offset(0f, 10f))
                assertThat(isHovered1.value).isTrue()
                assertThat(isHovered2.value).isFalse()

                scene.render()   // this causes the test to fail before the fixing of 2147
                scene.sendPointerEvent(PointerEventType.Move, Offset(0f, 30f))
                assertThat(isHovered1.value).isFalse()
                assertThat(isHovered2.value).isTrue()
            }
        }
    }

    @Test
    fun `only one rect can be hovered in random moves in random rects`() = ImageComposeScene(
        width = 8,
        height = 8
    ).use { scene ->
        fun log(obj: Any) = Unit
//        fun log(obj: Any) = println(obj)

        operator fun Constraints.contains(position: IntOffset) =
            position.x in 0 until maxWidth && position.y in 0 until maxHeight

        fun randomOffset() = IntOffset(
            Random.nextInt(-1, scene.constraints.maxWidth + 1),
            Random.nextInt(-1, scene.constraints.maxHeight + 1)
        )
        fun randomSize() = IntSize(
            Random.nextInt(0, scene.constraints.maxWidth),
            Random.nextInt(0, scene.constraints.maxHeight)
        )
        fun randomRect() = IntRect(randomOffset(), randomSize())
        fun randomRects() = (1..Random.nextInt(0, 8)).map { randomRect() }

        val rects = mutableStateListOf<IntRect>()
        var enteredIndex = -1

        scene.setContent {
            rects.forEachIndexed { index, rect ->
                DisposableEffect(Unit) {
                    onDispose {
                        if (index == enteredIndex) {
                            enteredIndex = -1
                        }
                    }
                }

                Box(
                    Modifier
                        .offset(rect.left.dp, rect.top.dp)
                        .size(rect.width.dp, rect.height.dp)
                        .onPointerEvent(PointerEventType.Enter) {
                            log("Enter $rect")
                            check(enteredIndex == -1) {
                                "There is already entered rect: $enteredIndex"
                            }
                            enteredIndex = index
                        }
                        .onPointerEvent(PointerEventType.Exit) {
                            log("Exit $rect")
                            // Because
                            check(enteredIndex == index) {
                                "Exit fired on different rect $index. Entered rect: $enteredIndex"
                            }
                            enteredIndex = -1
                        }
                )
            }
        }

        var pointer = IntOffset(100000, 100000)

        repeat(1000) {
            rects.clear()
            rects.addAll(randomRects())
            log("------")
            log(pointer)
            log(rects.joinToString("\n"))
            scene.render()

            repeat(Random.nextInt(4)) {
                pointer = randomOffset()
                log("Send $pointer")
                scene.sendPointerEvent(PointerEventType.Move, pointer.toOffset())
            }

            val expectedEnteredIndex =
                if (pointer in scene.constraints) {
                    rects.indexOfLast { it.contains(pointer) }
                } else {
                    -1
                }

            check(expectedEnteredIndex == enteredIndex) {
                "Entered rect is different.\n" +
                    "Expected: $expectedEnteredIndex\n" +
                    "Actual: $enteredIndex"
            }
        }
    }

    @Test
    fun `window exit followed by layout does not trigger mouse enter`() = ImageComposeScene(
        width = 100,
        height = 100,
    ).use { scene ->
        val collector = EventCollector()
        var yOffset by mutableStateOf(0.dp)
        scene.setContent {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .offset(y = yOffset)
                    .collectPointerEvents(collector)
            )
        }

        scene.sendPointerEvent(PointerEventType.Enter, Offset(10f, 20f))
        collector.assertCounts(enter = 1, exit = 0, move = 0)

        // Deliberately send an exit event with coordinates still inside the box
        scene.sendPointerEvent(PointerEventType.Exit, Offset(10f, 20f))
        collector.assertCounts(enter = 1, exit = 1, move = 0)

        // Trigger a layout, which then triggers SyntheticEventSender.updatePointerPosition
        yOffset = 10.dp
        scene.renderUntilIdle()

        collector.assertCounts(enter = 1, exit = 1, move = 0)
    }

    @Test
    fun `window exit while pressed followed by layout triggers mouse move`() = ImageComposeScene(
        width = 100,
        height = 100,
    ).use { scene ->
        val collector = EventCollector()
        var yOffset by mutableStateOf(0.dp)
        scene.setContent {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .offset(y = yOffset)
                    .collectPointerEvents(collector)
            )
        }

        scene.sendPointerEvent(PointerEventType.Enter, Offset(10f, 20f))
        collector.assertCounts(enter = 1, exit = 0, move = 0)

        scene.sendPointerEvent(PointerEventType.Press, Offset(10f, 20f))
        collector.assertCounts(enter = 1, exit = 0, move = 0, press = 1)

        scene.sendPointerEvent(PointerEventType.Exit, Offset(-10f, 20f))
        collector.assertCounts(enter = 1, exit = 1, move = 0, press = 1)

        // Trigger a layout, which then triggers SyntheticEventSender.updatePointerPosition
        yOffset = 10.dp
        scene.renderUntilIdle()

        collector.assertCounts(enter = 1, exit = 1, move = 1, press = 1)
    }

    @Test
    fun `window exit while pressed followed by mouse up and layout does not trigger mouse move`() = ImageComposeScene(
        width = 100,
        height = 100,
    ).use { scene ->
        val collector = EventCollector()
        var yOffset by mutableStateOf(0.dp)
        scene.setContent {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .offset(y = yOffset)
                    .collectPointerEvents(collector)
            )
        }

        scene.sendPointerEvent(PointerEventType.Enter, Offset(10f, 20f))
        collector.assertCounts(enter = 1, exit = 0, move = 0)

        scene.sendPointerEvent(PointerEventType.Press, Offset(10f, 20f))
        collector.assertCounts(enter = 1, exit = 0, move = 0, press = 1)

        scene.sendPointerEvent(PointerEventType.Exit, Offset(-10f, 20f))
        collector.assertCounts(enter = 1, exit = 1, move = 0, press = 1)

        scene.sendPointerEvent(PointerEventType.Release, Offset(-10f, 20f))
        collector.assertCounts(enter = 1, exit = 1, move = 0, press = 1, release = 1)

        // Trigger a layout, which then triggers SyntheticEventSender.updatePointerPosition
        yOffset = 10.dp
        scene.renderUntilIdle()

        collector.assertCounts(enter = 1, exit = 1, move = 0, press = 1, release = 1)
    }

    @Test
    fun `window exit while pressed continues to send mouse events to target element`() = ImageComposeScene(
        width = 100,
        height = 100,
    ).use { scene ->
        val collector = EventCollector()
        scene.setContent {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .collectPointerEvents(collector)
            )
        }

        scene.sendPointerEvent(PointerEventType.Enter, Offset(10f, 20f))
        collector.assertCounts(enter = 1, exit = 0, move = 0)

        scene.sendPointerEvent(PointerEventType.Press, Offset(10f, 20f))
        collector.assertCounts(enter = 1, exit = 0, move = 0, press = 1)

        scene.sendPointerEvent(PointerEventType.Exit, Offset(-10f, 20f))
        collector.assertCounts(enter = 1, exit = 1, move = 0, press = 1)

        scene.sendPointerEvent(PointerEventType.Move, Offset(-20f, 20f))
        collector.assertCounts(enter = 1, exit = 1, move = 1, press = 1)
    }

    @Test
    fun `window exit clears lastKnownPointerPosition`() = ImageComposeScene(
        width = 100,
        height = 100,
    ).use { scene ->
        lateinit var composeScene: ComposeScene
        scene.setContent {
            composeScene = LocalComposeScene.current!!
            Box(Modifier.size(100.dp))
        }

        scene.sendPointerEvent(PointerEventType.Enter, Offset(10f, 20f))
        // Deliberately send an exit event with coordinates still inside the box
        scene.sendPointerEvent(PointerEventType.Exit, Offset(0f, 50f))

        assertNull(composeScene.lastKnownPointerPosition)
    }

    // https://youtrack.jetbrains.com/issue/CMP-9964
    @Test
    fun magicMouseScenario() = ImageComposeScene(
        width = 100,
        height = 100,
    ).use { scene ->
        var clicksCount = 0
        scene.setContent {
            Box(modifier = Modifier
                .fillMaxSize()
                .clickable {
                    clicksCount++
                }
            )
        }

        // The bug in this scenario is that no mouse-release event was generated
        // - SyntheticEventSender.sendMissingReleases didn't include the last released pointer
        //   when receiving the 3rd event.
        // - HitPathTracker ignores the 3rd event because it's a Move event at the same
        //   coordinates as the previous (2nd) event.
        // - CanvasLayersComposeScene.processPointerInputEvent clears `gestureOwner` after
        //   receiving the 3rd event, so when it receives the 4th event, it doesn't send it.
        scene.sendPointerEvent(PointerEventType.Press, Offset(10f, 10f), buttons = PointerButtons(isPrimaryPressed = true))
        scene.sendPointerEvent(PointerEventType.Move, Offset(10f, 10f), buttons = PointerButtons(isPrimaryPressed = true))
        scene.sendPointerEvent(PointerEventType.Move, Offset(10f, 10f), buttons = PointerButtons(isPrimaryPressed = false))
        scene.sendPointerEvent(PointerEventType.Release, Offset(10f, 10f))

        assertEquals(1, clicksCount)
    }

    @Test
    fun allNativeMouseEventsAreSent1() = ImageComposeScene(
        width = 100,
        height = 100,
    ).use { scene ->
        val nativeEventsReceived = mutableListOf<Any>()
        scene.setContent {
            Box(modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            event.nativeEvent?.let { nativeEventsReceived.add(it) }
                        }
                    }
                }
            )
        }

        scene.sendPointerEvent(PointerEventType.Press, Offset(10f, 10f), buttons = PointerButtons(isPrimaryPressed = true), nativeEvent = 1)
        scene.sendPointerEvent(PointerEventType.Move, Offset(10f, 10f), buttons = PointerButtons(isPrimaryPressed = true), nativeEvent = 2)
        scene.sendPointerEvent(PointerEventType.Move, Offset(10f, 10f), buttons = PointerButtons(isPrimaryPressed = false), nativeEvent = 3)
        scene.sendPointerEvent(PointerEventType.Release, Offset(10f, 10f), nativeEvent = 4)

        assertContentEquals(listOf(1, 2, 3, 4), nativeEventsReceived)
    }
}

private fun Modifier.collectPointerEvents(
    collector: EventCollector,
): Modifier = pointerInput(collector) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            collector.onPointerEvent(event)
        }
    }
}

private class EventCollector {
    private var enterCount = 0
    private var exitCount = 0
    private var moveCount = 0
    private var pressCount = 0
    private var releaseCount = 0
    private var enterPosition: Offset? = null
    private var exitPosition: Offset? = null
    private var movePosition: Offset? = null
    private var pressPosition: Offset? = null

    fun onPointerEvent(event: PointerEvent) {
        when (event.type) {
            PointerEventType.Move -> {
                moveCount++
                movePosition = event.changes[0].position
            }
            PointerEventType.Enter -> {
                enterCount++
                enterPosition = event.changes[0].position
            }
            PointerEventType.Exit -> {
                exitCount++
                exitPosition = event.changes[0].position
            }
            PointerEventType.Press -> {
                pressCount++
                pressPosition = event.changes[0].position
            }
            PointerEventType.Release -> {
                releaseCount++
                pressPosition = null
            }
        }
    }

    fun assertCounts(
        enter: Int = -1,
        exit: Int = -1,
        move: Int = -1,
        press: Int = -1,
        release: Int = -1,
    ) {
        if (enter >= 0) {
            assertWithMessage("enter count").that(this.enterCount).isEqualTo(enter)
        }
        if (exit >= 0) {
            assertWithMessage("exit count").that(this.exitCount).isEqualTo(exit)
        }
        if (move >= 0) {
            assertWithMessage("move count").that(this.moveCount).isEqualTo(move)
        }
        if (press >= 0) {
            assertWithMessage("press count").that(this.pressCount).isEqualTo(press)
        }
        if (release >= 0) {
            assertWithMessage("release count").that(this.releaseCount).isEqualTo(release)
        }
    }

    fun assertPositions(
        enter: Offset? = null,
        exit: Offset? = null,
        move: Offset? = null,
        press: Offset? = null,
    ) {
        if (enter != null) {
            assertWithMessage("enter position").that(this.enterPosition).isEqualTo(enter)
        }
        if (exit != null) {
            assertWithMessage("exit position").that(this.exitPosition).isEqualTo(exit)
        }
        if (move != null) {
            assertWithMessage("move position").that(this.movePosition).isEqualTo(move)
        }
        if (press != null) {
            assertWithMessage("press position").that(this.pressPosition).isEqualTo(press)
        }
    }
}

/**
 * Render the current content until there are no more invalidations.
 *
 * @param initialNanoTime The time to start rendering from.
 * @param stepNanoTime The time to step rendering by.
 */
internal fun ImageComposeScene.renderUntilIdle(initialNanoTime: Long = 0, stepNanoTime: Long = 16L): Long {
    var time = initialNanoTime
    Snapshot.sendApplyNotifications()  // Needed for the scene to be notified of state changes
    while (hasInvalidations()) {
        render(time)
        time += stepNanoTime
    }
    return time
}

