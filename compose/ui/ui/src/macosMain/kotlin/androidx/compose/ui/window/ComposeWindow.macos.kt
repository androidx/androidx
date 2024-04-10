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
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.native.ComposeLayer
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.MacosTextInputService
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.WindowInfoImpl
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import kotlinx.cinterop.useContents
import org.jetbrains.skiko.SkiaLayer
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

fun Window(
    title: String = "ComposeWindow",
    content: @Composable () -> Unit,
) {
    ComposeWindow(
        content = content,
    )
}

private class ComposeWindow(
    content: @Composable () -> Unit,
) : LifecycleOwner {
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
    private val composeLayer = ComposeLayer(
        layer = skiaLayer,
        platformContext = platformContext
    )

    override val lifecycle = LifecycleRegistry(this)

    private val windowStyle =
        NSWindowStyleMaskTitled or
        NSWindowStyleMaskMiniaturizable or
        NSWindowStyleMaskClosable or
        NSWindowStyleMaskResizable

    private val contentRect = NSMakeRect(0.0, 0.0, 640.0, 480.0)

    private val nsWindow = NSWindow(
        contentRect = contentRect,
        styleMask = windowStyle,
        backing = NSBackingStoreBuffered,
        defer = true
    )

    private val nsView = object : NSView(nsWindow.frame) {
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
            composeLayer.onMouseEvent(event, PointerEventType.Press, PointerButton.Primary)
        }
        override fun mouseUp(event: NSEvent) {
            composeLayer.onMouseEvent(event, PointerEventType.Release, PointerButton.Primary)
        }
        override fun rightMouseDown(event: NSEvent) {
            composeLayer.onMouseEvent(event, PointerEventType.Press, PointerButton.Secondary)
        }
        override fun rightMouseUp(event: NSEvent) {
            composeLayer.onMouseEvent(event, PointerEventType.Release, PointerButton.Secondary)
        }
        override fun otherMouseDown(event: NSEvent) {
            composeLayer.onMouseEvent(event, PointerEventType.Release, PointerButton(event.buttonNumber.toInt()))
        }
        override fun otherMouseUp(event: NSEvent) {
            composeLayer.onMouseEvent(event, PointerEventType.Press, PointerButton(event.buttonNumber.toInt()))
        }
        override fun mouseMoved(event: NSEvent) {
            composeLayer.onMouseEvent(event, PointerEventType.Move)
        }
        override fun mouseDragged(event: NSEvent) {
            composeLayer.onMouseEvent(event, PointerEventType.Move)
        }
        override fun scrollWheel(event: NSEvent) {
            composeLayer.onMouseEvent(event, PointerEventType.Scroll)
        }
//        override fun keyDown(event: NSEvent) {
//            composeLayer.onKeyboardEvent(toSkikoEvent(event, SkikoKeyboardEventKind.DOWN))
//            interpretKeyEvents(listOf(event))
//        }
//        override fun flagsChanged(event: NSEvent) {
//            composeLayer.onKeyboardEvent(toSkikoEvent(event))
//        }
//        override fun keyUp(event: NSEvent) {
//            composeLayer.onKeyboardEvent(toSkikoEvent(event, SkikoKeyboardEventKind.UP))
//        }
    }

    init {
        nsWindow.contentView = nsView
        skiaLayer.attachTo(nsView)
        nsWindow.orderFrontRegardless()
        val scale = nsWindow.backingScaleFactor.toFloat()
        val size = contentRect.useContents {
            IntSize(
                width = (size.width * scale).toInt(),
                height = (size.height * scale).toInt()
            )
        }
        _windowInfo.containerSize = size
        composeLayer.setDensity(Density(scale))
        composeLayer.setSize(size.width, size.height)
        composeLayer.setContent {
            CompositionLocalProvider(
                LocalLifecycleOwner provides this,
                content = content
            )
        }

        // TODO: Handle lifecycle events
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    // TODO: need to call .dispose() on window close.
    fun dispose() {
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        composeLayer.dispose()
    }

    private fun ComposeLayer.onMouseEvent(
        event: NSEvent,
        eventType: PointerEventType,
        button: PointerButton? = null,
    ) {
        onMouseEvent(
            eventType = eventType,
            position = event.offset,
            scrollDelta = Offset(x = event.deltaX.toFloat(), y = event.deltaY.toFloat()),
            nativeEvent = event,
            button = button,
        )
    }

    private val NSEvent.offset: Offset get() {
        val position = locationInWindow.useContents {
            Offset(x = x.toFloat(), y = y.toFloat())
        }
        val height = nsView.frame.useContents { size.height }
        return Offset(
            x = position.x,
            y = height.toFloat() - position.y,
        ) * nsWindow.backingScaleFactor.toFloat()
    }
}
