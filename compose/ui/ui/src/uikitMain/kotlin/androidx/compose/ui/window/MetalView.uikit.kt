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

package androidx.compose.ui.window

import androidx.compose.ui.uikit.toNanoSeconds
import androidx.compose.ui.viewinterop.UIKitInteropTransaction
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Color
import platform.CoreGraphics.CGRectIsEmpty
import platform.CoreGraphics.CGRectZero
import platform.CoreGraphics.CGSizeMake
import platform.Metal.MTLCreateSystemDefaultDevice
import platform.Metal.MTLDeviceProtocol
import platform.Metal.MTLPixelFormatBGRA8Unorm
import platform.QuartzCore.CAMetalLayer
import platform.UIKit.UIColor
import platform.UIKit.UITraitCollection
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIView
import platform.UIKit.UIViewMeta

internal class MetalView(
    retrieveInteropTransaction: () -> UIKitInteropTransaction,
    useSeparateRenderThreadWhenPossible: Boolean,
    render: (Canvas, nanoTime: Long) -> Unit,
) : UIView(frame = CGRectZero.readValue()) {
    companion object : UIViewMeta() {
        @BetaInteropApi
        override fun layerClass() = CAMetalLayer
    }

    private val device: MTLDeviceProtocol =
        MTLCreateSystemDefaultDevice()
            ?: throw IllegalStateException("Metal is not supported on this system")

    private val metalLayer: CAMetalLayer get() = layer as CAMetalLayer
    private var canvasBackground: Int = Color.TRANSPARENT

    val redrawer = MetalRedrawer(
        metalLayer,
        retrieveInteropTransaction,
        useSeparateRenderThreadWhenPossible
    ) { canvas, targetTimestamp ->
        canvas.clear(canvasBackground)
        render(canvas, targetTimestamp.toNanoSeconds())
    }

    /**
     * @see [MetalRedrawer.canBeOpaque]
     */
    var canBeOpaque: Boolean
        get() = redrawer.canBeOpaque
        set(value) {
            redrawer.canBeOpaque = value
            updateCanvasBackgroundColor()
        }

    /**
     * Indicates that the view needs to be drawn synchronously with the next layout pass to avoid
     * flickering.
     */
    private var needsSynchronousDraw = true

    /**
     * Raise the flag to indicate that the view needs to be drawn synchronously with the next layout.
     */
    fun setNeedsSynchronousDrawOnNextLayout() {
        needsSynchronousDraw = true
    }

    init {
        userInteractionEnabled = false

        metalLayer.also {
            // Workaround for cinterop issue
            // Type mismatch: inferred type is platform.Metal.MTLDeviceProtocol but objcnames.protocols.MTLDeviceProtocol? was expected
            it.device = device as objcnames.protocols.MTLDeviceProtocol?

            it.pixelFormat = MTLPixelFormatBGRA8Unorm
            it.backgroundColor = UIColor.clearColor.CGColor
            it.framebufferOnly = false
        }

        updateCanvasBackgroundColor()
    }

    override fun traitCollectionDidChange(previousTraitCollection: UITraitCollection?) {
        super.traitCollectionDidChange(previousTraitCollection)
        updateCanvasBackgroundColor()
    }

    private fun updateCanvasBackgroundColor() {
        canvasBackground = if (redrawer.canBeOpaque) {
            when (traitCollection.userInterfaceStyle) {
                UIUserInterfaceStyle.UIUserInterfaceStyleDark -> Color.BLACK
                UIUserInterfaceStyle.UIUserInterfaceStyleLight -> Color.WHITE
                else -> Color.TRANSPARENT
            }
        } else {
            Color.TRANSPARENT
        }
    }

    fun dispose() {
        redrawer.dispose()
    }

    override fun didMoveToWindow() {
        super.didMoveToWindow()

        val window = window ?: return

        val screen = window.screen
        contentScaleFactor = screen.scale
        redrawer.maximumFramesPerSecond = screen.maximumFramesPerSecond
        redrawer.preferredFramesPerSecond = screen.maximumFramesPerSecond

        updateMetalLayerSize()
    }

    override fun layoutSubviews() {
        super.layoutSubviews()

        updateMetalLayerSize()
    }

    private fun updateMetalLayerSize() {
        if (window == null || CGRectIsEmpty(bounds)) {
            return
        }

        metalLayer.drawableSize = bounds.useContents {
            CGSizeMake(
                width = size.width * contentScaleFactor,
                height = size.height * contentScaleFactor
            )
        }

        if (needsSynchronousDraw) {
            redrawer.draw(waitUntilCompletion = true)

            needsSynchronousDraw = false
        }
    }

    override fun canBecomeFirstResponder() = false
}
