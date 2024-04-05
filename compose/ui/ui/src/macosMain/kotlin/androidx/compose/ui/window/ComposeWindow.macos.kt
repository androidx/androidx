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
import androidx.compose.ui.input.key.MouseButtons
import androidx.compose.ui.input.key.NativePointerEvent
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
            composeLayer.view.onPointerEvent(
                event.toNativePointerEvent(MouseButtons.LEFT, PointerEventType.Press, this)
            )
        }
        override fun mouseUp(event: NSEvent) {
            composeLayer.view.onPointerEvent(
                event.toNativePointerEvent(MouseButtons.LEFT, PointerEventType.Release, this)
            )
        }
        override fun rightMouseDown(event: NSEvent) {
            composeLayer.view.onPointerEvent(
                event.toNativePointerEvent(MouseButtons.RIGHT, PointerEventType.Press, this)
            )
        }
        override fun rightMouseUp(event: NSEvent) {
            composeLayer.view.onPointerEvent(
                event.toNativePointerEvent(MouseButtons.RIGHT, PointerEventType.Press, this)
            )
        }
//        override fun otherMouseDown(event: NSEvent) {
//            composeLayer.view.onPointerEvent(toSkikoEvent(event, SkikoPointerEventKind.DOWN, nsView))
//        }
//        override fun otherMouseUp(event: NSEvent) {
//            composeLayer.view.onPointerEvent(toSkikoEvent(event, SkikoPointerEventKind.UP, nsView))
//        }
        override fun mouseMoved(event: NSEvent) {
            composeLayer.view.onPointerEvent(
                event.toNativePointerEvent(PointerEventType.Move, this)
            )
        }
        override fun mouseDragged(event: NSEvent) {
            composeLayer.view.onPointerEvent(
                event.toNativePointerEvent(PointerEventType.Move, this)
            )
        }
//        override fun scrollWheel(event: NSEvent) {
//            // FIXME: MacOsScrollConfig expect NSEvent instead of NativePointerEvent
//            composeLayer.view.onPointerEvent(toSkikoScrollEvent(event, nsView))
//        }
//        override fun keyDown(event: NSEvent) {
//            composeLayer.view.onKeyboardEvent(toSkikoEvent(event, SkikoKeyboardEventKind.DOWN))
//            interpretKeyEvents(listOf(event))
//        }
//        override fun flagsChanged(event: NSEvent) {
//            composeLayer.view.onKeyboardEvent(toSkikoEvent(event))
//        }
//        override fun keyUp(event: NSEvent) {
//            composeLayer.view.onKeyboardEvent(toSkikoEvent(event, SkikoKeyboardEventKind.UP))
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
}

private fun NSEvent.toNativePointerEvent(
    kind: PointerEventType,
    view: NSView
): NativePointerEvent {
    var (xpos, ypos) = this.locationInWindow.useContents {
        x to y
    }
    view.frame.useContents {
        ypos = size.height - ypos
    }
    val timestamp = (this.timestamp * 1_000).toLong()
    return NativePointerEvent(
        x = xpos,
        y = ypos,
        kind = kind,
        pressedButtons = toPressedMouseButtons(this, kind),
        timestamp = timestamp,
    )
}

private fun NSEvent.toNativePointerEvent(
    button: MouseButtons,
    kind: PointerEventType,
    view: NSView
): NativePointerEvent {
    var (xpos, ypos) = this.locationInWindow.useContents {
        x to y
    }
    view.frame.useContents {
        ypos = size.height - ypos
    }
    val timestamp = (this.timestamp * 1_000).toLong()
    if (kind == PointerEventType.Press) {
        buttonsFlags = buttonsFlags.or(button.value)
    } else {
        buttonsFlags = buttonsFlags.xor(button.value)
    }
    val buttons = MouseButtons(buttonsFlags)
    return NativePointerEvent(
        x = xpos,
        y = ypos,
        kind = kind,
        pressedButtons = buttons,
        timestamp = timestamp,
    )
}

private var buttonsFlags = 0
private fun toPressedMouseButtons(
    event: NSEvent,
    kind: PointerEventType
): MouseButtons {
    val button = event.buttonNumber.toInt()
    if (kind == PointerEventType.Press) {
        buttonsFlags = buttonsFlags.or(getButtonValue(button))
        return MouseButtons(buttonsFlags)
    }
    buttonsFlags = buttonsFlags.xor(getButtonValue(button))
    return MouseButtons(buttonsFlags)
}

private fun getButtonValue(button: Int): Int {
    return when (button) {
        2 -> MouseButtons.MIDDLE.value
        3 -> MouseButtons.BUTTON_4.value
        4 -> MouseButtons.BUTTON_5.value
        5 -> MouseButtons.BUTTON_6.value
        6 -> MouseButtons.BUTTON_7.value
        7 -> MouseButtons.BUTTON_8.value
        else -> 0
    }
}
