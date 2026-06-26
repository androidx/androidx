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

package androidx.compose.ui.scene

import androidx.compose.ui.awt.toAwtRectangle
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.awt.toAwtColor
import androidx.compose.ui.scene.skia.SkiaLayerComponent
import androidx.compose.ui.scene.skia.SwingSkiaLayerComponent
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.window.density
import androidx.compose.ui.window.sizeInPx
import java.awt.Dimension
import java.awt.Graphics
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JLayeredPane
import org.jetbrains.skiko.SkiaLayerAnalytics

internal class SwingComposeSceneLayer(
    composeContainer: ComposeContainer,
    private val skiaLayerAnalytics: SkiaLayerAnalytics,
    density: Density,
    layoutDirection: LayoutDirection,
    focusable: Boolean,
    override var consumePointerInputOutside: Boolean = focusable,
) : DesktopComposeSceneLayer(composeContainer, density, layoutDirection) {
    private val backgroundMouseListener = object : MouseAdapter() {
        override fun mousePressed(event: MouseEvent) = onMouseEventOutside(event)
        override fun mouseReleased(event: MouseEvent) = onMouseEventOutside(event)
    }

    private val container = object : JLayeredPane() {
        override fun addNotify() {
            super.addNotify()
            mediator?.onComponentAttached()
            updateBounds()

            if (focusable) {
                mediator?.contentComponent?.requestFocusInWindow()
            }
        }

        override fun removeNotify() {
            mediator?.onComponentDetached()
            super.removeNotify()
        }

        override fun paint(g: Graphics) {
            scrimColor?.let { scrimColor ->
                g.color = scrimColor.toAwtColor()
                g.fillRect(0, 0, width, height)
            }

            // Draw content after the background
            super.paint(g)
        }

        override fun toString() = "SwingComposeSceneLayer container"
    }.also {
        it.layout = null
        it.isOpaque = false
        it.size = Dimension(windowContainer.width, windowContainer.height)
        it.addMouseListener(backgroundMouseListener)
    }

    override var mediator: ComposeSceneMediator? = null

    override var focusable: Boolean = focusable
        set(value) {
            if (field == value) return
            field = value
            mediator?.contentComponent?.isFocusable = value
            updateBounds()
        }

    override var scrimColor: Color? = null

    init {
        drawBounds = IntRect(
            0,
            0,
            windowContainer.sizeInPx.width.fastRoundToInt(),
            windowContainer.sizeInPx.height.fastRoundToInt()
        )
        mediator = ComposeSceneMediator(
            container = container,
            isWindowLevel = false,
            windowContext = composeContainer.windowContext,
            exceptionHandler = {
                composeContainer.exceptionHandler?.onException(it) ?: throw it
            },
            eventListener = eventListener,
            measureDrawLayerBounds = true,
            architectureComponentsOwner = composeContainer.architectureComponentsOwner,
            coroutineContext = composeContainer.coroutineContext,
            skiaLayerComponentFactory = ::createSkiaLayerComponent,
            composeSceneFactory = ::createComposeScene,
        ).also {
            it.onWindowTransparencyChanged(true)
            it.contentComponent.isFocusable = focusable
        }

        // TODO: Currently it works only with offscreen rendering
        // TODO: Do not clip this from main scene if layersContainer == main container
        windowContainer.add(container, JLayeredPane.POPUP_LAYER, 0)

        composeContainer.attachLayer(this)
    }

    override fun close() {
        super.close()
        composeContainer.detachLayer(this)
        mediator?.dispose()
        mediator = null

        windowContainer.remove(container)
        windowContainer.invalidate()
        windowContainer.repaint()
    }

    override fun onWindowContainerSizeChanged() {
        updateBounds()
    }

    override fun onDrawBoundsChanged() {
        updateBounds()
    }

    // Updates the bounds of the container and the content component.
    private fun updateBounds() {
        if (!isBoundsInWindowSet) {
            container.setBounds(0, 0, windowContainer.width, windowContainer.height)
            mediator?.contentComponent?.setBounds(0, 0, container.width, container.height)
        } else {
            val contentComponent = mediator?.contentComponent ?: return
            val localDrawBounds = drawBounds.toAwtRectangle(density)

            if (consumePointerInputOutside) {
                container.setBounds(0, 0, windowContainer.width, windowContainer.height)
                contentComponent.bounds = localDrawBounds
                mediator?.sceneBoundsInPx = null
            } else {
                container.bounds = localDrawBounds
                contentComponent.setBounds(0, 0, localDrawBounds.width, localDrawBounds.height)
                mediator?.sceneBoundsInPx = Rect(
                    offset = -drawBounds.topLeft.toOffset(),
                    size = windowContainer.sizeInPx
                )
            }
        }
    }

    private fun createSkiaLayerComponent(mediator: ComposeSceneMediator): SkiaLayerComponent {
        val renderDelegate = recordDrawBounds(mediator)
        return SwingSkiaLayerComponent(
            mediator = mediator,
            renderDelegate = renderDelegate,
            skiaLayerAnalytics = skiaLayerAnalytics
        )
    }

    private fun createComposeScene(mediator: ComposeSceneMediator): ComposeScene {
        val density = container.density
        return PlatformLayersComposeScene(
            frameRecomposer = mediator.frameRecomposer,
            density = density,
            // TODO: Route layout invalidations through Swing layout and draw invalidations
            // through repaint instead of collapsing both to `onComposeInvalidation`.
            invalidateLayout = mediator::onComposeInvalidation,
            invalidateDraw = mediator::onComposeInvalidation,
            layoutDirection = layoutDirection,
            composeSceneContext = composeContainer.createComposeSceneContext(
                platformContext = mediator.platformContext
            ),
        )
    }
}
