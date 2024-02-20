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

package androidx.compose.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.LocalSystemTheme
import androidx.compose.ui.events.toSkikoDragEvent
import androidx.compose.ui.events.toSkikoEvent
import androidx.compose.ui.events.toSkikoScrollEvent
import androidx.compose.ui.input.pointer.BrowserCursor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.native.ComposeLayer
import androidx.compose.ui.platform.JSTextInputService
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.WindowInfoImpl
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkikoKeyboardEventKind
import org.jetbrains.skiko.SkikoPointerEventKind
import org.jetbrains.skiko.SkikoView
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLStyleElement
import org.w3c.dom.HTMLTitleElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.events.WheelEvent
import org.w3c.dom.TouchEvent


@OptIn(InternalComposeApi::class)
private class ComposeWindow(
    canvasId: String,
    content: @Composable () -> Unit,
)  {

    private val density: Density = Density(
        density = window.devicePixelRatio.toFloat(),
        fontScale = 1f
    )

    private val _windowInfo = WindowInfoImpl().apply {
        isWindowFocused = true
    }
    private val jsTextInputService = JSTextInputService()
    private val platformContext: PlatformContext =
        object : PlatformContext by PlatformContext.Empty {
            override val windowInfo get() = _windowInfo
            override val textInputService = jsTextInputService
            override val viewConfiguration =
                object : ViewConfiguration by PlatformContext.Empty.viewConfiguration {
                    override val touchSlop: Float get() = with(density) { 18.dp.toPx() }
                }

            override fun setPointerIcon(pointerIcon: PointerIcon) {
                if (pointerIcon is BrowserCursor) {
                    setCursor(canvasId, pointerIcon.id)
                }
            }
        }

    private val layer = ComposeLayer(
        layer = SkiaLayer(),
        platformContext = platformContext,
        input = jsTextInputService.input
    )
    private val systemThemeObserver = getSystemThemeObserver()

    val canvas = document.getElementById(canvasId) as HTMLCanvasElement

    private fun <T : Event> HTMLCanvasElement.addTypedEvent(type: String, handler: (event: T, skikoView: SkikoView) -> Unit) {
        addEventListener(type, { event ->
            layer.layer?.skikoView?.let { skikoView -> handler(event as T, skikoView) }
        })
    }

    private fun initEvents(canvas: HTMLCanvasElement) {
        var offsetX = 0.0
        var offsetY = 0.0
        var isPointerPressed = false
        canvas.addTypedEvent<TouchEvent>("touchstart") { event, skikoView ->
            event.preventDefault()

            canvas.getBoundingClientRect().apply {
                offsetX = left
                offsetY = top
            }

            val skikoEvent = event.toSkikoEvent(SkikoPointerEventKind.DOWN, offsetX, offsetY)
            skikoView.onPointerEvent(skikoEvent)
        }

        canvas.addTypedEvent<TouchEvent>("touchmove") { event, skikoView ->
            event.preventDefault()
            skikoView.onPointerEvent(event.toSkikoEvent(SkikoPointerEventKind.MOVE, offsetX, offsetY))
        }

        canvas.addTypedEvent<TouchEvent>("touchend") { event, skikoView ->
            event.preventDefault()
            skikoView.onPointerEvent(event.toSkikoEvent(SkikoPointerEventKind.UP, offsetX, offsetY))
        }

        canvas.addTypedEvent<TouchEvent>("touchcancel") { event, skikoView ->
            event.preventDefault()
            skikoView.onPointerEvent(event.toSkikoEvent(SkikoPointerEventKind.UP, offsetX, offsetY))
        }

        canvas.addTypedEvent<MouseEvent>("mousedown") { event, skikoView ->
            isPointerPressed = true
            skikoView.onPointerEvent(event.toSkikoEvent(SkikoPointerEventKind.DOWN))
        }

        canvas.addTypedEvent<MouseEvent>("mouseup") { event, skikoView ->
            isPointerPressed = false
            skikoView.onPointerEvent(event.toSkikoEvent(SkikoPointerEventKind.UP))
        }

        canvas.addTypedEvent<MouseEvent>("mousemove") { event, skikoView ->
            if (isPointerPressed) {
                skikoView.onPointerEvent(event.toSkikoDragEvent())
            } else {
                skikoView.onPointerEvent(event.toSkikoEvent(SkikoPointerEventKind.MOVE))
            }
        }

        canvas.addTypedEvent<WheelEvent>("wheel") { event, skikoView ->
            skikoView.onPointerEvent(event.toSkikoScrollEvent())
        }

        canvas.addEventListener("contextmenu", { event ->
            event.preventDefault()
        })

        canvas.addTypedEvent<KeyboardEvent>("keydown") { event, skikoView ->
            event.preventDefault()
            skikoView.onKeyboardEvent(event.toSkikoEvent(SkikoKeyboardEventKind.DOWN))
        }

        canvas.addTypedEvent<KeyboardEvent>("keyup") { event, skikoView ->
            event.preventDefault()
            skikoView.onKeyboardEvent(event.toSkikoEvent(SkikoKeyboardEventKind.UP))
        }
    }

    init {
        layer.layer.attachTo(canvas)
        initEvents(canvas)

        canvas.setAttribute("tabindex", "0")
        layer.layer.needRedraw()

        _windowInfo.containerSize = IntSize(canvas.width, canvas.height)
        layer.setSize(canvas.width, canvas.height)

        layer.setDensity(density)
        layer.setContent {
            CompositionLocalProvider(
                LocalSystemTheme provides systemThemeObserver.currentSystemTheme.value,
                content = content
            )
        }
    }

    fun resize(newSize: IntSize) {
        canvas.width = newSize.width
        canvas.height = newSize.height
        _windowInfo.containerSize = IntSize(canvas.width, canvas.height)
        layer.layer.attachTo(canvas)
        layer.setSize(canvas.width, canvas.height)
        layer.layer.needRedraw()
    }

    // TODO: need to call .dispose() on window close.
    fun dispose() {
        layer.dispose()
        systemThemeObserver.dispose()
    }
}

