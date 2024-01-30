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
import androidx.compose.ui.native.ComposeLayer
import androidx.compose.ui.platform.MacosTextInputService
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.platform.WindowInfoImpl
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import kotlinx.cinterop.*
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkikoInput
import platform.AppKit.*
import platform.Foundation.*

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
) {
    private val macosTextInputService = MacosTextInputService()
    private val _windowInfo = WindowInfoImpl().apply {
        isWindowFocused = true
    }
    private val platformContext: PlatformContext =
        object : PlatformContext by PlatformContext.Empty {
            override val windowInfo get() = _windowInfo
            override val textInputService get() = macosTextInputService
        }
    private val layer = ComposeLayer(
        layer = SkiaLayer(),
        platformContext = platformContext,
        input = SkikoInput.Empty
    )

    private val windowStyle =
        NSWindowStyleMaskTitled or
        NSWindowStyleMaskMiniaturizable or
        NSWindowStyleMaskClosable or
        NSWindowStyleMaskResizable

    private val contentRect = NSMakeRect(0.0, 0.0, 640.0, 480.0)

    private val nsWindow = NSWindow(
        contentRect = contentRect,
        styleMask = windowStyle,
        backing =  NSBackingStoreBuffered,
        defer =  true
    )

    init {
        layer.layer.attachTo(nsWindow)
        nsWindow.orderFrontRegardless()
        val scale = nsWindow.backingScaleFactor.toFloat()
        val size = contentRect.useContents {
            IntSize(
                width = (size.width * scale).toInt(),
                height = (size.height * scale).toInt()
            )
        }
        _windowInfo.containerSize = size
        layer.setDensity(Density(scale))
        layer.setSize(size.width, size.height)
        layer.setContent(content = content)
    }

    // TODO: need to call .dispose() on window close.
    fun dispose() {
        layer.dispose()
    }
}
