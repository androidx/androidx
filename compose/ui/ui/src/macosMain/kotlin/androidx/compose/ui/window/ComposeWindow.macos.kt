/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.toComposeEvent
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.MacosTextInputService
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.WindowInfoImpl
import androidx.compose.ui.scene.ComposeSceneContext
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toOffset
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import org.jetbrains.skia.Canvas
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkikoRenderDelegate
import platform.AppKit.NSBackingStoreBuffered
import platform.AppKit.NSEvent
import platform.AppKit.NSTrackingActiveAlways
import platform.AppKit.NSTrackingActiveInKeyWindow
import platform.AppKit.NSTrackingArea
import platform.AppKit.NSTrackingAssumeInside
import platform.AppKit.NSTrackingInVisibleRect
import platform.AppKit.NSTrackingMouseEnteredAndExited
import platform.AppKit.NSTrackingMouseMoved
import platform.AppKit.NSView
import platform.AppKit.NSWindow
import platform.AppKit.NSWindowStyleMaskClosable
import platform.AppKit.NSWindowStyleMaskMiniaturizable
import platform.AppKit.NSWindowStyleMaskResizable
import platform.AppKit.NSWindowStyleMaskTitled
import platform.Foundation.NSMakeRect

interface WindowScope {
    /**
     * [NSWindow] that was created inside [androidx.compose.ui.window.Window]
     */
    val window: NSWindow
}

fun Window(
    title: String = "ComposeWindow",
    size: DpSize = DpSize(800.dp, 600.dp),
    content: @Composable WindowScope.() -> Unit,
) {
    ComposeWindow(
        title = title,
        size = size,
        content = content,
    )
}

private class ComposeWindow(
    title: String,
    size: DpSize,
    content: @Composable WindowScope.() -> Unit,
) : LifecycleOwner, WindowScope {
    private var isDisposed = false
    private val macosTextInputService = MacosTextInputService()
    private val _windowInfo = WindowInfoImpl().apply {
        isWindowFocused = true
    }
    private val platformContext: PlatformContext =
        object : PlatformContext by PlatformContext.Empty {
            override val windowInfo get() = _windowInfo
            override val textInputService get() = macosTextInputService
        }
    private val skiaLayer = SkiaLayer()
    private val scene = CanvasLayersComposeScene(
        coroutineContext = Dispatchers.Main,
        composeSceneContext = object : ComposeSceneContext {
            override val platformContext get() = this@ComposeWindow.platformContext
        },
        invalidate = skiaLayer::needRedraw,
    )
    private val renderDelegate = object : SkikoRenderDelegate {
        override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
            val sizeInPx = IntSize(width, height)
            _windowInfo.containerSize = sizeInPx
            scene.size = sizeInPx // TODO: Move it out from onRender to avoid extra invalidation
            scene.render(canvas.asComposeCanvas(), nanoTime)
        }
    }

    override val lifecycle = LifecycleRegistry(this)

    private val windowStyle =
        NSWindowStyleMaskTitled or
        NSWindowStyleMaskMiniaturizable or
        NSWindowStyleMaskClosable or
        NSWindowStyleMaskResizable

    override val window = object : NSWindow(
        contentRect = NSMakeRect(
            x = 0.0,
            y = 0.0,
            w = size.width.value.toDouble(),
            h = size.height.value.toDouble()
        ),
        styleMask = windowStyle,
        backing = NSBackingStoreBuffered,
        defer = true
    ) {
        override fun canBecomeKeyWindow() = true
        override fun canBecomeMainWindow() = true
    }

    private val view = object : NSView(window.frame) {
        private var trackingArea : NSTrackingArea? = null
        override fun wantsUpdateLayer() = true
        override fun acceptsFirstResponder() = true
        override fun viewWillMoveToWindow(newWindow: NSWindow?) {
            updateTrackingAreas()
        }

        override fun updateTrackingAreas() {
            trackingArea?.let { removeTrackingArea(it) }
            trackingArea = NSTrackingArea(
                rect = bounds,
                options = NSTrackingActiveAlways or
                    NSTrackingMouseEnteredAndExited or
                    NSTrackingMouseMoved or
                    NSTrackingActiveInKeyWindow or
                    NSTrackingAssumeInside or
                    NSTrackingInVisibleRect,
                owner = this, userInfo = null)
            addTrackingArea(trackingArea!!)
        }

        override fun mouseDown(event: NSEvent) {
            onMouseEvent(event, PointerEventType.Press, PointerButton.Primary)
        }
        override fun mouseUp(event: NSEvent) {
            onMouseEvent(event, PointerEventType.Release, PointerButton.Primary)
        }
        override fun rightMouseDown(event: NSEvent) {
            onMouseEvent(event, PointerEventType.Press, PointerButton.Secondary)
        }
        override fun rightMouseUp(event: NSEvent) {
            onMouseEvent(event, PointerEventType.Release, PointerButton.Secondary)
        }
        override fun otherMouseDown(event: NSEvent) {
            onMouseEvent(event, PointerEventType.Release, PointerButton(event.buttonNumber.toInt()))
        }
        override fun otherMouseUp(event: NSEvent) {
            onMouseEvent(event, PointerEventType.Press, PointerButton(event.buttonNumber.toInt()))
        }
        override fun mouseMoved(event: NSEvent) {
            onMouseEvent(event, PointerEventType.Move)
        }
        override fun mouseDragged(event: NSEvent) {
            onMouseEvent(event, PointerEventType.Move)
        }
        override fun scrollWheel(event: NSEvent) {
            onMouseEvent(event, PointerEventType.Scroll)
        }
        override fun keyDown(event: NSEvent) {
            val consumed = onKeyboardEvent(event.toComposeEvent())
            if (!consumed) {
                // Pass only unconsumed event to system handler.
                // It will trigger the system's "beep" sound for unconsumed events.
                super.keyDown(event)
            }
        }
        override fun keyUp(event: NSEvent) {
            onKeyboardEvent(event.toComposeEvent())
        }
    }

    init {
        window.title = title
        window.contentView = view

        skiaLayer.renderDelegate = renderDelegate
        skiaLayer.attachTo(view) // Should be called after attaching to window

        // TODO: Expose some API to control showing outside
        window.center()
        window.makeKeyAndOrderFront(null)

        scene.density = Density(window.backingScaleFactor.toFloat())
        scene.setContent {
            CompositionLocalProvider(
                LocalLifecycleOwner provides this
            ) {
                content()
            }
        }

        // TODO: Properly handle lifecycle events
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    // TODO: need to call .dispose() on window close.
    fun dispose() {
        check(!isDisposed) { "ComposeWindow is already disposed" }
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        skiaLayer.detach()
        scene.close()
        isDisposed = true
    }

    private fun onKeyboardEvent(event: KeyEvent): Boolean {
        if (isDisposed) return false
        return scene.sendKeyEvent(event)
    }

    private fun onMouseEvent(
        event: NSEvent,
        eventType: PointerEventType,
        button: PointerButton? = null,
    ) {
        if (isDisposed) return
        scene.sendPointerEvent(
            eventType = eventType,
            position = event.offset.toOffset(scene.density),
            scrollDelta = Offset(x = event.deltaX.toFloat(), y = event.deltaY.toFloat()),
            nativeEvent = event,
            button = button,
        )
    }

    private val NSEvent.offset: DpOffset get() {
        val position = locationInWindow.useContents {
            DpOffset(x = x.dp, y = y.dp)
        }
        val height = view.frame.useContents { size.height.dp }
        return DpOffset(
            x = position.x,
            y = height - position.y,
        )
    }
}