private val defaultCanvasElementId = "ComposeTarget"

@ExperimentalComposeUiApi
/**
 * EXPERIMENTAL! Might be deleted or changed in the future!
 *
 * Initializes the composition in HTML canvas identified by [canvasElementId].
 *
 * It can be resized by providing [requestResize].
 * By default, it will listen to the window resize events.
 *
 * By default, styles will be applied to use the entire inner window, disabling scrollbars.
 * This can be turned off by setting [applyDefaultStyles] to false.
 */
fun CanvasBasedWindow(
    title: String? = null,
    canvasElementId: String = defaultCanvasElementId,
    requestResize: (suspend () -> IntSize)? = null,
    applyDefaultStyles: Boolean = true,
    content: @Composable () -> Unit = { }
) {
    if (title != null) {
        val htmlTitleElement = (
            document.head!!.getElementsByTagName("title").item(0)
                ?: document.createElement("title").also { document.head!!.appendChild(it) }
            ) as HTMLTitleElement
        htmlTitleElement.textContent = title
    }

    if (applyDefaultStyles) {
        document.head!!.appendChild(
            (document.createElement("style") as HTMLStyleElement).apply {
                type = "text/css"
                appendChild(
                    document.createTextNode(
                        "body { margin: 0; overflow: hidden; } #$canvasElementId { outline: none; }"
                    )
                )
            }
        )
    }

    val actualRequestResize: suspend () -> IntSize = if (requestResize != null) {
        requestResize
    } else {
        // we use Channel instead of suspendCancellableCoroutine,
        // because we want to drop old resize events
        val channel = Channel<IntSize>(capacity = CONFLATED)

        // we subscribe to 'resize' only once and never unsubscribe,
        // because the default behaviour expects that the Canvas takes the entire window space,
        // so the app has the same lifecycle as the browser tab.
        window.addEventListener("resize", { _ ->
            val w = document.documentElement?.clientWidth ?: 0
            val h = document.documentElement?.clientHeight ?: 0
            channel.trySend(IntSize(w, h))
        })

        suspend {
            channel.receive()
        }
    }

    if (requestResize == null) {
        (document.getElementById(canvasElementId) as? HTMLCanvasElement)?.let {
            it.width = document.documentElement?.clientWidth ?: 0
            it.height = document.documentElement?.clientHeight ?: 0
        }
    }

    var composeWindow: ComposeWindow? = null
    composeWindow = ComposeWindow(
        canvasId = canvasElementId,
        content = {
            content()
            LaunchedEffect(Unit) {
                while (isActive) {
                    val newSize = actualRequestResize()
                    composeWindow?.resize(newSize)
                    delay(100) // throttle
                }
            }
        }
    )
}

private fun setCursor(elementId: String, value: String): Unit =
    js("document.getElementById(elementId).style.cursor = value")
