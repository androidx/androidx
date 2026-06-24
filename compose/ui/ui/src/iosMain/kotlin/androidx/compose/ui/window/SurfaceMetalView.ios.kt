/*
 * Copyright 2026 The Android Open Source Project
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
import androidx.compose.ui.uikit.utils.CMPMetalLayer
import androidx.compose.ui.viewinterop.UIKitInteropTransaction
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ObjCClass
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Color
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectIsEmpty
import platform.CoreGraphics.CGRectZero
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UITraitCollection
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIView
import platform.UIKit.UIViewMeta

// https://youtrack.jetbrains.com/issue/CMP-9722
// Copy of the class LegacyMetalView with a different layer.
// All changes made here must also be implemented in the `LegacyMetalView`.
internal class SurfaceMetalView(
    retrieveInteropTransaction: () -> UIKitInteropTransaction,
    render: (Canvas, nanoTime: Long) -> Unit,
) : UIView(frame = CGRectZero.readValue()) {
    companion object : UIViewMeta() {
        @OptIn(BetaInteropApi::class)
        override fun layerClass(): ObjCClass = CMPMetalLayer
    }

    private val metalLayer: CMPMetalLayer get() = layer as CMPMetalLayer
    private var canvasBackground: Int = Color.TRANSPARENT

    val holder: MetalViewHolder get() = SurfaceMetalViewHolder(this)

    val redrawer = SurfaceMetalRedrawer(
        metalLayer,
        retrieveInteropTransaction,
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

        metalLayer.contentsScale = this.contentScaleFactor

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
        cancelPendingDrawableDrain()
        redrawer.dispose()
    }

    private var drainJob: Job? = null

    private fun scheduleDrawableDrain() {
        drainJob?.cancel()
        drainJob = MainScope().launch {
            // Await a safe time to ensure the metal view won't be displayed again soon
            delay(500)
            if (window == null) {
                redrawer.drainSkiaSurfaces()
            }
        }
    }

    private fun cancelPendingDrawableDrain() {
        drainJob?.cancel()
    }

    override fun didMoveToWindow() {
        super.didMoveToWindow()

        if (window == null) {
            scheduleDrawableDrain()
            return
        }

        cancelPendingDrawableDrain()

        val screen = window?.screen ?: return
        redrawer.displayLinkFrameRate?.maximumFramesPerSecond = screen.maximumFramesPerSecond
        redrawer.displayLinkFrameRate?.preferredFramesPerSecond = screen.maximumFramesPerSecond

        contentScaleFactor = screen.scale
        metalLayer.contentsScale = screen.scale
    }

    override fun layoutSubviews() {
        super.layoutSubviews()

        updateMetalLayerSize()
    }

    override fun setFrame(frame: CValue<CGRect>) {
        super.setFrame(frame)

        updateMetalLayerSize()
    }

    private fun updateMetalLayerSize() {
        if (CGRectIsEmpty(bounds)) {
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

private class SurfaceMetalViewHolder(
    private val metalView: SurfaceMetalView
): MetalViewHolder {
    override val view: UIView get() = metalView
    override val redrawer: MetalRedrawer get() = metalView.redrawer
    override var canBeOpaque: Boolean by metalView::canBeOpaque
    override fun dispose() = metalView.dispose()
}
