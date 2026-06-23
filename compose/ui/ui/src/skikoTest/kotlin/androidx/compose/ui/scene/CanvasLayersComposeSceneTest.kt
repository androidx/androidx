/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.platform.FrameRecomposer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.jetbrains.skia.Surface

class CanvasLayersComposeSceneTest {

    @Test
    fun sceneSizeChangeTriggersInvalidation() = runTest(StandardTestDispatcher()) {
        var invalidationCount = 0
        val frameRecomposer = FrameRecomposer(coroutineContext)
        CanvasLayersComposeScene(
            frameRecomposer = frameRecomposer,
            size = IntSize(100, 100),
            invalidateLayout = { invalidationCount++ },
            invalidateDraw = { invalidationCount++ },
        ).use { scene ->
            scene.setContent { Box(Modifier.fillMaxSize()) }

            assertEquals(2, invalidationCount)
            scene.size = IntSize(120, 120)
            assertEquals(4, invalidationCount)
        }
        frameRecomposer.close()
    }

    @Test
    fun cancelClickForGestureOwner() = runTest(StandardTestDispatcher()) {
        var rootCancelled = false
        var popupCancelled = false
        val frameRecomposer = FrameRecomposer(coroutineContext)
        CanvasLayersComposeScene(
            frameRecomposer = frameRecomposer,
            size = IntSize(100, 100),
        ).use { scene ->
            scene.setContent {
                Box(modifier = Modifier.fillMaxSize().onCancel { rootCancelled = true })

                Dialog(onDismissRequest = {}, properties = DialogProperties()) {
                    Box(modifier = Modifier.fillMaxSize().onCancel { popupCancelled = true })
                }
            }

            scene.sendPointerEvent(PointerEventType.Press, Offset(10f, 10f))
            scene.cancelPointerInput()

            assertFalse(rootCancelled)
            assertTrue(popupCancelled)
        }
        frameRecomposer.close()
    }

    @Test
    fun detachingLayerRequestsDrawPass() = runTest(StandardTestDispatcher()) {
        var drawInvalidations = 0
        var layer: ComposeSceneLayer? = null
        val frameRecomposer = FrameRecomposer(coroutineContext)
        val surface = Surface.makeRasterN32Premul(100, 100)
        CanvasLayersComposeScene(
            frameRecomposer = frameRecomposer,
            size = IntSize(100, 100),
            invalidateDraw = { drawInvalidations++ },
        ).use { scene ->
            scene.setContent {
                Box(Modifier.fillMaxSize())
                layer = rememberComposeSceneLayer(focusable = true)
            }

            // Settle measure/layout/draw so every owner's pending-draw flag is cleared; otherwise
            // the close below would invalidate simply because an owner was still dirty.
            scene.measureAndLayout()
            scene.draw(surface.canvas.asComposeCanvas())
            assertFalse(scene.hasPendingMeasureOrLayout)
            assertFalse(scene.hasPendingDraw)

            val drawInvalidationsBeforeClose = drawInvalidations
            layer!!.close()

            assertTrue(scene.hasPendingMeasureOrLayout)
            assertTrue(scene.hasPendingDraw)
            assertTrue(
                drawInvalidations > drawInvalidationsBeforeClose,
                "Detaching a layer must request a draw pass to repaint the scene without it",
            )
        }
        frameRecomposer.close()
    }
}
