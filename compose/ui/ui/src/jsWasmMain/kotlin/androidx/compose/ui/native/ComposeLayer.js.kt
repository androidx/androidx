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

package androidx.compose.ui.native

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.scene.ComposeSceneContext
import androidx.compose.ui.scene.ComposeScenePointer
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.scene.platformContext
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import org.jetbrains.skia.Canvas
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkikoRenderDelegate

// TODO: Align with Container/Mediator architecture
internal class ComposeLayer(
    internal val layer: SkiaLayer,
    platformContext: PlatformContext,
) {
    private var isDisposed = false

    // Should be set to an actual value by ComposeWindow implementation
    private var density = Density(1f)

    init {
        layer.renderDelegate = object : SkikoRenderDelegate {
            override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
                scene.render(canvas.asComposeCanvas(), nanoTime)
            }
        }
    }

    private val scene = CanvasLayersComposeScene(
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

    fun onKeyboardEvent(event: KeyEvent): Boolean {
        if (isDisposed) return false
        return scene.sendKeyEvent(event)
    }

    fun onMouseEvent(
        eventType: PointerEventType,
        position: Offset,
        scrollDelta: Offset = Offset.Zero,
        buttons: PointerButtons? = null,
        keyboardModifiers: PointerKeyboardModifiers? = null,
        nativeEvent: Any? = null,
        button: PointerButton? = null,
    ) {
        if (isDisposed) return
        scene.sendPointerEvent(
            eventType = eventType,
            position = position,
            scrollDelta = scrollDelta,
            buttons = buttons,
            keyboardModifiers = keyboardModifiers,
            nativeEvent = nativeEvent,
            button = button
        )
    }

    fun onTouchEvent(
        eventType: PointerEventType,
        pointers: List<ComposeScenePointer>,
        buttons: PointerButtons = PointerButtons(),
        keyboardModifiers: PointerKeyboardModifiers = PointerKeyboardModifiers(),
        scrollDelta: Offset = Offset.Zero,
        nativeEvent: Any? = null,
        button: PointerButton? = null,
    ) {
        if (isDisposed) return
        val inputModeManager = scene.platformContext.inputModeManager
        if (inputModeManager.inputMode != InputMode.Touch) {
            inputModeManager.requestInputMode(InputMode.Touch)
        }
        scene.sendPointerEvent(
            eventType = eventType,
            pointers = pointers,
            buttons = buttons,
            keyboardModifiers = keyboardModifiers,
            scrollDelta = scrollDelta,
            nativeEvent = nativeEvent,
            button = button
        )
    }
}
