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

/**
 * Hides implementation details of LegacyMetalView and SurfaceMetalView.
 * Must be removed after https://youtrack.jetbrains.com/issue/CMP-9722
 */
internal sealed interface MetalViewHolder {
    val view: UIView
    val redrawer: MetalRedrawer
    var canBeOpaque: Boolean
    fun dispose()
}

internal fun MetalView(
    retrieveInteropTransaction: () -> UIKitInteropTransaction,
    useSeparateRenderThreadWhenPossible: Boolean,
    render: (Canvas, nanoTime: Long) -> Unit,
): MetalViewHolder = if (useSeparateRenderThreadWhenPossible) {
    SurfaceMetalView(retrieveInteropTransaction, render).holder
} else {
    LegacyMetalView(retrieveInteropTransaction, render).holder
}

// https://youtrack.jetbrains.com/issue/CMP-9722
// Copy of the class SurfaceMetalView with a different layer.
// All changes made here must also be implemented in the `SurfaceMetalView`.
private class LegacyMetalView(
    retrieveInteropTransaction: () -> UIKitInteropTransaction,
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

    val holder: MetalViewHolder get() = LegacyMetalViewHolder(this)

    val redrawer = LegacyMetalRedrawer(
        metalLayer,
        retrieveInteropTransaction
    ) { canvas, targetTimestamp ->
        canvas.clear(canvasBackground)
        render(canvas, targetTimestamp.toNanoSeconds())
    }

    var canBeOpaque: Boolean
        get() = redrawer.canBeOpaque
        set(value) {
            redrawer.canBeOpaque = value
            updateCanvasBackgroundColor()
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

        val screen = window?.screen ?: return
        contentScaleFactor = screen.scale
        redrawer.displayLinkFrameRate?.maximumFramesPerSecond = screen.maximumFramesPerSecond
        redrawer.displayLinkFrameRate?.preferredFramesPerSecond = screen.maximumFramesPerSecond
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
    }

    override fun canBecomeFirstResponder() = false
}

private class LegacyMetalViewHolder(
    private val metalView: LegacyMetalView
): MetalViewHolder {
    override val view: UIView get() = metalView
    override val redrawer: MetalRedrawer get() = metalView.redrawer
    override var canBeOpaque: Boolean by metalView::canBeOpaque
    override fun dispose() = metalView.dispose()
}
