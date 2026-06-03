/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.scene

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.platform.FrameRecomposer
import androidx.compose.ui.unit.IntSize
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest

class BaseComposeSceneTest {

    @Test
    fun testMoveEventsConsumption() = runTest(StandardTestDispatcher()) {
        val scenes = listOf(
            createPlatformLayersScene(coroutineContext, IntSize(100, 100)),
            createCanvasLayersScene(coroutineContext, IntSize(100, 100))
        )

        try {
            scenes.forEach { (scene, _) ->
                var consumeAll = false
                scene.setContent {
                    Box(modifier = Modifier.fillMaxSize().pointerInput(PointerEventPass.Initial) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (consumeAll) {
                                    event.changes.forEach {
                                        if ((it.previousPosition - it.position) != Offset.Zero) it.consume()
                                    }
                                }
                            }
                        }
                    })
                }
                scene.sendPointerEvent(PointerEventType.Press, Offset(10f, 10f))
                assertFalse(
                    scene.sendPointerEvent(PointerEventType.Move, Offset(11f, 10f))
                        .anyMovementConsumed
                )
                assertFalse(
                    scene.sendPointerEvent(PointerEventType.Release, Offset(12f, 10f))
                        .anyMovementConsumed
                )

                consumeAll = true

                scene.sendPointerEvent(PointerEventType.Press, Offset(10f, 10f))
                assertTrue(
                    scene.sendPointerEvent(PointerEventType.Move, Offset(11f, 10f))
                        .anyMovementConsumed
                )
                assertTrue(
                    scene.sendPointerEvent(PointerEventType.Release, Offset(12f, 10f))
                        .anyMovementConsumed
                )
            }
        } finally {
            scenes.forEach { (_, dispose) -> dispose.close() }
        }
    }

    @Test
    fun cancelAllPointersShouldCancelInputCoroutines() = runTest(StandardTestDispatcher()) {
        val scenes = listOf(
            createPlatformLayersScene(coroutineContext, IntSize(100, 100)),
            createCanvasLayersScene(coroutineContext, IntSize(100, 100))
        )

        try {
            scenes.forEach { (scene, _) ->
                var cancellationsCount = 0
                scene.setContent {
                    Box(modifier = Modifier.fillMaxSize().onCancel {
                        cancellationsCount++
                    })
                }

                scene.sendPointerEvent(PointerEventType.Press, Offset(10f, 10f))
                scene.cancelPointerInput()

                assertEquals(1, cancellationsCount)
            }
        } finally {
            scenes.forEach { (_, dispose) -> dispose.close() }
        }
    }

    @Test
    fun cancelAllPointersShouldCancelClicks() = runTest(StandardTestDispatcher()) {
        val scenes = listOf(
            createPlatformLayersScene(coroutineContext, IntSize(100, 100)),
            createCanvasLayersScene(coroutineContext, IntSize(100, 100))
        )

        try {
            scenes.forEach { (scene, _) ->
                var clicksCount = 0
                scene.setContent {
                    Box(modifier = Modifier.fillMaxSize().clickable {
                        clicksCount++
                    })
                }

                // Perform first click
                scene.sendPointerEvent(PointerEventType.Press, Offset(10f, 10f))
                scene.sendPointerEvent(PointerEventType.Release, Offset(40f, 40f))

                // Start and cancel click
                scene.sendPointerEvent(PointerEventType.Press, Offset(10f, 10f))
                scene.cancelPointerInput()
                scene.sendPointerEvent(PointerEventType.Release, Offset(40f, 40f))

                // Perform second click
                scene.sendPointerEvent(PointerEventType.Press, Offset(10f, 10f))
                scene.sendPointerEvent(PointerEventType.Release, Offset(40f, 40f))

                // Should be only two clicks
                assertEquals(2, clicksCount)
            }
        } finally {
            scenes.forEach { (_, dispose) -> dispose.close() }
        }
    }
}

private fun createPlatformLayersScene(
    coroutineContext: CoroutineContext,
    size: IntSize,
    invalidateLayout: () -> Unit = {},
    invalidateDraw: () -> Unit = {},
): Pair<ComposeScene, AutoCloseable> {
    val frameRecomposer = FrameRecomposer(coroutineContext)
    val scene = PlatformLayersComposeScene(
        frameRecomposer = frameRecomposer,
        size = size,
        invalidateLayout = invalidateLayout,
        invalidateDraw = invalidateDraw,
    )
    return scene to AutoCloseable {
        scene.close()
        frameRecomposer.close()
    }
}

private fun createCanvasLayersScene(
    coroutineContext: CoroutineContext,
    size: IntSize,
    invalidateLayout: () -> Unit = {},
    invalidateDraw: () -> Unit = {},
): Pair<ComposeScene, AutoCloseable> {
    val frameRecomposer = FrameRecomposer(coroutineContext)
    val scene = CanvasLayersComposeScene(
        frameRecomposer = frameRecomposer,
        size = size,
        invalidateLayout = invalidateLayout,
        invalidateDraw = invalidateDraw,
    )
    return scene to AutoCloseable {
        scene.close()
        frameRecomposer.close()
    }
}

internal fun Modifier.onCancel(onCancel: () -> Unit) = this then TestCancellable(onCancel)

private class TestCancellable(
    private val onCancel: () -> Unit
) : ModifierNodeElement<CancellableNode>() {
    override fun create() = CancellableNode(onCancel)
    override fun hashCode(): Int = 0
    override fun equals(other: Any?): Boolean = false
    override fun update(node: CancellableNode) { node.onCancel = onCancel }
}

private class CancellableNode(
    var onCancel: () -> Unit
): DelegatingNode(), PointerInputModifierNode {
    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {}

    override fun onCancelPointerInput() {
        onCancel()
    }
}
