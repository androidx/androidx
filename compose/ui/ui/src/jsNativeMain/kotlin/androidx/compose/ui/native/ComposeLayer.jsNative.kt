/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.native

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.toCompose
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.scene.ComposeSceneContext
import androidx.compose.ui.scene.ComposeScenePointer
import androidx.compose.ui.scene.MultiLayerComposeScene
import androidx.compose.ui.scene.platformContext
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import org.jetbrains.skia.Canvas
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkikoInput
import org.jetbrains.skiko.SkikoKeyboardEvent
import org.jetbrains.skiko.SkikoPointerDevice
import org.jetbrains.skiko.SkikoPointerEvent
import org.jetbrains.skiko.SkikoPointerEventKind
import org.jetbrains.skiko.SkikoView
import org.jetbrains.skiko.currentNanoTime

internal class ComposeLayer(
    internal val layer: SkiaLayer,
    platformContext: PlatformContext,
    private val input: SkikoInput,
) {
    private var isDisposed = false

    // Should be set to an actual value by ComposeWindow implementation
    private var density = Density(1f)

    private inner class ComponentImpl : SkikoViewExtended {
        override val input = this@ComposeLayer.input

        override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
            scene.render(canvas.asComposeCanvas(), nanoTime)
        }

        override fun onKeyboardEvent(event: SkikoKeyboardEvent) {
            onKeyboardEventWithResult(event)
        }

        override fun onKeyboardEventWithResult(event: SkikoKeyboardEvent): Boolean {
            if (isDisposed) return false
            return scene.sendKeyEvent(KeyEvent(event))
        }

        override fun onPointerEvent(event: SkikoPointerEvent) {
            if (event.pointers.firstOrNull()?.device == SkikoPointerDevice.TOUCH) {
                if (scene.platformContext.inputModeManager.inputMode != InputMode.Touch) {
                    scene.platformContext.inputModeManager.requestInputMode(InputMode.Touch)
                }
                onPointerEventWithMultitouch(event)
            } else {
                // macos and desktop`s web don't work properly when using onPointerEventWithMultitouch
                onPointerEventNoMultitouch(event)
            }
        }

        @OptIn(ExperimentalComposeUiApi::class)
        private fun onPointerEventWithMultitouch(event: SkikoPointerEvent) {
            val scale = density.density

            scene.sendPointerEvent(
                eventType = event.kind.toCompose(),
                pointers = event.pointers.map {
                    ComposeScenePointer(
                        id = PointerId(it.id),
                        position = Offset(
                            x = it.x.toFloat() * scale,
                            y = it.y.toFloat() * scale
                        ),
                        pressed = it.pressed,
                        type = it.device.toCompose(),
                        pressure = it.pressure.toFloat(),
                    )
                },
                timeMillis = event.timestamp,
                nativeEvent = event
            )
        }

        private fun onPointerEventNoMultitouch(event: SkikoPointerEvent) {
            val scale = density.density
            scene.sendPointerEvent(
                eventType = event.kind.toCompose(),
                scrollDelta = event.getScrollDelta(),
                position = Offset(
                    x = event.x.toFloat() * scale,
                    y = event.y.toFloat() * scale
                ),
                timeMillis = currentMillis(),
                type = PointerType.Mouse,
                nativeEvent = event
            )
        }
    }

    internal val view: SkikoViewExtended = ComponentImpl()

    init {
        layer.skikoView = view
    }

    private val scene = MultiLayerComposeScene(
        coroutineContext = Dispatchers.Main,
        composeSceneContext = object : ComposeSceneContext {
            override val platformContext get() = platformContext
        },
        density = density,
        invalidate = layer::needRedraw,
    )

    fun setDensity(newDensity: Density) {
        density = newDensity
        scene.density = newDensity
    }

    fun dispose() {
        check(!isDisposed)
        layer.detach()
        scene.close()
        _initContent = null
        isDisposed = true
    }

    fun setSize(width: Int, height: Int) {
        scene.size = IntSize(width, height)

        layer.needRedraw()
    }

    fun setContent(content: @Composable () -> Unit) {
        // If we call it before attaching, everything probably will be fine,
        // but the first composition will be useless, as we set density=1
        // (we don't know the real density if we have unattached component)
        _initContent = {
            scene.setContent(content)
        }

        initContent()
    }

    private var _initContent: (() -> Unit)? = null

    private fun initContent() {
        // TODO: do we need isDisplayable on SkiaLyer?
        // if (layer.isDisplayable) {
        _initContent?.invoke()
        _initContent = null
        // }
    }
}

private fun currentMillis() = (currentNanoTime() / 1E6).toLong()


internal expect val supportsMultitouch: Boolean

internal fun SkikoPointerEvent.getScrollDelta(): Offset {
    return this.takeIf {
        it.kind == SkikoPointerEventKind.SCROLL
    }?.let {
        Offset(it.deltaX.toFloat(), it.deltaY.toFloat())
    } ?: Offset.Zero
}

internal interface SkikoViewExtended : SkikoView {
    fun onKeyboardEventWithResult(event: SkikoKeyboardEvent): Boolean
}